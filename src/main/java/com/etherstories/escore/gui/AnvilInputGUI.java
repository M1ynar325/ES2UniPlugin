package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnvilInputGUI {

    public enum Context {
        CREATE_EVENT_NAME, PAY_AMOUNT, ADD_FRIEND_NAME, SET_STATUS, ADD_WAYPOINT, CREATE_KIT_NAME,
        MUSIC_SEARCH, MUSIC_ADD_ID,
        JOB_TITLE, JOB_REWARD, JOB_SLOTS, JOB_DESC, JOB_EDIT_TITLE
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
        open(player, Context.SET_STATUS, current != null ? current : "输入签名",
                null, "个人签名",
                "输入签名内容（最多 " + plugin.getStatusManager().maxLength() + " 字），留空可清签名请输入 -");
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

    /** Admin: create kit name. */
    public void openForKitName(Player player) {
        open(player, Context.CREATE_KIT_NAME, "套件名称", null,
                "新建 Kit", "输入套件 ID（字母/数字/_/-）");
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
        }
    }

    public void cancel(Player player) {
        pending.remove(player.getUniqueId());
        jobDrafts.remove(player.getUniqueId());
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
        if (text.equals(sanitize(state.placeholder())) || text.equals("输入签名") || text.equals("-")) text = "";
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
            player.sendMessage(ColorUtil.colorize("&c[ES2] 活动名称不能为空。"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminEventGUI().open(player));
            return;
        }
        String name = sanitize(state.currentInput());
        plugin.getEventManager().createEvent(player, name, "", 0);
        player.sendMessage(ColorUtil.colorize("&a[ES2] 活动 &f" + name + " &a已创建！"));
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
            player.sendMessage(ColorUtil.colorize("&c[ES2] 找不到收款玩家。"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEcosTerminalGUI().open(player));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(sanitize(state.currentInput()));
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.colorize("&c[ES2] 请输入有效数字。"));
            Bukkit.getScheduler().runTask(plugin, () -> openForPayAmount(player, target));
            return;
        }

        Bukkit.getScheduler().runTask(plugin,
                () -> plugin.getPayConfirmGUI().open(player, target, amount));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void open(Player player, Context ctx, String placeholder, UUID meta,
                      String title, String loreHint) {
        player.closeInventory();
        pending.put(player.getUniqueId(), new InputState(ctx, "", meta, placeholder));

        player.sendMessage(ColorUtil.colorize("&8──────── &f" + title + " &8────────"));
        player.sendMessage(ColorUtil.colorize("&7" + loreHint));
        player.sendMessage(ColorUtil.colorize("&7默认/示例: &f" + placeholder));
        player.sendMessage(ColorUtil.colorize("&a▸ 直接在聊天栏输入内容并回车确认"));
        player.sendMessage(ColorUtil.colorize("&8▸ 输入 &fcancel &8或 &f取消 &8退出"));
    }
}
