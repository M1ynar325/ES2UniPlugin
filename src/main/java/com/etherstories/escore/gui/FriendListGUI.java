package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.FriendManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class FriendListGUI {

    public enum ViewMode { FRIENDS, REQUESTS }

    public static final String TITLE_FRIENDS  = "&8好友";
    public static final String TITLE_REQUESTS = "&8好友请求";

    // ── Control slots (bottom row of 4-row chest) ─────────────────────────────
    private static final int SIZE             = 36;
    public  static final int SLOT_CLOSE       = 27;
    public  static final int SLOT_LOC_TOGGLE  = 29;
    public  static final int SLOT_REQUESTS    = 31;  // FRIENDS mode only
    public  static final int SLOT_ADD         = 33;  // FRIENDS mode: add friend
    public  static final int SLOT_BACK        = 33;  // REQUESTS mode: back

    private static final Material BG = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin plugin;
    private final Map<UUID, ViewMode>   modes    = new HashMap<>();
    private final Map<UUID, List<UUID>> slotMaps = new HashMap<>();

    public FriendListGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        openInMode(player, ViewMode.FRIENDS);
    }

    public void openInMode(Player player, ViewMode mode) {
        modes.put(player.getUniqueId(), mode);

        FriendManager fm = plugin.getFriendManager();
        UUID uuid = player.getUniqueId();

        String titleKey = mode == ViewMode.FRIENDS ? TITLE_FRIENDS : TITLE_REQUESTS;
        Inventory inv = Bukkit.createInventory(null, SIZE, ColorUtil.colorize(titleKey));

        ItemStack bg = ECOSTerminalGUI.bg(BG);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, bg);

        // ── Entry list ────────────────────────────────────────────────────────
        List<UUID> entries = mode == ViewMode.FRIENDS
                ? fm.getFriends(uuid)
                : fm.getPendingRequests(uuid);

        List<UUID> slotMap = new ArrayList<>();
        for (int i = 0; i < Math.min(entries.size(), 27); i++) {
            UUID entryUuid = entries.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(entryUuid);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(op);
                String name = op.getName() != null ? op.getName() : "?";
                meta.setDisplayName(ColorUtil.colorize("&f" + name));

                if (mode == ViewMode.FRIENDS) {
                    List<String> lore = new ArrayList<>();
                    Player online = Bukkit.getPlayer(entryUuid);
                    if (online != null) {
                        lore.add(ColorUtil.colorize("&7在线"));
                        if (fm.isLocationSharing(entryUuid)) {
                            Location loc = online.getLocation();
                            lore.add(ColorUtil.colorize(String.format(
                                    "&8%s  %.0f, %.0f, %.0f",
                                    loc.getWorld().getName(),
                                    loc.getX(), loc.getY(), loc.getZ())));
                        }
                    } else {
                        lore.add(ColorUtil.colorize("&8离线"));
                    }
                    lore.add("");
                    lore.add(ColorUtil.colorize("&a左键: 发送我的坐标卡片"));
                    lore.add(ColorUtil.colorize("&8右键: 删除好友"));
                    meta.setLore(lore);
                } else {
                    meta.setLore(List.of(
                            ColorUtil.colorize("&7好友请求"),
                            ColorUtil.colorize(""),
                            ColorUtil.colorize("&7左键: 接受"),
                            ColorUtil.colorize("&8右键: 拒绝")));
                }
                skull.setItemMeta(meta);
            }
            inv.setItem(i, skull);
            slotMap.add(entryUuid);
        }
        slotMaps.put(uuid, slotMap);

        // ── Controls ──────────────────────────────────────────────────────────
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&7返回终端", null));

        if (mode == ViewMode.FRIENDS) {
            boolean sharing = fm.isLocationSharing(uuid);
            inv.setItem(SLOT_LOC_TOGGLE, ECOSTerminalGUI.item(
                    sharing ? Material.CLOCK : Material.COMPASS,
                    "&7位置分享  " + (sharing ? "&a开启" : "&8关闭"),
                    List.of(ColorUtil.colorize("&8好友可见你的坐标"),
                            ColorUtil.colorize("&8点击切换"))));

            int reqCount = fm.getPendingRequests(uuid).size();
            if (reqCount > 0) {
                inv.setItem(SLOT_REQUESTS, ECOSTerminalGUI.item(Material.BELL,
                        "&7好友请求  &f" + reqCount,
                        List.of(ColorUtil.colorize("&8点击查看"))));
            }

            inv.setItem(SLOT_ADD, ECOSTerminalGUI.item(Material.NAME_TAG,
                    "&a添加好友",
                    List.of("&7选择在线玩家头颅",
                            "&7或聊天输入名字",
                            "",
                            "&a▸ 点击打开")));
        } else {
            inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW,
                    "&7返回好友列表", null));
        }

        player.openInventory(inv);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public UUID getEntryAt(Player player, int slot) {
        List<UUID> map = slotMaps.get(player.getUniqueId());
        if (map == null || slot < 0 || slot >= map.size()) return null;
        return map.get(slot);
    }

    public ViewMode getViewMode(Player player) {
        return modes.getOrDefault(player.getUniqueId(), ViewMode.FRIENDS);
    }

    public void cleanup(Player player) {
        modes.remove(player.getUniqueId());
        slotMaps.remove(player.getUniqueId());
    }
}
