package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.items.WeaponPreset;
import com.etherstories.escore.weapons.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 技能商店上架：哪些技能可买、多少钱。买到的是绑好技能的武器。 */
public class SkillShopManager {

    public record Listing(SkillType skill, boolean enabled, double price) {}

    private final ES2UniPlugin plugin;
    private final File file;
    private final Map<SkillType, Listing> listings = new EnumMap<>(SkillType.class);

    public SkillShopManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "skillshop.yml");
        load();
    }

    public void load() {
        listings.clear();
        if (!file.exists()) {
            set(SkillType.PALE_LINE, true, suggested(SkillType.PALE_LINE));
            save();
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yml.getConfigurationSection("listings");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            SkillType t = SkillType.fromKey(key);
            if (t == null) continue;
            boolean on = sec.getBoolean(key + ".enabled", false);
            double price = Math.max(0, sec.getDouble(key + ".price", suggested(t)));
            listings.put(t, new Listing(t, on, price));
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Listing l : listings.values()) {
            String k = "listings." + l.skill.configKey;
            yml.set(k + ".enabled", l.enabled);
            yml.set(k + ".price", l.price);
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存 skillshop.yml: " + e.getMessage());
        }
    }

    public List<Listing> forSale() {
        List<Listing> out = new ArrayList<>();
        for (SkillType t : SkillType.values()) {
            Listing l = listings.get(t);
            if (l != null && l.enabled) out.add(l);
        }
        return out;
    }

    public Listing of(SkillType skill) {
        Listing l = listings.get(skill);
        if (l != null) return l;
        return new Listing(skill, false, suggested(skill));
    }

    public boolean isForSale(SkillType skill) {
        Listing l = listings.get(skill);
        return l != null && l.enabled;
    }

    public double price(SkillType skill) {
        return of(skill).price;
    }

    public void setEnabled(SkillType skill, boolean on) {
        Listing cur = of(skill);
        listings.put(skill, new Listing(skill, on, cur.price));
        save();
    }

    public void setPrice(SkillType skill, double price) {
        Listing cur = of(skill);
        listings.put(skill, new Listing(skill, cur.enabled, Math.max(0, price)));
        save();
    }

    public void set(SkillType skill, boolean on, double price) {
        listings.put(skill, new Listing(skill, on, Math.max(0, price)));
        save();
    }

    public String buy(Player player, SkillType skill) {
        if (!isForSale(skill)) return "该技能未上架";
        if (!plugin.getVaultHook().isEnabled()) return "经济系统不可用";
        double cost = price(skill);
        if (cost > 0) {
            if (plugin.getVaultHook().getBalance(player) + 1e-6 < cost) {
                return "余额不足（需要 " + plugin.getVaultHook().format(cost) + "）";
            }
            String err = plugin.getVaultHook().withdraw(player, cost);
            if (err != null) return err;
        }
        ItemStack goods = WeaponPreset.boundBlade(skill);
        var leftover = player.getInventory().addItem(goods);
        leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
        return null;
    }

    public static double suggested(SkillType t) {
        return switch (t) {
            case PALE_LINE -> 400;
            case LUMINAL_STRIKE -> 800;
            case HOMING -> 1500;
            case GLEAM_ARC -> 2000;
            case CELESTIAL_ASCENT -> 2000;
            case SWORD_RAIN -> 2500;
            case ECHO_SCATTER, ECHO_BARRAGE -> 3000;
            case STELLAR_CONV -> 3500;
            case AXIOM_BREACH -> 4000;
            case STILL_VEIL -> 5000;
        };
    }
}
