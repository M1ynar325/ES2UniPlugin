package com.etherstories.escore.weapons.skills;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.WeaponManager;
import com.etherstories.escore.weapons.FlyingSwordVisual;
import com.etherstories.escore.weapons.SkillInstance;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.*;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Resonance Lock「共鸣锁定」—
 * 准星指定玩家/敌对；技能持续期间看向新目标可换锁。
 */
public class HomingSkill implements SkillInstance {

    private final Player        player;
    private final WeaponManager wm;
    private final UUID          ownerUUID;

    private final int    ringCount;
    private final int    perRing;
    private final double speed;
    private final int    maxLifetime;
    private final double targetRange;
    private final double damage;

    private final List<FlyingSwordVisual> swords = new ArrayList<>();
    private final List<Double> swordAngles = new ArrayList<>();
    private LivingEntity lockedTarget = null;
    private int tick = 0;

    public HomingSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.player    = player;
        this.wm        = wm;
        this.ownerUUID = player.getUniqueId();

        ringCount   = wm.getCfgInt(SkillType.HOMING, "rings", 3);
        int legacy  = wm.getCfgInt(SkillType.HOMING, "sword-count", 15);
        perRing     = wm.getCfgInt(SkillType.HOMING, "per-ring",
                Math.max(4, legacy / Math.max(1, ringCount)));
        speed       = wm.getCfgDouble(SkillType.HOMING, "speed", 0.85);
        maxLifetime = wm.getCfgInt(SkillType.HOMING, "max-lifetime-ticks", 220);
        targetRange = wm.getCfgDouble(SkillType.HOMING, "target-range", 32.0);
        damage      = wm.getCfgDouble(SkillType.HOMING, "damage", 6.0);

        lockedTarget = findByCrosshair();
        spawnSwords();
        if (lockedTarget != null) {
            playLockFx(lockedTarget, false);
        } else {
            WeaponManager.sendActionBar(player, "&7准星对准玩家/敌对再释放；飞剑待命中可换锁");
        }
    }

    private void playLockFx(LivingEntity target, boolean retarget) {
        player.getWorld().playSound(player.getLocation(),
                retarget ? Sound.BLOCK_NOTE_BLOCK_CHIME : Sound.BLOCK_NOTE_BLOCK_BELL,
                0.8f, retarget ? 1.8f : 1.4f);
        Location lockFx = target.getLocation().add(0, 1.2, 0);
        World lw = target.getWorld();
        lw.spawnParticle(Particle.FLASH, lockFx, 1, 0, 0, 0, 0);
        lw.spawnParticle(Particle.WITCH, lockFx, retarget ? 20 : 40, 0.5, 0.7, 0.5, 0.04);
        lw.spawnParticle(Particle.DUST, lockFx, 30, 0.45, 0.6, 0.45, 0,
                new Particle.DustOptions(Color.fromRGB(200, 90, 255), 1.5f));
        for (int i = 0; i < 20; i++) {
            double a = (Math.PI * 2 / 20) * i;
            lw.spawnParticle(Particle.END_ROD,
                    lockFx.clone().add(Math.cos(a) * 1.3, 0, Math.sin(a) * 1.3),
                    1, 0, 0, 0, 0);
        }
        WeaponManager.sendActionBar(player,
                (retarget ? "&e换锁 &f→ &d" : "&d共鸣锁定 &f→ &e") + nameOf(target), 40);
    }

    private void spawnSwords() {
        Location origin = player.getLocation().add(0, 1.2, 0);
        World world = origin.getWorld();
        int total = 0;
        for (int r = 0; r < ringCount; r++) {
            double radius = 1.2 + r * 0.85;
            double yOff = 0.25 + r * 0.2;
            for (int i = 0; i < perRing; i++) {
                double angle = (2 * Math.PI / perRing) * i + r * 0.35;
                Location loc = origin.clone().add(
                        radius * Math.cos(angle), yOff, radius * Math.sin(angle));
                Location face = loc.clone();
                face.setYaw((float) Math.toDegrees(angle) + 90f);
                face.setPitch(-15f);
                FlyingSwordVisual sword = FlyingSwordVisual.spawn(face, wm, false);
                swords.add(sword);
                swordAngles.add(angle);
                world.spawnParticle(Particle.CRIT, loc, 4, 0.1, 0.1, 0.1, 0.02);
                total++;
            }
        }
        world.playSound(origin, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.2f);
        world.spawnParticle(Particle.NOTE, origin, Math.min(total * 2, 48), 0.7, 0.5, 0.7, 0);
        world.spawnParticle(Particle.END_ROD, origin, 28, 0.5, 0.6, 0.5, 0.04);
        world.spawnParticle(Particle.ENCHANT, origin, 50, 0.8, 0.6, 0.8, 0.4);
        world.spawnParticle(Particle.DUST, origin, 40, 0.8, 0.5, 0.8, 0,
                new Particle.DustOptions(Color.fromRGB(160, 100, 255), 1.3f));
    }

    @Override
    public boolean tick() {
        tick++;
        if (tick >= maxLifetime || swords.isEmpty()) {
            cleanup();
            return false;
        }

        if (lockedTarget != null && (!lockedTarget.isValid() || lockedTarget.isDead())) {
            lockedTarget = null;
            WeaponManager.sendActionBar(player, "&7目标消失 · 准星对准可换锁", 30);
        }

        // 每 5 tick：准星对准新的可锁目标则换锁
        if (tick % 5 == 0) {
            LivingEntity aim = findByCrosshair();
            if (aim != null && (lockedTarget == null || !aim.getUniqueId().equals(lockedTarget.getUniqueId()))) {
                lockedTarget = aim;
                playLockFx(aim, true);
            }
        }

        if (lockedTarget != null) {
            // 轻量锁定：每 6 tick 一帧，3 点环，不糊脸
            if (tick % 6 == 0) {
                Location t = lockedTarget.getLocation().add(0, 1.05, 0);
                World tw = lockedTarget.getWorld();
                tw.spawnParticle(Particle.DUST, t, 2, 0.12, 0.18, 0.12, 0,
                        new Particle.DustOptions(Color.fromRGB(180, 100, 255), 0.7f));
                double spin = tick * 0.1;
                for (int i = 0; i < 3; i++) {
                    double a = spin + (Math.PI * 2 / 3) * i;
                    tw.spawnParticle(Particle.END_ROD,
                            t.clone().add(Math.cos(a) * 0.75, 0.02, Math.sin(a) * 0.75),
                            1, 0, 0, 0, 0);
                }
            }
            if (lockedTarget instanceof Player victim && tick % 20 == 0) {
                WeaponManager.sendActionBar(victim,
                        "&d锁定 · &f" + player.getName(), 20);
            }
            if (tick % 20 == 0) {
                WeaponManager.sendActionBar(player, "&d锁定中 &f" + nameOf(lockedTarget)
                        + " &8· 准星可换锁", 25);
            }
        }

        Iterator<FlyingSwordVisual> it = swords.iterator();
        int idx = 0;
        while (it.hasNext()) {
            FlyingSwordVisual sword = it.next();
            if (sword.dead()) {
                it.remove();
                if (idx < swordAngles.size()) swordAngles.remove(idx);
                continue;
            }

            if (lockedTarget == null) {
                orbitPlayer(sword, idx);
                idx++;
                continue;
            }

            Location tLoc = lockedTarget.getLocation().add(0, 1, 0);
            Vector dir = tLoc.clone().subtract(sword.getLocation()).toVector();
            double dist = dir.length();
            if (dist < 1.0) {
                onHit(lockedTarget, sword.getLocation());
                sword.remove();
                it.remove();
                if (idx < swordAngles.size()) swordAngles.remove(idx);
                continue;
            }
            dir.normalize().multiply(Math.min(speed, dist * 0.5 + 0.3));
            Location next = sword.getLocation().add(dir);
            sword.move(next, dir);
            World sw = sword.getLocation().getWorld();
            sw.spawnParticle(Particle.END_ROD, sword.getLocation(), 2, 0.04, 0.04, 0.04, 0.015);
            sw.spawnParticle(Particle.DUST, sword.getLocation(), 2, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 120, 255), 0.8f));
            if (tick % 2 == 0) {
                sw.spawnParticle(Particle.CRIT, sword.getLocation(), 1, 0.02, 0.02, 0.02, 0);
            }
            idx++;
        }
        return true;
    }

    private void orbitPlayer(FlyingSwordVisual sword, int idx) {
        Location pl = player.getLocation().add(0, 1.35, 0);
        double spawnAngle = idx < swordAngles.size() ? swordAngles.get(idx) : idx * 0.7;
        // 固定初始角 + 匀速公转（不要每帧把角度写回累加）
        double a = spawnAngle + tick * 0.08;
        double ring = 1.15 + (idx % Math.max(1, ringCount)) * 0.55;
        Location orbit = pl.clone().add(Math.cos(a) * ring, 0.15, Math.sin(a) * ring);
        orbit.setYaw((float) Math.toDegrees(a) + 90f);
        orbit.setPitch(-20f);
        sword.hoverTo(orbit);
    }

    private void onHit(LivingEntity target, Location loc) {
        target.damage(damage, player);
        World w = loc.getWorld();
        w.spawnParticle(Particle.CRIT, loc, 28, 0.35, 0.35, 0.35, 0.2);
        w.spawnParticle(Particle.SWEEP_ATTACK, loc, 3, 0.2, 0.2, 0.2, 0);
        w.spawnParticle(Particle.DUST, loc, 20, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(255, 180, 255), 1.4f));
        w.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.4f);
        w.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 1.8f);
    }

    /** 优先准星射线实体；否则视线锥内最近可锁目标。 */
    private LivingEntity findByCrosshair() {
        RayTraceResult ray = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                targetRange,
                0.45,
                e -> e instanceof LivingEntity le
                        && !e.equals(player)
                        && !le.isDead()
                        && isLockable(le));
        if (ray != null && ray.getHitEntity() instanceof LivingEntity hit) {
            return hit;
        }

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection();

        for (org.bukkit.entity.Entity e : player.getWorld()
                .getNearbyEntities(player.getLocation(), targetRange, targetRange, targetRange)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e.equals(player) || le.isDead() || !isLockable(le)) continue;

            Vector to = le.getEyeLocation().toVector().subtract(eye.toVector());
            double dist = to.length();
            if (dist < 0.2 || dist > targetRange) continue;
            to.multiply(1.0 / dist);
            double dot = look.dot(to);
            if (dot < 0.82) continue; // 较窄准星锥
            double score = dist * (2.0 - dot);
            if (score < bestScore) {
                bestScore = score;
                best = le;
            }
        }
        return best;
    }

    private static boolean isLockable(LivingEntity le) {
        if (le instanceof Player) return true;
        if (le instanceof Monster) return true;
        if (le instanceof Animals) return false;
        if (le instanceof Villager) return false;
        return true;
    }

    private static String nameOf(LivingEntity le) {
        if (le instanceof Player p) return p.getName();
        return le.getType().name();
    }

    @Override
    public void cleanup() {
        for (FlyingSwordVisual s : swords) s.remove();
        swords.clear();
    }

    @Override public UUID getOwnerUUID() { return ownerUUID; }
}
