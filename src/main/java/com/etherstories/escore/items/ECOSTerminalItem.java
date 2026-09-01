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
        try { meta.setEnchantmentGlintOverride(false); } catch (Throwable ignored) {}
        try {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ENCHANTS);
        } catch (Throwable ignored) {
            try { meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS); } catch (Throwable ignored2) {}
        }
        if (key != null) meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isTerminalItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (key != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE))
            return true;
        String name = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
        if (name != null && name.contains("ECOS")) return true;
        var lore = meta.getLore();
        if (lore == null) return false;
        for (String line : lore) {
            String s = org.bukkit.ChatColor.stripColor(line);
            if (s != null && (s.contains("右键接入") || s.contains("Etharia Central"))) return true;
        }
        return false;
    }
}
