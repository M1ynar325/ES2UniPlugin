package com.etherstories.escore.hooks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** 反射调 ESLink，没装也能编译。税率和交易开关存在 ESLink 配置里。 */
public final class ESLinkHook {

    public boolean present() {
        Plugin p = Bukkit.getPluginManager().getPlugin("ESLink");
        return p != null && p.isEnabled();
    }

    private Object plugin() {
        return Bukkit.getPluginManager().getPlugin("ESLink");
    }

    public boolean tradeEnabled() {
        return bool("tradeEnabled", true);
    }

    public double taxRate() {
        Object p = plugin();
        if (p == null) return 0;
        try {
            Object r = p.getClass().getMethod("taxRate").invoke(p);
            return r instanceof Number n ? n.doubleValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public void setTradeEnabled(boolean v) {
        Object p = plugin();
        if (p == null) return;
        try {
            p.getClass().getMethod("setTradeEnabled", boolean.class).invoke(p, v);
        } catch (Throwable ignored) {
        }
    }

    public void setTaxRate(double rate) {
        Object p = plugin();
        if (p == null) return;
        try {
            p.getClass().getMethod("setTaxRate", double.class).invoke(p, rate);
        } catch (Throwable ignored) {
        }
    }

    public String formatRate() {
        return String.format("%.0f%%", taxRate() * 100.0);
    }

    private boolean bool(String method, boolean def) {
        Object p = plugin();
        if (p == null) return def;
        try {
            Object r = p.getClass().getMethod(method).invoke(p);
            return r instanceof Boolean b ? b : def;
        } catch (Throwable t) {
            return def;
        }
    }
}
