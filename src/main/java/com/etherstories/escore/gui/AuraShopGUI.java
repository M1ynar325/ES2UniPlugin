package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.auras.AuraType;
import com.etherstories.escore.auras.ParticleBrightness;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 特效商店 GUI（54格）— 支持最多 21 种特效
 */
public class AuraShopGUI {

    public static final String TITLE = ColorUtil.colorize("&b✦ &f&l特效商店 &b✦");

    // 三行密集摆放
    public static final int[] AURA_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public static final int SLOT_INFO       = 40;
    public static final int SLOT_BRIGHTNESS = 43;
    public static final int SLOT_UNEQUIP    = 46;
    public static final int SLOT_CLOSE      = 52;

    private final ES2UniPlugin plugin;

    public AuraShopGUI(ES2UniPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack bg = ECOSTerminalGUI.bg(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        UUID uuid = player.getUniqueId();
        Set<AuraType> ownedSet = plugin.getAuraManager().getOwned(uuid);
        AuraType equipped      = plugin.getAuraManager().getEquipped(uuid);

        AuraType[] auras = AuraType.values();
        for (int i = 0; i < Math.min(auras.length, AURA_SLOTS.length); i++) {
            AuraType aura = auras[i];
            inv.setItem(AURA_SLOTS[i], buildAuraItem(aura, ownedSet.contains(aura), aura == equipped, player));
        }

        if (equipped != null) {
            inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.LIME_CONCRETE,
                    "&a当前装备: &f" + equipped.displayName(),
                    List.of(" " + equipped.description, "", "&a▸ 点击对应特效可卸除")));
        } else {
            inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.GRAY_CONCRETE,
                    "&7未装备特效",
                    List.of("", "&8点击图标购买或装备")));
        }

        ParticleBrightness br = plugin.getAuraManager().getBrightness(uuid);
        Material brMat = switch (br) {
            case LOW -> Material.COAL;
            case MEDIUM -> Material.GLOWSTONE_DUST;
            case HIGH -> Material.BLAZE_POWDER;
        };
        inv.setItem(SLOT_BRIGHTNESS, ECOSTerminalGUI.item(brMat,
                "&e粒子亮度: &f" + br.label,
                List.of("&7低 / 中 / 高（默认中）",
                        "&7按特效类型分别调节密度与尺寸",
                        "&8低档隔帧以减轻卡顿",
                        "",
                        "&e▸ 点击切换")));

        inv.setItem(SLOT_UNEQUIP, ECOSTerminalGUI.item(
                equipped != null ? Material.MAGMA_CREAM : Material.GRAY_DYE,
                equipped != null ? "&c卸除当前特效" : "&8无特效可卸除",
                List.of("", equipped != null ? "&c▸ 点击卸除 " + equipped.displayName() : "&8暂未装备")));

        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));

        player.openInventory(inv);
    }

    private ItemStack buildAuraItem(AuraType aura, boolean owned, boolean equipped, Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(" " + aura.description);
        lore.add(" " + aura.tier);
        lore.add("");

        if (equipped) {
            lore.add("&a▸ 当前装备中");
            lore.add("&8点击卸除");
            ItemStack item = ECOSTerminalGUI.item(aura.icon, "&a&l" + aura.displayName(), lore);
            return ECOSTerminalGUI.glint(item);
        } else if (owned) {
            lore.add("&7▸ 已拥有");
            lore.add("&e点击装备");
            return ECOSTerminalGUI.item(aura.icon, "&e" + aura.displayName(), lore);
        } else {
            boolean canAfford = plugin.getVaultHook().isEnabled()
                    && plugin.getVaultHook().getBalance(player) >= aura.price;
            lore.add("&7价格: " + (plugin.getVaultHook().isEnabled()
                    ? plugin.getVaultHook().format(aura.price)
                    : aura.price + " 货币"));
            lore.add(canAfford ? "&a▸ 点击购买" : "&c余额不足");
            return ECOSTerminalGUI.item(aura.icon, "&7" + aura.displayName(), lore);
        }
    }
}
