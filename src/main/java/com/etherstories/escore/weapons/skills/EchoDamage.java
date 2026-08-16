package com.etherstories.escore.weapons.skills;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 回声伤害：魔法系，绕过护甲（不抗魔的坦克吃满）。
 * 结算仍是 A：每发独立 damage，可累加。
 */
public final class EchoDamage {

    private EchoDamage() {}

    public static void magic(LivingEntity target, Player attacker, double amount) {
        if (target == null || target.isDead() || attacker == null || amount <= 0) return;
        try {
            DamageSource src = DamageSource.builder(DamageType.MAGIC)
                    .withCausingEntity(attacker)
                    .withDirectEntity(attacker)
                    .build();
            target.damage(amount, src);
        } catch (Throwable t) {
            // 极旧 API 兜底（仍可能吃甲，尽量不用）
            target.damage(amount, attacker);
        }
    }
}
