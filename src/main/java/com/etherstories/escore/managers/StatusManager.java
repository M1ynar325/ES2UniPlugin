package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StatusManager {

    private static final int MAX_LENGTH = 40;

    private final ES2UniPlugin    plugin;
    private final File            dataFile;
    private final Map<UUID, String> statuses = new HashMap<>();

    public StatusManager(ES2UniPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "status.yml");
        load();
    }

    public void setStatus(UUID player, String text) {
        if (text == null || text.isBlank()) {
            statuses.remove(player);
        } else {
            statuses.put(player, text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text);
        }
        save();
    }

    public String getStatus(UUID player) {
        return statuses.get(player);
    }

    public int maxLength() { return MAX_LENGTH; }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : cfg.getKeys(false)) {
            try { statuses.put(UUID.fromString(key), cfg.getString(key)); }
            catch (IllegalArgumentException ignored) {}
        }
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        statuses.forEach((uuid, s) -> cfg.set(uuid.toString(), s));
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("status.yml 保存失败: " + e.getMessage());
        }
    }
}
