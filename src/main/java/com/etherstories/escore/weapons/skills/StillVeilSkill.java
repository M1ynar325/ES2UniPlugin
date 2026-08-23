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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Still Veil「静幕」— 圆柱内时停。人只能转视角；敌对生物定住。
 * 机械动力实体不冻，避免 Create 碰撞/对实体 tick 崩溃。
 */
public class StillVeilSkill implements SkillInstance {

    static final Color WHITE = Color.fromRGB(245, 250, 255);
    static final Color JIQING = Color.fromRGB(110, 210, 200);

    private static final Map<UUID, Freeze> FROZEN = new ConcurrentHashMap<>();
    private static final Set<UUID> NIGHT = ConcurrentHashMap.newKeySet();

    private final Player player;
    private final UUID ownerUUID;
    private final double radius;
    private final double halfH;
    private final int holdTicks;
    private final int settleTicks;
    private final int moteCount;
    private final List<Mote> motes = new ArrayList<>();

    private int tick;
    private int settleAt = -1;
    private Location center;
    private final Set<UUID> mine = new HashSet<>();
    private final Set<UUID> lastPlayers = new HashSet<>();

    public StillVeilSkill(ES2UniPlugin plugin, Player player, WeaponManager wm) {
        this.player = player;
        this.ownerUUID = player.getUniqueId();
        this.radius = Math.max(8.0, wm.getCfgDouble(SkillType.STILL_VEIL, "radius", 30.0));
        this.halfH = Math.max(6.0, wm.getCfgDouble(SkillType.STILL_VEIL, "height", 24.0) / 2.0);
        this.holdTicks = Math.max(20, wm.getCfgInt(SkillType.STILL_VEIL, "duration-ticks", 160));
        this.settleTicks = Math.max(10, wm.getCfgInt(SkillType.STILL_VEIL, "settle-ticks", 40));
        this.moteCount = Math.max(80, Math.min(900, wm.getCfgInt(SkillType.STILL_VEIL, "particle-count", 520)));
        this.center = player.getLocation().clone();
        seedMotes();
        player.getWorld().playSound(center, Sound.BLOCK_BELL_RESONATE, 0.85f, 0.7f);
        player.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.55f);
        applyNight(player);
        burst(center);
        WeaponManager.sendActionBar(player, "&b静幕 &f展开", 25);
    }

    public void requestEnd() {
        beginSettle();
    }

    public static boolean frozen(UUID uuid) {
        return FROZEN.containsKey(uuid);
    }

    public static Location stayAt(UUID uuid) {
        Freeze f = FROZEN.get(uuid);
        return f == null ? null : f.loc;
    }

    public static void forget(UUID uuid) {
        Freeze f = FROZEN.remove(uuid);
        NIGHT.remove(uuid);
        if (f != null) thaw(f);
    }

    @Override
    public boolean tick() {
        tick++;
        if (!player.isOnline() || player.isDead()) {
            cleanup();
            return false;
        }
        if (settleAt < 0) {
            center = player.getLocation().clone();
            refreshTargets();
            pinBodies();
            drawMotes(0);
            drawRim();
            int left = Math.max(0, holdTicks - tick);
            if (tick % 5 == 0) {
                int pct = (int) Math.min(10, (long) tick * 10 / Math.max(1, holdTicks));
                String bar = "&b" + "█".repeat(10 - pct) + "&8" + "░".repeat(pct);
                String sec = String.format("%.1f", left / 20.0);
                WeaponManager.sendActionBar(player,
                        "  " + bar + "  &b静幕 &f" + sec + "s &8再按技能结束", 12);
                barFrozen("  " + bar + "  &b静幕 · 时停 &f" + sec + "s", 12);
            }
            if (tick >= holdTicks) beginSettle();
            return true;
        }

        int age = tick - settleAt;
        drawMotes(Math.min(1.0, age / (double) settleTicks));
        if (age % 8 == 0) {
            WeaponManager.sendActionBar(player, "&7静幕 · 沉降", 12);
            barSettling();
        }
        if (age >= settleTicks) {
            cleanup();
            return false;
        }
        return true;
    }

    private void beginSettle() {
        if (settleAt >= 0) return;
        settleAt = tick;
        for (Mote m : motes) {
            Location a = abs(m, 0);
            if (a.getWorld() == null) continue;
            int ground = a.getWorld().getHighestBlockYAt(a.getBlockX(), a.getBlockZ());
            m.fallTo = ground + 0.12;
            if (m.fallTo > a.getY()) m.fallTo = a.getY();
        }
        barFrozen("&7静幕 · 沉降", 20);
        releaseAll();
        if (player.isOnline()) {
            burst(player.getLocation());
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.35f);
            WeaponManager.sendActionBar(player, "&7静幕 · 沉降", 20);
        }
    }

    private boolean inside(Location loc) {
        if (loc == null || center.getWorld() == null || loc.getWorld() != center.getWorld()) return false;
        double dx = loc.getX() - center.getX();
        double dz = loc.getZ() - center.getZ();
        double dy = loc.getY() - center.getY();
        return dx * dx + dz * dz <= radius * radius && Math.abs(dy) <= halfH;
    }

    private void refreshTargets() {
        if (center.getWorld() == null) return;
        Set<UUID> now = new HashSet<>();
        applyNight(player);
        for (Player p : center.getWorld().getPlayers()) {
            if (!p.isOnline() || p.isDead()) continue;
            if (p.getUniqueId().equals(ownerUUID)) continue;
            if (!inside(p.getLocation())) continue;
            now.add(p.getUniqueId());
            freeze(p, true);
            applyNight(p);
        }
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, halfH, radius)) {
            if (!freezableMob(e) || !inside(e.getLocation())) continue;
            now.add(e.getUniqueId());
            freeze(e, false);
        }
        for (UUID id : new HashSet<>(mine)) {
            if (!now.contains(id)) release(id);
        }
        mine.clear();
        mine.addAll(now);
        lastPlayers.clear();
        lastPlayers.addAll(now);
    }

    private void barFrozen(String msg, int ticks) {
        for (UUID id : lastPlayers) {
            if (id.equals(ownerUUID)) continue;
            Player p = player.getServer().getPlayer(id);
            if (p != null && p.isOnline()) WeaponManager.sendActionBar(p, msg, ticks);
        }
    }

    private void barSettling() {
        barFrozen("&7静幕 · 沉降", 12);
        if (center.getWorld() == null) return;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getUniqueId().equals(ownerUUID)) continue;
            if (inside(p.getLocation())) WeaponManager.sendActionBar(p, "&7静幕 · 沉降", 12);
        }
    }

    private void burst(Location at) {
        World w = at.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.END_ROD, at.clone().add(0, 1, 0), 18, 0.4, 0.6, 0.4, 0.02, null, true);
        w.spawnParticle(Particle.FLASH, at.clone().add(0, 1, 0), 1, 0, 0, 0, 0, null, true);
    }

    private void drawRim() {
        World w = center.getWorld();
        if (w == null || tick % 2 != 0) return;
        int n = 16;
        double spin = tick * 0.08;
        for (int i = 0; i < n; i++) {
            double a = spin + Math.PI * 2.0 * i / n;
            Location p = center.clone().add(Math.cos(a) * radius, 0.15, Math.sin(a) * radius);
            w.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0, null, true);
            if (i % 2 == 0) {
                Location top = center.clone().add(Math.cos(a) * radius, halfH, Math.sin(a) * radius);
                w.spawnParticle(Particle.ENCHANT, top, 1, 0.05, 0.05, 0.05, 0, null, true);
            }
        }
        if (tick % 4 == 0) {
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 1.2, 0), 4, 0.5, 0.8, 0.5, 0.2, null, true);
        }
    }

    static boolean freezableMob(Entity e) {
        if (!(e instanceof LivingEntity le) || le.isDead()) return false;
        if (e instanceof Player || e instanceof ArmorStand) return false;
        if (e instanceof Animals || e instanceof Villager) return false;
        if (createRelated(e)) return false;
        if (e instanceof Enemy || e instanceof Monster || e instanceof Slime) return true;
        return false;
    }

    static boolean createRelated(Entity e) {
        try {
            String key = e.getType().getKey().toString().toLowerCase(Locale.ROOT);
            if (key.startsWith("create:") || key.contains("contraption") || key.contains("carriage"))
                return true;
        } catch (Throwable ignored) {
        }
        String cls = e.getClass().getName().toLowerCase(Locale.ROOT);
        return cls.contains("create") || cls.contains("contraption") || cls.contains("simibubi");
    }

    private void pinBodies() {
        for (UUID id : mine) {
            Freeze f = FROZEN.get(id);
            if (f == null || f.entity == null || !f.entity.isValid()) {
                release(id);
                continue;
            }
            f.entity.setVelocity(new Vector(0, 0, 0));
            Location look = f.loc.clone();
            if (f.look) {
                look.setYaw(f.entity.getLocation().getYaw());
                look.setPitch(f.entity.getLocation().getPitch());
            }
            f.entity.teleport(look);
        }
    }

    private void freeze(Entity e, boolean look) {
        UUID id = e.getUniqueId();
        Freeze existing = FROZEN.get(id);
        if (existing != null) {
            existing.owners.add(ownerUUID);
            return;
        }
        if (e instanceof LivingEntity le && le.isInsideVehicle()) le.leaveVehicle();
        boolean ai = e instanceof Mob m && m.hasAI();
        Freeze f = new Freeze(e, e.getLocation().clone(), e.hasGravity(), ai, look);
        f.owners.add(ownerUUID);
        FROZEN.put(id, f);
        e.setGravity(false);
        e.setVelocity(new Vector(0, 0, 0));
        e.setFallDistance(0);
        if (e instanceof Mob m) m.setAI(false);
        if (e instanceof LivingEntity le) le.setCollidable(false);
    }

    private void release(UUID id) {
        Freeze f = FROZEN.get(id);
        if (f == null) return;
        f.owners.remove(ownerUUID);
        if (!f.owners.isEmpty()) return;
        FROZEN.remove(id);
        thaw(f);
        dropNight(id);
        mine.remove(id);
    }

    private static void thaw(Freeze f) {
        Entity e = f.entity;
        if (e == null || !e.isValid()) return;
        e.setGravity(f.gravity);
        e.setFallDistance(0);
        if (e instanceof Mob m) m.setAI(f.ai);
        if (e instanceof LivingEntity le) {
            le.setCollidable(true);
            if (e instanceof Player p && p.isOnline()) {
                p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
        }
    }

    private void releaseAll() {
        for (UUID id : new HashSet<>(mine)) release(id);
        mine.clear();
        dropNight(ownerUUID);
    }

    private void applyNight(Player p) {
        NIGHT.add(p.getUniqueId());
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 80, 0, true, false, false));
    }

    private void dropNight(UUID id) {
        if (FROZEN.containsKey(id)) return;
        NIGHT.remove(id);
        Player p = player.getServer().getPlayer(id);
        if (p != null) p.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    private void seedMotes() {
        motes.clear();
        Random rng = new Random(ownerUUID.getMostSignificantBits() ^ 0x5E11L);
        for (int i = 0; i < moteCount; i++) {
            double theta = 2 * Math.PI * rng.nextDouble();
            double rr = radius * Math.sqrt(rng.nextDouble());
            double y = (rng.nextDouble() * 2 - 1) * halfH;
            motes.add(new Mote(
                    rr * Math.cos(theta),
                    y,
                    rr * Math.sin(theta),
                    i % 2 == 0 ? JIQING : WHITE));
        }
    }

    private void drawMotes(double fall) {
        World w = center.getWorld();
        if (w == null) return;
        for (Mote m : motes) {
            Location a = abs(m, fall);
            Particle.DustOptions dust = new Particle.DustOptions(m.color, 0.95f);
            w.spawnParticle(Particle.DUST, a, 1, 0, 0, 0, 0, dust, true);
        }
    }

    private Location abs(Mote m, double fall) {
        Location a = center.clone().add(m.ox, m.oy, m.oz);
        if (fall > 0) {
            double y0 = center.getY() + m.oy;
            a.setY(y0 + (m.fallTo - y0) * ease(fall));
        }
        return a;
    }

    private static double ease(double t) {
        return t * t * (3 - 2 * t);
    }

    @Override
    public void cleanup() {
        releaseAll();
        motes.clear();
        dropNight(ownerUUID);
    }

    @Override
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    private static final class Mote {
        final double ox, oy, oz;
        final Color color;
        double fallTo;

        Mote(double ox, double oy, double oz, Color color) {
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.color = color;
            this.fallTo = oy;
        }
    }

    static final class Freeze {
        final Entity entity;
        final Location loc;
        final boolean gravity;
        final boolean ai;
        final boolean look;
        final Set<UUID> owners = new HashSet<>();

        Freeze(Entity entity, Location loc, boolean gravity, boolean ai, boolean look) {
            this.entity = entity;
            this.loc = loc;
            this.gravity = gravity;
            this.ai = ai;
            this.look = look;
        }
    }
}
