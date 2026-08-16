package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 节拍盲盒（原幸运方块）：每日 1 次 + 额外次数券。
 * fx-style: hypixel（音高爬升后爆发）| soft（轻量点击风）
 */
public class LuckyBlockManager {

    private final ES2UniPlugin plugin;
    private final File dataFile;
    private final Set<UUID> claimed = new HashSet<>();
    private final Set<UUID> opening = new HashSet<>();
    private final Map<UUID, Integer> tickets = new HashMap<>();
    private String day = LocalDate.now().toString();
    private final Random rng = new Random();

    public LuckyBlockManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "lucky.yml");
        load();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("lucky-block.enabled", true);
    }

    public String displayName() {
        return plugin.getConfig().getString("lucky-block.display-name", "幸运方块");
    }

    /** hypixel | soft */
    public String fxStyle() {
        return plugin.getConfig().getString("lucky-block.fx-style", "hypixel").toLowerCase();
    }

    public boolean hasClaimedToday(UUID uuid) {
        ensureDay();
        return claimed.contains(uuid);
    }

    public int getTickets(UUID uuid) {
        return tickets.getOrDefault(uuid, 0);
    }

    public boolean canDraw(UUID uuid) {
        ensureDay();
        return !claimed.contains(uuid) || getTickets(uuid) > 0;
    }

    public boolean isOpening(UUID uuid) {
        return opening.contains(uuid);
    }

    public String giveTickets(UUID uuid, int n) {
        if (n <= 0) return "数量无效";
        tickets.put(uuid, getTickets(uuid) + n);
        save();
        return null;
    }

    public String claim(Player player) {
        return claimInternal(player);
    }

    public void claimAnimated(Player player, Consumer<String> onDone) {
        if (!isEnabled()) {
            onDone.accept("&8[ECOS] &7" + displayName() + "已关闭");
            return;
        }
        ensureDay();
        if (!canDraw(player.getUniqueId())) {
            onDone.accept("&8[ECOS] &7今天已抽过，且没有额外次数");
            return;
        }
        if (!opening.add(player.getUniqueId())) {
            onDone.accept("&8[ECOS] &7正在开启中…");
            return;
        }

        if ("soft".equals(fxStyle())) {
            playSoftFx(player, onDone);
        } else {
            playHypixelFx(player, onDone);
        }
    }

    /** Hypixel 风：音高逐渐上升 → 最后爆发 */
    private void playHypixelFx(Player player, Consumer<String> onDone) {
        Location loc = player.getLocation().add(0, 1.2, 0);
        player.playSound(loc, Sound.BLOCK_CHEST_OPEN, 1f, 0.9f);

        int steps = 12;
        for (int i = 0; i < steps; i++) {
            final int step = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                Location l = player.getLocation().add(0, 1.2, 0);
                float pitch = 0.5f + step * (1.5f / steps);
                player.playSound(l, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, pitch);
                player.playSound(l, Sound.BLOCK_NOTE_BLOCK_HARP, 0.25f, pitch);
                player.getWorld().spawnParticle(Particle.CRIT, l, 6, 0.3, 0.3, 0.3, 0.02);
                player.getWorld().spawnParticle(Particle.NOTE, l, 2, 0.2, 0.2, 0.2, 0);
                bar(player, step, steps - 1);
            }, 2L + i * 2L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            opening.remove(player.getUniqueId());
            if (!player.isOnline()) return;
            String result = claimInternal(player);
            Location l = player.getLocation().add(0, 1.2, 0);
            boolean rare = result.contains("补签券");
            player.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 0.55f, 1.4f);
            player.playSound(l, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.2f);
            if (rare) {
                player.playSound(l, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.1f);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, l, 50, 0.5, 0.7, 0.5, 0.2);
            } else {
                player.playSound(l, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);
                player.getWorld().spawnParticle(Particle.FIREWORK, l, 30, 0.4, 0.5, 0.4, 0.08);
            }
            player.getWorld().spawnParticle(Particle.FLASH, l, 1, 0, 0, 0, 0);
            barDone(player);
            onDone.accept(result);
        }, 2L + steps * 2L + 4L);
    }

    /** 轻量风：短点击 + 收束粒子（保留） */
    private void playSoftFx(Player player, Consumer<String> onDone) {
        for (int i = 0; i < 8; i++) {
            final int step = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                Location l = player.getLocation().add(0, 1.2, 0);
                player.playSound(l, Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 0.7f + step * 0.12f);
                player.playSound(l, Sound.UI_BUTTON_CLICK, 0.3f, 1.4f);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, l, 4, 0.25, 0.25, 0.25, 0);
                bar(player, step, 7);
            }, 3L + i * 3L);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            opening.remove(player.getUniqueId());
            if (!player.isOnline()) return;
            String result = claimInternal(player);
            Location l = player.getLocation().add(0, 1.2, 0);
            player.playSound(l, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.3f);
            player.playSound(l, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
            player.getWorld().spawnParticle(Particle.FIREWORK, l, 15, 0.3, 0.4, 0.3, 0.05);
            barDone(player);
            onDone.accept(result);
        }, 30L);
    }

    private void bar(Player player, int step, int max) {
        ES2UniPlugin.getInstance().getActionBarManager().sendTemp(player,
                "&d✦ " + displayName() + " "
                        + "■".repeat(step + 1) + "&8" + "□".repeat(Math.max(0, max - step)),
                20);
    }

    private void barDone(Player player) {
        ES2UniPlugin.getInstance().getActionBarManager().sendTemp(player,
                "&a✔ " + displayName() + " 开启完成", 40);
    }

    private String claimInternal(Player player) {
        if (!isEnabled()) return "&8[ECOS] &7" + displayName() + "已关闭";
        ensureDay();
        UUID id = player.getUniqueId();
        boolean usedTicket = false;
        if (claimed.contains(id)) {
            int t = getTickets(id);
            if (t <= 0) return "&8[ECOS] &7今天已抽过，且没有额外次数";
            tickets.put(id, t - 1);
            usedTicket = true;
        } else {
            claimed.add(id);
        }
        save();

        String tip = usedTicket ? " &8(使用额外次数，剩余 " + getTickets(id) + ")" : "";
        int roll = rng.nextInt(100);
        if (roll < 50 && plugin.getVaultHook().isEnabled()) {
            double amount = 50 + rng.nextInt(151);
            plugin.getVaultHook().deposit(player, amount);
            return "&8[ECOS] &d" + displayName() + " &7→ &e"
                    + plugin.getVaultHook().format(amount) + tip;
        }
        if (roll < 80) {
            ItemStack item = new ItemStack(Material.COOKED_BEEF, 8 + rng.nextInt(9));
            player.getInventory().addItem(item).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            return "&8[ECOS] &d" + displayName() + " &7→ &f烤牛肉 x" + item.getAmount() + tip;
        }
        if (roll < 95) {
            ItemStack item = new ItemStack(Material.IRON_INGOT, 2 + rng.nextInt(5));
            player.getInventory().addItem(item).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            return "&8[ECOS] &d" + displayName() + " &7→ &f铁锭 x" + item.getAmount() + tip;
        }
        plugin.getCheckInManager().giveMakeupTickets(player.getUniqueId(), 1);
        return "&8[ECOS] &d" + displayName() + " &7→ &a补签券 x1" + tip;
    }

    private void ensureDay() {
        String today = LocalDate.now().toString();
        if (!today.equals(day)) {
            day = today;
            claimed.clear();
            save();
        }
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        day = cfg.getString("day", day);
        for (String s : cfg.getStringList("claimed")) {
            try { claimed.add(UUID.fromString(s)); } catch (Exception ignored) {}
        }
        if (cfg.isConfigurationSection("tickets")) {
            for (String k : cfg.getConfigurationSection("tickets").getKeys(false)) {
                try {
                    tickets.put(UUID.fromString(k), cfg.getInt("tickets." + k));
                } catch (Exception ignored) {}
            }
        }
        ensureDay();
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("day", day);
        List<String> list = new ArrayList<>();
        claimed.forEach(u -> list.add(u.toString()));
        cfg.set("claimed", list);
        for (var e : tickets.entrySet()) {
            if (e.getValue() > 0) cfg.set("tickets." + e.getKey(), e.getValue());
        }
        try { cfg.save(dataFile); }
        catch (IOException e) { plugin.getLogger().warning("lucky.yml 保存失败: " + e.getMessage()); }
    }
}
