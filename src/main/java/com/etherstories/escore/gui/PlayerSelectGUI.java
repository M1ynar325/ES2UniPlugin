package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerSelectGUI {

    public enum SelectContext { TPA, TPA_HERE, PAY, MAIL }

    private static final int SIZE = 54;
    private static final int BACK_SLOT  = SIZE - 5;
    private static final int CLOSE_SLOT = SIZE - 1;
    private static final Material BG    = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin plugin;
    // Tracks which context each player's GUI is in
    private final Map<UUID, SelectContext> contexts = new HashMap<>();
    // Slot → displayed player UUID mapping per viewer
    private final Map<UUID, Map<Integer, UUID>> slotMaps = new HashMap<>();

    public PlayerSelectGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, SelectContext context) {
        contexts.put(viewer.getUniqueId(), context);

        String titleKey = switch (context) {
            case TPA      -> plugin.getConfigManager().getTpaSelectTitle();
            case TPA_HERE -> plugin.getConfigManager().getTpaHereSelectTitle();
            case PAY      -> plugin.getConfigManager().getPaySelectTitle();
            case MAIL     -> "&7选择留言对象";
        };
        Inventory inv = Bukkit.createInventory(null, SIZE, ColorUtil.colorize(titleKey));

        ItemStack bg = ECOSTerminalGUI.item(BG, " ", null);
        for (int i = SIZE - 9; i < SIZE; i++) inv.setItem(i, bg);

        Map<Integer, UUID> slotMap = new HashMap<>();
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(viewer)) continue;
            if (slot >= SIZE - 9) break;
            inv.setItem(slot, buildHead(target, context));
            slotMap.put(slot, target.getUniqueId());
            slot++;
        }

        if (slot == 0) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER,
                    ColorUtil.colorize("&7没有其他在线玩家"), null));
        }

        slotMaps.put(viewer.getUniqueId(), slotMap);

        inv.setItem(BACK_SLOT,  ECOSTerminalGUI.item(Material.ARROW, ColorUtil.colorize("&f返回终端"), null));
        inv.setItem(CLOSE_SLOT, ECOSTerminalGUI.item(Material.BARRIER, ColorUtil.colorize("&c关闭"), null));

        viewer.openInventory(inv);
    }

    private ItemStack buildHead(Player target, SelectContext context) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;
        meta.setOwningPlayer(target);
        meta.setDisplayName(ColorUtil.colorize("&f" + target.getName()));
        String action = switch (context) {
            case TPA      -> "&8点击: 请求传送至该玩家";
            case TPA_HERE -> "&8点击: 请求该玩家传送至你";
            case PAY      -> "&8点击: 向该玩家转账";
            case MAIL     -> "&8点击: 给该玩家留言";
        };
        meta.setLore(List.of(
                ColorUtil.colorize("&7延迟: &f" + target.getPing() + "ms"),
                "",
                ColorUtil.colorize(action)));
        skull.setItemMeta(meta);
        return skull;
    }

    /** Returns the UUID of the player at the clicked slot, or null. */
    public UUID getTargetAt(Player viewer, int slot) {
        Map<Integer, UUID> map = slotMaps.get(viewer.getUniqueId());
        return map != null ? map.get(slot) : null;
    }

    public SelectContext getContext(Player viewer) {
        return contexts.get(viewer.getUniqueId());
    }

    public void cleanup(Player viewer) {
        contexts.remove(viewer.getUniqueId());
        slotMaps.remove(viewer.getUniqueId());
    }

    public static int getBackSlot()  { return BACK_SLOT;  }
    public static int getCloseSlot() { return CLOSE_SLOT; }
}
