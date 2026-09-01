package com.etherstories.escore.gui.terminal;

import org.bukkit.entity.Player;

/** 渲染后端。聊天是默认实现；以后可换书本或箱子，只 setView。 */
public interface TerminalView {
    void open(Player player, TerminalSession session);
    default void refresh(Player player, TerminalSession session) { open(player, session); }
    default void close(Player player) {}
}
