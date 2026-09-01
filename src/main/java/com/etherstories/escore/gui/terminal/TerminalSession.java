package com.etherstories.escore.gui.terminal;

import java.util.HashMap;
import java.util.Map;

/** 一名玩家的终端会话。扩展字段给以后分页、筛选、选中项用。 */
public final class TerminalSession {
    private String pageId;
    private final long openedAt = System.currentTimeMillis();
    private final Map<String, String> extra = new HashMap<>();

    public String pageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }
    public long openedAt() { return openedAt; }

    public String get(String key) { return extra.get(key); }
    public void put(String key, String value) {
        if (value == null) extra.remove(key);
        else extra.put(key, value);
    }
    public Map<String, String> extra() { return extra; }
}
