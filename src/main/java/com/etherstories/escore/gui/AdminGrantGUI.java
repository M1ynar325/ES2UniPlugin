package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** 管理：发补签券 / 幸运方块次数。 */
public class AdminGrantGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l发放");
    public static final int SLOT_MAKEUP = 11;
    public static final int SLOT_LUCKY = 15;
    public static final int SLOT_BACK = 22;
    public static final int SLOT_CLOSE = 26;

    private final ES2UniPlugin plugin;

    public AdminGrantGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        inv.setItem(SLOT_MAKEUP, ECOSTerminalGUI.item(Material.PAPER, "&e补签券",
                List.of(" &7聊天输入: &f玩家 数量",
                        " &8离线也可（进过服）",
                        "", "&e▸ 发放")));
        inv.setItem(SLOT_LUCKY, ECOSTerminalGUI.item(Material.GOLD_BLOCK, "&d" + plugin.getLuckyBlockManager().displayName(),
                List.of(" &7聊天输入: &f玩家 数量",
                        " &8额外抽奖次数",
                        "", "&d▸ 发放")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }
}
