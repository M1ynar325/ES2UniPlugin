package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.estate.EstateUnit;
import com.etherstories.escore.hotel.HotelRoomType;
import com.etherstories.escore.managers.HotelManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HotelDeskGUI {

    public static final String TITLE = EcosStyle.hub("房间");
    public static final int SLOT_BIND = 45;
    public static final int SLOT_MGR = 46;
    public static final int SLOT_TRANSFER = 47;
    public static final int SLOT_DELETE = 48;
    public static final int SLOT_GUEST_OUT = 49;
    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, String> hotelId = new HashMap<>();
    private final Map<UUID, List<HotelManager.Room>> listed = new HashMap<>();

    public HotelDeskGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, HotelManager.Hotel hotel) {
        hotelId.put(player.getUniqueId(), hotel.id);
        List<HotelManager.Room> rooms = new ArrayList<>(hotel.rooms);
        listed.put(player.getUniqueId(), rooms);
        boolean staff = plugin.getHotelManager().staff(player, hotel);
        Inventory inv = EcosHolder.of("hotel-desk", 54, TITLE);
        for (int i = 0; i < Math.min(rooms.size(), 45); i++) {
            HotelManager.Room r = rooms.get(i);
            EstateUnit u = plugin.getHotelManager().unitOf(r);
            String addr = u == null ? "?" : u.address();
            double tax = plugin.getHotelManager().taxOf(r.price);
            List<String> lore = new ArrayList<>();
            lore.add(" &7" + addr);
            lore.add(" &7房型: &f" + r.type.label);
            lore.add(" &7标价: &e" + plugin.getHotelManager().money(r.price)
                    + (tax > 0 ? " &8含税 " + plugin.getHotelManager().money(r.price + tax) : ""));
            lore.add(r.locked ? " &c已上锁" : " &a未上锁");
            lore.add(r.vacant() ? " &a空房" : " &7住客 &f" + r.guestName);
            lore.add("");
            if (r.vacant()) lore.add("&a左键: 入住");
            else if (r.guest != null && r.guest.equals(player.getUniqueId())) lore.add("&c&l左键: 退房");
            else if (staff) lore.add("&c左键: 驱逐住客");
            if (staff) {
                lore.add("&e右键: 开关锁");
                lore.add("&e潜行右键: 改价");
                lore.add("&e潜行左键: 换房型");
            }
            boolean mine = r.guest != null && r.guest.equals(player.getUniqueId());
            ItemStack roomItem = ECOSTerminalGUI.item(mine ? Material.RED_BED : r.type.icon,
                    (mine ? "&c" : "&f") + addr, lore);
            if (mine) roomItem = ECOSTerminalGUI.glint(roomItem);
            inv.setItem(i, roomItem);
        }
        if (rooms.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7还没绑房间",
                    List.of("&8站在房产里点下方「绑当前房」")));
        }
        if (staff) {
            inv.setItem(SLOT_BIND, ECOSTerminalGUI.item(Material.OAK_DOOR, "&a绑当前房",
                    List.of("&8站在住宅/公寓/客房里点", "&8商铺不能绑", "", "&a▸ 绑进这家酒店")));
            inv.setItem(SLOT_MGR, ECOSTerminalGUI.item(Material.NAME_TAG, "&e管理者",
                    List.of("&8add 玩家 / remove 玩家", "", "&e▸ 输入")));
            if (plugin.getHotelManager().owner(player, hotel)) {
                inv.setItem(SLOT_TRANSFER, ECOSTerminalGUI.item(Material.GOLD_INGOT, "&6转让酒店",
                        List.of("&8输入新店主名", "", "&6▸ 转让")));
                inv.setItem(SLOT_DELETE, ECOSTerminalGUI.item(Material.TNT, "&c删除酒店",
                        List.of("&8输入酒店全名确认",
                                "&c住客会退房，房间解绑",
                                "&8房产还在",
                                "", "&c▸ 删除")));
            }
        }
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回酒店列表", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        var stay = plugin.getHotelManager().stayOf(player.getUniqueId());
        if (stay != null && hotel.rooms.contains(stay)) {
            String label = plugin.getHotelManager().stayLabel(player.getUniqueId());
            inv.setItem(SLOT_GUEST_OUT, ECOSTerminalGUI.glint(ECOSTerminalGUI.item(Material.RED_BED, "&c&l退房",
                    List.of(" &f" + (label == null ? "" : label), "", "&c▸ 退掉这间"))));
        }
        EcosHolder.open(player, inv);
    }

    public HotelManager.Hotel hotelOf(Player p) {
        return plugin.getHotelManager().byId(hotelId.get(p.getUniqueId()));
    }

    public HotelManager.Room roomAt(Player p, int slot) {
        List<HotelManager.Room> list = listed.get(p.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size() || slot >= 45) return null;
        return list.get(slot);
    }

    public HotelRoomType nextType(HotelRoomType cur) {
        HotelRoomType[] all = HotelRoomType.values();
        int i = 0;
        for (int n = 0; n < all.length; n++) if (all[n] == cur) { i = n; break; }
        return all[(i + 1) % all.length];
    }

    public void cleanup(Player p) {
        hotelId.remove(p.getUniqueId());
        listed.remove(p.getUniqueId());
    }
}
