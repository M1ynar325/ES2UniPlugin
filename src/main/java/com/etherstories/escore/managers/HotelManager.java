package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.estate.EstateUnit;
import com.etherstories.escore.hotel.HotelRoomType;
import com.etherstories.escore.items.HotelCard;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class HotelManager {

    public static final class Hotel {
        public final String id;
        public String name;
        public UUID owner;
        public String ownerName;
        public final List<UUID> managers = new ArrayList<>();
        public final List<Room> rooms = new ArrayList<>();

        Hotel(String id, String name, UUID owner, String ownerName) {
            this.id = id;
            this.name = name;
            this.owner = owner;
            this.ownerName = ownerName;
        }
    }

    public static final class Room {
        public final String unitId;
        public HotelRoomType type = HotelRoomType.STANDARD;
        public double price;
        public boolean locked = true;
        public UUID guest;
        public String guestName;
        public long checkinMs;

        Room(String unitId) {
            this.unitId = unitId;
        }

        public boolean vacant() {
            return guest == null;
        }
    }

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private final List<Hotel> hotels = new ArrayList<>();

    public HotelManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "hotels.yml");
        load();
        sweepOrphans();
    }

    public List<Hotel> all() {
        return List.copyOf(hotels);
    }

    public Hotel byId(String id) {
        if (id == null) return null;
        for (Hotel h : hotels) if (h.id.equalsIgnoreCase(id)) return h;
        return null;
    }

    public Hotel byName(String name) {
        if (name == null) return null;
        for (Hotel h : hotels) if (h.name.equalsIgnoreCase(name) || h.id.equalsIgnoreCase(name)) return h;
        return null;
    }

    public Room roomOf(EstateUnit unit) {
        if (unit == null) return null;
        for (Hotel h : hotels) {
            for (Room r : h.rooms) if (r.unitId.equals(unit.id())) return r;
        }
        return null;
    }

    public Hotel hotelOf(EstateUnit unit) {
        if (unit == null) return null;
        for (Hotel h : hotels) {
            for (Room r : h.rooms) if (r.unitId.equals(unit.id())) return h;
        }
        return null;
    }

    public Hotel hotelOfRoom(Room room) {
        if (room == null) return null;
        for (Hotel h : hotels) {
            if (h.rooms.contains(room)) return h;
        }
        return null;
    }

    public List<Hotel> ownedOrManaged(UUID uuid) {
        List<Hotel> list = new ArrayList<>();
        for (Hotel h : hotels) {
            if (uuid.equals(h.owner) || h.managers.contains(uuid)) list.add(h);
        }
        return list;
    }

    public Room stayOf(UUID uuid) {
        for (Hotel h : hotels) {
            for (Room r : h.rooms) if (uuid.equals(r.guest)) return r;
        }
        return null;
    }

    public String stayLabel(UUID uuid) {
        Room r = stayOf(uuid);
        Hotel h = hotelOfRoom(r);
        if (h == null || r == null) return null;
        EstateUnit u = unitOf(r);
        return h.name + " · " + (u == null ? "?" : u.address());
    }

    public boolean staff(Player p, Hotel h) {
        if (h == null || p == null) return false;
        if (p.hasPermission("es2uni.admin")) return true;
        return p.getUniqueId().equals(h.owner) || h.managers.contains(p.getUniqueId());
    }

    public boolean owner(Player p, Hotel h) {
        return h != null && (p.hasPermission("es2uni.admin") || p.getUniqueId().equals(h.owner));
    }

    public String create(Player p, String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty() || n.length() > 24) return "酒店名 1–24 字";
        String id = slug(n);
        if (byId(id) != null || byName(n) != null) return "这个酒店名已经有了";
        hotels.add(new Hotel(id, n, p.getUniqueId(), p.getName()));
        save();
        return null;
    }

    public String bind(Player p, Hotel hotel, EstateUnit unit) {
        if (hotel == null) return "酒店不存在";
        if (unit == null) return "先站在已登记的房产里，或指定门牌";
        if (!staff(p, hotel)) return "只有店主或管理者能绑房";
        if (!p.hasPermission("es2uni.admin") && !p.getUniqueId().equals(unit.owner())
                && !p.getUniqueId().equals(unit.createdBy())) {
            return "这间房产不是你的";
        }
        if (!unit.kind().hotelBindable()) return "只能绑住宅、公寓或客房";
        if (unit.listed() && unit.price() > 0) return "先下架再绑成客房";
        if (hotelOf(unit) != null) return "这间已经绑过酒店了";
        Room r = new Room(unit.id());
        r.price = Math.max(0, plugin.getConfig().getDouble("hotel.default-price", 256));
        hotel.rooms.add(r);
        save();
        return null;
    }

    public String unbind(Player p, Hotel hotel, EstateUnit unit) {
        if (hotel == null || unit == null) return "找不到房间";
        if (!staff(p, hotel)) return "只有店主或管理者能解绑";
        Room r = null;
        for (Room x : hotel.rooms) if (x.unitId.equals(unit.id())) { r = x; break; }
        if (r == null) return "这间不在该酒店";
        if (!r.vacant()) return "还有住客，先退房";
        hotel.rooms.remove(r);
        save();
        return null;
    }

    public String setType(Player p, Room room, HotelRoomType type) {
        Hotel h = hotelOfRoom(room);
        if (!staff(p, h)) return "没有权限";
        if (type == null) return "房型: " + HotelRoomType.hint();
        room.type = type;
        save();
        return null;
    }

    public String setPrice(Player p, Room room, double price) {
        Hotel h = hotelOfRoom(room);
        if (!staff(p, h)) return "没有权限";
        if (price < 0) return "价格不能为负";
        room.price = price;
        save();
        return null;
    }

    public String setLocked(Player p, Room room, boolean locked) {
        Hotel h = hotelOfRoom(room);
        if (!staff(p, h)) return "没有权限";
        room.locked = locked;
        save();
        return null;
    }

    public String addManager(Player p, Hotel hotel, OfflinePlayer who) {
        if (!owner(p, hotel)) return "只有店主能改管理者";
        if (who == null || (!who.hasPlayedBefore() && !who.isOnline())) return "找不到玩家";
        if (who.getUniqueId().equals(hotel.owner)) return "店主不用再加";
        if (hotel.managers.contains(who.getUniqueId())) return "已经是管理者";
        hotel.managers.add(who.getUniqueId());
        save();
        return null;
    }

    public String removeManager(Player p, Hotel hotel, UUID who) {
        if (!owner(p, hotel)) return "只有店主能改管理者";
        hotel.managers.remove(who);
        save();
        return null;
    }

    public String transfer(Player p, Hotel hotel, OfflinePlayer to) {
        if (!owner(p, hotel)) return "只有店主能转让";
        if (to == null || (!to.hasPlayedBefore() && !to.isOnline())) return "找不到玩家";
        if (to.getUniqueId().equals(hotel.owner)) return "已经是店主";
        hotel.owner = to.getUniqueId();
        hotel.ownerName = to.getName() != null ? to.getName() : hotel.ownerName;
        hotel.managers.clear();
        save();
        return null;
    }

    /** 店主删除整家酒店：住客退房、房间解绑，房产本身还在。 */
    public String delete(Player p, Hotel hotel) {
        if (hotel == null) return "酒店不存在";
        if (!owner(p, hotel)) return "只有店主能删除";
        String hotelName = hotel.name;
        List<UUID> guests = new ArrayList<>();
        for (Room r : new ArrayList<>(hotel.rooms)) {
            if (r.guest != null) {
                guests.add(r.guest);
                stripCards(r.guest, hotel.id, r.unitId);
            }
        }
        hotels.remove(hotel);
        save();
        for (UUID g : guests) {
            Player gp = Bukkit.getPlayer(g);
            if (gp != null && gp.isOnline())
                gp.sendMessage(ColorUtil.colorize("&8[酒店] &c" + hotelName + " &7已关闭，你已退房"));
        }
        return null;
    }

    public String checkin(Player p, Room room) {
        Hotel hotel = hotelOfRoom(room);
        if (hotel == null || room == null) return "房间不在酒店里";
        EstateUnit unit = plugin.getEstateManager().byId(room.unitId);
        if (unit == null) return "房产登记已丢失";
        if (unit.listed() && unit.price() > 0) return "这间正在挂牌出售，先下架再入住";
        if (!room.vacant()) return "这间有人住";
        if (stayOf(p.getUniqueId()) != null) return "你已经入住过一间了，先退房";
        if (room.price <= 0) return "店主还没标价";
        if (!plugin.getVaultHook().isEnabled()) return "经济系统不可用";
        OfflinePlayer owner = Bukkit.getOfflinePlayer(hotel.owner);
        double tax = plugin.getTaxManager().isPayTaxEnabled()
                ? plugin.getTaxManager().calcTax(room.price, plugin.getTaxManager().getPayTaxRate()) : 0;
        String err = plugin.getVaultHook().pay(p, owner, room.price, tax);
        if (err != null) return err;
        room.guest = p.getUniqueId();
        room.guestName = p.getName();
        room.checkinMs = System.currentTimeMillis();
        save();
        giveCard(p, hotel, room, unit);
        return null;
    }

    public String checkout(Player p, Room room) {
        Hotel hotel = hotelOfRoom(room);
        if (hotel == null || room == null) return "房间不在酒店里";
        boolean self = room.guest != null && room.guest.equals(p.getUniqueId());
        if (!self && !staff(p, hotel)) return "这不是你的入住，也不是店员";
        UUID guest = room.guest;
        room.guest = null;
        room.guestName = null;
        room.checkinMs = 0;
        save();
        if (guest != null) stripCards(guest, hotel.id, room.unitId);
        return null;
    }

    public String giveCard(Player p, Hotel hotel, Room room, EstateUnit unit) {
        if (room.guest == null || !room.guest.equals(p.getUniqueId())) return "你不是这间的住客";
        ItemStack card = HotelCard.create(hotel.id, room.unitId, p.getUniqueId(), room.checkinMs,
                hotel.name, unit == null ? room.unitId : unit.address());
        p.getInventory().addItem(card).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
        return null;
    }

    public boolean canOpen(Player p, EstateUnit unit) {
        Hotel hotel = hotelOf(unit);
        Room room = roomOf(unit);
        if (hotel == null || room == null) return true;
        if (!room.locked) return true;
        if (staff(p, hotel)) return true;
        if (room.guest != null && room.guest.equals(p.getUniqueId())) return true;
        return holdingValidCard(p, hotel, room);
    }

    public boolean holdingValidCard(Player p, Hotel hotel, Room room) {
        PlayerInventory inv = p.getInventory();
        for (ItemStack it : inv.getContents()) {
            if (validCard(it, p, hotel, room)) return true;
        }
        return validCard(inv.getItemInOffHand(), p, hotel, room);
    }

    private boolean validCard(ItemStack stack, Player p, Hotel hotel, Room room) {
        if (!HotelCard.isCard(stack)) return false;
        if (!hotel.id.equals(HotelCard.hotelId(stack))) return false;
        if (!room.unitId.equals(HotelCard.unitId(stack))) return false;
        if (!p.getUniqueId().equals(HotelCard.guest(stack))) return false;
        return room.checkinMs > 0 && room.checkinMs == HotelCard.token(stack);
    }

    private void stripCards(UUID guest, String hotelId, String unitId) {
        Player p = Bukkit.getPlayer(guest);
        if (p == null || !p.isOnline()) return;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (!HotelCard.isCard(it)) continue;
            if (hotelId.equals(HotelCard.hotelId(it)) && unitId.equals(HotelCard.unitId(it))) {
                p.getInventory().setItem(i, null);
            }
        }
    }

    public void sendHere(Player p, EstateUnit unit) {
        Hotel hotel = hotelOf(unit);
        Room room = roomOf(unit);
        if (hotel == null || room == null) {
            p.sendMessage(ColorUtil.colorize("&8[酒店] &7这里不是酒店房间"));
            return;
        }
        p.sendMessage(ColorUtil.colorize("&8[酒店] &f" + hotel.name
                + " &8· &f" + (unit == null ? "?" : unit.address())
                + " &8" + room.type.label
                + " &e" + money(room.price)
                + (room.locked ? " &c锁" : " &a开")
                + (room.vacant() ? " &a空房" : " &7住客 " + room.guestName)));
    }

    public String money(double v) {
        if (plugin.getVaultHook().isEnabled()) return plugin.getVaultHook().format(v);
        return String.format("%.0f", v);
    }

    public double taxOf(double price) {
        if (!plugin.getTaxManager().isPayTaxEnabled()) return 0;
        return plugin.getTaxManager().calcTax(price, plugin.getTaxManager().getPayTaxRate());
    }

    public EstateUnit unitOf(Room room) {
        return room == null ? null : plugin.getEstateManager().byId(room.unitId);
    }

    /** 买卖或删除房产时清入住并解绑。 */
    public void detach(String unitId) {
        if (unitId == null) return;
        boolean dirty = false;
        for (Hotel h : hotels) {
            Room r = null;
            for (Room x : h.rooms) if (x.unitId.equals(unitId)) { r = x; break; }
            if (r == null) continue;
            if (r.guest != null) stripCards(r.guest, h.id, r.unitId);
            h.rooms.remove(r);
            dirty = true;
        }
        if (dirty) save();
    }

    public void sweepOrphans() {
        if (plugin.getEstateManager() == null) return;
        boolean dirty = false;
        for (Hotel h : hotels) {
            dirty |= h.rooms.removeIf(r -> plugin.getEstateManager().byId(r.unitId) == null);
        }
        if (dirty) save();
    }

    public EstateUnit at(Location loc) {
        return plugin.getEstateManager().at(loc);
    }

    private static String slug(String name) {
        String s = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\u4e00-\\u9fff]+", "");
        if (s.isEmpty()) s = "hotel" + Integer.toHexString(name.hashCode());
        if (s.length() > 16) s = s.substring(0, 16);
        return s;
    }

    public void load() {
        hotels.clear();
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = cfg.getConfigurationSection("hotels");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection hs = root.getConfigurationSection(id);
            if (hs == null) continue;
            UUID owner;
            try { owner = UUID.fromString(hs.getString("owner", "")); }
            catch (Exception e) { continue; }
            Hotel h = new Hotel(id, hs.getString("name", id), owner, hs.getString("owner-name", "?"));
            for (String m : hs.getStringList("managers")) {
                try { h.managers.add(UUID.fromString(m)); } catch (Exception ignored) {}
            }
            ConfigurationSection rs = hs.getConfigurationSection("rooms");
            if (rs != null) {
                for (String uid : rs.getKeys(false)) {
                    ConfigurationSection rr = rs.getConfigurationSection(uid);
                    if (rr == null) continue;
                    Room room = new Room(uid);
                    HotelRoomType t = HotelRoomType.fromKey(rr.getString("type", "standard"));
                    room.type = t == null ? HotelRoomType.STANDARD : t;
                    room.price = rr.getDouble("price", 0);
                    room.locked = rr.getBoolean("locked", true);
                    String g = rr.getString("guest");
                    if (g != null && !g.isEmpty()) {
                        try {
                            room.guest = UUID.fromString(g);
                            room.guestName = rr.getString("guest-name");
                            room.checkinMs = rr.getLong("checkin-ms", 0);
                        } catch (Exception ignored) {}
                    }
                    h.rooms.add(room);
                }
            }
            hotels.add(h);
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Hotel h : hotels) {
            String base = "hotels." + h.id;
            cfg.set(base + ".name", h.name);
            cfg.set(base + ".owner", h.owner.toString());
            cfg.set(base + ".owner-name", h.ownerName);
            List<String> mgr = new ArrayList<>();
            h.managers.forEach(u -> mgr.add(u.toString()));
            cfg.set(base + ".managers", mgr);
            for (Room r : h.rooms) {
                String rb = base + ".rooms." + r.unitId;
                cfg.set(rb + ".type", r.type.key);
                cfg.set(rb + ".price", r.price);
                cfg.set(rb + ".locked", r.locked);
                cfg.set(rb + ".guest", r.guest == null ? null : r.guest.toString());
                cfg.set(rb + ".guest-name", r.guestName);
                cfg.set(rb + ".checkin-ms", r.checkinMs);
            }
        }
        try { cfg.save(dataFile); }
        catch (IOException e) { plugin.getLogger().warning("hotels.yml 保存失败: " + e.getMessage()); }
    }
}
