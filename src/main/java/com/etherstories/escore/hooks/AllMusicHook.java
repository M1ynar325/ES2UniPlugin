package com.etherstories.escore.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * AllMusic 联动（反射，兼容 Paper 插件 / NeoForge+Arclight 同 JVM 类）。
 * 不编译期依赖 AllMusic。
 */
public class AllMusicHook {

    public record SongEntry(int selectIndex, String id, String name, String author, String album) {}

    public record NowPlaying(String id, String name, String author, String album, String caller, int queueSize) {}

    public record QueueSong(int index, String id, String name, String author, String album, String caller) {}

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

    /** 用歌曲 ID 点歌（默认 API） */
    public void addById(Plugin plugin, Player player, String id) {
        if (id == null || id.isBlank()) return;
        Bukkit.getScheduler().runTask(plugin, () ->
                player.performCommand("music " + id.trim()));
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
        if (!available || player == null) return false;
        try {
            Class<?> ban = Class.forName("com.coloryr.allmusic.server.core.saves.BanSave");
            return (Boolean) ban.getMethod("checkMutePlayer", String.class)
                    .invoke(null, player.getName());
        } catch (Exception e) {
            return false;
        }
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
            if (song == null) return new NowPlaying("", "（无）", "", "", "", size);
            String id     = str(song, "getID");
            if (id.isEmpty()) id = str(song, "getId");
            String name   = str(song, "getName");
            String author = str(song, "getAuthor");
            String al     = str(song, "getAl");
            String call   = str(song, "getCall");
            return new NowPlaying(id, name, author, al, call, size);
        } catch (Exception e) {
            return null;
        }
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
