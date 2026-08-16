package com.etherstories.escore.listeners;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.items.PveItems;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

public class PveListener implements Listener {
    private final ES2UniPlugin plugin;

    public PveListener(ES2UniPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        String kind = PveItems.kind(item);
        if (kind == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!p.hasPermission("es2uni.admin")) {
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &c只有管理能用刷怪棒"));
            return;
        }
        boolean elite = PveItems.KIND_ELITE.equals(kind);
        if (p.isSneaking()) {
            EntityType next = PveItems.cycle(item, elite);
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7种类: &f" + PveItems.cn(next)));
            return;
        }
        EntityType type = PveItems.typeOf(item, elite);
        Location origin = p.getEyeLocation().add(p.getLocation().getDirection().multiply(3));
        if (elite) {
            spawnOne(origin, type, true);
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &6精英 &f" + PveItems.cn(type)));
        } else {
            int n = 6;
            for (int i = 0; i < n; i++) {
                Location at = origin.clone().add(
                        ThreadLocalRandom.current().nextDouble(-2.2, 2.2),
                        0,
                        ThreadLocalRandom.current().nextDouble(-2.2, 2.2));
                spawnOne(at, type, false);
            }
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &c刷出 &f" + n + " &c只 " + PveItems.cn(type)));
        }
    }

    private void spawnOne(Location loc, EntityType type, boolean elite) {
        if (loc.getWorld() == null) return;
        loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);
        var ent = loc.getWorld().spawnEntity(loc, type);
        if (!(ent instanceof LivingEntity living)) return;
        living.setRemoveWhenFarAway(true);
        if (!elite) return;
        living.setCustomName(ColorUtil.colorize("&6精英 " + PveItems.cn(type)));
        living.setCustomNameVisible(true);
        living.setGlowing(true);
        var attr = living.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(attr.getBaseValue() * 3);
            living.setHealth(attr.getBaseValue());
        }
        living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60 * 10, 1, false, false));
        living.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60 * 10, 0, false, false));
    }
}
