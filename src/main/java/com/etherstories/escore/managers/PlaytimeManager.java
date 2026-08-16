package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlaytimeManager {

    private final Map<UUID, Long> joinTimes   = new HashMap<>();
    private final Map<UUID, Long> totalMillis = new HashMap<>();
    private final File            dataFile;
    private final ES2UniPlugin    plugin;

    public PlaytimeManager(ES2UniPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playtime.yml");
        load();
    }

    public void onJoin(Player player) {
        joinTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void onQuit(Player player) {
        UUID uuid = player.getUniqueId();
        long joined = joinTimes.getOrDefault(uuid, System.currentTimeMillis());
        long session = System.currentTimeMillis() - joined;
        totalMillis.merge(uuid, session, Long::sum);
        joinTimes.remove(uuid);
        save();
    }

    /** Total playtime in ms (cumulative + current session if online). */
    public long getTotalMillis(UUID uuid) {
        long base = totalMillis.getOrDefault(uuid, 0L);
        Long joined = joinTimes.get(uuid);
        if (joined != null) base += System.currentTimeMillis() - joined;
        return base;
    }

    /** Current session playtime in ms (for display in terminal). */
    public long getSessionMillis(Player player) {
        Long joined = joinTimes.get(player.getUniqueId());
        return joined == null ? 0L : System.currentTimeMillis() - joined;
    }

    public String getFormatted(Player player) {
        return formatMillis(getSessionMillis(player));
    }

    public String getTotalFormatted(UUID uuid) {
        return formatMillis(getTotalMillis(uuid));
    }

    /** Returns top N players sorted by total playtime descending. */
    public List<Map.Entry<UUID, Long>> getTopPlayers(int limit) {
        Map<UUID, Long> snapshot = new HashMap<>(totalMillis);
        for (Map.Entry<UUID, Long> e : joinTimes.entrySet())
            snapshot.merge(e.getKey(), System.currentTimeMillis() - e.getValue(), Long::sum);
        return snapshot.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }

    public static String formatMillis(long millis) {
        long minutes = millis / 60_000;
        long hours   = minutes / 60;
        minutes = minutes % 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m";
        return "< 1m";
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : cfg.getKeys(false)) {
            try {
                totalMillis.put(UUID.fromString(key), cfg.getLong(key));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        totalMillis.forEach((uuid, ms) -> cfg.set(uuid.toString(), ms));
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("playtime.yml 保存失败: " + e.getMessage());
        }
    }
}
