package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransitHistoryGUI {

    public static final String TITLE = ColorUtil.colorize("&7&l乘车记录");
    public static final int SLOT_BACK = 49;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ES2UniPlugin plugin;

    public TransitHistoryGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.GRAY_STAINED_GLASS_PANE));

        TransitManager tm = plugin.getTransitManager();
        List<TransitManager.Ride> rides = tm.ridesOf(player.getUniqueId());
        int slot = 0;
        for (TransitManager.Ride r : rides) {
            if (slot >= 45) break;
            TransitManager.Station from = tm.getStation(r.fromId());
            TransitManager.Station to = tm.getStation(r.toId());
            String fn = from == null ? r.fromId() : from.displayName();
            String tn = to == null ? r.toId() : to.displayName();
            List<String> lore = new ArrayList<>();
            lore.add(" &7" + FMT.format(Instant.ofEpochMilli(r.at())));
            lore.add(" &7票价: &e" + fmt(r.fare()));
            lore.add(" &8" + noteZh(r.note()));
            inv.setItem(slot++, ECOSTerminalGUI.item(
                    r.fare() <= 0 ? Material.MAP : Material.PAPER,
                    "&f" + fn + " → " + tn, lore));
        }
        if (rides.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BOOK, "&7暂无记录", List.of("&8刷闸后会出现在这里")));
        }
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回交通处", null));
        player.openInventory(inv);
    }

    private static String noteZh(String n) {
        return switch (n) {
            case "cancel" -> "宽限取消";
            case "adjust-ghost" -> "补票 · 无进站记录";
            case "adjust-unlinked" -> "补票 · 无票价路径";
            case "adjust-settle" -> "补票 · 行程结算";
            case "adjust-ticket-reroute" -> "补票 · 单程票改站";
            case "adjust-out" -> "补票凭证出站";
            case "timeout-expire" -> "超时未出站 · 行程失效";
            case "admin-clear" -> "管理清除行程";
            case "timeout-maxfare" -> "超时未出站 · 全程票";
            case "incomplete-maxfare" -> "未出站再进 · 全程票";
            case "unlinked-maxfare" -> "无路径 · 按全程";
            case "ticket" -> "单程票";
            default -> "正常计价";
        };
    }

    private String fmt(double v) {
        return plugin.getVaultHook().isEnabled() ? plugin.getVaultHook().format(v) : String.format("%.2f", v);
    }
}
