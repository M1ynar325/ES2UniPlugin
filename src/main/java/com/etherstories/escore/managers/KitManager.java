package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ECOS 自有 Kit 系统。
 * claimMode: ONCE = 每人一次；COOLDOWN = 冷却后可重复领取。
 */
public class KitManager {

    public enum ClaimMode { ONCE, COOLDOWN }

    public static class KitDef {
        public final String name;
        public ClaimMode claimMode;
        public int cooldownSeconds;
        public List<ItemStack> items;

        public KitDef(String name, ClaimMode mode, int cd, List<ItemStack> items) {
            this.name = name;
            this.claimMode = mode;
            this.cooldownSeconds = Math.max(0, cd);
            this.items = items;
        }
    }

    private final ES2UniPlugin plugin;
    private final File kitsFile;
    private final File claimsFile;
    private final Map<String, KitDef> kits = new LinkedHashMap<>();
    /** uuid → kitName → lastClaimEpochMs (ONCE 用 Long.MAX_VALUE 标记已领) */
    private final Map<UUID, Map<String, Long>> claims = new HashMap<>();

    public KitManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        this.claimsFile = new File(plugin.getDataFolder(), "kit_claims.yml");
        load();
    }

    public boolean exists(String name) {
        return kits.containsKey(normalize(name));
    }

    public KitDef get(String name) {
        return kits.get(normalize(name));
    }

    public Collection<KitDef> all() {
        return kits.values();
    }

    public List<String> names() {
        return new ArrayList<>(kits.keySet());
    }

    public KitDef createEmpty(String name) {
        String key = normalize(name);
        KitDef def = new KitDef(key, ClaimMode.ONCE, 0, new ArrayList<>());
        kits.put(key, def);
        saveKits();
        return def;
    }

    public void saveKit(KitDef def) {
        kits.put(normalize(def.name), def);
        saveKits();
    }

    public boolean delete(String name) {
        KitDef removed = kits.remove(normalize(name));
        if (removed == null) return false;
        saveKits();
        return true;
    }

    /**
     * 领取本地 kit。
     * @return null = 成功；否则为错误信息
     */
    public String claim(Player player, String name) {
        KitDef def = get(name);
        if (def == null) return "套件不存在";
        if (def.items == null || def.items.isEmpty()) return "套件为空，请联系管理员";

        UUID uuid = player.getUniqueId();
        String key = normalize(name);
        long now = System.currentTimeMillis();
        Long last = claims.computeIfAbsent(uuid, u -> new HashMap<>()).get(key);

        if (def.claimMode == ClaimMode.ONCE) {
            if (last != null) return "该套件每人只能领取一次";
        } else {
            if (last != null) {
                long remainMs = last + def.cooldownSeconds * 1000L - now;
                if (remainMs > 0) {
                    long sec = (remainMs + 999) / 1000;
                    return "冷却中，剩余 " + formatDuration(sec);
                }
            }
        }

        // 背包空间粗检
        int empty = 0;
        for (ItemStack s : player.getInventory().getStorageContents())
            if (s == null || s.getType().isAir()) empty++;
        if (empty < def.items.size()) return "背包空间不足（需要至少 " + def.items.size() + " 格）";

        for (ItemStack stack : def.items) {
            if (stack == null || stack.getType().isAir()) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack.clone());
            leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        }

        claims.get(uuid).put(key, def.claimMode == ClaimMode.ONCE ? Long.MAX_VALUE : now);
        saveClaims();
        return null;
    }

    public String describeRule(KitDef def) {
        if (def.claimMode == ClaimMode.ONCE) return "每人一次";
        return "冷却 " + formatDuration(def.cooldownSeconds);
    }

    public String claimStatus(Player player, KitDef def) {
        Long last = claims.getOrDefault(player.getUniqueId(), Map.of()).get(def.name);
        if (def.claimMode == ClaimMode.ONCE) {
            return last != null ? "&c已领取" : "&a可领取";
        }
        if (last == null) return "&a可领取";
        long remain = last + def.cooldownSeconds * 1000L - System.currentTimeMillis();
        if (remain <= 0) return "&a可领取";
        return "&e冷却 " + formatDuration((remain + 999) / 1000);
    }

    public static String formatDuration(long seconds) {
        if (seconds < 60) return seconds + "秒";
        if (seconds < 3600) return (seconds / 60) + "分" + (seconds % 60) + "秒";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        return h + "时" + m + "分";
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        kits.clear();
        if (kitsFile.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(kitsFile);
            ConfigurationSection root = cfg.getConfigurationSection("kits");
            if (root != null) {
                for (String key : root.getKeys(false)) {
                    ConfigurationSection sec = root.getConfigurationSection(key);
                    if (sec == null) continue;
                    ClaimMode mode = "COOLDOWN".equalsIgnoreCase(sec.getString("claim-mode", "ONCE"))
                            ? ClaimMode.COOLDOWN : ClaimMode.ONCE;
                    int cd = sec.getInt("cooldown-seconds", 0);
                    @SuppressWarnings("unchecked")
                    List<ItemStack> items = (List<ItemStack>) (List<?>) sec.getList("items", List.of());
                    List<ItemStack> clean = new ArrayList<>();
                    if (items != null) {
                        for (ItemStack i : items) {
                            if (i != null && !i.getType().isAir()) clean.add(i);
                        }
                    }
                    kits.put(normalize(key), new KitDef(normalize(key), mode, cd, clean));
                }
            }
        }

        claims.clear();
        if (claimsFile.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(claimsFile);
            for (String uuidStr : cfg.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ConfigurationSection sec = cfg.getConfigurationSection(uuidStr);
                    if (sec == null) continue;
                    Map<String, Long> map = new HashMap<>();
                    for (String kit : sec.getKeys(false))
                        map.put(normalize(kit), sec.getLong(kit));
                    claims.put(uuid, map);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void reload() {
        load();
    }

    private void saveKits() {
        FileConfiguration cfg = new YamlConfiguration();
        for (KitDef def : kits.values()) {
            String path = "kits." + def.name;
            cfg.set(path + ".claim-mode", def.claimMode.name());
            cfg.set(path + ".cooldown-seconds", def.cooldownSeconds);
            cfg.set(path + ".items", def.items);
        }
        try { cfg.save(kitsFile); } catch (IOException e) {
            plugin.getLogger().warning("kits.yml 保存失败: " + e.getMessage());
        }
    }

    private void saveClaims() {
        FileConfiguration cfg = new YamlConfiguration();
        claims.forEach((uuid, map) -> map.forEach((kit, ts) ->
                cfg.set(uuid.toString() + "." + kit, ts)));
        try { cfg.save(claimsFile); } catch (IOException e) {
            plugin.getLogger().warning("kit_claims.yml 保存失败: " + e.getMessage());
        }
    }
}
