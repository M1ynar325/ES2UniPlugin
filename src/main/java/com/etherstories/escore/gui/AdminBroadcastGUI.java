package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.broadcast.BroadcastEntry;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员编辑定时广播消息列表（写入 config.yml）
 * Shift+左键：切换 每天 / 工作日 / 周末
 */
public class AdminBroadcastGUI {

    public static final String TITLE = ColorUtil.colorize("&b&l定时广播");

    public static final int SLOT_ADD = 45;
    public static final int SLOT_INTERVAL = 47;
    public static final int SLOT_ONCE = 49;
    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;

    public AdminBroadcastGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = EcosHolder.of("admin-bc", 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.BLACK_STAINED_GLASS_PANE));

        List<BroadcastEntry> entries = plugin.getConfigManager().getBroadcastEntries();
        for (int i = 0; i < Math.min(entries.size(), 45); i++) {
            BroadcastEntry e = entries.get(i);
            String preview = e.text.length() > 36 ? e.text.substring(0, 36) + "…" : e.text;
            boolean today = e.activeToday();
            inv.setItem(i, ECOSTerminalGUI.item(
                    today ? Material.PAPER : Material.MAP,
                    "&f#" + (i + 1) + " &8[" + e.daysLabel() + "]",
                    List.of(
                            "&7" + preview,
                            today ? "&a今日会播" : "&8今日跳过",
                            "",
                            "&a左键: 编辑内容",
                            "&eShift+左键: 切换星期(每天/工作日/周末)",
                            "&c右键: 删除"
                    )));
        }

        long interval = plugin.getConfigManager().getBroadcastInterval();
        inv.setItem(SLOT_ADD, ECOSTerminalGUI.item(Material.WRITABLE_BOOK,
                "&a添加消息", List.of("&7点击后在聊天输入内容", "&8默认每天播", "&8cancel 取消")));
        inv.setItem(SLOT_INTERVAL, ECOSTerminalGUI.item(Material.CLOCK,
                "&e间隔: &f" + interval + "s",
                List.of("&7左键 +30s  右键 -30s", "&8最短 30 秒", "", "&e改完会重启定时任务")));
        inv.setItem(SLOT_ONCE, ECOSTerminalGUI.item(Material.BELL,
                "&6立即广播一条", List.of("&7点击后聊天输入，立刻全服发送")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }

    public void saveMessages(List<String> messages) {
        plugin.getConfig().set("broadcast.messages", messages);
        plugin.saveConfig();
    }

    public void saveEntries(List<BroadcastEntry> entries) {
        List<String> raw = new ArrayList<>();
        for (BroadcastEntry e : entries) raw.add(e.serialize());
        saveMessages(raw);
    }

    public void cycleDays(int index) {
        List<BroadcastEntry> entries = new ArrayList<>(plugin.getConfigManager().getBroadcastEntries());
        if (index < 0 || index >= entries.size()) return;
        entries.set(index, entries.get(index).cyclePreset());
        saveEntries(entries);
    }

    public void setInterval(long seconds) {
        if (seconds < 30) seconds = 30;
        plugin.getConfig().set("broadcast.interval-seconds", seconds);
        plugin.saveConfig();
        plugin.reload();
    }
}
