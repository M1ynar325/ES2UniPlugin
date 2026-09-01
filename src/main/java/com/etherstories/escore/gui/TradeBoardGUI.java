package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TradeStatsManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/**
 * 今日交易量简易榜。
 */
public class TradeBoardGUI {

    public static final String TITLE = ColorUtil.colorize("&6&l今日交易榜");

    public static final int SLOT_BACK = 45;
    public static final int SLOT_CLOSE = 53;
    public static final int SLOT_INFO = 49;

    private final ES2UniPlugin plugin;

    public TradeBoardGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = EcosHolder.of("trade", 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.BLACK_STAINED_GLASS_PANE));

        TradeStatsManager stats = plugin.getTradeStatsManager();
        List<TradeStatsManager.VolumeEntry> top = stats.getTopPlayers(27);

        for (int i = 0; i < top.size(); i++) {
            TradeStatsManager.VolumeEntry e = top.get(i);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(e.uuid()));
                String rankColor = i == 0 ? "&6" : i == 1 ? "&f" : i == 2 ? "&e" : "&7";
                meta.setDisplayName(ColorUtil.colorize(rankColor + "#" + (i + 1) + " &f" + e.name()));
                meta.setLore(List.of(
                        ColorUtil.colorize("&7交易量: &f" + fmt(e.volume())),
                        ColorUtil.colorize("&8含转账与箱子店购买")
                ));
                skull.setItemMeta(meta);
            }
            inv.setItem(i, skull);
        }

        if (top.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7今日暂无交易记录", null));
        }

        inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.GOLD_INGOT, "&e今日总览",
                List.of(
                        "&7交易总量: &f" + fmt(stats.getTotalVolume()),
                        "&7转账: &f" + fmt(stats.getPayVolume()),
                        "&7商店: &f" + fmt(stats.getQsVolume()),
                        "&7税收: &e" + fmt(stats.getTaxCollected())
                )));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }

    private String fmt(double v) {
        return plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(v) : String.format("%.2f", v);
    }
}
