package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/** 管理处 · 交通处 */
public class TransitOfficeGUI {

    public static final String TITLE = ColorUtil.colorize("&b&l交通处");

    public static final int SLOT_STATUS = 4;
    public static final int SLOT_TAPPAY = 11;
    public static final int SLOT_CARD = 13;
    public static final int SLOT_LOST = 15;
    public static final int SLOT_TICKET = 20;
    public static final int SLOT_MAP = 22;
    public static final int SLOT_HISTORY = 24;
    public static final int SLOT_BOARD = 26;
    public static final int SLOT_ADMIN = 31;
    public static final int SLOT_INSURE = 29;
    public static final int SLOT_HELP = 33;
    public static final int SLOT_BACK = 48;
    public static final int SLOT_CLOSE = 49;

    private final ES2UniPlugin plugin;

    public TransitOfficeGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.LIGHT_BLUE_STAINED_GLASS_PANE));

        TransitManager tm = plugin.getTransitManager();
        TransitManager.Journey j = tm.getJourney(player.getUniqueId());
        TransitManager.Account acc = tm.getAccount(player.getUniqueId());
        String hint = tm.journeyHint(player.getUniqueId());

        List<String> status = new ArrayList<>();
        status.add(" &7闪付: " + (acc.tapPay() ? "&a已开通" : "&8未开通"));
        status.add(" &7交通卡: " + (acc.cardId() != null ? "&a已登记" : "&8无"));
        status.add(hint == null ? " &8当前无行程" : " &b" + hint);
        if (j != null) {
            TransitManager.Station s = tm.getStation(j.originId());
            status.add(" &7进站: &f" + (s == null ? j.originId() : s.displayName()));
        }
        status.add("");
        status.add("&b▸ 轨道交通 · 扣插件余额");
        inv.setItem(SLOT_STATUS, ECOSTerminalGUI.item(Material.RAIL, "&b&lECOS 轨道交通", status));

        inv.setItem(SLOT_TAPPAY, ECOSTerminalGUI.item(
                acc.tapPay() ? Material.LIME_DYE : Material.GRAY_DYE,
                acc.tapPay() ? "&a闪付已开通" : "&7开通 ECOS 闪付",
                List.of(" &7开通后空手右键闸机即可进出",
                        " &7工本费: &e" + fmt(plugin.getConfig().getDouble("transit.tap-pay-fee", 8)),
                        "",
                        acc.tapPay() ? "&c▸ 点击关闭闪付" : "&a▸ 点击开通")));

        inv.setItem(SLOT_CARD, ECOSTerminalGUI.item(Material.MAP,
                "&b领取 / 补办交通卡",
                List.of(" &7工本费: &e" + fmt(plugin.getConfig().getDouble("transit.card-fee", 16)),
                        " &7刷卡扣 Vault 余额，不是卡内钱包",
                        acc.cardId() != null ? " &8补办会使旧卡失效" : " &8尚未领卡",
                        "",
                        "&b▸ 点击领取")));

        inv.setItem(SLOT_LOST, ECOSTerminalGUI.item(Material.BARRIER,
                "&c挂失交通卡",
                List.of(" &7作废当前卡号",
                        " &7之后需重新领取",
                        "",
                        "&c▸ 点击挂失")));

        inv.setItem(SLOT_TICKET, ECOSTerminalGUI.item(Material.PAPER,
                "&f购买单程票",
                List.of(" &7预付最短路票价",
                        " &7须从起点进、终点出",
                        "",
                        "&f▸ 选择车站")));

        inv.setItem(SLOT_MAP, ECOSTerminalGUI.item(Material.MAP,
                "&e线路图",
                List.of(" &7线路: &f" + tm.allLines().size(),
                        " &7车站: &f" + tm.allStations().size(),
                        "",
                        "&e▸ 查看")));

        inv.setItem(SLOT_HISTORY, ECOSTerminalGUI.item(Material.BOOK,
                "&7乘车记录",
                List.of(" &7最近 " + plugin.getConfig().getInt("transit.history-size", 20) + " 条",
                        "",
                        "&7▸ 打开")));

        var top = tm.topRiders(1);
        inv.setItem(SLOT_BOARD, ECOSTerminalGUI.item(Material.GOLDEN_HELMET,
                "&e轨道交通排行",
                List.of(top.isEmpty() ? " &8暂无数据" : " &7榜首: &f" + top.get(0).name()
                                + " &8" + top.get(0).rides() + " 次",
                        " &7按乘次 / 票款",
                        "",
                        "&e▸ 打开")));

        boolean riding = j != null;
        inv.setItem(SLOT_INSURE, ECOSTerminalGUI.item(Material.SHIELD,
                riding ? "&a行程异常申报 &8免费" : "&7行程异常申报 &8免费",
                List.of(" &7车辆异常、连接中断或未能完成行程时办理",
                        " &7经管理员核准后撤销未完成行程",
                        " &7不补收全程票",
                        riding ? " &a当前有未完成行程" : " &8当前没有进行中的行程",
                        "",
                        riding ? "&a▸ 提交申报" : "&8▸ 有行程时方可申报")));

        inv.setItem(SLOT_HELP, ECOSTerminalGUI.item(Material.WRITABLE_BOOK, "&f乘车须知",
                List.of(" &71. 请先开通闪付，或领取交通卡 / 购买单程票",
                        " &72. 闪付须空手右键闸机；亦可持卡或单程票检票",
                        " &73. 进站后十五分钟内于本站再次刷卡，可撤销且不产生费用",
                        " &74. 抵达目的地后再次刷卡出站，按最短路径计费",
                        " &75. 换乘通道请勿刷卡，以免按出站结算",
                        " &76. 无进站记录、未出站再进站、线路未连接：闸机不扣全程。请至补票处或售票机补票后再出站",
                        " &77. 行程超时会自动失效；出站仍须补票。亦可至本处办理行程异常申报")));

        if (player.hasPermission("es2uni.admin")) {
            inv.setItem(SLOT_ADMIN, ECOSTerminalGUI.item(Material.COMMAND_BLOCK,
                    "&c管理 · 线路连接",
                    List.of(" &7点线路 → 连边 / 删线",
                            " &7点车站 → 搬家 / 所属线 / 删除",
                            "",
                            "&c▸ 打开管理面板")));
        }

        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回管理处", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    private String fmt(double v) {
        return plugin.getVaultHook().isEnabled() ? plugin.getVaultHook().format(v) : String.format("%.2f", v);
    }
}
