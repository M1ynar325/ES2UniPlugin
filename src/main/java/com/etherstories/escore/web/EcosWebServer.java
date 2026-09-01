package com.etherstories.escore.web;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.hooks.AllMusicHook;
import com.etherstories.escore.managers.LoginLogManager;
import com.etherstories.escore.managers.MusicFavoritesManager;
import com.etherstories.escore.managers.MusicHistoryManager;
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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.Callable;

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
        http.createContext("/v1/library", wrap("library", this::library));
        http.createContext("/v1/life", wrap("life", this::life));
        http.createContext("/v1/act", wrap("act", this::act));
        http.createContext("/v1/music", wrap("music", this::music));
        http.createContext("/v1/chat", wrap("say", this::say));
        http.createContext("/v1/complete", wrap("complete", this::complete));
        http.createContext("/v1/link", wrap("link", this::link));
        http.createContext("/v1/mute", wrap("mute", this::mute));
        http.createContext("/v1/stream", wrap("stream", this::stream));
        http.createContext("/v1/skin", wrap("skin", this::skin));
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
        String json = sync(() -> buildDesk(s, ex));
        if (json == null) {
            send(ex, 500, "application/json", "{\"ok\":false,\"error\":\"终端暂时无法应答\"}");
            return;
        }
        send(ex, 200, "application/json; charset=utf-8", json);
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
        String json = sync(() -> buildNow(true, webOnline(s)));
        if (json == null) {
            send(ex, 500, "application/json", "{\"ok\":false,\"error\":\"终端暂时无法应答\"}");
            return;
        }
        send(ex, 200, "application/json; charset=utf-8", json);
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
        sendSearch(ex, pack, s.uuid());
    }

    private void sendSearch(HttpExchange ex, AllMusicHook.SearchPack pack, UUID uuid) throws IOException {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"ok\":true,\"q\":\"").append(esc(pack.q())).append("\",\"page\":")
                .append(pack.page()).append(",\"pages\":").append(pack.pages())
                .append(",\"prev\":").append(pack.prev()).append(",\"next\":").append(pack.next())
                .append(",\"songs\":[");
        List<AllMusicHook.SongEntry> songs = pack.songs();
        var favs = plugin.getMusicFavoritesManager();
        for (int i = 0; i < songs.size(); i++) {
            AllMusicHook.SongEntry song = songs.get(i);
            if (i > 0) sb.append(',');
            boolean fav = uuid != null && favs != null && favs.has(uuid, song.id());
            sb.append("{\"id\":\"").append(esc(song.id())).append("\",\"name\":\"")
                    .append(esc(song.name())).append("\",\"author\":\"")
                    .append(esc(song.author())).append("\",\"album\":\"")
                    .append(esc(song.album())).append("\",\"fav\":").append(fav).append('}');
        }
        sb.append("]}");
        send(ex, 200, "application/json", sb.toString());
    }

    private void library(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"请先接入终端\"}");
            return;
        }
        String json = sync(() -> buildLibrary(s.uuid()));
        if (json == null) {
            send(ex, 500, "application/json", "{\"ok\":false,\"error\":\"终端暂时无法应答\"}");
            return;
        }
        send(ex, 200, "application/json; charset=utf-8", json);
    }

    private String buildLibrary(UUID uuid) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"ok\":true,\"favorites\":[");
        var favs = plugin.getMusicFavoritesManager() == null
                ? List.<MusicFavoritesManager.FavSong>of()
                : plugin.getMusicFavoritesManager().list(uuid);
        for (int i = 0; i < favs.size(); i++) {
            var song = favs.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":\"").append(esc(song.id())).append("\",\"name\":\"")
                    .append(esc(song.name())).append("\",\"author\":\"")
                    .append(esc(song.author())).append("\",\"album\":\"")
                    .append(esc(song.album())).append("\"}");
        }
        sb.append("],\"history\":[");
        var hist = plugin.getMusicHistoryManager() == null
                ? List.<MusicHistoryManager.Entry>of()
                : plugin.getMusicHistoryManager().recent();
        for (int i = 0; i < hist.size(); i++) {
            var e = hist.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":\"").append(esc(nz(e.id()))).append("\",\"name\":\"")
                    .append(esc(nz(e.name()))).append("\",\"author\":\"")
                    .append(esc(nz(e.author()))).append("\",\"album\":\"")
                    .append("\",\"caller\":\"").append(esc(nz(e.caller())))
                    .append("\",\"time\":\"").append(esc(nz(e.time()))).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private void life(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "application/json", "{\"ok\":false}");
            return;
        }
        WebSessions.Session s = sessionOf(ex);
        if (s == null) {
            send(ex, 401, "application/json", "{\"ok\":false,\"error\":\"请先接入终端\"}");
            return;
        }
        String json = sync(() -> buildLife(s));
        if (json == null) {
            send(ex, 500, "application/json", "{\"ok\":false,\"error\":\"终端暂时无法应答\"}");
            return;
        }
        send(ex, 200, "application/json; charset=utf-8", json);
    }

    private String buildLife(WebSessions.Session s) {
        UUID u = s.uuid();
        StringBuilder sb = new StringBuilder(2048);
        boolean checked = plugin.getCheckInManager() != null
                && plugin.getCheckInManager().hasCheckedInToday(u);
        int streak = plugin.getCheckInManager() == null ? 0 : plugin.getCheckInManager().getStreak(u);
        int mk = plugin.getCheckInManager() == null ? 0 : plugin.getCheckInManager().getMakeupTickets(u);
        String status = plugin.getStatusManager() == null ? "" : nz(plugin.getStatusManager().getStatus(u));
        String bal = "";
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled())
            bal = plugin.getVaultHook().format(plugin.getVaultHook().getBalance(Bukkit.getOfflinePlayer(u)));
        String stay = plugin.getHotelManager() == null ? "" : nz(plugin.getHotelManager().stayLabel(u));
        sb.append("{\"ok\":true,\"checked\":").append(checked)
                .append(",\"streak\":").append(streak)
                .append(",\"makeup\":").append(mk)
                .append(",\"status\":\"").append(esc(status)).append('"')
                .append(",\"balance\":\"").append(esc(bal)).append('"')
                .append(",\"stay\":\"").append(esc(stay)).append('"')
                .append(",\"share\":").append(plugin.getFriendManager() != null
                        && plugin.getFriendManager().isLocationSharing(u))
                .append(",\"played\":\"").append(esc(plugin.getPlaytimeManager() == null
                        ? "" : plugin.getPlaytimeManager().getTotalFormatted(u))).append('"')
                .append(",\"taxOn\":").append(plugin.getTaxManager() != null
                        && plugin.getTaxManager().isPayTaxEnabled())
                .append(",\"taxRate\":").append(plugin.getTaxManager() == null
                        ? 0 : plugin.getTaxManager().getPayTaxRate())
                .append(",\"recycle\":").append(plugin.getRecycleBinManager() == null
                        ? 0 : plugin.getRecycleBinManager().countFor(u))
                .append(",\"rideHint\":\"").append(esc(plugin.getTransitManager() == null
                        ? "" : nz(plugin.getTransitManager().journeyHint(u)))).append('"')
                .append(",\"tap\":").append(plugin.getTransitManager() != null
                        && plugin.getTransitManager().isTapPay(u));
        sb.append(",\"mail\":[");
        var box = plugin.getMailManager() == null ? List.<com.etherstories.escore.managers.MailManager.Mail>of()
                : plugin.getMailManager().getMails(u);
        for (int i = 0; i < box.size(); i++) {
            var m = box.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"i\":").append(i)
                    .append(",\"from\":\"").append(esc(m.senderName()))
                    .append("\",\"date\":\"").append(esc(m.date()))
                    .append("\",\"read\":").append(m.read())
                    .append(",\"text\":\"").append(esc(m.content())).append("\"}");
        }
        sb.append("],\"friends\":[");
        var fm = plugin.getFriendManager();
        if (fm != null) {
            int fi = 0;
            for (UUID id : fm.getFriends(u)) {
                if (fi++ > 0) sb.append(',');
                var off = Bukkit.getOfflinePlayer(id);
                String name = off.getName() == null ? fm.getDisplayName(id) : off.getName();
                Player on = off.getPlayer();
                sb.append("{\"id\":\"").append(id)
                        .append("\",\"name\":\"").append(esc(name))
                        .append("\",\"online\":").append(on != null && on.isOnline())
                        .append(",\"share\":").append(fm.isLocationSharing(id));
                if (on != null && on.isOnline() && fm.isLocationSharing(id)) {
                    var loc = on.getLocation();
                    String w = loc.getWorld() == null ? "" : loc.getWorld().getName();
                    sb.append(",\"loc\":\"").append(esc(w + " " + loc.getBlockX()
                            + " " + loc.getBlockY() + " " + loc.getBlockZ())).append('"');
                }
                sb.append('}');
            }
        }
        sb.append("],\"reqs\":[");
        if (fm != null) {
            int ri = 0;
            for (UUID id : fm.getPendingRequests(u)) {
                if (ri++ > 0) sb.append(',');
                var off = Bukkit.getOfflinePlayer(id);
                String name = off.getName() == null ? "?" : off.getName();
                sb.append("{\"id\":\"").append(id)
                        .append("\",\"name\":\"").append(esc(name)).append("\"}");
            }
        }
        sb.append("],\"notices\":[");
        var notices = plugin.getNoticeManager() == null ? List.<com.etherstories.escore.managers.NoticeManager.Notice>of()
                : plugin.getNoticeManager().getNotices();
        for (int i = 0; i < notices.size(); i++) {
            var n = notices.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"from\":\"").append(esc(n.author()))
                    .append("\",\"date\":\"").append(esc(n.date()))
                    .append("\",\"text\":\"").append(esc(n.content())).append("\"}");
        }
        sb.append("],\"jobs\":[");
        if (plugin.getJobBoardManager() != null) {
            int ji = 0;
            for (var j : plugin.getJobBoardManager().listActive()) {
                if (ji++ > 0) sb.append(',');
                sb.append("{\"id\":\"").append(esc(j.id))
                        .append("\",\"title\":\"").append(esc(j.title))
                        .append("\",\"who\":\"").append(esc(j.posterName))
                        .append("\",\"slots\":\"").append(esc(j.slotsLabel()))
                        .append("\",\"pay\":\"").append(esc(money(j.reward)))
                        .append("\",\"mine\":").append(j.workers.contains(u)).append('}');
            }
        }
        sb.append("],\"events\":[");
        if (plugin.getEventManager() != null) {
            int ei = 0;
            for (var ev : plugin.getEventManager().getAllEvents()) {
                if (ei++ > 0) sb.append(',');
                sb.append("{\"id\":\"").append(esc(ev.getId()))
                        .append("\",\"name\":\"").append(esc(ev.getName()))
                        .append("\",\"n\":").append(ev.getParticipants().size())
                        .append(",\"max\":").append(ev.getMaxParticipants())
                        .append(",\"in\":").append(ev.hasJoined(u)).append('}');
            }
        }
        sb.append("],\"show\":[");
        if (plugin.getShowcaseManager() != null) {
            int si = 0;
            for (var sc : plugin.getShowcaseManager().topByVotes(20)) {
                if (si++ > 0) sb.append(',');
                sb.append("{\"id\":\"").append(esc(sc.id()))
                        .append("\",\"title\":\"").append(esc(sc.title()))
                        .append("\",\"who\":\"").append(esc(sc.ownerName()))
                        .append("\",\"votes\":").append(sc.votes()).append('}');
            }
        }
        sb.append("],\"play\":[");
        if (plugin.getPlaytimeManager() != null) {
            int pi = 0;
            for (var e : plugin.getPlaytimeManager().getTopPlayers(10)) {
                if (pi++ > 0) sb.append(',');
                var off = Bukkit.getOfflinePlayer(e.getKey());
                String name = off.getName() == null ? "?" : off.getName();
                sb.append("{\"name\":\"").append(esc(name))
                        .append("\",\"v\":\"").append(esc(
                                com.etherstories.escore.managers.PlaytimeManager.formatMillis(e.getValue())))
                        .append("\"}");
            }
        }
        sb.append("],\"trade\":[");
        if (plugin.getTradeStatsManager() != null) {
            int ti = 0;
            for (var e : plugin.getTradeStatsManager().getTopPlayers(10)) {
                if (ti++ > 0) sb.append(',');
                sb.append("{\"name\":\"").append(esc(e.name()))
                        .append("\",\"v\":\"").append(esc(money(e.volume()))).append("\"}");
            }
        }
        sb.append("],\"rides\":[");
        if (plugin.getTransitManager() != null) {
            int ri = 0;
            for (var e : plugin.getTransitManager().topRiders(10)) {
                if (ri++ > 0) sb.append(',');
                sb.append("{\"name\":\"").append(esc(e.name()))
                        .append("\",\"v\":\"").append(e.rides()).append(" 次\"}");
            }
        }
        sb.append("],\"deaths\":[");
        if (plugin.getDeathManager() != null) {
            int di = 0;
            for (var d : plugin.getDeathManager().get(u)) {
                if (di++ > 0) sb.append(',');
                sb.append("{\"w\":\"").append(esc(d.world()))
                        .append("\",\"x\":").append(d.x())
                        .append(",\"y\":").append(d.y())
                        .append(",\"z\":").append(d.z())
                        .append(",\"cause\":\"").append(esc(d.cause()))
                        .append("\",\"date\":\"").append(esc(d.date())).append("\"}");
            }
        }
        sb.append("],\"wps\":[");
        if (plugin.getWaypointManager() != null) {
            int wi = 0;
            for (var w : plugin.getWaypointManager().get(u)) {
                if (wi++ > 0) sb.append(',');
                sb.append("{\"name\":\"").append(esc(w.name()))
                        .append("\",\"w\":\"").append(esc(w.world()))
                        .append("\",\"x\":").append(w.x())
                        .append(",\"y\":").append(w.y())
                        .append(",\"z\":").append(w.z()).append('}');
            }
        }
        sb.append("],\"ins\":[");
        if (plugin.getInsuranceManager() != null) {
            int ii = 0;
            for (var b : plugin.getInsuranceManager().getHistory(u)) {
                if (ii++ > 0) sb.append(',');
                sb.append("{\"when\":\"").append(esc(b.timeLabel()))
                        .append("\",\"n\":").append(b.itemCount()).append('}');
            }
        }
        var cover = plugin.getInsuranceManager() == null
                ? null : plugin.getInsuranceManager().lastDeath(u);
        sb.append("],\"cover\":").append(cover == null ? "null"
                : "{\"ok\":" + cover.covered()
                + ",\"kind\":\"" + esc(cover.kind())
                + "\",\"place\":\"" + esc(cover.place()) + "\"}");
        sb.append(",\"histRide\":[");
        if (plugin.getTransitManager() != null) {
            int hi = 0;
            for (var r : plugin.getTransitManager().ridesOf(u)) {
                if (hi++ >= 12) break;
                if (hi > 1) sb.append(',');
                sb.append("{\"from\":\"").append(esc(plugin.getTransitManager().stationName(r.fromId())))
                        .append("\",\"to\":\"").append(esc(plugin.getTransitManager().stationName(r.toId())))
                        .append("\",\"fare\":\"").append(esc(money(r.fare()))).append("\"}");
            }
        }
        sb.append("],\"stops\":[");
        if (plugin.getTransitManager() != null) {
            int si = 0;
            for (var st : plugin.getTransitManager().allStations()) {
                if (si++ > 0) sb.append(',');
                sb.append("{\"id\":\"").append(esc(st.id()))
                        .append("\",\"name\":\"").append(esc(st.displayName())).append("\"}");
            }
        }
        sb.append("],\"homes\":[");
        if (plugin.getEstateManager() != null) {
            int ei = 0;
            for (var e : plugin.getEstateManager().ownedBy(u)) {
                if (ei++ > 0) sb.append(',');
                sb.append("{\"id\":\"").append(esc(e.id()))
                        .append("\",\"addr\":\"").append(esc(e.address()))
                        .append("\",\"sale\":").append(e.listed())
                        .append(",\"price\":\"").append(esc(e.listed() ? money(e.price()) : "")).append("\"}");
            }
        }
        sb.append("],\"sale\":[");
        if (plugin.getEstateManager() != null) {
            int si = 0;
            for (var e : plugin.getEstateManager().listedForSale()) {
                if (si++ >= 20) break;
                if (si > 1) sb.append(',');
                sb.append("{\"addr\":\"").append(esc(e.address()))
                        .append("\",\"who\":\"").append(esc(nz(e.ownerName())))
                        .append("\",\"price\":\"").append(esc(money(e.price()))).append("\"}");
            }
        }
        sb.append("],\"kits\":[");
        if (plugin.getKitManager() != null) {
            int ki = 0;
            for (var k : plugin.getKitManager().all()) {
                if (ki++ > 0) sb.append(',');
                sb.append("{\"name\":\"").append(esc(k.name))
                        .append("\",\"rule\":\"").append(esc(plugin.getKitManager().describeRule(k)))
                        .append("\",\"n\":").append(k.items == null ? 0 : k.items.size()).append('}');
            }
        }
        sb.append("],\"lands\":[");
        if (plugin.getRegionManager() != null) {
            int li = 0;
            for (var r : plugin.getRegionManager().getByOwner(u)) {
                if (li++ > 0) sb.append(',');
                sb.append("{\"name\":\"").append(esc(r.name()))
                        .append("\",\"vis\":").append(r.visible())
                        .append(",\"area\":").append(r.area()).append('}');
            }
        }
        sb.append("],\"pubs\":[");
        if (plugin.getRegionManager() != null) {
            int pi = 0;
            for (var r : plugin.getRegionManager().getPublicTerritories()) {
                if (pi++ >= 20) break;
                if (pi > 1) sb.append(',');
                String who = r.owner() == null ? "" : nz(Bukkit.getOfflinePlayer(r.owner()).getName());
                sb.append("{\"name\":\"").append(esc(r.name()))
                        .append("\",\"who\":\"").append(esc(who)).append("\"}");
            }
        }
        sb.append("],\"auras\":[");
        if (plugin.getAuraManager() != null) {
            var eq = plugin.getAuraManager().getEquipped(u);
            int ai = 0;
            for (var a : plugin.getAuraManager().getOwned(u)) {
                if (ai++ > 0) sb.append(',');
                sb.append("{\"id\":\"").append(esc(a.key))
                        .append("\",\"name\":\"").append(esc(a.chineseName))
                        .append("\",\"on\":").append(eq == a).append('}');
            }
            sb.append("],\"bright\":\"").append(esc(
                    plugin.getAuraManager().getBrightness(u).label)).append('"');
        } else {
            sb.append("],\"bright\":\"\"");
        }
        sb.append(",\"miles\":[");
        if (plugin.getMilestoneManager() != null) {
            int mi = 0;
            for (String m : plugin.getMilestoneManager().getAchieved(u)) {
                if (mi++ > 0) sb.append(',');
                sb.append('"').append(esc(m)).append('"');
            }
        }
        sb.append("],\"skills\":[");
        if (plugin.getSkillShopManager() != null) {
            int ski = 0;
            for (var l : plugin.getSkillShopManager().forSale()) {
                if (ski++ > 0) sb.append(',');
                sb.append("{\"name\":\"").append(esc(l.skill().displayName()))
                        .append("\",\"price\":\"").append(esc(money(l.price()))).append("\"}");
            }
        }
        YearMonth ym = YearMonth.now();
        LocalDate today = LocalDate.now();
        sb.append("],\"cal\":{\"y\":").append(ym.getYear())
                .append(",\"m\":").append(ym.getMonthValue())
                .append(",\"today\":").append(today.getDayOfMonth())
                .append(",\"days\":[");
        if (plugin.getCheckInManager() != null) {
            int di = 0;
            for (int d : plugin.getCheckInManager().getCheckedDaysInMonth(u, ym)) {
                if (di++ > 0) sb.append(',');
                sb.append(d);
            }
        }
        sb.append("]}}");
        return sb.toString();
    }

    private String money(double n) {
        return plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(n) : String.format("%.0f", n);
    }

    private void act(HttpExchange ex) throws IOException {
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
        String doWhat = extract(body, "do");
        if (doWhat.isBlank()) doWhat = extract(body, "action");
        final String act = doWhat;
        final String raw = extract(body, "id");
        final String name = extract(body, "name");
        final String text = extract(body, "text");
        final String amt = extract(body, "amount");
        String json = sync(() -> runAct(s, act, raw, name, text, amt));
        if (json == null) {
            send(ex, 500, "application/json", "{\"ok\":false,\"error\":\"终端暂时无法应答\"}");
            return;
        }
        send(ex, 200, "application/json", json);
    }

    private String runAct(WebSessions.Session s, String act, String id, String name, String text, String amt) {
        UUID u = s.uuid();
        if (act == null || act.isBlank()) return "{\"ok\":false,\"error\":\"没有动作\"}";
        return switch (act) {
            case "checkin" -> {
                if (plugin.getCheckInManager() == null) yield "{\"ok\":false,\"error\":\"签到不可用\"}";
                String msg = plugin.getCheckInManager().checkInWeb(u);
                boolean ok = msg.startsWith("签到成功");
                yield ok(ok, msg);
            }
            case "makeup" -> {
                if (plugin.getCheckInManager() == null) yield "{\"ok\":false,\"error\":\"签到不可用\"}";
                LocalDate day;
                try { day = LocalDate.parse(id.trim()); }
                catch (Exception e) { yield "{\"ok\":false,\"error\":\"日期无效\"}"; }
                String err = plugin.getCheckInManager().makeup(u, day);
                yield err == null ? ok(true, "已补签 " + day) : "{\"ok\":false,\"error\":\"" + esc(err) + "\"}";
            }
            case "status" -> {
                if (plugin.getStatusManager() == null) yield "{\"ok\":false,\"error\":\"签名不可用\"}";
                String t = text == null ? "" : text.replace('\n', ' ').trim();
                if (t.length() > plugin.getStatusManager().maxLength())
                    t = t.substring(0, plugin.getStatusManager().maxLength());
                plugin.getStatusManager().setStatus(u, t.isBlank() ? null : t);
                yield ok(true, t.isBlank() ? "已清除签名" : "签名已更新");
            }
            case "mail.del" -> {
                if (plugin.getMailManager() == null) yield "{\"ok\":false,\"error\":\"邮箱不可用\"}";
                plugin.getMailManager().delete(u, parseIdx(id));
                yield ok(true, "已删除");
            }
            case "mail.read" -> {
                if (plugin.getMailManager() == null) yield "{\"ok\":false,\"error\":\"邮箱不可用\"}";
                plugin.getMailManager().markRead(u, parseIdx(id));
                yield ok(true, "已读");
            }
            case "mail.send" -> {
                if (plugin.getMailManager() == null) yield "{\"ok\":false,\"error\":\"邮箱不可用\"}";
                if (name == null || name.isBlank()) yield "{\"ok\":false,\"error\":\"请写下收件人\"}";
                if (text == null || text.isBlank()) yield "{\"ok\":false,\"error\":\"请写下内容\"}";
                OfflinePlayer to = findPlayed(name);
                if (to == null) yield "{\"ok\":false,\"error\":\"找不到这个人\"}";
                if (to.getUniqueId().equals(u)) yield "{\"ok\":false,\"error\":\"不能给自己写信\"}";
                String body = text.length() > 800 ? text.substring(0, 800) : text;
                plugin.getMailManager().deliver(u, s.name(), to.getUniqueId(), body);
                yield ok(true, "已送达 " + (to.getName() == null ? name : to.getName()));
            }
            case "friend.add" -> {
                if (plugin.getFriendManager() == null) yield "{\"ok\":false,\"error\":\"好友不可用\"}";
                if (name == null || name.isBlank()) yield "{\"ok\":false,\"error\":\"请写下游戏名\"}";
                OfflinePlayer to = findPlayed(name);
                if (to == null) yield "{\"ok\":false,\"error\":\"找不到这个人\"}";
                if (to.getUniqueId().equals(u)) yield "{\"ok\":false,\"error\":\"不能加自己\"}";
                if (plugin.getFriendManager().areFriends(u, to.getUniqueId()))
                    yield "{\"ok\":false,\"error\":\"已经是好友\"}";
                boolean ok = plugin.getFriendManager().sendRequest(u, to.getUniqueId());
                yield ok(ok, ok ? "已发出好友请求" : "已经发过请求了");
            }
            case "friend.acc" -> {
                if (plugin.getFriendManager() == null) yield "{\"ok\":false,\"error\":\"好友不可用\"}";
                UUID idu = parseUuid(id);
                if (idu == null) yield "{\"ok\":false,\"error\":\"无效\"}";
                boolean ok = plugin.getFriendManager().acceptRequest(u, idu);
                yield ok(ok, ok ? "已接受" : "没有这条请求");
            }
            case "friend.den" -> {
                if (plugin.getFriendManager() == null) yield "{\"ok\":false,\"error\":\"好友不可用\"}";
                UUID idu = parseUuid(id);
                if (idu == null) yield "{\"ok\":false,\"error\":\"无效\"}";
                plugin.getFriendManager().denyRequest(u, idu);
                yield ok(true, "已拒绝");
            }
            case "friend.del" -> {
                if (plugin.getFriendManager() == null) yield "{\"ok\":false,\"error\":\"好友不可用\"}";
                UUID idu = parseUuid(id);
                if (idu == null) yield "{\"ok\":false,\"error\":\"无效\"}";
                plugin.getFriendManager().removeFriend(u, idu);
                yield ok(true, "已删除好友");
            }
            case "friend.share" -> {
                if (plugin.getFriendManager() == null) yield "{\"ok\":false,\"error\":\"好友不可用\"}";
                boolean on = plugin.getFriendManager().toggleLocationSharing(u);
                yield ok(true, on ? "已开启位置分享" : "已关闭位置分享");
            }
            case "pay" -> {
                if (plugin.getVaultHook() == null || !plugin.getVaultHook().isEnabled())
                    yield "{\"ok\":false,\"error\":\"经济不可用\"}";
                OfflinePlayer to = findPlayed(name);
                if (to == null) yield "{\"ok\":false,\"error\":\"找不到收款人\"}";
                double n;
                try { n = Double.parseDouble(amt.trim()); }
                catch (Exception e) { yield "{\"ok\":false,\"error\":\"金额无效\"}"; }
                if (n <= 0 || n > 1_000_000_000) yield "{\"ok\":false,\"error\":\"金额无效\"}";
                var tax = plugin.getTaxManager();
                double fee = tax != null && tax.isPayTaxEnabled()
                        ? tax.calcTax(n, tax.getPayTaxRate()) : 0;
                String err = plugin.getVaultHook().pay(Bukkit.getOfflinePlayer(u), to, n, fee);
                yield err == null
                        ? ok(true, "已转 " + plugin.getVaultHook().format(n)
                        + (to.getName() == null ? "" : " → " + to.getName())
                        + (fee > 0 ? "（税 " + plugin.getVaultHook().format(fee) + "）" : ""))
                        : "{\"ok\":false,\"error\":\"" + esc(err) + "\"}";
            }
            case "job.take" -> {
                if (plugin.getJobBoardManager() == null) yield "{\"ok\":false,\"error\":\"招工不可用\"}";
                String err = plugin.getJobBoardManager().take(u, s.name(), id);
                yield err == null ? ok(true, "已接委托") : "{\"ok\":false,\"error\":\"" + esc(err) + "\"}";
            }
            case "show.vote" -> {
                if (plugin.getShowcaseManager() == null) yield "{\"ok\":false,\"error\":\"展示不可用\"}";
                String err = plugin.getShowcaseManager().vote(u, id);
                yield err == null ? ok(true, "已投票") : "{\"ok\":false,\"error\":\"" + esc(err) + "\"}";
            }
            case "wp.del" -> {
                if (plugin.getWaypointManager() == null) yield "{\"ok\":false,\"error\":\"坐标不可用\"}";
                if (id == null || id.isBlank()) yield "{\"ok\":false,\"error\":\"没有这个坐标\"}";
                boolean ok = plugin.getWaypointManager().remove(u, id);
                yield ok(ok, ok ? "已删除坐标" : "没有这个坐标");
            }
            case "aura.eq" -> {
                if (plugin.getAuraManager() == null) yield "{\"ok\":false,\"error\":\"特效不可用\"}";
                var t = com.etherstories.escore.auras.AuraType.fromKey(id);
                if (t == null) yield "{\"ok\":false,\"error\":\"没有这个特效\"}";
                if (!plugin.getAuraManager().owns(u, t)) yield "{\"ok\":false,\"error\":\"尚未拥有\"}";
                plugin.getAuraManager().equip(u, t);
                yield ok(true, "已装备 " + t.chineseName + "（需在游戏中可见）");
            }
            case "aura.off" -> {
                if (plugin.getAuraManager() == null) yield "{\"ok\":false,\"error\":\"特效不可用\"}";
                plugin.getAuraManager().unequip(u);
                yield ok(true, "已卸下特效");
            }
            case "aura.bright" -> {
                if (plugin.getAuraManager() == null) yield "{\"ok\":false,\"error\":\"特效不可用\"}";
                var b = plugin.getAuraManager().cycleBrightness(u);
                yield ok(true, "亮度：" + b.label);
            }
            case "land.vis" -> {
                if (plugin.getRegionManager() == null) yield "{\"ok\":false,\"error\":\"领地不可用\"}";
                var r = plugin.getRegionManager().toggleVisible(id, u, false);
                if (r.isEmpty()) yield "{\"ok\":false,\"error\":\"无法更改（需是你的领地）\"}";
                yield ok(true, r.get() ? "已公开" : "已隐藏");
            }
            case "route" -> {
                if (plugin.getTransitManager() == null) yield "{\"ok\":false,\"error\":\"交通不可用\"}";
                String from = stationId(name), to = stationId(text);
                if (from.isBlank() || to.isBlank()) yield "{\"ok\":false,\"error\":\"请写下起点和终点\"}";
                var path = plugin.getTransitManager().shortest(from, to);
                if (!path.reachable()) yield "{\"ok\":false,\"error\":\"这两站之间没有通路\"}";
                String hops = path.hops().isEmpty() ? "同站"
                        : path.hops().stream().map(plugin.getTransitManager()::stationName)
                        .reduce((a, b) -> a + " → " + b).orElse("");
                yield "{\"ok\":true,\"msg\":\"" + esc(money(path.fare()) + " · " + hops) + "\"}";
            }
            case "ev.toggle" -> {
                if (plugin.getEventManager() == null) yield "{\"ok\":false,\"error\":\"活动不可用\"}";
                var ev = plugin.getEventManager().getEvent(id);
                if (ev == null) yield "{\"ok\":false,\"error\":\"活动不存在\"}";
                if (ev.hasJoined(u)) {
                    plugin.getEventManager().leave(id, u);
                    yield ok(true, "已退出");
                }
                boolean ok = plugin.getEventManager().join(id, u);
                yield ok(ok, ok ? "已参加" : "无法加入");
            }
            default -> "{\"ok\":false,\"error\":\"未知动作\"}";
        };
    }

    private static String ok(boolean ok, String msg) {
        return "{\"ok\":" + ok + ",\"" + (ok ? "msg" : "error") + "\":\"" + esc(msg) + "\"}";
    }

    private static int parseIdx(String raw) {
        try { return Integer.parseInt(raw.trim()); }
        catch (Exception e) { return -1; }
    }

    private static UUID parseUuid(String raw) {
        try { return UUID.fromString(raw.trim()); }
        catch (Exception e) { return null; }
    }

    private String stationId(String raw) {
        if (raw == null || raw.isBlank() || plugin.getTransitManager() == null) return raw == null ? "" : raw.trim();
        String q = raw.trim();
        if (plugin.getTransitManager().getStation(q) != null) return q;
        for (var s : plugin.getTransitManager().allStations()) {
            if (s.id().equalsIgnoreCase(q)) return s.id();
            if (s.displayName() != null && s.displayName().equalsIgnoreCase(q)) return s.id();
        }
        return q;
    }

    private static OfflinePlayer findPlayed(String name) {
        if (name == null || name.isBlank()) return null;
        Player on = Bukkit.getPlayerExact(name.trim());
        if (on != null) return on;
        for (OfflinePlayer off : Bukkit.getOfflinePlayers()) {
            if (off.getName() != null && off.getName().equalsIgnoreCase(name.trim())
                    && (off.hasPlayedBefore() || off.isOnline()))
                return off;
        }
        return null;
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
        if ("fav".equalsIgnoreCase(act)) {
            String id = extract(body, "id");
            if (id.isBlank()) {
                send(ex, 400, "application/json", "{\"ok\":false,\"error\":\"请先选择一首歌曲\"}");
                return;
            }
            String name = extract(body, "name");
            String author = extract(body, "author");
            String album = extract(body, "album");
            String json = sync(() -> {
                var fm = plugin.getMusicFavoritesManager();
                if (fm == null) return "{\"ok\":false,\"error\":\"收藏暂不可用\"}";
                if (fm.has(s.uuid(), id)) {
                    fm.remove(s.uuid(), id);
                    return "{\"ok\":true,\"fav\":false,\"msg\":\"已取消收藏\"}";
                }
                if (fm.size(s.uuid()) >= 60) {
                    return "{\"ok\":false,\"error\":\"收藏已满（最多 60 首）\"}";
                }
                fm.add(s.uuid(), id, name, author, album);
                return "{\"ok\":true,\"fav\":true,\"msg\":\"已收藏\"}";
            });
            if (json == null) {
                send(ex, 500, "application/json", "{\"ok\":false,\"error\":\"终端暂时无法应答\"}");
                return;
            }
            send(ex, 200, "application/json", json);
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
        final String name = s.name();
        final String msg = text;
        final UUID uid = s.uuid();
        String json = sync(() -> {
            boolean cross = linkOn(uid);
            String shown = cross
                    ? plugin.getESLinkHook().localLine(name, msg)
                    : ColorUtil.colorize("&7[Web] &f<" + name + "> " + msg);
            chat.add(name, msg, cross ? "link" : "web", null, shown);
            if (cross) {
                for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(shown);
                plugin.getESLinkHook().forwardChat(uid, name, msg);
            } else {
                Bukkit.broadcastMessage(shown);
            }
            return "{\"ok\":true,\"link\":" + cross + "}";
        });
        if (json == null) {
            send(ex, 500, "application/json", "{\"ok\":false,\"error\":\"终端暂时无法应答\"}");
            return;
        }
        send(ex, 200, "application/json", json);
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
        final boolean post = "POST".equalsIgnoreCase(ex.getRequestMethod());
        final boolean want;
        if (post) {
            String body = readBody(ex);
            want = body.contains("\"all\":true")
                    || "true".equalsIgnoreCase(extract(body, "all"))
                    || "all".equalsIgnoreCase(extract(body, "all"));
        } else {
            want = false;
        }
        String json = sync(() -> {
            if (post) {
                plugin.getESLinkHook().setChatAll(s.uuid(), want);
                linkPref.put(s.uuid(), want);
                return "{\"ok\":true,\"hasLink\":true,\"link\":" + want + "}";
            }
            return "{\"ok\":true,\"hasLink\":true,\"link\":" + linkOn(s.uuid()) + "}";
        });
        if (json == null) {
            send(ex, 500, "application/json", "{\"ok\":false,\"error\":\"终端暂时无法应答\"}");
            return;
        }
        send(ex, 200, "application/json", json);
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

    /** Arclight 的 Player 没有 Paper 的 isConnected()，调用会 NoSuchMethodError 刷屏。 */
    private static boolean webOnline(WebSessions.Session s) {
        if (s == null) return false;
        try {
            Player p = Bukkit.getPlayer(s.uuid());
            return p != null && p.isOnline();
        } catch (Throwable ignored) {
            return false;
        }
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
        String skin = s == null || name.isBlank() ? "" : "/v1/skin?n=" + urlEnc(name);
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
                .append(",\"stay\":\"").append(esc(s == null || plugin.getHotelManager() == null
                        ? "" : nz(plugin.getHotelManager().stayLabel(s.uuid())))).append('"')
                .append(",\"makeup\":").append(s == null || plugin.getCheckInManager() == null
                        ? 0 : plugin.getCheckInManager().getMakeupTickets(s.uuid()))
                .append(",\"reqs\":").append(s == null || plugin.getFriendManager() == null
                        ? 0 : plugin.getFriendManager().getPendingRequests(s.uuid()).size())
                .append(",\"played\":\"").append(esc(s == null || plugin.getPlaytimeManager() == null
                        ? "" : plugin.getPlaytimeManager().getTotalFormatted(s.uuid()))).append('"')
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
                    .append(",\"skin\":\"").append(esc("/v1/skin?n=" + urlEnc(p.getName()))).append('"')
                    .append(",\"head\":\"/v1/skin?n=").append(esc(urlEnc(p.getName()))).append('"')
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
                    .append(",\"kind\":\"").append(esc(line.kind()))
                    .append("\",\"display\":\"").append(esc(nz(line.display()))).append("\"}");
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

    private String sync(Callable<String> job) {
        try {
            if (Bukkit.isPrimaryThread()) return job.call();
            return Bukkit.getScheduler().callSyncMethod(plugin, job).get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            plugin.getLogger().warning("ECOS Web 主线程超时");
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("ECOS Web sync: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            return null;
        }
    }

    private static String urlEnc(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private void skin(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "text/plain", "method");
            return;
        }
        String name = "";
        String raw = ex.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String part : raw.split("&")) {
                int eq = part.indexOf('=');
                if (eq <= 0 || !"n".equals(part.substring(0, eq))) continue;
                name = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        if (name.isBlank()) {
            send(ex, 404, "text/plain", "no name");
            return;
        }
        final String who = name;
        String url = sync(() -> {
            Player p = Bukkit.getPlayerExact(who);
            if (p == null) {
                for (Player on : Bukkit.getOnlinePlayers()) {
                    if (on.getName().equalsIgnoreCase(who)) { p = on; break; }
                }
            }
            if (p != null) {
                try {
                    var u = p.getPlayerProfile().getTextures().getSkin();
                    if (u != null) return httpsUrl(u.toString());
                } catch (Throwable ignored) {}
            }
            return "https://mc-heads.net/skin/" + who;
        });
        if (url == null || url.isBlank()) {
            send(ex, 502, "text/plain", "skin");
            return;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> res = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() >= 400 || res.body() == null || res.body().length == 0) {
                send(ex, 502, "text/plain", "skin");
                return;
            }
            sendBytes(ex, 200, "image/png", res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            send(ex, 502, "text/plain", "skin");
        } catch (Exception e) {
            send(ex, 502, "text/plain", "skin");
        }
    }

    private static String httpsUrl(String url) {
        if (url == null || url.isBlank()) return "";
        if (url.startsWith("http://")) return "https://" + url.substring(7);
        return url;
    }

    private static String skinOf(Player p) {
        if (p == null) return "https://mc-heads.net/skin/Steve";
        try {
            var url = p.getPlayerProfile().getTextures().getSkin();
            if (url != null) {
                String s = httpsUrl(url.toString());
                if (!s.isBlank()) return s;
            }
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

    /** 空 map-url：HTTPS 反代下用同站 map- 主机；局域网仍走 访问主机:8100。 */
    private String mapUrl(HttpExchange ex) {
        String cfg = plugin.getConfig().getString("web.map-url", "");
        if (cfg != null && !cfg.isBlank()) return cfg.trim();
        String host = header(ex, "X-Forwarded-Host");
        if (host == null || host.isBlank()) host = header(ex, "Host");
        String name = "127.0.0.1";
        if (host != null && !host.isBlank()) {
            int comma = host.indexOf(',');
            if (comma > 0) host = host.substring(0, comma).trim();
            if (host.startsWith("[")) {
                int end = host.indexOf(']');
                name = end > 0 ? host.substring(0, end + 1) : host;
            } else {
                int colon = host.lastIndexOf(':');
                name = colon > 0 ? host.substring(0, colon) : host;
            }
        }
        String proto = header(ex, "X-Forwarded-Proto");
        if (proto == null || proto.isBlank()) {
            String pub = plugin.getConfig().getString("web.public-url", "");
            proto = pub != null && pub.startsWith("https") ? "https" : "http";
        }
        proto = proto.split(",")[0].trim();
        if ("https".equalsIgnoreCase(proto)) {
            if (name.startsWith("ecos-")) name = "map-" + name.substring(5);
            return "https://" + name + "/";
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
        if (type != null && type.startsWith("application/json") && !type.contains("charset"))
            type = type + "; charset=utf-8";
        sendBytes(ex, code, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendBytes(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "private, no-store, max-age=0");
        ex.getResponseHeaders().set("Pragma", "no-cache");
        if (type != null && type.contains("json")) {
            ex.getResponseHeaders().set("Vary", "Authorization");
        }
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }

}
