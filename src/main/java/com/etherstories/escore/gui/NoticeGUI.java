package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.NoticeManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoticeGUI {

    public static final String TITLE      = "&8公告板";
    public static final int    CLOSE_SLOT = 53;
    public static final int    BACK_SLOT  = 45;
    public static final int    POST_SLOT  = 49; // admin only
    private static final int   SIZE       = 54;
    private static final int   LIST_SIZE  = SIZE - 9;
    private static final Material BG      = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin plugin;

    public NoticeGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = EcosHolder.of("notice", SIZE, ColorUtil.colorize(TITLE));

        ItemStack bg = ECOSTerminalGUI.bg(BG);
        for (int i = LIST_SIZE; i < SIZE; i++) inv.setItem(i, bg);

        List<NoticeManager.Notice> notices = new ArrayList<>(plugin.getNoticeManager().getNotices());
        Collections.reverse(notices);

        for (int i = 0; i < Math.min(notices.size(), 27); i++) {
            NoticeManager.Notice n = notices.get(i);
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&8" + n.date() + "  &7by " + n.author()));
            lore.addAll(wrapLore("&7", n.content(), 30, 4));
            lore.add(ColorUtil.colorize(""));
            lore.add(ColorUtil.colorize("&a左键: 查看全文"));
            if (player.hasPermission("es2uni.admin")) {
                lore.add(ColorUtil.colorize("&c右键: 删除"));
            }
            inv.setItem(i, ECOSTerminalGUI.item(Material.PAPER, "&f公告 #" + (i + 1), lore));
        }

        List<String> history = plugin.getNoticeManager().getBroadcastHistory();
        List<String> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        for (int i = 0; i < Math.min(reversed.size(), 18); i++) {
            inv.setItem(27 + i, ECOSTerminalGUI.item(Material.BELL,
                    "&7广播记录",
                    List.of(ColorUtil.colorize("&8" + reversed.get(i)))));
        }

        if (notices.isEmpty() && history.isEmpty())
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7暂无公告", null));

        if (player.hasPermission("es2uni.admin"))
            inv.setItem(POST_SLOT, ECOSTerminalGUI.item(Material.WRITABLE_BOOK,
                    "&a发布公告",
                    List.of("&7点击后获得书与笔",
                            "&7写完后签名即可发布",
                            "&8支持多页长文")));

        inv.setItem(BACK_SLOT,  ECOSTerminalGUI.item(Material.ARROW,   "&7返回终端", null));
        inv.setItem(CLOSE_SLOT, ECOSTerminalGUI.item(Material.BARRIER, "&7关闭",     null));
        EcosHolder.open(player, inv);
    }

    /** 打开书本查看全文 */
    public void openDetail(Player player, int displaySlot) {
        int idx = getNoticeIndex(player, displaySlot);
        if (idx < 0) return;
        NoticeManager.Notice n = plugin.getNoticeManager().getNotices().get(idx);

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bm = (BookMeta) book.getItemMeta();
        if (bm != null) {
            bm.setAuthor(n.author());
            bm.setTitle("公告 " + n.date());
            String content = "§0" + n.date() + " · " + n.author() + "\n\n§0" + n.content();
            List<String> pages = new ArrayList<>();
            while (content.length() > 240) {
                pages.add(content.substring(0, 240));
                content = content.substring(240);
            }
            if (!content.isEmpty()) pages.add(content);
            if (pages.isEmpty()) pages.add("§7（空）");
            bm.setPages(pages);
            book.setItemMeta(bm);
        }
        player.closeInventory();
        player.openBook(book);
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 40L);
    }

    public int getNoticeIndex(Player player, int slot) {
        List<NoticeManager.Notice> notices = plugin.getNoticeManager().getNotices();
        int originalIndex = notices.size() - 1 - slot;
        return (originalIndex >= 0 && originalIndex < notices.size()) ? originalIndex : -1;
    }

    public boolean isNoticeSlot(int slot) {
        if (slot < 0 || slot >= 27) return false;
        List<NoticeManager.Notice> notices = plugin.getNoticeManager().getNotices();
        int originalIndex = notices.size() - 1 - slot;
        return originalIndex >= 0 && originalIndex < notices.size();
    }

    private static List<String> wrapLore(String color, String text, int width, int maxLines) {
        List<String> out = new ArrayList<>();
        if (text == null) text = "";
        String flat = text.replace('\n', ' ');
        int i = 0;
        int lines = 0;
        while (i < flat.length() && lines < maxLines) {
            int end = Math.min(i + width, flat.length());
            String part = flat.substring(i, end);
            if (end < flat.length() && lines == maxLines - 1) part = part + "…";
            out.add(ColorUtil.colorize(color + part));
            i = end;
            lines++;
        }
        if (out.isEmpty()) out.add(ColorUtil.colorize(color + "（无内容）"));
        return out;
    }
}
