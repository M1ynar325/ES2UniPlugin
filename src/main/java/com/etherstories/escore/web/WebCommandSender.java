package com.etherstories.escore.web;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 离线网页指令发送者：权限跟该玩家走，不走控制台。 */
public class WebCommandSender implements CommandSender {

    private final UUID uuid;
    private final String name;
    private final boolean adminSnap;
    private final List<String> replies = new ArrayList<>();

    public WebCommandSender(UUID uuid, String name, boolean adminSnap) {
        this.uuid = uuid;
        this.name = name;
        this.adminSnap = adminSnap;
    }

    public List<String> replies() {
        return List.copyOf(replies);
    }

    private void accept(String message) {
        if (message == null) return;
        String plain = ChatColor.stripColor(message).trim();
        if (!plain.isBlank()) replies.add(plain);
    }

    private OfflinePlayer offline() {
        return Bukkit.getOfflinePlayer(uuid);
    }

    @Override
    public void sendMessage(String message) {
        accept(message);
    }

    @Override
    public void sendMessage(String... messages) {
        if (messages == null) return;
        for (String m : messages) accept(m);
    }

    @Override
    public void sendMessage(UUID sender, String message) {
        accept(message);
    }

    @Override
    public void sendMessage(UUID sender, String... messages) {
        sendMessage(messages);
    }

    @Override
    public Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Component name() {
        return Component.text(name);
    }

    @Override
    public Spigot spigot() {
        return new CommandSender.Spigot();
    }

    @Override
    public boolean isPermissionSet(String name) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.isPermissionSet(name);
        return offline().isOp() || adminSnap;
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return perm != null && isPermissionSet(perm.getName());
    }

    @Override
    public boolean hasPermission(String perm) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.hasPermission(perm);
        if (offline().isOp()) return true;
        if (adminSnap && (perm == null || perm.equals("es2uni.admin") || perm.equals("*"))) return true;
        Permission node = perm == null ? null : Bukkit.getPluginManager().getPermission(perm);
        return node != null && node.getDefault().getValue(false);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return perm != null && hasPermission(perm.getName());
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return new PermissionAttachment(plugin, this);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return new PermissionAttachment(plugin, this);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return addAttachment(plugin);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {}

    @Override
    public void recalculatePermissions() {}

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getEffectivePermissions();
        return Set.of();
    }

    @Override
    public boolean isOp() {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.isOp();
        return offline().isOp();
    }

    @Override
    public void setOp(boolean value) {
        // 网页不能改 op
    }
}
