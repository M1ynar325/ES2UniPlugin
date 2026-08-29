package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异常登录日志（换 IP / 被拒 / 网页新 IP / 握手未进服）。独立于 audit.enabled，默认开。
 */
public class LoginLogManager {

    public record Event(long ts, String name, String uuid, String ip, String kind, String note) {}

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private final File logDir;
    private final Map<UUID, String> lastIps = new LinkedHashMap<>();
    private final List<Event> events = new ArrayList<>();
    private final Set<UUID> pendingJoin = ConcurrentHashMap.newKeySet();

    public LoginLogManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "login-log.yml");
        this.logDir = new File(plugin.getDataFolder(), "logs");
        load();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("login-log.enabled", true);
    }

    public void onLogin(PlayerLoginEvent event) {
        if (!isEnabled()) return;
        String ip = host(event.getAddress() == null ? null : event.getAddress().getHostAddress());
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            add(event.getPlayer().getName(), event.getPlayer().getUniqueId(), ip,
                    "FAIL", event.getResult().name());
            return;
        }
        UUID id = event.getPlayer().getUniqueId();
        String name = event.getPlayer().getName();
        pendingJoin.add(id);
        long ticks = Math.max(40L, plugin.getConfig().getLong("login-log.probe-ticks", 300));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!pendingJoin.remove(id)) return;
            if (Bukkit.getPlayer(id) != null) return;
            if (recentProbe(ip)) return;
            add(name, id, ip, "PROBE", "握手后未进服");
        }, ticks);
    }

    public void onJoin(Player player) {
        if (player != null) pendingJoin.remove(player.getUniqueId());
        if (!isEnabled() || player == null) return;
        String ip = host(player.getAddress() == null || player.getAddress().getAddress() == null
                ? null : player.getAddress().getAddress().getHostAddress());
        UUID id = player.getUniqueId();
        String last = lastIps.get(id);
        if (last != null && last.equals(ip)) return;
        lastIps.put(id, ip);
        if (last != null) {
            add(player.getName(), id, ip, "NEW_IP", "上次 " + last);
            return;
        }
        saveState();
    }

    public void onWeb(String name, UUID uuid, String ip) {
        if (!isEnabled() || uuid == null) return;
        String now = host(ip);
        if (loopback(now)) return;
        String last = lastIps.get(uuid);
        if (last != null && !last.equals(now) && !loopback(last)) {
            add(name, uuid, now, "WEB_NEW", "上次游戏 " + last);
        }
    }

    public List<Event> recent(int limit) {
        int n = Math.max(1, limit);
        synchronized (events) {
            int from = Math.max(0, events.size() - n);
            return new ArrayList<>(events.subList(from, events.size()));
        }
    }

    private void add(String name, UUID uuid, String ip, String kind, String note) {
        Event ev = new Event(System.currentTimeMillis(),
                name == null ? "?" : name,
                uuid == null ? "-" : uuid.toString(),
                ip, kind, note == null ? "" : note);
        synchronized (events) {
            events.add(ev);
            while (events.size() > keep()) events.remove(0);
        }
        saveState();
        writeDay(ev);
        if (plugin.getChatFeed() != null) {
            plugin.getChatFeed().add("系统", format(ev), "admin");
        }
    }

    public static String format(Event ev) {
        String line = ev.name() + " " + label(ev.kind()) + "  " + ev.ip();
        if (ev.note() != null && !ev.note().isBlank()) line += "  " + ev.note();
        return line;
    }

    private static String label(String kind) {
        return switch (kind) {
            case "NEW_IP" -> "换 IP";
            case "FAIL" -> "登录被拒";
            case "WEB_NEW" -> "网页新 IP";
            case "PROBE" -> "扫到未进";
            default -> kind;
        };
    }

    private boolean recentProbe(String ip) {
        if (ip == null || ip.equals("?")) return false;
        long now = System.currentTimeMillis();
        synchronized (events) {
            for (int i = events.size() - 1; i >= 0; i--) {
                Event e = events.get(i);
                if (now - e.ts() > 90_000) break;
                if ("PROBE".equals(e.kind()) && ip.equals(e.ip())) return true;
            }
        }
        return false;
    }

    private int keep() {
        return Math.max(20, plugin.getConfig().getInt("login-log.keep", 80));
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection last = cfg.getConfigurationSection("last");
        if (last != null) {
            for (String key : last.getKeys(false)) {
                try {
                    lastIps.put(UUID.fromString(key), host(last.getString(key)));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        List<Map<?, ?>> raw = cfg.getMapList("events");
        for (Map<?, ?> row : raw) {
            try {
                events.add(new Event(
                        asLong(row.get("ts")),
                        str(row, "name", "?"),
                        str(row, "uuid", "-"),
                        str(row, "ip", "?"),
                        str(row, "kind", "?"),
                        str(row, "note", "")));
            } catch (Exception ignored) {}
        }
    }

    private void saveState() {
        FileConfiguration cfg = new YamlConfiguration();
        lastIps.forEach((id, ip) -> cfg.set("last." + id, ip));
        List<Map<String, Object>> rows = new ArrayList<>();
        synchronized (events) {
            for (Event ev : events) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ts", ev.ts());
                row.put("name", ev.name());
                row.put("uuid", ev.uuid());
                row.put("ip", ev.ip());
                row.put("kind", ev.kind());
                row.put("note", ev.note());
                rows.add(row);
            }
        }
        cfg.set("events", rows);
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("login-log.yml 保存失败: " + e.getMessage());
        }
    }

    private void writeDay(Event ev) {
        if (!logDir.exists() && !logDir.mkdirs()) {
            plugin.getLogger().warning("无法创建日志目录: " + logDir.getAbsolutePath());
            return;
        }
        File file = new File(logDir, "login-" + LocalDate.now() + ".log");
        String line = TS.format(LocalDateTime.now()) + " | " + ev.kind() + " | "
                + ev.name() + " (" + ev.uuid() + ") | " + ev.ip()
                + (ev.note().isBlank() ? "" : " | " + ev.note());
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, true))) {
            w.write(line);
            w.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("写入登录日志失败: " + e.getMessage());
        }
    }

    private static String str(Map<?, ?> row, String key, String def) {
        Object v = row.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private static long asLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String host(String ip) {
        if (ip == null || ip.isBlank()) return "?";
        return ip.startsWith("/") ? ip.substring(1) : ip;
    }

    private static boolean loopback(String ip) {
        return ip == null || ip.equals("?")
                || ip.equals("127.0.0.1") || ip.equals("https://example.net/id/garnet")
                || ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1");
    }
}
