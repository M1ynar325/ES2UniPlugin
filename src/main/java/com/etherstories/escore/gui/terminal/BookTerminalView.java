package com.etherstories.escore.gui.terminal;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.gui.EcosStyle;
import com.etherstories.escore.utils.ColorUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/** 书本。Bungee 写页。按钮多时自动分页，避免一页写爆。 */
public final class BookTerminalView implements TerminalView {
    private static final int BUTTONS_PER_PAGE = 12;
    /**
     * session.extra 里的键：该玩家上一次真正展示出来的渲染指纹。
     * 跟着 TerminalSession 走，关终端 / 退服即随会话一起丢弃，不会只增不减。
     */
    private static final String FINGERPRINT_KEY = "book.fp";
    private final ES2UniPlugin plugin;

    public BookTerminalView(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void open(Player player, TerminalSession session) {
        try {
            show(player, session, build(player, session));
        } catch (Throwable t) {
            plugin.getLogger().warning("写书失败，改聊天: " + t);
            new ChatTerminalView(plugin).open(player, session);
        }
    }

    /**
     * 刷新。先照常构建新书，再和玩家上一次真正看到的渲染指纹比：
     * 完全一致就直接不 openBook —— 重开也只会把页码打回第 1 页，纯属白折腾。
     */
    @Override
    public void refresh(Player player, TerminalSession session) {
        Rendered rendered;
        try {
            rendered = build(player, session);
        } catch (Throwable t) {
            plugin.getLogger().warning("写书失败，改聊天: " + t);
            new ChatTerminalView(plugin).open(player, session);
            return;
        }
        if (rendered.fingerprint().equals(session.get(FINGERPRINT_KEY))) return;
        show(player, session, rendered);
    }

    @Override
    public void close(Player player) {
        if (player.isOnline()) {
            try { player.closeInventory(); } catch (Throwable ignored) {}
        }
    }

    private void show(Player player, TerminalSession session, Rendered rendered) {
        // 先把指纹记下：同一 tick 内重复 refresh 不必再 openBook 一次。
        session.put(FINGERPRINT_KEY, rendered.fingerprint());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            try {
                player.openBook(rendered.book());
            } catch (Throwable t) {
                // 没真正展示成功，清掉指纹，别让下一次刷新误判成"已展示过"。
                session.put(FINGERPRINT_KEY, null);
                plugin.getLogger().warning("openBook 失败，改聊天: " + t);
                new ChatTerminalView(plugin).open(player, session);
            }
        });
    }

    private Rendered build(Player player, TerminalSession session) {
        TerminalHub hub = plugin.getTerminalHub();
        TerminalPage page = hub.page(session.pageId());
        String label = page == null ? "?" : page.label();

        List<BaseComponent> head = new ArrayList<>();
        addPlain(head, EcosStyle.BOOK_JIQING + "ECOS");
        addPlain(head, EcosStyle.BOOK_LINE + "  ·  ");
        addPlain(head, EcosStyle.BOOK_INK + label + "\n");
        addPlain(head, EcosStyle.BOOK_LINE + "────────────\n");

        List<TerminalPage> tabs = hub.visiblePages(player);
        if (!tabs.isEmpty()) {
            for (int i = 0; i < tabs.size(); i++) {
                TerminalPage tab = tabs.get(i);
                boolean on = tab.id().equals(session.pageId());
                addBtn(head,
                        on ? EcosStyle.BOOK_COBALT + "▸" + tab.label()
                           : EcosStyle.BOOK_MUTED + tab.label(),
                        "tab." + tab.id(),
                        on ? "当前页" : "打开 " + tab.label());
                addPlain(head, (i + 1) % 3 == 0 || i == tabs.size() - 1 ? "\n" : "  ");
            }
            addPlain(head, EcosStyle.BOOK_LINE + "────────────\n");
        }

        if (page != null) {
            for (String line : page.header(player, session)) {
                addPlain(head, line.endsWith("\n") ? line : line + "\n");
            }
        }

        List<TerminalButton> buttons = page == null ? List.of() : page.buttons(player, session);
        List<List<BaseComponent>> pages = new ArrayList<>();
        if (buttons.isEmpty()) {
            pages.add(new ArrayList<>(head));
        } else {
            for (int from = 0; from < buttons.size(); from += BUTTONS_PER_PAGE) {
                List<BaseComponent> body = from == 0 ? new ArrayList<>(head) : new ArrayList<>();
                if (from > 0) {
                    addPlain(body, EcosStyle.BOOK_MUTED + "续 · " + label
                            + "  " + (from / BUTTONS_PER_PAGE + 1) + "\n");
                    addPlain(body, EcosStyle.BOOK_LINE + "────────────\n");
                }
                int col = 0;
                int to = Math.min(from + BUTTONS_PER_PAGE, buttons.size());
                for (int i = from; i < to; i++) {
                    TerminalButton button = buttons.get(i);
                    addBtn(body, button.label(), button.id(), button.hover(), button.raw());
                    col++;
                    addPlain(body, col % 2 == 0 ? "\n" : "   ");
                }
                if (col % 2 == 1) addPlain(body, "\n");
                pages.add(body);
            }
        }

        List<BaseComponent> last = pages.get(pages.size() - 1);
        addPlain(last, "\n");
        addBtn(last, EcosStyle.BOOK_MUTED + "版本", "head", "点看版本");
        addPlain(last, EcosStyle.BOOK_LINE + "  ·  ");
        addBtn(last, EcosStyle.BOOK_MUTED + "合上", "close", "合上书");

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("ECOS");
        meta.setAuthor("ECOS");
        for (List<BaseComponent> bits : pages)
            meta.spigot().addPage(bits.toArray(BaseComponent[]::new));
        book.setItemMeta(meta);
        return new Rendered(book, fingerprint(pages));
    }

    /** 书 + 它的渲染指纹，一次构建出来，免得刷新时重复写书。 */
    private record Rendered(ItemStack book, String fingerprint) {}

    /**
     * 渲染指纹：逐页逐段的文本（含颜色格式）+ 点击目标 + 页边界。
     * 只要玩家能看见或能点到的东西有一处不同，指纹就一定不同。
     */
    private static String fingerprint(List<List<BaseComponent>> pages) {
        StringBuilder sb = new StringBuilder();
        for (List<BaseComponent> bits : pages) {
            for (BaseComponent bit : bits) {
                sb.append(bit.toLegacyText());
                ClickEvent click = bit.getClickEvent();
                if (click != null) {
                    sb.append('\u0001').append(click.getAction()).append('\u0002').append(click.getValue());
                }
                sb.append('\u0003');
            }
            sb.append('\u0004');
        }
        return sha256(sb.toString());
    }

    private static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(text.hashCode());
        }
    }

    private static void addPlain(List<BaseComponent> bits, String colored) {
        for (BaseComponent c : TextComponent.fromLegacyText(ColorUtil.colorize(colored)))
            bits.add(c);
    }

    private static void addBtn(List<BaseComponent> bits, String label, String action, String hover) {
        addBtn(bits, label, action, hover, "");
    }

    private static void addBtn(List<BaseComponent> bits, String label, String action, String hover, String raw) {
        String cmd = "/ecos t " + action + (raw == null || raw.isBlank() ? "" : " " + raw);
        ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd);
        HoverEvent hoverEv = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(EcosStyle.hoverTip(hover)));
        for (BaseComponent c : TextComponent.fromLegacyText(ColorUtil.colorize(label))) {
            c.setClickEvent(click);
            c.setHoverEvent(hoverEv);
            bits.add(c);
        }
    }
}
