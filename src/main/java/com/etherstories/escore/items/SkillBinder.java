package com.etherstories.escore.items;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能绑定器。
 *
 * 物品上存两个 NBT 键：
 *   es2_right       — 右键（不蹲）触发的技能
 *   es2_sneak_right — Shift+右键触发的技能
 *
 * Lore 中对应显示（不覆盖已有 Lore，仅追加/替换技能行）。
 */
public class SkillBinder {

    private static NamespacedKey keyRight;
    private static NamespacedKey keySneakRight;

    /** 触发槽位 */
    public enum Slot { RIGHT, SNEAK_RIGHT }

    public static void init(ES2UniPlugin plugin) {
        keyRight      = new NamespacedKey(plugin, "es2_right");
        keySneakRight = new NamespacedKey(plugin, "es2_sneak_right");
    }

    // ── 读取 ──────────────────────────────────────────────────────────────────

    public static SkillType getSkill(ItemStack item, Slot slot) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        NamespacedKey key = slot == Slot.RIGHT ? keyRight : keySneakRight;
        String val = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return val == null ? null : SkillType.fromKey(val);
    }

    public static boolean hasAnySkill(ItemStack item) {
        return getSkill(item, Slot.RIGHT) != null || getSkill(item, Slot.SNEAK_RIGHT) != null;
    }

    // ── 写入 ──────────────────────────────────────────────────────────────────

    /** 将技能绑定到物品的指定槽位，同时更新 Lore。 */
    public static void bind(ItemStack item, SkillType skill, Slot slot) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = slot == Slot.RIGHT ? keyRight : keySneakRight;
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, skill.name());

        refreshLore(meta,
                getSkill(item, Slot.RIGHT),
                getSkill(item, Slot.SNEAK_RIGHT),
                // 覆盖当前槽
                slot == Slot.RIGHT      ? skill : getSkill(item, Slot.RIGHT),
                slot == Slot.SNEAK_RIGHT ? skill : getSkill(item, Slot.SNEAK_RIGHT));

        item.setItemMeta(meta);
    }

    /** 解绑指定槽位的技能。 */
    public static void unbind(ItemStack item, Slot slot) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = slot == Slot.RIGHT ? keyRight : keySneakRight;
        meta.getPersistentDataContainer().remove(key);

        SkillType right      = slot == Slot.RIGHT       ? null : getSkill(item, Slot.RIGHT);
        SkillType sneakRight = slot == Slot.SNEAK_RIGHT ? null : getSkill(item, Slot.SNEAK_RIGHT);
        refreshLore(meta, null, null, right, sneakRight);

        item.setItemMeta(meta);
    }

    // ── Lore 刷新 ────────────────────────────────────────────────────────────

    private static final String LORE_PREFIX = "§8[ES2] ";

    private static void refreshLore(ItemMeta meta,
                                    SkillType oldRight, SkillType oldSneakRight,
                                    SkillType newRight, SkillType newSneakRight) {
        List<String> lore = meta.hasLore()
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();

        // 移除旧的技能 Lore 行
        lore.removeIf(line -> line.startsWith(LORE_PREFIX));

        // 追加新的技能 Lore 行（底部）
        if (!lore.isEmpty() && !lore.get(lore.size() - 1).isEmpty()) {
            lore.add(""); // 空行分隔
        }

        if (newRight != null) {
            lore.add(ColorUtil.colorize(
                    "&8[ES2] &7右键 &f" + newRight.displayName()));
        }
        if (newSneakRight != null) {
            lore.add(ColorUtil.colorize(
                    "&8[ES2] &7Shift+右键 &f" + newSneakRight.displayName()));
        }

        // 移除尾部多余空行
        while (!lore.isEmpty() && lore.get(lore.size() - 1).isEmpty()) {
            lore.remove(lore.size() - 1);
        }

        meta.setLore(lore.isEmpty() ? null : lore);
    }
}
