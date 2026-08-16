package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * 添加好友：点在线头颅 或 聊天输入名字。
 */
public class AddFriendGUI {

    public static final String TITLE = ColorUtil.colorize("&8添加好友");

    public static final int SLOT_CHAT  = 49;
    public static final int SLOT_BACK  = 45;
    public static final int SLOT_CLOSE = 53;

    private static final int SIZE = 54;
    private static final int LIST = 45;

    private final ES2UniPlugin plugin;
    private final Map<UUID, Map<Integer, UUID>> slotMaps = new HashMap<>();

    public AddFriendGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack bg = ECOSTerminalGUI.bg(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = LIST; i < SIZE; i++) inv.setItem(i, bg);

        Map<Integer, UUID> map = new HashMap<>();
        int slot = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(viewer) || slot >= LIST) continue;
            if (plugin.getFriendManager().areFriends(viewer.getUniqueId(), p.getUniqueId())) continue;
            inv.setItem(slot, head(p));
            map.put(slot, p.getUniqueId());
            slot++;
        }
        if (slot == 0) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER, "&7暂无可添加的在线玩家",
                    List.of("&7已是好友的不会显示", "&7也可下方用聊天输入名字")));
        }

        slotMaps.put(viewer.getUniqueId(), map);
        inv.setItem(SLOT_CHAT, ECOSTerminalGUI.item(Material.NAME_TAG, "&a聊天输入名字",
                List.of("&7适合不在当前列表 / 刚上线的玩家",
                        "&7点击后在聊天栏输入游戏名",
                        "&8cancel 取消")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回好友", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        viewer.openInventory(inv);
    }

    private ItemStack head(Player target) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.setDisplayName(ColorUtil.colorize("&f" + target.getName()));
            meta.setLore(List.of(
                    ColorUtil.colorize("&7延迟 &f" + target.getPing() + "ms"),
                    ColorUtil.colorize(""),
                    ColorUtil.colorize("&a▸ 点击发送好友请求")));
            skull.setItemMeta(meta);
        }
        return skull;
    }

    public UUID getAt(Player viewer, int slot) {
        Map<Integer, UUID> m = slotMaps.get(viewer.getUniqueId());
        return m == null ? null : m.get(slot);
    }

    public boolean isListSlot(int slot) {
        return slot >= 0 && slot < LIST;
    }

    public void cleanup(Player viewer) {
        slotMaps.remove(viewer.getUniqueId());
    }
}
