package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.MailManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MailboxGUI {

    public static final String TITLE = "&8邮箱";

    private static final int SIZE       = 54;
    public  static final int SLOT_CLOSE = 53;
    public  static final int SLOT_BACK  = 45;
    public  static final int SLOT_SEND  = 49;

    private static final Material BG     = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material UNREAD = Material.WRITTEN_BOOK;
    private static final Material READ   = Material.BOOK;

    private final ES2UniPlugin plugin;
    private final Map<UUID, Integer> offsets = new HashMap<>(); // for future pagination

    public MailboxGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        offsets.put(player.getUniqueId(), 0);
        render(player);
    }

    private void render(Player player) {
        MailManager mm = plugin.getMailManager();
        List<MailManager.Mail> mails = new ArrayList<>(mm.getMails(player.getUniqueId()));
        Collections.reverse(mails); // newest first

        Inventory inv = EcosHolder.of("mail", SIZE, ColorUtil.colorize(TITLE));
        ItemStack bg = ECOSTerminalGUI.bg(BG);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, bg);

        int mailSlots = SIZE - 9; // 45 slots for mails
        for (int i = 0; i < Math.min(mails.size(), mailSlots); i++) {
            MailManager.Mail mail = mails.get(i);
            boolean isFriend = plugin.getFriendManager()
                    .areFriends(player.getUniqueId(), mail.senderUuid());

            ItemStack book = new ItemStack(mail.read() ? READ : UNREAD);
            ItemMeta meta = book.getItemMeta();
            if (meta != null) {
                String friendTag = isFriend ? " &8[好友]" : "";
                meta.setDisplayName(ColorUtil.colorize(
                        (mail.read() ? "&8" : "&f") + mail.senderName() + friendTag));

                // Content preview (first 40 chars)
                String preview = mail.content().replace("\n", " ");
                if (preview.length() > 40) preview = preview.substring(0, 40) + "...";

                meta.setLore(List.of(
                        ColorUtil.colorize("&8" + mail.date()),
                        ColorUtil.colorize("&7" + preview),
                        ColorUtil.colorize(""),
                        ColorUtil.colorize("&8左键: 阅读   右键: 删除")
                ));
                book.setItemMeta(meta);
            }
            inv.setItem(i, book);
        }

        inv.setItem(SLOT_SEND,  ECOSTerminalGUI.item(Material.WRITABLE_BOOK, "&a写邮件 / 发送",
                List.of("&7选择在线玩家", "&7用书与笔写内容后签名发送",
                        "&8也可: /ecos mail send <玩家> <内容>")));
        inv.setItem(SLOT_BACK,  ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&7关闭", null));

        EcosHolder.open(player, inv);
    }

    /** Open the mail for reading as a book. index = slot (reversed list). */
    public void openMail(Player player, int slot) {
        MailManager mm = plugin.getMailManager();
        List<MailManager.Mail> mails = new ArrayList<>(mm.getMails(player.getUniqueId()));
        Collections.reverse(mails);
        if (slot < 0 || slot >= mails.size()) return;

        mm.markRead(player.getUniqueId(), mails.size() - 1 - slot); // original index

        MailManager.Mail mail = mails.get(slot);
        boolean isFriend = plugin.getFriendManager()
                .areFriends(player.getUniqueId(), mail.senderUuid());
        String friendTag = isFriend ? " [好友]" : "";

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bm = (BookMeta) book.getItemMeta();
        if (bm != null) {
            bm.setAuthor(mail.senderName() + friendTag);
            bm.setTitle(mail.date());
            // Split content into pages (max 256 chars each)
            String content = mail.content();
            List<String> pages = new ArrayList<>();
            while (content.length() > 256) {
                pages.add(content.substring(0, 256));
                content = content.substring(256);
            }
            if (!content.isEmpty()) pages.add(content);
            bm.setPages(pages);
            book.setItemMeta(bm);
        }
        player.openBook(book);
        // Reopen GUI after slight delay (opening book closes inventory)
        Bukkit.getScheduler().runTaskLater(plugin, () -> render(player), 2L);
    }

    public void deleteMail(Player player, int slot) {
        MailManager mm = plugin.getMailManager();
        List<MailManager.Mail> mails = new ArrayList<>(mm.getMails(player.getUniqueId()));
        int realIndex = mails.size() - 1 - slot;
        mm.delete(player.getUniqueId(), realIndex);
        render(player);
    }

    public boolean isMailSlot(int slot) {
        return slot >= 0 && slot < SIZE - 9;
    }
}
