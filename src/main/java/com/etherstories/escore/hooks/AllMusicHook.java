package com.etherstories.escore.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * AllMusic 联动（反射，兼容 Paper 插件 / NeoForge+Arclight 同 JVM 类）。
 * 不编译期依赖 AllMusic。
 */
public class AllMusicHook {

    public record SongEntry(int selectIndex, String id, String name, String author, String album) {}

    public record NowPlaying(String id, String name, String author, String album, String pic,
                             String caller, int queueSize) {}

    public record LyricNow(String lyric, String tlyric, String prev, String next, long nowMs, long allMs) {}

    public record QueueSong(int index, String id, String name, String author, String album, String caller) {}

    public record SearchPack(String q, int page, int pages, boolean prev, boolean next, List<SongEntry> songs) {}

    private boolean available;
    private Class<?> musicSearchClz;
    private Class<?> playMusicClz;
    private Method getSearch;
    private Method getIndex;
    private Method getRes;
    private Method getPage;
    private Method haveNext;
    private Method haveLast;
    private Method nextPage;
    private Method lastPage;
    private Method getList;
    private Method getListSize;

    public void hook(Logger log) {
        available = false;
        try {
            musicSearchClz = Class.forName("com.coloryr.allmusic.server.core.music.MusicSearch");
            playMusicClz   = Class.forName("com.coloryr.allmusic.server.core.music.PlayMusic");
            getSearch = musicSearchClz.getMethod("getSearch", String.class);
            Class<?> pageClz = Class.forName("com.coloryr.allmusic.server.core.objs.music.SearchPageObj");
            getIndex  = pageClz.getMethod("getIndex");
            getRes    = pageClz.getMethod("getRes", int.class);
            getPage   = pageClz.getMethod("getPage");
            haveNext  = pageClz.getMethod("haveNextPage");
            haveLast  = pageClz.getMethod("haveLastPage");
            nextPage  = pageClz.getMethod("nextPage");
            lastPage  = pageClz.getMethod("lastPage");
            getList     = playMusicClz.getMethod("getList");
            getListSize = playMusicClz.getMethod("getListSize");
            available = true;
            log.info("AllMusic 已挂钩（反射）");
        } catch (ClassNotFoundException e) {
            Plugin p = Bukkit.getPluginManager().getPlugin("allmusic");
            if (p != null && p.isEnabled()) {
                log.warning("检测到 allmusic 插件，但无法反射核心类: " + e.getMessage());
            }
        } catch (Exception e) {
            log.warning("AllMusic 挂钩失败: " + e.getMessage());
        }
    }

    /** Arclight 上 mod 可能晚于插件加载，打开菜单时再试一次 */
    public boolean ensureHooked(Logger log) {
        if (available) return true;
        hook(log);
        return available;
    }

    public boolean isAvailable() {
        return available;
    }

    public void search(Plugin plugin, Player player, String query) {
        if (query == null || query.isBlank()) return;
        Bukkit.getScheduler().runTask(plugin, () ->
                player.performCommand("music search " + query.trim()));
    }

    /**
     * 网页用：直接打 AllMusic API，结果写入 MusicSearch（按玩家名），一页 10 条。
     * AllMusic 未挂钩返回 null。
     */
    @SuppressWarnings("unchecked")
    public SearchPack searchByName(String playerName, String query) {
        if (query == null || query.isBlank()) return new SearchPack(query, 1, 0, false, false, List.of());
        if (!available) return null;
        try {
            Class<?> all = Class.forName("com.coloryr.allmusic.server.core.AllMusic");
            Object config = all.getMethod("getConfig").invoke(null);
            String apiName = "";
            if (config != null) {
                Object raw = config.getClass().getField("defaultApi").get(config);
                apiName = raw == null ? "" : String.valueOf(raw);
            }
            Map<String, Object> apis = (Map<String, Object>) all.getField("MUSIC_APIS").get(null);
            Object api = apis == null ? null : apis.get(apiName);
            if (api == null && apis != null && !apis.isEmpty())
                api = apis.values().iterator().next();
            if (api == null) return null;
            Method search = api.getClass().getMethod("search", String[].class, boolean.class);
            Object page = search.invoke(api, new String[]{query.trim()}, Boolean.TRUE);
            if (page == null) return new SearchPack(query.trim(), 1, 0, false, false, List.of());
            saveSearch(playerName, page);
            return pack(query.trim(), page);
        } catch (Exception e) {
            return null;
        }
    }

    public SearchPack flipSearch(String playerName, boolean next) {
        if (!available) return null;
        try {
            Object page = getSearch.invoke(null, playerName);
            if (page == null) return new SearchPack("", 1, 0, false, false, List.of());
            boolean ok = (Boolean) (next ? nextPage : lastPage).invoke(page);
            if (!ok) return pack("", page);
            return pack("", page);
        } catch (Exception e) {
            return null;
        }
    }

    /** 用歌曲 ID 点歌（默认 API） */
    public void addById(Plugin plugin, Player player, String id) {
        if (id == null || id.isBlank()) return;
        Bukkit.getScheduler().runTask(plugin, () ->
                player.performCommand("music " + id.trim()));
    }

    /** 网页离线点歌：按会话玩家名记到 AllMusic，不走 console 的无名点歌 */
    public void addByIdAs(Plugin plugin, String playerName, Player online, String id) {
        if (id == null || id.isBlank()) return;
        if (online != null) {
            addById(plugin, online, id);
            return;
        }
        final String name = playerName == null ? "web" : playerName;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Class<?> all = Class.forName("com.coloryr.allmusic.server.core.AllMusic");
                Object config = all.getMethod("getConfig").invoke(null);
                String api = "netapi";
                if (config != null) {
                    Object raw = config.getClass().getField("defaultApi").get(config);
                    if (raw != null && !String.valueOf(raw).isBlank()) api = String.valueOf(raw);
                }
                Class<?> cex = Class.forName("com.coloryr.allmusic.server.core.command.CommandEX");
                cex.getMethod("addMusic", Object.class, String.class, String.class, String.class)
                        .invoke(null, Bukkit.getConsoleSender(), name, api, id.trim());
            } catch (Exception e) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "music " + id.trim());
            }
        });
    }

    public void addById(Plugin plugin, Player player, String api, String id) {
        if (id == null || id.isBlank()) return;
        if (api == null || api.isBlank()) {
            addById(plugin, player, id);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () ->
                player.performCommand("music " + api.trim() + " " + id.trim()));
    }

    public void select(Plugin plugin, Player player, int index1Based) {
        Bukkit.getScheduler().runTask(plugin, () ->
                player.performCommand("music select " + index1Based));
    }

    public void vote(Plugin plugin, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("music vote"));
    }

    public void stop(Plugin plugin, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("music stop"));
    }

    public void mute(Plugin plugin, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("music mute"));
    }

    public void join(Plugin plugin, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("music join"));
    }

    public void cancel(Plugin plugin, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("music cancel"));
    }

    /** 是否已静音（不听歌）；走 AllMusic BanSave，重进仍有效 */
    public boolean isMuted(Player player) {
        return player != null && isMuted(player.getName());
    }

    public boolean isMuted(String name) {
        if (!available || name == null || name.isBlank()) return false;
        try {
            Class<?> ban = Class.forName("com.coloryr.allmusic.server.core.saves.BanSave");
            return (Boolean) ban.getMethod("checkMutePlayer", String.class).invoke(null, name);
        } catch (Exception e) {
            return false;
        }
    }

    public String getPlayUrl() {
        if (!available) return "";
        try {
            Object url = playMusicClz.getField("url").get(null);
            return url == null ? "" : String.valueOf(url);
        } catch (Exception e) {
            return "";
        }
    }

    public void setMuted(Plugin plugin, String name, Player online, boolean muted) {
        if (online != null) {
            setListening(plugin, online, !muted);
            return;
        }
        if (!available || name == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Class<?> ban = Class.forName("com.coloryr.allmusic.server.core.saves.BanSave");
                boolean now = (Boolean) ban.getMethod("checkMutePlayer", String.class).invoke(null, name);
                if (now == muted) return;
                ban.getMethod(muted ? "addMutePlayer" : "removeMutePlayer", String.class).invoke(null, name);
            } catch (Exception ignored) {}
        });
    }

    /**
     * 设置是否听歌。listening=true 取消静音；false 静音并 stop 自己。
     * @return 设置后是否处于静音
     */
    public boolean setListening(Plugin plugin, Player player, boolean listening) {
        boolean wantMute = !listening;
        boolean nowMuted = isMuted(player);
        if (wantMute == nowMuted) return nowMuted;
        // 切换：调用 /music mute
        mute(plugin, player);
        if (wantMute) {
            stop(plugin, player);
        }
        // BanSave 可能异步，稍后再读不可靠；返回目标状态
        return wantMute;
    }

    public void list(Plugin plugin, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("music list"));
    }

    public List<SongEntry> getSearchPage(Player player) {
        if (!available) return null;
        try {
            Object page = getSearch.invoke(null, player.getName());
            if (page == null) return Collections.emptyList();

            int count = (Integer) getIndex.invoke(page);
            int pageNum = (Integer) getPage.invoke(page);
            List<SongEntry> out = new ArrayList<>(count);
            for (int a = 0; a < count; a++) {
                Object item = getRes.invoke(page, a + pageNum * 10);
                String id     = (String) item.getClass().getField("id").get(item);
                String name   = (String) item.getClass().getField("name").get(item);
                String author = (String) item.getClass().getField("author").get(item);
                String al     = (String) item.getClass().getField("al").get(item);
                out.add(new SongEntry(a + 1, id, name, author, al));
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasSearch(Player player) {
        if (!available) return false;
        try {
            return getSearch.invoke(null, player.getName()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean nextSearchPage(Player player) {
        return flipPage(player, true);
    }

    public boolean prevSearchPage(Player player) {
        return flipPage(player, false);
    }

    public boolean canNext(Player player) {
        return pageFlag(player, haveNext);
    }

    public boolean canPrev(Player player) {
        return pageFlag(player, haveLast);
    }

    public int currentPage(Player player) {
        if (!available) return 0;
        try {
            Object page = getSearch.invoke(null, player.getName());
            if (page == null) return 0;
            return (Integer) getPage.invoke(page);
        } catch (Exception e) {
            return 0;
        }
    }

    public NowPlaying getNowPlaying() {
        if (!available) return null;
        try {
            Object song = playMusicClz.getField("nowPlayMusic").get(null);
            int size = (Integer) getListSize.invoke(null);
            if (song == null) return new NowPlaying("", "（无）", "", "", "", "", size);
            String id     = str(song, "getID");
            if (id.isEmpty()) id = str(song, "getId");
            String name   = str(song, "getName");
            String author = str(song, "getAuthor");
            String al     = str(song, "getAl");
            String pic    = str(song, "getPicUrl");
            String call   = str(song, "getCall");
            return new NowPlaying(id, name, author, al, pic, call, size);
        } catch (Exception e) {
            return null;
        }
    }

    /** 当前歌词行（AllMusic LyricSave，跟 HUD 同一条） */
    @SuppressWarnings("unchecked")
    public LyricNow getLyricNow() {
        if (!available) return new LyricNow("", "", "", "", 0, 0);
        try {
            Object save = playMusicClz.getField("lyric").get(null);
            long nowMs = num(playMusicClz.getField("musicNowTime").get(null));
            long allMs = num(playMusicClz.getField("musicAllTime").get(null));
            String lyric = "", tlyric = "", prev = "", next = "";
            if (save != null) {
                Object cur = save.getClass().getMethod("getNow").invoke(save);
                if (cur != null) {
                    lyric = field(cur, "lyric");
                    tlyric = field(cur, "tlyric");
                }
                Field mapF = save.getClass().getDeclaredField("lyric");
                mapF.setAccessible(true);
                Object raw = mapF.get(save);
                if (raw instanceof Map<?, ?> map && !map.isEmpty()) {
                    List<Long> keys = new ArrayList<>();
                    for (Object k : map.keySet()) {
                        if (k instanceof Number n) keys.add(n.longValue());
                    }
                    Collections.sort(keys);
                    int idx = -1;
                    for (int i = 0; i < keys.size(); i++) {
                        if (keys.get(i) <= nowMs) idx = i;
                        else break;
                    }
                    if (idx < 0) idx = 0;
                    Object item = map.get(keys.get(idx));
                    if (lyric.isBlank() && item != null) {
                        lyric = field(item, "lyric");
                        tlyric = field(item, "tlyric");
                    }
                    if (idx > 0) prev = field(map.get(keys.get(idx - 1)), "lyric");
                    if (idx + 1 < keys.size()) next = field(map.get(keys.get(idx + 1)), "lyric");
                }
            }
            return new LyricNow(nz(lyric), nz(tlyric), nz(prev), nz(next), nowMs, allMs);
        } catch (Exception e) {
            return new LyricNow("", "", "", "", 0, 0);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static long num(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }

    /** 播放队列（不含正在播的那首时由 AllMusic getList 决定） */
    @SuppressWarnings("unchecked")
    public List<QueueSong> getQueue() {
        if (!available) return Collections.emptyList();
        try {
            Object raw = getList.invoke(null);
            if (!(raw instanceof List<?> list)) return Collections.emptyList();
            List<QueueSong> out = new ArrayList<>(list.size());
            int i = 1;
            for (Object song : list) {
                if (song == null) continue;
                out.add(new QueueSong(
                        i++,
                        str(song, "getID"),
                        str(song, "getName"),
                        str(song, "getAuthor"),
                        str(song, "getAl"),
                        str(song, "getCall")));
            }
            return out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void saveSearch(String playerName, Object page) {
        try {
            Class<?> clz = Class.forName("com.coloryr.allmusic.server.core.music.MusicSearch");
            clz.getMethod("addSearch", String.class, page.getClass()).invoke(null, playerName, page);
        } catch (Exception ignored) {}
    }

    private SearchPack pack(String q, Object page) throws Exception {
        int page0 = (Integer) getPage.invoke(page);
        int pages = 1;
        try {
            Field f = page.getClass().getDeclaredField("maxpage");
            f.setAccessible(true);
            Object raw = f.get(page);
            if (raw instanceof Integer n && n > 0) pages = n;
        } catch (Exception ignored) {}
        boolean prev = (Boolean) haveLast.invoke(page);
        boolean next = (Boolean) haveNext.invoke(page);
        return new SearchPack(q == null ? "" : q, page0 + 1, pages, prev, next, extractPage(page));
    }

    private List<SongEntry> extractPage(Object page) throws Exception {
        int count = (Integer) getIndex.invoke(page);
        int pageNum = (Integer) getPage.invoke(page);
        List<SongEntry> out = new ArrayList<>(count);
        for (int a = 0; a < count; a++) {
            Object item = getRes.invoke(page, a + pageNum * 10);
            out.add(new SongEntry(a + 1,
                    field(item, "id"), field(item, "name"),
                    field(item, "author"), field(item, "al")));
        }
        return out;
    }

    private static String field(Object item, String name) {
        if (item == null) return "";
        try {
            Object v = item.getClass().getField(name).get(item);
            return v == null ? "" : String.valueOf(v);
        } catch (Exception e) {
            return "";
        }
    }

    private static String str(Object song, String method) {
        try {
            Object v = song.getClass().getMethod(method).invoke(song);
            return v == null ? "" : String.valueOf(v);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean flipPage(Player player, boolean next) {
        if (!available) return false;
        try {
            Object page = getSearch.invoke(null, player.getName());
            if (page == null) return false;
            return (Boolean) (next ? nextPage : lastPage).invoke(page);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean pageFlag(Player player, Method m) {
        if (!available) return false;
        try {
            Object page = getSearch.invoke(null, player.getName());
            if (page == null) return false;
            return (Boolean) m.invoke(page);
        } catch (Exception e) {
            return false;
        }
    }
}
