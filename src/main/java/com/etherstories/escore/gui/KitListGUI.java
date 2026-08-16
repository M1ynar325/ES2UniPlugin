package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.KitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/** 玩家 Kit 领取列表（本地 + Essentials） */
public class KitListGUI {

    public static final String TITLE = ColorUtil.colorize("&6&lKit 领取");
    public static final int BACK_SLOT  = 52;
    public static final int CLOSE_SLOT = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<String>> entries = new HashMap<>();

    public KitListGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        ItemStack bg = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        List<String> list = new ArrayList<>();

        // Essentials kits first
        if (plugin.getEssentialsHook().isEnabled()) {
            for (String name : plugin.getEssentialsHook().getKitNames()) {
                list.add("ess:" + name);
            }
        }
        for (String name : plugin.getKitManager().names()) {
            list.add("local:" + name);
        }
        entries.put(player.getUniqueId(), list);

        for (int i = 0; i < Math.min(list.size(), 45); i++) {
            String entry = list.get(i);
            if (entry.startsWith("ess:")) {
                String name = entry.substring(4);
                inv.setItem(i, item(Material.ENDER_CHEST, "&b" + name + " &8[Essentials]",
                        List.of("&7点击领取 Essentials Kit",
                                "&8/ecos kit " + name)));
            } else {
                String name = entry.substring(6);
                KitManager.KitDef def = plugin.getKitManager().get(name);
                if (def == null) continue;
                inv.setItem(i, item(Material.CHEST, "&e" + name + " &8[ECOS]",
                        List.of(
                                " &7规则: &f" + plugin.getKitManager().describeRule(def),
                                " &7状态: " + plugin.getKitManager().claimStatus(player, def),
                                " &7物品: &f" + def.items.size() + " 件",
                                "",
                                "&a点击领取"
                        )));
            }
        }

        inv.setItem(BACK_SLOT, item(Material.ARROW, "&7返回终端", List.of()));
        inv.setItem(CLOSE_SLOT, item(Material.BARRIER, "&c关闭", List.of()));
        player.openInventory(inv);
    }

    public String getAt(Player player, int slot) {
        List<String> list = entries.get(player.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size() || slot >= 45) return null;
        return list.get(slot);
    }

    public void cleanup(Player player) {
        entries.remove(player.getUniqueId());
    }

    private static ItemStack pane(Material m) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); i.setItemMeta(meta); }
        return i;
    }

    private static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            List<String> colored = new ArrayList<>();
            for (String l : lore) colored.add(ColorUtil.colorize(l));
            meta.setLore(colored);
            i.setItemMeta(meta);
        }
        return i;
    }
}
