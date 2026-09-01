package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.ShowcaseManager;
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

public class ShowcaseGUI {

    public static final String TITLE = ColorUtil.colorize("&d&l机器展示");

    public static final int SLOT_REGISTER = 45;
    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, List<String>> slotIds = new HashMap<>();

    public ShowcaseGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = EcosHolder.of("showcase", 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.PURPLE_STAINED_GLASS_PANE));

        List<ShowcaseManager.Showcase> list = plugin.getShowcaseManager().topByVotes(45);
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ShowcaseManager.Showcase s = list.get(i);
            ids.add(s.id());
            inv.setItem(i, ECOSTerminalGUI.item(Material.CRAFTING_TABLE,
                    "&f" + s.title(),
                    List.of(
                            "&7作者: &f" + s.ownerName(),
                            "&7票数: &e" + s.votes(),
                            String.format("&8%s %.0f %.0f %.0f", s.world(), s.x(), s.y(), s.z()),
                            "",
                            "&a左键: 传送参观",
                            "&e右键: 投票（每天每点 1 次）",
                            "&cShift+右键: 删除自己的点"
                    )));
        }
        slotIds.put(player.getUniqueId(), ids);

        inv.setItem(SLOT_REGISTER, ECOSTerminalGUI.item(Material.END_CRYSTAL,
                "&a登记当前坐标为展示点",
                List.of("&7站在机器旁点击", "&7聊天输入标题，或直接回车用默认名",
                        "&8cancel 取消")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
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
