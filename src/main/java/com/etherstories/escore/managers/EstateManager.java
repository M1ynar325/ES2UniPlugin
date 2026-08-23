package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.estate.BuildingCategory;
import com.etherstories.escore.estate.EstateUnit;
import com.etherstories.escore.estate.UnitKind;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class EstateManager {

    public record Selection(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        public boolean complete() { return world != null; }
        public EstateUnit asProbe() {
            return new EstateUnit("probe", "", 0, "", UnitKind.OTHER, BuildingCategory.PUBLIC, false,
                    world, x1, y1, z1, x2, y2, z2, null, null, null, null, null,
                    null, null, null,
                    null, null, 0, false, null);
        }
    }

    public record PendingBuy(UUID buyer, String unitId, long expireMs) {}

    private static final Pattern ROOM = Pattern.compile("[\\p{L}\\p{N}\\-_．·]{1,16}");

    public record ParsedAddress(String building, int floor, String room) {}

    public record ParsedRegister(String building, int floor, String room,
                                 UnitKind kind, BuildingCategory cat, Double price) {}

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private final List<EstateUnit> units = new ArrayList<>();
    private final Map<UUID, int[]> pos1 = new HashMap<>();
    private final Map<UUID, String> pos1World = new HashMap<>();
    private final Map<UUID, int[]> pos2 = new HashMap<>();
    private final Map<UUID, PendingBuy> pendingBuy = new HashMap<>();

    public EstateManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "estates.yml");
        load();
    }

    public void setPos1(Player p) {
        Location loc = p.getLocation();
        pos1.put(p.getUniqueId(), new int[]{loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()});
        pos1World.put(p.getUniqueId(), loc.getWorld().getName());
        pos2.remove(p.getUniqueId());
    }

    public String setPos2(Player p) {
        String w = pos1World.get(p.getUniqueId());
        if (w == null) return "请先 /ecos estate pos1";
        if (!w.equals(p.getWorld().getName())) return "点和 pos1 不在同一个世界";
        Location loc = p.getLocation();
        pos2.put(p.getUniqueId(), new int[]{loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()});
        return null;
    }

    public Selection selectionOf(UUID uuid) {
        int[] a = pos1.get(uuid);
        int[] b = pos2.get(uuid);
        String w = pos1World.get(uuid);
        if (a == null || b == null || w == null) return null;
        return new Selection(w, a[0], a[1], a[2], b[0], b[1], b[2]);
    }

    public boolean hasSelection(UUID uuid) {
        return selectionOf(uuid) != null;
    }

    public boolean hasPos1(UUID uuid) {
        return pos1.containsKey(uuid);
    }

    /** 玩家自建住宅/公寓收注册费；管理预制和商铺/公共免费。 */
    public double registerFee(boolean admin, BuildingCategory category, UnitKind kind) {
        if (admin) return 0;
        boolean home = category == BuildingCategory.RESIDENTIAL
                || kind == UnitKind.HOUSE || kind == UnitKind.APARTMENT || kind == UnitKind.HOTEL;
        if (!home) return 0;
        return Math.max(0, plugin.getConfig().getDouble("estate.register-fee", 2048));
    }

    public String register(Player p, String building, int floor, String room,
                           UnitKind kind, BuildingCategory category, boolean admin, double price) {
        Selection sel = selectionOf(p.getUniqueId());
        if (sel == null) return "请先 /ecos estate pos1 和 pos2 圈出房间";
        String bName = building.trim();
        if (bName.isEmpty() || bName.length() > 32) return "楼名 1–32 字（可含空格）";
        String roomId = room.trim();
        if (!ROOM.matcher(roomId).matches()) return "房号 1–16 字（中文/字母/数字/短横）";
        if (kind == null) return "未知用途";
        if (category == null) category = kind.defaultCategory();
        EstateUnit probe = new EstateUnit(UUID.randomUUID().toString(), bName, floor, roomId, kind, category,
                admin, sel.world(), sel.x1(), sel.y1(), sel.z1(), sel.x2(), sel.y2(), sel.z2(),
                null, null, null, null, null,
                null, null, null,
                admin ? null : p.getUniqueId(), admin ? null : p.getName(),
                Math.max(0, price), admin && price > 0, p.getUniqueId());
        if (probe.volume() < 8) return "空间太小（至少 8 格）";
        int maxVol = plugin.getConfig().getInt("estate.max-volume", 32768);
        if (!admin && probe.volume() > maxVol) return "超过体积上限 " + maxVol + " 格";
        if (find(bName, floor, roomId) != null) return "这个门牌已经有了: " + probe.address();
        EstateUnit hit = overlap(probe, null);
        if (hit != null) return "与「" + hit.address() + "」空间重叠";
        double fee = registerFee(admin, category, kind);
        if (fee > 0) {
            if (!plugin.getVaultHook().isEnabled()) return "经济系统不可用，无法缴纳住宅注册费";
            if (plugin.getVaultHook().getBalance(p) < fee) {
                return "余额不足，住宅注册费 " + money(fee);
            }
            String pay = plugin.getVaultHook().withdraw(p, fee);
            if (pay != null) return "扣款失败: " + pay;
            depositSink(fee);
        }
        units.add(probe);
        save();
        pos1.remove(p.getUniqueId());
        pos2.remove(p.getUniqueId());
        pos1World.remove(p.getUniqueId());
        return null;
    }

    public EstateUnit find(String building, int floor, String room) {
        for (EstateUnit u : units) {
            if (u.building().equalsIgnoreCase(building) && u.floor() == floor
                    && u.room().equalsIgnoreCase(room)) return u;
        }
        return null;
    }

    public EstateUnit byId(String id) {
        for (EstateUnit u : units) if (u.id().equals(id)) return u;
        return null;
    }

    public EstateUnit at(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        String w = loc.getWorld().getName();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        for (EstateUnit u : units) {
            if (u.contains(w, x, y, z)) return u;
        }
        return null;
    }

    public EstateUnit overlap(EstateUnit probe, String exceptId) {
        for (EstateUnit u : units) {
            if (exceptId != null && u.id().equals(exceptId)) continue;
            if (probe.overlaps(u)) return u;
        }
        return null;
    }

    public List<EstateUnit> all() {
        return List.copyOf(units);
    }

    public List<String> buildings(BuildingCategory cat) {
        List<String> names = new ArrayList<>();
        for (EstateUnit u : units) {
            if (cat != null && u.category() != cat) continue;
            boolean seen = false;
            for (String n : names) if (n.equalsIgnoreCase(u.building())) { seen = true; break; }
            if (!seen) names.add(u.building());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public List<EstateUnit> listedForSale() {
        List<EstateUnit> list = new ArrayList<>();
        for (EstateUnit u : units) {
            if (u.listed() && u.price() > 0) list.add(u);
        }
        list.sort(Comparator.comparing(EstateUnit::building, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(EstateUnit::floor)
                .thenComparing(EstateUnit::room));
        return list;
    }

    public List<EstateUnit> inBuilding(String building) {
        List<EstateUnit> list = new ArrayList<>();
        for (EstateUnit u : units) {
            if (u.building().equalsIgnoreCase(building)) list.add(u);
        }
        list.sort(Comparator.comparingInt(EstateUnit::floor).thenComparing(EstateUnit::room));
        return list;
    }

    public List<EstateUnit> ownedBy(UUID uuid) {
        List<EstateUnit> list = new ArrayList<>();
        for (EstateUnit u : units) {
            if (uuid.equals(u.owner())) list.add(u);
        }
        list.sort(Comparator.comparing(EstateUnit::building).thenComparingInt(EstateUnit::floor)
                .thenComparing(EstateUnit::room));
        return list;
    }

    public BuildingCategory categoryOf(String building) {
        BuildingCategory first = null;
        for (EstateUnit u : units) {
            if (!u.building().equalsIgnoreCase(building)) continue;
            if (first == null) first = u.category();
            else if (first != u.category()) return first;
        }
        return first == null ? BuildingCategory.RESIDENTIAL : first;
    }

    public boolean mixedCategory(String building) {
        BuildingCategory first = null;
        for (EstateUnit u : units) {
            if (!u.building().equalsIgnoreCase(building)) continue;
            if (first == null) first = u.category();
            else if (first != u.category()) return true;
        }
        return false;
    }

    public static ParsedAddress parseAddress(String[] tokens) {
        if (tokens == null || tokens.length < 3) return null;
        try {
            int floor = Integer.parseInt(tokens[tokens.length - 2]);
            String room = tokens[tokens.length - 1].trim();
            String building = String.join(" ", Arrays.copyOfRange(tokens, 0, tokens.length - 2)).trim();
            if (building.isEmpty()) return null;
            return new ParsedAddress(building, floor, room);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static ParsedRegister parseRegister(String[] tokens, boolean allowPrice) {
        if (tokens == null || tokens.length < 4) return null;
        int end = tokens.length - 1;
        Double price = null;
        if (allowPrice) {
            try {
                price = Double.parseDouble(tokens[end]);
                end--;
            } catch (NumberFormatException ignored) {}
        }
        if (end < 3) return null;
        BuildingCategory cat = null;
        UnitKind kind = UnitKind.fromKey(tokens[end]);
        if (kind == null) {
            cat = BuildingCategory.fromKey(tokens[end]);
            if (cat == null || end < 4) return null;
            end--;
            kind = UnitKind.fromKey(tokens[end]);
            if (kind == null) return null;
        }
        ParsedAddress addr = parseAddress(Arrays.copyOfRange(tokens, 0, end));
        if (addr == null) return null;
        if (cat == null) cat = kind.defaultCategory();
        return new ParsedRegister(addr.building(), addr.floor(), addr.room(), kind, cat, price);
    }

    public int vacantCount(String building) {
        int n = 0;
        for (EstateUnit u : inBuilding(building)) if (u.vacant()) n++;
        return n;
    }

    public String setDoor(Player p, EstateUnit unit) {
        if (unit == null) return "先站在房间里，或指定门牌";
        if (!p.hasPermission("es2uni.admin") && !p.getUniqueId().equals(unit.owner())
                && !p.getUniqueId().equals(unit.createdBy())) {
            return "这不是你的房间";
        }
        Location loc = p.getLocation();
        if (!unit.contains(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
            return "请站在该房间里面再设门口";
        }
        replace(unit.withDoor(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getYaw(), loc.getPitch()));
        return null;
    }

    public String setSale(Player p, EstateUnit unit, double price, boolean listed) {
        if (unit == null) return "房间不存在";
        boolean admin = p.hasPermission("es2uni.admin");
        if (!admin && !p.getUniqueId().equals(unit.owner())) return "只有房主能上架";
        if (listed && price <= 0) return "上架需要价格大于 0";
        if (listed && plugin.getHotelManager() != null) {
            var room = plugin.getHotelManager().roomOf(unit);
            if (room != null && !room.vacant()) return "酒店还有住客，先退房再上架";
        }
        replace(unit.withSale(Math.max(0, price), listed));
        return null;
    }

    public String delete(Player p, EstateUnit unit) {
        if (unit == null) return "房间不存在";
        boolean admin = p.hasPermission("es2uni.admin");
        if (!admin && !p.getUniqueId().equals(unit.owner()) && !p.getUniqueId().equals(unit.createdBy())) {
            return "没有权限拆这间";
        }
        units.removeIf(u -> u.id().equals(unit.id()));
        if (plugin.getHotelManager() != null) plugin.getHotelManager().detach(unit.id());
        save();
        return null;
    }

    public String beginBuy(Player p, EstateUnit unit) {
        if (unit == null) return "房间不存在";
        if (!unit.listed() || unit.price() <= 0) return "这间没有挂牌出售";
        if (p.getUniqueId().equals(unit.owner())) return "这已经是你的";
        if (!plugin.getVaultHook().isEnabled()) return "经济系统不可用";
        if (plugin.getVaultHook().getBalance(p) < unit.price()) return "余额不足";
        pendingBuy.put(p.getUniqueId(), new PendingBuy(p.getUniqueId(), unit.id(), System.currentTimeMillis() + 15_000));
        return null;
    }

    public boolean hasPending(UUID uuid, String unitId) {
        PendingBuy pend = pendingBuy.get(uuid);
        return pend != null && pend.unitId().equals(unitId)
                && System.currentTimeMillis() <= pend.expireMs();
    }

    public String confirmBuy(Player p) {
        PendingBuy pend = pendingBuy.remove(p.getUniqueId());
        if (pend == null || System.currentTimeMillis() > pend.expireMs()) return "没有待确认的购买，或已过期";
        EstateUnit unit = byId(pend.unitId());
        if (unit == null || !unit.listed() || unit.price() <= 0) return "挂牌已取消";
        if (!plugin.getVaultHook().isEnabled()) return "经济系统不可用";
        String err = plugin.getVaultHook().withdraw(p, unit.price());
        if (err != null) return "扣款失败: " + err;
        UUID prev = unit.owner();
        double paid = unit.price();
        if (prev != null && !prev.equals(p.getUniqueId())) {
            plugin.getVaultHook().deposit(Bukkit.getOfflinePlayer(prev), paid);
            Player seller = Bukkit.getPlayer(prev);
            if (seller != null) {
                seller.sendMessage(ColorUtil.colorize("&8[房产] &f" + p.getName()
                        + " &7买下了 &f" + unit.address() + " &e" + plugin.getVaultHook().format(paid)));
            }
        } else if (prev == null) {
            depositSink(paid);
        }
        replace(unit.withOwner(p.getUniqueId(), p.getName()).withSale(0, false));
        if (plugin.getHotelManager() != null) plugin.getHotelManager().detach(unit.id());
        return null;
    }

    public String visit(Player p, EstateUnit unit) {
        if (unit == null) return "房间不存在";
        boolean ok = p.hasPermission("es2uni.admin") || p.getUniqueId().equals(unit.owner())
                || (unit.listed() && unit.price() > 0);
        if (!ok) return "这间没有挂牌，不能看房";
        if (!teleport(p, unit)) return "无法传送";
        return null;
    }

    public void sendHere(Player p, EstateUnit unit) {
        if (unit == null) {
            p.sendMessage(ColorUtil.colorize("&8[房产] &7这里不是登记房产"));
            return;
        }
        boolean mine = p.getUniqueId().equals(unit.owner());
        p.sendMessage(ColorUtil.colorize("&8[房产] &f" + unit.address()));
        p.sendMessage(ColorUtil.colorize(" &7" + unit.kind().label + " · " + unit.category().label
                + (mine ? " &a(你的)" : "")));
        if (unit.vacant()) {
            p.sendMessage(ColorUtil.colorize(unit.listed() && unit.price() > 0
                    ? " &a空闲  &e待售 " + money(unit.price())
                    : " &a空闲  &8未挂牌"));
        } else {
            p.sendMessage(ColorUtil.colorize(" &7房主 &f" + (unit.ownerName() == null ? "?" : unit.ownerName())
                    + (unit.listed() && unit.price() > 0 ? "  &e挂牌 " + money(unit.price()) : "  &8未挂牌")));
        }
        p.sendMessage(ColorUtil.colorize(unit.hasDoor() ? " &7门口已设" : " &8门口未设"));
        if (!mine && unit.listed() && unit.price() > 0) {
            p.sendMessage(ColorUtil.colorize("&8看房/购买: 终端买房页左键看房，右键购买"));
        }
    }

    private String money(double v) {
        if (plugin.getVaultHook().isEnabled()) return plugin.getVaultHook().format(v);
        return String.format("%.0f", v);
    }

    private void depositSink(double amount) {
        if (amount <= 0 || plugin.getTaxManager() == null) return;
        String sink = plugin.getTaxManager().getSinkAccount();
        if (sink == null || sink.isBlank()) return;
        Player online = Bukkit.getPlayerExact(sink);
        if (online != null) plugin.getVaultHook().deposit(online, amount);
        else plugin.getVaultHook().deposit(Bukkit.getOfflinePlayer(sink), amount);
    }

    public boolean teleport(Player p, EstateUnit unit) {
        if (unit == null) return false;
        Location loc = unit.teleportLoc();
        if (loc == null) {
            p.sendMessage(ColorUtil.colorize("&8[房产] &7世界不在线"));
            return false;
        }
        p.teleport(loc);
        if (!unit.hasDoor()) {
            p.sendMessage(ColorUtil.colorize("&8[房产] &7还没设门口，已传到房间内。站门口 &f/ecos estate door"));
        }
        return true;
    }

    public void placeSign(Player p, EstateUnit unit) {
        Block look = p.getTargetBlockExact(5);
        if (look == null || look.getType().isAir()) {
            p.sendMessage(ColorUtil.colorize("&8[房产] &7请看着一面墙再放牌子"));
            return;
        }
        BlockFace face = faceFrom(p, look);
        Block dest;
        if (isSignBlock(look)) {
            dest = look;
        } else {
            Block front = look.getRelative(face);
            if (isSignBlock(front)) {
                dest = front;
            } else if (front.getType().isAir()) {
                dest = front;
                dest.setType(Material.OAK_WALL_SIGN, false);
                if (dest.getBlockData() instanceof Directional dir) {
                    dir.setFacing(face);
                    dest.setBlockData(dir, false);
                }
            } else {
                p.sendMessage(ColorUtil.colorize("&8[房产] &7那面没有空位放牌子"));
                return;
            }
        }
        if (!unit.near(dest.getWorld().getName(), dest.getX(), dest.getY(), dest.getZ())) {
            p.sendMessage(ColorUtil.colorize("&8[房产] &7牌子要贴在这间房的墙上"));
            return;
        }
        if (!(dest.getState() instanceof Sign sign)) {
            p.sendMessage(ColorUtil.colorize("&8[房产] &7放牌子失败"));
            return;
        }
        writeSign(sign, unit);
        replace(unit.withSign(dest.getX(), dest.getY(), dest.getZ()));
        p.sendMessage(ColorUtil.colorize("&8[房产] &7已写 &f" + signHeader(unit.kind())));
    }

    private void rewriteSign(EstateUnit unit) {
        if (!unit.hasSign()) return;
        org.bukkit.World w = Bukkit.getWorld(unit.world());
        if (w == null) return;
        Block b = w.getBlockAt(unit.signX(), unit.signY(), unit.signZ());
        if (b.getState() instanceof Sign sign) writeSign(sign, unit);
    }

    private static void writeSign(Sign sign, EstateUnit unit) {
        sign.setLine(0, ColorUtil.colorize("&8" + signHeader(unit.kind())));
        sign.setLine(1, ColorUtil.colorize("&f" + clipWidth(unit.building(), 16)));
        sign.setLine(2, ColorUtil.colorize("&7" + unit.floor() + "-" + unit.room()));
        String ownerLine;
        if (unit.vacant()) ownerLine = unit.listed() && unit.price() > 0 ? "待售" : "空闲";
        else ownerLine = unit.ownerName() == null ? "?" : unit.ownerName();
        sign.setLine(3, ColorUtil.colorize("&8" + clipWidth(ownerLine, 16)));
        sign.update();
    }

    static String signHeader(UnitKind kind) {
        String full = "ES2注册单位-" + kind.label;
        if (signWidth(full) <= 16) return full;
        return clipWidth("ES2单位-" + kind.label, 16);
    }

    private static boolean isSignBlock(Block b) {
        return b.getState() instanceof Sign;
    }

    private static BlockFace faceFrom(Player p, Block look) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        Location eye = p.getEyeLocation();
        BlockFace best = BlockFace.NORTH;
        double bestDot = -2;
        for (BlockFace f : faces) {
            org.bukkit.util.Vector n = new org.bukkit.util.Vector(f.getModX(), 0, f.getModZ());
            org.bukkit.util.Vector to = look.getLocation().add(0.5, 0.5, 0.5).toVector()
                    .subtract(eye.toVector()).setY(0);
            if (to.lengthSquared() < 0.01) continue;
            double dot = to.normalize().dot(n);
            if (dot > bestDot) {
                bestDot = dot;
                best = f;
            }
        }
        return best.getOppositeFace();
    }

    private static int signWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += s.charAt(i) > 0x7F ? 2 : 1;
        return w;
    }

    private static String clipWidth(String s, int max) {
        if (s == null) return "";
        if (signWidth(s) <= max) return s;
        StringBuilder sb = new StringBuilder();
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            int cw = s.charAt(i) > 0x7F ? 2 : 1;
            if (w + cw > max) break;
            sb.append(s.charAt(i));
            w += cw;
        }
        return sb.toString();
    }

    private void replace(EstateUnit next) {
        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).id().equals(next.id())) {
                units.set(i, next);
                save();
                rewriteSign(next);
                return;
            }
        }
    }

    public void load() {
        units.clear();
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("units");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection u = sec.getConfigurationSection(id);
            if (u == null) continue;
            try {
                UnitKind kind = UnitKind.fromKey(u.getString("kind", "house"));
                if (kind == null) kind = UnitKind.HOUSE;
                BuildingCategory cat = BuildingCategory.fromKey(u.getString("category", kind.defaultCategory().key));
                if (cat == null) cat = kind.defaultCategory();
                String ownerStr = u.getString("owner", "");
                UUID owner = ownerStr == null || ownerStr.isBlank() ? null : UUID.fromString(ownerStr);
                String created = u.getString("created-by", "");
                UUID createdBy = created == null || created.isBlank() ? null : UUID.fromString(created);
                Integer dx = u.contains("door.x") ? u.getInt("door.x") : null;
                Integer dy = u.contains("door.y") ? u.getInt("door.y") : null;
                Integer dz = u.contains("door.z") ? u.getInt("door.z") : null;
                Float yaw = u.contains("door.yaw") ? (float) u.getDouble("door.yaw") : null;
                Float pitch = u.contains("door.pitch") ? (float) u.getDouble("door.pitch") : null;
                Integer sx = u.contains("sign.x") ? u.getInt("sign.x") : null;
                Integer sy = u.contains("sign.y") ? u.getInt("sign.y") : null;
                Integer sz = u.contains("sign.z") ? u.getInt("sign.z") : null;
                units.add(new EstateUnit(
                        id,
                        u.getString("building", "未命名"),
                        u.getInt("floor", 1),
                        u.getString("room", "1"),
                        kind, cat,
                        u.getBoolean("admin", false),
                        u.getString("world", "world"),
                        u.getInt("x1"), u.getInt("y1"), u.getInt("z1"),
                        u.getInt("x2"), u.getInt("y2"), u.getInt("z2"),
                        dx, dy, dz, yaw, pitch,
                        sx, sy, sz,
                        owner,
                        u.getString("owner-name", owner == null ? null : "未知"),
                        u.getDouble("price", 0),
                        u.getBoolean("listed", false),
                        createdBy
                ));
            } catch (Exception e) {
                plugin.getLogger().warning("跳过损坏房产 " + id + ": " + e.getMessage());
            }
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (EstateUnit u : units) {
            String p = "units." + u.id() + ".";
            cfg.set(p + "building", u.building());
            cfg.set(p + "floor", u.floor());
            cfg.set(p + "room", u.room());
            cfg.set(p + "kind", u.kind().key);
            cfg.set(p + "category", u.category().key);
            cfg.set(p + "admin", u.adminSource());
            cfg.set(p + "world", u.world());
            cfg.set(p + "x1", u.x1());
            cfg.set(p + "y1", u.y1());
            cfg.set(p + "z1", u.z1());
            cfg.set(p + "x2", u.x2());
            cfg.set(p + "y2", u.y2());
            cfg.set(p + "z2", u.z2());
            if (u.hasDoor()) {
                cfg.set(p + "door.x", u.doorX());
                cfg.set(p + "door.y", u.doorY());
                cfg.set(p + "door.z", u.doorZ());
                cfg.set(p + "door.yaw", u.doorYaw());
                cfg.set(p + "door.pitch", u.doorPitch());
            }
            if (u.hasSign()) {
                cfg.set(p + "sign.x", u.signX());
                cfg.set(p + "sign.y", u.signY());
                cfg.set(p + "sign.z", u.signZ());
            }
            cfg.set(p + "owner", u.owner() == null ? "" : u.owner().toString());
            cfg.set(p + "owner-name", u.ownerName());
            cfg.set(p + "price", u.price());
            cfg.set(p + "listed", u.listed());
            cfg.set(p + "created-by", u.createdBy() == null ? "" : u.createdBy().toString());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("estates.yml 保存失败: " + e.getMessage());
        }
    }

    public static String kindsHint() {
        StringBuilder sb = new StringBuilder();
        for (UnitKind k : UnitKind.values()) {
            if (!sb.isEmpty()) sb.append(" / ");
            sb.append(k.key);
        }
        return sb.toString();
    }

    public static String catsHint() {
        return "residential / public / commercial";
    }
}
