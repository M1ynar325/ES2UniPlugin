package com.etherstories.escore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemChat {
    public static final String TOKEN = "[i]";

    private ItemChat() {}

    public static boolean hasToken(String s) {
        if (s == null) return false;
        return indexOf(s, 0) >= 0;
    }

    public static Component replace(String plain, ItemStack item) {
        Component piece = itemPart(item);
        Component out = Component.empty();
        int i = 0;
        while (i < plain.length()) {
            int idx = indexOf(plain, i);
            if (idx < 0) {
                out = out.append(Component.text(plain.substring(i)));
                break;
            }
            if (idx > i) out = out.append(Component.text(plain.substring(i, idx)));
            out = out.append(piece);
            i = idx + TOKEN.length();
        }
        return out;
    }

    public static Component itemPart(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Component.text("[空手]");
        }
        String name = display(item);
        String label = item.getAmount() > 1
                ? "[" + name + " x" + item.getAmount() + "]"
                : "[" + name + "]";
        try {
            return Component.text(label).hoverEvent(item.asHoverEvent());
        } catch (Throwable t) {
            return Component.text(label)
                    .hoverEvent(HoverEvent.showText(Component.text(name)));
        }
    }

    public static String display(ItemStack item) {
        if (item == null) return "?";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName())
            return ChatColor.stripColor(meta.getDisplayName());
        if (meta != null && meta.hasItemName())
            return ChatColor.stripColor(meta.getItemName());
        String n = item.getType().name().toLowerCase().replace('_', ' ');
        return n;
    }

    private static int indexOf(String s, int from) {
        int n = s.length();
        for (int i = from; i <= n - 3; i++) {
            char a = s.charAt(i);
            if (a != '[' ) continue;
            if ((s.charAt(i + 1) == 'i' || s.charAt(i + 1) == 'I') && s.charAt(i + 2) == ']') return i;
        }
        return -1;
    }
}
