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

/** 管理选择要恢复的备份 */
public class InsuranceBackupGUI {

    public static final String TITLE = ColorUtil.colorize("&b&l选择备份恢复");
    public static final int SLOT_BACK = 45;
    public static final int SLOT_DENY = 49;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, String> claimOf = new HashMap<>();
    private final Map<UUID, List<Integer>> slotIndex = new HashMap<>();

    public InsuranceBackupGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player admin, String claimId) {
        InsuranceManager.Claim c = plugin.getInsuranceManager().getClaim(claimId);
        if (c == null) return;
        List<InsuranceManager.Backup> hist = plugin.getInsuranceManager().getHistory(c.player);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.LIGHT_BLUE_STAINED_GLASS_PANE));

        inv.setItem(4, ECOSTerminalGUI.item(Material.PLAYER_HEAD,
                "&f" + c.playerName + " &8#" + claimId,
                List.of("&7选择一份备份恢复",
                        "&7并通过后赔偿 &e" + (int) plugin.getInsuranceManager().compensation())));

        List<Integer> idxs = new ArrayList<>();
        int slot = 9;
        for (int i = hist.size() - 1; i >= 0 && slot < 45; i--) {
            InsuranceManager.Backup b = hist.get(i);
            idxs.add(i);
            List<String> lore = new ArrayList<>();
            lore.add(" &7时间: &f" + b.timeLabel());
            lore.add(" &7物品数: &f" + b.itemCount());
            for (String s : b.sampleNames(6)) lore.add(" &8· " + s);
            lore.add("");
            lore.add("&a▸ 点击恢复此备份并赔偿");
            inv.setItem(slot++, ECOSTerminalGUI.item(Material.CHEST,
                    "&b备份 #" + (i + 1) + " &8(" + b.timeLabel() + ")", lore));
        }
        claimOf.put(admin.getUniqueId(), claimId);
        slotIndex.put(admin.getUniqueId(), idxs);

        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回保险柜", null));
        inv.setItem(SLOT_DENY, ECOSTerminalGUI.item(Material.BARRIER, "&c拒绝理赔", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.STRUCTURE_VOID, "&c关闭", null));
        admin.openInventory(inv);
    }

    public String getClaimId(Player p) { return claimOf.get(p.getUniqueId()); }

    public Integer getBackupIndex(Player p, int slot) {
        List<Integer> idxs = slotIndex.get(p.getUniqueId());
        if (idxs == null) return null;
        int i = slot - 9;
        if (i < 0 || i >= idxs.size()) return null;
        return idxs.get(i);
    }

    public void cleanup(Player p) {
        claimOf.remove(p.getUniqueId());
        slotIndex.remove(p.getUniqueId());
    }
}
