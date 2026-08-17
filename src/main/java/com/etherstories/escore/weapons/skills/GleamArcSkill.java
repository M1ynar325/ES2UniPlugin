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
import org.bukkit.entity.Villager;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Gleam Arc「霁弧」— 白/霁青弧钉住一名目标，随后同一人连挨落雷。
 */
public class GleamArcSkill implements SkillInstance {

    private static final Color WHITE = Color.fromRGB(245, 250, 255);
    private static final Color JIQING = Color.fromRGB(110, 210, 200);

    private final Player player;
    private final UUID ownerUUID;
    private final double range;
    private final double followRange;
    private final double damage;
    private final int strikeCount;
    private final int strikeInterval;
    private final boolean realLightning;

    private LivingEntity target;
    private int tick;
    private int strikesDone;
    private int nextStrikeTick;

    public GleamArcSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.player = player;
        this.ownerUUID = player.getUniqueId();
        this.range = wm.getCfgDouble(SkillType.GLEAM_ARC, "range", 16.0);
        this.followRange = wm.getCfgDouble(SkillType.GLEAM_ARC, "follow-range", 40.0);
        this.damage = wm.getCfgDouble(SkillType.GLEAM_ARC, "damage", 4.0);
        this.strikeCount = Math.max(1, wm.getCfgInt(SkillType.GLEAM_ARC, "strike-count", 4));
        this.strikeInterval = Math.max(4, wm.getCfgInt(SkillType.GLEAM_ARC, "strike-interval-ticks", 12));
        this.realLightning = plugin.getConfig().getBoolean(
                "weapons.skills.gleam_arc.real-lightning", false);
        recast();
    }

    public boolean isLocked() {
        return target != null && target.isValid() && !target.isDead();
    }

    public void recast() {
        LivingEntity next = findByCrosshair();
        Location from = player.getEyeLocation();
        Location dest = next != null ? next.getEyeLocation() : missPoint(from);
        drawArc(from, dest);
        if (next == null) {
            WeaponManager.sendActionBar(player, "&7霁弧 · 未锁定", 25);
            return;
        }
        target = next;
        tick = 0;
        strikesDone = 0;
        nextStrikeTick = 2;
        playLockFx(next);
        WeaponManager.sendActionBar(player, "&b霁弧 &f→ &e" + nameOf(next), 40);
    }

    @Override
    public boolean tick() {
        tick++;
        if (!player.isOnline() || player.isDead()) {
            cleanup();
            return false;
        }
        if (!isLocked()) {
            cleanup();
            return false;
        }
        if (target.getWorld() != player.getWorld()) {
            cleanup();
            return false;
        }
        if (target.getLocation().distanceSquared(player.getLocation()) > followRange * followRange) {
            WeaponManager.sendActionBar(player, "&7霁弧 · 目标过远", 25);
            cleanup();
            return false;
        }

        if (tick % 5 == 0) {
            drawBrand(target);
        }
        if (target instanceof Player victim && tick % 20 == 0) {
            WeaponManager.sendActionBar(victim, "&b霁弧 · &f" + player.getName(), 20);
        }

        if (strikesDone < strikeCount && tick >= nextStrikeTick) {
            strike(target);
            strikesDone++;
            nextStrikeTick = tick + strikeInterval;
        }

        if (strikesDone >= strikeCount && tick >= nextStrikeTick) {
            cleanup();
            return false;
        }
        return true;
    }

    private void strike(LivingEntity le) {
        Location loc = le.getLocation();
        World w = loc.getWorld();
        if (w == null) return;
        if (realLightning) {
            w.strikeLightning(loc);
        } else {
            w.strikeLightningEffect(loc);
        }
        EchoDamage.magic(le, player, damage);
        Location mid = loc.clone().add(0, 1.1, 0);
        w.spawnParticle(Particle.ELECTRIC_SPARK, mid, 22, 0.28, 0.7, 0.28, 0.12);
        w.spawnParticle(Particle.DUST, mid, 16, 0.25, 0.55, 0.25, 0,
                new Particle.DustOptions(JIQING, 1.3f));
        w.spawnParticle(Particle.END_ROD, mid, 6, 0.15, 0.4, 0.15, 0.02);
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.55f, 1.4f);
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.28f, 1.85f);
    }

    private void drawArc(Location from, Location to) {
        World w = from.getWorld();
        if (w == null || to.getWorld() != w) return;
        Vector delta = to.toVector().subtract(from.toVector());
        double len = delta.length();
        if (len < 0.05) return;
        Vector step = delta.normalize().multiply(0.28);
        Location p = from.clone();
        int n = (int) (len / 0.28);
        for (int i = 0; i <= n; i++) {
            boolean cyan = (i % 2) == 0;
            w.spawnParticle(Particle.DUST, p, 2, 0.02, 0.02, 0.02, 0,
                    new Particle.DustOptions(cyan ? JIQING : WHITE, cyan ? 1.2f : 1.0f));
            if (i % 3 == 0) w.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0);
            if (i % 4 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.04, 0.04, 0.04, 0.01);
            p.add(step);
        }
        w.playSound(from, Sound.ENTITY_EVOKER_CAST_SPELL, 0.7f, 1.65f);
        w.playSound(to, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
    }

    private void playLockFx(LivingEntity le) {
        Location t = le.getLocation().add(0, 1.15, 0);
        World w = t.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.FLASH, t, 1, 0, 0, 0, 0);
        drawBrand(le);
        w.playSound(t, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.75f, 1.7f);
    }

    private void drawBrand(LivingEntity le) {
        Location t = le.getLocation().add(0, le.getHeight() + 0.15, 0);
        World w = t.getWorld();
        if (w == null) return;
        double spin = tick * 0.18;
        for (int i = 0; i < 8; i++) {
            double a = spin + (Math.PI * 2 / 8) * i;
            Location p = t.clone().add(Math.cos(a) * 0.55, 0.02, Math.sin(a) * 0.55);
            boolean cyan = (i % 2) == 0;
            w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(cyan ? JIQING : WHITE, 0.9f));
        }
    }

    private Location missPoint(Location eye) {
        RayTraceResult hit = player.getWorld().rayTraceBlocks(eye, eye.getDirection(), range);
        if (hit != null && hit.getHitPosition() != null) {
            return hit.getHitPosition().toLocation(eye.getWorld());
        }
        return eye.clone().add(eye.getDirection().multiply(range));
    }

    private LivingEntity findByCrosshair() {
        RayTraceResult ray = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                0.45,
                e -> e instanceof LivingEntity le
                        && !e.equals(player)
                        && !le.isDead()
                        && isLockable(le));
        if (ray != null && ray.getHitEntity() instanceof LivingEntity hit) return hit;

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection();
        for (org.bukkit.entity.Entity e : player.getWorld()
                .getNearbyEntities(player.getLocation(), range, range, range)) {
            if (!(e instanceof LivingEntity le) || e.equals(player) || le.isDead() || !isLockable(le))
                continue;
            Vector to = le.getEyeLocation().toVector().subtract(eye.toVector());
            double dist = to.length();
            if (dist < 0.2 || dist > range) continue;
            to.multiply(1.0 / dist);
            double dot = look.dot(to);
            if (dot < 0.82) continue;
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
        if (le instanceof Animals || le instanceof Villager) return false;
        return true;
    }

    private static String nameOf(LivingEntity le) {
        if (le instanceof Player p) return p.getName();
        return le.getType().name();
    }

    @Override
    public void cleanup() {
        target = null;
    }

    @Override
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
}
