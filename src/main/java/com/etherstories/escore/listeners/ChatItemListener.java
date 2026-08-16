package com.etherstories.escore.listeners;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ItemChat;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ChatItemListener implements Listener {
    private final ES2UniPlugin plugin;

    public ChatItemListener(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (!plugin.getConfig().getBoolean("chat-item.enabled", true)) return;
        Player p = e.getPlayer();
        String plain = PlainTextComponentSerializer.plainText().serialize(e.message());
        if (!ItemChat.hasToken(plain)) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        ItemStack clone = (hand == null || hand.getType().isAir()) ? null : hand.clone();
        e.message(ItemChat.replace(plain, clone));
    }
}
