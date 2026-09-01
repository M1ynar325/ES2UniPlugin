package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.events.ServerEvent;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminEventGUI {

    private static final int SIZE = 36;
    private static final int EVENT_SLOTS = 27;
    private static final int BACK_SLOT   = 27;
    private static final int CREATE_SLOT = 29;
    private static final int CLOSE_SLOT  = 35;
    private static final Material GLASS  = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin plugin;

    public AdminEventGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = ColorUtil.colorize(plugin.getConfigManager().getAdminEventGUITitle());
        Inventory inv = EcosHolder.of("admin-event", SIZE, title);

        List<ServerEvent> list = new ArrayList<>(plugin.getEventManager().getAllEvents());

        for (int i = 0; i < Math.min(list.size(), EVENT_SLOTS); i++) {
            inv.setItem(i, buildEventItem(list.get(i)));
        }

        // Bottom row
        ItemStack glass = EventListGUI.makeItem(GLASS, " ", null);
        for (int i = 27; i < SIZE; i++) inv.setItem(i, glass);

        inv.setItem(BACK_SLOT, EventListGUI.makeItem(Material.ARROW,
                ColorUtil.colorize("&7返回终端"), null));

        inv.setItem(CREATE_SLOT, EventListGUI.makeItem(Material.EMERALD,
                ColorUtil.colorize("&a&l✦ 创建新活动"),
                List.of(ColorUtil.colorize("&7点击 → 铁砧界面输入活动名"))));

        inv.setItem(CLOSE_SLOT, EventListGUI.makeItem(Material.BARRIER,
                ColorUtil.colorize("&c关闭"), null));

        if (list.isEmpty()) {
            inv.setItem(13, EventListGUI.makeItem(Material.PAPER,
                    ColorUtil.colorize("&7暂无活动"),
                    List.of(ColorUtil.colorize("&8点击右下角创建"))));
        }

        EcosHolder.open(player, inv);
    }

    private ItemStack buildEventItem(ServerEvent event) {
        Set<UUID> parts = event.getParticipants();
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtil.colorize("&7描述: &f" + (event.getDescription().isEmpty() ? "无" : event.getDescription())));
        lore.add(ColorUtil.colorize("&7创建者: &f" + event.getCreator()));
        lore.add(ColorUtil.colorize("&7参与人数: &f" + event.getParticipantDisplay()));
        if (!parts.isEmpty()) {
            lore.add("");
            lore.add(ColorUtil.colorize("&7参与者:"));
            parts.stream()
                    .map(uuid -> {
                        var p = Bukkit.getOfflinePlayer(uuid);
                        return ColorUtil.colorize("  &f" + (p.getName() != null ? p.getName() : uuid));
                    })
                    .limit(10)
                    .forEach(lore::add);
            if (parts.size() > 10) lore.add(ColorUtil.colorize("  &8...及 " + (parts.size() - 10) + " 人"));
        }
        lore.add("");
        lore.add(ColorUtil.colorize("&e左键 &7— 查看详情"));
        lore.add(ColorUtil.colorize("&c右键 &7— 删除活动"));

        return EventListGUI.makeItem(Material.PAPER,
                ColorUtil.colorize("&f&l" + event.getName()), lore);
    }

    public ServerEvent getEventAt(int slot) {
        if (slot >= EVENT_SLOTS) return null;
        List<ServerEvent> list = new ArrayList<>(plugin.getEventManager().getAllEvents());
        return slot < list.size() ? list.get(slot) : null;
    }

    public static boolean isBackSlot(int slot)   { return slot == BACK_SLOT;   }
    public static boolean isCreateSlot(int slot) { return slot == CREATE_SLOT; }
    public static boolean isCloseSlot(int slot)  { return slot == CLOSE_SLOT;  }
}
