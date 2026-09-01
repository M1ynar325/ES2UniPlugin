package com.etherstories.escore.gui.terminal;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.gui.EcosStyle;
import com.etherstories.escore.managers.MailManager;
import com.etherstories.escore.managers.NoticeManager;
import com.etherstories.escore.managers.WaypointManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 终端里原先开箱子的名单页，改成书。 */
final class ListPages {
    private ListPages() {}

    static void register(TerminalHub hub, ES2UniPlugin plugin) {
        hub.registerPage(hidden("homes", "家园", (p, s) -> homes(plugin, p)));
        hub.registerPage(hidden("friends-list", "好友", (p, s) -> friends(plugin, p)));
        hub.registerPage(hidden("mail-list", "邮箱", (p, s) -> mail(plugin, p)));
        hub.registerPage(hidden("online-list", "在线", (p, s) -> online(plugin, p)));
        hub.registerPage(hidden("notices-list", "公告", (p, s) -> notices(plugin, p)));
        hub.registerPage(hidden("wp-list", "坐标", (p, s) -> waypoints(plugin, p)));
    }

    static void registerActions(TerminalHub hub, ES2UniPlugin plugin) {
        hub.registerAction("list.back", (p, s, raw) -> {
            String from = s.get("from");
            hub.open(p, from == null || from.isBlank() ? "overview" : from);
        });
        hub.registerAction("home.go", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            Bukkit.getScheduler().runTask(plugin, () -> p.performCommand("home " + raw));
        });
        hub.registerAction("friend.go", (p, s, raw) -> {
            UUID id = parseUuid(raw);
            if (id == null) return;
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getPlayerActionGUI().open(p, Bukkit.getOfflinePlayer(id)));
        });
        hub.registerAction("friend.add", (p, s, raw) ->
                plugin.getAnvilInputGUI().openForFriendName(p));
        hub.registerAction("friend.acc", (p, s, raw) -> {
            UUID id = parseUuid(raw);
            if (id == null) return;
            if (plugin.getFriendManager().acceptRequest(p.getUniqueId(), id)) {
                p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&8[ECOS] &7已接受请求"));
                Player sender = Bukkit.getPlayer(id);
                if (sender != null)
                    sender.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                            "&8[ECOS] &f" + p.getName() + " &7接受了你的好友请求"));
            }
            hub.refresh(p);
        });
        hub.registerAction("friend.den", (p, s, raw) -> {
            UUID id = parseUuid(raw);
            if (id == null) return;
            plugin.getFriendManager().denyRequest(p.getUniqueId(), id);
            p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&8[ECOS] &7已拒绝请求"));
            hub.refresh(p);
        });
        hub.registerAction("wp.add", (p, s, raw) ->
                plugin.getAnvilInputGUI().openForWaypointName(p));
        hub.registerAction("wp.del", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            boolean ok = plugin.getWaypointManager().remove(p.getUniqueId(), raw);
            p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                    ok ? "&8[ECOS] &7已删除 &f" + raw : "&8[ECOS] &7没有这个坐标"));
            hub.refresh(p);
        });
        hub.registerAction("mail.read", (p, s, raw) -> {
            int i = idx(raw);
            List<MailManager.Mail> box = plugin.getMailManager().getMails(p.getUniqueId());
            if (i < 0 || i >= box.size()) return;
            MailManager.Mail m = box.get(i);
            plugin.getMailManager().markRead(p.getUniqueId(), i);
            p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                    "&8[邮件] &f" + m.senderName() + " &8· &7" + m.date()));
            p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&7" + m.content()));
            hub.refresh(p);
        });
        hub.registerAction("mail.del", (p, s, raw) -> {
            plugin.getMailManager().delete(p.getUniqueId(), idx(raw));
            hub.refresh(p);
        });
        hub.registerAction("mail.write", (p, s, raw) -> {
            s.put("from", "mail-list");
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getPlayerSelectGUI().open(p, com.etherstories.escore.gui.PlayerSelectGUI.SelectContext.MAIL));
        });
        hub.registerAction("mail.name", (p, s, raw) ->
                plugin.getAnvilInputGUI().openForMailName(p));
        hub.registerAction("friend.share", (p, s, raw) -> {
            boolean on = plugin.getFriendManager().toggleLocationSharing(p.getUniqueId());
            p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                    on ? "&8[ECOS] &a已开位置分享" : "&8[ECOS] &7已关位置分享"));
            hub.refresh(p);
        });
        hub.registerAction("friend.card", (p, s, raw) -> {
            UUID id = parseUuid(raw);
            if (id == null) return;
            Player friend = Bukkit.getPlayer(id);
            if (friend == null || !friend.isOnline()) {
                p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&8[ECOS] &7对方不在线，无法发坐标"));
                return;
            }
            if (!plugin.getFriendManager().isLocationSharing(p.getUniqueId())) {
                p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&8[ECOS] &7先开「位置分享」再发坐标"));
                return;
            }
            var loc = p.getLocation();
            String world = loc.getWorld() != null ? loc.getWorld().getName() : "?";
            String line = "&b[坐标卡片] &f" + p.getName() + " &7→ &f"
                    + world + " &e" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
            p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&8[ECOS] &7已发给 &f" + friend.getName()));
            friend.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(line));
        });
        hub.registerAction("online.go", (p, s, raw) -> {
            Player t = raw == null ? null : Bukkit.getPlayerExact(raw);
            if (t == null) return;
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getPlayerActionGUI().open(p, t));
        });
        hub.registerAction("notice.read", (p, s, raw) -> {
            List<NoticeManager.Notice> all = new ArrayList<>(plugin.getNoticeManager().getNotices());
            Collections.reverse(all);
            int i = idx(raw);
            if (i < 0 || i >= all.size()) return;
            NoticeManager.Notice n = all.get(i);
            p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                    "&8[公告] &7" + n.date() + "  &f" + n.author()));
            p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&7" + n.content()));
        });
        hub.registerAction("wp.go", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            for (WaypointManager.Waypoint wp : plugin.getWaypointManager().get(p.getUniqueId())) {
                if (!wp.name().equals(raw)) continue;
                World world = Bukkit.getWorld(wp.world());
                if (world == null) {
                    p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&8[ECOS] &7世界不存在"));
                    return;
                }
                p.teleport(new Location(world, wp.x() + 0.5, wp.y(), wp.z() + 0.5));
                p.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&8[ECOS] &7已到 &f" + wp.name()));
                return;
            }
        });
    }

    private static int idx(String raw) {
        try { return Integer.parseInt(raw.trim()); }
        catch (Exception e) { return -1; }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return UUID.fromString(raw.trim()); }
        catch (Exception e) { return null; }
    }

    private interface Buttons {
        List<TerminalButton> get(Player player, TerminalSession session);
    }

    private static TerminalPage hidden(String id, String label, Buttons buttons) {
        return new TerminalPage() {
            @Override public String id() { return id; }
            @Override public String label() { return label; }
            @Override public boolean listed(Player player) { return false; }
            @Override public List<TerminalButton> buttons(Player player, TerminalSession session) {
                return buttons.get(player, session);
            }
        };
    }

    private static List<TerminalButton> homes(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        if (!plugin.getEssentialsHook().isEnabled()) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "需要 EssentialsX"));
            return out;
        }
        List<String> homes = plugin.getEssentialsHook().getHomes(player);
        if (homes.isEmpty()) {
            out.add(TerminalButton.of("sethome", EcosStyle.BOOK_ORANGE + "还没有家 · 设家"));
        } else {
            for (String name : homes) {
                out.add(TerminalButton.of("home.go",
                        EcosStyle.BOOK_COBALT + "▸ " + name,
                        "传送到 " + name, name));
            }
        }
        return out;
    }

    private static List<TerminalButton> friends(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("friend.add", EcosStyle.BOOK_JIQING + "加好友", "聊天输入游戏名"));
        boolean sharing = plugin.getFriendManager().isLocationSharing(player.getUniqueId());
        out.add(TerminalButton.of("friend.share",
                sharing ? EcosStyle.BOOK_JIQING + "位置分享开" : EcosStyle.BOOK_MUTED + "位置分享关",
                "好友可见你的坐标"));
        List<UUID> reqs = plugin.getFriendManager().getPendingRequests(player.getUniqueId());
        for (UUID id : reqs) {
            var op = Bukkit.getOfflinePlayer(id);
            String name = op.getName() == null ? "?" : op.getName();
            out.add(TerminalButton.of("friend.acc", EcosStyle.BOOK_ORANGE + "接受 " + name, "通过请求", id.toString()));
            out.add(TerminalButton.of("friend.den", EcosStyle.BOOK_MUTED + "拒绝 " + name, "忽略", id.toString()));
        }
        List<UUID> list = plugin.getFriendManager().getFriends(player.getUniqueId());
        if (list.isEmpty() && reqs.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "还没有好友"));
        for (UUID id : list) {
            var op = Bukkit.getOfflinePlayer(id);
            String name = op.getName() == null ? "?" : op.getName();
            Player online = Bukkit.getPlayer(id);
            out.add(TerminalButton.of("friend.go",
                    (online != null ? EcosStyle.BOOK_COBALT : EcosStyle.BOOK_MUTED) + "▸ " + name,
                    online != null ? "在线 · 打开操作" : "离线 · 转账/写信/删好友", id.toString()));
            if (online != null)
                out.add(TerminalButton.of("friend.card", EcosStyle.BOOK_MUTED + "发坐标 " + name,
                        "发给对方", id.toString()));
        }
        return out;
    }

    private static List<TerminalButton> mail(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("mail.write", EcosStyle.BOOK_JIQING + "写信", "选在线或输入名字"));
        out.add(TerminalButton.of("mail.name", EcosStyle.BOOK_ORANGE + "输入名字", "离线也可写"));
        List<MailManager.Mail> box = plugin.getMailManager().getMails(player.getUniqueId());
        if (box.isEmpty()) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "没有邮件"));
            return out;
        }
        for (int i = box.size() - 1; i >= 0; i--) {
            MailManager.Mail m = box.get(i);
            String preview = m.content().replace('\n', ' ');
            if (preview.length() > 20) preview = preview.substring(0, 20) + "…";
            out.add(TerminalButton.of("mail.read",
                    (m.read() ? EcosStyle.BOOK_MUTED : EcosStyle.BOOK_ORANGE) + "▸ " + m.senderName(),
                    m.date() + "  " + preview, String.valueOf(i)));
            out.add(TerminalButton.of("mail.del", EcosStyle.BOOK_MUTED + "删", "删除此信", String.valueOf(i)));
        }
        return out;
    }

    private static List<TerminalButton> online(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        int n = 0;
        for (Player t : Bukkit.getOnlinePlayers()) {
            if (t.equals(player)) continue;
            out.add(TerminalButton.of("online.go",
                    EcosStyle.BOOK_COBALT + "▸ " + t.getName(),
                    t.getPing() + "ms", t.getName()));
            n++;
        }
        if (n == 0) out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "没有其他人"));
        return out;
    }

    private static List<TerminalButton> notices(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        List<NoticeManager.Notice> all = new ArrayList<>(plugin.getNoticeManager().getNotices());
        Collections.reverse(all);
        if (all.isEmpty()) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "暂无公告"));
            return out;
        }
        for (int i = 0; i < all.size(); i++) {
            NoticeManager.Notice n = all.get(i);
            out.add(TerminalButton.of("notice.read",
                    EcosStyle.BOOK_INK + "▸ " + n.author(),
                    n.date() + "  点看全文", String.valueOf(i)));
        }
        return out;
    }

    private static List<TerminalButton> waypoints(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("wp.add", EcosStyle.BOOK_JIQING + "收藏脚下", "聊天输入名称"));
        List<WaypointManager.Waypoint> list = plugin.getWaypointManager().get(player.getUniqueId());
        if (list.isEmpty()) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "还没有坐标"));
            return out;
        }
        for (WaypointManager.Waypoint wp : list) {
            out.add(TerminalButton.of("wp.go",
                    EcosStyle.BOOK_COBALT + "▸ " + wp.name(),
                    wp.world() + "  " + wp.x() + "," + wp.y() + "," + wp.z(), wp.name()));
            out.add(TerminalButton.of("wp.del", EcosStyle.BOOK_MUTED + "删 " + wp.name(), "删除此坐标", wp.name()));
        }
        return out;
    }

    private static TerminalButton back() {
        return TerminalButton.of("list.back", EcosStyle.BOOK_JIQING + "← 返回");
    }
}
