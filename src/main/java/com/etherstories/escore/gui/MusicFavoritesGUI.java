package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.MusicFavoritesManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

/** 个人收藏歌单 */
public class MusicFavoritesGUI {

    public static final String TITLE = ColorUtil.colorize("&6&l我的收藏");

    public static final int SLOT_BACK = 45;
    public static final int SLOT_HISTORY = 49;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<MusicFavoritesManager.FavSong>> cache = new HashMap<>();

    public MusicFavoritesGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.BLACK_STAINED_GLASS_PANE));

        List<MusicFavoritesManager.FavSong> list = plugin.getMusicFavoritesManager().list(player.getUniqueId());
        cache.put(player.getUniqueId(), list);

        for (int i = 0; i < Math.min(list.size(), 45); i++) {
            MusicFavoritesManager.FavSong s = list.get(i);
            inv.setItem(i, ECOSTerminalGUI.item(Material.MUSIC_DISC_MALL,
                    "&6★ &f" + s.name(),
                    List.of(
                            "&7歌手: &f" + s.author(),
                            "&7专辑: &f" + s.album(),
                            "&7ID: &f" + s.id(),
                            "",
                            "&a左键: 详情",
                            "&bShift+左键: 点播",
                            "&c右键: 取消收藏"
                    )));
        }
        if (list.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7收藏为空",
                    List.of("&7在最近播放里右键歌曲可收藏")));
        }

        inv.setItem(SLOT_HISTORY, ECOSTerminalGUI.item(Material.WRITABLE_BOOK, "&e最近播放",
                List.of("&7▸ 点击打开")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回点歌", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    public MusicFavoritesManager.FavSong getAt(Player player, int slot) {
        List<MusicFavoritesManager.FavSong> list = cache.get(player.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size()) return null;
        return list.get(slot);
    }

    public void cleanup(Player player) {
        cache.remove(player.getUniqueId());
    }
}
