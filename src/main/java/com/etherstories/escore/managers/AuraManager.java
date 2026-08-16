package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.auras.AuraType;
import com.etherstories.escore.auras.FieldRigSession;
import com.etherstories.escore.auras.ParticleBrightness;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 管理玩家光环特效。
 *
 * 设计：
 *  - 每 4 tick 检测一次玩家是否静止（位移 < 0.08 块）
 *  - 静止约 0.8s 后开始播放光环粒子
 *  - 无人装备时跳过整轮
 */
public class AuraManager {

    private final ES2UniPlugin plugin;
    private final File         dataFile;

    // 装备中的光环
    private final Map<UUID, AuraType>      equipped    = new HashMap<>();
    // 拥有的光环
    private final Map<UUID, Set<AuraType>> owned       = new HashMap<>();
    // 粒子亮度（默认中）
    private final Map<UUID, ParticleBrightness> brightness = new HashMap<>();
    // 立场装置会话（常驻，不依赖静止）
    private final Map<UUID, FieldRigSession> fieldRigs = new HashMap<>();

    // 当前帧亮度上下文（仅主线程 spawnAura 使用）
    private ParticleBrightness curBr = ParticleBrightness.MEDIUM;
    private AuraType curType = null;

    // 运动检测（内存）
    private final Map<UUID, Location> lastLoc         = new HashMap<>();
    private final Map<UUID, Integer>  stationaryTicks = new HashMap<>();

    private BukkitTask task;

    public AuraManager(ES2UniPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "auras.yml");
        load();
        startTask();
    }

    // ── Task ─────────────────────────────────────────────────────────────────

    private void startTask() {
        // 每 4 tick（0.2s）：静止判定；有人装备光环才扫描
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (equipped.isEmpty()) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                AuraType aura = equipped.get(uuid);
                if (aura == null) continue;

                if (aura == AuraType.FIELD_RIG) {
                    // 立场装置由 FieldRigSession 单独驱动，此处跳过静止粒子
                    continue;
                }

                Location curr = player.getLocation();
                Location prev = lastLoc.get(uuid);
                lastLoc.put(uuid, curr.clone());

                boolean moved = prev == null
                        || Math.abs(curr.getX() - prev.getX()) > 0.08
                        || Math.abs(curr.getZ() - prev.getZ()) > 0.08
                        || Math.abs(curr.getY() - prev.getY()) > 0.10;

                if (moved) {
                    stationaryTicks.put(uuid, 0);
                    continue;
                }

                // 周期变慢后阈值按次数计：约 0.8s 静止
                int ticks = stationaryTicks.merge(uuid, 1, Integer::sum);
                if (ticks >= 4) {
                    spawnAura(player, aura, ticks);
                }
            }
        }, 4L, 4L);
    }

    // ── 粒子效果 ──────────────────────────────────────────────────────────────

    // ── 亮度缩放（按特效类型微调）──────────────────────────────────────────

    private void beginBright(Player player, AuraType type) {
        curType = type;
        curBr = getBrightness(player.getUniqueId());
    }

    /** 数量：密集体（电弧/花火/极光）高档加成更大；雾类略克制数量。 */
    private int n(int base) {
        double mul = curBr.countMul;
        if (curType != null) {
            switch (curType) {
                case ELECTRIC_HALO, SPARKLE_RING, AURORA, ENCHANT_DRIFT -> mul *= (curBr == ParticleBrightness.HIGH ? 1.15 : 1.0);
                case CLOUD_PUFF, SNOW_VEIL, ASH_DRIFT, BUBBLE_WELL -> mul *= 0.9;
                case SCULK_WHISPER, VOID_ECHO -> mul *= (curBr == ParticleBrightness.LOW ? 0.85 : 1.0);
                default -> { }
            }
        }
        return Math.max(base > 0 ? 1 : 0, (int) Math.round(base * mul));
    }

    private float sz(float base) {
        double mul = curBr.sizeMul;
        if (curType == AuraType.HEARTBEAT || curType == AuraType.GOLD_ORBIT || curType == AuraType.AURORA)
            mul *= (curBr == ParticleBrightness.HIGH ? 1.1 : 1.0);
        return (float) (base * mul);
    }

    private double spr(double base) {
        return base * (0.85 + 0.15 * curBr.sizeMul);
    }

    private boolean every(int t, int period) {
        int p = Math.max(1, period * curBr.periodMul);
        if (curBr == ParticleBrightness.HIGH && period >= 3) p = Math.max(1, period - 1);
        return t % p == 0;
    }


    private void spawnAura(Player player, AuraType type, int t) {
        beginBright(player, type);
        // 低亮度隔帧，降粒子压力
        if (curBr == ParticleBrightness.LOW && (t % 2) != 0) return;
        Location feet = player.getLocation().add(0, 0.05, 0);
        World world   = player.getWorld();

        switch (type) {

            case ENCHANT_DRIFT -> {
                // 附魔符文螺旋升腾（贵价款：双螺旋 + 中心微光）
                double spin = t * 0.18;
                for (int i = 0; i < n(6); i++) {
                    double a = spin + Math.PI * 2.0 / 6 * i;
                    double r = 0.55 + (i % 2) * 0.2;
                    double y = (Math.sin(t * 0.1 + i) * 0.5 + 0.9);
                    world.spawnParticle(Particle.ENCHANT,
                            feet.clone().add(Math.cos(a)*r, y, Math.sin(a)*r),
                            0, 0, 0.18, 0, 1.2);
                }
                if (every(t, 4)) {
                    world.spawnParticle(Particle.ENCHANTED_HIT,
                            feet.clone().add(0, 1.1, 0), n(2), 0.35, 0.4, 0.35, 0.02);
                }
            }

            case STARDUST -> {
                double angle = (t * 0.12) % (Math.PI * 2);
                for (int i = 0; i < n(4); i++) {
                    double a = angle + Math.PI / 2.0 * i;
                    double y = 1.0 + Math.sin(t * 0.08 + i) * 0.25;
                    world.spawnParticle(Particle.END_ROD,
                            feet.clone().add(Math.cos(a)*0.75, y, Math.sin(a)*0.75),
                            0, 0, 0.01, 0, 0.005);
                }
                if (every(t, 5)) {
                    world.spawnParticle(Particle.CRIT,
                            feet.clone().add(0, 1.0, 0), n(2), 0.4, 0.4, 0.4, 0.01);
                }
            }

            case NOTE_CIRCLE -> {
                double angle = (t * 0.2) % (Math.PI * 2);
                for (int i = 0; i < n(5); i++) {
                    double a = angle + Math.PI * 2.0 / 5 * i;
                    world.spawnParticle(Particle.NOTE,
                            feet.clone().add(Math.cos(a)*0.8, 1.0 + Math.sin(t * 0.1 + i) * 0.2, Math.sin(a)*0.8),
                            1, 0, 0, 0, 1);
                }
            }

            case SNOW_VEIL -> {
                for (int i = 0; i < n(5); i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 0.9;
                    world.spawnParticle(Particle.SNOWFLAKE,
                            feet.clone().add(Math.cos(a)*r, 2.0 + Math.random() * 0.3, Math.sin(a)*r),
                            0, (Math.random()-0.5)*0.03, -0.08, (Math.random()-0.5)*0.03, 1.0);
                }
            }

            case ASH_DRIFT -> {
                for (int i = 0; i < n(4); i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 0.2 + Math.random() * 0.5;
                    world.spawnParticle(Particle.ASH,
                            feet.clone().add(Math.cos(a)*r, 0.1, Math.sin(a)*r),
                            0, 0, 0.08, 0, 0.02);
                }
            }

            case VOID_ECHO -> {
                double angle = (t * 0.25) % (Math.PI * 2);
                for (int i = 0; i < n(3); i++) {
                    double a = angle + Math.PI * 2.0 / 3 * i;
                    double r = 0.5 + i * 0.15;
                    double y = (t % 20) / 20.0 * 2.0;
                    world.spawnParticle(Particle.PORTAL,
                            feet.clone().add(Math.cos(a)*r, y, Math.sin(a)*r),
                            0, 0, 0.04, 0, 0.02);
                }
            }

            case HEARTBEAT -> {
                double pulse = 0.55 + Math.sin(t * 0.35) * 0.25;
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(220, 40, 60), sz(1.2f));
                int pts = n(8);
                for (int i = 0; i < pts; i++) {
                    double a = Math.PI * 2.0 / pts * i;
                    world.spawnParticle(Particle.DUST,
                            feet.clone().add(Math.cos(a)*pulse, 1.0, Math.sin(a)*pulse),
                            1, 0, 0, 0, 0, dust);
                }
                if (every(t, 6)) {
                    world.spawnParticle(Particle.HEART,
                            feet.clone().add(0, 1.6, 0), n(1), 0.15, 0.1, 0.15, 0);
                }
            }

            case BUBBLE_WELL -> {
                for (int i = 0; i < n(4); i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 0.45;
                    world.spawnParticle(Particle.BUBBLE_COLUMN_UP,
                            feet.clone().add(Math.cos(a)*r, 0.05, Math.sin(a)*r),
                            0, 0, 0.12, 0, 0.02);
                }
            }

            case DRAGON_SOUL -> {
                double angle = (t * 0.10) % (Math.PI * 2);
                int pts = n(10);
                for (int i = 0; i < pts; i++) {
                    double a = angle + Math.PI * 2.0 / pts * i;
                    double y = 1.0 + Math.sin(a * 2 + t * 0.05) * 0.15;
                    world.spawnParticle(Particle.DRAGON_BREATH,
                            feet.clone().add(Math.cos(a)*0.85, y, Math.sin(a)*0.85),
                            0, 0, 0.02, 0, 0.01);
                }
                if (every(t, 8)) {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            feet.clone().add(0, 0.3, 0), n(3), 0.2, 0.1, 0.2, 0.02);
                }
            }

            case GOLD_ORBIT -> {
                double angle = (t * 0.16) % (Math.PI * 2);
                Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 200, 40), sz(1.1f));
                for (int i = 0; i < n(8); i++) {
                    double a = angle + Math.PI * 2.0 / 8 * i;
                    double y = 0.7 + (i % 2) * 0.5;
                    world.spawnParticle(Particle.DUST,
                            feet.clone().add(Math.cos(a)*0.9, y, Math.sin(a)*0.9),
                            1, 0, 0, 0, 0, gold);
                }
                if (every(t, 5)) {
                    world.spawnParticle(Particle.FLAME,
                            feet.clone().add(0, 0.2, 0), n(2), 0.25, 0.05, 0.25, 0.01);
                }
            }

            case ELECTRIC_HALO -> {
                double angle = (t * 0.35) % (Math.PI * 2);
                int pts = n(12);
                for (int i = 0; i < pts; i++) {
                    double a = angle + Math.PI * 2.0 / pts * i;
                    world.spawnParticle(Particle.ELECTRIC_SPARK,
                            feet.clone().add(Math.cos(a)*0.7, 1.4, Math.sin(a)*0.7),
                            0, 0, 0.04, 0, 0.05);
                }
                double angle2 = -(t * 0.20) % (Math.PI * 2);
                for (int i = 0; i < n(6); i++) {
                    double a = angle2 + Math.PI / 3.0 * i;
                    world.spawnParticle(Particle.CRIT,
                            feet.clone().add(Math.cos(a)*0.4, 0.6, Math.sin(a)*0.4),
                            0, 0, 0.02, 0, 0.01);
                }
            }

            case WITCH_RING -> {
                double angle = (t * 0.14) % (Math.PI * 2);
                for (int i = 0; i < n(10); i++) {
                    double a = angle + Math.PI * 2.0 / 10 * i;
                    world.spawnParticle(Particle.WITCH,
                            feet.clone().add(Math.cos(a)*0.95, 0.15, Math.sin(a)*0.95),
                            0, 0, 0.08, 0, 0.02);
                }
                if (every(t, 4)) {
                    world.spawnParticle(Particle.ENTITY_EFFECT,
                            feet.clone().add(0, 1.2, 0), n(2), 0.3, 0.2, 0.3, 0.01);
                }
            }

            case CLOUD_PUFF -> {
                if (every(t, 3)) {
                    world.spawnParticle(Particle.CLOUD,
                            feet.clone().add(0, 0.2, 0), n(3), 0.35, 0.05, 0.35, 0.01);
                }
                world.spawnParticle(Particle.WHITE_ASH,
                        feet.clone().add(0, 0.4, 0), n(2), 0.4, 0.1, 0.4, 0.01);
            }

            case NETHER_FLAME -> {
                for (int i = 0; i < n(3); i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 0.2 + Math.random() * 0.4;
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            feet.clone().add(Math.cos(a)*r, 0.05, Math.sin(a)*r),
                            0, (Math.random()-0.5)*0.05, 0.12, (Math.random()-0.5)*0.05, 1.0);
                }
                if (every(t, 4)) {
                    world.spawnParticle(Particle.SOUL,
                            feet.clone().add(0, 0.8, 0), n(1), 0.3, 0.1, 0.3, 0.02);
                }
            }

            case SAKURA -> {
                for (int i = 0; i < n(4); i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 0.8;
                    world.spawnParticle(Particle.CHERRY_LEAVES,
                            feet.clone().add(Math.cos(a)*r, 2.2, Math.sin(a)*r),
                            0, (Math.random()-0.5)*0.04, -0.06, (Math.random()-0.5)*0.04, 1.0);
                }
                if (every(t, 6)) {
                    world.spawnParticle(Particle.HAPPY_VILLAGER,
                            feet.clone().add(0, 1.0, 0), n(2), 0.4, 0.5, 0.4, 0.01);
                }
            }

            case TOTEM_BLESS -> {
                double angle = (t * 0.11) % (Math.PI * 2);
                for (int i = 0; i < n(6); i++) {
                    double a = angle + Math.PI * 2.0 / 6 * i;
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING,
                            feet.clone().add(Math.cos(a)*0.7, 0.8 + Math.sin(t * 0.1 + i) * 0.3, Math.sin(a)*0.7),
                            0, 0, 0.05, 0, 0.02);
                }
                if (every(t, 5)) {
                    world.spawnParticle(Particle.HAPPY_VILLAGER,
                            feet.clone().add(0, 1.5, 0), n(3), 0.35, 0.2, 0.35, 0);
                }
            }

            case SPARKLE_RING -> {
                double angle = (t * 0.28) % (Math.PI * 2);
                for (int i = 0; i < n(10); i++) {
                    double a = angle + Math.PI * 2.0 / 10 * i;
                    world.spawnParticle(Particle.FIREWORK,
                            feet.clone().add(Math.cos(a)*0.75, 1.35, Math.sin(a)*0.75),
                            0, 0, 0.02, 0, 0.01);
                }
                if (every(t, 3)) {
                    world.spawnParticle(Particle.END_ROD,
                            feet.clone().add(0, 1.2, 0), n(1), 0.4, 0.3, 0.4, 0.02);
                }
            }

            case SCULK_WHISPER -> {
                double angle = (t * 0.12) % (Math.PI * 2);
                for (int i = 0; i < n(6); i++) {
                    double a = angle + Math.PI * 2.0 / 6 * i;
                    world.spawnParticle(Particle.SCULK_SOUL,
                            feet.clone().add(Math.cos(a)*0.7, 0.3 + (i % 2) * 0.5, Math.sin(a)*0.7),
                            0, 0, 0.04, 0, 0.01);
                }
                if (every(t, 5)) {
                    world.spawnParticle(Particle.SCULK_CHARGE_POP,
                            feet.clone().add(0, 1.0, 0), n(2), 0.3, 0.3, 0.3, 0);
                }
            }

            case AURORA -> {
                for (int i = 0; i < n(6); i++) {
                    float hue   = ((t * 4 + i * 60) % 360) / 360.0f;
                    java.awt.Color c = java.awt.Color.getHSBColor(hue, 0.85f, 1.0f);
                    Particle.DustOptions dust = new Particle.DustOptions(
                            Color.fromRGB(c.getRed(), c.getGreen(), c.getBlue()), sz(1.4f));
                    double a = (t * 0.15 + Math.PI * 2.0 / 6 * i) % (Math.PI * 2);
                    double r = 0.85;
                    double y = 0.9 + Math.sin(t * 0.12 + i) * 0.45;
                    world.spawnParticle(Particle.DUST,
                            feet.clone().add(Math.cos(a)*r, y, Math.sin(a)*r),
                            1, 0, 0, 0, 0, dust);
                }
                if (every(t, 3)) {
                    world.spawnParticle(Particle.END_ROD,
                            feet.clone().add(0, 1.0, 0), n(1), 0.5, 0.6, 0.5, 0.03);
                }
            }

            case FIELD_RIG -> { /* 由 FieldRigSession 处理 */ }
        }
    }

    // ── 公开 API ──────────────────────────────────────────────────────────────

    public boolean owns(UUID uuid, AuraType type) {
        return owned.getOrDefault(uuid, Collections.emptySet()).contains(type);
    }

    public void giveAura(UUID uuid, AuraType type) {
        owned.computeIfAbsent(uuid, k -> new HashSet<>()).add(type);
        save();
    }

    public void equip(UUID uuid, AuraType type) {
        if (!owns(uuid, type)) return;
        stopFieldRig(uuid);
        equipped.put(uuid, type);
        stationaryTicks.put(uuid, 0);
        if (type == AuraType.FIELD_RIG) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                fieldRigs.put(uuid, new FieldRigSession(plugin, p));
            }
        }
        save();
    }

    public void unequip(UUID uuid) {
        stopFieldRig(uuid);
        equipped.remove(uuid);
        stationaryTicks.remove(uuid);
        save();
    }

    private void stopFieldRig(UUID uuid) {
        FieldRigSession s = fieldRigs.remove(uuid);
        if (s != null) s.stop();
    }

    public AuraType getEquipped(UUID uuid) { return equipped.get(uuid); }

    public Set<AuraType> getOwned(UUID uuid) {
        return Collections.unmodifiableSet(owned.getOrDefault(uuid, Collections.emptySet()));
    }

    public ParticleBrightness getBrightness(UUID uuid) {
        return brightness.getOrDefault(uuid, ParticleBrightness.MEDIUM);
    }

    public ParticleBrightness cycleBrightness(UUID uuid) {
        ParticleBrightness next = getBrightness(uuid).next();
        brightness.put(uuid, next);
        save();
        return next;
    }

    public void setBrightness(UUID uuid, ParticleBrightness b) {
        brightness.put(uuid, b == null ? ParticleBrightness.MEDIUM : b);
        save();
    }

    public void onPlayerQuit(UUID uuid) {
        stopFieldRig(uuid);
        lastLoc.remove(uuid);
        stationaryTicks.remove(uuid);
    }

    public void disable() {
        if (task != null) task.cancel();
        new HashMap<>(fieldRigs).forEach((u, s) -> s.stop());
        fieldRigs.clear();
        save();
    }

    // ── 持久化 ────────────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);

        if (cfg.isConfigurationSection("owned")) {
            for (String key : cfg.getConfigurationSection("owned").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    Set<AuraType> set = new HashSet<>();
                    for (String s : cfg.getStringList("owned." + key)) {
                        AuraType t = AuraType.fromKey(s);
                        if (t != null) set.add(t);
                    }
                    owned.put(uuid, set);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (cfg.isConfigurationSection("equipped")) {
            for (String key : cfg.getConfigurationSection("equipped").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    AuraType t = AuraType.fromKey(cfg.getString("equipped." + key, ""));
                    if (t != null) equipped.put(uuid, t);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (cfg.isConfigurationSection("brightness")) {
            for (String key : cfg.getConfigurationSection("brightness").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    brightness.put(uuid, ParticleBrightness.fromKey(cfg.getString("brightness." + key)));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        owned.forEach((uuid, set) -> {
            List<String> list = new ArrayList<>();
            set.forEach(t -> list.add(t.name()));
            cfg.set("owned." + uuid, list);
        });
        equipped.forEach((uuid, t) -> cfg.set("equipped." + uuid, t.name()));
        brightness.forEach((uuid, b) -> {
            if (b != null && b != ParticleBrightness.MEDIUM)
                cfg.set("brightness." + uuid, b.name());
        });
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("无法保存 auras.yml: " + e.getMessage());
        }
    }
}
