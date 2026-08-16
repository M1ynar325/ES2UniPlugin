package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PayConfirmGUI {

    public static final int CONFIRM_SLOT = 11;
    public static final int CANCEL_SLOT  = 15;
    private static final int SIZE        = 27;
    private static final Material BG     = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin plugin;
    // Store pending transaction per viewer
    private final Map<UUID, PendingPay> pending = new HashMap<>();

    public record PendingPay(UUID targetUuid, double amount) {}

    public PayConfirmGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player payer, org.bukkit.OfflinePlayer target, double amount) {
        pending.put(payer.getUniqueId(), new PendingPay(target.getUniqueId(), amount));

        String title = ColorUtil.colorize(plugin.getConfigManager().getPayConfirmTitle());
        Inventory inv = Bukkit.createInventory(null, SIZE, title);

        ItemStack bg = ECOSTerminalGUI.item(BG, " ", null);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, bg);

        String fmt = plugin.getVaultHook().format(amount);
        double tax = plugin.getTaxManager().isPayTaxEnabled()
                ? plugin.getTaxManager().calcTax(amount, plugin.getTaxManager().getPayTaxRate()) : 0;
        double total = amount + tax;
        String targetName = target.getName() != null ? target.getName() : "未知";

        // Target head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(target);
            skullMeta.setDisplayName(ColorUtil.colorize("&f" + targetName
                    + (target.isOnline() ? "" : " &8(离线)")));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(ColorUtil.colorize("&7转账金额: &a" + fmt));
            if (tax > 0) {
                lore.add(ColorUtil.colorize("&7税费: &e" + plugin.getVaultHook().format(tax)
                        + " &8(" + plugin.getTaxManager().formatRate(plugin.getTaxManager().getPayTaxRate()) + ")"));
                lore.add(ColorUtil.colorize("&7实付: &c" + plugin.getVaultHook().format(total)));
            }
            lore.add(ColorUtil.colorize("&7当前余额: &f" + plugin.getVaultHook().format(plugin.getVaultHook().getBalance(payer))));
            lore.add("");
            lore.add(ColorUtil.colorize("&e请确认此操作"));
            skullMeta.setLore(lore);
            head.setItemMeta(skullMeta);
        }
        inv.setItem(13, head);

        // Confirm
        java.util.List<String> confirmLore = new java.util.ArrayList<>();
        confirmLore.add(ColorUtil.colorize("&7向 &f" + targetName + " &7转账"));
        confirmLore.add(ColorUtil.colorize("&7金额: &a" + fmt));
        if (tax > 0) confirmLore.add(ColorUtil.colorize("&7含税实付: &c" + plugin.getVaultHook().format(total)));
        confirmLore.add("");
        confirmLore.add(ColorUtil.colorize("&a点击确认（不可撤销）"));
        inv.setItem(CONFIRM_SLOT, ECOSTerminalGUI.item(Material.LIME_WOOL,
                ColorUtil.colorize("&a&l✔ 确认转账"), confirmLore));

        // Cancel
        inv.setItem(CANCEL_SLOT, ECOSTerminalGUI.item(Material.RED_WOOL,
                ColorUtil.colorize("&c&l✗ 取消"),
                List.of(ColorUtil.colorize("&7返回终端，不执行转账"))));

        payer.openInventory(inv);
    }

    public void open(Player payer, Player target, double amount) {
        open(payer, (org.bukkit.OfflinePlayer) target, amount);
    }

    public PendingPay getPending(Player payer) {
        return pending.get(payer.getUniqueId());
    }

    public void cleanup(Player payer) {
        pending.remove(payer.getUniqueId());
    }
}
