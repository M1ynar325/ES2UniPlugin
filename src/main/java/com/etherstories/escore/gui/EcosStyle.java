package com.etherstories.escore.gui;

import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Etharia / ECOS：静谧、秩序。霁青、钴蓝、蓝白灰。
 */
public final class EcosStyle {

    /** 霁青 — 品牌字、系统名（屏/聊天） */
    public static final String JIQING = "&#8FB9C6";
    /** 钴蓝 — 当前页、主操作（屏/聊天） */
    public static final String COBALT = "&#4A7EB8";
    /** 灰 */
    public static final String MIST = "&#8A96A3";
    /** 白 */
    public static final String SNOW = "&#F4F7FA";
    /** 橙 — 钱、提醒 */
    public static final String ORANGE = "&#C45C12";

    /** 书页浅底：深霁青 */
    public static final String BOOK_JIQING = "&#2A5F6B";
    /** 书页浅底：深钴蓝 */
    public static final String BOOK_COBALT = "&#1B4F8A";
    /** 书页浅底：墨 */
    public static final String BOOK_INK = "&0";
    /** 书页浅底：灰 */
    public static final String BOOK_MUTED = "&#4A5560";
    /** 书页浅底：橙 */
    public static final String BOOK_ORANGE = "&#C45C12";
    /** 书页浅底：分隔 */
    public static final String BOOK_LINE = "&#8A8070";
    /** 悬停黑框：浅霁青 */
    public static final String HOVER = "&#C5E4EC";
    /** 悬停黑框：浅灰白 */
    public static final String HOVER_DIM = "&#E8EEF2";

    /** 悬停框是黑底，必须浅色。先剥色再套浅霁青。 */
    public static String hoverTip(String raw) {
        String t = raw == null || raw.isBlank() ? "点击" : raw.replace('\n', ' ');
        t = org.bukkit.ChatColor.stripColor(ColorUtil.colorize(t));
        if (t == null || t.isBlank()) t = "点击";
        return ColorUtil.colorize(HOVER + t);
    }

    /** @deprecated 用 {@link #JIQING} */
    public static final String CYAN = JIQING;

    private EcosStyle() {}

    public static String hello(String name) {
        int h = java.time.LocalTime.now().getHour();
        String g = h < 5 ? "夜深了" : h < 9 ? "早上好" : h < 11 ? "上午好"
                : h < 14 ? "中午好" : h < 18 ? "下午好" : h < 22 ? "晚上好" : "夜深了";
        return name == null || name.isBlank() ? g : g + "，" + name;
    }

    public static String terminal(String page) {
        return ColorUtil.colorize("&0[ &b&lECOS &8" + page + " &0]");
    }

    public static String hub(String page) {
        return ColorUtil.colorize(MIST + "ECOS / " + JIQING + page);
    }

    public static ItemStack chrome() {
        return ECOSTerminalGUI.bg(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    }
}
