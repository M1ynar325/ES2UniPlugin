package com.etherstories.escore.utils;

import com.etherstories.escore.ES2UniPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TPSUtil {

    // ── Reflection cache (looked up once, reused forever) ─────────────────────
    private static Object  nmsServer;
    private static Field   recentTpsField;
    private static Method  avgTickTimeMethod;  // getAverageTickTime()
    private static Field   tickTimesField;     // tickTimes[] fallback

    private static Object getNmsServer() {
        if (nmsServer == null) {
            try {
                nmsServer = Bukkit.getServer().getClass()
                        .getMethod("getServer").invoke(Bukkit.getServer());
            } catch (Exception ignored) {}
        }
        return nmsServer;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static double getTPS() {
        return getAllTPS()[0];
    }

    public static double[] getAllTPS() {
        Object server = getNmsServer();
        if (server == null) return new double[]{20.0, 20.0, 20.0};
        try {
            if (recentTpsField == null)
                recentTpsField = server.getClass().getField("recentTps");
            return (double[]) recentTpsField.get(server);
        } catch (Exception e) {
            return new double[]{20.0, 20.0, 20.0};
        }
    }

    public static double getMSPT() {
        Object server = getNmsServer();
        if (server == null) return estimateMSPT();
        try {
            if (avgTickTimeMethod == null)
                avgTickTimeMethod = server.getClass().getMethod("getAverageTickTime");
            return (double) avgTickTimeMethod.invoke(server);
        } catch (NoSuchMethodException ignored) {
            // fallback: tickTimes[]
        } catch (Exception e) {
            return estimateMSPT();
        }
        try {
            if (tickTimesField == null) {
                tickTimesField = server.getClass().getDeclaredField("tickTimes");
                tickTimesField.setAccessible(true);
            }
            long[] tickTimes = (long[]) tickTimesField.get(server);
            long sum = 0;
            for (long t : tickTimes) sum += t;
            return (sum / (double) tickTimes.length) / 1_000_000.0;
        } catch (Exception e) {
            return estimateMSPT();
        }
    }

    // ── Color & formatting ────────────────────────────────────────────────────

    private static double estimateMSPT() {
        double tps = getTPS();
        return tps > 0 ? Math.min(50.0, 1000.0 / tps) : 50.0;
    }

    public static String getTpsColor(double tps) {
        if (tps >= ES2UniPlugin.getInstance().getConfigManager().getTpsGreenThreshold())  return "\u00a7a";
        if (tps >= ES2UniPlugin.getInstance().getConfigManager().getTpsYellowThreshold()) return "\u00a7e";
        return "\u00a7c";
    }

    public static String getMsptColor(double mspt) {
        if (mspt <= ES2UniPlugin.getInstance().getConfigManager().getMsptGreenThreshold())  return "\u00a7a";
        if (mspt <= ES2UniPlugin.getInstance().getConfigManager().getMsptYellowThreshold()) return "\u00a7e";
        return "\u00a7c";
    }

    public static String formatTPS(double tps) {
        return getTpsColor(tps) + String.format("%.1f", Math.min(tps, 20.0));
    }

    public static String formatMSPT(double mspt) {
        return getMsptColor(mspt) + String.format("%.1f", mspt) + "ms";
    }
}
