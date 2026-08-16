package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.PlaytimeManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class LeaderboardGUI {

    public static final String TITLE      = "&8在线时长排行";
    public static final int    CLOSE_SLOT = 53;
    public static final int    BACK_SLOT  = 45;
    public static final int    TRANSIT_SLOT = 47;
    private static final int   SIZE       = 54;
    private static final Material BG      = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin plugin;

    public LeaderboardGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, ColorUtil.colorize(TITLE));

        ItemStack bg = ECOSTerminalGUI.bg(BG);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, bg);

        List<Map.Entry<UUID, Long>> top = plugin.getPlaytimeManager().getTopPlayers(45);

        for (int i = 0; i < top.size(); i++) {
            Map.Entry<UUID, Long> entry = top.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : "?";

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(op);
                String rank = switch (i) {
                    case 0 -> "&e#1";
                    case 1 -> "&7#2";
                    case 2 -> "&6#3";
                    default -> "&8#" + (i + 1);
                };
                meta.setDisplayName(ColorUtil.colorize(rank + " &f" + name));
                boolean online = op.isOnline();
                meta.setLore(List.of(
                        ColorUtil.colorize("&7总在线: &f" + PlaytimeManager.formatMillis(entry.getValue())),
                        ColorUtil.colorize(online ? "&7状态: &a在线" : "&7状态: &8离线")));
                skull.setItemMeta(meta);
            }
            inv.setItem(i, skull);
        }

        if (top.isEmpty())
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7暂无数据", null));

        inv.setItem(BACK_SLOT,  ECOSTerminalGUI.item(Material.ARROW,   "&7返回终端", null));
        inv.setItem(TRANSIT_SLOT, ECOSTerminalGUI.item(Material.POWERED_RAIL, "&b轨道交通排行",
                List.of(ColorUtil.colorize("&8点击切换"))));
        inv.setItem(CLOSE_SLOT, ECOSTerminalGUI.item(Material.BARRIER, "&7关闭",     null));
        player.openInventory(inv);
    }
}
