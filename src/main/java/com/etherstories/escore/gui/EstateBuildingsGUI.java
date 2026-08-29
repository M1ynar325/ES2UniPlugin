package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.estate.BuildingCategory;
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

/** 房产：按建筑分类浏览。 */
public class EstateBuildingsGUI {

    public static final String TITLE = EcosStyle.hub("房产");
    public static final int SLOT_SALE = 45;
    public static final int SLOT_ALL = 46;
    public static final int SLOT_RES = 47;
    public static final int SLOT_PUB = 48;
    public static final int SLOT_COM = 49;
    public static final int SLOT_MINE = 50;
    public static final int SLOT_TOOLS = 51;
    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, BuildingCategory> filter = new HashMap<>();
    private final Map<UUID, List<String>> listed = new HashMap<>();

    public EstateBuildingsGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, filter.get(player.getUniqueId()));
    }

    public void open(Player player, BuildingCategory cat) {
        if (cat == null) filter.remove(player.getUniqueId());
        else filter.put(player.getUniqueId(), cat);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        ItemStack bg = EcosStyle.chrome();
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        List<String> names = plugin.getEstateManager().buildings(cat);
        listed.put(player.getUniqueId(), names);
        for (int i = 0; i < Math.min(names.size(), 45); i++) {
            String name = names.get(i);
            var rooms = plugin.getEstateManager().inBuilding(name);
            int vacant = plugin.getEstateManager().vacantCount(name);
            BuildingCategory c = plugin.getEstateManager().categoryOf(name);
            List<String> lore = new ArrayList<>();
            lore.add(" &7" + (cat != null ? cat.label : c.label)
                    + (plugin.getEstateManager().mixedCategory(name) ? " &8(混合)" : ""));
            lore.add(" &7房间: &f" + rooms.size() + "  &8空闲 &a" + vacant);
            lore.add("");
            lore.add("&a▸ 点击查看房间");
            inv.setItem(i, ECOSTerminalGUI.item(cat != null ? cat.icon : c.icon, "&f" + name, lore));
        }
        if (names.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7这一类还没有楼",
                    List.of("&8点下方「登记房间」",
                            "&8自建住宅收注册费")));
        }

        int onSale = plugin.getEstateManager().listedForSale().size();
        ItemStack sale = ECOSTerminalGUI.item(Material.GOLD_INGOT, "&e买房 · 待售",
                List.of(" &7挂牌: &f" + onSale, "", "&e▸ 打开"));
        if (onSale > 0) sale = ECOSTerminalGUI.glint(sale);
        inv.setItem(SLOT_SALE, sale);
        inv.setItem(SLOT_ALL, tab(Material.MAP, "全部", cat == null));
        inv.setItem(SLOT_RES, tab(BuildingCategory.RESIDENTIAL.icon, "住宅", cat == BuildingCategory.RESIDENTIAL));
        inv.setItem(SLOT_PUB, tab(BuildingCategory.PUBLIC.icon, "公共建筑", cat == BuildingCategory.PUBLIC));
        inv.setItem(SLOT_COM, tab(BuildingCategory.COMMERCIAL.icon, "商业建筑", cat == BuildingCategory.COMMERCIAL));
        inv.setItem(SLOT_MINE, ECOSTerminalGUI.item(Material.OAK_DOOR, "&e我的房产",
                List.of(" &7" + plugin.getEstateManager().ownedBy(player.getUniqueId()).size() + " 间",
                        "", "&e▸ 点击打开")));
        inv.setItem(SLOT_TOOLS, ECOSTerminalGUI.item(Material.OAK_SIGN, "&a登记房间",
                List.of("&8①选区棒  ②对角  ③写门牌", "", "&a▸ 打开")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    private ItemStack tab(Material mat, String name, boolean on) {
        return ECOSTerminalGUI.item(on ? mat : Material.GRAY_DYE, (on ? "&a" : "&7") + name,
                List.of(on ? "&8当前筛选" : "&8点击筛选"));
    }

    public String buildingAt(Player p, int slot) {
        List<String> names = listed.get(p.getUniqueId());
        if (names == null || slot < 0 || slot >= names.size() || slot >= 45) return null;
        return names.get(slot);
    }

    public void cleanup(Player p) {
        listed.remove(p.getUniqueId());
        filter.remove(p.getUniqueId());
    }
}
