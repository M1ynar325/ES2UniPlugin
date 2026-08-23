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

/** 挂牌待售：买房入口。 */
public class EstateSaleGUI {

    public static final String TITLE = ColorUtil.colorize("&e&l买房 · 待售");
    public static final int SLOT_BUILDINGS = 18;
    public static final int SLOT_MINE = 19;
    public static final int SLOT_TOOLS = 20;
    public static final int SLOT_HELP = 21;
    public static final int SLOT_HERE = 22;
    public static final int SLOT_CLOSE = 26;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<EstateUnit>> listed = new HashMap<>();

    public EstateSaleGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<EstateUnit> rooms = plugin.getEstateManager().listedForSale();
        listed.put(player.getUniqueId(), rooms);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        for (int i = 0; i < Math.min(rooms.size(), 18); i++) {
            EstateUnit u = rooms.get(i);
            boolean mine = player.getUniqueId().equals(u.owner());
            List<String> lore = new ArrayList<>();
            lore.add(" &f" + u.address());
            lore.add(" &7" + u.kind().label + " · " + u.category().label);
            lore.add(" &e" + price(u.price()));
            lore.add(u.vacant() ? " &a空闲挂牌" : " &7房主 &f" + (u.ownerName() == null ? "?" : u.ownerName()));
            lore.add("");
            if (mine) {
                lore.add("&8这是你挂的");
                lore.add("&a左键: 传到门口");
            } else {
                lore.add("&a左键: 看房（传到门口）");
                lore.add("&e右键: 购买（再点一次确认）");
            }
            ItemStack it = ECOSTerminalGUI.item(Material.GOLD_INGOT, "&e" + u.address(), lore);
            inv.setItem(i, it);
        }
        if (rooms.isEmpty()) {
            inv.setItem(4, ECOSTerminalGUI.item(Material.BARRIER, "&7现在没有挂牌房",
                    List.of("&8等管理预制或房主上架", "&8楼盘页可浏览全部房间")));
        }
        inv.setItem(SLOT_BUILDINGS, ECOSTerminalGUI.item(Material.DARK_OAK_DOOR, "&2楼盘",
                List.of("&8全部楼 / 房间", "", "&2▸ 打开")));
        inv.setItem(SLOT_MINE, ECOSTerminalGUI.item(Material.OAK_DOOR, "&e我的房产",
                List.of(" &7" + plugin.getEstateManager().ownedBy(player.getUniqueId()).size() + " 间", "", "&e▸ 打开")));
        inv.setItem(SLOT_TOOLS, ECOSTerminalGUI.item(Material.OAK_SIGN, "&a登记 / 门口",
                List.of("&8点1 · 点2 · 登记 · 牌子", "", "&a▸ 打开")));
        inv.setItem(SLOT_HELP, ECOSTerminalGUI.item(Material.WRITTEN_BOOK, "&f房产指南",
                List.of("&8怎么注册、怎么买", "", "&f▸ 打开")));
        inv.setItem(SLOT_HERE, ECOSTerminalGUI.item(Material.COMPASS, "&7当前门牌",
                List.of("&8站在房间里点", "", "&7▸ 查看")));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    private String price(double v) {
        if (plugin.getVaultHook().isEnabled()) return plugin.getVaultHook().format(v);
        return String.format("%.0f", v);
    }

    public EstateUnit unitAt(Player p, int slot) {
        List<EstateUnit> list = listed.get(p.getUniqueId());
        if (list == null || slot < 0 || slot >= list.size() || slot >= 18) return null;
        return list.get(slot);
    }

    public void cleanup(Player p) {
        listed.remove(p.getUniqueId());
    }
}
