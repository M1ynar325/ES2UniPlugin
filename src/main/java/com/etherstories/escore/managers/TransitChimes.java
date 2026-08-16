package com.etherstories.escore.managers;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 到站 / 发车：只用音符盒，一拍一个音，旋律能听出来。
 */
public final class TransitChimes {

    public record N(Sound sound, float pitch, int tick) {}

    private TransitChimes() {}

    public static void play(Plugin plugin, Player player, float vol, N... notes) {
        float v = Math.min(2.4f, Math.max(0.9f, vol) * 1.35f);
        for (N n : notes) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                player.playSound(player.getLocation(), n.sound, SoundCategory.RECORDS, v, n.pitch);
            }, n.tick);
        }
    }

    private static final Sound BELL = Sound.BLOCK_NOTE_BLOCK_BELL;
    private static final Sound CHIME = Sound.BLOCK_NOTE_BLOCK_CHIME;
    private static final Sound IRON = Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE;
    private static final Sound XYLO = Sound.BLOCK_NOTE_BLOCK_XYLOPHONE;
    private static final Sound BASS = Sound.BLOCK_NOTE_BLOCK_BASS;

    /** 一串同乐器，步长 step tick；stack 时连续同音并成一拍（加厚） */
    private static N[] line(Sound inst, int step, boolean stack, float... pitches) {
        if (!stack) {
            N[] out = new N[pitches.length];
            for (int i = 0; i < pitches.length; i++) out[i] = new N(inst, pitches[i], i * step);
            return out;
        }
        List<N> out = new ArrayList<>();
        int i = 0;
        int tick = 0;
        while (i < pitches.length) {
            float p = pitches[i];
            int n = 1;
            while (i + n < pitches.length && pitches[i + n] == p) n++;
            out.add(new N(inst, p, tick));
            if (n >= 2) out.add(new N(CHIME, p, tick));
            if (n >= 3) out.add(new N(IRON, p, tick));
            tick += step;
            i += n;
        }
        return out.toArray(N[]::new);
    }

    public static void arrive(Plugin plugin, Player player, String preset, float vol) {
        arrive(plugin, player, preset, vol, true);
    }

    public static void arrive(Plugin plugin, Player player, String preset, float vol, boolean stack) {
        N[] notes = switch (preset == null ? "jr" : preset.toLowerCase()) {
            case "yamanote" -> line(BELL, 3, stack, 1.19f, 1.19f, 0.89f, 1.19f, 1.41f);
            case "mtr" -> line(BELL, 4, stack, 1.59f, 1.00f, 1.59f, 1.00f);
            case "tube" -> line(IRON, 3, stack, 1.78f, 1.78f, 0.71f, 0.71f);
            case "osaka" -> line(CHIME, 3, stack, 0.84f, 1.06f, 1.33f, 1.06f);
            case "chime" -> line(CHIME, 3, stack, 1.78f, 1.59f, 1.41f, 1.19f, 1.00f, 0.84f);
            case "cr" -> line(BELL, 4, stack, 0.84f, 0.84f, 1.19f, 1.41f, 1.19f);
            case "crh" -> line(IRON, 2, stack, 1.19f, 1.41f, 1.59f, 1.78f, 1.59f);
            case "beijing" -> line(BELL, 4, stack, 1.41f, 1.41f, 0.84f, 0.84f);
            case "shanghai" -> line(CHIME, 3, stack, 1.00f, 1.26f, 1.59f, 1.26f);
            case "guangzhou" -> line(BELL, 3, stack, 1.41f, 1.19f, 1.41f, 1.00f);
            default -> line(BELL, 3, stack, 1.00f, 1.19f, 1.41f, 1.19f);
        };
        play(plugin, player, vol, notes);
    }

    public static void depart(Plugin plugin, Player player, String preset, float vol) {
        depart(plugin, player, preset, vol, true);
    }

    public static void depart(Plugin plugin, Player player, String preset, float vol, boolean stack) {
        N[] notes = switch (preset == null ? "jrgo" : preset.toLowerCase()) {
            case "doors" -> line(BELL, 3, stack, 1.19f, 1.19f, 1.19f, 0.71f);
            case "whistle" -> line(BASS, 6, stack, 0.59f, 0.59f, 0.71f);
            case "shinkansen" -> line(IRON, 3, stack, 0.84f, 1.00f, 1.26f, 1.59f, 1.78f);
            case "crgo" -> line(BELL, 3, stack, 1.00f, 1.00f, 1.00f, 0.71f);
            case "crhgo" -> line(CHIME, 3, stack, 1.59f, 1.26f, 0.84f);
            default -> line(XYLO, 3, stack, 1.41f, 1.19f, 1.00f, 0.84f);
        };
        play(plugin, player, vol, notes);
    }
}
