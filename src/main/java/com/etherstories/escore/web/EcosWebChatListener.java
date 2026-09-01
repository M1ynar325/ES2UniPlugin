package com.etherstories.escore.web;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.BroadcastMessageEvent;

public class EcosWebChatListener implements Listener {

    private final ES2UniPlugin plugin;
    private final ChatFeed feed;

    public EcosWebChatListener(ES2UniPlugin plugin, ChatFeed feed) {
        this.plugin = plugin;
        this.feed = feed;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        if (linkAll(p)) return;
        String msg = event.getMessage();
        String shown;
        try {
            shown = String.format(event.getFormat(), p.getDisplayName(), msg);
        } catch (Exception e) {
            shown = "<" + p.getDisplayName() + "> " + msg;
        }
        feed.add(p.getName(), msg, "chat", null, shown);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBroadcast(BroadcastMessageEvent event) {
        String raw = event.getMessage();
        String plain = ChatFeed.strip(raw);
        if (plain.isBlank() || plain.startsWith("[Web]")) return;
        int n = event.getRecipients().size();
        int online = Bukkit.getOnlinePlayers().size();
        if (n > 0 && online > 0 && n < online) return;
        feed.add("系统", plain, "sys", null, raw);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        String msg = event.getJoinMessage();
        if (msg == null) return;
        feed.add(event.getPlayer().getName(), msg, "join", null, msg);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        String msg = event.getQuitMessage();
        if (msg == null) return;
        feed.add(event.getPlayer().getName(), msg, "quit", null, msg);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        String msg = event.getDeathMessage();
        if (msg == null) return;
        feed.add(event.getEntity().getName(), msg, "death", null, msg);
    }

    private boolean linkAll(Player p) {
        if (p == null || plugin.getESLinkHook() == null || !plugin.getESLinkHook().present()) return false;
        return Boolean.TRUE.equals(plugin.getESLinkHook().isChatAll(p.getUniqueId()));
    }
}
