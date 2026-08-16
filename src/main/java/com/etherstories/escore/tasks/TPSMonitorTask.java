package com.etherstories.escore.tasks;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.ConfigManager;
import com.etherstories.escore.utils.ColorUtil;
import com.etherstories.escore.utils.TPSUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class TPSMonitorTask extends BukkitRunnable {

    private final ES2UniPlugin plugin;
    private boolean warningSent = false;
    private boolean criticalSent = false;
    private long lastAlertTime = 0L;
    private boolean muted = false;

    public void setMuted(boolean muted) { this.muted = muted; }
    public boolean isMuted()            { return muted; }

    public TPSMonitorTask(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.isTpsAlertEnabled() || muted) return;

        double tps = TPSUtil.getTPS();
        double warnThreshold = cfg.getTpsWarnThreshold();
        double criticalThreshold = cfg.getTpsCriticalThreshold();
        long cooldownMs = (cfg.getTpsAlertCooldownTicks() / 20L) * 1000L;
        long now = System.currentTimeMillis();
        boolean cooldownExpired = (now - lastAlertTime) >= cooldownMs;

        if (tps < criticalThreshold) {
            if (!criticalSent || cooldownExpired) {
                String msg = cfg.getTpsCriticalMessage().replace("{tps}", String.format("%.1f", tps));
                Bukkit.broadcastMessage(ColorUtil.colorize(msg));
                criticalSent = true;
                warningSent = true;
                lastAlertTime = now;
            }
        } else if (tps < warnThreshold) {
            if (!warningSent || cooldownExpired) {
                String msg = cfg.getTpsWarnMessage().replace("{tps}", String.format("%.1f", tps));
                Bukkit.broadcastMessage(ColorUtil.colorize(msg));
                warningSent = true;
                criticalSent = false;
                lastAlertTime = now;
            }
        } else {
            if (warningSent || criticalSent) {
                Bukkit.broadcastMessage(ColorUtil.colorize(cfg.getTpsRecoverMessage()));
            }
            warningSent = false;
            criticalSent = false;
        }
    }
}
