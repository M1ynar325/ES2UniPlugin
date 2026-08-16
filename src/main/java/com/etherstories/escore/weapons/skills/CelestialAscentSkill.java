package com.etherstories.escore.weapons.skills;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.WeaponManager;
import com.etherstories.escore.weapons.SkillInstance;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Celestial Ascent「天升」
 *
 * Shift+右键触发：
 *  - 玩家缓缓漂浮升天（LEVITATION 效果）
 *  - 获得抗性 II + 生命恢复 II
 *  - END_ROD + SOUL_FIRE_FLAME 双环环绕
 *  - 持续 ascendTicks tick 后给予 SLOW_FALLING 缓降防摔伤
 */
public class CelestialAscentSkill implements SkillInstance {

    private final ES2UniPlugin  plugin;
    private final Player        player;
    private final UUID          ownerUUID;

    private final int    ascendTicks;
    private final int    slowFallTicks;
    private final int    levitationLevel;

    private int  tick = 0;
    private boolean ended = false;

    public CelestialAscentSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.plugin    = plugin;
        this.player    = player;
        this.ownerUUID = player.getUniqueId();

        ascendTicks     = wm.getCfgInt(SkillType.CELESTIAL_ASCENT, "ascend-ticks",        100);
        slowFallTicks   = wm.getCfgInt(SkillType.CELESTIAL_ASCENT, "slow-fall-ticks",      80);
        levitationLevel = wm.getCfgInt(SkillType.CELESTIAL_ASCENT, "levitation-level",      1);

        int resistTicks = ascendTicks + slowFallTicks + 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION,   ascendTicks,     levitationLevel,  true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,   resistTicks,     1,               true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, resistTicks,     1,               true, false));

        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE,    0.6f, 1.8f);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0,1,0), 30, 0.5, 0.5, 0.5, 0.3);
    }

    @Override
    public boolean tick() {
        tick++;
        if (!player.isOnline()) return false;

        Location loc = player.getLocation();
        World    world = player.getWorld();

        // ① 双环粒子（外圈 END_ROD，内圈 SOUL_FIRE_FLAME）
        double angle = tick * 0.22;
        int pts = 10;
        for (int i = 0; i < pts; i++) {
            double a  = angle + Math.PI * 2.0 / pts * i;
            double y  = 1.0 + Math.sin(tick * 0.12 + i * 0.6) * 0.3;

            // 外环
            world.spawnParticle(Particle.END_ROD,
                    loc.clone().add(Math.cos(a)*1.1, y, Math.sin(a)*1.1),
                    0, 0, 0.02, 0, 0.01);

            // 内环（反向转）
            double a2 = -angle * 1.4 + Math.PI * 2.0 / pts * i;
            world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                    loc.clone().add(Math.cos(a2)*0.55, y + 0.1, Math.sin(a2)*0.55),
                    0, 0, 0.01, 0, 0.01);
        }

        // ② 上升时从脚底喷出 DRAGON_BREATH 尾迹
        if (tick <= ascendTicks) {
            world.spawnParticle(Particle.DRAGON_BREATH,
                    loc.clone().add(0, 0.1, 0), 4, 0.3, 0.05, 0.3, 0.02);
        }

        // ③ 达到最大上升时间 → 结束漂浮，给予缓降（不立刻结束 tick，继续播粒子）
        if (tick >= ascendTicks && !ended) {
            player.removePotionEffect(PotionEffectType.LEVITATION);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,
                    slowFallTicks, 0, true, false));
            world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.8f);
            ended = true;
        }

        // ④ 缓降结束后技能彻底完成
        if (tick >= ascendTicks + slowFallTicks) return false;
        return true;
    }

    @Override
    public void cleanup() {
        if (player.isOnline()) {
            player.removePotionEffect(PotionEffectType.LEVITATION);
        }
    }

    @Override
    public UUID getOwnerUUID() { return ownerUUID; }
}
