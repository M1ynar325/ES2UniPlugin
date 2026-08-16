package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * 管理员预设歌单：按序/打乱续播（队列将空时自动点下一首）。
 * 配置在 config.yml → music.presets
 */
public class MusicPlaylistManager {

    public record Preset(String key, String displayName, String description, List<String> songIds) {}

    private final ES2UniPlugin plugin;
    private BukkitTask runner;
    private List<String> activeQueue = List.of();
    private int cursor = 0;
    private boolean shuffled;
    private String activeName = "";
    private UUID starter;

    public MusicPlaylistManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public List<Preset> listPresets() {
        List<Preset> out = new ArrayList<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("music.presets");
        if (sec == null) return out;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection p = sec.getConfigurationSection(key);
            if (p == null) continue;
            String name = p.getString("name", key);
            String desc = p.getString("description", "");
            List<String> ids = p.getStringList("songs");
            if (ids == null) ids = List.of();
            out.add(new Preset(key, name, desc, List.copyOf(ids)));
        }
        return out;
    }

    public Preset get(String key) {
        for (Preset p : listPresets()) if (p.key().equalsIgnoreCase(key)) return p;
        return null;
    }

    public boolean isRunning() {
        return runner != null && !runner.isCancelled();
    }

    public String statusLine() {
        if (!isRunning()) return "未在播放预设";
        return activeName + (shuffled ? " · 打乱" : " · 顺序")
                + " · " + Math.min(cursor, activeQueue.size()) + "/" + activeQueue.size();
    }

    /**
     * 启动预设。shuffle=true 时打乱整表再播。
     * @return 错误信息；null 成功
     */
    public String start(Player admin, String presetKey, boolean shuffle) {
        Preset preset = get(presetKey);
        if (preset == null) return "找不到预设: " + presetKey;
        if (preset.songIds().isEmpty()) return "该预设没有歌曲 ID";
        if (!plugin.getAllMusicHook().ensureHooked(plugin.getLogger())) return "AllMusic 未加载";

        stop(false);
        List<String> ids = new ArrayList<>(preset.songIds());
        ids.removeIf(s -> s == null || s.isBlank());
        if (ids.isEmpty()) return "歌曲列表为空";
        if (shuffle) Collections.shuffle(ids);

        activeQueue = ids;
        cursor = 0;
        shuffled = shuffle;
        activeName = preset.displayName();
        starter = admin.getUniqueId();

        // 先塞 1～2 首，后续由 runner 续播
        enqueueNext(admin, Math.min(2, ids.size()));

        runner = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickFeed(admin.getUniqueId()), 40L, 60L);
        return null;
    }

    public void stop(boolean announce) {
        if (runner != null) {
            runner.cancel();
            runner = null;
        }
        if (announce && starter != null) {
            Player p = Bukkit.getPlayer(starter);
            if (p != null) p.sendMessage(ColorUtil.colorize("&8[ECOS] &7预设歌单已停止"));
        }
        activeQueue = List.of();
        cursor = 0;
        activeName = "";
        starter = null;
        shuffled = false;
    }

    private void tickFeed(UUID adminUuid) {
        if (activeQueue.isEmpty() || cursor >= activeQueue.size()) {
            stop(true);
            Player p = Bukkit.getPlayer(adminUuid);
            if (p != null) p.sendMessage(ColorUtil.colorize("&8[ECOS] &a预设歌单已播完"));
            return;
        }
        if (!plugin.getAllMusicHook().isAvailable()) return;
        int q = 0;
        try {
            var now = plugin.getAllMusicHook().getNowPlaying();
            q = now == null ? 0 : now.queueSize();
        } catch (Exception ignored) {
        }
        // 队列偏空时续点（含正在播时 queueSize 多为剩余排队数）
        if (q <= 1) {
            Player admin = Bukkit.getPlayer(adminUuid);
            if (admin == null || !admin.isOnline()) {
                // 管理员离线仍用控制台玩家名点歌：找任意在线 OP，否则停
                admin = Bukkit.getOnlinePlayers().stream()
                        .filter(pl -> pl.hasPermission("es2uni.admin"))
                        .findFirst().orElse(null);
                if (admin == null) {
                    stop(false);
                    return;
                }
            }
            enqueueNext(admin, 1);
        }
    }

    private void enqueueNext(Player via, int count) {
        for (int i = 0; i < count && cursor < activeQueue.size(); i++) {
            String id = activeQueue.get(cursor++);
            plugin.getAllMusicHook().addById(plugin, via, id);
            plugin.getMusicHistoryManager().recordManual(id, id, "", via.getName() + "/预设");
        }
    }
}
