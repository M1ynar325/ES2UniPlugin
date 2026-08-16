package com.etherstories.escore.weapons.skills;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.WeaponManager;
import com.etherstories.escore.weapons.SkillInstance;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Axiom Breach「公理破界」
 *
 * 无蓄力版（蓄力由 WeaponManager 处理，已配置 60 ticks）。
 * 释放后：在玩家周围以随机但均匀方式落下 N 道闪电，
 * 每道闪电间隔若干 tick，营造矩阵轰炸感。
 * 使用 strikeLightningEffect()（纯视觉）+ 手动伤害，避免引发火灾。
 */
public class AxiomBreachSkill implements SkillInstance {

    private final ES2UniPlugin  plugin;
    private final Player        player;
    private final WeaponManager wm;
    private final UUID          ownerUUID;

    private final int    strikeCount;
    private final double strikeRadius;
    private final double damage;
    private final double damageRadius;
    private final int    strikeIntervalTicks;

    // 预计算的落雷坐标序列
    private final List<Location> strikePoints = new ArrayList<>();
    private int nextStrikeIndex = 0;
    private int tick = 0;

    public AxiomBreachSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.plugin    = plugin;
        this.player    = player;
        this.wm        = wm;
        this.ownerUUID = player.getUniqueId();

        strikeCount    = wm.getCfgInt   (SkillType.AXIOM_BREACH, "strike-count",          9);
        strikeRadius   = wm.getCfgDouble(SkillType.AXIOM_BREACH, "strike-radius",         8.0);
        damage         = wm.getCfgDouble(SkillType.AXIOM_BREACH, "damage",                15.0);
        damageRadius   = wm.getCfgDouble(SkillType.AXIOM_BREACH, "damage-radius",         3.0);
        strikeIntervalTicks = wm.getCfgInt(SkillType.AXIOM_BREACH, "strike-interval-ticks", 6);

        precomputePoints();

        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.4f);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 2, 0), 20, 0.5, 0.5, 0.5, 0.1);
    }

    /** 均匀 + 轻微随机分布落雷点（圆形矩阵感） */
    private void precomputePoints() {
        Random rng = new Random();
        Location origin = player.getLocation();
        // 中心一击：玩家前方 1.5 格（避免正好落在玩家脚下触发自伤）
        Vector forward = player.getLocation().getDirection().setY(0).normalize().multiply(1.5);
        strikePoints.add(origin.clone().add(forward));
        // 外圈均匀分布
        int outer = strikeCount - 1;
        for (int i = 0; i < outer; i++) {
            double angle  = (2 * Math.PI / outer) * i + rng.nextDouble() * 0.3;
            double radius = strikeRadius * (0.7 + rng.nextDouble() * 0.3);
            strikePoints.add(origin.clone().add(
                    radius * Math.cos(angle), 0,
                    radius * Math.sin(angle)));
        }
        // 全部打乱（含中心），不让中心永远第一落
        java.util.Collections.shuffle(strikePoints, rng);
    }

    @Override
    public boolean tick() {
        tick++;
        if (nextStrikeIndex >= strikePoints.size()) return false;
        if (tick % strikeIntervalTicks != 0) return true; // 还没到下一击

        Location loc = strikePoints.get(nextStrikeIndex);
        // 补正高度（落在地面上）
        loc = findGround(loc);
        nextStrikeIndex++;

        // 视觉闪电
        loc.getWorld().strikeLightningEffect(loc);

        // 预警粒子（落点圈）
        for (double a = 0; a < Math.PI * 2; a += Math.PI / 8) {
            loc.getWorld().spawnParticle(Particle.END_ROD,
                    loc.clone().add(Math.cos(a) * 0.8, 0.1, Math.sin(a) * 0.8),
                    1, 0, 0.1, 0, 0.01);
        }

        // 伤害
        for (org.bukkit.entity.Entity e : loc.getWorld()
                .getNearbyEntities(loc, damageRadius, damageRadius + 1, damageRadius)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.damage(damage, player);
            }
        }

        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 1.0f);
        return nextStrikeIndex < strikePoints.size();
    }

    /** 向下找最近的实体方块表面（最多 10 格） */
    private Location findGround(Location loc) {
        Location l = loc.clone();
        for (int i = 0; i < 10; i++) {
            if (l.getBlock().getType().isSolid()) {
                return l.clone().add(0, 1, 0);
            }
            l.subtract(0, 1, 0);
        }
        return loc;
    }

    @Override public void cleanup()         { /* 无实体 */ }
    @Override public UUID getOwnerUUID()    { return ownerUUID; }
}
