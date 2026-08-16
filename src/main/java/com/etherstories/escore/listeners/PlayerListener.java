package com.etherstories.escore.listeners;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.items.ECOSTerminalItem;
import com.etherstories.escore.managers.DeathManager;
import com.etherstories.escore.managers.FriendManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final ES2UniPlugin plugin;

    public PlayerListener(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String ip = event.getAddress().getHostAddress();
        // Notify online admins of the login IP
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("es2uni.admin"))
                    admin.sendMessage(ColorUtil.colorize(
                            "&8[ECOS] &7" + player.getName() + " 登录  &8IP: &f" + ip));
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlaytimeManager().onJoin(player);
        plugin.getNewbieGuideManager().onJoin(player);
        // 立场装置：重进服恢复会话
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            var aura = plugin.getAuraManager().getEquipped(player.getUniqueId());
            if (aura == com.etherstories.escore.auras.AuraType.FIELD_RIG) {
                plugin.getAuraManager().equip(player.getUniqueId(), aura);
            }
        }, 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) plugin.getInsuranceManager().backup(player);
        }, 40L);

        String msg = plugin.getConfigManager().getJoinMessage()
                .replace("{player}", player.getName());
        event.setJoinMessage(ColorUtil.colorize(msg));

        if (!player.hasPlayedBefore()) {
            // 首次加入
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(ColorUtil.colorize(
                        plugin.getConfigManager().getFirstJoinMessage()));
                player.getInventory().addItem(ECOSTerminalItem.create());
                if (plugin.getNewbieGuideManager().isEnabled()) {
                    player.sendMessage(ColorUtil.colorize(
                            "&8[ECOS] &a打开终端查看「新手引导」清单，约 30 分钟上手"));
                }
            }, 20L);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(ColorUtil.colorize(
                        plugin.getConfigManager().getWelcomeBackMessage()
                                .replace("{player}", player.getName())));

                // 未读留言
                int unread = plugin.getMailManager().unreadCount(player.getUniqueId());
                if (unread > 0)
                    player.sendMessage(ColorUtil.colorize(
                            "&8[ECOS] &7未读留言: &f" + unread + " &7条"));

                // 待处理好友请求
                int reqs = plugin.getFriendManager().getPendingRequests(player.getUniqueId()).size();
                if (reqs > 0)
                    player.sendMessage(ColorUtil.colorize(
                            "&8[ECOS] &7好友请求: &f" + reqs + " &7条待处理"));

                if (plugin.getNewbieGuideManager().shouldShow(player)) {
                    player.sendMessage(ColorUtil.colorize(
                            "&8[ECOS] &7新手引导未完成，终端可打开清单 &8(/ecos guide)"));
                }
                plugin.getTransitManager().onJoin(player);

                // 通知好友
                notifyFriends(player, true);
            }, 20L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlaytimeManager().onQuit(player);
        plugin.getAfkManager().remove(player);
        plugin.getAuraManager().onPlayerQuit(player.getUniqueId());
        plugin.getTransitManager().cleanupPlayer(player.getUniqueId());

        // 里程碑检查（在 playtime 保存后）
        long total = plugin.getPlaytimeManager().getTotalMillis(player.getUniqueId());
        plugin.getMilestoneManager().check(player, total);

        // 通知好友
        notifyFriends(player, false);

        String msg = plugin.getConfigManager().getQuitMessage()
                .replace("{player}", player.getName());
        event.setQuitMessage(ColorUtil.colorize(msg));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String cause = event.getDeathMessage() != null
                ? event.getDeathMessage().replaceAll("§[0-9a-fk-or]", "").replace(player.getName(), "").trim()
                : "未知";
        org.bukkit.Location loc = player.getLocation();
        plugin.getDeathManager().record(
                player.getUniqueId(),
                loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                cause);
        plugin.getInsuranceManager().onDeath(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!ECOSTerminalItem.isTerminalItem(item)) return;
        event.setCancelled(true);
        plugin.getEcosTerminalGUI().open(player);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void notifyFriends(Player player, boolean isJoin) {
        List<UUID> friends = plugin.getFriendManager().getFriends(player.getUniqueId());
        String msg = isJoin
                ? "&8[ECOS] &7好友 &f" + player.getName() + " &7已上线"
                : "&8[ECOS] &7好友 &f" + player.getName() + " &7已离线";
        for (UUID friendUuid : friends) {
            Player friend = Bukkit.getPlayer(friendUuid);
            if (friend != null && friend.isOnline())
                friend.sendMessage(ColorUtil.colorize(msg));
        }
    }
}
