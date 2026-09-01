package com.etherstories.escore.gui.terminal;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.gui.EcosStyle;
import com.etherstories.escore.managers.DeathManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

final class BuiltinPages {
    private BuiltinPages() {}

    static void register(TerminalHub hub, ES2UniPlugin plugin) {
        hub.registerPage(new TerminalPage() {
            @Override public String id() { return "overview"; }
            @Override public String label() { return "概览"; }
            @Override public String hint() { return "签到 · 邮件 · 钱"; }
            @Override public List<String> header(Player player, TerminalSession session) {
                return overviewHeader(plugin, player);
            }
            @Override public List<TerminalButton> buttons(Player player, TerminalSession session) {
                return overview(plugin, player);
            }
        });
        hub.registerPage(page("social", "社交", "好友 · 在线 · 公告",
                (p, s) -> social(plugin, p)));
        hub.registerPage(page("travel", "出行", "家 · 传送 · 领地",
                (p, s) -> travel(plugin, p)));
        hub.registerPage(page("city", "城市", "房产 · 酒店 · 市政",
                (p, s) -> city(plugin, p)));
        hub.registerPage(page("play", "娱乐", "点歌 · 活动 · 特效",
                (p, s) -> play(plugin, p)));
        hub.registerPage(new TerminalPage() {
            @Override public String id() { return "admin"; }
            @Override public String label() { return "管理"; }
            @Override public String hint() { return "活动 · 广播 · 税收"; }
            @Override public boolean visible(Player player) {
                return player.hasPermission("es2uni.admin");
            }
            @Override public List<TerminalButton> buttons(Player player, TerminalSession session) {
                return admin(plugin, player);
            }
        });
    }

    private interface Buttons {
        List<TerminalButton> get(Player player, TerminalSession session);
    }

    private static TerminalPage page(String id, String label, String hint, Buttons buttons) {
        return new TerminalPage() {
            @Override public String id() { return id; }
            @Override public String label() { return label; }
            @Override public String hint() { return hint; }
            @Override public List<TerminalButton> buttons(Player player, TerminalSession session) {
                return buttons.get(player, session);
            }
        };
    }

    private static List<String> overviewHeader(ES2UniPlugin plugin, Player player) {
        boolean checked = plugin.getCheckInManager().hasCheckedInToday(player.getUniqueId());
        int streak = plugin.getCheckInManager().getStreak(player.getUniqueId());
        int unread = plugin.getMailManager().unreadCount(player.getUniqueId());
        String stay = plugin.getHotelManager() == null ? null : plugin.getHotelManager().stayLabel(player.getUniqueId());
        String pt = plugin.getPlaytimeManager().getFormatted(player);
        String journey = plugin.getTransitManager() == null ? null
                : plugin.getTransitManager().journeyHint(player.getUniqueId());
        List<String> h = new ArrayList<>();
        h.add(EcosStyle.BOOK_MUTED + EcosStyle.hello(player.getName()));
        h.add(EcosStyle.BOOK_INK + "签到  "
                + (checked ? EcosStyle.BOOK_JIQING + "已签" : EcosStyle.BOOK_ORANGE + "未签")
                + EcosStyle.BOOK_MUTED + "  ·  连" + streak);
        h.add(EcosStyle.BOOK_INK + "邮件  "
                + (unread > 0 ? EcosStyle.BOOK_ORANGE + unread + " 未读" : EcosStyle.BOOK_MUTED + "无未读"));
        if (stay != null) h.add(EcosStyle.BOOK_ORANGE + "入住  " + stay);
        else h.add(EcosStyle.BOOK_INK + "在线  " + EcosStyle.BOOK_COBALT + Bukkit.getOnlinePlayers().size());
        h.add(journey != null
                ? EcosStyle.BOOK_JIQING + journey
                : EcosStyle.BOOK_INK + "时长  " + EcosStyle.BOOK_MUTED + pt);
        h.add("");
        return h;
    }

    private static List<TerminalButton> overview(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        boolean checked = plugin.getCheckInManager().hasCheckedInToday(player.getUniqueId());
        int streak = plugin.getCheckInManager().getStreak(player.getUniqueId());
        int unread = plugin.getMailManager().unreadCount(player.getUniqueId());
        String stay = plugin.getHotelManager() == null ? null : plugin.getHotelManager().stayLabel(player.getUniqueId());

        out.add(TerminalButton.of("checkin",
                checked ? EcosStyle.BOOK_JIQING + "签到" : EcosStyle.BOOK_ORANGE + "签到",
                checked ? "已签 · 连" + streak + " · 月历" : "去签到 · 连" + streak));
        out.add(TerminalButton.of("mail",
                unread > 0 ? EcosStyle.BOOK_ORANGE + "邮箱" : EcosStyle.BOOK_COBALT + "邮箱",
                unread > 0 ? unread + " 未读" : "没有未读"));
        if (plugin.getVaultHook().isEnabled()) {
            out.add(TerminalButton.of("pay", EcosStyle.BOOK_ORANGE + "转账",
                    plugin.getVaultHook().format(plugin.getVaultHook().getBalance(player))));
        } else {
            out.add(TerminalButton.of("balance", EcosStyle.BOOK_MUTED + "无经济", "需要 Vault"));
        }
        out.add(TerminalButton.of("web-pair", EcosStyle.BOOK_JIQING + "网页", "连接码两分钟"));
        if (stay != null) {
            out.add(TerminalButton.of("hotel-checkout", EcosStyle.BOOK_ORANGE + "退房", stay));
            out.add(TerminalButton.of("hotel", EcosStyle.BOOK_COBALT + "酒店", "打开列表"));
        }
        boolean afk = plugin.getAfkManager().isAFK(player);
        out.add(TerminalButton.of("afk",
                afk ? EcosStyle.BOOK_ORANGE + "AFK" : EcosStyle.BOOK_MUTED + "AFK",
                player.getPing() + "ms"));
        if (plugin.getNewbieGuideManager().shouldShow(player)
                || !plugin.getNewbieGuideManager().isComplete(player.getUniqueId())) {
            out.add(TerminalButton.of("newbie",
                    EcosStyle.BOOK_JIQING + "引导 " + plugin.getNewbieGuideManager().progress(player.getUniqueId()) + "%",
                    "还没做完"));
        }
        return out;
    }

    private static List<TerminalButton> social(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        int friends = plugin.getFriendManager().getFriends(player.getUniqueId()).size();
        int reqs = plugin.getFriendManager().getPendingRequests(player.getUniqueId()).size();
        int unread = plugin.getMailManager().unreadCount(player.getUniqueId());
        int total = plugin.getMailManager().getMails(player.getUniqueId()).size();
        String status = plugin.getStatusManager().getStatus(player.getUniqueId());
        out.add(TerminalButton.of("friends",
                reqs > 0 ? EcosStyle.BOOK_ORANGE + "好友" : EcosStyle.BOOK_COBALT + "好友",
                friends + " 人" + (reqs > 0 ? "  待处理 " + reqs : "")));
        out.add(TerminalButton.of("mail",
                unread > 0 ? EcosStyle.BOOK_ORANGE + "邮箱" : EcosStyle.BOOK_COBALT + "邮箱",
                "未读 " + unread + " / " + total));
        out.add(TerminalButton.of("online", EcosStyle.BOOK_JIQING + "在线",
                "其他 " + Math.max(0, Bukkit.getOnlinePlayers().size() - 1) + " 人"));
        out.add(TerminalButton.of("notices", EcosStyle.BOOK_INK + "公告",
                plugin.getNoticeManager().getNotices().size() + " 条"));
        out.add(TerminalButton.of("leaderboard", EcosStyle.BOOK_ORANGE + "时长榜", "打开"));
        out.add(TerminalButton.of("status", EcosStyle.BOOK_COBALT + "签名",
                status != null ? status : "未设置"));
        return out;
    }

    private static List<TerminalButton> travel(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        if (plugin.getEssentialsHook().isEnabled()) {
            out.add(TerminalButton.of("home", EcosStyle.BOOK_COBALT + "家",
                    plugin.getEssentialsHook().getHomes(player).size() + " 个"));
            out.add(TerminalButton.of("sethome", EcosStyle.BOOK_JIQING + "设家", "在此设家"));
            out.add(TerminalButton.of("tpa", EcosStyle.BOOK_ORANGE + "传送", "TPA 到别人那里"));
            out.add(TerminalButton.of("tpahere", EcosStyle.BOOK_ORANGE + "召唤", "叫别人过来"));
        } else {
            out.add(TerminalButton.of("home", EcosStyle.BOOK_MUTED + "家", "需要 EssentialsX"));
            out.add(TerminalButton.of("tpa", EcosStyle.BOOK_MUTED + "传送", "需要 EssentialsX"));
            out.add(TerminalButton.of("tpahere", EcosStyle.BOOK_MUTED + "召唤", "需要 EssentialsX"));
        }
        out.add(TerminalButton.of("waypoints", EcosStyle.BOOK_COBALT + "坐标",
                plugin.getWaypointManager().get(player.getUniqueId()).size()
                        + " / " + plugin.getWaypointManager().maxPerPlayer()));
        DeathManager.DeathRecord last = plugin.getDeathManager().getLast(player.getUniqueId());
        out.add(TerminalButton.of("death", EcosStyle.BOOK_MUTED + "死亡点",
                last == null ? "还没有"
                        : last.date() + "  " + last.world() + " "
                        + last.x() + "," + last.y() + "," + last.z()));
        out.add(TerminalButton.of("my-territory", EcosStyle.BOOK_JIQING + "领地",
                plugin.getRegionManager().getByOwner(player.getUniqueId()).size() + " 块"));
        out.add(TerminalButton.of("hot-places", EcosStyle.BOOK_ORANGE + "热门",
                "公开 " + plugin.getRegionManager().getPublicTerritories().size()));
        return out;
    }

    private static List<TerminalButton> city(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        int rooms = plugin.getEstateManager() == null ? 0
                : plugin.getEstateManager().ownedBy(player.getUniqueId()).size();
        int sale = plugin.getEstateManager() == null ? 0
                : plugin.getEstateManager().listedForSale().size();
        out.add(TerminalButton.of("estate", EcosStyle.BOOK_JIQING + "房产",
                "我的 " + rooms + "  待售 " + sale));
        String stay = plugin.getHotelManager() == null ? null : plugin.getHotelManager().stayLabel(player.getUniqueId());
        int hotels = plugin.getHotelManager() == null ? 0 : plugin.getHotelManager().all().size();
        if (stay != null) {
            out.add(TerminalButton.of("hotel-checkout", EcosStyle.BOOK_ORANGE + "退房", stay));
            out.add(TerminalButton.of("hotel", EcosStyle.BOOK_COBALT + "酒店", hotels + " 家"));
        } else {
            out.add(TerminalButton.of("hotel", EcosStyle.BOOK_COBALT + "酒店", hotels + " 家"));
        }
        int jobs = plugin.getJobBoardManager().listActive().size();
        out.add(TerminalButton.of("municipal", EcosStyle.BOOK_COBALT + "市政",
                "招工 · 保险 · 交通  委托 " + jobs));
        out.add(TerminalButton.of("kit", EcosStyle.BOOK_ORANGE + "Kit", "新手包 / 礼包"));
        boolean luckyDone = !plugin.getLuckyBlockManager().canDraw(player.getUniqueId());
        int tickets = plugin.getLuckyBlockManager().getTickets(player.getUniqueId());
        String luckyName = plugin.getLuckyBlockManager().displayName();
        out.add(TerminalButton.of("lucky",
                luckyDone ? EcosStyle.BOOK_MUTED + luckyName : EcosStyle.BOOK_ORANGE + luckyName,
                "额外 " + tickets + (luckyDone && tickets <= 0 ? "  明天再来" : "")));
        var trade = plugin.getTradeStatsManager();
        String vol = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(trade.getTotalVolume())
                : String.format("%.2f", trade.getTotalVolume());
        out.add(TerminalButton.of("trade", EcosStyle.BOOK_ORANGE + "交易榜", "今日 " + vol));
        out.add(TerminalButton.of("showcase", EcosStyle.BOOK_JIQING + "展示",
                plugin.getShowcaseManager().all().size() + " 处"));
        return out;
    }

    private static List<TerminalButton> play(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        plugin.getAllMusicHook().ensureHooked(plugin.getLogger());
        out.add(TerminalButton.of("music", EcosStyle.BOOK_COBALT + "点歌",
                plugin.getAllMusicHook().isAvailable() ? "AllMusic 已连接" : "AllMusic 未加载"));
        if (plugin.getAllMusicHook().isAvailable()) {
            boolean muted = plugin.getAllMusicHook().isMuted(player);
            out.add(TerminalButton.of("music-listen",
                    muted ? EcosStyle.BOOK_ORANGE + "听歌关" : EcosStyle.BOOK_JIQING + "听歌开",
                    muted ? "开启" : "关闭"));
        } else {
            out.add(TerminalButton.of("music-listen", EcosStyle.BOOK_MUTED + "听歌", "AllMusic 未加载"));
        }
        var curAura = plugin.getAuraManager().getEquipped(player.getUniqueId());
        out.add(TerminalButton.of("aura", EcosStyle.BOOK_COBALT + "特效",
                "已拥有 " + plugin.getAuraManager().getOwned(player.getUniqueId()).size()
                        + " / " + com.etherstories.escore.auras.AuraType.values().length
                        + (curAura != null ? "  装备 " + curAura.displayName() : "  未装备")));
        boolean linkOn = Bukkit.getPluginManager().getPlugin("ESLink") != null
                && Bukkit.getPluginManager().getPlugin("ESLink").isEnabled();
        out.add(TerminalButton.of("link",
                linkOn ? EcosStyle.BOOK_JIQING + "互通" : EcosStyle.BOOK_MUTED + "互通",
                linkOn ? "市场 / 箱子 / 红石" : "未装 ESLink"));
        out.add(TerminalButton.of("events", EcosStyle.BOOK_ORANGE + "活动",
                plugin.getEventManager().getAllEvents().size() + " 个"));
        int listed = plugin.getSkillShopManager() == null ? 0
                : plugin.getSkillShopManager().forSale().size();
        out.add(TerminalButton.of("skillshop", EcosStyle.BOOK_COBALT + "技能店", "在售 " + listed));
        return out;
    }

    private static List<TerminalButton> admin(ES2UniPlugin plugin, Player player) {
        List<TerminalButton> out = new ArrayList<>();
        var tax = plugin.getTaxManager();
        out.add(TerminalButton.of("adm-events", EcosStyle.BOOK_ORANGE + "活动", "打开"));
        out.add(TerminalButton.of("adm-bc", EcosStyle.BOOK_ORANGE + "广播", "打开"));
        out.add(TerminalButton.of("adm-reload", EcosStyle.BOOK_MUTED + "重载", "执行"));
        out.add(TerminalButton.of("adm-territory", EcosStyle.BOOK_COBALT + "领地",
                plugin.getRegionManager().getAllTerritories().size() + " 块"));
        out.add(TerminalButton.of("adm-regions", EcosStyle.BOOK_COBALT + "地区",
                plugin.getRegionManager().getDistricts().size() + " 个"));
        out.add(TerminalButton.of("adm-kit", EcosStyle.BOOK_ORANGE + "Kit", "打开"));
        out.add(TerminalButton.of("adm-tax", EcosStyle.BOOK_INK + "税收",
                "转账 " + (tax.isPayTaxEnabled() ? "开" : "关")
                        + "  箱子店 " + (tax.isQuickShopTaxEnabled() ? "开" : "关")));
        out.add(TerminalButton.of("adm-estate", EcosStyle.BOOK_JIQING + "房产",
                plugin.getEstateManager().all().size() + " 间"));
        out.add(TerminalButton.of("adm-grant", EcosStyle.BOOK_ORANGE + "发放", "补签券 / 幸运方块"));
        out.add(TerminalButton.of("adm-skillshop", EcosStyle.BOOK_COBALT + "技能上架",
                "在售 " + plugin.getSkillShopManager().forSale().size()));
        return out;
    }
}
