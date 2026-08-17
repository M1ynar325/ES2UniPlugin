package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 把手持物品整份存下来（含 Quark 符文/染料等组件），用 ID 再取出。
 */
public class WeaponSaveManager {

    public record Saved(String id, String label, ItemStack item) {}

    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,32}");

    private final ES2UniPlugin plugin;
    private final File file;
    private final Map<String, Saved> saved = new LinkedHashMap<>();

    public WeaponSaveManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "weapon-saves.yml");
        load();
    }

    public static boolean validId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    public Saved get(String id) {
        return saved.get(normalize(id));
    }

    public List<String> ids() {
        return new ArrayList<>(saved.keySet());
    }

    public List<Saved> all() {
        return new ArrayList<>(saved.values());
    }

    /**
     * @param displayName 可空；非空则写入物品显示名（支持 & 颜色）
     * @return 错误信息，成功为 null
     */
    public String save(String rawId, String displayName, ItemStack hand) {
        if (hand == null || hand.getType().isAir()) return "请手持要保存的物品";
        String id = normalize(rawId);
        if (!validId(id)) return "ID 只能用小写字母、数字、下划线，最长 32";

        ItemStack copy = hand.clone();
        copy.setAmount(1);
        String label = displayName == null ? "" : displayName.trim();
        if (!label.isEmpty()) {
            ItemMeta meta = copy.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.colorize(label));
                copy.setItemMeta(meta);
            }
        } else {
            ItemMeta meta = copy.getItemMeta();
            if (meta != null && meta.hasDisplayName()) label = meta.getDisplayName();
            else label = copy.getType().name();
        }

        saved.put(id, new Saved(id, stripColor(label), copy));
        persist();
        return null;
    }

    public boolean delete(String rawId) {
        Saved removed = saved.remove(normalize(rawId));
        if (removed == null) return false;
        persist();
        return true;
    }

    public ItemStack cloneOf(String rawId) {
        Saved s = get(rawId);
        return s == null ? null : s.item().clone();
    }

    public void reload() {
        load();
    }

    private void load() {
        saved.clear();
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("items");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            ItemStack item = decode(sec);
            if (item == null || item.getType().isAir()) continue;
            String id = normalize(key);
            String label = sec.getString("label", item.getType().name());
            saved.put(id, new Saved(id, label, item));
        }
    }

    private void persist() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Saved s : saved.values()) {
            String path = "items." + s.id();
            cfg.set(path + ".label", s.label());
            cfg.set(path + ".item", s.item());
            String b64 = encodeBytes(s.item());
            if (b64 != null) cfg.set(path + ".item-b64", b64);
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("weapon-saves.yml 保存失败: " + e.getMessage());
        }
    }

    private static ItemStack decode(ConfigurationSection sec) {
        String b64 = sec.getString("item-b64");
        if (b64 != null && !b64.isBlank()) {
            try {
                byte[] raw = Base64.getDecoder().decode(b64);
                ItemStack fromBytes = ItemStack.deserializeBytes(raw);
                if (fromBytes != null && !fromBytes.getType().isAir()) return fromBytes;
            } catch (Throwable ignored) {
            }
        }
        ItemStack yaml = sec.getItemStack("item");
        return yaml;
    }

    private static String encodeBytes(ItemStack item) {
        try {
            return Base64.getEncoder().encodeToString(item.serializeAsBytes());
        } catch (Throwable t) {
            return null;
        }
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private static String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }
}
