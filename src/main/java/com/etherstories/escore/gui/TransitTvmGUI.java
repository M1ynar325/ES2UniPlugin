package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** 售票机：本站买单程 / 领卡 / 闪付 */
public class TransitTvmGUI {

    public static final String TITLE = ColorUtil.colorize("&e&l售票机");
    public static final int SLOT_TICKET = 11;
    public static final int SLOT_CARD = 13;
    public static final int SLOT_TAPPAY = 15;
    public static final int SLOT_ADJUST = 20;
    public static final int SLOT_CLOSE = 22;

    private final ES2UniPlugin plugin;
    private final java.util.Map<java.util.UUID, String> stationOf = new java.util.HashMap<>();

    public TransitTvmGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public String stationOf(Player player) {
        return stationOf.get(player.getUniqueId());
    }

    public void open(Player player, String stationId) {
        stationOf.put(player.getUniqueId(), stationId);
        TransitManager.Station s = plugin.getTransitManager().getStation(stationId);
        String name = s == null ? stationId : s.displayName();
        Inventory inv = EcosHolder.of("transit-tvm", 27, TITLE);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.YELLOW_STAINED_GLASS_PANE));

        inv.setItem(4, ECOSTerminalGUI.item(Material.LODESTONE, "&e" + name + "站 售票机",
                List.of(" &7本站单程票起点已锁定",
                        " &7闪付：开通后空手刷闸机",
                        " &7交通卡：手持刷闸，扣余额不是卡内钱包",
                        " &7单程票：先付钱，必须从本站进、票面终点出",
                        " &7补票：无进站记录 / 走错站可在此缴费后再出闸")));
        inv.setItem(SLOT_TICKET, ECOSTerminalGUI.item(Material.PAPER, "&f买单程票",
                List.of(" &7从 &f" + name + " &7出发",
                        " &7预付最短路票价，2 小时有效",
                        "",
                        "&f▸ 选择终点")));
        inv.setItem(SLOT_CARD, ECOSTerminalGUI.item(Material.MAP, "&b领取交通卡",
                List.of(" &7工本 &e" + fmt(plugin.getConfig().getDouble("transit.card-fee", 16)),
                        " &7补办会使旧卡失效")));
        boolean tap = plugin.getTransitManager().isTapPay(player.getUniqueId());
        inv.setItem(SLOT_TAPPAY, ECOSTerminalGUI.item(
                tap ? Material.LIME_DYE : Material.GRAY_DYE,
                tap ? "&a闪付已开通" : "&7开通闪付",
                List.of(" &7开通后必须空手右键闸机",
                        " &7手里拿着东西刷会被拒绝",
                        " &7工本 &e" + fmt(plugin.getConfig().getDouble("transit.tap-pay-fee", 8)))));
        inv.setItem(SLOT_ADJUST, ECOSTerminalGUI.item(Material.BIRCH_SIGN, "&6补票 / 行程结算",
                List.of(" &7无进站记录、走错站、线路未连接",
                        " &7先在此缴费，再到出站检票口刷卡",
                        " &7闸机不会直接扣全程票",
                        "",
                        "&6▸ 办理")));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
        plugin.getTransitTicketGUI().setOrigin(player, stationId);
    }

    private String fmt(double v) {
        return plugin.getVaultHook().isEnabled() ? plugin.getVaultHook().format(v) : String.format("%.2f", v);
    }
}
