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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ECOSTerminalGUI {

    public enum Page {
        OVERVIEW("概览"), SOCIAL("社交"), TRAVEL("出行"),
        CITY("城市"), PLAY("娱乐"), ADMIN("管理");
        public final String label;
        Page(String label) { this.label = label; }
    }

    public static final int SLOT_TAB_OVERVIEW = 0;
    public static final int SLOT_TAB_SOCIAL   = 1;
    public static final int SLOT_TAB_TRAVEL   = 2;
    public static final int SLOT_TAB_CITY     = 3;
    public static final int SLOT_TAB_PLAY     = 4;
    public static final int SLOT_TAB_ADMIN    = 5;
    public static final int SLOT_HEAD         = 7;
    public static final int SLOT_CLOSE        = 8;

    private static final int SIZE = 27;
    private static final int[] CONTENT = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    private final ES2UniPlugin plugin;
    private final Map<UUID, Page> pages = new HashMap<>();
    private final Map<UUID, Map<Integer, String>> actions = new HashMap<>();

    public ECOSTerminalGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isTitle(String title) {
        if (title == null) return false;
        String s = org.bukkit.ChatColor.stripColor(title);
        return s != null && s.contains("[ ECOS");
    }

    public void open(Player player) {
        open(player, pages.getOrDefault(player.getUniqueId(), Page.OVERVIEW));
    }

    public void open(Player player, Page page) {
        if (page == Page.ADMIN && !player.hasPermission("es2uni.admin")) page = Page.OVERVIEW;
        pages.put(player.getUniqueId(), page);
        Map<Integer, String> act = new HashMap<>();
        actions.put(player.getUniqueId(), act);

        String title = ColorUtil.colorize("&0[ &b&lECOS &8" + page.label + " &0]");
        Inventory inv = Bukkit.createInventory(null, SIZE, title);

        putTab(inv, act, SLOT_TAB_OVERVIEW, Page.OVERVIEW, page, Material.BOOK, "概览");
        putTab(inv, act, SLOT_TAB_SOCIAL, Page.SOCIAL, page, Material.PLAYER_HEAD, "社交");
        putTab(inv, act, SLOT_TAB_TRAVEL, Page.TRAVEL, page, Material.ENDER_PEARL, "出行");
        putTab(inv, act, SLOT_TAB_CITY, Page.CITY, page, Material.BEACON, "城市");
        putTab(inv, act, SLOT_TAB_PLAY, Page.PLAY, page, Material.JUKEBOX, "娱乐");
        if (player.hasPermission("es2uni.admin")) {
            putTab(inv, act, SLOT_TAB_ADMIN, Page.ADMIN, page, Material.COMMAND_BLOCK, "管理");
        }
        putHead(inv, player);
        act.put(SLOT_HEAD, "head");
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&c关闭",
                List.of(" &8v" + plugin.getDescription().getVersion())));
        act.put(SLOT_CLOSE, "close");

        switch (page) {
            case OVERVIEW -> fillOverview(inv, act, player);
            case SOCIAL -> fillSocial(inv, act, player);
            case TRAVEL -> fillTravel(inv, act, player);
            case CITY -> fillCity(inv, act, player);
            case PLAY -> fillPlay(inv, act, player);
            case ADMIN -> fillAdmin(inv, act, player);
        }
        player.openInventory(inv);
    }

    public String actionAt(Player player, int slot) {
        Map<Integer, String> map = actions.get(player.getUniqueId());
        return map == null ? null : map.get(slot);
    }

    public Page pageOf(Player player) {
        return pages.getOrDefault(player.getUniqueId(), Page.OVERVIEW);
    }

    private void putTab(Inventory inv, Map<Integer, String> act, int slot, Page page, Page cur,
                        Material mat, String name) {
        boolean on = page == cur;
        ItemStack it = item(mat, (on ? "&a" : "&7") + name,
                List.of(on ? "&8当前分类" : "&8点击切换"));
        if (on) it = glint(it);
        inv.setItem(slot, it);
        act.put(slot, "tab:" + page.name());
    }

    private void put(Inventory inv, Map<Integer, String> act, int index, String id, ItemStack stack) {
        if (index < 0 || index >= CONTENT.length) return;
        int slot = CONTENT[index];
        inv.setItem(slot, stack);
        act.put(slot, id);
    }

    private void putHead(Inventory inv, Player player) {
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
    }

    private void fillOverview(Inventory inv, Map<Integer, String> act, Player player) {
        String pt = plugin.getPlaytimeManager().getFormatted(player);
        boolean checked = plugin.getCheckInManager().hasCheckedInToday(player.getUniqueId());
        int streak = plugin.getCheckInManager().getStreak(player.getUniqueId());
        int unread = plugin.getMailManager().unreadCount(player.getUniqueId());
        var trade = plugin.getTradeStatsManager();
        String volStr = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(trade.getTotalVolume())
                : String.format("%.2f", trade.getTotalVolume());
        var top1 = trade.getTopPlayers(1);
        String topStr = top1.isEmpty() ? "暂无" : top1.get(0).name();
        put(inv, act, 0, "summary", item(Material.BOOK, "&b今日摘要", List.of(
                " &7签到: " + (checked ? "&a已签" : "&e未签") + " &8(连" + streak + ")",
                " &7未读邮件: &f" + unread,
                " &7今日交易: &f" + volStr,
                " &7交易最高: &f" + topStr,
                plugin.getTransitManager() != null && plugin.getTransitManager().journeyHint(player.getUniqueId()) != null
                        ? " &b" + plugin.getTransitManager().journeyHint(player.getUniqueId())
                        : " &7在线: &f" + Bukkit.getOnlinePlayers().size(),
                " &7TPS: &f" + TPSUtil.formatTPS(TPSUtil.getTPS()),
                "", "&b▸ 点击刷新")));

        double tps = TPSUtil.getTPS();
        put(inv, act, 1, "tps", item(Material.COMPARATOR, "&b性能", List.of(
                " &7TPS  &f" + TPSUtil.formatTPS(tps),
                " &7MSPT &f" + TPSUtil.formatMSPT(TPSUtil.getMSPT()),
                " &7在线 &f" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers(),
                "", "&8▸ 详情")));

        if (plugin.getVaultHook().isEnabled()) {
            put(inv, act, 2, "balance", item(Material.GOLD_INGOT, "&6余额",
                    List.of(" &7" + plugin.getVaultHook().format(plugin.getVaultHook().getBalance(player)),
                            "", "&8仅显示")));
        } else {
            put(inv, act, 2, "balance", item(Material.BARRIER, "&8无经济", List.of("&8需要 Vault")));
        }

        boolean afk = plugin.getAfkManager().isAFK(player);
        put(inv, act, 3, "afk", item(afk ? Material.RED_BED : Material.LIME_BED,
                afk ? "&cAFK &8[开]" : "&aAFK &8[关]",
                List.of(" &7在线: &f" + pt, " &7延迟: &f" + player.getPing() + "ms",
                        "", afk ? "&8▸ 取消 AFK" : "&8▸ 标记 AFK")));

        int ev = plugin.getEventManager().getAllEvents().size();
        put(inv, act, 4, "events", item(Material.BOOK, "&e活动",
                List.of(" &7当前: &f" + ev, "", "&e▸ 打开")));

        if (plugin.getNewbieGuideManager().shouldShow(player)
                || !plugin.getNewbieGuideManager().isComplete(player.getUniqueId())) {
            put(inv, act, 5, "newbie", item(Material.FILLED_MAP,
                    "&a新手引导 &8" + plugin.getNewbieGuideManager().progress(player.getUniqueId()) + "%",
                    List.of(" &7上手清单", "", "&a▸ 打开")));
        }
    }

    private void fillSocial(Inventory inv, Map<Integer, String> act, Player player) {
        int friends = plugin.getFriendManager().getFriends(player.getUniqueId()).size();
        int reqs = plugin.getFriendManager().getPendingRequests(player.getUniqueId()).size();
        ItemStack friendItem = item(Material.PLAYER_HEAD, "&7好友", List.of(
                " &7好友数: &f" + friends,
                reqs > 0 ? " &e待处理: &f" + reqs : "",
                "", "&8点击打开"));
        if (reqs > 0) friendItem = glint(friendItem);
        put(inv, act, 0, "friends", friendItem);

        int unread = plugin.getMailManager().unreadCount(player.getUniqueId());
        int total = plugin.getMailManager().getMails(player.getUniqueId()).size();
        ItemStack mail = item(Material.WRITABLE_BOOK, "&7邮箱", List.of(
                " &7未读: &f" + unread + "  &8/ " + total, "", "&8点击打开"));
        if (unread > 0) mail = glint(mail);
        put(inv, act, 1, "mail", mail);

        put(inv, act, 2, "online", item(Material.COMPASS, "&7在线玩家", List.of(
                " &7其他在线: &f" + Math.max(0, Bukkit.getOnlinePlayers().size() - 1), "", "&8点击打开")));
        put(inv, act, 3, "notices", item(Material.PAPER, "&7公告板", List.of(
                " &7公告: &f" + plugin.getNoticeManager().getNotices().size(), "", "&8点击查看")));
        put(inv, act, 4, "leaderboard", item(Material.GOLDEN_HELMET, "&7在线时长排行", List.of("", "&8点击查看")));
        String status = plugin.getStatusManager().getStatus(player.getUniqueId());
        put(inv, act, 5, "status", item(Material.NAME_TAG, "&7个人签名", List.of(
                status != null ? " &f" + status : " &8未设置", "", "&8点击修改")));
    }

    private void fillTravel(Inventory inv, Map<Integer, String> act, Player player) {
        if (plugin.getEssentialsHook().isEnabled()) {
            put(inv, act, 0, "home", item(Material.RED_BED, "&d家园传送", List.of(
                    " &7家园: &f" + plugin.getEssentialsHook().getHomes(player).size(),
                    " &7右键: 在此设家", "", "&d▸ 左键打开列表")));
            put(inv, act, 1, "tpa", item(Material.ENDER_PEARL, "&6传送请求", List.of(" &7TPA", "", "&6▸ 选玩家")));
            put(inv, act, 2, "tpahere", item(Material.ENDER_EYE, "&6召唤请求", List.of(" &7TPA Here", "", "&6▸ 选玩家")));
        } else {
            ItemStack na = item(Material.BARRIER, "&8需要 EssentialsX", null);
            put(inv, act, 0, "home", na);
            put(inv, act, 1, "tpa", na);
            put(inv, act, 2, "tpahere", na);
        }
        put(inv, act, 3, "waypoints", item(Material.COMPASS, "&7坐标收藏", List.of(
                " &7" + plugin.getWaypointManager().get(player.getUniqueId()).size()
                        + " / " + plugin.getWaypointManager().maxPerPlayer(), "", "&8打开")));
        DeathManager.DeathRecord last = plugin.getDeathManager().getLast(player.getUniqueId());
        if (last != null) {
            put(inv, act, 4, "death", item(Material.BONE, "&7死亡记录", List.of(
                    " &8" + last.date(),
                    " &8" + last.world() + "  " + last.x() + ", " + last.y() + ", " + last.z(),
                    " &8" + last.cause(), "", "&8查看全部")));
        } else {
            put(inv, act, 4, "death", item(Material.BONE, "&7死亡记录", List.of(" &8暂无")));
        }
        var myTerr = plugin.getRegionManager().getByOwner(player.getUniqueId());
        put(inv, act, 5, "my-territory", item(Material.MAP, "&d我的领地", List.of(
                " &7" + myTerr.size() + " 块", "", "&d▸ 管理")));
        put(inv, act, 6, "hot-places", item(Material.BEACON, "&6热门地点", List.of(
                " &7公开领地: &f" + plugin.getRegionManager().getPublicTerritories().size(), "", "&6▸ 传送")));
        int rooms = plugin.getEstateManager() == null ? 0
                : plugin.getEstateManager().ownedBy(player.getUniqueId()).size();
        put(inv, act, 7, "estate", item(Material.OAK_DOOR, "&2房产", List.of(
                " &7我的房间: &f" + rooms,
                " &7楼盘 · 登记 · 门牌",
                "", "&2▸ 打开")));
        int sale = plugin.getEstateManager() == null ? 0
                : plugin.getEstateManager().listedForSale().size();
        put(inv, act, 8, "estate-sale", item(Material.GOLD_INGOT, "&e买房", List.of(
                " &7挂牌: &f" + sale,
                " &7左键看房 · 右键购买",
                "", "&e▸ 打开")));
        put(inv, act, 9, "estate-tools", item(Material.OAK_SIGN, "&a登记房产", List.of(
                "&8点1 / 点2 / 登记 / 门口", "", "&a▸ 打开")));
        put(inv, act, 10, "estate-list", item(Material.GOLD_NUGGET, "&e挂牌", List.of(
                "&8改价 / 上架 / 下架", "", "&e▸ 打开我的房产")));
        int hotels = plugin.getHotelManager() == null ? 0 : plugin.getHotelManager().all().size();
        put(inv, act, 11, "hotel", item(Material.BELL, "&9酒店", List.of(
                " &7酒店: &f" + hotels, " &7入住 · 房卡 · 门锁", "", "&9▸ 打开")));
    }

    private void fillCity(Inventory inv, Map<Integer, String> act, Player player) {
        boolean checked = plugin.getCheckInManager().hasCheckedInToday(player.getUniqueId());
        put(inv, act, 0, "checkin", item(
                checked ? Material.LIME_CONCRETE : Material.YELLOW_CONCRETE,
                checked ? "&a签到日历 &8[已签]" : "&e签到日历",
                List.of(" &7连续: &f" + plugin.getCheckInManager().getStreak(player.getUniqueId()) + " 天", "", "&8打开月历")));
        if (plugin.getVaultHook().isEnabled()) {
            put(inv, act, 1, "pay", item(Material.SUNFLOWER, "&a转账", List.of(" &7二次确认", "", "&a▸ 选收款人")));
        } else {
            put(inv, act, 1, "pay", item(Material.BARRIER, "&8转账不可用", List.of("&8需要 Vault")));
        }
        var trade = plugin.getTradeStatsManager();
        String vol = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(trade.getTotalVolume())
                : String.format("%.2f", trade.getTotalVolume());
        put(inv, act, 2, "trade", item(Material.EMERALD, "&6今日交易榜", List.of(" &7总量: &f" + vol, "", "&6▸ 打开")));
        int jobs = plugin.getJobBoardManager().listActive().size();
        put(inv, act, 3, "municipal", item(Material.BEACON, "&9管理处", List.of(
                " &7招工 · 清理 · 保险 · 交通", " &7委托: &f" + jobs, "", "&9▸ 打开")));
        put(inv, act, 4, "kit", item(Material.BUNDLE, "&6Kit 套件", List.of(" &7新手包 / 礼包", "", "&6▸ 打开")));
        boolean luckyDone = !plugin.getLuckyBlockManager().canDraw(player.getUniqueId());
        int tickets = plugin.getLuckyBlockManager().getTickets(player.getUniqueId());
        String luckyName = plugin.getLuckyBlockManager().displayName();
        put(inv, act, 5, "lucky", item(
                luckyDone ? Material.GRAY_CONCRETE : Material.GOLD_BLOCK,
                luckyDone ? "&8" + luckyName + " &7[已抽]" : "&d" + luckyName,
                List.of(" &7额外次数: &f" + tickets, "",
                        luckyDone && tickets <= 0 ? "&8明天再来" : "&d▸ 开启")));
        put(inv, act, 6, "showcase", item(Material.CRAFTING_TABLE, "&d机器展示", List.of(
                " &7展示点: &f" + plugin.getShowcaseManager().all().size(), "", "&d▸ 打开")));
        put(inv, act, 7, "estate", item(Material.DARK_OAK_DOOR, "&2房产", List.of(
                " &7住宅 / 公共 / 商业", "", "&2▸ 打开")));
        int sale = plugin.getEstateManager() == null ? 0
                : plugin.getEstateManager().listedForSale().size();
        put(inv, act, 8, "estate-sale", item(Material.GOLD_INGOT, "&e买房", List.of(
                " &7挂牌: &f" + sale, "", "&e▸ 打开")));
        put(inv, act, 9, "estate-list", item(Material.GOLD_NUGGET, "&e挂牌", List.of(
                "&8我的房产上架 / 改价", "", "&e▸ 打开")));
        int hotels = plugin.getHotelManager() == null ? 0 : plugin.getHotelManager().all().size();
        put(inv, act, 10, "hotel", item(Material.BELL, "&9酒店", List.of(
                " &7" + hotels + " 家", "", "&9▸ 打开")));
    }

    private void fillPlay(Inventory inv, Map<Integer, String> act, Player player) {
        put(inv, act, 0, "music", item(Material.JUKEBOX, "&d点歌", List.of(
                plugin.getAllMusicHook().isAvailable() ? " &aAllMusic 已连接" : " &cAllMusic 未加载",
                "", "&d▸ 打开")));
        plugin.getAllMusicHook().ensureHooked(plugin.getLogger());
        if (plugin.getAllMusicHook().isAvailable()) {
            boolean muted = plugin.getAllMusicHook().isMuted(player);
            put(inv, act, 1, "music-listen", item(
                    muted ? Material.RED_DYE : Material.LIME_DYE,
                    muted ? "&c听歌: 关" : "&a听歌: 开",
                    List.of(muted ? " &7静音中" : " &7正在收听", "", muted ? "&a▸ 开启" : "&c▸ 关闭")));
        } else {
            put(inv, act, 1, "music-listen", item(Material.GRAY_DYE, "&8听歌开关", List.of("&cAllMusic 未加载")));
        }
        var curAura = plugin.getAuraManager().getEquipped(player.getUniqueId());
        put(inv, act, 2, "aura", item(Material.AMETHYST_SHARD, "&b特效商店", List.of(
                " &7已拥有: &f" + plugin.getAuraManager().getOwned(player.getUniqueId()).size()
                        + " / " + com.etherstories.escore.auras.AuraType.values().length,
                curAura != null ? " &7装备: &f" + curAura.displayName() : " &7未装备",
                "", "&b▸ 打开")));
        boolean linkOn = Bukkit.getPluginManager().getPlugin("ESLink") != null
                && Bukkit.getPluginManager().getPlugin("ESLink").isEnabled();
        put(inv, act, 3, "link", item(linkOn ? Material.ENDER_CHEST : Material.GRAY_DYE,
                linkOn ? "&b互通大厅" : "&8互通大厅",
                List.of(linkOn ? " &7跨服市场 / 运输箱" : " &c未安装 ESLink", "", linkOn ? "&b▸ 打开" : "&8需要 ESLink")));
        put(inv, act, 4, "events", item(Material.BOOK, "&e活动中心", List.of(
                " &7活动: &f" + plugin.getEventManager().getAllEvents().size(), "", "&e▸ 打开")));
    }

    private void fillAdmin(Inventory inv, Map<Integer, String> act, Player player) {
        if (!player.hasPermission("es2uni.admin")) return;
        put(inv, act, 0, "adm-events", item(Material.WRITABLE_BOOK, "&c活动管理", List.of("", "&c▸ 打开")));
        put(inv, act, 1, "adm-bc", item(Material.BELL, "&c定时广播", List.of("", "&c▸ 打开")));
        put(inv, act, 2, "adm-reload", item(Material.REDSTONE, "&c重载配置", List.of("", "&c▸ 执行")));
        put(inv, act, 3, "adm-territory", item(Material.FILLED_MAP, "&c领地管理", List.of(
                " &7" + plugin.getRegionManager().getAllTerritories().size() + " 块", "", "&c▸ 打开")));
        put(inv, act, 4, "adm-regions", item(Material.PAPER, "&c地区管理", List.of(
                " &7" + plugin.getRegionManager().getDistricts().size() + " 个", "", "&c▸ 打开")));
        put(inv, act, 5, "adm-kit", item(Material.CHEST, "&cKit 管理", List.of("", "&c▸ 打开")));
        var tax = plugin.getTaxManager();
        put(inv, act, 6, "adm-tax", item(Material.GOLD_INGOT, "&c税收设置", List.of(
                " &7转账: " + (tax.isPayTaxEnabled() ? "&a开" : "&c关"),
                " &7箱子店: " + (tax.isQuickShopTaxEnabled() ? "&a开" : "&c关"),
                "", "&c▸ 打开")));
        put(inv, act, 7, "adm-estate", item(Material.COMMAND_BLOCK, "&c全部房产", List.of(
                " &7" + plugin.getEstateManager().all().size() + " 间", "", "&c▸ 打开")));
        put(inv, act, 8, "adm-grant", item(Material.SUNFLOWER, "&c发放", List.of(
                " &7补签券 / 幸运方块次数", "", "&c▸ 打开")));
    }

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

    public static boolean isCloseSlot(int slot) { return slot == SLOT_CLOSE; }
}
