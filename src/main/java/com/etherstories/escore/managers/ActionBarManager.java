package com.etherstories.escore.managers;

import com.etherstories.escore.utils.ColorUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ActionBar 统一调度：
 * - sticky：地区等常驻（高频刷新防淡出）
 * - temp：技能/抽奖等临时（未到期时压制 sticky，避免被地区刷屏盖掉）
 */
public class ActionBarManager {

    private final Map<UUID, String> sticky = new ConcurrentHashMap<>();
    private final Map<UUID, Temp> temp = new ConcurrentHashMap<>();

    private record Temp(String text, long untilMs) {}

    public void setSticky(Player player, String coloredText) {
        if (coloredText == null || coloredText.isEmpty()) {
            sticky.remove(player.getUniqueId());
            return;
        }
        sticky.put(player.getUniqueId(), coloredText);
    }

    public void clearSticky(UUID uuid) {
        sticky.remove(uuid);
        temp.remove(uuid);
    }

    /** 临时 ActionBar；durationTicks 内不会被 sticky 覆盖。重复调用会续期。 */
    public void sendTemp(Player player, String msg, int durationTicks) {
        if (player == null || msg == null) return;
        String text = ColorUtil.colorize(msg);
        long until = System.currentTimeMillis() + Math.max(5, durationTicks) * 50L;
        temp.put(player.getUniqueId(), new Temp(text, until));
        push(player, text);
    }

    /** 默认显示约 2.5 秒 */
    public void sendTemp(Player player, String msg) {
        sendTemp(player, msg, 50);
    }

    /** 由定时任务调用：优先 temp，否则 sticky；持续推送避免客户端淡出。 */
    public void pulse() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            Temp t = temp.get(id);
            if (t != null) {
                if (now < t.untilMs) {
                    push(player, t.text);
                    continue;
                }
                temp.remove(id);
            }
            String s = sticky.get(id);
            if (s != null) push(player, s);
        }
    }

    private static void push(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
    }
}
