package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TaxManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * 管理员税收设置
 */
public class AdminTaxGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l税收设置");

    public static final int SLOT_PAY_TOGGLE = 11;
    public static final int SLOT_PAY_RATE_DOWN = 19;
    public static final int SLOT_PAY_RATE_UP = 21;
    public static final int SLOT_QS_TOGGLE = 15;
    public static final int SLOT_QS_RATE_DOWN = 23;
    public static final int SLOT_QS_RATE_UP = 25;
    public static final int SLOT_LINK_RATE_DOWN = 28;
    public static final int SLOT_LINK_TOGGLE = 29;
    public static final int SLOT_LINK_RATE_UP = 30;
    public static final int SLOT_INFO = 13;
    public static final int SLOT_BACK = 31;
    public static final int SLOT_CLOSE = 35;

    private final ES2UniPlugin plugin;

    public AdminTaxGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = EcosHolder.of("admin-tax", 36, TITLE);
        for (int i = 0; i < 36; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.GRAY_STAINED_GLASS_PANE));

        TaxManager tax = plugin.getTaxManager();

        var stats = plugin.getTradeStatsManager();
        String taxToday = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(stats.getTaxCollected())
                : String.format("%.2f", stats.getTaxCollected());
        String volToday = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(stats.getTotalVolume())
                : String.format("%.2f", stats.getTotalVolume());

        inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.GOLD_INGOT, "&e税收池 · 今日",
                java.util.List.of(
                        "&7今日税收合计: &e" + taxToday,
                        "&7今日交易总量: &f" + volToday,
                        "&7转账额: &f" + (plugin.getVaultHook().isEnabled()
                                ? plugin.getVaultHook().format(stats.getPayVolume())
                                : String.format("%.2f", stats.getPayVolume())),
                        "&7商店额: &f" + (plugin.getVaultHook().isEnabled()
                                ? plugin.getVaultHook().format(stats.getQsVolume())
                                : String.format("%.2f", stats.getQsVolume())),
                        "",
                        "&7付款方在本金外加付税",
                        "&8sink: &f" + (tax.getSinkAccount().isBlank() ? "（销毁）" : tax.getSinkAccount())
                )));

        inv.setItem(SLOT_PAY_TOGGLE, ECOSTerminalGUI.item(
                tax.isPayTaxEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                tax.isPayTaxEnabled() ? "&a转账税: 开" : "&7转账税: 关",
                java.util.List.of(
                        "&7作用于 /pay 与终端转账",
                        "&7当前税率: &f" + tax.formatRate(tax.getPayTaxRate()),
                        "",
                        "&e▸ 点击开关"
                )));

        inv.setItem(SLOT_PAY_RATE_DOWN, ECOSTerminalGUI.item(Material.RED_CONCRETE,
                "&c转账税率 -1%", java.util.List.of("&7当前 &f" + tax.formatRate(tax.getPayTaxRate()))));
        inv.setItem(SLOT_PAY_RATE_UP, ECOSTerminalGUI.item(Material.LIME_CONCRETE,
                "&a转账税率 +1%", java.util.List.of("&7当前 &f" + tax.formatRate(tax.getPayTaxRate()))));

        inv.setItem(SLOT_QS_TOGGLE, ECOSTerminalGUI.item(
                tax.isQuickShopTaxEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                tax.isQuickShopTaxEnabled() ? "&a商店税: 开" : "&7商店税: 关",
                java.util.List.of(
                        "&7作用于 QuickShop 箱子店（需装 QS）",
                        "&7当前税率: &f" + tax.formatRate(tax.getQuickShopTaxRate()),
                        "&8会叠在 QS 自带税之外",
                        "",
                        "&e▸ 点击开关"
                )));

        inv.setItem(SLOT_QS_RATE_DOWN, ECOSTerminalGUI.item(Material.RED_CONCRETE,
                "&c商店税率 -1%", java.util.List.of("&7当前 &f" + tax.formatRate(tax.getQuickShopTaxRate()))));
        inv.setItem(SLOT_QS_RATE_UP, ECOSTerminalGUI.item(Material.LIME_CONCRETE,
                "&a商店税率 +1%", java.util.List.of("&7当前 &f" + tax.formatRate(tax.getQuickShopTaxRate()))));

        var link = plugin.getESLinkHook();
        if (link.present()) {
            inv.setItem(SLOT_LINK_TOGGLE, ECOSTerminalGUI.item(
                    link.tradeEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                    link.tradeEnabled() ? "&a互通交易: 开" : "&7互通交易: 关",
                    java.util.List.of(
                            "&7作用于 ESLink 跨服市场买卖",
                            "&7当前税率: &f" + link.formatRate(),
                            "&7买家多付，卖家仍收标价",
                            "",
                            "&e▸ 点击开关"
                    )));
            inv.setItem(SLOT_LINK_RATE_DOWN, ECOSTerminalGUI.item(Material.RED_CONCRETE,
                    "&c互通税率 -1%", java.util.List.of("&7当前 &f" + link.formatRate())));
            inv.setItem(SLOT_LINK_RATE_UP, ECOSTerminalGUI.item(Material.LIME_CONCRETE,
                    "&a互通税率 +1%", java.util.List.of("&7当前 &f" + link.formatRate())));
        } else {
            inv.setItem(SLOT_LINK_TOGGLE, ECOSTerminalGUI.item(Material.BARRIER, "&8互通交易",
                    java.util.List.of("&c未安装 ESLink")));
        }

        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }
}
