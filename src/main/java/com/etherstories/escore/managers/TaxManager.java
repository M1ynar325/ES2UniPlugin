package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * 税收：/pay（Essentials）+ 终端转账、QuickShop/ChestShop 可分别开关。
 * rate = 0.05 表示加收 5%（买家/付款方多付）。
 */
public class TaxManager {

    private final ES2UniPlugin plugin;

    public TaxManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isPayTaxEnabled() {
        return plugin.getConfig().getBoolean("tax.pay.enabled", false);
    }

    public double getPayTaxRate() {
        return clampRate(plugin.getConfig().getDouble("tax.pay.rate", 0.05));
    }

    public boolean isQuickShopTaxEnabled() {
        return plugin.getConfig().getBoolean("tax.quickshop.enabled", false);
    }

    public double getQuickShopTaxRate() {
        return clampRate(plugin.getConfig().getDouble("tax.quickshop.rate", 0.02));
    }

    public String getSinkAccount() {
        return plugin.getConfig().getString("tax.sink-account", "");
    }

    public void setPayEnabled(boolean v) {
        plugin.getConfig().set("tax.pay.enabled", v);
        plugin.saveConfig();
    }

    public void setPayRate(double rate) {
        plugin.getConfig().set("tax.pay.rate", clampRate(rate));
        plugin.saveConfig();
    }

    public void setQuickShopEnabled(boolean v) {
        plugin.getConfig().set("tax.quickshop.enabled", v);
        plugin.saveConfig();
    }

    public void setQuickShopRate(double rate) {
        plugin.getConfig().set("tax.quickshop.rate", clampRate(rate));
        plugin.saveConfig();
    }

    public double calcTax(double amount, double rate) {
        if (amount <= 0 || rate <= 0) return 0;
        return Math.round(amount * rate * 100.0) / 100.0;
    }

    /** 付款方应付总额 = 本金 + 税 */
    public double calcPayTotal(double amount) {
        if (!isPayTaxEnabled()) return amount;
        return amount + calcTax(amount, getPayTaxRate());
    }

    /**
     * 从付款方扣税并入账 sink（或销毁）。
     * @return null 成功，否则错误信息
     */
    public String collectTax(Player from, double tax) {
        if (tax <= 0) return null;
        if (!plugin.getVaultHook().isEnabled()) return "经济系统不可用";
        String err = plugin.getVaultHook().withdrawForced(from, tax);
        if (err != null) return err;
        String sink = getSinkAccount();
        if (sink != null && !sink.isBlank()) {
            Player sinkPlayer = Bukkit.getPlayerExact(sink);
            if (sinkPlayer != null) {
                plugin.getVaultHook().deposit(sinkPlayer, tax);
            } else {
                // 离线入账：Vault OfflinePlayer
                try {
                    var eco = Bukkit.getServicesManager()
                            .getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (eco != null) {
                        eco.getProvider().depositPlayer(Bukkit.getOfflinePlayer(sink), tax);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    public String formatRate(double rate) {
        return String.format("%.1f%%", rate * 100.0);
    }

    private static double clampRate(double rate) {
        if (rate < 0) return 0;
        if (rate > 1) return 1;
        return rate;
    }

    public String payTaxHint(double amount) {
        if (!isPayTaxEnabled()) return "";
        double tax = calcTax(amount, getPayTaxRate());
        if (tax <= 0) return "";
        return ChatColor.GRAY + "含税 " + plugin.getVaultHook().format(tax)
                + "（税率 " + formatRate(getPayTaxRate()) + "），实付 "
                + plugin.getVaultHook().format(calcPayTotal(amount));
    }
}
