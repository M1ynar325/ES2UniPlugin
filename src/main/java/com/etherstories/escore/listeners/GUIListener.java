package com.etherstories.escore.listeners;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.auras.AuraType;
import com.etherstories.escore.gui.AuraShopGUI;
import com.etherstories.escore.commands.ES2UniCommand;
import com.etherstories.escore.events.ServerEvent;
import com.etherstories.escore.gui.*;
import com.etherstories.escore.hooks.AllMusicHook;
import com.etherstories.escore.managers.DeathManager;
import com.etherstories.escore.managers.KitManager;
import com.etherstories.escore.managers.RegionManager;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class GUIListener implements Listener {

    private final ES2UniPlugin plugin;
    private final NamespacedKey noticeDraftKey;

    private final Set<UUID>        awaitingBroadcast = new HashSet<>(); // 立即广播一条
    private final Set<UUID>        awaitingBcAdd     = new HashSet<>();
    private final Map<UUID, Integer> awaitingBcEdit  = new HashMap<>();
    private final Set<UUID>        awaitingFriendName = new HashSet<>();
    private final Map<UUID, UUID>  awaitingReport    = new HashMap<>(); // reporter → about
    private final Set<UUID>        awaitingShowcaseTitle = new HashSet<>();
    private final Set<UUID>        confirming        = new HashSet<>();
    private final Set<UUID>        awaitingStatus    = new HashSet<>();
    private final Set<UUID>        awaitingKitCd     = new HashSet<>();
    private final Map<UUID, UUID>  awaitingWhisper   = new HashMap<>(); // sender → target
    private final Set<UUID>        awaitingTypeCreate = new HashSet<>();
    private final Map<UUID, String> awaitingTypeRename = new HashMap<>();
    private final Map<UUID, String> awaitingStationNameZh = new HashMap<>();
    private final Map<UUID, String> awaitingStationNameEn = new HashMap<>();

    public GUIListener(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.noticeDraftKey = new NamespacedKey(plugin, "notice_draft");
    }

    // ── Anvil 已废弃：Arclight 虚拟铁砧改名不同步，改用聊天输入 ───────────────

    // ── All inventory clicks ──────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 聊天输入等待中时，点任何 GUI 不误触确认
        if (plugin.getAnvilInputGUI().isInInput(player)) {
            event.setCancelled(true);
            return;
        }

        String title = event.getView().getTitle();

        if (ECOSTerminalGUI.isTitle(title)) {
            handleTerminal(event, player);
        } else if (title.equals(t(plugin.getConfigManager().getEventGUITitle()))) {
            handleEventList(event, player);
        } else if (title.equals(t(plugin.getConfigManager().getAdminEventGUITitle()))) {
            handleAdminEvent(event, player);
        } else if (title.equals(t(plugin.getConfigManager().getHomeGUITitle()))) {
            handleHomeList(event, player);
        } else if (isPlayerSelectTitle(title)) {
            handlePlayerSelect(event, player);
        } else if (title.equals(t(plugin.getConfigManager().getPayConfirmTitle()))) {
            handlePayConfirm(event, player);
        } else if (title.equals(t(FriendListGUI.TITLE_FRIENDS))
                || title.equals(t(FriendListGUI.TITLE_REQUESTS))) {
            handleFriendList(event, player);
        } else if (title.equals(t(MailboxGUI.TITLE))) {
            handleMailbox(event, player);
        } else if (title.equals(t(OnlineListGUI.TITLE))) {
            handleOnlineList(event, player);
        } else if (title.startsWith(t(PlayerActionGUI.TITLE))) {
            handlePlayerAction(event, player);
        } else if (title.equals(t(NoticeGUI.TITLE))) {
            handleNotice(event, player);
        } else if (title.equals(t(LeaderboardGUI.TITLE))) {
            handleLeaderboard(event, player);
        } else if (title.equals(t(TransitBoardGUI.TITLE))) {
            handleTransitBoard(event, player);
        } else if (title.equals(t(WaypointGUI.TITLE))) {
            handleWaypointGUI(event, player);
        } else if (title.equals(t(TerritoryListGUI.TITLE))) {
            handleTerritoryList(event, player);
        } else if (title.equals(t(AdminTerritoryGUI.TITLE))) {
            handleAdminTerritory(event, player);
        } else if (title.equals(t(MyTerritoryGUI.TITLE))) {
            handleMyTerritory(event, player);
        } else if (title.equals(EstateBuildingsGUI.TITLE)) {
            handleEstateBuildings(event, player);
        } else if (EstateRoomsGUI.isTitle(title)) {
            handleEstateRooms(event, player);
        } else if (title.equals(EstateMineGUI.TITLE) || title.equals(EstateMineGUI.ADMIN_TITLE)) {
            handleEstateMine(event, player);
        } else if (title.equals(t(AdminRegionGUI.TITLE))) {
            handleAdminRegion(event, player);
        } else if (title.equals(AuraShopGUI.TITLE)) {
            handleAuraShop(event, player);
        } else if (title.equals(AdminKitGUI.TITLE)) {
            handleAdminKit(event, player);
        } else if (title.startsWith(KitEditGUI.TITLE_PREFIX)) {
            handleKitEdit(event, player);
        } else if (title.equals(KitListGUI.TITLE)) {
            handleKitList(event, player);
        } else if (title.equals(MusicMenuGUI.TITLE)) {
            handleMusicMenu(event, player);
        } else if (title.equals(MusicSearchResultGUI.TITLE)) {
            handleMusicSearch(event, player);
        } else if (title.equals(MusicHistoryGUI.TITLE)) {
            handleMusicHistory(event, player);
        } else if (title.equals(MusicFavoritesGUI.TITLE)) {
            handleMusicFavorites(event, player);
        } else if (title.equals(MusicPlaylistGUI.TITLE)) {
            handleMusicPlaylist(event, player);
        } else if (title.equals(AdminTaxGUI.TITLE)) {
            handleAdminTax(event, player);
        } else if (title.equals(AdminBroadcastGUI.TITLE)) {
            handleAdminBroadcast(event, player);
        } else if (title.equals(AddFriendGUI.TITLE)) {
            handleAddFriend(event, player);
        } else if (title.equals(CheckInCalendarGUI.TITLE)) {
            handleCheckInCalendar(event, player);
        } else if (title.equals(TradeBoardGUI.TITLE)) {
            handleTradeBoard(event, player);
        } else if (title.equals(ShowcaseGUI.TITLE)) {
            handleShowcase(event, player);
        } else if (title.equals(NewbieGuideGUI.TITLE)) {
            handleNewbieGuide(event, player);
        } else if (title.equals(MunicipalOfficeGUI.TITLE)) {
            handleMunicipalOffice(event, player);
        } else if (title.equals(TransitOfficeGUI.TITLE)) {
            handleTransitOffice(event, player);
        } else if (title.equals(TransitMapGUI.TITLE)) {
            handleTransitMap(event, player);
        } else if (title.equals(TransitTicketGUI.TITLE)) {
            handleTransitTicket(event, player);
        } else if (title.equals(TransitHistoryGUI.TITLE)) {
            handleTransitHistory(event, player);
        } else if (title.equals(TransitTvmGUI.TITLE)) {
            handleTransitTvm(event, player);
        } else if (title.equals(TransitAdjustGUI.TITLE)) {
            handleTransitAdjust(event, player);
        } else if (title.equals(TransitAdminGUI.TITLE)) {
            handleTransitAdmin(event, player);
        } else if (title.equals(TransitEdgesGUI.TITLE)) {
            handleTransitEdges(event, player);
        } else if (title.equals(TransitStationGUI.TITLE)) {
            handleTransitStation(event, player);
        } else if (title.equals(TransitAnnounceGUI.TITLE)) {
            handleTransitAnnounce(event, player);
        } else if (title.equals(TransitTypesGUI.TITLE)) {
            handleTransitTypes(event, player);
        } else if (title.equals(TransitTypeEditGUI.TITLE)) {
            handleTransitTypeEdit(event, player);
        } else if (title.equals(TransitClaimsGUI.TITLE)) {
            handleTransitClaims(event, player);
        } else if (title.equals(TransitRidersGUI.TITLE)) {
            handleTransitRiders(event, player);
        } else if (title.equals(JobBoardGUI.TITLE)) {
            handleJobBoard(event, player);
        } else if (title.equals(JobHistoryGUI.TITLE)) {
            handleJobHistory(event, player);
        } else if (title.equals(InsuranceGUI.TITLE)) {
            handleInsurance(event, player);
        } else if (title.equals(InsuranceBackupGUI.TITLE)) {
            handleInsuranceBackup(event, player);
        } else if (title.equals(RecycleBinGUI.TITLE)) {
            handleRecycleBin(event, player);
        }

        // 打开终端即完成引导第一步
        if (ECOSTerminalGUI.isTitle(title)) {
            plugin.getNewbieGuideManager().mark(player.getUniqueId(),
                    com.etherstories.escore.managers.NewbieGuideManager.Step.OPEN_TERMINAL);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (ECOSTerminalGUI.isTitle(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    // ── ECOS Terminal ─────────────────────────────────────────────────────────

    private void handleTerminal(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        String act = plugin.getEcosTerminalGUI().actionAt(player, event.getRawSlot());
        if (act == null) return;
        if (act.startsWith("tab:")) {
            ECOSTerminalGUI.Page page = ECOSTerminalGUI.Page.valueOf(act.substring(4));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player, page));
            return;
        }
        switch (act) {
            case "head" -> player.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getVersionDisplayTemplate()
                            .replace("{version}",    plugin.getDescription().getVersion())
                            .replace("{mc_version}", plugin.getServer().getVersion())
                            .replace("{authors}",    String.join(", ", plugin.getDescription().getAuthors()))
            ));

            case "close" -> player.closeInventory();
            case "aura" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAuraShopGUI().open(player));
            }
            case "friends" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getFriendListGUI().open(player));
            }
            case "mail" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMailboxGUI().open(player));
            }
            case "online" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getOnlineListGUI().open(player));
            }
            case "notices" -> {
                player.closeInventory();
                plugin.getNewbieGuideManager().mark(player.getUniqueId(),
                        com.etherstories.escore.managers.NewbieGuideManager.Step.READ_NOTICE);
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getNoticeGUI().open(player));
            }
            case "leaderboard" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getLeaderboardGUI().open(player));
            }
            case "waypoints" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getWaypointGUI().open(player));
            }
            case "status" -> {
                player.closeInventory();
                awaitingStatus.add(player.getUniqueId());
                String cur = plugin.getStatusManager().getStatus(player.getUniqueId());
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7在聊天框输入签名内容，或输入 &fcancel &7取消"
                        + (cur != null ? "&8（当前: &7" + cur + "&8）" : "")));
            }
            case "death" -> {
                player.closeInventory();
                List<DeathManager.DeathRecord> deaths = plugin.getDeathManager().get(player.getUniqueId());
                if (deaths.isEmpty()) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7暂无死亡记录"));
                } else {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7死亡记录:"));
                    for (DeathManager.DeathRecord d : deaths)
                        player.sendMessage(ColorUtil.colorize("  &8" + d.date() + "  &7"
                                + d.world() + " (" + d.x() + "," + d.y() + "," + d.z()
                                + ")  &8" + d.cause()));
                }
            }
            case "checkin" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getCheckInCalendarGUI().open(player));
            }
            case "summary" ->
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            case "balance" -> {
                if (!plugin.getVaultHook().isEnabled()) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7经济系统不可用"));
                    return;
                }
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7余额: &f"
                        + plugin.getVaultHook().format(plugin.getVaultHook().getBalance(player))));
            }
            case "estate" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateBuildingsGUI().open(player));
            }
            case "adm-estate" -> {
                if (!player.hasPermission("es2uni.admin")) return;
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateMineGUI().openAdmin(player));
            }
            case "trade" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTradeBoardGUI().open(player));
            }
            case "lucky" -> {
                player.closeInventory();
                if (plugin.getLuckyBlockManager().isOpening(player.getUniqueId())) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7正在开启中…"));
                    return;
                }
                boolean before = plugin.getLuckyBlockManager().hasClaimedToday(player.getUniqueId());
                plugin.getLuckyBlockManager().claimAnimated(player, msg -> {
                    player.sendMessage(ColorUtil.colorize(msg));
                    if (!before && plugin.getLuckyBlockManager().hasClaimedToday(player.getUniqueId())) {
                        plugin.getNewbieGuideManager().mark(player.getUniqueId(),
                                com.etherstories.escore.managers.NewbieGuideManager.Step.CLAIM_LUCKY);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> plugin.getEcosTerminalGUI().open(player), 8L);
                });
            }
            case "showcase" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getShowcaseGUI().open(player));
            }
            case "municipal" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMunicipalOfficeGUI().open(player));
            }
            case "newbie" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getNewbieGuideGUI().open(player));
            }
            case "tps" -> {
                player.closeInventory();
                double[] all = com.etherstories.escore.utils.TPSUtil.getAllTPS();
                double mspt = com.etherstories.escore.utils.TPSUtil.getMSPT();
                player.sendMessage(ColorUtil.colorize(
                        plugin.getConfigManager().getTpsDisplayTemplate()
                                .replace("{tps_1m}",  com.etherstories.escore.utils.TPSUtil.formatTPS(all[0]))
                                .replace("{tps_5m}",  com.etherstories.escore.utils.TPSUtil.formatTPS(all[1]))
                                .replace("{tps_15m}", com.etherstories.escore.utils.TPSUtil.formatTPS(all[2]))
                                .replace("{mspt}",    com.etherstories.escore.utils.TPSUtil.formatMSPT(mspt))
                ));
            }
            case "events" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getEventListGUI().open(player));
            }
            case "afk" -> {
                player.closeInventory();
                plugin.getAfkManager().manualToggle(player);
            }
            case "home" -> {
                if (!plugin.getEssentialsHook().isEnabled()) return;
                if (event.isRightClick()) {
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> player.performCommand("sethome"), 1L);
                } else {
                    player.closeInventory();
                    Bukkit.getScheduler().runTask(plugin,
                            () -> plugin.getHomeListGUI().open(player));
                }
            }
            case "tpa" -> {
                if (!plugin.getEssentialsHook().isEnabled()) return;
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getPlayerSelectGUI().open(player, PlayerSelectGUI.SelectContext.TPA));
            }
            case "tpahere" -> {
                if (!plugin.getEssentialsHook().isEnabled()) return;
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getPlayerSelectGUI().open(player, PlayerSelectGUI.SelectContext.TPA_HERE));
            }
            case "pay" -> {
                if (!plugin.getVaultHook().isEnabled()) return;
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getPlayerSelectGUI().open(player, PlayerSelectGUI.SelectContext.PAY));
            }
            case "adm-events" -> {
                if (!player.hasPermission("es2uni.admin")) return;
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminEventGUI().open(player));
            }
            case "adm-bc" -> {
                if (!player.hasPermission("es2uni.admin")) return;
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminBroadcastGUI().open(player));
            }
            case "adm-tax" -> {
                if (!player.hasPermission("es2uni.admin")) return;
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminTaxGUI().open(player));
            }
            case "adm-reload" -> {
                if (!player.hasPermission("es2uni.admin")) return;
                plugin.reload();
                plugin.getAuditLogManager().log(player, "RELOAD", "config reload");
                player.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getReloadMessage()));
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            }
            case "adm-territory" -> {
                if (!player.hasPermission("es2uni.admin")) return;
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminTerritoryGUI().open(player));
            }
            case "adm-regions" -> {
                if (!player.hasPermission("es2uni.admin")) return;
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminRegionGUI().open(player));
            }
            case "hot-places" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTerritoryListGUI().open(player));
            }
            case "my-territory" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMyTerritoryGUI().open(player));
            }
            case "kit" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getKitListGUI().open(player));
            }
            case "adm-kit" -> {
                if (!player.hasPermission("es2uni.admin")) return;
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminKitGUI().open(player));
            }
            case "music" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicMenuGUI().open(player));
            }
            case "link" -> {
                player.closeInventory();
                if (Bukkit.getPluginManager().getPlugin("ESLink") == null
                        || !Bukkit.getPluginManager().getPlugin("ESLink").isEnabled()) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &c未安装 ESLink"));
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("link"));
            }
            case "music-listen" -> {
                if (!plugin.getAllMusicHook().ensureHooked(plugin.getLogger())) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &cAllMusic 未加载"));
                    return;
                }
                boolean muted = plugin.getAllMusicHook().isMuted(player);
                plugin.getAllMusicHook().setListening(plugin, player, muted);
                boolean nowMuted = !muted;
                player.sendMessage(ColorUtil.colorize(nowMuted
                        ? "&8[ECOS] &c已关闭听歌（静音）。别人点歌你仍能看到提示，但不会播放。"
                        : "&8[ECOS] &a已开启听歌。"));
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> plugin.getEcosTerminalGUI().open(player), 3L);
            }
        }
    }

    private void handleEstateBuildings(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == EstateBuildingsGUI.SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot == EstateBuildingsGUI.SLOT_BACK) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == EstateBuildingsGUI.SLOT_ALL) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateBuildingsGUI().open(player, null));
            return;
        }
        if (slot == EstateBuildingsGUI.SLOT_RES) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateBuildingsGUI().open(player,
                    com.etherstories.escore.estate.BuildingCategory.RESIDENTIAL));
            return;
        }
        if (slot == EstateBuildingsGUI.SLOT_PUB) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateBuildingsGUI().open(player,
                    com.etherstories.escore.estate.BuildingCategory.PUBLIC));
            return;
        }
        if (slot == EstateBuildingsGUI.SLOT_COM) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateBuildingsGUI().open(player,
                    com.etherstories.escore.estate.BuildingCategory.COMMERCIAL));
            return;
        }
        if (slot == EstateBuildingsGUI.SLOT_MINE) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateMineGUI().openMine(player));
            return;
        }
        if (slot == EstateBuildingsGUI.SLOT_ADMIN) {
            if (!player.hasPermission("es2uni.admin")) return;
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateMineGUI().openAdmin(player));
            return;
        }
        String name = plugin.getEstateBuildingsGUI().buildingAt(player, slot);
        if (name == null) return;
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateRoomsGUI().open(player, name));
    }

    private void handleEstateRooms(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        String building = plugin.getEstateRoomsGUI().buildingOf(player);
        if (slot == EstateRoomsGUI.SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot == EstateRoomsGUI.SLOT_BACK) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateBuildingsGUI().open(player));
            return;
        }
        var u = plugin.getEstateRoomsGUI().unitAt(player, slot);
        if (u == null) return;
        var em = plugin.getEstateManager();
        u = em.byId(u.id());
        if (u == null) return;
        boolean mine = player.getUniqueId().equals(u.owner());
        boolean admin = player.hasPermission("es2uni.admin");
        if (event.isRightClick() && mine) {
            boolean next = !u.listed();
            String err = em.setSale(player, u, u.price(), next);
            if (err != null) player.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
            else player.sendMessage(ColorUtil.colorize(next
                    ? "&8[房产] &a已挂牌 &f" + u.address()
                    : "&8[房产] &7已下架 &f" + u.address()));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateRoomsGUI().open(player, building));
            return;
        }
        if (event.isLeftClick() && event.isShiftClick() && admin) {
            String err = em.delete(player, u);
            if (err != null) player.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
            else player.sendMessage(ColorUtil.colorize("&8[房产] &7已删除 &f" + u.address()));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateRoomsGUI().open(player, building));
            return;
        }
        if (event.isLeftClick() && event.isShiftClick() && u.listed() && u.price() > 0 && !mine) {
            estateTryBuy(player, u, () -> plugin.getEstateRoomsGUI().open(player, building));
            return;
        }
        if (event.isLeftClick() && (mine || admin || (u.listed() && u.price() > 0))) {
            player.closeInventory();
            String err = em.visit(player, u);
            if (err != null) {
                player.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                return;
            }
            player.sendMessage(ColorUtil.colorize(mine
                    ? "&8[房产] &7已到 &f" + u.address()
                    : "&8[房产] &7看房 &f" + u.address()
                    + (u.listed() && u.price() > 0 && !mine ? "  &8潜行左键购买" : "")));
        }
    }

    private void handleEstateMine(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        boolean adminView = plugin.getEstateMineGUI().isAdminView(player);
        if (slot == EstateMineGUI.SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot == EstateMineGUI.SLOT_BACK) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateBuildingsGUI().open(player));
            return;
        }
        var u = plugin.getEstateMineGUI().unitAt(player, slot);
        if (u == null) return;
        var em = plugin.getEstateManager();
        u = em.byId(u.id());
        if (u == null) return;
        if (event.isLeftClick() && event.isShiftClick() && adminView && player.hasPermission("es2uni.admin")) {
            String err = em.delete(player, u);
            if (err != null) player.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
            else player.sendMessage(ColorUtil.colorize("&8[房产] &7已删除 &f" + u.address()));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateMineGUI().openAdmin(player));
            return;
        }
        if (event.isLeftClick()) {
            player.closeInventory();
            em.teleport(player, u);
            player.sendMessage(ColorUtil.colorize("&8[房产] &7已到 &f" + u.address()));
        }
    }

    private void estateTryBuy(Player player, com.etherstories.escore.estate.EstateUnit u, Runnable reopen) {
        var em = plugin.getEstateManager();
        if (em.hasPending(player.getUniqueId(), u.id())) {
            String err = em.confirmBuy(player);
            if (err != null) {
                player.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                return;
            }
            player.sendMessage(ColorUtil.colorize("&8[房产] &a已购入 &f" + u.address()));
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, reopen);
            return;
        }
        String err = em.beginBuy(player, u);
        if (err != null) {
            player.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
            return;
        }
        String price = plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(u.price())
                : String.format("%.0f", u.price());
        player.sendMessage(ColorUtil.colorize("&8[房产] &e15秒内再点一次确认购买 &f" + u.address() + " &e" + price));
    }

    // ── Player Event List ─────────────────────────────────────────────────────

    private void handleEventList(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (EventListGUI.isCloseSlot(slot)) { player.closeInventory(); return; }

        ServerEvent ev = plugin.getEventListGUI().getEventAt(slot, player);
        if (ev == null) return;

        if (ev.hasJoined(player.getUniqueId())) {
            plugin.getEventManager().leave(ev.getId(), player.getUniqueId());
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已退出活动 &f" + ev.getName()));
        } else {
            boolean ok = plugin.getEventManager().join(ev.getId(), player.getUniqueId());
            player.sendMessage(ok
                    ? ColorUtil.colorize("&8[ECOS] &7已参与活动 &f" + ev.getName())
                    : ColorUtil.colorize("&8[ECOS] &7活动已满员"));
        }
        plugin.getEventListGUI().open(player);
    }

    // ── Admin Event ───────────────────────────────────────────────────────────

    private void handleAdminEvent(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (AdminEventGUI.isCloseSlot(slot)) { player.closeInventory(); return; }
        if (AdminEventGUI.isBackSlot(slot)) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (AdminEventGUI.isCreateSlot(slot)) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> plugin.getAnvilInputGUI().openForEventName(player), 1L);
            return;
        }
        ServerEvent ev = plugin.getAdminEventGUI().getEventAt(slot);
        if (ev == null) return;
        if (event.isRightClick()) {
            plugin.getEventManager().removeEvent(ev.getId());
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7活动 &f" + ev.getName() + " &7已删除"));
        }
        plugin.getAdminEventGUI().open(player);
    }

    // ── Home List ─────────────────────────────────────────────────────────────

    private void handleHomeList(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        int size = event.getView().getTopInventory().getSize();

        if (slot == HomeListGUI.getCloseSlot(size)) { player.closeInventory(); return; }
        if (slot == HomeListGUI.getBackSlot(size)) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String homeName = item.getItemMeta().getDisplayName()
                .replaceAll("§[0-9a-fk-or]", "").trim();

        player.closeInventory();
        player.performCommand("home " + homeName);
    }

    // ── Player Select (TPA / TPA Here / Pay / Mail) ───────────────────────────

    private void handlePlayerSelect(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        PlayerSelectGUI gui = plugin.getPlayerSelectGUI();

        if (slot == PlayerSelectGUI.getCloseSlot()) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == PlayerSelectGUI.getBackSlot()) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }

        UUID targetUuid = gui.getTargetAt(player, slot);
        if (targetUuid == null) return;

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) { player.sendMessage(ColorUtil.colorize("&8[ECOS] &7玩家已离线")); return; }

        PlayerSelectGUI.SelectContext ctx = gui.getContext(player);
        gui.cleanup(player);
        player.closeInventory();

        switch (ctx) {
            case TPA      -> Bukkit.getScheduler().runTaskLater(plugin,
                    () -> player.performCommand("tpa " + target.getName()), 1L);
            case TPA_HERE -> Bukkit.getScheduler().runTaskLater(plugin,
                    () -> player.performCommand("tpahere " + target.getName()), 1L);
            case PAY      -> Bukkit.getScheduler().runTask(plugin,
                    () -> plugin.getAnvilInputGUI().openForPayAmount(player, target));
            case MAIL     -> {
                plugin.getMailManager().setPending(player.getUniqueId(), target.getUniqueId());
                ItemStack book = new ItemStack(org.bukkit.Material.WRITABLE_BOOK);
                BookMeta bm = (BookMeta) book.getItemMeta();
                if (bm != null) {
                    bm.setTitle("draft");
                    bm.setAuthor(player.getName());
                    book.setItemMeta(bm);
                }
                int emptySlot = player.getInventory().firstEmpty();
                if (emptySlot == -1) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7背包已满，无法给予书"));
                    plugin.getMailManager().consumePending(player.getUniqueId());
                } else {
                    player.getInventory().setItem(emptySlot, book);
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7书已放入背包，写完后签名即可发送"));
                }
            }
        }
    }

    // ── Pay Confirm ───────────────────────────────────────────────────────────

    private void handlePayConfirm(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        PayConfirmGUI gui = plugin.getPayConfirmGUI();
        PayConfirmGUI.PendingPay pay = gui.getPending(player);

        if (slot == PayConfirmGUI.CANCEL_SLOT) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }

        if (slot == PayConfirmGUI.CONFIRM_SLOT) {
            if (pay == null) { player.closeInventory(); return; }
            gui.cleanup(player);
            player.closeInventory();

            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(pay.targetUuid());
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7找不到收款玩家"));
                return;
            }

            double tax = plugin.getTaxManager().isPayTaxEnabled()
                    ? plugin.getTaxManager().calcTax(pay.amount(), plugin.getTaxManager().getPayTaxRate()) : 0;
            String error = plugin.getVaultHook().pay(player, target, pay.amount(), tax);
            if (error != null) {
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7转账失败: " + error));
            } else {
                String fmt = plugin.getVaultHook().format(pay.amount());
                String name = target.getName() != null ? target.getName() : "玩家";
                if (tax > 0) {
                    player.sendMessage(ColorUtil.colorize(
                            "&8[ECOS] &7已向 &f" + name + " &7转账 &f" + fmt
                                    + (target.isOnline() ? "" : " &8(离线)")
                                    + " &8(含税实付 " + plugin.getVaultHook().format(pay.amount() + tax) + ")"));
                } else {
                    player.sendMessage(ColorUtil.colorize(
                            "&8[ECOS] &7已向 &f" + name + " &7转账 &f" + fmt
                                    + (target.isOnline() ? "" : " &8(离线)")));
                }
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(ColorUtil.colorize(
                            "&8[ECOS] &7收到 &f" + player.getName() + " &7的转账 &f" + fmt));
                }
            }
        }
    }

    // ── Online List ───────────────────────────────────────────────────────────

    private void handleOnlineList(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        OnlineListGUI gui = plugin.getOnlineListGUI();

        if (slot == OnlineListGUI.CLOSE_SLOT) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == OnlineListGUI.BACK_SLOT) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }

        UUID targetUuid = gui.getTargetAt(player, slot);
        if (targetUuid == null) return;
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7玩家已离线"));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }

        gui.cleanup(player);
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin,
                () -> plugin.getPlayerActionGUI().open(player, target));
    }

    // ── Player Action ─────────────────────────────────────────────────────────

    private void handlePlayerAction(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        PlayerActionGUI gui = plugin.getPlayerActionGUI();
        UUID targetUuid = gui.getTarget(player);

        if (slot == PlayerActionGUI.SLOT_CLOSE) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == PlayerActionGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getOnlineListGUI().open(player));
            return;
        }

        if (targetUuid == null) return;
        Player target = Bukkit.getPlayer(targetUuid);

        switch (slot) {
            case PlayerActionGUI.SLOT_WHISPER -> {
                gui.cleanup(player);
                player.closeInventory();
                if (target == null) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7玩家已离线"));
                    return;
                }
                awaitingWhisper.put(player.getUniqueId(), targetUuid);
                player.sendMessage(ColorUtil.colorize(
                        "&8[ECOS] &7正在私信 &f" + target.getName() + " &7— 在聊天框输入内容，或输入 &fcancel &7取消"));
            }
            case PlayerActionGUI.SLOT_MAIL -> {
                gui.cleanup(player);
                player.closeInventory();
                if (target == null) { player.sendMessage(ColorUtil.colorize("&8[ECOS] &7玩家已离线")); return; }
                plugin.getMailManager().setPending(player.getUniqueId(), targetUuid);
                ItemStack book = new ItemStack(org.bukkit.Material.WRITABLE_BOOK);
                BookMeta bm = (BookMeta) book.getItemMeta();
                if (bm != null) { bm.setTitle("draft"); bm.setAuthor(player.getName()); book.setItemMeta(bm); }
                int emptySlot = player.getInventory().firstEmpty();
                if (emptySlot == -1) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7背包已满"));
                    plugin.getMailManager().consumePending(player.getUniqueId());
                } else {
                    player.getInventory().setItem(emptySlot, book);
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7书已放入背包，写完后签名即可发送"));
                }
            }
            case PlayerActionGUI.SLOT_FRIEND -> {
                if (plugin.getFriendManager().areFriends(player.getUniqueId(), targetUuid)) {
                    plugin.getFriendManager().removeFriend(player.getUniqueId(), targetUuid);
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已删除好友"));
                } else {
                    if (target == null) { player.sendMessage(ColorUtil.colorize("&8[ECOS] &7玩家已离线")); return; }
                    boolean sent = plugin.getFriendManager().sendRequest(player, target);
                    player.sendMessage(sent
                            ? ColorUtil.colorize("&8[ECOS] &7好友请求已发送")
                            : ColorUtil.colorize("&8[ECOS] &7请求已发送或已是好友"));
                    if (sent && target.isOnline())
                        target.sendMessage(ColorUtil.colorize("&8[ECOS] &f" + player.getName() + " &7向你发送了好友请求"));
                }
                if (target != null && target.isOnline())
                    Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, target));
                else {
                    gui.cleanup(player); player.closeInventory();
                }
            }
            case PlayerActionGUI.SLOT_PAY -> {
                if (!plugin.getVaultHook().isEnabled()) return;
                gui.cleanup(player);
                player.closeInventory();
                if (target == null) { player.sendMessage(ColorUtil.colorize("&8[ECOS] &7玩家已离线")); return; }
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getAnvilInputGUI().openForPayAmount(player, target));
            }
            case PlayerActionGUI.SLOT_REPORT -> {
                if (!plugin.getReportManager().isEnabled()) return;
                UUID about = targetUuid;
                gui.cleanup(player);
                player.closeInventory();
                awaitingReport.put(player.getUniqueId(), about);
                player.sendMessage(ColorUtil.colorize(
                        "&8[ECOS] &7输入悄悄话内容（关于 &f"
                                + (target != null ? target.getName() : "?")
                                + "&7），仅管理员可见。&fcancel &7取消"));
            }
        }
    }

    // ── Notice Board ──────────────────────────────────────────────────────────

    private void handleNotice(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == NoticeGUI.CLOSE_SLOT) { player.closeInventory(); return; }
        if (slot == NoticeGUI.BACK_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == NoticeGUI.POST_SLOT && player.hasPermission("es2uni.admin")) {
            player.closeInventory();
            giveNoticeDraftBook(player);
            return;
        }

        if (plugin.getNoticeGUI().isNoticeSlot(slot)) {
            if (event.isLeftClick()) {
                plugin.getNoticeGUI().openDetail(player, slot);
            } else if (event.isRightClick() && player.hasPermission("es2uni.admin")) {
                int idx = plugin.getNoticeGUI().getNoticeIndex(player, slot);
                if (idx >= 0) {
                    plugin.getNoticeManager().removeNotice(idx);
                    plugin.getAuditLogManager().log(player, "NOTICE_DELETE", "index=" + idx);
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7公告已删除"));
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getNoticeGUI().open(player));
                }
            }
        }
    }

    // ── Leaderboard ───────────────────────────────────────────────────────────

    private void handleLeaderboard(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == LeaderboardGUI.CLOSE_SLOT) { player.closeInventory(); return; }
        if (slot == LeaderboardGUI.BACK_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == LeaderboardGUI.TRANSIT_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitBoardGUI().open(player));
        }
    }

    private void handleTransitBoard(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == TransitBoardGUI.CLOSE_SLOT) { player.closeInventory(); return; }
        if (slot == TransitBoardGUI.BACK_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitOfficeGUI().open(player));
            return;
        }
        if (slot == TransitBoardGUI.PLAYTIME_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getLeaderboardGUI().open(player));
        }
    }

    // ── Waypoint GUI ──────────────────────────────────────────────────────────

    private void handleWaypointGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        WaypointGUI gui = plugin.getWaypointGUI();

        if (slot == WaypointGUI.CLOSE_SLOT) { player.closeInventory(); return; }
        if (slot == WaypointGUI.BACK_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == WaypointGUI.ADD_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> plugin.getAnvilInputGUI().openForWaypointName(player), 1L);
            return;
        }
        if (gui.isListSlot(slot, player)) {
            if (event.isLeftClick()) {
                player.closeInventory();
                gui.teleport(player, slot);
            } else if (event.isRightClick()) {
                gui.delete(player, slot);
            }
        }
    }

    // ── Territory List (热门地点) ──────────────────────────────────────────────

    private void handleTerritoryList(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        TerritoryListGUI gui = plugin.getTerritoryListGUI();

        if (slot == TerritoryListGUI.CLOSE_SLOT) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == TerritoryListGUI.BACK_SLOT) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == TerritoryListGUI.PREV_SLOT) { gui.prevPage(player); return; }
        if (slot == TerritoryListGUI.NEXT_SLOT) { gui.nextPage(player); return; }
        if (slot == TerritoryListGUI.INFO_SLOT)  return;

        if (slot >= 0 && slot < 45) {
            RegionManager.Region r = gui.getAt(player, slot);
            if (r == null) return;
            gui.cleanup(player);
            player.closeInventory();
            gui.teleport(player, r);
        }
    }

    // ── Admin Territory GUI ───────────────────────────────────────────────────

    private void handleAdminTerritory(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (!player.hasPermission("es2uni.admin")) return;
        int slot = event.getRawSlot();
        AdminTerritoryGUI gui = plugin.getAdminTerritoryGUI();

        if (slot == AdminTerritoryGUI.CLOSE_SLOT) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == AdminTerritoryGUI.BACK_SLOT) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == AdminTerritoryGUI.PREV_SLOT) { gui.prevPage(player); return; }
        if (slot == AdminTerritoryGUI.NEXT_SLOT) { gui.nextPage(player); return; }
        if (slot == AdminTerritoryGUI.INFO_SLOT)  return;

        if (slot >= 0 && slot < 45) {
            RegionManager.Region r = gui.getAt(player, slot);
            if (r == null) return;
            if (event.isLeftClick()) {
                player.closeInventory();
                gui.teleport(player, r);
            } else if (event.isRightClick()) {
                plugin.getRegionManager().toggleVisible(r.name(), player.getUniqueId(), true);
                boolean newState = !r.visible();
                player.sendMessage(ColorUtil.colorize(
                        "&8[ECOS] &7领地 &f" + r.name() + " &7已设为" + (newState ? "&a公开" : "&8隐藏")));
                Bukkit.getScheduler().runTask(plugin, () -> gui.refresh(player));
            }
        }
    }

    // ── My Territory GUI ──────────────────────────────────────────────────────

    private void handleMyTerritory(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        MyTerritoryGUI gui = plugin.getMyTerritoryGUI();

        if (slot == MyTerritoryGUI.CLOSE_SLOT) { gui.cleanup(player); player.closeInventory(); return; }
        if (slot == MyTerritoryGUI.BACK_SLOT) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == MyTerritoryGUI.PREV_SLOT) { gui.prevPage(player); return; }
        if (slot == MyTerritoryGUI.NEXT_SLOT) { gui.nextPage(player); return; }
        if (slot == MyTerritoryGUI.INFO_SLOT)  return;

        if (slot >= 0 && slot < 45) {
            RegionManager.Region r = gui.getAt(player, slot);
            if (r == null) return;
            if (event.isLeftClick() && event.isShiftClick()) {
                var loc = player.getLocation();
                plugin.getRegionManager().setSpawn(r.name(), player.getUniqueId(), false,
                        loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())
                        .ifPresentOrElse(
                                next -> player.sendMessage(ColorUtil.colorize(
                                        "&8[ECOS] &7领地 &f" + next.name() + " &7传送点已设为 &f"
                                                + next.spawnX() + " " + next.spawnY() + " " + next.spawnZ())),
                                () -> player.sendMessage(ColorUtil.colorize("&8[ECOS] &7未找到领地或无权限")));
                Bukkit.getScheduler().runTask(plugin, () -> gui.refresh(player));
            } else if (event.isLeftClick()) {
                player.closeInventory();
                gui.teleport(player, r);
            } else if (event.isRightClick()) {
                plugin.getRegionManager().toggleVisible(r.name(), player.getUniqueId(), false);
                boolean newState = !r.visible();
                player.sendMessage(ColorUtil.colorize(
                        "&8[ECOS] &7领地 &f" + r.name() + " &7已设为" + (newState ? "&a公开" : "&8隐藏")));
                Bukkit.getScheduler().runTask(plugin, () -> gui.refresh(player));
            }
        }
    }

    // ── Admin Region (District) GUI ───────────────────────────────────────────

    private void handleAdminRegion(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (!player.hasPermission("es2uni.admin")) return;
        int slot = event.getRawSlot();
        AdminRegionGUI gui = plugin.getAdminRegionGUI();

        if (slot == AdminRegionGUI.CLOSE_SLOT) { gui.cleanup(player); player.closeInventory(); return; }
        if (slot == AdminRegionGUI.BACK_SLOT) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == AdminRegionGUI.PREV_SLOT) { gui.prevPage(player); return; }
        if (slot == AdminRegionGUI.NEXT_SLOT) { gui.nextPage(player); return; }
        if (slot == AdminRegionGUI.INFO_SLOT)  return;

        if (slot >= 0 && slot < 45) {
            RegionManager.Region r = gui.getAt(player, slot);
            if (r == null) return;
            if (event.isLeftClick()) {
                player.closeInventory();
                gui.teleport(player, r);
            } else if (event.isRightClick()) {
                plugin.getRegionManager().deleteByName(r.name(), player.getUniqueId(), true);
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7地区 &f" + r.name() + " &7已删除"));
                Bukkit.getScheduler().runTask(plugin, () -> gui.refresh(player));
            }
        }
    }

    // ── Mailbox ───────────────────────────────────────────────────────────────

    private void handleMailbox(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        MailboxGUI gui = plugin.getMailboxGUI();

        if (slot == MailboxGUI.SLOT_CLOSE || slot == MailboxGUI.SLOT_BACK) {
            player.closeInventory();
            if (slot == MailboxGUI.SLOT_BACK)
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }

        if (slot == MailboxGUI.SLOT_SEND) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin,
                    () -> plugin.getPlayerSelectGUI().open(player, PlayerSelectGUI.SelectContext.MAIL));
            return;
        }

        if (gui.isMailSlot(slot)) {
            if (event.isLeftClick())       gui.openMail(player, slot);
            else if (event.isRightClick()) gui.deleteMail(player, slot);
        }
    }

    private void handleAdminTax(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (!player.hasPermission("es2uni.admin")) return;
        int slot = event.getRawSlot();
        var tax = plugin.getTaxManager();

        switch (slot) {
            case AdminTaxGUI.SLOT_CLOSE -> player.closeInventory();
            case AdminTaxGUI.SLOT_BACK -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            }
            case AdminTaxGUI.SLOT_PAY_TOGGLE -> {
                tax.setPayEnabled(!tax.isPayTaxEnabled());
                plugin.getAuditLogManager().log(player, "TAX_PAY_TOGGLE", "enabled=" + tax.isPayTaxEnabled());
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminTaxGUI().open(player));
            }
            case AdminTaxGUI.SLOT_PAY_RATE_DOWN -> {
                tax.setPayRate(tax.getPayTaxRate() - 0.01);
                plugin.getAuditLogManager().log(player, "TAX_PAY_RATE", tax.formatRate(tax.getPayTaxRate()));
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminTaxGUI().open(player));
            }
            case AdminTaxGUI.SLOT_PAY_RATE_UP -> {
                tax.setPayRate(tax.getPayTaxRate() + 0.01);
                plugin.getAuditLogManager().log(player, "TAX_PAY_RATE", tax.formatRate(tax.getPayTaxRate()));
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminTaxGUI().open(player));
            }
            case AdminTaxGUI.SLOT_QS_TOGGLE -> {
                tax.setQuickShopEnabled(!tax.isQuickShopTaxEnabled());
                plugin.getAuditLogManager().log(player, "TAX_QS_TOGGLE", "enabled=" + tax.isQuickShopTaxEnabled());
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminTaxGUI().open(player));
            }
            case AdminTaxGUI.SLOT_QS_RATE_DOWN -> {
                tax.setQuickShopRate(tax.getQuickShopTaxRate() - 0.01);
                plugin.getAuditLogManager().log(player, "TAX_QS_RATE", tax.formatRate(tax.getQuickShopTaxRate()));
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminTaxGUI().open(player));
            }
            case AdminTaxGUI.SLOT_QS_RATE_UP -> {
                tax.setQuickShopRate(tax.getQuickShopTaxRate() + 0.01);
                plugin.getAuditLogManager().log(player, "TAX_QS_RATE", tax.formatRate(tax.getQuickShopTaxRate()));
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminTaxGUI().open(player));
            }
            case AdminTaxGUI.SLOT_LINK_TOGGLE -> {
                var link = plugin.getESLinkHook();
                if (!link.present()) return;
                link.setTradeEnabled(!link.tradeEnabled());
                plugin.getAuditLogManager().log(player, "TAX_LINK_TOGGLE", "enabled=" + link.tradeEnabled());
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminTaxGUI().open(player));
            }
            case AdminTaxGUI.SLOT_LINK_RATE_DOWN -> {
                var link = plugin.getESLinkHook();
                if (!link.present()) return;
                link.setTaxRate(link.taxRate() - 0.01);
                plugin.getAuditLogManager().log(player, "TAX_LINK_RATE", link.formatRate());
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminTaxGUI().open(player));
            }
            case AdminTaxGUI.SLOT_LINK_RATE_UP -> {
                var link = plugin.getESLinkHook();
                if (!link.present()) return;
                link.setTaxRate(link.taxRate() + 0.01);
                plugin.getAuditLogManager().log(player, "TAX_LINK_RATE", link.formatRate());
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminTaxGUI().open(player));
            }
            default -> { }
        }
    }

    private void handleAdminBroadcast(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (!player.hasPermission("es2uni.admin")) return;
        int slot = event.getRawSlot();
        AdminBroadcastGUI gui = plugin.getAdminBroadcastGUI();

        if (slot == AdminBroadcastGUI.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == AdminBroadcastGUI.SLOT_BACK) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == AdminBroadcastGUI.SLOT_ADD) {
            player.closeInventory();
            awaitingBcAdd.add(player.getUniqueId());
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7在聊天输入要添加的定时消息，或 &fcancel"));
            return;
        }
        if (slot == AdminBroadcastGUI.SLOT_INTERVAL) {
            long cur = plugin.getConfigManager().getBroadcastInterval();
            long next = event.isRightClick() ? cur - 30 : cur + 30;
            gui.setInterval(next);
            plugin.getAuditLogManager().log(player, "BC_INTERVAL",
                    plugin.getConfigManager().getBroadcastInterval() + "s");
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7广播间隔已设为 &f"
                    + plugin.getConfigManager().getBroadcastInterval() + "s"));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        if (slot == AdminBroadcastGUI.SLOT_ONCE) {
            player.closeInventory();
            awaitingBroadcast.add(player.getUniqueId());
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7在聊天输入立即广播内容，或 &fcancel"));
            return;
        }
        if (slot >= 0 && slot < 45) {
            var entries = new ArrayList<>(plugin.getConfigManager().getBroadcastEntries());
            if (slot >= entries.size()) return;
            if (event.isRightClick()) {
                entries.remove(slot);
                gui.saveEntries(entries);
                plugin.getAuditLogManager().log(player, "BC_DELETE", "index=" + (slot + 1));
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已删除消息 #" + (slot + 1)));
                Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            } else if (event.isLeftClick() && event.isShiftClick()) {
                gui.cycleDays(slot);
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已切换星期限制"));
                Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            } else if (event.isLeftClick()) {
                player.closeInventory();
                awaitingBcEdit.put(player.getUniqueId(), slot);
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7编辑消息 #" + (slot + 1)
                        + "，在聊天输入新内容，或 &fcancel"));
            }
        }
    }

    private void handleCheckInCalendar(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        CheckInCalendarGUI gui = plugin.getCheckInCalendarGUI();
        if (slot == CheckInCalendarGUI.SLOT_CLOSE) {
            gui.cleanup(player);
            player.closeInventory();
            return;
        }
        if (slot == CheckInCalendarGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == CheckInCalendarGUI.SLOT_PREV) {
            Bukkit.getScheduler().runTask(plugin,
                    () -> gui.open(player, gui.getMonth(player).minusMonths(1)));
            return;
        }
        if (slot == CheckInCalendarGUI.SLOT_NEXT) {
            Bukkit.getScheduler().runTask(plugin,
                    () -> gui.open(player, gui.getMonth(player).plusMonths(1)));
            return;
        }
        if (slot == CheckInCalendarGUI.SLOT_DO) {
            String msg = plugin.getCheckInManager().checkInWithReward(player);
            player.sendMessage(ColorUtil.colorize(msg));
            if (msg.contains("签到成功")) {
                plugin.getNewbieGuideManager().mark(player.getUniqueId(),
                        com.etherstories.escore.managers.NewbieGuideManager.Step.CHECK_IN);
            }
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }

        // 红日补签
        int day = gui.getDayAt(player, slot);
        if (day > 0) {
            var month = gui.getMonth(player);
            var date = month.atDay(day);
            if (date.isBefore(java.time.LocalDate.now())
                    && !plugin.getCheckInManager().hasCheckedOn(player.getUniqueId(), date)) {
                String err = plugin.getCheckInManager().makeup(player.getUniqueId(), date);
                if (err != null) player.sendMessage(ColorUtil.colorize("&8[ECOS] &7" + err));
                else player.sendMessage(ColorUtil.colorize(
                        "&8[ECOS] &a已补签 &f" + date + " &7剩余券 &e"
                                + plugin.getCheckInManager().getMakeupTickets(player.getUniqueId())));
                Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            }
        }
    }

    private void handleTradeBoard(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == TradeBoardGUI.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == TradeBoardGUI.SLOT_BACK) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
        }
    }

    private void handleShowcase(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ShowcaseGUI gui = plugin.getShowcaseGUI();
        if (slot == ShowcaseGUI.SLOT_CLOSE) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == ShowcaseGUI.SLOT_BACK) {
            gui.cleanup(player); player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == ShowcaseGUI.SLOT_REGISTER) {
            player.closeInventory();
            awaitingShowcaseTitle.add(player.getUniqueId());
            player.sendMessage(ColorUtil.colorize(
                    "&8[ECOS] &7输入展示点标题，或输入 &f- &7用默认名。&fcancel &7取消"));
            return;
        }
        String id = gui.getId(player, slot);
        if (id == null) return;
        if (event.isShiftClick() && event.isRightClick()) {
            boolean ok = plugin.getShowcaseManager().remove(id, player.getUniqueId(),
                    player.hasPermission("es2uni.admin"));
            player.sendMessage(ColorUtil.colorize(ok ? "&8[ECOS] &7已删除展示点" : "&8[ECOS] &7无法删除"));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        if (event.isRightClick()) {
            String err = plugin.getShowcaseManager().vote(player, id);
            player.sendMessage(ColorUtil.colorize(err == null ? "&8[ECOS] &a投票成功" : "&8[ECOS] &7" + err));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        if (event.isLeftClick()) {
            gui.cleanup(player);
            player.closeInventory();
            if (!plugin.getShowcaseManager().teleport(player, id))
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7传送失败（世界未加载）"));
            else
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已传送到展示点"));
        }
    }

    private void handleNewbieGuide(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == NewbieGuideGUI.SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot == NewbieGuideGUI.SLOT_BACK) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        var steps = com.etherstories.escore.managers.NewbieGuideManager.Step.values();
        int idx = slot - 9;
        if (idx >= 0 && idx < steps.length) {
            plugin.getNewbieGuideManager().toggle(player.getUniqueId(), steps[idx]);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getNewbieGuideGUI().open(player));
        }
    }

    private void handleMunicipalOffice(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == MunicipalOfficeGUI.SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot == MunicipalOfficeGUI.SLOT_BACK) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == MunicipalOfficeGUI.SLOT_JOBS) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getJobBoardGUI().open(player));
            return;
        }
        if (slot == MunicipalOfficeGUI.SLOT_CLEANUP) {
            player.closeInventory();
            String err = plugin.getCleanupRequestManager().request(player);
            player.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[城管] &a已发起清理申请，等待投票"
                    : "&8[城管] &c" + err));
            return;
        }
        if (slot == MunicipalOfficeGUI.SLOT_RECYCLE) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getRecycleBinGUI().open(player));
            return;
        }
        if (slot == MunicipalOfficeGUI.SLOT_INSURANCE) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getInsuranceGUI().open(player));
            return;
        }
        if (slot == MunicipalOfficeGUI.SLOT_TRANSIT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitOfficeGUI().open(player));
            return;
        }
        if (slot == MunicipalOfficeGUI.SLOT_PVE) {
            if (!player.hasPermission("es2uni.admin")) return;
            player.closeInventory();
            player.getInventory().addItem(com.etherstories.escore.items.PveItems.pack(),
                    com.etherstories.escore.items.PveItems.elite());
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已给刷怪棒 / 精英棒  &8右键刷、潜行换种类"));
        }
    }

    private void handleJobBoard(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        JobBoardGUI gui = plugin.getJobBoardGUI();
        if (slot == JobBoardGUI.SLOT_CLOSE) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == JobBoardGUI.SLOT_BACK) {
            gui.cleanup(player); player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMunicipalOfficeGUI().open(player));
            return;
        }
        if (slot == JobBoardGUI.SLOT_POST) {
            player.closeInventory();
            plugin.getAnvilInputGUI().openForJobPost(player, false);
            return;
        }
        if (slot == JobBoardGUI.SLOT_POST_OFFICIAL) {
            if (!player.hasPermission("es2uni.admin")) return;
            player.closeInventory();
            plugin.getAnvilInputGUI().openForJobPost(player, true);
            return;
        }
        if (slot == JobBoardGUI.SLOT_HISTORY) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getJobHistoryGUI().open(player));
            return;
        }
        String id = gui.getId(player, slot);
        if (id == null) return;
        var job = plugin.getJobBoardManager().get(id);
        if (job == null) return;

        if (event.isShiftClick() && event.isLeftClick() && player.hasPermission("es2uni.admin")) {
            String err = plugin.getJobBoardManager().forceRemove(player, id);
            player.sendMessage(ColorUtil.colorize(err == null ? "&8[招工处] &7已强制删除" : "&8[招工处] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        if (event.isShiftClick() && event.isRightClick()) {
            if (job.poster.equals(player.getUniqueId()) || player.hasPermission("es2uni.admin")) {
                if (!job.hasWorkers() && !job.locked) {
                    String err = plugin.getJobBoardManager().cancel(player, id);
                    player.sendMessage(ColorUtil.colorize(err == null
                            ? "&8[招工处] &7已取消并退款" : "&8[招工处] &c" + err));
                } else {
                    boolean lock = !job.locked;
                    String err = plugin.getJobBoardManager().setLocked(player, id, lock);
                    player.sendMessage(ColorUtil.colorize(err == null
                            ? (lock ? "&8[招工处] &c已锁定，不再接受新人"
                            : "&8[招工处] &a已解锁，可继续加人")
                            : "&8[招工处] &c" + err));
                }
                Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
                return;
            }
        }
        if (event.isRightClick() && player.hasPermission("es2uni.admin") && !event.isShiftClick()) {
            player.closeInventory();
            plugin.getAnvilInputGUI().openForJobEditTitle(player, id);
            return;
        }
        if (event.isLeftClick()) {
            if (job.hasRoom() && !job.poster.equals(player.getUniqueId())
                    && !job.workers.contains(player.getUniqueId())) {
                String err = plugin.getJobBoardManager().take(player, id);
                player.sendMessage(ColorUtil.colorize(err == null
                        ? "&8[招工处] &a已加入接单 (" + plugin.getJobBoardManager().get(id).slotsLabel() + ")"
                        : "&8[招工处] &c" + err));
            } else if (job.hasWorkers()
                    && (job.poster.equals(player.getUniqueId()) || player.hasPermission("es2uni.admin"))) {
                String err = plugin.getJobBoardManager().complete(player, id);
                player.sendMessage(ColorUtil.colorize(err == null
                        ? "&8[招工处] &a已完成并均分打款" : "&8[招工处] &c" + err));
            } else if (job.poster.equals(player.getUniqueId()) && !job.hasWorkers()) {
                player.sendMessage(ColorUtil.colorize("&8[招工处] &7等待他人接单中…"));
            } else if (job.workers.contains(player.getUniqueId())) {
                player.sendMessage(ColorUtil.colorize("&8[招工处] &7你已在名单中，等待发布者确认完成"));
            }
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
        }
    }

    private void handleJobHistory(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        JobHistoryGUI gui = plugin.getJobHistoryGUI();
        if (slot == JobHistoryGUI.SLOT_CLOSE) {
            gui.cleanup(player);
            player.closeInventory();
            return;
        }
        if (slot == JobHistoryGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getJobBoardGUI().open(player));
        }
    }

    private void handleInsurance(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        InsuranceGUI gui = plugin.getInsuranceGUI();
        if (slot == InsuranceGUI.SLOT_CLOSE) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == InsuranceGUI.SLOT_BACK) {
            gui.cleanup(player); player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMunicipalOfficeGUI().open(player));
            return;
        }
        if (slot == InsuranceGUI.SLOT_BACKUP) {
            plugin.getInsuranceManager().backup(player);
            var b = plugin.getInsuranceManager().getBackup(player.getUniqueId());
            player.sendMessage(ColorUtil.colorize(b == null
                    ? "&8[保险] &7背包为空，未更新备份"
                    : "&8[保险] &a已备份 &f" + b.itemCount() + " &a件"));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        if (slot == InsuranceGUI.SLOT_CLAIM) {
            String err = plugin.getInsuranceManager().requestClaim(player);
            player.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[保险] &a已提交理赔，等待管理员审批"
                    : "&8[保险] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        if (player.hasPermission("es2uni.admin")) {
            String claimId = gui.getClaimId(player, slot);
            if (claimId == null) return;
            if (event.isRightClick()) {
                String err = plugin.getInsuranceManager().deny(player, claimId);
                player.sendMessage(ColorUtil.colorize(err == null
                        ? "&8[保险] &7已拒绝" : "&8[保险] &c" + err));
                Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            } else {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getInsuranceBackupGUI().open(player, claimId));
            }
        }
    }

    private void handleInsuranceBackup(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        InsuranceBackupGUI gui = plugin.getInsuranceBackupGUI();
        String claimId = gui.getClaimId(player);
        if (slot == InsuranceBackupGUI.SLOT_CLOSE) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == InsuranceBackupGUI.SLOT_BACK) {
            gui.cleanup(player); player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getInsuranceGUI().open(player));
            return;
        }
        if (slot == InsuranceBackupGUI.SLOT_DENY) {
            if (claimId != null) {
                String err = plugin.getInsuranceManager().deny(player, claimId);
                player.sendMessage(ColorUtil.colorize(err == null
                        ? "&8[保险] &7已拒绝" : "&8[保险] &c" + err));
            }
            gui.cleanup(player); player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getInsuranceGUI().open(player));
            return;
        }
        Integer idx = gui.getBackupIndex(player, slot);
        if (idx == null || claimId == null) return;
        String err = plugin.getInsuranceManager().approve(player, claimId, idx);
        player.sendMessage(ColorUtil.colorize(err == null
                ? "&8[保险] &a已恢复所选备份并赔偿"
                : "&8[保险] &c" + err));
        gui.cleanup(player);
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getInsuranceGUI().open(player));
    }

    private void handleRecycleBin(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        RecycleBinGUI gui = plugin.getRecycleBinGUI();
        if (slot == RecycleBinGUI.SLOT_CLOSE) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == RecycleBinGUI.SLOT_BACK) {
            gui.cleanup(player); player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMunicipalOfficeGUI().open(player));
            return;
        }
        String id = gui.getId(player, slot);
        if (id == null) return;
        String err = plugin.getRecycleBinManager().reclaim(player, id);
        player.sendMessage(ColorUtil.colorize(err == null
                ? "&8[回收站] &a已取回"
                : "&8[回收站] &c" + err));
        Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
    }

    private void handleTransitOffice(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == TransitOfficeGUI.SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot == TransitOfficeGUI.SLOT_BACK) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMunicipalOfficeGUI().open(player));
            return;
        }
        var tm = plugin.getTransitManager();
        if (slot == TransitOfficeGUI.SLOT_TAPPAY) {
            boolean on = !tm.isTapPay(player.getUniqueId());
            String err = tm.setTapPay(player, on);
            if (err != null) player.sendMessage(ColorUtil.colorize("&8[交通] &c" + err));
            else player.sendMessage(ColorUtil.colorize(on ? "&8[交通] &a已开通闪付" : "&8[交通] &7已关闭闪付"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitOfficeGUI().open(player));
            return;
        }
        if (slot == TransitOfficeGUI.SLOT_CARD) {
            String err = tm.buyCard(player);
            player.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已发放交通卡" : "&8[交通] &c" + err));
            return;
        }
        if (slot == TransitOfficeGUI.SLOT_LOST) {
            String err = tm.reportLost(player);
            player.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7旧卡已作废" : "&8[交通] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitOfficeGUI().open(player));
            return;
        }
        if (slot == TransitOfficeGUI.SLOT_TICKET) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTicketGUI().open(player, null));
            return;
        }
        if (slot == TransitOfficeGUI.SLOT_MAP) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitMapGUI().open(player));
            return;
        }
        if (slot == TransitOfficeGUI.SLOT_HISTORY) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitHistoryGUI().open(player));
            return;
        }
        if (slot == TransitOfficeGUI.SLOT_BOARD) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitBoardGUI().open(player));
            return;
        }
        if (slot == TransitOfficeGUI.SLOT_INSURE) {
            String err = tm.fileClaim(player);
            player.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &a已提交行程异常申报，请等待管理员审核。"
                    : "&8[交通] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitOfficeGUI().open(player));
            return;
        }
        if (slot == TransitOfficeGUI.SLOT_HELP) {
            player.sendMessage(ColorUtil.colorize(
                    "&8[交通] &f乘车须知\n"
                            + " &7开通闪付后须 &f空手 &7右键闸机。亦可持交通卡或单程票检票。\n"
                            + " &7进站刷一次；十五分钟内于本站再次刷卡可撤销行程，不产生费用。\n"
                            + " &7抵达目的地后再次刷卡出站，按最短路径扣取插件余额。\n"
                            + " &7换乘通道请勿刷卡，以免按出站结算。\n"
                            + " &7无进站记录、未出站再进站、或线路未连接，闸机不会直接扣全程。请至补票处或售票机补票后再出站。\n"
                            + " &7行程超时会自动失效。未能完成行程：交通处 → 行程异常申报（免费，须经审核）。"));
            return;
        }
        if (slot == TransitOfficeGUI.SLOT_ADMIN && player.hasPermission("es2uni.admin")) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(player));
        }
    }

    private void handleTransitMap(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        int p = plugin.getTransitMapGUI().currentPage(player);
        if (slot == TransitMapGUI.SLOT_BACK) {
            plugin.getTransitMapGUI().cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitOfficeGUI().open(player));
            return;
        }
        if (slot == TransitMapGUI.SLOT_PREV) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitMapGUI().open(player, p - 1));
            return;
        }
        if (slot == TransitMapGUI.SLOT_NEXT) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitMapGUI().open(player, p + 1));
            return;
        }
        if (slot == TransitMapGUI.SLOT_CLEAR) {
            plugin.getTransitMapGUI().clearFare(player);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitMapGUI().open(player, p));
            return;
        }
        String sid = plugin.getTransitMapGUI().stationAt(player, slot);
        if (sid != null) {
            String msg = plugin.getTransitMapGUI().clickStation(player, sid);
            player.sendMessage(ColorUtil.colorize("&8[交通] " + msg));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitMapGUI().open(player, p));
        }
    }

    private void handleTransitTicket(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        TransitTicketGUI gui = plugin.getTransitTicketGUI();
        if (slot == TransitTicketGUI.SLOT_BACK) {
            String tvm = plugin.getTransitTvmGUI().stationOf(player);
            if (gui.originOf(player) != null && (tvm == null || !tvm.equals(gui.originOf(player)))) {
                gui.openOrigin(player);
                return;
            }
            gui.cleanup(player);
            player.closeInventory();
            if (tvm != null) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTvmGUI().open(player, tvm));
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitOfficeGUI().open(player));
            }
            return;
        }
        String id = gui.stationAt(player, slot);
        if (id == null) return;
        if (gui.originOf(player) == null) {
            gui.setOrigin(player, id);
            gui.openDest(player);
            return;
        }
        String from = gui.originOf(player);
        player.closeInventory();
        String err = plugin.getTransitManager().buyTicket(player, from, id);
        if (err != null) player.sendMessage(ColorUtil.colorize("&8[交通] &c" + err));
        else player.sendMessage(ColorUtil.colorize("&8[交通] &a已购买单程票"));
        gui.cleanup(player);
    }

    private void handleTransitHistory(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getRawSlot() == TransitHistoryGUI.SLOT_BACK) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitOfficeGUI().open(player));
        }
    }

    private void handleTransitTvm(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        String station = plugin.getTransitTvmGUI().stationOf(player);
        if (slot == TransitTvmGUI.SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot == TransitTvmGUI.SLOT_TICKET && station != null) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTicketGUI().open(player, station));
            return;
        }
        if (slot == TransitTvmGUI.SLOT_CARD) {
            String err = plugin.getTransitManager().buyCard(player);
            player.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已发放交通卡" : "&8[交通] &c" + err));
            return;
        }
        if (slot == TransitTvmGUI.SLOT_TAPPAY) {
            boolean on = !plugin.getTransitManager().isTapPay(player.getUniqueId());
            String err = plugin.getTransitManager().setTapPay(player, on);
            player.sendMessage(ColorUtil.colorize(err != null ? "&8[交通] &c" + err
                    : (on ? "&8[交通] &a已开通闪付" : "&8[交通] &7已关闭闪付")));
            if (station != null) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTvmGUI().open(player, station));
            }
            return;
        }
        if (slot == TransitTvmGUI.SLOT_ADJUST && station != null) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdjustGUI().open(player, station));
        }
    }

    private void handleTransitAdjust(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        TransitAdjustGUI gui = plugin.getTransitAdjustGUI();
        String station = gui.stationOf(player);
        if (slot == TransitAdjustGUI.SLOT_CLOSE) {
            gui.cleanup(player);
            player.closeInventory();
            return;
        }
        if (slot != TransitAdjustGUI.SLOT_PAY || station == null) return;
        TransitManager.AdjustQuote q = plugin.getTransitManager().quoteAdjust(player, station);
        if (!q.payable()) {
            player.sendMessage(ColorUtil.colorize("&8[交通] &7" + q.title() + "。" + q.detail()));
            return;
        }
        String err = plugin.getTransitManager().applyAdjust(player, station);
        if (err != null) {
            player.sendMessage(ColorUtil.colorize("&8[交通] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, station));
            return;
        }
        int sec = plugin.getTransitManager().adjustPassSeconds();
        player.sendMessage(ColorUtil.colorize("&8[交通] &a补票完成"
                + (q.amount() <= 0 ? "" : " · 已扣 " + fmtMoney(q.amount()))
                + "\n&8[交通] &7请在 &f" + (sec / 60) + " 分钟 &7内从本站出站检票口刷卡离开"));
        gui.cleanup(player);
        player.closeInventory();
    }

    private String fmtMoney(double v) {
        return plugin.getVaultHook().isEnabled() ? plugin.getVaultHook().format(v) : String.format("%.2f", v);
    }

    private void handleTransitAdmin(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        TransitAdminGUI gui = plugin.getTransitAdminGUI();
        if (slot == TransitAdminGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitOfficeGUI().open(player));
            return;
        }
        int page = gui.pageOf(player);
        if (slot == TransitAdminGUI.SLOT_PREV) {
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, page - 1));
            return;
        }
        if (slot == TransitAdminGUI.SLOT_NEXT) {
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, page + 1));
            return;
        }
        if (slot == TransitAdminGUI.SLOT_TYPES) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTypesGUI().open(player));
            return;
        }
        if (slot == TransitAdminGUI.SLOT_ANNOUNCE) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAnnounceGUI().open(player));
            return;
        }
        if (slot == TransitAdminGUI.SLOT_CLAIMS) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitClaimsGUI().open(player));
            return;
        }
        if (slot == TransitAdminGUI.SLOT_RIDERS) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitRidersGUI().open(player));
            return;
        }
        String lineId = gui.lineAt(player, slot);
        if (lineId != null) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitEdgesGUI().open(player, lineId));
            return;
        }
        String st = gui.stationAt(player, slot);
        if (st == null) return;
        gui.cleanup(player);
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitStationGUI().open(player, st));
    }

    private void handleTransitEdges(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        TransitEdgesGUI gui = plugin.getTransitEdgesGUI();
        String lineId = gui.lineOf(player);
        if (slot == TransitEdgesGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(player));
            return;
        }
        if (lineId == null) return;
        var tm = plugin.getTransitManager();
        if (slot == TransitEdgesGUI.SLOT_DELETE) {
            if (!gui.isArmed(player)) {
                gui.armDelete(player, true);
                gui.open(player, lineId);
                return;
            }
            String err = tm.deleteLine(lineId);
            gui.cleanup(player);
            player.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &7已删除线路 &f" + lineId
                    : "&8[交通] &c" + err));
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(player));
            return;
        }
        gui.armDelete(player, false);
        TransitManager.Edge edge = gui.edgeAt(player, slot);
        if (edge != null) {
            String err = tm.removeEdge(edge.from(), edge.to(), lineId);
            player.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &7已断开 &f" + tm.stationName(edge.from()) + " ↔ " + tm.stationName(edge.to())
                    : "&8[交通] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, lineId));
            return;
        }
        String st = gui.stationAt(player, slot);
        if (st == null) return;
        String from = gui.pendingFrom(player);
        if (from == null || from.equals(st)) {
            gui.setPendingFrom(player, from != null && from.equals(st) ? null : st);
            gui.open(player, lineId);
            return;
        }
        String err = tm.addEdge(from, st, lineId, -1);
        gui.setPendingFrom(player, null);
        player.sendMessage(ColorUtil.colorize(err == null
                ? "&8[交通] &a已连接 &f" + tm.stationName(from) + " ↔ " + tm.stationName(st)
                : "&8[交通] &c" + err));
        Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, lineId));
    }

    private void handleTransitStation(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        TransitStationGUI gui = plugin.getTransitStationGUI();
        String sid = gui.stationOf(player);
        if (slot == TransitStationGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(player));
            return;
        }
        if (sid == null) return;
        var tm = plugin.getTransitManager();
        TransitManager.Station s = tm.getStation(sid);
        if (s == null) {
            gui.cleanup(player);
            player.sendMessage(ColorUtil.colorize("&8[交通] &c车站已不存在"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(player));
            return;
        }
        if (slot == TransitStationGUI.SLOT_DELETE) {
            if (!gui.isArmed(player)) {
                gui.armDelete(player, true);
                gui.open(player, sid);
                return;
            }
            String err = tm.deleteStation(sid);
            gui.cleanup(player);
            player.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &7已删除车站 &f" + s.displayName()
                    : "&8[交通] &c" + err));
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(player));
            return;
        }
        gui.armDelete(player, false);
        if (slot == TransitStationGUI.SLOT_POS1) {
            tm.markBoxCorner(player, 1);
            player.sendMessage(ColorUtil.colorize("&8[交通] &a角点1已记在脚下。再去对角点开本页点角点2，然后点应用。"));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, sid));
            return;
        }
        if (slot == TransitStationGUI.SLOT_POS2) {
            tm.markBoxCorner(player, 2);
            player.sendMessage(ColorUtil.colorize("&8[交通] &6角点2已记在脚下。两点都有了就点「应用框选范围」。"));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, sid));
            return;
        }
        if (slot == TransitStationGUI.SLOT_APPLY) {
            String err = tm.applyBox(player, sid);
            player.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &a范围已更新: &f" + tm.boxLabel(tm.getStation(sid))
                    : "&8[交通] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, sid));
            return;
        }
        if (slot == TransitStationGUI.SLOT_MOVE) {
            String err = tm.moveStation(sid, player.getLocation(), tm.stationRadius(s));
            player.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &a判定区已移到脚下（半径 " + tm.stationRadius(tm.getStation(sid)) + "）"
                    : "&8[交通] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, sid));
            return;
        }
        if (slot == TransitStationGUI.SLOT_TP) {
            gui.cleanup(player);
            player.closeInventory();
            String err = tm.teleportToStation(player, sid);
            if (err != null) player.sendMessage(ColorUtil.colorize("&8[交通] &c" + err));
            return;
        }
        if (slot == TransitStationGUI.SLOT_NAME_ZH) {
            awaitingStationNameZh.put(player.getUniqueId(), sid);
            player.closeInventory();
            player.sendMessage(ColorUtil.colorize("&8[交通] &f请在聊天输入车站中文名（可直接打汉字），cancel 取消"));
            return;
        }
        if (slot == TransitStationGUI.SLOT_NAME_EN) {
            awaitingStationNameEn.put(player.getUniqueId(), sid);
            player.closeInventory();
            player.sendMessage(ColorUtil.colorize("&8[交通] &fType the English station name in chat, cancel to abort"));
            return;
        }
        if (slot == TransitStationGUI.SLOT_GATE_MODE) {
            String m = gui.cycleGateMode(player);
            player.sendMessage(ColorUtil.colorize("&8[交通] &e闸机模式: &f" + m
                    + " &7（BOTH 进出 / IN 仅进 / OUT 仅出）"));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, sid));
            return;
        }
        if (slot == TransitStationGUI.SLOT_GATE) {
            String mode = gui.gateModeOf(player);
            player.getInventory().addItem(com.etherstories.escore.items.TransitItems.gate(sid, mode));
            player.sendMessage(ColorUtil.colorize("&8[交通] &a已给予闸机牌 &8[" + mode + "] &7放下即写字，拆掉即注销。"));
            return;
        }
        if (slot == TransitStationGUI.SLOT_BIND_TVM) {
            tm.beginTvmBind(player, sid);
            player.closeInventory();
            player.sendMessage(ColorUtil.colorize("&8[交通] &e看向要当售票机的方块（模组方块也行），右键绑定。"));
            return;
        }
        if (slot == TransitStationGUI.SLOT_TVM) {
            player.getInventory().addItem(com.etherstories.escore.items.TransitItems.tvm(sid));
            player.sendMessage(ColorUtil.colorize("&8[交通] &a已给予原版售票机（磁石）"));
            return;
        }
        if (slot == TransitStationGUI.SLOT_ADJUST) {
            player.getInventory().addItem(com.etherstories.escore.items.TransitItems.adjust(sid));
            player.sendMessage(ColorUtil.colorize("&8[交通] &a已给予补票处牌 &7放下即写字，拆掉即注销。"));
            return;
        }
        String lineId = gui.lineAt(player, slot);
        if (lineId == null) return;
        boolean on = s.lineIds().contains(lineId);
        String err = on ? tm.removeStationLine(sid, lineId) : tm.addStationLine(sid, lineId);
        player.sendMessage(ColorUtil.colorize(err == null
                ? (on ? "&8[交通] &7已从该站摘掉线路" : "&8[交通] &a已挂上线路，去该线连接页把邻站连上")
                : "&8[交通] &c" + err));
        Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, sid));
    }

    private void handleTransitAnnounce(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == TransitAnnounceGUI.SLOT_BACK) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(player));
            return;
        }
        var tm = plugin.getTransitManager();
        if (slot == TransitAnnounceGUI.SLOT_ARRIVE_TOGGLE) {
            boolean on = plugin.getConfig().getBoolean("transit.announce.enabled", true);
            tm.setAnnounceEnabled(!on);
            player.sendMessage(ColorUtil.colorize(!on ? "&8[交通] &a到站铃已开启" : "&8[交通] &7到站铃已关闭"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAnnounceGUI().open(player));
            return;
        }
        if (slot == TransitAnnounceGUI.SLOT_DEPART_TOGGLE) {
            boolean on = plugin.getConfig().getBoolean("transit.announce.depart-enabled", true);
            tm.setDepartEnabled(!on);
            player.sendMessage(ColorUtil.colorize(!on ? "&8[交通] &a发车铃已开启" : "&8[交通] &7发车铃已关闭"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAnnounceGUI().open(player));
            return;
        }
        if (slot == TransitAnnounceGUI.SLOT_STACK) {
            boolean on = tm.stackRepeats();
            tm.setStackRepeats(!on);
            player.sendMessage(ColorUtil.colorize(!on ? "&8[交通] &a重复音已并成一拍" : "&8[交通] &7重复音分开播"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAnnounceGUI().open(player));
            return;
        }
        if (slot == TransitAnnounceGUI.SLOT_ARRIVE_PREVIEW) {
            tm.playArriveSound(player);
            player.sendMessage(ColorUtil.colorize("&8[交通] &a试听到站铃: "
                    + plugin.getConfig().getString("transit.announce.preset", "jr")));
            return;
        }
        if (slot == TransitAnnounceGUI.SLOT_DEPART_PREVIEW) {
            tm.playDepartSound(player);
            player.sendMessage(ColorUtil.colorize("&8[交通] &a试听发车铃: "
                    + plugin.getConfig().getString("transit.announce.depart-preset", "jrgo")));
            return;
        }
        String arrive = TransitAnnounceGUI.arrivePresetAt(slot);
        if (arrive != null) {
            tm.setAnnouncePreset(arrive);
            tm.playArriveSound(player);
            player.sendMessage(ColorUtil.colorize("&8[交通] &a已选用并试听到站铃 &f" + arrive));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAnnounceGUI().open(player));
            return;
        }
        String depart = TransitAnnounceGUI.departPresetAt(slot);
        if (depart != null) {
            tm.setDepartPreset(depart);
            tm.playDepartSound(player);
            player.sendMessage(ColorUtil.colorize("&8[交通] &a已选用并试听发车铃 &f" + depart));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAnnounceGUI().open(player));
        }
    }

    private void handleTransitTypes(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        TransitTypesGUI gui = plugin.getTransitTypesGUI();
        if (slot == TransitTypesGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(player));
            return;
        }
        if (slot == TransitTypesGUI.SLOT_NEW) {
            awaitingTypeCreate.add(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(ColorUtil.colorize("&8[交通] &f请在聊天输入类型 ID，可选：&eID 显示名"));
            player.sendMessage(ColorUtil.colorize("&8[交通] &7例: &fmag 磁悬浮  &8输入 cancel 取消"));
            return;
        }
        String id = gui.typeAt(player, slot);
        if (id == null) return;
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTypeEditGUI().open(player, id));
    }

    private void handleTransitTypeEdit(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        TransitTypeEditGUI gui = plugin.getTransitTypeEditGUI();
        String id = gui.editing(player);
        if (id == null) return;
        if (slot == TransitTypeEditGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTypesGUI().open(player));
            return;
        }
        if (slot == TransitTypeEditGUI.SLOT_RENAME) {
            awaitingTypeRename.put(player.getUniqueId(), id);
            player.closeInventory();
            player.sendMessage(ColorUtil.colorize("&8[交通] &f请在聊天输入新的显示名，cancel 取消"));
            return;
        }
        if (slot == TransitTypeEditGUI.SLOT_DELETE) {
            if (!event.isShiftClick()) {
                player.sendMessage(ColorUtil.colorize("&8[交通] &7请 Shift 点击以确认删除"));
                return;
            }
            String err = plugin.getTransitManager().deleteType(id);
            player.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7已删除类型" : "&8[交通] &c" + err));
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTypesGUI().open(player));
            return;
        }
        String color = gui.colorAt(slot);
        if (color == null) return;
        plugin.getTransitManager().setTypeColor(id, color);
        Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, id));
    }

    private void handleTransitClaims(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        TransitClaimsGUI gui = plugin.getTransitClaimsGUI();
        if (slot == TransitClaimsGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(player));
            return;
        }
        String id = gui.claimAt(player, slot);
        if (id == null) return;
        boolean approve = event.isLeftClick();
        String err = plugin.getTransitManager().resolveClaim(player, id, approve);
        player.sendMessage(ColorUtil.colorize(err == null
                ? (approve ? "&8[交通] &a已核准" : "&8[交通] &7已驳回")
                : "&8[交通] &c" + err));
        Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
    }

    private void handleTransitRiders(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        TransitRidersGUI gui = plugin.getTransitRidersGUI();
        if (slot == TransitRidersGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(player));
            return;
        }
        UUID target = gui.riderAt(player, slot);
        if (target == null) return;
        String err = plugin.getTransitManager().adminClearJourney(target);
        OfflinePlayer off = Bukkit.getOfflinePlayer(target);
        String name = off.getName() == null ? target.toString().substring(0, 8) : off.getName();
        player.sendMessage(ColorUtil.colorize(err == null
                ? "&8[交通] &7已清除 &f" + name + " &7的在乘行程"
                : "&8[交通] &c" + err));
        Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
    }

    private void sendCoordCard(Player from, UUID friendUuid) {
        Player friend = Bukkit.getPlayer(friendUuid);
        if (friend == null || !friend.isOnline()) {
            from.sendMessage(ColorUtil.colorize("&8[ECOS] &7对方不在线，无法发送坐标卡片"));
            return;
        }
        if (!plugin.getFriendManager().isLocationSharing(from.getUniqueId())) {
            from.sendMessage(ColorUtil.colorize(
                    "&8[ECOS] &7请先开启「位置分享」（好友页底部），再发送坐标卡片"));
            return;
        }
        var loc = from.getLocation();
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "?";
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        String line = "&b[坐标卡片] &f" + from.getName() + " &7→ &f"
                + world + " &e" + x + " " + y + " " + z;
        from.sendMessage(ColorUtil.colorize("&8[ECOS] &7已向 &f" + friend.getName() + " &7发送坐标卡片"));
        friend.sendMessage(ColorUtil.colorize(line));
        // 可点击建议坐标（复制到聊天）
        net.md_5.bungee.api.chat.TextComponent click = new net.md_5.bungee.api.chat.TextComponent(
                ColorUtil.colorize("&a▶ 点击复制坐标"));
        click.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND,
                x + " " + y + " " + z));
        click.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("点击填入坐标").create()));
        friend.spigot().sendMessage(click);
        plugin.getNewbieGuideManager().mark(from.getUniqueId(),
                com.etherstories.escore.managers.NewbieGuideManager.Step.ADD_FRIEND);
    }

    private void giveNoticeDraftBook(Player player) {
        ItemStack book = new ItemStack(org.bukkit.Material.WRITABLE_BOOK);
        BookMeta bm = (BookMeta) book.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(ColorUtil.colorize("&a公告草稿"));
            bm.getPersistentDataContainer().set(noticeDraftKey, PersistentDataType.BYTE, (byte) 1);
            book.setItemMeta(bm);
        }
        int empty = player.getInventory().firstEmpty();
        if (empty == -1) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7背包已满，无法给予书与笔"));
            return;
        }
        player.getInventory().setItem(empty, book);
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7书与笔已放入背包，写完后&f签名&7即可发布公告"));
    }

    // ── Book Sign ─────────────────────────────────────────────────────────────

    @EventHandler
    public void onBookSign(PlayerEditBookEvent event) {
        if (!event.isSigning()) return;
        Player player = event.getPlayer();
        BookMeta bm = event.getNewBookMeta();
        String content = String.join("\n", bm.getPages());

        // 公告草稿（PDC 标在 signing 前的书上；签名后 PDC 可能丢失，先查 previous 再查 new）
        boolean noticeDraft = false;
        BookMeta previous = event.getPreviousBookMeta();
        if (previous != null && previous.getPersistentDataContainer()
                .has(noticeDraftKey, PersistentDataType.BYTE)) {
            noticeDraft = true;
        } else if (bm.getPersistentDataContainer().has(noticeDraftKey, PersistentDataType.BYTE)) {
            noticeDraft = true;
        }

        if (noticeDraft) {
            if (content.isBlank()) {
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7公告内容为空，未发布"));
                return;
            }
            plugin.getNoticeManager().addNotice(player.getName(), content);
            plugin.getAuditLogManager().log(player, "NOTICE_POST",
                    content.length() > 80 ? content.substring(0, 80) + "…" : content);
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7公告已发布"));
            removeSignedBookLater(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getNoticeGUI().open(player), 2L);
            return;
        }

        UUID recipientUuid = plugin.getMailManager().consumePending(player.getUniqueId());
        if (recipientUuid == null) return;

        if (content.isBlank()) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7留言内容为空，未发送"));
            return;
        }

        plugin.getMailManager().deliver(
                player.getUniqueId(), player.getName(), recipientUuid, content);
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7留言已发送"));
        removeSignedBookLater(player);
    }

    private void removeSignedBookLater(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() == org.bukkit.Material.WRITTEN_BOOK) {
                    player.getInventory().setItem(i, null);
                    break;
                }
            }
        }, 1L);
    }

    // ── Close handler ─────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (confirming.contains(player.getUniqueId())) return;

        // Kit 编辑关闭时自动保存物品，避免忘点保存
        if (plugin.getKitEditGUI().isEditing(player)
                && event.getView().getTitle().startsWith(KitEditGUI.TITLE_PREFIX)) {
            plugin.getKitEditGUI().captureItems(player, event.getInventory());
        }
        // 文本输入已改聊天，关 GUI 不再 cancel pending
    }

    // ── Player quit: clean all GUI caches to prevent memory leaks ─────────────

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // GUIListener internal state
        awaitingBroadcast.remove(uuid);
        awaitingBcAdd.remove(uuid);
        awaitingBcEdit.remove(uuid);
        awaitingFriendName.remove(uuid);
        awaitingReport.remove(uuid);
        awaitingShowcaseTitle.remove(uuid);
        awaitingStatus.remove(uuid);
        plugin.getCheckInCalendarGUI().cleanup(event.getPlayer());
        plugin.getShowcaseGUI().cleanup(event.getPlayer());
        plugin.getJobBoardGUI().cleanup(event.getPlayer());
        plugin.getJobHistoryGUI().cleanup(event.getPlayer());
        plugin.getInsuranceGUI().cleanup(event.getPlayer());
        plugin.getInsuranceBackupGUI().cleanup(event.getPlayer());
        plugin.getRecycleBinGUI().cleanup(event.getPlayer());
        awaitingWhisper.remove(uuid);
        awaitingKitCd.remove(uuid);
        awaitingTypeCreate.remove(uuid);
        awaitingTypeRename.remove(uuid);
        awaitingStationNameZh.remove(uuid);
        awaitingStationNameEn.remove(uuid);
        confirming.remove(uuid);
        // GUI caches
        plugin.getAnvilInputGUI().cancel(event.getPlayer());
        plugin.getFriendListGUI().cleanup(event.getPlayer());
        plugin.getAddFriendGUI().cleanup(event.getPlayer());
        plugin.getPayConfirmGUI().cleanup(event.getPlayer());
        plugin.getPlayerSelectGUI().cleanup(event.getPlayer());
        plugin.getOnlineListGUI().cleanup(event.getPlayer());
        plugin.getPlayerActionGUI().cleanup(event.getPlayer());
        plugin.getTerritoryListGUI().cleanup(event.getPlayer());
        plugin.getAdminTerritoryGUI().cleanup(event.getPlayer());
        plugin.getMyTerritoryGUI().cleanup(event.getPlayer());
        plugin.getEstateBuildingsGUI().cleanup(event.getPlayer());
        plugin.getEstateRoomsGUI().cleanup(event.getPlayer());
        plugin.getEstateMineGUI().cleanup(event.getPlayer());
        plugin.getAdminRegionGUI().cleanup(event.getPlayer());
        plugin.getAdminKitGUI().cleanup(event.getPlayer());
        plugin.getKitEditGUI().cleanup(event.getPlayer());
        plugin.getKitListGUI().cleanup(event.getPlayer());
        plugin.getMusicSearchResultGUI().cleanup(event.getPlayer());
        plugin.getMusicHistoryGUI().cleanup(event.getPlayer());
        plugin.getMusicFavoritesGUI().cleanup(event.getPlayer());
        plugin.getMusicPlaylistGUI().cleanup(event.getPlayer());
        plugin.getTransitAdminGUI().cleanup(event.getPlayer());
        plugin.getTransitEdgesGUI().cleanup(event.getPlayer());
        plugin.getTransitStationGUI().cleanup(event.getPlayer());
        plugin.getTransitAnnounceGUI().cleanup(event.getPlayer());
        plugin.getTransitTypesGUI().cleanup(event.getPlayer());
        plugin.getTransitTypeEditGUI().cleanup(event.getPlayer());
        plugin.getTransitClaimsGUI().cleanup(event.getPlayer());
        plugin.getTransitMapGUI().cleanup(event.getPlayer());
        plugin.getTransitAdjustGUI().cleanup(event.getPlayer());
        plugin.getTransitRidersGUI().cleanup(event.getPlayer());
        plugin.getMailManager().consumePending(uuid);
        plugin.getActionBarTask().cleanup(uuid);
        plugin.getEcosCommand().cleanupParticles(uuid);
    }

    // ── Friend List ───────────────────────────────────────────────────────────

    private void handleFriendList(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        FriendListGUI gui = plugin.getFriendListGUI();
        FriendListGUI.ViewMode mode = gui.getViewMode(player);

        if (slot == FriendListGUI.SLOT_CLOSE) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }

        if (mode == FriendListGUI.ViewMode.FRIENDS) {
            if (slot == FriendListGUI.SLOT_LOC_TOGGLE) {
                boolean now = plugin.getFriendManager().toggleLocationSharing(player.getUniqueId());
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7位置分享已" + (now ? "开启" : "关闭")));
                Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
                return;
            }
            if (slot == FriendListGUI.SLOT_REQUESTS) {
                gui.openInMode(player, FriendListGUI.ViewMode.REQUESTS);
                return;
            }
            if (slot == FriendListGUI.SLOT_ADD) {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getAddFriendGUI().open(player));
                return;
            }
            UUID friendUuid = gui.getEntryAt(player, slot);
            if (friendUuid == null) return;
            if (event.isRightClick()) {
                plugin.getFriendManager().removeFriend(player.getUniqueId(), friendUuid);
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已删除好友 "
                        + plugin.getFriendManager().getDisplayName(friendUuid)));
                Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            } else if (event.isLeftClick()) {
                sendCoordCard(player, friendUuid);
            }
        } else {
            if (slot == FriendListGUI.SLOT_BACK) {
                gui.openInMode(player, FriendListGUI.ViewMode.FRIENDS);
                return;
            }
            UUID senderUuid = gui.getEntryAt(player, slot);
            if (senderUuid == null) return;
            if (event.isLeftClick()) {
                boolean ok = plugin.getFriendManager().acceptRequest(player.getUniqueId(), senderUuid);
                if (ok) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已添加好友 "
                            + plugin.getFriendManager().getDisplayName(senderUuid)));
                    Player sender = Bukkit.getPlayer(senderUuid);
                    if (sender != null)
                        sender.sendMessage(ColorUtil.colorize("&8[ECOS] &f" + player.getName() + " &7接受了你的好友请求"));
                }
            } else if (event.isRightClick()) {
                plugin.getFriendManager().denyRequest(player.getUniqueId(), senderUuid);
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已拒绝请求"));
            }
            Bukkit.getScheduler().runTask(plugin, () -> gui.openInMode(player, FriendListGUI.ViewMode.REQUESTS));
        }
    }

    // ── Chat intercept ────────────────────────────────────────────────────────

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 通用文本输入（原铁砧）：聊天确认
        if (plugin.getAnvilInputGUI().isInInput(player)) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
                    plugin.getAnvilInputGUI().cancel(player);
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已取消输入"));
                    return;
                }
                plugin.getAnvilInputGUI().updateInput(player, msg);
                plugin.getAnvilInputGUI().confirm(player);
            });
            return;
        }

        if (awaitingTypeCreate.remove(uuid)) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
                    player.sendMessage(ColorUtil.colorize("&8[交通] &7已取消"));
                    plugin.getTransitTypesGUI().open(player);
                    return;
                }
                String[] parts = msg.split("\\s+", 2);
                String id = parts[0].trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "");
                String name = parts.length > 1 ? parts[1] : id;
                String err = plugin.getTransitManager().createType(id, "&b", name);
                if (err != null) {
                    player.sendMessage(ColorUtil.colorize("&8[交通] &c" + err));
                    plugin.getTransitTypesGUI().open(player);
                    return;
                }
                player.sendMessage(ColorUtil.colorize("&8[交通] &a类型已创建"));
                plugin.getTransitTypeEditGUI().open(player, id);
            });
            return;
        }

        String renameId = awaitingTypeRename.remove(uuid);
        if (renameId != null) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
                    player.sendMessage(ColorUtil.colorize("&8[交通] &7已取消"));
                    plugin.getTransitTypeEditGUI().open(player, renameId);
                    return;
                }
                String err = plugin.getTransitManager().renameType(renameId, msg);
                player.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已改名" : "&8[交通] &c" + err));
                plugin.getTransitTypeEditGUI().open(player, renameId);
            });
            return;
        }

        String stZh = awaitingStationNameZh.remove(uuid);
        if (stZh != null) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
                    player.sendMessage(ColorUtil.colorize("&8[交通] &7已取消"));
                    plugin.getTransitStationGUI().open(player, stZh);
                    return;
                }
                String err = plugin.getTransitManager().renameStation(stZh, msg);
                player.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a中文名已更新: &f" + msg : "&8[交通] &c" + err));
                plugin.getTransitStationGUI().open(player, stZh);
            });
            return;
        }

        String stEn = awaitingStationNameEn.remove(uuid);
        if (stEn != null) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
                    player.sendMessage(ColorUtil.colorize("&8[交通] &7已取消"));
                    plugin.getTransitStationGUI().open(player, stEn);
                    return;
                }
                String err = plugin.getTransitManager().renameStationEn(stEn, msg);
                player.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &aEnglish name: &f" + msg : "&8[交通] &c" + err));
                plugin.getTransitStationGUI().open(player, stEn);
            });
            return;
        }

        // 立即广播一条
        if (awaitingBroadcast.remove(uuid)) {
            event.setCancelled(true);
            String msg = event.getMessage();
            if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7广播已取消")));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                String full = plugin.getConfigManager().getBroadcastPrefix() + msg;
                Bukkit.broadcastMessage(ColorUtil.colorize(full));
                plugin.getNoticeManager().recordBroadcast(msg);
            });
            return;
        }

        // 定时广播：添加
        if (awaitingBcAdd.remove(uuid)) {
            event.setCancelled(true);
            String msg = event.getMessage();
            if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已取消"));
                    plugin.getAdminBroadcastGUI().open(player);
                });
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<String> list = new ArrayList<>(plugin.getConfigManager().getBroadcastMessages());
                list.add(msg);
                plugin.getAdminBroadcastGUI().saveMessages(list);
                plugin.getAuditLogManager().log(player, "BC_ADD",
                        msg.length() > 60 ? msg.substring(0, 60) + "…" : msg);
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已添加定时消息"));
                plugin.getAdminBroadcastGUI().open(player);
            });
            return;
        }

        // 展示点标题
        if (awaitingShowcaseTitle.remove(uuid)) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();
            if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已取消"));
                    plugin.getShowcaseGUI().open(player);
                });
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                String title = msg.equals("-") || msg.isEmpty() ? null : msg;
                String err = plugin.getShowcaseManager().register(player, title);
                if (err != null) player.sendMessage(ColorUtil.colorize("&8[ECOS] &7" + err));
                else player.sendMessage(ColorUtil.colorize("&8[ECOS] &a展示点已登记"));
                plugin.getShowcaseGUI().open(player);
            });
            return;
        }

        // 悄悄话 / 举报
        if (awaitingReport.containsKey(uuid)) {
            event.setCancelled(true);
            UUID about = awaitingReport.remove(uuid);
            String msg = event.getMessage().trim();
            if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已取消")));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                String aboutName = null;
                if (about != null) {
                    Player aboutP = Bukkit.getPlayer(about);
                    aboutName = aboutP != null ? aboutP.getName()
                            : Bukkit.getOfflinePlayer(about).getName();
                }
                plugin.getReportManager().submit(player, aboutName, msg);
            });
            return;
        }

        // 添加好友：聊天输入名字
        if (awaitingFriendName.remove(uuid)) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();
            if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已取消"));
                    plugin.getAddFriendGUI().open(player);
                });
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> trySendFriendRequest(player, msg));
            return;
        }

        // 定时广播：编辑
        Integer editIdx = awaitingBcEdit.remove(uuid);
        if (editIdx != null) {
            event.setCancelled(true);
            String msg = event.getMessage();
            if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已取消"));
                    plugin.getAdminBroadcastGUI().open(player);
                });
                return;
            }
            final int idx = editIdx;
            Bukkit.getScheduler().runTask(plugin, () -> {
                var entries = new ArrayList<>(plugin.getConfigManager().getBroadcastEntries());
                if (idx >= 0 && idx < entries.size()) {
                    var old = entries.get(idx);
                    entries.set(idx, new com.etherstories.escore.broadcast.BroadcastEntry(msg, old.days));
                    plugin.getAdminBroadcastGUI().saveEntries(entries);
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已更新消息 #" + (idx + 1)));
                }
                plugin.getAdminBroadcastGUI().open(player);
            });
            return;
        }

        // Status / 个人签名
        if (awaitingStatus.remove(uuid)) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();
            if (msg.equalsIgnoreCase("cancel")) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已取消")));
                return;
            }
            int max = plugin.getStatusManager().maxLength();
            if (msg.length() > max) msg = msg.substring(0, max);
            final String finalMsg = msg;
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getStatusManager().setStatus(player.getUniqueId(), finalMsg.isEmpty() ? null : finalMsg);
                player.sendMessage(ColorUtil.colorize(finalMsg.isEmpty()
                        ? "&8[ECOS] &7签名已清除"
                        : "&8[ECOS] &7签名已更新: &f" + finalMsg));
            });
            return;
        }

        // Kit 冷却秒数
        if (awaitingKitCd.remove(uuid)) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();
            if (msg.equalsIgnoreCase("cancel")) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已取消")));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                String kitName = plugin.getKitEditGUI().getEditing(player);
                if (kitName == null) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7编辑会话已失效"));
                    return;
                }
                try {
                    int sec = Integer.parseInt(msg);
                    if (sec < 0) throw new NumberFormatException();
                    var def = plugin.getKitManager().get(kitName);
                    if (def == null) {
                        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7套件不存在"));
                        return;
                    }
                    def.cooldownSeconds = sec;
                    def.claimMode = KitManager.ClaimMode.COOLDOWN;
                    plugin.getKitManager().saveKit(def);
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7冷却已设为 &f"
                            + KitManager.formatDuration(sec)));
                    plugin.getKitEditGUI().open(player, kitName);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7请输入非负整数秒数"));
                    awaitingKitCd.add(uuid);
                }
            });
            return;
        }

        // Whisper
        UUID targetUuid = awaitingWhisper.remove(uuid);
        if (targetUuid != null) {
            event.setCancelled(true);
            String msg = event.getMessage();
            if (msg.equalsIgnoreCase("cancel")) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7私信已取消")));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player target = Bukkit.getPlayer(targetUuid);
                if (target == null) {
                    player.sendMessage(ColorUtil.colorize("&8[ECOS] &7玩家已离线"));
                    return;
                }
                String out = "&8[私信] &f" + player.getName() + " &8→ &f" + target.getName() + ": &7" + msg;
                player.sendMessage(ColorUtil.colorize(out));
                target.sendMessage(ColorUtil.colorize(out));
                ES2UniCommand cmd = plugin.getEcosCommand();
                if (cmd != null) cmd.recordWhisper(uuid, targetUuid);
            });
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void handleMusicMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == MusicMenuGUI.SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot == MusicMenuGUI.SLOT_BACK) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (!plugin.getAllMusicHook().isAvailable()) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &cAllMusic 未加载"));
            return;
        }
        switch (slot) {
            case MusicMenuGUI.SLOT_SEARCH -> {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> plugin.getAnvilInputGUI().openForMusicSearch(player), 1L);
            }
            case MusicMenuGUI.SLOT_ADD_ID -> {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> plugin.getAnvilInputGUI().openForMusicAddId(player), 1L);
            }
            case MusicMenuGUI.SLOT_STOP -> {
                plugin.getAllMusicHook().stop(plugin, player);
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已请求停止播放"));
            }
            case MusicMenuGUI.SLOT_QUEUE -> {
                player.closeInventory();
                sendQueueChat(player);
            }
            case MusicMenuGUI.SLOT_HISTORY -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicHistoryGUI().open(player));
            }
            case MusicMenuGUI.SLOT_FAVORITES -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicFavoritesGUI().open(player));
            }
            case MusicMenuGUI.SLOT_PRESETS -> {
                if (!player.hasPermission("es2uni.admin")) return;
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicPlaylistGUI().open(player));
            }
            case MusicMenuGUI.SLOT_VOTE -> {
                plugin.getAllMusicHook().vote(plugin, player);
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已投票切歌"));
            }
            case MusicMenuGUI.SLOT_MUTE -> {
                boolean muted = plugin.getAllMusicHook().isMuted(player);
                plugin.getAllMusicHook().setListening(plugin, player, muted);
                boolean nowMuted = !muted;
                player.sendMessage(ColorUtil.colorize(nowMuted
                        ? "&8[ECOS] &c已静音：别人点歌你仍能看到消息，但不会播放"
                        : "&8[ECOS] &a已取消静音，开始听歌"));
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> plugin.getMusicMenuGUI().open(player), 3L);
            }
            case MusicMenuGUI.SLOT_JOIN -> {
                plugin.getAllMusicHook().join(plugin, player);
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已请求重新加入播放"));
            }
            case MusicMenuGUI.SLOT_CANCEL -> {
                plugin.getAllMusicHook().cancel(plugin, player);
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已请求取消自己点的歌"));
            }
            case MusicMenuGUI.SLOT_HELP -> {
                player.closeInventory();
                player.performCommand("ecos music help");
            }
            case MusicMenuGUI.SLOT_NOW -> {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicMenuGUI().open(player));
            }
        }
    }

    private void handleMusicSearch(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        MusicSearchResultGUI gui = plugin.getMusicSearchResultGUI();

        if (slot == MusicSearchResultGUI.BACK_SLOT) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicMenuGUI().open(player));
            return;
        }
        if (slot == MusicSearchResultGUI.RETRY_SLOT) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> plugin.getAnvilInputGUI().openForMusicSearch(player), 1L);
            return;
        }
        if (slot == MusicSearchResultGUI.PREV_SLOT) {
            if (plugin.getAllMusicHook().prevSearchPage(player))
                Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        if (slot == MusicSearchResultGUI.NEXT_SLOT) {
            if (plugin.getAllMusicHook().nextSearchPage(player))
                Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        if (slot == MusicSearchResultGUI.INFO_SLOT) return;

        var song = gui.getAt(player, slot);
        if (song == null) return;
        player.closeInventory();
        gui.cleanup(player);
        plugin.getAllMusicHook().select(plugin, player, song.selectIndex());
        plugin.getMusicHistoryManager().recordManual(
                song.id(), song.name(), song.author(), player.getName());
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已点歌: &f" + song.name()
                + " &8- &7" + song.author()));
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getMusicMenuGUI().open(player), 20L);
    }

    private void handleMusicHistory(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        MusicHistoryGUI gui = plugin.getMusicHistoryGUI();
        if (slot == MusicHistoryGUI.SLOT_CLOSE) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == MusicHistoryGUI.SLOT_BACK) {
            gui.cleanup(player); player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicMenuGUI().open(player));
            return;
        }
        if (slot == MusicHistoryGUI.SLOT_FAV) {
            gui.cleanup(player); player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicFavoritesGUI().open(player));
            return;
        }
        var e = gui.getAt(player, slot);
        if (e == null) return;
        if (event.isShiftClick() && event.isLeftClick()) {
            if (e.id() == null || e.id().isBlank()) {
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7无歌曲 ID，无法点播"));
                return;
            }
            plugin.getAllMusicHook().addById(plugin, player, e.id());
            plugin.getMusicHistoryManager().recordManual(e.id(), e.name(), e.author(), player.getName());
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已点歌: &f" + e.name()));
            return;
        }
        if (event.isRightClick()) {
            if (e.id() == null || e.id().isBlank()) {
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7无歌曲 ID，无法收藏"));
                return;
            }
            boolean nowFav = plugin.getMusicFavoritesManager().toggle(
                    player, e.id(), e.name(), e.author(), "");
            player.sendMessage(ColorUtil.colorize(nowFav
                    ? "&8[ECOS] &6已收藏 &f" + e.name()
                    : "&8[ECOS] &7已取消收藏 &f" + e.name()));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        if (event.isLeftClick()) {
            plugin.getMusicDetailGUI().openBook(player, e.id(), e.name(), e.author(), "",
                    "点歌者 " + e.caller() + " · " + e.time());
            Bukkit.getScheduler().runTaskLater(plugin, () -> gui.open(player), 40L);
        }
    }

    private void handleMusicFavorites(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        MusicFavoritesGUI gui = plugin.getMusicFavoritesGUI();
        if (slot == MusicFavoritesGUI.SLOT_CLOSE) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == MusicFavoritesGUI.SLOT_BACK) {
            gui.cleanup(player); player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicMenuGUI().open(player));
            return;
        }
        if (slot == MusicFavoritesGUI.SLOT_HISTORY) {
            gui.cleanup(player); player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicHistoryGUI().open(player));
            return;
        }
        var s = gui.getAt(player, slot);
        if (s == null) return;
        if (event.isRightClick()) {
            plugin.getMusicFavoritesManager().remove(player.getUniqueId(), s.id());
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已取消收藏"));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        if (event.isShiftClick() && event.isLeftClick()) {
            plugin.getAllMusicHook().addById(plugin, player, s.id());
            plugin.getMusicHistoryManager().recordManual(s.id(), s.name(), s.author(), player.getName());
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已点歌: &f" + s.name()));
            return;
        }
        if (event.isLeftClick()) {
            plugin.getMusicDetailGUI().openBook(player, s.id(), s.name(), s.author(), s.album(), "来自收藏");
            Bukkit.getScheduler().runTaskLater(plugin, () -> gui.open(player), 40L);
        }
    }

    private void handleMusicPlaylist(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (!player.hasPermission("es2uni.admin")) return;
        int slot = event.getRawSlot();
        MusicPlaylistGUI gui = plugin.getMusicPlaylistGUI();
        if (slot == MusicPlaylistGUI.SLOT_CLOSE) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == MusicPlaylistGUI.SLOT_BACK) {
            gui.cleanup(player); player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicMenuGUI().open(player));
            return;
        }
        if (slot == MusicPlaylistGUI.SLOT_STOP) {
            plugin.getMusicPlaylistManager().stop(true);
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        if (slot == MusicPlaylistGUI.SLOT_STATUS) {
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        String key = gui.getKey(player, slot);
        if (key == null) return;
        boolean shuffle = event.isRightClick();
        String err = plugin.getMusicPlaylistManager().start(player, key, shuffle);
        if (err != null) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c" + err));
        } else {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &a已启动预设"
                    + (shuffle ? "（打乱）" : "（顺序）")
                    + " &7— 队列空时自动续点"));
        }
        Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
    }

    private void sendQueueChat(Player player) {
        AllMusicHook.NowPlaying now = plugin.getAllMusicHook().getNowPlaying();
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &b播放序列"));
        if (now != null) {
            player.sendMessage(ColorUtil.colorize(" &e▶ 正在播 &f" + now.name()
                    + " &8- &7" + now.author()
                    + (now.caller().isEmpty() ? "" : " &8by &7" + now.caller())));
        }
        var queue = plugin.getAllMusicHook().getQueue();
        if (queue.isEmpty()) {
            player.sendMessage(ColorUtil.colorize(" &7队列为空"));
            return;
        }
        for (var q : queue) {
            player.sendMessage(ColorUtil.colorize(" &f#" + q.index() + " &7" + q.name()
                    + " &8- &7" + q.author()
                    + (q.caller().isEmpty() ? "" : " &8by &7" + q.caller())));
        }
    }

    private void sendHistoryChat(Player player) {
        var list = plugin.getMusicHistoryManager().recent();
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &e播放记录 &8(会话内存，重启清空)"));
        if (list.isEmpty()) {
            player.sendMessage(ColorUtil.colorize(" &7暂无记录"));
            return;
        }
        int i = 1;
        for (var e : list) {
            player.sendMessage(ColorUtil.colorize(" &f" + i++ + ". &8[" + e.time() + "] &7"
                    + e.name() + " &8- &7" + e.author()
                    + (e.caller().isEmpty() ? "" : " &8by &7" + e.caller())));
        }
    }

    // ── Kit 管理 / 领取 ───────────────────────────────────────────────────────

    private void handleAdminKit(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (!player.hasPermission("es2uni.admin")) return;
        int slot = event.getRawSlot();
        AdminKitGUI gui = plugin.getAdminKitGUI();

        if (slot == AdminKitGUI.CLOSE_SLOT) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == AdminKitGUI.BACK_SLOT) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }
        if (slot == AdminKitGUI.CREATE_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> plugin.getAnvilInputGUI().openForKitName(player), 1L);
            return;
        }
        if (slot == AdminKitGUI.INFO_SLOT) return;

        String name = gui.getAt(player, slot);
        if (name == null) return;
        if (event.isRightClick()) {
            plugin.getKitManager().delete(name);
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已删除套件 &f" + name));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
            return;
        }
        gui.cleanup(player);
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getKitEditGUI().open(player, name));
    }

    private void handleKitEdit(InventoryClickEvent event, Player player) {
        if (!player.hasPermission("es2uni.admin")) {
            event.setCancelled(true);
            return;
        }
        int slot = event.getRawSlot();
        // 0-44 允许自由放物品
        if (slot >= 0 && slot < 45) return;
        if (slot >= 54) return; // 玩家背包

        event.setCancelled(true);
        KitEditGUI gui = plugin.getKitEditGUI();
        Inventory top = event.getView().getTopInventory();

        if (slot == KitEditGUI.SLOT_CLOSE) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == KitEditGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminKitGUI().open(player));
            return;
        }
        if (slot == KitEditGUI.SLOT_MODE) {
            gui.toggleMode(player, top);
            return;
        }
        if (slot == KitEditGUI.SLOT_COOLDOWN) {
            awaitingKitCd.add(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7在聊天输入冷却秒数，或 &fcancel &7取消"));
            return;
        }
        if (slot == KitEditGUI.SLOT_IMPORT) {
            gui.importFromInventory(player, top);
            return;
        }
        if (slot == KitEditGUI.SLOT_SAVE) {
            gui.captureItems(player, top);
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7套件已保存"));
            String name = gui.getEditing(player);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (name != null) gui.open(player, name);
            });
        }
    }

    private void handleKitList(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        KitListGUI gui = plugin.getKitListGUI();

        if (slot == KitListGUI.CLOSE_SLOT) {
            gui.cleanup(player); player.closeInventory(); return;
        }
        if (slot == KitListGUI.BACK_SLOT) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }

        String entry = gui.getAt(player, slot);
        if (entry == null) return;
        player.closeInventory();
        gui.cleanup(player);

        if (entry.startsWith("ess:")) {
            String name = entry.substring(4);
            String err = plugin.getEssentialsHook().giveKit(player, name);
            player.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[ECOS] &7已领取 Essentials 套件 &f" + name
                    : "&8[ECOS] &c" + err));
        } else {
            String name = entry.substring(6);
            String err = plugin.getKitManager().claim(player, name);
            player.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[ECOS] &7已领取套件 &f" + name
                    : "&8[ECOS] &c" + err));
        }
    }

    // ── 特效商店 ──────────────────────────────────────────────────────────────

    private void handleAddFriend(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        AddFriendGUI gui = plugin.getAddFriendGUI();

        if (slot == AddFriendGUI.SLOT_CLOSE) {
            gui.cleanup(player);
            player.closeInventory();
            return;
        }
        if (slot == AddFriendGUI.SLOT_BACK) {
            gui.cleanup(player);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getFriendListGUI().open(player));
            return;
        }
        if (slot == AddFriendGUI.SLOT_CHAT) {
            player.closeInventory();
            awaitingFriendName.add(player.getUniqueId());
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7在聊天输入玩家名发送好友请求，或 &fcancel"));
            return;
        }
        if (gui.isListSlot(slot)) {
            UUID targetUuid = gui.getAt(player, slot);
            if (targetUuid == null) return;
            Player target = Bukkit.getPlayer(targetUuid);
            gui.cleanup(player);
            player.closeInventory();
            if (target == null) {
                player.sendMessage(ColorUtil.colorize("&8[ECOS] &7玩家已离线"));
                Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
                return;
            }
            trySendFriendRequest(player, target.getName());
        }
    }

    private void trySendFriendRequest(Player player, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7玩家不在线或不存在: &f" + name));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAddFriendGUI().open(player));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7不能添加自己"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAddFriendGUI().open(player));
            return;
        }
        boolean sent = plugin.getFriendManager().sendRequest(player, target);
        if (sent) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已向 &f" + target.getName() + " &7发送好友请求"));
            target.sendMessage(ColorUtil.colorize("&8[ECOS] &f" + player.getName() + " &7向你发送了好友请求"));
        } else {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已是好友或请求已发送"));
        }
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getFriendListGUI().open(player));
    }

    private void handleAuraShop(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // 关闭
        if (slot == AuraShopGUI.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        if (slot == AuraShopGUI.SLOT_BRIGHTNESS) {
            var br = plugin.getAuraManager().cycleBrightness(player.getUniqueId());
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7粒子亮度: &f" + br.label));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAuraShopGUI().open(player));
            return;
        }

        // 卸除当前光环
        if (slot == AuraShopGUI.SLOT_UNEQUIP) {
            plugin.getAuraManager().unequip(player.getUniqueId());
            player.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&8[ECOS] &7已卸除光环"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAuraShopGUI().open(player));
            return;
        }

        // 光环槽位
        int[] auraSlots = AuraShopGUI.AURA_SLOTS;
        AuraType[] auras = AuraType.values();
        for (int i = 0; i < Math.min(auras.length, auraSlots.length); i++) {
            if (slot != auraSlots[i]) continue;
            AuraType aura = auras[i];
            UUID uuid = player.getUniqueId();
            boolean owns = plugin.getAuraManager().owns(uuid, aura);

            if (owns) {
                // 已拥有：切换装备/卸除
                if (plugin.getAuraManager().getEquipped(uuid) == aura) {
                    plugin.getAuraManager().unequip(uuid);
                    player.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                            "&8[ECOS] &7已卸除 &f" + aura.displayName()));
                } else {
                    plugin.getAuraManager().equip(uuid, aura);
                    player.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                            "&8[ECOS] &7已装备 &b" + aura.displayName()));
                }
            } else {
                // 未拥有：购买
                if (!plugin.getVaultHook().isEnabled()) {
                    player.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&8[ECOS] &7经济系统不可用"));
                    return;
                }
                String err = plugin.getVaultHook().withdraw(player, aura.price);
                if (err != null) {
                    player.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize("&8[ECOS] &c" + err));
                    return;
                }
                plugin.getAuraManager().giveAura(uuid, aura);
                plugin.getAuraManager().equip(uuid, aura);
                player.sendMessage(com.etherstories.escore.utils.ColorUtil.colorize(
                        "&8[ECOS] &7已购买并装备 &b" + aura.displayName()
                        + " &8（扣款 " + plugin.getVaultHook().format(aura.price) + "）"));
            }
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAuraShopGUI().open(player));
            return;
        }
    }

    private String t(String raw) { return ColorUtil.colorize(raw); }

    private boolean isPlayerSelectTitle(String title) {
        return title.equals(t(plugin.getConfigManager().getTpaSelectTitle()))
                || title.equals(t(plugin.getConfigManager().getTpaHereSelectTitle()))
                || title.equals(t(plugin.getConfigManager().getPaySelectTitle()))
                || title.equals(t("&7选择留言对象"));
    }
}
