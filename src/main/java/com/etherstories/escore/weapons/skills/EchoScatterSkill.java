package com.etherstories.escore.weapons.skills;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.WeaponManager;
import com.etherstories.escore.weapons.SkillInstance;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Echo Scatter「回声散射」—
 * 身后散发大量粒子 → 高速直线射出；快速跑动能小概率闪避；
 * 击中小爆炸（不破方块）；不伤害友好生物。
 */
public class EchoScatterSkill implements SkillInstance {

    private static final class Bolt {
        Location loc;
        Vector dir;
        int life;
        final Color color;

        Bolt(Location loc, Vector dir, int life, Color color) {
            this.loc = loc;
            this.dir = dir;
            this.life = life;
            this.color = color;
        }
    }

    private final Player player;
    private final UUID ownerUUID;
    private final double damage;
    private final double speed;
    private final double hitRadius;
    private final double explodePower;
    private final double explodeDamage;
    private final double explodeRadius;
    private final double dodgeChance;
    private final double dodgeSpeedMin;
    private final List<Bolt> bolts = new ArrayList<>();
    private final Random rng = new Random();
    private int tick = 0;
    private final int expandTicks;
    private final int flyTicks;
    private boolean launched = false;
    private final List<Location> seeds = new ArrayList<>();
    private Location burstOrigin;

    public EchoScatterSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.player = player;
        this.ownerUUID = player.getUniqueId();
        int count = wm.getCfgInt(SkillType.ECHO_SCATTER, "particle-count", 72);
        this.damage = wm.getCfgDouble(SkillType.ECHO_SCATTER, "damage", 4.0);
        this.speed = wm.getCfgDouble(SkillType.ECHO_SCATTER, "speed", 2.4);
        this.hitRadius = wm.getCfgDouble(SkillType.ECHO_SCATTER, "hit-radius", 1.0);
        this.expandTicks = wm.getCfgInt(SkillType.ECHO_SCATTER, "expand-ticks", 10);
        this.flyTicks = wm.getCfgInt(SkillType.ECHO_SCATTER, "fly-ticks", 28);
        this.explodePower = wm.getCfgDouble(SkillType.ECHO_SCATTER, "explode-power", 1.2);
        this.explodeDamage = wm.getCfgDouble(SkillType.ECHO_SCATTER, "explode-damage", 3.0);
        this.explodeRadius = wm.getCfgDouble(SkillType.ECHO_SCATTER, "explode-radius", 2.2);
        this.dodgeChance = wm.getCfgDouble(SkillType.ECHO_SCATTER, "dodge-chance", 0.22);
        this.dodgeSpeedMin = wm.getCfgDouble(SkillType.ECHO_SCATTER, "dodge-speed-min", 0.28);

        Location back = player.getLocation().add(0, 1.0, 0)
                .subtract(player.getLocation().getDirection().normalize().multiply(0.8));
        this.burstOrigin = back.clone();
        World w = back.getWorld();
        w.playSound(back, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 0.8f);
        w.playSound(back, Sound.ENTITY_ENDER_EYE_LAUNCH, 0.5f, 1.6f);
        w.playSound(back, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
        w.spawnParticle(Particle.FLASH, back, 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.FIREWORK, back, 35, 0.4, 0.4, 0.4, 0.08);
        w.spawnParticle(Particle.END_ROD, back, 40, 0.5, 0.5, 0.5, 0.05);

        for (int i = 0; i < count; i++) {
            double yaw = (2 * Math.PI * i) / count + Math.random() * 0.08;
            double pitch = (Math.random() - 0.5) * 0.85;
            Vector radial = new Vector(Math.cos(yaw), pitch, Math.sin(yaw)).normalize();
            seeds.add(back.clone());
            float t = (float) i / Math.max(1, count - 1);
            Color c = Color.fromRGB(
                    (int) (80 + 100 * t),
                    (int) (180 + 40 * (1 - t)),
                    (int) (255 - 40 * t));
            bolts.add(new Bolt(back.clone(), radial, flyTicks, c));
        }
    }

    @Override
    public boolean tick() {
        tick++;
        World w = player.getWorld();

        if (!launched) {
            double t = Math.min(1.0, tick / (double) expandTicks);
            if (burstOrigin != null && tick % 2 == 0) {
                double ringR = t * 2.8;
                for (int i = 0; i < 16; i++) {
                    double a = (Math.PI * 2 / 16) * i + tick * 0.1;
                    Location ring = burstOrigin.clone().add(Math.cos(a) * ringR, 0.2, Math.sin(a) * ringR);
                    w.spawnParticle(Particle.NOTE, ring, 1, 0, 0, 0, 0);
                }
            }
            for (int i = 0; i < bolts.size(); i++) {
                Bolt b = bolts.get(i);
                Location base = seeds.get(i);
                Location pos = base.clone().add(b.dir.clone().multiply(t * 2.6));
                b.loc = pos;
                w.spawnParticle(Particle.DUST, pos, 2, 0.02, 0.02, 0.02, 0,
                        new Particle.DustOptions(b.color, 1.15f));
                if (i % 2 == 0) w.spawnParticle(Particle.END_ROD, pos, 1, 0.03, 0.03, 0.03, 0);
                if (i % 4 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 1, 0.02, 0.02, 0.02, 0);
            }
            if (tick >= expandTicks) {
                launched = true;
                w.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.5f);
                w.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.55f, 1.3f);
                Location burst = player.getLocation().add(0, 1, 0);
                w.spawnParticle(Particle.FLASH, burst, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.FIREWORK, burst, 50, 0.6, 0.5, 0.6, 0.12);
                w.spawnParticle(Particle.END_ROD, burst, 30, 0.5, 0.4, 0.5, 0.08);
                // 以准星方向为主的直线扇形（不锁定）
                Vector forward = player.getEyeLocation().getDirection().normalize();
                for (Bolt b : bolts) {
                    double spread = 0.12 + Math.random() * 0.22;
                    Vector jitter = new Vector(
                            (Math.random() - 0.5) * spread,
                            (Math.random() - 0.5) * spread * 0.7,
                            (Math.random() - 0.5) * spread);
                    Vector out = forward.clone().add(jitter);
                    if (out.lengthSquared() < 1e-4) out = forward.clone();
                    b.dir = out.normalize();
                }
            }
            return true;
        }

        Iterator<Bolt> it = bolts.iterator();
        while (it.hasNext()) {
            Bolt b = it.next();
            b.life--;
            if (b.life <= 0) {
                w.spawnParticle(Particle.CLOUD, b.loc, 3, 0.05, 0.05, 0.05, 0.01);
                it.remove();
                continue;
            }
            b.loc = b.loc.clone().add(b.dir.clone().multiply(speed));
            w.spawnParticle(Particle.DUST, b.loc, 2, 0.02, 0.02, 0.02, 0,
                    new Particle.DustOptions(b.color, 1.0f));
            w.spawnParticle(Particle.END_ROD, b.loc, 1, 0.01, 0.01, 0.01, 0);
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, b.loc, 1, 0.02, 0.02, 0.02, 0);
            }

            boolean hitSomething = false;
            for (org.bukkit.entity.Entity e : w.getNearbyEntities(b.loc, hitRadius, hitRadius, hitRadius)) {
                if (!(e instanceof LivingEntity le)) continue;
                if (e.equals(player) || le.isDead()) continue;
                if (isFriendly(le)) continue;

                if (canDodge(le)) {
                    w.spawnParticle(Particle.CLOUD, b.loc, 6, 0.15, 0.15, 0.15, 0.02);
                    w.playSound(b.loc, Sound.ENTITY_BREEZE_IDLE_GROUND, 0.35f, 1.8f);
                    if (le instanceof Player p) {
                        WeaponManager.sendActionBar(p, "&b闪避 &7回声散射", 20);
                    }
                    // 粒子擦过继续飞，不销毁
                    continue;
                }

                EchoDamage.magic(le, player, damage);
                EchoResonance.onHit(le, player, false);
                smallBlast(w, b.loc);
                it.remove();
                hitSomething = true;
                break;
            }
            if (hitSomething) continue;
        }
        return !bolts.isEmpty();
    }

    private boolean canDodge(LivingEntity le) {
        Vector v = le.getVelocity();
        double horiz = Math.hypot(v.getX(), v.getZ());
        boolean sprinting = le instanceof Player p && p.isSprinting();
        if (horiz < dodgeSpeedMin && !sprinting) return false;
        double chance = dodgeChance;
        if (sprinting) chance += 0.08;
        if (horiz > dodgeSpeedMin * 1.6) chance += 0.06;
        return rng.nextDouble() < Math.min(0.45, chance);
    }

    private void smallBlast(World w, Location at) {
        // 视效爆炸，实体走魔法绕甲（不抗魔坦克吃满）
        w.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.55f);
        w.spawnParticle(Particle.EXPLOSION, at, 2, 0.1, 0.1, 0.1, 0);
        w.spawnParticle(Particle.FIREWORK, at, 16, 0.35, 0.35, 0.35, 0.07);
        w.spawnParticle(Particle.FLASH, at, 1, 0, 0, 0, 0);
        for (org.bukkit.entity.Entity e : w.getNearbyEntities(at, explodeRadius, explodeRadius, explodeRadius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e.equals(player) || le.isDead() || isFriendly(le)) continue;
            double d = e.getLocation().distance(at);
            if (d > explodeRadius) continue;
            double scale = 1.0 - d / explodeRadius;
            EchoDamage.magic(le, player, explodeDamage * scale);
        }
    }

    private static boolean isFriendly(LivingEntity le) {
        if (le instanceof Animals) return true;
        if (le instanceof Villager) return true;
        if (le instanceof Tameable t && t.isTamed()) return true;
        return false;
    }

    @Override
    public void cleanup() {
        bolts.clear();
        seeds.clear();
    }

    @Override public UUID getOwnerUUID() { return ownerUUID; }
}
