package com.etherstories.escore.auras;

import org.bukkit.Material;

/**
 * 玩家光环 / 粒子特效枚举。
 */
public enum AuraType {

    STARDUST (
        "stardust", "Stardust", "星尘", 2800,
        Material.END_CRYSTAL,
        "&7脚边缓缓漂浮的淡色光点。",
        "&8基础"
    ),
    NOTE_CIRCLE (
        "note_circle", "Note Circle", "音符", 3200,
        Material.NOTE_BLOCK,
        "&7一圈缓慢旋转的音符粒子。",
        "&8基础"
    ),
    SNOW_VEIL (
        "snow_veil", "Snow Veil", "细雪", 3600,
        Material.SNOWBALL,
        "&f从头顶轻轻落下的雪花。",
        "&8基础"
    ),
    ASH_DRIFT (
        "ash_drift", "Ash Drift", "灰烬", 3900,
        Material.BASALT,
        "&8深色灰烬粒子向上飘散。",
        "&8基础"
    ),
    VOID_ECHO (
        "void_echo", "Void Echo", "末影雾", 4200,
        Material.ENDER_PEARL,
        "&7脚下升起的紫色末影雾。",
        "&8进阶"
    ),
    HEARTBEAT (
        "heartbeat", "Heartbeat", "红环", 4800,
        Material.REDSTONE,
        "&c红色光点组成的呼吸式圆环。",
        "&8进阶"
    ),
    BUBBLE_WELL (
        "bubble_well", "Bubble Well", "水泡", 5200,
        Material.HEART_OF_THE_SEA,
        "&b脚边不断冒出的水下气泡。",
        "&8进阶"
    ),
    DRAGON_SOUL (
        "dragon_soul", "Dragon Soul", "龙息", 5500,
        Material.DRAGON_EGG,
        "&5淡紫龙息粒子环绕身体。",
        "&8进阶"
    ),
    GOLD_ORBIT (
        "gold_orbit", "Gold Orbit", "金环", 6200,
        Material.GOLD_NUGGET,
        "&6金色光点绕身公转。",
        "&8稀有"
    ),
    ELECTRIC_HALO (
        "electric_halo", "Electric Halo", "电弧", 7000,
        Material.LIGHTNING_ROD,
        "&b肩高处快速流转的电火花。",
        "&8稀有"
    ),
    WITCH_RING (
        "witch_ring", "Witch Ring", "魔尘", 7800,
        Material.BREWING_STAND,
        "&5脚边一圈紫色魔法粒子。",
        "&8稀有"
    ),
    CLOUD_PUFF (
        "cloud_puff", "Cloud Puff", "云雾", 8200,
        Material.WHITE_WOOL,
        "&f低处轻软的白色烟云。",
        "&8稀有"
    ),
    NETHER_FLAME (
        "nether_flame", "Nether Flame", "魂火", 8500,
        Material.SOUL_LANTERN,
        "&9蓝色魂火从地面升起。",
        "&8精英"
    ),
    ENCHANT_DRIFT (
        "enchant_drift", "Enchant Drift", "附魔符文", 9800,
        Material.ENCHANTING_TABLE,
        "&d附魔台风格的符文粒子螺旋上升。",
        "&8精英"
    ),
    SAKURA (
        "sakura", "Sakura", "樱花", 10500,
        Material.CHERRY_LEAVES,
        "&d樱花花瓣从头顶飘落。",
        "&8精英"
    ),
    TOTEM_BLESS (
        "totem_bless", "Totem Bless", "图腾光", 11200,
        Material.TOTEM_OF_UNDYING,
        "&a黄绿色图腾粒子环绕。",
        "&8传说"
    ),
    SPARKLE_RING (
        "sparkle_ring", "Sparkle Ring", "花火", 11800,
        Material.NETHER_STAR,
        "&e肩高处飞旋的烟花火花。",
        "&8传说"
    ),
    SCULK_WHISPER (
        "sculk_whisper", "Sculk Whisper", "幽匿", 13000,
        Material.SCULK,
        "&3幽匿感测体风格的深色粒子。",
        "&8传说"
    ),
    AURORA (
        "aurora", "Aurora", "极光", 15000,
        Material.AMETHYST_SHARD,
        "&d彩色尘埃粒子沿环流动。",
        "&5&l传说"
    ),
    FIELD_RIG (
        "field_rig", "Field Rig", "立场装置", 16000,
        Material.PURPLE_STAINED_GLASS_PANE,
        "&d紫色玻璃板环绕，拦截来袭箭矢等弹射物。",
        "&5&l功能"
    );

    public final String   key;
    public final String   englishName;
    public final String   chineseName;
    public final int      price;
    public final Material icon;
    public final String   description;
    public final String   tier;

    AuraType(String key, String en, String zh, int price,
             Material icon, String description, String tier) {
        this.key         = key;
        this.englishName = en;
        this.chineseName = zh;
        this.price       = price;
        this.icon        = icon;
        this.description = description;
        this.tier        = tier;
    }

    public String displayName() { return englishName + "「" + chineseName + "」"; }

    public static AuraType fromKey(String key) {
        for (AuraType t : values())
            if (t.key.equalsIgnoreCase(key) || t.name().equalsIgnoreCase(key)) return t;
        return null;
    }
}
