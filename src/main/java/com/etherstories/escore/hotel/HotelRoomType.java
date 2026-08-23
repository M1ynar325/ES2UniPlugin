package com.etherstories.escore.hotel;

import org.bukkit.Material;

public enum HotelRoomType {
    STANDARD("standard", "标准", Material.WHITE_BED),
    KING    ("king",     "大床", Material.RED_BED),
    SUITE   ("suite",    "套房", Material.YELLOW_BED),
    HOURLY  ("hourly",   "钟点", Material.CLOCK);

    public final String key;
    public final String label;
    public final Material icon;

    HotelRoomType(String key, String label, Material icon) {
        this.key = key;
        this.label = label;
        this.icon = icon;
    }

    public static HotelRoomType fromKey(String raw) {
        if (raw == null || raw.isBlank()) return STANDARD;
        for (HotelRoomType t : values()) {
            if (t.key.equalsIgnoreCase(raw) || t.name().equalsIgnoreCase(raw) || t.label.equals(raw))
                return t;
        }
        return null;
    }

    public static String hint() {
        return "standard/king/suite/hourly";
    }
}
