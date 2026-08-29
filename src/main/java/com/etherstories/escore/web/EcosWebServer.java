package com.etherstories.escore.web;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.hooks.AllMusicHook;
import com.etherstories.escore.managers.LoginLogManager;
import com.etherstories.escore.utils.ColorUtil;
import com.etherstories.escore.utils.TPSUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EcosWebServer {

    private final ES2UniPlugin plugin;
    private final WebSessions sessions;
    private final ChatFeed chat;
    private HttpServer http;
    private byte[] indexHtml = new byte[0];
    private final Map<UUID, Long> lastSay = new ConcurrentHashMap<>();

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
        http = HttpServer.create(new InetSocketAddress(bind, port), 0);
        http.createContext("/", this::root);
        http.createContext("/v1/info", this::info);
        http.createContext("/v1/pair", this::pair);
        http.createContext("/v1/desk", this::desk);
        http.createContext("/v1/now", this::now);
        http.createContext("/v1/logout", this::logout);
        http.createContext("/v1/search", this::search);
        http.createContext("/v1/music", this::music);
        http.createContext("/v1/chat", this::say);
        http.createContext("/v1/mute", this::mute);
        http.setExecutor(Executors.newFixedThreadPool(4));
        http.start();
        plugin.getLogger().info("ECOS Web  http://" + bind + ":" + port + "/");
    }

    public void stop() {
        if (http != null) {
            http.stop(0);
            http = null;
        }
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
        AllMusicHook.NowPlaying now = plugin.getAllMusicHook().getNowPlaying();
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
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String code = extract(body, "code");
        WebSessions.Session s = sessions.consume(code);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"码无效或过期\"}");
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
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"未连接\"}");
            return;
        }
        send(ex, 200, "application/json", buildDesk(s, ex));
    }

    private void now(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        if (sessionOf(ex) == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"未连接\"}");
            return;
        }
        send(ex, 200, "application/json", buildNow(true));
    }

    private void search(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"未连接\"}");
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
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
                send(ex, 400, "application/json", "{\"ok\":false,\"error\":\"输入歌名\"}");
                return;
            }
            pack = plugin.getAllMusicHook().searchByName(s.name(), q);
        }
        if (pack == null) {
            send(ex, 503, "application/json", "{\"ok\":false,\"error\":\"AllMusic 未加载\"}");
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
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"未连接\"}");
            return;
        }
        String id = extract(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), "id");
        if (id.isBlank()) {
            send(ex, 400, "application/json", "{\"ok\":false,\"error\":\"缺少歌曲 id\"}");
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
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"未连接\"}");
            return;
        }
        long now = System.currentTimeMillis();
        Long prev = lastSay.get(s.uuid());
        if (prev != null && now - prev < 800) {
            send(ex, 429, "application/json", "{\"ok\":false,\"error\":\"慢一点\"}");
            return;
        }
        String text = extract(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), "text");
        text = text.replace('\n', ' ').replace('\r', ' ').replace('\u00a7', ' ')
                .replaceAll("&[0-9A-Fa-fk-orK-OR#]", "").trim();
        if (text.length() > 200) text = text.substring(0, 200);
        if (text.isBlank()) {
            send(ex, 400, "application/json", "{\"ok\":false,\"error\":\"空消息\"}");
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
        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.broadcastMessage(ColorUtil.colorize("&7[Web] &f<" + name + "> " + msg)));
        send(ex, 200, "application/json", "{\"ok\":true}");
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
                    if (!ok) replies.add("未知指令");
                    else replies.add("已执行，回执在游戏里");
                } else {
                    WebCommandSender sender = new WebCommandSender(s.uuid(), s.name(), isAdmin(s));
                    ok = Bukkit.dispatchCommand(sender, run);
                    replies.addAll(sender.replies());
                    if (replies.isEmpty()) replies.add(ok ? "已执行 /" + run : "未知指令或需要上线");
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
            send(ex, 504, "application/json", "{\"ok\":false,\"error\":\"服务器忙\"}");
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

    private void mute(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"未连接\"}");
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
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
        return sessions.get(header(ex, "X-ECOS-Token"));
    }

    private String buildDesk(WebSessions.Session s, HttpExchange ex) {
        String name = s == null ? "" : s.name();
        boolean online = s != null && Bukkit.getPlayer(s.uuid()) != null;
        boolean admin = isAdmin(s);
        int mail = s == null ? 0 : plugin.getMailManager().unreadCount(s.uuid());
        boolean checked = s != null && plugin.getCheckInManager().hasCheckedInToday(s.uuid());
        int streak = s == null ? 0 : plugin.getCheckInManager().getStreak(s.uuid());
        String status = s == null ? "" : nz(plugin.getStatusManager().getStatus(s.uuid()));
        String bal = "";
        if (s != null && plugin.getVaultHook().isEnabled()) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(s.uuid());
            bal = plugin.getVaultHook().format(plugin.getVaultHook().getBalance(off));
        }
        AllMusicHook.NowPlaying now = plugin.getAllMusicHook().getNowPlaying();
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
                .append(",\"url\":\"").append(esc(online ? "" : plugin.getAllMusicHook().getPlayUrl())).append("\"");
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
        appendNowObj(sb, now, plugin.getAllMusicHook().getLyricNow());
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
        t.add("Etharia Central OS · " + (name.isBlank() ? "ECOS" : name) + " · " + (online ? "终端在线" : "网页待机"));
        t.add("在线 " + Bukkit.getOnlinePlayers().size() + " 人");
        if (!bal.isBlank()) t.add("余额 " + bal);
        t.add("邮件未读 " + mail + " · 签到" + (checked ? "已签" : "未签") + " · 连签 " + streak);
        if (status != null && !status.isBlank()) t.add("签名 「" + status + "」");
        if (now != null && now.name() != null && !now.name().isBlank() && !now.name().equals("（无）")) {
            String song = now.author() == null || now.author().isBlank() ? now.name() : now.author() + " / " + now.name();
            if (now.album() != null && !now.album().isBlank()) song += " · " + now.album();
            if (now.caller() != null && !now.caller().isBlank()) song += " · " + now.caller() + " 点的";
            t.add("点播 " + song + " · 队列 " + now.queueSize());
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

    private String buildNow(boolean wrap) {
        AllMusicHook.NowPlaying now = plugin.getAllMusicHook().getNowPlaying();
        AllMusicHook.LyricNow ly = plugin.getAllMusicHook().getLyricNow();
        StringBuilder sb = new StringBuilder(384);
        if (wrap) sb.append("{\"ok\":true,\"now\":");
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

    private static String extract(String json, String key) {
        if (json == null) return "";
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return json.replaceAll("[^0-9A-Za-z]", "").trim();
        int c = json.indexOf(':', i + needle.length());
        if (c < 0) return "";
        int q = json.indexOf('"', c + 1);
        if (q < 0) return json.substring(c + 1).replaceAll("[^0-9A-Za-z]", "").trim();
        int q2 = json.indexOf('"', q + 1);
        return q2 < 0 ? "" : json.substring(q + 1, q2);
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
