package com.etherstories.escore.managers;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.hooks.AllMusicHook;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话内播放记录（纯内存）。重启清空，不写盘，避免日志/存档膨胀。
 */
public class MusicHistoryManager {

    public record Entry(String id, String name, String author, String caller, String time) {}

    private static final int MAX = 40;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ES2UniPlugin plugin;
    private final ArrayDeque<Entry> history = new ArrayDeque<>();
    private String lastKey = "";
    private BukkitRunnable pollTask;

    public MusicHistoryManager(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        pollTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        pollTask.runTaskTimer(plugin, 40L, 40L); // 2s
    }

    public void stop() {
        if (pollTask != null && !pollTask.isCancelled()) {
            pollTask.cancel();
        }
        pollTask = null;
    }

    public void clear() {
        history.clear();
        lastKey = "";
    }

    private void tick() {
        AllMusicHook hook = plugin.getAllMusicHook();
        if (hook == null || !hook.ensureHooked(plugin.getLogger())) return;
        AllMusicHook.NowPlaying now = hook.getNowPlaying();
        if (now == null) return;
        String id = now.id() == null ? "" : now.id();
        String name = now.name() == null ? "" : now.name();
        if (name.isEmpty() || name.equals("（无）")) return;
        String key = id + "|" + name + "|" + now.author() + "|" + now.caller();
        if (key.equals(lastKey)) return;
        lastKey = key;
        push(new Entry(id, name, now.author(), now.caller(), LocalTime.now().format(FMT)));
    }

    /** 点歌时也可主动记一条（与轮询去重靠 lastKey） */
    public void recordManual(String id, String name, String author, String caller) {
        String key = (name == null ? "" : name) + "|" + (author == null ? "" : author) + "|" + (caller == null ? "" : caller);
        lastKey = key;
        push(new Entry(id == null ? "" : id, name == null ? "" : name,
                author == null ? "" : author, caller == null ? "" : caller,
                LocalTime.now().format(FMT)));
    }

    private void push(Entry e) {
        history.addFirst(e);
        while (history.size() > MAX) history.removeLast();
    }

    public List<Entry> recent() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public int size() {
        return history.size();
    }
}
