package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class NoticeManager {

    public record Notice(String author, String content, String date) {}

    private static final int              MAX_NOTICES    = 15;
    private static final int              MAX_BROADCASTS = 10;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final ES2UniPlugin  plugin;
    private final File          dataFile;
    private final List<Notice>  notices          = new ArrayList<>();
    private final List<String>  broadcastHistory = new ArrayList<>();

    public NoticeManager(ES2UniPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "notices.yml");
        load();
    }

    public void addNotice(String author, String content) {
        notices.add(new Notice(author, content, LocalDateTime.now().format(FMT)));
        if (notices.size() > MAX_NOTICES) notices.remove(0);
        save();
    }

    public void removeNotice(int index) {
        if (index >= 0 && index < notices.size()) {
            notices.remove(index);
            save();
        }
    }

    public List<Notice> getNotices() { return Collections.unmodifiableList(notices); }

    public void recordBroadcast(String message) {
        broadcastHistory.add(LocalDateTime.now().format(FMT) + " " + message);
        if (broadcastHistory.size() > MAX_BROADCASTS) broadcastHistory.remove(0);
    }

    public List<String> getBroadcastHistory() {
        return Collections.unmodifiableList(broadcastHistory);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        List<?> list = cfg.getList("notices", Collections.emptyList());
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> raw)) continue;
            @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) raw;
            notices.add(new Notice(
                    (String) m.getOrDefault("author",  "?"),
                    (String) m.getOrDefault("content", ""),
                    (String) m.getOrDefault("date",    "")));
        }
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Notice n : notices) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("author",  n.author());
            m.put("content", n.content());
            m.put("date",    n.date());
            list.add(m);
        }
        cfg.set("notices", list);
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("notices.yml 保存失败: " + e.getMessage());
        }
    }
}
