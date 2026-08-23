package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.HotelManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HotelListGUI {

    public static final String TITLE = ColorUtil.colorize("&9&l酒店");
    public static final int SLOT_MINE = 45;
    public static final int SLOT_CREATE = 46;
    public static final int SLOT_CARD = 47;
    public static final int SLOT_CHECKOUT = 48;
    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<HotelManager.Hotel>> listed = new HashMap<>();

    public HotelListGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<HotelManager.Hotel> hotels = new ArrayList<>(plugin.getHotelManager().all());
        listed.put(player.getUniqueId(), hotels);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < Math.min(hotels.size(), 45); i++) {
            HotelManager.Hotel h = hotels.get(i);
            int vacant = 0;
            for (HotelManager.Room r : h.rooms) if (r.vacant()) vacant++;
            boolean staff = plugin.getHotelManager().staff(player, h);
            List<String> lore = new ArrayList<>();
            lore.add(" &7店主: &f" + h.ownerName);
            lore.add(" &7房间: &f" + h.rooms.size() + "  &8空 " + vacant);
            lore.add("");
            lore.add("&9▸ 查看房间");
            if (staff) lore.add("&e店员: 可改价 / 锁 / 退房");
            inv.setItem(i, ECOSTerminalGUI.item(Material.BELL, "&f" + h.name, lore));
        }
        if (hotels.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7还没有酒店",
                    List.of("&8店主: /ecos hotel create <名>")));
        }
        HotelManager.Room stay = plugin.getHotelManager().stayOf(player.getUniqueId());
        inv.setItem(SLOT_MINE, ECOSTerminalGUI.item(Material.CHEST, "&e我管理的",
                List.of(" &7" + plugin.getHotelManager().ownedOrManaged(player.getUniqueId()).size() + " 家",
                        "", "&e▸ 只看自己的")));
        inv.setItem(SLOT_CREATE, ECOSTerminalGUI.item(Material.WRITABLE_BOOK, "&a创建酒店",
                List.of("&8聊天输入酒店名", "", "&a▸ 开始")));
        inv.setItem(SLOT_CARD, ECOSTerminalGUI.item(Material.PAPER, "&b补领房卡",
                List.of(stay == null ? "&8当前没有入住" : "&b▸ 再给一张当前房间的卡")));
        inv.setItem(SLOT_CHECKOUT, ECOSTerminalGUI.item(Material.IRON_DOOR, "&c退房",
                List.of(stay == null ? "&8当前没有入住" : "&c▸ 退掉当前房间")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    public void openMine(Player player) {
        List<HotelManager.Hotel> hotels = plugin.getHotelManager().ownedOrManaged(player.getUniqueId());
        listed.put(player.getUniqueId(), hotels);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < Math.min(hotels.size(), 45); i++) {
            HotelManager.Hotel h = hotels.get(i);
            inv.setItem(i, ECOSTerminalGUI.item(Material.BELL, "&e" + h.name,
                    List.of(" &7房间: &f" + h.rooms.size(), "", "&e▸ 管理")));
        }
        if (hotels.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7你没有酒店",
                    List.of("&8先 /ecos hotel create")));
        }
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回全部", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    public HotelManager.Hotel hotelAt(Player p, int slot) {
        List<HotelManager.Hotel> list = listed.get(p.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size() || slot >= 45) return null;
        return list.get(slot);
    }

    public void cleanup(Player p) {
        listed.remove(p.getUniqueId());
    }
}
