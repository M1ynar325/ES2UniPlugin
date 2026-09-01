package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 其它箱子 GUI 仍用这里的 item/bg 帮手。
 * 主终端 /ecos menu 已改走 {@link com.etherstories.escore.gui.terminal.TerminalHub}，不再开箱子。
 */
public class ECOSTerminalGUI {

    public enum Page {
        OVERVIEW("overview"), SOCIAL("social"), TRAVEL("travel"),
        CITY("city"), PLAY("play"), ADMIN("admin");
        public final String id;
        Page(String id) { this.id = id; }
    }

    public static final int SLOT_CLOSE = 8;

    private final ES2UniPlugin plugin;

    public ECOSTerminalGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        try {
            var hub = plugin.getTerminalHub();
            if (hub == null) {
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &c终端未就绪"));
                return;
            }
            hub.open(player);
        } catch (Throwable t) {
            plugin.getLogger().warning("打开终端失败: " + t);
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c终端打开失败，看后台日志"));
        }
    }

    public void open(Player player, Page page) {
        try {
            var hub = plugin.getTerminalHub();
            if (hub == null) {
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &c终端未就绪"));
                return;
            }
            hub.open(player, page == null ? null : page.id);
        } catch (Throwable t) {
            plugin.getLogger().warning("打开终端失败: " + t);
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c终端打开失败，看后台日志"));
        }
    }

    public void open(Player player, Page page, boolean ignored) {
        open(player, page);
    }

    public void cleanup(Player player) {
        plugin.getTerminalHub().close(player);
    }

    public void playClick(Player player) {
        plugin.getTerminalHub().playClick(player);
    }

    public static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(ColorUtil.colorize(name));
        if (lore != null)
            meta.setLore(lore.stream().map(ColorUtil::colorize).toList());
        try {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        } catch (Throwable ignored) {
            try { meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS); } catch (Throwable ignored2) {}
        }
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack glint(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setEnchantmentGlintOverride(true);
        stack.setItemMeta(meta);
        return stack;
    }

    static void fillTabBar(Inventory inv) {
        ItemStack pane = EcosStyle.chrome();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane);
        }
    }

    static void fillEmpty(Inventory inv) {
        fillTabBar(inv);
    }

    static ItemStack bg(Material mat) {
        ItemStack s = new ItemStack(mat);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName(" ");
            m.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            s.setItemMeta(m);
        }
        return s;
    }

    public static boolean isCloseSlot(int slot) { return slot == SLOT_CLOSE; }
}
