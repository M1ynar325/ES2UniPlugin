package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 招工处：多人、锁定、移除成员。 */
public class JobBoardManager {

    public enum Status { OPEN, TAKEN, DONE, CANCELLED }

    public static final class Job {
        public final String id;
        public String title;
        public String description;
        public double reward;
        public final UUID poster;
        public final String posterName;
        public boolean official;
        public Status status;
        public int maxWorkers;
        public boolean locked; // 锁定后不可再加入
        public final List<UUID> workers = new ArrayList<>();
        public final List<String> workerNames = new ArrayList<>();
        public final long createdAt;
        public long completedAt;
        public long adminModifiedAt;
        public String adminModifier;

        Job(String id, String title, String description, double reward,
            UUID poster, String posterName, boolean official, Status status,
            int maxWorkers, boolean locked, long createdAt, long completedAt,
            long adminModifiedAt, String adminModifier) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.reward = reward;
            this.poster = poster;
            this.posterName = posterName;
            this.official = official;
            this.status = status;
            this.maxWorkers = Math.max(1, maxWorkers);
            this.locked = locked;
            this.createdAt = createdAt;
            this.completedAt = completedAt;
            this.adminModifiedAt = adminModifiedAt;
            this.adminModifier = adminModifier;
        }

        public boolean escrowHeld() {
            return status == Status.OPEN || status == Status.TAKEN;
        }

        public boolean hasRoom() {
            return escrowHeld() && !locked && workers.size() < maxWorkers;
        }

        public boolean hasWorkers() { return !workers.isEmpty(); }

        public String slotsLabel() { return workers.size() + "/" + maxWorkers; }

        public String workersLabel() {
            if (workers.isEmpty()) return "暂无";
            return String.join(", ", workerNames);
        }

        public String completedLabel() {
            long t = completedAt > 0 ? completedAt : createdAt;
            return new SimpleDateFormat("MM-dd HH:mm").format(new Date(t));
        }

        public String adminModifiedLabel() {
            if (adminModifiedAt <= 0) return null;
            String t = new SimpleDateFormat("MM-dd HH:mm").format(new Date(adminModifiedAt));
            String who = adminModifier == null || adminModifier.isBlank() ? "管理员" : adminModifier;
            return "已被管理员修改 · " + t + " · " + who;
        }

        public void refreshStatus() {
            if (!escrowHeld()) return;
            if (workers.size() >= maxWorkers) status = Status.TAKEN;
            else status = Status.OPEN;
        }
    }

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private final Map<String, Job> jobs = new LinkedHashMap<>();
    private int nextId = 1;

    public JobBoardManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "jobs.yml");
        load();
    }

    public List<Job> listActive() {
        List<Job> out = new ArrayList<>();
        for (Job j : jobs.values()) {
            if (j.escrowHeld()) out.add(j);
        }
        out.sort(Comparator.comparing((Job j) -> !j.official).thenComparingLong(j -> -j.createdAt));
        return out;
    }

    /** 已完成委托，全服可看；按完成时间新→旧。 */
    public List<Job> listHistory() {
        List<Job> out = new ArrayList<>();
        for (Job j : jobs.values()) {
            if (j.status == Status.DONE) out.add(j);
        }
        out.sort(Comparator.comparingLong((Job j) ->
                -(j.completedAt > 0 ? j.completedAt : j.createdAt)));
        return out;
    }

    public int historySize() {
        return plugin.getConfig().getInt("municipal.jobs.history-size", 40);
    }

    public Job get(String id) { return jobs.get(id); }

    public int maxActivePerPlayer() {
        return plugin.getConfig().getInt("municipal.jobs.max-active-per-player", 5);
    }

    public double minReward() {
        return plugin.getConfig().getDouble("municipal.jobs.min-reward", 100);
    }

    public int maxWorkersCap() {
        return plugin.getConfig().getInt("municipal.jobs.max-workers", 8);
    }

    public int countActiveBy(UUID uuid) {
        int n = 0;
        for (Job j : jobs.values()) {
            if (j.poster.equals(uuid) && j.escrowHeld()) n++;
        }
        return n;
    }

    public String create(Player poster, String title, String description,
                         double reward, boolean official, int maxWorkers) {
        if (!plugin.getVaultHook().isEnabled()) return "经济系统不可用";
        if (title == null || title.isBlank()) return "标题不能为空";
        if (reward < minReward()) return "报酬至少 " + plugin.getVaultHook().format(minReward());
        if (!official && countActiveBy(poster.getUniqueId()) >= maxActivePerPlayer())
            return "进行中的委托已达上限 (" + maxActivePerPlayer() + ")";
        if (official && !poster.hasPermission("es2uni.admin")) return "无权发布官方委托";
        int slots = Math.max(1, Math.min(maxWorkers, maxWorkersCap()));

        String err = plugin.getVaultHook().withdraw(poster, reward);
        if (err != null) return err;

        String id = String.valueOf(nextId++);
        Job job = new Job(id, title.trim(), description == null ? "" : description.trim(),
                reward, poster.getUniqueId(), poster.getName(), official,
                Status.OPEN, slots, false, System.currentTimeMillis(), 0L, 0L, null);
        jobs.put(id, job);
        save();
        return null;
    }

    public String take(Player worker, String id) {
        return take(worker.getUniqueId(), worker.getName(), id);
    }

    public String take(UUID uuid, String name, String id) {
        Job j = jobs.get(id);
        if (j == null) return "委托不存在";
        if (j.locked) return "委托已锁定，不再接受新人";
        if (!j.hasRoom()) return "人数已满或不可接";
        if (j.poster.equals(uuid)) return "不能接自己的委托";
        if (j.workers.contains(uuid)) return "你已接过此委托";

        j.workers.add(uuid);
        j.workerNames.add(name == null ? "?" : name);
        j.refreshStatus();
        save();
        notify(j.poster, "&a" + (name == null ? "?" : name) + " &7加入委托 &f" + j.title
                + " &8(" + j.slotsLabel() + ")");
        return null;
    }

    /** 发布者/管理移除接单人（不退款，席位腾出）。 */
    public String removeWorker(Player actor, String id, String workerNameOrUuid) {
        Job j = jobs.get(id);
        if (j == null) return "委托不存在";
        if (!j.escrowHeld()) return "委托已结束";
        boolean admin = actor.hasPermission("es2uni.admin");
        if (!admin && !j.poster.equals(actor.getUniqueId())) return "无权移除";

        int idx = -1;
        for (int i = 0; i < j.workers.size(); i++) {
            if (j.workerNames.get(i).equalsIgnoreCase(workerNameOrUuid)
                    || j.workers.get(i).toString().equalsIgnoreCase(workerNameOrUuid)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) return "未找到该接单人";
        UUID removed = j.workers.remove(idx);
        String name = j.workerNames.remove(idx);
        j.refreshStatus();
        save();
        notify(removed, "&7你已被移出委托 &f" + j.title);
        notify(j.poster, "&7已移除 &f" + name + " &8(" + j.slotsLabel() + ")");
        return null;
    }

    public String setLocked(Player actor, String id, boolean lock) {
        Job j = jobs.get(id);
        if (j == null) return "委托不存在";
        if (!j.escrowHeld()) return "委托已结束";
        boolean admin = actor.hasPermission("es2uni.admin");
        if (!admin && !j.poster.equals(actor.getUniqueId())) return "无权操作";
        j.locked = lock;
        save();
        return null;
    }

    public String complete(Player actor, String id) {
        Job j = jobs.get(id);
        if (j == null) return "委托不存在";
        if (!j.escrowHeld()) return "委托已结束";
        if (!j.hasWorkers()) return "还没有人接单";
        boolean admin = actor.hasPermission("es2uni.admin");
        if (!admin && !j.poster.equals(actor.getUniqueId())) return "只有发布者可确认完成";

        int n = j.workers.size();
        double each = Math.floor((j.reward / n) * 100.0) / 100.0;
        double paid = 0;
        for (int i = 0; i < n; i++) {
            double share = (i == n - 1) ? (j.reward - paid) : each;
            paid += share;
            OfflinePlayer op = Bukkit.getOfflinePlayer(j.workers.get(i));
            String err = plugin.getVaultHook().deposit(op, share);
            if (err != null) return "打款失败(" + j.workerNames.get(i) + "): " + err;
            notify(j.workers.get(i), "&7委托完成，收到 &a" + plugin.getVaultHook().format(share)
                    + " &7— &f" + j.title + " &8(均分 " + n + " 人)");
        }
        j.status = Status.DONE;
        j.completedAt = System.currentTimeMillis();
        pruneHistory();
        save();
        return null;
    }

    /** 超出上限时删掉最旧的已完成记录（进行中的不动）。 */
    private void pruneHistory() {
        int max = historySize();
        if (max <= 0) return;
        List<Job> done = listHistory();
        if (done.size() <= max) return;
        for (int i = max; i < done.size(); i++) {
            jobs.remove(done.get(i).id);
        }
    }

    public String cancel(Player actor, String id) {
        Job j = jobs.get(id);
        if (j == null) return "委托不存在";
        if (!j.escrowHeld()) return "无法取消";
        boolean admin = actor.hasPermission("es2uni.admin");
        if (!admin && !j.poster.equals(actor.getUniqueId())) return "无权取消";
        if (!admin && j.hasWorkers()) return "已有人接单，请先移除成员或联系管理员";

        String err = plugin.getVaultHook().deposit(Bukkit.getOfflinePlayer(j.poster), j.reward);
        if (err != null) return "退款失败: " + err;
        j.status = Status.CANCELLED;
        j.workers.clear();
        j.workerNames.clear();
        save();
        return null;
    }

    public String adminEdit(Player admin, String id, String newTitle, String newDesc, Double newReward) {
        if (!admin.hasPermission("es2uni.admin")) return "无权";
        Job j = jobs.get(id);
        if (j == null) return "委托不存在";
        if (!j.escrowHeld()) return "已结束，无法改";
        if (newTitle != null && !newTitle.isBlank()) j.title = newTitle.trim();
        if (newDesc != null) j.description = newDesc.trim();
        if (newReward != null && newReward >= minReward() && Math.abs(newReward - j.reward) > 1e-6) {
            double diff = newReward - j.reward;
            if (diff > 0) {
                String err = plugin.getVaultHook().withdraw(admin, diff);
                if (err != null) return "补差额失败: " + err;
            } else {
                String err = plugin.getVaultHook().deposit(admin, -diff);
                if (err != null) return "退差额失败: " + err;
            }
            j.reward = newReward;
        }
        j.adminModifiedAt = System.currentTimeMillis();
        j.adminModifier = admin.getName();
        save();
        return null;
    }

    public String forceRemove(Player admin, String id) {
        if (!admin.hasPermission("es2uni.admin")) return "无权";
        Job j = jobs.get(id);
        if (j == null) return "委托不存在";
        if (j.escrowHeld()) {
            plugin.getVaultHook().deposit(Bukkit.getOfflinePlayer(j.poster), j.reward);
        }
        jobs.remove(id);
        save();
        return null;
    }

    private void notify(UUID uuid, String msg) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&8[招工处] " + msg));
        }
    }

    private void load() {
        jobs.clear();
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        nextId = cfg.getInt("next-id", 1);
        ConfigurationSection sec = cfg.getConfigurationSection("jobs");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try {
                Status st = Status.valueOf(s.getString("status", "OPEN"));
                Job job = new Job(id, s.getString("title", "?"), s.getString("description", ""),
                        s.getDouble("reward", 0), UUID.fromString(s.getString("poster")),
                        s.getString("poster-name", "?"), s.getBoolean("official", false), st,
                        s.getInt("max-workers", 1), s.getBoolean("locked", false),
                        s.getLong("created", System.currentTimeMillis()),
                        s.getLong("completed", 0L),
                        s.getLong("admin-modified", 0L), s.getString("admin-modifier"));
                // 旧数据：DONE 但无 completed 字段时用 created 兜底
                if (st == Status.DONE && job.completedAt <= 0) {
                    job.completedAt = job.createdAt;
                }
                List<String> wList = s.getStringList("workers");
                List<String> nList = s.getStringList("worker-names");
                if (wList.isEmpty() && s.contains("worker")) {
                    job.workers.add(UUID.fromString(s.getString("worker")));
                    job.workerNames.add(s.getString("worker-name", "?"));
                } else {
                    for (int i = 0; i < wList.size(); i++) {
                        job.workers.add(UUID.fromString(wList.get(i)));
                        job.workerNames.add(i < nList.size() ? nList.get(i) : "?");
                    }
                }
                jobs.put(id, job);
                try {
                    int n = Integer.parseInt(id);
                    if (n >= nextId) nextId = n + 1;
                } catch (NumberFormatException ignored) {}
            } catch (Exception ex) {
                plugin.getLogger().warning("jobs.yml 跳过 " + id + ": " + ex.getMessage());
            }
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("next-id", nextId);
        for (Job j : jobs.values()) {
            String p = "jobs." + j.id + ".";
            cfg.set(p + "title", j.title);
            cfg.set(p + "description", j.description);
            cfg.set(p + "reward", j.reward);
            cfg.set(p + "poster", j.poster.toString());
            cfg.set(p + "poster-name", j.posterName);
            cfg.set(p + "official", j.official);
            cfg.set(p + "status", j.status.name());
            cfg.set(p + "max-workers", j.maxWorkers);
            cfg.set(p + "locked", j.locked);
            List<String> w = new ArrayList<>();
            for (UUID u : j.workers) w.add(u.toString());
            cfg.set(p + "workers", w);
            cfg.set(p + "worker-names", new ArrayList<>(j.workerNames));
            cfg.set(p + "created", j.createdAt);
            cfg.set(p + "completed", j.completedAt);
            cfg.set(p + "admin-modified", j.adminModifiedAt);
            cfg.set(p + "admin-modifier", j.adminModifier);
        }
        try { cfg.save(dataFile); }
        catch (IOException e) { plugin.getLogger().warning("jobs.yml 保存失败: " + e.getMessage()); }
    }
}
