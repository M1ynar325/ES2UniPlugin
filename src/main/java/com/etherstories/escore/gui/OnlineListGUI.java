package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class OnlineListGUI {

    public static final String TITLE      = "&8在线玩家";
    public static final int    CLOSE_SLOT = 53;
    public static final int    BACK_SLOT  = 45;
    private static final int   SIZE       = 54;
    private static final int   LIST_SIZE  = SIZE - 9;
    private static final Material BG      = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin            plugin;
    private final Map<UUID, Map<Integer, UUID>> slotMaps = new HashMap<>();

    public OnlineListGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer) {
        Inventory inv = EcosHolder.of("online", SIZE, ColorUtil.colorize(TITLE));

        ItemStack bg = ECOSTerminalGUI.bg(BG);
        for (int i = LIST_SIZE; i < SIZE; i++) inv.setItem(i, bg);

        Map<Integer, UUID> slotMap = new HashMap<>();
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(viewer) || slot >= LIST_SIZE) continue;
            inv.setItem(slot, buildHead(target));
            slotMap.put(slot, target.getUniqueId());
            slot++;
        }

        if (slot == 0)
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7无其他在线玩家", null));

        slotMaps.put(viewer.getUniqueId(), slotMap);

        inv.setItem(BACK_SLOT,  ECOSTerminalGUI.item(Material.ARROW,   "&7返回终端", null));
        inv.setItem(CLOSE_SLOT, ECOSTerminalGUI.item(Material.BARRIER, "&7关闭",     null));
        EcosHolder.open(viewer, inv);
    }

    private ItemStack buildHead(Player target) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;
        meta.setOwningPlayer(target);
        meta.setDisplayName(ColorUtil.colorize("&f" + target.getName()));
        meta.setLore(List.of(
                ColorUtil.colorize("&7延迟: &f" + target.getPing() + "ms"),
                ColorUtil.colorize(""),
                ColorUtil.colorize("&8点击查看操作")));
        skull.setItemMeta(meta);
        return skull;
    }

    public UUID getTargetAt(Player viewer, int slot) {
        Map<Integer, UUID> map = slotMaps.get(viewer.getUniqueId());
        return map != null ? map.get(slot) : null;
    }

    public boolean isListSlot(int slot) { return slot >= 0 && slot < LIST_SIZE; }

    public void cleanup(Player viewer) { slotMaps.remove(viewer.getUniqueId()); }
}
