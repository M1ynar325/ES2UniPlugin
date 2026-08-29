package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.DeathManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ECOSTerminalGUI {

    public enum Page {
        OVERVIEW("概览", "01"), SOCIAL("社交", "02"), TRAVEL("出行", "03"),
        CITY("城市", "04"), PLAY("娱乐", "05"), ADMIN("管理", "06");
        public final String label;
        public final String code;
        Page(String label, String code) { this.label = label; this.code = code; }
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
    private static org.bukkit.NamespacedKey menuKey;

    public ECOSTerminalGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
        menuKey = new org.bukkit.NamespacedKey(plugin, "ecos_menu");
    }

    public static boolean isMenuItem(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta() || menuKey == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(menuKey, PersistentDataType.BYTE);
    }

    public static boolean isTitle(String title) {
        if (title == null) return false;
        String s = org.bukkit.ChatColor.stripColor(title);
        return s != null && s.contains("[ ECOS");
    }

    /** 27 格且至少 8 个带标 → 主菜单（holder/标题在 Youer 上会丢）。单箱里 1～2 个脏格子不够。 */
    public static boolean looksLikeMenu(org.bukkit.inventory.Inventory top) {
        if (top == null || top.getSize() != SIZE) return false;
        int tagged = 0;
        for (int i = 0; i < SIZE; i++) {
            if (isMenuItem(top.getItem(i))) tagged++;
        }
        return tagged >= 8;
    }

    public void cleanup(Player player) {
        UUID id = player.getUniqueId();
        pages.remove(id);
        actions.remove(id);
    }

    public void open(Player player) {
        open(player, pages.getOrDefault(player.getUniqueId(), Page.OVERVIEW));
    }

    public void open(Player player, Page page) {
        if (page == Page.ADMIN && !player.hasPermission("es2uni.admin")) page = Page.OVERVIEW;
        pages.put(player.getUniqueId(), page);
        Map<Integer, String> act = new HashMap<>();
        actions.put(player.getUniqueId(), act);

        String title = EcosStyle.terminal(page.label);
        EcosHolder holder = new EcosHolder(EcosHolder.Kind.TERMINAL);
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.bind(inv);

        putTab(inv, act, SLOT_TAB_OVERVIEW, Page.OVERVIEW, page, Material.BOOK, "概览", "签到 · 邮件 · 钱");
        putTab(inv, act, SLOT_TAB_SOCIAL, Page.SOCIAL, page, Material.PLAYER_HEAD, "社交", "好友 · 在线 · 公告");
        putTab(inv, act, SLOT_TAB_TRAVEL, Page.TRAVEL, page, Material.ENDER_PEARL, "出行", "家 · 传送 · 领地");
        putTab(inv, act, SLOT_TAB_CITY, Page.CITY, page, Material.BEACON, "城市", "房产 · 酒店 · 市政");
        putTab(inv, act, SLOT_TAB_PLAY, Page.PLAY, page, Material.JUKEBOX, "娱乐", "点歌 · 活动 · 特效");
        if (player.hasPermission("es2uni.admin")) {
            putTab(inv, act, SLOT_TAB_ADMIN, Page.ADMIN, page, Material.COMMAND_BLOCK, "管理", "活动 · 广播 · 税收");
        }
        putHead(inv, player);
        act.put(SLOT_HEAD, "head");
        inv.setItem(SLOT_CLOSE, item(Material.LIGHT_GRAY_DYE,
                EcosStyle.MIST + "断开  ·  v" + plugin.getDescription().getVersion(),
                List.of(" " + EcosStyle.MIST + "结束会话")));
        act.put(SLOT_CLOSE, "close");

        switch (page) {
            case OVERVIEW -> fillOverview(inv, act, player);
            case SOCIAL -> fillSocial(inv, act, player);
            case TRAVEL -> fillTravel(inv, act, player);
            case CITY -> fillCity(inv, act, player);
            case PLAY -> fillPlay(inv, act, player);
            case ADMIN -> fillAdmin(inv, act, player);
        }
        fillTabBar(inv);
        for (int i = 0; i < SIZE; i++) mark(inv.getItem(i));
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
                        Material mat, String name, String hint) {
        boolean on = page == cur;
        String nameCol = on ? EcosStyle.SNOW : EcosStyle.MIST;
        String codeCol = on ? EcosStyle.COBALT : EcosStyle.MIST;
        ItemStack it = item(mat, codeCol + page.code + "  " + nameCol + name,
                List.of(" " + EcosStyle.MIST + hint, "",
                        on ? EcosStyle.COBALT + "▸ 当前" : EcosStyle.MIST + "▸ 切换"));
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
            skullMeta.setDisplayName(ColorUtil.colorize(EcosStyle.JIQING + EcosStyle.hello(player.getName())));
            skullMeta.setLore(List.of(
                    ColorUtil.colorize(" " + EcosStyle.MIST + "ECOS  v" + plugin.getDescription().getVersion()),
                    ColorUtil.colorize(" " + EcosStyle.MIST + "在线  " + EcosStyle.SNOW + pt),
                    ColorUtil.colorize(" " + EcosStyle.MIST + "延迟  " + EcosStyle.SNOW + player.getPing() + "ms")
            ));
            skull.setItemMeta(skullMeta);
        }
        mark(skull);
        inv.setItem(SLOT_HEAD, skull);
    }

    private void fillOverview(Inventory inv, Map<Integer, String> act, Player player) {
        String pt = plugin.getPlaytimeManager().getFormatted(player);
        boolean checked = plugin.getCheckInManager().hasCheckedInToday(player.getUniqueId());
        int streak = plugin.getCheckInManager().getStreak(player.getUniqueId());
        int unread = plugin.getMailManager().unreadCount(player.getUniqueId());
        String stayLabel = plugin.getHotelManager() == null ? null : plugin.getHotelManager().stayLabel(player.getUniqueId());

        put(inv, act, 0, "summary", item(Material.NETHER_STAR, EcosStyle.JIQING + "ECOS", List.of(
                " " + EcosStyle.MIST + "Etharia Central OS",
                " &7签到: " + (checked ? "&a已签" : "&e未签") + " &8连" + streak,
                " &7邮件: " + (unread > 0 ? "&e" + unread + " 未读" : "&7无未读"),
                stayLabel != null ? " &c入住: &f" + stayLabel : " &7在线: &f" + Bukkit.getOnlinePlayers().size(),
                plugin.getTransitManager() != null && plugin.getTransitManager().journeyHint(player.getUniqueId()) != null
                        ? " " + EcosStyle.JIQING + plugin.getTransitManager().journeyHint(player.getUniqueId())
                        : " &7时长: &f" + pt,
                "", EcosStyle.MIST + "▸ 刷新")));

        ItemStack checkin = item(
                checked ? Material.LIME_CONCRETE : Material.SUNFLOWER,
                checked ? "&a签到 &8已签" : "&e签到",
                List.of(" &7连续 &f" + streak + " 天", "", checked ? "&8打开月历" : "&e▸ 去签到"));
        if (!checked) checkin = glint(checkin);
        put(inv, act, 1, "checkin", checkin);

        ItemStack mail = item(Material.WRITABLE_BOOK,
                unread > 0 ? "&e邮箱 &8" + unread + " 未读" : "&f邮箱",
                List.of(unread > 0 ? " &e有新邮件" : " &7没有未读", "", "&f▸ 打开"));
        if (unread > 0) mail = glint(mail);
        put(inv, act, 2, "mail", mail);

        put(inv, act, 5, "web-pair", item(Material.ENDER_EYE, EcosStyle.JIQING + "网页",
                List.of(" &7连接码两分钟", " &8/ecos web", "", EcosStyle.JIQING + "▸ 生成")));

        if (plugin.getVaultHook().isEnabled()) {
            put(inv, act, 3, "pay", item(Material.GOLD_INGOT, "&6转账",
                    List.of(" &f" + plugin.getVaultHook().format(plugin.getVaultHook().getBalance(player)),
                            "", "&6▸ 选人转账")));
        } else {
            put(inv, act, 3, "balance", item(Material.BARRIER, "&8无经济", List.of("&8需要 Vault")));
        }

        if (stayLabel != null) {
            put(inv, act, 4, "hotel-checkout", glint(item(Material.RED_BED, "&c退房",
                    List.of(" &f" + stayLabel,
                            " &8左键退房  右键打开酒店",
                            "", "&c▸ 退房"))));
        }

        boolean afk = plugin.getAfkManager().isAFK(player);
        put(inv, act, 9, "afk", item(afk ? Material.RED_BED : Material.CLOCK,
                afk ? "&cAFK 开着" : "&7AFK",
                List.of(" &7延迟 &f" + player.getPing() + "ms",
                        "", afk ? "&a▸ 回来" : "&8▸ 标记离开")));

        if (plugin.getNewbieGuideManager().shouldShow(player)
                || !plugin.getNewbieGuideManager().isComplete(player.getUniqueId())) {
            put(inv, act, 10, "newbie", glint(item(Material.FILLED_MAP,
                    "&a新手引导 &8" + plugin.getNewbieGuideManager().progress(player.getUniqueId()) + "%",
                    List.of(" &7还没做完", "", "&a▸ 打开"))));
        }
    }

    private void fillSocial(Inventory inv, Map<Integer, String> act, Player player) {
        int friends = plugin.getFriendManager().getFriends(player.getUniqueId()).size();
        int reqs = plugin.getFriendManager().getPendingRequests(player.getUniqueId()).size();
        ItemStack friendItem = item(Material.PLAYER_HEAD, "&d好友", List.of(
                " &7" + friends + " 人",
                reqs > 0 ? " &e待处理 " + reqs : " &8没有申请",
                "", "&d▸ 打开"));
        if (reqs > 0) friendItem = glint(friendItem);
        put(inv, act, 0, "friends", friendItem);

        int unread = plugin.getMailManager().unreadCount(player.getUniqueId());
        int total = plugin.getMailManager().getMails(player.getUniqueId()).size();
        ItemStack mail = item(Material.WRITABLE_BOOK, unread > 0 ? "&e邮箱" : "&f邮箱", List.of(
                " &7未读 &f" + unread + "  &8/ " + total, "", "&f▸ 打开"));
        if (unread > 0) mail = glint(mail);
        put(inv, act, 1, "mail", mail);

        put(inv, act, 2, "online", item(Material.COMPASS, "&a在线", List.of(
                " &7其他 &f" + Math.max(0, Bukkit.getOnlinePlayers().size() - 1) + " 人", "", "&a▸ 打开")));
        put(inv, act, 3, "notices", item(Material.PAPER, "&f公告", List.of(
                " &7" + plugin.getNoticeManager().getNotices().size() + " 条", "", "&f▸ 查看")));
        put(inv, act, 4, "leaderboard", item(Material.GOLDEN_HELMET, "&6时长榜", List.of("", "&6▸ 打开")));
        String status = plugin.getStatusManager().getStatus(player.getUniqueId());
        put(inv, act, 5, "status", item(Material.NAME_TAG, EcosStyle.COBALT + "签名", List.of(
                status != null ? " &f" + status : " &8未设置", "", EcosStyle.COBALT + "▸ 修改")));
    }

    private void fillTravel(Inventory inv, Map<Integer, String> act, Player player) {
        if (plugin.getEssentialsHook().isEnabled()) {
            put(inv, act, 0, "home", item(Material.RED_BED, "&d家", List.of(
                    " &7" + plugin.getEssentialsHook().getHomes(player).size() + " 个",
                    " &8右键: 在此设家", "", "&d▸ 左键列表")));
            put(inv, act, 1, "tpa", item(Material.ENDER_PEARL, "&6传送", List.of(" &7TPA 到别人那里", "", "&6▸ 选玩家")));
            put(inv, act, 2, "tpahere", item(Material.ENDER_EYE, "&6召唤", List.of(" &7叫别人过来", "", "&6▸ 选玩家")));
        } else {
            ItemStack na = item(Material.BARRIER, "&8需要 EssentialsX", null);
            put(inv, act, 0, "home", na);
            put(inv, act, 1, "tpa", na);
            put(inv, act, 2, "tpahere", na);
        }
        put(inv, act, 3, "waypoints", item(Material.COMPASS, EcosStyle.COBALT + "坐标", List.of(
                " &7" + plugin.getWaypointManager().get(player.getUniqueId()).size()
                        + " / " + plugin.getWaypointManager().maxPerPlayer(), "", EcosStyle.COBALT + "▸ 打开")));
        DeathManager.DeathRecord last = plugin.getDeathManager().getLast(player.getUniqueId());
        if (last != null) {
            put(inv, act, 4, "death", item(Material.BONE, "&7死亡点", List.of(
                    " &8" + last.date(),
                    " &8" + last.world() + "  " + last.x() + ", " + last.y() + ", " + last.z(),
                    "", "&8查看全部")));
        } else {
            put(inv, act, 4, "death", item(Material.BONE, "&7死亡点", List.of(" &8还没有")));
        }
        var myTerr = plugin.getRegionManager().getByOwner(player.getUniqueId());
        put(inv, act, 5, "my-territory", item(Material.MAP, "&d领地", List.of(
                " &7" + myTerr.size() + " 块", "", "&d▸ 管理")));
        put(inv, act, 9, "hot-places", item(Material.BEACON, "&6热门地点", List.of(
                " &7公开领地 &f" + plugin.getRegionManager().getPublicTerritories().size(), "", "&6▸ 传送")));
    }

    private void fillCity(Inventory inv, Map<Integer, String> act, Player player) {
        int rooms = plugin.getEstateManager() == null ? 0
                : plugin.getEstateManager().ownedBy(player.getUniqueId()).size();
        int sale = plugin.getEstateManager() == null ? 0
                : plugin.getEstateManager().listedForSale().size();
        put(inv, act, 0, "estate", item(Material.OAK_DOOR, EcosStyle.JIQING + "房产", List.of(
                " &7我的 &f" + rooms + "  &8待售 &e" + sale,
                " &8买房 · 登记 · 挂牌都在里面",
                "", EcosStyle.JIQING + "▸ 打开")));

        String stayLabel = plugin.getHotelManager() == null ? null : plugin.getHotelManager().stayLabel(player.getUniqueId());
        int hotels = plugin.getHotelManager() == null ? 0 : plugin.getHotelManager().all().size();
        if (stayLabel != null) {
            put(inv, act, 1, "hotel-checkout", glint(item(Material.RED_BED, "&c退房",
                    List.of(" &f" + stayLabel, " &8左键退房  右键打开酒店", "", "&c▸ 退房"))));
        } else {
            put(inv, act, 1, "hotel", item(Material.BELL, EcosStyle.COBALT + "酒店",
                    List.of(" &7" + hotels + " 家", "", EcosStyle.COBALT + "▸ 打开")));
        }

        int jobs = plugin.getJobBoardManager().listActive().size();
        put(inv, act, 2, "municipal", item(Material.BEACON, EcosStyle.COBALT + "市政", List.of(
                " &7招工 · 清理 · 保险 · 交通", " &7委托 &f" + jobs, "", EcosStyle.COBALT + "▸ 打开")));

        put(inv, act, 3, "kit", item(Material.BUNDLE, "&6Kit", List.of(" &7新手包 / 礼包", "", "&6▸ 打开")));

        boolean luckyDone = !plugin.getLuckyBlockManager().canDraw(player.getUniqueId());
        int tickets = plugin.getLuckyBlockManager().getTickets(player.getUniqueId());
        String luckyName = plugin.getLuckyBlockManager().displayName();
        put(inv, act, 4, "lucky", item(
                luckyDone ? Material.GRAY_CONCRETE : Material.GOLD_BLOCK,
                luckyDone ? "&8" + luckyName : "&d" + luckyName,
                List.of(" &7额外 &f" + tickets,
                        "", luckyDone && tickets <= 0 ? "&8明天再来" : "&d▸ 开启")));

        var trade = plugin.getTradeStatsManager();
        String vol = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(trade.getTotalVolume())
                : String.format("%.2f", trade.getTotalVolume());
        put(inv, act, 5, "trade", item(Material.EMERALD, "&6交易榜", List.of(" &7今日 &f" + vol, "", "&6▸ 打开")));

        put(inv, act, 9, "showcase", item(Material.CRAFTING_TABLE, "&d展示", List.of(
                " &7" + plugin.getShowcaseManager().all().size() + " 处", "", "&d▸ 打开")));
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
                    muted ? "&c听歌 关" : "&a听歌 开",
                    List.of("", muted ? "&a▸ 开启" : "&c▸ 关闭")));
        } else {
            put(inv, act, 1, "music-listen", item(Material.GRAY_DYE, "&8听歌", List.of("&cAllMusic 未加载")));
        }
        var curAura = plugin.getAuraManager().getEquipped(player.getUniqueId());
        put(inv, act, 2, "aura", item(Material.AMETHYST_SHARD, EcosStyle.COBALT + "特效", List.of(
                " &7已拥有 &f" + plugin.getAuraManager().getOwned(player.getUniqueId()).size()
                        + " / " + com.etherstories.escore.auras.AuraType.values().length,
                curAura != null ? " &7装备 &f" + curAura.displayName() : " &7未装备",
                "", EcosStyle.COBALT + "▸ 打开")));
        boolean linkOn = Bukkit.getPluginManager().getPlugin("ESLink") != null
                && Bukkit.getPluginManager().getPlugin("ESLink").isEnabled();
        put(inv, act, 3, "link", item(linkOn ? Material.ENDER_CHEST : Material.GRAY_DYE,
                linkOn ? EcosStyle.JIQING + "互通" : "&8互通",
                List.of(linkOn ? " &7市场 / 箱子 / 红石" : " &c未装 ESLink", "",
                        linkOn ? EcosStyle.COBALT + "▸ 打开" : "&8—")));
        put(inv, act, 4, "events", item(Material.FIREWORK_ROCKET, "&e活动", List.of(
                " &7" + plugin.getEventManager().getAllEvents().size() + " 个", "", "&e▸ 打开")));
        int listed = plugin.getSkillShopManager() == null ? 0
                : plugin.getSkillShopManager().forSale().size();
        put(inv, act, 5, "skillshop", item(Material.END_ROD, EcosStyle.COBALT + "技能店", List.of(
                " &7在售 &f" + listed, "", EcosStyle.COBALT + "▸ 打开")));
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
        put(inv, act, 9, "adm-skillshop", item(Material.END_ROD, "&c技能上架", List.of(
                " &7在售: &f" + plugin.getSkillShopManager().forSale().size(),
                " &7左键上下架 · 右键改价", "", "&c▸ 打开")));
    }

    public static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(ColorUtil.colorize(name));
        if (lore != null)
            meta.setLore(lore.stream().map(ColorUtil::colorize).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack glint(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setEnchantmentGlintOverride(true);
        stack.setItemMeta(meta);
        return stack;
    }

    static void fillTabBar(Inventory inv) {
        ItemStack pane = EcosStyle.chrome();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane);
        }
    }

    static void fillEmpty(Inventory inv) {
        fillTabBar(inv);
    }

    static ItemStack bg(Material mat) {
        ItemStack s = new ItemStack(mat);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName(" ");
            m.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            s.setItemMeta(m);
        }
        return s;
    }

    private static void mark(ItemStack stack) {
        if (stack == null || menuKey == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(menuKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
    }

    public static boolean isCloseSlot(int slot) { return slot == SLOT_CLOSE; }
}
