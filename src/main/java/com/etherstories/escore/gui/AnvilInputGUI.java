package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.HotelManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnvilInputGUI {

    public enum Context {
        CREATE_EVENT_NAME, PAY_AMOUNT, PAY_ALL_AMOUNT, ADD_FRIEND_NAME, SET_STATUS, ADD_WAYPOINT, CREATE_KIT_NAME,
        MUSIC_SEARCH, MUSIC_ADD_ID,
        JOB_TITLE, JOB_REWARD, JOB_SLOTS, JOB_DESC, JOB_EDIT_TITLE,
        ESTATE_REGISTER, ESTATE_PRICE,
        HOTEL_CREATE, HOTEL_PRICE, HOTEL_TRANSFER, HOTEL_MGR, HOTEL_DELETE, GRANT_TICKET,
        SKILL_SHOP_PRICE
    }

    public record InputState(Context context, String currentInput, UUID metadata, String placeholder) {}

    /** 招工发布草稿 */
    public static final class JobDraft {
        public boolean official;
        public String title;
        public double reward;
        public int maxWorkers = 1;
        public String jobId; // 编辑用
    }

    private final ES2UniPlugin plugin;
    private final Map<UUID, InputState> pending = new HashMap<>();
    private final Map<UUID, JobDraft> jobDrafts = new HashMap<>();
    private final Map<UUID, String> estatePriceUnit = new HashMap<>();
    private final Map<UUID, String> hotelPriceUnit = new HashMap<>();
    private final Map<UUID, String> hotelContext = new HashMap<>();
    private final Map<UUID, String> grantKind = new HashMap<>();
    private final Map<UUID, String> skillShopKey = new HashMap<>();

    public AnvilInputGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    /** Admin: create event name. */
    public void openForEventName(Player player) {
        String placeholder = plugin.getConfigManager().getAnvilEventNamePlaceholder();
        open(player, Context.CREATE_EVENT_NAME, placeholder, null,
                "创建活动", "输入活动名称");
    }

    /** Set status/bio. */
    public void openForStatus(Player player) {
        String current = plugin.getStatusManager().getStatus(player.getUniqueId());
        open(player, Context.SET_STATUS, current != null ? current : "",
                null, "个人签名",
                "输入签名（最多 " + plugin.getStatusManager().maxLength() + " 字），打 - 清除");
    }

    /** Add waypoint: stores current location in metadata slot. */
    public void openForWaypointName(Player player) {
        open(player, Context.ADD_WAYPOINT, "坐标名称", null,
                "收藏坐标", "输入收藏名称");
    }

    /** Friend request: enter target player name. */
    public void openForFriendName(Player player) {
        open(player, Context.ADD_FRIEND_NAME, "玩家名称", null,
                "添加好友", "输入对方游戏名（需在线）");
    }

    /** Pay: enter transfer amount. metadata = target player UUID. */
    public void openForPayAmount(Player player, Player target) {
        openForPayAmount(player, (org.bukkit.OfflinePlayer) target);
    }

    public void openForPayAmount(Player player, org.bukkit.OfflinePlayer target) {
        String name = target.getName() != null ? target.getName() : "玩家";
        open(player, Context.PAY_AMOUNT, "0", target.getUniqueId(),
                "转账金额",
                "目标: " + name + (target.isOnline() ? "" : " (离线)") + " — 输入数字金额");
    }

    public void openForPayAllAmount(Player player) {
        int n = Math.max(0, org.bukkit.Bukkit.getOnlinePlayers().size() - 1);
        open(player, Context.PAY_ALL_AMOUNT, "0", null,
                "全员转账",
                "给除自己外 " + n + " 人各转一笔 — 输入每人金额");
    }

    /** Admin: create kit name. */
    public void openForKitName(Player player) {
        open(player, Context.CREATE_KIT_NAME, "套件名称", null,
                "新建 Kit", "输入套件 ID（字母/数字/_/-）");
    }

    public void openForEstateRegister(Player player) {
        open(player, Context.ESTATE_REGISTER, "星港商场 1 101 shop", null,
                "登记房产",
                "输入: 楼名 层 号 用途   例 星港一号 3 301 house");
    }

    public void openForEstatePrice(Player player, com.etherstories.escore.estate.EstateUnit unit) {
        estatePriceUnit.put(player.getUniqueId(), unit.id());
        String hint = unit.price() > 0 ? String.format("%.0f", unit.price()) : "2048";
        open(player, Context.ESTATE_PRICE, hint, null,
                "挂牌标价",
                "输入 " + unit.address() + " 的出售金额，确认后上架");
    }

    public void openForHotelCreate(Player player) {
        open(player, Context.HOTEL_CREATE, "星港酒店", null, "创建酒店", "输入酒店名称");
    }

    public void openForHotelPrice(Player player, String unitId, String hint) {
        hotelPriceUnit.put(player.getUniqueId(), unitId);
        open(player, Context.HOTEL_PRICE, hint, null, "酒店房价", "输入入住标价（客人另付税）");
    }

    public void openForHotelTransfer(Player player, String hotelId) {
        hotelContext.put(player.getUniqueId(), hotelId);
        open(player, Context.HOTEL_TRANSFER, "玩家名", null, "转让酒店", "输入新店主游戏名（进过服即可）");
    }

    public void openForHotelMgr(Player player, String hotelId) {
        hotelContext.put(player.getUniqueId(), hotelId);
        open(player, Context.HOTEL_MGR, "add 玩家", null, "酒店管理者", "输入: add 玩家  或  remove 玩家");
    }

    public void openForHotelDelete(Player player, String hotelId, String hotelName) {
        hotelContext.put(player.getUniqueId(), hotelId);
        open(player, Context.HOTEL_DELETE, "输入全名确认", null, "删除酒店",
                "输入「" + (hotelName == null ? "" : hotelName) + "」确认。住客会退房，房间解绑，房产还在。");
    }

    public void openForSkillShopPrice(Player player, com.etherstories.escore.weapons.SkillType skill) {
        skillShopKey.put(player.getUniqueId(), skill.configKey);
        String hint = String.format("%.0f", plugin.getSkillShopManager().price(skill));
        open(player, Context.SKILL_SHOP_PRICE, hint, null,
                "技能定价", "输入 " + skill.displayName() + " 的出售价格");
    }

    public void openForGrant(Player player, String kind) {
        grantKind.put(player.getUniqueId(), kind);
        open(player, Context.GRANT_TICKET, "玩家 1", null,
                kind.equals("lucky") ? "发幸运次数" : "发补签券",
                "输入: 玩家名 数量   离线也可");
    }

    /** AllMusic: search song by name. */
    public void openForMusicSearch(Player player) {
        open(player, Context.MUSIC_SEARCH, "歌名", null,
                "搜歌", "输入歌名关键词");
    }

    /** AllMusic: add by song ID. */
    public void openForMusicAddId(Player player) {
        open(player, Context.MUSIC_ADD_ID, "歌曲ID", null,
                "ID 点歌", "输入歌曲数字 ID（如网易云）");
    }

    /** 招工处：发布委托（个人/官方）。 */
    public void openForJobPost(Player player, boolean official) {
        JobDraft d = new JobDraft();
        d.official = official;
        jobDrafts.put(player.getUniqueId(), d);
        open(player, Context.JOB_TITLE, "委托标题", null,
                official ? "官方委托标题" : "个人委托标题",
                "例如：帮建车站 / 修主城喷泉");
    }

    /** 管理员改委托标题。 */
    public void openForJobEditTitle(Player player, String jobId) {
        JobDraft d = new JobDraft();
        d.jobId = jobId;
        jobDrafts.put(player.getUniqueId(), d);
        open(player, Context.JOB_EDIT_TITLE, "新标题", null,
                "修改委托标题", "输入新标题（会标记管理员已修改）");
    }

    public boolean isInInput(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public void updateInput(Player player, String text) {
        InputState state = pending.get(player.getUniqueId());
        if (state == null) return;
        String cleaned = sanitize(text);
        pending.put(player.getUniqueId(),
                new InputState(state.context(), cleaned, state.metadata(), state.placeholder()));
    }

    public void confirm(Player player) {
        InputState state = pending.remove(player.getUniqueId());
        if (state == null) return;

        switch (state.context()) {
            case CREATE_EVENT_NAME -> confirmEventName(player, state);
            case PAY_AMOUNT        -> confirmPayAmount(player, state);
            case PAY_ALL_AMOUNT    -> confirmPayAllAmount(player, state);
            case ADD_FRIEND_NAME   -> confirmFriendName(player, state);
            case SET_STATUS        -> confirmStatus(player, state);
            case ADD_WAYPOINT      -> confirmWaypoint(player, state);
            case CREATE_KIT_NAME   -> confirmKitName(player, state);
            case MUSIC_SEARCH      -> confirmMusicSearch(player, state);
            case MUSIC_ADD_ID      -> confirmMusicAddId(player, state);
            case JOB_TITLE         -> confirmJobTitle(player, state);
            case JOB_REWARD        -> confirmJobReward(player, state);
            case JOB_SLOTS         -> confirmJobSlots(player, state);
            case JOB_DESC          -> confirmJobDesc(player, state);
            case JOB_EDIT_TITLE    -> confirmJobEditTitle(player, state);
            case ESTATE_REGISTER  -> confirmEstateRegister(player, state);
            case ESTATE_PRICE     -> confirmEstatePrice(player, state);
            case HOTEL_CREATE     -> confirmHotelCreate(player, state);
            case HOTEL_PRICE      -> confirmHotelPrice(player, state);
            case HOTEL_TRANSFER   -> confirmHotelTransfer(player, state);
            case HOTEL_MGR        -> confirmHotelMgr(player, state);
            case HOTEL_DELETE     -> confirmHotelDelete(player, state);
            case GRANT_TICKET     -> confirmGrant(player, state);
            case SKILL_SHOP_PRICE -> confirmSkillShopPrice(player, state);
        }
    }

    public void cancel(Player player) {
        pending.remove(player.getUniqueId());
        jobDrafts.remove(player.getUniqueId());
        estatePriceUnit.remove(player.getUniqueId());
        hotelPriceUnit.remove(player.getUniqueId());
        hotelContext.remove(player.getUniqueId());
        grantKind.remove(player.getUniqueId());
        skillShopKey.remove(player.getUniqueId());
    }

    /** True if text is empty or still the placeholder. */
    public boolean isBlankOrPlaceholder(InputState state) {
        String text = sanitize(state.currentInput());
        return text.isEmpty() || text.equals(sanitize(state.placeholder()));
    }

    public static String sanitize(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(text).trim();
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void confirmFriendName(Player player, InputState state) {
        if (isBlankOrPlaceholder(state)) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7请输入玩家名称"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getFriendListGUI().open(player));
            return;
        }
        String name = sanitize(state.currentInput());
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7玩家不在线"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getFriendListGUI().open(player));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7不能添加自己"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getFriendListGUI().open(player));
            return;
        }
        boolean sent = plugin.getFriendManager().sendRequest(player, target);
        if (sent) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已向 " + target.getName() + " 发送好友请求"));
            target.sendMessage(ColorUtil.colorize("&8[ECOS] &f" + player.getName() + " &7向你发送了好友请求"));
        } else {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已是好友或请求已发送"));
        }
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getFriendListGUI().open(player));
    }

    private void confirmStatus(Player player, InputState state) {
        String text = sanitize(state.currentInput());
        if (text.equals("-") || text.equals("签名") || text.equals("输入签名")) text = "";
        plugin.getStatusManager().setStatus(player.getUniqueId(), text.isEmpty() ? null : text);
        player.sendMessage(ColorUtil.colorize(text.isEmpty()
                ? "&8[ECOS] &7签名已清除"
                : "&8[ECOS] &7签名已更新: &f" + text));
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
    }

    private void confirmWaypoint(Player player, InputState state) {
        if (isBlankOrPlaceholder(state)) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7请输入名称"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getWaypointGUI().open(player));
            return;
        }
        String name = sanitize(state.currentInput());
        org.bukkit.Location loc = player.getLocation();
        boolean ok = plugin.getWaypointManager().add(
                player.getUniqueId(), name,
                loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        player.sendMessage(ok
                ? ColorUtil.colorize("&8[ECOS] &7已收藏 &f" + name)
                : ColorUtil.colorize("&8[ECOS] &7收藏已满（上限 " + plugin.getWaypointManager().maxPerPlayer() + " 个）"));
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getWaypointGUI().open(player));
    }

    private void confirmEventName(Player player, InputState state) {
        if (isBlankOrPlaceholder(state)) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c活动名称不能为空。"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminEventGUI().open(player));
            return;
        }
        String name = sanitize(state.currentInput());
        plugin.getEventManager().createEvent(player, name, "", 0);
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &a活动 &f" + name + " &a已创建"));
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminEventGUI().open(player));
    }

    private void confirmKitName(Player player, InputState state) {
        if (isBlankOrPlaceholder(state)) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7请输入套件名称"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminKitGUI().open(player));
            return;
        }
        String name = sanitize(state.currentInput()).replace(' ', '_').toLowerCase();
        if (!name.matches("[a-z0-9_\\-]+")) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7名称仅允许字母/数字/_/-"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminKitGUI().open(player));
            return;
        }
        if (plugin.getKitManager().exists(name)) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7套件已存在: &f" + name));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminKitGUI().open(player));
            return;
        }
        plugin.getKitManager().createEmpty(name);
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已创建套件 &f" + name + " &7— 放入物品后点保存"));
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getKitEditGUI().open(player, name));
    }

    private void confirmMusicSearch(Player player, InputState state) {
        if (isBlankOrPlaceholder(state)) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7请输入歌名"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicMenuGUI().open(player));
            return;
        }
        String query = sanitize(state.currentInput());
        if (!plugin.getAllMusicHook().isAvailable()) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &cAllMusic 未加载"));
            return;
        }
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7正在搜索: &f" + query));
        plugin.getAllMusicHook().search(plugin, player, query);
        // 异步轮询搜歌结果（最多约 15 秒）
        Bukkit.getScheduler().runTaskLater(plugin, () -> pollMusicSearch(player, 0), 10L);
    }

    private void confirmMusicAddId(Player player, InputState state) {
        if (isBlankOrPlaceholder(state)) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7请输入歌曲 ID"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicMenuGUI().open(player));
            return;
        }
        String id = sanitize(state.currentInput()).trim();
        if (!plugin.getAllMusicHook().isAvailable()) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &cAllMusic 未加载"));
            return;
        }
        plugin.getAllMusicHook().addById(plugin, player, id);
        plugin.getMusicHistoryManager().recordManual(id, id, "", player.getName());
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已点歌 ID &f" + id));
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getMusicMenuGUI().open(player), 15L);
    }

    private void pollMusicSearch(Player player, int attempt) {
        if (!player.isOnline()) return;
        if (plugin.getAllMusicHook().hasSearch(player)) {
            plugin.getMusicSearchResultGUI().open(player);
            return;
        }
        if (attempt >= 30) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c搜索超时或无结果，请稍后重试"));
            plugin.getMusicMenuGUI().open(player);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> pollMusicSearch(player, attempt + 1), 10L);
    }

    private void confirmJobTitle(Player player, InputState state) {
        JobDraft d = jobDrafts.get(player.getUniqueId());
        if (d == null) return;
        if (isBlankOrPlaceholder(state)) {
            player.sendMessage(ColorUtil.colorize("&8[招工处] &7标题不能为空"));
            Bukkit.getScheduler().runTask(plugin, () -> openForJobPost(player, d.official));
            return;
        }
        d.title = sanitize(state.currentInput());
        open(player, Context.JOB_REWARD, String.valueOf((int) plugin.getJobBoardManager().minReward()),
                null, "委托报酬",
                "输入数字金额（将从余额托管）");
    }

    private void confirmJobReward(Player player, InputState state) {
        JobDraft d = jobDrafts.get(player.getUniqueId());
        if (d == null) return;
        double amount;
        try {
            amount = Double.parseDouble(sanitize(state.currentInput()));
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.colorize("&8[招工处] &7请输入有效数字"));
            open(player, Context.JOB_REWARD, String.valueOf((int) plugin.getJobBoardManager().minReward()),
                    null, "委托报酬", "输入数字金额");
            return;
        }
        d.reward = amount;
        open(player, Context.JOB_SLOTS, "1", null,
                "需要人数",
                "输入 1～" + plugin.getJobBoardManager().maxWorkersCap()
                        + "（多人时报酬均分）");
    }

    private void confirmJobSlots(Player player, InputState state) {
        JobDraft d = jobDrafts.get(player.getUniqueId());
        if (d == null) return;
        int slots;
        try {
            slots = Integer.parseInt(sanitize(state.currentInput()));
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.colorize("&8[招工处] &7请输入整数人数"));
            open(player, Context.JOB_SLOTS, "1", null, "需要人数", "输入人数");
            return;
        }
        int cap = plugin.getJobBoardManager().maxWorkersCap();
        if (slots < 1 || slots > cap) {
            player.sendMessage(ColorUtil.colorize("&8[招工处] &7人数需在 1～" + cap));
            open(player, Context.JOB_SLOTS, "1", null, "需要人数", "输入人数");
            return;
        }
        d.maxWorkers = slots;
        open(player, Context.JOB_DESC, "无", null,
                "委托说明", "输入建设要求说明；输入 - 跳过");
    }

    private void confirmJobDesc(Player player, InputState state) {
        JobDraft d = jobDrafts.remove(player.getUniqueId());
        if (d == null) return;
        String desc = sanitize(state.currentInput());
        if (desc.equals("-") || desc.equals("无") || isBlankOrPlaceholder(state)) desc = "";
        String err = plugin.getJobBoardManager().create(
                player, d.title, desc, d.reward, d.official, d.maxWorkers);
        if (err != null) {
            player.sendMessage(ColorUtil.colorize("&8[招工处] &c" + err));
        } else {
            player.sendMessage(ColorUtil.colorize("&8[招工处] &a已发布"
                    + (d.official ? "官方" : "个人") + "委托: &f" + d.title
                    + " &8(" + d.maxWorkers + " 人)"));
            Bukkit.broadcastMessage(ColorUtil.colorize(
                    "&8[招工处] &f" + player.getName() + " &7发布了"
                            + (d.official ? "&6官方" : "&e个人")
                            + "委托 &f" + d.title + " &7报酬 &a"
                            + plugin.getVaultHook().format(d.reward)
                            + " &8×" + d.maxWorkers + "人"));
        }
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getJobBoardGUI().open(player));
    }

    private void confirmJobEditTitle(Player player, InputState state) {
        JobDraft d = jobDrafts.remove(player.getUniqueId());
        if (d == null || d.jobId == null) return;
        if (isBlankOrPlaceholder(state)) {
            player.sendMessage(ColorUtil.colorize("&8[招工处] &7已取消"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getJobBoardGUI().open(player));
            return;
        }
        String err = plugin.getJobBoardManager().adminEdit(player, d.jobId,
                sanitize(state.currentInput()), null, null);
        player.sendMessage(ColorUtil.colorize(err == null
                ? "&8[招工处] &a已修改（会显示管理员修改标记）"
                : "&8[招工处] &c" + err));
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getJobBoardGUI().open(player));
    }

    private void confirmPayAmount(Player player, InputState state) {
        UUID targetUuid = state.metadata();
        if (targetUuid == null) return;

        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c找不到收款玩家。"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(sanitize(state.currentInput()));
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c请输入有效数字。"));
            Bukkit.getScheduler().runTask(plugin, () -> openForPayAmount(player, target));
            return;
        }

        Bukkit.getScheduler().runTask(plugin,
                () -> plugin.getPayConfirmGUI().open(player, target, amount));
    }

    private void confirmPayAllAmount(Player player, InputState state) {
        double amount;
        try {
            amount = Double.parseDouble(sanitize(state.currentInput()));
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c请输入有效数字。"));
            Bukkit.getScheduler().runTask(plugin, () -> openForPayAllAmount(player));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7金额必须大于 0"));
            Bukkit.getScheduler().runTask(plugin, () -> openForPayAllAmount(player));
            return;
        }
        Bukkit.getScheduler().runTask(plugin,
                () -> plugin.getPayConfirmGUI().openAll(player, amount));
    }

    private void confirmEstateRegister(Player player, InputState state) {
        if (isBlankOrPlaceholder(state)) {
            player.sendMessage(ColorUtil.colorize("&8[房产] &7已取消"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateToolsGUI().open(player));
            return;
        }
        var spec = com.etherstories.escore.managers.EstateManager.parseRegister(
                sanitize(state.currentInput()).split("\\s+"), false);
        if (spec == null) {
            player.sendMessage(ColorUtil.colorize("&8[房产] &c格式: 楼名 层 号 用途  &8例: 星港 商场 1 101 shop"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateToolsGUI().open(player));
            return;
        }
        String err = plugin.getEstateManager().register(player, spec.building(), spec.floor(), spec.room(),
                spec.kind(), spec.cat(), false, 0);
        if (err != null) {
            player.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateToolsGUI().open(player));
            return;
        }
        var u = plugin.getEstateManager().find(spec.building(), spec.floor(), spec.room());
        double fee = plugin.getEstateManager().registerFee(false, spec.cat(), spec.kind());
        player.sendMessage(ColorUtil.colorize("&8[房产] &a已登记 &f" + (u == null ? spec.building() : u.address())
                + " &8" + spec.kind().label + " · " + spec.cat().label
                + (fee > 0 ? "  &e注册费已扣" : " &7免费")));
        player.sendMessage(ColorUtil.colorize("&8没按规定注册的房子不追究，但出问题不保障。注册成房产才受保障。"));
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateToolsGUI().open(player));
    }

    private void confirmEstatePrice(Player player, InputState state) {
        String unitId = estatePriceUnit.remove(player.getUniqueId());
        if (isBlankOrPlaceholder(state) || unitId == null) {
            player.sendMessage(ColorUtil.colorize("&8[房产] &7已取消"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateMineGUI().openMine(player));
            return;
        }
        double price;
        try {
            price = Double.parseDouble(sanitize(state.currentInput()));
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.colorize("&8[房产] &c金额必须是数字"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateMineGUI().openMine(player));
            return;
        }
        var u = plugin.getEstateManager().byId(unitId);
        String err = plugin.getEstateManager().setSale(player, u, price, true);
        if (err != null) player.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
        else player.sendMessage(ColorUtil.colorize("&8[房产] &a已挂牌 &f"
                + (u == null ? "" : u.address()) + " &e"
                + (plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(price) : String.format("%.0f", price))));
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateMineGUI().openMine(player));
    }

    private void confirmHotelCreate(Player player, InputState state) {
        if (isBlankOrPlaceholder(state)) {
            player.sendMessage(ColorUtil.colorize("&8[酒店] &7已取消"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelListGUI().open(player));
            return;
        }
        String err = plugin.getHotelManager().create(player, sanitize(state.currentInput()));
        if (err != null) player.sendMessage(ColorUtil.colorize("&8[酒店] &c" + err));
        else player.sendMessage(ColorUtil.colorize("&8[酒店] &a已创建 &f" + sanitize(state.currentInput())
                + " &7站在房产里打开酒店页绑房"));
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelListGUI().open(player));
    }

    private void confirmHotelPrice(Player player, InputState state) {
        String unitId = hotelPriceUnit.remove(player.getUniqueId());
        var unit = plugin.getEstateManager().byId(unitId);
        var room = plugin.getHotelManager().roomOf(unit);
        var hotel = plugin.getHotelManager().hotelOf(unit);
        if (isBlankOrPlaceholder(state) || room == null) {
            player.sendMessage(ColorUtil.colorize("&8[酒店] &7已取消"));
            if (hotel != null) Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelDeskGUI().open(player, hotel));
            return;
        }
        try {
            double price = Double.parseDouble(sanitize(state.currentInput()));
            String err = plugin.getHotelManager().setPrice(player, room, price);
            player.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[酒店] &7标价 &f" + plugin.getHotelManager().money(price) : "&8[酒店] &c" + err));
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.colorize("&8[酒店] &c金额必须是数字"));
        }
        if (hotel != null) Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelDeskGUI().open(player, hotel));
    }

    private void confirmHotelTransfer(Player player, InputState state) {
        String hid = hotelContext.remove(player.getUniqueId());
        var hotel = plugin.getHotelManager().byId(hid);
        if (isBlankOrPlaceholder(state) || hotel == null) {
            player.sendMessage(ColorUtil.colorize("&8[酒店] &7已取消"));
            if (hotel != null) {
                HotelManager.Hotel open = hotel;
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelDeskGUI().open(player, open));
            }
            return;
        }
        OfflinePlayer to = Bukkit.getOfflinePlayer(sanitize(state.currentInput()));
        String err = plugin.getHotelManager().transfer(player, hotel, to);
        player.sendMessage(ColorUtil.colorize(err == null
                ? "&8[酒店] &a已转让给 &f" + (to.getName() == null ? sanitize(state.currentInput()) : to.getName())
                : "&8[酒店] &c" + err));
        HotelManager.Hotel after = plugin.getHotelManager().byId(hid);
        if (after != null) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelDeskGUI().open(player, after));
        }
    }

    private void confirmHotelMgr(Player player, InputState state) {
        String hid = hotelContext.remove(player.getUniqueId());
        var hotel = plugin.getHotelManager().byId(hid);
        if (isBlankOrPlaceholder(state) || hotel == null) {
            player.sendMessage(ColorUtil.colorize("&8[酒店] &7已取消"));
            if (hotel != null) Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelDeskGUI().open(player, hotel));
            return;
        }
        String[] parts = sanitize(state.currentInput()).split("\\s+");
        if (parts.length < 2) {
            player.sendMessage(ColorUtil.colorize("&8[酒店] &c用法: add 玩家  或  remove 玩家"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelDeskGUI().open(player, hotel));
            return;
        }
        OfflinePlayer who = Bukkit.getOfflinePlayer(parts[1]);
        String err;
        if (parts[0].equalsIgnoreCase("add")) err = plugin.getHotelManager().addManager(player, hotel, who);
        else if (parts[0].equalsIgnoreCase("remove") || parts[0].equalsIgnoreCase("rm"))
            err = plugin.getHotelManager().removeManager(player, hotel, who.getUniqueId());
        else err = "用法: add 玩家  或  remove 玩家";
        player.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &a已更新管理者" : "&8[酒店] &c" + err));
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelDeskGUI().open(player, hotel));
    }

    private void confirmHotelDelete(Player player, InputState state) {
        String hid = hotelContext.remove(player.getUniqueId());
        var hotel = plugin.getHotelManager().byId(hid);
        if (isBlankOrPlaceholder(state) || hotel == null) {
            player.sendMessage(ColorUtil.colorize("&8[酒店] &7已取消"));
            if (hotel != null) Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelDeskGUI().open(player, hotel));
            else Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelListGUI().open(player));
            return;
        }
        String typed = sanitize(state.currentInput());
        if (!typed.equalsIgnoreCase(hotel.name) && !typed.equalsIgnoreCase(hotel.id)) {
            player.sendMessage(ColorUtil.colorize("&8[酒店] &c名称不对，要输入 &f" + hotel.name));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelDeskGUI().open(player, hotel));
            return;
        }
        String name = hotel.name;
        String err = plugin.getHotelManager().delete(player, hotel);
        player.sendMessage(ColorUtil.colorize(err == null
                ? "&8[酒店] &c已删除 &f" + name + " &7房间已解绑"
                : "&8[酒店] &c" + err));
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelListGUI().open(player));
    }

    private void confirmGrant(Player player, InputState state) {
        String kind = grantKind.remove(player.getUniqueId());
        if (isBlankOrPlaceholder(state)) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已取消"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminGrantGUI().open(player));
            return;
        }
        String[] parts = sanitize(state.currentInput()).split("\\s+");
        if (parts.length < 2) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c用法: 玩家 数量"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminGrantGUI().open(player));
            return;
        }
        OfflinePlayer t = Bukkit.getOfflinePlayer(parts[0]);
        if (!t.hasPlayedBefore() && !t.isOnline()) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c找不到玩家（需进过服）"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminGrantGUI().open(player));
            return;
        }
        int n;
        try { n = Integer.parseInt(parts[1]); }
        catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &c数量无效"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminGrantGUI().open(player));
            return;
        }
        String name = t.getName() != null ? t.getName() : parts[0];
        if ("lucky".equals(kind)) {
            plugin.getLuckyBlockManager().giveTickets(t.getUniqueId(), n);
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已给 &f" + name + " &7额外抽奖 &a+" + n));
            if (t.isOnline() && t.getPlayer() != null)
                t.getPlayer().sendMessage(ColorUtil.colorize("&8[ECOS] &a获得 "
                        + plugin.getLuckyBlockManager().displayName() + " 额外次数 &f+" + n));
        } else {
            plugin.getCheckInManager().giveMakeupTickets(t.getUniqueId(), n);
            player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已给 &f" + name + " &7补签券 &e" + n));
            if (t.isOnline() && t.getPlayer() != null)
                t.getPlayer().sendMessage(ColorUtil.colorize("&8[ECOS] &a获得补签券 x" + n));
        }
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminGrantGUI().open(player));
    }

    private void confirmSkillShopPrice(Player player, InputState state) {
        String key = skillShopKey.remove(player.getUniqueId());
        var skill = com.etherstories.escore.weapons.SkillType.fromKey(key);
        if (isBlankOrPlaceholder(state) || skill == null) {
            player.sendMessage(ColorUtil.colorize("&8[技能商店] &7已取消"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getSkillShopGUI().openAdmin(player));
            return;
        }
        double price;
        try {
            price = Double.parseDouble(sanitize(state.currentInput()));
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.colorize("&8[技能商店] &c金额必须是数字"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getSkillShopGUI().openAdmin(player));
            return;
        }
        if (price < 0) {
            player.sendMessage(ColorUtil.colorize("&8[技能商店] &c价格不能为负"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getSkillShopGUI().openAdmin(player));
            return;
        }
        plugin.getSkillShopManager().setPrice(skill, price);
        plugin.getAuditLogManager().log(player, "SKILL_SHOP_PRICE",
                skill.configKey + "=" + price);
        player.sendMessage(ColorUtil.colorize("&8[技能商店] &7" + skill.displayName()
                + " &7定价 &f" + (plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(price) : String.format("%.0f", price))));
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getSkillShopGUI().openAdmin(player));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void open(Player player, Context ctx, String placeholder, UUID meta,
                      String title, String loreHint) {
        player.closeInventory();
        pending.put(player.getUniqueId(), new InputState(ctx, "", meta, placeholder));

        player.sendMessage(ColorUtil.colorize("&8[ECOS] &f" + title + " &8· &7" + loreHint));
        boolean sample = placeholder != null && !placeholder.isEmpty() && !placeholder.contains("输入");
        if (sample)
            player.sendMessage(ColorUtil.colorize("&8示例 &f" + placeholder + "  &8·  &fcancel &8取消"));
        else
            player.sendMessage(ColorUtil.colorize("&8聊天输入，&fcancel &8取消"));
    }
}
