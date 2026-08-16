package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.hooks.AllMusicHook;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/** AllMusic 搜歌结果列表 — 点击物品点歌 */
public class MusicSearchResultGUI {

    public static final String TITLE = ColorUtil.colorize("&a&l搜歌结果");

    public static final int PREV_SLOT   = 45;
    public static final int INFO_SLOT   = 49;
    public static final int NEXT_SLOT   = 53;
    public static final int BACK_SLOT   = 48;
    public static final int RETRY_SLOT  = 50;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<AllMusicHook.SongEntry>> cache = new HashMap<>();

    public MusicSearchResultGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        AllMusicHook hook = plugin.getAllMusicHook();
        List<AllMusicHook.SongEntry> songs = hook.getSearchPage(player);
        if (songs == null) songs = List.of();
        cache.put(player.getUniqueId(), songs);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        ItemStack bg = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        Material[] discs = {
                Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS,
                Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR,
                Material.MUSIC_DISC_MALL, Material.MUSIC_DISC_MELLOHI,
                Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD,
                Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_WAIT
        };

        for (int i = 0; i < Math.min(songs.size(), 45); i++) {
            AllMusicHook.SongEntry s = songs.get(i);
            inv.setItem(i, item(discs[i % discs.length],
                    "&f" + s.selectIndex() + ". &e" + s.name(),
                    List.of(
                            " &7歌手: &f" + s.author(),
                            " &7专辑: &f" + s.album(),
                            " &8ID: " + s.id(),
                            "",
                            "&a▸ 点击点歌"
                    )));
        }

        int page = hook.currentPage(player) + 1;
        inv.setItem(INFO_SLOT, item(Material.PAPER, "&7第 &f" + page + " &7页",
                List.of("&7共本页 &f" + songs.size() + " &7首",
                        songs.isEmpty() ? "&c暂无结果，可重新搜索" : "&a点击唱片点歌")));

        if (hook.canPrev(player))
            inv.setItem(PREV_SLOT, item(Material.ARROW, "&e上一页", List.of("&7点击翻页")));
        if (hook.canNext(player))
            inv.setItem(NEXT_SLOT, item(Material.ARROW, "&e下一页", List.of("&7点击翻页")));

        inv.setItem(BACK_SLOT, item(Material.OAK_DOOR, "&7返回点歌菜单", List.of()));
        inv.setItem(RETRY_SLOT, item(Material.NAME_TAG, "&a重新搜索", List.of("&7再输入歌名")));

        player.openInventory(inv);
    }

    public AllMusicHook.SongEntry getAt(Player player, int slot) {
        List<AllMusicHook.SongEntry> list = cache.get(player.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size() || slot >= 45) return null;
        return list.get(slot);
    }

    public void cleanup(Player player) {
        cache.remove(player.getUniqueId());
    }

    private static ItemStack pane(Material m) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); i.setItemMeta(meta); }
        return i;
    }

    private static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            List<String> colored = new ArrayList<>();
            for (String l : lore) colored.add(ColorUtil.colorize(l));
            meta.setLore(colored);
            i.setItemMeta(meta);
        }
        return i;
    }
}
