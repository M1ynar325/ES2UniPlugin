package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.InsuranceManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** 市政：招工 / 清理 / 保险 / 交通。 */
public class MunicipalOfficeGUI {

    public static final String TITLE = EcosStyle.hub("市政");

    public static final int SLOT_JOBS = 0;
    public static final int SLOT_CLEANUP = 1;
    public static final int SLOT_RECYCLE = 2;
    public static final int SLOT_INSURANCE = 3;
    public static final int SLOT_TRANSIT = 4;
    public static final int SLOT_PVE = 5;
    public static final int SLOT_BACK = 8;

    private final ES2UniPlugin plugin;

    public MunicipalOfficeGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        ECOSTerminalGUI.fillTabBar(inv);

        int jobs = plugin.getJobBoardManager().listActive().size();
        inv.setItem(SLOT_JOBS, ECOSTerminalGUI.item(Material.IRON_PICKAXE, "&e招工",
                List.of(" &7进行中 &f" + jobs, "", "&e▸ 打开")));

        inv.setItem(SLOT_CLEANUP, ECOSTerminalGUI.item(Material.BUCKET, "&a清理",
                List.of(" &7清附近掉落物", " &7进回收站约 1 天", "", "&a▸ 发起投票")));

        int bin = plugin.getRecycleBinManager().countFor(player.getUniqueId());
        inv.setItem(SLOT_RECYCLE, ECOSTerminalGUI.item(Material.CAULDRON, "&2回收站",
                List.of(" &7我的物品 &f" + bin, "", "&2▸ 打开")));

        InsuranceManager.Backup bak = plugin.getInsuranceManager().getBackup(player.getUniqueId());
        inv.setItem(SLOT_INSURANCE, ECOSTerminalGUI.item(Material.SHIELD, EcosStyle.COBALT + "保险",
                List.of(bak == null ? " &8尚无备份" : " &7最近 &f" + bak.timeLabel(),
                        "", EcosStyle.COBALT + "▸ 打开")));

        int lines = plugin.getTransitManager() == null ? 0 : plugin.getTransitManager().allLines().size();
        String ride = plugin.getTransitManager() == null ? null
                : plugin.getTransitManager().journeyHint(player.getUniqueId());
        inv.setItem(SLOT_TRANSIT, ECOSTerminalGUI.item(Material.POWERED_RAIL, EcosStyle.COBALT + "交通",
                List.of(" &7线路 &f" + lines,
                        ride == null ? " &8当前无行程" : " " + EcosStyle.JIQING + ride,
                        "", EcosStyle.COBALT + "▸ 打开")));

        if (player.hasPermission("es2uni.admin")) {
            inv.setItem(SLOT_PVE, ECOSTerminalGUI.item(Material.BLAZE_ROD, "&cPVE 棒",
                    List.of(" &7右键刷、潜行换种类", "", "&c▸ 领取")));
        }

        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        player.openInventory(inv);
    }
}
