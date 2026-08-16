package com.etherstories.escore.items;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class PveItems {

    public static final String KIND_PACK = "pack";
    public static final String KIND_ELITE = "elite";

    static final EntityType[] PACK = {
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER,
            EntityType.DROWNED, EntityType.HUSK, EntityType.PILLAGER, EntityType.WITCH
    };
    static final EntityType[] ELITE = {
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.VINDICATOR, EntityType.EVOKER,
            EntityType.RAVAGER, EntityType.WITHER_SKELETON, EntityType.PILLAGER, EntityType.WITCH
    };

    private static NamespacedKey kindKey;
    private static NamespacedKey typeKey;

    private PveItems() {}

    public static void init(ES2UniPlugin plugin) {
        kindKey = new NamespacedKey(plugin, "pve_kind");
        typeKey = new NamespacedKey(plugin, "pve_type");
    }

    public static ItemStack pack() {
        return make(Material.BLAZE_ROD, KIND_PACK, PACK[0],
                "&c刷怪棒",
                List.of("&7右键: 面前刷 6 只",
                        "&7潜行右键: 换种类",
                        "&8管理道具"));
    }

    public static ItemStack elite() {
        return make(Material.BONE, KIND_ELITE, ELITE[0],
                "&6精英棒",
                List.of("&7右键: 刷 1 只精英",
                        "&7潜行右键: 换种类",
                        "&8三倍血 / 发光 / 力+速"));
    }

    public static boolean isPve(ItemStack item) {
        return kind(item) != null;
    }

    public static String kind(ItemStack item) {
        if (item == null || !item.hasItemMeta() || kindKey == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(kindKey, PersistentDataType.STRING);
    }

    public static EntityType typeOf(ItemStack item, boolean elite) {
        EntityType[] all = elite ? ELITE : PACK;
        if (item == null || !item.hasItemMeta() || typeKey == null) return all[0];
        String raw = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (raw == null) return all[0];
        try {
            return EntityType.valueOf(raw);
        } catch (Exception e) {
            return all[0];
        }
    }

    public static EntityType cycle(ItemStack item, boolean elite) {
        EntityType[] all = elite ? ELITE : PACK;
        EntityType cur = typeOf(item, elite);
        int idx = 0;
        for (int i = 0; i < all.length; i++) if (all[i] == cur) { idx = i; break; }
        EntityType next = all[(idx + 1) % all.length];
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, next.name());
            String title = elite ? "&6精英棒 &8" + cn(next) : "&c刷怪棒 &8" + cn(next);
            meta.setDisplayName(ColorUtil.colorize(title));
            item.setItemMeta(meta);
        }
        return next;
    }

    public static String cn(EntityType t) {
        return switch (t) {
            case ZOMBIE -> "僵尸";
            case SKELETON -> "骷髅";
            case SPIDER -> "蜘蛛";
            case CREEPER -> "苦力怕";
            case DROWNED -> "溺尸";
            case HUSK -> "尸壳";
            case PILLAGER -> "掠夺者";
            case WITCH -> "女巫";
            case VINDICATOR -> "卫道士";
            case EVOKER -> "唤魔者";
            case RAVAGER -> "劫掠兽";
            case WITHER_SKELETON -> "凋灵骷髅";
            default -> t.name();
        };
    }

    private static ItemStack make(Material mat, String kind, EntityType type, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ColorUtil.colorize(name + " &8" + cn(type)));
        meta.setLore(lore.stream().map(ColorUtil::colorize).toList());
        meta.getPersistentDataContainer().set(kindKey, PersistentDataType.STRING, kind);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }
}
