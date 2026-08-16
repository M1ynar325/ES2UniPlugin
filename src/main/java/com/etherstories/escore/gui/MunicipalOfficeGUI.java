package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.InsuranceManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** ECOS 管理处总入口 */
public class MunicipalOfficeGUI {

    public static final String TITLE = ColorUtil.colorize("&9&l管理处");

    public static final int SLOT_JOBS = 11;
    public static final int SLOT_CLEANUP = 13;
    public static final int SLOT_RECYCLE = 14;
    public static final int SLOT_INSURANCE = 15;
    public static final int SLOT_TRANSIT = 29;
    public static final int SLOT_PVE = 31;
    public static final int SLOT_BACK = 48;
    public static final int SLOT_CLOSE = 49;

    private final ES2UniPlugin plugin;

    public MunicipalOfficeGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.BLUE_STAINED_GLASS_PANE));

        int jobs = plugin.getJobBoardManager().listActive().size();
        inv.setItem(SLOT_JOBS, ECOSTerminalGUI.item(Material.IRON_PICKAXE,
                "&e招工处",
                List.of(" &7进行中委托: &f" + jobs,
                        " &7个人 / 官方建设委托",
                        " &7托管报酬 · 完成后打款",
                        "",
                        "&e▸ 点击进入")));

        inv.setItem(SLOT_CLEANUP, ECOSTerminalGUI.item(Material.BUCKET,
                "&a垃圾实体清理",
                List.of(" &7申请清理附近掉落物/箭矢",
                        " &7掉落物进入回收站（约1天）",
                        " &7全服广播 · 点击投票",
                        "",
                        "&a▸ 发起申请")));

        int bin = plugin.getRecycleBinManager().countFor(player.getUniqueId());
        int binAll = plugin.getRecycleBinManager().totalAlive();
        inv.setItem(SLOT_RECYCLE, ECOSTerminalGUI.item(Material.CAULDRON,
                "&2回收站",
                List.of(" &7我的物品: &f" + bin,
                        player.hasPermission("es2uni.admin")
                                ? " &7全站暂存: &f" + binAll : "",
                        " &7清理掉落物可在此取回",
                        " &8保存约 1 天",
                        "",
                        "&2▸ 打开回收站")));

        InsuranceManager.Backup bak = plugin.getInsuranceManager().getBackup(player.getUniqueId());
        int pending = plugin.getInsuranceManager().pendingClaims().size();
        inv.setItem(SLOT_INSURANCE, ECOSTerminalGUI.item(Material.SHIELD,
                "&b保险柜",
                List.of(" &7背包清空险 &a免费",
                        bak == null ? " &8尚无备份"
                                : " &7最近备份: &f" + bak.timeLabel() + " &8(" + bak.itemCount() + "件)",
                        player.hasPermission("es2uni.admin")
                                ? " &e待审理赔: &f" + pending : "",
                        "",
                        "&b▸ 点击打开")));

        int lines = plugin.getTransitManager() == null ? 0 : plugin.getTransitManager().allLines().size();
        int stops = plugin.getTransitManager() == null ? 0 : plugin.getTransitManager().allStations().size();
        String ride = plugin.getTransitManager() == null ? null
                : plugin.getTransitManager().journeyHint(player.getUniqueId());
        inv.setItem(SLOT_TRANSIT, ECOSTerminalGUI.item(Material.POWERED_RAIL,
                "&b交通处",
                List.of(" &7线路 &f" + lines + " &7· 车站 &f" + stops,
                        ride == null ? " &8当前无行程" : " &b" + ride,
                        " &7闸机 · 单程票 · 交通卡 · 闪付",
                        "",
                        "&b▸ 进入交通处")));

        if (player.hasPermission("es2uni.admin")) {
            inv.setItem(SLOT_PVE, ECOSTerminalGUI.item(Material.BLAZE_ROD,
                    "&cPVE 刷怪棒",
                    List.of(" &7刷怪棒 + 精英棒",
                            " &7右键刷、潜行换种类",
                            " &8/ecos pve",
                            "",
                            "&c▸ 领取")));
        }

        inv.setItem(4, ECOSTerminalGUI.item(Material.BEACON,
                "&9&l市政管理处",
                List.of(" &7招工 · 城管 · 保险 · 交通",
                        " &8EtherStories 市民服务")));

        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }
}
