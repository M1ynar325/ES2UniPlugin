package com.etherstories.escore.weapons.skills;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 回声伤害：魔法系，绕过护甲。清无敌帧，否则连发只结算 1～2 下。
 */
public final class EchoDamage {

    private EchoDamage() {}

    public static void magic(LivingEntity target, Player attacker, double amount) {
        if (target == null || target.isDead() || attacker == null || amount <= 0) return;
        target.setNoDamageTicks(0);
        try {
            DamageSource src = DamageSource.builder(DamageType.MAGIC)
                    .withCausingEntity(attacker)
                    .withDirectEntity(attacker)
                    .build();
            target.damage(amount, src);
        } catch (Throwable t) {
            target.damage(amount, attacker);
        }
    }
}
