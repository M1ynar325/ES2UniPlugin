package com.etherstories.escore.listeners;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QuickShop 箱子店税收：
 * 1) 优先 ShopEnhancedTaxEvent（QS 6.2.0.11+）叠买家税率
 * 2) 回退 ShopPurchaseEvent.setTotal 加价，成交后从店主抽走税进 ES2 sink
 * 3) ShopSuccessPurchaseEvent 记统计
 */
public class QuickShopTaxListener implements Listener {

    private final ES2UniPlugin plugin;
    /** 新版 QS 走 EnhancedTax 时，不再 setTotal，避免双税 */
    private boolean enhancedTaxHooked = false;
    /** buyer -> (baseTotal, tax) 用于 setTotal / ChestShop 回退路径 */
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    private record Pending(double base, double tax, long at) {}

    public QuickShopTaxListener(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void tryRegister() {
        boolean any = false;
        enhancedTaxHooked = register(
                "com.ghostchu.quickshop.api.event.economy.ShopEnhancedTaxEvent", this::onEnhancedTax);
        any |= enhancedTaxHooked;
        any |= register("com.ghostchu.quickshop.api.event.economy.ShopPurchaseEvent", this::onPurchase);
        any |= register("com.ghostchu.quickshop.api.event.ShopPurchaseEvent", this::onPurchase);
        any |= register("org.maxgamer.quickshop.event.ShopPurchaseEvent", this::onPurchase);
        any |= register("com.ghostchu.quickshop.api.event.economy.ShopSuccessPurchaseEvent", this::onSuccess);
        any |= register("com.ghostchu.quickshop.api.event.ShopSuccessPurchaseEvent", this::onSuccess);
        any |= register("org.maxgamer.quickshop.event.ShopSuccessPurchaseEvent", this::onSuccess);

        // ChestShop（若装了）
        any |= register("com.Acrobot.ChestShop.Events.PreTransactionEvent", this::onChestShopPre);
        any |= register("com.Acrobot.ChestShop.Events.TransactionEvent", this::onChestShopDone);

        if (any) {
            plugin.getLogger().info("商店税收挂钩已注册"
                    + (enhancedTaxHooked ? "（EnhancedTax）" : "（Purchase.setTotal 回退）"));
        } else if (Bukkit.getPluginManager().getPlugin("QuickShop-Hikari") != null
                || Bukkit.getPluginManager().getPlugin("QuickShop") != null
                || Bukkit.getPluginManager().getPlugin("ChestShop") != null) {
            plugin.getLogger().warning("检测到商店插件，但未找到可挂钩事件（版本可能不兼容）");
        }
    }

    @SuppressWarnings("unchecked")
    private boolean register(String className, EventExecutor exec) {
        try {
            Class<? extends Event> clz = (Class<? extends Event>) Class.forName(className);
            Bukkit.getPluginManager().registerEvent(
                    clz, this, EventPriority.NORMAL, exec, plugin, false);
            plugin.getLogger().info("  挂钩: " + className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (Throwable t) {
            plugin.getLogger().warning("挂钩失败 " + className + ": " + t.getMessage());
            return false;
        }
    }

    private void onEnhancedTax(Listener listener, Event event) {
        if (!"ShopEnhancedTaxEvent".equals(event.getClass().getSimpleName())) return;
        if (!plugin.getTaxManager().isQuickShopTaxEnabled()) return;
        try {
            double rate = plugin.getTaxManager().getQuickShopTaxRate();
            if (rate <= 0) return;

            // setInteractorTax(double) 或 getTax().interactorRate(double)
            try {
                Method set = event.getClass().getMethod("setInteractorTax", double.class);
                // 读当前税率再叠加
                double cur = 0;
                try {
                    Object tax = event.getClass().getMethod("getTax").invoke(event);
                    if (tax != null) {
                        try {
                            cur = ((Number) tax.getClass().getMethod("interactorRate").invoke(tax)).doubleValue();
                        } catch (NoSuchMethodException e) {
                            cur = ((Number) tax.getClass().getMethod("getInteractorRate").invoke(tax)).doubleValue();
                        }
                    }
                } catch (Throwable ignored) {
                }
                set.invoke(event, Math.min(1.0, cur + rate));
                return;
            } catch (NoSuchMethodException ignored) {
            }

            Object tax = event.getClass().getMethod("getTax").invoke(event);
            if (tax == null) return;
            double cur;
            try {
                cur = ((Number) tax.getClass().getMethod("interactorRate").invoke(tax)).doubleValue();
                tax.getClass().getMethod("interactorRate", double.class).invoke(tax, Math.min(1.0, cur + rate));
            } catch (NoSuchMethodException e) {
                cur = ((Number) tax.getClass().getMethod("getInteractorRate").invoke(tax)).doubleValue();
                tax.getClass().getMethod("setInteractorRate", double.class)
                        .invoke(tax, Math.min(1.0, cur + rate));
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("ShopEnhancedTax 处理失败: " + t.getMessage());
        }
    }

    private void onPurchase(Listener listener, Event event) {
        String simple = event.getClass().getSimpleName();
        if (!simple.equals("ShopPurchaseEvent")) return;
        if (enhancedTaxHooked) return; // 新版 QS 已用 EnhancedTax
        if (!plugin.getTaxManager().isQuickShopTaxEnabled()) return;
        try {
            double total = readDouble(event, "getTotal");
            if (total <= 0) return;
            double rate = plugin.getTaxManager().getQuickShopTaxRate();
            double tax = plugin.getTaxManager().calcTax(total, rate);
            if (tax <= 0) return;

            if (!invokeSetTotal(event, total + tax)) {
                return;
            }
            Player buyer = resolveBuyer(event);
            if (buyer != null) {
                pending.put(buyer.getUniqueId(), new Pending(total, tax, System.currentTimeMillis()));
                buyer.sendMessage(ColorUtil.colorize(
                        "&8[ECOS] &7商店交易加收税 &f" + plugin.getVaultHook().format(tax)
                                + " &8(" + plugin.getTaxManager().formatRate(rate) + ")"));
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("ShopPurchase 税收失败: " + t.getMessage());
        }
    }

    private void onSuccess(Listener listener, Event event) {
        String simple = event.getClass().getSimpleName();
        if (!simple.equals("ShopSuccessPurchaseEvent")) return;
        try {
            Player buyer = resolveBuyer(event);
            double total = 0;
            try {
                total = readDouble(event, "getBalanceWithoutTax");
            } catch (Throwable e) {
                try {
                    total = readDouble(event, "getTotal");
                } catch (Throwable ignored) {
                }
            }

            double es2Tax = 0;
            if (buyer != null) {
                Pending p = pending.remove(buyer.getUniqueId());
                if (p != null && System.currentTimeMillis() - p.at < 15_000) {
                    es2Tax = p.tax;
                    // setTotal 回退：店主多收了税，抽走进 sink
                    try {
                        Object shop = event.getClass().getMethod("getShop").invoke(event);
                        Object owner = shop.getClass().getMethod("getOwner").invoke(shop);
                        Player ownerP = resolveQUser(owner);
                        if (ownerP != null && es2Tax > 0) {
                            String err = plugin.getTaxManager().collectTax(ownerP, es2Tax);
                            if (err != null) {
                                plugin.getLogger().warning("商店税入账失败: " + err);
                            }
                        } else if (es2Tax > 0) {
                            withdrawOfflineOwner(owner, es2Tax);
                        }
                    } catch (Throwable t) {
                        plugin.getLogger().warning("商店税抽走失败: " + t.getMessage());
                    }
                } else if (enhancedTaxHooked && plugin.getTaxManager().isQuickShopTaxEnabled() && total > 0) {
                    // EnhancedTax：税由 QS 扣，这里只记 ES2 税率对应金额
                    es2Tax = plugin.getTaxManager().calcTax(total, plugin.getTaxManager().getQuickShopTaxRate());
                    if (es2Tax > 0 && buyer != null) {
                        buyer.sendMessage(ColorUtil.colorize(
                                "&8[ECOS] &7商店税约 &f" + plugin.getVaultHook().format(es2Tax)
                                        + " &8(" + plugin.getTaxManager().formatRate(
                                        plugin.getTaxManager().getQuickShopTaxRate()) + ")"));
                    }
                } else if (!enhancedTaxHooked && plugin.getTaxManager().isQuickShopTaxEnabled() && total > 0) {
                    // 最后手段：成交后向买家补扣
                    es2Tax = plugin.getTaxManager().calcTax(total, plugin.getTaxManager().getQuickShopTaxRate());
                    if (es2Tax > 0) {
                        String err = plugin.getTaxManager().collectTax(buyer, es2Tax);
                        if (err != null) {
                            es2Tax = 0;
                            buyer.sendMessage(ColorUtil.colorize("&8[ECOS] &c商店税补扣失败: &7" + err));
                        } else {
                            buyer.sendMessage(ColorUtil.colorize(
                                    "&8[ECOS] &7商店税 &f" + plugin.getVaultHook().format(es2Tax)));
                        }
                    }
                }
            }

            plugin.getTradeStatsManager().recordQuickShop(buyer, total, es2Tax);
        } catch (Throwable t) {
            plugin.getLogger().warning("ShopSuccess 统计失败: " + t.getMessage());
        }
    }

    /** ChestShop：预交易抬价 */
    private void onChestShopPre(Listener listener, Event event) {
        if (!event.getClass().getSimpleName().equals("PreTransactionEvent")) return;
        if (!plugin.getTaxManager().isQuickShopTaxEnabled()) return;
        try {
            double price = readDouble(event, "getExactPrice");
            if (price <= 0) return;
            double tax = plugin.getTaxManager().calcTax(price, plugin.getTaxManager().getQuickShopTaxRate());
            if (tax <= 0) return;
            for (String m : new String[]{"setExactPrice", "setPrice"}) {
                try {
                    event.getClass().getMethod(m, double.class).invoke(event, price + tax);
                    Player client = null;
                    try {
                        Object c = event.getClass().getMethod("getClient").invoke(event);
                        if (c instanceof Player p) client = p;
                    } catch (Throwable ignored) {
                    }
                    if (client != null) {
                        pending.put(client.getUniqueId(), new Pending(price, tax, System.currentTimeMillis()));
                        client.sendMessage(ColorUtil.colorize(
                                "&8[ECOS] &7箱子店加收税 &f" + plugin.getVaultHook().format(tax)));
                    }
                    return;
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("ChestShop 预税失败: " + t.getMessage());
        }
    }

    private void onChestShopDone(Listener listener, Event event) {
        if (!event.getClass().getSimpleName().equals("TransactionEvent")) return;
        try {
            Player client = null;
            try {
                Object c = event.getClass().getMethod("getClient").invoke(event);
                if (c instanceof Player p) client = p;
            } catch (Throwable ignored) {
            }
            double price = 0;
            try {
                price = readDouble(event, "getExactPrice");
            } catch (Throwable ignored) {
            }
            double tax = 0;
            if (client != null) {
                Pending p = pending.remove(client.getUniqueId());
                if (p != null && System.currentTimeMillis() - p.at < 15_000) {
                    tax = p.tax;
                    // 店主多收：从 owner 抽税
                    try {
                        Object owner = event.getClass().getMethod("getOwner").invoke(event);
                        if (owner instanceof Player op) {
                            plugin.getTaxManager().collectTax(op, tax);
                        } else if (owner instanceof org.bukkit.OfflinePlayer op && op.isOnline()) {
                            plugin.getTaxManager().collectTax(op.getPlayer(), tax);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            plugin.getTradeStatsManager().recordQuickShop(client, price > 0 ? price - tax : 0, tax);
        } catch (Throwable t) {
            plugin.getLogger().warning("ChestShop 成交统计失败: " + t.getMessage());
        }
    }

    private void withdrawOfflineOwner(Object owner, double tax) {
        try {
            UUID uuid = null;
            for (String m : new String[]{"getUniqueId", "getUuid", "getUUID"}) {
                try {
                    Object id = owner.getClass().getMethod(m).invoke(owner);
                    if (id instanceof UUID u) { uuid = u; break; }
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (uuid == null) return;
            var eco = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (eco == null) return;
            var offline = Bukkit.getOfflinePlayer(uuid);
            var w = eco.getProvider().withdrawPlayer(offline, tax);
            if (w != null && w.transactionSuccess()) {
                String sink = plugin.getTaxManager().getSinkAccount();
                if (sink != null && !sink.isBlank()) {
                    eco.getProvider().depositPlayer(Bukkit.getOfflinePlayer(sink), tax);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean invokeSetTotal(Event event, double value) {
        try {
            event.getClass().getMethod("setTotal", double.class).invoke(event, value);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static double readDouble(Event event, String method) throws Exception {
        Object v = event.getClass().getMethod(method).invoke(event);
        return ((Number) v).doubleValue();
    }

    private Player resolveBuyer(Event event) {
        try {
            Object purchaser = null;
            for (String m : new String[]{"getPurchaser", "getBuyer", "getClient"}) {
                try {
                    purchaser = event.getClass().getMethod(m).invoke(event);
                    if (purchaser != null) break;
                } catch (NoSuchMethodException ignored) {
                }
            }
            return resolveQUser(purchaser);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Player resolveQUser(Object purchaser) {
        if (purchaser == null) return null;
        if (purchaser instanceof Player p) return p;
        for (String m : new String[]{"getUniqueId", "getUuid", "getUUID"}) {
            try {
                Object id = purchaser.getClass().getMethod(m).invoke(purchaser);
                if (id instanceof UUID uuid) return Bukkit.getPlayer(uuid);
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable ignored) {
            }
        }
        try {
            Object opt = purchaser.getClass().getMethod("getBukkitPlayer").invoke(purchaser);
            if (opt instanceof java.util.Optional<?> o && o.isPresent() && o.get() instanceof Player p)
                return p;
        } catch (Throwable ignored) {
        }
        return null;
    }
}
