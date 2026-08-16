package com.etherstories.escore.weapons.skills;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.WeaponManager;
import com.etherstories.escore.weapons.FlyingSwordVisual;
import com.etherstories.escore.weapons.SkillInstance;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Aether Verdict「以太裁决」— ItemDisplay 剑雨。
 */
public class SwordRainSkill implements SkillInstance {

    private enum Phase { RISING, HOVERING, DEPLOYING, DONE }

    private final ES2UniPlugin  plugin;
    private final Player        player;
    private final WeaponManager wm;
    private final UUID          ownerUUID;

    private final int    swordCount;
    private final double riseSpeed;
    private final int    riseTicks;
    private final double hoverRadius;
    private final int    deployInterval;
    private final double diveSpeed;
    private final double damage;
    private final double damageRadius;

    private Phase phase = Phase.RISING;
    private int   tick  = 0;

    private FlyingSwordVisual mainSword;
    private final List<FlyingSwordVisual> hovering = new ArrayList<>();
    private final List<FlyingSwordVisual> diving   = new ArrayList<>();
    private int deployTick = 0;

    public SwordRainSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.plugin = plugin;
        this.player = player;
        this.wm = wm;
        this.ownerUUID = player.getUniqueId();

        swordCount     = Math.min(16, wm.getCfgInt(SkillType.SWORD_RAIN, "sword-count", 8));
        riseSpeed      = wm.getCfgDouble(SkillType.SWORD_RAIN, "rise-speed", 0.8);
        riseTicks      = wm.getCfgInt(SkillType.SWORD_RAIN, "rise-ticks", 25);
        hoverRadius    = wm.getCfgDouble(SkillType.SWORD_RAIN, "hover-radius", 3.2);
        deployInterval = wm.getCfgInt(SkillType.SWORD_RAIN, "deploy-interval-ticks", 8);
        diveSpeed      = wm.getCfgDouble(SkillType.SWORD_RAIN, "dive-speed", 1.35);
        damage         = wm.getCfgDouble(SkillType.SWORD_RAIN, "damage", 8.0);
        damageRadius   = wm.getCfgDouble(SkillType.SWORD_RAIN, "damage-radius", 2.5);

        Location start = player.getLocation().add(0, 1.6, 0);
        mainSword = FlyingSwordVisual.spawn(start, wm, false);
        player.getWorld().playSound(start, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 0.7f);
        player.getWorld().spawnParticle(Particle.END_ROD, start, 10, 0.15, 0.3, 0.15, 0.02);
    }

    @Override
    public boolean tick() {
        tick++;
        return switch (phase) {
            case RISING -> tickRising();
            case HOVERING -> tickHovering();
            case DEPLOYING -> tickDeploying();
            case DONE -> false;
        };
    }

    private boolean tickRising() {
        if (mainSword == null || mainSword.dead()) { phase = Phase.DONE; return false; }
        if (tick >= riseTicks) {
            expandToHover();
            return true;
        }
        Location loc = mainSword.getLocation().add(0, riseSpeed, 0);
        mainSword.move(loc, new Vector(0, 1, 0));
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 2, 0.06, 0.06, 0.06, 0.01);
        return true;
    }

    private void expandToHover() {
        Location peak = mainSword.getLocation();
        mainSword.remove();
        mainSword = null;

        World world = peak.getWorld();
        world.playSound(peak, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.9f);
        world.spawnParticle(Particle.END_ROD, peak, 16, 0.5, 0.2, 0.5, 0.05);

        // 单圈即可，避免 24 把剑糊成一团
        for (int i = 0; i < swordCount; i++) {
            double angle = (2 * Math.PI / swordCount) * i;
            Location loc = peak.clone().add(
                    hoverRadius * Math.cos(angle),
                    0.1 * Math.sin(i),
                    hoverRadius * Math.sin(angle));
            hovering.add(FlyingSwordVisual.spawn(loc, wm, true));
        }

        phase = Phase.HOVERING;
        deployTick = deployInterval;
    }

    private boolean tickHovering() {
        for (FlyingSwordVisual s : hovering) {
            if (!s.dead())
                s.getLocation().getWorld().spawnParticle(Particle.END_ROD,
                        s.getLocation(), 1, 0.08, 0.08, 0.08, 0.005);
        }
        phase = Phase.DEPLOYING;
        return true;
    }

    private boolean tickDeploying() {
        Location lookTarget = player.isOnline() ? getLookTarget() : null;

        Iterator<FlyingSwordVisual> it = diving.iterator();
        while (it.hasNext()) {
            FlyingSwordVisual sword = it.next();
            if (sword.dead()) { it.remove(); continue; }

            if (lookTarget != null) {
                Vector dir = lookTarget.clone().subtract(sword.getLocation()).toVector();
                double dist = dir.length();
                if (dist < 1.2) {
                    onHit(sword.getLocation());
                    sword.remove();
                    it.remove();
                    continue;
                }
                dir.normalize().multiply(Math.min(diveSpeed, dist));
                sword.move(sword.getLocation().add(dir), dir);
            }
            sword.getLocation().getWorld().spawnParticle(Particle.CRIT,
                    sword.getLocation(), 2, 0.05, 0.05, 0.05, 0.02);
        }

        deployTick++;
        if (deployTick >= deployInterval && !hovering.isEmpty()) {
            deployTick = 0;
            FlyingSwordVisual next = hovering.remove(0);
            if (!next.dead()) {
                diving.add(next);
                next.getLocation().getWorld().playSound(
                        next.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8f, 0.6f);
            }
        }

        for (FlyingSwordVisual s : hovering) {
            if (!s.dead())
                s.getLocation().getWorld().spawnParticle(Particle.END_ROD,
                        s.getLocation(), 1, 0.06, 0.06, 0.06, 0.005);
        }

        if (tick > riseTicks + (long) (swordCount + 2) * deployInterval + 200) {
            cleanup();
            phase = Phase.DONE;
            return false;
        }

        if (hovering.isEmpty() && diving.isEmpty()) {
            phase = Phase.DONE;
            return false;
        }
        return true;
    }

    private void onHit(Location loc) {
        World world = loc.getWorld();
        world.spawnParticle(Particle.SWEEP_ATTACK, loc, 2, 0.2, 0.1, 0.2, 0);
        world.spawnParticle(Particle.CRIT, loc, 18, 0.45, 0.35, 0.45, 0.2);
        world.spawnParticle(Particle.END_ROD, loc, 6, 0.3, 0.25, 0.3, 0.04);
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9f, 0.9f);

        for (org.bukkit.entity.Entity e : world.getNearbyEntities(loc, damageRadius, damageRadius, damageRadius)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.damage(damage, player);
            }
        }
    }

    private Location getLookTarget() {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                60.0, FluidCollisionMode.NEVER, true);
        if (result != null && result.getHitBlock() != null) {
            return result.getHitBlock().getLocation().add(0.5, 1.0, 0.5);
        }
        return player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(50));
    }

    @Override
    public void cleanup() {
        if (mainSword != null) mainSword.remove();
        for (FlyingSwordVisual s : hovering) s.remove();
        for (FlyingSwordVisual s : diving) s.remove();
        hovering.clear();
        diving.clear();
    }

    @Override public UUID getOwnerUUID() { return ownerUUID; }
}
