package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 清理掉落物回收站：保存 1 天，玩家可取回自己扔出/附近清理的物品。
 */
public class RecycleBinManager {

    public static final class Entry {
        public final String id;
        public final ItemStack stack;
        public final UUID owner;       // 掉落物 thrower，未知则为清理申请人
        public final String ownerName;
        public final String world;
        public final int x, y, z;
        public final String cleanupId;
        public final long storedAt;
        public final long expireAt;

        Entry(String id, ItemStack stack, UUID owner, String ownerName,
              String world, int x, int y, int z, String cleanupId,
              long storedAt, long expireAt) {
            this.id = id;
            this.stack = stack;
            this.owner = owner;
            this.ownerName = ownerName;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.cleanupId = cleanupId;
            this.storedAt = storedAt;
            this.expireAt = expireAt;
        }

        public boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }

        public String expireLabel() {
            long left = expireAt - System.currentTimeMillis();
            if (left <= 0) return "已过期";
            long h = left / 3_600_000L;
            long m = (left % 3_600_000L) / 60_000L;
            return h > 0 ? h + "小时后失效" : m + "分钟后失效";
        }

        public String storedLabel() {
            return new SimpleDateFormat("MM-dd HH:mm").format(new Date(storedAt));
        }
    }

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private int nextId = 1;

    public RecycleBinManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "recyclebin.yml");
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::purgeExpired, 20L * 60, 20L * 60 * 5);
    }

    public long retainMs() {
        return plugin.getConfig().getLong("municipal.cleanup.recyclebin-hours", 24) * 3_600_000L;
    }

    public int maxEntries() {
        return plugin.getConfig().getInt("municipal.cleanup.recyclebin-max", 500);
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("municipal.cleanup.recyclebin", true);
    }

    /**
     * 入库掉落物。成功入库的实体会立刻 remove。
     * @return 入库件数；回收站关闭时返回 -1（由调用方自行删除实体）
     */
    public int storeFromItems(Iterable<Item> items, String cleanupId, UUID fallbackOwner, String fallbackName) {
        if (!isEnabled()) return -1;
        purgeExpired();
        int n = 0;
        long now = System.currentTimeMillis();
        long exp = now + retainMs();
        for (Item item : items) {
            if (!item.isValid()) continue;
            if (entries.size() >= maxEntries()) break;
            ItemStack stack = item.getItemStack();
            if (stack == null || stack.getType().isAir()) {
                item.remove();
                continue;
            }
            UUID owner = item.getThrower();
            String name = fallbackName;
            if (owner != null) {
                var op = Bukkit.getOfflinePlayer(owner);
                if (op.getName() != null) name = op.getName();
            } else {
                owner = fallbackOwner;
            }
            var loc = item.getLocation();
            String id = String.valueOf(nextId++);
            entries.put(id, new Entry(
                    id, stack.clone(), owner, name == null ? "?" : name,
                    loc.getWorld() != null ? loc.getWorld().getName() : "?",
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                    cleanupId, now, exp));
            item.remove();
            n++;
        }
        if (n > 0) save();
        return n;
    }

    public List<Entry> listFor(Player player) {
        purgeExpired();
        List<Entry> out = new ArrayList<>();
        boolean admin = player.hasPermission("es2uni.admin");
        for (Entry e : entries.values()) {
            if (e.expired()) continue;
            if (admin || (e.owner != null && e.owner.equals(player.getUniqueId())))
                out.add(e);
        }
        return out;
    }

    public int countFor(UUID uuid) {
        int n = 0;
        for (Entry e : entries.values()) {
            if (!e.expired() && e.owner != null && e.owner.equals(uuid)) n++;
        }
        return n;
    }

    public int totalAlive() {
        int n = 0;
        for (Entry e : entries.values()) if (!e.expired()) n++;
        return n;
    }

    /** 取回一件；成功返回 null。 */
    public String reclaim(Player player, String id) {
        purgeExpired();
        Entry e = entries.get(id);
        if (e == null || e.expired()) {
            entries.remove(id);
            return "物品不存在或已过期";
        }
        boolean admin = player.hasPermission("es2uni.admin");
        if (!admin && (e.owner == null || !e.owner.equals(player.getUniqueId())))
            return "这不是你的回收物";

        Map<Integer, ItemStack> left = player.getInventory().addItem(e.stack.clone());
        if (!left.isEmpty()) {
            // 背包满：掉在脚边
            for (ItemStack s : left.values())
                player.getWorld().dropItemNaturally(player.getLocation(), s);
            player.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                    "&8[回收站] &7背包已满，多余物品掉在脚下"));
        }
        entries.remove(id);
        save();
        return null;
    }

    public void purgeExpired() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        Iterator<Map.Entry<String, Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expireAt < now) {
                it.remove();
                changed = true;
            }
        }
        if (changed) save();
    }

    private void load() {
        entries.clear();
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        nextId = cfg.getInt("next-id", 1);
        ConfigurationSection sec = cfg.getConfigurationSection("items");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try {
                ItemStack stack = s.getItemStack("stack");
                if (stack == null) continue;
                UUID owner = s.contains("owner") ? UUID.fromString(s.getString("owner")) : null;
                Entry e = new Entry(
                        id, stack, owner, s.getString("owner-name", "?"),
                        s.getString("world", "?"),
                        s.getInt("x"), s.getInt("y"), s.getInt("z"),
                        s.getString("cleanup-id", ""),
                        s.getLong("stored", System.currentTimeMillis()),
                        s.getLong("expire", System.currentTimeMillis()));
                if (!e.expired()) entries.put(id, e);
                try {
                    int n = Integer.parseInt(id);
                    if (n >= nextId) nextId = n + 1;
                } catch (NumberFormatException ignored) {}
            } catch (Exception ex) {
                plugin.getLogger().warning("recyclebin 跳过 " + id + ": " + ex.getMessage());
            }
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("next-id", nextId);
        for (Entry e : entries.values()) {
            if (e.expired()) continue;
            String p = "items." + e.id + ".";
            cfg.set(p + "stack", e.stack);
            if (e.owner != null) cfg.set(p + "owner", e.owner.toString());
            cfg.set(p + "owner-name", e.ownerName);
            cfg.set(p + "world", e.world);
            cfg.set(p + "x", e.x);
            cfg.set(p + "y", e.y);
            cfg.set(p + "z", e.z);
            cfg.set(p + "cleanup-id", e.cleanupId);
            cfg.set(p + "stored", e.storedAt);
            cfg.set(p + "expire", e.expireAt);
        }
        try {
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("recyclebin.yml 保存失败: " + ex.getMessage());
        }
    }
}
