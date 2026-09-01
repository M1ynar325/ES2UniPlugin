package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.HotelManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HotelListGUI {

    public static final String TITLE = EcosStyle.hub("酒店");

    public static boolean isTitle(String title) {
        if (title == null) return false;
        String s = org.bukkit.ChatColor.stripColor(title);
        return s != null && s.contains("酒店") && !s.contains("房间");
    }
    public static final int SLOT_CHECKOUT = 45;
    public static final int SLOT_CARD = 46;
    public static final int SLOT_MINE = 47;
    public static final int SLOT_CREATE = 48;
    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<HotelManager.Hotel>> listed = new HashMap<>();
    private final Set<UUID> mineView = new HashSet<>();

    public HotelListGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        mineView.remove(player.getUniqueId());
        List<HotelManager.Hotel> hotels = new ArrayList<>(plugin.getHotelManager().all());
        listed.put(player.getUniqueId(), hotels);
        String stayLabel = plugin.getHotelManager().stayLabel(player.getUniqueId());
        String title = stayLabel == null
                ? TITLE
                : ColorUtil.colorize(EcosStyle.MIST + "ECOS / " + EcosStyle.JIQING + "酒店 " + EcosStyle.MIST + "· &c入住");
        Inventory inv = EcosHolder.of("hotel", 54, title);
        for (int i = 0; i < Math.min(hotels.size(), 45); i++) {
            HotelManager.Hotel h = hotels.get(i);
            int vacant = 0;
            for (HotelManager.Room r : h.rooms) if (r.vacant()) vacant++;
            boolean staff = plugin.getHotelManager().staff(player, h);
            List<String> lore = new ArrayList<>();
            lore.add(" &7店主: &f" + h.ownerName);
            lore.add(" &7房间: &f" + h.rooms.size() + "  &8空 " + vacant);
            lore.add("");
            lore.add(EcosStyle.COBALT + "▸ 查看房间");
            if (staff) lore.add("&e店员: 可改价 / 锁 / 退房");
            inv.setItem(i, ECOSTerminalGUI.item(Material.BELL, "&f" + h.name, lore));
        }
        if (hotels.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7还没有酒店",
                    List.of("&8点下方创建，或 /ecos hotel create")));
        }

        fillStayBar(inv, player);
        inv.setItem(SLOT_MINE, ECOSTerminalGUI.item(Material.CHEST, "&e我管理的",
                List.of(" &7" + plugin.getHotelManager().ownedOrManaged(player.getUniqueId()).size() + " 家",
                        "", "&e▸ 只看自己的")));
        inv.setItem(SLOT_CREATE, ECOSTerminalGUI.item(Material.WRITABLE_BOOK, "&a创建酒店",
                List.of("&8输入酒店名", "", "&a▸ 开始")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }

    public void openMine(Player player) {
        mineView.add(player.getUniqueId());
        List<HotelManager.Hotel> hotels = plugin.getHotelManager().ownedOrManaged(player.getUniqueId());
        listed.put(player.getUniqueId(), hotels);
        Inventory inv = EcosHolder.of("hotel", 54, TITLE);
        for (int i = 0; i < Math.min(hotels.size(), 45); i++) {
            HotelManager.Hotel h = hotels.get(i);
            inv.setItem(i, ECOSTerminalGUI.item(Material.BELL, "&e" + h.name,
                    List.of(" &7房间: &f" + h.rooms.size(), "", "&e▸ 管理")));
        }
        if (hotels.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7你没有酒店",
                    List.of("&8先创建一家")));
        }
        fillStayBar(inv, player);
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回全部", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }

    private void fillStayBar(Inventory inv, Player player) {
        HotelManager.Room stay = plugin.getHotelManager().stayOf(player.getUniqueId());
        String stayLabel = plugin.getHotelManager().stayLabel(player.getUniqueId());
        ItemStack out;
        if (stay != null && stayLabel != null) {
            out = ECOSTerminalGUI.glint(ECOSTerminalGUI.item(Material.RED_BED, "&c&l退房",
                    List.of(" &f" + stayLabel,
                            " &7房卡会收回",
                            "", "&c▸ 点击退房")));
        } else {
            out = ECOSTerminalGUI.item(Material.IRON_DOOR, "&7退房",
                    List.of(" &8当前没有入住"));
        }
        inv.setItem(SLOT_CHECKOUT, out);
        inv.setItem(SLOT_CARD, ECOSTerminalGUI.item(Material.PAPER,
                stay == null ? "&7补领房卡" : EcosStyle.COBALT + "补领房卡",
                List.of(stay == null ? "&8当前没有入住" : EcosStyle.COBALT + "▸ 再给一张")));
    }

    public boolean isMineView(Player p) {
        return mineView.contains(p.getUniqueId());
    }

    public HotelManager.Hotel hotelAt(Player p, int slot) {
        List<HotelManager.Hotel> list = listed.get(p.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size() || slot >= 45) return null;
        return list.get(slot);
    }

    public void cleanup(Player p) {
        listed.remove(p.getUniqueId());
        mineView.remove(p.getUniqueId());
    }
}
