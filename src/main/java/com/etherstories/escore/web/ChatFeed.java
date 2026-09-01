package com.etherstories.escore.web;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatFeed {

    /** kind: chat / web / link / sys / join / quit / death / admin / cmd */
    public record Line(long ts, String name, String text, String kind, UUID only, String display) {
        public boolean web() {
            return "web".equals(kind);
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private final int max;

    public ChatFeed(int max) {
        this.max = Math.max(20, max);
    }

    public synchronized void add(String name, String text) {
        add(name, text, "chat", null, text);
    }

    public synchronized void add(String name, String text, boolean web) {
        add(name, text, web ? "web" : "chat", null, text);
    }

    public synchronized void add(String name, String text, String kind) {
        add(name, text, kind, null, text);
    }

    public synchronized void add(String name, String text, String kind, UUID only) {
        add(name, text, kind, only, text);
    }

    public synchronized void add(String name, String text, String kind, UUID only, String display) {
        if (name == null || text == null) return;
        String plain = strip(text);
        if (plain.isBlank()) return;
        String k = kind == null || kind.isBlank() ? "chat" : kind;
        String shown = display == null || display.isBlank() ? text : display;
        long now = System.currentTimeMillis();
        if (!lines.isEmpty()) {
            Line last = lines.get(lines.size() - 1);
            if (last.text().equals(plain) && now - last.ts() < 400) return;
        }
        lines.add(new Line(now, name, plain, k, only, shown));
        while (lines.size() > max) lines.removeFirst();
    }

    public synchronized List<Line> recent() {
        return recent(null, true);
    }

    public synchronized List<Line> recent(UUID viewer, boolean admin) {
        List<Line> out = new ArrayList<>();
        for (Line line : lines) {
            if (line.only() != null && (viewer == null || !line.only().equals(viewer))) continue;
            if ("admin".equals(line.kind()) && !admin) continue;
            out.add(line);
        }
        return out;
    }

    public static String strip(String s) {
        if (s == null) return "";
        return ChatColor.stripColor(s).replace('\u00a7', ' ').trim();
    }
}
