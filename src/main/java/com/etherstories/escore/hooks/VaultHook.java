package com.etherstories.escore.hooks;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private Economy economy;

    public void hook() {
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) economy = rsp.getProvider();
    }

    public boolean isEnabled() { return economy != null; }

    public double getBalance(Player player) {
        return isEnabled() ? economy.getBalance(player) : 0;
    }

    public String format(double amount) {
        return isEnabled() ? economy.format(amount) : String.format("%.2f", amount);
    }

    public String pay(Player from, Player to, double amount) {
        return pay(from, to, amount, 0);
    }

    public String pay(Player from, Player to, double amount, double tax) {
        return pay(from, (org.bukkit.OfflinePlayer) to, amount, tax);
    }

    public String pay(Player from, org.bukkit.OfflinePlayer to, double amount, double tax) {
        if (!isEnabled()) return "经济系统不可用。";
        if (amount <= 0) return "金额必须大于 0。";
        if (to == null || (to.getName() == null && !to.hasPlayedBefore() && !to.isOnline()))
            return "找不到收款玩家。";
        if (to.getUniqueId().equals(from.getUniqueId())) return "不能转给自己。";
        double total = amount + Math.max(0, tax);
        String guard = checkBankruptcy(from, total, "转账");
        if (guard != null) return guard;
        ES2UniPlugin pl = ES2UniPlugin.getInstance();
        if (pl == null || !pl.getConfigManager().isBankruptcyEnabled()) {
            if (!economy.has(from, total))
                return "余额不足（含税需 " + format(total) + "）。";
        }
        var w = economy.withdrawPlayer(from, amount);
        if (w != null && !w.transactionSuccess()) {
            return w.errorMessage != null ? w.errorMessage : "扣款失败（经济插件可能禁止负余额）";
        }
        economy.depositPlayer(to, amount);
        if (tax > 0) {
            String err = ES2UniPlugin.getInstance().getTaxManager().collectTax(from, tax);
            if (err != null) {
                economy.withdrawPlayer(to, amount);
                economy.depositPlayer(from, amount);
                return "扣税失败: " + err;
            }
        }
        try {
            Player onlineTo = to.getPlayer();
            ES2UniPlugin.getInstance().getTradeStatsManager()
                    .recordPay(from, onlineTo != null ? onlineTo : to, amount, tax);
        } catch (Throwable ignored) {
        }
        return null;
    }

    public String withdraw(Player player, double amount) {
        return withdraw(player, amount, true, "消费");
    }

    public String withdrawForced(Player player, double amount) {
        return withdraw(player, amount, false, null);
    }

    private String withdraw(Player player, double amount, boolean bankruptcy, String reason) {
        if (!isEnabled()) return "经济系统不可用";
        if (amount <= 0) return "金额无效";
        if (bankruptcy) {
            String guard = checkBankruptcy(player, amount, reason);
            if (guard != null) return guard;
        } else if (!economy.has(player, amount)) {
            return "余额不足";
        }
        var resp = economy.withdrawPlayer(player, amount);
        if (resp != null && !resp.transactionSuccess()) {
            return resp.errorMessage != null ? resp.errorMessage : "扣款失败（经济插件可能禁止负余额）";
        }
        return null;
    }

    public String deposit(Player player, double amount) {
        if (!isEnabled()) return "经济系统不可用";
        economy.depositPlayer(player, amount);
        return null;
    }

    public String deposit(org.bukkit.OfflinePlayer player, double amount) {
        if (!isEnabled()) return "经济系统不可用";
        if (amount <= 0) return "金额无效";
        economy.depositPlayer(player, amount);
        return null;
    }

    /**
     * 负债规则：
     * - 扣款后 &lt; floor（默认 -500）→ 拦截
     * - 当前余额在 [floor, 0) → 允许消费，但提示管理员
     */
    public String checkBankruptcy(Player player, double spend, String reason) {
        ES2UniPlugin plugin = ES2UniPlugin.getInstance();
        if (plugin == null || !plugin.getConfigManager().isBankruptcyEnabled()) return null;
        double floor = plugin.getConfigManager().getBankruptcyReserve();
        double bal = economy.getBalance(player);
        double after = bal - spend;

        if (after < floor - 1e-9) {
            String msg = "负债已达上限 " + format(floor)
                    + "（当前 " + format(bal) + "，本次 " + format(spend)
                    + " 后将为 " + format(after) + "）";
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c" + msg));
            notifyAdmins(plugin, player, reason, spend, bal, floor, true);
            return msg;
        }

        // 负余额区间消费：提示管理（不拦截）
        if (bal < -1e-9 && bal >= floor - 1e-9
                && plugin.getConfigManager().isBankruptcyNotifyAdmins()) {
            notifyAdmins(plugin, player, reason, spend, bal, floor, false);
            player.sendMessage(ColorUtil.colorize(
                    "&8[ECOS] &e你处于负债状态（" + format(bal) + "），本次消费已通知管理员"));
        }
        return null;
    }

    private void notifyAdmins(ES2UniPlugin plugin, Player player, String reason,
                              double spend, double bal, double floor, boolean blocked) {
        String adminMsg = ColorUtil.colorize(
                (blocked ? "&c[负债拦截] " : "&e[负债消费] ")
                        + "&f" + player.getName()
                        + " &7" + (reason == null ? "扣款" : reason)
                        + " &f" + format(spend)
                        + " &8余额 " + format(bal)
                        + " → " + format(bal - spend)
                        + " &7下限 &e" + format(floor));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("es2uni.admin") || p.isOp()) p.sendMessage(adminMsg);
        }
    }
}
