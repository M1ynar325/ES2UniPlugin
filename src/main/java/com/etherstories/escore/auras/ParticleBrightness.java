package com.etherstories.escore.auras;

/**
 * 粒子亮度 / 密度：低省性能，高更华丽。
 */
public enum ParticleBrightness {

    LOW("低", 0.45, 0.80, 2),
    MEDIUM("中", 1.00, 1.00, 1),
    HIGH("高", 1.70, 1.30, 1);

    public final String label;
    /** 数量倍率 */
    public final double countMul;
    /** Dust / 散布尺寸倍率 */
    public final double sizeMul;
    /** 额外脉冲周期倍率（越大越稀疏） */
    public final int periodMul;

    ParticleBrightness(String label, double countMul, double sizeMul, int periodMul) {
        this.label = label;
        this.countMul = countMul;
        this.sizeMul = sizeMul;
        this.periodMul = Math.max(1, periodMul);
    }

    public ParticleBrightness next() {
        return switch (this) {
            case LOW -> MEDIUM;
            case MEDIUM -> HIGH;
            case HIGH -> LOW;
        };
    }

    public static ParticleBrightness fromKey(String key) {
        if (key == null) return MEDIUM;
        try {
            return valueOf(key.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
