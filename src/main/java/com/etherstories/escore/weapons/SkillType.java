package com.etherstories.escore.weapons;

/**
 * Etharia 技能展示名（configKey / enum 名不变，兼容已绑定物品）。
 */
public enum SkillType {

    SWORD_RAIN        ("Aether Verdict",       "以太裁决",  "sword_rain"),
    HOMING            ("Resonance Lock",       "共鸣锁定",  "homing"),
    LUMINAL_STRIKE    ("Luminal Strike",       "光速斩",    "luminal_strike"),
    AXIOM_BREACH      ("Axiom Breach",         "公理破界",  "axiom_breach"),
    STELLAR_CONV      ("Stellar Convergence",  "星力汇聚",  "stellar_convergence"),
    CELESTIAL_ASCENT  ("Celestial Ascent",     "天升",      "celestial_ascent"),
    ECHO_SCATTER      ("Echo Scatter",         "回声散射",  "echo_scatter"),
    ECHO_BARRAGE      ("Echo Seek",            "回声寻踪",  "echo_barrage");

    public final String englishName;
    public final String chineseName;
    public final String configKey;

    SkillType(String en, String zh, String configKey) {
        this.englishName = en;
        this.chineseName = zh;
        this.configKey   = configKey;
    }

    public String displayName() {
        return englishName + "「" + chineseName + "」";
    }

    public static SkillType fromKey(String key) {
        for (SkillType t : values()) {
            if (t.configKey.equalsIgnoreCase(key) || t.name().equalsIgnoreCase(key)) return t;
        }
        // 旧立场盾技能键兼容：忽略
        if ("glass_orbit".equalsIgnoreCase(key) || "GLASS_ORBIT".equalsIgnoreCase(key)) return null;
        return null;
    }
}
