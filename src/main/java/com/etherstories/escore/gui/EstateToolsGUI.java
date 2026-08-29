package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** 登记房间：①棒 ②对角 ③写门牌 ④门口 ⑤牌子。 */
public class EstateToolsGUI {

    public static final String TITLE = EcosStyle.hub("登记");
    public static final int SLOT_WAND = 0;
    public static final int SLOT_SEL = 1;
    public static final int SLOT_REGISTER = 2;
    public static final int SLOT_DOOR = 3;
    public static final int SLOT_SIGN = 4;
    public static final int SLOT_HELP = 5;
    public static final int SLOT_BACK = 8;

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
        boolean hasWand = com.etherstories.escore.items.EstateWand.hasWand(player);
        String feeStr = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(fee) : String.format("%.0f", fee);

        inv.setItem(SLOT_WAND, ECOSTerminalGUI.item(Material.WOODEN_AXE, "&a① 选区棒",
                List.of("&8左键一角  右键对角（含屋顶）",
                        hasWand ? " &a已在背包" : " &e还没领",
                        "", "&a▸ 领取，关掉后再点方块")));
        inv.setItem(SLOT_SEL, ECOSTerminalGUI.item(
                ready ? Material.LIME_CONCRETE : (hasP1 ? Material.YELLOW_CONCRETE : Material.GRAY_CONCRETE),
                ready ? "&a② 已圈好" : (hasP1 ? "&e② 再点对角" : "&7② 选区"),
                List.of(ready ? " &a" + sel.asProbe().volume() + " 格" : (hasP1 ? " &e点1已有，右键屋顶对角" : " &8先领棒去点方块"),
                        "", "&7▸ 看粒子框")));
        inv.setItem(SLOT_REGISTER, ECOSTerminalGUI.item(Material.WRITABLE_BOOK, "&6③ 写门牌",
                List.of("&8输入: 楼名 层 号 用途",
                        "&8例如: 星港一号 3 301 house",
                        fee > 0 ? " &e住宅收 " + feeStr + "  商铺免费" : " &8注册费已关",
                        ready ? " &a可以登记了" : " &c先完成①②",
                        "", "&6▸ 开始")));
        inv.setItem(SLOT_DOOR, ECOSTerminalGUI.item(Material.OAK_DOOR, "&7④ 设门口",
                List.of("&8站在自己房间里点", "&8以后传送落在这里", "", "&7▸ 设传送点")));
        inv.setItem(SLOT_SIGN, ECOSTerminalGUI.item(Material.OAK_SIGN, "&7⑤ 挂牌子",
                List.of("&8看着墙点", "", "&7▸ 放门牌")));
        inv.setItem(SLOT_HELP, ECOSTerminalGUI.item(Material.WRITTEN_BOOK, "&f指南",
                List.of("&8注册和买卖说明", "", "&f▸ 打开")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回房产", null));
        player.openInventory(inv);
    }
}
