package com.etherstories.escore.weapons.skills;

import com.etherstories.escore.managers.WeaponManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 回声共鸣层：伤害按发独立结算（A），层数只影响缓慢深度 + 满层余韵爆发。
 */
public final class EchoResonance {

    private static final class Mark {
        int stacks;
        long expireMs;
        UUID lastAttacker;

        Mark(int stacks, long expireMs, UUID lastAttacker) {
            this.stacks = stacks;
            this.expireMs = expireMs;
            this.lastAttacker = lastAttacker;
        }
    }

    private static final Map<UUID, Mark> MARKS = new ConcurrentHashMap<>();
    private static final int MAX_STACKS = 5;
    private static final long WINDOW_MS = 3500L;

    private EchoResonance() {}

    /**
     * 命中后调用：先假定调用方已 damage。
     * @return 当前层数
     */
    public static int onHit(LivingEntity target, Player attacker, boolean fromSeek) {
        if (target == null || target.isDead() || attacker == null) return 0;
        long now = System.currentTimeMillis();
        UUID tid = target.getUniqueId();

        Mark m = MARKS.get(tid);
        if (m == null || m.expireMs < now) {
            m = new Mark(0, now + WINDOW_MS, attacker.getUniqueId());
        }
        m.stacks = Math.min(MAX_STACKS, m.stacks + 1);
        m.expireMs = now + WINDOW_MS;
        m.lastAttacker = attacker.getUniqueId();
        MARKS.put(tid, m);

        // 先伤后缓：层数加深缓慢
        int amp = Math.min(2, (m.stacks - 1) / 2); // 1→I, 3→II, 5→III capped at II → wait amp 0,1,2 = I,II,III
        // user said 缓慢 - amp 0 = Slow I. Let's use: stacks 1-2 → 0, 3-4 → 1, 5 → 2
        amp = Math.min(2, (m.stacks - 1) / 2);
        int duration = 30 + m.stacks * 8; // 1.5s ~ 3.5s
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amp, false, true, true));

        World w = target.getWorld();
        Location at = target.getLocation().add(0, 1.0, 0);
        w.spawnParticle(Particle.DUST, at, 2 + m.stacks, 0.15, 0.25, 0.15, 0,
                new Particle.DustOptions(Color.fromRGB(100 + m.stacks * 20, 160, 255), 0.65f));

        // 满 5 层：余韵爆发（额外一小段伤，仍是累加）
        if (m.stacks >= MAX_STACKS) {
            double bonus = fromSeek ? 6.5 : 5.0;
            EchoDamage.magic(target, attacker, bonus);
            w.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.7f, 1.6f);
            w.spawnParticle(Particle.END_ROD, at, 12, 0.35, 0.4, 0.35, 0.04);
            w.spawnParticle(Particle.FLASH, at, 1, 0, 0, 0, 0);
            MARKS.remove(tid); // 爆发后清层，可重新叠
            if (attacker.isOnline()) {
                WeaponManager.sendActionBar(attacker, "&b回声余韵 &f×5 爆发", 30);
            }
            return MAX_STACKS;
        }

        if (fromSeek && attacker.isOnline() && m.stacks >= 3) {
            WeaponManager.sendActionBar(attacker, "&b共鸣 &f" + m.stacks + "/" + MAX_STACKS, 15);
        }
        return m.stacks;
    }

    public static void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Mark>> it = MARKS.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expireMs < now) it.remove();
        }
    }
}
