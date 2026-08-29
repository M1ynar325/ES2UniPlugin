package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.SkillShopManager;
import com.etherstories.escore.utils.ColorUtil;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 技能商店：玩家购买绑好技能的武器；管理员另开上架页。 */
public class SkillShopGUI {

    public static final String TITLE = EcosStyle.hub("技能商店");
    public static final String ADMIN_TITLE = ColorUtil.colorize("&c✦ &f&l技能上架 &c✦");

    public static final int[] SKILL_SLOTS = AuraShopGUI.AURA_SLOTS;
    public static final int SLOT_INFO = 45;
    public static final int SLOT_CLOSE = 52;

    private final ES2UniPlugin plugin;

    public SkillShopGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBg(inv);
        List<SkillShopManager.Listing> sale = plugin.getSkillShopManager().forSale();
        for (int i = 0; i < Math.min(sale.size(), SKILL_SLOTS.length); i++) {
            inv.setItem(SKILL_SLOTS[i], buildSaleItem(sale.get(i), player));
        }
        inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.BOOK, EcosStyle.JIQING + "技能商店",
                List.of(" &7买到的是绑好技能的武器",
                        " &7上架: &f" + sale.size(),
                        "", "&8右键释放")));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    public void openAdmin(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ADMIN_TITLE);
        fillBg(inv);
        SkillType[] all = SkillType.values();
        for (int i = 0; i < Math.min(all.length, SKILL_SLOTS.length); i++) {
            inv.setItem(SKILL_SLOTS[i], buildAdminItem(plugin.getSkillShopManager().of(all[i])));
        }
        inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.COMMAND_BLOCK, "&c上架管理",
                List.of(" &a左键 &7上下架",
                        " &e右键 &7改价格",
                        "", "&8改完立即写入 skillshop.yml")));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    public SkillType skillAt(int slot, boolean admin) {
        int idx = -1;
        for (int i = 0; i < SKILL_SLOTS.length; i++) {
            if (SKILL_SLOTS[i] == slot) {
                idx = i;
                break;
            }
        }
        if (idx < 0) return null;
        if (admin) {
            SkillType[] all = SkillType.values();
            return idx < all.length ? all[idx] : null;
        }
        List<SkillShopManager.Listing> sale = plugin.getSkillShopManager().forSale();
        return idx < sale.size() ? sale.get(idx).skill() : null;
    }

    private ItemStack buildSaleItem(SkillShopManager.Listing l, Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(" &7" + l.skill().displayName());
        lore.add("");
        boolean vault = plugin.getVaultHook().isEnabled();
        String price = vault ? plugin.getVaultHook().format(l.price()) : String.format("%.0f", l.price());
        boolean ok = !vault || l.price() <= 0 || plugin.getVaultHook().getBalance(player) + 1e-6 >= l.price();
        lore.add("&7价格: &f" + (l.price() <= 0 ? "免费" : price));
        lore.add(ok ? "&a▸ 点击购买" : "&c余额不足");
        return ECOSTerminalGUI.item(icon(l.skill()), "&f" + l.skill().chineseName, lore);
    }

    private ItemStack buildAdminItem(SkillShopManager.Listing l) {
        List<String> lore = new ArrayList<>();
        lore.add(" &7" + l.skill().configKey);
        lore.add(l.enabled() ? " &a出售中" : " &8未上架");
        lore.add(" &7价格: &f" + (plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(l.price()) : String.format("%.0f", l.price())));
        lore.add("");
        lore.add("&a左键 &7上下架");
        lore.add("&e右键 &7改价格");
        ItemStack it = ECOSTerminalGUI.item(icon(l.skill()),
                (l.enabled() ? "&a" : "&8") + l.skill().displayName(), lore);
        return l.enabled() ? ECOSTerminalGUI.glint(it) : it;
    }

    private static void fillBg(Inventory inv) {
        ItemStack bg = EcosStyle.chrome();
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);
    }

    public static Material icon(SkillType t) {
        return switch (t) {
            case PALE_LINE -> Material.END_ROD;
            case GLEAM_ARC -> Material.LIGHTNING_ROD;
            case STILL_VEIL -> Material.WHITE_STAINED_GLASS;
            case LUMINAL_STRIKE -> Material.SPECTRAL_ARROW;
            case SWORD_RAIN -> Material.NETHERITE_SWORD;
            case HOMING -> Material.COMPASS;
            case AXIOM_BREACH -> Material.BLAZE_ROD;
            case STELLAR_CONV -> Material.NETHER_STAR;
            case CELESTIAL_ASCENT -> Material.FEATHER;
            case ECHO_SCATTER -> Material.PRISMARINE_SHARD;
            case ECHO_BARRAGE -> Material.PRISMARINE_CRYSTALS;
        };
    }
}
