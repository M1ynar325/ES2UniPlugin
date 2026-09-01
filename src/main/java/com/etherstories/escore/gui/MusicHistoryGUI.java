package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.MusicHistoryManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

/** 最近播放（会话内存） */
public class MusicHistoryGUI {

    public static final String TITLE = ColorUtil.colorize("&e&l最近播放");

    public static final int SLOT_BACK = 45;
    public static final int SLOT_FAV = 49;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<MusicHistoryManager.Entry>> cache = new HashMap<>();

    public MusicHistoryGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = EcosHolder.of("music-history", 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.BLACK_STAINED_GLASS_PANE));

        List<MusicHistoryManager.Entry> list = new ArrayList<>(plugin.getMusicHistoryManager().recent());
        cache.put(player.getUniqueId(), list);

        for (int i = 0; i < Math.min(list.size(), 45); i++) {
            MusicHistoryManager.Entry e = list.get(i);
            boolean fav = e.id() != null && !e.id().isBlank()
                    && plugin.getMusicFavoritesManager().has(player.getUniqueId(), e.id());
            inv.setItem(i, ECOSTerminalGUI.item(
                    fav ? Material.GOLDEN_APPLE : Material.MUSIC_DISC_CAT,
                    "&f" + e.name(),
                    List.of(
                            "&7歌手: &f" + nullToEmpty(e.author()),
                            "&7点歌: &f" + nullToEmpty(e.caller()),
                            "&7时间: &f" + e.time(),
                            e.id() == null || e.id().isBlank() ? "&8ID: 未知" : "&7ID: &f" + e.id(),
                            fav ? "&6★ 已收藏" : "&8☆ 未收藏",
                            "",
                            "&a左键: 详情",
                            e.id() == null || e.id().isBlank() ? "&8无 ID 无法点播/收藏" : "&e右键: 收藏/取消",
                            e.id() == null || e.id().isBlank() ? "" : "&bShift+左键: 再点一次"
                    )));
        }
        if (list.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7暂无播放记录",
                    List.of("&7点歌后会自动出现在这里", "&8重启服务器会清空")));
        }

        inv.setItem(SLOT_FAV, ECOSTerminalGUI.item(Material.NETHER_STAR, "&6我的收藏",
                List.of("&7共 &f" + plugin.getMusicFavoritesManager().size(player.getUniqueId()) + " &7首",
                        "", "&6▸ 点击打开")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回点歌", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }

    public MusicHistoryManager.Entry getAt(Player player, int slot) {
        List<MusicHistoryManager.Entry> list = cache.get(player.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size()) return null;
        return list.get(slot);
    }

    public void cleanup(Player player) {
        cache.remove(player.getUniqueId());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
