package com.etherstories.escore.gui.terminal;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface TerminalAction {
    void run(Player player, TerminalSession session, String raw);
}
