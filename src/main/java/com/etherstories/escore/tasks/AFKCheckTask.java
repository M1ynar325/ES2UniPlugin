package com.etherstories.escore.tasks;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AFKCheckTask extends BukkitRunnable {

    private final ES2UniPlugin plugin;

    public AFKCheckTask(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getAfkManager().tick();
    }
}
