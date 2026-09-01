package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.RegionManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AdminTerritoryGUI {

    public static final String TITLE      = "&c&l⚙ 领地管理 [管理员]";
    public static final int    SIZE       = 54;
    private static final int   LIST_SIZE  = 45;
    public static final int    PREV_SLOT  = 45;
    public static final int    BACK_SLOT  = 46;
    public static final int    INFO_SLOT  = 49;
    public static final int    CLOSE_SLOT = 52;
    public static final int    NEXT_SLOT  = 53;

    private final ES2UniPlugin                          plugin;
    private final Map<UUID, Integer>                    pages = new HashMap<>();
    private final Map<UUID, List<RegionManager.Region>> cache = new HashMap<>();

    public AdminTerritoryGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<RegionManager.Region> list = new ArrayList<>(plugin.getRegionManager().getAllTerritories());
        list.sort(Comparator.comparing(RegionManager.Region::name));
        cache.put(player.getUniqueId(), list);
        pages.putIfAbsent(player.getUniqueId(), 0);
        render(player);
    }

    /** Refresh without resetting page (call after toggle). */
    public void refresh(Player player) {
        List<RegionManager.Region> list = new ArrayList<>(plugin.getRegionManager().getAllTerritories());
        list.sort(Comparator.comparing(RegionManager.Region::name));
        cache.put(player.getUniqueId(), list);
        render(player);
    }

    private void render(Player player) {
        List<RegionManager.Region> list = cache.getOrDefault(player.getUniqueId(), List.of());
        int page    = pages.getOrDefault(player.getUniqueId(), 0);
        int maxPage = list.isEmpty() ? 0 : (list.size() - 1) / LIST_SIZE;

        Inventory inv = EcosHolder.of("admin-territory", SIZE, ColorUtil.colorize(TITLE));
        ItemStack bg  = ECOSTerminalGUI.bg(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = LIST_SIZE; i < SIZE; i++) inv.setItem(i, bg);

        int start = page * LIST_SIZE;
        for (int i = 0; i < LIST_SIZE && (start + i) < list.size(); i++) {
            RegionManager.Region r  = list.get(start + i);
            String ownerName = ownerName(r);
            Material mat    = r.visible() ? Material.GRASS_BLOCK : Material.DIRT;
            String status   = r.visible() ? "&a公开" : "&8隐藏";
            String nameColor = r.visible() ? "&f&l" : "&7";
            inv.setItem(i, ECOSTerminalGUI.item(mat,
                    nameColor + r.name(),
                    List.of(ColorUtil.colorize(" &7所有者: &f" + ownerName),
                            ColorUtil.colorize(" &7世界: &8" + r.world()),
                            ColorUtil.colorize(" &7面积: &8" + r.area() + " 格"),
                            ColorUtil.colorize(" &7状态: " + status),
                            ColorUtil.colorize(""),
                            ColorUtil.colorize("&8左键: 传送   右键: 切换公开/隐藏"))));
        }

        if (list.isEmpty())
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7暂无领地", null));

        inv.setItem(PREV_SLOT, page > 0
                ? ECOSTerminalGUI.item(Material.ARROW, "&7上一页", List.of(ColorUtil.colorize("&8第 " + page + " / " + (maxPage + 1) + " 页")))
                : bg);
        inv.setItem(BACK_SLOT, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(INFO_SLOT, ECOSTerminalGUI.item(Material.PAPER,
                "&7第 " + (page + 1) + " / " + (maxPage + 1) + " 页",
                List.of(ColorUtil.colorize(" &8共 " + list.size() + " 个领地"))));
        inv.setItem(CLOSE_SLOT, ECOSTerminalGUI.item(Material.BARRIER, "&7关闭", null));
        inv.setItem(NEXT_SLOT, page < maxPage
                ? ECOSTerminalGUI.item(Material.ARROW, "&7下一页", List.of(ColorUtil.colorize("&8第 " + (page + 2) + " / " + (maxPage + 1) + " 页")))
                : bg);

        EcosHolder.open(player, inv);
    }

    public void prevPage(Player player) {
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        if (page > 0) { pages.put(player.getUniqueId(), page - 1); render(player); }
    }

    public void nextPage(Player player) {
        List<RegionManager.Region> list = cache.getOrDefault(player.getUniqueId(), List.of());
        int page    = pages.getOrDefault(player.getUniqueId(), 0);
        int maxPage = list.isEmpty() ? 0 : (list.size() - 1) / LIST_SIZE;
        if (page < maxPage) { pages.put(player.getUniqueId(), page + 1); render(player); }
    }

    public RegionManager.Region getAt(Player player, int slot) {
        List<RegionManager.Region> list = cache.getOrDefault(player.getUniqueId(), List.of());
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        int idx  = page * LIST_SIZE + slot;
        return (slot >= 0 && slot < LIST_SIZE && idx < list.size()) ? list.get(idx) : null;
    }

    public void teleport(Player player, RegionManager.Region r) {
        World world = Bukkit.getWorld(r.world());
        if (world == null) { player.sendMessage(ColorUtil.colorize("&8[ECOS] &7世界不存在")); return; }
        player.teleport(r.teleportLoc(world));
        player.sendMessage(ColorUtil.colorize("&8[ECOS] &7已传送到 &c" + r.name()));
    }

    public void cleanup(Player player) {
        pages.remove(player.getUniqueId());
        cache.remove(player.getUniqueId());
    }

    private static String ownerName(RegionManager.Region r) {
        if (r.owner() == null) return "未知";
        String name = Bukkit.getOfflinePlayer(r.owner()).getName();
        return name != null ? name : r.owner().toString().substring(0, 8);
    }
}
