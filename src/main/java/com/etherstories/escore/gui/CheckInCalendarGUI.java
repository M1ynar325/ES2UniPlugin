package com.etherstories.escore.gui;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

/**
 * 签到月历：展示本月签到，今日可点签。
 */
public class CheckInCalendarGUI {

    public static final String TITLE = ColorUtil.colorize("&a&l签到日历");

    public static final int SLOT_PREV = 45;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_NEXT = 47;
    public static final int SLOT_DO   = 51;
    public static final int SLOT_BACK = 52;
    public static final int SLOT_CLOSE = 53;

    private final ES2UniPlugin plugin;
    private final Map<UUID, YearMonth> months = new HashMap<>();

    public CheckInCalendarGUI(ES2UniPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, months.getOrDefault(player.getUniqueId(), YearMonth.now()));
    }

    public void open(Player player, YearMonth month) {
        months.put(player.getUniqueId(), month);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, ECOSTerminalGUI.bg(Material.GRAY_STAINED_GLASS_PANE));

        // 表头 一…日 → slots 0-6
        DayOfWeek[] order = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };
        for (int i = 0; i < 7; i++) {
            inv.setItem(i, ECOSTerminalGUI.item(Material.PAPER,
                    "&f" + order[i].getDisplayName(TextStyle.SHORT, Locale.CHINA), null));
        }

        Set<Integer> checked = plugin.getCheckInManager().getCheckedDaysInMonth(player.getUniqueId(), month);
        LocalDate today = LocalDate.now();
        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek().getValue() - 1; // Mon=0
        int daysInMonth = month.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            int slot = 9 + offset + (day - 1);
            if (slot >= 45) break;
            LocalDate date = month.atDay(day);
            boolean isChecked = checked.contains(day);
            boolean isToday = date.equals(today);
            boolean future = date.isAfter(today);

            Material mat;
            String name;
            List<String> lore = new ArrayList<>();
            if (future) {
                mat = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
                name = "&8" + day;
                lore.add("&8尚未到来");
            } else if (isChecked) {
                mat = Material.LIME_CONCRETE;
                name = "&a" + day + (isToday ? " &8(今天)" : "");
                lore.add("&a已签到");
            } else if (isToday) {
                mat = Material.YELLOW_CONCRETE;
                name = "&e" + day + " &8(今天)";
                lore.add("&e今日未签到");
                lore.add("&a▸ 点击下方「立即签到」");
            } else {
                mat = Material.RED_CONCRETE;
                name = "&c" + day;
                lore.add("&7未签到");
                lore.add("&e▸ 点击消耗补签券补签");
            }
            inv.setItem(slot, ECOSTerminalGUI.item(mat, name, lore));
        }

        int streak = plugin.getCheckInManager().getStreak(player.getUniqueId());
        boolean done = plugin.getCheckInManager().hasCheckedInToday(player.getUniqueId());
        int tickets = plugin.getCheckInManager().getMakeupTickets(player.getUniqueId());
        double nextReward = plugin.getConfigManager().calcCheckInReward(done ? streak : streak + 1);

        inv.setItem(SLOT_PREV, ECOSTerminalGUI.item(Material.ARROW, "&7上个月", null));
        inv.setItem(SLOT_NEXT, ECOSTerminalGUI.item(Material.ARROW, "&7下个月", null));
        inv.setItem(SLOT_INFO, ECOSTerminalGUI.item(Material.CLOCK,
                "&f" + month.getYear() + "年" + month.getMonthValue() + "月",
                List.of(
                        "&7连续签到: &f" + streak + " 天",
                        "&7本月已签: &f" + checked.size() + " 天",
                        "&7补签券: &e" + tickets + " &8(活动发放)",
                        done ? "&a今日已签到" : "&e今日未签到",
                        "&7下次奖励约: &e" + (plugin.getVaultHook().isEnabled()
                                ? plugin.getVaultHook().format(nextReward) : String.valueOf(nextReward))
                )));
        inv.setItem(SLOT_DO, ECOSTerminalGUI.item(
                done ? Material.GRAY_DYE : Material.EMERALD,
                done ? "&8今日已签到" : "&a立即签到",
                List.of(done ? "&7明天再来" : "&a▸ 点击签到领奖")));
        inv.setItem(SLOT_BACK, ECOSTerminalGUI.item(Material.ARROW, "&7返回终端", null));
        inv.setItem(SLOT_CLOSE, ECOSTerminalGUI.item(Material.BARRIER, "&c关闭", null));
        player.openInventory(inv);
    }

    public YearMonth getMonth(Player player) {
        return months.getOrDefault(player.getUniqueId(), YearMonth.now());
    }

    /** 日历格 slot → 日（1–31），无效返回 -1 */
    public int getDayAt(Player player, int slot) {
        YearMonth month = getMonth(player);
        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek().getValue() - 1;
        int day = slot - 9 - offset + 1;
        if (day < 1 || day > month.lengthOfMonth()) return -1;
        if (slot < 9 || slot >= 45) return -1;
        return day;
    }

    public void cleanup(Player player) {
        months.remove(player.getUniqueId());
    }
}
