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

public class JobBoardGUI {

    public static final String TITLE = ColorUtil.colorize("&e&l招工处");

    public static final int SLOT_POST = 45;
    public static final int SLOT_POST_OFFICIAL = 46;
    public static final int SLOT_HISTORY = 48;
    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<String>> slotIds = new HashMap<>();

    public JobBoardGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.YELLOW_STAINED_GLASS_PANE));

        List<JobBoardManager.Job> list = plugin.getJobBoardManager().listActive();
        List<String> ids = new ArrayList<>();
        int slot = 0;
        for (JobBoardManager.Job j : list) {
            if (slot >= 45) break;
            ids.add(j.id);
            boolean recruiting = j.hasRoom();
            Material mat = j.official ? Material.GOLDEN_HELMET
                    : (recruiting ? Material.PAPER : Material.MAP);
            List<String> lore = new ArrayList<>();
            lore.add(j.official ? " &6官方委托" : " &7个人委托");
            lore.add(" &7发布: &f" + j.posterName);
            lore.add(" &7总报酬: &a" + plugin.getVaultHook().format(j.reward)
                    + " &8(完成均分)");
            lore.add(" &7人数: &f" + j.slotsLabel()
                    + (j.locked ? " &c已锁定" : (recruiting ? " &a可接" : " &c已满")));
            lore.add(" &7接单: &f" + j.workersLabel());
            if (j.description != null && !j.description.isBlank()) {
                String d = j.description.length() > 40 ? j.description.substring(0, 40) + "…" : j.description;
                lore.add(" &8" + d);
            }
            String mod = j.adminModifiedLabel();
            if (mod != null) lore.add(" &c" + mod);
            lore.add("");
            if (recruiting && !j.poster.equals(player.getUniqueId())
                    && !j.workers.contains(player.getUniqueId()))
                lore.add("&a左键: 加入接单");
            if (j.hasWorkers()
                    && (j.poster.equals(player.getUniqueId()) || player.hasPermission("es2uni.admin")))
                lore.add("&a左键: 确认完成并均分打款");
            if (j.poster.equals(player.getUniqueId()) || player.hasPermission("es2uni.admin")) {
                lore.add("&eShift+右键: " + (j.locked ? "解锁加人" : "锁定不再加人"));
                if (!j.hasWorkers())
                    lore.add("&c丢弃键位: 用指令取消 — /ecos municipal job cancel <id>");
                lore.add("&7移除成员: &f/ecos municipal job kick <id> <名>");
            }
            if (player.hasPermission("es2uni.admin")) {
                lore.add("&e右键: 编辑标题");
                lore.add("&cShift+左键: 强制删除并退款");
            }
            inv.setItem(slot, ECOSTerminalGUI.item(mat, "&f" + j.title + " &8#" + j.id, lore));
            slot++;
        }
        slotIds.put(player.getUniqueId(), ids);

        inv.setItem(SLOT_POST, ECOSTerminalGUI.item(Material.WRITABLE_BOOK,
                "&a发布个人委托",
                List.of("&7聊天依次: 标题 → 报酬 → 人数 → 说明",
                        "&7多人时总报酬托管，完成均分",
                        "&8最低 " + plugin.getVaultHook().format(plugin.getJobBoardManager().minReward())
                                + " · 最多 " + plugin.getJobBoardManager().maxWorkersCap() + " 人")));
        if (player.hasPermission("es2uni.admin")) {
            inv.setItem(SLOT_POST_OFFICIAL, ECOSTerminalGUI.item(Material.GOLDEN_PICKAXE,
                    "&6发布官方委托",
                    List.of("&7同样可设多人", "&7列表中金色头盔标识")));
        }
        int hist = plugin.getJobBoardManager().listHistory().size();
        inv.setItem(SLOT_HISTORY, ECOSTerminalGUI.item(Material.BOOKSHELF,
                "&e完成记录",
                List.of("&7全服可查看已完成的委托",
                        "&7当前 &f" + hist + " &7条",
                        "",
                        "&e▸ 点击打开")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回管理处", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    public String getId(Player player, int slot) {
        List<String> ids = slotIds.get(player.getUniqueId());
        if (ids == null || slot < 0 || slot >= ids.size()) return null;
        return ids.get(slot);
    }

    public void cleanup(Player player) { slotIds.remove(player.getUniqueId()); }
}
