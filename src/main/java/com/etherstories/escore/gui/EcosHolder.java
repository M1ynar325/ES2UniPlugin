package com.etherstories.escore.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** 认 ECOS 菜单，不靠标题（Youer/Arclight 上 getTitle 会对不上）。 */
public final class EcosHolder implements InventoryHolder {

    public enum Kind { TERMINAL }

    private final Kind kind;
    private Inventory inv;

    public EcosHolder(Kind kind) {
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public void bind(Inventory inv) {
        this.inv = inv;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public static boolean isTerminal(InventoryHolder h) {
        return h instanceof EcosHolder eh && eh.kind == Kind.TERMINAL;
    }
}
