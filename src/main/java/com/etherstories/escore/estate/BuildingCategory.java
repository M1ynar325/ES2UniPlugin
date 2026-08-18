package com.etherstories.escore.estate;

import org.bukkit.Material;

/** 建筑大类：ECOS 楼盘按这个分。 */
public enum BuildingCategory {
    RESIDENTIAL ("residential", "住宅",     Material.RED_BED),
    PUBLIC      ("public",      "公共建筑", Material.BEACON),
    COMMERCIAL  ("commercial",  "商业建筑", Material.GOLD_BLOCK);

    public final String key;
    public final String label;
    public final Material icon;

    BuildingCategory(String key, String label, Material icon) {
        this.key = key;
        this.label = label;
        this.icon = icon;
    }

    public static BuildingCategory fromKey(String raw) {
        if (raw == null || raw.isBlank()) return RESIDENTIAL;
        for (BuildingCategory c : values()) {
            if (c.key.equalsIgnoreCase(raw) || c.name().equalsIgnoreCase(raw)
                    || c.label.equals(raw)) return c;
        }
        if (raw.contains("公共") || raw.equalsIgnoreCase("pub")) return PUBLIC;
        if (raw.contains("商") || raw.equalsIgnoreCase("shop")) return COMMERCIAL;
        if (raw.contains("住") || raw.equalsIgnoreCase("home")) return RESIDENTIAL;
        return null;
    }
}
