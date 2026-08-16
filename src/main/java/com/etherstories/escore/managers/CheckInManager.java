package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CheckInManager {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int HISTORY_KEEP_DAYS = 120;

    private final ES2UniPlugin         plugin;
    private final File                 dataFile;
    private final Map<UUID, String>    lastCheckIn = new HashMap<>();
    private final Map<UUID, Integer>   streaks     = new HashMap<>();
    private final Map<UUID, Set<String>> history   = new HashMap<>();
    private final Map<UUID, Integer>   makeupTickets = new HashMap<>(); // 漏签补签券

    public CheckInManager(ES2UniPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "checkin.yml");
        load();
    }

    /** Returns true if check-in was successful; false if already checked in today. */
    public boolean checkIn(UUID uuid) {
        String today = LocalDate.now().format(DATE_FMT);
        if (today.equals(lastCheckIn.get(uuid))) return false;

        String yesterday = LocalDate.now().minusDays(1).format(DATE_FMT);
        if (yesterday.equals(lastCheckIn.get(uuid))) {
            streaks.merge(uuid, 1, Integer::sum);
        } else {
            streaks.put(uuid, 1);
        }
        lastCheckIn.put(uuid, today);
        history.computeIfAbsent(uuid, k -> new HashSet<>()).add(today);
        pruneHistory(uuid);
        save();
        return true;
    }

    /**
     * 签到并发放奖励。
     * @return null 已签过；否则提示文案（已 colorize 前缀）
     */
    public String checkInWithReward(Player player) {
        if (!plugin.getConfigManager().isCheckInEnabled()) {
            return "&8[ECOS] &7签到功能已关闭";
        }
        if (!checkIn(player.getUniqueId())) {
            return "&8[ECOS] &7今日已签到";
        }
        int streak = getStreak(player.getUniqueId());
        double reward = plugin.getConfigManager().calcCheckInReward(streak);
        String moneyPart = "";
        if (reward > 0 && plugin.getVaultHook().isEnabled()) {
            // 签到奖励不受破产保护限制
            plugin.getVaultHook().deposit(player, reward);
            moneyPart = "  &7获得 &e" + plugin.getVaultHook().format(reward);
        } else if (reward > 0) {
            moneyPart = "  &c经济系统不可用，未发放货币";
        }
        return "&8[ECOS] &a签到成功！ &7连续 &f" + streak + " &7天" + moneyPart;
    }

    public boolean hasCheckedInToday(UUID uuid) {
        return LocalDate.now().format(DATE_FMT).equals(lastCheckIn.get(uuid));
    }

    public int getStreak(UUID uuid) {
        return streaks.getOrDefault(uuid, 0);
    }

    public boolean hasCheckedOn(UUID uuid, LocalDate date) {
        return history.getOrDefault(uuid, Collections.emptySet()).contains(date.format(DATE_FMT))
                || date.format(DATE_FMT).equals(lastCheckIn.get(uuid));
    }

    public int getMakeupTickets(UUID uuid) {
        return makeupTickets.getOrDefault(uuid, 0);
    }

    public void giveMakeupTickets(UUID uuid, int amount) {
        if (amount == 0) return;
        makeupTickets.merge(uuid, amount, Integer::sum);
        if (makeupTickets.get(uuid) < 0) makeupTickets.put(uuid, 0);
        save();
    }

    /**
     * 用补签券补某一天（不含今天、不含未来；最多回溯 60 天）。
     * @return 错误信息；null 成功
     */
    public String makeup(UUID uuid, LocalDate date) {
        if (date == null) return "日期无效";
        LocalDate today = LocalDate.now();
        if (!date.isBefore(today)) return "只能补签过去的日期";
        if (date.isBefore(today.minusDays(60))) return "只能补签近 60 天内";
        if (hasCheckedOn(uuid, date)) return "该日已签到";
        if (getMakeupTickets(uuid) <= 0) return "没有补签券（活动发放）";
        makeupTickets.merge(uuid, -1, Integer::sum);
        history.computeIfAbsent(uuid, k -> new HashSet<>()).add(date.format(DATE_FMT));
        pruneHistory(uuid);
        save();
        return null;
    }

    /** 某月已签到的日（1–31） */
    public Set<Integer> getCheckedDaysInMonth(UUID uuid, YearMonth month) {
        Set<Integer> days = new HashSet<>();
        Set<String> set = history.getOrDefault(uuid, Collections.emptySet());
        String prefix = month.toString(); // yyyy-MM
        for (String d : set) {
            if (d.startsWith(prefix + "-")) {
                try {
                    days.add(Integer.parseInt(d.substring(8)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        String last = lastCheckIn.get(uuid);
        if (last != null && last.startsWith(prefix + "-")) {
            try {
                days.add(Integer.parseInt(last.substring(8)));
            } catch (NumberFormatException ignored) {
            }
        }
        return days;
    }

    private void pruneHistory(UUID uuid) {
        Set<String> set = history.get(uuid);
        if (set == null) return;
        LocalDate cutoff = LocalDate.now().minusDays(HISTORY_KEEP_DAYS);
        set.removeIf(s -> {
            try {
                return LocalDate.parse(s, DATE_FMT).isBefore(cutoff);
            } catch (Exception e) {
                return true;
            }
        });
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                lastCheckIn.put(uuid, cfg.getString(key + ".date", ""));
                streaks.put(uuid, cfg.getInt(key + ".streak", 0));
                List<String> hist = cfg.getStringList(key + ".history");
                Set<String> set = new HashSet<>(hist);
                String last = lastCheckIn.get(uuid);
                if (last != null && !last.isBlank()) set.add(last);
                history.put(uuid, set);
                makeupTickets.put(uuid, cfg.getInt(key + ".makeup", 0));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        Set<UUID> all = new HashSet<>();
        all.addAll(lastCheckIn.keySet());
        all.addAll(makeupTickets.keySet());
        for (UUID uuid : all) {
            String k = uuid.toString();
            if (lastCheckIn.containsKey(uuid)) {
                cfg.set(k + ".date", lastCheckIn.get(uuid));
                cfg.set(k + ".streak", streaks.getOrDefault(uuid, 0));
            }
            List<String> hist = new ArrayList<>(history.getOrDefault(uuid, Collections.emptySet()));
            Collections.sort(hist);
            if (!hist.isEmpty()) cfg.set(k + ".history", hist);
            int mk = makeupTickets.getOrDefault(uuid, 0);
            if (mk > 0) cfg.set(k + ".makeup", mk);
        }
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("checkin.yml 保存失败: " + e.getMessage());
        }
    }
}
