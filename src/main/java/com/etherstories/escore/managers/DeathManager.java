package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DeathManager {

    public record DeathRecord(String world, int x, int y, int z, String cause, String date) {}

    private static final int              MAX_PER_PLAYER = 5;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final ES2UniPlugin                   plugin;
    private final File                           dataFile;
    private final Map<UUID, List<DeathRecord>>   records = new HashMap<>();

    public DeathManager(ES2UniPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "deaths.yml");
        load();
    }

    public void record(UUID player, String world, int x, int y, int z, String cause) {
        List<DeathRecord> list = records.computeIfAbsent(player, k -> new ArrayList<>());
        list.add(new DeathRecord(world, x, y, z, cause, LocalDateTime.now().format(FMT)));
        if (list.size() > MAX_PER_PLAYER) list.remove(0);
        save();
    }

    public List<DeathRecord> get(UUID player) {
        return Collections.unmodifiableList(records.getOrDefault(player, Collections.emptyList()));
    }

    /** Most recent death record, or null. */
    public DeathRecord getLast(UUID player) {
        List<DeathRecord> list = records.getOrDefault(player, Collections.emptyList());
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<?> list = cfg.getList(key, Collections.emptyList());
                List<DeathRecord> recs = new ArrayList<>();
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> raw)) continue;
                    @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) raw;
                    recs.add(new DeathRecord(
                            (String) m.get("world"),
                            ((Number) m.get("x")).intValue(),
                            ((Number) m.get("y")).intValue(),
                            ((Number) m.get("z")).intValue(),
                            (String) m.getOrDefault("cause", "未知"),
                            (String) m.getOrDefault("date",  "")));
                }
                records.put(uuid, recs);
            } catch (Exception ignored) {}
        }
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        records.forEach((uuid, list) -> {
            List<Map<String, Object>> ser = new ArrayList<>();
            for (DeathRecord r : list) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("world", r.world());
                m.put("x", r.x()); m.put("y", r.y()); m.put("z", r.z());
                m.put("cause", r.cause()); m.put("date", r.date());
                ser.add(m);
            }
            cfg.set(uuid.toString(), ser);
        });
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("deaths.yml 保存失败: " + e.getMessage());
        }
    }
}
