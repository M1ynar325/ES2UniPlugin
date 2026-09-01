package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FriendManager {

    private final ES2UniPlugin plugin;
    private final Map<UUID, Set<UUID>> friends        = new HashMap<>();
    private final Map<UUID, Set<UUID>> pending         = new HashMap<>(); // target -> senders
    private final Set<UUID>            locationSharing = new HashSet<>();
    private final File                 dataFile;

    public FriendManager(ES2UniPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "friends.yml");
        load();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);

        friends.clear();
        pending.clear();
        locationSharing.clear();

        if (cfg.isConfigurationSection("friends")) {
            for (String key : cfg.getConfigurationSection("friends").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                Set<UUID> set = new HashSet<>();
                for (String s : cfg.getStringList("friends." + key))
                    set.add(UUID.fromString(s));
                friends.put(uuid, set);
            }
        }
        for (String s : cfg.getStringList("location-sharing"))
            locationSharing.add(UUID.fromString(s));
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> e : friends.entrySet())
            cfg.set("friends." + e.getKey(),
                    e.getValue().stream().map(UUID::toString).toList());
        cfg.set("location-sharing",
                locationSharing.stream().map(UUID::toString).toList());
        try { cfg.save(dataFile); }
        catch (IOException e) { plugin.getLogger().warning("friends.yml 保存失败: " + e.getMessage()); }
    }

    // ── Requests ──────────────────────────────────────────────────────────────

    /** Returns false if already friends or request already sent. */
    public boolean sendRequest(Player sender, Player target) {
        return sendRequest(sender.getUniqueId(), target.getUniqueId());
    }

    public boolean sendRequest(UUID sender, UUID target) {
        if (sender == null || target == null || sender.equals(target)) return false;
        if (areFriends(sender, target)) return false;
        if (pending.getOrDefault(target, Collections.emptySet()).contains(sender)) return false;
        pending.computeIfAbsent(target, k -> new HashSet<>()).add(sender);
        return true;
    }

    public boolean acceptRequest(UUID target, UUID sender) {
        Set<UUID> reqs = pending.get(target);
        if (reqs == null || !reqs.remove(sender)) return false;
        friends.computeIfAbsent(target, k -> new HashSet<>()).add(sender);
        friends.computeIfAbsent(sender, k -> new HashSet<>()).add(target);
        save();
        return true;
    }

    public void denyRequest(UUID target, UUID sender) {
        Set<UUID> reqs = pending.get(target);
        if (reqs != null) reqs.remove(sender);
    }

    public List<UUID> getPendingRequests(UUID player) {
        return new ArrayList<>(pending.getOrDefault(player, Collections.emptySet()));
    }

    public boolean hasPendingRequestFrom(UUID target, UUID sender) {
        return pending.getOrDefault(target, Collections.emptySet()).contains(sender);
    }

    // ── Friends ───────────────────────────────────────────────────────────────

    public boolean areFriends(UUID a, UUID b) {
        return friends.getOrDefault(a, Collections.emptySet()).contains(b);
    }

    public List<UUID> getFriends(UUID player) {
        return new ArrayList<>(friends.getOrDefault(player, Collections.emptySet()));
    }

    public void removeFriend(UUID a, UUID b) {
        Set<UUID> fa = friends.get(a), fb = friends.get(b);
        if (fa != null) fa.remove(b);
        if (fb != null) fb.remove(a);
        save();
    }

    // ── Location Sharing ──────────────────────────────────────────────────────

    public boolean isLocationSharing(UUID player) {
        return locationSharing.contains(player);
    }

    public boolean toggleLocationSharing(UUID player) {
        boolean now = !isLocationSharing(player);
        if (now) locationSharing.add(player); else locationSharing.remove(player);
        save();
        return now;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String getDisplayName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }
}
