package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.InsuranceManager;
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

public class InsuranceGUI {

    public static final String TITLE = ColorUtil.colorize("&b&l保险柜");

    public static final int SLOT_BACKUP = 11;
    public static final int SLOT_CLAIM = 15;
    public static final int SLOT_INFO = 13;
    public static final int SLOT_BACK = 48;
    public static final int SLOT_CLOSE = 49;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<String>> claimSlots = new HashMap<>();

    public InsuranceGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = EcosHolder.of("insurance", 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.LIGHT_BLUE_STAINED_GLASS_PANE));

        List<InsuranceManager.Backup> hist = plugin.getInsuranceManager().getHistory(player.getUniqueId());
        InsuranceManager.Backup bak = plugin.getInsuranceManager().getBackup(player.getUniqueId());
        InsuranceManager.DeathCover death = plugin.getInsuranceManager().lastDeath(player.getUniqueId());
        String coverLine = death == null ? " &8最近死亡点未记录"
                : (death.covered() ? " &a最近死亡: " + death.label() : " &7最近死亡: &8" + death.label());
        inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.BOOK,
                "&b背包清空险 &a免费",
                List.of(" &7异常清包可申请理赔",
                        " &7保障: 公开领地内 / 车站内死亡",
                        " &7范围内死亡会立刻备份背包",
                        " &7保留最近 &f" + plugin.getInsuranceManager().historySize() + " &7次备份",
                        " &7通过后恢复物品 + 赔偿 &e" + (int) plugin.getInsuranceManager().compensation(),
                        "",
                        coverLine,
                        bak == null ? " &8当前无备份"
                                : " &7最新: &f" + bak.timeLabel() + " &8(" + bak.itemCount() + "件)",
                        " &7历史份数: &f" + hist.size())));

        inv.setItem(SLOT_BACKUP, ECOSTerminalGUI.item(Material.CHEST,
                "&a立即备份背包",
                List.of("&7写入历史快照",
                        "&8空包 / 物品骤减不会写入")));

        inv.setItem(SLOT_CLAIM, ECOSTerminalGUI.item(Material.TOTEM_OF_UNDYING,
                "&e申请理赔",
                List.of("&7提交后管理员可浏览备份并选择恢复")));

        List<String> ids = new ArrayList<>();
        if (player.hasPermission("es2uni.admin")) {
            List<InsuranceManager.Claim> pending = plugin.getInsuranceManager().pendingClaims();
            int slot = 27;
            for (InsuranceManager.Claim c : pending) {
                if (slot >= 45) break;
                ids.add(c.id);
                int n = plugin.getInsuranceManager().getHistory(c.player).size();
                String place = c.coverPlace == null || c.coverPlace.isBlank() ? "未记录" : c.coverPlace;
                inv.setItem(slot, ECOSTerminalGUI.item(Material.PAPER,
                        (c.covered ? "&a" : "&e") + "理赔 #" + c.id + " &f" + c.playerName,
                        List.of(" &7范围: " + (c.covered ? "&a" + place : "&8不在保障 · " + place),
                                " &7可选备份: &f" + n + " 份",
                                " &7赔偿: &e" + (int) plugin.getInsuranceManager().compensation(),
                                "",
                                "&a左键: 打开备份选择",
                                "&c右键: 拒绝")));
                slot++;
            }
            if (pending.isEmpty()) {
                inv.setItem(31, ECOSTerminalGUI.item(Material.LIME_DYE, "&7暂无待审理赔", null));
            }
        }
        claimSlots.put(player.getUniqueId(), ids);

        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回管理处", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }

    public String getClaimId(Player player, int slot) {
        List<String> ids = claimSlots.get(player.getUniqueId());
        if (ids == null) return null;
        int idx = slot - 27;
        if (idx < 0 || idx >= ids.size()) return null;
        return ids.get(idx);
    }

    public void cleanup(Player player) { claimSlots.remove(player.getUniqueId()); }
}
