package com.etherstories.escore.gui;

import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Etharia / ECOS：静谧、秩序。霁青、钴蓝、蓝白灰。
 */
public final class EcosStyle {

    /** 霁青 — 品牌字、系统名 */
    public static final String JIQING = "&#8FB9C6";
    /** 钴蓝 — 当前页、主操作 */
    public static final String COBALT = "&#4A7EB8";
    /** 灰 */
    public static final String MIST = "&#8A96A3";
    /** 白 */
    public static final String SNOW = "&#F4F7FA";

    /** @deprecated 用 {@link #JIQING} */
    public static final String CYAN = JIQING;

    private EcosStyle() {}

    public static String terminal(String page) {
        return ColorUtil.colorize("&8[ " + JIQING + "&lECOS " + MIST + "· " + SNOW + page + " &8]");
    }

    public static String hub(String page) {
        return ColorUtil.colorize(MIST + "ECOS / " + JIQING + page);
    }

    public static ItemStack chrome() {
        return ECOSTerminalGUI.bg(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    }
}
