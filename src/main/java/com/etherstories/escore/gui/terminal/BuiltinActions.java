package com.etherstories.escore.gui.terminal;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.gui.PlayerSelectGUI;
import com.etherstories.escore.managers.DeathManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

final class BuiltinActions {
    private BuiltinActions() {}

    static void register(TerminalHub hub, ES2UniPlugin plugin) {
        hub.registerAction("close", (p, s, raw) -> hub.close(p));
        hub.registerAction("head", (p, s, raw) -> p.sendMessage(ColorUtil.colorize(
                plugin.getConfigManager().getVersionDisplayTemplate()
                        .replace("{version}", plugin.getDescription().getVersion())
                        .replace("{mc_version}", plugin.getServer().getVersion())
                        .replace("{authors}", String.join(", ", plugin.getDescription().getAuthors())))));
        hub.registerAction("summary", (p, s, raw) -> hub.refresh(p));
        hub.registerAction("web-pair", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(p, "ecos web")));
        hub.registerAction("aura", (p, s, raw) -> later(plugin, () -> plugin.getAuraShopGUI().open(p)));
        hub.registerAction("skillshop", (p, s, raw) -> later(plugin, () -> plugin.getSkillShopGUI().open(p)));
        hub.registerAction("adm-skillshop", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            later(plugin, () -> plugin.getSkillShopGUI().openAdmin(p));
        });
        hub.registerAction("friends", (p, s, raw) -> {
            s.put("from", "social");
            hub.open(p, "friends-list");
        });
        hub.registerAction("mail", (p, s, raw) -> {
            s.put("from", s.pageId() == null ? "overview" : s.pageId());
            hub.open(p, "mail-list");
        });
        hub.registerAction("online", (p, s, raw) -> {
            s.put("from", "social");
            hub.open(p, "online-list");
        });
        hub.registerAction("notices", (p, s, raw) -> {
            plugin.getNewbieGuideManager().mark(p.getUniqueId(),
                    com.etherstories.escore.managers.NewbieGuideManager.Step.READ_NOTICE);
            s.put("from", "social");
            hub.open(p, "notices-list");
        });
        hub.registerAction("leaderboard", (p, s, raw) -> later(plugin, () -> plugin.getLeaderboardGUI().open(p)));
        hub.registerAction("waypoints", (p, s, raw) -> {
            s.put("from", "travel");
            hub.open(p, "wp-list");
        });
        hub.registerAction("status", (p, s, raw) -> plugin.getAnvilInputGUI().openForStatus(p));
        hub.registerAction("death", (p, s, raw) -> {
            List<DeathManager.DeathRecord> deaths = plugin.getDeathManager().get(p.getUniqueId());
            if (deaths.isEmpty()) {
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7暂无死亡记录"));
                return;
            }
            DeathManager.DeathRecord last = deaths.get(deaths.size() - 1);
            org.bukkit.World w = Bukkit.getWorld(last.world());
            if (w != null) {
                p.teleport(new org.bukkit.Location(w, last.x() + 0.5, last.y(), last.z() + 0.5));
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已到最近死亡点 &f" + last.date()));
            }
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7死亡记录:"));
            for (DeathManager.DeathRecord d : deaths)
                p.sendMessage(ColorUtil.colorize("  &8" + d.date() + "  &7"
                        + d.world() + " (" + d.x() + "," + d.y() + "," + d.z()
                        + ")  &8" + d.cause()));
        });
        hub.registerAction("checkin", (p, s, raw) -> later(plugin, () -> plugin.getCheckInCalendarGUI().open(p)));
        hub.registerAction("balance", (p, s, raw) -> {
            if (!plugin.getVaultHook().isEnabled()) {
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7经济系统不可用"));
                return;
            }
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7余额: &f"
                    + plugin.getVaultHook().format(plugin.getVaultHook().getBalance(p))));
        });
        hub.registerAction("estate", (p, s, raw) -> later(plugin, () -> plugin.getEstateBuildingsGUI().open(p)));
        hub.registerAction("estate-sale", (p, s, raw) -> later(plugin, () -> plugin.getEstateSaleGUI().open(p)));
        hub.registerAction("estate-tools", (p, s, raw) -> later(plugin, () -> plugin.getEstateToolsGUI().open(p)));
        hub.registerAction("adm-estate", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            later(plugin, () -> plugin.getEstateMineGUI().openAdmin(p));
        });
        hub.registerAction("estate-list", (p, s, raw) -> later(plugin, () -> plugin.getEstateMineGUI().openMine(p)));
        hub.registerAction("hotel", (p, s, raw) -> later(plugin, () -> plugin.getHotelListGUI().open(p)));
        hub.registerAction("hotel-checkout", (p, s, raw) -> {
            var stay = plugin.getHotelManager().stayOf(p.getUniqueId());
            String err = plugin.getHotelManager().checkout(p, stay);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &7已退房" : "&8[酒店] &c" + err));
            later(plugin, () -> hub.open(p));
        });
        hub.registerAction("adm-grant", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            later(plugin, () -> plugin.getAdminGrantGUI().open(p));
        });
        hub.registerAction("trade", (p, s, raw) -> later(plugin, () -> plugin.getTradeBoardGUI().open(p)));
        hub.registerAction("lucky", (p, s, raw) -> {
            if (plugin.getLuckyBlockManager().isOpening(p.getUniqueId())) {
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7正在开启中…"));
                return;
            }
            boolean before = plugin.getLuckyBlockManager().hasClaimedToday(p.getUniqueId());
            plugin.getLuckyBlockManager().claimAnimated(p, msg -> {
                p.sendMessage(ColorUtil.colorize(msg));
                if (!before && plugin.getLuckyBlockManager().hasClaimedToday(p.getUniqueId())) {
                    plugin.getNewbieGuideManager().mark(p.getUniqueId(),
                            com.etherstories.escore.managers.NewbieGuideManager.Step.CLAIM_LUCKY);
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> hub.open(p), 8L);
            });
        });
        hub.registerAction("showcase", (p, s, raw) -> later(plugin, () -> plugin.getShowcaseGUI().open(p)));
        hub.registerAction("municipal", (p, s, raw) -> later(plugin, () -> plugin.getMunicipalOfficeGUI().open(p)));
        hub.registerAction("newbie", (p, s, raw) -> later(plugin, () -> plugin.getNewbieGuideGUI().open(p)));
        hub.registerAction("tps", (p, s, raw) -> {
            double[] all = com.etherstories.escore.utils.TPSUtil.getAllTPS();
            double mspt = com.etherstories.escore.utils.TPSUtil.getMSPT();
            p.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getTpsDisplayTemplate()
                            .replace("{tps_1m}", com.etherstories.escore.utils.TPSUtil.formatTPS(all[0]))
                            .replace("{tps_5m}", com.etherstories.escore.utils.TPSUtil.formatTPS(all[1]))
                            .replace("{tps_15m}", com.etherstories.escore.utils.TPSUtil.formatTPS(all[2]))
                            .replace("{mspt}", com.etherstories.escore.utils.TPSUtil.formatMSPT(mspt))));
        });
        hub.registerAction("events", (p, s, raw) -> later(plugin, () -> plugin.getEventListGUI().open(p)));
        hub.registerAction("afk", (p, s, raw) -> plugin.getAfkManager().manualToggle(p));
        hub.registerAction("home", (p, s, raw) -> {
            if (!plugin.getEssentialsHook().isEnabled()) return;
            s.put("from", "travel");
            hub.open(p, "homes");
        });
        hub.registerAction("sethome", (p, s, raw) -> {
            if (!plugin.getEssentialsHook().isEnabled()) return;
            later(plugin, () -> p.performCommand("sethome"));
        });
        hub.registerAction("tpa", (p, s, raw) -> {
            if (!plugin.getEssentialsHook().isEnabled()) return;
            later(plugin, () -> plugin.getPlayerSelectGUI().open(p, PlayerSelectGUI.SelectContext.TPA));
        });
        hub.registerAction("tpahere", (p, s, raw) -> {
            if (!plugin.getEssentialsHook().isEnabled()) return;
            later(plugin, () -> plugin.getPlayerSelectGUI().open(p, PlayerSelectGUI.SelectContext.TPA_HERE));
        });
        hub.registerAction("pay", (p, s, raw) -> {
            if (!plugin.getVaultHook().isEnabled()) return;
            later(plugin, () -> plugin.getPlayerSelectGUI().open(p, PlayerSelectGUI.SelectContext.PAY));
        });
        hub.registerAction("adm-events", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            later(plugin, () -> plugin.getAdminEventGUI().open(p));
        });
        hub.registerAction("adm-bc", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            later(plugin, () -> plugin.getAdminBroadcastGUI().open(p));
        });
        hub.registerAction("adm-tax", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            later(plugin, () -> plugin.getAdminTaxGUI().open(p));
        });
        hub.registerAction("adm-reload", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.reload();
            plugin.getAuditLogManager().log(p, "RELOAD", "config reload");
            p.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getReloadMessage()));
            later(plugin, () -> hub.open(p));
        });
        hub.registerAction("adm-territory", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            later(plugin, () -> plugin.getAdminTerritoryGUI().open(p));
        });
        hub.registerAction("adm-regions", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            later(plugin, () -> plugin.getAdminRegionGUI().open(p));
        });
        hub.registerAction("hot-places", (p, s, raw) -> later(plugin, () -> plugin.getTerritoryListGUI().open(p)));
        hub.registerAction("my-territory", (p, s, raw) -> later(plugin, () -> plugin.getMyTerritoryGUI().open(p)));
        hub.registerAction("kit", (p, s, raw) -> later(plugin, () -> plugin.getKitListGUI().open(p)));
        hub.registerAction("adm-kit", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            later(plugin, () -> plugin.getAdminKitGUI().open(p));
        });
        hub.registerAction("music", (p, s, raw) -> later(plugin, () -> plugin.getMusicMenuGUI().open(p)));
        hub.registerAction("link", (p, s, raw) -> {
            if (Bukkit.getPluginManager().getPlugin("ESLink") == null
                    || !Bukkit.getPluginManager().getPlugin("ESLink").isEnabled()) {
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &c未安装 ESLink"));
                return;
            }
            later(plugin, () -> p.performCommand("link"));
        });
        hub.registerAction("music-listen", (p, s, raw) -> {
            if (!plugin.getAllMusicHook().ensureHooked(plugin.getLogger())) {
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &cAllMusic 未加载"));
                return;
            }
            boolean muted = plugin.getAllMusicHook().isMuted(p);
            plugin.getAllMusicHook().setListening(plugin, p, muted);
            p.sendMessage(ColorUtil.colorize(!muted
                    ? "&8[ECOS] &c已关闭听歌（静音）。别人点歌你仍能看到提示，但不会播放。"
                    : "&8[ECOS] &a已开启听歌。"));
            Bukkit.getScheduler().runTaskLater(plugin, () -> hub.open(p), 3L);
        });
    }

    private static void later(ES2UniPlugin plugin, Runnable run) {
        Bukkit.getScheduler().runTask(plugin, run);
    }
}
