package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.PlaytimeManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class PlayerActionGUI {

    public static final String TITLE = "&8玩家操作";

    public static final int SLOT_WHISPER  = 10;
    public static final int SLOT_MAIL     = 12;
    public static final int SLOT_FRIEND   = 14;
    public static final int SLOT_PAY      = 16;
    public static final int SLOT_REPORT   = 20; // 悄悄话 / 简易举报
    public static final int SLOT_BACK     = 22;
    public static final int SLOT_CLOSE    = 26;
    private static final int SIZE         = 27;
    private static final Material BG      = Material.GRAY_STAINED_GLASS_PANE;

    private final ES2UniPlugin        plugin;
    private final Map<UUID, UUID>     openTargets = new HashMap<>(); // viewer → target

    public PlayerActionGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Player target) {
        open(viewer, (org.bukkit.OfflinePlayer) target);
    }

    public void open(Player viewer, org.bukkit.OfflinePlayer target) {
        if (target == null) return;
        openTargets.put(viewer.getUniqueId(), target.getUniqueId());
        String name = target.getName() != null ? target.getName() : "?";

        Inventory inv = EcosHolder.of("player-action", SIZE,
                ColorUtil.colorize(TITLE + " &7» &f" + name));

        ItemStack bg = ECOSTerminalGUI.bg(BG);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, bg);

        // Target skull
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) skull.getItemMeta();
        if (sm != null) {
            sm.setOwningPlayer(target);
            sm.setDisplayName(ColorUtil.colorize("&f" + name));
            long total = plugin.getPlaytimeManager().getTotalMillis(target.getUniqueId());
            Player online = target.getPlayer();
            sm.setLore(List.of(
                    ColorUtil.colorize("&7总在线: &f" + PlaytimeManager.formatMillis(total)),
                    ColorUtil.colorize(online != null ? "&7延迟: &f" + online.getPing() + "ms" : "&8离线")));
            skull.setItemMeta(sm);
        }
        inv.setItem(4, skull);

        boolean isFriend = plugin.getFriendManager()
                .areFriends(viewer.getUniqueId(), target.getUniqueId());

        inv.setItem(SLOT_WHISPER, ECOSTerminalGUI.item(Material.PAPER,
                "&f私信", List.of("&7向该玩家发送私信", "", "&8点击后在聊天框输入")));

        inv.setItem(SLOT_MAIL, ECOSTerminalGUI.item(Material.WRITABLE_BOOK,
                "&f留言", List.of("&7发送书信留言", "", "&8点击获取书本")));

        inv.setItem(SLOT_FRIEND, ECOSTerminalGUI.item(
                isFriend ? Material.RED_WOOL : Material.LIME_WOOL,
                isFriend ? "&7删除好友" : "&7添加好友",
                List.of(isFriend ? "&8点击删除" : "&8点击发送请求")));

        if (plugin.getVaultHook().isEnabled()) {
            inv.setItem(SLOT_PAY, ECOSTerminalGUI.item(Material.SUNFLOWER,
                    "&f转账", List.of("&7向该玩家转账", "", "&8点击输入金额")));
        }

        if (plugin.getReportManager().isEnabled()) {
            inv.setItem(SLOT_REPORT, ECOSTerminalGUI.item(Material.DARK_OAK_SIGN,
                    "&c悄悄话 / 举报",
                    List.of("&7仅管理员可见",
                            "&7可反馈问题或举报行为",
                            "",
                            "&c▸ 点击后在聊天输入内容")));
        }

        inv.setItem(SLOT_BACK,  ECOSTerminalGUI.item(Material.ARROW,   "&7返回列表", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&7关闭",     null));

        EcosHolder.open(viewer, inv);
    }

    public UUID getTarget(Player viewer) {
        return openTargets.get(viewer.getUniqueId());
    }

    public void cleanup(Player viewer) { openTargets.remove(viewer.getUniqueId()); }
}
