package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TransitManager;
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

/** 补票处 / 售票机补票 */
public class TransitAdjustGUI {

    public static final String TITLE = ColorUtil.colorize("&6&l补票处");
    public static final int SLOT_INFO = 4;
    public static final int SLOT_PAY = 13;
    public static final int SLOT_CLOSE = 22;

    private final ES2UniPlugin plugin;
    private final Map<UUID, String> stationOf = new HashMap<>();

    public TransitAdjustGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public String stationOf(Player player) { return stationOf.get(player.getUniqueId()); }

    public void open(Player player, String stationId) {
        stationOf.put(player.getUniqueId(), stationId);
        TransitManager tm = plugin.getTransitManager();
        TransitManager.Station s = tm.getStation(stationId);
        String name = s == null ? stationId : s.displayName();
        TransitManager.AdjustQuote q = tm.quoteAdjust(player, stationId);

        Inventory inv = EcosHolder.of("transit-adjust", 27, TITLE);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.ORANGE_STAINED_GLASS_PANE));

        List<String> info = new ArrayList<>();
        info.add(" &7本站: &f" + name);
        info.add(" &7" + q.title());
        info.add(" &8" + q.detail());
        if (q.payable()) info.add(" &7应缴: &e" + fmt(q.amount()));
        inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.MAP, "&6补票 / 行程结算", info));

        if (q.payable()) {
            inv.setItem(SLOT_PAY, ECOSTerminalGUI.item(Material.GOLD_INGOT, "&e确认缴费",
                    List.of(" &7" + q.title(),
                            " &7金额: &e" + fmt(q.amount()),
                            " &8缴费后请在时限内从本站出站检票口刷卡",
                            "",
                            "&e▸ 付款")));
        } else {
            inv.setItem(SLOT_PAY, ECOSTerminalGUI.item(Material.GRAY_DYE, "&7无需补票",
                    List.of(" &7" + q.title(),
                            " &8" + q.detail())));
        }
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }

    public void cleanup(Player player) { stationOf.remove(player.getUniqueId()); }

    private String fmt(double v) {
        if (v <= 0) return "免费";
        return plugin.getVaultHook().isEnabled() ? plugin.getVaultHook().format(v) : String.format("%.2f", v);
    }
}
