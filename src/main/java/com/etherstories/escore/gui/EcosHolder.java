package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Arclight 上 getHolder() / getTitle() 经常丢。
 * 打开时自己记 kind，并给格子打 PDC（跟 ESLink 一样）。
 */
public final class EcosHolder implements InventoryHolder {
    public final String kind;
    public Inventory inv;

    private static final Map<UUID, String> OPEN = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> LAST = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> OPENING = new ThreadLocal<>();

    public EcosHolder(String kind) {
        this.kind = kind;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public static Inventory of(String kind, int size, String title) {
        LAST.set(kind);
        EcosHolder h = new EcosHolder(kind);
        Inventory inv = Bukkit.createInventory(h, size, title);
        h.inv = inv;
        return inv;
    }

    /** 不再开箱子。Arclight 箱 UI 认不出，一律转书本。 */
    public static void open(Player player, Inventory inv) {
        String kind = resolveKind(inv);
        LAST.remove();
        ES2UniPlugin plugin = ES2UniPlugin.getInstance();
        if (plugin != null && plugin.getTerminalHub() != null && kind != null) {
            plugin.getTerminalHub().open(player, pageId(kind));
            return;
        }
        if (player != null)
            player.sendMessage("§8[ECOS] §c界面未就绪: " + kind);
    }

    public static boolean opening() {
        return Boolean.TRUE.equals(OPENING.get());
    }

    private static String pageId(String kind) {
        if (kind == null) return null;
        return switch (kind) {
            case "home" -> "homes";
            case "friends", "friends-req" -> "friends-list";
            case "mail" -> "mail-list";
            case "online" -> "online-list";
            case "notice" -> "notices-list";
            case "waypoint" -> "wp-list";
            default -> kind;
        };
    }

    public static String kindOf(Player player, Inventory top) {
        if (top != null && top.getHolder() instanceof EcosHolder h) return h.kind;
        String fromItem = kindFrom(top);
        if (fromItem != null) return fromItem;
        return player == null ? null : OPEN.get(player.getUniqueId());
    }

    public static String kindFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(kindKey(), PersistentDataType.STRING);
    }

    public static boolean ours(Player player, Inventory top) {
        return kindOf(player, top) != null;
    }

    public static void forget(UUID uuid) {
        if (uuid != null) OPEN.remove(uuid);
    }

    public static boolean is(Inventory inv) {
        return inv != null && inv.getHolder() instanceof EcosHolder;
    }

    public static boolean is(Inventory inv, String kind) {
        return inv != null && inv.getHolder() instanceof EcosHolder h && kind.equals(h.kind);
    }

    private static String resolveKind(Inventory inv) {
        if (inv != null && inv.getHolder() instanceof EcosHolder h) return h.kind;
        String fromItem = kindFrom(inv);
        if (fromItem != null) return fromItem;
        return LAST.get();
    }

    private static String kindFrom(Inventory inv) {
        if (inv == null) return null;
        for (ItemStack it : inv.getContents()) {
            String k = kindFromItem(it);
            if (k != null && !k.isBlank()) return k;
        }
        return null;
    }

    private static void stamp(Inventory inv, String kind) {
        if (inv == null || kind == null) return;
        NamespacedKey ui = uiKey();
        NamespacedKey k = kindKey();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType().isAir()) continue;
            ItemMeta meta = it.getItemMeta();
            if (meta == null) continue;
            try {
                meta.getPersistentDataContainer().set(ui, PersistentDataType.BYTE, (byte) 1);
                meta.getPersistentDataContainer().set(k, PersistentDataType.STRING, kind);
                it.setItemMeta(meta);
                inv.setItem(i, it);
            } catch (Throwable ignored) {}
        }
    }

    private static NamespacedKey uiKey() {
        return new NamespacedKey(ES2UniPlugin.getInstance(), "ui");
    }

    private static NamespacedKey kindKey() {
        return new NamespacedKey(ES2UniPlugin.getInstance(), "ui_kind");
    }
}
