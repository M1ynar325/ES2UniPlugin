package com.etherstories.escore.estate;

import org.bukkit.Material;

/** 房间用途。 */
public enum UnitKind {
    HOUSE     ("house",     "住宅",   Material.OAK_DOOR),
    APARTMENT ("apartment", "公寓",   Material.DARK_OAK_DOOR),
    HOTEL     ("hotel",     "客房",   Material.WHITE_BED),
    SHOP      ("shop",      "商铺",   Material.CHEST),
    WORKSHOP  ("workshop",  "工坊",   Material.SMITHING_TABLE),
    STUDIO    ("studio",    "工作室", Material.LECTERN),
    STORAGE   ("storage",   "仓库",   Material.BARREL),
    OTHER     ("other",     "其他",   Material.OAK_SIGN);

    public final String key;
    public final String label;
    public final Material icon;

    UnitKind(String key, String label, Material icon) {
        this.key = key;
        this.label = label;
        this.icon = icon;
    }

    public BuildingCategory defaultCategory() {
        return switch (this) {
            case HOUSE, APARTMENT, HOTEL -> BuildingCategory.RESIDENTIAL;
            case SHOP, WORKSHOP, STUDIO, STORAGE -> BuildingCategory.COMMERCIAL;
            case OTHER -> BuildingCategory.PUBLIC;
        };
    }

    public static UnitKind fromKey(String raw) {
        if (raw == null || raw.isBlank()) return HOUSE;
        for (UnitKind k : values()) {
            if (k.key.equalsIgnoreCase(raw) || k.name().equalsIgnoreCase(raw)
                    || k.label.equals(raw)) return k;
        }
        return null;
    }

    public boolean hotelBindable() {
        return this == HOUSE || this == APARTMENT || this == HOTEL;
    }
}
