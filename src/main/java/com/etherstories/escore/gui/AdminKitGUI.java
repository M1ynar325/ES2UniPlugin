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

/** 管理员：Kit 列表 / 新建 / 删除入口 */
public class AdminKitGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l⚙ Kit 管理");
    public static final int CREATE_SLOT = 45;
    public static final int INFO_SLOT   = 49;
    public static final int BACK_SLOT   = 52;
    public static final int CLOSE_SLOT  = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<String>> pageKits = new HashMap<>();

    public AdminKitGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        ItemStack bg = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        List<String> names = plugin.getKitManager().names();
        pageKits.put(player.getUniqueId(), names);

        for (int i = 0; i < Math.min(names.size(), 45); i++) {
            String name = names.get(i);
            KitManager.KitDef def = plugin.getKitManager().get(name);
            if (def == null) continue;
            inv.setItem(i, item(Material.CHEST,
                    "&e" + name,
                    List.of(
                            " &7规则: &f" + plugin.getKitManager().describeRule(def),
                            " &7物品数: &f" + def.items.size(),
                            "",
                            "&a左键 &7编辑",
                            "&c右键 &7删除"
                    )));
        }

        inv.setItem(CREATE_SLOT, item(Material.EMERALD, "&a新建 Kit",
                List.of("&7点击后用铁砧输入名称")));
        inv.setItem(INFO_SLOT, item(Material.BOOK, "&7说明",
                List.of(
                        "&7自有 Kit 存于 plugins/ES2UniPlugin/kits.yml",
                        "&7玩家用 &f/ecos kit <名> &7领取",
                        "&7若 Essentials 有同名 kit，优先走 Essentials"
                )));
        inv.setItem(BACK_SLOT, item(Material.ARROW, "&7返回终端", List.of()));
        inv.setItem(CLOSE_SLOT, item(Material.BARRIER, "&c关闭", List.of()));
        player.openInventory(inv);
    }

    public String getAt(Player player, int slot) {
        List<String> list = pageKits.get(player.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size() || slot >= 45) return null;
        return list.get(slot);
    }

    public void cleanup(Player player) {
        pageKits.remove(player.getUniqueId());
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
