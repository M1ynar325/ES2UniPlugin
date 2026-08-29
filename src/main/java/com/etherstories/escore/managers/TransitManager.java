package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.items.TransitItems;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Openable;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 轨道交通：线路图、闸机进出、八达通/闪付/单程票、未出站补收。
 */
public class TransitManager {

    public record Line(String id, String displayName, String type, String color,
                       double hopFare, double maxFare) {}

    public record Station(String id, String displayName, String nameEn, String world,
                          int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                          List<String> lineIds, String hintWorld, Double hintX, Double hintY, Double hintZ) {
        public boolean contains(Location loc) {
            if (loc.getWorld() == null || !loc.getWorld().getName().equals(world)) return false;
            int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        public Location center() {
            World w = Bukkit.getWorld(world);
            if (w == null) return null;
            return new Location(w, (minX + maxX) / 2.0 + 0.5, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0 + 0.5);
        }
    }

    public record Edge(String from, String to, String lineId, double fare, boolean bidirectional) {}

    public record Gate(String world, int x, int y, int z, String stationId, String mode, String cabin) {
        public Gate {
            if (cabin == null || cabin.isBlank()) cabin = Cabin.STD;
        }
        public String key() { return locKey(world, x, y, z); }
        public boolean allowsIn() { return "IN".equals(mode) || "BOTH".equals(mode); }
        public boolean allowsOut() { return "OUT".equals(mode) || "BOTH".equals(mode); }
    }

    public record Tvm(String world, int x, int y, int z, String stationId) {
        public String key() { return locKey(world, x, y, z); }
    }

    public record AdjustBooth(String world, int x, int y, int z, String stationId) {
        public String key() { return locKey(world, x, y, z); }
    }

    public record ExitPass(String stationId, long untilMs, String cabin) {
        public ExitPass {
            if (cabin == null || cabin.isBlank()) cabin = Cabin.STD;
        }
    }

    public record AdjustQuote(boolean payable, double amount, String kind, String title, String detail) {
        public static AdjustQuote none(String title, String detail) {
            return new AdjustQuote(false, 0, "none", title, detail);
        }
    }

    public record Journey(String originId, long tapInMs, String method, String ticketDest, String cabin) {
        public Journey {
            if (cabin == null || cabin.isBlank()) cabin = Cabin.STD;
        }
    }

    public record Ride(String fromId, String toId, double fare, long at, String note) {}

    public record RideStat(UUID uuid, String name, int rides, double fare) {}

    public record PathResult(boolean reachable, double fare, List<String> hops) {
        public static PathResult none() { return new PathResult(false, 0, List.of()); }
        public static PathResult same() { return new PathResult(true, 0, List.of()); }
    }

    public record Account(boolean tapPay, UUID cardId) {}

    public record LineType(String id, String displayName, String color) {}

    /** 席别：同一车站可挂多种闸机（普通 / 商务 / 自定义名），进出必须对上。 */
    public record Cabin(String id, String displayName, double fareMul) {
        public static final String STD = "std";
        public Cabin {
            if (fareMul < 0) fareMul = 0;
        }
    }

    public record Claim(String id, UUID player, String playerName, String originId, String originName,
                        long tapInMs, long createdAt, String status) {}

    public record TapResult(boolean success, boolean openFlaps, String msg) {
        public static TapResult ok(String msg) { return new TapResult(true, true, msg); }
        public static TapResult info(String msg) { return new TapResult(true, true, msg); }
        public static TapResult fail(String msg) {
            if (msg == null || msg.isBlank()) msg = "操作失败";
            if (!msg.contains("&")) msg = "&c" + msg;
            return new TapResult(false, false, msg);
        }
    }

    private final ES2UniPlugin plugin;
    private final File dataFile;

    private final Map<String, Line> lines = new LinkedHashMap<>();
    private final Map<String, Station> stations = new LinkedHashMap<>();
    private final Set<String> skipStops = new LinkedHashSet<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<String, Gate> gates = new HashMap<>();
    private final Map<String, Tvm> tvms = new HashMap<>();
    private final Map<String, AdjustBooth> adjusts = new HashMap<>();
    private final Map<UUID, ExitPass> exitPasses = new HashMap<>();
    private final Map<UUID, Account> accounts = new HashMap<>();
    private final Map<UUID, Journey> journeys = new HashMap<>();
    private final Map<UUID, List<Ride>> history = new HashMap<>();
    private final Map<UUID, RideStat> rideStats = new HashMap<>();

    private final Map<UUID, Long> tapCool = new HashMap<>();
    private final Map<String, LineType> types = new LinkedHashMap<>();
    private final Map<String, Cabin> cabins = new LinkedHashMap<>();
    private final List<Claim> claims = new ArrayList<>();
    private final Map<UUID, Location> boxPos1 = new HashMap<>();
    private final Map<UUID, Location> boxPos2 = new HashMap<>();
    private final Map<UUID, String> pendingTvmBind = new HashMap<>();
    private final Map<UUID, RideFx> rideFx = new HashMap<>();

    private static final class RideFx {
        final String originId;
        long originInsideSince;
        long pendingSince;
        long announcedAt;
        String pendingArrive;
        String announcedId;
        boolean departed;
        boolean leftOrigin;
        final java.util.Set<String> arrived = new java.util.HashSet<>();
        RideFx(String originId) { this.originId = originId; }
    }

    public TransitManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "transit.yml");
        load();
        ensureDefaultTypes();
        ensureDefaultCabins();
    }

    public Collection<LineType> allTypes() { return Collections.unmodifiableCollection(types.values()); }
    public LineType getType(String id) { return types.get(id); }
    public Collection<Cabin> allCabins() { return Collections.unmodifiableCollection(cabins.values()); }
    public Cabin getCabin(String id) { return resolveCabin(id); }
    public List<Claim> pendingClaims() {
        List<Claim> out = new ArrayList<>();
        for (Claim c : claims) if ("PENDING".equals(c.status())) out.add(c);
        return out;
    }
    public List<Claim> claimsOf(UUID uuid) {
        List<Claim> out = new ArrayList<>();
        for (Claim c : claims) if (c.player().equals(uuid)) out.add(c);
        return out;
    }
    public Collection<Line> allLines() { return Collections.unmodifiableCollection(lines.values()); }
    public Collection<Station> allStations() { return Collections.unmodifiableCollection(stations.values()); }
    public List<Edge> allEdges() { return Collections.unmodifiableList(edges); }
    public Line getLine(String id) { return lines.get(id); }
    public Station getStation(String id) { return stations.get(id); }
    public Journey getJourney(UUID uuid) {
        expireStale(uuid, true);
        return journeys.get(uuid);
    }

    public List<Map.Entry<UUID, Journey>> activeJourneys() {
        expireAllStale();
        return new ArrayList<>(journeys.entrySet());
    }

    public String adminClearJourney(UUID uuid) {
        expireStale(uuid, false);
        Journey j = journeys.remove(uuid);
        if (j == null) return "该玩家没有在乘行程";
        rideFx.remove(uuid);
        addRide(uuid, j.originId(), j.originId(), 0, "admin-clear");
        save();
        return null;
    }
    public Account getAccount(UUID uuid) { return accounts.getOrDefault(uuid, new Account(false, null)); }
    public List<RideStat> topRiders(int limit) {
        List<RideStat> out = new ArrayList<>(rideStats.values());
        out.sort((a, b) -> {
            int c = Integer.compare(b.rides(), a.rides());
            if (c != 0) return c;
            return Double.compare(b.fare(), a.fare());
        });
        if (out.size() > limit) return out.subList(0, limit);
        return out;
    }

    public List<Ride> ridesOf(UUID uuid) {
        List<Ride> list = history.get(uuid);
        return list == null ? List.of() : Collections.unmodifiableList(list);
    }

    public boolean isSkipStop(String id) { return skipStops.contains(id); }

    public String setSkipStop(String id, Boolean on) {
        if (!stations.containsKey(id)) return "车站不存在";
        boolean next = on != null ? on : !skipStops.contains(id);
        if (next) skipStops.add(id);
        else skipStops.remove(id);
        save();
        return null;
    }

    public String stationName(String id) {
        Station s = stations.get(id);
        return s == null ? id : s.displayName();
    }

    public List<Edge> edgesOfLine(String lineId) {
        List<Edge> out = new ArrayList<>();
        for (Edge e : edges) if (e.lineId().equals(lineId)) out.add(e);
        return out;
    }

    /** 本站在该线上的邻站（显示名 + 票价） */
    public List<String> neighborLabels(String stationId, String lineId) {
        List<String> out = new ArrayList<>();
        for (Edge e : edges) {
            if (lineId != null && !e.lineId().equals(lineId)) continue;
            String other = null;
            if (e.from().equals(stationId)) other = e.to();
            else if (e.bidirectional() && e.to().equals(stationId)) other = e.from();
            if (other == null) continue;
            double fare = e.fare() >= 0 ? e.fare() : (getLine(e.lineId()) == null
                    ? plugin.getConfig().getDouble("transit.default-hop-fare", 2.0)
                    : getLine(e.lineId()).hopFare());
            out.add(stationName(other) + (isSkipStop(other) ? " &e通过" : "") + " &8(" + fare + ")");
        }
        return out;
    }

    public Gate gateAt(Block block) {
        if (block == null) return null;
        return gates.get(locKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
    }

    public Tvm tvmAt(Block block) {
        if (block == null) return null;
        return tvms.get(locKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
    }

    public AdjustBooth adjustAt(Block block) {
        if (block == null) return null;
        return adjusts.get(locKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
    }

    public Station stationAt(Location loc) {
        return stationAt(loc, null);
    }

    public Station stationAt(Location loc, String excludeId) {
        if (loc == null) return null;
        Station excluded = null;
        for (Station s : stations.values()) {
            if (!s.contains(loc)) continue;
            if (excludeId != null && excludeId.equals(s.id())) {
                excluded = s;
                continue;
            }
            return s;
        }
        return excluded;
    }

    public String formatActionBar(Player player, Location loc) {
        Journey j = player == null ? null : getJourney(player.getUniqueId());
        Station s = stationAt(loc, j == null ? null : j.originId());
        if (s == null) return null;
        String lineNames = lineNamesOf(s);
        String title = barStationTitle(s);
        RideFx fx = player == null ? null : rideFx.get(player.getUniqueId());
        if (fx != null && s.id().equals(fx.announcedId)
                && System.currentTimeMillis() - fx.announcedAt < 8000L) {
            title = title + " &e到了";
        }
        return ColorUtil.colorize("&b" + title + " &8· &f" + lineNames);
    }

    public String barStationTitle(Station s) {
        String zh = s.displayName() == null || s.displayName().isBlank() ? s.id() : s.displayName().trim();
        String en = s.nameEn() == null ? "" : s.nameEn().trim();
        boolean both = !en.isBlank() && !en.equalsIgnoreCase(zh);
        if (both) {
            long sec = Math.max(2, plugin.getConfig().getInt("transit.bilingual-seconds", 3));
            boolean enPhase = (System.currentTimeMillis() / (sec * 1000L)) % 2 == 1;
            if (enPhase) return en;
        }
        if (isAsciiName(zh)) return zh;
        return zh.endsWith("站") ? zh : zh + "站";
    }

    private String tapStationLabel(Station s) {
        String zh = s.displayName() == null || s.displayName().isBlank() ? s.id() : s.displayName().trim();
        if (isAsciiName(zh)) return zh;
        return zh.endsWith("站") ? zh : zh + "站";
    }

    private static boolean isAsciiName(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) > 127) return false;
        return true;
    }

    public String lineNamesOf(Station s) {
        List<String> names = new ArrayList<>();
        for (String lid : s.lineIds()) {
            Line l = lines.get(lid);
            if (l != null) names.add(l.color() + l.displayName());
            else names.add(lid);
        }
        return names.isEmpty() ? "未接线" : String.join("&8 / &f", names);
    }

    public boolean hasCreateHint(Station s) {
        if (s.hintWorld() != null && s.hintX() != null) return true;
        Location c = s.center();
        if (c == null) return false;
        List<String> mats = plugin.getConfig().getStringList("transit.create-blocks");
        if (mats == null || mats.isEmpty()) return false;
        Set<String> want = new HashSet<>();
        for (String m : mats) want.add(m.toLowerCase(Locale.ROOT));
        int r = plugin.getConfig().getInt("transit.create-scan-radius", 8);
        World w = c.getWorld();
        int cx = c.getBlockX(), cy = c.getBlockY(), cz = c.getBlockZ();
        try {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -3; dy <= 4; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        Material mat = w.getBlockAt(cx + dx, cy + dy, cz + dz).getType();
                        if (want.contains(mat.name().toLowerCase(Locale.ROOT))) return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    // ── Network edit ──────────────────────────────────────────────────────────

    public String createLine(String id, String type) {
        id = sanitize(id);
        if (id.isEmpty()) return "线路 ID 无效";
        if (lines.containsKey(id)) return "线路已存在";
        LineType t = resolveType(type, true);
        double hop = plugin.getConfig().getDouble("transit.default-hop-fare", 2.0);
        double max = plugin.getConfig().getDouble("transit.default-max-fare", 18.0);
        lines.put(id, new Line(id, id, t.id(), t.color(), hop, max));
        save();
        return null;
    }

    public String setLineType(String lineId, String typeId) {
        Line l = lines.get(lineId);
        if (l == null) return "线路不存在";
        LineType t = resolveType(typeId, false);
        if (t == null) return "类型不存在，先在管理里创建";
        lines.put(lineId, new Line(l.id(), l.displayName(), t.id(), t.color(), l.hopFare(), l.maxFare()));
        save();
        return null;
    }

    public String renameLine(String id, String name) {
        Line l = lines.get(id);
        if (l == null) return "线路不存在";
        if (name == null || name.isBlank()) return "名称不能为空";
        name = name.trim();
        if (name.length() > 24) name = name.substring(0, 24);
        lines.put(id, new Line(l.id(), name, l.type(), l.color(), l.hopFare(), l.maxFare()));
        save();
        return null;
    }

    public String setLineFares(String id, double hop, double max) {
        Line l = lines.get(id);
        if (l == null) return "线路不存在";
        if (hop < 0 || max < 0) return "票价不能为负";
        lines.put(id, new Line(l.id(), l.displayName(), l.type(), l.color(), hop, max));
        save();
        return null;
    }

    public String deleteLine(String id) {
        if (!lines.containsKey(id)) return "线路不存在";
        lines.remove(id);
        edges.removeIf(e -> e.lineId().equals(id));
        for (Station s : new ArrayList<>(stations.values())) {
            if (!s.lineIds().contains(id)) continue;
            List<String> lids = new ArrayList<>(s.lineIds());
            lids.remove(id);
            stations.put(s.id(), withLines(s, lids));
        }
        save();
        return null;
    }

    public String createStation(String id, Location loc, int radius) {
        id = sanitize(id);
        if (id.isEmpty()) return "车站 ID 无效";
        if (stations.containsKey(id)) return "车站已存在";
        if (loc.getWorld() == null) return "世界无效";
        radius = Math.max(2, Math.min(64, radius));
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        stations.put(id, new Station(id, id, id, loc.getWorld().getName(),
                x - radius, y - 8, z - radius, x + radius, y + 16, z + radius,
                new ArrayList<>(), null, null, null, null));
        save();
        return null;
    }

    public String renameStation(String id, String name) {
        Station s = stations.get(id);
        if (s == null) return "车站不存在";
        String cleaned = cleanName(name, 16);
        if (cleaned == null) return "名称不能为空";
        stations.put(id, withNames(s, cleaned, s.nameEn()));
        save();
        refreshGateSigns(id);
        refreshAdjustSigns(id);
        return null;
    }

    public String renameStationEn(String id, String name) {
        Station s = stations.get(id);
        if (s == null) return "车站不存在";
        String cleaned = cleanName(name, 24);
        if (cleaned == null) return "名称不能为空";
        stations.put(id, withNames(s, s.displayName(), cleaned));
        save();
        refreshGateSigns(id);
        refreshAdjustSigns(id);
        return null;
    }

    private static String cleanName(String name, int maxChars) {
        if (name == null) return null;
        name = name.replace("§", "").replace("&", "").trim();
        if (name.isEmpty()) return null;
        if (name.length() > maxChars) name = name.substring(0, maxChars);
        return name;
    }

    public String addStationLine(String stationId, String lineId) {
        Station s = stations.get(stationId);
        if (s == null) return "车站不存在";
        if (!lines.containsKey(lineId)) return "线路不存在";
        List<String> lids = new ArrayList<>(s.lineIds());
        if (!lids.contains(lineId)) lids.add(lineId);
        stations.put(stationId, withLines(s, lids));
        save();
        return null;
    }

    public String removeStationLine(String stationId, String lineId) {
        Station s = stations.get(stationId);
        if (s == null) return "车站不存在";
        List<String> lids = new ArrayList<>(s.lineIds());
        if (!lids.remove(lineId)) return "该站未挂这条线";
        stations.put(stationId, withLines(s, lids));
        edges.removeIf(e -> e.lineId().equals(lineId)
                && (e.from().equals(stationId) || e.to().equals(stationId)));
        save();
        return null;
    }

    public int stationRadius(Station s) {
        int rx = (s.maxX() - s.minX()) / 2;
        int rz = (s.maxZ() - s.minZ()) / 2;
        return Math.max(2, Math.min(64, Math.max(rx, rz)));
    }

    public String moveStation(String id, Location loc, int radius) {
        Station s = stations.get(id);
        if (s == null) return "车站不存在";
        if (loc.getWorld() == null) return "世界无效";
        radius = Math.max(2, Math.min(64, radius));
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        stations.put(id, new Station(s.id(), s.displayName(), s.nameEn(), loc.getWorld().getName(),
                x - radius, y - 8, z - radius, x + radius, y + 16, z + radius,
                s.lineIds(), s.hintWorld(), s.hintX(), s.hintY(), s.hintZ()));
        save();
        return null;
    }

    public String teleportToStation(Player player, String id) {
        Station s = stations.get(id);
        if (s == null) return "车站不存在";
        Location c = s.center();
        if (c == null) return "车站所在世界未加载";
        player.teleport(c);
        return null;
    }

    public String markBoxCorner(Player player, int which) {
        Location loc = player.getLocation();
        if (loc.getWorld() == null) return "世界无效";
        if (which == 1) boxPos1.put(player.getUniqueId(), loc.clone());
        else boxPos2.put(player.getUniqueId(), loc.clone());
        return null;
    }

    public Location boxCorner(Player player, int which) {
        return which == 1 ? boxPos1.get(player.getUniqueId()) : boxPos2.get(player.getUniqueId());
    }

    public String applyBox(Player player, String stationId) {
        Station s = stations.get(stationId);
        if (s == null) return "车站不存在";
        Location a = boxPos1.get(player.getUniqueId());
        Location b = boxPos2.get(player.getUniqueId());
        if (a == null || b == null) return "先站在两个对角点，分别点「角点1」「角点2」";
        if (a.getWorld() == null || b.getWorld() == null) return "世界无效";
        if (!a.getWorld().getName().equals(b.getWorld().getName())) return "两个角点必须在同一世界";
        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int maxX = Math.max(a.getBlockX(), b.getBlockX());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ());
        int minY = Math.min(a.getBlockY(), b.getBlockY()) - 8;
        int maxY = Math.max(a.getBlockY(), b.getBlockY()) + 16;
        if (maxX - minX > 384 || maxZ - minZ > 384) return "XZ 范围太大（最大 384）";
        if (maxX - minX < 2 || maxZ - minZ < 2) return "范围太小，对角再拉开一点";
        stations.put(stationId, new Station(s.id(), s.displayName(), s.nameEn(), a.getWorld().getName(),
                minX, minY, minZ, maxX, maxY, maxZ,
                s.lineIds(), s.hintWorld(), s.hintX(), s.hintY(), s.hintZ()));
        save();
        return null;
    }

    public String boxLabel(Station s) {
        return s.minX() + "," + s.minZ() + " → " + s.maxX() + "," + s.maxZ()
                + "  Y " + s.minY() + "~" + s.maxY();
    }

    public void beginTvmBind(Player player, String stationId) {
        pendingTvmBind.put(player.getUniqueId(), stationId);
    }

    public String consumeTvmBind(Player player) {
        return pendingTvmBind.remove(player.getUniqueId());
    }

    public boolean isBindingTvm(Player player) {
        return pendingTvmBind.containsKey(player.getUniqueId());
    }

    public String typeName(String typeId) {
        LineType t = types.get(typeId);
        return t == null ? typeId : t.displayName();
    }

    public String setCreateHint(String stationId, Location loc) {
        Station s = stations.get(stationId);
        if (s == null) return "车站不存在";
        if (loc.getWorld() == null) return "世界无效";
        stations.put(stationId, new Station(s.id(), s.displayName(), s.nameEn(), s.world(),
                s.minX(), s.minY(), s.minZ(), s.maxX(), s.maxY(), s.maxZ(),
                s.lineIds(), loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
        save();
        return null;
    }

    public String deleteStation(String id) {
        if (!stations.containsKey(id)) return "车站不存在";
        stations.remove(id);
        skipStops.remove(id);
        edges.removeIf(e -> e.from().equals(id) || e.to().equals(id));
        gates.values().removeIf(g -> g.stationId().equals(id));
        tvms.values().removeIf(t -> t.stationId().equals(id));
        adjusts.values().removeIf(t -> t.stationId().equals(id));
        save();
        return null;
    }

    public String addEdge(String from, String to, String lineId, double fare) {
        if (!stations.containsKey(from) || !stations.containsKey(to)) return "车站不存在";
        if (!lines.containsKey(lineId)) return "线路不存在";
        if (from.equals(to)) return "不能连接同一站";
        for (Edge e : edges) {
            if (!e.lineId().equals(lineId)) continue;
            boolean same = (e.from().equals(from) && e.to().equals(to))
                    || (e.bidirectional() && e.from().equals(to) && e.to().equals(from));
            if (same) return "这两站已经连过了";
        }
        edges.add(new Edge(from, to, lineId, fare, true));
        addStationLine(from, lineId);
        addStationLine(to, lineId);
        save();
        return null;
    }

    public String removeEdge(String from, String to, String lineId) {
        boolean ok = edges.removeIf(e -> e.lineId().equals(lineId)
                && ((e.from().equals(from) && e.to().equals(to))
                || (e.bidirectional() && e.from().equals(to) && e.to().equals(from))));
        if (!ok) return "找不到这段连接";
        save();
        return null;
    }

    public void registerGate(Block block, String stationId, String mode) {
        registerGate(block, stationId, mode, Cabin.STD);
    }

    public void registerGate(Block block, String stationId, String mode, String cabin) {
        String m = mode.toUpperCase(Locale.ROOT);
        if (!m.equals("IN") && !m.equals("OUT") && !m.equals("BOTH")) m = "BOTH";
        final String modeSaved = m;
        final String cabinSaved = resolveCabin(cabin).id();
        Gate g = new Gate(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(),
                stationId, modeSaved, cabinSaved);
        gates.put(g.key(), g);
        save();
        plugin.getServer().getScheduler().runTask(plugin, () -> writeGateSign(block, stationId, modeSaved, cabinSaved));
    }

    public void writeGateSign(Block block, String stationId, String mode) {
        writeGateSign(block, stationId, mode, Cabin.STD);
    }

    public void writeGateSign(Block block, String stationId, String mode, String cabin) {
        if (!(block.getState() instanceof Sign sign)) return;
        applyGateSign(sign, stationId, mode, cabin);
        sign.update(true, false);
    }

    public void applyGateSign(Sign sign, String stationId, String mode) {
        applyGateSign(sign, stationId, mode, Cabin.STD);
    }

    public void applyGateSign(Sign sign, String stationId, String mode, String cabin) {
        Station s = getStation(stationId);
        String name = fitSign(s == null ? stationId : s.displayName());
        String modeLabel = switch (mode.toUpperCase(Locale.ROOT)) {
            case "IN" -> "进站";
            case "OUT" -> "出站";
            default -> "进出";
        };
        String modeLine = fitSign(modeLabel + " · " + cabinName(cabin));
        String[] lines = {
                ColorUtil.colorize("&b检票口"),
                ColorUtil.colorize("&f" + name),
                ColorUtil.colorize("&e" + modeLine),
                ColorUtil.colorize("&7请刷卡")
        };
        for (Side side : Side.values()) {
            SignSide face = sign.getSide(side);
            face.setColor(DyeColor.CYAN);
            face.setGlowingText(true);
            for (int i = 0; i < 4; i++) face.setLine(i, lines[i]);
        }
        sign.setWaxed(true);
    }

    private void refreshGateSigns(String stationId) {
        for (Gate g : gates.values()) {
            if (!g.stationId().equals(stationId)) continue;
            World w = Bukkit.getWorld(g.world());
            if (w == null) continue;
            writeGateSign(w.getBlockAt(g.x(), g.y(), g.z()), g.stationId(), g.mode(), g.cabin());
        }
    }

    private static String fitSign(String s) {
        if (s == null) return "";
        int w = 0;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            int cw = s.charAt(i) < 128 ? 1 : 2;
            if (w + cw > 14) break;
            out.append(s.charAt(i));
            w += cw;
        }
        return out.toString();
    }

    public void unregisterGate(Block block) {
        gates.remove(locKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
        save();
    }

    public void registerTvm(Block block, String stationId) {
        Tvm t = new Tvm(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), stationId);
        tvms.put(t.key(), t);
        save();
    }

    public void unregisterTvm(Block block) {
        tvms.remove(locKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
        save();
    }

    public void registerAdjust(Block block, String stationId) {
        AdjustBooth t = new AdjustBooth(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), stationId);
        adjusts.put(t.key(), t);
        save();
        plugin.getServer().getScheduler().runTask(plugin, () -> writeAdjustSign(block, stationId));
    }

    public void unregisterAdjust(Block block) {
        adjusts.remove(locKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
        save();
    }

    public void writeAdjustSign(Block block, String stationId) {
        if (!(block.getState() instanceof Sign sign)) return;
        applyAdjustSign(sign, stationId);
        sign.update(true, false);
    }

    public void applyAdjustSign(Sign sign, String stationId) {
        Station s = getStation(stationId);
        String name = fitSign(s == null ? stationId : s.displayName());
        String[] lines = {
                ColorUtil.colorize("&6补票处"),
                ColorUtil.colorize("&f" + name),
                ColorUtil.colorize("&e补票 / 结算"),
                ColorUtil.colorize("&7右键办理")
        };
        for (Side side : Side.values()) {
            SignSide face = sign.getSide(side);
            face.setColor(DyeColor.ORANGE);
            face.setGlowingText(true);
            for (int i = 0; i < 4; i++) face.setLine(i, lines[i]);
        }
        sign.setWaxed(true);
    }

    private void refreshAdjustSigns(String stationId) {
        for (AdjustBooth t : adjusts.values()) {
            if (!t.stationId().equals(stationId)) continue;
            World w = Bukkit.getWorld(t.world());
            if (w == null) continue;
            writeAdjustSign(w.getBlockAt(t.x(), t.y(), t.z()), t.stationId());
        }
    }

    // ── Accounts ──────────────────────────────────────────────────────────────

    public boolean isTapPay(UUID uuid) {
        return getAccount(uuid).tapPay();
    }

    public String setTapPay(Player player, boolean on) {
        Account a = getAccount(player.getUniqueId());
        if (on && !a.tapPay()) {
            double fee = plugin.getConfig().getDouble("transit.tap-pay-fee", 8.0);
            if (fee > 0) {
                String err = plugin.getVaultHook().withdraw(player, fee);
                if (err != null) return err;
                sink(fee);
            }
        }
        accounts.put(player.getUniqueId(), new Account(on, a.cardId()));
        save();
        return null;
    }

    public ItemStack issueCard(Player player) {
        UUID card = UUID.randomUUID();
        Account a = getAccount(player.getUniqueId());
        accounts.put(player.getUniqueId(), new Account(a.tapPay(), card));
        save();
        return TransitItems.octopus(card, player.getName());
    }

    public String buyCard(Player player) {
        double fee = plugin.getConfig().getDouble("transit.card-fee", 16.0);
        if (fee > 0) {
            String err = plugin.getVaultHook().withdraw(player, fee);
            if (err != null) return err;
            sink(fee);
        }
        ItemStack card = issueCard(player);
        var left = player.getInventory().addItem(card);
        if (!left.isEmpty()) left.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        return null;
    }

    public String reportLost(Player player) {
        Account a = getAccount(player.getUniqueId());
        if (a.cardId() == null) return "你没有已登记的交通卡";
        accounts.put(player.getUniqueId(), new Account(a.tapPay(), null));
        save();
        return null;
    }

    public boolean cardValid(Player player, ItemStack item) {
        if (!TransitItems.isCard(item)) return false;
        String raw = TransitItems.cardId(item);
        if (raw == null) return false;
        Account a = getAccount(player.getUniqueId());
        if (a.cardId() == null) return false;
        try {
            return a.cardId().equals(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ── Tap ───────────────────────────────────────────────────────────────────

    public TapResult tap(Player player, Gate gate, ItemStack hand) {
        long now = System.currentTimeMillis();
        Long cool = tapCool.get(player.getUniqueId());
        if (cool != null && now - cool < 400) return TapResult.fail("请稍候再刷");
        tapCool.put(player.getUniqueId(), now);

        Station here = stations.get(gate.stationId());
        if (here == null) return TapResult.fail("闸机未绑定有效车站，请通知管理员");

        boolean ticket = TransitItems.isTicket(hand);
        boolean card = TransitItems.isCard(hand);
        boolean tapPay = (hand == null || hand.getType() == Material.AIR) && isTapPay(player.getUniqueId());

        if (ticket && TransitItems.ticketExp(hand) > 0 && TransitItems.ticketExp(hand) < now) {
            return TapResult.fail("单程票已过期，请重新购票");
        }
        if (card && !cardValid(player, hand)) return TapResult.fail("此卡已挂失或不是你的卡");
        if (!ticket && !card && !tapPay) {
            return TapResult.fail("请空手刷（需先开通闪付），或手持交通卡 / 单程票");
        }

        String method = ticket ? "TICKET" : (card ? "CARD" : "TAPPAY");
        expireStale(player.getUniqueId(), true);
        Journey cur = journeys.get(player.getUniqueId());
        boolean wantIn = gate.allowsIn();
        boolean wantOut = gate.allowsOut();

        String cabin = gate.cabin();
        if (gate.mode().equals("BOTH")) {
            if (cur == null && peekPass(player, here.id(), cabin) != null)
                return tapOut(player, here, method, hand, cabin);
            if (cur == null) return tapIn(player, here, method, hand, cabin);
            return tapOut(player, here, method, hand, cabin);
        }
        if (wantIn && !wantOut) return tapIn(player, here, method, hand, cabin);
        if (wantOut && !wantIn) return tapOut(player, here, method, hand, cabin);
        return TapResult.fail("闸机模式无效，请通知管理员");
    }

    private TapResult tapIn(Player player, Station here, String method, ItemStack hand, String cabin) {
        Journey cur = journeys.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        long grace = plugin.getConfig().getLong("transit.cancel-grace-seconds", 900) * 1000L;

        if (cur != null) {
            if (!sameCabin(cur.cabin(), cabin)) {
                return TapResult.fail("请走 " + cabinName(cur.cabin()) + " 检票口"
                        + "\n&7当前行程席别与此闸机不符");
            }
            boolean same = cur.originId().equals(here.id());
            long age = now - cur.tapInMs();
            if (same && age <= grace) {
                journeys.remove(player.getUniqueId());
                rideFx.remove(player.getUniqueId());
                save();
                addRide(player.getUniqueId(), cur.originId(), here.id(), 0, "cancel");
                return TapResult.info("&e行程已撤销\n&7未产生费用。十五分钟内于本站再次刷卡均可撤销。");
            }
            return TapResult.fail("存在未完成行程（进站 " + stationName(cur.originId()) + "）"
                    + "\n&7请先到补票处或售票机办理补票，再从出站检票口刷卡离开");
        }

        if ("TICKET".equals(method)) {
            String from = TransitItems.ticketFrom(hand);
            if (from == null || !from.equals(here.id())) {
                return TapResult.fail("本票起点并非本站，请前往票面标明的起点进站。");
            }
            if (!sameCabin(TransitItems.ticketCabin(hand), cabin)) {
                return TapResult.fail("本票席别为 " + cabinName(TransitItems.ticketCabin(hand))
                        + "\n&7请走对应席别检票口进站");
            }
            journeys.put(player.getUniqueId(),
                    new Journey(here.id(), now, method, TransitItems.ticketTo(hand), cabin));
        } else {
            journeys.put(player.getUniqueId(), new Journey(here.id(), now, method, null, cabin));
        }
        RideFx fx = new RideFx(here.id());
        if (here.contains(player.getLocation())) fx.originInsideSince = now;
        rideFx.put(player.getUniqueId(), fx);
        save();
        String destHint = "TICKET".equals(method)
                ? "&8请于票面终点的 " + cabinName(cabin) + " 检票口出站"
                : "&8请于目的地 " + cabinName(cabin) + " 检票口出站结算 · 换乘通道请勿刷卡";
        return TapResult.ok("&b进站成功\n"
                + "&f" + tapStationLabel(here) + " &8· &e" + cabinName(cabin) + " &8· &7" + lineNamesOf(here) + "\n"
                + "&7十五分钟内本站再刷可撤销，不产生费用\n"
                + destHint);
    }

    private TapResult tapOut(Player player, Station here, String method, ItemStack hand, String cabin) {
        Journey cur = journeys.get(player.getUniqueId());
        if (cur == null) {
            ExitPass any = peekPass(player, here.id());
            if (any != null && !sameCabin(any.cabin(), cabin)) {
                return TapResult.fail("请走 " + cabinName(any.cabin()) + " 检票口出站");
            }
            if (consumePass(player, here.id(), cabin) != null) {
                addRide(player.getUniqueId(), here.id(), here.id(), 0, "adjust-out");
                save();
                return TapResult.ok("&b出站成功\n&f" + tapStationLabel(here)
                        + " &8· &e" + cabinName(cabin) + "\n&7补票凭证已核销");
            }
            return TapResult.fail("未查询到进站记录\n&7请前往本站补票处或售票机办理补票后再出站");
        }

        if (!sameCabin(cur.cabin(), cabin)) {
            return TapResult.fail("请走 " + cabinName(cur.cabin()) + " 检票口出站"
                    + "\n&7进站席别与此闸机不符");
        }

        long now = System.currentTimeMillis();
        long grace = plugin.getConfig().getLong("transit.cancel-grace-seconds", 900) * 1000L;
        if (cur.originId().equals(here.id()) && now - cur.tapInMs() <= grace) {
            journeys.remove(player.getUniqueId());
            rideFx.remove(player.getUniqueId());
            save();
            addRide(player.getUniqueId(), cur.originId(), here.id(), 0, "cancel");
            return TapResult.info("&e行程已撤销\n&7未产生费用");
        }

        if ("TICKET".equals(cur.method())) {
            if (cur.ticketDest() == null || !cur.ticketDest().equals(here.id())) {
                Station dest = cur.ticketDest() == null ? null : stations.get(cur.ticketDest());
                return TapResult.fail("本票终点为 " + (dest == null ? "?" : dest.displayName())
                        + "\n&7请前往该站出站，或至补票处 / 售票机办理补票");
            }
            if (!consumeMatchingTicket(player, cur.originId(), here.id(), cabin)) {
                return TapResult.fail("请持对应席别单程票办理出站");
            }
            journeys.remove(player.getUniqueId());
            rideFx.remove(player.getUniqueId());
            addRide(player.getUniqueId(), cur.originId(), here.id(), 0, "ticket");
            save();
            return TapResult.ok("&b出站成功\n&f" + tapStationLabel(here)
                    + " &8· &e" + cabinName(cabin) + "\n&7单程票已核销");
        }

        PathResult path = shortest(cur.originId(), here.id());
        if (!path.reachable()) {
            return TapResult.fail("本站与进站无票价路径\n&7请前往补票处或售票机办理补票后再出站");
        }
        double fare = applyCabin(path.fare(), cabin);
        String note = "normal";
        if (fare < 0) fare = 0;

        if (fare > 0) {
            String err = charge(player, fare);
            if (err != null) return TapResult.fail("出站扣费失败：" + err + "。请充值后再刷卡，行程仍然保留。");
        }
        journeys.remove(player.getUniqueId());
        rideFx.remove(player.getUniqueId());
        addRide(player.getUniqueId(), cur.originId(), here.id(), fare, note);
        save();
        Station origin = stations.get(cur.originId());
        String on = origin == null ? cur.originId() : origin.displayName();
        return TapResult.ok("&b出站成功\n"
                + "&f" + on + " &8→ &f" + tapStationLabel(here) + " &8· &e" + cabinName(cabin) + "\n"
                + (fare <= 0 ? "&7本次免费" : "&e" + fmt(fare)) + "\n"
                + "&8欢迎再次乘车");
    }

    public String buyTicket(Player player, String fromId, String toId) {
        return buyTicket(player, fromId, toId, Cabin.STD);
    }

    public String buyTicket(Player player, String fromId, String toId, String cabinId) {
        Station from = stations.get(fromId);
        Station to = stations.get(toId);
        if (from == null || to == null) return "车站不存在";
        if (fromId.equals(toId)) return "起点终点不能相同";
        Cabin cabin = resolveCabin(cabinId);
        PathResult path = shortest(fromId, toId);
        if (!path.reachable()) return "两站之间没有线路连接";
        double fare = path.fare();
        if (fare <= 0) fare = plugin.getConfig().getDouble("transit.default-hop-fare", 2.0);
        fare = applyCabin(fare, cabin.id());
        String err = charge(player, fare);
        if (err != null) return err;
        long exp = System.currentTimeMillis()
                + plugin.getConfig().getLong("transit.ticket-expire-seconds", 7200) * 1000L;
        ItemStack ticket = TransitItems.ticket(fromId, toId, from.displayName(), to.displayName(),
                exp, cabin.id(), cabin.displayName());
        var left = player.getInventory().addItem(ticket);
        if (!left.isEmpty()) left.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        return null;
    }

    public AdjustQuote quoteAdjust(Player player, String stationId) {
        Station here = stations.get(stationId);
        if (here == null) return AdjustQuote.none("车站无效", "请通知管理员");
        expireStale(player.getUniqueId(), true);
        long now = System.currentTimeMillis();
        if (peekPass(player, stationId) != null) {
            return AdjustQuote.none("已有本站出站凭证", "请前往对应席别出站检票口刷卡离开");
        }
        Journey cur = journeys.get(player.getUniqueId());
        long grace = plugin.getConfig().getLong("transit.cancel-grace-seconds", 900) * 1000L;
        if (cur != null && cur.originId().equals(stationId) && now - cur.tapInMs() <= grace) {
            return AdjustQuote.none("可撤销行程", "请在本站同席别检票口再刷一次即可免费取消，不必补票");
        }
        if (cur == null) {
            return new AdjustQuote(true, round2(maxFareOf(stationId)), "ghost",
                    "未查询到进站记录", "按本站全程票补缴后，可在时限内从本站普通检票口离开");
        }
        String cabinHint = "，请走 " + cabinName(cur.cabin()) + " 检票口离开";
        if ("TICKET".equals(cur.method())) {
            if (cur.ticketDest() != null && cur.ticketDest().equals(stationId)) {
                return AdjustQuote.none("单程票终点为本站", "请持票在 " + cabinName(cur.cabin()) + " 出站检票口刷卡核销");
            }
            PathResult path = shortest(cur.originId(), stationId);
            double fare = path.reachable() ? path.fare() : maxFareOf(cur.originId());
            fare = applyCabin(Math.max(0, fare), cur.cabin());
            Station dest = cur.ticketDest() == null ? null : stations.get(cur.ticketDest());
            String destName = dest == null ? "?" : dest.displayName();
            return new AdjustQuote(true, fare, "ticket-reroute",
                    "单程票终点不符（票面 " + destName + "）",
                    "补缴后核销本票" + cabinHint);
        }
        PathResult path = shortest(cur.originId(), stationId);
        if (!path.reachable()) {
            return new AdjustQuote(true, applyCabin(maxFareOf(cur.originId()), cur.cabin()), "unlinked",
                    "进站站与本站无票价路径", "按进站站全程票补缴后" + cabinHint);
        }
        return new AdjustQuote(true, applyCabin(Math.max(0, path.fare()), cur.cabin()), "settle",
                "结算当前行程（" + stationName(cur.originId()) + " → " + here.displayName()
                        + " · " + cabinName(cur.cabin()) + "）",
                "按最短路缴费后" + cabinHint);
    }

    public String applyAdjust(Player player, String stationId) {
        AdjustQuote q = quoteAdjust(player, stationId);
        if (!q.payable()) return q.title() + "。" + q.detail();
        Station here = stations.get(stationId);
        if (here == null) return "车站不存在";
        Journey cur = journeys.get(player.getUniqueId());
        String err = charge(player, q.amount());
        if (err != null) return err;

        String fromId = cur == null ? stationId : cur.originId();
        if ("ticket-reroute".equals(q.kind()) && cur != null) {
            boolean eaten = cur.ticketDest() != null
                    && consumeMatchingTicket(player, cur.originId(), cur.ticketDest(), cur.cabin());
            if (!eaten) consumeAnyTicketFrom(player, cur.originId());
        }
        journeys.remove(player.getUniqueId());
        rideFx.remove(player.getUniqueId());
        grantExitPass(player, stationId, cur == null ? Cabin.STD : cur.cabin());
        addRide(player.getUniqueId(), fromId, stationId, q.amount(), "adjust-" + q.kind());
        save();
        if ("ghost".equals(q.kind())) {
            notifyAdmins(player.getName() + " 无进站记录，已在 " + here.displayName()
                    + " 补票处缴纳 " + fmt(q.amount()));
        } else if ("unlinked".equals(q.kind())) {
            notifyAdmins(player.getName() + " 无票价路径出站 " + here.displayName()
                    + "，已在补票处缴纳 " + fmt(q.amount())
                    + "（进站 " + stationName(fromId) + "）");
        }
        return null;
    }

    public int adjustPassSeconds() {
        return Math.max(60, plugin.getConfig().getInt("transit.adjust-pass-seconds", 600));
    }

    private void grantExitPass(Player player, String stationId, String cabin) {
        exitPasses.put(player.getUniqueId(),
                new ExitPass(stationId, System.currentTimeMillis() + adjustPassSeconds() * 1000L,
                        resolveCabin(cabin).id()));
    }

    private ExitPass peekPass(Player player, String stationId) {
        ExitPass p = exitPasses.get(player.getUniqueId());
        if (p == null) return null;
        if (p.untilMs() < System.currentTimeMillis()) {
            exitPasses.remove(player.getUniqueId());
            return null;
        }
        if (!p.stationId().equals(stationId)) return null;
        return p;
    }

    private ExitPass peekPass(Player player, String stationId, String cabin) {
        ExitPass p = peekPass(player, stationId);
        if (p == null || !sameCabin(p.cabin(), cabin)) return null;
        return p;
    }

    private ExitPass consumePass(Player player, String stationId, String cabin) {
        ExitPass p = peekPass(player, stationId, cabin);
        if (p == null) return null;
        exitPasses.remove(player.getUniqueId());
        save();
        return p;
    }

    private boolean expireStale(UUID uuid, boolean persist) {
        Journey j = journeys.get(uuid);
        if (j == null) return false;
        long timeout = plugin.getConfig().getLong("transit.journey-timeout-seconds", 5400) * 1000L;
        if (System.currentTimeMillis() - j.tapInMs() <= timeout) return false;
        journeys.remove(uuid);
        rideFx.remove(uuid);
        addRide(uuid, j.originId(), j.originId(), 0, "timeout-expire");
        if (persist) save();
        return true;
    }

    private void expireAllStale() {
        boolean any = false;
        for (UUID id : new ArrayList<>(journeys.keySet())) {
            if (expireStale(id, false)) any = true;
        }
        pruneExpiredPasses();
        if (any) save();
    }

    private void pruneExpiredPasses() {
        long now = System.currentTimeMillis();
        exitPasses.entrySet().removeIf(e -> e.getValue().untilMs() < now);
    }

    public PathResult shortest(String from, String to) {
        if (from == null || to == null) return PathResult.none();
        if (from.equals(to)) return PathResult.same();

        Map<String, List<AbstractMap.SimpleEntry<String, Double>>> adj = new HashMap<>();
        for (Edge e : edges) {
            double w = edgeFare(e);
            adj.computeIfAbsent(e.from(), k -> new ArrayList<>())
                    .add(new AbstractMap.SimpleEntry<>(e.to(), w));
            if (e.bidirectional()) {
                adj.computeIfAbsent(e.to(), k -> new ArrayList<>())
                        .add(new AbstractMap.SimpleEntry<>(e.from(), w));
            }
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<AbstractMap.SimpleEntry<String, Double>> pq =
                new PriorityQueue<>(Comparator.comparingDouble(AbstractMap.SimpleEntry::getValue));
        dist.put(from, 0.0);
        pq.add(new AbstractMap.SimpleEntry<>(from, 0.0));
        while (!pq.isEmpty()) {
            var cur = pq.poll();
            if (cur.getValue() > dist.getOrDefault(cur.getKey(), Double.MAX_VALUE)) continue;
            if (cur.getKey().equals(to)) break;
            for (var n : adj.getOrDefault(cur.getKey(), List.of())) {
                double nd = cur.getValue() + n.getValue();
                if (nd < dist.getOrDefault(n.getKey(), Double.MAX_VALUE)) {
                    dist.put(n.getKey(), nd);
                    prev.put(n.getKey(), cur.getKey());
                    pq.add(new AbstractMap.SimpleEntry<>(n.getKey(), nd));
                }
            }
        }
        if (!dist.containsKey(to)) return PathResult.none();
        LinkedList<String> hops = new LinkedList<>();
        String at = to;
        while (at != null) {
            hops.addFirst(at);
            at = prev.get(at);
        }
        return new PathResult(true, round2(dist.get(to)), hops);
    }

    public double maxFareOf(String stationId) {
        Station s = stations.get(stationId);
        double def = plugin.getConfig().getDouble("transit.default-max-fare", 18.0);
        if (s == null || s.lineIds().isEmpty()) return def;
        double m = 0;
        for (String lid : s.lineIds()) {
            Line l = lines.get(lid);
            if (l != null) m = Math.max(m, l.maxFare());
        }
        return m > 0 ? m : def;
    }

    public String journeyHint(UUID uuid) {
        Journey j = getJourney(uuid);
        if (j == null) return null;
        Station s = stations.get(j.originId());
        String name = s == null ? j.originId() : s.displayName();
        String cabin = cabinName(j.cabin());
        return "在乘: " + name + " → ? · " + cabin;
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private double edgeFare(Edge e) {
        if (e.fare() >= 0) return e.fare();
        Line l = lines.get(e.lineId());
        return l == null ? plugin.getConfig().getDouble("transit.default-hop-fare", 2.0) : l.hopFare();
    }

    private String charge(Player player, double amount) {
        amount = round2(amount);
        if (amount <= 0) return null;
        if (!plugin.getVaultHook().isEnabled()) return "经济系统不可用";
        String err = plugin.getVaultHook().withdraw(player, amount);
        if (err != null) return err;
        sink(amount);
        try {
            plugin.getTradeStatsManager().recordTransit(player, amount);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void sink(double amount) {
        if (amount <= 0) return;
        String name = plugin.getConfig().getString("transit.sink-account", "");
        if (name == null || name.isBlank()) name = plugin.getTaxManager().getSinkAccount();
        if (name == null || name.isBlank()) return;
        try {
            Player online = Bukkit.getPlayerExact(name);
            if (online != null) plugin.getVaultHook().deposit(online, amount);
            else plugin.getVaultHook().deposit(Bukkit.getOfflinePlayer(name), amount);
        } catch (Throwable ignored) {
        }
    }

    private void addRide(UUID uuid, String from, String to, double fare, String note) {
        List<Ride> list = history.computeIfAbsent(uuid, k -> new ArrayList<>());
        list.add(0, new Ride(from, to, fare, System.currentTimeMillis(), note));
        int cap = plugin.getConfig().getInt("transit.history-size", 20);
        while (list.size() > cap) list.remove(list.size() - 1);
        if (countsForBoard(note, fare)) bumpStat(uuid, fare);
    }

    private static boolean countsForBoard(String note, double fare) {
        if ("cancel".equals(note) || "admin-clear".equals(note) || "timeout-expire".equals(note)) return false;
        return fare > 0 || "ticket".equals(note);
    }

    private void bumpStat(UUID uuid, double fare) {
        RideStat cur = rideStats.get(uuid);
        String name = cur != null && cur.name() != null ? cur.name() : nameOf(uuid);
        int rides = cur == null ? 1 : cur.rides() + 1;
        double sum = (cur == null ? 0 : cur.fare()) + Math.max(0, fare);
        rideStats.put(uuid, new RideStat(uuid, name, rides, sum));
    }

    private static String nameOf(UUID uuid) {
        String n = Bukkit.getOfflinePlayer(uuid).getName();
        return n == null ? uuid.toString().substring(0, 8) : n;
    }

    private boolean consumeMatchingTicket(Player player, String from, String to, String cabin) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (isMatchingTicket(hand, from, to, cabin)) {
            consumeOne(player, hand);
            return true;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (isMatchingTicket(off, from, to, cabin)) {
            if (off.getAmount() <= 1) player.getInventory().setItemInOffHand(null);
            else off.setAmount(off.getAmount() - 1);
            return true;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (!isMatchingTicket(it, from, to, cabin)) continue;
            if (it.getAmount() <= 1) player.getInventory().setItem(i, null);
            else it.setAmount(it.getAmount() - 1);
            return true;
        }
        return false;
    }

    private boolean consumeAnyTicketFrom(Player player, String from) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (!TransitItems.isTicket(it)) continue;
            if (!from.equals(TransitItems.ticketFrom(it))) continue;
            if (it.getAmount() <= 1) player.getInventory().setItem(i, null);
            else it.setAmount(it.getAmount() - 1);
            return true;
        }
        return false;
    }

    private boolean isMatchingTicket(ItemStack item, String from, String to, String cabin) {
        if (!TransitItems.isTicket(item)) return false;
        return from.equals(TransitItems.ticketFrom(item)) && to.equals(TransitItems.ticketTo(item))
                && sameCabin(TransitItems.ticketCabin(item), cabin);
    }

    private void consumeOne(Player player, ItemStack hand) {
        if (hand == null) return;
        if (hand.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else hand.setAmount(hand.getAmount() - 1);
    }

    private String fmt(double v) {
        return plugin.getVaultHook().isEnabled() ? plugin.getVaultHook().format(v) : String.format("%.2f", v);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String locKey(String world, int x, int y, int z) {
        return world + "," + x + "," + y + "," + z;
    }

    private static String sanitize(String id) {
        if (id == null) return "";
        return id.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "");
    }

    private void ensureDefaultTypes() {
        if (!types.containsKey("local")) types.put("local", new LineType("local", "普通", "&b"));
        if (!types.containsKey("express")) types.put("express", new LineType("express", "快线", "&c"));
        if (!types.containsKey("suburban")) types.put("suburban", new LineType("suburban", "市郊", "&6"));
    }

    private LineType resolveType(String type, boolean createIfMissing) {
        ensureDefaultTypes();
        if (type == null || type.isBlank()) return types.get("local");
        String raw = type.trim();
        String id = sanitize(raw);
        if (id.isEmpty()) id = "local";
        if (types.containsKey(id)) return types.get(id);
        if ("快线".equals(raw) || "fast".equalsIgnoreCase(raw)) return types.get("express");
        if ("市郊".equals(raw)) return types.get("suburban");
        if (!createIfMissing) return null;
        LineType t = new LineType(id, raw.length() > 12 ? raw.substring(0, 12) : raw, "&b");
        types.put(id, t);
        save();
        return t;
    }

    public String createType(String id, String color, String name) {
        id = sanitize(id);
        if (id.isEmpty()) return "类型 ID 无效";
        if (types.containsKey(id)) return "类型已存在";
        if (color == null || color.isBlank()) color = "&b";
        if (!color.startsWith("&") && !color.startsWith("§")) color = "&" + color;
        if (name == null || name.isBlank()) name = id;
        if (name.length() > 12) name = name.substring(0, 12);
        types.put(id, new LineType(id, name, color));
        save();
        return null;
    }

    public String renameType(String id, String name) {
        LineType t = types.get(id);
        if (t == null) return "类型不存在";
        if (name == null || name.isBlank()) return "名称不能为空";
        if (name.length() > 12) name = name.substring(0, 12);
        types.put(id, new LineType(t.id(), name, t.color()));
        save();
        return null;
    }

    public String cycleTypeColor(String id) {
        LineType t = types.get(id);
        if (t == null) return "类型不存在";
        String[] pal = com.etherstories.escore.gui.TransitTypeEditGUI.COLORS;
        int i = 0;
        for (int k = 0; k < pal.length; k++) if (pal[k].equals(t.color())) { i = k; break; }
        return setTypeColor(id, pal[(i + 1) % pal.length]);
    }

    public String setTypeColor(String id, String color) {
        LineType t = types.get(id);
        if (t == null) return "类型不存在";
        if (color == null || color.isBlank()) return "颜色无效";
        if (!color.startsWith("&") && !color.startsWith("§")) color = "&" + color;
        types.put(id, new LineType(t.id(), t.displayName(), color));
        for (Line l : new ArrayList<>(lines.values())) {
            if (l.type().equals(id))
                lines.put(l.id(), new Line(l.id(), l.displayName(), l.type(), color, l.hopFare(), l.maxFare()));
        }
        save();
        return null;
    }

    public String deleteType(String id) {
        if (!types.containsKey(id)) return "类型不存在";
        if (types.size() <= 1) return "至少保留一种类型";
        types.remove(id);
        LineType fallback = types.get("local");
        if (fallback == null) fallback = types.values().iterator().next();
        for (Line l : new ArrayList<>(lines.values())) {
            if (l.type().equals(id))
                lines.put(l.id(), new Line(l.id(), l.displayName(), fallback.id(), fallback.color(), l.hopFare(), l.maxFare()));
        }
        save();
        return null;
    }

    public String createCabin(String id, String name, double fareMul) {
        id = sanitize(id);
        if (id.isEmpty()) return "席别 ID 无效";
        if (cabins.containsKey(id)) return "席别已存在";
        if (name == null || name.isBlank()) name = id;
        if (name.length() > 8) name = name.substring(0, 8);
        if (fareMul < 0) fareMul = 0;
        cabins.put(id, new Cabin(id, name, fareMul));
        save();
        return null;
    }

    public String renameCabin(String id, String name) {
        Cabin c = cabins.get(sanitize(id));
        if (c == null) return "席别不存在";
        if (name == null || name.isBlank()) return "名称不能为空";
        if (name.length() > 8) name = name.substring(0, 8);
        cabins.put(c.id(), new Cabin(c.id(), name, c.fareMul()));
        refreshCabinSigns(c.id());
        save();
        return null;
    }

    public String setCabinMul(String id, double fareMul) {
        Cabin c = cabins.get(sanitize(id));
        if (c == null) return "席别不存在";
        if (fareMul < 0) return "倍率不能为负";
        cabins.put(c.id(), new Cabin(c.id(), c.displayName(), fareMul));
        save();
        return null;
    }

    public String deleteCabin(String id) {
        id = sanitize(id);
        if (Cabin.STD.equals(id)) return "不能删除默认席别「普通」";
        Cabin c = cabins.get(id);
        if (c == null) return "席别不存在";
        for (Gate g : gates.values()) {
            if (sameCabin(g.cabin(), id)) return "仍有闸机使用此席别，先改闸或拆掉";
        }
        for (Journey j : journeys.values()) {
            if (sameCabin(j.cabin(), id)) return "仍有在乘行程使用此席别";
        }
        cabins.remove(id);
        save();
        return null;
    }

    public String cabinName(String id) {
        return resolveCabin(id).displayName();
    }

    public double quoteTicketFare(String from, String to, String cabin) {
        PathResult path = shortest(from, to);
        if (!path.reachable()) return -1;
        double fare = path.fare();
        if (fare <= 0) fare = plugin.getConfig().getDouble("transit.default-hop-fare", 2.0);
        return applyCabin(fare, cabin);
    }

    public Cabin resolveCabin(String id) {
        ensureDefaultCabins();
        if (id == null || id.isBlank()) return cabins.get(Cabin.STD);
        Cabin c = cabins.get(sanitize(id));
        return c != null ? c : cabins.get(Cabin.STD);
    }

    private boolean sameCabin(String a, String b) {
        return resolveCabin(a).id().equals(resolveCabin(b).id());
    }

    private double applyCabin(double base, String cabin) {
        return round2(Math.max(0, base) * resolveCabin(cabin).fareMul());
    }

    private void ensureDefaultCabins() {
        if (!cabins.containsKey(Cabin.STD)) cabins.put(Cabin.STD, new Cabin(Cabin.STD, "普通", 1.0));
    }

    private void refreshCabinSigns(String cabinId) {
        for (Gate g : gates.values()) {
            if (!sameCabin(g.cabin(), cabinId)) continue;
            World w = Bukkit.getWorld(g.world());
            if (w == null) continue;
            writeGateSign(w.getBlockAt(g.x(), g.y(), g.z()), g.stationId(), g.mode(), g.cabin());
        }
    }

    public void tickArrive(Player player, Location loc) {
        Journey j = getJourney(player.getUniqueId());
        if (j == null) {
            rideFx.remove(player.getUniqueId());
            return;
        }
        RideFx fx = rideFx.get(player.getUniqueId());
        if (fx == null) {
            fx = new RideFx(j.originId());
            rideFx.put(player.getUniqueId(), fx);
        }
        Station origin = stations.get(fx.originId);
        Station here = stationAt(loc, fx.originId);
        long now = System.currentTimeMillis();
        long dwellTicks = plugin.getConfig().getLong("transit.announce.arrive-dwell-ticks", 10);
        if (dwellTicks >= 40) dwellTicks = 10;
        if (player.getVehicle() != null) dwellTicks = Math.min(dwellTicks, 8);
        long arriveDwell = Math.max(0, dwellTicks) * 50L;
        long departDwell = plugin.getConfig().getLong("transit.announce.depart-dwell-ticks", 240) * 50L;
        boolean inOther = here != null && !here.id().equals(fx.originId);
        boolean inOriginOnly = origin != null && origin.contains(loc) && !inOther;

        if (inOriginOnly) {
            if (fx.originInsideSince == 0) fx.originInsideSince = now;
        } else {
            if (!fx.departed
                    && plugin.getConfig().getBoolean("transit.announce.depart-enabled", true)
                    && fx.originInsideSince > 0
                    && now - fx.originInsideSince >= departDwell) {
                fx.departed = true;
                playDepartSound(player);
            }
            fx.originInsideSince = 0;
            fx.leftOrigin = true;
        }

        if (!plugin.getConfig().getBoolean("transit.announce.enabled", true) || !fx.leftOrigin) {
            fx.pendingArrive = null;
            fx.pendingSince = 0;
            return;
        }
        if (here == null || here.id().equals(fx.originId) || fx.arrived.contains(here.id())) {
            fx.pendingArrive = null;
            fx.pendingSince = 0;
            return;
        }
        if (!here.id().equals(fx.pendingArrive)) {
            fx.pendingArrive = here.id();
            fx.pendingSince = now;
            if (arriveDwell > 0) return;
        }
        if (now - fx.pendingSince < arriveDwell) return;
        fx.arrived.add(here.id());
        fx.announcedId = here.id();
        fx.announcedAt = now;
        fx.pendingArrive = null;
        fx.pendingSince = 0;
        announceArrive(player, here);
    }

    public void announceArrive(Player player, Station s) {
        String zh = s.displayName() == null || s.displayName().isBlank() ? s.id() : s.displayName().trim();
        if (!isAsciiName(zh) && !zh.endsWith("站")) zh = zh + "站";
        String en = s.nameEn() == null ? "" : s.nameEn().trim();
        String label = zh;
        if (!en.isBlank() && !en.equalsIgnoreCase(s.displayName()) && !en.equals(s.id()))
            label = zh + " / " + en;
        if (isSkipStop(s.id())) {
            String text = "&e" + label + " &f通过，不停车";
            if (plugin.getActionBarManager() != null)
                plugin.getActionBarManager().sendTemp(player, text, 80);
            if (plugin.getConfig().getBoolean("transit.announce.chat", true))
                player.sendMessage(ColorUtil.colorize("&8[交通] " + text));
            return;
        }
        String tmpl = plugin.getConfig().getString("transit.announce.message",
                "&b{station} &f到了，请注意下车");
        String text = tmpl.replace("{station}", label);
        if (plugin.getActionBarManager() != null)
            plugin.getActionBarManager().sendTemp(player, text, 120);
        if (plugin.getConfig().getBoolean("transit.announce.chat", true))
            player.sendMessage(ColorUtil.colorize("&8[交通] " + text));
        playArriveSound(player);
    }

    public void playArriveSound(Player player) {
        String preset = plugin.getConfig().getString("transit.announce.preset", "jr");
        TransitChimes.arrive(plugin, player, mapArrivePreset(preset), announceVolume(), stackRepeats());
    }

    public void playDepartSound(Player player) {
        String preset = plugin.getConfig().getString("transit.announce.depart-preset", "jrgo");
        TransitChimes.depart(plugin, player, preset, announceVolume(), stackRepeats());
    }

    public boolean stackRepeats() {
        return plugin.getConfig().getBoolean("transit.announce.stack-repeats", true);
    }

    public void setStackRepeats(boolean on) {
        plugin.getConfig().set("transit.announce.stack-repeats", on);
        plugin.saveConfig();
    }

    private float announceVolume() {
        float vol = (float) plugin.getConfig().getDouble("transit.announce.volume", 1.4);
        return vol < 1.0f ? 1.2f : vol;
    }

    private static String mapArrivePreset(String preset) {
        if (preset == null) return "jr";
        return switch (preset.toLowerCase(Locale.ROOT)) {
            case "metro" -> "jr";
            case "bell" -> "tube";
            case "ding" -> "mtr";
            default -> preset.toLowerCase(Locale.ROOT);
        };
    }

    public String setAnnouncePreset(String preset) {
        Set<String> ok = Set.of("jr", "yamanote", "mtr", "tube", "osaka", "chime",
                "cr", "crh", "beijing", "shanghai", "guangzhou",
                "metro", "bell", "ding");
        if (!ok.contains(preset)) return "未知到站音效";
        plugin.getConfig().set("transit.announce.preset", mapArrivePreset(preset));
        plugin.saveConfig();
        return null;
    }

    public String setDepartPreset(String preset) {
        Set<String> ok = Set.of("jrgo", "doors", "whistle", "shinkansen", "crgo", "crhgo");
        if (!ok.contains(preset)) return "未知发车音效";
        plugin.getConfig().set("transit.announce.depart-preset", preset);
        plugin.saveConfig();
        return null;
    }

    public void setAnnounceEnabled(boolean on) {
        plugin.getConfig().set("transit.announce.enabled", on);
        plugin.saveConfig();
    }

    public void setDepartEnabled(boolean on) {
        plugin.getConfig().set("transit.announce.depart-enabled", on);
        plugin.saveConfig();
    }

    public void pulseGateFlaps(Block gate) {
        int r = plugin.getConfig().getInt("transit.gate-flap-radius", 2);
        int ticks = plugin.getConfig().getInt("transit.gate-flap-open-ticks", 40);
        List<Block> opened = new ArrayList<>();
        World w = gate.getWorld();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block b = w.getBlockAt(gate.getX() + dx, gate.getY() + dy, gate.getZ() + dz);
                    if (!(b.getBlockData() instanceof Openable open)) continue;
                    if (!b.getType().name().contains("TRAPDOOR")) continue;
                    if (open.isOpen()) continue;
                    open.setOpen(true);
                    b.setBlockData(open, false);
                    opened.add(b);
                }
            }
        }
        if (opened.isEmpty()) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Block b : opened) {
                if (!(b.getBlockData() instanceof Openable open)) continue;
                open.setOpen(false);
                b.setBlockData(open, false);
            }
        }, Math.max(10, ticks));
    }

    public String fileClaim(Player player) {
        Journey j = getJourney(player.getUniqueId());
        if (j == null) return "当前没有进行中的行程。如已误扣费用，请联系管理员。";
        for (Claim c : claims) {
            if (c.player().equals(player.getUniqueId()) && "PENDING".equals(c.status()))
                return "您已有一份待审核的行程异常申报";
        }
        String id = Integer.toHexString((int) (System.currentTimeMillis() & 0xfffffff));
        claims.add(new Claim(id, player.getUniqueId(), player.getName(), j.originId(),
                stationName(j.originId()), j.tapInMs(), System.currentTimeMillis(), "PENDING"));
        save();
        notifyAdmins(player.getName() + " 提交行程异常申报（进站 " + stationName(j.originId()) + "），请至交通管理审核");
        return null;
    }

    public Claim getClaim(String id) {
        for (Claim c : claims) if (c.id().equals(id)) return c;
        return null;
    }

    public String resolveClaim(Player admin, String claimId, boolean approve) {
        Claim found = null;
        int idx = -1;
        for (int i = 0; i < claims.size(); i++) {
            if (claims.get(i).id().equals(claimId)) { found = claims.get(i); idx = i; break; }
        }
        if (found == null) return "申请不存在";
        if (!"PENDING".equals(found.status())) return "已经处理过了";
        claims.set(idx, new Claim(found.id(), found.player(), found.playerName(), found.originId(),
                found.originName(), found.tapInMs(), found.createdAt(), approve ? "APPROVED" : "DENIED"));
        if (approve) {
            journeys.remove(found.player());
            rideFx.remove(found.player());
        }
        save();
        Player p = Bukkit.getPlayer(found.player());
        if (p != null && p.isOnline()) {
            p.sendMessage(ColorUtil.colorize(approve
                    ? "&8[交通] &a行程异常申报已核准，未完成行程已撤销，不会补收全程票。"
                    : "&8[交通] &c行程异常申报未获核准。请正常办理出站，或联系管理员。"));
        }
        return null;
    }

    public void notifyAdmins(String msg) {
        String line = ColorUtil.colorize("&c[交通] &f" + msg);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("es2uni.admin") || p.isOp()) p.sendMessage(line);
        }
        plugin.getLogger().info("[Transit] " + msg);
        try { plugin.getAuditLogManager().logRaw("transit", "-", "VIOLATION", msg); } catch (Throwable ignored) {}
    }

    public void onJoin(Player player) {
        Journey j = getJourney(player.getUniqueId());
        if (j == null) return;
        player.sendMessage(ColorUtil.colorize(
                "&8[交通] &e您有未完成行程（进站 " + stationName(j.originId())
                        + " · " + cabinName(j.cabin()) + "）。\n"
                        + "&8[交通] &7请于目的地对应席别出站闸机刷卡结算；如因车辆异常或连接中断未能完成行程，请至交通处办理行程异常申报。"));
    }

    public void cleanupPlayer(UUID uuid) {
        rideFx.remove(uuid);
        boxPos1.remove(uuid);
        boxPos2.remove(uuid);
        pendingTvmBind.remove(uuid);
    }

    private static Station withNames(Station s, String name, String nameEn) {
        return new Station(s.id(), name, nameEn, s.world(), s.minX(), s.minY(), s.minZ(),
                s.maxX(), s.maxY(), s.maxZ(), s.lineIds(), s.hintWorld(), s.hintX(), s.hintY(), s.hintZ());
    }

    private static Station withLines(Station s, List<String> lids) {
        return new Station(s.id(), s.displayName(), s.nameEn(), s.world(), s.minX(), s.minY(), s.minZ(),
                s.maxX(), s.maxY(), s.maxZ(), lids, s.hintWorld(), s.hintX(), s.hintY(), s.hintZ());
    }

    // ── YAML ──────────────────────────────────────────────────────────────────

    public void load() {
        lines.clear(); stations.clear(); edges.clear();
        gates.clear(); tvms.clear(); adjusts.clear(); accounts.clear();
        journeys.clear(); history.clear(); rideStats.clear();
        types.clear(); cabins.clear(); claims.clear();
        skipStops.clear();
        rideFx.clear();
        exitPasses.clear();
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection ls = cfg.getConfigurationSection("lines");
        if (ls != null) {
            for (String id : ls.getKeys(false)) {
                String p = "lines." + id + ".";
                lines.put(id, new Line(id,
                        cfg.getString(p + "name", id),
                        cfg.getString(p + "type", "local"),
                        cfg.getString(p + "color", "&b"),
                        cfg.getDouble(p + "hop-fare", 2),
                        cfg.getDouble(p + "max-fare", 18)));
            }
        }
        ConfigurationSection tps = cfg.getConfigurationSection("types");
        if (tps != null) {
            for (String id : tps.getKeys(false)) {
                types.put(id, new LineType(id,
                        cfg.getString("types." + id + ".name", id),
                        cfg.getString("types." + id + ".color", "&b")));
            }
        }
        ConfigurationSection cbs = cfg.getConfigurationSection("cabins");
        if (cbs != null) {
            for (String id : cbs.getKeys(false)) {
                cabins.put(id, new Cabin(id,
                        cfg.getString("cabins." + id + ".name", id),
                        cfg.getDouble("cabins." + id + ".mul", 1.0)));
            }
        }
        ensureDefaultCabins();
        ConfigurationSection ss = cfg.getConfigurationSection("stations");
        if (ss != null) {
            for (String id : ss.getKeys(false)) {
                String p = "stations." + id + ".";
                List<Integer> min = cfg.getIntegerList(p + "min");
                List<Integer> max = cfg.getIntegerList(p + "max");
                if (min.size() < 3 || max.size() < 3) continue;
                List<String> lids = cfg.getStringList(p + "lines");
                String hw = cfg.getString(p + "create.world");
                Double hx = cfg.contains(p + "create.x") ? cfg.getDouble(p + "create.x") : null;
                Double hy = cfg.contains(p + "create.y") ? cfg.getDouble(p + "create.y") : null;
                Double hz = cfg.contains(p + "create.z") ? cfg.getDouble(p + "create.z") : null;
                stations.put(id, new Station(id, cfg.getString(p + "name", id),
                        cfg.getString(p + "name-en", id),
                        cfg.getString(p + "world", "world"),
                        min.get(0), min.get(1), min.get(2),
                        max.get(0), max.get(1), max.get(2),
                        lids, hw, hx, hy, hz));
                if (cfg.getBoolean(p + "skip", false)) skipStops.add(id);
            }
        }
        List<Map<?, ?>> el = cfg.getMapList("edges");
        for (Map<?, ?> m : el) {
            String from = String.valueOf(m.get("from"));
            String to = String.valueOf(m.get("to"));
            String line = String.valueOf(m.get("line"));
            double fare = m.get("fare") instanceof Number n ? n.doubleValue() : -1;
            boolean bi = m.get("bi") instanceof Boolean b ? b : true;
            edges.add(new Edge(from, to, line, fare, bi));
        }
        ConfigurationSection gs = cfg.getConfigurationSection("gates");
        if (gs != null) {
            for (String k : gs.getKeys(false)) {
                String[] p = k.split(",");
                if (p.length != 4) continue;
                try {
                    Gate g = new Gate(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                            cfg.getString("gates." + k + ".station"),
                            cfg.getString("gates." + k + ".mode", "BOTH"),
                            cfg.getString("gates." + k + ".cabin", Cabin.STD));
                    gates.put(g.key(), g);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        ConfigurationSection ts = cfg.getConfigurationSection("tvms");
        if (ts != null) {
            for (String k : ts.getKeys(false)) {
                String[] p = k.split(",");
                if (p.length != 4) continue;
                try {
                    Tvm t = new Tvm(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                            cfg.getString("tvms." + k + ".station"));
                    tvms.put(t.key(), t);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        ConfigurationSection ads = cfg.getConfigurationSection("adjusts");
        if (ads != null) {
            for (String k : ads.getKeys(false)) {
                String[] p = k.split(",");
                if (p.length != 4) continue;
                try {
                    AdjustBooth t = new AdjustBooth(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                            cfg.getString("adjusts." + k + ".station"));
                    adjusts.put(t.key(), t);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        ConfigurationSection as = cfg.getConfigurationSection("accounts");
        if (as != null) {
            for (String k : as.getKeys(false)) {
                try {
                    UUID u = UUID.fromString(k);
                    boolean tap = cfg.getBoolean("accounts." + k + ".tap-pay", false);
                    String cid = cfg.getString("accounts." + k + ".card");
                    UUID card = cid == null || cid.isBlank() ? null : UUID.fromString(cid);
                    accounts.put(u, new Account(tap, card));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        ConfigurationSection js = cfg.getConfigurationSection("journeys");
        if (js != null) {
            for (String k : js.getKeys(false)) {
                try {
                    UUID u = UUID.fromString(k);
                    journeys.put(u, new Journey(
                            cfg.getString("journeys." + k + ".origin"),
                            cfg.getLong("journeys." + k + ".at"),
                            cfg.getString("journeys." + k + ".method", "CARD"),
                            cfg.getString("journeys." + k + ".dest"),
                            cfg.getString("journeys." + k + ".cabin", Cabin.STD)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        ConfigurationSection eps = cfg.getConfigurationSection("exit-passes");
        if (eps != null) {
            long now = System.currentTimeMillis();
            for (String k : eps.getKeys(false)) {
                try {
                    UUID u = UUID.fromString(k);
                    long until = cfg.getLong("exit-passes." + k + ".until", 0);
                    String st = cfg.getString("exit-passes." + k + ".station");
                    if (st == null || until < now) continue;
                    exitPasses.put(u, new ExitPass(st, until,
                            cfg.getString("exit-passes." + k + ".cabin", Cabin.STD)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        ConfigurationSection hs = cfg.getConfigurationSection("history");
        if (hs != null) {
            for (String k : hs.getKeys(false)) {
                try {
                    UUID u = UUID.fromString(k);
                    List<Ride> rides = new ArrayList<>();
                    List<Map<?, ?>> maps = cfg.getMapList("history." + k);
                    for (Map<?, ?> m : maps) {
                        rides.add(new Ride(
                                String.valueOf(m.get("from")),
                                String.valueOf(m.get("to")),
                                m.get("fare") instanceof Number n ? n.doubleValue() : 0,
                                m.get("at") instanceof Number n ? n.longValue() : 0,
                                String.valueOf(m.get("note") == null ? "normal" : m.get("note"))));
                    }
                    history.put(u, rides);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        ConfigurationSection rs = cfg.getConfigurationSection("ride-stats");
        if (rs != null) {
            for (String k : rs.getKeys(false)) {
                try {
                    UUID u = UUID.fromString(k);
                    rideStats.put(u, new RideStat(u,
                            cfg.getString("ride-stats." + k + ".name", nameOf(u)),
                            cfg.getInt("ride-stats." + k + ".rides", 0),
                            cfg.getDouble("ride-stats." + k + ".fare", 0)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (rideStats.isEmpty()) {
            for (var e : history.entrySet()) {
                int n = 0;
                double f = 0;
                for (Ride r : e.getValue()) {
                    if (!countsForBoard(r.note(), r.fare())) continue;
                    n++;
                    f += Math.max(0, r.fare());
                }
                if (n > 0) rideStats.put(e.getKey(), new RideStat(e.getKey(), nameOf(e.getKey()), n, f));
            }
        }
        List<Map<?, ?>> cl = cfg.getMapList("claims");
        for (Map<?, ?> m : cl) {
            try {
                claims.add(new Claim(
                        String.valueOf(m.get("id")),
                        UUID.fromString(String.valueOf(m.get("player"))),
                        String.valueOf(m.get("name")),
                        String.valueOf(m.get("origin")),
                        String.valueOf(m.get("originName") == null ? m.get("origin") : m.get("originName")),
                        m.get("tapIn") instanceof Number n ? n.longValue() : 0,
                        m.get("at") instanceof Number n ? n.longValue() : 0,
                        String.valueOf(m.get("status") == null ? "PENDING" : m.get("status"))));
            } catch (Exception ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Line l : lines.values()) {
            String p = "lines." + l.id() + ".";
            cfg.set(p + "name", l.displayName());
            cfg.set(p + "type", l.type());
            cfg.set(p + "color", l.color());
            cfg.set(p + "hop-fare", l.hopFare());
            cfg.set(p + "max-fare", l.maxFare());
        }
        for (LineType t : types.values()) {
            cfg.set("types." + t.id() + ".name", t.displayName());
            cfg.set("types." + t.id() + ".color", t.color());
        }
        for (Cabin c : cabins.values()) {
            cfg.set("cabins." + c.id() + ".name", c.displayName());
            cfg.set("cabins." + c.id() + ".mul", c.fareMul());
        }
        for (Station s : stations.values()) {
            String p = "stations." + s.id() + ".";
            cfg.set(p + "name", s.displayName());
            cfg.set(p + "name-en", s.nameEn());
            cfg.set(p + "world", s.world());
            cfg.set(p + "min", List.of(s.minX(), s.minY(), s.minZ()));
            cfg.set(p + "max", List.of(s.maxX(), s.maxY(), s.maxZ()));
            cfg.set(p + "lines", s.lineIds());
            if (skipStops.contains(s.id())) cfg.set(p + "skip", true);
            if (s.hintWorld() != null) {
                cfg.set(p + "create.world", s.hintWorld());
                cfg.set(p + "create.x", s.hintX());
                cfg.set(p + "create.y", s.hintY());
                cfg.set(p + "create.z", s.hintZ());
            }
        }
        List<Map<String, Object>> el = new ArrayList<>();
        for (Edge e : edges) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("from", e.from());
            m.put("to", e.to());
            m.put("line", e.lineId());
            m.put("fare", e.fare());
            m.put("bi", e.bidirectional());
            el.add(m);
        }
        cfg.set("edges", el);
        for (Gate g : gates.values()) {
            cfg.set("gates." + g.key() + ".station", g.stationId());
            cfg.set("gates." + g.key() + ".mode", g.mode());
            cfg.set("gates." + g.key() + ".cabin", g.cabin());
        }
        for (Tvm t : tvms.values()) {
            cfg.set("tvms." + t.key() + ".station", t.stationId());
        }
        for (AdjustBooth t : adjusts.values()) {
            cfg.set("adjusts." + t.key() + ".station", t.stationId());
        }
        for (var e : accounts.entrySet()) {
            cfg.set("accounts." + e.getKey() + ".tap-pay", e.getValue().tapPay());
            if (e.getValue().cardId() != null)
                cfg.set("accounts." + e.getKey() + ".card", e.getValue().cardId().toString());
        }
        for (var e : journeys.entrySet()) {
            cfg.set("journeys." + e.getKey() + ".origin", e.getValue().originId());
            cfg.set("journeys." + e.getKey() + ".at", e.getValue().tapInMs());
            cfg.set("journeys." + e.getKey() + ".method", e.getValue().method());
            if (e.getValue().ticketDest() != null)
                cfg.set("journeys." + e.getKey() + ".dest", e.getValue().ticketDest());
            cfg.set("journeys." + e.getKey() + ".cabin", e.getValue().cabin());
        }
        pruneExpiredPasses();
        for (var e : exitPasses.entrySet()) {
            cfg.set("exit-passes." + e.getKey() + ".station", e.getValue().stationId());
            cfg.set("exit-passes." + e.getKey() + ".until", e.getValue().untilMs());
            cfg.set("exit-passes." + e.getKey() + ".cabin", e.getValue().cabin());
        }
        for (var e : history.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Ride r : e.getValue()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("from", r.fromId());
                m.put("to", r.toId());
                m.put("fare", r.fare());
                m.put("at", r.at());
                m.put("note", r.note());
                list.add(m);
            }
            cfg.set("history." + e.getKey(), list);
        }
        List<Map<String, Object>> cl = new ArrayList<>();
        for (Claim c : claims) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.id());
            m.put("player", c.player().toString());
            m.put("name", c.playerName());
            m.put("origin", c.originId());
            m.put("originName", c.originName());
            m.put("tapIn", c.tapInMs());
            m.put("at", c.createdAt());
            m.put("status", c.status());
            cl.add(m);
        }
        cfg.set("claims", cl);
        for (RideStat st : rideStats.values()) {
            cfg.set("ride-stats." + st.uuid() + ".name", st.name());
            cfg.set("ride-stats." + st.uuid() + ".rides", st.rides());
            cfg.set("ride-stats." + st.uuid() + ".fare", st.fare());
        }
        try {
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("无法保存 transit.yml: " + ex.getMessage());
        }
    }
}
