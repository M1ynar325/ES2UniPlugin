package com.etherstories.escore.web;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 每位玩家一枚长期 ECOS Token（XXXXXXXX-XXXXXXXX），游戏里可反复查看、随时重置。 */
public class WebSessions {

    public record Session(String token, UUID uuid, String name, long created, boolean admin) {}

    private final Plugin plugin;
    private final File dataFile;
    private final Map<String, Cred> byPin = new ConcurrentHashMap<>();
    private final Map<UUID, String> byUuid = new ConcurrentHashMap<>();
    private final SecureRandom rng = new SecureRandom();

    public WebSessions(Plugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "web-sessions.yml");
        load();
    }

    public String reveal(Player player) {
        UUID uuid = player.getUniqueId();
        boolean admin = isAdmin(player);
        String name = player.getName();
        synchronized (this) {
            String pin = byUuid.get(uuid);
            if (pin == null) {
                pin = newPin();
                put(pin, new Cred(uuid, name, admin));
                save();
                return pin;
            }
            Cred cur = byPin.get(norm(pin));
            if (cur == null || !cur.name.equals(name) || cur.admin != admin) {
                put(pin, new Cred(uuid, name, admin));
                save();
            }
            return pin;
        }
    }

    public String reset(Player player) {
        UUID uuid = player.getUniqueId();
        synchronized (this) {
            String old = byUuid.remove(uuid);
            if (old != null) byPin.remove(norm(old));
            String pin = newPin();
            put(pin, new Cred(uuid, player.getName(), isAdmin(player)));
            save();
            return pin;
        }
    }

    public Session consume(String code) {
        Cred c = lookup(code);
        if (c == null) return null;
        String pin = byUuid.get(c.uuid);
        if (pin == null) return null;
        return new Session(pin, c.uuid, c.name, 0L, c.admin);
    }

    public Session get(String token) {
        Cred c = lookup(token);
        if (c == null) return null;
        String pin = byUuid.get(c.uuid);
        if (pin == null) return null;
        return new Session(pin, c.uuid, c.name, 0L, c.admin);
    }

    public void revoke(UUID uuid) {
        if (uuid == null) return;
        synchronized (this) {
            String old = byUuid.remove(uuid);
            if (old != null) byPin.remove(norm(old));
            save();
        }
    }

    public void revoke(String token) {
        /* 网页断开只清本机，不改令牌 */
    }

    private Cred lookup(String raw) {
        String n = norm(raw);
        if (n.length() != 16) return null;
        return byPin.get(n);
    }

    private void put(String pin, Cred c) {
        byPin.put(norm(pin), c);
        byUuid.put(c.uuid, pin);
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = cfg.getConfigurationSection("tokens");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String pin = format(cfg.getString("tokens." + key + ".pin", ""));
                if (pin == null) continue;
                String name = cfg.getString("tokens." + key + ".name", "");
                boolean admin = cfg.getBoolean("tokens." + key + ".admin", false);
                put(pin, new Cred(uuid, name, admin));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : byUuid.entrySet()) {
            String path = "tokens." + e.getKey();
            Cred c = byPin.get(norm(e.getValue()));
            cfg.set(path + ".pin", e.getValue());
            cfg.set(path + ".name", c == null ? "" : c.name);
            cfg.set(path + ".admin", c != null && c.admin);
        }
        try {
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("web-sessions.yml 保存失败: " + ex.getMessage());
        }
    }

    private String newPin() {
        String pin;
        do {
            byte[] b = new byte[8];
            rng.nextBytes(b);
            StringBuilder hex = new StringBuilder(16);
            for (byte v : b) hex.append(String.format("%02X", v));
            pin = hex.substring(0, 8) + "-" + hex.substring(8);
        } while (byPin.containsKey(norm(pin)));
        return pin;
    }

    static String norm(String raw) {
        if (raw == null) return "";
        StringBuilder d = new StringBuilder(16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') d.append(c);
            else if (c >= 'a' && c <= 'f') d.append(c);
            else if (c >= 'A' && c <= 'F') d.append((char) (c + 32));
        }
        return d.toString();
    }

    static String format(String raw) {
        String n = norm(raw);
        if (n.length() != 16) return null;
        return n.substring(0, 8).toUpperCase(Locale.ROOT) + "-" + n.substring(8).toUpperCase(Locale.ROOT);
    }

    private static boolean isAdmin(Player player) {
        return player.isOp() || player.hasPermission("es2uni.admin");
    }

    private record Cred(UUID uuid, String name, boolean admin) {}
}
