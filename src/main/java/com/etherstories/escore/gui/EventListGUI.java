package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.events.ServerEvent;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EventListGUI {

    private static final int ROWS = 4;
    private static final int SIZE = ROWS * 9; // 36
    private static final int EVENT_SLOTS = 27;  // first 3 rows
    private static final Material GLASS = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin plugin;

    public EventListGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = ColorUtil.colorize(plugin.getConfigManager().getEventGUITitle());
        Inventory inv = EcosHolder.of("event", SIZE, title);

        Collection<ServerEvent> events = plugin.getEventManager().getAllEvents();
        List<ServerEvent> list = new ArrayList<>(events);

        // Populate events (first 3 rows = slots 0-26)
        for (int i = 0; i < Math.min(list.size(), EVENT_SLOTS); i++) {
            inv.setItem(i, buildEventItem(list.get(i), player));
        }

        // Separator row (row 4, slots 27-35)
        ItemStack glass = makeItem(GLASS, " ", null);
        for (int i = 27; i < SIZE; i++) inv.setItem(i, glass);

        // Close button slot 35
        inv.setItem(35, makeItem(Material.BARRIER,
                ColorUtil.colorize("&c关闭"), null));

        // Empty hint if no events
        if (list.isEmpty()) {
            inv.setItem(13, makeItem(Material.PAPER,
                    ColorUtil.colorize("&7当前没有活动"),
                    List.of(ColorUtil.colorize("&8等待管理员创建活动"))));
        }

        EcosHolder.open(player, inv);
    }

    private ItemStack buildEventItem(ServerEvent event, Player player) {
        boolean joined = event.hasJoined(player.getUniqueId());
        Material mat = joined ? Material.LIME_DYE : Material.PAPER;

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtil.colorize("&7" + (event.getDescription().isEmpty() ? "无描述" : event.getDescription())));
        lore.add("");
        lore.add(ColorUtil.colorize("&7参与人数: &f" + event.getParticipantDisplay()));
        lore.add(ColorUtil.colorize("&7创建者: &f" + event.getCreator()));
        lore.add("");
        if (joined) {
            lore.add(ColorUtil.colorize("&a✔ 已参与  &7（左键退出）"));
        } else {
            lore.add(ColorUtil.colorize("&e▶ 点击参与活动"));
        }

        return makeItem(mat, ColorUtil.colorize("&f&l" + event.getName()), lore);
    }

    /** Returns the event at a given slot index, or null if none. */
    public ServerEvent getEventAt(int slot, Player player) {
        if (slot >= EVENT_SLOTS) return null;
        List<ServerEvent> list = new ArrayList<>(plugin.getEventManager().getAllEvents());
        return slot < list.size() ? list.get(slot) : null;
    }

    public static boolean isCloseSlot(int slot) {
        return slot == 35;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
