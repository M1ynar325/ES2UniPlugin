package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class TransitBoardGUI {

    public static final String TITLE = "&b轨道交通排行";
    public static final int CLOSE_SLOT = 53;
    public static final int BACK_SLOT = 45;
    public static final int PLAYTIME_SLOT = 47;
    private static final int SIZE = 54;

    private final ES2UniPlugin plugin;

    public TransitBoardGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, ColorUtil.colorize(TITLE));
        ItemStack bg = ECOSTerminalGUI.bg(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, bg);

        List<TransitManager.RideStat> top = plugin.getTransitManager().topRiders(45);
        for (int i = 0; i < top.size(); i++) {
            TransitManager.RideStat st = top.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(st.uuid());
            String name = st.name() == null || st.name().isBlank()
                    ? (op.getName() != null ? op.getName() : "?") : st.name();
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(op);
                String rank = switch (i) {
                    case 0 -> "&e#1";
                    case 1 -> "&7#2";
                    case 2 -> "&6#3";
                    default -> "&8#" + (i + 1);
                };
                meta.setDisplayName(ColorUtil.colorize(rank + " &f" + name));
                String fare = plugin.getVaultHook().isEnabled()
                        ? plugin.getVaultHook().format(st.fare()) : String.format("%.0f", st.fare());
                meta.setLore(List.of(
                        ColorUtil.colorize("&7乘次 &f" + st.rides()),
                        ColorUtil.colorize("&7票款 &e" + fare)));
                skull.setItemMeta(meta);
            }
            inv.setItem(i, skull);
        }
        if (top.isEmpty())
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7暂无乘车数据",
                    List.of(ColorUtil.colorize("&8出站计费后计入"))));

        inv.setItem(BACK_SLOT, ECOSTerminalGUI.item(Material.ARROW, "&7返回交通处", null));
        inv.setItem(PLAYTIME_SLOT, ECOSTerminalGUI.item(Material.GOLDEN_HELMET, "&7在线时长排行",
                List.of(ColorUtil.colorize("&8点击切换"))));
        inv.setItem(CLOSE_SLOT, ECOSTerminalGUI.item(Material.BARRIER, "&7关闭", null));
        player.openInventory(inv);
    }
}
