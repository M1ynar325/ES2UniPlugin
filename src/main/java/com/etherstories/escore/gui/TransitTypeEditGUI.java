package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TransitTypeEditGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l编辑类型");
    public static final int SLOT_INFO = 4;
    public static final int SLOT_RENAME = 11;
    public static final int SLOT_DELETE = 15;
    public static final int COLOR_START = 19;
    public static final int COLOR_END = 27;
    public static final int SLOT_BACK = 49;

    public static final String[] COLORS = {"&b", "&3", "&9", "&a", "&2", "&e", "&6", "&c", "&d"};
    private static final Material[] WOOL = {
            Material.LIGHT_BLUE_WOOL, Material.CYAN_WOOL, Material.BLUE_WOOL,
            Material.LIME_WOOL, Material.GREEN_WOOL, Material.YELLOW_WOOL,
            Material.ORANGE_WOOL, Material.RED_WOOL, Material.MAGENTA_WOOL
    };

    private final ES2UniPlugin plugin;
    private final Map<UUID, String> editing = new HashMap<>();

    public TransitTypeEditGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player, String typeId) {
        TransitManager.LineType t = plugin.getTransitManager().getType(typeId);
        if (t == null) {
            player.sendMessage(ColorUtil.colorize("&8[交通] &c类型不存在"));
            return;
        }
        editing.put(player.getUniqueId(), typeId);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.RED_STAINED_GLASS_PANE));

        int used = 0;
        for (TransitManager.Line l : plugin.getTransitManager().allLines())
            if (l.type().equals(t.id())) used++;

        inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.NAME_TAG, t.color() + t.displayName(),
                List.of(" &7ID: &f" + t.id(),
                        " &7颜色 " + t.color() + "■■",
                        " &7使用中线路: &f" + used)));
        inv.setItem(SLOT_RENAME, ECOSTerminalGUI.item(Material.WRITABLE_BOOK, "&f修改显示名",
                List.of(" &7点击后在聊天输入新名称", " &7输入 &fcancel &7取消", "", "&f▸ 改名")));
        inv.setItem(SLOT_DELETE, ECOSTerminalGUI.item(Material.BARRIER, "&c删除此类型",
                List.of(" &7使用中的线路会改回「普通」",
                        " &7至少保留一种类型",
                        "",
                        "&c▸ Shift 点击确认删除")));

        for (int i = 0; i < COLORS.length; i++) {
            boolean on = COLORS[i].equals(t.color());
            inv.setItem(COLOR_START + i, ECOSTerminalGUI.item(WOOL[i],
                    COLORS[i] + (on ? "当前颜色" : "选用此色"),
                    List.of(on ? " &a正在使用" : " &a▸ 点击")));
        }
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回类型列表", null));
        player.openInventory(inv);
    }

    public String editing(Player player) { return editing.get(player.getUniqueId()); }

    public String colorAt(int slot) {
        if (slot < COLOR_START || slot > COLOR_END) return null;
        return COLORS[slot - COLOR_START];
    }

    public void cleanup(Player player) { editing.remove(player.getUniqueId()); }
}
