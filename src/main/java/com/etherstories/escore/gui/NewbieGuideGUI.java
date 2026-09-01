package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.NewbieGuideManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class NewbieGuideGUI {

    public static final String TITLE = ColorUtil.colorize("&a&l新手引导");

    public static final int SLOT_BACK = 45;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;

    public NewbieGuideGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = EcosHolder.of("newbie", 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.LIME_STAINED_GLASS_PANE));

        NewbieGuideManager ng = plugin.getNewbieGuideManager();
        NewbieGuideManager.Step[] steps = NewbieGuideManager.Step.values();
        inv.setItem(4, ECOSTerminalGUI.item(Material.BOOK,
                "&a上手清单 &8" + ng.progress(player.getUniqueId()) + "%",
                List.of("&7建议前 30 分钟完成必做项",
                        "&7点击条目可手动勾选/取消",
                        "&8可选步骤不影响完成度")));

        for (int i = 0; i < steps.length; i++) {
            NewbieGuideManager.Step step = steps[i];
            boolean done = ng.isDone(player.getUniqueId(), step);
            boolean optional = step == NewbieGuideManager.Step.ADD_FRIEND
                    || step == NewbieGuideManager.Step.SET_HOME;
            inv.setItem(9 + i, ECOSTerminalGUI.item(
                    done ? Material.LIME_CONCRETE : Material.YELLOW_CONCRETE,
                    (done ? "&a✔ " : "&e○ ") + step.title + (optional ? " &8(可选)" : ""),
                    List.of("&7" + step.tip, "", "&8▸ 点击切换勾选")));
        }

        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }
}
