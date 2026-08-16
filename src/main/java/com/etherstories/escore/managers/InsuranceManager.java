package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 背包清空险：保留最近多次备份；管理选一次恢复，并赔偿货币。
 */
public class InsuranceManager {

    public static final class Backup {
        public final long time;
        public final ItemStack[] contents;
        public final ItemStack[] armor;
        public final ItemStack offhand;

        public Backup(long time, ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
            this.time = time;
            this.contents = contents;
            this.armor = armor;
            this.offhand = offhand;
        }

        public String timeLabel() {
            return new SimpleDateFormat("MM-dd HH:mm").format(new Date(time));
        }

        public int itemCount() {
            int n = 0;
            if (contents != null) for (ItemStack i : contents) if (i != null && !i.getType().isAir()) n++;
            if (armor != null) for (ItemStack i : armor) if (i != null && !i.getType().isAir()) n++;
            if (offhand != null && !offhand.getType().isAir()) n++;
            return n;
        }

        public List<String> sampleNames(int max) {
            List<String> names = new ArrayList<>();
            if (contents != null) {
                for (ItemStack i : contents) {
                    if (i == null || i.getType().isAir()) continue;
                    names.add(i.getType().name() + "×" + i.getAmount());
                    if (names.size() >= max) return names;
                }
            }
            return names;
        }
    }

    public static final class Claim {
        public final String id;
        public final UUID player;
        public final String playerName;
        public final long createdAt;
        public String status; // PENDING, APPROVED, DENIED
        public int chosenBackupIndex = -1;
        public boolean covered;
        public String coverKind = "none";
        public String coverPlace = "";

        Claim(String id, UUID player, String playerName, long createdAt, String status) {
            this.id = id;
            this.player = player;
            this.playerName = playerName;
            this.createdAt = createdAt;
            this.status = status;
        }
    }

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private final Map<UUID, List<Backup>> histories = new LinkedHashMap<>();
    private final Map<String, Claim> claims = new LinkedHashMap<>();
    private final Map<UUID, DeathCover> lastDeath = new LinkedHashMap<>();
    private int nextClaimId = 1;

    public record DeathCover(boolean covered, String kind, String place) {
        public String label() {
            if (!covered) return "不在保障范围";
            if ("transit".equals(kind)) return "车站内 · " + place;
            if ("territory".equals(kind)) return "公开领地 · " + place;
            return place;
        }
    }

    public InsuranceManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "insurance.yml");
        load();
        startAutoBackup();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("municipal.insurance.enabled", true);
    }

    public int historySize() {
        return plugin.getConfig().getInt("municipal.insurance.history-size", 5);
    }

    public double compensation() {
        return plugin.getConfig().getDouble("municipal.insurance.compensation", 10000);
    }

    public List<Backup> getHistory(UUID uuid) {
        return histories.getOrDefault(uuid, List.of());
    }

    /** 最新一次（兼容旧 GUI） */
    public Backup getBackup(UUID uuid) {
        List<Backup> list = histories.get(uuid);
        if (list == null || list.isEmpty()) return null;
        return list.get(list.size() - 1);
    }

    public List<Claim> pendingClaims() {
        List<Claim> out = new ArrayList<>();
        for (Claim c : claims.values()) {
            if ("PENDING".equals(c.status)) out.add(c);
        }
        return out;
    }

    public Claim getClaim(String id) { return claims.get(id); }

    public DeathCover lastDeath(UUID uuid) {
        return lastDeath.get(uuid);
    }

    public DeathCover inspect(org.bukkit.Location loc) {
        if (loc == null) return new DeathCover(false, "none", "");
        if (plugin.getConfig().getBoolean("municipal.insurance.cover-transit", true)
                && plugin.getTransitManager() != null) {
            var s = plugin.getTransitManager().stationAt(loc);
            if (s != null) {
                String n = s.displayName() == null || s.displayName().isBlank() ? s.id() : s.displayName();
                return new DeathCover(true, "transit", n);
            }
        }
        if (plugin.getConfig().getBoolean("municipal.insurance.cover-territory", true)
                && plugin.getRegionManager() != null && loc.getWorld() != null) {
            var r = plugin.getRegionManager().getLocation(
                    loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
            if (r.territory() != null && r.territory().visible()) {
                return new DeathCover(true, "territory", r.territory().name());
            }
        }
        return new DeathCover(false, "none", "");
    }

    public void onDeath(Player player) {
        if (!isEnabled()) return;
        lastDeath.put(player.getUniqueId(), inspect(player.getLocation()));
        backup(player, true);
    }

    public void backup(Player player) {
        backup(player, false);
    }

    public void backup(Player player, boolean force) {
        if (!isEnabled()) return;
        PlayerInventory inv = player.getInventory();
        ItemStack[] storage = cloneArr(inv.getStorageContents());
        ItemStack[] armor = cloneArr(inv.getArmorContents());
        ItemStack off = inv.getItemInOffHand() == null ? null : inv.getItemInOffHand().clone();
        Backup b = new Backup(System.currentTimeMillis(), storage, armor, off);
        if (b.itemCount() == 0) return;

        List<Backup> list = histories.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        if (!force && !list.isEmpty()) {
            Backup last = list.get(list.size() - 1);
            if (b.itemCount() < Math.max(1, (int) (last.itemCount() * 0.4))) return;
        }
        list.add(b);
        while (list.size() > historySize()) list.remove(0);
        save();
    }

    public String requestClaim(Player player) {
        if (!isEnabled()) return "保险功能未开启";
        List<Backup> hist = histories.get(player.getUniqueId());
        if (hist == null || hist.isEmpty()) return "没有可用备份";
        for (Claim c : claims.values()) {
            if (c.player.equals(player.getUniqueId()) && "PENDING".equals(c.status))
                return "已有待审申请 #" + c.id;
        }
        String id = String.valueOf(nextClaimId++);
        Claim claim = new Claim(id, player.getUniqueId(), player.getName(),
                System.currentTimeMillis(), "PENDING");
        DeathCover cover = lastDeath.get(player.getUniqueId());
        if (cover != null) {
            claim.covered = cover.covered();
            claim.coverKind = cover.kind();
            claim.coverPlace = cover.place();
        }
        claims.put(id, claim);
        save();

        Backup latest = hist.get(hist.size() - 1);
        String coverTxt = cover == null ? "未记录死亡点" : cover.label();
        String msg = com.etherstories.escore.utils.ColorUtil.colorize(
                "&8[保险] &f" + player.getName()
                        + " &7申请背包理赔 &8#" + id
                        + " &7" + coverTxt
                        + " &7历史备份 &f" + hist.size()
                        + " &7份（最新 " + latest.timeLabel()
                        + " / " + latest.itemCount() + "件）"
                        + " &8赔付 " + (int) compensation());
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("es2uni.admin") || p.isOp()) p.sendMessage(msg);
        }
        return null;
    }

    /** 管理批准：backupIndex 为历史列表下标；-1 = 最新。 */
    public String approve(Player admin, String claimId, int backupIndex) {
        if (!admin.hasPermission("es2uni.admin")) return "无权";
        Claim c = claims.get(claimId);
        if (c == null) return "申请不存在";
        if (!"PENDING".equals(c.status)) return "申请已处理";

        List<Backup> hist = histories.get(c.player);
        if (hist == null || hist.isEmpty()) return "备份已丢失";
        int idx = backupIndex < 0 ? hist.size() - 1 : backupIndex;
        if (idx < 0 || idx >= hist.size()) return "备份序号无效";

        Player target = Bukkit.getPlayer(c.player);
        if (target == null || !target.isOnline()) return "玩家不在线，无法恢复";

        Backup b = hist.get(idx);
        restore(target, b);
        double pay = compensation();
        if (pay > 0 && plugin.getVaultHook().isEnabled()) {
            plugin.getVaultHook().deposit(target, pay);
        }
        c.status = "APPROVED";
        c.chosenBackupIndex = idx;
        save();
        target.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                "&8[保险] &a理赔通过：已恢复备份 &f" + b.timeLabel()
                        + " &a并赔偿 &e" + plugin.getVaultHook().format(pay)));
        return null;
    }

    public String deny(Player admin, String claimId) {
        if (!admin.hasPermission("es2uni.admin")) return "无权";
        Claim c = claims.get(claimId);
        if (c == null) return "申请不存在";
        if (!"PENDING".equals(c.status)) return "申请已处理";
        c.status = "DENIED";
        save();
        Player target = Bukkit.getPlayer(c.player);
        if (target != null && target.isOnline()) {
            target.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                    "&8[保险] &c管理员拒绝了你的背包理赔申请 #" + claimId));
        }
        return null;
    }

    private void restore(Player player, Backup b) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        if (b.contents != null) inv.setStorageContents(cloneArr(b.contents));
        if (b.armor != null) inv.setArmorContents(cloneArr(b.armor));
        if (b.offhand != null) inv.setItemInOffHand(b.offhand.clone());
        player.updateInventory();
    }

    private ItemStack[] cloneArr(ItemStack[] src) {
        if (src == null) return new ItemStack[0];
        ItemStack[] out = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) out[i] = src[i] == null ? null : src[i].clone();
        return out;
    }

    private void startAutoBackup() {
        long ticks = plugin.getConfig().getLong("municipal.insurance.auto-backup-minutes", 10) * 60L * 20L;
        if (ticks < 20L * 60) ticks = 20L * 60;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isEnabled()) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                try { backup(p); } catch (Exception ignored) {}
            }
        }, ticks, ticks);
    }

    private void load() {
        histories.clear();
        claims.clear();
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        nextClaimId = cfg.getInt("next-claim-id", 1);

        // 新格式 histories.<uuid>.<i>
        ConfigurationSection hs = cfg.getConfigurationSection("histories");
        if (hs != null) {
            for (String key : hs.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    ConfigurationSection playerSec = hs.getConfigurationSection(key);
                    if (playerSec == null) continue;
                    List<Backup> list = new ArrayList<>();
                    for (String idx : playerSec.getKeys(false)) {
                        ConfigurationSection s = playerSec.getConfigurationSection(idx);
                        if (s == null) continue;
                        list.add(readBackup(s));
                    }
                    if (!list.isEmpty()) histories.put(uuid, list);
                } catch (Exception ex) {
                    plugin.getLogger().warning("insurance histories 跳过 " + key);
                }
            }
        }

        // 旧格式 backups.<uuid> 单份 → 迁入 histories
        ConfigurationSection bs = cfg.getConfigurationSection("backups");
        if (bs != null) {
            for (String key : bs.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    if (histories.containsKey(uuid)) continue;
                    ConfigurationSection s = bs.getConfigurationSection(key);
                    if (s == null) continue;
                    List<Backup> list = new ArrayList<>();
                    list.add(readBackup(s));
                    histories.put(uuid, list);
                } catch (Exception ignored) {}
            }
        }

        ConfigurationSection cs = cfg.getConfigurationSection("claims");
        if (cs != null) {
            for (String id : cs.getKeys(false)) {
                ConfigurationSection s = cs.getConfigurationSection(id);
                if (s == null) continue;
                try {
                    Claim c = new Claim(id, UUID.fromString(s.getString("player")),
                            s.getString("name", "?"), s.getLong("created", 0L),
                            s.getString("status", "PENDING"));
                    c.chosenBackupIndex = s.getInt("chosen-backup", -1);
                    c.covered = s.getBoolean("covered", false);
                    c.coverKind = s.getString("cover-kind", "none");
                    c.coverPlace = s.getString("cover-place", "");
                    claims.put(id, c);
                    try {
                        int n = Integer.parseInt(id);
                        if (n >= nextClaimId) nextClaimId = n + 1;
                    } catch (NumberFormatException ignored) {}
                } catch (Exception ignored) {}
            }
        }
    }

    private Backup readBackup(ConfigurationSection s) {
        List<ItemStack> storage = castItemList(s.getList("storage"));
        List<ItemStack> armor = castItemList(s.getList("armor"));
        return new Backup(s.getLong("time", 0L), listToArr(storage, 36),
                listToArr(armor, 4), s.getItemStack("offhand"));
    }

    private List<ItemStack> castItemList(List<?> raw) {
        List<ItemStack> out = new ArrayList<>();
        if (raw == null) return out;
        for (Object o : raw) out.add(o instanceof ItemStack is ? is : null);
        return out;
    }

    private ItemStack[] listToArr(List<ItemStack> list, int size) {
        ItemStack[] arr = new ItemStack[size];
        if (list == null) return arr;
        for (int i = 0; i < Math.min(size, list.size()); i++) arr[i] = list.get(i);
        return arr;
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("next-claim-id", nextClaimId);
        for (Map.Entry<UUID, List<Backup>> e : histories.entrySet()) {
            List<Backup> list = e.getValue();
            for (int i = 0; i < list.size(); i++) {
                Backup b = list.get(i);
                String p = "histories." + e.getKey() + "." + i + ".";
                cfg.set(p + "time", b.time);
                cfg.set(p + "storage", b.contents);
                cfg.set(p + "armor", b.armor);
                cfg.set(p + "offhand", b.offhand);
            }
        }
        for (Claim c : claims.values()) {
            String p = "claims." + c.id + ".";
            cfg.set(p + "player", c.player.toString());
            cfg.set(p + "name", c.playerName);
            cfg.set(p + "created", c.createdAt);
            cfg.set(p + "status", c.status);
            cfg.set(p + "chosen-backup", c.chosenBackupIndex);
            cfg.set(p + "covered", c.covered);
            cfg.set(p + "cover-kind", c.coverKind);
            cfg.set(p + "cover-place", c.coverPlace);
        }
        try { cfg.save(dataFile); }
        catch (IOException e) { plugin.getLogger().warning("insurance.yml 保存失败: " + e.getMessage()); }
    }
}
