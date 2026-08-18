package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.broadcast.BroadcastEntry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ConfigManager {

    /** 与 config.yml 中 config-version 保持一致，每次新增配置项时 +1 */
    private static final int CURRENT_VERSION = 30;

    private final ES2UniPlugin plugin;
    private YamlConfiguration jarDefaults;

    public ConfigManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        loadJarDefaults();
        migrate();
    }

    public void reload() {
        loadJarDefaults();
        migrate();
    }

    private void loadJarDefaults() {
        try {
            Reader reader = new InputStreamReader(
                    Objects.requireNonNull(plugin.getResource("config.yml")), StandardCharsets.UTF_8);
            jarDefaults = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            jarDefaults = new YamlConfiguration();
            plugin.getLogger().warning("无法读取内置 config.yml: " + e.getMessage());
        }
    }

    /**
     * 启动 / reload：
     * 1. 深度补全新键（不覆盖已有值）
     * 2. 升 config-version
     * 3. 可选同步 commands.help
     * 4. 可选写出 CONFIG.md
     */
    private void migrate() {
        FileConfiguration cfg = plugin.getConfig();
        int fileVersion = cfg.getInt("config-version", 0);
        boolean needSave = false;

        if (fileVersion < 3) {
            double cur = cfg.getDouble("territory.price-per-block", -1);
            if (cur == 0.5) {
                cfg.set("territory.price-per-block", 1.5);
                plugin.getLogger().info("配置迁移 v3: territory.price-per-block 0.5 → 1.5");
                needSave = true;
            }
        }

        // v14: 破产保护改为负债下限
        if (fileVersion < 14) {
            double cur = cfg.getDouble("bankruptcy.reserve", 100);
            if (cur >= 0) {
                cfg.set("bankruptcy.reserve", -500);
                plugin.getLogger().info("配置迁移 v14: bankruptcy.reserve " + cur + " → -500（负债上限）");
                needSave = true;
            }
        }
        // v15: 负债上限统一为 -500；补回收站配置
        if (fileVersion < 15) {
            double cur = cfg.getDouble("bankruptcy.reserve", -500);
            if (cur < -500) {
                cfg.set("bankruptcy.reserve", -500);
                plugin.getLogger().info("配置迁移 v15: bankruptcy.reserve " + cur + " → -500");
                needSave = true;
            }
        }

        // v19: 回声斩 → 无CD魔法绕甲（只覆盖这两段技能键，其它配置不动）
        if (fileVersion < 19 && jarDefaults != null) {
            if (syncSectionFromJar(cfg, jarDefaults, "weapons.skills.echo_scatter")) {
                needSave = true;
                plugin.getLogger().info("配置迁移 v19: weapons.skills.echo_scatter → 无CD魔法散射");
            }
            if (syncSectionFromJar(cfg, jarDefaults, "weapons.skills.echo_barrage")) {
                needSave = true;
                plugin.getLogger().info("配置迁移 v19: weapons.skills.echo_barrage → 无CD魔法制导");
            }
            // 顺带改幸运方块显示名（若仍是旧名）
            String lucky = cfg.getString("lucky-block.display-name", "");
            if ("节拍盲盒".equals(lucky)) {
                cfg.set("lucky-block.display-name", "幸运方块");
                needSave = true;
                plugin.getLogger().info("配置迁移 v19: lucky-block.display-name → 幸运方块");
            }
        }

        // v25: 回声斩伤害上调（旧 1.8/1.7 像挠痒）
        if (fileVersion < 25) {
            cfg.set("weapons.skills.echo_scatter.damage", 4.5);
            cfg.set("weapons.skills.echo_scatter.explode-damage", 2.2);
            cfg.set("weapons.skills.echo_barrage.damage", 4.0);
            cfg.set("weapons.skills.echo_barrage.explode-damage", 1.8);
            plugin.getLogger().info("配置迁移 v25: 回声散射/寻踪伤害上调");
            needSave = true;
        }

        // v28: 散射改为空中展开+逐发制导；两技能再加伤害
        if (fileVersion < 28) {
            cfg.set("weapons.skills.echo_scatter.particle-count", 52);
            cfg.set("weapons.skills.echo_scatter.expand-ticks", 8);
            cfg.set("weapons.skills.echo_scatter.fire-interval-ticks", 1);
            cfg.set("weapons.skills.echo_scatter.fly-ticks", 34);
            cfg.set("weapons.skills.echo_scatter.speed", 2.4);
            cfg.set("weapons.skills.echo_scatter.damage", 6.5);
            cfg.set("weapons.skills.echo_scatter.explode-damage", 3.2);
            cfg.set("weapons.skills.echo_scatter.target-range", 36.0);
            cfg.set("weapons.skills.echo_scatter.blindness-ticks", 30);
            cfg.set("weapons.skills.echo_barrage.damage", 5.5);
            cfg.set("weapons.skills.echo_barrage.explode-damage", 2.6);
            plugin.getLogger().info("配置迁移 v28: 回声散射制导展开 / 两技能伤害上调");
            needSave = true;
        }

        boolean autoFill = cfg.getBoolean("config.auto-fill-missing", true);
        if (autoFill && jarDefaults != null) {
            int added = fillMissingKeys(cfg, jarDefaults, "");
            if (added > 0) {
                needSave = true;
                plugin.getLogger().info("配置自动补全: 新增 " + added + " 个缺失键（已有值未改动）");
            }
            cfg.setDefaults(jarDefaults);
            cfg.options().copyDefaults(true);
        }

        if (fileVersion < CURRENT_VERSION) {
            cfg.set("config-version", CURRENT_VERSION);
            needSave = true;
            plugin.getLogger().info("配置版本 v" + fileVersion + " → v" + CURRENT_VERSION);
        }

        if (cfg.getBoolean("commands.auto-update-help", true) && jarDefaults != null) {
            String jarHelp = jarDefaults.getString("commands.help");
            if (jarHelp != null && !jarHelp.equals(cfg.getString("commands.help", ""))) {
                cfg.set("commands.help", jarHelp);
                needSave = true;
                plugin.getLogger().info("已自动同步 commands.help");
            }
        }

        if (needSave) {
            plugin.saveConfig();
        }

        if (cfg.getBoolean("config.auto-update-docs", true)) {
            syncConfigDocs();
        }
    }

    /** 递归补全 defaults 有、target 无的键；已有键一律跳过。 */
    private int fillMissingKeys(ConfigurationSection target, ConfigurationSection defaults, String path) {
        int added = 0;
        Set<String> keys = defaults.getKeys(false);
        for (String key : keys) {
            String full = path.isEmpty() ? key : path + "." + key;
            if (defaults.isConfigurationSection(key)) {
                ConfigurationSection defChild = defaults.getConfigurationSection(key);
                if (defChild == null) continue;
                if (!target.contains(key)) {
                    ConfigurationSection created = target.createSection(key);
                    added += 1 + fillMissingKeys(created, defChild, full);
                } else if (target.isConfigurationSection(key)) {
                    ConfigurationSection tgtChild = target.getConfigurationSection(key);
                    if (tgtChild != null) {
                        added += fillMissingKeys(tgtChild, defChild, full);
                    }
                }
            } else if (!target.contains(key)) {
                target.set(key, defaults.get(key));
                added++;
            }
        }
        return added;
    }

    /**
     * 把 jar 内某个 section 整段同步到磁盘配置（用于版本迁移强制更新）。
     * 只动该 path 下的键，其它配置不受影响。
     */
    private boolean syncSectionFromJar(FileConfiguration cfg, ConfigurationSection jar,
                                       String path) {
        ConfigurationSection src = jar.getConfigurationSection(path);
        if (src == null) return false;
        boolean changed = false;
        for (String key : src.getKeys(true)) {
            if (src.isConfigurationSection(key)) continue;
            Object nv = src.get(key);
            String full = path + "." + key;
            Object ov = cfg.get(full);
            if (!Objects.equals(ov, nv)) {
                cfg.set(full, nv);
                changed = true;
            }
        }
        return changed;
    }

    private void syncConfigDocs() {
        try (InputStream in = plugin.getResource("CONFIG.md")) {
            if (in == null) return;
            File out = new File(plugin.getDataFolder(), "CONFIG.md");
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("已更新配置说明: " + out.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("同步 CONFIG.md 失败: " + e.getMessage());
        }
    }

    // ── TAB ──────────────────────────────────────────────────────────────────

    public String getTabHeader() {
        return plugin.getConfig().getString("tab.header", "");
    }

    public String getTabFooter() {
        return plugin.getConfig().getString("tab.footer", "");
    }

    public long getTabUpdateInterval() {
        return plugin.getConfig().getLong("tab.update-interval-ticks", 40L);
    }

    public String getTabPlayerNameFormat() {
        return plugin.getConfig().getString("tab.player-name-format", "{player}  &8({playtime})");
    }

    public String getTabAfkNameFormat() {
        return plugin.getConfig().getString("tab.afk-name-format", "&8[AFK] &7{player}  &8({playtime})");
    }

    // ── Join / Quit ──────────────────────────────────────────────────────────

    public String getJoinMessage() {
        return plugin.getConfig().getString("messages.join", "&a{player} &7进入了服务器");
    }

    public String getQuitMessage() {
        return plugin.getConfig().getString("messages.quit", "&c{player} &7离开了服务器");
    }

    public String getFirstJoinMessage() {
        return plugin.getConfig().getString("messages.first-join", "&b&l欢迎来到 EtherStories！");
    }

    public String getWelcomeBackMessage() {
        return plugin.getConfig().getString("messages.welcome-back", "&6&l欢迎回家，&f{player}&6！");
    }

    // ── Broadcast ────────────────────────────────────────────────────────────

    public List<String> getBroadcastMessages() {
        return plugin.getConfig().getStringList("broadcast.messages");
    }

    public List<BroadcastEntry> getBroadcastEntries() {
        List<BroadcastEntry> out = new ArrayList<>();
        for (String raw : getBroadcastMessages()) {
            out.add(BroadcastEntry.parse(raw));
        }
        return out;
    }

    public List<String> getBroadcastMessagesForToday() {
        List<String> out = new ArrayList<>();
        for (BroadcastEntry e : getBroadcastEntries()) {
            if (e.activeToday() && !e.text.isBlank()) out.add(e.text);
        }
        return out;
    }

    public long getBroadcastInterval() {
        return plugin.getConfig().getLong("broadcast.interval-seconds", 300L);
    }

    public String getBroadcastPrefix() {
        return plugin.getConfig().getString("broadcast.prefix", "&b&l[ES2] &r");
    }

    // ── Bankruptcy ───────────────────────────────────────────────────────────

    public boolean isBankruptcyEnabled() {
        return plugin.getConfig().getBoolean("bankruptcy.enabled", true);
    }

    public double getBankruptcyReserve() {
        // 最低余额（可为负）：默认 -5000，到此不能再消费
        return plugin.getConfig().getDouble("bankruptcy.reserve", -500.0);
    }

    public boolean isBankruptcyNotifyAdmins() {
        return plugin.getConfig().getBoolean("bankruptcy.notify-admins", true);
    }

    // ── TPS / MSPT ───────────────────────────────────────────────────────────

    public boolean isTpsAlertEnabled() {
        return plugin.getConfig().getBoolean("tps.alert-enabled", true);
    }

    public double getTpsWarnThreshold() {
        return plugin.getConfig().getDouble("tps.warn-threshold", 17.0);
    }

    public double getTpsCriticalThreshold() {
        return plugin.getConfig().getDouble("tps.critical-threshold", 12.0);
    }

    public long getTpsAlertCooldownTicks() {
        return plugin.getConfig().getLong("tps.alert-cooldown-seconds", 60L) * 20L;
    }

    public String getTpsWarnMessage() {
        return plugin.getConfig().getString("tps.warn-message", "&e⚠ [ES2] TPS low: {tps}");
    }

    public String getTpsCriticalMessage() {
        return plugin.getConfig().getString("tps.critical-message", "&c&l⚠ [ES2] TPS critical: {tps}");
    }

    public String getTpsRecoverMessage() {
        return plugin.getConfig().getString("tps.recover-message", "&a✔ [ES2] Performance recovered.");
    }

    public String getAdminAlertPrefix() {
        return plugin.getConfig().getString("tps.admin-alert-prefix", "&c&l[ES2-ALERT] &r");
    }

    public double getTpsGreenThreshold() {
        return plugin.getConfig().getDouble("tps.color.green-above", 19.0);
    }

    public double getTpsYellowThreshold() {
        return plugin.getConfig().getDouble("tps.color.yellow-above", 15.0);
    }

    public double getMsptGreenThreshold() {
        return plugin.getConfig().getDouble("tps.color.mspt-green-below", 40.0);
    }

    public double getMsptYellowThreshold() {
        return plugin.getConfig().getDouble("tps.color.mspt-yellow-below", 55.0);
    }

    // ── AFK ──────────────────────────────────────────────────────────────────

    public boolean isAfkEnabled() {
        return plugin.getConfig().getBoolean("afk.enabled", true);
    }

    public long getAfkTimeoutMillis() {
        return plugin.getConfig().getLong("afk.timeout-seconds", 300L) * 1000L;
    }

    public String getAfkTabPrefix() {
        return plugin.getConfig().getString("afk.tab-prefix", "&8[AFK] &7");
    }

    public String getAfkEnterMessage() {
        return plugin.getConfig().getString("afk.enter-message", "&8[ES2] &7{player} 进入了 AFK 状态。");
    }

    public String getAfkReturnMessage() {
        return plugin.getConfig().getString("afk.return-message", "&8[ES2] &7{player} 已返回。");
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    public String getHelpMessage() {
        return plugin.getConfig().getString("commands.help", "&7/ecos help");
    }

    public String getVersionDisplayTemplate() {
        return plugin.getConfig().getString("commands.version-display", "&b[ECOS] &fv{version}");
    }

    public String getReloadMessage() {
        return plugin.getConfig().getString("commands.reload-success", "&a[ES2] 配置已重载。");
    }

    public String getNoPermMessage() {
        return plugin.getConfig().getString("commands.no-permission", "&c[ES2] 你没有权限。");
    }

    public String getTpsDisplayTemplate() {
        return plugin.getConfig().getString("commands.tps-display", "&bTPS {tps_1m}");
    }

    // ── Event GUI ────────────────────────────────────────────────────────────

    public String getEventGUITitle() {
        return plugin.getConfig().getString("event-gui.player-title", "&b活动列表");
    }

    public String getAdminEventGUITitle() {
        return plugin.getConfig().getString("event-gui.admin-title", "&c活动管理");
    }

    public String getAnvilEventNamePlaceholder() {
        return plugin.getConfig().getString("event-gui.anvil-placeholder", "输入活动名称");
    }

    // ── Terminal ─────────────────────────────────────────────────────────────

    public String getTerminalTitle() {
        return plugin.getConfig().getString("terminal.title", "&0[ &bECOS &0]");
    }

    public String getHomeGUITitle() {
        return plugin.getConfig().getString("terminal.home-title", "&d家园列表");
    }

    public String getTpaSelectTitle() {
        return plugin.getConfig().getString("terminal.tpa-title", "&6选择传送目标");
    }

    public String getTpaHereSelectTitle() {
        return plugin.getConfig().getString("terminal.tpahere-title", "&6选择召唤目标");
    }

    public String getPaySelectTitle() {
        return plugin.getConfig().getString("terminal.pay-select-title", "&a选择收款玩家");
    }

    public String getPayConfirmTitle() {
        return plugin.getConfig().getString("terminal.pay-confirm-title", "&e确认转账");
    }

    // ── Check-in ─────────────────────────────────────────────────────────────

    public boolean isCheckInEnabled() {
        return plugin.getConfig().getBoolean("checkin.enabled", true);
    }

    public double getCheckInBaseReward() {
        return plugin.getConfig().getDouble("checkin.base-reward", 500.0);
    }

    public double getCheckInStreakBonus() {
        return plugin.getConfig().getDouble("checkin.streak-bonus", 50.0);
    }

    public double getCheckInMaxExtra() {
        return plugin.getConfig().getDouble("checkin.max-extra", 1000.0);
    }

    public double calcCheckInReward(int streak) {
        double base = getCheckInBaseReward();
        double bonus = getCheckInStreakBonus();
        double maxExtra = getCheckInMaxExtra();
        if (streak <= 1) return base;
        return base + Math.min((streak - 1) * bonus, maxExtra);
    }

    // ── Territory ────────────────────────────────────────────────────────────

    public double getTerritoryPricePerBlock() {
        return plugin.getConfig().getDouble("territory.price-per-block", 1.5);
    }

    public double getTerritoryMinCost() {
        return plugin.getConfig().getDouble("territory.min-cost", 500.0);
    }

    public long getTerritoryMinArea() {
        return plugin.getConfig().getLong("territory.min-area", 25L);
    }

    public double getTerritoryRefundRatio() {
        return plugin.getConfig().getDouble("territory.refund-ratio", 0.5);
    }

    /** 单价别名（指令文案用）。 */
    public double getTerritoryCost() {
        return getTerritoryPricePerBlock();
    }

    public double calcTerritoryCost(long area) {
        return Math.max(getTerritoryPricePerBlock() * area, getTerritoryMinCost());
    }
}
