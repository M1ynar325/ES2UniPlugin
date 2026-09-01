package com.etherstories.escore.gui.terminal;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TerminalHub {
    private final ES2UniPlugin plugin;
    private final Map<String, TerminalPage> pages = new LinkedHashMap<>();
    private final Map<String, TerminalAction> actions = new LinkedHashMap<>();
    private final Map<UUID, TerminalSession> sessions = new ConcurrentHashMap<>();
    private TerminalView view = (player, session) -> {};

    public TerminalHub(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void installDefaults() {
        BuiltinPages.register(this, plugin);
        ListPages.register(this, plugin);
        ChestlessPages.register(this, plugin);
        BuiltinActions.register(this, plugin);
        ListPages.registerActions(this, plugin);
        ChestlessPages.registerActions(this, plugin);
        setView(new BookTerminalView(plugin));
    }

    public ES2UniPlugin plugin() { return plugin; }

    public void setView(TerminalView view) {
        if (view != null) this.view = view;
    }

    public TerminalView view() { return view; }

    public void registerPage(TerminalPage page) {
        pages.put(page.id(), page);
    }

    public void registerAction(String id, TerminalAction action) {
        actions.put(id, action);
    }

    public TerminalPage page(String id) {
        return id == null ? null : pages.get(id);
    }

    public List<TerminalPage> visiblePages(Player player) {
        return pages.values().stream().filter(p -> p.listed(player)).toList();
    }

    public Collection<TerminalPage> pages() { return pages.values(); }

    public TerminalSession session(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void open(Player player) {
        open(player, null);
    }

    public void open(Player player, String pageId) {
        TerminalSession session = sessions.computeIfAbsent(player.getUniqueId(), id -> new TerminalSession());
        if (pageId != null) {
            TerminalPage page = pages.get(pageId);
            if (page != null && page.visible(player)) {
                String cur = session.pageId();
                if (cur != null && !pageId.equals(cur)) {
                    TerminalPage prev = pages.get(cur);
                    if (!page.listed(player) && prev != null && prev.listed(player))
                        session.put("from", cur);
                }
                if (!pageId.equals(cur)) session.put("off", "0");
                session.setPageId(pageId);
            }
        }
        if (session.pageId() == null || page(session.pageId()) == null
                || !page(session.pageId()).visible(player)) {
            session.setPageId(firstVisible(player));
        }
        view.open(player, session);
        try {
            if (plugin.getNewbieGuideManager() != null)
                plugin.getNewbieGuideManager().mark(player.getUniqueId(),
                        com.etherstories.escore.managers.NewbieGuideManager.Step.OPEN_TERMINAL);
        } catch (Throwable ignored) {}
    }

    public void refresh(Player player) {
        TerminalSession session = sessions.get(player.getUniqueId());
        if (session == null) open(player);
        else view.refresh(player, session);
    }

    public void close(Player player) {
        boolean had = sessions.remove(player.getUniqueId()) != null;
        if (had && player.isOnline()) view.close(player);
    }

    /** 隐藏指令 /ecos _t <actionId> [raw] */
    public void click(Player player, String actionId, String raw) {
        if (actionId == null || actionId.isBlank()) return;
        TerminalSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            open(player);
            session = sessions.get(player.getUniqueId());
            if (session == null) return;
        }
        if (actionId.startsWith("tab:") || actionId.startsWith("tab.")) {
            String id = actionId.substring(4).toLowerCase();
            TerminalPage page = pages.get(id);
            if (page == null || !page.visible(player)) return;
            session.setPageId(id);
            playClick(player);
            view.refresh(player, session);
            return;
        }
        if (!allowed(player, session, actionId)) return;
        TerminalAction action = actions.get(actionId);
        if (action == null) return;
        playClick(player);
        action.run(player, session, raw == null ? "" : raw);
    }

    private boolean allowed(Player player, TerminalSession session, String actionId) {
        if (actionId.equals("close") || actionId.equals("head")) return true;
        TerminalPage page = page(session.pageId());
        if (page == null) return false;
        for (TerminalButton button : page.buttons(player, session)) {
            if (button.id().equals(actionId)) return true;
        }
        return false;
    }

    private String firstVisible(Player player) {
        for (TerminalPage page : pages.values()) {
            if (page.visible(player)) return page.id();
        }
        return null;
    }

    public void playClick(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
        } catch (Throwable ignored) {}
    }
}
