package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 今日交易量 / 税收统计，并在配置的时刻广播日报。
 */
public class TradeStatsManager {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private BukkitTask task;

    private String day = LocalDate.now().format(DAY);
    private double payVolume;
    private double qsVolume;
    private double transitVolume;
    private double taxCollected;
    private final Map<UUID, Double> playerVolume = new HashMap<>();
    private final Map<UUID, String> nameCache = new HashMap<>();
    private final Set<String> firedSlots = new HashSet<>(); // date|HH:mm

    public TradeStatsManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "trade-stats.yml");
        load();
        startScheduler();
    }

    public void startScheduler() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickMinute, 20L * 30, 20L * 30);
    }

    public void stop() {
        if (task != null) task.cancel();
        save();
    }

    private void tickMinute() {
        ensureDay();
        if (!plugin.getConfig().getBoolean("tax-report.enabled", true)) return;
        List<String> times = plugin.getConfig().getStringList("tax-report.times");
        if (times == null || times.isEmpty()) times = List.of("12:00", "20:00");
        LocalTime now = LocalTime.now();
        String hm = String.format("%02d:%02d", now.getHour(), now.getMinute());
        for (String t : times) {
            if (t == null) continue;
            String key = day + "|" + t.trim();
            if (!t.trim().equals(hm)) continue;
            if (!firedSlots.add(key)) continue;
            String label = t.trim().startsWith("12") ? "午间" : "晚间";
            broadcastReport(label);
            save();
        }
    }

    private void ensureDay() {
        String today = LocalDate.now().format(DAY);
        if (!today.equals(day)) {
            day = today;
            payVolume = 0;
            qsVolume = 0;
            taxCollected = 0;
            transitVolume = 0;
            playerVolume.clear();
            nameCache.clear();
            firedSlots.removeIf(s -> !s.startsWith(today));
            save();
        }
    }

    public void recordPay(Player from, Player to, double amount, double tax) {
        recordPay(from, (org.bukkit.OfflinePlayer) to, amount, tax);
    }

    public void recordPay(Player from, org.bukkit.OfflinePlayer to, double amount, double tax) {
        if (amount <= 0) return;
        ensureDay();
        payVolume += amount;
        taxCollected += Math.max(0, tax);
        if (from != null) bump(from, amount);
        if (to != null) {
            // OfflinePlayer bump
            playerVolume.merge(to.getUniqueId(), amount * 0.5, Double::sum);
            if (to.getName() != null) nameCache.put(to.getUniqueId(), to.getName());
        }
        saveLazy();
    }

    public void recordTransit(Player from, double amount) {
        if (amount <= 0) return;
        ensureDay();
        transitVolume += amount;
        if (from != null) bump(from, amount);
        saveLazy();
    }

    public void recordQuickShop(Player buyer, double total, double tax) {
        if (total <= 0) return;
        ensureDay();
        qsVolume += total;
        taxCollected += Math.max(0, tax);
        if (buyer != null) bump(buyer, total);
        saveLazy();
    }

    private void bump(Player p, double v) {
        if (p == null || v <= 0) return;
        playerVolume.merge(p.getUniqueId(), v, Double::sum);
        nameCache.put(p.getUniqueId(), p.getName());
    }

    private long lastSaveMs;

    private void saveLazy() {
        long now = System.currentTimeMillis();
        if (now - lastSaveMs > 15_000) {
            lastSaveMs = now;
            save();
        }
    }

    public double getTotalVolume() {
        ensureDay();
        return payVolume + qsVolume + transitVolume;
    }

    public double getPayVolume() { ensureDay(); return payVolume; }
    public double getQsVolume() { ensureDay(); return qsVolume; }
    public double getTaxCollected() { ensureDay(); return taxCollected; }

    public record VolumeEntry(UUID uuid, String name, double volume) {}

    public List<VolumeEntry> getTopPlayers(int limit) {
        ensureDay();
        List<VolumeEntry> list = new ArrayList<>();
        for (Map.Entry<UUID, Double> e : playerVolume.entrySet()) {
            list.add(new VolumeEntry(e.getKey(),
                    nameCache.getOrDefault(e.getKey(), "?"), e.getValue()));
        }
        list.sort((a, b) -> Double.compare(b.volume(), a.volume()));
        if (list.size() > limit) return list.subList(0, limit);
        return list;
    }

    public double getPlayerVolume(UUID uuid) {
        ensureDay();
        return playerVolume.getOrDefault(uuid, 0.0);
    }

    public void broadcastReport(String label) {
        ensureDay();
        String topName = "暂无";
        double topVol = 0;
        for (Map.Entry<UUID, Double> e : playerVolume.entrySet()) {
            if (e.getValue() > topVol) {
                topVol = e.getValue();
                topName = nameCache.getOrDefault(e.getKey(), e.getKey().toString().substring(0, 8));
            }
        }
        String fmt = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(getTotalVolume())
                : String.format("%.2f", getTotalVolume());
        String taxFmt = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(taxCollected)
                : String.format("%.2f", taxCollected);
        String topFmt = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(topVol)
                : String.format("%.2f", topVol);

        Bukkit.broadcastMessage(ColorUtil.colorize("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        Bukkit.broadcastMessage(ColorUtil.colorize("&b[ES2] &f" + label + "交易日报 &8(" + day + ")"));
        Bukkit.broadcastMessage(ColorUtil.colorize(
                "&7今日交易总量 &f" + fmt
                        + " &8(转账 " + fmtNum(payVolume) + " / 商店 " + fmtNum(qsVolume)
                        + " / 交通 " + fmtNum(transitVolume) + ")"));
        Bukkit.broadcastMessage(ColorUtil.colorize("&7今日税收合计 &e" + taxFmt));
        Bukkit.broadcastMessage(ColorUtil.colorize("&7交易量最高 &f" + topName + " &8→ &f" + topFmt));
        Bukkit.broadcastMessage(ColorUtil.colorize("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    private String fmtNum(double v) {
        return plugin.getVaultHook().isEnabled() ? plugin.getVaultHook().format(v) : String.format("%.2f", v);
    }

    public void sendPersonalReport(Player player) {
        ensureDay();
        String topName = "暂无";
        double topVol = 0;
        for (Map.Entry<UUID, Double> e : playerVolume.entrySet()) {
            if (e.getValue() > topVol) {
                topVol = e.getValue();
                topName = nameCache.getOrDefault(e.getKey(), "?");
            }
        }
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &f今日交易总量 &7" + fmtNum(getTotalVolume())
                + " &8| 税 &e" + fmtNum(taxCollected)
                + " &8| 最高 &f" + topName + " &7" + fmtNum(topVol)));
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        day = cfg.getString("day", day);
        payVolume = cfg.getDouble("pay-volume", 0);
        qsVolume = cfg.getDouble("qs-volume", 0);
        transitVolume = cfg.getDouble("transit-volume", 0);
        taxCollected = cfg.getDouble("tax-collected", 0);
        if (cfg.isConfigurationSection("players")) {
            for (String k : cfg.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID u = UUID.fromString(k);
                    playerVolume.put(u, cfg.getDouble("players." + k + ".volume", 0));
                    String n = cfg.getString("players." + k + ".name");
                    if (n != null) nameCache.put(u, n);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        firedSlots.addAll(cfg.getStringList("fired"));
        ensureDay();
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("day", day);
        cfg.set("pay-volume", payVolume);
        cfg.set("qs-volume", qsVolume);
        cfg.set("transit-volume", transitVolume);
        cfg.set("tax-collected", taxCollected);
        for (Map.Entry<UUID, Double> e : playerVolume.entrySet()) {
            cfg.set("players." + e.getKey() + ".volume", e.getValue());
            cfg.set("players." + e.getKey() + ".name", nameCache.get(e.getKey()));
        }
        cfg.set("fired", new ArrayList<>(firedSlots));
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存 trade-stats.yml: " + e.getMessage());
        }
    }
}
