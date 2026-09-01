package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.estate.EstateUnit;
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

public class EstateRoomsGUI {

    public static final String TITLE_PREFIX = EcosStyle.hub("房产 · ");
    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, String> building = new HashMap<>();
    private final Map<UUID, List<EstateUnit>> listed = new HashMap<>();

    public EstateRoomsGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isTitle(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    public void open(Player player, String buildingName) {
        building.put(player.getUniqueId(), buildingName);
        Inventory inv = EcosHolder.of("estate-rooms", 54, TITLE_PREFIX + buildingName);
        ItemStack bg = EcosStyle.chrome();
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        List<EstateUnit> rooms = plugin.getEstateManager().inBuilding(buildingName);
        listed.put(player.getUniqueId(), rooms);
        for (int i = 0; i < Math.min(rooms.size(), 45); i++) {
            inv.setItem(i, icon(player, rooms.get(i)));
        }
        if (rooms.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7这栋还没有房间", null));
        }
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回楼盘", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }

    private ItemStack icon(Player viewer, EstateUnit u) {
        Material mat = u.vacant() ? Material.LIME_CONCRETE : u.kind().icon;
        List<String> lore = new ArrayList<>();
        lore.add(" &7" + u.kind().label + " · " + u.category().label);
        lore.add(u.vacant() ? " &a空闲" : " &7房主 &f" + (u.ownerName() == null ? "?" : u.ownerName()));
        if (u.listed() && u.price() > 0) {
            lore.add(" &e挂牌 " + price(u.price()));
        }
        lore.add(" &8" + u.world() + " " + u.minX() + "," + u.minY() + "," + u.minZ());
        lore.add("");
        boolean mine = viewer.getUniqueId().equals(u.owner());
        if (mine) {
            lore.add("&a左键: 传送到门口");
            lore.add(u.listed() ? "&e右键: 取消挂牌" : "&e右键: 上架（没标价会问金额）");
        } else if (u.listed() && u.price() > 0) {
            lore.add("&a左键: 看房（传到门口）");
            lore.add("&e右键: 购买（再点一次确认）");
        } else if (viewer.hasPermission("es2uni.admin")) {
            lore.add("&c管理: 左键传送  潜行左键删除");
        } else {
            lore.add("&8未挂牌");
        }
        String title = (u.vacant() ? "&a" : "&f") + u.floor() + "-" + u.room();
        return ECOSTerminalGUI.item(mat, title, lore);
    }

    private String price(double v) {
        if (plugin.getVaultHook().isEnabled()) return plugin.getVaultHook().format(v);
        return String.format("%.0f", v);
    }

    public EstateUnit unitAt(Player p, int slot) {
        List<EstateUnit> list = listed.get(p.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size() || slot >= 45) return null;
        return list.get(slot);
    }

    public String buildingOf(Player p) {
        return building.get(p.getUniqueId());
    }

    public void cleanup(Player p) {
        building.remove(p.getUniqueId());
        listed.remove(p.getUniqueId());
    }
}
