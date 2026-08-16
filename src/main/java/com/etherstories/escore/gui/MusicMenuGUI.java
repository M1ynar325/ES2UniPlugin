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

import java.util.ArrayList;
import java.util.List;

/** AllMusic 点歌主菜单 — 覆盖常用玩家指令 */
public class MusicMenuGUI {

    public static final String TITLE = ColorUtil.colorize("&d&lECOS &8· &f点歌");

    public static final int SLOT_NOW      = 13;
    public static final int SLOT_SEARCH   = 28;
    public static final int SLOT_ADD_ID   = 29;
    public static final int SLOT_STOP     = 30;
    public static final int SLOT_QUEUE    = 31;
    public static final int SLOT_HISTORY  = 32;
    public static final int SLOT_VOTE     = 33;
    public static final int SLOT_MUTE     = 34;
    public static final int SLOT_JOIN     = 35;
    public static final int SLOT_CANCEL   = 39;
    public static final int SLOT_HELP     = 40;
    public static final int SLOT_FAVORITES = 37;
    public static final int SLOT_PRESETS  = 38; // admin
    public static final int SLOT_BACK     = 48;
    public static final int SLOT_CLOSE    = 50;

    private final ES2UniPlugin plugin;

    public MusicMenuGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        plugin.getAllMusicHook().ensureHooked(plugin.getLogger());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        ItemStack bg = pane(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        AllMusicHook hook = plugin.getAllMusicHook();
        if (!hook.isAvailable()) {
            inv.setItem(SLOT_NOW, item(Material.BARRIER, "&cAllMusic 未加载",
                    List.of("&7请确认服务端已安装 AllMusic",
                            "&7且客户端已装 AllMusic Client")));
            inv.setItem(SLOT_BACK, item(Material.ARROW, "&7返回终端", List.of()));
            inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&c关闭", List.of()));
            player.openInventory(inv);
            return;
        }

        AllMusicHook.NowPlaying now = hook.getNowPlaying();
        if (now == null) {
            inv.setItem(SLOT_NOW, item(Material.JUKEBOX, "&e正在播放",
                    List.of("&7无法读取播放信息", "", "&7▸ 点击刷新")));
        } else {
            inv.setItem(SLOT_NOW, item(Material.JUKEBOX, "&e正在播放",
                    List.of(
                            " &f" + now.name(),
                            " &7歌手: &f" + now.author(),
                            " &7专辑: &f" + now.album(),
                            " &7点歌: &f" + now.caller(),
                            " &7队列: &f" + now.queueSize() + " 首",
                            "",
                            "&7▸ 点击刷新"
                    )));
        }

        inv.setItem(SLOT_SEARCH, item(Material.NAME_TAG, "&a搜歌",
                List.of("&7输入歌名 → 结果里点选",
                        "&7/ecos music search <歌名>",
                        "",
                        "&a▸ 点击")));

        inv.setItem(SLOT_ADD_ID, item(Material.PAPER, "&a用 ID 点歌",
                List.of("&7直接输入网易云等歌曲 ID",
                        "&7/ecos music add <ID>",
                        "",
                        "&a▸ 点击输入")));

        inv.setItem(SLOT_STOP, item(Material.REDSTONE, "&c停止（仅自己）",
                List.of("&7/music stop", "", "&c▸ 点击")));

        inv.setItem(SLOT_QUEUE, item(Material.BOOK, "&b播放队列",
                List.of("&7正在播 + 排队序列",
                        "&7/ecos music queue",
                        "",
                        "&b▸ 点击")));

        int hist = plugin.getMusicHistoryManager().size();
        inv.setItem(SLOT_HISTORY, item(Material.WRITABLE_BOOK, "&e最近播放",
                List.of("&7会话内存，可点详情/收藏",
                        " &7条数: &f" + hist,
                        "",
                        "&e▸ 打开 GUI")));

        int favs = plugin.getMusicFavoritesManager().size(player.getUniqueId());
        inv.setItem(SLOT_FAVORITES, item(Material.NETHER_STAR, "&6我的收藏",
                List.of(" &7已收藏: &f" + favs + " &8/ 60",
                        "&7可从最近播放右键加入",
                        "",
                        "&6▸ 打开")));

        if (player.hasPermission("es2uni.admin")) {
            var pm = plugin.getMusicPlaylistManager();
            inv.setItem(SLOT_PRESETS, item(Material.JUKEBOX, "&c预设歌单",
                    List.of("&7管理员氛围序列",
                            "&7顺序 / 打乱续播",
                            " &7状态: &f" + pm.statusLine(),
                            "",
                            "&c▸ 打开")));
        }

        inv.setItem(SLOT_VOTE, item(Material.BELL, "&6投票切歌",
                List.of("&7/music vote", "", "&6▸ 点击")));

        inv.setItem(SLOT_MUTE, item(Material.NOTE_BLOCK,
                plugin.getAllMusicHook().isMuted(player) ? "&c静音: 开（不听歌）" : "&a静音: 关（听歌中）",
                List.of("&7切换是否收听服务器点歌",
                        "&7等价终端「听歌开关」/ /music mute",
                        "&8重进服务器仍然有效",
                        "",
                        "&d▸ 点击切换")));

        inv.setItem(SLOT_JOIN, item(Material.ENDER_EYE, "&b重新加入播放",
                List.of("&7进服漏听时用",
                        "&7/music join",
                        "",
                        "&b▸ 点击")));

        inv.setItem(SLOT_CANCEL, item(Material.BARRIER, "&c取消自己点的歌",
                List.of("&7从队列移除你点的歌",
                        "&7/music cancel",
                        "",
                        "&c▸ 点击")));

        inv.setItem(SLOT_HELP, item(Material.KNOWLEDGE_BOOK, "&f指令帮助",
                List.of(
                        "&7/ecos music search <歌名>",
                        "&7/ecos music add <ID>",
                        "&7/ecos music queue / now / history",
                        "&7/ecos music vote / stop / mute / join",
                        "&7/ecos music cancel",
                        "&7/ecos music help"
                )));

        inv.setItem(SLOT_BACK, item(Material.ARROW, "&7返回终端", List.of()));
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&c关闭", List.of()));
        player.openInventory(inv);
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
