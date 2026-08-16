package com.etherstories.escore.items;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public final class TransitItems {

    public static final String KIND_GATE = "gate";
    public static final String KIND_TVM = "tvm";
    public static final String KIND_ADJUST = "adjust";
    public static final String KIND_CARD = "card";
    public static final String KIND_TICKET = "ticket";

    private static NamespacedKey kindKey;
    private static NamespacedKey stationKey;
    private static NamespacedKey modeKey;
    private static NamespacedKey cardKey;
    private static NamespacedKey fromKey;
    private static NamespacedKey toKey;
    private static NamespacedKey expKey;

    private TransitItems() {}

    public static void init(ES2UniPlugin plugin) {
        kindKey = new NamespacedKey(plugin, "transit_kind");
        stationKey = new NamespacedKey(plugin, "transit_station");
        modeKey = new NamespacedKey(plugin, "transit_mode");
        cardKey = new NamespacedKey(plugin, "transit_card");
        fromKey = new NamespacedKey(plugin, "transit_from");
        toKey = new NamespacedKey(plugin, "transit_to");
        expKey = new NamespacedKey(plugin, "transit_exp");
    }

    public static ItemStack gate(String stationId, String mode) {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ColorUtil.colorize("&b检票闸机 &8[" + mode.toUpperCase() + "]"));
        meta.setLore(List.of(
                ColorUtil.colorize("&8进站 / 出站检票口"),
                ColorUtil.colorize("&7车站: &f" + stationId),
                ColorUtil.colorize("&7模式: &f" + mode.toUpperCase()),
                ColorUtil.colorize(""),
                ColorUtil.colorize("&7放置后写成牌子，右键刷卡")
        ));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(kindKey, PersistentDataType.STRING, KIND_GATE);
        pdc.set(stationKey, PersistentDataType.STRING, stationId);
        pdc.set(modeKey, PersistentDataType.STRING, mode.toUpperCase());
        ES2UniPlugin plugin = ES2UniPlugin.getInstance();
        if (plugin != null && plugin.getTransitManager() != null
                && meta instanceof org.bukkit.inventory.meta.BlockStateMeta bsm
                && bsm.getBlockState() instanceof org.bukkit.block.Sign sign) {
            plugin.getTransitManager().applyGateSign(sign, stationId, mode);
            bsm.setBlockState(sign);
            meta = bsm;
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack tvm(String stationId) {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ColorUtil.colorize("&eECOS 售票机"));
        meta.setLore(List.of(
                ColorUtil.colorize("&8本站: &f" + stationId),
                ColorUtil.colorize("&7右键打开: 单程票 / 交通卡 / 闪付 / 补票")
        ));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(kindKey, PersistentDataType.STRING, KIND_TVM);
        pdc.set(stationKey, PersistentDataType.STRING, stationId);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack adjust(String stationId) {
        ItemStack item = new ItemStack(Material.BIRCH_SIGN);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ColorUtil.colorize("&6补票处"));
        meta.setLore(List.of(
                ColorUtil.colorize("&8补票 / 行程结算"),
                ColorUtil.colorize("&7车站: &f" + stationId),
                ColorUtil.colorize(""),
                ColorUtil.colorize("&7放置后写成牌子，右键办理")
        ));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(kindKey, PersistentDataType.STRING, KIND_ADJUST);
        pdc.set(stationKey, PersistentDataType.STRING, stationId);
        ES2UniPlugin plugin = ES2UniPlugin.getInstance();
        if (plugin != null && plugin.getTransitManager() != null
                && meta instanceof org.bukkit.inventory.meta.BlockStateMeta bsm
                && bsm.getBlockState() instanceof org.bukkit.block.Sign sign) {
            plugin.getTransitManager().applyAdjustSign(sign, stationId);
            bsm.setBlockState(sign);
            meta = bsm;
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack octopus(UUID cardId, String ownerName) {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        // item_name：库存显示名。不用 custom_name，命名牌才不会触发给生物起名
        meta.setItemName(ColorUtil.colorize("&bECOS 交通卡"));
        meta.setLore(List.of(
                ColorUtil.colorize("&8持卡人: &f" + ownerName),
                ColorUtil.colorize("&7手持右键闸机，扣插件余额，不是卡内钱包"),
                ColorUtil.colorize("&7进站、出站各刷一次"),
                ColorUtil.colorize("&8" + cardId.toString().substring(0, 8))
        ));
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(kindKey, PersistentDataType.STRING, KIND_CARD);
        pdc.set(cardKey, PersistentDataType.STRING, cardId.toString());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack ticket(String fromId, String toId, String fromName, String toName, long expireMs) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ColorUtil.colorize("&f单程票 &8" + fromName + " → " + toName));
        meta.setLore(List.of(
                ColorUtil.colorize("&7起点: &f" + fromName),
                ColorUtil.colorize("&7终点: &f" + toName),
                ColorUtil.colorize("&8进站须为本票起点，出站须为终点")
        ));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(kindKey, PersistentDataType.STRING, KIND_TICKET);
        pdc.set(fromKey, PersistentDataType.STRING, fromId);
        pdc.set(toKey, PersistentDataType.STRING, toId);
        pdc.set(expKey, PersistentDataType.LONG, expireMs);
        item.setItemMeta(meta);
        return item;
    }

    public static String kind(ItemStack item) {
        String v = str(item, kindKey);
        return v == null ? "" : v;
    }

    public static boolean isGate(ItemStack item) { return KIND_GATE.equals(kind(item)); }
    public static boolean isTvm(ItemStack item) { return KIND_TVM.equals(kind(item)); }
    public static boolean isAdjust(ItemStack item) { return KIND_ADJUST.equals(kind(item)); }
    public static boolean isCard(ItemStack item) { return KIND_CARD.equals(kind(item)); }
    public static boolean isTicket(ItemStack item) { return KIND_TICKET.equals(kind(item)); }

    public static String stationId(ItemStack item) { return str(item, stationKey); }
    public static String mode(ItemStack item) { return str(item, modeKey); }
    public static String cardId(ItemStack item) { return str(item, cardKey); }
    public static String ticketFrom(ItemStack item) { return str(item, fromKey); }
    public static String ticketTo(ItemStack item) { return str(item, toKey); }

    public static long ticketExp(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Long v = item.getItemMeta().getPersistentDataContainer().get(expKey, PersistentDataType.LONG);
        return v == null ? 0 : v;
    }

    private static String str(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}
