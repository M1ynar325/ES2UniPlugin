package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFKManager {

    private final ES2UniPlugin plugin;
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final Map<UUID, Boolean> afkState = new HashMap<>();

    public AFKManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateActivity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        if (Boolean.TRUE.equals(afkState.get(player.getUniqueId()))) {
            setAFK(player, false);
        }
    }

    public void setAFK(Player player, boolean afk) {
        afkState.put(player.getUniqueId(), afk);
        // TAB list name is managed by TabTask — only broadcast the message here
        ConfigManager cfg = plugin.getConfigManager();
        String msg = afk ? cfg.getAfkEnterMessage() : cfg.getAfkReturnMessage();
        msg = msg.replace("{player}", player.getName());
        if (!msg.isEmpty()) Bukkit.broadcastMessage(ColorUtil.colorize(msg));
    }

    public boolean isAFK(Player player) {
        return Boolean.TRUE.equals(afkState.get(player.getUniqueId()));
    }

    public void remove(Player player) {
        lastActivity.remove(player.getUniqueId());
        afkState.remove(player.getUniqueId());
    }

    /** Manual toggle from GUI. */
    public void manualToggle(Player player) {
        boolean current = Boolean.TRUE.equals(afkState.get(player.getUniqueId()));
        setAFK(player, !current);
        // Reset activity so auto-detection doesn't immediately flip back
        if (current) lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /** Called every second by AFKCheckTask. */
    public void tick() {
        long now = System.currentTimeMillis();
        long threshold = plugin.getConfigManager().getAfkTimeoutMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("es2uni.afk.exempt")) continue;

            UUID uuid = player.getUniqueId();
            lastActivity.putIfAbsent(uuid, now);

            boolean currentlyAFK = Boolean.TRUE.equals(afkState.get(uuid));
            boolean shouldBeAFK = (now - lastActivity.get(uuid)) >= threshold;

            if (shouldBeAFK && !currentlyAFK) {
                setAFK(player, true);
            }
        }
    }
}
