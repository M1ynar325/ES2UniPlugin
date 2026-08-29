package com.etherstories.escore.tasks;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.ActionBarManager;
import com.etherstories.escore.managers.RegionManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 地区进出检测 + 更新 sticky ActionBar。
 * 实际推送由 {@link ActionBarManager#pulse()} 高频维持，避免淡出抽搐。
 */
public class ActionBarTask extends BukkitRunnable {

    private static final String SEPARATOR = " &8— ";

    private final ES2UniPlugin plugin;
    private final Map<UUID, RegionManager.RegionResult> previous = new HashMap<>();
    private int beat;

    public ActionBarTask(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void cleanup(UUID uuid) {
        previous.remove(uuid);
        plugin.getActionBarManager().clearSticky(uuid);
    }

    @Override
    public void run() {
        RegionManager rm = plugin.getRegionManager();
        ActionBarManager ab = plugin.getActionBarManager();
        boolean scan = (beat++ & 1) == 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!scan) continue;
            Location loc = player.getLocation();
            if (loc.getWorld() == null) continue;

            RegionManager.RegionResult result = rm.getLocation(
                    loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());

            RegionManager.RegionResult prev = previous.get(player.getUniqueId());
            previous.put(player.getUniqueId(), result);

            if (prev != null && prev.district() != null) {
                boolean leftDistrict = result.district() == null
                        || !result.district().id().equals(prev.district().id());
                if (leftDistrict) {
                    player.sendMessage(ColorUtil.colorize(
                            "&8[ES2] &7你离开了 &f" + prev.district().name()));
                }
            }

            if (prev != null && prev.territory() != null) {
                boolean leftTerritory = result.territory() == null
                        || !result.territory().id().equals(prev.territory().id());
                if (leftTerritory) {
                    player.sendMessage(ColorUtil.colorize(
                            "&8[ES2] &7你离开了 &f" + prev.territory().name()));
                }
            }

            String text = result.format(ColorUtil.colorize(SEPARATOR));
            if (plugin.getTransitManager() != null) {
                plugin.getTransitManager().tickArrive(player, loc);
            }
            String station =                 plugin.getTransitManager() == null
                    ? null : plugin.getTransitManager().formatActionBar(player, loc);
            if (station != null && text != null) {
                ab.setSticky(player, station + ColorUtil.colorize(SEPARATOR) + ColorUtil.colorize("&7" + text));
            } else if (station != null) {
                ab.setSticky(player, station);
            } else if (text == null) {
                ab.setSticky(player, null);
            } else {
                ab.setSticky(player, ColorUtil.colorize("&7当前地区: &f" + text));
            }
        }

        ab.pulse();
    }
}
