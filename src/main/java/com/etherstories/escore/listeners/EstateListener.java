package com.etherstories.escore.listeners;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.items.EstateWand;
import com.etherstories.escore.managers.EstateManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class EstateListener implements Listener {

    private final ES2UniPlugin plugin;

    public EstateListener(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onWand(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;
        if (!EstateWand.isWand(event.getItem())
                && !EstateWand.isWand(event.getPlayer().getInventory().getItemInMainHand())) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        event.setCancelled(true);
        Player p = event.getPlayer();
        EstateManager em = plugin.getEstateManager();
        if (action == Action.LEFT_CLICK_BLOCK) {
            em.setPos1(p, block.getLocation());
            p.sendMessage(ColorUtil.colorize("&8[房产] &a点1 &f"
                    + block.getX() + ", " + block.getY() + ", " + block.getZ()
                    + "  &7再右键对角（含屋顶）"));
        } else {
            String err = em.setPos2(p, block.getLocation());
            if (err != null) {
                p.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                return;
            }
            var sel = em.selectionOf(p.getUniqueId());
            long vol = sel == null ? 0 : sel.asProbe().volume();
            p.sendMessage(ColorUtil.colorize("&8[房产] &a点2 &f"
                    + block.getX() + ", " + block.getY() + ", " + block.getZ()
                    + "  &7" + vol + " 格  ·  打开登记页写门牌"));
        }
        if (plugin.getEcosCommand() != null) plugin.getEcosCommand().refreshEstatePreview(p);
    }
}
