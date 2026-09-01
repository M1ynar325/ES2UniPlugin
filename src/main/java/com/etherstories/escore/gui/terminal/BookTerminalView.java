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

import java.util.ArrayList;
import java.util.List;

/** 书本。Bungee 写页。按钮多时自动分页，避免一页写爆。 */
public final class BookTerminalView implements TerminalView {
    private static final int BUTTONS_PER_PAGE = 12;
    private final ES2UniPlugin plugin;

    public BookTerminalView(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void open(Player player, TerminalSession session) {
        try {
            ItemStack book = build(player, session);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                try {
                    player.openBook(book);
                } catch (Throwable t) {
                    plugin.getLogger().warning("openBook 失败，改聊天: " + t);
                    new ChatTerminalView(plugin).open(player, session);
                }
            });
        } catch (Throwable t) {
            plugin.getLogger().warning("写书失败，改聊天: " + t);
            new ChatTerminalView(plugin).open(player, session);
        }
    }

    @Override
    public void close(Player player) {
        if (player.isOnline()) {
            try { player.closeInventory(); } catch (Throwable ignored) {}
        }
    }

    private ItemStack build(Player player, TerminalSession session) {
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
        return book;
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
