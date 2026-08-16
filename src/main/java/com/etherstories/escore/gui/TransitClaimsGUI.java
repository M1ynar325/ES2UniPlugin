package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 行程异常申报审核 */
public class TransitClaimsGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l行程异常申报");
    public static final int SLOT_BACK = 49;
    public static final int START = 9;
    public static final int END = 44;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<String>> slots = new HashMap<>();

    public TransitClaimsGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.RED_STAINED_GLASS_PANE));

        List<TransitManager.Claim> pending = plugin.getTransitManager().pendingClaims();
        inv.setItem(4, ECOSTerminalGUI.item(Material.SHIELD, "&c行程异常申报 · 待审 " + pending.size(),
                List.of(" &7车辆异常、连接中断或未能完成行程",
                        " &7核准：撤销未完成行程，不补收全程票",
                        " &a左键核准  &c右键驳回")));

        List<String> ids = new ArrayList<>();
        int slot = START;
        SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm");
        for (TransitManager.Claim c : pending) {
            if (slot > END) break;
            inv.setItem(slot++, ECOSTerminalGUI.item(Material.PAPER,
                    "&f" + c.playerName(),
                    List.of(" &7进站: &f" + c.originName(),
                            " &7申请: &f" + fmt.format(new Date(c.createdAt())),
                            " &7" + c.id(),
                            "",
                            "&a▸ 左键通过",
                            "&c▸ 右键驳回")));
            ids.add(c.id());
        }
        slots.put(player.getUniqueId(), ids);
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回管理", null));
        player.openInventory(inv);
    }

    public String claimAt(Player player, int slot) {
        List<String> ids = slots.get(player.getUniqueId());
        if (ids == null || slot < START || slot > END) return null;
        int i = slot - START;
        if (i < 0 || i >= ids.size()) return null;
        return ids.get(i);
    }

    public void cleanup(Player player) { slots.remove(player.getUniqueId()); }
}
