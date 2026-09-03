package com.etherstories.escore.commands;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.auras.AuraType;
import com.etherstories.escore.hooks.AllMusicHook;
import com.etherstories.escore.items.ECOSTerminalItem;
import com.etherstories.escore.items.SkillBinder;
import com.etherstories.escore.items.WeaponPreset;
import com.etherstories.escore.estate.BuildingCategory;
import com.etherstories.escore.estate.EstateUnit;
import com.etherstories.escore.estate.UnitKind;
import com.etherstories.escore.hotel.HotelRoomType;
import com.etherstories.escore.managers.EstateManager;
import com.etherstories.escore.managers.HotelManager;
import com.etherstories.escore.managers.PlaytimeManager;
import com.etherstories.escore.managers.RegionManager;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.managers.WaypointManager;
import com.etherstories.escore.managers.WeaponSaveManager;
import com.etherstories.escore.weapons.PlayerSkills;
import com.etherstories.escore.weapons.SkillType;
import com.etherstories.escore.tasks.TPSMonitorTask;
import com.etherstories.escore.utils.ColorUtil;
import com.etherstories.escore.utils.TPSUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ES2UniCommand implements CommandExecutor, TabCompleter {

    private final ES2UniPlugin    plugin;
    // Last whisper target per sender (for /ecos r)
    private final Map<UUID, UUID> lastWhisperTarget = new HashMap<>();
    // Selection particle tasks & display positions (X,Y,Z)
    private final Map<UUID, BukkitRunnable> selectionTasks = new HashMap<>();
    private final Map<UUID, int[]>          displayPos1    = new HashMap<>();
    private final Map<UUID, int[]>          displayPos2    = new HashMap<>();

    public ES2UniCommand(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void recordWhisper(UUID sender, UUID target) {
        lastWhisperTarget.put(sender, target);
        lastWhisperTarget.put(target, sender); // target can reply back
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            return runCommand(sender, args);
        } catch (Throwable t) {
            plugin.getLogger().warning("/ecos 失败: " + t);
            sender.sendMessage(ColorUtil.colorize("&8[ECOS] &c指令失败: " + t.getClass().getSimpleName()));
            return true;
        }
    }

    private boolean runCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            try {
                if (sender instanceof Player p) plugin.getEcosTerminalGUI().open(p);
                else sendHelp(sender);
            } catch (Throwable t) {
                plugin.getLogger().warning(" /ecos 打开失败: " + t);
                sender.sendMessage(ColorUtil.colorize("&8[ECOS] &c打开失败"));
            }
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                plugin.reload();
                if (sender instanceof Player p)
                    plugin.getAuditLogManager().log(p, "RELOAD", "command");
                else
                    plugin.getAuditLogManager().logRaw("CONSOLE", "-", "RELOAD", "command");
                sender.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getReloadMessage()));
            }

            case "audit" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                if (!plugin.getAuditLogManager().isEnabled()) {
                    sender.sendMessage(ColorUtil.colorize(
                            "&8[ECOS] &7审计日志已关闭（不占磁盘）。需要时在 config.yml 设 &faudit.enabled: true"));
                    return true;
                }
                int limit = 20;
                if (args.length >= 2) {
                    try { limit = Math.min(100, Math.max(1, Integer.parseInt(args[1]))); }
                    catch (NumberFormatException ignored) {}
                }
                List<String> lines = plugin.getAuditLogManager().readToday(limit);
                if (lines.isEmpty()) lines = plugin.getAuditLogManager().getRecent(limit);
                sender.sendMessage(ColorUtil.colorize("&8[ECOS] &f审计日志 &7(最近 " + lines.size() + " 条)"));
                if (lines.isEmpty()) {
                    sender.sendMessage(ColorUtil.colorize("&7暂无记录。文件: plugins/ES2UniPlugin/audit/"));
                } else {
                    for (String line : lines)
                        sender.sendMessage(ColorUtil.colorize("&8" + line));
                }
            }

            case "tradereport", "taxreport" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                if (args.length >= 2 && args[1].equalsIgnoreCase("broadcast")) {
                    plugin.getTradeStatsManager().broadcastReport("手动");
                    plugin.getAuditLogManager().log(
                            sender instanceof Player p ? p : null, "TRADE_REPORT", "broadcast");
                } else if (sender instanceof Player p) {
                    plugin.getTradeStatsManager().sendPersonalReport(p);
                } else {
                    plugin.getTradeStatsManager().broadcastReport("控制台");
                }
            }

            case "tps" -> {
                double[] all = TPSUtil.getAllTPS();
                sender.sendMessage(ColorUtil.colorize(
                        plugin.getConfigManager().getTpsDisplayTemplate()
                                .replace("{tps_1m}",  TPSUtil.formatTPS(all[0]))
                                .replace("{tps_5m}",  TPSUtil.formatTPS(all[1]))
                                .replace("{tps_15m}", TPSUtil.formatTPS(all[2]))
                                .replace("{mspt}",    TPSUtil.formatMSPT(TPSUtil.getMSPT()))
                ));
            }

            case "version" -> {
                sender.sendMessage(ColorUtil.colorize(
                        plugin.getConfigManager().getVersionDisplayTemplate()
                                .replace("{version}",    plugin.getDescription().getVersion())
                                .replace("{mc_version}", plugin.getServer().getVersion())
                                .replace("{authors}",    String.join(", ", plugin.getDescription().getAuthors()))
                ));
            }

            case "broadcast" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                if (args.length < 2) { sender.sendMessage(ColorUtil.colorize("&c用法: /ecos broadcast <消息>")); return true; }
                String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                String full = plugin.getConfigManager().getBroadcastPrefix() + msg;
                Bukkit.broadcastMessage(ColorUtil.colorize(full));
                plugin.getNoticeManager().recordBroadcast(msg);
            }

            case "event" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleEvent(p, args);
            }

            case "web" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleWeb(p, args);
            }

            case "t", "_t" -> {
                if (!(sender instanceof Player p)) return true;
                if (args.length < 2) return true;
                String raw = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "";
                var hub = plugin.getTerminalHub();
                if (hub == null) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &c终端未就绪"));
                    return true;
                }
                hub.click(p, args[1], raw);
            }

            case "menu", "gui" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                plugin.getEcosTerminalGUI().open(p);
            }

            case "give" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                p.getInventory().addItem(ECOSTerminalItem.create());
                sender.sendMessage(ColorUtil.colorize("&8[ECOS] &7已获得 ECOS 终端物品（管理）"));
            }

            case "pve" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                p.getInventory().addItem(com.etherstories.escore.items.PveItems.pack(),
                        com.etherstories.escore.items.PveItems.elite());
                sender.sendMessage(ColorUtil.colorize("&8[ECOS] &7已给刷怪棒 / 精英棒  &8右键刷、潜行换种类"));
            }

            case "claim" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleTerminalClaim(p);
            }

            case "tpsalert" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                TPSMonitorTask task = plugin.getTpsMonitorTask();
                boolean nowMuted = args.length >= 2 ? args[1].equalsIgnoreCase("off") : !task.isMuted();
                task.setMuted(nowMuted);
                sender.sendMessage(ColorUtil.colorize("&8[ECOS] &7TPS 告警已" + (nowMuted ? "关闭" : "开启")));
            }

            // ── 光环 / 粒子特效（particle = aura 别名）────────────────────────
            case "aura", "particle", "particles" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleAura(p, args);
            }

            // ── 武器技能绑定（管理员） ─────────────────────────────────────────
            case "weapon" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleWeapon(p, args);
            }

            case "skill" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                handleSkill(sender, args);
            }

            case "skillshop", "skill-shop" -> handleSkillShop(sender, args);

            case "admin" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                if (args.length < 2 || !args[1].equalsIgnoreCase("event")) {
                    sender.sendMessage(ColorUtil.colorize("&c用法: /ecos admin event")); return true;
                }
                plugin.getAdminEventGUI().open(p);
            }

            // ── 私信回复 ──────────────────────────────────────────────────────
            case "r", "reply" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                UUID target = lastWhisperTarget.get(p.getUniqueId());
                if (target == null) { p.sendMessage(ColorUtil.colorize("&8[ECOS] &7没有可回复的私信")); return true; }
                Player tPlayer = Bukkit.getPlayer(target);
                if (tPlayer == null) { p.sendMessage(ColorUtil.colorize("&8[ECOS] &7对方已离线")); return true; }
                if (args.length < 2) { p.sendMessage(ColorUtil.colorize("&8[ECOS] &7用法: /ecos r <内容>")); return true; }
                String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                String out = "&8[私信] &f" + p.getName() + " &8→ &f" + tPlayer.getName() + ": &7" + msg;
                p.sendMessage(ColorUtil.colorize(out));
                tPlayer.sendMessage(ColorUtil.colorize(out));
                recordWhisper(p.getUniqueId(), target);
            }

            // ── 地区（管理员） ─────────────────────────────────────────────────
            case "region" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleRegion(p, args);
            }

            // ── 领地（玩家，需经济） ───────────────────────────────────────────
            case "territory" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleTerritory(p, args);
            }

            case "hotel" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleHotel(p, args);
            }

            case "estate" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleEstate(p, args);
            }

            // ── 坐标收藏 ──────────────────────────────────────────────────────
            case "waypoint", "wp" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleWaypoint(p, args);
            }

            // ── 个人签名 ──────────────────────────────────────────────────────
            case "status" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                if (args.length < 2) {
                    String cur = plugin.getStatusManager().getStatus(p.getUniqueId());
                    p.sendMessage(ColorUtil.colorize(cur != null
                            ? "&8[ECOS] &7当前签名: &f" + cur
                            : "&8[ECOS] &7未设置签名"));
                    return true;
                }
                String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                plugin.getStatusManager().setStatus(p.getUniqueId(), text);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7签名已更新: &f" + text));
            }

            case "friends", "friend" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleFriend(p, args);
            }

            case "mail", "mailbox" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleMail(p, args);
            }

            case "online" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleOnline(p, args);
            }

            case "notice", "notices" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleNotice(p, args);
            }

            case "leaderboard", "lb", "top" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleLeaderboard(p, args);
            }

            case "checkin" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("ticket")) {
                    if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                    if (args.length < 4) {
                        sender.sendMessage(ColorUtil.colorize("&c用法: /ecos checkin ticket <玩家> <数量>"));
                        return true;
                    }
                    OfflinePlayer t = resolvePlayed(args[2]);
                    if (t == null) { sender.sendMessage(ColorUtil.colorize("&c找不到玩家（需进过服）")); return true; }
                    int n;
                    try { n = Integer.parseInt(args[3]); } catch (NumberFormatException e) {
                        sender.sendMessage(ColorUtil.colorize("&c数量无效")); return true;
                    }
                    plugin.getCheckInManager().giveMakeupTickets(t.getUniqueId(), n);
                    String name = t.getName() != null ? t.getName() : args[2];
                    sender.sendMessage(ColorUtil.colorize("&8[ECOS] &7已给 &f" + name
                            + " &7补签券 &e" + n + " &8(现有 "
                            + plugin.getCheckInManager().getMakeupTickets(t.getUniqueId()) + ")"));
                    if (t.isOnline() && t.getPlayer() != null)
                        t.getPlayer().sendMessage(ColorUtil.colorize("&8[ECOS] &a获得补签券 x" + n + " &7（签到日历点红日使用）"));
                    return true;
                }
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                if (args.length >= 2 && args[1].equalsIgnoreCase("gui")) {
                    plugin.getCheckInCalendarGUI().open(p);
                    return true;
                }
                p.sendMessage(ColorUtil.colorize(plugin.getCheckInManager().checkInWithReward(p)));
            }

            case "showcase", "machine" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                plugin.getShowcaseGUI().open(p);
            }

            case "lucky" -> {
                if (args.length >= 3 && args[1].equalsIgnoreCase("ticket")) {
                    if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                    OfflinePlayer t = resolvePlayed(args[2]);
                    if (t == null) { sender.sendMessage(ColorUtil.colorize("&7找不到玩家（需进过服）")); return true; }
                    int n = 1;
                    if (args.length >= 4) {
                        try { n = Integer.parseInt(args[3]); } catch (NumberFormatException e) {
                            sender.sendMessage(ColorUtil.colorize("&7数量无效")); return true;
                        }
                    }
                    plugin.getLuckyBlockManager().giveTickets(t.getUniqueId(), n);
                    String name = t.getName() != null ? t.getName() : args[2];
                    sender.sendMessage(ColorUtil.colorize("&8[ECOS] &7已给 &f" + name
                            + " &7额外抽奖 &a+" + n + " &8(现有 "
                            + plugin.getLuckyBlockManager().getTickets(t.getUniqueId()) + ")"));
                    if (t.isOnline() && t.getPlayer() != null)
                        t.getPlayer().sendMessage(ColorUtil.colorize("&8[ECOS] &a获得 "
                                + plugin.getLuckyBlockManager().displayName() + " 额外次数 &f+" + n));
                    return true;
                }
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                plugin.getLuckyBlockManager().claimAnimated(p, msg ->
                        p.sendMessage(ColorUtil.colorize(msg)));
            }

            case "guide", "newbie" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                plugin.getNewbieGuideGUI().open(p);
            }

            case "municipal", "muni", "city" -> handleMunicipal(sender, args);

            case "transit", "rail", "metro" -> handleTransit(sender, args);

            case "reports", "report" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                List<String> lines = plugin.getReportManager().recent(20);
                sender.sendMessage(ColorUtil.colorize("&8[ECOS] &f悄悄话 &7(最近 " + lines.size() + " 条)"));
                if (lines.isEmpty()) sender.sendMessage(ColorUtil.colorize("&7暂无"));
                else for (String line : lines) sender.sendMessage(ColorUtil.colorize("&8" + line));
            }

            case "death", "deaths" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                var deaths = plugin.getDeathManager().get(p.getUniqueId());
                if (deaths.isEmpty()) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7暂无死亡记录"));
                } else {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7死亡记录:"));
                    for (var d : deaths)
                        p.sendMessage(ColorUtil.colorize("  &8" + d.date() + "  &7"
                                + d.world() + " (" + d.x() + "," + d.y() + "," + d.z()
                                + ")  &8" + d.cause()));
                }
            }

            case "afk" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                plugin.getAfkManager().manualToggle(p);
            }

            case "kit" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleKit(p, args);
            }

            case "kitadmin", "kits" -> {
                if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return true; }
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                plugin.getAdminKitGUI().open(p);
            }

            case "music" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return true; }
                handleMusic(p, args);
            }

            case "help" -> sendHelp(sender);

            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleFriend(Player p, String[] args) {
        var fm = plugin.getFriendManager();
        if (args.length < 2) {
            plugin.getFriendListGUI().open(p);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "gui", "menu" -> plugin.getFriendListGUI().open(p);
            case "list", "ls" -> {
                var list = fm.getFriends(p.getUniqueId());
                if (list.isEmpty()) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7好友列表为空"));
                    return;
                }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &b好友列表 &8(" + list.size() + ")"));
                for (UUID id : list) {
                    Player online = Bukkit.getPlayer(id);
                    String name = fm.getDisplayName(id);
                    p.sendMessage(ColorUtil.colorize("  " + (online != null ? "&a● " : "&7○ ")
                            + "&f" + name));
                }
            }
            case "add", "request" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos friend add <玩家>"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7玩家不在线"));
                    return;
                }
                if (!fm.sendRequest(p, target)) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7无法发送：已是好友或请求已存在/不能加自己"));
                    return;
                }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已向 &f" + target.getName() + " &7发送好友请求"));
                target.sendMessage(ColorUtil.colorize("&8[ECOS] &f" + p.getName()
                        + " &7请求加好友  &a/ecos friend accept " + p.getName()
                        + "  &c/ecos friend deny " + p.getName()));
            }
            case "remove", "del", "rm" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos friend remove <玩家>"));
                    return;
                }
                UUID target = resolveFriendUuid(p, args[2]);
                if (target == null) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7未找到该好友"));
                    return;
                }
                String name = fm.getDisplayName(target);
                fm.removeFriend(p.getUniqueId(), target);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已删除好友 &f" + name));
            }
            case "accept" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos friend accept <玩家>"));
                    return;
                }
                UUID sender = resolvePendingSender(p, args[2]);
                if (sender == null || !fm.acceptRequest(p.getUniqueId(), sender)) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7没有来自该玩家的好友请求"));
                    return;
                }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已接受 &f" + fm.getDisplayName(sender)));
                Player online = Bukkit.getPlayer(sender);
                if (online != null) {
                    online.sendMessage(ColorUtil.colorize("&8[ECOS] &f" + p.getName() + " &7接受了你的好友请求"));
                }
            }
            case "deny", "reject" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos friend deny <玩家>"));
                    return;
                }
                UUID sender = resolvePendingSender(p, args[2]);
                if (sender == null) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7没有来自该玩家的好友请求"));
                    return;
                }
                fm.denyRequest(p.getUniqueId(), sender);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已拒绝 &f" + fm.getDisplayName(sender)));
            }
            case "requests", "pending" -> {
                var reqs = fm.getPendingRequests(p.getUniqueId());
                if (reqs.isEmpty()) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7没有待处理好友请求"));
                    return;
                }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &e待处理请求:"));
                for (UUID id : reqs) {
                    p.sendMessage(ColorUtil.colorize("  &f" + fm.getDisplayName(id)
                            + "  &a/ecos friend accept " + fm.getDisplayName(id)));
                }
            }
            case "share", "location" -> {
                boolean on = fm.toggleLocationSharing(p.getUniqueId());
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7位置共享已" + (on ? "&a开启" : "&c关闭")));
            }
            case "help" -> p.sendMessage(ColorUtil.colorize(
                    "&8[ECOS] &bfriend\n"
                            + " &7/ecos friend [gui]          &f开界面\n"
                            + " &7/ecos friend list           &f列表\n"
                            + " &7/ecos friend add <玩家>     &f申请\n"
                            + " &7/ecos friend remove <玩家>  &f删除\n"
                            + " &7/ecos friend accept|deny <玩家>\n"
                            + " &7/ecos friend requests       &f待处理\n"
                            + " &7/ecos friend share          &f位置共享"));
            default -> p.sendMessage(ColorUtil.colorize("&c未知子命令，见 /ecos friend help"));
        }
    }

    private UUID resolveFriendUuid(Player p, String name) {
        for (UUID id : plugin.getFriendManager().getFriends(p.getUniqueId())) {
            if (plugin.getFriendManager().getDisplayName(id).equalsIgnoreCase(name)) return id;
        }
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online.getUniqueId() : null;
    }

    private UUID resolvePendingSender(Player p, String name) {
        for (UUID id : plugin.getFriendManager().getPendingRequests(p.getUniqueId())) {
            if (plugin.getFriendManager().getDisplayName(id).equalsIgnoreCase(name)) return id;
        }
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online.getUniqueId() : null;
    }

    private void handleMail(Player p, String[] args) {
        var mm = plugin.getMailManager();
        if (args.length < 2) {
            plugin.getMailboxGUI().open(p);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "gui", "menu" -> plugin.getMailboxGUI().open(p);
            case "list", "ls" -> {
                var mails = mm.getMails(p.getUniqueId());
                if (mails.isEmpty()) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7邮箱为空"));
                    return;
                }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &b邮箱 &8(未读 " + mm.unreadCount(p.getUniqueId()) + ")"));
                for (int i = 0; i < mails.size(); i++) {
                    var m = mails.get(i);
                    p.sendMessage(ColorUtil.colorize("  &f#" + i + " "
                            + (m.read() ? "&7" : "&a") + m.senderName()
                            + " &8" + m.date() + " &7" + truncate(m.content(), 40)));
                }
            }
            case "read" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos mail read <序号>"));
                    return;
                }
                int idx;
                try { idx = Integer.parseInt(args[2]); }
                catch (NumberFormatException e) {
                    p.sendMessage(ColorUtil.colorize("&c序号必须是数字"));
                    return;
                }
                var mails = mm.getMails(p.getUniqueId());
                if (idx < 0 || idx >= mails.size()) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7无效序号"));
                    return;
                }
                var m = mails.get(idx);
                mm.markRead(p.getUniqueId(), idx);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &b来自 &f" + m.senderName() + " &8" + m.date()));
                p.sendMessage(ColorUtil.colorize("&7" + m.content()));
            }
            case "send" -> {
                if (args.length < 4) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos mail send <玩家> <内容>"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                OfflinePlayer offline = target != null ? target : Bukkit.getOfflinePlayer(args[2]);
                if (offline.getName() == null && !offline.hasPlayedBefore() && !offline.isOnline()) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7找不到玩家（对方需进过服）"));
                    return;
                }
                String content = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                mm.deliver(p.getUniqueId(), p.getName(), offline.getUniqueId(), content);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7邮件已发送给 &f"
                        + (offline.getName() != null ? offline.getName() : args[2])));
                if (target != null) {
                    target.sendMessage(ColorUtil.colorize("&8[ECOS] &7你收到来自 &f" + p.getName() + " &7的邮件"));
                }
            }
            case "delete", "del", "rm" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos mail delete <序号>"));
                    return;
                }
                int idx;
                try { idx = Integer.parseInt(args[2]); }
                catch (NumberFormatException e) {
                    p.sendMessage(ColorUtil.colorize("&c序号必须是数字"));
                    return;
                }
                mm.delete(p.getUniqueId(), idx);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已删除 #" + idx));
            }
            case "help" -> p.sendMessage(ColorUtil.colorize(
                    "&8[ECOS] &bmail\n"
                            + " &7/ecos mail [gui]              &f开界面\n"
                            + " &7/ecos mail list               &f列表\n"
                            + " &7/ecos mail read <序号>         &f阅读\n"
                            + " &7/ecos mail send <玩家> <内容>\n"
                            + " &7/ecos mail delete <序号>"));
            default -> p.sendMessage(ColorUtil.colorize("&c未知子命令，见 /ecos mail help"));
        }
    }

    private void handleOnline(Player p, String[] args) {
        if (args.length >= 2) {
            switch (args[1].toLowerCase()) {
                case "gui", "menu" -> {
                    plugin.getOnlineListGUI().open(p);
                    return;
                }
                case "help" -> {
                    p.sendMessage(ColorUtil.colorize("&7/ecos online [list|gui]"));
                    return;
                }
                case "list", "ls" -> { /* fall through to chat list */ }
                default -> {
                    plugin.getOnlineListGUI().open(p);
                    return;
                }
            }
        }
        var online = Bukkit.getOnlinePlayers();
        p.sendMessage(ColorUtil.colorize("&8[ECOS] &a在线 &f" + online.size()
                + " &7/ &f" + Bukkit.getMaxPlayers()));
        StringJoiner sj = new StringJoiner("&8, &f", "&f", "");
        for (Player o : online) sj.add(o.getName());
        p.sendMessage(ColorUtil.colorize(sj.toString()));
        p.sendMessage(ColorUtil.colorize("&8开界面: &7/ecos online gui"));
    }

    private void handleNotice(Player p, String[] args) {
        if (args.length < 2) {
            plugin.getNoticeGUI().open(p);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "gui", "menu" -> plugin.getNoticeGUI().open(p);
            case "list", "ls" -> {
                var notices = plugin.getNoticeManager().getNotices();
                if (notices.isEmpty()) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7暂无公告"));
                    return;
                }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &b公告板"));
                int i = 0;
                for (var n : notices) {
                    p.sendMessage(ColorUtil.colorize("  &f#" + (i++) + " &8[" + n.date() + "] &7"
                            + n.author() + ": &f" + truncate(n.content(), 50)));
                }
            }
            case "help" -> p.sendMessage(ColorUtil.colorize("&7/ecos notice [gui|list]"));
            default -> plugin.getNoticeGUI().open(p);
        }
    }

    private void handleLeaderboard(Player p, String[] args) {
        if (args.length >= 2) {
            switch (args[1].toLowerCase()) {
                case "gui", "menu" -> {
                    plugin.getLeaderboardGUI().open(p);
                    return;
                }
                case "help" -> {
                    p.sendMessage(ColorUtil.colorize("&7/ecos leaderboard [list|gui]"));
                    return;
                }
                case "list", "ls", "top" -> { /* chat */ }
                default -> {
                    plugin.getLeaderboardGUI().open(p);
                    return;
                }
            }
        }
        var top = plugin.getPlaytimeManager().getTopPlayers(10);
        p.sendMessage(ColorUtil.colorize("&8[ECOS] &e在线时长排行 TOP10"));
        int rank = 1;
        for (var e : top) {
            String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
            p.sendMessage(ColorUtil.colorize("  &f#" + (rank++) + " &7"
                    + (name != null ? name : e.getKey().toString().substring(0, 8))
                    + " &8— &f" + PlaytimeManager.formatMillis(e.getValue())));
        }
        p.sendMessage(ColorUtil.colorize("&8开界面: &7/ecos leaderboard gui"));
    }

    private void handleEvent(Player p, String[] args) {
        if (args.length < 2) {
            plugin.getEventListGUI().open(p);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "gui", "menu" -> plugin.getEventListGUI().open(p);
            case "list", "ls" -> {
                var events = plugin.getEventManager().getAllEvents();
                if (events.isEmpty()) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7暂无活动"));
                    return;
                }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &b活动列表"));
                for (var ev : events) {
                    p.sendMessage(ColorUtil.colorize("  &f" + ev.getId() + " &7" + ev.getName()
                            + " &8by " + ev.getCreator()
                            + " &7(" + ev.getParticipants().size()
                            + (ev.getMaxParticipants() > 0 ? "/" + ev.getMaxParticipants() : "") + ")"));
                }
            }
            case "join" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos event join <id>"));
                    return;
                }
                boolean ok = plugin.getEventManager().join(args[2], p.getUniqueId());
                p.sendMessage(ColorUtil.colorize(ok ? "&8[ECOS] &7已加入活动" : "&8[ECOS] &c无法加入（满员或不存在）"));
            }
            case "leave" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos event leave <id>"));
                    return;
                }
                boolean ok = plugin.getEventManager().leave(args[2], p.getUniqueId());
                p.sendMessage(ColorUtil.colorize(ok ? "&8[ECOS] &7已退出活动" : "&8[ECOS] &7未在该活动中"));
            }
            case "help" -> p.sendMessage(ColorUtil.colorize(
                    "&7/ecos event [gui|list|join <id>|leave <id>]"));
            default -> plugin.getEventListGUI().open(p);
        }
    }

    private void handleMusic(Player p, String[] args) {
        plugin.getAllMusicHook().ensureHooked(plugin.getLogger());
        if (!plugin.getAllMusicHook().isAvailable()) {
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &cAllMusic 未加载"));
            return;
        }
        if (args.length < 2) {
            plugin.getMusicMenuGUI().open(p);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "gui", "menu" -> plugin.getMusicMenuGUI().open(p);
            case "search", "s" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos music search <歌名>"));
                    return;
                }
                String q = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7搜索: &f" + q));
                plugin.getAllMusicHook().search(plugin, p, q);
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> plugin.getMusicSearchResultGUI().open(p), 25L);
            }
            case "add", "play", "id" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos music add <歌曲ID> 或 /ecos music add <api> <ID>"));
                    return;
                }
                if (args.length >= 4) {
                    plugin.getAllMusicHook().addById(plugin, p, args[2], args[3]);
                    plugin.getMusicHistoryManager().recordManual(args[3], args[3], "", p.getName());
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已点歌 ID &f" + args[3] + " &8via &7" + args[2]));
                } else {
                    plugin.getAllMusicHook().addById(plugin, p, args[2]);
                    plugin.getMusicHistoryManager().recordManual(args[2], args[2], "", p.getName());
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已点歌 ID &f" + args[2]));
                }
            }
            case "select" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos music select <序号>"));
                    return;
                }
                int idx;
                try { idx = Integer.parseInt(args[2]); }
                catch (NumberFormatException e) {
                    p.sendMessage(ColorUtil.colorize("&c序号必须是数字"));
                    return;
                }
                plugin.getAllMusicHook().select(plugin, p, idx);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已选择搜索结果 #" + idx));
            }
            case "queue", "list" -> {
                AllMusicHook.NowPlaying now = plugin.getAllMusicHook().getNowPlaying();
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &b播放序列"));
                if (now != null) {
                    p.sendMessage(ColorUtil.colorize(" &e▶ 正在播 &f" + now.name()
                            + " &8- &7" + now.author()
                            + (now.caller().isEmpty() ? "" : " &8by &7" + now.caller())
                            + " &8(队列 " + now.queueSize() + ")"));
                }
                var queue = plugin.getAllMusicHook().getQueue();
                if (queue.isEmpty()) {
                    p.sendMessage(ColorUtil.colorize(" &7后续队列为空"));
                } else {
                    for (var q : queue) {
                        p.sendMessage(ColorUtil.colorize(" &f#" + q.index() + " &7" + q.name()
                                + " &8- &7" + q.author()
                                + (q.caller().isEmpty() ? "" : " &8by &7" + q.caller())));
                    }
                }
            }
            case "now", "np", "playing" -> {
                AllMusicHook.NowPlaying now = plugin.getAllMusicHook().getNowPlaying();
                if (now == null) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7无法读取播放信息"));
                    return;
                }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &e正在播放\n"
                        + " &f" + now.name() + "\n"
                        + " &7歌手 &f" + now.author() + "\n"
                        + " &7专辑 &f" + now.album() + "\n"
                        + " &7点歌 &f" + now.caller() + "\n"
                        + " &7ID &f" + (now.id().isEmpty() ? "—" : now.id()) + "\n"
                        + " &7队列 &f" + now.queueSize() + " 首"));
            }
            case "history", "hist", "log" -> plugin.getMusicHistoryGUI().open(p);
            case "fav", "favorite", "favorites" -> plugin.getMusicFavoritesGUI().open(p);
            case "preset", "presets", "playlist" -> {
                if (!p.hasPermission("es2uni.admin")) { noPerms(p); return; }
                if (args.length >= 3 && args[2].equalsIgnoreCase("stop")) {
                    plugin.getMusicPlaylistManager().stop(true);
                    return;
                }
                plugin.getMusicPlaylistGUI().open(p);
            }
            case "vote" -> {
                plugin.getAllMusicHook().vote(plugin, p);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已投票切歌"));
            }
            case "stop" -> {
                plugin.getAllMusicHook().stop(plugin, p);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已请求停止播放（仅自己）"));
            }
            case "mute" -> {
                boolean muted = plugin.getAllMusicHook().isMuted(p);
                plugin.getAllMusicHook().setListening(plugin, p, muted);
                boolean nowMuted = !muted;
                p.sendMessage(ColorUtil.colorize(nowMuted
                        ? "&8[ECOS] &c听歌已关闭（静音），重进仍有效"
                        : "&8[ECOS] &a听歌已开启"));
            }
            case "join" -> {
                plugin.getAllMusicHook().join(plugin, p);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已请求重新加入播放"));
            }
            case "cancel" -> {
                plugin.getAllMusicHook().cancel(plugin, p);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已请求取消自己点的歌"));
            }
            case "help" -> p.sendMessage(ColorUtil.colorize(
                    "&8[ECOS] &bmusic\n"
                            + " &7/ecos music [gui]                 &f菜单\n"
                            + " &7/ecos music search <歌名>         &f搜歌\n"
                            + " &7/ecos music add <ID>              &fID点歌\n"
                            + " &7/ecos music select <序号>         &f选搜索结果\n"
                            + " &7/ecos music queue|list|now        &f队列/正在播\n"
                            + " &7/ecos music history               &f最近播放 GUI\n"
                            + " &7/ecos music fav                   &f收藏列表\n"
                            + " &7/ecos music preset                &f管理员预设歌单\n"
                            + " &7/ecos music queue / now\n"
                            + " &7/ecos music vote|stop|mute|join|cancel"));
            default -> p.sendMessage(ColorUtil.colorize("&c未知子命令，见 /ecos music help"));
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private void handleKit(Player p, String[] args) {
        if (args.length < 2) {
            plugin.getKitListGUI().open(p);
            return;
        }
        if (args[1].equalsIgnoreCase("list")) {
            plugin.getKitListGUI().open(p);
            return;
        }
        String name = args[1];
        // Essentials 优先
        if (plugin.getEssentialsHook().isEnabled() && plugin.getEssentialsHook().hasKit(name)) {
            String err = plugin.getEssentialsHook().giveKit(p, name);
            p.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[ECOS] &7已领取 Essentials 套件 &f" + name
                    : "&8[ECOS] &c" + err));
            return;
        }
        if (plugin.getKitManager().exists(name)) {
            String err = plugin.getKitManager().claim(p, name);
            p.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[ECOS] &7已领取套件 &f" + name
                    : "&8[ECOS] &c" + err));
            return;
        }
        p.sendMessage(ColorUtil.colorize("&8[ECOS] &7未知套件 &f" + name + " &8— /ecos kit 查看列表"));
    }

    // ── Region subcommand handler ──────────────────────────────────────────────

    private void handleRegion(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos region <pos1|pos2|create|delete|list>")); return; }
        RegionManager rm = plugin.getRegionManager();

        switch (args[1].toLowerCase()) {
            case "pos1" -> {
                rm.setPos1(p.getUniqueId(), p.getWorld().getName(), p.getLocation().getBlockX(), p.getLocation().getBlockZ());
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7地区点1已设置: &f" + p.getLocation().getBlockX() + ", " + p.getLocation().getBlockZ()));
                displayPos1.put(p.getUniqueId(), new int[]{p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ()});
                displayPos2.remove(p.getUniqueId());
                startSelectionParticles(p);
            }
            case "pos2" -> {
                rm.setPos2(p.getUniqueId(), p.getWorld().getName(), p.getLocation().getBlockX(), p.getLocation().getBlockZ());
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7地区点2已设置: &f" + p.getLocation().getBlockX() + ", " + p.getLocation().getBlockZ()));
                displayPos2.put(p.getUniqueId(), new int[]{p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ()});
                startSelectionParticles(p);
                String info = rm.getSelectionInfo(p.getUniqueId());
                if (info != null) p.sendMessage(ColorUtil.colorize("&8选区: &7" + info));
            }
            case "create" -> {
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos region create <名称>")); return; }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (!rm.hasSelection(p.getUniqueId())) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7请先设置 pos1 和 pos2")); return;
                }
                rm.createDistrict(p.getUniqueId(), name);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7地区 &f" + name + " &7已创建"));
            }
            case "delete" -> {
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos region delete <名称>")); return; }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                boolean ok = rm.deleteByName(name, p.getUniqueId(), true);
                p.sendMessage(ColorUtil.colorize(ok ? "&8[ECOS] &7地区已删除" : "&8[ECOS] &7未找到该地区"));
            }
            case "list" -> {
                List<RegionManager.Region> districts = rm.getDistricts();
                if (districts.isEmpty()) { p.sendMessage(ColorUtil.colorize("&8[ECOS] &7暂无地区")); return; }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7地区列表:"));
                for (RegionManager.Region r : districts)
                    p.sendMessage(ColorUtil.colorize("  &f" + r.name() + " &8" + r.world() + " (" + r.x1() + "," + r.z1() + ")→(" + r.x2() + "," + r.z2() + ")"));
            }
            default -> p.sendMessage(ColorUtil.colorize("&c用法: /ecos region <pos1|pos2|create|delete|list>"));
        }
    }

    // ── Territory subcommand handler ───────────────────────────────────────────

    private void handleTerritory(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos territory <pos1|pos2|claim|delete|list|visible|spawn|help>")); return; }
        RegionManager rm = plugin.getRegionManager();
        double cost = plugin.getConfigManager().getTerritoryCost();

        switch (args[1].toLowerCase()) {
            case "help" -> openTerritoryHelp(p);
            case "pos1" -> {
                rm.setPos1(p.getUniqueId(), p.getWorld().getName(), p.getLocation().getBlockX(), p.getLocation().getBlockZ());
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7领地点1已设置: &f" + p.getLocation().getBlockX() + ", " + p.getLocation().getBlockZ()));
                displayPos1.put(p.getUniqueId(), new int[]{p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ()});
                displayPos2.remove(p.getUniqueId());
                startSelectionParticles(p);
            }
            case "pos2" -> {
                rm.setPos2(p.getUniqueId(), p.getWorld().getName(), p.getLocation().getBlockX(), p.getLocation().getBlockZ());
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7领地点2已设置: &f" + p.getLocation().getBlockX() + ", " + p.getLocation().getBlockZ()));
                displayPos2.put(p.getUniqueId(), new int[]{p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ()});
                startSelectionParticles(p);
                String info = rm.getSelectionInfo(p.getUniqueId());
                if (info != null) {
                    p.sendMessage(ColorUtil.colorize("&8选区: &7" + info));
                    long area = rm.getSelectionArea(p.getUniqueId());
                    long minArea = plugin.getConfigManager().getTerritoryMinArea();
                    if (area < minArea) {
                        p.sendMessage(ColorUtil.colorize("&c面积 " + area + " 格，低于最小限制 " + minArea + " 格"));
                    } else {
                        double est = plugin.getConfigManager().calcTerritoryCost(area);
                        String estStr = plugin.getVaultHook().isEnabled()
                                ? plugin.getVaultHook().format(est)
                                : String.format("%.2f", est);
                        p.sendMessage(ColorUtil.colorize("&8预估费用: &e" + estStr
                                + " &8（" + area + " 格 × " + plugin.getConfigManager().getTerritoryCost() + "）"));
                    }
                }
            }
            case "claim" -> {
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos territory claim <名称>")); return; }
                if (!rm.hasSelection(p.getUniqueId())) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7请先设置 pos1 和 pos2")); return;
                }
                // 领地重叠检查
                RegionManager.Region conflict = rm.checkTerritoryOverlap(p.getUniqueId());
                if (conflict != null) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &c选区与已有领地「&f"
                            + conflict.name() + "&c」重叠，请重新选择范围"));
                    return;
                }
                if (!plugin.getVaultHook().isEnabled()) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7经济系统不可用")); return;
                }
                // 取选区面积计算实际费用
                long area = rm.getSelectionArea(p.getUniqueId());
                long minArea = plugin.getConfigManager().getTerritoryMinArea();
                if (area < minArea) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &c领地面积太小（" + area + " 格），最小需要 &f" + minArea + " &c格"));
                    return;
                }
                double actualCost = plugin.getConfigManager().calcTerritoryCost(area);
                double bal = plugin.getVaultHook().getBalance(p);
                if (bal < actualCost) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7余额不足，需要 &f"
                            + plugin.getVaultHook().format(actualCost)
                            + " &8（面积 " + area + " 格 × " + plugin.getConfigManager().getTerritoryCost() + "）"
                            + "  &7当前余额 &f" + plugin.getVaultHook().format(bal)));
                    return;
                }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                String error = plugin.getVaultHook().withdraw(p, actualCost);
                if (error != null) { p.sendMessage(ColorUtil.colorize("&8[ECOS] &7扣款失败: " + error)); return; }
                var loc = p.getLocation();
                rm.claimTerritory(p.getUniqueId(), name, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7领地 &f" + name + " &7已创建"
                        + "  &8面积: " + area + " 格  扣款: &f" + plugin.getVaultHook().format(actualCost)
                        + "\n&8传送点: &f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()
                        + "  &8（可用 /ecos territory spawn " + name + " 改）"
                        + "\n&8（默认隐藏，使用 /ecos territory visible " + name + " 设为公开）"));
            }
            case "delete" -> {
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos territory delete <名称>")); return; }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                RegionManager.Region deleted = rm.deleteByNameWithInfo(name, p.getUniqueId(), p.hasPermission("es2uni.admin"));
                if (deleted == null) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7未找到领地或无权限")); return;
                }
                // 退款：仅对有 owner 的领地退钱
                if (deleted.owner() != null && plugin.getVaultHook().isEnabled()) {
                    double ratio      = plugin.getConfigManager().getTerritoryRefundRatio();
                    double origCost   = plugin.getConfigManager().calcTerritoryCost(deleted.area());
                    double refund     = origCost * ratio;
                    plugin.getVaultHook().deposit(p, refund);
                    p.sendMessage(ColorUtil.colorize(
                            "&8[ECOS] &7领地 &f" + name + " &7已删除，退款 &f"
                            + plugin.getVaultHook().format(refund)
                            + " &8（原价 " + plugin.getVaultHook().format(origCost)
                            + " × " + (int)(ratio * 100) + "%）"));
                } else {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7领地已删除"));
                }
            }
            case "list" -> {
                List<RegionManager.Region> own = rm.getByOwner(p.getUniqueId());
                if (own.isEmpty()) { p.sendMessage(ColorUtil.colorize("&8[ECOS] &7你暂无领地")); return; }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7你的领地:"));
                for (RegionManager.Region r : own)
                    p.sendMessage(ColorUtil.colorize("  &f" + r.name()
                            + " &8" + r.world()
                            + (r.visible() ? " &a[公开]" : " &8[隐藏]")));
            }
            case "visible" -> {
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos territory visible <名称>")); return; }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                plugin.getRegionManager().toggleVisible(name, p.getUniqueId(), p.hasPermission("es2uni.admin"))
                        .ifPresentOrElse(
                                v -> p.sendMessage(ColorUtil.colorize("&8[ECOS] &7领地 &f" + name + " &7已设为" + (v ? "&a公开" : "&8隐藏"))),
                                () -> p.sendMessage(ColorUtil.colorize("&8[ECOS] &7未找到领地或无权限")));
            }
            case "spawn", "tpset", "setspawn" -> {
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos territory spawn <名称>")); return; }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                var loc = p.getLocation();
                rm.setSpawn(name, p.getUniqueId(), p.hasPermission("es2uni.admin"),
                        loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())
                        .ifPresentOrElse(
                                r -> p.sendMessage(ColorUtil.colorize("&8[ECOS] &7领地 &f" + r.name()
                                        + " &7传送点已设为 &f" + r.spawnX() + " " + r.spawnY() + " " + r.spawnZ()
                                        + (r.contains(p.getWorld().getName(), loc.getBlockX(), loc.getBlockZ())
                                        ? "" : "\n&8（在圈地范围外，热门地点会传到这里）"))),
                                () -> p.sendMessage(ColorUtil.colorize("&8[ECOS] &7未找到领地或无权限")));
            }
            default -> p.sendMessage(ColorUtil.colorize("&c用法: /ecos territory <pos1|pos2|claim|delete|list|visible|spawn>"));
        }
    }

    // ── 房产（3D 房间，和领地独立）──────────────────────────────────────────

    private static final String ESTATE_DISCLAIMER =
            "&8没按规定注册的房子不追究，但出问题不保障。注册成房产才受保障。";

    private void handleEstate(Player p, String[] args) {
        EstateManager em = plugin.getEstateManager();
        if (args.length < 2) {
            plugin.getEstateSaleGUI().open(p);
            return;
        }
        if (args[1].equalsIgnoreCase("gui") || args[1].equalsIgnoreCase("menu")) {
            plugin.getEstateBuildingsGUI().open(p);
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "sale", "buy", "shop" -> {
                plugin.getEstateSaleGUI().open(p);
                return;
            }
            case "tools" -> {
                plugin.getEstateToolsGUI().open(p);
                return;
            }
            case "help" -> openEstateHelp(p);
            case "pos1" -> {
                em.setPos1(p);
                var loc = p.getLocation();
                p.sendMessage(ColorUtil.colorize("&8[房产] &7点1（站立）: &f"
                        + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                        + "  &8建议用选区棒点方块"));
                refreshEstatePreview(p);
            }
            case "pos2" -> {
                String err = em.setPos2(p);
                if (err != null) {
                    p.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                    return;
                }
                var loc = p.getLocation();
                p.sendMessage(ColorUtil.colorize("&8[房产] &7点2（站立）: &f"
                        + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
                refreshEstatePreview(p);
                var sel = em.selectionOf(p.getUniqueId());
                if (sel != null) {
                    EstateUnit probe = sel.asProbe();
                    p.sendMessage(ColorUtil.colorize("&8空间: &7" + probe.volume() + " 格  "
                            + probe.minX() + "," + probe.minY() + "," + probe.minZ()
                            + " → " + probe.maxX() + "," + probe.maxY() + "," + probe.maxZ()));
                    double homeFee = em.registerFee(false, BuildingCategory.RESIDENTIAL, UnitKind.HOUSE);
                    if (homeFee > 0) {
                        p.sendMessage(ColorUtil.colorize("&8自建住宅登记费 &f" + money(homeFee)
                                + "  &8管理预制免费"));
                    }
                }
            }
            case "wand", "stick", "棒" -> {
                com.etherstories.escore.items.EstateWand.give(p);
                p.closeInventory();
                p.sendMessage(ColorUtil.colorize("&8[房产] &a选区棒: &f左键一角  右键对角（点到方块，含屋顶）"));
            }
            case "register" -> {
                EstateManager.ParsedRegister spec = EstateManager.parseRegister(
                        Arrays.copyOfRange(args, 2, args.length), false);
                if (spec == null) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos estate register <楼名> <层> <号> <用途> [分类]"));
                    p.sendMessage(ColorUtil.colorize("&8楼名可含空格。用途: " + EstateManager.kindsHint()));
                    p.sendMessage(ColorUtil.colorize("&8分类: " + EstateManager.catsHint() + "  &7可省略"));
                    return;
                }
                double fee = em.registerFee(false, spec.cat(), spec.kind());
                String err = em.register(p, spec.building(), spec.floor(), spec.room(), spec.kind(), spec.cat(), false, 0);
                if (err != null) {
                    p.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                    return;
                }
                EstateUnit u = em.find(spec.building(), spec.floor(), spec.room());
                p.sendMessage(ColorUtil.colorize("&8[房产] &a已登记 &f" + (u == null ? spec.building() : u.address())
                        + " &8" + spec.kind().label + " · " + spec.cat().label
                        + (fee > 0 ? "  &e注册费 " + money(fee) : " &7免费")));
                p.sendMessage(ColorUtil.colorize(ESTATE_DISCLAIMER));
                p.sendMessage(ColorUtil.colorize("&8站在门口: &f/ecos estate door"));
                p.sendMessage(ColorUtil.colorize("&8看墙写牌子: &f/ecos estate sign"));
                p.sendMessage(ColorUtil.colorize("&8上架出售: &f/ecos estate sell <楼> <层> <号> [金额]"));
            }
            case "set" -> {
                if (!p.hasPermission("es2uni.admin")) { noPerms(p); return; }
                EstateManager.ParsedRegister spec = EstateManager.parseRegister(
                        Arrays.copyOfRange(args, 2, args.length), true);
                if (spec == null) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos estate set <楼名> <层> <号> <用途> [分类] [价格]"));
                    return;
                }
                double price = spec.price() == null ? 0 : spec.price();
                String err = em.register(p, spec.building(), spec.floor(), spec.room(), spec.kind(), spec.cat(), true, price);
                if (err != null) {
                    p.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                    return;
                }
                EstateUnit u = em.find(spec.building(), spec.floor(), spec.room());
                p.sendMessage(ColorUtil.colorize("&8[房产] &a管理登记 &f" + (u == null ? spec.building() : u.address())
                        + (price > 0 ? " &e挂牌 " + money(price) : " &7空闲未挂牌")));
                p.sendMessage(ColorUtil.colorize("&8门口 / 牌子: &f/ecos estate door &7· &f/ecos estate sign"));
            }
            case "here", "info" -> em.sendHere(p, em.at(p.getLocation()));
            case "visit", "view" -> {
                EstateUnit u = resolveEstate(p, args, 2);
                if (u == null) {
                    p.sendMessage(ColorUtil.colorize("&8[房产] &7找不到房间。用法: /ecos estate visit <楼> <层> <号>"));
                    return;
                }
                p.closeInventory();
                String err = em.visit(p, u);
                if (err != null) p.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                else p.sendMessage(ColorUtil.colorize("&8[房产] &7看房 &f" + u.address()));
            }
            case "door" -> {
                EstateUnit u = resolveEstate(p, args, 2);
                String err = em.setDoor(p, u);
                if (err != null) p.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                else p.sendMessage(ColorUtil.colorize("&8[房产] &a门口已设 &f" + u.address()));
            }
            case "sign" -> {
                EstateUnit u = resolveEstate(p, args, 2);
                if (u == null) {
                    p.sendMessage(ColorUtil.colorize("&8[房产] &7站在房间里，或指定 楼 层 号"));
                    return;
                }
                if (!p.hasPermission("es2uni.admin") && !p.getUniqueId().equals(u.owner())
                        && !p.getUniqueId().equals(u.createdBy())) {
                    p.sendMessage(ColorUtil.colorize("&8[房产] &c这不是你的房间"));
                    return;
                }
                em.placeSign(p, u);
            }
            case "delete", "remove" -> {
                EstateUnit u = resolveEstate(p, args, 2);
                String err = em.delete(p, u);
                if (err != null) p.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                else p.sendMessage(ColorUtil.colorize("&8[房产] &7已删除 &f" + u.address()));
            }
            case "list" -> {
                List<EstateUnit> rooms = p.hasPermission("es2uni.admin") && args.length >= 3
                        && args[2].equalsIgnoreCase("all")
                        ? new ArrayList<>(em.all())
                        : em.ownedBy(p.getUniqueId());
                if (rooms.isEmpty()) {
                    p.sendMessage(ColorUtil.colorize("&8[房产] &7没有房间。终端城市页打开房产，或 /ecos estate help"));
                    return;
                }
                p.sendMessage(ColorUtil.colorize("&8[房产] &f" + rooms.size() + " 间"));
                for (EstateUnit u : rooms) {
                    p.sendMessage(ColorUtil.colorize("  &f" + u.address()
                            + " &8" + u.kind().label + " · " + u.category().label
                            + (u.vacant() ? " &a空闲" : " &7" + (u.ownerName() == null ? "?" : u.ownerName()))
                            + (u.listed() && u.price() > 0 ? " &e" + money(u.price()) : "")));
                }
            }
            case "tp" -> {
                EstateUnit u = resolveEstate(p, args, 2);
                if (u == null) {
                    p.sendMessage(ColorUtil.colorize("&8[房产] &7找不到房间"));
                    return;
                }
                boolean ok = p.hasPermission("es2uni.admin") || p.getUniqueId().equals(u.owner());
                if (!ok) {
                    p.sendMessage(ColorUtil.colorize("&8[房产] &c只能传送到自己的房间"));
                    return;
                }
                p.closeInventory();
                em.teleport(p, u);
                p.sendMessage(ColorUtil.colorize("&8[房产] &7已到 &f" + u.address()));
            }
            case "price" -> {
                SaleArgs sale = parseSaleArgs(p, args, false);
                if (sale == null) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos estate price <楼> <层> <号> <金额>"));
                    p.sendMessage(ColorUtil.colorize("&8或: /ecos estate price <金额>  &7(站在房里)"));
                    return;
                }
                if (sale.price == null) {
                    p.sendMessage(ColorUtil.colorize("&c请写金额"));
                    return;
                }
                boolean listed = sale.unit != null && sale.unit.listed() && sale.price > 0;
                String err = em.setSale(p, sale.unit, sale.price, listed);
                if (err != null) p.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                else p.sendMessage(ColorUtil.colorize("&8[房产] &7标价 &f" + money(sale.price)
                        + (listed ? " &e(仍挂牌)" : " &8未上架，/ecos estate sell")));
            }
            case "sell" -> {
                SaleArgs sale = parseSaleArgs(p, args, true);
                if (sale == null || sale.unit == null) {
                    p.sendMessage(ColorUtil.colorize("&8[房产] &7找不到房间"));
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos estate sell <楼> <层> <号> [金额]"));
                    return;
                }
                double price = sale.price != null && sale.price > 0 ? sale.price : sale.unit.price();
                String err = em.setSale(p, sale.unit, price, true);
                if (err != null) p.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                else p.sendMessage(ColorUtil.colorize("&8[房产] &a已挂牌 &f" + sale.unit.address() + " &e" + money(price)));
            }
            case "unsell" -> {
                EstateUnit u = resolveEstate(p, args, 2);
                String err = em.setSale(p, u, u == null ? 0 : u.price(), false);
                if (err != null) p.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                else p.sendMessage(ColorUtil.colorize("&8[房产] &7已下架 &f" + u.address()));
            }
            case "admin" -> {
                if (!p.hasPermission("es2uni.admin")) { noPerms(p); return; }
                plugin.getEstateMineGUI().openAdmin(p);
            }
            default -> p.sendMessage(ColorUtil.colorize(
                    "&c用法: /ecos estate <wand|tools|register|sale|gui|door|sign|here|help>"));
        }
    }

    private void handleHotel(Player p, String[] args) {
        HotelManager hm = plugin.getHotelManager();
        if (args.length < 2) {
            plugin.getHotelListGUI().open(p);
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "gui", "menu" -> plugin.getHotelListGUI().open(p);
            case "create" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos hotel create <名称>"));
                    return;
                }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                String err = hm.create(p, name);
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &a已创建 &f" + name : "&8[酒店] &c" + err));
            }
            case "bind" -> {
                HotelManager.Hotel h;
                EstateUnit u;
                if (args.length >= 3 && hm.byName(args[2]) != null) {
                    h = hm.byName(args[2]);
                    u = resolveEstate(p, args, 3);
                } else {
                    h = resolveHotel(p, null);
                    u = resolveEstate(p, args, 2);
                }
                if (u == null) u = hm.at(p.getLocation());
                String err = hm.bind(p, h, u);
                p.sendMessage(ColorUtil.colorize(err == null
                        ? "&8[酒店] &a已绑 &f" + (u == null ? "" : u.address()) : "&8[酒店] &c" + err));
            }
            case "unbind" -> {
                EstateUnit u = resolveEstate(p, args, 2);
                if (u == null) u = hm.at(p.getLocation());
                String err = hm.unbind(p, hm.hotelOf(u), u);
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &7已解绑" : "&8[酒店] &c" + err));
            }
            case "type" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos hotel type <房型>  &8" + HotelRoomType.hint()));
                    return;
                }
                EstateUnit u = hm.at(p.getLocation());
                String err = hm.setType(p, hm.roomOf(u), HotelRoomType.fromKey(args[2]));
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &7房型已改" : "&8[酒店] &c" + err));
            }
            case "price" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos hotel price <金额>"));
                    return;
                }
                try {
                    double price = Double.parseDouble(args[2]);
                    EstateUnit u = hm.at(p.getLocation());
                    String err = hm.setPrice(p, hm.roomOf(u), price);
                    p.sendMessage(ColorUtil.colorize(err == null
                            ? "&8[酒店] &7标价 &f" + hm.money(price) : "&8[酒店] &c" + err));
                } catch (NumberFormatException e) {
                    p.sendMessage(ColorUtil.colorize("&c金额必须是数字"));
                }
            }
            case "lock" -> {
                String err = hm.setLocked(p, hm.roomOf(hm.at(p.getLocation())), true);
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &c已上锁" : "&8[酒店] &c" + err));
            }
            case "unlock" -> {
                String err = hm.setLocked(p, hm.roomOf(hm.at(p.getLocation())), false);
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &a已开锁" : "&8[酒店] &c" + err));
            }
            case "checkin" -> {
                EstateUnit u = resolveEstate(p, args, 2);
                if (u == null) u = hm.at(p.getLocation());
                String err = hm.checkin(p, hm.roomOf(u));
                p.sendMessage(ColorUtil.colorize(err == null
                        ? "&8[酒店] &a已入住，房卡已发放"
                        : "&8[酒店] &c" + err));
            }
            case "checkout" -> {
                HotelManager.Room stay = hm.stayOf(p.getUniqueId());
                EstateUnit u = hm.at(p.getLocation());
                HotelManager.Room room = stay != null ? stay : hm.roomOf(u);
                String err = hm.checkout(p, room);
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &7已退房" : "&8[酒店] &c" + err));
            }
            case "card" -> {
                HotelManager.Room stay = hm.stayOf(p.getUniqueId());
                HotelManager.Hotel h = hm.hotelOfRoom(stay);
                String err = h == null ? "当前没有入住" : hm.giveCard(p, h, stay, hm.unitOf(stay));
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &a已补领房卡" : "&8[酒店] &c" + err));
            }
            case "manager" -> {
                if (args.length < 4) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos hotel manager <add|remove> <玩家> [酒店]"));
                    return;
                }
                HotelManager.Hotel h = resolveHotel(p, args.length >= 5 ? args[4] : null);
                OfflinePlayer who = resolvePlayed(args[3]);
                String err;
                if (args[2].equalsIgnoreCase("add")) err = hm.addManager(p, h, who);
                else err = who == null ? "找不到玩家" : hm.removeManager(p, h, who.getUniqueId());
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &a已更新管理者" : "&8[酒店] &c" + err));
            }
            case "transfer" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos hotel transfer <玩家> [酒店]"));
                    return;
                }
                String err = hm.transfer(p, resolveHotel(p, args.length >= 4 ? args[3] : null), resolvePlayed(args[2]));
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &a已转让" : "&8[酒店] &c" + err));
            }
            case "delete", "remove", "del" -> {
                String name = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;
                HotelManager.Hotel h = resolveHotel(p, name);
                if (h == null) return;
                String err = hm.delete(p, h);
                p.sendMessage(ColorUtil.colorize(err == null
                        ? "&8[酒店] &c已删除 &f" + h.name + " &7房间已解绑"
                        : "&8[酒店] &c" + err));
            }
            case "here", "info" -> hm.sendHere(p, hm.at(p.getLocation()));
            case "list" -> {
                var list = hm.ownedOrManaged(p.getUniqueId());
                if (list.isEmpty()) list = hm.all();
                p.sendMessage(ColorUtil.colorize("&8[酒店] &f" + list.size() + " 家"));
                for (HotelManager.Hotel h : list) {
                    int vacant = 0;
                    for (HotelManager.Room r : h.rooms) if (r.vacant()) vacant++;
                    p.sendMessage(ColorUtil.colorize("  &f" + h.name + " &8" + h.rooms.size()
                            + " 间 空 " + vacant + "  &7" + h.ownerName));
                }
            }
            case "help" -> p.sendMessage(ColorUtil.colorize(
                    "&8[酒店] &f/ecos hotel create <名>\n"
                            + " &7bind / unbind / type / price / lock / unlock\n"
                            + " &7checkin / checkout / card / manager add|remove\n"
                            + " &7transfer / delete / here / list / gui"));
            default -> p.sendMessage(ColorUtil.colorize("&c用法: /ecos hotel <gui|create|bind|checkin|delete|help>"));
        }
    }

    private HotelManager.Hotel resolveHotel(Player p, String name) {
        HotelManager hm = plugin.getHotelManager();
        if (name != null && !name.isBlank()) {
            HotelManager.Hotel h = hm.byName(name);
            if (h == null) p.sendMessage(ColorUtil.colorize("&8[酒店] &c没有这家: &f" + name));
            return h;
        }
        var list = hm.ownedOrManaged(p.getUniqueId());
        if (list.isEmpty()) {
            p.sendMessage(ColorUtil.colorize("&8[酒店] &c你没有酒店。&7/ecos hotel create <名>"));
            return null;
        }
        if (list.size() > 1) {
            p.sendMessage(ColorUtil.colorize("&8[酒店] &c你有多家酒店，请在指令里写店名"));
            return null;
        }
        return list.get(0);
    }

    private OfflinePlayer resolvePlayed(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        if (off.hasPlayedBefore() || off.isOnline()) return off;
        return null;
    }

    private Integer parseFloor(Player p, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            p.sendMessage(ColorUtil.colorize("&c层必须是整数，例如 3 或 -1"));
            return null;
        }
    }

    private record SaleArgs(EstateUnit unit, Double price) {}

    /** 同时接受 金额在前 / 门牌在前；楼名可含空格。 */
    private SaleArgs parseSaleArgs(Player p, String[] args, boolean priceOptional) {
        EstateManager em = plugin.getEstateManager();
        if (args.length == 2) {
            EstateUnit u = em.at(p.getLocation());
            return u == null ? null : new SaleArgs(u, null);
        }
        String[] rest = Arrays.copyOfRange(args, 2, args.length);
        Double price = null;
        String[] addrTok = rest;
        try {
            double last = Double.parseDouble(rest[rest.length - 1]);
            if (rest.length == 1) return new SaleArgs(em.at(p.getLocation()), last);
            if (rest.length >= 4) {
                String[] maybe = Arrays.copyOfRange(rest, 0, rest.length - 1);
                if (EstateManager.parseAddress(maybe) != null) {
                    price = last;
                    addrTok = maybe;
                }
            }
        } catch (NumberFormatException ignored) {}
        if (price == null && addrTok.length >= 1) {
            try {
                double first = Double.parseDouble(addrTok[0]);
                if (addrTok.length == 1) return new SaleArgs(em.at(p.getLocation()), first);
                String[] maybe = Arrays.copyOfRange(addrTok, 1, addrTok.length);
                if (EstateManager.parseAddress(maybe) != null) {
                    price = first;
                    addrTok = maybe;
                }
            } catch (NumberFormatException ignored) {}
        }
        EstateManager.ParsedAddress addr = EstateManager.parseAddress(addrTok);
        if (addr == null) {
            if (priceOptional) {
                EstateUnit here = em.at(p.getLocation());
                return here == null ? null : new SaleArgs(here, price);
            }
            return null;
        }
        EstateUnit u = em.find(addr.building(), addr.floor(), addr.room());
        if (u == null) p.sendMessage(ColorUtil.colorize("&8[房产] &7没有这间: " + addr.building()
                + " " + addr.floor() + "-" + addr.room()));
        return new SaleArgs(u, price);
    }

    private EstateUnit resolveEstate(Player p, String[] args, int start) {
        EstateManager em = plugin.getEstateManager();
        if (args.length >= start + 3) {
            EstateManager.ParsedAddress addr = EstateManager.parseAddress(
                    Arrays.copyOfRange(args, start, args.length));
            if (addr == null) {
                p.sendMessage(ColorUtil.colorize("&c层必须是整数，例如 3 或 -1"));
                return null;
            }
            EstateUnit u = em.find(addr.building(), addr.floor(), addr.room());
            if (u == null) p.sendMessage(ColorUtil.colorize("&8[房产] &7没有这间: "
                    + addr.building() + " " + addr.floor() + "-" + addr.room()));
            return u;
        }
        return em.at(p.getLocation());
    }

    private String money(double v) {
        if (plugin.getVaultHook().isEnabled()) return plugin.getVaultHook().format(v);
        return String.format("%.0f", v);
    }

    private void openEstateHelp(Player p) {
        ItemStack book = new ItemStack(org.bukkit.Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;
        meta.setTitle("房产指南");
        meta.setAuthor("ECOS");
        meta.addPage(
                "§0§l房产系统\n\n" +
                "§8和领地不是一回事。\n" +
                "§0领地是 2D 地皮保护。\n" +
                "§0房产是按规定登记的房间：\n" +
                "登记、买卖、传送。\n\n" +
                "§c没按规定注册的房子，\n" +
                "§c出问题不保障。\n" +
                "§0注册成房产才受保障。\n\n" +
                "§0地址例：\n" +
                "§8星港一号 3-301");
        meta.addPage(
                "§0§l建筑分类\n\n" +
                "§c住宅 residential\n" +
                "§b公共建筑 public\n" +
                "§6商业建筑 commercial\n\n" +
                "§0§l房间用途\n" +
                "§8house 住宅\n" +
                "apartment 公寓\n" +
                "hotel 客房（可绑酒店）\n" +
                "shop 商铺（免费）\n" +
                "workshop 工坊\n" +
                "studio 工作室\n" +
                "storage 仓库\n" +
                "other 其他");
        meta.addPage(
                "§0§l玩家登记\n\n" +
                "§81. §0领选区棒（木斧）\n" +
                "§8终端 → 登记 → 选区棒\n\n" +
                "§82. §0左键点房间一角\n" +
                "§0右键点对角（含屋顶）\n" +
                "§8会出粒子框\n\n" +
                "§83. §0登记（住宅收注册费）\n" +
                "§8打开登记页写：\n" +
                "§8楼名 层 号 用途\n" +
                "§8商铺 shop 免费；住宅收费\n" +
                "§8管理预制 /ecos estate set 免费\n\n" +
                "§84. §0站门口\n" +
                "§8/ecos estate door\n\n" +
                "§85. §0看墙放牌子\n" +
                "§8/ecos estate sign\n\n" +
                "§0牌子四行：\n" +
                "§8ES2注册单位-住宅\n" +
                "§8楼名 / 3-301 / 房主");
        meta.addPage(
                "§0§l上架 / 购买\n\n" +
                "§0自助登记默认不上架。\n\n" +
                "§8/ecos estate price <金额>\n" +
                "§8/ecos estate sell\n" +
                "§8/ecos estate unsell\n\n" +
                "§0终端出行/城市：§6买房§0 待售列表，\n" +
                "左键看房，右键购买（再点确认）。\n" +
                "§8登记 / 门口 在「登记房产」页。\n\n" +
                "§8/ecos estate here\n" +
                "§0站在房间里看门牌。\n" +
                "§8/ecos estate visit 楼 层 号\n\n" +
                "§0好友开了位置分享，\n" +
                "且人在自己房间里时，\n" +
                "好友列表显示门牌。");
        meta.addPage(
                "§0§l管理\n\n" +
                "§8/ecos estate set\n" +
                "§8<楼> <层> <号> <用途>\n" +
                "§8[分类] [价格]\n\n" +
                "§0空闲 + 价格>0 即挂牌。\n" +
                "商店、公寓都可以这样放。\n\n" +
                "§8/ecos estate admin\n" +
                "§0全部房产总表。\n\n" +
                "§8终端 → 城市 / 出行 → 房产");
        book.setItemMeta(meta);
        p.openBook(book);
    }

    // ── Waypoint subcommand handler ────────────────────────────────────────────

    private void handleWaypoint(Player p, String[] args) {
        if (args.length < 2) {
            plugin.getWaypointGUI().open(p); return;
        }
        WaypointManager wm = plugin.getWaypointManager();

        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos wp add <名称>")); return; }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                org.bukkit.Location loc = p.getLocation();
                boolean ok = wm.add(p.getUniqueId(), name, loc.getWorld().getName(),
                        loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                p.sendMessage(ColorUtil.colorize(ok
                        ? "&8[ECOS] &7已收藏 &f" + name
                        : "&8[ECOS] &7收藏已满（上限 " + wm.maxPerPlayer() + " 个）"));
            }
            case "remove", "del" -> {
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos wp remove <名称>")); return; }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                boolean ok = wm.remove(p.getUniqueId(), name);
                p.sendMessage(ColorUtil.colorize(ok ? "&8[ECOS] &7已删除 &f" + name : "&8[ECOS] &7未找到该坐标"));
            }
            case "tp" -> {
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos wp tp <名称>")); return; }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                WaypointManager.Waypoint wp = wm.getByName(p.getUniqueId(), name);
                if (wp == null) { p.sendMessage(ColorUtil.colorize("&8[ECOS] &7未找到该坐标")); return; }
                org.bukkit.World world = Bukkit.getWorld(wp.world());
                if (world == null) { p.sendMessage(ColorUtil.colorize("&8[ECOS] &7世界不存在")); return; }
                p.teleport(new org.bukkit.Location(world, wp.x() + 0.5, wp.y(), wp.z() + 0.5));
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已传送到 &f" + wp.name()));
            }
            case "list" -> {
                List<WaypointManager.Waypoint> list = wm.get(p.getUniqueId());
                if (list.isEmpty()) { p.sendMessage(ColorUtil.colorize("&8[ECOS] &7暂无收藏")); return; }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7坐标收藏:"));
                for (WaypointManager.Waypoint wp : list)
                    p.sendMessage(ColorUtil.colorize("  &f" + wp.name() + " &8" + wp.world() + " " + wp.x() + "," + wp.y() + "," + wp.z()));
            }
            default -> plugin.getWaypointGUI().open(p);
        }
    }

    private void openTerritoryHelp(Player p) {
        double pricePerBlock = plugin.getConfigManager().getTerritoryCost();
        double minCost       = plugin.getConfigManager().getTerritoryMinCost();
        String minStr = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(minCost)
                : String.format("%.2f", minCost);

        ItemStack book = new ItemStack(org.bukkit.Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;
        meta.setTitle("领地指南");
        meta.setAuthor("ECOS");

        // Page 1 — 什么是领地
        meta.addPage(
                "§0§l领地系统\n\n" +
                "§8ECOS Territory Guide\n" +
                "§r\n" +
                "§0领地是你在服务器\n" +
                "上圈定的一块专属\n" +
                "区域。\n\n" +
                "§0设为公开后，其他\n" +
                "玩家可以在「热门\n" +
                "地点」看到并传送\n" +
                "到你的领地。");

        // Page 2 — 圈地步骤
        meta.addPage(
                "§0§l如何圈地\n\n" +
                "§81. §0站在区域一角\n" +
                "§8/ecos territory pos1\n\n" +
                "§82. §0走到对角\n" +
                "§8/ecos territory pos2\n\n" +
                "§83. §0确认范围后\n" +
                "§8/ecos territory\n" +
                "§8claim §7<名称>\n\n" +
                "§c费用: §0" + pricePerBlock + " /格\n" +
                "§8最低: " + minStr);

        // Page 3 — 公开与传送
        meta.addPage(
                "§0§l公开 / 隐藏\n\n" +
                "§0新建领地默认隐藏。\n\n" +
                "§0切换公开状态:\n" +
                "§8/ecos territory\n" +
                "§8visible §7<名称>\n\n" +
                "§0公开后会出现在\n" +
                "终端「§6热门地点§0」\n" +
                "中，任何人可以\n" +
                "点击传送过来。\n\n" +
                "§0再次输入则切回隐藏。");

        // Page 4 — 管理指令
        meta.addPage(
                "§0§l领地管理\n\n" +
                "§0查看你的领地:\n" +
                "§8/ecos territory list\n\n" +
                "§0删除领地:\n" +
                "§8/ecos territory\n" +
                "§8delete §7<名称>\n\n" +
                "§0打开终端热门地点:\n" +
                "§8/ecos menu\n" +
                "§7→ 热门地点\n\n" +
                "§8注：面积越大费用越高");

        // Page 5 — 注意事项
        meta.addPage(
                "§0§l注意事项\n\n" +
                "§0· 圈地范围仅限水\n" +
                "  平（X/Z），高度\n" +
                "  全覆盖\n\n" +
                "· 传送点可单独指定，\n" +
                "  与 pos1/pos2 无关\n" +
                "§8/ecos territory\n" +
                "§8spawn §7<名称>\n" +
                "§0站在落点上再输入。\n" +
                "  终端里也可潜行\n" +
                "  左键设置。\n\n" +
                "· 费用按面积计算，\n" +
                "  单价可由管理员\n" +
                "  在配置文件调整\n\n" +
                "§8祝你玩得愉快！");

        book.setItemMeta(meta);
        p.openBook(book);
    }

    // ── Selection particle display ─────────────────────────────────────────────

    /** Cancel any running selection particle task and clean display data for this player. */
    public void cleanupParticles(UUID uuid) {
        BukkitRunnable old = selectionTasks.remove(uuid);
        if (old != null && !old.isCancelled()) old.cancel();
        displayPos1.remove(uuid);
        displayPos2.remove(uuid);
    }

    public void refreshEstatePreview(Player p) {
        UUID uuid = p.getUniqueId();
        int[] a = plugin.getEstateManager().pos1Of(uuid);
        int[] b = plugin.getEstateManager().pos2Of(uuid);
        if (a != null) displayPos1.put(uuid, a.clone());
        else displayPos1.remove(uuid);
        if (b != null) displayPos2.put(uuid, b.clone());
        else displayPos2.remove(uuid);
        startSelectionParticles(p);
    }

    /**
     * Start (or restart) a 5-second particle display for the current selection.
     * pos1 = green, pos2 = orange, outline edges = yellow.
     */
    private void startSelectionParticles(Player p) {
        UUID uuid = p.getUniqueId();
        BukkitRunnable old = selectionTasks.remove(uuid);
        if (old != null && !old.isCancelled()) old.cancel();

        Particle.DustOptions green  = new Particle.DustOptions(Color.fromRGB(0,   220,  50),  1.5f);
        Particle.DustOptions orange = new Particle.DustOptions(Color.fromRGB(255, 100,   0),  1.5f);
        Particle.DustOptions yellow = new Particle.DustOptions(Color.fromRGB(255, 220,   0),  1.0f);

        BukkitRunnable task = new BukkitRunnable() {
            int iterations = 0;
            final int[] dp1 = displayPos1.get(uuid);
            final int[] dp2 = displayPos2.get(uuid);

            @Override
            public void run() {
                if (!p.isOnline() || iterations >= 10) {
                    cancel(); selectionTasks.remove(uuid); return;
                }
                iterations++;

                if (dp1 != null)
                    p.spawnParticle(Particle.DUST, dp1[0] + 0.5, dp1[1] + 1.0, dp1[2] + 0.5,
                            10, 0.2, 0.2, 0.2, 0, green);

                if (dp2 != null) {
                    p.spawnParticle(Particle.DUST, dp2[0] + 0.5, dp2[1] + 1.0, dp2[2] + 0.5,
                            10, 0.2, 0.2, 0.2, 0, orange);

                    if (dp1 != null) {
                        int ax1 = Math.min(dp1[0], dp2[0]), ax2 = Math.max(dp1[0], dp2[0]);
                        int az1 = Math.min(dp1[2], dp2[2]), az2 = Math.max(dp1[2], dp2[2]);
                        int y   = Math.min(dp1[1], dp2[1]) + 1;
                        drawEdge(p, y, ax1, az1, ax2, az1, yellow);
                        drawEdge(p, y, ax2, az1, ax2, az2, yellow);
                        drawEdge(p, y, ax2, az2, ax1, az2, yellow);
                        drawEdge(p, y, ax1, az2, ax1, az1, yellow);
                    }
                }
            }
        };
        selectionTasks.put(uuid, task);
        task.runTaskTimer(plugin, 0L, 10L);
    }

    /** Draw a line of dust particles from (x1,z1) to (x2,z2) at height y, only for player p. */
    private void drawEdge(Player p, int y, int x1, int z1, int x2, int z2, Particle.DustOptions opts) {
        int dx = x2 - x1, dz = z2 - z1;
        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        if (steps == 0) return;
        int step = Math.max(1, steps / 48); // at most ~48 particles per edge
        for (int i = 0; i <= steps; i += step) {
            double t = (double) i / steps;
            p.spawnParticle(Particle.DUST,
                    x1 + 0.5 + dx * t, y + 0.5, z1 + 0.5 + dz * t,
                    1, 0, 0, 0, 0, opts);
        }
    }

    // ── 光环特效 ──────────────────────────────────────────────────────────────

    private void handleAura(Player p, String[] args) {
        if (args.length < 2) {
            plugin.getAuraShopGUI().open(p);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "shop" -> plugin.getAuraShopGUI().open(p);

            case "equip" -> {
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&c用法: /ecos particle equip <光环ID>")); return; }
                AuraType type = AuraType.fromKey(args[2]);
                if (type == null) { p.sendMessage(ColorUtil.colorize("&c未知光环: " + args[2])); return; }
                if (!plugin.getAuraManager().owns(p.getUniqueId(), type)) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7你尚未拥有该光环，请在特效商店购买。")); return;
                }
                plugin.getAuraManager().equip(p.getUniqueId(), type);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已装备 &b" + type.displayName()));
            }

            case "unequip" -> {
                plugin.getAuraManager().unequip(p.getUniqueId());
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已卸除光环"));
            }

            case "brightness", "bright" -> {
                if (args.length >= 3) {
                    var b = com.etherstories.escore.auras.ParticleBrightness.fromKey(args[2]);
                    plugin.getAuraManager().setBrightness(p.getUniqueId(), b);
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7粒子亮度: &f" + b.label));
                } else {
                    var b = plugin.getAuraManager().cycleBrightness(p.getUniqueId());
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7粒子亮度: &f" + b.label
                            + " &8(/ecos particle brightness low|medium|high)"));
                }
            }

            case "give" -> {
                if (!p.hasPermission("es2uni.admin")) { noPerms(p); return; }
                if (args.length < 4) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos particle give <玩家> <光环ID>")); return;
                }
                Player target = org.bukkit.Bukkit.getPlayerExact(args[2]);
                if (target == null) { p.sendMessage(ColorUtil.colorize("&c玩家不在线")); return; }
                AuraType type = AuraType.fromKey(args[3]);
                if (type == null) { p.sendMessage(ColorUtil.colorize("&c未知光环: " + args[3])); return; }
                plugin.getAuraManager().giveAura(target.getUniqueId(), type);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已赠予 &f" + target.getName()
                        + " &b" + type.displayName()));
                target.sendMessage(ColorUtil.colorize("&8[ECOS] &7你获得了光环 &b" + type.displayName()));
            }

            case "list" -> {
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7可用光环:"));
                for (AuraType t : AuraType.values()) {
                    boolean owns = plugin.getAuraManager().owns(p.getUniqueId(), t);
                    p.sendMessage(ColorUtil.colorize("  " + (owns ? "&a" : "&7") + t.displayName()
                            + " &8[" + t.key + "] " + (owns ? "&a已拥有" : "&7¥" + t.price)));
                }
            }

            default -> p.sendMessage(ColorUtil.colorize(
                    "&c用法: /ecos aura|particle <shop|equip|unequip|list>"));
        }
    }

    // ── 武器技能绑定 ──────────────────────────────────────────────────────────

    private void handleWeapon(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ColorUtil.colorize("&c用法: /ecos weapon <give|save|delete|list|bind|unbind|info>"));
            return;
        }

        switch (args[1].toLowerCase()) {

            case "give" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos weapon give <武器ID>"));
                    listPresets(p); return;
                }
                ItemStack saved = plugin.getWeaponSaveManager().cloneOf(args[2]);
                if (saved != null) {
                    p.getInventory().addItem(saved);
                    WeaponSaveManager.Saved rec = plugin.getWeaponSaveManager().get(args[2]);
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已获得存档 &f" + args[2].toLowerCase()
                            + (rec != null && rec.label() != null ? " &8(" + rec.label() + ")" : "")));
                    return;
                }
                WeaponPreset.Preset preset = WeaponPreset.find(args[2]);
                if (preset == null) {
                    p.sendMessage(ColorUtil.colorize("&c未知武器: " + args[2]));
                    listPresets(p); return;
                }
                p.getInventory().addItem(preset.item().clone());
                p.sendMessage(ColorUtil.colorize(
                        "&8[ECOS] &7已获得 &f" + preset.enName() + " &8「&7" + preset.zhName() + "&8」"));
            }

            case "save" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos weapon save <id> [显示名]"));
                    p.sendMessage(ColorUtil.colorize("&7把手持物品存成预设。显示名支持 & 颜色。Quark 符文会一起保存。"));
                    return;
                }
                String name = args.length >= 4
                        ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length))
                        : null;
                String err = plugin.getWeaponSaveManager().save(args[2], name, p.getInventory().getItemInMainHand());
                if (err != null) {
                    p.sendMessage(ColorUtil.colorize("&c[ECOS] " + err));
                    return;
                }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已保存 &f" + args[2].toLowerCase()
                        + " &8— /ecos weapon give " + args[2].toLowerCase()));
            }

            case "delete", "remove" -> {
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos weapon delete <id>"));
                    return;
                }
                if (!plugin.getWeaponSaveManager().delete(args[2])) {
                    p.sendMessage(ColorUtil.colorize("&c[ECOS] 没有这份存档: " + args[2]));
                    return;
                }
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已删除存档 &f" + args[2].toLowerCase()));
            }

            case "list" -> listPresets(p);

            case "info" -> {
                ItemStack held = p.getInventory().getItemInMainHand();
                if (held == null || held.getType() == org.bukkit.Material.AIR) {
                    p.sendMessage(ColorUtil.colorize("&c[ECOS] 请手持目标物品")); return;
                }
                SkillType r  = SkillBinder.getSkill(held, SkillBinder.Slot.RIGHT);
                SkillType sr = SkillBinder.getSkill(held, SkillBinder.Slot.SNEAK_RIGHT);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7手持物品技能绑定:"));
                p.sendMessage(ColorUtil.colorize("  &7右键       : &f" + (r  == null ? "（无）" : r.displayName())));
                p.sendMessage(ColorUtil.colorize("  &7Shift+右键 : &f" + (sr == null ? "（无）" : sr.displayName())));
            }

            case "bind" -> {
                ItemStack held = p.getInventory().getItemInMainHand();
                if (held == null || held.getType() == org.bukkit.Material.AIR) {
                    p.sendMessage(ColorUtil.colorize("&c[ECOS] 请手持目标物品")); return;
                }
                if (args.length < 4) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos weapon bind <技能> <right|sneak_right>"));
                    listSkills(p); return;
                }
                SkillType skill = SkillType.fromKey(args[2]);
                if (skill == null) {
                    p.sendMessage(ColorUtil.colorize("&c未知技能: " + args[2]));
                    listSkills(p); return;
                }
                SkillBinder.Slot slot = parseSlot(args[3]);
                if (slot == null) {
                    p.sendMessage(ColorUtil.colorize("&c槽位须为 right 或 sneak_right")); return;
                }
                SkillBinder.bind(held, skill, slot);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已将 &f" + skill.displayName()
                        + " &7绑定到 &f" + slotName(slot)));
            }

            case "unbind" -> {
                ItemStack held = p.getInventory().getItemInMainHand();
                if (held == null || held.getType() == org.bukkit.Material.AIR) {
                    p.sendMessage(ColorUtil.colorize("&c[ECOS] 请手持目标物品")); return;
                }
                if (args.length < 3) {
                    p.sendMessage(ColorUtil.colorize("&c用法: /ecos weapon unbind <right|sneak_right>")); return;
                }
                SkillBinder.Slot slot = parseSlot(args[2]);
                if (slot == null) {
                    p.sendMessage(ColorUtil.colorize("&c槽位须为 right 或 sneak_right")); return;
                }
                SkillBinder.unbind(held, slot);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已解绑 &f" + slotName(slot) + " &7槽位"));
            }

            default -> p.sendMessage(ColorUtil.colorize(
                    "&c用法: /ecos weapon <give|save|delete|list|bind|unbind|info>"));
        }
    }

    private void listPresets(Player p) {
        p.sendMessage(ColorUtil.colorize("&8内置武器:"));
        for (String id : WeaponPreset.allIds()) {
            WeaponPreset.Preset pr = WeaponPreset.find(id);
            if (pr == null) continue;
            p.sendMessage(ColorUtil.colorize("  &7" + pr.id()
                    + " &8→ &f" + pr.enName() + "「" + pr.zhName() + "」"));
        }
        var custom = plugin.getWeaponSaveManager().all();
        if (!custom.isEmpty()) {
            p.sendMessage(ColorUtil.colorize("&8已保存（含符文/NBT）:"));
            for (WeaponSaveManager.Saved s : custom) {
                p.sendMessage(ColorUtil.colorize("  &d" + s.id() + " &8→ &f" + s.label()));
            }
        }
        p.sendMessage(ColorUtil.colorize("&8/ecos weapon save <id> [显示名] &7— 手持存档"));
    }

    private void listSkills(Player p) {
        p.sendMessage(ColorUtil.colorize("&8可绑武器:"));
        for (SkillType t : SkillType.values()) {
            p.sendMessage(ColorUtil.colorize("  &7" + t.configKey + " &8→ &f" + t.displayName()
                    + (t.playerSkill() ? " &8也可 skill grant + 潜行+F" : "")));
        }
    }

    private void handleSkillShop(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("open") || args[1].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("仅玩家可打开商店");
                return;
            }
            plugin.getSkillShopGUI().open(p);
            return;
        }
        if (args[1].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return; }
            if (!(sender instanceof Player p)) { sender.sendMessage("仅玩家可执行"); return; }
            plugin.getSkillShopGUI().openAdmin(p);
            return;
        }
        if (!sender.hasPermission("es2uni.admin")) { noPerms(sender); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "on", "enable" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.colorize("&c用法: /ecos skillshop on <技能> [价格]"));
                    return;
                }
                SkillType skill = SkillType.fromKey(args[2]);
                if (skill == null) {
                    sender.sendMessage(ColorUtil.colorize("&c未知技能: " + args[2]));
                    return;
                }
                double price = args.length >= 4
                        ? parsePrice(args[3], plugin.getSkillShopManager().price(skill))
                        : plugin.getSkillShopManager().price(skill);
                if (price < 0) {
                    sender.sendMessage(ColorUtil.colorize("&c价格必须是数字"));
                    return;
                }
                plugin.getSkillShopManager().set(skill, true, price);
                sender.sendMessage(ColorUtil.colorize("&8[技能商店] &a已上架 &f" + skill.displayName()
                        + " &7" + (plugin.getVaultHook().isEnabled()
                        ? plugin.getVaultHook().format(price) : String.format("%.0f", price))));
            }
            case "off", "disable" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.colorize("&c用法: /ecos skillshop off <技能>"));
                    return;
                }
                SkillType skill = SkillType.fromKey(args[2]);
                if (skill == null) {
                    sender.sendMessage(ColorUtil.colorize("&c未知技能: " + args[2]));
                    return;
                }
                plugin.getSkillShopManager().setEnabled(skill, false);
                sender.sendMessage(ColorUtil.colorize("&8[技能商店] &8已下架 &f" + skill.displayName()));
            }
            case "price" -> {
                if (args.length < 4) {
                    sender.sendMessage(ColorUtil.colorize("&c用法: /ecos skillshop price <技能> <价格>"));
                    return;
                }
                SkillType skill = SkillType.fromKey(args[2]);
                if (skill == null) {
                    sender.sendMessage(ColorUtil.colorize("&c未知技能: " + args[2]));
                    return;
                }
                double price = parsePrice(args[3], -1);
                if (price < 0) {
                    sender.sendMessage(ColorUtil.colorize("&c价格必须是数字"));
                    return;
                }
                plugin.getSkillShopManager().setPrice(skill, price);
                sender.sendMessage(ColorUtil.colorize("&8[技能商店] &7" + skill.displayName()
                        + " &7定价 &f" + (plugin.getVaultHook().isEnabled()
                        ? plugin.getVaultHook().format(price) : String.format("%.0f", price))));
            }
            default -> sender.sendMessage(ColorUtil.colorize(
                    "&c用法: /ecos skillshop [admin|on|off|price]"));
        }
    }

    private static double parsePrice(String raw, double fallback) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void handleSkill(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.colorize("&c用法: /ecos skill <grant|revoke|list> [玩家] [gleam_arc]"));
            sender.sendMessage(ColorUtil.colorize("&7学会后潜行+F 释放。普通 F 仍换副手。也可 /ecos weapon bind gleam_arc。"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "grant", "give" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.colorize("&c用法: /ecos skill grant <玩家> [gleam_arc]"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(ColorUtil.colorize("&c玩家不在线: " + args[2]));
                    return;
                }
                SkillType skill = args.length >= 4 ? SkillType.fromKey(args[3]) : SkillType.GLEAM_ARC;
                if (skill == null || !skill.playerSkill()) {
                    sender.sendMessage(ColorUtil.colorize("&c玩家技能只有 gleam_arc（也可 /ecos weapon bind gleam_arc）"));
                    return;
                }
                PlayerSkills.setGleamArc(target, true);
                sender.sendMessage(ColorUtil.colorize("&8[ECOS] &7已让 &f" + target.getName()
                        + " &7学会 &f" + skill.displayName() + " &8(潜行+F)"));
                target.sendMessage(ColorUtil.colorize("&8[ECOS] &7已学会 &f" + skill.displayName()
                        + " &8— 潜行+F"));
            }
            case "revoke", "remove", "ungive" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.colorize("&c用法: /ecos skill revoke <玩家> [gleam_arc]"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(ColorUtil.colorize("&c玩家不在线: " + args[2]));
                    return;
                }
                PlayerSkills.setGleamArc(target, false);
                sender.sendMessage(ColorUtil.colorize("&8[ECOS] &7已收回 &f" + target.getName() + " &7的霁弧"));
                target.sendMessage(ColorUtil.colorize("&8[ECOS] &7霁弧已收回"));
            }
            case "list" -> {
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) {
                        sender.sendMessage(ColorUtil.colorize("&c玩家不在线: " + args[2]));
                        return;
                    }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(ColorUtil.colorize("&c用法: /ecos skill list <玩家>"));
                    return;
                }
                sender.sendMessage(ColorUtil.colorize("&8[ECOS] &f" + target.getName() + " &7玩家技能:"));
                sender.sendMessage(ColorUtil.colorize("  gleam_arc &8→ &fGleam Arc「霁弧」 "
                        + (PlayerSkills.hasGleamArc(target) ? "&a已学会" : "&7未学会")));
            }
            default -> sender.sendMessage(ColorUtil.colorize(
                    "&c用法: /ecos skill <grant|revoke|list> [玩家] [gleam_arc]"));
        }
    }

    private SkillBinder.Slot parseSlot(String s) {
        return switch (s.toLowerCase()) {
            case "right"       -> SkillBinder.Slot.RIGHT;
            case "sneak_right" -> SkillBinder.Slot.SNEAK_RIGHT;
            default            -> null;
        };
    }

    private String slotName(SkillBinder.Slot slot) {
        return slot == SkillBinder.Slot.RIGHT ? "右键" : "Shift+右键";
    }

    private void handleMunicipal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("仅玩家可执行");
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("cleanup")) {
            // /ecos municipal cleanup yes|no <id>
            if (args.length < 4) {
                p.sendMessage(ColorUtil.colorize("&7/ecos municipal cleanup <yes|no> <id>"));
                return;
            }
            boolean yes = args[2].equalsIgnoreCase("yes") || args[2].equalsIgnoreCase("agree");
            boolean no = args[2].equalsIgnoreCase("no") || args[2].equalsIgnoreCase("refuse");
            if (!yes && !no) {
                p.sendMessage(ColorUtil.colorize("&7请用 yes 或 no"));
                return;
            }
            String err = plugin.getCleanupRequestManager().vote(p, args[3], yes);
            if (err != null) p.sendMessage(ColorUtil.colorize("&8[城管] &7" + err));
            return;
        }
        if (args.length >= 2) {
            switch (args[1].toLowerCase()) {
                case "jobs", "job" -> {
                    if (args.length >= 4 && args[2].equalsIgnoreCase("kick")) {
                        // /ecos municipal job kick <id> <name>
                        if (args.length < 5) {
                            p.sendMessage(ColorUtil.colorize("&7/ecos municipal job kick <委托ID> <玩家名>"));
                            return;
                        }
                        String err = plugin.getJobBoardManager().removeWorker(p, args[3], args[4]);
                        p.sendMessage(ColorUtil.colorize(err == null
                                ? "&8[招工处] &a已移除成员" : "&8[招工处] &c" + err));
                        return;
                    }
                    if (args.length >= 4 && (args[2].equalsIgnoreCase("lock")
                            || args[2].equalsIgnoreCase("unlock"))) {
                        boolean lock = args[2].equalsIgnoreCase("lock");
                        String err = plugin.getJobBoardManager().setLocked(p, args[3], lock);
                        p.sendMessage(ColorUtil.colorize(err == null
                                ? (lock ? "&8[招工处] &c已锁定" : "&8[招工处] &a已解锁")
                                : "&8[招工处] &c" + err));
                        return;
                    }
                    if (args.length >= 4 && args[2].equalsIgnoreCase("cancel")) {
                        String err = plugin.getJobBoardManager().cancel(p, args[3]);
                        p.sendMessage(ColorUtil.colorize(err == null
                                ? "&8[招工处] &7已取消退款" : "&8[招工处] &c" + err));
                        return;
                    }
                    plugin.getJobBoardGUI().open(p);
                    return;
                }
                case "insurance", "ins" -> { plugin.getInsuranceGUI().open(p); return; }
                case "recycle", "recyclebin", "bin" -> { plugin.getRecycleBinGUI().open(p); return; }
                case "cleanup", "clean" -> {
                    String err = plugin.getCleanupRequestManager().request(p);
                    p.sendMessage(ColorUtil.colorize(err == null
                            ? "&8[城管] &a已发起清理申请"
                            : "&8[城管] &c" + err));
                    return;
                }
                case "transit", "rail" -> {
                    plugin.getTransitOfficeGUI().open(p);
                    return;
                }
                case "pve" -> {
                    if (!p.hasPermission("es2uni.admin")) { noPerms(p); return; }
                    p.getInventory().addItem(com.etherstories.escore.items.PveItems.pack(),
                            com.etherstories.escore.items.PveItems.elite());
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已给刷怪棒 / 精英棒  &8右键刷、潜行换种类"));
                    return;
                }
                case "help" -> {
                    p.sendMessage(ColorUtil.colorize(
                            "&8[管理处]\n"
                                    + " &7/ecos municipal jobs\n"
                                    + " &7/ecos municipal job kick <id> <名>\n"
                                    + " &7/ecos municipal job lock|unlock <id>\n"
                                    + " &7/ecos municipal cleanup / recyclebin / insurance / transit"
                                    + (p.hasPermission("es2uni.admin") ? " / pve" : "")));
                    return;
                }
            }
        }
        plugin.getMunicipalOfficeGUI().open(p);
    }

    private void handleTransit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("仅玩家可执行");
            return;
        }
        var tm = plugin.getTransitManager();
        if (args.length < 2) {
            plugin.getTransitOfficeGUI().open(p);
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        boolean admin = p.hasPermission("es2uni.admin");
        switch (sub) {
            case "gui", "office" -> plugin.getTransitOfficeGUI().open(p);
            case "map" -> plugin.getTransitMapGUI().open(p);
            case "history" -> plugin.getTransitHistoryGUI().open(p);
            case "board", "rank", "ranks" -> plugin.getTransitBoardGUI().open(p);
            case "list" -> {
                p.sendMessage(ColorUtil.colorize("&8[交通] &f线路 " + tm.allLines().size()
                        + " &8· 车站 " + tm.allStations().size()
                        + " &8· 连接 " + tm.allEdges().size()
                        + " &8· 席别 " + tm.allCabins().size()));
                for (var c : tm.allCabins()) {
                    p.sendMessage(ColorUtil.colorize(" &f席别 &e" + c.id() + " &8" + c.displayName()
                            + " &7×" + c.fareMul()));
                }
                for (var l : tm.allLines()) {
                    p.sendMessage(ColorUtil.colorize(" &f线路 &e" + l.id() + " &8" + l.displayName()
                            + " &7" + l.type() + " hop=" + l.hopFare() + " max=" + l.maxFare()));
                    for (var e : tm.edgesOfLine(l.id())) {
                        p.sendMessage(ColorUtil.colorize("   &7" + e.from() + " &8↔ &7" + e.to()
                                + " &f" + tm.stationName(e.from()) + " — " + tm.stationName(e.to())
                                + (e.fare() >= 0 ? " &e" + e.fare() : "")));
                    }
                }
                for (var s : tm.allStations()) {
                    var nb = tm.neighborLabels(s.id(), null);
                    p.sendMessage(ColorUtil.colorize(" &f站 &e" + s.id() + " &8" + s.displayName()
                            + (tm.isSkipStop(s.id()) ? " &e通过" : "")
                            + (nb.isEmpty() ? " &8无连接" : " &7→ &f" + String.join("&7, &f", nb))));
                }
            }
            case "help" -> p.sendMessage(ColorUtil.colorize(
                    "&8[交通]\n"
                            + " &7/ecos transit                 &f交通处\n"
                            + " &7/ecos transit map / history / list / board\n"
                            + (admin ? " &c管理:\n"
                            + " &7/ecos transit line create <id> [类型id]\n"
                            + " &7/ecos transit line type <id> <类型id>\n"
                            + " &7/ecos transit line name <id> <显示名>\n"
                            + " &7/ecos transit line fare <id> <每站> <全程上限>\n"
                            + " &7/ecos transit line delete <id>\n"
                            + " &7/ecos transit type create <id> [颜色] [显示名]\n"
                            + " &7/ecos transit type name <id> <显示名>\n"
                            + " &7/ecos transit type delete <id>\n"
                            + " &7/ecos transit cabin create <id> <显示名> [倍率]\n"
                            + " &7/ecos transit cabin name <id> <显示名>\n"
                            + " &7/ecos transit cabin fare <id> <倍率>\n"
                            + " &7/ecos transit cabin delete <id>\n"
                            + " &7/ecos transit cabin list\n"
                            + " &7/ecos transit station create <id> [半径]\n"
                            + " &7/ecos transit station name <id> <中文名>\n"
                            + " &7/ecos transit station nameen <id> <English>\n"
                            + " &7/ecos transit station move <id> [半径]  &8脚下\n"
                            + " &7/ecos transit station skip <站> [on|off]  &8通过不停车\n"
                            + " &7/ecos transit station line|unline <站> <线路>\n"
                            + " &7/ecos transit edge add <from> <to> <line> [fare]\n"
                            + " &7/ecos transit edge remove <from> <to> <line>\n"
                            + " &7/ecos transit give gate <站> <in|out|both> [席别]\n"
                            + " &7/ecos transit give tvm <站>\n"
                            + " &7/ecos transit give adjust <站>\n"
                            + " &7/ecos transit gate set <站> <in|out|both> [席别]  &8看向方块\n"
                            + " &7/ecos transit tvm set <站>\n"
                            + " &7/ecos transit adjust set <站>\n" : "")));
            case "line" -> {
                if (!admin) { noPerms(p); return; }
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&7/ecos transit line create|name|fare|delete ...")); return; }
                switch (args[2].toLowerCase(Locale.ROOT)) {
                    case "create" -> {
                        if (args.length < 4) { p.sendMessage(ColorUtil.colorize("&7/ecos transit line create <id> [type]")); return; }
                        String err = tm.createLine(args[3], args.length >= 5 ? args[4] : "local");
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a线路已创建" : "&8[交通] &c" + err));
                    }
                    case "name" -> {
                        if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7/ecos transit line name <id> <名>")); return; }
                        String name = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                        String err = tm.renameLine(args[3], name);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已改名" : "&8[交通] &c" + err));
                    }
                    case "fare" -> {
                        if (args.length < 6) { p.sendMessage(ColorUtil.colorize("&7/ecos transit line fare <id> <hop> <max>")); return; }
                        try {
                            String err = tm.setLineFares(args[3], Double.parseDouble(args[4]), Double.parseDouble(args[5]));
                            p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a票价已更新" : "&8[交通] &c" + err));
                        } catch (NumberFormatException e) {
                            p.sendMessage(ColorUtil.colorize("&8[交通] &c数字无效"));
                        }
                    }
                    case "delete" -> {
                        if (args.length < 4) return;
                        String err = tm.deleteLine(args[3]);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7已删除线路" : "&8[交通] &c" + err));
                    }
                    case "type" -> {
                        if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7/ecos transit line type <线路> <类型id>")); return; }
                        String err = tm.setLineType(args[3], args[4]);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a线路类型已更新" : "&8[交通] &c" + err));
                    }
                    default -> p.sendMessage(ColorUtil.colorize("&7create / name / fare / type / delete"));
                }
            }
            case "type" -> {
                if (!admin) { noPerms(p); return; }
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&7/ecos transit type create|name|delete ...")); return; }
                switch (args[2].toLowerCase(Locale.ROOT)) {
                    case "create" -> {
                        if (args.length < 4) { p.sendMessage(ColorUtil.colorize("&7/ecos transit type create <id> [颜色] [名]")); return; }
                        String color = args.length >= 5 ? args[4] : "&b";
                        String name = args.length >= 6 ? String.join(" ", Arrays.copyOfRange(args, 5, args.length)) : args[3];
                        String err = tm.createType(args[3], color, name);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a类型已创建，建线时写这个 id" : "&8[交通] &c" + err));
                    }
                    case "name" -> {
                        if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7/ecos transit type name <id> <名>")); return; }
                        String name = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                        String err = tm.renameType(args[3], name);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已改名" : "&8[交通] &c" + err));
                    }
                    case "delete" -> {
                        if (args.length < 4) { p.sendMessage(ColorUtil.colorize("&7/ecos transit type delete <id>")); return; }
                        String err = tm.deleteType(args[3]);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7已删除类型" : "&8[交通] &c" + err));
                    }
                    default -> p.sendMessage(ColorUtil.colorize("&7create / name / delete"));
                }
            }
            case "cabin", "class" -> {
                if (!admin) { noPerms(p); return; }
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&7/ecos transit cabin create|name|fare|delete|list")); return; }
                switch (args[2].toLowerCase(Locale.ROOT)) {
                    case "create", "add" -> {
                        if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7/ecos transit cabin create <id> <显示名> [倍率]")); return; }
                        double mul = 1.0;
                        int nameEnd = args.length;
                        if (args.length >= 6) {
                            try {
                                mul = Double.parseDouble(args[args.length - 1]);
                                nameEnd = args.length - 1;
                            } catch (NumberFormatException ignored) {}
                        }
                        String name = String.join(" ", Arrays.copyOfRange(args, 4, nameEnd));
                        String err = tm.createCabin(args[3], name, mul);
                        p.sendMessage(ColorUtil.colorize(err == null
                                ? "&8[交通] &a席别已创建 · 给闸机时写这个 id"
                                : "&8[交通] &c" + err));
                    }
                    case "name" -> {
                        if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7/ecos transit cabin name <id> <显示名>")); return; }
                        String name = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                        String err = tm.renameCabin(args[3], name);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已改名" : "&8[交通] &c" + err));
                    }
                    case "fare", "mul" -> {
                        if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7/ecos transit cabin fare <id> <倍率>")); return; }
                        try {
                            String err = tm.setCabinMul(args[3], Double.parseDouble(args[4]));
                            p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a倍率已更新" : "&8[交通] &c" + err));
                        } catch (NumberFormatException e) {
                            p.sendMessage(ColorUtil.colorize("&8[交通] &c数字无效"));
                        }
                    }
                    case "delete" -> {
                        if (args.length < 4) { p.sendMessage(ColorUtil.colorize("&7/ecos transit cabin delete <id>")); return; }
                        String err = tm.deleteCabin(args[3]);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7已删除席别" : "&8[交通] &c" + err));
                    }
                    case "list" -> {
                        for (var c : tm.allCabins()) {
                            p.sendMessage(ColorUtil.colorize(" &f" + c.id() + " &8" + c.displayName() + " &7×" + c.fareMul()));
                        }
                    }
                    default -> p.sendMessage(ColorUtil.colorize("&7create / name / fare / delete / list"));
                }
            }
            case "station" -> {
                if (!admin) { noPerms(p); return; }
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&7/ecos transit station create|name|nameen|move|skip|line|unline|delete")); return; }
                switch (args[2].toLowerCase(Locale.ROOT)) {
                    case "create" -> {
                        if (args.length < 4) { p.sendMessage(ColorUtil.colorize("&7/ecos transit station create <id> [半径]")); return; }
                        int r = plugin.getConfig().getInt("transit.default-radius", 16);
                        if (args.length >= 5) try { r = Integer.parseInt(args[4]); } catch (NumberFormatException ignored) {}
                        String err = tm.createStation(args[3], p.getLocation(), r);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a车站已创建（脚下半径 " + r + "）" : "&8[交通] &c" + err));
                    }
                    case "name" -> {
                        if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7/ecos transit station name <id> <中文名>")); return; }
                        String name = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                        String err = tm.renameStation(args[3], name);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a中文名已更新" : "&8[交通] &c" + err));
                    }
                    case "nameen", "en" -> {
                        if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7/ecos transit station nameen <id> <English>")); return; }
                        String name = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                        String err = tm.renameStationEn(args[3], name);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &aEnglish name updated" : "&8[交通] &c" + err));
                    }
                    case "line" -> {
                        if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7/ecos transit station line <站> <线路>")); return; }
                        String err = tm.addStationLine(args[3], args[4]);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已接线" : "&8[交通] &c" + err));
                    }
                    case "unline" -> {
                        if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7/ecos transit station unline <站> <线路>")); return; }
                        String err = tm.removeStationLine(args[3], args[4]);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7已摘线" : "&8[交通] &c" + err));
                    }
                    case "move" -> {
                        if (args.length < 4) { p.sendMessage(ColorUtil.colorize("&7/ecos transit station move <id> [半径]")); return; }
                        TransitManager.Station cur = tm.getStation(args[3]);
                        int r = cur == null ? 8 : tm.stationRadius(cur);
                        if (args.length >= 5) try { r = Integer.parseInt(args[4]); } catch (NumberFormatException ignored) {}
                        String err = tm.moveStation(args[3], p.getLocation(), r);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a判定区已移到脚下（半径 " + r + "）" : "&8[交通] &c" + err));
                    }
                    case "skip", "pass" -> {
                        if (args.length < 4) { p.sendMessage(ColorUtil.colorize("&7/ecos transit station skip <站> [on|off]")); return; }
                        Boolean on = null;
                        if (args.length >= 5) {
                            String a = args[4].toLowerCase(Locale.ROOT);
                            if (a.equals("on") || a.equals("true") || a.equals("1") || a.equals("yes")) on = true;
                            else if (a.equals("off") || a.equals("false") || a.equals("0") || a.equals("no")) on = false;
                        }
                        String err = tm.setSkipStop(args[3], on);
                        p.sendMessage(ColorUtil.colorize(err != null ? "&8[交通] &c" + err
                                : (tm.isSkipStop(args[3]) ? "&8[交通] &e已设为通过不停车" : "&8[交通] &a已恢复停车")));
                    }
                    case "hint" -> p.sendMessage(ColorUtil.colorize("&8[交通] &7Create 坐标标记已停用"));
                    case "delete" -> {
                        if (args.length < 4) return;
                        String err = tm.deleteStation(args[3]);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7已删除车站" : "&8[交通] &c" + err));
                    }
                    default -> p.sendMessage(ColorUtil.colorize("&7create / name / move / skip / line / unline / delete"));
                }
            }
            case "edge" -> {
                if (!admin) { noPerms(p); return; }
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&7/ecos transit edge add|remove ...")); return; }
                if (args[2].equalsIgnoreCase("add")) {
                    if (args.length < 6) { p.sendMessage(ColorUtil.colorize("&7/ecos transit edge add <from> <to> <line> [fare]")); return; }
                    double fare = -1;
                    if (args.length >= 7) try { fare = Double.parseDouble(args[6]); } catch (NumberFormatException ignored) {}
                    String err = tm.addEdge(args[3], args[4], args[5], fare);
                    p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已连接" : "&8[交通] &c" + err));
                } else if (args[2].equalsIgnoreCase("remove")) {
                    if (args.length < 6) return;
                    String err = tm.removeEdge(args[3], args[4], args[5]);
                    p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7已断开" : "&8[交通] &c" + err));
                }
            }
            case "give" -> {
                if (!admin) { noPerms(p); return; }
                if (args.length < 3) { p.sendMessage(ColorUtil.colorize("&7/ecos transit give gate|tvm|adjust|card ...")); return; }
                switch (args[2].toLowerCase(Locale.ROOT)) {
                    case "gate" -> {
                        if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7/ecos transit give gate <站> <in|out|both> [席别]")); return; }
                        if (tm.getStation(args[3]) == null) { p.sendMessage(ColorUtil.colorize("&8[交通] &c车站不存在")); return; }
                        String cabin = args.length >= 6 ? args[5] : TransitManager.Cabin.STD;
                        p.getInventory().addItem(com.etherstories.escore.items.TransitItems.gate(args[3], args[4], cabin));
                        p.sendMessage(ColorUtil.colorize("&8[交通] &a已给予闸机 · &e" + tm.cabinName(cabin)));
                    }
                    case "tvm" -> {
                        if (args.length < 4) { p.sendMessage(ColorUtil.colorize("&7/ecos transit give tvm <站>")); return; }
                        if (tm.getStation(args[3]) == null) { p.sendMessage(ColorUtil.colorize("&8[交通] &c车站不存在")); return; }
                        p.getInventory().addItem(com.etherstories.escore.items.TransitItems.tvm(args[3]));
                        p.sendMessage(ColorUtil.colorize("&8[交通] &a已给予售票机"));
                    }
                    case "adjust" -> {
                        if (args.length < 4) { p.sendMessage(ColorUtil.colorize("&7/ecos transit give adjust <站>")); return; }
                        if (tm.getStation(args[3]) == null) { p.sendMessage(ColorUtil.colorize("&8[交通] &c车站不存在")); return; }
                        p.getInventory().addItem(com.etherstories.escore.items.TransitItems.adjust(args[3]));
                        p.sendMessage(ColorUtil.colorize("&8[交通] &a已给予补票处牌"));
                    }
                    case "card" -> {
                        String err = tm.buyCard(p);
                        p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已发放交通卡" : "&8[交通] &c" + err));
                    }
                    default -> p.sendMessage(ColorUtil.colorize("&7gate / tvm / adjust / card"));
                }
            }
            case "gate" -> {
                if (!admin) { noPerms(p); return; }
                if (args.length >= 3 && args[2].equalsIgnoreCase("set")) {
                    if (args.length < 5) { p.sendMessage(ColorUtil.colorize("&7看向方块: /ecos transit gate set <站> <in|out|both> [席别]")); return; }
                    var target = p.getTargetBlockExact(6);
                    if (target == null) { p.sendMessage(ColorUtil.colorize("&8[交通] &c请看向一个方块")); return; }
                    if (tm.getStation(args[3]) == null) { p.sendMessage(ColorUtil.colorize("&8[交通] &c车站不存在")); return; }
                    String cabin = args.length >= 6 ? args[5] : TransitManager.Cabin.STD;
                    tm.registerGate(target, args[3], args[4], cabin);
                    p.sendMessage(ColorUtil.colorize("&8[交通] &a已把该方块登记为闸机 · &e" + tm.cabinName(cabin)));
                }
            }
            case "tvm" -> {
                if (!admin) { noPerms(p); return; }
                if (args.length >= 3 && args[2].equalsIgnoreCase("set")) {
                    if (args.length < 4) return;
                    var target = p.getTargetBlockExact(6);
                    if (target == null) { p.sendMessage(ColorUtil.colorize("&8[交通] &c请看向一个方块")); return; }
                    if (tm.getStation(args[3]) == null) { p.sendMessage(ColorUtil.colorize("&8[交通] &c车站不存在")); return; }
                    tm.registerTvm(target, args[3]);
                    p.sendMessage(ColorUtil.colorize("&8[交通] &a已把该方块登记为售票机"));
                }
            }
            case "adjust" -> {
                if (!admin) { noPerms(p); return; }
                if (args.length >= 3 && args[2].equalsIgnoreCase("set")) {
                    if (args.length < 4) return;
                    var target = p.getTargetBlockExact(6);
                    if (target == null) { p.sendMessage(ColorUtil.colorize("&8[交通] &c请看向一个方块")); return; }
                    if (tm.getStation(args[3]) == null) { p.sendMessage(ColorUtil.colorize("&8[交通] &c车站不存在")); return; }
                    tm.registerAdjust(target, args[3]);
                    p.sendMessage(ColorUtil.colorize("&8[交通] &a已把该方块登记为补票处"));
                }
            }
            default -> plugin.getTransitOfficeGUI().open(p);
        }
    }

    private void handleTerminalClaim(Player p) {
        for (var stack : p.getInventory().getContents()) {
            if (ECOSTerminalItem.isTerminalItem(stack)) {
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7你已有终端物品"));
                return;
            }
        }
        double cost = plugin.getConfig().getDouble("terminal.claim-cost", 4096);
        if (!plugin.getVaultHook().isEnabled()) {
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &c经济系统不可用"));
            return;
        }
        String err = plugin.getVaultHook().withdraw(p, cost);
        if (err != null) {
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &c领取失败: " + err
                    + " &8(需要 " + plugin.getVaultHook().format(cost) + ")"));
            return;
        }
        var left = p.getInventory().addItem(ECOSTerminalItem.create());
        if (!left.isEmpty()) {
            left.values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
        }
        p.sendMessage(ColorUtil.colorize("&8[ECOS] &a已花费 &e"
                + plugin.getVaultHook().format(cost) + " &a领取终端"));
    }

    private void handleWeb(Player p, String[] args) {
        if (plugin.getWebSessions() == null) {
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7网页终端未启动"));
            return;
        }
        boolean reset = args.length >= 2 && (args[1].equalsIgnoreCase("reset")
                || args[1].equalsIgnoreCase("revoke") || args[1].equalsIgnoreCase("renew"));
        String pin = reset ? plugin.getWebSessions().reset(p) : plugin.getWebSessions().reveal(p);
        if (reset) {
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已为你换好新的令牌，旧的不能再用来接入。"));
        }
        p.sendMessage(ColorUtil.colorize("&8[ECOS] &fECOS Token &a" + pin));
        p.sendMessage(ColorUtil.colorize("&8[ECOS] &7请自己留着。各处登录都用这一串，忘记了再输入本指令即可再看。"));
        String href = webPairUrl(pin);
        if (!href.isBlank()) {
            TextComponent link = new TextComponent(ColorUtil.colorize("&8[ECOS] &a▶ 点此打开网页终端"));
            link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, href));
            link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(ColorUtil.colorize("&7" + href + "\n&8点开后自动填入令牌"))));
            p.spigot().sendMessage(link);
        } else {
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7网页端口 &f"
                    + plugin.getConfig().getInt("web.port", 8766)
                    + "  &8（管理填 web.public-url 才会出可点链接）"));
        }
        p.sendMessage(ColorUtil.colorize("&8[ECOS] &7重置令牌: &f/ecos web reset"));
    }

    private String webPairUrl(String code) {
        String url = plugin.getConfig().getString("web.public-url", "");
        if (url == null || url.isBlank()) return "";
        url = url.trim();
        if (!url.contains("://")) url = "http://" + url;
        int hash = url.indexOf('#');
        if (hash >= 0) url = url.substring(0, hash);
        return url + (url.contains("?") ? "&" : "?") + "code=" + code;
    }

    private void noPerms(CommandSender s) {
        s.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getNoPermMessage()));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getHelpMessage()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        try {
            return tabCompleteInner(sender, args);
        } catch (Throwable t) {
            plugin.getLogger().warning("tab 失败: " + t);
            return List.of();
        }
    }

    private List<String> tabCompleteInner(CommandSender sender, String[] args) {
        if (args.length == 1)
            return filterPrefix(args[0], List.of(
                    "help", "menu", "gui", "web", "tps", "version", "event", "afk",
                    "friends", "friend", "mail", "mailbox", "online",
                    "notice", "notices", "leaderboard", "lb", "top",
                    "checkin", "death", "deaths", "status", "r", "reply",
                    "wp", "waypoint", "estate", "hotel", "territory", "aura", "particle", "particles", "kit", "music",
                    "reload", "broadcast", "give", "claim", "tpsalert", "admin",
                    "region", "weapon", "skill", "skillshop", "kitadmin", "kits",
                    "audit", "tradereport", "taxreport", "reports", "report",
                    "showcase", "machine", "lucky", "guide", "newbie",
                    "municipal", "muni", "city", "transit", "rail", "metro", "pve"));

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "weapon"                -> filterPrefix(args[1], List.of("give", "save", "delete", "list", "bind", "unbind", "info"));
                case "skill"                 -> filterPrefix(args[1], List.of("grant", "revoke", "list"));
                case "skillshop", "skill-shop" -> filterPrefix(args[1], List.of("admin", "on", "off", "price"));
                case "aura", "particle", "particles" -> filterPrefix(args[1], List.of("shop", "equip", "unequip", "list", "give", "brightness"));
                case "tradereport", "taxreport" -> filterPrefix(args[1], List.of("broadcast"));
                case "friend", "friends"     -> filterPrefix(args[1], List.of(
                        "gui", "list", "add", "remove", "accept", "deny", "requests", "share", "help"));
                case "mail", "mailbox"       -> filterPrefix(args[1], List.of(
                        "gui", "list", "read", "send", "delete", "help"));
                case "music"                 -> filterPrefix(args[1], List.of(
                        "gui", "search", "add", "select", "queue", "list", "now", "history",
                        "fav", "favorites", "preset", "presets", "playlist",
                        "vote", "stop", "mute", "join", "cancel", "help"));
                case "web"                   -> filterPrefix(args[1], List.of("reset", "revoke", "renew"));
                case "online"                -> filterPrefix(args[1], List.of("list", "gui", "help"));
                case "notice", "notices"     -> filterPrefix(args[1], List.of("list", "gui", "help"));
                case "leaderboard", "lb", "top" -> filterPrefix(args[1], List.of("list", "gui", "help"));
                case "event"                 -> filterPrefix(args[1], List.of("gui", "list", "join", "leave", "help"));
                case "kit"                   -> {
                    List<String> names = new ArrayList<>();
                    names.add("list");
                    if (plugin.getEssentialsHook().isEnabled())
                        names.addAll(plugin.getEssentialsHook().getKitNames());
                    names.addAll(plugin.getKitManager().names());
                    yield filterPrefix(args[1], names);
                }
                case "tpsalert"          -> filterPrefix(args[1], List.of("on", "off"));
                case "admin"             -> filterPrefix(args[1], List.of("event"));
                case "region"            -> filterPrefix(args[1], List.of("pos1", "pos2", "create", "delete", "list"));
                case "territory"         -> filterPrefix(args[1], List.of("pos1", "pos2", "claim", "delete", "list", "visible", "spawn", "help"));
                case "estate"            -> filterPrefix(args[1], List.of(
                        "gui", "sale", "buy", "tools", "help", "wand", "pos1", "pos2", "register", "set", "door", "sign",
                        "here", "visit", "delete", "list", "tp", "price", "sell", "unsell", "admin"));
                case "hotel"             -> filterPrefix(args[1], List.of(
                        "gui", "create", "bind", "unbind", "type", "price", "lock", "unlock",
                        "checkin", "checkout", "card", "manager", "transfer", "delete", "here", "list", "help"));
                case "lucky"             -> filterPrefix(args[1], List.of("ticket"));
                case "checkin"           -> filterPrefix(args[1], List.of("ticket", "gui"));
                case "wp","waypoint"     -> filterPrefix(args[1], List.of("add", "remove", "tp", "list"));
                case "transit", "rail", "metro" -> filterPrefix(args[1], List.of(
                        "gui", "map", "history", "board", "rank", "list", "help", "line", "station", "edge", "give", "gate", "tvm", "adjust", "type", "cabin"));
                case "municipal", "muni", "city" -> filterPrefix(args[1], List.of(
                        "jobs", "job", "cleanup", "recycle", "insurance", "transit", "pve", "help"));
                case "broadcast"         -> List.of("<消息>");
                case "status"            -> List.of("<签名>");
                case "r","reply"         -> List.of("<内容>");
                default                  -> null;
            };
        }

        // Level 3: suggest names from data
        if (args.length == 3 && sender instanceof Player p) {
            UUID uuid = p.getUniqueId();
            boolean isAdmin = p.hasPermission("es2uni.admin");
            return switch (args[0].toLowerCase()) {
                case "friend", "friends" -> {
                    if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("request")) {
                        yield filterPrefix(args[2], Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName).toList());
                    }
                    if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("del")
                            || args[1].equalsIgnoreCase("rm")) {
                        yield filterPrefix(args[2], plugin.getFriendManager().getFriends(uuid).stream()
                                .map(plugin.getFriendManager()::getDisplayName).toList());
                    }
                    if (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("deny")
                            || args[1].equalsIgnoreCase("reject")) {
                        yield filterPrefix(args[2], plugin.getFriendManager().getPendingRequests(uuid).stream()
                                .map(plugin.getFriendManager()::getDisplayName).toList());
                    }
                    yield null;
                }
                case "mail", "mailbox" -> {
                    if (args[1].equalsIgnoreCase("send")) {
                        yield filterPrefix(args[2], Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName).toList());
                    }
                    yield null;
                }
                case "music" -> {
                    if (args[1].equalsIgnoreCase("search")) yield List.of("<歌名>");
                    if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("play"))
                        yield List.of("<歌曲ID>");
                    if (args[1].equalsIgnoreCase("select")) yield List.of("1", "2", "3", "4", "5");
                    yield null;
                }
                case "event" -> {
                    if (args[1].equalsIgnoreCase("join") || args[1].equalsIgnoreCase("leave")) {
                        yield plugin.getEventManager().getAllEvents().stream()
                                .map(com.etherstories.escore.events.ServerEvent::getId).toList();
                    }
                    yield null;
                }
                case "territory" -> {
                    if (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("visible")
                            || args[1].equalsIgnoreCase("spawn") || args[1].equalsIgnoreCase("tpset")
                            || args[1].equalsIgnoreCase("setspawn")) {
                        List<String> names = new ArrayList<>();
                        plugin.getRegionManager().getByOwner(uuid)
                                .forEach(r -> names.add(r.name()));
                        if (isAdmin) plugin.getRegionManager().getAllTerritories()
                                .forEach(r -> { if (!names.contains(r.name())) names.add(r.name()); });
                        yield names;
                    }
                    yield null;
                }
                case "estate" -> estateTab(args, p);
                case "wp","waypoint" -> {
                    if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("tp")) {
                        yield plugin.getWaypointManager().get(uuid).stream()
                                .map(WaypointManager.Waypoint::name).toList();
                    }
                    yield null;
                }
                case "region" -> {
                    if (isAdmin && (args[1].equalsIgnoreCase("delete"))) {
                        yield plugin.getRegionManager().getDistricts().stream()
                                .map(RegionManager.Region::name).toList();
                    }
                    yield null;
                }
                case "weapon" -> {
                    if (args[1].equalsIgnoreCase("bind")) {
                        yield java.util.Arrays.stream(SkillType.values())
                                .map(t -> t.configKey).toList();
                    }
                    if (args[1].equalsIgnoreCase("unbind")) {
                        yield List.of("right", "sneak_right");
                    }
                    if (args[1].equalsIgnoreCase("give")) {
                        List<String> ids = new ArrayList<>(WeaponPreset.tabComplete(args[2]));
                        ids.addAll(plugin.getWeaponSaveManager().ids());
                        yield filterPrefix(args[2], ids);
                    }
                    if (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("remove")) {
                        yield filterPrefix(args[2], plugin.getWeaponSaveManager().ids());
                    }
                    if (args[1].equalsIgnoreCase("save")) {
                        yield filterPrefix(args[2], plugin.getWeaponSaveManager().ids());
                    }
                    yield null;
                }
                case "skill" -> {
                    if (!isAdmin) yield null;
                    if (args[1].equalsIgnoreCase("grant") || args[1].equalsIgnoreCase("give")
                            || args[1].equalsIgnoreCase("revoke") || args[1].equalsIgnoreCase("remove")
                            || args[1].equalsIgnoreCase("list")) {
                        yield filterPrefix(args[2], Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName).toList());
                    }
                    yield null;
                }
                case "skillshop", "skill-shop" -> {
                    if (!isAdmin) yield null;
                    if (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off")
                            || args[1].equalsIgnoreCase("price") || args[1].equalsIgnoreCase("enable")
                            || args[1].equalsIgnoreCase("disable")) {
                        yield filterPrefix(args[2], java.util.Arrays.stream(SkillType.values())
                                .map(t -> t.configKey).toList());
                    }
                    yield null;
                }
                case "transit", "rail", "metro" -> {
                    if (!isAdmin) yield null;
                    yield switch (args[1].toLowerCase(Locale.ROOT)) {
                        case "line" -> filterPrefix(args[2], List.of("create", "name", "fare", "type", "delete"));
                        case "type" -> filterPrefix(args[2], List.of("create", "name", "delete"));
                        case "cabin", "class" -> filterPrefix(args[2], List.of("create", "name", "fare", "delete", "list"));
                        case "station" -> filterPrefix(args[2], List.of("create", "name", "nameen", "move", "skip", "line", "unline", "delete"));
                        case "edge" -> filterPrefix(args[2], List.of("add", "remove"));
                        case "give" -> filterPrefix(args[2], List.of("gate", "tvm", "adjust", "card"));
                        case "gate", "tvm", "adjust" -> filterPrefix(args[2], List.of("set"));
                        default -> null;
                    };
                }
                default -> null;
            };
        }

        if (args.length == 4 && sender.hasPermission("es2uni.admin")
                && args[0].equalsIgnoreCase("skill")
                && (args[1].equalsIgnoreCase("grant") || args[1].equalsIgnoreCase("give")
                || args[1].equalsIgnoreCase("revoke") || args[1].equalsIgnoreCase("remove"))) {
            return filterPrefix(args[3], List.of("gleam_arc"));
        }

        if (args.length >= 4 && sender instanceof Player ep
                && args[0].equalsIgnoreCase("estate")) {
            return estateTab(args, ep);
        }

        if (args.length >= 4 && sender instanceof Player p
                && List.of("transit", "rail", "metro").contains(args[0].toLowerCase(Locale.ROOT))
                && p.hasPermission("es2uni.admin")) {
            return transitTab(args);
        }

        return null;
    }

    private List<String> estateTab(String[] args, Player p) {
        EstateManager em = plugin.getEstateManager();
        String sub = args[1].toLowerCase(Locale.ROOT);
        List<String> buildings = em.buildings(null);
        List<String> kinds = new ArrayList<>();
        for (UnitKind k : UnitKind.values()) kinds.add(k.key);
        List<String> cats = List.of("residential", "public", "commercial");
        List<String> owned = new ArrayList<>();
        for (EstateUnit u : em.ownedBy(p.getUniqueId())) owned.add(u.building());
        if (p.hasPermission("es2uni.admin")) {
            for (String b : buildings) if (!owned.contains(b)) owned.add(b);
        }
        if (args.length == 3) {
            return switch (sub) {
                case "register", "set" -> List.of("<楼名>");
                case "door", "sign", "delete", "remove", "tp", "sell", "unsell", "price", "visit", "view" ->
                        filterPrefix(args[2], owned.isEmpty() ? buildings : owned);
                case "list" -> p.hasPermission("es2uni.admin") ? filterPrefix(args[2], List.of("all")) : null;
                default -> null;
            };
        }
        if (args.length == 4) {
            if (sub.equals("register") || sub.equals("set") || sub.equals("door") || sub.equals("sign")
                    || sub.equals("delete") || sub.equals("remove") || sub.equals("tp")
                    || sub.equals("sell") || sub.equals("unsell") || sub.equals("price")
                    || sub.equals("visit") || sub.equals("view")) {
                return List.of("<层>");
            }
            return null;
        }
        if (args.length == 5) {
            if (sub.equals("register") || sub.equals("set") || sub.equals("door") || sub.equals("sign")
                    || sub.equals("delete") || sub.equals("remove") || sub.equals("tp")
                    || sub.equals("sell") || sub.equals("unsell") || sub.equals("price")
                    || sub.equals("visit") || sub.equals("view")) {
                return List.of("<房号>");
            }
            return null;
        }
        if (args.length == 6 && (sub.equals("register") || sub.equals("set"))) {
            return filterPrefix(args[5], kinds);
        }
        if (args.length == 7 && (sub.equals("register") || sub.equals("set"))) {
            return filterPrefix(args[6], cats);
        }
        return null;
    }

    private List<String> transitTab(String[] args) {
        var tm = plugin.getTransitManager();
        List<String> stations = new ArrayList<>();
        for (TransitManager.Station s : tm.allStations()) stations.add(s.id());
        List<String> lines = new ArrayList<>();
        for (TransitManager.Line l : tm.allLines()) lines.add(l.id());
        List<String> types = new ArrayList<>();
        for (TransitManager.LineType t : tm.allTypes()) types.add(t.id());
        List<String> cabins = new ArrayList<>();
        for (TransitManager.Cabin c : tm.allCabins()) cabins.add(c.id());
        String a1 = args[1].toLowerCase(Locale.ROOT);
        String a2 = args[2].toLowerCase(Locale.ROOT);
        if (args.length == 4) {
            return switch (a1) {
                case "line" -> switch (a2) {
                    case "name", "fare", "type", "delete" -> filterPrefix(args[3], lines);
                    default -> null;
                };
                case "type" -> switch (a2) {
                    case "name", "delete" -> filterPrefix(args[3], types);
                    default -> null;
                };
                case "cabin", "class" -> switch (a2) {
                    case "name", "fare", "delete" -> filterPrefix(args[3], cabins);
                    default -> null;
                };
                case "station" -> switch (a2) {
                    case "name", "nameen", "en", "move", "skip", "line", "unline", "delete" -> filterPrefix(args[3], stations);
                    default -> null;
                };
                case "edge" -> filterPrefix(args[3], stations);
                case "give" -> switch (a2) {
                    case "gate", "tvm", "adjust" -> filterPrefix(args[3], stations);
                    default -> null;
                };
                case "gate", "tvm", "adjust" -> "set".equals(a2) ? filterPrefix(args[3], stations) : null;
                default -> null;
            };
        }
        if (args.length == 5) {
            return switch (a1) {
                case "line" -> switch (a2) {
                    case "create", "type" -> filterPrefix(args[4], types);
                    default -> null;
                };
                case "type" -> "create".equals(a2)
                        ? filterPrefix(args[4], List.of("&b", "&3", "&9", "&a", "&2", "&e", "&6", "&c", "&d"))
                        : null;
                case "station" -> {
                    if ("line".equals(a2) || "unline".equals(a2)) yield filterPrefix(args[4], lines);
                    if ("skip".equals(a2)) yield filterPrefix(args[4], List.of("on", "off"));
                    yield null;
                }
                case "edge" -> filterPrefix(args[4], stations);
                case "give" -> "gate".equals(a2) ? filterPrefix(args[4], List.of("in", "out", "both")) : null;
                case "gate" -> "set".equals(a2) ? filterPrefix(args[4], List.of("in", "out", "both")) : null;
                default -> null;
            };
        }
        if (args.length == 6) {
            if ("edge".equals(a1)) return filterPrefix(args[5], lines);
            if ("give".equals(a1) && "gate".equals(a2)) return filterPrefix(args[5], cabins);
            if ("gate".equals(a1) && "set".equals(a2)) return filterPrefix(args[5], cabins);
        }
        return null;
    }

    private static List<String> filterPrefix(String input, List<String> options) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(o);
        }
        return out;
    }
}
