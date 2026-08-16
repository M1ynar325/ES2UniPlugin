package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.WaypointManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WaypointGUI {

    public static final String TITLE      = "&8坐标收藏";
    public static final int    CLOSE_SLOT = 53;
    public static final int    BACK_SLOT  = 45;
    public static final int    ADD_SLOT   = 49;
    private static final int   SIZE       = 54;
    private static final int   LIST_SIZE  = SIZE - 9;
    private static final Material BG      = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin plugin;

    public WaypointGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, ColorUtil.colorize(TITLE));

        ItemStack bg = ECOSTerminalGUI.bg(BG);
        for (int i = LIST_SIZE; i < SIZE; i++) inv.setItem(i, bg);

        List<WaypointManager.Waypoint> list = plugin.getWaypointManager().get(player.getUniqueId());
        for (int i = 0; i < list.size(); i++) {
            WaypointManager.Waypoint wp = list.get(i);
            inv.setItem(i, ECOSTerminalGUI.item(Material.COMPASS,
                    "&f" + wp.name(),
                    List.of(ColorUtil.colorize("&8" + wp.world()),
                            ColorUtil.colorize("&7" + wp.x() + ", " + wp.y() + ", " + wp.z()),
                            ColorUtil.colorize(""),
                            ColorUtil.colorize("&8左键: 传送   右键: 删除"))));
        }

        if (list.isEmpty())
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7暂无收藏坐标", null));

        inv.setItem(ADD_SLOT, ECOSTerminalGUI.item(Material.BEACON,
                "&7添加当前位置", List.of(
                        ColorUtil.colorize("&8点击后输入名称"),
                        ColorUtil.colorize("&8最多 " + plugin.getWaypointManager().maxPerPlayer() + " 个"))));

        inv.setItem(BACK_SLOT,  ECOSTerminalGUI.item(Material.ARROW,   "&7返回终端", null));
        inv.setItem(CLOSE_SLOT, ECOSTerminalGUI.item(Material.BARRIER, "&7关闭",     null));
        player.openInventory(inv);
    }

    public boolean isListSlot(int slot, Player player) {
        return slot >= 0 && slot < LIST_SIZE
                && slot < plugin.getWaypointManager().get(player.getUniqueId()).size();
    }

    public void teleport(Player player, int slot) {
        List<WaypointManager.Waypoint> list = plugin.getWaypointManager().get(player.getUniqueId());
        if (slot < 0 || slot >= list.size()) return;
        WaypointManager.Waypoint wp = list.get(slot);
        World world = Bukkit.getWorld(wp.world());
        if (world == null) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7世界不存在: " + wp.world()));
            return;
        }
        player.teleport(new Location(world, wp.x() + 0.5, wp.y(), wp.z() + 0.5));
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已传送到 &f" + wp.name()));
    }

    public void delete(Player player, int slot) {
        plugin.getWaypointManager().removeByIndex(player.getUniqueId(), slot);
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已删除"));
        open(player);
    }
}
