package com.etherstories.escore.weapons.skills;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.WeaponManager;
import com.etherstories.escore.weapons.SkillInstance;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
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
 * Pale Line「霁线」— 准星直线粒子，打到第一个生物或方块为止。
 */
public class PaleLineSkill implements SkillInstance {

    private static final Color WHITE = Color.fromRGB(245, 250, 255);
    private static final Color JIQING = Color.fromRGB(110, 210, 200);

    private final UUID ownerUUID;

    public PaleLineSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.ownerUUID = player.getUniqueId();
        fire(player, wm);
    }

    private void fire(Player player, WeaponManager wm) {
        double range = Math.max(4.0, wm.getCfgDouble(SkillType.PALE_LINE, "range", 20.0));
        double damage = Math.max(0.5, wm.getCfgDouble(SkillType.PALE_LINE, "damage", 6.0));
        Location eye = player.getEyeLocation();
        World w = eye.getWorld();
        if (w == null) return;
        Vector dir = eye.getDirection();
        if (dir.lengthSquared() < 1e-6) return;
        dir.normalize();

        RayTraceResult blockHit = w.rayTraceBlocks(eye, dir, range, FluidCollisionMode.NEVER, true);
        RayTraceResult entHit = w.rayTraceEntities(eye, dir, range, 0.4, e ->
                e instanceof LivingEntity le
                        && !e.equals(player)
                        && !le.isDead()
                        && lockable(le));

        Vector origin = eye.toVector();
        double blockDist = blockHit != null && blockHit.getHitPosition() != null
                ? blockHit.getHitPosition().distance(origin) : range + 0.01;
        double entDist = entHit != null && entHit.getHitPosition() != null
                && entHit.getHitEntity() instanceof LivingEntity
                ? entHit.getHitPosition().distance(origin) : Double.MAX_VALUE;

        Location dest;
        LivingEntity victim = null;
        if (entDist < blockDist) {
            victim = (LivingEntity) entHit.getHitEntity();
            dest = entHit.getHitPosition().toLocation(w);
        } else if (blockHit != null && blockHit.getHitPosition() != null) {
            dest = blockHit.getHitPosition().toLocation(w);
        } else {
            dest = eye.clone().add(dir.clone().multiply(range));
        }

        drawLine(eye, dest);
        if (victim != null) {
            EchoDamage.magic(victim, player, damage);
            burst(dest);
            WeaponManager.sendActionBar(player, "&b霁线 &f→ &e" + nameOf(victim), 20);
        } else {
            WeaponManager.sendActionBar(player, "&7霁线", 12);
        }
        w.playSound(eye, Sound.ENTITY_EVOKER_CAST_SPELL, 0.45f, 1.8f);
        w.playSound(dest, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.55f, 1.9f);
    }

    private static void drawLine(Location from, Location to) {
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
                    new Particle.DustOptions(cyan ? JIQING : WHITE, cyan ? 1.15f : 0.95f), true);
            if (i % 3 == 0) w.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0, null, true);
            if (i % 4 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.03, 0.03, 0.03, 0.01);
            p.add(step);
        }
    }

    private static void burst(Location at) {
        World w = at.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.ELECTRIC_SPARK, at, 10, 0.12, 0.12, 0.12, 0.04);
        w.spawnParticle(Particle.DUST, at, 8, 0.1, 0.1, 0.1, 0,
                new Particle.DustOptions(JIQING, 1.2f), true);
    }

    private static boolean lockable(LivingEntity le) {
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
    public boolean tick() {
        return false;
    }

    @Override
    public void cleanup() {
    }

    @Override
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
}
