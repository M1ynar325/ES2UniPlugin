package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.JobBoardManager;
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

/** 招工处完成历史 — 所有人只读。 */
public class JobHistoryGUI {

    public static final String TITLE = ColorUtil.colorize("&e&l招工处 · 完成记录");

    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<String>> slotIds = new HashMap<>();

    public JobHistoryGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = EcosHolder.of("job-history", 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.LIME_STAINED_GLASS_PANE));

        List<JobBoardManager.Job> list = plugin.getJobBoardManager().listHistory();
        List<String> ids = new ArrayList<>();
        int slot = 0;
        for (JobBoardManager.Job j : list) {
            if (slot >= 45) break;
            ids.add(j.id);
            List<String> lore = new ArrayList<>();
            lore.add(j.official ? " &6官方委托" : " &7个人委托");
            lore.add(" &7完成: &f" + j.completedLabel());
            lore.add(" &7发布: &f" + j.posterName);
            lore.add(" &7接单: &f" + j.workersLabel());
            lore.add(" &7报酬: &a" + plugin.getVaultHook().format(j.reward)
                    + " &8(已均分 " + Math.max(1, j.workers.size()) + " 人)");
            if (j.description != null && !j.description.isBlank()) {
                String d = j.description.length() > 48
                        ? j.description.substring(0, 48) + "…" : j.description;
                lore.add(" &8" + d);
            }
            lore.add("");
            lore.add("&8只读记录 · 全服可见");
            inv.setItem(slot, ECOSTerminalGUI.item(
                    j.official ? Material.GOLDEN_HELMET : Material.BOOK,
                    "&a✔ &f" + j.title + " &8#" + j.id, lore));
            slot++;
        }
        if (ids.isEmpty()) {
            inv.setItem(22, ECOSTerminalGUI.item(Material.BARRIER,
                    "&7暂无完成记录",
                    List.of("&8委托确认完成后会出现在这里")));
        }
        slotIds.put(player.getUniqueId(), ids);

        inv.setItem(49, ECOSTerminalGUI.item(Material.KNOWLEDGE_BOOK,
                "&e完成记录",
                List.of("&7共 &f" + list.size() + " &7条",
                        "&8最多保留 "
                                + plugin.getJobBoardManager().historySize() + " 条")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回招工处", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        EcosHolder.open(player, inv);
    }

    public void cleanup(Player player) { slotIds.remove(player.getUniqueId()); }
}
