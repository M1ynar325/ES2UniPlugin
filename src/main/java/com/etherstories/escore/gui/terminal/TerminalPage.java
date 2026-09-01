package com.etherstories.escore.gui.terminal;

import org.bukkit.entity.Player;

import java.util.List;

public interface TerminalPage {
    String id();
    String label();
    default String hint() { return ""; }
    default boolean visible(Player player) { return true; }
    /** false = 不进顶栏，但仍可 open(pageId) */
    default boolean listed(Player player) { return visible(player); }
    default List<String> header(Player player, TerminalSession session) { return List.of(); }
    List<TerminalButton> buttons(Player player, TerminalSession session);
}
