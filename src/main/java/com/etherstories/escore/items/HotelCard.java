package com.etherstories.escore.items;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public final class HotelCard {

    private static NamespacedKey hotelKey;
    private static NamespacedKey unitKey;
    private static NamespacedKey guestKey;
    private static NamespacedKey tokenKey;

    private HotelCard() {}

    public static void init(ES2UniPlugin plugin) {
        hotelKey = new NamespacedKey(plugin, "hotel_id");
        unitKey = new NamespacedKey(plugin, "hotel_unit");
        guestKey = new NamespacedKey(plugin, "hotel_guest");
        tokenKey = new NamespacedKey(plugin, "hotel_token");
    }

    public static ItemStack create(String hotelId, String unitId, UUID guest, long token,
                                   String hotelName, String address) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(ColorUtil.colorize("&b房卡 &f" + address));
        meta.setLore(List.of(
                ColorUtil.colorize(" &7酒店: &f" + hotelName),
                ColorUtil.colorize(" &7房间: &f" + address),
                ColorUtil.colorize(" &8住客本人或背包里有此卡可开门")
        ));
        meta.getPersistentDataContainer().set(hotelKey, PersistentDataType.STRING, hotelId);
        meta.getPersistentDataContainer().set(unitKey, PersistentDataType.STRING, unitId);
        meta.getPersistentDataContainer().set(guestKey, PersistentDataType.STRING, guest.toString());
        meta.getPersistentDataContainer().set(tokenKey, PersistentDataType.LONG, token);
        stack.setItemMeta(meta);
        return stack;
    }

    public static boolean isCard(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        return stack.getItemMeta().getPersistentDataContainer().has(hotelKey, PersistentDataType.STRING);
    }

    public static String hotelId(ItemStack stack) {
        return stack.getItemMeta().getPersistentDataContainer().get(hotelKey, PersistentDataType.STRING);
    }

    public static String unitId(ItemStack stack) {
        return stack.getItemMeta().getPersistentDataContainer().get(unitKey, PersistentDataType.STRING);
    }

    public static UUID guest(ItemStack stack) {
        String s = stack.getItemMeta().getPersistentDataContainer().get(guestKey, PersistentDataType.STRING);
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    public static long token(ItemStack stack) {
        Long v = stack.getItemMeta().getPersistentDataContainer().get(tokenKey, PersistentDataType.LONG);
        return v == null ? 0 : v;
    }
}
