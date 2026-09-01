package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.RecycleBinManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RecycleBinGUI {

    public static final String TITLE = ColorUtil.colorize("&2&l回收站");

    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<String>> slotIds = new HashMap<>();

    public RecycleBinGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = EcosHolder.of("recycle", 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.GREEN_STAINED_GLASS_PANE));

        List<RecycleBinManager.Entry> list = plugin.getRecycleBinManager().listFor(player);
        List<String> ids = new ArrayList<>();
        int slot = 0;
        for (RecycleBinManager.Entry e : list) {
            if (slot >= 45) break;
            ids.add(e.id);
            ItemStack display = e.stack.clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = new ArrayList<>();
            if (meta != null && meta.hasLore() && meta.getLore() != null)
                lore.addAll(meta.getLore());
            lore.add(ColorUtil.colorize("&8────────"));
            lore.add(ColorUtil.colorize("&7归属: &f" + e.ownerName));
            lore.add(ColorUtil.colorize("&7入库: &f" + e.storedLabel()));
            lore.add(ColorUtil.colorize("&7&o" + e.expireLabel()));
            lore.add(ColorUtil.colorize("&8清理 #" + e.cleanupId
                    + " @ " + e.world + " " + e.x + " " + e.y + " " + e.z));
            lore.add("");
            lore.add(ColorUtil.colorize("&a▸ 点击取回"));
            if (meta != null) {
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(slot, display);
            slot++;
        }
        slotIds.put(player.getUniqueId(), ids);

        if (list.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.STRUCTURE_VOID,
                    "&7回收站为空",
                    List.of("&7清理掉落物后会暂存此处",
                            "&7保存约 1 天")));
        }

        inv.setItem(45, ECOSTerminalGUI.item(Material.HOPPER,
                "&2回收站说明",
                List.of(" &7城管清理的掉落物会进入回收站",
                        " &7仅可取回自己的物品（管理可见全部）",
                        " &7时效: &f"
                                + plugin.getConfig().getLong("municipal.cleanup.recyclebin-hours", 24)
                                + " 小时")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回管理处", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }

    public String getId(Player player, int slot) {
        List<String> ids = slotIds.get(player.getUniqueId());
        if (ids == null || slot < 0 || slot >= ids.size()) return null;
        return ids.get(slot);
    }

    public void cleanup(Player player) { slotIds.remove(player.getUniqueId()); }
}
