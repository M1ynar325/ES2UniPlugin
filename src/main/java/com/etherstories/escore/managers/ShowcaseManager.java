package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * Create 机器展示点：登记坐标 + 每日一票。
 */
public class ShowcaseManager {

    public record Showcase(String id, UUID owner, String ownerName, String title,
                           String world, double x, double y, double z, int votes) {}

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private final Map<String, Showcase> showcases = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> votesToday = new HashMap<>(); // voter -> showcase ids
    private String voteDay = LocalDate.now().toString();

    public ShowcaseManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "showcases.yml");
        load();
    }

    public Collection<Showcase> all() {
        return Collections.unmodifiableCollection(showcases.values());
    }

    public List<Showcase> topByVotes(int limit) {
        List<Showcase> list = new ArrayList<>(showcases.values());
        list.sort((a, b) -> Integer.compare(b.votes(), a.votes()));
        if (list.size() > limit) return list.subList(0, limit);
        return list;
    }

    public String register(Player player, String title) {
        if (title == null || title.isBlank()) title = player.getName() + " 的机器";
        title = title.trim();
        if (title.length() > 32) title = title.substring(0, 32);
        long owned = showcases.values().stream().filter(s -> s.owner().equals(player.getUniqueId())).count();
        int max = plugin.getConfig().getInt("showcase.max-per-player", 3);
        if (owned >= max) return "每人最多登记 " + max + " 个展示点";

        String id = player.getUniqueId().toString().substring(0, 8) + "-" + System.currentTimeMillis() % 100000;
        Location loc = player.getLocation();
        Showcase s = new Showcase(id, player.getUniqueId(), player.getName(), title,
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), 0);
        showcases.put(id, s);
        save();
        return null;
    }

    public boolean remove(String id, UUID actor, boolean admin) {
        Showcase s = showcases.get(id);
        if (s == null) return false;
        if (!admin && !s.owner().equals(actor)) return false;
        showcases.remove(id);
        save();
        return true;
    }

    public String vote(Player voter, String id) {
        ensureVoteDay();
        Showcase s = showcases.get(id);
        if (s == null) return "展示点不存在";
        if (s.owner().equals(voter.getUniqueId())) return "不能给自己投票";
        Set<String> set = votesToday.computeIfAbsent(voter.getUniqueId(), k -> new HashSet<>());
        if (set.contains(id)) return "今天已经给这个点投过票了";
        set.add(id);
        showcases.put(id, new Showcase(s.id(), s.owner(), s.ownerName(), s.title(),
                s.world(), s.x(), s.y(), s.z(), s.votes() + 1));
        save();
        return null;
    }

    public boolean teleport(Player player, String id) {
        Showcase s = showcases.get(id);
        if (s == null) return false;
        World w = Bukkit.getWorld(s.world());
        if (w == null) return false;
        player.teleport(new Location(w, s.x(), s.y(), s.z()));
        return true;
    }

    public Showcase get(String id) { return showcases.get(id); }

    private void ensureVoteDay() {
        String today = LocalDate.now().toString();
        if (!today.equals(voteDay)) {
            voteDay = today;
            votesToday.clear();
        }
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        voteDay = cfg.getString("vote-day", voteDay);
        if (cfg.isConfigurationSection("list")) {
            for (String id : cfg.getConfigurationSection("list").getKeys(false)) {
                String p = "list." + id + ".";
                try {
                    UUID owner = UUID.fromString(cfg.getString(p + "owner"));
                    showcases.put(id, new Showcase(id, owner,
                            cfg.getString(p + "owner-name", "?"),
                            cfg.getString(p + "title", "未命名"),
                            cfg.getString(p + "world", "world"),
                            cfg.getDouble(p + "x"), cfg.getDouble(p + "y"), cfg.getDouble(p + "z"),
                            cfg.getInt(p + "votes", 0)));
                } catch (Exception ignored) {}
            }
        }
        if (cfg.isConfigurationSection("votes")) {
            for (String u : cfg.getConfigurationSection("votes").getKeys(false)) {
                try {
                    votesToday.put(UUID.fromString(u), new HashSet<>(cfg.getStringList("votes." + u)));
                } catch (Exception ignored) {}
            }
        }
        ensureVoteDay();
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("vote-day", voteDay);
        for (Showcase s : showcases.values()) {
            String p = "list." + s.id() + ".";
            cfg.set(p + "owner", s.owner().toString());
            cfg.set(p + "owner-name", s.ownerName());
            cfg.set(p + "title", s.title());
            cfg.set(p + "world", s.world());
            cfg.set(p + "x", s.x());
            cfg.set(p + "y", s.y());
            cfg.set(p + "z", s.z());
            cfg.set(p + "votes", s.votes());
        }
        for (Map.Entry<UUID, Set<String>> e : votesToday.entrySet()) {
            cfg.set("votes." + e.getKey(), new ArrayList<>(e.getValue()));
        }
        try { cfg.save(dataFile); } catch (IOException ex) {
            plugin.getLogger().warning("showcases.yml 保存失败: " + ex.getMessage());
        }
    }
}
