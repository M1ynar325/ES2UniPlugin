package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 垃圾实体清理申请：聊天投票；掉落物进回收站。 */
public class CleanupRequestManager {

    public static final class Request {
        public final String id;
        public final UUID requester;
        public final String requesterName;
        public final String world;
        public final int x, y, z;
        public final int radius;
        public final long createdAt;
        public final Set<UUID> yes = ConcurrentHashMap.newKeySet();
        public final Set<UUID> no = ConcurrentHashMap.newKeySet();
        public volatile boolean settled;

        Request(String id, Player p, int radius) {
            this.id = id;
            this.requester = p.getUniqueId();
            this.requesterName = p.getName();
            Location loc = p.getLocation();
            this.world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
            this.radius = radius;
            this.createdAt = System.currentTimeMillis();
        }
    }

    private final ES2UniPlugin plugin;
    private final Map<String, Request> active = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();
    private int nextId = 1;

    public CleanupRequestManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public int agreeNeed() {
        return plugin.getConfig().getInt("municipal.cleanup.agree-votes", 2);
    }

    public int refuseNeed() {
        return plugin.getConfig().getInt("municipal.cleanup.refuse-votes", 3);
    }

    public int defaultRadius() {
        return plugin.getConfig().getInt("municipal.cleanup.radius", 32);
    }

    public long cooldownMs() {
        return plugin.getConfig().getLong("municipal.cleanup.cooldown-seconds", 180) * 1000L;
    }

    public String request(Player player) {
        Long until = cooldownUntil.get(player.getUniqueId());
        if (until != null && until > System.currentTimeMillis()
                && !player.hasPermission("es2uni.admin")) {
            long left = (until - System.currentTimeMillis() + 999) / 1000;
            return "冷却中，还需 " + left + " 秒";
        }
        for (Request r : active.values()) {
            if (!r.settled && r.requester.equals(player.getUniqueId()))
                return "你已有进行中的清理申请";
        }

        String id = String.valueOf(nextId++);
        Request req = new Request(id, player, defaultRadius());
        active.put(id, req);
        cooldownUntil.put(player.getUniqueId(), System.currentTimeMillis() + cooldownMs());

        String loc = req.world + " " + req.x + " " + req.y + " " + req.z;
        Bukkit.broadcastMessage(ColorUtil.colorize(
                "&8[城管] &f" + player.getName()
                        + " &7申请清理附近掉落物/投射物 &8(r=" + req.radius + ") &7@ &f" + loc
                        + " &8#" + id));
        Bukkit.broadcastMessage(ColorUtil.colorize(
                "&8[城管] &7需 &a" + agreeNeed() + " &7人同意，或 &c" + refuseNeed()
                        + " &7人拒绝；掉落物进 &a回收站 &7(约1天)"));

        TextComponent yes = new TextComponent(ColorUtil.colorize("&a&l[同意清理] "));
        yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/ecos municipal cleanup yes " + id));
        yes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("同意清理 #" + id).create()));

        TextComponent no = new TextComponent(ColorUtil.colorize("&c&l[拒绝]"));
        no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/ecos municipal cleanup no " + id));
        no.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("拒绝清理 #" + id).create()));

        TextComponent line = new TextComponent("");
        line.addExtra(yes);
        line.addExtra(no);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(line);
        }

        long expire = plugin.getConfig().getLong("municipal.cleanup.expire-seconds", 120) * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Request r = active.get(id);
            if (r != null && !r.settled) {
                r.settled = true;
                active.remove(id);
                Bukkit.broadcastMessage(ColorUtil.colorize("&8[城管] &7清理申请 #" + id + " 已超时取消"));
            }
        }, expire);

        return null;
    }

    public String vote(Player voter, String id, boolean agree) {
        Request r = active.get(id);
        if (r == null || r.settled) return "申请不存在或已结束";
        if (r.requester.equals(voter.getUniqueId()) && !voter.hasPermission("es2uni.admin"))
            return "不能给自己的申请投票";
        r.yes.remove(voter.getUniqueId());
        r.no.remove(voter.getUniqueId());
        if (agree) r.yes.add(voter.getUniqueId());
        else r.no.add(voter.getUniqueId());

        // 管理员强制也广播
        if (voter.hasPermission("es2uni.admin") && agree) {
            Bukkit.broadcastMessage(ColorUtil.colorize(
                    "&8[城管] &c管理员 &f" + voter.getName() + " &7强制通过清理 #" + id));
            return execute(r, "管理员强制 · " + voter.getName());
        }

        if (r.yes.size() >= agreeNeed()) {
            Bukkit.broadcastMessage(ColorUtil.colorize(
                    "&8[城管] &a" + voter.getName() + " &7同意了清理 #" + id
                            + " &8(" + r.yes.size() + "/" + agreeNeed() + ") → 执行"));
            return execute(r, "投票通过 · 最后同意: " + voter.getName());
        }
        if (r.no.size() >= refuseNeed()) {
            r.settled = true;
            active.remove(id);
            Bukkit.broadcastMessage(ColorUtil.colorize(
                    "&8[城管] &c" + voter.getName() + " &7拒绝票使清理 #" + id
                            + " 失败（拒绝 " + r.no.size() + "/" + refuseNeed() + "）"));
            return null;
        }
        Bukkit.broadcastMessage(ColorUtil.colorize(
                "&8[城管] &f" + voter.getName()
                        + (agree ? " &a同意" : " &c拒绝")
                        + " &7了清理 #" + id
                        + " &8(同意 " + r.yes.size() + "/" + agreeNeed()
                        + " · 拒绝 " + r.no.size() + "/" + refuseNeed() + ")"));
        return "已记录你的投票";
    }

    private String execute(Request r, String reason) {
        if (r.settled) return "已结束";
        r.settled = true;
        active.remove(r.id);
        World w = Bukkit.getWorld(r.world);
        if (w == null) return "世界未加载";
        Location center = new Location(w, r.x + 0.5, r.y, r.z + 0.5);

        List<Item> drops = new ArrayList<>();
        int projectiles = 0;
        for (Entity e : w.getNearbyEntities(center, r.radius, r.radius, r.radius)) {
            if (e instanceof Item item) {
                drops.add(item);
            } else if (e instanceof Projectile) {
                e.remove();
                projectiles++;
            }
        }

        int stored = plugin.getRecycleBinManager().storeFromItems(
                drops, r.id, r.requester, r.requesterName);
        int deleted = 0;
        int leftover = 0;
        if (stored < 0) {
            // 回收站关闭：直接删除
            for (Item item : drops) {
                if (item.isValid()) { item.remove(); deleted++; }
            }
            stored = 0;
        } else {
            // 已入库的已 remove；满仓剩的留在地上
            for (Item item : drops) {
                if (item.isValid()) leftover++;
            }
            deleted = drops.size() - leftover;
        }

        String extra = leftover > 0
                ? " &c回收站已满，残留 &f" + leftover + " &c件未清理"
                : "";
        Bukkit.broadcastMessage(ColorUtil.colorize(
                "&8[城管] &a清理完成 &7#" + r.id + " &8(" + reason + ") &7掉落物 &f"
                        + drops.size() + " &7→回收站 &a" + stored
                        + " &7· 投射物 &f" + projectiles + extra));
        return null;
    }

    public void purgeExpired() {
        long maxAge = plugin.getConfig().getLong("municipal.cleanup.expire-seconds", 120) * 1000L;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Request>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            Request r = it.next().getValue();
            if (!r.settled && now - r.createdAt > maxAge) {
                r.settled = true;
                it.remove();
            }
        }
    }
}
