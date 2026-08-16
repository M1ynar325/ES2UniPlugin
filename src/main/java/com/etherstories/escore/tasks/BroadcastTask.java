package com.etherstories.escore.tasks;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class BroadcastTask extends BukkitRunnable {

    private final ES2UniPlugin plugin;
    private int index = 0;

    public BroadcastTask(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        List<String> messages = plugin.getConfigManager().getBroadcastMessagesForToday();
        if (messages == null || messages.isEmpty()) return;

        String prefix = plugin.getConfigManager().getBroadcastPrefix();
        String message = messages.get(index % messages.size());
        index++;

        Bukkit.broadcastMessage(ColorUtil.colorize(prefix + message));
    }
}
