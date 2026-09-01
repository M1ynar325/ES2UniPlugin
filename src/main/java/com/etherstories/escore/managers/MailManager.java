package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MailManager {

    public record Mail(UUID senderUuid, String senderName,
                       String content, String date, boolean read) {}

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<Mail>> box = new HashMap<>();
    private final Map<UUID, UUID>       pendingTo = new HashMap<>(); // writer → recipient
    private final File                  dataFile;

    public MailManager(ES2UniPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "mail.yml");
        load();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void load() {
        if (!dataFile.exists()) return;
        box.clear();
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (!cfg.isConfigurationSection("mail")) return;

        for (String key : cfg.getConfigurationSection("mail").getKeys(false)) {
            UUID recipient = UUID.fromString(key);
            List<Mail> mails = new ArrayList<>();
            List<?> list = cfg.getList("mail." + key, Collections.emptyList());
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> raw)) continue;
                @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) raw;
                mails.add(new Mail(
                        UUID.fromString((String) m.get("sender")),
                        (String) m.getOrDefault("sender-name", "?"),
                        (String) m.getOrDefault("content", ""),
                        (String) m.getOrDefault("date", ""),
                        Boolean.TRUE.equals(m.get("read"))
                ));
            }
            box.put(recipient, mails);
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, List<Mail>> e : box.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Mail mail : e.getValue()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("sender",      mail.senderUuid().toString());
                m.put("sender-name", mail.senderName());
                m.put("date",        mail.date());
                m.put("read",        mail.read());
                m.put("content",     mail.content());
                list.add(m);
            }
            cfg.set("mail." + e.getKey(), list);
        }
        try { cfg.save(dataFile); }
        catch (IOException ex) { plugin.getLogger().warning("mail.yml 保存失败: " + ex.getMessage()); }
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    /** Called from PlayerEditBookEvent after the player signs. */
    public void deliver(UUID senderUuid, String senderName, UUID recipientUuid, String content) {
        String date = LocalDateTime.now().format(FMT);
        Mail mail = new Mail(senderUuid, senderName, content, date, false);
        box.computeIfAbsent(recipientUuid, k -> new ArrayList<>()).add(mail);
        save();

        // Notify if online
        Player target = Bukkit.getPlayer(recipientUuid);
        if (target != null) {
            target.sendMessage("\u00a78[ECOS] \u00a77\u00a7f" + senderName + " \u00a77给你留了一条消息");
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
        }
    }

    // ── Pending book flow ─────────────────────────────────────────────────────

    public void setPending(UUID writer, UUID recipient) {
        pendingTo.put(writer, recipient);
    }

    /** 发书与笔，签名后投递给 recipient（可离线）。背包满则不挂 pending。 */
    public boolean giveDraftBook(Player writer, OfflinePlayer recipient) {
        if (writer == null || recipient == null) return false;
        UUID to = recipient.getUniqueId();
        if (to.equals(writer.getUniqueId())) {
            writer.sendMessage(ColorUtil.colorize("&8[ECOS] &7不能给自己写信"));
            return false;
        }
        if (!recipient.hasPlayedBefore() && !recipient.isOnline()) {
            writer.sendMessage(ColorUtil.colorize("&8[ECOS] &7这个玩家没进过服"));
            return false;
        }
        int empty = writer.getInventory().firstEmpty();
        if (empty == -1) {
            writer.sendMessage(ColorUtil.colorize("&8[ECOS] &7背包已满"));
            return false;
        }
        setPending(writer.getUniqueId(), to);
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta bm = (BookMeta) book.getItemMeta();
        if (bm != null) {
            bm.setTitle("draft");
            bm.setAuthor(writer.getName());
            book.setItemMeta(bm);
        }
        writer.getInventory().setItem(empty, book);
        String name = recipient.getName() == null ? "玩家" : recipient.getName();
        writer.sendMessage(ColorUtil.colorize("&8[ECOS] &7书已放入背包，写完签名发给 &f" + name
                + (recipient.isOnline() ? "" : " &8(离线)")));
        return true;
    }

    public UUID consumePending(UUID writer) {
        return pendingTo.remove(writer);
    }

    public boolean hasPending(UUID writer) {
        return pendingTo.containsKey(writer);
    }

    // ── Mailbox ───────────────────────────────────────────────────────────────

    public List<Mail> getMails(UUID player) {
        return Collections.unmodifiableList(box.getOrDefault(player, Collections.emptyList()));
    }

    public int unreadCount(UUID player) {
        return (int) getMails(player).stream().filter(m -> !m.read()).count();
    }

    /** Returns a new list with the mail at index marked as read. */
    public void markRead(UUID player, int index) {
        List<Mail> mails = box.get(player);
        if (mails == null || index < 0 || index >= mails.size()) return;
        Mail old = mails.get(index);
        if (old.read()) return;
        mails.set(index, new Mail(old.senderUuid(), old.senderName(),
                old.content(), old.date(), true));
        save();
    }

    public void delete(UUID player, int index) {
        List<Mail> mails = box.get(player);
        if (mails == null || index < 0 || index >= mails.size()) return;
        mails.remove(index);
        save();
    }
}
