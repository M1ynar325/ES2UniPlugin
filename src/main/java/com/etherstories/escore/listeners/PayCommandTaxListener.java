package com.etherstories.escore.listeners;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拦截 Essentials /pay：二次确认后转账，税与终端 UI 一致。
 * /pay * 给除自己外每个在线玩家各转一笔。
 */
public class PayCommandTaxListener implements Listener {

    private static final long CONFIRM_MS = 15_000L;

    private record Pending(UUID to, String toName, double amount, boolean all, long expireMs) {}

    private final ES2UniPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public PayCommandTaxListener(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPayCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getVaultHook().isEnabled()) return;

        String raw = event.getMessage().trim();
        if (raw.isEmpty() || raw.charAt(0) != '/') return;

        String body = raw.substring(1).trim();
        int colon = body.indexOf(':');
        if (colon >= 0) body = body.substring(colon + 1).trim();

        String[] parts = body.split("\\s+");
        if (parts.length < 3) return;
        if (!isPayLabel(parts[0])) return;

        Player from = event.getPlayer();
        event.setCancelled(true);

        double amount;
        try {
            amount = Double.parseDouble(parts[2].replace(",", ""));
        } catch (NumberFormatException e) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7金额无效"));
            return;
        }
        if (amount <= 0) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7金额必须大于 0"));
            return;
        }

        if (isAllToken(parts[1])) {
            confirmOrPayAll(from, amount);
            return;
        }

        OfflinePlayer to = resolveReceiver(parts[1]);
        if (to == null) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7找不到收款玩家（需进过服）"));
            return;
        }
        if (to.getUniqueId().equals(from.getUniqueId())) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7不能转给自己"));
            return;
        }

        double tax = taxEach(amount);
        String fmt = plugin.getVaultHook().format(amount);
        String name = to.getName() != null ? to.getName() : parts[1];
        long now = System.currentTimeMillis();
        Pending prev = pending.get(from.getUniqueId());
        boolean confirmed = prev != null
                && !prev.all()
                && prev.expireMs() >= now
                && prev.to().equals(to.getUniqueId())
                && Math.abs(prev.amount() - amount) < 1e-6;

        if (!confirmed) {
            pending.put(from.getUniqueId(), new Pending(to.getUniqueId(), name, amount, false, now + CONFIRM_MS));
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &e确认转账 &a" + fmt + " &e→ &f" + name
                    + (to.isOnline() ? "" : " &8(离线)")
                    + (tax > 0 ? " &8(含税实付 " + plugin.getVaultHook().format(amount + tax) + ")" : "")));
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7再输入一次同一指令以确认，&f15秒 &7内有效"));
            return;
        }
        pending.remove(from.getUniqueId());

        String err = plugin.getVaultHook().pay(from, to, amount, tax);
        if (err != null) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &c转账失败: &7" + err));
            return;
        }
        from.sendMessage(ColorUtil.colorize("&8[ECOS] &a已转账 &e" + fmt + " &a→ &f" + name
                + (to.isOnline() ? "" : " &8(离线)")
                + (tax > 0 ? " &8(含税实付 " + plugin.getVaultHook().format(amount + tax) + ")" : "")));
        if (to.isOnline() && to.getPlayer() != null) {
            to.getPlayer().sendMessage(ColorUtil.colorize(
                    "&8[ECOS] &f" + from.getName() + " &a向你转账 &e" + fmt));
        }
    }

    private void confirmOrPayAll(Player from, double amount) {
        List<Player> targets = othersOnline(from);
        if (targets.isEmpty()) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7没有其他在线玩家"));
            return;
        }
        double tax = taxEach(amount);
        double total = targets.size() * (amount + tax);
        String fmt = plugin.getVaultHook().format(amount);
        long now = System.currentTimeMillis();
        Pending prev = pending.get(from.getUniqueId());
        boolean confirmed = prev != null
                && prev.all()
                && prev.expireMs() >= now
                && Math.abs(prev.amount() - amount) < 1e-6;

        if (!confirmed) {
            pending.put(from.getUniqueId(), new Pending(null, "*", amount, true, now + CONFIRM_MS));
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &e确认全员转账 &a" + fmt
                    + " &e× &f" + targets.size() + " 人"
                    + " &8合计 " + plugin.getVaultHook().format(targets.size() * amount)
                    + (tax > 0 ? " 含税实付 " + plugin.getVaultHook().format(total) : "")));
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7再输入一次同一指令以确认，&f15秒 &7内有效"));
            return;
        }
        pending.remove(from.getUniqueId());
        from.sendMessage(ColorUtil.colorize(payAll(plugin, from, amount)));
    }

    public static List<Player> othersOnline(Player from) {
        List<Player> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(from.getUniqueId())) list.add(p);
        }
        return list;
    }

    public static String payAll(ES2UniPlugin plugin, Player from, double amount) {
        List<Player> targets = othersOnline(from);
        if (targets.isEmpty()) return "&8[ECOS] &7没有其他在线玩家";
        double tax = plugin.getTaxManager().isPayTaxEnabled()
                ? plugin.getTaxManager().calcTax(amount, plugin.getTaxManager().getPayTaxRate()) : 0;
        double total = targets.size() * (amount + tax);
        String guard = plugin.getVaultHook().checkBankruptcy(from, total, "全员转账");
        if (guard != null) return "&8[ECOS] &c转账失败: &7" + guard;
        if (!plugin.getConfigManager().isBankruptcyEnabled()
                && plugin.getVaultHook().getBalance(from) + 1e-6 < total) {
            return "&8[ECOS] &c余额不足（含税需 " + plugin.getVaultHook().format(total) + "）";
        }
        int ok = 0;
        List<String> fail = new ArrayList<>();
        for (Player to : targets) {
            if (!to.isOnline()) {
                fail.add(to.getName() + ": 已离线");
                continue;
            }
            String err = plugin.getVaultHook().pay(from, to, amount, tax);
            if (err != null) fail.add(to.getName() + ": " + err);
            else {
                ok++;
                to.sendMessage(ColorUtil.colorize(
                        "&8[ECOS] &f" + from.getName() + " &a向你转账 &e" + plugin.getVaultHook().format(amount)));
            }
        }
        String msg = "&8[ECOS] &a已全员转账 &e" + plugin.getVaultHook().format(amount)
                + " &a→ &f" + ok + " 人"
                + (tax > 0 ? " &8(含税实付 " + plugin.getVaultHook().format(ok * (amount + tax)) + ")" : "");
        if (!fail.isEmpty()) {
            msg += "\n&8[ECOS] &c失败 " + fail.size() + " 人: &7" + String.join("；", fail);
        }
        return msg;
    }

    private double taxEach(double amount) {
        return plugin.getTaxManager().isPayTaxEnabled()
                ? plugin.getTaxManager().calcTax(amount, plugin.getTaxManager().getPayTaxRate()) : 0;
    }

    public static boolean isAllToken(String name) {
        return name.equals("*") || name.equalsIgnoreCase("@a") || name.equalsIgnoreCase("@all");
    }

    private OfflinePlayer resolveReceiver(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        if (off.hasPlayedBefore() || off.isOnline()) return off;
        return null;
    }

    private static boolean isPayLabel(String label) {
        return label.equalsIgnoreCase("pay") || label.equalsIgnoreCase("epay");
    }
}
