package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WaypointManager {

    public record Waypoint(String name, String world, int x, int y, int z) {}

    private static final int MAX_PER_PLAYER = 10;

    private final ES2UniPlugin              plugin;
    private final File                      dataFile;
    private final Map<UUID, List<Waypoint>> waypoints = new HashMap<>();

    public WaypointManager(ES2UniPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "waypoints.yml");
        load();
    }

    public boolean add(UUID player, String name, String world, int x, int y, int z) {
        List<Waypoint> list = waypoints.computeIfAbsent(player, k -> new ArrayList<>());
        if (list.size() >= MAX_PER_PLAYER) return false;
        list.removeIf(w -> w.name().equalsIgnoreCase(name)); // overwrite same name
        list.add(new Waypoint(name, world, x, y, z));
        save(); return true;
    }

    public boolean remove(UUID player, String name) {
        List<Waypoint> list = waypoints.get(player);
        if (list == null) return false;
        boolean removed = list.removeIf(w -> w.name().equalsIgnoreCase(name));
        if (removed) save();
        return removed;
    }

    public void removeByIndex(UUID player, int index) {
        List<Waypoint> list = waypoints.get(player);
        if (list != null && index >= 0 && index < list.size()) {
            list.remove(index);
            save();
        }
    }

    public List<Waypoint> get(UUID player) {
        return Collections.unmodifiableList(waypoints.getOrDefault(player, Collections.emptyList()));
    }

    public Waypoint getByName(UUID player, String name) {
        return waypoints.getOrDefault(player, Collections.emptyList()).stream()
                .filter(w -> w.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public int maxPerPlayer() { return MAX_PER_PLAYER; }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<?> list = cfg.getList(key, Collections.emptyList());
                List<Waypoint> wps = new ArrayList<>();
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> raw)) continue;
                    @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) raw;
                    wps.add(new Waypoint(
                            (String) m.get("name"),
                            (String) m.get("world"),
                            ((Number) m.get("x")).intValue(),
                            ((Number) m.get("y")).intValue(),
                            ((Number) m.get("z")).intValue()));
                }
                waypoints.put(uuid, wps);
            } catch (Exception ignored) {}
        }
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        waypoints.forEach((uuid, list) -> {
            List<Map<String, Object>> ser = new ArrayList<>();
            for (Waypoint w : list) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", w.name()); m.put("world", w.world());
                m.put("x", w.x()); m.put("y", w.y()); m.put("z", w.z());
                ser.add(m);
            }
            cfg.set(uuid.toString(), ser);
        });
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("waypoints.yml 保存失败: " + e.getMessage());
        }
    }
}
