package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 新玩家引导清单（约 30 分钟上手）。
 * 步骤可手动勾选；部分步骤可被游戏行为自动完成。
 */
public class NewbieGuideManager {

    public enum Step {
        OPEN_TERMINAL("打开 ECOS 终端", "输入 /ecos menu 或使用终端物品"),
        CHECK_IN("完成首次签到", "终端 → 签到日历"),
        READ_NOTICE("浏览公告板", "终端 → 公告"),
        FIND_SPAWN("熟悉出生点周边", "四处走走，别马上跑图"),
        CLAIM_LUCKY("开一次幸运方块", "终端 → 幸运方块"),
        ADD_FRIEND("添加一位好友（可选）", "终端 → 好友"),
        SET_HOME("设置一个家（可选）", "有 Essentials 时 /sethome");

        public final String title;
        public final String tip;
        Step(String title, String tip) { this.title = title; this.tip = tip; }
    }

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private final Map<UUID, Set<String>> done = new HashMap<>();
    private final Map<UUID, Long> firstJoinMs = new HashMap<>();

    public NewbieGuideManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "newbie.yml");
        load();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("newbie-guide.enabled", true);
    }

    public long windowMs() {
        return plugin.getConfig().getLong("newbie-guide.window-minutes", 30) * 60_000L;
    }

    public void onJoin(Player player) {
        firstJoinMs.putIfAbsent(player.getUniqueId(), System.currentTimeMillis());
        save();
    }

    public boolean shouldShow(Player player) {
        if (!isEnabled()) return false;
        UUID u = player.getUniqueId();
        long start = firstJoinMs.getOrDefault(u, player.getFirstPlayed() > 0 ? player.getFirstPlayed() : System.currentTimeMillis());
        firstJoinMs.putIfAbsent(u, start);
        if (System.currentTimeMillis() - start > windowMs() && isComplete(u)) return false;
        // 窗口内，或尚未完成全部
        return !isComplete(u) && (System.currentTimeMillis() - start <= windowMs() * 3);
    }

    public boolean isComplete(UUID uuid) {
        Set<String> set = done.getOrDefault(uuid, Collections.emptySet());
        for (Step s : Step.values()) {
            if (s == Step.ADD_FRIEND || s == Step.SET_HOME) continue; // 可选
            if (!set.contains(s.name())) return false;
        }
        return true;
    }

    public boolean isDone(UUID uuid, Step step) {
        return done.getOrDefault(uuid, Collections.emptySet()).contains(step.name());
    }

    public void mark(UUID uuid, Step step) {
        done.computeIfAbsent(uuid, k -> new HashSet<>()).add(step.name());
        save();
    }

    public void toggle(UUID uuid, Step step) {
        Set<String> set = done.computeIfAbsent(uuid, k -> new HashSet<>());
        if (!set.add(step.name())) set.remove(step.name());
        save();
    }

    public int progress(UUID uuid) {
        int need = 0, got = 0;
        for (Step s : Step.values()) {
            if (s == Step.ADD_FRIEND || s == Step.SET_HOME) continue;
            need++;
            if (isDone(uuid, s)) got++;
        }
        return need == 0 ? 100 : (got * 100 / need);
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (cfg.isConfigurationSection("players")) {
            for (String k : cfg.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID u = UUID.fromString(k);
                    done.put(u, new HashSet<>(cfg.getStringList("players." + k + ".done")));
                    if (cfg.contains("players." + k + ".first"))
                        firstJoinMs.put(u, cfg.getLong("players." + k + ".first"));
                } catch (Exception ignored) {}
            }
        }
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (UUID u : done.keySet()) {
            cfg.set("players." + u + ".done", new ArrayList<>(done.get(u)));
            if (firstJoinMs.containsKey(u))
                cfg.set("players." + u + ".first", firstJoinMs.get(u));
        }
        for (UUID u : firstJoinMs.keySet()) {
            if (!done.containsKey(u))
                cfg.set("players." + u + ".first", firstJoinMs.get(u));
        }
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("newbie.yml 保存失败: " + e.getMessage());
        }
    }
}
