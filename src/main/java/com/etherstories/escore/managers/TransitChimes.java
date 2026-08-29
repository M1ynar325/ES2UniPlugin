package com.etherstories.escore.managers;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 到站 / 发车：只用音符盒「音符」（竖琴），旋律略长，贴近地铁 / 高铁广播。
 */
public final class TransitChimes {

    public record N(Sound sound, float pitch, int tick) {}

    private TransitChimes() {}

    private static final Sound NOTE = Sound.BLOCK_NOTE_BLOCK_HARP;

    private static final float A3  = 0.59f;
    private static final float B3  = 0.67f;
    private static final float C4  = 0.71f;
    private static final float D4  = 0.84f;
    private static final float E4  = 0.89f;
    private static final float Fs4 = 1.00f;
    private static final float G4  = 1.06f;
    private static final float A4  = 1.19f;
    private static final float As4 = 1.26f;
    private static final float B4  = 1.33f;
    private static final float C5  = 1.41f;
    private static final float D5  = 1.59f;
    private static final float E5  = 1.78f;
    private static final float Fs5 = 2.00f;

    public static void play(Plugin plugin, Player player, float vol, N... notes) {
        float v = Math.min(2.4f, Math.max(1.0f, vol) * 1.55f);
        for (N n : notes) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                player.playSound(player.getLocation(), n.sound, SoundCategory.RECORDS, v, n.pitch);
            }, n.tick);
        }
    }

    /** pitch,dur, pitch,dur…；pitch=0 为空拍。stack 时叠低八度。 */
    private static N[] score(boolean stack, float... pd) {
        List<N> out = new ArrayList<>();
        int tick = 0;
        for (int i = 0; i + 1 < pd.length; i += 2) {
            float p = pd[i];
            int dur = Math.max(1, (int) pd[i + 1]);
            if (p >= 0.5f) {
                out.add(new N(NOTE, p, tick));
                if (stack) {
                    float oct = p >= 1.0f ? p * 0.5f : Math.min(2.0f, p * 2.0f);
                    oct = Math.min(2.0f, Math.max(0.5f, oct));
                    if (Math.abs(oct - p) > 0.04f) out.add(new N(NOTE, oct, tick));
                }
            }
            tick += dur;
        }
        return out.toArray(N[]::new);
    }

    public static void arrive(Plugin plugin, Player player, String preset, float vol) {
        arrive(plugin, player, preset, vol, true);
    }

    public static void arrive(Plugin plugin, Player player, String preset, float vol, boolean stack) {
        N[] notes = switch (preset == null ? "jr" : preset.toLowerCase()) {
            case "yamanote" -> score(stack,
                    A4, 4, A4, 4, E4, 6, A4, 4, C5, 6,
                    A4, 4, A4, 4, E4, 6, A4, 8, C5, 12);
            case "mtr" -> score(stack,
                    D5, 5, Fs4, 5, D5, 5, Fs4, 5,
                    D5, 5, Fs4, 5, C5, 6, Fs4, 12);
            case "tube" -> score(stack,
                    E5, 4, E5, 4, C4, 6, C4, 10, 0, 4,
                    E5, 4, E5, 4, C4, 6, C4, 12);
            case "osaka" -> score(stack,
                    D4, 5, G4, 5, B4, 5, G4, 5,
                    D4, 5, G4, 5, B4, 6, D5, 8, B4, 12);
            case "chime" -> score(stack,
                    E5, 4, D5, 4, C5, 4, A4, 4, Fs4, 4, D4, 5,
                    Fs4, 5, D4, 5, C4, 12);
            case "cr" -> score(stack,
                    D4, 6, D4, 6, A4, 6, C5, 6, A4, 10, 0, 4,
                    Fs4, 6, A4, 6, C5, 6, D5, 6, C5, 12);
            case "crh" -> score(stack,
                    Fs4, 5, A4, 5, C5, 5, D5, 5, E5, 8,
                    D5, 5, C5, 5, D5, 8, E5, 14);
            case "beijing" -> score(stack,
                    C5, 5, C5, 5, D4, 5, D4, 8,
                    C5, 5, A4, 5, Fs4, 5, D4, 5, Fs4, 12);
            case "shanghai" -> score(stack,
                    Fs4, 5, As4, 5, D5, 5, As4, 5,
                    Fs4, 5, As4, 5, D5, 6, Fs5, 8, D5, 12);
            case "guangzhou" -> score(stack,
                    C5, 5, A4, 5, C5, 5, Fs4, 6,
                    A4, 5, C5, 5, D5, 6, C5, 12);
            default -> score(stack,
                    Fs4, 5, A4, 5, C5, 5, A4, 5,
                    Fs4, 5, A4, 5, C5, 8, D5, 12);
        };
        play(plugin, player, vol, notes);
    }

    public static void depart(Plugin plugin, Player player, String preset, float vol) {
        depart(plugin, player, preset, vol, true);
    }

    public static void depart(Plugin plugin, Player player, String preset, float vol, boolean stack) {
        N[] notes = switch (preset == null ? "jrgo" : preset.toLowerCase()) {
            case "doors" -> score(stack,
                    A4, 4, A4, 4, A4, 4, A4, 4, A4, 4, A4, 4, C4, 12);
            case "whistle" -> score(stack,
                    A3, 8, A3, 8, A3, 10, 0, 4, B3, 8, B3, 12);
            case "shinkansen" -> score(stack,
                    D4, 5, Fs4, 5, As4, 5, D5, 5, E5, 8,
                    D5, 5, As4, 5, D5, 6, E5, 6, Fs5, 14);
            case "crgo" -> score(stack,
                    Fs4, 5, Fs4, 5, Fs4, 8, C4, 8,
                    Fs4, 5, Fs4, 5, Fs4, 8, C4, 12);
            case "crhgo" -> score(stack,
                    D5, 5, As4, 5, D4, 6, Fs4, 5, As4, 5,
                    D5, 6, C5, 5, As4, 5, D5, 14);
            default -> score(stack,
                    C5, 5, A4, 5, Fs4, 5, D4, 6,
                    C5, 5, A4, 5, Fs4, 5, C4, 12);
        };
        play(plugin, player, vol, notes);
    }
}
