package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** 到站 / 发车铃：每站一次，不循环 */
public class TransitAnnounceGUI {

    public static final String TITLE = ColorUtil.colorize("&c&l到站 / 发车铃");
    public static final int SLOT_ARRIVE_TOGGLE = 10;
    public static final int SLOT_ARRIVE_PREVIEW = 11;
    public static final int SLOT_STACK = 13;
    public static final int SLOT_DEPART_TOGGLE = 15;
    public static final int SLOT_DEPART_PREVIEW = 16;

    public static final int SLOT_ARRIVE_JR = 19;
    public static final int SLOT_ARRIVE_YAMANOTE = 20;
    public static final int SLOT_ARRIVE_MTR = 21;
    public static final int SLOT_ARRIVE_TUBE = 22;
    public static final int SLOT_ARRIVE_OSAKA = 23;
    public static final int SLOT_ARRIVE_CHIME = 24;

    public static final int SLOT_ARRIVE_CR = 28;
    public static final int SLOT_ARRIVE_CRH = 29;
    public static final int SLOT_ARRIVE_BEIJING = 30;
    public static final int SLOT_ARRIVE_SHANGHAI = 31;
    public static final int SLOT_ARRIVE_GUANGZHOU = 32;

    public static final int SLOT_DEPART_JRGO = 37;
    public static final int SLOT_DEPART_DOORS = 38;
    public static final int SLOT_DEPART_WHISTLE = 39;
    public static final int SLOT_DEPART_SHINKANSEN = 40;
    public static final int SLOT_DEPART_CRGO = 41;
    public static final int SLOT_DEPART_CRHGO = 42;
    public static final int SLOT_BACK = 49;

    private final ES2UniPlugin plugin;

    public TransitAnnounceGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.RED_STAINED_GLASS_PANE));

        boolean arriveOn = plugin.getConfig().getBoolean("transit.announce.enabled", true);
        boolean departOn = plugin.getConfig().getBoolean("transit.announce.depart-enabled", true);
        boolean stack = plugin.getConfig().getBoolean("transit.announce.stack-repeats", true);
        String arrive = plugin.getConfig().getString("transit.announce.preset", "jr");
        String depart = plugin.getConfig().getString("transit.announce.depart-preset", "jrgo");

        inv.setItem(4, ECOSTerminalGUI.item(Material.NOTE_BLOCK, "&c到站 / 发车铃",
                List.of(" &7到站：离开起点后，在新站停留约 2 秒播一次",
                        " &7路过、站偏、框外不响；本行程每站只响一次",
                        " &7发车：进站后在起点站停留约 12 秒再离开才响",
                        " &7刷完立刻跑出站不响")));

        inv.setItem(SLOT_ARRIVE_TOGGLE, ECOSTerminalGUI.item(
                arriveOn ? Material.LIME_DYE : Material.GRAY_DYE,
                arriveOn ? "&a到站铃已开启" : "&7到站铃已关闭",
                List.of("&a▸ 点击开关")));
        inv.setItem(SLOT_ARRIVE_PREVIEW, ECOSTerminalGUI.item(Material.JUKEBOX, "&a试听到站铃",
                List.of(" &7" + arrive, "", "&a▸ 播放")));
        inv.setItem(SLOT_DEPART_TOGGLE, ECOSTerminalGUI.item(
                departOn ? Material.LIME_DYE : Material.GRAY_DYE,
                departOn ? "&a发车铃已开启" : "&7发车铃已关闭",
                List.of("&a▸ 点击开关")));
        inv.setItem(SLOT_DEPART_PREVIEW, ECOSTerminalGUI.item(Material.JUKEBOX, "&a试听发车铃",
                List.of(" &7" + depart, "", "&a▸ 播放")));
        inv.setItem(SLOT_STACK, ECOSTerminalGUI.item(
                stack ? Material.LIME_DYE : Material.GRAY_DYE,
                stack ? "&a和声加厚" : "&7单音旋律",
                List.of(" &7音符盒「音符」",
                        stack ? " &a每拍叠低八度" : " &7只有单音",
                        "",
                        "&a▸ 点击切换")));

        inv.setItem(SLOT_ARRIVE_JR, presetItem("jr", "JR 到站", Material.NOTE_BLOCK, arrive));
        inv.setItem(SLOT_ARRIVE_YAMANOTE, presetItem("yamanote", "山手线", Material.GOLD_NUGGET, arrive));
        inv.setItem(SLOT_ARRIVE_MTR, presetItem("mtr", "港铁", Material.IRON_NUGGET, arrive));
        inv.setItem(SLOT_ARRIVE_TUBE, presetItem("tube", "伦敦地铁", Material.BELL, arrive));
        inv.setItem(SLOT_ARRIVE_OSAKA, presetItem("osaka", "大阪地铁", Material.AMETHYST_SHARD, arrive));
        inv.setItem(SLOT_ARRIVE_CHIME, presetItem("chime", "下行琶音", Material.GOLD_NUGGET, arrive));

        inv.setItem(27, ECOSTerminalGUI.item(Material.POWERED_RAIL, "&e国铁 / 地铁",
                List.of(" &7CR 客站、动车、北上广")));
        inv.setItem(SLOT_ARRIVE_CR, presetItem("cr", "国铁到站", Material.POWERED_RAIL, arrive));
        inv.setItem(SLOT_ARRIVE_CRH, presetItem("crh", "动车到站", Material.MINECART, arrive));
        inv.setItem(SLOT_ARRIVE_BEIJING, presetItem("beijing", "北京地铁", Material.RED_CONCRETE, arrive));
        inv.setItem(SLOT_ARRIVE_SHANGHAI, presetItem("shanghai", "上海地铁", Material.WHITE_CONCRETE, arrive));
        inv.setItem(SLOT_ARRIVE_GUANGZHOU, presetItem("guangzhou", "广州地铁", Material.MAGENTA_CONCRETE, arrive));

        inv.setItem(36, ECOSTerminalGUI.item(Material.HOPPER, "&e发车铃",
                List.of(" &7须在起点站停留后再离开")));
        inv.setItem(SLOT_DEPART_JRGO, presetItem("jrgo", "JR 发车", Material.NOTE_BLOCK, depart));
        inv.setItem(SLOT_DEPART_DOORS, presetItem("doors", "车门关闭", Material.IRON_DOOR, depart));
        inv.setItem(SLOT_DEPART_WHISTLE, presetItem("whistle", "汽笛", Material.GOAT_HORN, depart));
        inv.setItem(SLOT_DEPART_SHINKANSEN, presetItem("shinkansen", "新干线", Material.POWERED_RAIL, depart));
        inv.setItem(SLOT_DEPART_CRGO, presetItem("crgo", "国铁发车", Material.BELL, depart));
        inv.setItem(SLOT_DEPART_CRHGO, presetItem("crhgo", "动车发车", Material.MINECART, depart));

        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回管理", null));
        player.openInventory(inv);
    }

    private org.bukkit.inventory.ItemStack presetItem(String id, String name, Material mat, String current) {
        boolean on = id.equalsIgnoreCase(current);
        return ECOSTerminalGUI.item(mat, (on ? "&a" : "&7") + name,
                List.of(" &7" + id, on ? " &a当前使用" : " &a▸ 选用并试听"));
    }

    public static String arrivePresetAt(int slot) {
        return switch (slot) {
            case SLOT_ARRIVE_JR -> "jr";
            case SLOT_ARRIVE_YAMANOTE -> "yamanote";
            case SLOT_ARRIVE_MTR -> "mtr";
            case SLOT_ARRIVE_TUBE -> "tube";
            case SLOT_ARRIVE_OSAKA -> "osaka";
            case SLOT_ARRIVE_CHIME -> "chime";
            case SLOT_ARRIVE_CR -> "cr";
            case SLOT_ARRIVE_CRH -> "crh";
            case SLOT_ARRIVE_BEIJING -> "beijing";
            case SLOT_ARRIVE_SHANGHAI -> "shanghai";
            case SLOT_ARRIVE_GUANGZHOU -> "guangzhou";
            default -> null;
        };
    }

    public static String departPresetAt(int slot) {
        return switch (slot) {
            case SLOT_DEPART_JRGO -> "jrgo";
            case SLOT_DEPART_DOORS -> "doors";
            case SLOT_DEPART_WHISTLE -> "whistle";
            case SLOT_DEPART_SHINKANSEN -> "shinkansen";
            case SLOT_DEPART_CRGO -> "crgo";
            case SLOT_DEPART_CRHGO -> "crhgo";
            default -> null;
        };
    }

    public void cleanup(Player player) {}
}
