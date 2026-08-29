package com.etherstories.escore.items;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Etharia 预制武器 — 保留原 ID/显示名，文案偏音游设定。
 */
public class WeaponPreset {

    public record Preset(String id, String enName, String zhName, ItemStack item) {}

    private static List<Preset> presets;

    /** 旧 ID / 别名 → 当前预设 ID */
    private static final Map<String, String> ALIASES = Map.of(
            "pulse_blade", "aether_edge",
            "stream_foil", "phantom_needle",
            "overdrive_rod", "axiom_scepter",
            "chorus_edge", "nova_blade",
            "echo_wand", "echo_slash",
            "null_edge", "echo_slash",
            "finale", "singularity",
            "lift_blade", "celestial_wing"
    );

    public static void init(ES2UniPlugin plugin) {
        presets = List.of(
            build("aether_edge",
                "Aether Edge", "以太之刃",
                Material.DIAMOND_SWORD,
                new String[]{"sharpness:7", "unbreaking:3"},
                new String[]{
                    "&8标准以太武器",
                    "",
                    "&7右键共鸣锁定（准星指定，可中途换锁），Shift+右键以太裁决。",
                    "&7适合清场与追击。"
                },
                SkillBinder.Slot.RIGHT,       SkillType.HOMING,
                SkillBinder.Slot.SNEAK_RIGHT, SkillType.SWORD_RAIN),

            build("phantom_needle",
                "Phantom Needle", "幻影针",
                Material.IRON_SWORD,
                new String[]{"sharpness:10", "unbreaking:3"},
                new String[]{
                    "&8速攻武器",
                    "",
                    "&7右键光速斩。"
                },
                SkillBinder.Slot.RIGHT, SkillType.LUMINAL_STRIKE,
                null, null),

            build("axiom_scepter",
                "Axiom Scepter", "公理权杖",
                Material.BLAZE_ROD,
                new String[]{"unbreaking:10"},
                new String[]{
                    "&8爆发型法器",
                    "",
                    "&7右键共鸣锁定（准星指定，可换锁），Shift+右键公理破界。"
                },
                SkillBinder.Slot.RIGHT,       SkillType.HOMING,
                SkillBinder.Slot.SNEAK_RIGHT, SkillType.AXIOM_BREACH),

            build("nova_blade",
                "Nova Blade", "新星剑",
                Material.DIAMOND_SWORD,
                new String[]{"sharpness:6", "fire_aspect:4", "unbreaking:3"},
                new String[]{
                    "&8爆发型武器",
                    "",
                    "&7右键星力汇聚，Shift+右键以太裁决。"
                },
                SkillBinder.Slot.RIGHT,       SkillType.STELLAR_CONV,
                SkillBinder.Slot.SNEAK_RIGHT, SkillType.SWORD_RAIN),

            build("echo_slash",
                "Echo Strike", "回声斩",
                Material.IRON_SWORD,
                new String[]{"sharpness:10", "unbreaking:3"},
                new String[]{
                    "&f&l第一击，是剑；其余的，是回声。",
                    "&3霁光凝于刃上，回响化作万千星屑，追逐每一个被锁定的目标。"
                },
                SkillBinder.Slot.RIGHT,       SkillType.ECHO_SCATTER,
                SkillBinder.Slot.SNEAK_RIGHT, SkillType.ECHO_BARRAGE,
                true),

            build("still_veil",
                "Still Veil", "静幕",
                Material.IRON_SWORD,
                new String[]{"unbreaking:3"},
                new String[]{
                    "&f&l不是刀快，是这一秒被你裁掉了。",
                    "&3霁青与灰白漫开三十步，幕在，谁都不许先动。"
                },
                SkillBinder.Slot.RIGHT, SkillType.STILL_VEIL,
                null, null,
                true),

            build("pale_line",
                "Pale Line", "霁线",
                Material.IRON_SWORD,
                new String[]{"sharpness:3", "unbreaking:2"},
                new String[]{
                    "&f&l对准，按下。就这一条线。",
                    "&3霁青直线，打到谁算谁。日常够用。"
                },
                SkillBinder.Slot.RIGHT, SkillType.PALE_LINE,
                null, null,
                true),

            build("singularity",
                "Singularity", "奇点",
                Material.NETHERITE_SWORD,
                new String[]{"sharpness:10", "unbreaking:10", "looting:3", "knockback:2"},
                new String[]{
                    "&8高阶武器",
                    "",
                    "&7右键星力汇聚，Shift+右键公理破界。"
                },
                SkillBinder.Slot.RIGHT,       SkillType.STELLAR_CONV,
                SkillBinder.Slot.SNEAK_RIGHT, SkillType.AXIOM_BREACH),

            build("celestial_wing",
                "Celestial Wing", "天升之翼",
                Material.NETHERITE_SWORD,
                new String[]{"sharpness:5", "unbreaking:5"},
                new String[]{
                    "&8升空型武器",
                    "",
                    "&7右键光速斩，Shift+右键天升。"
                },
                SkillBinder.Slot.RIGHT,       SkillType.LUMINAL_STRIKE,
                SkillBinder.Slot.SNEAK_RIGHT, SkillType.CELESTIAL_ASCENT)
        );
    }

    /** 技能商店发货：霁线用预制，其余铁剑绑右键。 */
    public static ItemStack boundBlade(SkillType skill) {
        if (skill == SkillType.PALE_LINE) {
            Preset p = find("pale_line");
            if (p != null) return p.item().clone();
        }
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ColorUtil.colorize("&f&l" + skill.englishName + " &8「&7" + skill.chineseName + "&8」"));
        meta.setLore(List.of(
                ColorUtil.colorize("&7技能商店"),
                ColorUtil.colorize("&3右键释放 " + skill.displayName())));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        Enchantment unb = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
        if (unb != null) item.addUnsafeEnchantment(unb, 2);
        SkillBinder.bind(item, skill, SkillBinder.Slot.RIGHT);
        return item;
    }

    public static Preset find(String id) {
        if (id == null || presets == null) return null;
        String key = ALIASES.getOrDefault(id.toLowerCase(java.util.Locale.ROOT), id);
        for (Preset p : presets) {
            if (p.id().equalsIgnoreCase(key) || p.id().equalsIgnoreCase(id)) return p;
            if (p.enName().equalsIgnoreCase(id) || p.zhName().equalsIgnoreCase(id)) return p;
        }
        return null;
    }

    public static List<String> allIds() {
        List<String> ids = new ArrayList<>();
        if (presets == null) return ids;
        for (Preset p : presets) ids.add(p.id());
        return ids;
    }

    /** Tab 补全：匹配 ID / 别名 / 中英文名（中文查询补中文名，避免被客户端前缀过滤掉） */
    public static List<String> tabComplete(String input) {
        String raw = input == null ? "" : input.trim();
        String q = raw.toLowerCase(java.util.Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (presets == null) return out;

        boolean hasCjk = raw.codePoints().anyMatch(
                cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);

        for (Preset p : presets) {
            boolean hit = q.isEmpty()
                    || p.id().toLowerCase(java.util.Locale.ROOT).startsWith(q)
                    || p.id().toLowerCase(java.util.Locale.ROOT).contains(q)
                    || p.enName().toLowerCase(java.util.Locale.ROOT).contains(q)
                    || p.zhName().contains(raw)
                    || p.zhName().toLowerCase(java.util.Locale.ROOT).contains(q);
            if (!hit) continue;
            if (hasCjk && p.zhName().contains(raw)) {
                if (!out.contains(p.zhName())) out.add(p.zhName());
            }
            if (!out.contains(p.id())) out.add(p.id());
        }
        for (Map.Entry<String, String> e : ALIASES.entrySet()) {
            String alias = e.getKey();
            if (q.isEmpty() || alias.startsWith(q) || alias.contains(q)) {
                if (!out.contains(alias) && find(e.getValue()) != null) out.add(alias);
            }
        }
        return out;
    }

    private static Preset build(String id,
                                String enName, String zhName,
                                Material mat,
                                String[] enchants,
                                String[] loreLines,
                                SkillBinder.Slot slot1, SkillType skill1,
                                SkillBinder.Slot slot2, SkillType skill2) {
        return build(id, enName, zhName, mat, enchants, loreLines, slot1, skill1, slot2, skill2, false);
    }

    private static Preset build(String id,
                                String enName, String zhName,
                                Material mat,
                                String[] enchants,
                                String[] loreLines,
                                SkillBinder.Slot slot1, SkillType skill1,
                                SkillBinder.Slot slot2, SkillType skill2,
                                boolean showEnchants) {

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new Preset(id, enName, zhName, item);

        meta.setDisplayName(ColorUtil.colorize("&f&l" + enName + " &8「&7" + zhName + "&8」"));
        List<String> lore = new ArrayList<>();
        for (String l : loreLines) lore.add(ColorUtil.colorize(l));
        meta.setLore(lore);
        if (!showEnchants) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);

        for (String spec : enchants) {
            String[] parts = spec.split(":");
            if (parts.length != 2) continue;
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(parts[0]));
            if (ench == null) continue;
            item.addUnsafeEnchantment(ench, Integer.parseInt(parts[1]));
        }

        if (slot1 != null && skill1 != null) SkillBinder.bind(item, skill1, slot1);
        if (slot2 != null && skill2 != null) SkillBinder.bind(item, skill2, slot2);
        return new Preset(id, enName, zhName, item.clone());
    }
}
