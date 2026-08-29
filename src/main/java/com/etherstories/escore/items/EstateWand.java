package com.etherstories.escore.items;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class EstateWand {

    private static NamespacedKey key;

    private EstateWand() {}

    public static void init(ES2UniPlugin plugin) {
        key = new NamespacedKey(plugin, "estate_wand");
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ColorUtil.colorize("&a房产选区棒"));
        meta.setLore(List.of(
                ColorUtil.colorize("&8点到方块，不要站角落开菜单"),
                ColorUtil.colorize(""),
                ColorUtil.colorize("&a左键 &f一角"),
                ColorUtil.colorize("&a右键 &f对角（含屋顶）"),
                ColorUtil.colorize("&7圈好后打开登记页写门牌")
        ));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isWand(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public static boolean hasWand(Player player) {
        for (ItemStack it : player.getInventory().getContents()) {
            if (isWand(it)) return true;
        }
        return isWand(player.getInventory().getItemInOffHand());
    }

    public static void give(Player player) {
        if (hasWand(player)) return;
        player.getInventory().addItem(create()).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }
}
