package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 简易举报 / 悄悄话：通知在线管理员，并写入有上限的小文件。
 */
public class ReportManager {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_ENTRIES = 80;

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private final List<String> entries = new ArrayList<>();

    public ReportManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "reports.yml");
        load();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("report.enabled", true);
    }

    public void submit(Player from, Player about, String reason) {
        submit(from, about != null ? about.getName() : null, reason);
    }

    public void submit(Player from, String aboutName, String reason) {
        if (!isEnabled()) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7悄悄话功能已关闭"));
            return;
        }
        String target = aboutName == null || aboutName.isBlank() ? "(无对象)" : aboutName;
        String line = TS.format(LocalDateTime.now()) + " | " + from.getName()
                + " → " + target + " | " + reason;
        synchronized (entries) {
            entries.add(line);
            while (entries.size() > MAX_ENTRIES) entries.remove(0);
            save();
        }

        from.sendMessage(ColorUtil.colorize("&8[ECOS] &7已发送悄悄话给管理员，感谢反馈"));

        String notify = ColorUtil.colorize(
                "&c[悄悄话] &f" + from.getName()
                        + " &7→ &f" + target
                        + "&7: &f" + reason);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("es2uni.admin") || p.isOp()) {
                p.sendMessage(notify);
            }
        }
        plugin.getLogger().info("[Report] " + line);
    }

    public List<String> recent(int limit) {
        synchronized (entries) {
            int from = Math.max(0, entries.size() - Math.max(1, limit));
            return new ArrayList<>(entries.subList(from, entries.size()));
        }
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        entries.addAll(cfg.getStringList("entries"));
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("entries", entries);
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("reports.yml 保存失败: " + e.getMessage());
        }
    }
}
