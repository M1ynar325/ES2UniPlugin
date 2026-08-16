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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 某条线路的站间连接：点车站 A 再点 B 加边；点已有连接删除。
 */
public class TransitEdgesGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l线路连接");
    public static final int SLOT_BACK = 49;
    public static final int SLOT_DELETE = 45;
    public static final int SLOT_INFO = 4;
    public static final int EDGE_START = 9;
    public static final int EDGE_END = 35;
    public static final int STATION_START = 36;
    public static final int STATION_END = 44;

    private final ES2UniPlugin plugin;
    private final Map<UUID, String> lineOf = new HashMap<>();
    private final Map<UUID, String> pendingFrom = new HashMap<>();
    private final Map<UUID, List<TransitManager.Edge>> edgeSlots = new HashMap<>();
    private final Map<UUID, List<String>> stationSlots = new HashMap<>();
    private final Set<UUID> armedDelete = new HashSet<>();

    public TransitEdgesGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player, String lineId) {
        TransitManager tm = plugin.getTransitManager();
        TransitManager.Line line = tm.getLine(lineId);
        if (line == null) {
            player.sendMessage(ColorUtil.colorize("&8[交通] &c线路不存在"));
            return;
        }
        lineOf.put(player.getUniqueId(), lineId);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.RED_STAINED_GLASS_PANE));

        String pending = pendingFrom.get(player.getUniqueId());
        inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.POWERED_RAIL,
                line.color() + line.displayName() + " &8连接",
                List.of(" &7ID: &f" + line.id(),
                        " &7每站 &e" + line.hopFare() + " &7全程上限 &e" + line.maxFare(),
                        pending == null ? " &a先点下方车站，再点另一站来连接"
                                : " &e已选起点: &f" + tm.stationName(pending) + " &8再点终点",
                        " &c点上方铁轨可断开该段")));

        List<TransitManager.Edge> edges = tm.edgesOfLine(lineId);
        edgeSlots.put(player.getUniqueId(), edges);
        int slot = EDGE_START;
        for (TransitManager.Edge e : edges) {
            if (slot > EDGE_END) break;
            double fare = e.fare() >= 0 ? e.fare() : line.hopFare();
            inv.setItem(slot++, ECOSTerminalGUI.item(Material.RAIL,
                    "&f" + tm.stationName(e.from()) + " &8↔ &f" + tm.stationName(e.to()),
                    List.of(" &7" + e.from() + " ↔ " + e.to(),
                            " &7票价: &e" + fare,
                            "",
                            "&c▸ 点击断开")));
        }

        List<String> stations = new ArrayList<>();
        for (TransitManager.Station s : tm.allStations()) {
            if (s.lineIds().contains(lineId)) stations.add(s.id());
        }
        for (TransitManager.Station s : tm.allStations()) {
            if (!stations.contains(s.id())) stations.add(s.id());
        }
        stationSlots.put(player.getUniqueId(), stations);
        slot = STATION_START;
        for (String sid : stations) {
            if (slot > STATION_END) break;
            TransitManager.Station s = tm.getStation(sid);
            List<String> lore = new ArrayList<>();
            lore.add(" &7ID: &f" + sid);
            List<String> nb = tm.neighborLabels(sid, lineId);
            lore.add(nb.isEmpty() ? " &8本线暂无邻站" : " &7邻站: &f" + String.join("&7, &f", nb));
            lore.add(sid.equals(pending) ? " &a已选为起点" : " &a▸ 点选连接");
            inv.setItem(slot++, ECOSTerminalGUI.item(
                    sid.equals(pending) ? Material.LIME_CONCRETE : Material.STONE_BRICKS,
                    "&f" + tm.stationName(sid) + "站", lore));
        }

        inv.setItem(SLOT_DELETE, ECOSTerminalGUI.item(Material.BARRIER,
                armedDelete.contains(player.getUniqueId()) ? "&c再点一次确认删线" : "&c删除本线",
                List.of(" &7拆掉本线全部连接",
                        " &7车站上的所属线也会摘掉",
                        armedDelete.contains(player.getUniqueId()) ? " &c▸ 确认删除" : " &8▸ 点两次删除")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回管理", null));
        player.openInventory(inv);
    }

    public String lineOf(Player player) { return lineOf.get(player.getUniqueId()); }

    public TransitManager.Edge edgeAt(Player player, int slot) {
        List<TransitManager.Edge> list = edgeSlots.get(player.getUniqueId());
        if (list == null || slot < EDGE_START || slot > EDGE_END) return null;
        int i = slot - EDGE_START;
        if (i < 0 || i >= list.size()) return null;
        return list.get(i);
    }

    public String stationAt(Player player, int slot) {
        List<String> list = stationSlots.get(player.getUniqueId());
        if (list == null || slot < STATION_START || slot > STATION_END) return null;
        int i = slot - STATION_START;
        if (i < 0 || i >= list.size()) return null;
        return list.get(i);
    }

    public String pendingFrom(Player player) { return pendingFrom.get(player.getUniqueId()); }

    public void setPendingFrom(Player player, String id) {
        if (id == null) pendingFrom.remove(player.getUniqueId());
        else pendingFrom.put(player.getUniqueId(), id);
    }

    public boolean isArmed(Player player) { return armedDelete.contains(player.getUniqueId()); }

    public void armDelete(Player player, boolean on) {
        if (on) armedDelete.add(player.getUniqueId());
        else armedDelete.remove(player.getUniqueId());
    }

    public void cleanup(Player player) {
        lineOf.remove(player.getUniqueId());
        pendingFrom.remove(player.getUniqueId());
        edgeSlots.remove(player.getUniqueId());
        stationSlots.remove(player.getUniqueId());
        armedDelete.remove(player.getUniqueId());
    }
}
