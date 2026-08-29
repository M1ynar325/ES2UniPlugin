package com.etherstories.escore.web;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.hooks.AllMusicHook;
import com.etherstories.escore.managers.LoginLogManager;
import com.etherstories.escore.utils.ColorUtil;
import com.etherstories.escore.utils.TPSUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EcosWebServer {

    private final ES2UniPlugin plugin;
    private final WebSessions sessions;
    private final ChatFeed chat;
    private HttpServer http;
    private ExecutorService pool;
    private byte[] indexHtml = new byte[0];
    private final Map<UUID, Long> lastSay = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> linkPref = new ConcurrentHashMap<>();
    private final Semaphore streams = new Semaphore(2);
    private volatile MusicSnap musicSnap;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private record MusicSnap(long at, AllMusicHook.NowPlaying now, AllMusicHook.LyricNow ly, String play) {}
    @FunctionalInterface
    private interface ExHandler { void handle(HttpExchange ex) throws Exception; }

    public EcosWebServer(ES2UniPlugin plugin, WebSessions sessions, ChatFeed chat) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.chat = chat;
    }

    public void start() throws IOException {
        if (!plugin.getConfig().getBoolean("web.enabled", true)) {
            plugin.getLogger().info("ECOS Web 已关闭（web.enabled）");
            return;
        }
        try (InputStream in = plugin.getResource("web/index.html")) {
            if (in != null) indexHtml = in.readAllBytes();
        }
        String bind = plugin.getConfig().getString("web.bind", "0.0.0.0");
        int port = plugin.getConfig().getInt("web.port", 8766);
        AtomicInteger n = new AtomicInteger();
        pool = Executors.newFixedThreadPool(16, r -> {
            Thread t = new Thread(r, "ecos-web-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        http = HttpServer.create(new InetSocketAddress(bind, port), 64);
        http.createContext("/", wrap("root", this::root));
        http.createContext("/v1/info", wrap("info", this::info));
        http.createContext("/v1/pair", wrap("pair", this::pair));
        http.createContext("/v1/desk", wrap("desk", this::desk));
        http.createContext("/v1/now", wrap("now", this::now));
        http.createContext("/v1/logout", wrap("logout", this::logout));
        http.createContext("/v1/search", wrap("search", this::search));
        http.createContext("/v1/music", wrap("music", this::music));
        http.createContext("/v1/chat", wrap("say", this::say));
        http.createContext("/v1/complete", wrap("complete", this::complete));
        http.createContext("/v1/link", wrap("link", this::link));
        http.createContext("/v1/mute", wrap("mute", this::mute));
        http.createContext("/v1/stream", wrap("stream", this::stream));
        http.setExecutor(pool);
        http.start();
        plugin.getLogger().info("ECOS Web  http://" + bind + ":" + port + "/");
    }

    public void stop() {
        if (http != null) {
            http.stop(0);
            http = null;
        }
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
    }

    private HttpHandler wrap(String name, ExHandler h) {
        return ex -> {
            try {
                h.handle(ex);
            } catch (Throwable t) {
                plugin.getLogger().warning("ECOS Web " + name + ": "
                        + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
                try {
                    if (ex.getResponseCode() == -1) {
                        send(ex, 500, "application/json", "{\"ok\":false,\"error\":\"终端暂时无法应答\"}");
                    }
                } catch (Exception ignored) {}
            } finally {
                try { ex.close(); } catch (Exception ignored) {}
            }
        };
    }

    private void root(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "text/plain", "method");
            return;
        }
        String path = ex.getRequestURI().getPath();
        if (!"/".equals(path) && !"/index.html".equals(path)) {
            send(ex, 404, "text/plain", "not found");
            return;
        }
        sendBytes(ex, 200, "text/html; charset=utf-8", indexHtml.length == 0
                ? "<!doctype html><p>missing web/index.html</p>".getBytes(StandardCharsets.UTF_8)
                : indexHtml);
    }

    private void info(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        AllMusicHook.NowPlaying now = musicSnap().now();
        String song = now == null ? "" : now.name();
        send(ex, 200, "application/json", "{\"ok\":true,\"name\":\"ECOS\",\"version\":\""
                + esc(pluginVer()) + "\",\"online\":"
                + Bukkit.getOnlinePlayers().size() + ",\"song\":\"" + esc(song) + "\"}");
    }

    private void pair(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        String body = readBody(ex);
        String code = extract(body, "code");
        WebSessions.Session s = sessions.consume(code);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"连接码无效或已过期\"}");
            return;
        }
        if (plugin.getLoginLogManager() != null) {
            plugin.getLoginLogManager().onWeb(s.name(), s.uuid(), clientIp(ex));
        }
        send(ex, 200, "application/json", "{\"ok\":true,\"token\":\"" + s.token()
                + "\",\"name\":\"" + esc(s.name()) + "\"}");
    }

    private void logout(HttpExchange ex) throws IOException {
        WebSessions.Session s = sessionOf(ex);
        if (s != null) sessions.revoke(s.uuid());
        send(ex, 200, "application/json", "{\"ok\":true}");
    }

    private void desk(HttpExchange ex) throws IOException {
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"请先接入终端\"}");
            return;
        }
        send(ex, 200, "application/json", buildDesk(s, ex));
    }

    private void now(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"请先接入终端\"}");
            return;
        }
        send(ex, 200, "application/json", buildNow(true, webOnline(s)));
    }

    private void search(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"请先接入终端\"}");
            return;
        }
        String body = readBody(ex);
        plugin.getAllMusicHook().ensureHooked(plugin.getLogger());
        String dir = extract(body, "dir");
        AllMusicHook.SearchPack pack;
        if ("next".equalsIgnoreCase(dir) || "prev".equalsIgnoreCase(dir)) {
            pack = plugin.getAllMusicHook().flipSearch(s.name(), "next".equalsIgnoreCase(dir));
        } else {
            String q = extract(body, "q");
            if (q.isBlank()) q = extract(body, "query");
            if (q.isBlank() && !body.contains("\"")) q = body.trim();
            if (q.isBlank()) {
                send(ex, 400, "application/json", "{\"ok\":false,\"error\":\"请先写下歌曲或歌手\"}");
                return;
            }
            pack = plugin.getAllMusicHook().searchByName(s.name(), q);
        }
        if (pack == null) {
            send(ex, 503, "application/json", "{\"ok\":false,\"error\":\"点歌服务尚未就绪\"}");
            return;
        }
        sendSearch(ex, pack);
    }

    private void sendSearch(HttpExchange ex, AllMusicHook.SearchPack pack) throws IOException {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"ok\":true,\"q\":\"").append(esc(pack.q())).append("\",\"page\":")
                .append(pack.page()).append(",\"pages\":").append(pack.pages())
                .append(",\"prev\":").append(pack.prev()).append(",\"next\":").append(pack.next())
                .append(",\"songs\":[");
        List<AllMusicHook.SongEntry> songs = pack.songs();
        for (int i = 0; i < songs.size(); i++) {
            AllMusicHook.SongEntry song = songs.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":\"").append(esc(song.id())).append("\",\"name\":\"")
                    .append(esc(song.name())).append("\",\"author\":\"")
                    .append(esc(song.author())).append("\",\"album\":\"")
                    .append(esc(song.album())).append("\"}");
        }
        sb.append("]}");
        send(ex, 200, "application/json", sb.toString());
    }

    private void music(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"请先接入终端\"}");
            return;
        }
        String body = readBody(ex);
        String act = extract(body, "action");
        if ("vote".equalsIgnoreCase(act)) {
            plugin.getAllMusicHook().ensureHooked(plugin.getLogger());
            runAsPlayer(s, "music vote");
            send(ex, 200, "application/json", "{\"ok\":true,\"msg\":\"已投下切歌一票\"}");
            return;
        }
        if ("next".equalsIgnoreCase(act)) {
            if (!isAdmin(s)) {
                send(ex, 403, "application/json", "{\"ok\":false,\"error\":\"切歌需要管理权限\"}");
                return;
            }
            plugin.getAllMusicHook().ensureHooked(plugin.getLogger());
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "music next"));
            send(ex, 200, "application/json", "{\"ok\":true,\"msg\":\"已切到下一首\"}");
            return;
        }
        String id = extract(body, "id");
        if (id.isBlank()) {
            send(ex, 400, "application/json", "{\"ok\":false,\"error\":\"请先选择一首歌曲\"}");
            return;
        }
        plugin.getAllMusicHook().ensureHooked(plugin.getLogger());
        Player online = Bukkit.getPlayer(s.uuid());
        plugin.getAllMusicHook().addByIdAs(plugin, s.name(), online, id);
        send(ex, 200, "application/json", "{\"ok\":true,\"via\":\"" + (online != null ? "player" : "name") + "\"}");
    }

    private void say(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"请先接入终端\"}");
            return;
        }
        String text = extract(readBody(ex), "text");
        text = text.replace('\n', ' ').replace('\r', ' ').replace('\u00a7', ' ')
                .replaceAll("&[0-9A-Fa-fk-orK-OR#]", "").trim();
        if (text.length() > 200) text = text.substring(0, 200);
        if (text.isBlank()) {
            send(ex, 400, "application/json", "{\"ok\":false,\"error\":\"请先写下想说的话\"}");
            return;
        }
        long now = System.currentTimeMillis();
        Long prev = lastSay.get(s.uuid());
        if (prev != null && now - prev < 400) {
            send(ex, 429, "application/json", "{\"ok\":false,\"error\":\"请稍候再发送\"}");
            return;
        }
        lastSay.put(s.uuid(), now);
        if (text.startsWith("/")) {
            runCommand(ex, s, text.substring(1).trim());
            return;
        }
        chat.add(s.name(), text, "web");
        final String name = s.name();
        final String msg = text;
        final UUID uid = s.uuid();
        final boolean cross = linkOn(uid);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.broadcastMessage(ColorUtil.colorize("&7[Web] &f<" + name + "> " + msg));
            if (cross) plugin.getESLinkHook().forwardChat(uid, name, "[Web] " + msg);
        });
        send(ex, 200, "application/json", "{\"ok\":true,\"link\":" + cross + "}");
    }

    private void link(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())
                && !"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"请先接入终端\"}");
            return;
        }
        if (!plugin.getESLinkHook().present()) {
            send(ex, 200, "application/json", "{\"ok\":true,\"hasLink\":false,\"link\":false}");
            return;
        }
        if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
            String body = readBody(ex);
            boolean all = body.contains("\"all\":true")
                    || "true".equalsIgnoreCase(extract(body, "all"))
                    || "all".equalsIgnoreCase(extract(body, "all"));
            linkPref.put(s.uuid(), all);
            plugin.getESLinkHook().setChatAll(s.uuid(), all);
        }
        boolean on = linkOn(s.uuid());
        send(ex, 200, "application/json", "{\"ok\":true,\"hasLink\":true,\"link\":" + on + "}");
    }

    private void runCommand(HttpExchange ex, WebSessions.Session s, String cmd) throws IOException {
        if (cmd.isBlank()) {
            send(ex, 400, "application/json", "{\"ok\":false,\"error\":\"空指令\"}");
            return;
        }
        if (cmd.length() > 180) cmd = cmd.substring(0, 180);
        final String run = cmd;
        CompletableFuture<List<String>> fut = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Player online = Bukkit.getPlayer(s.uuid());
                List<String> replies = new ArrayList<>();
                boolean ok;
                if (online != null) {
                    ok = online.performCommand(run);
                    if (!ok) replies.add("没有找到这条指令");
                    else replies.add("已为您执行，回执请在游戏中查看");
                } else {
                    WebCommandSender sender = new WebCommandSender(s.uuid(), s.name(), isAdmin(s));
                    ok = Bukkit.dispatchCommand(sender, run);
                    replies.addAll(sender.replies());
                    if (replies.isEmpty()) replies.add(ok ? "已为您执行 /" + run : "没有找到这条指令，或需要进入游戏");
                }
                chat.add(s.name(), "/" + run, "cmd", s.uuid());
                for (String line : replies) chat.add("系统", line, "sys", s.uuid());
                if (plugin.getAuditLogManager() != null) {
                    plugin.getAuditLogManager().logRaw(s.name(), s.uuid().toString(), "WEB_CMD", run);
                }
                fut.complete(replies);
            } catch (Exception e) {
                fut.complete(List.of(e.getMessage() == null ? "失败" : e.getMessage()));
            }
        });
        List<String> replies;
        try {
            replies = fut.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            send(ex, 504, "application/json", "{\"ok\":false,\"error\":\"服务器正忙，请稍后再试\"}");
            return;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("{\"ok\":true,\"command\":true,\"replies\":[");
        for (int i = 0; i < replies.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(esc(replies.get(i))).append('"');
        }
        sb.append("]}");
        send(ex, 200, "application/json", sb.toString());
    }

    private void complete(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())
                && !"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"请先接入终端\"}");
            return;
        }
        String text = "GET".equalsIgnoreCase(ex.getRequestMethod())
                ? queryValue(ex, "text")
                : extract(readBody(ex), "text");
        text = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (text.startsWith("/")) text = text.substring(1);
        if (text.isBlank() || text.length() > 180) {
            send(ex, 200, "application/json", "{\"ok\":true,\"hints\":[]}");
            return;
        }
        final String line = text;
        CompletableFuture<List<String>> fut = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Player online = Bukkit.getPlayer(s.uuid());
                org.bukkit.command.CommandSender sender = online != null
                        ? online
                        : new WebCommandSender(s.uuid(), s.name(), isAdmin(s));
                List<String> raw = Bukkit.getCommandMap().tabComplete(sender, line);
                List<String> hints = new ArrayList<>();
                if (raw != null) {
                    for (String h : raw) {
                        if (h == null || h.isBlank()) continue;
                        hints.add(h);
                        if (hints.size() >= 40) break;
                    }
                }
                fut.complete(hints);
            } catch (Exception e) {
                fut.complete(List.of());
            }
        });
        List<String> hints;
        try {
            hints = fut.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            send(ex, 200, "application/json", "{\"ok\":true,\"hints\":[]}");
            return;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("{\"ok\":true,\"hints\":[");
        for (int i = 0; i < hints.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(esc(hints.get(i))).append('"');
        }
        sb.append("]}");
        send(ex, 200, "application/json", sb.toString());
    }

    private static String queryValue(HttpExchange ex, String key) {
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return "";
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0 || !key.equals(part.substring(0, eq))) continue;
            return URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
        }
        return "";
    }

    private void mute(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"请先接入终端\"}");
            return;
        }
        String body = readBody(ex);
        boolean muted = body.contains("\"muted\":true") || "true".equalsIgnoreCase(extract(body, "muted"));
        plugin.getAllMusicHook().ensureHooked(plugin.getLogger());
        Player online = Bukkit.getPlayer(s.uuid());
        plugin.getAllMusicHook().setMuted(plugin, s.name(), online, muted);
        send(ex, 200, "application/json", "{\"ok\":true,\"muted\":" + muted + "}");
    }

    private WebSessions.Session sessionOf(HttpExchange ex) {
        String auth = header(ex, "Authorization");
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7))
            return sessions.get(auth.substring(7).trim());
        String cookie = header(ex, "Cookie");
        if (cookie != null) {
            for (String part : cookie.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals("ecos")) return sessions.get(kv[1]);
            }
        }
        WebSessions.Session header = sessions.get(header(ex, "X-ECOS-Token"));
        if (header != null) return header;
        String raw = ex.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String part : raw.split("&")) {
                int eq = part.indexOf('=');
                if (eq <= 0 || !"token".equals(part.substring(0, eq))) continue;
                String tok = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
                WebSessions.Session q = sessions.get(tok);
                if (q != null) return q;
            }
        }
        return null;
    }

    /** Arclight 退服后 getPlayer 有时还挂着，必须连着才算在线。 */
    private static boolean webOnline(WebSessions.Session s) {
        if (s == null) return false;
        Player p = Bukkit.getPlayer(s.uuid());
        return p != null && p.isOnline() && p.isConnected();
    }

    private void stream(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())
                && !"HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "text/plain", "method");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "text/plain", "未连接");
            return;
        }
        if (webOnline(s)) {
            send(ex, 204, "text/plain", "");
            return;
        }
        String url = musicSnap().play();
        if (url == null || url.isBlank()) {
            send(ex, 204, "text/plain", "");
            return;
        }
        if ("HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 200, "audio/mpeg", "");
            return;
        }
        if (!streams.tryAcquire()) {
            send(ex, 503, "text/plain", "busy");
            return;
        }
        try {
            proxyAudio(ex, url);
        } finally {
            streams.release();
        }
    }

    private void proxyAudio(HttpExchange ex, String url) throws IOException {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Referer", "https://music.163.com/")
                    .GET()
                    .build();
            HttpResponse<InputStream> res = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() >= 400) {
                res.body().close();
                send(ex, 502, "text/plain", "upstream");
                return;
            }
            String type = res.headers().firstValue("Content-Type").orElse("audio/mpeg");
            ex.getResponseHeaders().set("Content-Type", type);
            ex.getResponseHeaders().set("Cache-Control", "no-store");
            ex.sendResponseHeaders(200, 0);
            byte[] buf = new byte[8192];
            long n = 0;
            try (InputStream in = res.body(); OutputStream out = ex.getResponseBody()) {
                int r;
                while (n < 20_000_000 && (r = in.read(buf)) >= 0) {
                    out.write(buf, 0, r);
                    n += r;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (ex.getResponseCode() == -1) send(ex, 502, "text/plain", "stream");
        } catch (Exception e) {
            if (ex.getResponseCode() == -1) send(ex, 502, "text/plain", "stream");
        }
    }

    private String buildDesk(WebSessions.Session s, HttpExchange ex) {
        String name = s == null ? "" : s.name();
        boolean online = webOnline(s);
        boolean admin = isAdmin(s);
        int mail = 0;
        if (s != null && plugin.getMailManager() != null) mail = plugin.getMailManager().unreadCount(s.uuid());
        boolean checked = s != null && plugin.getCheckInManager() != null
                && plugin.getCheckInManager().hasCheckedInToday(s.uuid());
        int streak = s == null || plugin.getCheckInManager() == null
                ? 0 : plugin.getCheckInManager().getStreak(s.uuid());
        String status = s == null || plugin.getStatusManager() == null
                ? "" : nz(plugin.getStatusManager().getStatus(s.uuid()));
        String bal = "";
        if (s != null && plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(s.uuid());
            bal = plugin.getVaultHook().format(plugin.getVaultHook().getBalance(off));
        }
        MusicSnap snap = musicSnap();
        AllMusicHook.NowPlaying now = snap.now();
        StringBuilder sb = new StringBuilder(1536);
        String skin = "";
        if (s != null) {
            Player self = Bukkit.getPlayer(s.uuid());
            skin = self != null ? skinOf(self) : "https://mc-heads.net/skin/" + name;
        }
        sb.append("{\"ok\":true,\"name\":\"").append(esc(name)).append("\",\"version\":\"")
                .append(esc(pluginVer())).append("\",\"online\":")
                .append(online).append(",\"admin\":").append(admin)
                .append(",\"uuid\":\"").append(s == null ? "" : s.uuid()).append('"')
                .append(",\"skin\":\"").append(esc(skin)).append('"')
                .append(",\"players\":").append(Bukkit.getOnlinePlayers().size())
                .append(",\"mail\":").append(mail)
                .append(",\"checked\":").append(checked)
                .append(",\"streak\":").append(streak)
                .append(",\"status\":\"").append(esc(status)).append("\"")
                .append(",\"balance\":\"").append(esc(bal)).append("\"")
                .append(",\"map\":\"").append(esc(mapUrl(ex))).append("\"")
                .append(",\"muted\":").append(plugin.getAllMusicHook().isMuted(name))
                .append(",\"hasLink\":").append(plugin.getESLinkHook().present())
                .append(",\"link\":").append(s != null && linkOn(s.uuid()));
        String play = online ? "" : snap.play();
        String stream = "";
        if (play != null && !play.isBlank() && now != null && now.id() != null && !now.id().isBlank()) {
            stream = "/v1/stream?id=" + now.id();
        } else if (play != null && !play.isBlank()) {
            stream = "/v1/stream?id=live";
        }
        sb.append(",\"url\":\"").append(esc(stream)).append("\"");
        if (admin) {
            sb.append(",\"tps\":").append(String.format(java.util.Locale.US, "%.2f", TPSUtil.getTPS()))
                    .append(",\"mspt\":").append(String.format(java.util.Locale.US, "%.1f", TPSUtil.getMSPT()));
        }
        sb.append(",\"tape\":[");
        List<String> tape = tapeLines(name, online, mail, checked, streak, status, bal, now);
        for (int i = 0; i < tape.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(esc(tape.get(i))).append('"');
        }
        sb.append(']');
        sb.append(",\"who\":[");
        int wi = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (wi++ > 0) sb.append(',');
            var loc = p.getLocation();
            String world = loc.getWorld() == null ? "" : loc.getWorld().getName();
            sb.append("{\"name\":\"").append(esc(p.getName()))
                    .append("\",\"uuid\":\"").append(p.getUniqueId()).append('"')
                    .append(",\"skin\":\"").append(esc(skinOf(p))).append('"')
                    .append(",\"head\":\"https://mc-heads.net/head/").append(esc(p.getName())).append("/80\"")
                    .append(",\"world\":\"").append(esc(world))
                    .append("\",\"x\":").append(loc.getBlockX())
                    .append(",\"y\":").append(loc.getBlockY())
                    .append(",\"z\":").append(loc.getBlockZ());
            if (admin && p.getAddress() != null && p.getAddress().getAddress() != null) {
                sb.append(",\"ip\":\"").append(esc(p.getAddress().getAddress().getHostAddress())).append('"');
            }
            sb.append('}');
        }
        sb.append(']');
        sb.append(",\"now\":");
        appendNowObj(sb, now, snap.ly());
        sb.append(",\"queue\":[");
        List<AllMusicHook.QueueSong> q = plugin.getAllMusicHook().getQueue();
        for (int i = 0; i < q.size(); i++) {
            AllMusicHook.QueueSong song = q.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":\"").append(esc(song.id())).append("\",\"name\":\"")
                    .append(esc(song.name())).append("\",\"author\":\"").append(esc(song.author()))
                    .append("\",\"album\":\"").append(esc(song.album()))
                    .append("\",\"caller\":\"").append(esc(song.caller())).append("\"}");
        }
        sb.append("],\"chat\":[");
        UUID viewer = s == null ? null : s.uuid();
        List<ChatFeed.Line> lines = chat.recent(viewer, admin);
        for (int i = 0; i < lines.size(); i++) {
            ChatFeed.Line line = lines.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"ts\":").append(line.ts()).append(",\"name\":\"")
                    .append(esc(line.name())).append("\",\"text\":\"").append(esc(line.text()))
                    .append("\",\"web\":").append(line.web())
                    .append(",\"kind\":\"").append(esc(line.kind())).append("\"}");
        }
        if (admin) {
            sb.append("],\"logins\":[");
            List<LoginLogManager.Event> logins = plugin.getLoginLogManager() == null
                    ? List.of() : plugin.getLoginLogManager().recent(12);
            for (int i = 0; i < logins.size(); i++) {
                LoginLogManager.Event ev = logins.get(i);
                if (i > 0) sb.append(',');
                sb.append("{\"ts\":").append(ev.ts()).append(",\"name\":\"")
                        .append(esc(ev.name())).append("\",\"ip\":\"").append(esc(ev.ip()))
                        .append("\",\"kind\":\"").append(esc(ev.kind()))
                        .append("\",\"note\":\"").append(esc(ev.note())).append("\"}");
            }
            sb.append("],\"logs\":[");
            List<String> logs = plugin.getAuditLogManager() == null
                    ? List.of() : plugin.getAuditLogManager().getRecent(12);
            for (int i = 0; i < logs.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(esc(logs.get(i))).append('"');
            }
            sb.append("]}");
        } else {
            sb.append("]}");
        }
        return sb.toString();
    }

    private List<String> tapeLines(String name, boolean online, int mail, boolean checked,
                                  int streak, String status, String bal, AllMusicHook.NowPlaying now) {
        List<String> t = new ArrayList<>();
        t.add("Etharia Central OS · " + (name.isBlank() ? "ECOS" : name) + " · "
                + (online ? "您正在游戏中" : "您尚未进入游戏"));
        t.add("此刻在线 " + Bukkit.getOnlinePlayers().size() + " 人");
        if (!bal.isBlank()) t.add("余额 " + bal);
        t.add("未读来信 " + mail + " · 签到" + (checked ? "今日已签" : "今日尚未签到") + " · 连签 " + streak + " 日");
        if (status != null && !status.isBlank()) t.add("签名 「" + status + "」");
        if (now != null && now.name() != null && !now.name().isBlank() && !now.name().equals("（无）")) {
            String song = now.author() == null || now.author().isBlank() ? now.name() : now.author() + " / " + now.name();
            if (now.album() != null && !now.album().isBlank()) song += " · " + now.album();
            if (now.caller() != null && !now.caller().isBlank()) song += " · 由 " + now.caller() + " 点播";
            t.add("正在播放 " + song + " · 队列 " + now.queueSize());
        }
        List<ChatFeed.Line> lines = chat.recent(null, false);
        if (!lines.isEmpty()) {
            ChatFeed.Line last = lines.get(lines.size() - 1);
            String tag = last.web() ? "[Web] " : ("chat".equals(last.kind()) ? "" : "[" + last.kind() + "] ");
            t.add(tag + ("chat".equals(last.kind()) || last.web()
                    ? "<" + last.name() + "> " : "") + last.text());
        }
        return t;
    }

    private String buildNow(boolean wrap, boolean online) {
        MusicSnap snap = musicSnap();
        AllMusicHook.NowPlaying now = snap.now();
        AllMusicHook.LyricNow ly = snap.ly();
        StringBuilder sb = new StringBuilder(384);
        if (wrap) sb.append("{\"ok\":true,\"online\":").append(online).append(",\"now\":");
        appendNowObj(sb, now, ly);
        if (wrap) sb.append('}');
        return sb.toString();
    }

    private static void appendNowObj(StringBuilder sb, AllMusicHook.NowPlaying now, AllMusicHook.LyricNow ly) {
        if (now == null) {
            sb.append("null");
            return;
        }
        if (ly == null) ly = new AllMusicHook.LyricNow("", "", "", "", 0, 0);
        sb.append("{\"id\":\"").append(esc(now.id())).append("\",\"name\":\"")
                .append(esc(now.name())).append("\",\"author\":\"").append(esc(now.author()))
                .append("\",\"album\":\"").append(esc(now.album()))
                .append("\",\"pic\":\"").append(esc(now.pic()))
                .append("\",\"caller\":\"").append(esc(now.caller()))
                .append("\",\"queue\":").append(now.queueSize())
                .append(",\"lyric\":\"").append(esc(ly.lyric()))
                .append("\",\"tlyric\":\"").append(esc(ly.tlyric()))
                .append("\",\"prev\":\"").append(esc(ly.prev()))
                .append("\",\"next\":\"").append(esc(ly.next()))
                .append("\",\"now\":").append(ly.nowMs())
                .append(",\"all\":").append(ly.allMs()).append('}');
    }

    private static String skinOf(Player p) {
        if (p == null) return "https://mc-heads.net/skin/Steve";
        try {
            var url = p.getPlayerProfile().getTextures().getSkin();
            if (url != null) return url.toString();
        } catch (Throwable ignored) {
        }
        return "https://mc-heads.net/skin/" + p.getName();
    }

    private boolean isAdmin(WebSessions.Session s) {
        if (s == null) return false;
        Player p = Bukkit.getPlayer(s.uuid());
        if (p != null) return p.isOp() || p.hasPermission("es2uni.admin");
        if (s.admin()) return true;
        return Bukkit.getOfflinePlayer(s.uuid()).isOp();
    }

    private boolean linkOn(UUID uuid) {
        if (uuid == null || !plugin.getESLinkHook().present()) return false;
        Boolean from = plugin.getESLinkHook().isChatAll(uuid);
        if (from != null) return from;
        return linkPref.getOrDefault(uuid, false);
    }

    private void runAsPlayer(WebSessions.Session s, String cmd) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(s.uuid());
            if (online != null) {
                online.performCommand(cmd);
                return;
            }
            Bukkit.dispatchCommand(new WebCommandSender(s.uuid(), s.name(), isAdmin(s)), cmd);
        });
    }

    private static String clientIp(HttpExchange ex) {
        String fwd = header(ex, "X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            int comma = fwd.indexOf(',');
            return (comma < 0 ? fwd : fwd.substring(0, comma)).trim();
        }
        if (ex.getRemoteAddress() == null || ex.getRemoteAddress().getAddress() == null) return "";
        return ex.getRemoteAddress().getAddress().getHostAddress();
    }

    /** 空 map-url 则用访问主机:8100（BlueMap 默认） */
    private String mapUrl(HttpExchange ex) {
        String cfg = plugin.getConfig().getString("web.map-url", "");
        if (cfg != null && !cfg.isBlank()) return cfg.trim();
        String host = header(ex, "Host");
        String name = "127.0.0.1";
        if (host != null && !host.isBlank()) {
            if (host.startsWith("[")) {
                int end = host.indexOf(']');
                name = end > 0 ? host.substring(0, end + 1) : host;
            } else {
                int colon = host.lastIndexOf(':');
                name = colon > 0 ? host.substring(0, colon) : host;
            }
        }
        return "http://" + name + ":8100/";
    }

    private static String header(HttpExchange ex, String name) {
        return ex.getRequestHeaders().getFirst(name);
    }

    private MusicSnap musicSnap() {
        MusicSnap s = musicSnap;
        long now = System.currentTimeMillis();
        if (s != null && now - s.at() < 300) return s;
        synchronized (this) {
            s = musicSnap;
            if (s != null && now - s.at() < 300) return s;
            AllMusicHook hook = plugin.getAllMusicHook();
            musicSnap = new MusicSnap(now, hook.getNowPlaying(), hook.getLyricNow(), hook.getPlayUrl());
            return musicSnap;
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        byte[] raw = ex.getRequestBody().readNBytes(8193);
        if (raw.length > 8192) return "";
        return new String(raw, StandardCharsets.UTF_8);
    }

    private static String extract(String json, String key) {
        if (json == null || key == null) return "";
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return "";
        int c = json.indexOf(':', i + needle.length());
        if (c < 0) return "";
        int j = c + 1;
        while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
        if (j >= json.length()) return "";
        if (json.charAt(j) != '"') {
            int k = j;
            while (k < json.length()) {
                char ch = json.charAt(k);
                if (ch == ',' || ch == '}' || Character.isWhitespace(ch)) break;
                k++;
            }
            return json.substring(j, k);
        }
        StringBuilder b = new StringBuilder();
        for (int p = j + 1; p < json.length(); p++) {
            char ch = json.charAt(p);
            if (ch == '\\' && p + 1 < json.length()) {
                char n = json.charAt(++p);
                switch (n) {
                    case 'n' -> b.append('\n');
                    case 'r' -> b.append('\r');
                    case 't' -> b.append('\t');
                    case '"' -> b.append('"');
                    case '\\' -> b.append('\\');
                    case '/' -> b.append('/');
                    case 'u' -> {
                        if (p + 4 < json.length()) {
                            try {
                                b.append((char) Integer.parseInt(json.substring(p + 1, p + 5), 16));
                                p += 4;
                            } catch (NumberFormatException e) {
                                b.append('u');
                            }
                        } else {
                            b.append('u');
                        }
                    }
                    default -> b.append(n);
                }
                continue;
            }
            if (ch == '"') break;
            b.append(ch);
        }
        return b.toString();
    }

    private String pluginVer() {
        return plugin.getDescription().getVersion();
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> b.append("\\\\");
                case '"' -> b.append("\\\"");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 32) b.append(' ');
                    else b.append(c);
                }
            }
        }
        return b.toString();
    }

    private static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        sendBytes(ex, code, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendBytes(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }

}
