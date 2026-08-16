package com.etherstories.escore.tasks;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.managers.AFKManager;
import com.etherstories.escore.managers.PlaytimeManager;
import com.etherstories.escore.utils.ColorUtil;
import com.etherstories.escore.utils.TPSUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TabTask extends BukkitRunnable {

    private final ES2UniPlugin plugin;

    public TabTask(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;

        String tpsFormatted  = TPSUtil.formatTPS(TPSUtil.getTPS());
        String msptFormatted = TPSUtil.formatMSPT(TPSUtil.getMSPT());
        int online = Bukkit.getOnlinePlayers().size();
        int max    = Bukkit.getMaxPlayers();

        String headerTemplate = plugin.getConfigManager().getTabHeader();
        String footerTemplate = plugin.getConfigManager().getTabFooter();
        String nameFormat     = plugin.getConfigManager().getTabPlayerNameFormat();
        String afkNameFormat  = plugin.getConfigManager().getTabAfkNameFormat();

        AFKManager afk         = plugin.getAfkManager();
        PlaytimeManager playtime = plugin.getPlaytimeManager();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String pt = playtime.getFormatted(player);
            boolean isAfk = afk.isAFK(player);

            // ── B: player list name ──────────────────────────────────────
            String nameFmt = isAfk ? afkNameFormat : nameFormat;
            String listName = nameFmt
                    .replace("{player}", player.getName())
                    .replace("{playtime}", pt);
            player.setPlayerListName(ColorUtil.colorize(listName));

            // ── A: header / footer ───────────────────────────────────────
            String header = applyCommon(headerTemplate, player, tpsFormatted, msptFormatted, online, max, pt);
            String footer = applyCommon(footerTemplate, player, tpsFormatted, msptFormatted, online, max, pt);

            player.setPlayerListHeaderFooter(
                    ColorUtil.colorize(header),
                    ColorUtil.colorize(footer)
            );
        }
    }

    private String applyCommon(String template, Player player,
                                String tps, String mspt, int online, int max, String playtime) {
        return template
                .replace("{tps}", tps)
                .replace("{mspt}", mspt)
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max))
                .replace("{player}", player.getName())
                .replace("{ping}", String.valueOf(player.getPing()))
                .replace("{playtime}", playtime);
    }
}
