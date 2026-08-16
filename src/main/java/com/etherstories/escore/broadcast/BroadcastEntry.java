package com.etherstories.escore.broadcast;

import java.time.DayOfWeek;
import java.util.*;

/**
 * 定时广播条目。days 为空 = 每天。
 * 序列化前缀: [ALL] / [WD] / [WE] / [MON,TUE,...]
 */
public final class BroadcastEntry {

    public enum Preset { ALL, WEEKDAY, WEEKEND, CUSTOM }

    public final String text;
    public final Set<DayOfWeek> days; // empty = all

    public BroadcastEntry(String text, Set<DayOfWeek> days) {
        this.text = text == null ? "" : text;
        this.days = days == null || days.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class)
                : EnumSet.copyOf(days);
    }

    public boolean activeToday() {
        if (days.isEmpty()) return true;
        return days.contains(DayOfWeek.from(java.time.LocalDate.now()));
    }

    public Preset preset() {
        if (days.isEmpty()) return Preset.ALL;
        EnumSet<DayOfWeek> wd = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        EnumSet<DayOfWeek> we = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        if (days.equals(wd)) return Preset.WEEKDAY;
        if (days.equals(we)) return Preset.WEEKEND;
        return Preset.CUSTOM;
    }

    public String daysLabel() {
        return switch (preset()) {
            case ALL -> "每天";
            case WEEKDAY -> "工作日";
            case WEEKEND -> "周末";
            case CUSTOM -> {
                StringBuilder sb = new StringBuilder();
                for (DayOfWeek d : days) {
                    if (!sb.isEmpty()) sb.append(',');
                    sb.append(shortDay(d));
                }
                yield sb.toString();
            }
        };
    }

    public BroadcastEntry cyclePreset() {
        return switch (preset()) {
            case ALL -> weekday(text);
            case WEEKDAY -> weekend(text);
            default -> allDays(text);
        };
    }

    public String serialize() {
        if (days.isEmpty()) return text;
        Preset p = preset();
        if (p == Preset.WEEKDAY) return "[WD]" + text;
        if (p == Preset.WEEKEND) return "[WE]" + text;
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (DayOfWeek d : days) {
            if (!first) sb.append(',');
            sb.append(shortDay(d));
            first = false;
        }
        sb.append(']').append(text);
        return sb.toString();
    }

    public static BroadcastEntry parse(String raw) {
        if (raw == null) return new BroadcastEntry("", null);
        String s = raw.trim();
        if (s.startsWith("[ALL]")) return allDays(s.substring(5));
        if (s.startsWith("[WD]")) return weekday(s.substring(4));
        if (s.startsWith("[WE]")) return weekend(s.substring(4));
        if (s.startsWith("[")) {
            int end = s.indexOf(']');
            if (end > 1) {
                String body = s.substring(1, end);
                String text = s.substring(end + 1);
                Set<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
                for (String part : body.split(",")) {
                    DayOfWeek d = parseDay(part.trim());
                    if (d != null) set.add(d);
                }
                return new BroadcastEntry(text, set);
            }
        }
        return allDays(s);
    }

    public static BroadcastEntry allDays(String text) {
        return new BroadcastEntry(text, EnumSet.noneOf(DayOfWeek.class));
    }

    public static BroadcastEntry weekday(String text) {
        return new BroadcastEntry(text, EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
    }

    public static BroadcastEntry weekend(String text) {
        return new BroadcastEntry(text, EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
    }

    private static String shortDay(DayOfWeek d) {
        return switch (d) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }

    private static DayOfWeek parseDay(String s) {
        return switch (s.toUpperCase(Locale.ROOT)) {
            case "MON", "1", "一" -> DayOfWeek.MONDAY;
            case "TUE", "2", "二" -> DayOfWeek.TUESDAY;
            case "WED", "3", "三" -> DayOfWeek.WEDNESDAY;
            case "THU", "4", "四" -> DayOfWeek.THURSDAY;
            case "FRI", "5", "五" -> DayOfWeek.FRIDAY;
            case "SAT", "6", "六" -> DayOfWeek.SATURDAY;
            case "SUN", "7", "日" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }
}
