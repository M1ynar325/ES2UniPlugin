package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class HomeListGUI {

    private static final Material BG = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin plugin;

    public HomeListGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<String> homes = plugin.getEssentialsHook().getHomes(player);

        int rows = Math.max(2, (int) Math.ceil((homes.size() + 9) / 9.0));
        rows = Math.min(rows, 6);
        int size = rows * 9;

        String title = ColorUtil.colorize(plugin.getConfigManager().getHomeGUITitle());
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill last row with bg
        ItemStack bg = ECOSTerminalGUI.item(BG, " ", null);
        for (int i = size - 9; i < size; i++) inv.setItem(i, bg);

        // Home items
        for (int i = 0; i < Math.min(homes.size(), size - 9); i++) {
            String name = homes.get(i);
            inv.setItem(i, ECOSTerminalGUI.item(Material.RED_BED,
                    ColorUtil.colorize("&d" + name),
                    List.of(ColorUtil.colorize("&7点击传送至此家园"),
                            ColorUtil.colorize("&8名称: &f" + name))));
        }

        if (homes.isEmpty()) {
            inv.setItem(4, ECOSTerminalGUI.item(Material.BARRIER,
                    ColorUtil.colorize("&7还没有设置家"),
                    List.of(ColorUtil.colorize("&8使用 /sethome 设置"))));
        }

        // Controls
        int backSlot = size - 5;
        int closeSlot = size - 1;
        inv.setItem(backSlot, ECOSTerminalGUI.item(Material.ARROW,
                ColorUtil.colorize("&f返回终端"), null));
        inv.setItem(closeSlot, ECOSTerminalGUI.item(Material.BARRIER,
                ColorUtil.colorize("&c关闭"), null));

        player.openInventory(inv);
    }

    public static int getBackSlot(int size)  { return size - 5; }
    public static int getCloseSlot(int size) { return size - 1; }
}
