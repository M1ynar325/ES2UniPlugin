package com.etherstories.escore.hooks;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Kit;
import com.earth2me.essentials.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EssentialsHook {

    private Essentials essentials;

    public void hook() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
        if (plugin instanceof Essentials ess && ess.isEnabled()) {
            this.essentials = ess;
        }
    }

    public boolean isEnabled() {
        return essentials != null;
    }

    /** Returns the list of home names for this player, or empty list if unavailable. */
    public List<String> getHomes(Player player) {
        if (!isEnabled()) return Collections.emptyList();
        try {
            User user = essentials.getUser(player);
            return user.getHomes();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<String> getKitNames() {
        if (!isEnabled()) return Collections.emptyList();
        try {
            Set<String> keys = essentials.getKits().getKitKeys();
            List<String> names = new ArrayList<>(keys);
            names.sort(String.CASE_INSENSITIVE_ORDER);
            return names;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public boolean hasKit(String name) {
        if (!isEnabled() || name == null) return false;
        try {
            String key = name.toLowerCase(Locale.ENGLISH).replace('.', '_').replace('/', '_');
            return essentials.getKits().getKit(key) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 发放 Essentials Kit（含权限/冷却/扣费检查）。
     * @return null 成功；否则错误信息
     */
    public String giveKit(Player player, String kitName) {
        if (!isEnabled()) return "Essentials 未启用";
        try {
            User user = essentials.getUser(player);
            Kit kit = new Kit(kitName.toLowerCase(Locale.ENGLISH), essentials);
            kit.checkPerms(user);
            kit.checkDelay(user);
            kit.checkAffordable(user);
            kit.setTime(user);
            kit.expandItems(user);
            kit.chargeUser(user);
            return null;
        } catch (Exception e) {
            String msg = e.getMessage();
            return (msg == null || msg.isBlank()) ? "无法领取该套件" : msg;
        }
    }
}
