package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 歌曲详情（书本）+ 可选收藏/点播提示由调用方处理。
 */
public class MusicDetailGUI {

    private final ES2UniPlugin plugin;

    public MusicDetailGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void openBook(Player player, String id, String name, String author, String album, String extra) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bm = (BookMeta) book.getItemMeta();
        if (bm != null) {
            bm.setTitle(name == null || name.isBlank() ? "歌曲详情" : trim(name, 16));
            bm.setAuthor("ECOS Music");
            List<String> pages = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            sb.append("§0§l").append(nullSafe(name)).append("\n\n");
            sb.append("§8歌手 §0").append(nullSafe(author)).append("\n");
            sb.append("§8专辑 §0").append(nullSafe(album)).append("\n");
            if (id != null && !id.isBlank()) sb.append("§8ID §0").append(id).append("\n");
            if (extra != null && !extra.isBlank()) sb.append("\n§7").append(extra);
            boolean fav = id != null && !id.isBlank()
                    && plugin.getMusicFavoritesManager().has(player.getUniqueId(), id);
            sb.append("\n\n§8收藏: ").append(fav ? "§6已收藏" : "§7未收藏");
            sb.append("\n§8关书后可回列表继续操作");
            String content = sb.toString();
            while (content.length() > 240) {
                pages.add(content.substring(0, 240));
                content = content.substring(240);
            }
            if (!content.isEmpty()) pages.add(content);
            bm.setPages(pages);
            book.setItemMeta(bm);
        }
        player.closeInventory();
        player.openBook(book);
    }

    private static String nullSafe(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String trim(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n);
    }
}
