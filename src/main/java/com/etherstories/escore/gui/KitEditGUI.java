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

/** 管理员编辑单个 Kit：上方放物品，底栏改规则并保存 */
public class KitEditGUI {

    public static final String TITLE_PREFIX = ColorUtil.colorize("&e&l编辑 Kit &8| &f");

    public static final int SLOT_MODE     = 45;
    public static final int SLOT_COOLDOWN = 46;
    public static final int SLOT_IMPORT   = 48;
    public static final int SLOT_SAVE     = 50;
    public static final int SLOT_BACK     = 52;
    public static final int SLOT_CLOSE    = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, String> editing = new HashMap<>();

    public KitEditGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String kitName) {
        KitManager.KitDef def = plugin.getKitManager().get(kitName);
        if (def == null) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7套件不存在"));
            return;
        }
        editing.put(player.getUniqueId(), def.name);
        Inventory inv = EcosHolder.of("kit-edit", 54, TITLE_PREFIX + def.name);

        for (int i = 0; i < Math.min(def.items.size(), 45); i++)
            inv.setItem(i, def.items.get(i).clone());

        for (int i = 45; i < 54; i++)
            inv.setItem(i, pane(Material.GRAY_STAINED_GLASS_PANE));

        refreshControls(inv, def);
        EcosHolder.open(player, inv);
    }

    public boolean isEditing(Player player) {
        return editing.containsKey(player.getUniqueId());
    }

    public String getEditing(Player player) {
        return editing.get(player.getUniqueId());
    }

    public void cleanup(Player player) {
        editing.remove(player.getUniqueId());
    }

    public void refreshControls(Inventory inv, KitManager.KitDef def) {
        boolean once = def.claimMode == KitManager.ClaimMode.ONCE;
        inv.setItem(SLOT_MODE, item(
                once ? Material.IRON_DOOR : Material.CLOCK,
                once ? "&c领取规则: 每人一次" : "&a领取规则: 冷却重复",
                List.of("&7点击切换",
                        "&8当前: &f" + plugin.getKitManager().describeRule(def))));

        inv.setItem(SLOT_COOLDOWN, item(Material.REPEATER,
                "&e冷却时间: &f" + KitManager.formatDuration(def.cooldownSeconds),
                List.of("&7点击后在聊天输入秒数",
                        "&7例如 &f3600 &7= 1小时",
                        "&8仅冷却模式下生效")));

        inv.setItem(SLOT_IMPORT, item(Material.HOPPER,
                "&b从背包导入",
                List.of("&7用你当前背包内容覆盖本套件物品",
                        "&7（不含盔甲栏/副手）")));

        inv.setItem(SLOT_SAVE, item(Material.LIME_CONCRETE,
                "&a保存",
                List.of("&7保存上方物品与规则")));

        inv.setItem(SLOT_BACK, item(Material.ARROW, "&7返回列表", List.of()));
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&c关闭", List.of()));
    }

    public void captureItems(Player player, Inventory inv) {
        String name = editing.get(player.getUniqueId());
        if (name == null) return;
        KitManager.KitDef def = plugin.getKitManager().get(name);
        if (def == null) return;

        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && !stack.getType().isAir())
                items.add(stack.clone());
        }
        def.items = items;
        plugin.getKitManager().saveKit(def);
    }

    public void toggleMode(Player player) {
        String name = editing.get(player.getUniqueId());
        if (name == null) return;
        KitManager.KitDef def = plugin.getKitManager().get(name);
        if (def == null) return;
        def.claimMode = def.claimMode == KitManager.ClaimMode.ONCE
                ? KitManager.ClaimMode.COOLDOWN
                : KitManager.ClaimMode.ONCE;
        if (def.claimMode == KitManager.ClaimMode.COOLDOWN && def.cooldownSeconds <= 0)
            def.cooldownSeconds = 3600;
        plugin.getKitManager().saveKit(def);
    }

    public void toggleMode(Player player, Inventory inv) {
        toggleMode(player);
        String name = editing.get(player.getUniqueId());
        KitManager.KitDef def = name == null ? null : plugin.getKitManager().get(name);
        if (def != null && inv != null) refreshControls(inv, def);
    }

    public int importFromBackpack(Player player) {
        String name = editing.get(player.getUniqueId());
        if (name == null) return -1;
        KitManager.KitDef def = plugin.getKitManager().get(name);
        if (def == null) return -1;
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) continue;
            if (com.etherstories.escore.items.ECOSTerminalItem.isTerminalItem(stack)) continue;
            items.add(stack.clone());
        }
        def.items = items;
        plugin.getKitManager().saveKit(def);
        return items.size();
    }

    public void importFromInventory(Player player, Inventory inv) {
        String name = editing.get(player.getUniqueId());
        if (name == null) return;
        KitManager.KitDef def = plugin.getKitManager().get(name);
        if (def == null) return;

        int n = importFromBackpack(player);
        if (n < 0) return;
        List<ItemStack> items = def.items;

        for (int i = 0; i < 45; i++) inv.setItem(i, null);
        for (int i = 0; i < Math.min(items.size(), 45); i++)
            inv.setItem(i, items.get(i).clone());
        refreshControls(inv, def);
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已从背包导入 &f" + items.size() + " &7件物品"));
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
