package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 玩家个人音乐收藏（按歌曲 ID 去重，写盘）。
 */
public class MusicFavoritesManager {

    public record FavSong(String id, String name, String author, String album) {}

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private final Map<UUID, LinkedHashMap<String, FavSong>> byPlayer = new HashMap<>();

    public MusicFavoritesManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "music-favorites.yml");
        load();
    }

    public List<FavSong> list(UUID uuid) {
        LinkedHashMap<String, FavSong> map = byPlayer.get(uuid);
        if (map == null || map.isEmpty()) return List.of();
        return new ArrayList<>(map.values());
    }

    public int size(UUID uuid) {
        LinkedHashMap<String, FavSong> map = byPlayer.get(uuid);
        return map == null ? 0 : map.size();
    }

    public boolean has(UUID uuid, String id) {
        if (id == null || id.isBlank()) return false;
        LinkedHashMap<String, FavSong> map = byPlayer.get(uuid);
        return map != null && map.containsKey(id.trim());
    }

    public boolean add(UUID uuid, String id, String name, String author, String album) {
        if (id == null || id.isBlank()) return false;
        id = id.trim();
        LinkedHashMap<String, FavSong> map = byPlayer.computeIfAbsent(uuid, k -> new LinkedHashMap<>());
        if (map.containsKey(id)) return false;
        if (map.size() >= 60) return false;
        map.put(id, new FavSong(id,
                name == null || name.isBlank() ? id : name,
                author == null ? "" : author,
                album == null ? "" : album));
        save();
        return true;
    }

    public boolean remove(UUID uuid, String id) {
        if (id == null) return false;
        LinkedHashMap<String, FavSong> map = byPlayer.get(uuid);
        if (map == null) return false;
        boolean ok = map.remove(id.trim()) != null;
        if (ok) save();
        return ok;
    }

    public boolean toggle(Player player, String id, String name, String author, String album) {
        if (has(player.getUniqueId(), id)) {
            remove(player.getUniqueId(), id);
            return false;
        }
        add(player.getUniqueId(), id, name, author, album);
        return true;
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (!cfg.isConfigurationSection("players")) return;
        for (String uk : cfg.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uk);
                LinkedHashMap<String, FavSong> map = new LinkedHashMap<>();
                List<?> raw = cfg.getList("players." + uk);
                if (raw == null) continue;
                for (Object o : raw) {
                    if (!(o instanceof Map<?, ?> m)) continue;
                    Object idObj = m.get("id");
                    if (idObj == null) continue;
                    String id = String.valueOf(idObj);
                    if (id.isBlank() || "null".equals(id)) continue;
                    Object nameObj = m.get("name");
                    Object authorObj = m.get("author");
                    Object albumObj = m.get("album");
                    map.put(id, new FavSong(id,
                            nameObj == null ? id : String.valueOf(nameObj),
                            authorObj == null ? "" : String.valueOf(authorObj),
                            albumObj == null ? "" : String.valueOf(albumObj)));
                }
                byPlayer.put(uuid, map);
            } catch (Exception ignored) {
            }
        }
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, LinkedHashMap<String, FavSong>> e : byPlayer.entrySet()) {
            List<Map<String, String>> list = new ArrayList<>();
            for (FavSong s : e.getValue().values()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id", s.id());
                m.put("name", s.name());
                m.put("author", s.author());
                m.put("album", s.album());
                list.add(m);
            }
            cfg.set("players." + e.getKey(), list);
        }
        try {
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("music-favorites.yml 保存失败: " + ex.getMessage());
        }
    }
}
