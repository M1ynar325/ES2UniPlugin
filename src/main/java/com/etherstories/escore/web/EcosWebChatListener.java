package com.etherstories.escore.web;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.BroadcastMessageEvent;

public class EcosWebChatListener implements Listener {

    private final ChatFeed feed;

    public EcosWebChatListener(ChatFeed feed) {
        this.feed = feed;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        feed.add(event.getPlayer().getName(), event.getMessage(), "chat");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBroadcast(BroadcastMessageEvent event) {
        String plain = ChatFeed.strip(event.getMessage());
        if (plain.isBlank() || plain.startsWith("[Web]")) return;
        int n = event.getRecipients().size();
        int online = Bukkit.getOnlinePlayers().size();
        if (n > 0 && online > 0 && n < online) return;
        feed.add("系统", plain, "sys");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        String msg = event.getJoinMessage();
        if (msg == null) return;
        feed.add(event.getPlayer().getName(), msg, "join");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        String msg = event.getQuitMessage();
        if (msg == null) return;
        feed.add(event.getPlayer().getName(), msg, "quit");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        String msg = event.getDeathMessage();
        if (msg == null) return;
        feed.add(event.getEntity().getName(), msg, "death");
    }
}
