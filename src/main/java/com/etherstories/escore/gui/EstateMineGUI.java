package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.estate.EstateUnit;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EstateMineGUI {

    public static final String TITLE = ColorUtil.colorize("&e&l我的房产");
    public static final String ADMIN_TITLE = ColorUtil.colorize("&c&l全部房产");
    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<EstateUnit>> listed = new HashMap<>();
    private final Map<UUID, Boolean> adminView = new HashMap<>();

    public EstateMineGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMine(Player player) {
        open(player, false);
    }

    public void openAdmin(Player player) {
        open(player, true);
    }

    private void open(Player player, boolean admin) {
        adminView.put(player.getUniqueId(), admin);
        List<EstateUnit> rooms = admin
                ? new ArrayList<>(plugin.getEstateManager().all())
                : plugin.getEstateManager().ownedBy(player.getUniqueId());
        listed.put(player.getUniqueId(), rooms);
        Inventory inv = Bukkit.createInventory(null, 54, admin ? ADMIN_TITLE : TITLE);
        ItemStack bg = ECOSTerminalGUI.bg(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);
        for (int i = 0; i < Math.min(rooms.size(), 45); i++) {
            EstateUnit u = rooms.get(i);
            List<String> lore = new ArrayList<>();
            lore.add(" &7" + u.address());
            lore.add(" &7" + u.kind().label + " · " + u.category().label);
            lore.add(u.vacant() ? " &a空闲" : " &7房主 &f" + (u.ownerName() == null ? "?" : u.ownerName()));
            if (u.listed() && u.price() > 0) lore.add(" &e挂牌中");
            lore.add("");
            lore.add("&a左键: 传送");
            lore.add(u.listed() ? "&e右键: 下架" : "&e右键: 上架（没标价会问金额）");
            lore.add("&8潜行右键: 改价");
            if (admin) lore.add("&c潜行左键: 删除");
            inv.setItem(i, ECOSTerminalGUI.item(u.kind().icon, "&f" + u.address(), lore));
        }
        if (rooms.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER,
                    admin ? "&7还没有登记" : "&7你还没有房产",
                    List.of("&8终端出行页「登记房产」")));
        }
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回楼盘", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    public EstateUnit unitAt(Player p, int slot) {
        List<EstateUnit> list = listed.get(p.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size() || slot >= 45) return null;
        return list.get(slot);
    }

    public boolean isAdminView(Player p) {
        return Boolean.TRUE.equals(adminView.get(p.getUniqueId()));
    }

    public void cleanup(Player p) {
        listed.remove(p.getUniqueId());
        adminView.remove(p.getUniqueId());
    }
}
