package com.etherstories.escore.listeners;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.estate.EstateUnit;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class HotelListener implements Listener {

    private final ES2UniPlugin plugin;

    public HotelListener(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDoor(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || !doorLike(block.getType())) return;
        EstateUnit unit = plugin.getEstateManager().at(block.getLocation());
        if (unit == null) return;
        if (plugin.getHotelManager().hotelOf(unit) == null) return;
        Player p = event.getPlayer();
        if (plugin.getHotelManager().canOpen(p, unit)) return;
        event.setCancelled(true);
        p.sendMessage(ColorUtil.colorize("&8[酒店] &c门锁着。住客持房卡，店员可开。"));
    }

    private static boolean doorLike(Material m) {
        String n = m.name();
        return n.endsWith("_DOOR") || n.endsWith("DOOR")
                || n.endsWith("_TRAPDOOR") || n.endsWith("TRAPDOOR")
                || n.endsWith("_FENCE_GATE") || n.endsWith("FENCE_GATE");
    }
}
