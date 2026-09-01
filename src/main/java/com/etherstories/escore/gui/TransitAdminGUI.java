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

public class TransitAdminGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l交通管理");
    public static final int SLOT_BACK = 49;
    public static final int SLOT_PREV = 46;
    public static final int SLOT_NEXT = 52;
    public static final int SLOT_TYPES = 45;
    public static final int SLOT_ANNOUNCE = 47;
    public static final int SLOT_RIDERS = 8;
    public static final int SLOT_CLAIMS = 53;
    public static final int LINE_START = 9;
    public static final int LINE_END = 26;
    public static final int STATION_START = 27;
    public static final int STATION_END = 44;
    public static final int STATIONS_PER = STATION_END - STATION_START + 1;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<String>> lineSlots = new HashMap<>();
    private final Map<UUID, List<String>> stationSlots = new HashMap<>();
    private final Map<UUID, Integer> pageOf = new HashMap<>();

    public TransitAdminGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        open(player, pageOf.getOrDefault(player.getUniqueId(), 0));
    }

    public void open(Player player, int page) {
        Inventory inv = EcosHolder.of("transit-admin", 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.RED_STAINED_GLASS_PANE));

        TransitManager tm = plugin.getTransitManager();
        inv.setItem(4, ECOSTerminalGUI.item(Material.COMMAND_BLOCK, "&c轨道交通管理",
                List.of(" &7线路 " + tm.allLines().size() + " · 车站 " + tm.allStations().size(),
                        " &7连接 " + tm.allEdges().size(),
                        "",
                        "&a▸ 点线路: 连边 / 删线",
                        "&a▸ 点车站: 搬家 / 所属线 / 删除",
                        "&c▸ 右上角: 在乘名单")));

        List<String> lineIds = new ArrayList<>();
        int slot = LINE_START;
        for (TransitManager.Line l : tm.allLines()) {
            if (slot > LINE_END) break;
            int n = tm.edgesOfLine(l.id()).size();
            int stops = 0;
            for (TransitManager.Station s : tm.allStations()) {
                if (s.lineIds().contains(l.id())) stops++;
            }
            inv.setItem(slot++, ECOSTerminalGUI.item(Material.POWERED_RAIL,
                    l.color() + l.displayName(),
                    List.of(" &7" + l.id() + " · " + l.type(),
                            " &7hop " + l.hopFare() + " / max " + l.maxFare(),
                            " &7车站 &f" + stops + " &7· 连接段 &f" + n,
                            "",
                            "&a▸ 编辑连接 / 删除线路")));
            lineIds.add(l.id());
        }
        lineSlots.put(player.getUniqueId(), lineIds);

        List<TransitManager.Station> all = new ArrayList<>(tm.allStations());
        int maxPage = Math.max(0, (all.size() - 1) / STATIONS_PER);
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;
        pageOf.put(player.getUniqueId(), page);

        List<String> stIds = new ArrayList<>();
        int from = page * STATIONS_PER;
        slot = STATION_START;
        for (int i = from; i < all.size() && slot <= STATION_END; i++) {
            TransitManager.Station s = all.get(i);
            List<String> lore = new ArrayList<>();
            lore.add(" &7" + s.id() + " · 半径 " + tm.stationRadius(s));
            lore.add(" &7" + tm.lineNamesOf(s));
            List<String> nb = tm.neighborLabels(s.id(), null);
            lore.add(nb.isEmpty() ? " &8无连接" : " &7邻站: &f" + String.join("&7, &f", nb));
            lore.add("");
            lore.add("&a▸ 管理此站");
            inv.setItem(slot++, ECOSTerminalGUI.item(TransitMapGUI.stationIcon(s), "&f" + s.displayName() + "站", lore));
            stIds.add(s.id());
        }
        stationSlots.put(player.getUniqueId(), stIds);

        if (page > 0)
            inv.setItem(SLOT_PREV, ECOSTerminalGUI.item(Material.ARROW, "&7上一页车站", List.of(" &7" + (page + 1) + " / " + (maxPage + 1))));
        if (page < maxPage)
            inv.setItem(SLOT_NEXT, ECOSTerminalGUI.item(Material.ARROW, "&7下一页车站", List.of(" &7" + (page + 1) + " / " + (maxPage + 1))));

        inv.setItem(SLOT_TYPES, ECOSTerminalGUI.item(Material.NAME_TAG, "&c线路类型",
                List.of(" &7自定义快线/磁悬浮等", "", "&c▸ 打开")));
        inv.setItem(SLOT_ANNOUNCE, ECOSTerminalGUI.item(Material.NOTE_BLOCK, "&c到站 / 发车铃",
                List.of(" &7每站一次，不循环", " &7可试听", "", "&c▸ 打开")));
        int riding = tm.activeJourneys().size();
        inv.setItem(SLOT_RIDERS, ECOSTerminalGUI.item(Material.MINECART,
                "&c在乘名单" + (riding > 0 ? " &e" + riding : ""),
                List.of(" &7当前在乘 " + riding + " 人", " &7可清除卡住的行程", "", "&c▸ 打开")));
        int pending = tm.pendingClaims().size();
        inv.setItem(SLOT_CLAIMS, ECOSTerminalGUI.item(Material.SHIELD,
                "&c行程异常申报" + (pending > 0 ? " &e" + pending : ""),
                List.of(" &7待审 " + pending, "", "&c▸ 审核")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回交通处", null));
        EcosHolder.open(player, inv);
    }

    public String lineAt(Player player, int slot) {
        List<String> ids = lineSlots.get(player.getUniqueId());
        if (ids == null || slot < LINE_START || slot > LINE_END) return null;
        int i = slot - LINE_START;
        if (i < 0 || i >= ids.size()) return null;
        return ids.get(i);
    }

    public String stationAt(Player player, int slot) {
        List<String> ids = stationSlots.get(player.getUniqueId());
        if (ids == null || slot < STATION_START || slot > STATION_END) return null;
        int i = slot - STATION_START;
        if (i < 0 || i >= ids.size()) return null;
        return ids.get(i);
    }

    public int pageOf(Player player) { return pageOf.getOrDefault(player.getUniqueId(), 0); }

    public void cleanup(Player player) {
        lineSlots.remove(player.getUniqueId());
        stationSlots.remove(player.getUniqueId());
        pageOf.remove(player.getUniqueId());
    }
}
