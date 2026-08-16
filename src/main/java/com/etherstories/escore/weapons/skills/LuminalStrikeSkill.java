package com.etherstories.escore.weapons.skills;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.WeaponManager;
import com.etherstories.escore.weapons.SkillInstance;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Luminal Strike「光速斩」
 *
 * 玩家沿准星方向快速冲刺，穿过的敌人全部受伤。
 * 每 tick 向前步进，碰墙停止，不重复伤害同一实体。
 */
public class LuminalStrikeSkill implements SkillInstance {

    private final ES2UniPlugin  plugin;
    private final Player        player;
    private final WeaponManager wm;
    private final UUID          ownerUUID;

    private final int    dashTicks;
    private final double stepPerTick;   // 每 tick 移动的格数
    private final double damage;
    private final double hitRadius;

    private int  tick = 0;
    private final Vector direction;
    private final Set<UUID> hit = new HashSet<>();

    public LuminalStrikeSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.plugin    = plugin;
        this.player    = player;
        this.wm        = wm;
        this.ownerUUID = player.getUniqueId();

        int   dashDistance = wm.getCfgInt   (SkillType.LUMINAL_STRIKE, "dash-distance", 15);
        dashTicks  = wm.getCfgInt   (SkillType.LUMINAL_STRIKE, "dash-ticks",    8);
        damage     = wm.getCfgDouble(SkillType.LUMINAL_STRIKE, "damage",         10.0);
        hitRadius  = wm.getCfgDouble(SkillType.LUMINAL_STRIKE, "damage-radius",  1.5);
        stepPerTick = (double) dashDistance / dashTicks;

        // 水平方向（不让玩家飞上天）
        direction = player.getEyeLocation().getDirection().clone().setY(0).normalize();

        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.6f);
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.7f);
        loc.getWorld().spawnParticle(Particle.FLASH, loc.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1, 0), 20, 0.3, 0.4, 0.3, 0.05);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 25, 0.4, 0.5, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(100, 220, 255), 1.4f));
    }

    @Override
    public boolean tick() {
        tick++;
        if (tick > dashTicks || !player.isOnline()) {
            onDashEnd();
            return false;
        }

        Location cur = player.getLocation();
        Location next = cur.clone().add(direction.clone().multiply(stepPerTick));

        // 碰墙检测：目标位置是否是实体方块
        if (next.getBlock().getType().isSolid()
                || next.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
            onDashEnd();
            return false;
        }

        // 传送玩家（保留朝向）
        next.setYaw(cur.getYaw());
        next.setPitch(cur.getPitch());
        player.teleport(next);

        // 粒子拖尾
        Location mid = cur.clone().add(0, 1, 0);
        cur.getWorld().spawnParticle(Particle.END_ROD, mid, 10, 0.25, 0.35, 0.25, 0.06);
        cur.getWorld().spawnParticle(Particle.CLOUD, mid, 4, 0.15, 0.2, 0.15, 0.02);
        cur.getWorld().spawnParticle(Particle.DUST, mid, 8, 0.2, 0.25, 0.2, 0,
                new Particle.DustOptions(Color.fromRGB(120, 230, 255), 1.1f));
        cur.getWorld().spawnParticle(Particle.CRIT, mid, 4, 0.15, 0.2, 0.15, 0.05);

        // 命中判定
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), hitRadius, hitRadius + 0.5, hitRadius)) {
            if (e.equals(player)) continue;
            if (hit.contains(e.getUniqueId())) continue;
            if (e instanceof org.bukkit.entity.LivingEntity le) {
                le.damage(damage, player);
                hit.add(e.getUniqueId());
                e.getWorld().spawnParticle(Particle.CRIT, e.getLocation().add(0, 1, 0),
                        18, 0.35, 0.35, 0.35, 0.25);
                e.getWorld().spawnParticle(Particle.SWEEP_ATTACK, e.getLocation().add(0, 1, 0),
                        2, 0.2, 0.2, 0.2, 0);
            }
        }
        return true;
    }

    private void onDashEnd() {
        if (!player.isOnline()) return;
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1, 0), 6, 0.6, 0.35, 0.6, 0);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.06);
    }

    @Override public void cleanup()         { /* 无实体需清理 */ }
    @Override public UUID getOwnerUUID()    { return ownerUUID; }
}
