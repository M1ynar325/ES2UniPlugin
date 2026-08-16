package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

/** 单个车站：框选范围、闸机模式、绑定售票机、所属线路。 */
public class TransitStationGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l车站管理");
    public static final int SLOT_INFO = 4;
    public static final int SLOT_POS1 = 10;
    public static final int SLOT_POS2 = 11;
    public static final int SLOT_APPLY = 12;
    public static final int SLOT_MOVE = 13;
    public static final int SLOT_HINT = 14;
    public static final int SLOT_NAME_ZH = 15;
    public static final int SLOT_NAME_EN = 23;
    public static final int SLOT_TP = 16;
    public static final int SLOT_GATE_MODE = 19;
    public static final int SLOT_GATE = 20;
    public static final int SLOT_BIND_TVM = 21;
    public static final int SLOT_TVM = 22;
    public static final int SLOT_DELETE = 24;
    public static final int SLOT_ADJUST = 25;
    public static final int SLOT_BACK = 49;
    public static final int LINE_START = 27;
    public static final int LINE_END = 44;

    private static final String[] MODES = {"BOTH", "IN", "OUT"};

    private final ES2UniPlugin plugin;
    private final Map<UUID, String> stationOf = new HashMap<>();
    private final Map<UUID, List<String>> lineSlots = new HashMap<>();
    private final Map<UUID, String> gateMode = new HashMap<>();
    private final Set<UUID> armedDelete = new HashSet<>();

    public TransitStationGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player, String stationId) {
        TransitManager tm = plugin.getTransitManager();
        TransitManager.Station s = tm.getStation(stationId);
        if (s == null) {
            player.sendMessage(ColorUtil.colorize("&8[交通] &c车站不存在"));
            return;
        }
        stationOf.put(player.getUniqueId(), stationId);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.RED_STAINED_GLASS_PANE));

        Location c = s.center();
        String pos = c == null ? s.world() + " 未加载"
                : s.world() + " " + c.getBlockX() + " " + c.getBlockY() + " " + c.getBlockZ();
        List<String> nb = tm.neighborLabels(s.id(), null);
        inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.STONE_BRICKS,
                "&f" + s.displayName() + "站",
                List.of(" &7ID: &f" + s.id() + "  &8（建站用，须英文）",
                        " &7中文: &f" + s.displayName(),
                        " &7English: &f" + (s.nameEn() == null || s.nameEn().isBlank() ? "&8未设" : s.nameEn()),
                        " &7范围 &f" + tm.boxLabel(s),
                        " &7中心 &f" + pos,
                        " &7线路: &f" + tm.lineNamesOf(s),
                        nb.isEmpty() ? " &8无连接" : " &7邻站: &f" + String.join("&7, &f", nb),
                        "",
                        "&7立体站台：站在两个对角点框 XZ")));

        Location p1 = tm.boxCorner(player, 1);
        Location p2 = tm.boxCorner(player, 2);
        inv.setItem(SLOT_POS1, ECOSTerminalGUI.item(Material.LIME_WOOL, "&a角点1 · 脚下",
                List.of(p1 == null ? " &8未标记" : " &a" + fmtLoc(p1),
                        " &7站在车站范围的一个角",
                        "",
                        "&a▸ 记录脚下")));
        inv.setItem(SLOT_POS2, ECOSTerminalGUI.item(Material.ORANGE_WOOL, "&6角点2 · 脚下",
                List.of(p2 == null ? " &8未标记" : " &6" + fmtLoc(p2),
                        " &7站在对角（含上层/下层站台）",
                        "",
                        "&6▸ 记录脚下")));
        inv.setItem(SLOT_APPLY, ECOSTerminalGUI.item(Material.MAP, "&e应用框选范围",
                List.of(" &7用两个角点改本站 XZ",
                        " &7Y 会按你站的高低自动加厚",
                        "",
                        "&e▸ 应用")));
        inv.setItem(SLOT_MOVE, ECOSTerminalGUI.item(Material.ENDER_PEARL, "&7半径圈移到脚下",
                List.of(" &7不框选时的备用：当前半径 " + tm.stationRadius(s),
                        "",
                        "&7▸ 搬家")));
        inv.setItem(SLOT_TP, ECOSTerminalGUI.item(Material.ENDER_EYE, "&f传送到车站",
                List.of("&f▸ 传到范围中心")));
        inv.setItem(SLOT_NAME_ZH, ECOSTerminalGUI.item(Material.WRITABLE_BOOK, "&f改中文名",
                List.of(" &7当前: &f" + s.displayName(),
                        " &7聊天输入，支持中文",
                        " &7输入 cancel 取消",
                        "",
                        "&f▸ 改名")));
        inv.setItem(SLOT_NAME_EN, ECOSTerminalGUI.item(Material.NAME_TAG, "&fSet English name",
                List.of(" &7now: &f" + (s.nameEn() == null || s.nameEn().isBlank() ? "&8(none)" : s.nameEn()),
                        " &7ActionBar 会与中文轮流显示",
                        " &7type cancel to abort",
                        "",
                        "&f▸ rename")));

        String mode = gateMode.getOrDefault(player.getUniqueId(), "BOTH");
        String modeZh = switch (mode) {
            case "IN" -> "仅进站";
            case "OUT" -> "仅出站";
            default -> "进出均可";
        };
        inv.setItem(SLOT_GATE_MODE, ECOSTerminalGUI.item(Material.COMPARATOR, "&e闸机模式 &f" + modeZh,
                List.of(" &7BOTH = 没行程进、有行程出",
                        " &7IN / OUT = 单向闸口",
                        " &7牌子放下自动写字，旁边活板门会开合",
                        "",
                        "&e▸ 点击切换")));
        inv.setItem(SLOT_GATE, ECOSTerminalGUI.item(Material.OAK_SIGN, "&f拿闸机牌 &8[" + mode + "]",
                List.of(" &7放到出入口，自动显示站名",
                        " &7拆掉自动注销",
                        "",
                        "&f▸ 放入背包")));
        inv.setItem(SLOT_BIND_TVM, ECOSTerminalGUI.item(Material.ENDER_EYE, "&e绑定准星方块为售票机",
                List.of(" &7模组售票机也能绑",
                        " &7点这里后关上 GUI，右键看中的方块",
                        " &7之后玩家点它会打开本插件售票界面",
                        "",
                        "&e▸ 开始绑定")));
        inv.setItem(SLOT_TVM, ECOSTerminalGUI.item(Material.LODESTONE, "&f拿原版售票机",
                List.of(" &7磁石，可当备用",
                        " &7售票机也可办理补票",
                        "",
                        "&f▸ 放入背包")));
        inv.setItem(SLOT_ADJUST, ECOSTerminalGUI.item(Material.BIRCH_SIGN, "&6拿补票处牌",
                List.of(" &7放到付费区出口附近",
                        " &7无进站 / 无路径须先补票再出闸",
                        " &7拆掉自动注销",
                        "",
                        "&6▸ 放入背包")));

        boolean armed = armedDelete.contains(player.getUniqueId());
        inv.setItem(SLOT_DELETE, ECOSTerminalGUI.item(Material.BARRIER,
                armed ? "&c再点一次确认删除" : "&c删除车站",
                List.of(" &7连接 / 闸机 / 售票机 / 补票处一起拆",
                        armed ? " &c▸ 确认" : " &8▸ 点两次")));

        List<String> lids = new ArrayList<>();
        int slot = LINE_START;
        for (TransitManager.Line l : tm.allLines()) {
            if (slot > LINE_END) break;
            boolean on = s.lineIds().contains(l.id());
            List<String> lore = new ArrayList<>();
            lore.add(" &7" + l.id() + " · " + tm.typeName(l.type()));
            lore.add(on ? " &a本站已挂此线" : " &8本站未挂此线");
            lore.add(on ? " &c▸ 摘掉（并拆本线邻边）" : " &a▸ 挂上（再去连边）");
            inv.setItem(slot++, ECOSTerminalGUI.item(
                    on ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE,
                    l.color() + l.displayName(), lore));
            lids.add(l.id());
        }
        lineSlots.put(player.getUniqueId(), lids);
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回管理", null));
        player.openInventory(inv);
    }

    public String stationOf(Player player) { return stationOf.get(player.getUniqueId()); }

    public String lineAt(Player player, int slot) {
        List<String> ids = lineSlots.get(player.getUniqueId());
        if (ids == null || slot < LINE_START || slot > LINE_END) return null;
        int i = slot - LINE_START;
        if (i < 0 || i >= ids.size()) return null;
        return ids.get(i);
    }

    public String gateModeOf(Player player) {
        return gateMode.getOrDefault(player.getUniqueId(), "BOTH");
    }

    public String cycleGateMode(Player player) {
        String cur = gateModeOf(player);
        int i = 0;
        for (int k = 0; k < MODES.length; k++) if (MODES[k].equals(cur)) { i = k; break; }
        String next = MODES[(i + 1) % MODES.length];
        gateMode.put(player.getUniqueId(), next);
        return next;
    }

    public boolean isArmed(Player player) { return armedDelete.contains(player.getUniqueId()); }

    public void armDelete(Player player, boolean on) {
        if (on) armedDelete.add(player.getUniqueId());
        else armedDelete.remove(player.getUniqueId());
    }

    public void cleanup(Player player) {
        stationOf.remove(player.getUniqueId());
        lineSlots.remove(player.getUniqueId());
        armedDelete.remove(player.getUniqueId());
    }

    private static String fmtLoc(Location loc) {
        return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }
}
