package com.etherstories.escore.items;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.gui.EcosStyle;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ECOSTerminalItem {

    private static NamespacedKey key;

    public static void init(ES2UniPlugin plugin) {
        key = new NamespacedKey(plugin, "ecos_terminal");
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ColorUtil.colorize(EcosStyle.JIQING + "ECOS"));
        meta.setLore(List.of(
                ColorUtil.colorize(EcosStyle.SNOW + "Etharia Central OS"),
                ColorUtil.colorize(EcosStyle.MIST + "静谧 · 秩序"),
                ColorUtil.colorize(""),
                ColorUtil.colorize(EcosStyle.COBALT + "右键接入")
        ));
        meta.setEnchantmentGlintOverride(false);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isTerminalItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
