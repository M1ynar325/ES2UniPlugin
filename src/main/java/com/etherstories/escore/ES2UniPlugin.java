package com.etherstories.escore;

import com.etherstories.escore.commands.ES2UniCommand;
import com.etherstories.escore.gui.*;
import com.etherstories.escore.hooks.EssentialsHook;
import com.etherstories.escore.hooks.VaultHook;
import com.etherstories.escore.hooks.AllMusicHook;
import com.etherstories.escore.items.ECOSTerminalItem;
import com.etherstories.escore.items.SkillBinder;
import com.etherstories.escore.items.WeaponPreset;
import com.etherstories.escore.listeners.AFKListener;
import com.etherstories.escore.listeners.ChatItemListener;
import com.etherstories.escore.listeners.GUIListener;
import com.etherstories.escore.listeners.PlayerListener;
import com.etherstories.escore.listeners.WeaponListener;
import com.etherstories.escore.managers.*;
import com.etherstories.escore.managers.AuraManager;
import com.etherstories.escore.tasks.*;
import com.etherstories.escore.utils.ColorUtil;
import com.etherstories.escore.web.ChatFeed;
import com.etherstories.escore.web.EcosWebChatListener;
import com.etherstories.escore.web.EcosWebServer;
import com.etherstories.escore.web.WebSessions;
import org.bukkit.plugin.java.JavaPlugin;

public class ES2UniPlugin extends JavaPlugin {

    private static ES2UniPlugin instance;

    // Managers
    private ConfigManager    configManager;
    private AFKManager       afkManager;
    private PlaytimeManager  playtimeManager;
    private EventManager     eventManager;
    private FriendManager    friendManager;
    private MailManager      mailManager;
    private NoticeManager    noticeManager;
    private CheckInManager   checkInManager;
    private RegionManager    regionManager;
    private WaypointManager  waypointManager;
    private StatusManager    statusManager;
    private DeathManager     deathManager;
    private MilestoneManager milestoneManager;
    private WeaponManager    weaponManager;
    private AuraManager      auraManager;
    private KitManager       kitManager;
    private WeaponSaveManager weaponSaveManager;
    private MusicHistoryManager musicHistoryManager;
    private MusicFavoritesManager musicFavoritesManager;
    private MusicPlaylistManager musicPlaylistManager;
    private TaxManager       taxManager;
    private ActionBarManager actionBarManager;
    private TradeStatsManager tradeStatsManager;
    private AuditLogManager  auditLogManager;
    private LoginLogManager  loginLogManager;
    private ReportManager    reportManager;
    private ShowcaseManager  showcaseManager;
    private LuckyBlockManager luckyBlockManager;
    private NewbieGuideManager newbieGuideManager;
    private JobBoardManager  jobBoardManager;
    private CleanupRequestManager cleanupRequestManager;
    private InsuranceManager insuranceManager;
    private RecycleBinManager recycleBinManager;
    private TransitManager   transitManager;
    private EstateManager    estateManager;
    private HotelManager     hotelManager;
    private SkillShopManager skillShopManager;

    // Hooks
    private EssentialsHook essentialsHook;
    private VaultHook      vaultHook;
    private com.etherstories.escore.hooks.ESLinkHook esLinkHook = new com.etherstories.escore.hooks.ESLinkHook();
    private AllMusicHook   allMusicHook;

    // Command
    private ES2UniCommand ecosCommand;

    // GUIs
    private ECOSTerminalGUI  ecosTerminalGUI;
    private EventListGUI     eventListGUI;
    private AdminEventGUI    adminEventGUI;
    private AnvilInputGUI    anvilInputGUI;
    private HomeListGUI      homeListGUI;
    private PlayerSelectGUI  playerSelectGUI;
    private PayConfirmGUI    payConfirmGUI;
    private FriendListGUI    friendListGUI;
    private MailboxGUI       mailboxGUI;
    private OnlineListGUI    onlineListGUI;
    private PlayerActionGUI  playerActionGUI;
    private NoticeGUI        noticeGUI;
    private LeaderboardGUI   leaderboardGUI;
    private WaypointGUI      waypointGUI;
    private TerritoryListGUI  territoryListGUI;
    private AdminTerritoryGUI adminTerritoryGUI;
    private MyTerritoryGUI    myTerritoryGUI;
    private AdminRegionGUI    adminRegionGUI;
    private AuraShopGUI       auraShopGUI;
    private AdminKitGUI       adminKitGUI;
    private KitEditGUI        kitEditGUI;
    private KitListGUI        kitListGUI;
    private MusicMenuGUI      musicMenuGUI;
    private MusicSearchResultGUI musicSearchResultGUI;
    private MusicHistoryGUI   musicHistoryGUI;
    private MusicFavoritesGUI musicFavoritesGUI;
    private MusicPlaylistGUI  musicPlaylistGUI;
    private MusicDetailGUI    musicDetailGUI;
    private AdminTaxGUI       adminTaxGUI;
    private AdminBroadcastGUI adminBroadcastGUI;
    private AddFriendGUI      addFriendGUI;
    private CheckInCalendarGUI checkInCalendarGUI;
    private TradeBoardGUI     tradeBoardGUI;
    private ShowcaseGUI       showcaseGUI;
    private NewbieGuideGUI    newbieGuideGUI;
    private MunicipalOfficeGUI municipalOfficeGUI;
    private JobBoardGUI       jobBoardGUI;
    private JobHistoryGUI     jobHistoryGUI;
    private InsuranceGUI      insuranceGUI;
    private InsuranceBackupGUI insuranceBackupGUI;
    private RecycleBinGUI     recycleBinGUI;
    private TransitOfficeGUI  transitOfficeGUI;
    private TransitMapGUI     transitMapGUI;
    private TransitTicketGUI  transitTicketGUI;
    private TransitHistoryGUI transitHistoryGUI;
    private TransitTvmGUI     transitTvmGUI;
    private TransitAdjustGUI  transitAdjustGUI;
    private TransitAdminGUI   transitAdminGUI;
    private TransitEdgesGUI   transitEdgesGUI;
    private TransitStationGUI transitStationGUI;
    private TransitAnnounceGUI transitAnnounceGUI;
    private TransitTypesGUI transitTypesGUI;
    private TransitTypeEditGUI transitTypeEditGUI;
    private TransitClaimsGUI transitClaimsGUI;
    private TransitRidersGUI transitRidersGUI;
    private TransitBoardGUI transitBoardGUI;
    private EstateBuildingsGUI estateBuildingsGUI;
    private EstateRoomsGUI     estateRoomsGUI;
    private EstateMineGUI      estateMineGUI;
    private EstateSaleGUI      estateSaleGUI;
    private EstateToolsGUI     estateToolsGUI;
    private HotelListGUI       hotelListGUI;
    private HotelDeskGUI       hotelDeskGUI;
    private AdminGrantGUI      adminGrantGUI;
    private SkillShopGUI       skillShopGUI;

    // Tasks
    private TabTask        tabTask;
    private BroadcastTask  broadcastTask;
    private TPSMonitorTask tpsMonitorTask;
    private AFKCheckTask   afkCheckTask;
    private ActionBarTask  actionBarTask;
    private WebSessions    webSessions;
    private ChatFeed       chatFeed;
    private EcosWebServer  ecosWebServer;
    private boolean        webChatBound;

    @Override
    public void onEnable() {
        instance = this;

        ECOSTerminalItem.init(this);
        SkillBinder.init(this);
        com.etherstories.escore.weapons.PlayerSkills.init(this);
        WeaponPreset.init(this);
        com.etherstories.escore.items.TransitItems.init(this);
        com.etherstories.escore.items.PveItems.init(this);
        com.etherstories.escore.items.HotelCard.init(this);
        com.etherstories.escore.items.EstateWand.init(this);
        saveDefaultConfig();

        configManager    = new ConfigManager(this);
        afkManager       = new AFKManager(this);
        playtimeManager  = new PlaytimeManager(this);
        eventManager     = new EventManager();
        friendManager    = new FriendManager(this);
        mailManager      = new MailManager(this);
        noticeManager    = new NoticeManager(this);
        checkInManager   = new CheckInManager(this);
        regionManager    = new RegionManager(this);
        waypointManager  = new WaypointManager(this);
        statusManager    = new StatusManager(this);
        deathManager     = new DeathManager(this);
        milestoneManager = new MilestoneManager(this);
        weaponManager    = new WeaponManager(this);
        auraManager      = new AuraManager(this);
        kitManager       = new KitManager(this);
        weaponSaveManager = new WeaponSaveManager(this);
        musicHistoryManager = new MusicHistoryManager(this);
        musicFavoritesManager = new MusicFavoritesManager(this);
        musicPlaylistManager = new MusicPlaylistManager(this);
        taxManager       = new TaxManager(this);
        actionBarManager = new ActionBarManager();
        tradeStatsManager = new TradeStatsManager(this);
        auditLogManager  = new AuditLogManager(this);
        loginLogManager  = new LoginLogManager(this);
        chatFeed         = new ChatFeed(160);
        reportManager    = new ReportManager(this);
        showcaseManager  = new ShowcaseManager(this);
        luckyBlockManager = new LuckyBlockManager(this);
        newbieGuideManager = new NewbieGuideManager(this);
        jobBoardManager  = new JobBoardManager(this);
        cleanupRequestManager = new CleanupRequestManager(this);
        insuranceManager = new InsuranceManager(this);
        recycleBinManager = new RecycleBinManager(this);
        transitManager   = new TransitManager(this);
        estateManager    = new EstateManager(this);
        hotelManager     = new HotelManager(this);
        skillShopManager = new SkillShopManager(this);

        essentialsHook = new EssentialsHook(); essentialsHook.hook();
        vaultHook      = new VaultHook();      vaultHook.hook();
        esLinkHook     = new com.etherstories.escore.hooks.ESLinkHook();
        allMusicHook   = new AllMusicHook();   allMusicHook.hook(getLogger());

        ecosTerminalGUI = new ECOSTerminalGUI(this);
        eventListGUI    = new EventListGUI(this);
        adminEventGUI   = new AdminEventGUI(this);
        anvilInputGUI   = new AnvilInputGUI(this);
        homeListGUI     = new HomeListGUI(this);
        playerSelectGUI = new PlayerSelectGUI(this);
        payConfirmGUI   = new PayConfirmGUI(this);
        friendListGUI   = new FriendListGUI(this);
        mailboxGUI      = new MailboxGUI(this);
        onlineListGUI   = new OnlineListGUI(this);
        playerActionGUI = new PlayerActionGUI(this);
        noticeGUI       = new NoticeGUI(this);
        leaderboardGUI  = new LeaderboardGUI(this);
        waypointGUI       = new WaypointGUI(this);
        territoryListGUI  = new TerritoryListGUI(this);
        adminTerritoryGUI = new AdminTerritoryGUI(this);
        myTerritoryGUI    = new MyTerritoryGUI(this);
        adminRegionGUI    = new AdminRegionGUI(this);
        auraShopGUI       = new AuraShopGUI(this);
        adminKitGUI       = new AdminKitGUI(this);
        kitEditGUI        = new KitEditGUI(this);
        kitListGUI        = new KitListGUI(this);
        musicMenuGUI      = new MusicMenuGUI(this);
        musicSearchResultGUI = new MusicSearchResultGUI(this);
        musicHistoryGUI   = new MusicHistoryGUI(this);
        musicFavoritesGUI = new MusicFavoritesGUI(this);
        musicPlaylistGUI  = new MusicPlaylistGUI(this);
        musicDetailGUI    = new MusicDetailGUI(this);
        adminTaxGUI       = new AdminTaxGUI(this);
        adminBroadcastGUI = new AdminBroadcastGUI(this);
        addFriendGUI      = new AddFriendGUI(this);
        checkInCalendarGUI = new CheckInCalendarGUI(this);
        tradeBoardGUI     = new TradeBoardGUI(this);
        showcaseGUI       = new ShowcaseGUI(this);
        newbieGuideGUI    = new NewbieGuideGUI(this);
        municipalOfficeGUI = new MunicipalOfficeGUI(this);
        jobBoardGUI       = new JobBoardGUI(this);
        jobHistoryGUI     = new JobHistoryGUI(this);
        insuranceGUI      = new InsuranceGUI(this);
        insuranceBackupGUI = new InsuranceBackupGUI(this);
        recycleBinGUI     = new RecycleBinGUI(this);
        transitOfficeGUI  = new TransitOfficeGUI(this);
        transitMapGUI     = new TransitMapGUI(this);
        transitTicketGUI  = new TransitTicketGUI(this);
        transitHistoryGUI = new TransitHistoryGUI(this);
        transitTvmGUI     = new TransitTvmGUI(this);
        transitAdjustGUI  = new TransitAdjustGUI(this);
        transitAdminGUI   = new TransitAdminGUI(this);
        transitEdgesGUI   = new TransitEdgesGUI(this);
        transitStationGUI = new TransitStationGUI(this);
        transitAnnounceGUI = new TransitAnnounceGUI(this);
        transitTypesGUI = new TransitTypesGUI(this);
        transitTypeEditGUI = new TransitTypeEditGUI(this);
        transitClaimsGUI = new TransitClaimsGUI(this);
        transitRidersGUI = new TransitRidersGUI(this);
        transitBoardGUI = new TransitBoardGUI(this);
        estateBuildingsGUI = new EstateBuildingsGUI(this);
        estateRoomsGUI     = new EstateRoomsGUI(this);
        estateMineGUI      = new EstateMineGUI(this);
        estateSaleGUI      = new EstateSaleGUI(this);
        estateToolsGUI     = new EstateToolsGUI(this);
        hotelListGUI       = new HotelListGUI(this);
        hotelDeskGUI       = new HotelDeskGUI(this);
        adminGrantGUI      = new AdminGrantGUI(this);
        skillShopGUI       = new SkillShopGUI(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new AFKListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatItemListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new WeaponListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.etherstories.escore.listeners.TimeStopListener(), this);
        getServer().getPluginManager().registerEvents(
                new com.etherstories.escore.listeners.PveListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.etherstories.escore.listeners.TransitGateListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.etherstories.escore.listeners.PayCommandTaxListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.etherstories.escore.listeners.HotelListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.etherstories.escore.listeners.EstateListener(this), this);
        new com.etherstories.escore.listeners.QuickShopTaxListener(this).tryRegister();

        ES2UniCommand cmd = new ES2UniCommand(this);
        ecosCommand = cmd;
        getCommand("es2").setExecutor(cmd);
        getCommand("es2").setTabCompleter(cmd);

        startTasks();
        startWeb();

        String ver = getDescription().getVersion();
        getServer().getConsoleSender().sendMessage(ColorUtil.colorize(
                "&b  ___  ____ ___  ____\n" +
                "&b | __||    |   \\/ ___|\n" +
                "&b | _|  \\  / | | \\___ \\\n" +
                "&b |___| |__| |___/____/\n" +
                "&b ECOS Terminal &8v" + ver + " &7| EtherStories ES2\n" +
                "&8 Essentials: " + (essentialsHook.isEnabled() ? "&aOK" : "&cN/A") +
                "  &8Vault: "     + (vaultHook.isEnabled()      ? "&aOK" : "&cN/A")));
        getLogger().info("enabled v" + ver + ".");
    }

    @Override
    public void onDisable() {
        if (weaponManager != null) weaponManager.disable();
        if (auraManager   != null) auraManager.disable();
        if (musicHistoryManager != null) {
            musicHistoryManager.stop();
            musicHistoryManager.clear();
        }
        if (musicPlaylistManager != null) musicPlaylistManager.stop(false);
        if (tradeStatsManager != null) tradeStatsManager.stop();
        if (recycleBinManager != null) recycleBinManager.save();
        if (transitManager != null) transitManager.save();
        if (estateManager != null) estateManager.save();
        if (hotelManager != null) hotelManager.save();
        if (skillShopManager != null) skillShopManager.save();
        stopTasks();
        stopWeb();
        getLogger().info("ES2UniPlugin disabled.");
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        kitManager.reload();
        if (weaponSaveManager != null) weaponSaveManager.reload();
        if (transitManager != null) transitManager.load();
        if (estateManager != null) estateManager.load();
        if (hotelManager != null) {
            hotelManager.load();
            hotelManager.sweepOrphans();
        }
        if (skillShopManager != null) skillShopManager.load();
        stopTasks();
        startTasks();
        if (tradeStatsManager != null) tradeStatsManager.startScheduler();
        stopWeb();
        startWeb();
    }

    private void startTasks() {
        tabTask = new TabTask(this);
        tabTask.runTaskTimer(this, 20L, configManager.getTabUpdateInterval());
        broadcastTask = new BroadcastTask(this);
        broadcastTask.runTaskTimer(this, 200L, configManager.getBroadcastInterval() * 20L);
        tpsMonitorTask = new TPSMonitorTask(this);
        tpsMonitorTask.runTaskTimer(this, 100L, 100L);
        afkCheckTask = new AFKCheckTask(this);
        afkCheckTask.runTaskTimer(this, 20L, 20L);
        actionBarTask = new ActionBarTask(this);
        // 10 tick 刷新 sticky，避免客户端淡出抽搐；temp 优先级由 ActionBarManager 处理
        actionBarTask.runTaskTimer(this, 10L, 10L);
        if (musicHistoryManager != null) musicHistoryManager.start();
    }

    private void startWeb() {
        if (chatFeed == null) chatFeed = new ChatFeed(160);
        if (!webChatBound) {
            getServer().getPluginManager().registerEvents(new EcosWebChatListener(chatFeed), this);
            webChatBound = true;
        }
        webSessions = new WebSessions(
                getConfig().getInt("web.pair-seconds", 120),
                getConfig().getInt("web.session-days", 7));
        ecosWebServer = new EcosWebServer(this, webSessions, chatFeed);
        try {
            ecosWebServer.start();
        } catch (Exception e) {
            getLogger().warning("ECOS Web 启动失败: " + e.getMessage());
        }
    }

    private void stopWeb() {
        if (ecosWebServer != null) {
            ecosWebServer.stop();
            ecosWebServer = null;
        }
    }

    private void stopTasks() {
        if (tabTask        != null && !tabTask.isCancelled())        tabTask.cancel();
        if (broadcastTask  != null && !broadcastTask.isCancelled())  broadcastTask.cancel();
        if (tpsMonitorTask != null && !tpsMonitorTask.isCancelled()) tpsMonitorTask.cancel();
        if (afkCheckTask   != null && !afkCheckTask.isCancelled())   afkCheckTask.cancel();
        if (actionBarTask  != null && !actionBarTask.isCancelled())  actionBarTask.cancel();
        if (musicHistoryManager != null) musicHistoryManager.stop();
    }

    public static ES2UniPlugin getInstance() { return instance; }
    public WebSessions getWebSessions() { return webSessions; }
    public EcosWebServer getEcosWebServer() { return ecosWebServer; }

    public ES2UniCommand    getEcosCommand()      { return ecosCommand; }
    public ConfigManager    getConfigManager()    { return configManager; }
    public AFKManager       getAfkManager()       { return afkManager; }
    public PlaytimeManager  getPlaytimeManager()  { return playtimeManager; }
    public EventManager     getEventManager()     { return eventManager; }
    public EssentialsHook   getEssentialsHook()   { return essentialsHook; }
    public VaultHook        getVaultHook()        { return vaultHook; }
    public FriendManager    getFriendManager()    { return friendManager; }
    public MailManager      getMailManager()      { return mailManager; }
    public NoticeManager    getNoticeManager()    { return noticeManager; }
    public CheckInManager   getCheckInManager()   { return checkInManager; }
    public RegionManager    getRegionManager()    { return regionManager; }
    public WaypointManager  getWaypointManager()  { return waypointManager; }
    public StatusManager    getStatusManager()    { return statusManager; }
    public DeathManager     getDeathManager()     { return deathManager; }
    public MilestoneManager getMilestoneManager() { return milestoneManager; }

    public ECOSTerminalGUI  getEcosTerminalGUI()  { return ecosTerminalGUI; }
    public EventListGUI     getEventListGUI()     { return eventListGUI; }
    public AdminEventGUI    getAdminEventGUI()    { return adminEventGUI; }
    public AnvilInputGUI    getAnvilInputGUI()    { return anvilInputGUI; }
    public HomeListGUI      getHomeListGUI()      { return homeListGUI; }
    public PlayerSelectGUI  getPlayerSelectGUI()  { return playerSelectGUI; }
    public PayConfirmGUI    getPayConfirmGUI()    { return payConfirmGUI; }
    public FriendListGUI    getFriendListGUI()    { return friendListGUI; }
    public MailboxGUI       getMailboxGUI()       { return mailboxGUI; }
    public OnlineListGUI    getOnlineListGUI()    { return onlineListGUI; }
    public PlayerActionGUI  getPlayerActionGUI()  { return playerActionGUI; }
    public NoticeGUI        getNoticeGUI()        { return noticeGUI; }
    public LeaderboardGUI   getLeaderboardGUI()   { return leaderboardGUI; }
    public WaypointGUI      getWaypointGUI()        { return waypointGUI; }
    public TerritoryListGUI  getTerritoryListGUI()   { return territoryListGUI; }
    public AdminTerritoryGUI getAdminTerritoryGUI()  { return adminTerritoryGUI; }
    public MyTerritoryGUI    getMyTerritoryGUI()     { return myTerritoryGUI; }
    public AdminRegionGUI    getAdminRegionGUI()     { return adminRegionGUI; }
    public TPSMonitorTask   getTpsMonitorTask()   { return tpsMonitorTask; }
    public ActionBarTask    getActionBarTask()    { return actionBarTask; }
    public WeaponManager    getWeaponManager()    { return weaponManager; }
    public AuraManager      getAuraManager()      { return auraManager; }
    public AuraShopGUI      getAuraShopGUI()      { return auraShopGUI; }
    public KitManager       getKitManager()       { return kitManager; }
    public WeaponSaveManager getWeaponSaveManager() { return weaponSaveManager; }
    public AdminKitGUI      getAdminKitGUI()      { return adminKitGUI; }
    public KitEditGUI       getKitEditGUI()       { return kitEditGUI; }
    public KitListGUI       getKitListGUI()       { return kitListGUI; }
    public AllMusicHook     getAllMusicHook()     { return allMusicHook; }
    public TaxManager       getTaxManager()       { return taxManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
    public MusicHistoryManager getMusicHistoryManager() { return musicHistoryManager; }
    public MusicFavoritesManager getMusicFavoritesManager() { return musicFavoritesManager; }
    public MusicPlaylistManager getMusicPlaylistManager() { return musicPlaylistManager; }
    public MusicMenuGUI     getMusicMenuGUI()     { return musicMenuGUI; }
    public MusicSearchResultGUI getMusicSearchResultGUI() { return musicSearchResultGUI; }
    public MusicHistoryGUI  getMusicHistoryGUI()  { return musicHistoryGUI; }
    public MusicFavoritesGUI getMusicFavoritesGUI() { return musicFavoritesGUI; }
    public MusicPlaylistGUI getMusicPlaylistGUI() { return musicPlaylistGUI; }
    public MusicDetailGUI   getMusicDetailGUI()   { return musicDetailGUI; }
    public AdminTaxGUI      getAdminTaxGUI()      { return adminTaxGUI; }
    public com.etherstories.escore.hooks.ESLinkHook getESLinkHook() { return esLinkHook; }
    public AdminBroadcastGUI getAdminBroadcastGUI() { return adminBroadcastGUI; }
    public AddFriendGUI     getAddFriendGUI()     { return addFriendGUI; }
    public CheckInCalendarGUI getCheckInCalendarGUI() { return checkInCalendarGUI; }
    public TradeBoardGUI    getTradeBoardGUI()    { return tradeBoardGUI; }
    public TradeStatsManager getTradeStatsManager() { return tradeStatsManager; }
    public AuditLogManager  getAuditLogManager()  { return auditLogManager; }
    public LoginLogManager  getLoginLogManager()  { return loginLogManager; }
    public ChatFeed         getChatFeed()         { return chatFeed; }
    public ReportManager    getReportManager()    { return reportManager; }
    public ShowcaseManager  getShowcaseManager()  { return showcaseManager; }
    public LuckyBlockManager getLuckyBlockManager() { return luckyBlockManager; }
    public NewbieGuideManager getNewbieGuideManager() { return newbieGuideManager; }
    public ShowcaseGUI      getShowcaseGUI()      { return showcaseGUI; }
    public NewbieGuideGUI   getNewbieGuideGUI()   { return newbieGuideGUI; }
    public JobBoardManager  getJobBoardManager()  { return jobBoardManager; }
    public CleanupRequestManager getCleanupRequestManager() { return cleanupRequestManager; }
    public InsuranceManager getInsuranceManager() { return insuranceManager; }
    public RecycleBinManager getRecycleBinManager() { return recycleBinManager; }
    public MunicipalOfficeGUI getMunicipalOfficeGUI() { return municipalOfficeGUI; }
    public JobBoardGUI      getJobBoardGUI()      { return jobBoardGUI; }
    public JobHistoryGUI    getJobHistoryGUI()    { return jobHistoryGUI; }
    public InsuranceGUI     getInsuranceGUI()     { return insuranceGUI; }
    public InsuranceBackupGUI getInsuranceBackupGUI() { return insuranceBackupGUI; }
    public RecycleBinGUI    getRecycleBinGUI()    { return recycleBinGUI; }
    public TransitManager   getTransitManager()   { return transitManager; }
    public TransitOfficeGUI getTransitOfficeGUI() { return transitOfficeGUI; }
    public TransitMapGUI    getTransitMapGUI()    { return transitMapGUI; }
    public TransitTicketGUI getTransitTicketGUI() { return transitTicketGUI; }
    public TransitHistoryGUI getTransitHistoryGUI() { return transitHistoryGUI; }
    public TransitTvmGUI    getTransitTvmGUI()    { return transitTvmGUI; }
    public TransitAdjustGUI getTransitAdjustGUI() { return transitAdjustGUI; }
    public TransitAdminGUI  getTransitAdminGUI()  { return transitAdminGUI; }
    public TransitEdgesGUI  getTransitEdgesGUI()  { return transitEdgesGUI; }
    public TransitStationGUI getTransitStationGUI() { return transitStationGUI; }
    public TransitAnnounceGUI getTransitAnnounceGUI() { return transitAnnounceGUI; }
    public TransitTypesGUI getTransitTypesGUI() { return transitTypesGUI; }
    public TransitTypeEditGUI getTransitTypeEditGUI() { return transitTypeEditGUI; }
    public TransitClaimsGUI getTransitClaimsGUI() { return transitClaimsGUI; }
    public TransitRidersGUI getTransitRidersGUI() { return transitRidersGUI; }
    public TransitBoardGUI getTransitBoardGUI() { return transitBoardGUI; }
    public EstateManager getEstateManager() { return estateManager; }
    public EstateBuildingsGUI getEstateBuildingsGUI() { return estateBuildingsGUI; }
    public EstateRoomsGUI getEstateRoomsGUI() { return estateRoomsGUI; }
    public EstateMineGUI getEstateMineGUI() { return estateMineGUI; }
    public EstateSaleGUI getEstateSaleGUI() { return estateSaleGUI; }
    public EstateToolsGUI getEstateToolsGUI() { return estateToolsGUI; }
    public HotelManager getHotelManager() { return hotelManager; }
    public HotelListGUI getHotelListGUI() { return hotelListGUI; }
    public HotelDeskGUI getHotelDeskGUI() { return hotelDeskGUI; }
    public AdminGrantGUI getAdminGrantGUI() { return adminGrantGUI; }
    public SkillShopManager getSkillShopManager() { return skillShopManager; }
    public SkillShopGUI getSkillShopGUI() { return skillShopGUI; }
}
