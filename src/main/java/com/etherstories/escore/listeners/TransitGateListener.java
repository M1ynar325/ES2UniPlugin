package com.etherstories.escore.listeners;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.items.TransitItems;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class TransitGateListener implements Listener {

    private final ES2UniPlugin plugin;

    public TransitGateListener(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        TransitManager tm = plugin.getTransitManager();
        if (TransitItems.isGate(item)) {
            String station = TransitItems.stationId(item);
            String mode = TransitItems.mode(item);
            if (station == null || tm.getStation(station) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ColorUtil.colorize("&8[交通] &c闸机未绑定有效车站"));
                return;
            }
            String cabin = TransitItems.cabin(item);
            tm.registerGate(event.getBlockPlaced(), station, mode == null ? "BOTH" : mode, cabin);
            String zh = switch ((mode == null ? "BOTH" : mode).toUpperCase()) {
                case "IN" -> "仅进站";
                case "OUT" -> "仅出站";
                default -> "进出均可";
            };
            event.getPlayer().sendMessage(ColorUtil.colorize(
                    "&8[交通] &a闸机已放置并写牌 · &f" + tm.stationName(station)
                            + " &8" + zh + " &e" + tm.cabinName(cabin)
                            + "\n&8[交通] &7旁边的活板门会在刷卡后打开。拆掉牌子即注销。"));
            return;
        }
        if (TransitItems.isTvm(item)) {
            String station = TransitItems.stationId(item);
            if (station == null || tm.getStation(station) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ColorUtil.colorize("&8[交通] &c售票机未绑定有效车站"));
                return;
            }
            tm.registerTvm(event.getBlockPlaced(), station);
            event.getPlayer().sendMessage(ColorUtil.colorize("&8[交通] &a已放置售票机 &f" + tm.stationName(station)
                    + "\n&8[交通] &7玩家右键打开购票/领卡/闪付/补票。"));
            return;
        }
        if (TransitItems.isAdjust(item)) {
            String station = TransitItems.stationId(item);
            if (station == null || tm.getStation(station) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ColorUtil.colorize("&8[交通] &c补票处未绑定有效车站"));
                return;
            }
            tm.registerAdjust(event.getBlockPlaced(), station);
            event.getPlayer().sendMessage(ColorUtil.colorize("&8[交通] &a已放置补票处 &f" + tm.stationName(station)
                    + "\n&8[交通] &7玩家右键办理补票 / 行程结算。拆掉牌子即注销。"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignChange(org.bukkit.event.block.SignChangeEvent event) {
        TransitManager tm = plugin.getTransitManager();
        TransitManager.Gate g = tm.gateAt(event.getBlock());
        if (g != null) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    tm.writeGateSign(event.getBlock(), g.stationId(), g.mode(), g.cabin()));
            return;
        }
        TransitManager.AdjustBooth adj = tm.adjustAt(event.getBlock());
        if (adj == null) return;
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () ->
                tm.writeAdjustSign(event.getBlock(), adj.stationId()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        unregisterBlock(event.getBlock(), event.getPlayer(), true, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        for (Block b : event.blockList()) unregisterBlock(b, null, false, null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block b : event.blockList()) unregisterBlock(b, null, false, null);
    }

    private void unregisterBlock(Block b, Player player, boolean fromBreak, BlockBreakEvent breakEvent) {
        TransitManager tm = plugin.getTransitManager();
        if (tm.gateAt(b) != null) {
            if (fromBreak && player != null && !player.hasPermission("es2uni.admin")) {
                breakEvent.setCancelled(true);
                player.sendMessage(ColorUtil.colorize("&8[交通] &7只有管理员能拆除闸机"));
                return;
            }
            tm.unregisterGate(b);
            if (player != null) player.sendMessage(ColorUtil.colorize("&8[交通] &7闸机已拆除并注销"));
            return;
        }
        if (tm.tvmAt(b) != null) {
            if (fromBreak && player != null && !player.hasPermission("es2uni.admin")) {
                breakEvent.setCancelled(true);
                player.sendMessage(ColorUtil.colorize("&8[交通] &7只有管理员能拆除售票机"));
                return;
            }
            tm.unregisterTvm(b);
            if (player != null) player.sendMessage(ColorUtil.colorize("&8[交通] &7售票机已拆除并注销"));
            return;
        }
        if (tm.adjustAt(b) != null) {
            if (fromBreak && player != null && !player.hasPermission("es2uni.admin")) {
                breakEvent.setCancelled(true);
                player.sendMessage(ColorUtil.colorize("&8[交通] &7只有管理员能拆除补票处"));
                return;
            }
            tm.unregisterAdjust(b);
            if (player != null) player.sendMessage(ColorUtil.colorize("&8[交通] &7补票处已拆除并注销"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        TransitManager tm = plugin.getTransitManager();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (TransitItems.isCard(held)) {
            Block clicked = event.getClickedBlock();
            boolean transit = clicked != null && (tm.gateAt(clicked) != null
                    || tm.tvmAt(clicked) != null || tm.adjustAt(clicked) != null);
            if (!transit) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (tm.isBindingTvm(player) && player.hasPermission("es2uni.admin")) {
            String sid = tm.consumeTvmBind(player);
            if (sid != null && tm.getStation(sid) != null) {
                event.setCancelled(true);
                tm.registerTvm(block, sid);
                player.sendMessage(ColorUtil.colorize("&8[交通] &a已把准星方块绑成 &f"
                        + tm.stationName(sid) + " &a站售票机（" + block.getType().name() + "）"
                        + "\n&8[交通] &7玩家右键此方块会打开售票界面，模组自带 UI 会被挡住。"));
                return;
            }
        }

        TransitManager.Tvm tvm = tm.tvmAt(block);
        if (tvm != null) {
            if (holdingTransitBlock(player)) return;
            event.setCancelled(true);
            plugin.getTransitTvmGUI().open(player, tvm.stationId());
            return;
        }

        TransitManager.AdjustBooth adj = tm.adjustAt(block);
        if (adj != null) {
            if (holdingTransitBlock(player)) return;
            event.setCancelled(true);
            plugin.getTransitAdjustGUI().open(player, adj.stationId());
            return;
        }

        TransitManager.Gate gate = tm.gateAt(block);
        if (gate == null) return;
        if (holdingTransitBlock(player)) return;
        event.setCancelled(true);
        if (block.getState() instanceof Sign && player.isSneaking() && player.hasPermission("es2uni.admin"))
            return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        TransitManager.TapResult r = tm.tap(player, gate, hand);
        for (String line : r.msg().split("\n")) {
            if (line.isBlank()) continue;
            player.sendMessage(ColorUtil.colorize("&8[交通] " + line));
        }
        if (plugin.getActionBarManager() != null) {
            String first = r.msg().split("\n")[0];
            plugin.getActionBarManager().sendTemp(player, first, 70);
        }
        if (!r.success()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.7f);
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.85f, 1.41f);
        if (r.openFlaps()) tm.pulseGateFlaps(block);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getHand() == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (!TransitItems.isCard(item)) return;
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;
        player.sendMessage(ColorUtil.colorize("&8[交通] &7这是交通卡，不能给生物命名"));
    }

    private static boolean holdingTransitBlock(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        return TransitItems.isGate(hand) || TransitItems.isTvm(hand) || TransitItems.isAdjust(hand);
    }
}
