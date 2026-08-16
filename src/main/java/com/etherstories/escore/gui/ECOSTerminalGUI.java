package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.DeathManager;
import com.etherstories.escore.utils.ColorUtil;
import com.etherstories.escore.utils.TPSUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class ECOSTerminalGUI {

    // ── Slot map ──────────────────────────────────────────────────────────────
    //  Row 1 (0-8)   : Black glass border
    //  Row 2 (9-17)  : Status — TPS | Events | Balance | AFK
    //  Row 3 (18-26) : Cyan separator "TELEPORT"
    //  Row 4 (27-35) : Home | TPA | TPAHere | Pay
    //  Row 5 (36-44) : Gray separator "SYSTEM"
    //  Row 6 (45-53) : Admin: Events | Broadcast | Reload   | Version | Close

    public static final int SLOT_HEAD       =  4;
    public static final int SLOT_TPS        = 10;
    public static final int SLOT_EVENTS     = 12;
    public static final int SLOT_BALANCE    = 14;
    public static final int SLOT_AFK        = 16;
    public static final int SLOT_FRIENDS    =  9;
    public static final int SLOT_MAIL       = 11;
    public static final int SLOT_ONLINE     = 13;
    public static final int SLOT_NOTICES    = 15;
    public static final int SLOT_LEADERBOARD= 17;

    public static final int SLOT_SUMMARY     = 19; // 今日摘要
    public static final int SLOT_LUCKY       = 21; // 幸运方块
    public static final int SLOT_TRADE_BOARD = 23; // 今日交易榜
    public static final int SLOT_SHOWCASE    = 25; // 机器展示
    public static final int SLOT_MUNICIPAL   = 24; // 管理处
    public static final int SLOT_NEWBIE      = 20; // 新手引导
    public static final int SLOT_CHECKIN     = 27;
    public static final int SLOT_HOME       = 28;
    public static final int SLOT_WAYPOINTS  = 29;
    public static final int SLOT_TPA        = 30;
    public static final int SLOT_DEATH_LOG  = 31;
    public static final int SLOT_TPAHERE    = 32;
    public static final int SLOT_STATUS     = 33;
    public static final int SLOT_PAY        = 34;
    public static final int SLOT_MY_TERRITORY = 35;

    public static final int SLOT_ADM_EVENTS    = 45;
    public static final int SLOT_ADM_BC        = 46;
    public static final int SLOT_ADM_RELOAD    = 47;
    public static final int SLOT_ADM_TERRITORY = 48;
    public static final int SLOT_HOT_PLACES    = 49;
    public static final int SLOT_ADM_REGIONS   = 50;
    public static final int SLOT_KIT           = 51;
    public static final int SLOT_AURA_SHOP     = 52;
    public static final int SLOT_MUSIC         = 41; // 点歌
    public static final int SLOT_MUSIC_LISTEN  = 42; // 听歌开关
    public static final int SLOT_LINK          = 38; // 互通大厅（ESLink）
    public static final int SLOT_ADM_KIT       = 43; // Kit 管理
    public static final int SLOT_ADM_TAX       = 44; // 税收
    public static final int SLOT_CLOSE         = 53;

    private static final int SIZE      = 54;
    private static final Material BG   = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material SEP1 = Material.CYAN_STAINED_GLASS_PANE;
    private static final Material SEP2 = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin plugin;

    public ECOSTerminalGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = ColorUtil.colorize(plugin.getConfigManager().getTerminalTitle());
        Inventory inv = Bukkit.createInventory(null, SIZE, title);

        // Background
        ItemStack bg   = bg(BG);
        ItemStack sep1 = bg(SEP1);
        ItemStack sep2 = bg(SEP2);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, bg);
        for (int i = 18; i < 27; i++) inv.setItem(i, sep1);
        for (int i = 36; i < 45; i++) inv.setItem(i, sep2);

        // ── Slot 4: Player Head ───────────────────────────────────────────────
        String pt = plugin.getPlaytimeManager().getFormatted(player);
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(ColorUtil.colorize("&f&l" + player.getName()));
            skullMeta.setLore(List.of(
                    ColorUtil.colorize(" &7在线时长: &f" + pt),
                    ColorUtil.colorize(" &7延迟:    &f" + player.getPing() + "ms"),
                    ColorUtil.colorize(" &7插件:    &f" + plugin.getDescription().getVersion()),
                    ColorUtil.colorize(""),
                    ColorUtil.colorize("&7▸ 点击查看版本")
            ));
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(SLOT_HEAD, skull);

        // Section labels + 今日摘要 / 交易榜
        inv.setItem(22, label(Material.ENDER_EYE,       "&b▌ TELEPORT &8/ ECONOMY"));
        inv.setItem(40, label(Material.COMMAND_BLOCK,   "&c▌ SYSTEM &8/ ADMIN"));

        boolean checkedSummary = plugin.getCheckInManager().hasCheckedInToday(player.getUniqueId());
        int streakSummary = plugin.getCheckInManager().getStreak(player.getUniqueId());
        int unreadSummary = plugin.getMailManager().unreadCount(player.getUniqueId());
        var trade = plugin.getTradeStatsManager();
        String volStr = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(trade.getTotalVolume())
                : String.format("%.2f", trade.getTotalVolume());
        var top1 = trade.getTopPlayers(1);
        String topStr = top1.isEmpty() ? "暂无" : top1.get(0).name();
        inv.setItem(SLOT_SUMMARY, item(Material.BOOK,
                "&b今日摘要",
                List.of(
                        " &7签到: " + (checkedSummary ? "&a已签" : "&e未签") + " &8(连" + streakSummary + ")",
                        " &7未读邮件: &f" + unreadSummary,
                        " &7今日交易总量: &f" + volStr,
                        " &7交易最高: &f" + topStr,
                        plugin.getTransitManager() != null && plugin.getTransitManager().journeyHint(player.getUniqueId()) != null
                                ? " &b" + plugin.getTransitManager().journeyHint(player.getUniqueId())
                                : " &7在线: &f" + Bukkit.getOnlinePlayers().size(),
                        " &7TPS: &f" + TPSUtil.formatTPS(TPSUtil.getTPS()),
                        "",
                        "&b▸ 点击刷新查看"
                )));
        inv.setItem(SLOT_TRADE_BOARD, item(Material.EMERALD,
                "&6今日交易榜",
                List.of(" &7总量: &f" + volStr,
                        " &7税收: &e" + (plugin.getVaultHook().isEnabled()
                                ? plugin.getVaultHook().format(trade.getTaxCollected())
                                : String.format("%.2f", trade.getTaxCollected())),
                        "",
                        "&6▸ 点击打开")));

        boolean luckyDone = !plugin.getLuckyBlockManager().canDraw(player.getUniqueId());
        int tickets = plugin.getLuckyBlockManager().getTickets(player.getUniqueId());
        String luckyName = plugin.getLuckyBlockManager().displayName();
        inv.setItem(SLOT_LUCKY, item(
                luckyDone ? Material.GRAY_CONCRETE : Material.GOLD_BLOCK,
                luckyDone ? "&8" + luckyName + " &7[今日已抽]" : "&d" + luckyName,
                List.of(" &7每天一次 · 额外次数: &f" + tickets,
                        " &8音效: " + plugin.getLuckyBlockManager().fxStyle(),
                        "",
                        luckyDone && tickets <= 0 ? "&8明天再来 / 找管理要次数"
                                : "&d▸ 点击开启")));

        int scCount = plugin.getShowcaseManager().all().size();
        inv.setItem(SLOT_SHOWCASE, item(Material.CRAFTING_TABLE,
                "&d机器展示",
                List.of(" &7展示点: &f" + scCount,
                        " &7登记坐标 · 投票参观",
                        "",
                        "&d▸ 点击打开")));

        int openJobs = plugin.getJobBoardManager().listActive().size();
        inv.setItem(SLOT_MUNICIPAL, item(Material.BEACON,
                "&9管理处",
                List.of(" &7招工 · 清理 · 保险 · 交通",
                        " &7进行中委托: &f" + openJobs,
                        "",
                        "&9▸ 点击打开")));

        if (plugin.getNewbieGuideManager().shouldShow(player)
                || !plugin.getNewbieGuideManager().isComplete(player.getUniqueId())) {
            inv.setItem(SLOT_NEWBIE, item(Material.FILLED_MAP,
                    "&a新手引导 &8" + plugin.getNewbieGuideManager().progress(player.getUniqueId()) + "%",
                    List.of(" &7前 30 分钟上手清单",
                            "",
                            "&a▸ 点击打开")));
        }

        // ── Row 2: Status ─────────────────────────────────────────────────────
        double tps = TPSUtil.getTPS();

        int friendCount = plugin.getFriendManager().getFriends(player.getUniqueId()).size();
        int reqCount    = plugin.getFriendManager().getPendingRequests(player.getUniqueId()).size();
        ItemStack friendItem = item(Material.PLAYER_HEAD,
                "&7好友",
                List.of(" &7好友数: &f" + friendCount,
                        reqCount > 0 ? " &e待处理请求: &f" + reqCount : "",
                        "",
                        "&8点击打开"));
        if (reqCount > 0) friendItem = glint(friendItem);
        inv.setItem(SLOT_FRIENDS, friendItem);

        int unread = plugin.getMailManager().unreadCount(player.getUniqueId());
        int total  = plugin.getMailManager().getMails(player.getUniqueId()).size();
        ItemStack mailItem = item(Material.WRITABLE_BOOK,
                "&7邮箱",
                List.of(" &7未读: &f" + unread + "  &8/ " + total,
                        "",
                        "&8点击打开"));
        if (unread > 0) mailItem = glint(mailItem);
        inv.setItem(SLOT_MAIL, mailItem);
        inv.setItem(SLOT_TPS, item(Material.COMPARATOR,
                "&b性能监控",
                List.of(" &7TPS  &f" + TPSUtil.formatTPS(tps),
                        " &7MSPT &f" + TPSUtil.formatMSPT(TPSUtil.getMSPT()),
                        " &7在线 &f" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers(),
                        "",
                        "&8▸ 点击查看详情")));

        int evCount = plugin.getEventManager().getAllEvents().size();
        inv.setItem(SLOT_EVENTS, item(Material.BOOK,
                "&e活动中心",
                List.of(" &7当前活动: &f" + evCount,
                        "",
                        "&e▸ 点击打开活动列表")));

        if (plugin.getVaultHook().isEnabled()) {
            double bal = plugin.getVaultHook().getBalance(player);
            inv.setItem(SLOT_BALANCE, item(Material.GOLD_INGOT,
                    "&6余额查询",
                    List.of(" &7余额: &a" + plugin.getVaultHook().format(bal),
                            "",
                            "&8▸ 仅显示")));
        } else {
            inv.setItem(SLOT_BALANCE, item(Material.BARRIER, "&8经济系统不可用",
                    List.of("&8需要安装 Vault")));
        }

        boolean isAfk = plugin.getAfkManager().isAFK(player);
        inv.setItem(SLOT_AFK, item(
                isAfk ? Material.RED_BED : Material.LIME_BED,
                isAfk ? "&cAFK &8[激活]" : "&aAFK &8[关闭]",
                List.of(" &7在线时长: &f" + pt,
                        " &7延迟: &f" + player.getPing() + "ms",
                        "",
                        isAfk ? "&8▸ 点击: 取消 AFK" : "&8▸ 点击: 标记 AFK")));

        int onlineCount = Bukkit.getOnlinePlayers().size() - 1;
        inv.setItem(SLOT_ONLINE, item(Material.COMPASS,
                "&7在线玩家",
                List.of(" &7其他在线: &f" + onlineCount,
                        "",
                        "&8点击打开")));

        int noticeCount = plugin.getNoticeManager().getNotices().size();
        inv.setItem(SLOT_NOTICES, item(Material.PAPER,
                "&7公告板",
                List.of(" &7公告数: &f" + noticeCount,
                        "",
                        "&8点击查看")));

        inv.setItem(SLOT_LEADERBOARD, item(Material.GOLDEN_HELMET,
                "&7在线时长排行",
                List.of("",
                        "&8点击查看")));

        // ── Row 4: Teleport & Economy ─────────────────────────────────────────

        boolean checkedIn = plugin.getCheckInManager().hasCheckedInToday(player.getUniqueId());
        int streak = plugin.getCheckInManager().getStreak(player.getUniqueId());
        inv.setItem(SLOT_CHECKIN, item(
                checkedIn ? Material.LIME_CONCRETE : Material.YELLOW_CONCRETE,
                checkedIn ? "&a签到日历 &8[已完成]" : "&e签到日历",
                List.of(" &7连续签到: &f" + streak + " 天",
                        "",
                        "&8▸ 点击打开月历")));
        if (plugin.getEssentialsHook().isEnabled()) {
            int homeCount = plugin.getEssentialsHook().getHomes(player).size();
            inv.setItem(SLOT_HOME, item(Material.RED_BED,
                    "&d家园传送",
                    List.of(" &7已有家园: &f" + homeCount,
                            " &7右键: 在此设置家",
                            "",
                            "&d▸ 左键: 打开家园列表")));

            inv.setItem(SLOT_TPA, item(Material.ENDER_PEARL,
                    "&6传送请求 &8(TPA)",
                    List.of(" &7向玩家请求传送至其位置",
                            "",
                            "&6▸ 点击选择目标玩家")));

            inv.setItem(SLOT_TPAHERE, item(Material.ENDER_EYE,
                    "&6召唤请求 &8(TPA Here)",
                    List.of(" &7请求玩家传送至你的位置",
                            "",
                            "&6▸ 点击选择目标玩家")));
        } else {
            ItemStack na = item(Material.BARRIER, "&8传送不可用", List.of("&8需要 EssentialsX"));
            inv.setItem(SLOT_HOME, na); inv.setItem(SLOT_TPA, na); inv.setItem(SLOT_TPAHERE, na);
        }

        // 坐标收藏
        int wpCount = plugin.getWaypointManager().get(player.getUniqueId()).size();
        inv.setItem(SLOT_WAYPOINTS, item(Material.COMPASS,
                "&7坐标收藏",
                List.of(" &7已收藏: &f" + wpCount + " &8/ " + plugin.getWaypointManager().maxPerPlayer(),
                        "", "&8点击打开")));

        // 死亡记录
        DeathManager.DeathRecord lastDeath = plugin.getDeathManager().getLast(player.getUniqueId());
        if (lastDeath != null) {
            inv.setItem(SLOT_DEATH_LOG, item(Material.BONE,
                    "&7死亡记录",
                    List.of(" &8" + lastDeath.date(),
                            " &8" + lastDeath.world() + "  " + lastDeath.x() + ", " + lastDeath.y() + ", " + lastDeath.z(),
                            " &8" + lastDeath.cause(),
                            "", "&8点击查看全部")));
        } else {
            inv.setItem(SLOT_DEATH_LOG, item(Material.BONE, "&7死亡记录",
                    List.of(" &8暂无记录")));
        }

        // 个人签名
        String status = plugin.getStatusManager().getStatus(player.getUniqueId());
        inv.setItem(SLOT_STATUS, item(Material.NAME_TAG,
                "&7个人签名",
                List.of(status != null ? " &f" + status : " &8未设置",
                        "", "&8点击修改")));

        if (plugin.getVaultHook().isEnabled()) {
            inv.setItem(SLOT_PAY, item(Material.SUNFLOWER,
                    "&a转账 &8(Pay)",
                    List.of(" &7向玩家转账（支持离线）",
                            " &7需二次确认",
                            "",
                            "&a▸ 点击选择收款人")));
        } else {
            inv.setItem(SLOT_PAY, item(Material.BARRIER, "&8转账不可用", List.of("&8需要 Vault")));
        }

        // 我的领地
        List<com.etherstories.escore.managers.RegionManager.Region> myTerr =
                plugin.getRegionManager().getByOwner(player.getUniqueId());
        long pubCount2 = myTerr.stream().filter(com.etherstories.escore.managers.RegionManager.Region::visible).count();
        inv.setItem(SLOT_MY_TERRITORY, item(Material.MAP,
                "&d我的领地",
                List.of(" &7领地数: &f" + myTerr.size(),
                        " &7公开: &f" + pubCount2 + "  &8隐藏: &f" + (myTerr.size() - pubCount2),
                        "",
                        "&d▸ 点击管理")));

        // ── Row 6: Admin / System ─────────────────────────────────────────────
        if (player.hasPermission("es2uni.admin")) {
            inv.setItem(SLOT_ADM_EVENTS, item(Material.WRITABLE_BOOK,
                    "&c活动管理",
                    List.of(" &7创建 / 删除活动", "", "&c▸ 点击打开")));

            inv.setItem(SLOT_ADM_BC, item(Material.BELL,
                    "&c定时广播",
                    List.of(" &7管理聊天栏自动消息",
                            " &7增删改 / 间隔 / 开关",
                            "", "&c▸ 点击打开")));

            inv.setItem(SLOT_ADM_RELOAD, item(Material.REDSTONE,
                    "&c重载配置",
                    List.of(" &7重新读取 config.yml", "", "&c▸ 点击执行")));

            int allTerr = plugin.getRegionManager().getAllTerritories().size();
            int pubTerr = plugin.getRegionManager().getPublicTerritories().size();
            inv.setItem(SLOT_ADM_TERRITORY, item(Material.FILLED_MAP,
                    "&c领地管理",
                    List.of(" &7领地总数: &f" + allTerr,
                            " &7公开: &f" + pubTerr + "  &8隐藏: &f" + (allTerr - pubTerr),
                            "", "&c▸ 点击打开")));

            int distCount = plugin.getRegionManager().getDistricts().size();
            inv.setItem(SLOT_ADM_REGIONS, item(Material.PAPER,
                    "&c地区管理",
                    List.of(" &7地区总数: &f" + distCount,
                            "", "&c▸ 点击打开")));

            inv.setItem(SLOT_ADM_KIT, item(Material.CHEST,
                    "&cKit 管理",
                    List.of(" &7新建 / 编辑 / 删除套件",
                            " &7设置一次领取或冷却",
                            "", "&c▸ 点击打开")));

            var tax = plugin.getTaxManager();
            inv.setItem(SLOT_ADM_TAX, item(Material.GOLD_INGOT,
                    "&c税收设置",
                    List.of(" &7转账: " + (tax.isPayTaxEnabled() ? "&a开 &f" + tax.formatRate(tax.getPayTaxRate()) : "&c关"),
                            " &7箱子店: " + (tax.isQuickShopTaxEnabled() ? "&a开 &f" + tax.formatRate(tax.getQuickShopTaxRate()) : "&c关"),
                            " &7互通: " + (plugin.getESLinkHook().present()
                                    ? (plugin.getESLinkHook().tradeEnabled()
                                    ? "&a开 &f" + plugin.getESLinkHook().formatRate()
                                    : "&c关")
                                    : "&8未装 ESLink"),
                            "", "&c▸ 点击打开")));
        }

        boolean linkOn = Bukkit.getPluginManager().getPlugin("ESLink") != null
                && Bukkit.getPluginManager().getPlugin("ESLink").isEnabled();
        inv.setItem(SLOT_LINK, item(linkOn ? Material.ENDER_CHEST : Material.GRAY_DYE,
                linkOn ? "&b互通大厅" : "&8互通大厅",
                List.of(linkOn ? " &7跨服市场、聊天、运输箱" : " &c未安装 ESLink",
                        " &8/link",
                        "",
                        linkOn ? "&b▸ 点击打开" : "&8需要 ESLink 插件")));

        inv.setItem(SLOT_MUSIC, item(Material.JUKEBOX,
                "&d✦ 点歌",
                List.of(" &7搜歌 / 点歌 / 切歌投票",
                        plugin.getAllMusicHook().isAvailable()
                                ? " &aAllMusic 已连接"
                                : " &cAllMusic 未加载",
                        "",
                        "&d▸ 点击打开")));

        // 听歌开关（AllMusic 静音，重进仍有效）
        plugin.getAllMusicHook().ensureHooked(plugin.getLogger());
        if (plugin.getAllMusicHook().isAvailable()) {
            boolean muted = plugin.getAllMusicHook().isMuted(player);
            inv.setItem(SLOT_MUSIC_LISTEN, item(
                    muted ? Material.RED_DYE : Material.LIME_DYE,
                    muted ? "&c听歌: 关" : "&a听歌: 开",
                    List.of(
                            muted
                                    ? " &7当前静音，收不到服务器点歌音频"
                                    : " &7当前收听服务器点歌",
                            " &8状态重进仍保留",
                            "",
                            muted ? "&a▸ 点击开启听歌" : "&c▸ 点击关闭听歌"
                    )));
        } else {
            inv.setItem(SLOT_MUSIC_LISTEN, item(Material.GRAY_DYE, "&8听歌开关",
                    List.of("&cAllMusic 未加载")));
        }

        // 热门地点（所有人可见）
        int pubCount = plugin.getRegionManager().getPublicTerritories().size();
        inv.setItem(SLOT_HOT_PLACES, item(Material.BEACON,
                "&6热门地点",
                List.of(" &7公开领地: &f" + pubCount + " 个",
                        "",
                        "&6▸ 点击传送")));

        inv.setItem(SLOT_KIT, item(Material.BUNDLE,
                "&6Kit 套件",
                List.of(" &7领取新手包 / 礼包",
                        " &8/ecos kit [名称]",
                        "",
                        "&6▸ 点击打开")));

        // 特效商店（所有人可见）
        com.etherstories.escore.auras.AuraType curAura =
                plugin.getAuraManager().getEquipped(player.getUniqueId());
        int ownedCount = plugin.getAuraManager().getOwned(player.getUniqueId()).size();
        inv.setItem(SLOT_AURA_SHOP, item(Material.AMETHYST_SHARD,
                "&b✦ 特效商店",
                List.of(" &7已拥有: &f" + ownedCount + " &8/ " + com.etherstories.escore.auras.AuraType.values().length,
                        curAura != null ? " &7装备中: &f" + curAura.displayName() : " &7未装备光环",
                        "",
                        "&b▸ 点击打开")));

        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&c&l关闭终端",
                List.of(" &8ES2UniPlugin v" + plugin.getDescription().getVersion())));

        player.openInventory(inv);
    }

    // ── Static helpers shared by other GUIs ───────────────────────────────────

    public static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(ColorUtil.colorize(name));
        if (lore != null)
            meta.setLore(lore.stream().map(ColorUtil::colorize).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    /** Add enchantment glint to an item (visual shimmer only, no enchant text shown). */
    public static ItemStack glint(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
        return stack;
    }

    static ItemStack bg(Material mat) {
        ItemStack s = new ItemStack(mat);
        ItemMeta m = s.getItemMeta();
        if (m != null) { m.setDisplayName(" "); s.setItemMeta(m); }
        return s;
    }

    private static ItemStack label(Material mat, String name) {
        return item(mat, name, null);
    }

    public static boolean isCloseSlot(int slot) { return slot == SLOT_CLOSE; }
}
