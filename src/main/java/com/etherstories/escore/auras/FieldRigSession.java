package com.etherstories.escore.auras;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.WeaponManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * 立场装置：装备后长期有效。
 * 玻璃板被打掉后进入恢复队列，逐块重生；耗尽 / 回满有 actionbar 提示。
 */
public final class FieldRigSession {

    private enum PaneState { ORBIT, INTERCEPT, BROKEN }

    private static final class Pane {
        BlockDisplay display;
        PaneState state = PaneState.ORBIT;
        double angle;
        Projectile target;
        Location loc;

        Pane(BlockDisplay display, double angle) {
            this.display = display;
            this.angle = angle;
            this.loc = display.getLocation();
        }

        boolean dead() {
            return display == null || display.isDead() || !display.isValid();
        }

        boolean active() {
            return state == PaneState.ORBIT || state == PaneState.INTERCEPT;
        }
    }

    private final ES2UniPlugin plugin;
    private final UUID ownerId;
    private final List<Pane> panes = new ArrayList<>();
    private final Set<UUID> claimed = new HashSet<>();
    /** 待恢复队列：一次只重生一块 */
    private final Queue<Pane> recoverQueue = new ArrayDeque<>();
    private BukkitTask task;
    private int tick;
    /** 当前这块还要等多久才重生 */
    private int recoverCountdown;
    private boolean recovering;
    private Pane currentRecovering;

    private final int paneCount = 8;
    private final double orbitRadius = 1.55;
    private final double detectRange = 10.0;
    private final double interceptSpeed = 1.35;
    private final double hitRadius = 0.9;
    /** 每块恢复间隔（tick）——约 4 秒一块 */
    private final int reformInterval = 80;

    public FieldRigSession(ES2UniPlugin plugin, Player player) {
        this.plugin = plugin;
        this.ownerId = player.getUniqueId();
        spawnAll(player);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void spawnAll(Player player) {
        Location origin = player.getLocation().add(0, 1.05, 0);
        World w = origin.getWorld();
        w.playSound(origin, Sound.BLOCK_GLASS_PLACE, 0.7f, 1.3f);
        w.playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.2f);
        for (int i = 0; i < paneCount; i++) {
            double angle = (Math.PI * 2 / paneCount) * i;
            Location spawn = orbitPoint(origin, angle);
            panes.add(new Pane(spawnPane(spawn, angle), angle));
        }
    }

    private BlockDisplay spawnPane(Location loc, double angle) {
        BlockDisplay d = loc.getWorld().spawn(loc, BlockDisplay.class, disp -> {
            disp.setBlock(Material.PURPLE_STAINED_GLASS.createBlockData());
            disp.setBrightness(new Display.Brightness(15, 15));
            disp.setShadowRadius(0f);
            disp.setShadowStrength(0f);
            try {
                disp.setTeleportDuration(2);
                disp.setInterpolationDuration(2);
            } catch (Throwable ignored) {
            }
            applyPaneTransform(disp);
        });
        faceOutward(d, loc, angle);
        return d;
    }

    private static void applyPaneTransform(BlockDisplay disp) {
        disp.setTransformation(new Transformation(
                new Vector3f(-0.5f, -0.5f, -0.04f),
                new AxisAngle4f(0f, 0f, 1f, 0f),
                new Vector3f(0.95f, 1.15f, 0.08f),
                new AxisAngle4f(0f, 0f, 1f, 0f)
        ));
    }

    private Location orbitPoint(Location center, double angle) {
        return center.clone().add(
                Math.cos(angle) * orbitRadius,
                0.05,
                Math.sin(angle) * orbitRadius);
    }

    private void faceOutward(BlockDisplay disp, Location at, double angle) {
        Location l = at.clone();
        l.setYaw((float) Math.toDegrees(angle) - 90f);
        l.setPitch(0f);
        disp.teleport(l);
        applyPaneTransform(disp);
    }

    private int activeCount() {
        int n = 0;
        for (Pane p : panes) if (p.active()) n++;
        return n;
    }

    private void tick() {
        tick++;
        Player player = Bukkit.getPlayer(ownerId);
        if (player == null || !player.isOnline() || player.isDead()) {
            stop();
            return;
        }
        Location center = player.getLocation().add(0, 1.05, 0);
        World w = player.getWorld();

        if (tick % 5 == 0) {
            w.spawnParticle(Particle.DUST, center, 4, orbitRadius * 0.6, 0.25, orbitRadius * 0.6, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 50, 220), 0.75f));
        }

        // 逐块恢复
        tickRecover(player, center, w);

        if (activeCount() > 0 && tick % 5 == 0) {
            assign(center, player);
        }

        for (Pane pane : panes) {
            switch (pane.state) {
                case ORBIT -> {
                    if (pane.dead()) continue;
                    pane.angle += 0.09;
                    Location next = orbitPoint(center, pane.angle);
                    faceOutward(pane.display, next, pane.angle);
                    pane.loc = next;
                }
                case INTERCEPT -> tickIntercept(pane, w, player);
                case BROKEN -> { /* 等队列重生 */ }
            }
        }
    }

    private void tickRecover(Player player, Location center, World w) {
        if (!recovering) {
            Pane next = recoverQueue.poll();
            if (next == null) return;
            if (next.state != PaneState.BROKEN) return;
            currentRecovering = next;
            recovering = true;
            recoverCountdown = reformInterval;
            int queued = recoverQueue.size() + 1;
            WeaponManager.sendActionBar(player,
                    "&d立场恢复中 &8· &f下一块 &e" + (reformInterval / 20) + "s &8· 待恢复 &7" + queued, 30);
            return;
        }

        if (currentRecovering == null || currentRecovering.state != PaneState.BROKEN) {
            recovering = false;
            currentRecovering = null;
            return;
        }

        recoverCountdown--;
        if (recoverCountdown > 0) {
            if (recoverCountdown % 20 == 0) {
                WeaponManager.sendActionBar(player,
                        "&d立场恢复中 &8· &e" + Math.max(1, recoverCountdown / 20) + "s", 15);
            }
            return;
        }

        Pane pane = currentRecovering;
        Location next = orbitPoint(center, pane.angle);
        pane.display = spawnPane(next, pane.angle);
        pane.loc = next;
        pane.state = PaneState.ORBIT;
        pane.target = null;
        currentRecovering = null;
        recovering = false;

        w.playSound(next, Sound.BLOCK_GLASS_PLACE, 0.55f, 1.45f);
        w.spawnParticle(Particle.END_ROD, next, 10, 0.15, 0.2, 0.15, 0.02);
        w.spawnParticle(Particle.DUST, next, 8, 0.15, 0.2, 0.15, 0,
                new Particle.DustOptions(Color.fromRGB(180, 100, 255), 1.1f));

        int active = activeCount();
        if (active >= paneCount && recoverQueue.isEmpty()) {
            WeaponManager.sendActionBar(player, "&d立场 &a已全部恢复 &8(" + paneCount + "/" + paneCount + ")", 60);
            w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.35f);
        } else {
            WeaponManager.sendActionBar(player,
                    "&d立场 &f" + active + "/" + paneCount + " &8· 已恢复一块", 35);
        }
    }

    private void assign(Location center, Player owner) {
        for (Entity e : center.getWorld().getNearbyEntities(center, detectRange, detectRange, detectRange)) {
            if (!(e instanceof Projectile proj)) continue;
            if (!isThreat(proj, owner)) continue;
            if (claimed.contains(proj.getUniqueId())) continue;
            Vector toPlayer = center.toVector().subtract(proj.getLocation().toVector());
            Vector vel = proj.getVelocity();
            if (vel.lengthSquared() < 1e-6) continue;
            if (toPlayer.dot(vel) <= 0) continue;
            Vector cross = vel.clone().normalize().crossProduct(toPlayer.clone().normalize());
            if (cross.length() > 0.85 && toPlayer.length() > 2.5) continue;

            Pane free = null;
            double best = Double.MAX_VALUE;
            for (Pane p : panes) {
                if (p.state != PaneState.ORBIT || p.dead()) continue;
                double d = p.loc.distanceSquared(proj.getLocation());
                if (d < best) { best = d; free = p; }
            }
            if (free == null) return;
            free.state = PaneState.INTERCEPT;
            free.target = proj;
            claimed.add(proj.getUniqueId());
            free.loc.getWorld().playSound(free.loc, Sound.ENTITY_SHULKER_SHOOT, 0.45f, 1.6f);
        }
    }

    private boolean isThreat(Projectile proj, Player owner) {
        if (proj.isDead() || !proj.isValid()) return false;
        if (proj.getShooter() instanceof Player p && p.getUniqueId().equals(ownerId)) return false;
        return proj instanceof AbstractArrow
                || proj instanceof Snowball
                || proj instanceof Egg
                || proj instanceof Fireball
                || proj instanceof ShulkerBullet
                || proj instanceof LlamaSpit;
    }

    private void tickIntercept(Pane pane, World w, Player owner) {
        Projectile t = pane.target;
        if (t == null || t.isDead() || !t.isValid()) {
            // 目标消失：板回环绕，不进破碎队列
            pane.target = null;
            pane.state = PaneState.ORBIT;
            return;
        }
        Location goal = t.getLocation();
        Vector dir = goal.toVector().subtract(pane.loc.toVector());
        double dist = dir.length();
        if (dist < hitRadius) {
            claimed.remove(t.getUniqueId());
            t.remove();
            w.playSound(pane.loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.3f);
            w.spawnParticle(Particle.BLOCK, pane.loc, 24, 0.25, 0.25, 0.25, 0.04,
                    Material.PURPLE_STAINED_GLASS.createBlockData());
            w.spawnParticle(Particle.FLASH, pane.loc, 1, 0, 0, 0, 0);
            shatter(pane, w, owner);
            return;
        }
        dir.normalize().multiply(Math.min(interceptSpeed, dist));
        Location next = pane.loc.clone().add(dir);
        next.setYaw(pane.loc.getYaw());
        next.setPitch(0);
        if (!pane.dead()) {
            pane.display.teleport(next);
            applyPaneTransform(pane.display);
        }
        pane.loc = next;
        w.spawnParticle(Particle.DUST, next, 2, 0.04, 0.04, 0.04, 0,
                new Particle.DustOptions(Color.fromRGB(180, 80, 255), 1.0f));
    }

    private void shatter(Pane pane, World w, Player owner) {
        if (pane.target != null) claimed.remove(pane.target.getUniqueId());
        pane.target = null;
        if (!pane.dead()) {
            w.spawnParticle(Particle.BLOCK, pane.loc, 16, 0.2, 0.2, 0.2, 0.03,
                    Material.PURPLE_STAINED_GLASS.createBlockData());
            pane.display.remove();
        }
        pane.state = PaneState.BROKEN;
        recoverQueue.offer(pane);

        int active = activeCount();
        if (active <= 0) {
            WeaponManager.sendActionBar(owner,
                    "&c立场已耗尽 &8· &7逐块恢复中…", 70);
            w.playSound(owner.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 0.7f);
            w.playSound(owner.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 0.9f);
        } else {
            WeaponManager.sendActionBar(owner,
                    "&d立场 &f" + active + "/" + paneCount + " &8· 抵消", 40);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Pane p : panes) {
            if (p.display != null && !p.display.isDead()) p.display.remove();
        }
        panes.clear();
        claimed.clear();
        recoverQueue.clear();
        currentRecovering = null;
        recovering = false;
    }
}
