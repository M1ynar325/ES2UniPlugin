package com.etherstories.escore.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

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

    /** 网页公屏写入 ESLink 互通库。新版走 emitFromWeb；旧版人在线则 send。 */
    public void forwardChat(UUID uuid, String name, String msg) {
        Object p = plugin();
        if (p == null || msg == null || msg.isBlank()) return;
        try {
            Object chat = p.getClass().getMethod("chat").invoke(p);
            try {
                chat.getClass().getMethod("emitFromWeb", UUID.class, String.class, String.class)
                        .invoke(chat, uuid, name, msg);
                return;
            } catch (NoSuchMethodException ignored) {}
            Player pl = uuid == null ? null : Bukkit.getPlayer(uuid);
            if (pl != null && pl.isOnline()) {
                chat.getClass().getMethod("send", Player.class, String.class, ItemStack.class)
                        .invoke(chat, pl, msg, null);
            }
        } catch (Throwable ignored) {}
    }

    /** null = 没装 ESLink 或反射失败 */
    public Boolean isChatAll(UUID uuid) {
        Object p = plugin();
        if (p == null || uuid == null) return null;
        try {
            Object chat = p.getClass().getMethod("chat").invoke(p);
            try {
                Object r = chat.getClass().getMethod("isAll", UUID.class).invoke(chat, uuid);
                return r instanceof Boolean b ? b : null;
            } catch (NoSuchMethodException ignored) {}
            Player pl = Bukkit.getPlayer(uuid);
            if (pl == null || !pl.isOnline()) return null;
            Object r = chat.getClass().getMethod("isAll", Player.class).invoke(chat, pl);
            return r instanceof Boolean b ? b : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public void setChatAll(UUID uuid, boolean all) {
        Object p = plugin();
        if (p == null || uuid == null) return;
        try {
            Object chat = p.getClass().getMethod("chat").invoke(p);
            try {
                chat.getClass().getMethod("setAll", UUID.class, boolean.class).invoke(chat, uuid, all);
                return;
            } catch (NoSuchMethodException ignored) {}
            Player pl = Bukkit.getPlayer(uuid);
            if (pl != null && pl.isOnline()) {
                chat.getClass().getMethod("setAll", Player.class, boolean.class).invoke(chat, pl, all);
            }
        } catch (Throwable ignored) {}
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
