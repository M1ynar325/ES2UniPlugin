package com.etherstories.escore.weapons.skills;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.WeaponManager;
import com.etherstories.escore.weapons.SkillInstance;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Stellar Convergence「星力汇聚」
 *
 * 两阶段：
 *  CONVERGING — 星力从四面八方汇聚至落点，随时间加剧
 *  EXPLODING  — 落点爆发：中心巨爆 + 12 条放射线 + 3 轮扩散冲击波
 */
public class StellarConvergenceSkill implements SkillInstance {

    private enum Phase { CONVERGING, EXPLODING, DONE }

    private final ES2UniPlugin  plugin;
    private final Player        player;
    private final WeaponManager wm;
    private final UUID          ownerUUID;

    private final int    convergenceTicks;
    private final double initRadius;
    private final int    explosionRays;
    private final double rayLength;
    private final double damage;
    private final double damageRadius;

    private Phase    phase  = Phase.CONVERGING;
    private int      tick   = 0;
    private Location target;

    public StellarConvergenceSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.plugin    = plugin;
        this.player    = player;
        this.wm        = wm;
        this.ownerUUID = player.getUniqueId();

        convergenceTicks = wm.getCfgInt   (SkillType.STELLAR_CONV, "convergence-ticks",  50);
        initRadius       = wm.getCfgDouble(SkillType.STELLAR_CONV, "convergence-radius", 14.0);
        explosionRays    = wm.getCfgInt   (SkillType.STELLAR_CONV, "explosion-rays",     12);
        rayLength        = wm.getCfgDouble(SkillType.STELLAR_CONV, "ray-length",         20.0);
        damage           = wm.getCfgDouble(SkillType.STELLAR_CONV, "damage",             12.0);
        damageRadius     = wm.getCfgDouble(SkillType.STELLAR_CONV, "damage-radius",      3.5);

        target = resolveLookTarget();

        player.getWorld().playSound(target, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);
        player.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.5f);
    }

    @Override
    public boolean tick() {
        tick++;
        return switch (phase) {
            case CONVERGING -> tickConverging();
            case EXPLODING  -> { explode(); phase = Phase.DONE; yield false; }
            case DONE       -> false;
        };
    }

    // ── 收束阶段 ──────────────────────────────────────────────────────────────

    private boolean tickConverging() {
        if (tick >= convergenceTicks) {
            phase = Phase.EXPLODING;
            return true;
        }

        double progress = (double) tick / convergenceTicks;
        double radius   = initRadius * (1.0 - progress);
        World  world    = target.getWorld();

        // ① 从球面向中心飞来的粒子（随进度变密、变快）
        int streamCount = 8 + (int)(progress * 16);
        for (int i = 0; i < streamCount; i++) {
            Vector dir  = randomUnitVector();
            Location from = target.clone().add(dir.multiply(radius));
            double speed  = 0.5 + progress * 0.8;
            Vector vel    = dir.multiply(-1).normalize().multiply(speed);

            // 前期：END_ROD（白色星芒）；后期：DRAGON_BREATH（紫色星力）
            if (progress < 0.5 || RNG.nextDouble() < 0.5) {
                world.spawnParticle(Particle.END_ROD, from, 0,
                        vel.getX(), vel.getY(), vel.getZ(), 1.0);
            } else {
                world.spawnParticle(Particle.DRAGON_BREATH, from, 0,
                        vel.getX(), vel.getY(), vel.getZ(), 1.0);
            }
        }

        // ② 远距离漂浮的 ENCHANT 符文粒子
        if (progress < 0.7) {
            for (int i = 0; i < 4; i++) {
                Vector d = randomUnitVector().multiply(radius * 1.2);
                world.spawnParticle(Particle.ENCHANT,
                        target.clone().add(d), 0,
                        d.multiply(-0.03).getX(), d.getY(), d.getZ(), 1.0);
            }
        }

        // ③ 旋转双环（两圈以不同速度转，营造星力漩涡感）
        double rot1 = tick * 0.18;
        double rot2 = tick * 0.28 + Math.PI;
        for (int ri = 0; ri < 20; ri++) {
            double a1 = rot1 + (Math.PI * 2.0 / 20) * ri;
            double a2 = rot2 + (Math.PI * 2.0 / 20) * ri;
            double r  = damageRadius * (1.6 - progress * 0.4);
            world.spawnParticle(Particle.CRIT,
                    target.clone().add(Math.cos(a1) * r, 0.15, Math.sin(a1) * r),
                    0, 0, 0.04, 0, 0.01);
            world.spawnParticle(Particle.ELECTRIC_SPARK,
                    target.clone().add(Math.cos(a2) * r, 0.15, Math.sin(a2) * r),
                    0, 0, 0.04, 0, 0.02);
        }

        // ④ 后期（progress > 0.6）：中心开始喷出 SOUL_FIRE_FLAME，张力感
        if (progress > 0.6) {
            int coreCount = (int)((progress - 0.6) / 0.4 * 10);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, target,
                    coreCount, 0.3, 0.3, 0.3, 0.05);
            world.spawnParticle(Particle.ELECTRIC_SPARK, target,
                    coreCount / 2, 0.2, 0.2, 0.2, 0.1);
        }

        // ⑤ 脉冲扩散环（每 8 tick 爆一圈向外散的粒子）
        if (tick % 8 == 0) {
            double pulseR = damageRadius * 2.5;
            for (double a = 0; a < Math.PI * 2; a += Math.PI / 16) {
                world.spawnParticle(Particle.CRIT,
                        target.clone().add(Math.cos(a) * pulseR, 0.2, Math.sin(a) * pulseR),
                        0, Math.cos(a) * 0.15, 0.05, Math.sin(a) * 0.15, 0.01);
            }
        }

        // 音效（越来越高亢）
        if (tick % 5 == 0) {
            float pitch = 1.2f + (float) progress * 1.2f;
            world.playSound(target, Sound.BLOCK_BEACON_AMBIENT, 0.4f, pitch);
        }
        if (tick % 10 == 0 && progress > 0.4) {
            world.playSound(target, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.5f, 1.8f);
        }

        return true;
    }

    // ── 爆发阶段 ──────────────────────────────────────────────────────────────

    private void explode() {
        World world = target.getWorld();

        // ── 中心巨爆视觉 ──
        world.spawnParticle(Particle.EXPLOSION_EMITTER, target, 3, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.END_ROD,           target, 120, 1.0, 1.0, 1.0, 1.2);
        world.spawnParticle(Particle.DRAGON_BREATH,     target, 80,  0.8, 0.8, 0.8, 0.6);
        world.spawnParticle(Particle.ELECTRIC_SPARK,    target, 60,  0.6, 0.6, 0.6, 0.8);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME,   target, 40,  0.5, 0.5, 0.5, 0.4);
        // 向上喷射柱
        world.spawnParticle(Particle.END_ROD,           target, 40,  0.1, 0.5, 0.1, 1.5);

        // ── 音效层叠 ──
        world.playSound(target, Sound.ENTITY_GENERIC_EXPLODE,          1.0f, 0.6f);
        world.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,   1.0f, 0.9f);
        world.playSound(target, Sound.ENTITY_WITHER_DEATH,             0.5f, 1.8f);
        world.playSound(target, Sound.BLOCK_BEACON_POWER_SELECT,       0.8f, 0.5f);

        // 中心伤害
        damageNearby(target);

        // ── 3 轮扩散冲击波环（半径依次扩大） ──
        for (int wave = 1; wave <= 3; wave++) {
            double wR = damageRadius * wave * 1.8;
            for (double a = 0; a < Math.PI * 2; a += Math.PI / 20) {
                double ox = Math.cos(a) * wR, oz = Math.sin(a) * wR;
                world.spawnParticle(Particle.ELECTRIC_SPARK,
                        target.clone().add(ox, 0.2, oz),
                        0, ox * 0.08, 0.1, oz * 0.08, 0.05);
                world.spawnParticle(Particle.CRIT,
                        target.clone().add(ox, 0.5, oz),
                        0, ox * 0.06, 0.08, oz * 0.06, 0.02);
            }
        }

        // ── 12 条放射线（均匀 XZ 面，交替仰俯角，采样密度 1.0 格） ──
        for (int i = 0; i < explosionRays; i++) {
            double yaw   = (2 * Math.PI / explosionRays) * i;
            double pitch = switch (i % 3) { case 0 -> 0.2; case 1 -> -0.1; default -> 0.05; };
            Vector rayDir = new Vector(Math.cos(yaw), pitch, Math.sin(yaw)).normalize();

            for (double d = 0.5; d <= rayLength; d += 1.0) {
                Location point = target.clone().add(rayDir.clone().multiply(d));
                double t = d / rayLength; // 0→1：越远越暗淡

                world.spawnParticle(Particle.END_ROD,
                        point, 0, rayDir.getX()*0.05, rayDir.getY()*0.05, rayDir.getZ()*0.05, 1.0);

                // 前 60% 射程：DRAGON_BREATH 紫色焰尾
                if (t < 0.6) {
                    world.spawnParticle(Particle.DRAGON_BREATH,
                            point, 2, 0.15, 0.15, 0.15, 0.04);
                }
                // 前 40% 射程：ELECTRIC_SPARK 闪电芯
                if (t < 0.4) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK,
                            point, 1, 0.1, 0.1, 0.1, 0.1);
                }

                world.spawnParticle(Particle.CRIT, point, 2, 0.2, 0.2, 0.2, 0.1);
                damageNearby(point);
            }
        }
    }

    private void damageNearby(Location loc) {
        for (org.bukkit.entity.Entity e : loc.getWorld()
                .getNearbyEntities(loc, damageRadius, damageRadius, damageRadius)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.damage(damage, player);
            }
        }
    }

    // ── 工具 ─────────────────────────────────────────────────────────────────

    private Location resolveLookTarget() {
        RayTraceResult r = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                50.0, FluidCollisionMode.NEVER, true);
        if (r != null && r.getHitBlock() != null) {
            return r.getHitBlock().getLocation().add(0.5, 1.0, 0.5);
        }
        return player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(40));
    }

    private static final java.util.Random RNG = new java.util.Random();

    private static Vector randomUnitVector() {
        double theta = RNG.nextDouble() * 2 * Math.PI;
        double phi   = Math.acos(1 - 2 * RNG.nextDouble());
        return new Vector(
                Math.sin(phi) * Math.cos(theta),
                Math.sin(phi) * Math.sin(theta),
                Math.cos(phi));
    }

    @Override public void cleanup()         { /* 无实体 */ }
    @Override public UUID getOwnerUUID()    { return ownerUUID; }
}
