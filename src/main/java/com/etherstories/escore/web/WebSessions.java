package com.etherstories.escore.web;

import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebSessions {

    public record Session(String token, UUID uuid, String name, long created, boolean admin) {}

    private final Map<String, Pair> pairs = new ConcurrentHashMap<>();
    private final Map<String, Session> byToken = new ConcurrentHashMap<>();
    private final SecureRandom rng = new SecureRandom();
    private final int pairMs;
    private final long sessionMs;

    public WebSessions(int pairSeconds, int sessionDays) {
        this.pairMs = Math.max(30, pairSeconds) * 1000;
        this.sessionMs = Math.max(1, sessionDays) * 86400_000L;
    }

    public synchronized String issue(Player player) {
        sweep();
        String code;
        do {
            code = String.format("%06d", rng.nextInt(1_000_000));
        } while (pairs.containsKey(code));
        boolean admin = player.isOp() || player.hasPermission("es2uni.admin");
        pairs.put(code, new Pair(player.getUniqueId(), player.getName(),
                System.currentTimeMillis() + pairMs, admin));
        return code;
    }

    public Session consume(String code) {
        sweep();
        String digits = code == null ? "" : code.replaceAll("\\D", "");
        Pair p = pairs.remove(digits);
        if (p == null || p.until < System.currentTimeMillis()) return null;
        String token = newToken();
        Session s = new Session(token, p.uuid, p.name, System.currentTimeMillis(), p.admin);
        byToken.put(token, s);
        return s;
    }

    public Session get(String token) {
        if (token == null || token.isBlank()) return null;
        sweep();
        Session s = byToken.get(token);
        if (s == null) return null;
        if (System.currentTimeMillis() - s.created > sessionMs) {
            byToken.remove(token);
            return null;
        }
        return s;
    }

    public void revoke(UUID uuid) {
        byToken.entrySet().removeIf(e -> e.getValue().uuid.equals(uuid));
        pairs.entrySet().removeIf(e -> e.getValue().uuid.equals(uuid));
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        pairs.entrySet().removeIf(e -> e.getValue().until < now);
        Iterator<Map.Entry<String, Session>> it = byToken.entrySet().iterator();
        while (it.hasNext()) {
            Session s = it.next().getValue();
            if (now - s.created > sessionMs) it.remove();
        }
    }

    private String newToken() {
        byte[] b = new byte[24];
        rng.nextBytes(b);
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte v : b) sb.append(String.format("%02x", v));
        return sb.toString();
    }

    private record Pair(UUID uuid, String name, long until, boolean admin) {}
}
