package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 管理：谁在车上 */
public class TransitRidersGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l在乘名单");
    public static final int SLOT_BACK = 49;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<UUID>> slotsOf = new HashMap<>();

    public TransitRidersGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.RED_STAINED_GLASS_PANE));

        TransitManager tm = plugin.getTransitManager();
        List<Map.Entry<UUID, TransitManager.Journey>> all = tm.activeJourneys();
        inv.setItem(4, ECOSTerminalGUI.item(Material.MINECART, "&c在乘 " + all.size() + " 人",
                List.of(" &7点玩家可清除其未完成行程",
                        " &7超时行程已自动失效，不会出现在这里")));

        List<UUID> ids = new ArrayList<>();
        int slot = 9;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, TransitManager.Journey> e : all) {
            if (slot >= 45) break;
            UUID u = e.getKey();
            TransitManager.Journey j = e.getValue();
            OfflinePlayer off = Bukkit.getOfflinePlayer(u);
            String name = off.getName() == null ? u.toString().substring(0, 8) : off.getName();
            long min = Math.max(0, (now - j.tapInMs()) / 60000L);
            List<String> lore = new ArrayList<>();
            lore.add(" &7进站: &f" + tm.stationName(j.originId()));
            lore.add(" &7席别: &e" + tm.cabinName(j.cabin()));
            lore.add(" &7已过 &f" + min + " &7分钟 · " + j.method());
            lore.add(off.isOnline() ? " &a在线" : " &8离线");
            lore.add("");
            lore.add("&c▸ 清除行程（不扣费）");
            inv.setItem(slot++, ECOSTerminalGUI.item(
                    off.isOnline() ? Material.MINECART : Material.HOPPER_MINECART,
                    (off.isOnline() ? "&a" : "&7") + name, lore));
            ids.add(u);
        }
        if (all.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BOOK, "&7当前无人在乘", null));
        }
        slotsOf.put(player.getUniqueId(), ids);
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回管理", null));
        player.openInventory(inv);
    }

    public UUID riderAt(Player player, int slot) {
        List<UUID> ids = slotsOf.get(player.getUniqueId());
        if (ids == null) return null;
        int i = slot - 9;
        if (i < 0 || i >= ids.size()) return null;
        return ids.get(i);
    }

    public void cleanup(Player player) { slotsOf.remove(player.getUniqueId()); }
}
