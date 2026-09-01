package com.etherstories.escore.gui.terminal;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.gui.EcosStyle;
import com.etherstories.escore.utils.ColorUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** 聊天终端。Bungee 组件，跟网页配对链接同一套 API。 */
public final class ChatTerminalView implements TerminalView {
    private static final int ROW = 6;
    private final ES2UniPlugin plugin;

    public ChatTerminalView(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void open(Player player, TerminalSession session) {
        TerminalHub hub = plugin.getTerminalHub();
        TerminalPage page = hub.page(session.pageId());
        String label = page == null ? "?" : page.label();

        send(player, plain("&8———————— &bECOS &8· &f" + label + " &8————————"));

        List<BaseComponent> tabs = new ArrayList<>();
        boolean first = true;
        for (TerminalPage tab : hub.visiblePages(player)) {
            if (!first) tabs.addAll(plain(" "));
            first = false;
            boolean on = tab.id().equals(session.pageId());
            String name = on
                    ? EcosStyle.SNOW + "[" + tab.label() + "]"
                    : "&8[" + tab.label() + "]";
            tabs.addAll(btn(name, "tab." + tab.id(), on ? "当前页" : "切换到 " + tab.label()));
        }
        send(player, tabs);

        if (page != null) {
            List<TerminalButton> buttons = page.buttons(player, session);
            List<BaseComponent> row = new ArrayList<>();
            int n = 0;
            for (TerminalButton button : buttons) {
                if (n > 0) row.addAll(plain(" "));
                row.addAll(btn(button.label(), button.id(), button.hover(), button.raw()));
                n++;
                if (n == ROW) {
                    send(player, row);
                    row = new ArrayList<>();
                    n = 0;
                }
            }
            if (n > 0) send(player, row);
        }

        List<BaseComponent> foot = new ArrayList<>();
        foot.addAll(btn("&7[你好]", "head", "点看版本"));
        foot.addAll(plain("  "));
        foot.addAll(btn("&8[断开]", "close", "结束会话"));
        foot.addAll(plain(EcosStyle.MIST + "  点文字 · /ecos menu 再开"));
        send(player, foot);
    }

    @Override
    public void close(Player player) {
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已断开"));
    }

    private static void send(Player player, List<BaseComponent> bits) {
        player.spigot().sendMessage(bits.toArray(BaseComponent[]::new));
    }

    private static List<BaseComponent> plain(String colored) {
        return List.of(TextComponent.fromLegacyText(ColorUtil.colorize(colored)));
    }

    private static List<BaseComponent> btn(String label, String action, String hover) {
        return btn(label, action, hover, "");
    }

    private static List<BaseComponent> btn(String label, String action, String hover, String raw) {
        String cmd = "/ecos t " + action + (raw == null || raw.isBlank() ? "" : " " + raw);
        ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd);
        HoverEvent hoverEv = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(EcosStyle.hoverTip(hover)));
        List<BaseComponent> out = new ArrayList<>();
        for (BaseComponent c : TextComponent.fromLegacyText(ColorUtil.colorize(label))) {
            c.setClickEvent(click);
            c.setHoverEvent(hoverEv);
            out.add(c);
        }
        return out;
    }
}
