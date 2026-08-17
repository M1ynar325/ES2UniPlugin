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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Echo Seek「回声寻踪」—
 * 发散 → 停顿 → 逐发制导追击；伤害按发累加，命中后缓慢并叠共鸣层。
 */
public class EchoBarrageSkill implements SkillInstance {

    private enum Phase { EXPAND, HOLD, FIRE, DONE }

    private static final class Bolt {
        Location loc;
        Vector dir;
        int life;
        boolean flying;
        final Color color;

        Bolt(Location loc, Color color) {
            this.loc = loc;
            this.color = color;
            this.flying = false;
            this.life = 0;
        }
    }

    private final Player player;
    private final WeaponManager wm;
    private final UUID ownerUUID;
    private final List<Bolt> bolts = new ArrayList<>();
    private final Random rng = new Random();

    private final double damage;
    private final double speed;
    private final double hitRadius;
    private final double explodePower;
    private final double explodeDamage;
    private final double explodeRadius;
    private final double dodgeChance;
    private final double dodgeSpeedMin;
    private final double targetRange;
    private final int expandTicks;
    private final int holdTicks;
    private final int fireInterval;
    private final int flyTicks;

    private Phase phase = Phase.EXPAND;
    private LivingEntity lockedTarget;
    private int tick;
    private int nextFireIndex;
    private int fireCooldown;
    private Location center;

    public EchoBarrageSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.player = player;
        this.wm = wm;
        this.ownerUUID = player.getUniqueId();

        int count = wm.getCfgInt(SkillType.ECHO_BARRAGE, "particle-count", 16);
        this.damage = wm.getCfgDouble(SkillType.ECHO_BARRAGE, "damage", 5.5);
        this.speed = wm.getCfgDouble(SkillType.ECHO_BARRAGE, "speed", 2.0);
        this.hitRadius = wm.getCfgDouble(SkillType.ECHO_BARRAGE, "hit-radius", 1.05);
        this.explodePower = wm.getCfgDouble(SkillType.ECHO_BARRAGE, "explode-power", 0);
        this.explodeDamage = wm.getCfgDouble(SkillType.ECHO_BARRAGE, "explode-damage", 2.6);
        this.explodeRadius = wm.getCfgDouble(SkillType.ECHO_BARRAGE, "explode-radius", 2.0);
        this.dodgeChance = wm.getCfgDouble(SkillType.ECHO_BARRAGE, "dodge-chance", 0.2);
        this.dodgeSpeedMin = wm.getCfgDouble(SkillType.ECHO_BARRAGE, "dodge-speed-min", 0.28);
        this.targetRange = wm.getCfgDouble(SkillType.ECHO_BARRAGE, "target-range", 36.0);
        this.expandTicks = wm.getCfgInt(SkillType.ECHO_BARRAGE, "expand-ticks", 14);
        this.holdTicks = wm.getCfgInt(SkillType.ECHO_BARRAGE, "hold-ticks", 10);
        this.fireInterval = wm.getCfgInt(SkillType.ECHO_BARRAGE, "fire-interval-ticks", 2);
        this.flyTicks = wm.getCfgInt(SkillType.ECHO_BARRAGE, "fly-ticks", 30);

        this.center = player.getLocation().add(0, 1.1, 0);
        this.lockedTarget = findByCrosshair();

        World w = center.getWorld();
        w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.9f);
        w.playSound(center, Sound.ENTITY_ENDER_EYE_LAUNCH, 0.6f, 1.3f);
        w.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);

        for (int i = 0; i < count; i++) {
            double yaw = (2 * Math.PI * i) / count;
            double pitch = (Math.random() - 0.5) * 0.5;
            Vector radial = new Vector(Math.cos(yaw), pitch * 0.4, Math.sin(yaw)).normalize();
            Location seed = center.clone().add(radial.clone().multiply(0.3));
            float t = (float) i / Math.max(1, count - 1);
            Color c = Color.fromRGB(
                    (int) (100 + 80 * t),
                    (int) (140 + 60 * (1 - t)),
                    (int) (255 - 30 * t));
            Bolt b = new Bolt(seed, c);
            b.dir = radial;
            bolts.add(b);
        }

        if (lockedTarget != null) {
            WeaponManager.sendActionBar(player, "&d回声寻踪 &f锁定 &e" + nameOf(lockedTarget), 40);
        } else {
            WeaponManager.sendActionBar(player, "&7回声寻踪 · 未锁定，将按准星方向射出", 40);
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
        center = player.getLocation().add(0, 1.1, 0);

        if (lockedTarget != null && (!lockedTarget.isValid() || lockedTarget.isDead())) {
            lockedTarget = null;
        }
        // 持续锁定特效 + 允许换锁
        if (tick % 5 == 0) {
            LivingEntity aim = findByCrosshair();
            if (aim != null && (lockedTarget == null || !aim.getUniqueId().equals(lockedTarget.getUniqueId()))) {
                lockedTarget = aim;
                WeaponManager.sendActionBar(player, "&e换锁 &f→ &d" + nameOf(aim), 30);
            }
        }
        if (lockedTarget != null) {
            drawLockFx(lockedTarget);
        }

        return switch (phase) {
            case EXPAND -> tickExpand(w);
            case HOLD -> tickHold(w);
            case FIRE -> tickFire(w);
            case DONE -> false;
        };
    }

    private boolean tickExpand(World w) {
        double t = Math.min(1.0, tick / (double) expandTicks);
        for (int i = 0; i < bolts.size(); i++) {
            Bolt b = bolts.get(i);
            double r = 0.4 + t * 2.4;
            Location pos = center.clone().add(b.dir.clone().multiply(r));
            b.loc = pos;
            w.spawnParticle(Particle.DUST, pos, 2, 0.02, 0.02, 0.02, 0,
                    new Particle.DustOptions(b.color, 1.2f));
            if (i % 3 == 0) w.spawnParticle(Particle.END_ROD, pos, 1, 0.02, 0.02, 0.02, 0);
        }
        if (tick >= expandTicks) {
            phase = Phase.HOLD;
            tick = 0;
            w.playSound(center, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
        }
        return true;
    }

    private boolean tickHold(World w) {
        double ringR = 2.8;
        for (int i = 0; i < bolts.size(); i++) {
            Bolt b = bolts.get(i);
            // 悬浮跟身，避免玩家走开后粒子钉在原地
            Location pos = center.clone().add(b.dir.clone().multiply(ringR));
            pos.add(0, Math.sin((tick + i) * 0.25) * 0.03, 0);
            b.loc = pos;
            w.spawnParticle(Particle.DUST, pos, 1, 0.01, 0.01, 0.01, 0,
                    new Particle.DustOptions(b.color, 0.95f));
            if (i % 5 == 0) w.spawnParticle(Particle.END_ROD, pos, 1, 0.02, 0.02, 0.02, 0);
        }
        if (tick >= holdTicks) {
            phase = Phase.FIRE;
            tick = 0;
            nextFireIndex = 0;
            fireCooldown = 0;
            w.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7f, 1.4f);
        }
        return true;
    }

    private boolean tickFire(World w) {
        if (fireCooldown > 0) fireCooldown--;
        if (fireCooldown <= 0 && nextFireIndex < bolts.size()) {
            Bolt b = bolts.get(nextFireIndex++);
            // 从当前身周环上起飞，避免留在释放点原地
            double ringR = 2.8;
            b.loc = center.clone().add(b.dir.clone().multiply(ringR));
            Vector aim;
            if (lockedTarget != null && lockedTarget.isValid() && !lockedTarget.isDead()) {
                aim = lockedTarget.getEyeLocation().toVector().subtract(b.loc.toVector());
            } else {
                aim = player.getEyeLocation().getDirection();
            }
            if (aim.lengthSquared() < 1e-6) aim = player.getEyeLocation().getDirection();
            b.dir = aim.normalize();
            b.flying = true;
            b.life = flyTicks;
            fireCooldown = fireInterval;
            w.playSound(b.loc, Sound.ENTITY_BREEZE_SHOOT, 0.35f, 1.5f + nextFireIndex * 0.01f);
            w.spawnParticle(Particle.CRIT, b.loc, 3, 0.04, 0.04, 0.04, 0.015);
        }

        Iterator<Bolt> it = bolts.iterator();
        boolean anyAlive = nextFireIndex < bolts.size();
        while (it.hasNext()) {
            Bolt b = it.next();
            if (!b.flying) {
                // 未发射：跟着玩家身周环走，不再钉死在原地
                double ringR = 2.8;
                b.loc = center.clone().add(b.dir.clone().multiply(ringR));
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.DUST, b.loc, 1, 0.01, 0.01, 0.01, 0,
                            new Particle.DustOptions(b.color, 0.75f));
                }
                anyAlive = true;
                continue;
            }
            b.life--;
            if (b.life <= 0) {
                // 飞尽消散
                w.spawnParticle(Particle.CLOUD, b.loc, 3, 0.08, 0.08, 0.08, 0.01);
                it.remove();
                continue;
            }
            // 飞行中持续制导（软转向）
            if (lockedTarget != null && lockedTarget.isValid() && !lockedTarget.isDead()) {
                Vector desired = lockedTarget.getEyeLocation().toVector().subtract(b.loc.toVector());
                if (desired.lengthSquared() > 1e-4) {
                    desired.normalize();
                    b.dir = b.dir.clone().multiply(0.62).add(desired.multiply(0.38));
                    if (b.dir.lengthSquared() > 1e-6) b.dir.normalize();
                }
            }
            b.loc = b.loc.clone().add(b.dir.clone().multiply(speed));
            w.spawnParticle(Particle.DUST, b.loc, 1, 0.015, 0.015, 0.015, 0,
                    new Particle.DustOptions(b.color, 1.0f));
            if (b.life % 2 == 0) {
                w.spawnParticle(Particle.END_ROD, b.loc, 1, 0.01, 0.01, 0.01, 0);
            }

            boolean hit = false;
            for (org.bukkit.entity.Entity e : w.getNearbyEntities(b.loc, hitRadius, hitRadius, hitRadius)) {
                if (!(e instanceof LivingEntity le)) continue;
                if (e.equals(player) || le.isDead() || isFriendly(le)) continue;
                if (canDodge(le)) {
                    w.spawnParticle(Particle.CLOUD, b.loc, 5, 0.12, 0.12, 0.12, 0.02);
                    continue;
                }
                // A：每发独立魔法伤（绕甲）→ 再叠共鸣/缓慢
                EchoDamage.magic(le, player, damage);
                EchoResonance.onHit(le, player, true);
                smallBlast(w, b.loc);
                it.remove();
                hit = true;
                break;
            }
            if (!hit) anyAlive = true;
        }

        // 无目标/异常兜底：超时强制结束，避免残留
        if (tick > bolts.size() * Math.max(1, fireInterval) + flyTicks + 40) {
            bolts.clear();
            phase = Phase.DONE;
            return false;
        }

        if (!anyAlive && nextFireIndex >= bolts.size()) {
            phase = Phase.DONE;
            return false;
        }
        return true;
    }

    /** 轻量锁定标：稀疏、不喧宾夺主 */
    private void drawLockFx(LivingEntity target) {
        if (tick % 6 != 0) return;
        Location t = target.getLocation().add(0, 1.05, 0);
        World tw = target.getWorld();
        tw.spawnParticle(Particle.DUST, t, 2, 0.12, 0.18, 0.12, 0,
                new Particle.DustOptions(Color.fromRGB(120, 190, 255), 0.7f));
        double spin = tick * 0.1;
        for (int i = 0; i < 3; i++) {
            double a = spin + Math.PI * 2 / 3 * i;
            tw.spawnParticle(Particle.END_ROD,
                    t.clone().add(Math.cos(a) * 0.75, 0.02, Math.sin(a) * 0.75),
                    1, 0, 0, 0, 0);
        }
        if (target instanceof Player victim && tick % 20 == 0) {
            WeaponManager.sendActionBar(victim,
                    "&b锁定 · &f" + player.getName(), 20);
        }
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

    private boolean canDodge(LivingEntity le) {
        Vector v = le.getVelocity();
        double horiz = Math.hypot(v.getX(), v.getZ());
        boolean sprinting = le instanceof Player p && p.isSprinting();
        if (horiz < dodgeSpeedMin && !sprinting) return false;
        double chance = dodgeChance + (sprinting ? 0.08 : 0) + (horiz > dodgeSpeedMin * 1.6 ? 0.06 : 0);
        return rng.nextDouble() < Math.min(0.45, chance);
    }

    private void smallBlast(World w, Location at) {
        // 仅视效，实体伤走魔法绕甲
        w.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.45f, 1.65f);
        w.spawnParticle(Particle.EXPLOSION, at, 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.FIREWORK, at, 12, 0.28, 0.28, 0.28, 0.05);
        for (org.bukkit.entity.Entity e : w.getNearbyEntities(at, explodeRadius, explodeRadius, explodeRadius)) {
            if (!(e instanceof LivingEntity le) || e.equals(player) || le.isDead() || isFriendly(le))
                continue;
            double d = e.getLocation().distance(at);
            if (d > explodeRadius) continue;
            EchoDamage.magic(le, player, explodeDamage * (1.0 - d / explodeRadius));
        }
    }

    private static boolean isFriendly(LivingEntity le) {
        if (le instanceof Animals) return true;
        if (le instanceof Villager) return true;
        return le instanceof Tameable t && t.isTamed();
    }

    private static boolean isLockable(LivingEntity le) {
        if (le instanceof Player) return true;
        if (le instanceof Monster) return true;
        if (le instanceof Animals || le instanceof Villager) return false;
        return true;
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
