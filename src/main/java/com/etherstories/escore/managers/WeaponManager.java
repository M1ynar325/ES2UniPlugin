package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.items.SkillBinder;
import com.etherstories.escore.utils.ColorUtil;
import com.etherstories.escore.weapons.SkillInstance;
import com.etherstories.escore.weapons.SkillType;
import com.etherstories.escore.weapons.skills.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * 武器系统核心管理器。
 *
 * 性能设计：
 *  - 单一 master task 每 tick 驱动所有技能实例（不为每个实体单独创建 task）
 *  - 冷却使用 System.currentTimeMillis() 避免 tick 漂移
 *  - 飞剑等视觉实体集中注册，插件卸载时一次性清理
 */
public class WeaponManager {

    private final ES2UniPlugin plugin;

    // 冷却：uuid -> 到期时间戳 (ms)
    private final Map<UUID, Map<SkillType, Long>> cooldowns = new HashMap<>();

    // 蓄力：uuid -> 蓄力状态
    private final Map<UUID, ChargeState> charges = new HashMap<>();

    // 活跃技能实例列表（master task 每 tick 遍历）
    private final List<SkillInstance> activeSkills = new ArrayList<>();

    // 全局实体注册表（用于插件卸载时清理）
    private final Set<Entity> trackedEntities = new HashSet<>();

    private BukkitTask masterTask;

    public WeaponManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        startMasterTask();
    }

    // ── Master Task ───────────────────────────────────────────────────────────

    private void startMasterTask() {
        masterTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (charges.isEmpty() && activeSkills.isEmpty()) return;
            long now = System.currentTimeMillis();

            // 1. 蓄力心跳检测
            List<UUID> toFire   = new ArrayList<>();
            List<UUID> toCancel = new ArrayList<>();

            for (Map.Entry<UUID, ChargeState> entry : charges.entrySet()) {
                UUID uuid = entry.getKey();
                ChargeState cs = entry.getValue();
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) { toCancel.add(uuid); continue; }

                // 剑类物品按住右键不会持续触发 Interact，不能依赖心跳。
                // 蓄力开始后自动读条；再次右键 / 换手 / 受伤可取消。
                long chargeMs = getChargeTicks(cs.skillType) * 50L;
                long elapsed  = now - cs.startMs;

                if (elapsed >= chargeMs) {
                    toFire.add(uuid);
                    continue;
                }

                // 进度条动画
                int pct = (int) Math.min(10, elapsed * 10 / Math.max(1, chargeMs));
                String bar = "&b" + "█".repeat(pct) + "&8" + "░".repeat(10 - pct);
                sendActionBar(p, "  " + bar + "  &7" + cs.skillType.displayName() + " &8(再右键取消)", 15);
            }

            for (UUID uuid : toFire) {
                ChargeState cs = charges.remove(uuid);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && cs != null) fireSkill(p, cs.skillType);
            }
            for (UUID uuid : toCancel) {
                cancelCharge(uuid);
            }

            // 2. 驱动所有活跃技能
            EchoResonance.purgeExpired();
            Iterator<SkillInstance> it = activeSkills.iterator();
            while (it.hasNext()) {
                SkillInstance skill = it.next();
                try {
                    if (!skill.tick()) {
                        it.remove();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[WeaponManager] 技能 tick 异常: " + e.getMessage());
                    skill.cleanup();
                    it.remove();
                }
            }
        }, 1L, 1L);
    }

    // ── 蓄力 ─────────────────────────────────────────────────────────────────

    /**
     * 开始蓄力。若已在蓄力同一技能则取消；换技能则覆盖。
     * @return false = 冷却中 / 已取消，true = 蓄力已开始或已即时释放
     */
    public boolean startCharge(Player player, SkillType skillType) {
        UUID uuid = player.getUniqueId();

        // 蓄力中再点一次 → 取消（剑无法靠松手检测）
        ChargeState existing = charges.get(uuid);
        if (existing != null) {
            cancelCharge(uuid);
            return false;
        }

        if (isOnCooldown(uuid, skillType)) {
            long rem = getRemainingCooldownSec(uuid, skillType);
            sendActionBar(player, "&c" + skillType.displayName() + " &7冷却中 &f" + rem + "s");
            return false;
        }

        // 即时技能直接释放，无需蓄力
        if (getChargeTicks(skillType) <= 0) {
            fireSkill(player, skillType);
            return true;
        }

        charges.put(uuid, new ChargeState(skillType, System.currentTimeMillis()));
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 1.5f);
        sendActionBar(player, "&7蓄力 &b" + skillType.displayName() + " &8— 再右键取消");
        return true;
    }

    /** 保留兼容；蓄力已改为自动读条，不再依赖心跳 */
    @Deprecated
    public void keepCharge(UUID uuid) {
        // no-op
    }

    /** 取消蓄力（换手持/受伤/退出等） */
    public void cancelCharge(UUID uuid) {
        ChargeState cs = charges.remove(uuid);
        if (cs == null) return;
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            sendActionBar(p, "&7已取消蓄力");
            p.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.5f, 1.2f);
        }
    }

    public boolean isCharging(UUID uuid) {
        return charges.containsKey(uuid);
    }

    // ── 技能释放 ──────────────────────────────────────────────────────────────

    private void fireSkill(Player player, SkillType type) {
        if (!player.isOnline()) return;

        // 无 CD 回声技：并发软上限，防粒子刷爆
        if (type == SkillType.ECHO_SCATTER && countActive(player.getUniqueId(), EchoScatterSkill.class) >= 2) {
            sendActionBar(player, "&7回声散射 &8过载，稍候", 25);
            return;
        }
        if (type == SkillType.ECHO_BARRAGE && countActive(player.getUniqueId(), EchoBarrageSkill.class) >= 1) {
            sendActionBar(player, "&7回声寻踪 &8进行中", 25);
            return;
        }

        if (type == SkillType.GLEAM_ARC) {
            for (SkillInstance s : activeSkills) {
                if (s instanceof GleamArcSkill g && g.getOwnerUUID().equals(player.getUniqueId())) {
                    setCooldown(player.getUniqueId(), type);
                    g.recast();
                    return;
                }
            }
        }

        setCooldown(player.getUniqueId(), type);

        SkillInstance skill = switch (type) {
            case SWORD_RAIN       -> new SwordRainSkill(plugin, player, this);
            case HOMING           -> new HomingSkill(plugin, player, this);
            case LUMINAL_STRIKE   -> new LuminalStrikeSkill(plugin, player, this);
            case AXIOM_BREACH     -> new AxiomBreachSkill(plugin, player, this);
            case STELLAR_CONV     -> new StellarConvergenceSkill(plugin, player, this);
            case CELESTIAL_ASCENT -> new CelestialAscentSkill(plugin, player, this);
            case ECHO_SCATTER     -> new EchoScatterSkill(plugin, player, this);
            case ECHO_BARRAGE     -> new EchoBarrageSkill(plugin, player, this);
            case GLEAM_ARC        -> new GleamArcSkill(plugin, player, this);
        };

        if (skill instanceof GleamArcSkill g && !g.isLocked()) return;

        activeSkills.add(skill);
        if (type != SkillType.GLEAM_ARC) {
            sendActionBar(player, "&b" + type.displayName() + " &f已释放");
        }
    }

    private int countActive(UUID uuid, Class<? extends SkillInstance> cls) {
        int n = 0;
        for (SkillInstance s : activeSkills) {
            if (uuid.equals(s.getOwnerUUID()) && cls.isInstance(s)) n++;
        }
        return n;
    }

    // ── 冷却 ─────────────────────────────────────────────────────────────────

    private void setCooldown(UUID uuid, SkillType type) {
        long cd = getCooldownMs(type);
        if (cd <= 0) return;
        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(type, System.currentTimeMillis() + cd);
    }

    public boolean isOnCooldown(UUID uuid, SkillType type) {
        Map<SkillType, Long> m = cooldowns.get(uuid);
        if (m == null) return false;
        Long exp = m.get(type);
        return exp != null && exp > System.currentTimeMillis();
    }

    public long getRemainingCooldownSec(UUID uuid, SkillType type) {
        Map<SkillType, Long> m = cooldowns.get(uuid);
        if (m == null) return 0;
        Long exp = m.get(type);
        if (exp == null) return 0;
        return Math.max(0, (exp - System.currentTimeMillis()) / 1000);
    }

    // ── 实体追踪（防泄漏） ────────────────────────────────────────────────────

    public void registerEntity(Entity entity) {
        if (entity != null) trackedEntities.add(entity);
    }

    public void unregisterEntity(Entity entity) {
        trackedEntities.remove(entity);
    }

    // ── 玩家离线清理 ──────────────────────────────────────────────────────────

    public void onPlayerQuit(UUID uuid) {
        cancelCharge(uuid);
        cooldowns.remove(uuid);
        // 终止该玩家的所有活跃技能
        Iterator<SkillInstance> it = activeSkills.iterator();
        while (it.hasNext()) {
            SkillInstance s = it.next();
            if (s.getOwnerUUID().equals(uuid)) {
                s.cleanup();
                it.remove();
            }
        }
    }

    // ── 插件卸载 ──────────────────────────────────────────────────────────────

    public void disable() {
        if (masterTask != null) masterTask.cancel();
        // 取消所有蓄力
        new HashSet<>(charges.keySet()).forEach(this::cancelCharge);
        // 终止所有技能
        activeSkills.forEach(SkillInstance::cleanup);
        activeSkills.clear();
        // 清除残留实体
        trackedEntities.removeIf(ent -> {
            if (ent != null && !ent.isDead()) ent.remove();
            return true;
        });
    }

    // ── Config helpers ────────────────────────────────────────────────────────

    public int getChargeTicks(SkillType type) {
        int def = switch (type) {
            case AXIOM_BREACH    -> 60;
            case LUMINAL_STRIKE  -> 0;   // 即时触发
            case ECHO_SCATTER    -> 0;
            case ECHO_BARRAGE    -> 0;
            case GLEAM_ARC       -> 0;
            default              -> 20;
        };
        return plugin.getConfig().getInt(
                "weapons.skills." + type.configKey + ".charge-ticks", def);
    }

    private long getCooldownMs(SkillType type) {
        int sec = plugin.getConfig().getInt(
                "weapons.skills." + type.configKey + ".cooldown-seconds",
                defaultCooldown(type));
        return sec * 1000L;
    }

    private int defaultCooldown(SkillType type) {
        return switch (type) {
            case SWORD_RAIN       -> 30;
            case HOMING           -> 15;
            case LUMINAL_STRIKE   -> 3;
            case AXIOM_BREACH     -> 60;
            case STELLAR_CONV     -> 20;
            case CELESTIAL_ASCENT -> 30;
            case ECHO_SCATTER     -> 0;
            case ECHO_BARRAGE     -> 0;
            case GLEAM_ARC        -> 3;
        };
    }

    public int getCfgInt(SkillType type, String key, int def) {
        return plugin.getConfig().getInt("weapons.skills." + type.configKey + "." + key, def);
    }

    public double getCfgDouble(SkillType type, String key, double def) {
        return plugin.getConfig().getDouble("weapons.skills." + type.configKey + "." + key, def);
    }

    // ── 工具 ─────────────────────────────────────────────────────────────────

    public static void sendActionBar(Player p, String msg) {
        ES2UniPlugin pl = ES2UniPlugin.getInstance();
        if (pl != null && pl.getActionBarManager() != null) {
            pl.getActionBarManager().sendTemp(p, msg, 50);
        } else {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(ColorUtil.colorize(msg)));
        }
    }

    /** 蓄力进度等需要更短续期的临时条 */
    public static void sendActionBar(Player p, String msg, int durationTicks) {
        ES2UniPlugin pl = ES2UniPlugin.getInstance();
        if (pl != null && pl.getActionBarManager() != null) {
            pl.getActionBarManager().sendTemp(p, msg, durationTicks);
            return;
        }
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(ColorUtil.colorize(msg)));
    }

    // ── 内部类 ────────────────────────────────────────────────────────────────

    private static class ChargeState {
        final SkillType skillType;
        final long      startMs;

        ChargeState(SkillType type, long now) {
            this.skillType = type;
            this.startMs   = now;
        }
    }
}
