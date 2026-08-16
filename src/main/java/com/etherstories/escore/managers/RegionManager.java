package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RegionManager {

    public enum RegionType { DISTRICT, TERRITORY }

    public record Region(
            String id, String name, String world,
            RegionType type, UUID owner,
            int x1, int z1, int x2, int z2,
            int spawnX, int spawnY, int spawnZ, boolean visible) {

        static final int UNSET = Integer.MIN_VALUE;

        public boolean contains(String w, int x, int z) {
            return world.equals(w)
                    && x >= Math.min(x1, x2) && x <= Math.max(x1, x2)
                    && z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
        }

        public long area() {
            return (long) Math.abs(x2 - x1 + 1) * Math.abs(z2 - z1 + 1);
        }

        public int centerX() { return (x1 + x2) / 2; }
        public int centerZ() { return (z1 + z2) / 2; }

        public boolean hasSpawn() { return spawnX != UNSET && spawnZ != UNSET; }

        public int tpX() { return hasSpawn() ? spawnX : centerX(); }
        public int tpZ() { return hasSpawn() ? spawnZ : centerZ(); }

        public Location teleportLoc(World w) {
            int x = tpX();
            int z = tpZ();
            int y = spawnY > 0 ? spawnY : w.getHighestBlockYAt(x, z) + 1;
            return new Location(w, x + 0.5, y, z + 0.5);
        }

        public Region withVisible(boolean v) {
            return new Region(id, name, world, type, owner, x1, z1, x2, z2, spawnX, spawnY, spawnZ, v);
        }

        public Region withSpawn(int x, int y, int z) {
            return new Region(id, name, world, type, owner, x1, z1, x2, z2, x, y, z, visible);
        }
    }

    public record RegionResult(Region district, Region territory) {
        public String format(String separator) {
            if (district == null && territory == null) return null;
            if (district != null && territory != null)
                return district.name() + separator + territory.name();
            return district != null ? district.name() : territory.name();
        }
    }

    private final ES2UniPlugin      plugin;
    private final File              dataFile;
    private final List<Region>      regions  = new ArrayList<>();
    private final Map<UUID, int[]>  pos1     = new HashMap<>();
    private final Map<UUID, int[]>  pos2     = new HashMap<>();
    private final Map<UUID, String> posWorld = new HashMap<>();

    public RegionManager(ES2UniPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "regions.yml");
        load();
    }

    // ── Selection ─────────────────────────────────────────────────────────────

    public void setPos1(UUID player, String world, int x, int z) {
        pos1.put(player, new int[]{x, z});
        posWorld.put(player, world);
    }

    public void setPos2(UUID player, String world, int x, int z) {
        pos2.put(player, new int[]{x, z});
    }

    public boolean hasSelection(UUID player) {
        return pos1.containsKey(player) && pos2.containsKey(player);
    }

    public String getSelectionInfo(UUID player) {
        if (!hasSelection(player)) return null;
        int[] p1 = pos1.get(player);
        int[] p2 = pos2.get(player);
        return String.format("(%d,%d) → (%d,%d) | 世界: %s | 面积: %d",
                p1[0], p1[1], p2[0], p2[1], posWorld.get(player),
                (long) Math.abs(p2[0]-p1[0]+1) * Math.abs(p2[1]-p1[1]+1));
    }

    public long getSelectionArea(UUID player) {
        if (!hasSelection(player)) return 0;
        int[] p1 = pos1.get(player);
        int[] p2 = pos2.get(player);
        return (long) Math.abs(p2[0]-p1[0]+1) * Math.abs(p2[1]-p1[1]+1);
    }

    // ── Create / Delete ───────────────────────────────────────────────────────

    public boolean createDistrict(UUID creator, String name) {
        if (!hasSelection(creator)) return false;
        int[] p1    = pos1.remove(creator);
        int[] p2    = pos2.remove(creator);
        String world = posWorld.remove(creator);
        regions.add(new Region(UUID.randomUUID().toString(), name, world,
                RegionType.DISTRICT, null, p1[0], p1[1], p2[0], p2[1],
                Region.UNSET, 64, Region.UNSET, false));
        save(); return true;
    }

    /** 检查选区是否与已有领地重叠（区域不限制）。返回冲突的领地，无冲突返回 null。 */
    public Region checkTerritoryOverlap(UUID player) {
        if (!hasSelection(player)) return null;
        int[] p1 = pos1.get(player);
        int[] p2 = pos2.get(player);
        String w = posWorld.get(player);
        int ax1 = Math.min(p1[0], p2[0]), ax2 = Math.max(p1[0], p2[0]);
        int az1 = Math.min(p1[1], p2[1]), az2 = Math.max(p1[1], p2[1]);
        for (Region r : regions) {
            if (r.type() != RegionType.TERRITORY) continue;
            if (!r.world().equals(w)) continue;
            int bx1 = Math.min(r.x1(), r.x2()), bx2 = Math.max(r.x1(), r.x2());
            int bz1 = Math.min(r.z1(), r.z2()), bz2 = Math.max(r.z1(), r.z2());
            if (ax1 <= bx2 && ax2 >= bx1 && az1 <= bz2 && az2 >= bz1) return r;
        }
        return null;
    }

    public boolean claimTerritory(UUID owner, String name, int spawnX, int spawnY, int spawnZ) {
        if (!hasSelection(owner)) return false;
        int[] p1    = pos1.remove(owner);
        int[] p2    = pos2.remove(owner);
        String world = posWorld.remove(owner);
        regions.add(new Region(UUID.randomUUID().toString(), name, world,
                RegionType.TERRITORY, owner, p1[0], p1[1], p2[0], p2[1],
                spawnX, spawnY, spawnZ, false));
        save(); return true;
    }

    public Optional<Region> setSpawn(String name, UUID requester, boolean isAdmin, int x, int y, int z) {
        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);
            if (r.type() != RegionType.TERRITORY) continue;
            if (!r.name().equalsIgnoreCase(name)) continue;
            if (!isAdmin && !requester.equals(r.owner())) continue;
            Region next = r.withSpawn(x, y, z);
            regions.set(i, next);
            save();
            return Optional.of(next);
        }
        return Optional.empty();
    }

    public boolean deleteByName(String name, UUID requester, boolean isAdmin) {
        return regions.removeIf(r -> r.name().equalsIgnoreCase(name)
                && (isAdmin || (r.owner() != null && r.owner().equals(requester))))
                && (save() || true);
    }

    /**
     * 删除领地并返回被删除的 Region（用于计算退款）。
     * 找不到 / 无权限返回 null。
     */
    public Region deleteByNameWithInfo(String name, UUID requester, boolean isAdmin) {
        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);
            if (!r.name().equalsIgnoreCase(name)) continue;
            if (!isAdmin && (r.owner() == null || !r.owner().equals(requester))) continue;
            regions.remove(i);
            save();
            return r;
        }
        return null;
    }

    /**
     * Toggle territory visibility.
     * @return new state (true=public), or empty if not found / no permission.
     */
    public Optional<Boolean> toggleVisible(String name, UUID requester, boolean isAdmin) {
        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);
            if (r.type() != RegionType.TERRITORY) continue;
            if (!r.name().equalsIgnoreCase(name)) continue;
            if (!isAdmin && !requester.equals(r.owner())) continue;
            boolean next = !r.visible();
            regions.set(i, r.withVisible(next));
            save();
            return Optional.of(next);
        }
        return Optional.empty();
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public RegionResult getLocation(String world, int x, int z) {
        Region bestDistrict  = null;
        Region bestTerritory = null;
        for (Region r : regions) {
            if (!r.contains(world, x, z)) continue;
            if (r.type() == RegionType.DISTRICT) {
                if (bestDistrict == null || r.area() < bestDistrict.area())
                    bestDistrict = r;
            } else {
                if (bestTerritory == null || r.area() < bestTerritory.area())
                    bestTerritory = r;
            }
        }
        return new RegionResult(bestDistrict, bestTerritory);
    }

    public List<Region> getAll()              { return Collections.unmodifiableList(regions); }
    public List<Region> getByOwner(UUID uuid) { return regions.stream().filter(r -> uuid.equals(r.owner())).toList(); }
    public List<Region> getDistricts()        { return regions.stream().filter(r -> r.type() == RegionType.DISTRICT).toList(); }
    public List<Region> getAllTerritories()   { return regions.stream().filter(r -> r.type() == RegionType.TERRITORY).toList(); }
    public List<Region> getPublicTerritories(){ return regions.stream().filter(r -> r.type() == RegionType.TERRITORY && r.visible()).toList(); }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        List<?> list = cfg.getList("regions", Collections.emptyList());
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> raw)) continue;
            @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) raw;
            try {
                int spawnY  = m.containsKey("spawnY")  ? ((Number) m.get("spawnY")).intValue() : 64;
                int spawnX  = m.containsKey("spawnX")  ? ((Number) m.get("spawnX")).intValue() : Region.UNSET;
                int spawnZ  = m.containsKey("spawnZ")  ? ((Number) m.get("spawnZ")).intValue() : Region.UNSET;
                boolean vis = m.containsKey("visible") && (Boolean) m.get("visible");
                regions.add(new Region(
                        (String) m.get("id"),
                        (String) m.get("name"),
                        (String) m.get("world"),
                        RegionType.valueOf((String) m.get("type")),
                        m.get("owner") != null ? UUID.fromString((String) m.get("owner")) : null,
                        ((Number) m.get("x1")).intValue(),
                        ((Number) m.get("z1")).intValue(),
                        ((Number) m.get("x2")).intValue(),
                        ((Number) m.get("z2")).intValue(),
                        spawnX, spawnY, spawnZ, vis));
            } catch (Exception ignored) {}
        }
    }

    private boolean save() {
        FileConfiguration cfg = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Region r : regions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",      r.id());
            m.put("name",    r.name());
            m.put("world",   r.world());
            m.put("type",    r.type().name());
            m.put("owner",   r.owner() != null ? r.owner().toString() : null);
            m.put("x1", r.x1()); m.put("z1", r.z1());
            m.put("x2", r.x2()); m.put("z2", r.z2());
            if (r.hasSpawn()) {
                m.put("spawnX", r.spawnX());
                m.put("spawnZ", r.spawnZ());
            }
            m.put("spawnY",  r.spawnY());
            m.put("visible", r.visible());
            list.add(m);
        }
        cfg.set("regions", list);
        try { cfg.save(dataFile); return true; } catch (IOException e) {
            plugin.getLogger().warning("regions.yml 保存失败: " + e.getMessage());
            return false;
        }
    }
}
