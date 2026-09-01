package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 自定义线路类型：新建 / 编辑 / 删除 */
public class TransitTypesGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l线路类型");
    public static final int SLOT_NEW = 45;
    public static final int SLOT_BACK = 49;
    public static final int START = 9;
    public static final int END = 44;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<String>> slots = new HashMap<>();

    public TransitTypesGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = EcosHolder.of("transit-types", 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.RED_STAINED_GLASS_PANE));

        inv.setItem(4, ECOSTerminalGUI.item(Material.NAME_TAG, "&c线路类型",
                List.of(" &7服务等级，如普通 / 快线 / 磁悬浮",
                        " &7建线时填写类型 ID",
                        "",
                        "&a▸ 点击类型进入编辑",
                        "&a▸ 底栏新建")));

        List<String> ids = new ArrayList<>();
        int slot = START;
        TransitManager tm = plugin.getTransitManager();
        for (TransitManager.LineType t : tm.allTypes()) {
            if (slot > END) break;
            int used = 0;
            for (TransitManager.Line l : tm.allLines()) if (l.type().equals(t.id())) used++;
            inv.setItem(slot++, ECOSTerminalGUI.item(Material.WHITE_CONCRETE,
                    t.color() + t.displayName(),
                    List.of(" &7ID: &f" + t.id(),
                            " &7颜色 " + t.color() + "■■",
                            " &7使用中线路: &f" + used,
                            "",
                            "&a▸ 编辑名称 / 颜色 / 删除")));
            ids.add(t.id());
        }
        slots.put(player.getUniqueId(), ids);
        inv.setItem(SLOT_NEW, ECOSTerminalGUI.item(Material.LIME_DYE, "&a新建类型",
                List.of(" &7点击后在聊天输入 ID",
                        " &7可写: &fid 显示名",
                        " &7例: &fmag 磁悬浮",
                        "",
                        "&a▸ 新建")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回管理", null));
        EcosHolder.open(player, inv);
    }

    public String typeAt(Player player, int slot) {
        List<String> ids = slots.get(player.getUniqueId());
        if (ids == null || slot < START || slot > END) return null;
        int i = slot - START;
        if (i < 0 || i >= ids.size()) return null;
        return ids.get(i);
    }

    public void cleanup(Player player) { slots.remove(player.getUniqueId()); }
}
