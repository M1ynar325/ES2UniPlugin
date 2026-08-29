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

/** 买单程票：起点 → 终点 → 席别（仅多种席别时） */
public class TransitTicketGUI {

    public static final String TITLE = ColorUtil.colorize("&f&l购买单程票");
    public static final int SLOT_BACK = 49;

    private final ES2UniPlugin plugin;
    private final Map<UUID, String> origin = new HashMap<>();
    private final Map<UUID, String> dest = new HashMap<>();
    private final Map<UUID, Boolean> cabinPick = new HashMap<>();
    private final Map<UUID, List<String>> slotIds = new HashMap<>();

    public TransitTicketGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player, String presetOrigin) {
        dest.remove(player.getUniqueId());
        cabinPick.remove(player.getUniqueId());
        if (presetOrigin != null && plugin.getTransitManager().getStation(presetOrigin) != null) {
            origin.put(player.getUniqueId(), presetOrigin);
            openDest(player);
            return;
        }
        origin.remove(player.getUniqueId());
        openOrigin(player);
    }

    public void openOrigin(Player player) {
        dest.remove(player.getUniqueId());
        cabinPick.remove(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.WHITE_STAINED_GLASS_PANE));
        inv.setItem(4, ECOSTerminalGUI.item(Material.COMPASS, "&f选择起点", List.of("&7点击车站")));
        fillStations(player, inv, null);
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回", null));
        player.openInventory(inv);
    }

    public void openDest(Player player) {
        dest.remove(player.getUniqueId());
        cabinPick.remove(player.getUniqueId());
        String from = origin.get(player.getUniqueId());
        TransitManager.Station fs = plugin.getTransitManager().getStation(from);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.WHITE_STAINED_GLASS_PANE));
        inv.setItem(4, ECOSTerminalGUI.item(Material.PAPER,
                "&f起点: " + (fs == null ? from : fs.displayName()),
                List.of("&7再点终点买单程票")));
        fillStations(player, inv, from);
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7重选起点", null));
        player.openInventory(inv);
    }

    private void fillStations(Player player, Inventory inv, String exclude) {
        List<String> ids = new ArrayList<>();
        int slot = 9;
        TransitManager tm = plugin.getTransitManager();
        String from = origin.get(player.getUniqueId());
        for (TransitManager.Station s : tm.allStations()) {
            if (exclude != null && s.id().equals(exclude)) continue;
            if (slot >= 45) break;
            List<String> lore = new ArrayList<>();
            lore.add(" &7ID: &f" + s.id());
            if (from != null && exclude != null) {
                double fare = tm.quoteTicketFare(from, s.id(), TransitManager.Cabin.STD);
                lore.add(fare >= 0
                        ? " &7票价: &e" + fmt(fare) + (tm.allCabins().size() > 1 ? " &8（普通）" : "")
                        : " &c无法直达");
            }
            lore.add("");
            lore.add("&f▸ 选择");
            inv.setItem(slot, ECOSTerminalGUI.item(Material.PAPER, "&f" + s.displayName() + "站", lore));
            ids.add(s.id());
            slot++;
        }
        slotIds.put(player.getUniqueId(), ids);
    }

    public String originOf(Player player) { return origin.get(player.getUniqueId()); }
    public String destOf(Player player) { return dest.get(player.getUniqueId()); }
    public boolean pickingCabin(Player player) { return Boolean.TRUE.equals(cabinPick.get(player.getUniqueId())); }

    public void openCabin(Player player) {
        String from = origin.get(player.getUniqueId());
        String to = dest.get(player.getUniqueId());
        TransitManager tm = plugin.getTransitManager();
        cabinPick.put(player.getUniqueId(), true);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.WHITE_STAINED_GLASS_PANE));
        inv.setItem(4, ECOSTerminalGUI.item(Material.NAME_TAG, "&f选择席别",
                List.of("&7" + tm.stationName(from) + " → " + tm.stationName(to),
                        "&7进出须走对应席别闸机")));
        List<String> ids = new ArrayList<>();
        int slot = 9;
        for (TransitManager.Cabin c : tm.allCabins()) {
            if (slot >= 45) break;
            double fare = tm.quoteTicketFare(from, to, c.id());
            List<String> lore = new ArrayList<>();
            lore.add(" &7ID: &f" + c.id());
            lore.add(" &7倍率: &f×" + c.fareMul());
            lore.add(fare >= 0 ? " &7票价: &e" + fmt(fare) : " &c无法直达");
            lore.add("");
            lore.add("&f▸ 购买");
            inv.setItem(slot++, ECOSTerminalGUI.item(Material.PAPER, "&f" + c.displayName(), lore));
            ids.add(c.id());
        }
        slotIds.put(player.getUniqueId(), ids);
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7重选终点", null));
        player.openInventory(inv);
    }

    public String stationAt(Player player, int slot) {
        if (pickingCabin(player)) return null;
        List<String> ids = slotIds.get(player.getUniqueId());
        if (ids == null) return null;
        int idx = slot - 9;
        if (idx < 0 || idx >= ids.size()) return null;
        return ids.get(idx);
    }

    public String cabinAt(Player player, int slot) {
        if (!pickingCabin(player)) return null;
        List<String> ids = slotIds.get(player.getUniqueId());
        if (ids == null) return null;
        int idx = slot - 9;
        if (idx < 0 || idx >= ids.size()) return null;
        return ids.get(idx);
    }

    public void setOrigin(Player player, String id) { origin.put(player.getUniqueId(), id); }
    public void setDest(Player player, String id) { dest.put(player.getUniqueId(), id); }

    public void cleanup(Player player) {
        origin.remove(player.getUniqueId());
        dest.remove(player.getUniqueId());
        cabinPick.remove(player.getUniqueId());
        slotIds.remove(player.getUniqueId());
    }

    private String fmt(double v) {
        return plugin.getVaultHook().isEnabled() ? plugin.getVaultHook().format(v) : String.format("%.2f", v);
    }
}
