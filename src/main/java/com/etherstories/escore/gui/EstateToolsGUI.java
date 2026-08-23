package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** 房产指令对应的按钮：点位、登记、门口、牌子、门牌。 */
public class EstateToolsGUI {

    public static final String TITLE = ColorUtil.colorize("&2&l房产操作");
    public static final int SLOT_POS1 = 0;
    public static final int SLOT_POS2 = 1;
    public static final int SLOT_REGISTER = 2;
    public static final int SLOT_DOOR = 3;
    public static final int SLOT_SIGN = 4;
    public static final int SLOT_HERE = 5;
    public static final int SLOT_HELP = 6;
    public static final int SLOT_SALE = 7;
    public static final int SLOT_CLOSE = 8;
    public static final int SLOT_MINE = 9;
    public static final int SLOT_BUILDINGS = 10;
    public static final int SLOT_SELL = 11;
    public static final int SLOT_UNSELL = 12;

    private final ES2UniPlugin plugin;

    public EstateToolsGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        boolean hasP1 = plugin.getEstateManager().hasPos1(player.getUniqueId());
        var sel = plugin.getEstateManager().selectionOf(player.getUniqueId());
        boolean ready = sel != null && sel.complete();
        double fee = plugin.getEstateManager().registerFee(false,
                com.etherstories.escore.estate.BuildingCategory.RESIDENTIAL,
                com.etherstories.escore.estate.UnitKind.HOUSE);

        inv.setItem(SLOT_POS1, ECOSTerminalGUI.item(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, "&a点1",
                List.of("&8站在房间一角点击", "", "&a▸ 记录当前位置")));
        inv.setItem(SLOT_POS2, ECOSTerminalGUI.item(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, "&a点2",
                List.of("&8站在对角（含高度）点击",
                        ready ? " &a已圈好 " + (sel == null ? "" : sel.asProbe().volume() + " 格")
                                : (hasP1 ? " &e已有点1，再点对角" : " &8先点1"),
                        "", "&a▸ 记录当前位置")));
        String feeStr = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(fee) : String.format("%.0f", fee);
        inv.setItem(SLOT_REGISTER, ECOSTerminalGUI.item(Material.WRITABLE_BOOK, "&6登记房产",
                List.of("&8聊天: 楼名 层 号 用途  &7楼名可空格",
                        "&8住宅/公寓/客房: house apartment hotel",
                        "&8商铺/工坊/仓库: shop workshop storage",
                        fee > 0 ? " &e住宅/公寓/客房收 " + feeStr : " &8住宅注册费已关",
                        " &a商铺/工坊/公共免费",
                        ready ? "&a圈地已完成" : "&c请先点1、点2",
                        "", "&6▸ 开始登记")));
        inv.setItem(SLOT_DOOR, ECOSTerminalGUI.item(Material.OAK_DOOR, "&7设门口",
                List.of("&8站在自己房间里点", "", "&7▸ 设为传送点")));
        inv.setItem(SLOT_SIGN, ECOSTerminalGUI.item(Material.OAK_SIGN, "&7写牌子",
                List.of("&8看墙点，写 ES2注册单位", "", "&7▸ 放置门牌")));
        inv.setItem(SLOT_HERE, ECOSTerminalGUI.item(Material.COMPASS, "&7当前门牌",
                List.of("&8站在房间里点", "", "&7▸ 查看")));
        inv.setItem(SLOT_HELP, ECOSTerminalGUI.item(Material.WRITTEN_BOOK, "&f房产指南",
                List.of("&8注册 / 买卖说明", "", "&f▸ 打开")));
        inv.setItem(SLOT_SALE, ECOSTerminalGUI.item(Material.GOLD_INGOT, "&e买房 · 待售",
                List.of(" &7挂牌: &f" + plugin.getEstateManager().listedForSale().size(),
                        "", "&e▸ 打开")));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        inv.setItem(SLOT_MINE, ECOSTerminalGUI.item(Material.CHEST, "&e我的房产",
                List.of("", "&e▸ 打开")));
        inv.setItem(SLOT_BUILDINGS, ECOSTerminalGUI.item(Material.DARK_OAK_DOOR, "&2楼盘",
                List.of("", "&2▸ 打开")));
        inv.setItem(SLOT_SELL, ECOSTerminalGUI.item(Material.GOLD_NUGGET, "&e上架当前房间",
                List.of("&8站在自己房子里点", "&8没标价会问金额", "", "&e▸ 挂牌出售")));
        inv.setItem(SLOT_UNSELL, ECOSTerminalGUI.item(Material.IRON_NUGGET, "&7下架当前房间",
                List.of("&8站在自己房子里点", "", "&7▸ 取消挂牌")));
        player.openInventory(inv);
    }
}
