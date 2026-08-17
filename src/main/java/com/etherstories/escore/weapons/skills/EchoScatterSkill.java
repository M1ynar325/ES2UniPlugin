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
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 回声散射：身周（含上空）展开粒子，再逐发制导射向准星目标；命中短暂失明。
 */
public class EchoScatterSkill implements SkillInstance {

    private static final class Bolt {
        Location loc;
        Vector radial;
        Vector dir;
        int life;
        boolean flying;
        final Color color;

        Bolt(Location loc, Vector radial, Color color) {
            this.loc = loc;
            this.radial = radial;
            this.dir = radial.clone();
            this.color = color;
            this.flying = false;
            this.life = 0;
        }
    }

    private final Player player;
    private final UUID ownerUUID;
    private final double damage;
    private final double speed;
    private final double hitRadius;
    private final double explodeDamage;
    private final double explodeRadius;
    private final double dodgeChance;
    private final double dodgeSpeedMin;
    private final double targetRange;
    private final int expandTicks;
    private final int fireInterval;
    private final int flyTicks;
    private final int blindnessTicks;
    private final List<Bolt> bolts = new ArrayList<>();
    private final Random rng = new Random();
    private int tick = 0;
    private boolean launched = false;
    private LivingEntity lockedTarget;
    private int nextFireIndex;
    private int fireCooldown;
    private final int boltCount;
    private Location origin;

    public EchoScatterSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.player = player;
        this.ownerUUID = player.getUniqueId();
        int count = wm.getCfgInt(SkillType.ECHO_SCATTER, "particle-count", 52);
        this.damage = wm.getCfgDouble(SkillType.ECHO_SCATTER, "damage", 6.5);
        this.speed = wm.getCfgDouble(SkillType.ECHO_SCATTER, "speed", 2.4);
        this.hitRadius = wm.getCfgDouble(SkillType.ECHO_SCATTER, "hit-radius", 1.0);
        this.expandTicks = wm.getCfgInt(SkillType.ECHO_SCATTER, "expand-ticks", 8);
        this.fireInterval = Math.max(1, wm.getCfgInt(SkillType.ECHO_SCATTER, "fire-interval-ticks", 1));
        this.flyTicks = wm.getCfgInt(SkillType.ECHO_SCATTER, "fly-ticks", 34);
        this.explodeDamage = wm.getCfgDouble(SkillType.ECHO_SCATTER, "explode-damage", 3.2);
        this.explodeRadius = wm.getCfgDouble(SkillType.ECHO_SCATTER, "explode-radius", 1.6);
        this.dodgeChance = wm.getCfgDouble(SkillType.ECHO_SCATTER, "dodge-chance", 0.22);
        this.dodgeSpeedMin = wm.getCfgDouble(SkillType.ECHO_SCATTER, "dodge-speed-min", 0.28);
        this.targetRange = wm.getCfgDouble(SkillType.ECHO_SCATTER, "target-range", 36.0);
        this.blindnessTicks = wm.getCfgInt(SkillType.ECHO_SCATTER, "blindness-ticks", 30);

        this.origin = player.getLocation().add(0, 1.0, 0)
                .subtract(player.getLocation().getDirection().normalize().multiply(0.8));
        this.lockedTarget = findByCrosshair();
        World w = origin.getWorld();
        w.playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, 1.35f);
        w.playSound(origin, Sound.BLOCK_BELL_RESONATE, 0.45f, 1.8f);
        w.playSound(origin, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 0.7f, 1.6f);
        w.playSound(origin, Sound.ENTITY_ENDER_EYE_LAUNCH, 0.45f, 1.7f);
        w.spawnParticle(Particle.FLASH, origin, 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.SONIC_BOOM, origin.clone().add(0, 0.2, 0), 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.FIREWORK, origin, 55, 0.55, 0.55, 0.55, 0.12);
        w.spawnParticle(Particle.END_ROD, origin, 50, 0.6, 0.7, 0.6, 0.08);
        w.spawnParticle(Particle.GLOW, origin, 24, 0.4, 0.5, 0.4, 0.02);
        w.spawnParticle(Particle.ELECTRIC_SPARK, origin, 28, 0.5, 0.5, 0.5, 0.08);

        int airCount = Math.max(8, count / 3);
        for (int i = 0; i < count; i++) {
            double yaw = (2 * Math.PI * i) / count + rng.nextDouble() * 0.08;
            double pitch;
            if (i < airCount) {
                pitch = 0.55 + rng.nextDouble() * 0.85;
            } else {
                pitch = (rng.nextDouble() - 0.42) * 0.75;
            }
            Vector radial = new Vector(Math.cos(yaw), pitch, Math.sin(yaw));
            if (radial.lengthSquared() < 1e-6) radial = new Vector(0, 1, 0);
            radial.normalize();
            float t = (float) i / Math.max(1, count - 1);
            Color c = Color.fromRGB(
                    (int) (70 + 90 * t),
                    (int) (190 + 50 * (1 - t)),
                    255);
            bolts.add(new Bolt(origin.clone(), radial, c));
        }
        this.boltCount = bolts.size();

        if (lockedTarget != null) {
            WeaponManager.sendActionBar(player, "&b回声散射 &f目标 &e" + nameOf(lockedTarget), 30);
        } else {
            WeaponManager.sendActionBar(player, "&7回声散射 · 未锁定，将按准星方向射出", 30);
        }
    }

    @Override
    public boolean tick() {
        tick++;
        if (!player.isOnline()) {
            cleanup();
            return false;
        }
        World w = player.getWorld();
        origin = player.getLocation().add(0, 1.0, 0)
                .subtract(player.getLocation().getDirection().normalize().multiply(0.55));

        if (lockedTarget != null && (!lockedTarget.isValid() || lockedTarget.isDead())) {
            lockedTarget = null;
        }
        if (tick % 5 == 0) {
            LivingEntity aim = findByCrosshair();
            if (aim != null && (lockedTarget == null || !aim.getUniqueId().equals(lockedTarget.getUniqueId()))) {
                lockedTarget = aim;
            }
        }

        if (!launched) {
            return tickExpand(w);
        }
        return tickFire(w);
    }

    private boolean tickExpand(World w) {
        double t = Math.min(1.0, tick / (double) expandTicks);
        double bloom = t * 3.15;
        drawHalo(w, origin, bloom, tick);

        for (int i = 0; i < bolts.size(); i++) {
            Bolt b = bolts.get(i);
            Location pos = origin.clone().add(b.radial.clone().multiply(bloom));
            b.loc = pos;
            float size = 1.05f + (float) (0.45 * t);
            w.spawnParticle(Particle.DUST, pos, 3, 0.03, 0.03, 0.03, 0,
                    new Particle.DustOptions(b.color, size));
            if (i % 2 == 0) w.spawnParticle(Particle.END_ROD, pos, 1, 0.04, 0.04, 0.04, 0.01);
            if (i % 3 == 0) w.spawnParticle(Particle.GLOW, pos, 1, 0.02, 0.02, 0.02, 0);
            if (i % 5 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 1, 0.05, 0.05, 0.05, 0.02);
        }
        if (tick >= expandTicks) {
            launched = true;
            tick = 0;
            nextFireIndex = 0;
            fireCooldown = 0;
            w.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.9f, 1.7f);
            w.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.7f, 1.45f);
            w.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.6f);
            w.spawnParticle(Particle.FLASH, origin, 1, 0, 0, 0, 0);
            w.spawnParticle(Particle.SONIC_BOOM, origin, 1, 0, 0, 0, 0);
            w.spawnParticle(Particle.FIREWORK, origin, 70, 0.7, 0.8, 0.7, 0.16);
            w.spawnParticle(Particle.END_ROD, origin, 40, 0.6, 0.7, 0.6, 0.1);
            w.spawnParticle(Particle.GLOW, origin, 20, 0.5, 0.5, 0.5, 0.04);
            drawHalo(w, origin, 3.2, 0);
        }
        return true;
    }

    private boolean tickFire(World w) {
        if (fireCooldown > 0) fireCooldown--;
        if (fireCooldown <= 0 && nextFireIndex < bolts.size()) {
            Bolt b = bolts.get(nextFireIndex++);
            b.loc = origin.clone().add(b.radial.clone().multiply(2.8));
            b.dir = aimFrom(b.loc);
            b.flying = true;
            b.life = flyTicks;
            fireCooldown = fireInterval;
            w.playSound(b.loc, Sound.ENTITY_BREEZE_SHOOT, 0.32f, 1.55f + nextFireIndex * 0.01f);
            w.playSound(b.loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.22f, 1.8f);
            w.spawnParticle(Particle.FIREWORK, b.loc, 6, 0.06, 0.06, 0.06, 0.04);
            w.spawnParticle(Particle.ELECTRIC_SPARK, b.loc, 5, 0.08, 0.08, 0.08, 0.03);
            w.spawnParticle(Particle.GLOW, b.loc, 2, 0.04, 0.04, 0.04, 0);
        }

        Iterator<Bolt> it = bolts.iterator();
        boolean anyAlive = nextFireIndex < bolts.size();
        while (it.hasNext()) {
            Bolt b = it.next();
            if (!b.flying) {
                double hover = 2.8 + Math.sin((tick + iHash(b)) * 0.28) * 0.12;
                b.loc = origin.clone().add(b.radial.clone().multiply(hover));
                w.spawnParticle(Particle.DUST, b.loc, 2, 0.02, 0.02, 0.02, 0,
                        new Particle.DustOptions(b.color, 1.05f));
                if (tick % 2 == 0) w.spawnParticle(Particle.END_ROD, b.loc, 1, 0.02, 0.03, 0.02, 0);
                if (tick % 3 == 0) w.spawnParticle(Particle.GLOW, b.loc, 1, 0.03, 0.03, 0.03, 0);
                anyAlive = true;
                continue;
            }
            b.life--;
            if (b.life <= 0) {
                w.spawnParticle(Particle.CLOUD, b.loc, 3, 0.05, 0.05, 0.05, 0.01);
                it.remove();
                continue;
            }
            if (lockedTarget != null && lockedTarget.isValid() && !lockedTarget.isDead()) {
                Vector desired = lockedTarget.getEyeLocation().toVector().subtract(b.loc.toVector());
                if (desired.lengthSquared() > 1e-4) {
                    desired.normalize();
                    b.dir = b.dir.clone().multiply(0.58).add(desired.multiply(0.42));
                    if (b.dir.lengthSquared() > 1e-6) b.dir.normalize();
                }
            }
            b.loc = b.loc.clone().add(b.dir.clone().multiply(speed));
            w.spawnParticle(Particle.DUST, b.loc, 3, 0.03, 0.03, 0.03, 0,
                    new Particle.DustOptions(b.color, 1.25f));
            Location wake = b.loc.clone().subtract(b.dir.clone().multiply(0.45));
            w.spawnParticle(Particle.DUST, wake, 2, 0.04, 0.04, 0.04, 0,
                    new Particle.DustOptions(b.color, 0.7f));
            w.spawnParticle(Particle.END_ROD, b.loc, 1, 0.02, 0.02, 0.02, 0.01);
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.ELECTRIC_SPARK, b.loc, 2, 0.05, 0.05, 0.05, 0.04);
                w.spawnParticle(Particle.GLOW, b.loc, 1, 0.03, 0.03, 0.03, 0);
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
                    continue;
                }

                EchoDamage.magic(le, player, damage);
                EchoResonance.onHit(le, player, false);
                applyBlind(le);
                smallBlast(w, b.loc);
                it.remove();
                hitSomething = true;
                break;
            }
            if (hitSomething) continue;
            anyAlive = true;
        }

        if (tick > boltCount * fireInterval + flyTicks + 40) {
            bolts.clear();
            return false;
        }
        return anyAlive || nextFireIndex < bolts.size();
    }

    private Vector aimFrom(Location from) {
        Vector aim;
        if (lockedTarget != null && lockedTarget.isValid() && !lockedTarget.isDead()) {
            aim = lockedTarget.getEyeLocation().toVector().subtract(from.toVector());
        } else {
            aim = player.getEyeLocation().getDirection();
        }
        if (aim.lengthSquared() < 1e-6) aim = player.getEyeLocation().getDirection();
        return aim.normalize();
    }

    private LivingEntity findByCrosshair() {
        RayTraceResult ray = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                targetRange,
                0.45,
                e -> e instanceof LivingEntity le
                        && !e.equals(player) && !le.isDead() && isLockable(le));
        if (ray != null && ray.getHitEntity() instanceof LivingEntity hit) return hit;

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection();
        for (org.bukkit.entity.Entity e : player.getWorld()
                .getNearbyEntities(player.getLocation(), targetRange, targetRange, targetRange)) {
            if (!(e instanceof LivingEntity le) || e.equals(player) || le.isDead() || !isLockable(le))
                continue;
            Vector to = le.getEyeLocation().toVector().subtract(eye.toVector());
            double dist = to.length();
            if (dist < 0.2 || dist > targetRange) continue;
            to.multiply(1.0 / dist);
            double dot = look.dot(to);
            if (dot < 0.8) continue;
            double score = dist * (2.0 - dot);
            if (score < bestScore) { bestScore = score; best = le; }
        }
        return best;
    }

    private void applyBlind(LivingEntity le) {
        if (blindnessTicks <= 0) return;
        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessTicks, 0, false, true, true));
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
        w.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.45f, 1.7f);
        w.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.7f, 1.85f);
        w.spawnParticle(Particle.EXPLOSION, at, 2, 0.12, 0.12, 0.12, 0);
        w.spawnParticle(Particle.FLASH, at, 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.FIREWORK, at, 28, 0.4, 0.4, 0.4, 0.1);
        w.spawnParticle(Particle.END_ROD, at, 14, 0.3, 0.3, 0.3, 0.06);
        w.spawnParticle(Particle.ELECTRIC_SPARK, at, 16, 0.35, 0.35, 0.35, 0.08);
        w.spawnParticle(Particle.GLOW, at, 8, 0.25, 0.25, 0.25, 0.03);
        w.spawnParticle(Particle.DUST, at, 10, 0.28, 0.28, 0.28, 0,
                new Particle.DustOptions(Color.fromRGB(160, 230, 255), 1.4f));
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

    private static boolean isLockable(LivingEntity le) {
        if (le instanceof Player) return true;
        if (le instanceof Monster) return true;
        if (le instanceof Animals || le instanceof Villager) return false;
        return true;
    }

    private static void drawHalo(World w, Location origin, double radius, int spin) {
        Color ring = Color.fromRGB(120, 220, 255);
        for (int i = 0; i < 24; i++) {
            double a = (Math.PI * 2 / 24) * i + spin * 0.14;
            Location p = origin.clone().add(Math.cos(a) * radius, 0.15 + Math.sin(spin * 0.2) * 0.12, Math.sin(a) * radius);
            w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, new Particle.DustOptions(ring, 1.05f));
            if (i % 3 == 0) w.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0);
        }
        double r2 = radius * 0.62;
        for (int i = 0; i < 14; i++) {
            double a = (Math.PI * 2 / 14) * i - spin * 0.18;
            Location p = origin.clone().add(Math.cos(a) * r2, 1.15 + radius * 0.22, Math.sin(a) * r2);
            w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(200, 240, 255), 0.85f));
            if (i % 2 == 0) w.spawnParticle(Particle.GLOW, p, 1, 0, 0, 0, 0);
        }
    }

    private static int iHash(Bolt b) {
        return System.identityHashCode(b);
    }

    private static String nameOf(LivingEntity le) {
        return le instanceof Player p ? p.getName() : le.getType().name();
    }

    @Override
    public void cleanup() {
        bolts.clear();
    }

    @Override public UUID getOwnerUUID() { return ownerUUID; }
}
