package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.MusicPlaylistManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

/** 管理员预设歌单 */
public class MusicPlaylistGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l预设歌单");

    public static final int SLOT_STOP = 45;
    public static final int SLOT_STATUS = 49;
    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<String>> slotKeys = new HashMap<>();

    public MusicPlaylistGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.RED_STAINED_GLASS_PANE));

        List<MusicPlaylistManager.Preset> presets = plugin.getMusicPlaylistManager().listPresets();
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < Math.min(presets.size(), 45); i++) {
            MusicPlaylistManager.Preset p = presets.get(i);
            keys.add(p.key());
            inv.setItem(i, ECOSTerminalGUI.item(Material.JUKEBOX,
                    "&f" + p.displayName(),
                    List.of(
                            "&7" + (p.description().isBlank() ? "管理员氛围歌单" : p.description()),
                            "&7歌曲数: &f" + p.songIds().size(),
                            "",
                            "&a左键: 顺序播放",
                            "&e右键: 打乱后播放",
                            "&8队列将空时自动续点下一首"
                    )));
        }
        slotKeys.put(player.getUniqueId(), keys);

        if (presets.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7尚未配置预设",
                    List.of("&7在 config.yml → music.presets 添加",
                            "&7songs: 填网易云等歌曲 ID 列表")));
        }

        var pm = plugin.getMusicPlaylistManager();
        inv.setItem(SLOT_STATUS, ECOSTerminalGUI.item(Material.CLOCK,
                pm.isRunning() ? "&a运行中" : "&7空闲",
                List.of("&7" + pm.statusLine())));
        inv.setItem(SLOT_STOP, ECOSTerminalGUI.item(Material.REDSTONE, "&c停止预设续播",
                List.of("&7不会清空 AllMusic 当前队列", "&c▸ 点击停止自动续点")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回点歌", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    public String getKey(Player player, int slot) {
        List<String> keys = slotKeys.get(player.getUniqueId());
        if (keys == null || slot < 0 || slot >= keys.size()) return null;
        return keys.get(slot);
    }

    public void cleanup(Player player) {
        slotKeys.remove(player.getUniqueId());
    }
}
