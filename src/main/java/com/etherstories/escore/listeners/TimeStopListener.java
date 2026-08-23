package com.etherstories.escore.listeners;

import com.etherstories.escore.weapons.skills.StillVeilSkill;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

/** 静幕时停：身体锁死，只许转视角。 */
public final class TimeStopListener implements Listener {

    private static boolean ours(PlayerTeleportEvent e) {
        return e.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!StillVeilSkill.frozen(e.getPlayer().getUniqueId())) return;
        Location to = e.getTo();
        if (to == null) return;
        Location stay = StillVeilSkill.stayAt(e.getPlayer().getUniqueId());
        if (stay == null) return;
        if (e.getFrom().getX() == to.getX() && e.getFrom().getY() == to.getY() && e.getFrom().getZ() == to.getZ()) {
            return;
        }
        Location keep = stay.clone();
        keep.setYaw(to.getYaw());
        keep.setPitch(to.getPitch());
        e.setTo(keep);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (!StillVeilSkill.frozen(e.getPlayer().getUniqueId())) return;
        if (ours(e)) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (StillVeilSkill.frozen(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (StillVeilSkill.frozen(e.getDamager().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(org.bukkit.event.entity.EntityTargetEvent e) {
        if (StillVeilSkill.frozen(e.getEntity().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSprint(PlayerToggleSprintEvent e) {
        if (StillVeilSkill.frozen(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent e) {
        if (StillVeilSkill.frozen(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFly(PlayerToggleFlightEvent e) {
        if (StillVeilSkill.frozen(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent e) {
        if (e.getEntity() instanceof Player p && StillVeilSkill.frozen(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (StillVeilSkill.frozen(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (StillVeilSkill.frozen(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        StillVeilSkill.forget(e.getPlayer().getUniqueId());
    }
}
