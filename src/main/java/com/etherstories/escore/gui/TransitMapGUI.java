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

/** 按线路分页的车站图，点两站查票价 */
public class TransitMapGUI {

    public static final String TITLE = ColorUtil.colorize("&e&l线路图");
    public static final int SLOT_PREV = 45;
    public static final int SLOT_NEXT = 53;
    public static final int SLOT_BACK = 49;
    public static final int SLOT_CLEAR = 48;

    private static final Material[] ICONS = {
            Material.QUARTZ_BLOCK, Material.PRISMARINE, Material.PURPUR_BLOCK,
            Material.SANDSTONE, Material.RED_SANDSTONE, Material.PACKED_ICE,
            Material.MOSSY_STONE_BRICKS, Material.POLISHED_ANDESITE, Material.BLACKSTONE,
            Material.DEEPSLATE_TILES, Material.COPPER_BLOCK, Material.AMETHYST_BLOCK,
            Material.HONEYCOMB_BLOCK, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE,
            Material.BRICKS, Material.NETHER_BRICKS, Material.END_STONE_BRICKS,
            Material.MUD_BRICKS, Material.TUFF, Material.CHERRY_PLANKS,
            Material.BAMBOO_BLOCK, Material.WARPED_PLANKS, Material.SMOOTH_STONE
    };

    private final ES2UniPlugin plugin;
    private final Map<UUID, Integer> page = new HashMap<>();
    private final Map<UUID, List<String>> stationSlots = new HashMap<>();
    private final Map<UUID, String> fareFrom = new HashMap<>();

    public TransitMapGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        open(player, page.getOrDefault(player.getUniqueId(), 0));
    }

    public void open(Player player, int p) {
        List<TransitManager.Line> lines = new ArrayList<>(plugin.getTransitManager().allLines());
        if (lines.isEmpty()) {
            player.sendMessage(ColorUtil.colorize("&8[交通] &7还没有线路"));
            return;
        }
        if (p < 0) p = 0;
        if (p >= lines.size()) p = lines.size() - 1;
        page.put(player.getUniqueId(), p);

        TransitManager.Line line = lines.get(p);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.YELLOW_STAINED_GLASS_PANE));

        TransitManager tm = plugin.getTransitManager();
        String fromId = fareFrom.get(player.getUniqueId());
        TransitManager.Station fromSt = fromId == null ? null : tm.getStation(fromId);
        List<String> head = new ArrayList<>();
        head.add(" &7ID: &f" + line.id());
        head.add(" &7类型: &f" + tm.typeName(line.type()));
        head.add(" &7每站: &e" + fmt(line.hopFare()) + "  &7上限: &e" + fmt(line.maxFare()));
        head.add(" &8" + (p + 1) + "/" + lines.size());
        head.add("");
        if (fromSt == null) {
            head.add(" &7点一个站选起点，再点另一站查票价");
        } else {
            head.add(" &e起点: &f" + fromSt.displayName());
            head.add(" &7再点另一站查看最短路票价");
        }
        inv.setItem(4, ECOSTerminalGUI.item(Material.POWERED_RAIL,
                line.color() + line.displayName(), head));

        List<String> ids = new ArrayList<>();
        int slot = 9;
        for (TransitManager.Station s : tm.allStations()) {
            if (!s.lineIds().contains(line.id())) continue;
            if (slot >= 44) break;
            List<String> lore = new ArrayList<>();
            lore.add(" &7ID: &f" + s.id());
            lore.add(" &7" + tm.lineNamesOf(s));
            if (tm.isSkipStop(s.id())) lore.add(" &e通过不停车");
            List<String> nb = tm.neighborLabels(s.id(), line.id());
            lore.add(nb.isEmpty() ? " &8本线未连接邻站" : " &7连接: &f" + String.join("&7 → &f", nb));
            if (fromSt != null && !fromSt.id().equals(s.id())) {
                TransitManager.PathResult path = tm.shortest(fromSt.id(), s.id());
                lore.add(path.reachable()
                        ? " &e票价 " + fmt(path.fare()) + " &8· " + Math.max(0, path.hops().size() - 1) + " 段"
                        : " &c与起点无票价路径");
            }
            lore.add("");
            lore.add(s.id().equals(fromId) ? "&e▸ 已选为起点" : "&e▸ 点此查票价");
            String name = (s.id().equals(fromId) ? "&e" : "&f") + s.displayName() + "站";
            inv.setItem(slot++, ECOSTerminalGUI.item(stationIcon(s), name, lore));
            ids.add(s.id());
        }
        stationSlots.put(player.getUniqueId(), ids);

        if (p > 0) inv.setItem(SLOT_PREV, ECOSTerminalGUI.item(Material.ARROW, "&7上一线路", null));
        if (p < lines.size() - 1) inv.setItem(SLOT_NEXT, ECOSTerminalGUI.item(Material.ARROW, "&7下一线路", null));
        if (fromId != null)
            inv.setItem(SLOT_CLEAR, ECOSTerminalGUI.item(Material.BARRIER, "&7取消选站", null));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.BARRIER, "&7返回交通处", null));
        player.openInventory(inv);
    }

    public String clickStation(Player player, String stationId) {
        String cur = fareFrom.get(player.getUniqueId());
        TransitManager tm = plugin.getTransitManager();
        TransitManager.Station to = tm.getStation(stationId);
        String toName = to == null ? stationId : to.displayName();
        if (cur == null || cur.equals(stationId)) {
            fareFrom.put(player.getUniqueId(), stationId);
            return "&e已选起点 &f" + toName + " &7· 再点另一站查票价";
        }
        TransitManager.Station from = tm.getStation(cur);
        String fromName = from == null ? cur : from.displayName();
        TransitManager.PathResult path = tm.shortest(cur, stationId);
        fareFrom.put(player.getUniqueId(), stationId);
        if (!path.reachable()) {
            return "&c" + fromName + " → " + toName + " 无票价路径";
        }
        int hops = Math.max(0, path.hops().size() - 1);
        return "&e" + fromName + " → " + toName + " &f" + fmt(path.fare())
                + " &8· " + hops + " 段";
    }

    public void clearFare(Player player) { fareFrom.remove(player.getUniqueId()); }

    public String stationAt(Player player, int slot) {
        List<String> ids = stationSlots.get(player.getUniqueId());
        if (ids == null) return null;
        int i = slot - 9;
        if (i < 0 || i >= ids.size()) return null;
        return ids.get(i);
    }

    public static Material stationIcon(TransitManager.Station s) {
        int h = Math.abs(s.id().hashCode());
        return ICONS[h % ICONS.length];
    }

    public int currentPage(Player player) {
        return page.getOrDefault(player.getUniqueId(), 0);
    }

    public void cleanup(Player player) {
        page.remove(player.getUniqueId());
        stationSlots.remove(player.getUniqueId());
        fareFrom.remove(player.getUniqueId());
    }

    private String fmt(double v) {
        return plugin.getVaultHook().isEnabled() ? plugin.getVaultHook().format(v) : String.format("%.2f", v);
    }
}
