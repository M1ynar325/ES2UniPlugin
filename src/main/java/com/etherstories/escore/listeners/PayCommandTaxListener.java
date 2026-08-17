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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拦截 Essentials /pay：二次确认后转账，税与终端 UI 一致（支持离线收款）。
 */
public class PayCommandTaxListener implements Listener {

    private static final long CONFIRM_MS = 15_000L;

    private record Pending(UUID to, String toName, double amount, long expireMs) {}

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
        OfflinePlayer to = resolveReceiver(parts[1]);
        if (to == null) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7找不到收款玩家（需进过服）"));
            event.setCancelled(true);
            return;
        }
        if (to.getUniqueId().equals(from.getUniqueId())) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7不能转给自己"));
            event.setCancelled(true);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(parts[2].replace(",", ""));
        } catch (NumberFormatException e) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7金额无效"));
            event.setCancelled(true);
            return;
        }
        if (amount <= 0) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7金额必须大于 0"));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        double tax = plugin.getTaxManager().isPayTaxEnabled()
                ? plugin.getTaxManager().calcTax(amount, plugin.getTaxManager().getPayTaxRate()) : 0;
        String fmt = plugin.getVaultHook().format(amount);
        String name = to.getName() != null ? to.getName() : parts[1];
        long now = System.currentTimeMillis();
        Pending prev = pending.get(from.getUniqueId());
        boolean confirmed = prev != null
                && prev.expireMs() >= now
                && prev.to().equals(to.getUniqueId())
                && Math.abs(prev.amount() - amount) < 1e-6;

        if (!confirmed) {
            pending.put(from.getUniqueId(), new Pending(to.getUniqueId(), name, amount, now + CONFIRM_MS));
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
