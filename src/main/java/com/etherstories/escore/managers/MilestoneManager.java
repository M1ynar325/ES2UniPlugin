package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MilestoneManager {

    private static final long[] THRESHOLDS_MS = {
            1L   * 3_600_000,
            5L   * 3_600_000,
            10L  * 3_600_000,
            25L  * 3_600_000,
            50L  * 3_600_000,
            100L * 3_600_000,
            200L * 3_600_000,
            500L * 3_600_000
    };

    private static final String[] NAMES = {
            "探索者", "常旅者", "定居者", "建设者", "老兵", "传说", "史诗", "传奇"
    };

    private final ES2UniPlugin              plugin;
    private final File                      dataFile;
    private final Map<UUID, Set<Integer>>   achieved = new HashMap<>();

    public MilestoneManager(ES2UniPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "milestones.yml");
        load();
    }

    /**
     * Called after playtime is updated. Announces any newly reached milestones to the player.
     */
    public void check(Player player, long totalMillis) {
        Set<Integer> done = achieved.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        boolean changed = false;
        for (int i = 0; i < THRESHOLDS_MS.length; i++) {
            if (totalMillis >= THRESHOLDS_MS[i] && done.add(i)) {
                changed = true;
                final String milestone = NAMES[i];
                final long hours = THRESHOLDS_MS[i] / 3_600_000;
                Bukkit.broadcastMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                        "&8[ECOS] &f" + player.getName()
                                + " &7达成里程碑 &f" + milestone
                                + " &8[" + hours + "h]"));
            }
        }
        if (changed) save();
    }

    /** Returns list of achieved milestone names for this player. */
    public List<String> getAchieved(UUID player) {
        Set<Integer> done = achieved.getOrDefault(player, Collections.emptySet());
        List<String> list = new ArrayList<>();
        for (int i : done) list.add(NAMES[i]);
        return list;
    }

    public String[] allNames()       { return NAMES; }
    public long[] allThresholds()    { return THRESHOLDS_MS; }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<Integer> list = (List<Integer>) cfg.getList(key, Collections.emptyList());
                achieved.put(uuid, new HashSet<>(list));
            } catch (Exception ignored) {}
        }
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        achieved.forEach((uuid, set) -> cfg.set(uuid.toString(), new ArrayList<>(set)));
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("milestones.yml 保存失败: " + e.getMessage());
        }
    }
}
