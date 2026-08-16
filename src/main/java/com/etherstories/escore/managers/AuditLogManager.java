package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 管理员操作审计日志。默认关闭（不占磁盘）；config audit.enabled=true 才写入。
 */
public class AuditLogManager {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MEMORY_CAP = 200;

    private final ES2UniPlugin plugin;
    private final File dir;
    private final List<String> recent = Collections.synchronizedList(new ArrayList<>());

    public AuditLogManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "audit");
        // 不在构造时建目录；启用并首次写入时再创建
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("audit.enabled", false);
    }

    public void log(Player actor, String action, String detail) {
        if (!isEnabled()) return;
        String name = actor != null ? actor.getName() : "CONSOLE";
        String uuid = actor != null ? actor.getUniqueId().toString() : "-";
        logRaw(name, uuid, action, detail == null ? "" : detail);
    }

    public void logRaw(String actorName, String actorUuid, String action, String detail) {
        if (!isEnabled()) return;
        String line = TS.format(LocalDateTime.now()) + " | " + actorName + " (" + actorUuid + ") | "
                + action + (detail == null || detail.isBlank() ? "" : " | " + detail);
        recent.add(line);
        while (recent.size() > MEMORY_CAP) recent.remove(0);

        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("无法创建审计目录: " + dir.getAbsolutePath());
            return;
        }
        File file = new File(dir, "audit-" + LocalDate.now() + ".log");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, true))) {
            w.write(line);
            w.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("写入审计日志失败: " + e.getMessage());
        }
    }

    /** 最近内存日志，新在后。 */
    public List<String> getRecent(int limit) {
        synchronized (recent) {
            int from = Math.max(0, recent.size() - Math.max(1, limit));
            return new ArrayList<>(recent.subList(from, recent.size()));
        }
    }

    public List<String> readToday(int limit) {
        File file = new File(dir, "audit-" + LocalDate.now() + ".log");
        if (!file.exists()) return List.of();
        try {
            List<String> all = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            int from = Math.max(0, all.size() - Math.max(1, limit));
            return all.subList(from, all.size());
        } catch (IOException e) {
            return List.of();
        }
    }
}
