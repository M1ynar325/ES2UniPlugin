package com.etherstories.escore.gui.terminal;

import com.etherstories.escore.ES2UniPlugin;
import com.etherstories.escore.auras.AuraType;
import com.etherstories.escore.gui.AnvilInputGUI;
import com.etherstories.escore.gui.EcosStyle;
import com.etherstories.escore.gui.PlayerSelectGUI;
import com.etherstories.escore.gui.PayConfirmGUI;
import com.etherstories.escore.items.TransitItems;
import com.etherstories.escore.managers.HotelManager;
import com.etherstories.escore.managers.KitManager;
import com.etherstories.escore.managers.TaxManager;
import com.etherstories.escore.managers.TransitManager;
import com.etherstories.escore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 所有原箱子页。EcosHolder.open 只开这些书，不再弹箱。 */
final class ChestlessPages {
    private ChestlessPages() {}

    static void register(TerminalHub hub, ES2UniPlugin plugin) {
        hub.registerPage(hidden("transit-tvm", "售票机", (p, s) -> tvm(plugin, p)));
        hub.registerPage(hidden("transit-adjust", "补票处", (p, s) -> adjust(plugin, p)));
        hub.registerPage(hidden("transit-ticket", "单程票", (p, s) -> ticket(plugin, p)));
        hub.registerPage(hidden("transit-office", "交通处", (p, s) -> office(plugin, p)));
        hub.registerPage(hidden("transit-map", "线路图", (p, s) -> map(plugin, p)));
        hub.registerPage(hidden("transit-history", "乘车记录", (p, s) -> history(plugin, p)));
        hub.registerPage(hidden("transit-board", "乘车榜", (p, s) -> board(plugin, p)));
        hub.registerPage(hidden("hotel", "酒店", (p, s) -> hotels(plugin, p)));
        hub.registerPage(hidden("hotel-desk", "房间", (p, s) -> hotelDesk(plugin, p)));
        hub.registerPage(hidden("player-select", "选人", (p, s) -> playerSelect(plugin, p)));
        hub.registerPage(hidden("player-action", "玩家", (p, s) -> playerAction(plugin, p)));
        hub.registerPage(hidden("pay-confirm", "确认转账", (p, s) -> payConfirm(plugin, p)));
        hub.registerPage(hidden("checkin", "签到", (p, s) -> checkin(plugin, p, s)));
        hub.registerPage(hidden("municipal", "市政", (p, s) -> municipal(plugin, p)));
        hub.registerPage(hidden("kit", "Kit", (p, s) -> kits(plugin, p)));
        hub.registerPage(hidden("aura", "特效", (p, s) -> auras(plugin, p)));
        hub.registerPage(hidden("music", "点歌", (p, s) -> music(plugin, p)));
        hub.registerPage(hidden("skillshop", "技能店", (p, s) -> skillshop(plugin, p)));
        hub.registerPage(hidden("estate", "房产", (p, s) -> estate(plugin, p)));
        hub.registerPage(hidden("estate-rooms", "房间", (p, s) -> estateRooms(plugin, p)));
        hub.registerPage(hidden("estate-sale", "待售", (p, s) -> estateSale(plugin, p)));
        hub.registerPage(hidden("estate-tools", "工具", (p, s) -> estateTools(plugin, p)));
        hub.registerPage(hidden("estate-mine", "我的房产", (p, s) -> estateMine(plugin, p, false)));
        hub.registerPage(hidden("estate-mine-admin", "房产管理", (p, s) -> estateMine(plugin, p, true)));
        hub.registerPage(hidden("event", "活动", (p, s) -> events(plugin, p)));
        hub.registerPage(hidden("leaderboard", "时长榜", (p, s) -> leaderboard(plugin, p)));
        hub.registerPage(hidden("newbie", "引导", (p, s) -> newbie(plugin, p)));
        hub.registerPage(hidden("jobs", "招工", (p, s) -> jobs(plugin, p)));
        hub.registerPage(hidden("job-history", "委托记录", (p, s) -> jobHistory(plugin, p)));
        hub.registerPage(hidden("insurance", "保险", (p, s) -> insurance(plugin, p)));
        hub.registerPage(hidden("insurance-bak", "保险备份", (p, s) -> insuranceBak(plugin, p, s)));
        hub.registerPage(hidden("recycle", "回收站", (p, s) -> recycle(plugin, p)));
        hub.registerPage(hidden("trade", "交易榜", (p, s) -> trade(plugin, p)));
        hub.registerPage(hidden("showcase", "展示", (p, s) -> showcase(plugin, p)));
        hub.registerPage(hidden("admin-event", "活动管理", (p, s) -> adminEvent(plugin, p)));
        hub.registerPage(hidden("admin-bc", "广播", (p, s) -> adminBcHeader(plugin, p), (p, s) -> adminBc(plugin, p)));
        hub.registerPage(hidden("admin-tax", "税收", (p, s) -> taxHeader(plugin, p), (p, s) -> adminTax(plugin, p)));
        hub.registerPage(hidden("admin-grant", "发放", (p, s) -> adminGrant(plugin, p)));
        hub.registerPage(hidden("admin-kit", "Kit 管理", (p, s) -> adminKit(plugin, p)));
        hub.registerPage(hidden("kit-edit", "编辑 Kit", (p, s) -> kitEditHeader(plugin, p), (p, s) -> kitEdit(plugin, p, s)));
        hub.registerPage(hidden("admin-territory", "领地管理", (p, s) -> territory(plugin, p, true, true)));
        hub.registerPage(hidden("admin-region", "地区", (p, s) -> districts(plugin, p)));
        hub.registerPage(hidden("territory-list", "热门领地", (p, s) -> territory(plugin, p, true)));
        hub.registerPage(hidden("my-territory", "我的领地", (p, s) -> territory(plugin, p, false)));
        hub.registerPage(hidden("add-friend", "加好友", (p, s) -> List.of(
                back(), TerminalButton.of("friend.add", EcosStyle.BOOK_JIQING + "聊天输入名字"))));
        hub.registerPage(hidden("skillshop-admin", "技能上架", (p, s) -> skillshopAdmin(plugin, p)));
        hub.registerPage(hidden("music-search", "搜歌结果",
                (p, s) -> musicSearchHeader(plugin, p), (p, s) -> musicSearch(plugin, p)));
        hub.registerPage(hidden("music-history", "点歌历史", (p, s) -> musicHistory(plugin, p)));
        hub.registerPage(hidden("music-fav", "收藏", (p, s) -> musicFav(plugin, p)));
        hub.registerPage(hidden("music-playlist", "歌单", (p, s) -> musicPlaylist(plugin, p)));
        hub.registerPage(hidden("transit-admin", "交通管理", (p, s) -> transitAdmin(plugin, p)));
        hub.registerPage(hidden("transit-station", "车站",
                (p, s) -> stationHeader(plugin, p), (p, s) -> stationEdit(plugin, p)));
        hub.registerPage(hidden("transit-edges", "连边",
                (p, s) -> edgesHeader(plugin, p), (p, s) -> transitEdges(plugin, p)));
        hub.registerPage(hidden("transit-types", "车型", (p, s) -> transitTypes(plugin, p)));
        hub.registerPage(hidden("transit-type-edit", "编辑车型",
                (p, s) -> typeEditHeader(plugin, p), (p, s) -> typeEdit(plugin, p)));
        hub.registerPage(hidden("transit-announce", "报站", (p, s) -> transitAnnounce(plugin, p)));
        hub.registerPage(hidden("transit-claims", "申诉", (p, s) -> transitClaims(plugin, p)));
        hub.registerPage(hidden("transit-riders", "乘客", (p, s) -> transitRiders(plugin, p)));
    }

    static void registerActions(TerminalHub hub, ES2UniPlugin plugin) {
        hub.registerAction("tvm.ticket", (p, s, raw) -> {
            String st = plugin.getTransitTvmGUI().stationOf(p);
            if (st == null) return;
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTicketGUI().open(p, st));
        });
        hub.registerAction("tvm.card", (p, s, raw) -> {
            String err = plugin.getTransitManager().buyCard(p);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已发放交通卡" : "&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("tvm.tappay", (p, s, raw) -> {
            boolean on = !plugin.getTransitManager().isTapPay(p.getUniqueId());
            String err = plugin.getTransitManager().setTapPay(p, on);
            p.sendMessage(ColorUtil.colorize(err != null ? "&8[交通] &c" + err
                    : (on ? "&8[交通] &a已开通闪付" : "&8[交通] &7已关闭闪付")));
            hub.refresh(p);
        });
        hub.registerAction("tvm.adjust", (p, s, raw) -> {
            String st = plugin.getTransitTvmGUI().stationOf(p);
            if (st == null) return;
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdjustGUI().open(p, st));
        });
        hub.registerAction("adjust.pay", (p, s, raw) -> {
            String st = plugin.getTransitAdjustGUI().stationOf(p);
            if (st == null) return;
            String err = plugin.getTransitManager().applyAdjust(p, st);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a补票完成，请尽快出站" : "&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("ticket.pick", (p, s, raw) -> {
            var gui = plugin.getTransitTicketGUI();
            if (raw == null || raw.isBlank()) return;
            if (gui.originOf(p) == null) {
                gui.setOrigin(p, raw);
                Bukkit.getScheduler().runTask(plugin, () -> gui.openDest(p));
                return;
            }
            if (plugin.getTransitManager().allCabins().size() > 1) {
                gui.setDest(p, raw);
                Bukkit.getScheduler().runTask(plugin, () -> gui.openCabin(p));
                return;
            }
            String err = plugin.getTransitManager().buyTicket(p, gui.originOf(p), raw);
            p.sendMessage(ColorUtil.colorize(err != null ? "&8[交通] &c" + err : "&8[交通] &a已购买单程票"));
            gui.cleanup(p);
            String tvm = plugin.getTransitTvmGUI().stationOf(p);
            if (tvm != null) Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTvmGUI().open(p, tvm));
            else hub.open(p, "transit-office");
        });
        hub.registerAction("ticket.cabin", (p, s, raw) -> {
            var gui = plugin.getTransitTicketGUI();
            String err = plugin.getTransitManager().buyTicket(p, gui.originOf(p), gui.destOf(p), raw);
            p.sendMessage(ColorUtil.colorize(err != null ? "&8[交通] &c" + err : "&8[交通] &a已购买单程票"));
            gui.cleanup(p);
            String tvm = plugin.getTransitTvmGUI().stationOf(p);
            if (tvm != null) Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTvmGUI().open(p, tvm));
            else hub.open(p, "transit-office");
        });
        hub.registerAction("ticket.back", (p, s, raw) -> {
            var gui = plugin.getTransitTicketGUI();
            if (gui.pickingCabin(p)) {
                Bukkit.getScheduler().runTask(plugin, () -> gui.openDest(p));
                return;
            }
            String tvm = plugin.getTransitTvmGUI().stationOf(p);
            if (gui.originOf(p) != null && (tvm == null || !tvm.equals(gui.originOf(p)))) {
                Bukkit.getScheduler().runTask(plugin, () -> gui.openOrigin(p));
                return;
            }
            gui.cleanup(p);
            if (tvm != null) Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTvmGUI().open(p, tvm));
            else hub.open(p, "transit-office");
        });
        hub.registerAction("office.tappay", (p, s, raw) -> {
            boolean on = !plugin.getTransitManager().isTapPay(p.getUniqueId());
            String err = plugin.getTransitManager().setTapPay(p, on);
            p.sendMessage(ColorUtil.colorize(err != null ? "&8[交通] &c" + err
                    : (on ? "&8[交通] &a已开通闪付" : "&8[交通] &7已关闭闪付")));
            hub.refresh(p);
        });
        hub.registerAction("office.card", (p, s, raw) -> {
            String err = plugin.getTransitManager().buyCard(p);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已发放交通卡" : "&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("office.lost", (p, s, raw) -> {
            String err = plugin.getTransitManager().reportLost(p);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7已挂失" : "&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("office.ticket", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTicketGUI().open(p, null)));
        hub.registerAction("office.map", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitMapGUI().open(p)));
        hub.registerAction("office.history", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitHistoryGUI().open(p)));
        hub.registerAction("office.board", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitBoardGUI().open(p)));
        hub.registerAction("office.admin", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            s.put("from", "transit-office");
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(p));
        });
        hub.registerAction("map.st", (p, s, raw) -> {
            if (raw == null) return;
            String msg = plugin.getTransitMapGUI().clickStation(p, raw);
            p.sendMessage(ColorUtil.colorize("&8[交通] " + msg));
            hub.refresh(p);
        });
        hub.registerAction("hotel.open", (p, s, raw) -> {
            var h = plugin.getHotelManager().byId(raw);
            if (h == null) return;
            s.put("from", "hotel");
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelDeskGUI().open(p, h));
        });
        hub.registerAction("hotel.out", (p, s, raw) -> {
            var stay = plugin.getHotelManager().stayOf(p.getUniqueId());
            String err = plugin.getHotelManager().checkout(p, stay);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &7已退房" : "&8[酒店] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("hotel.mine", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelListGUI().openMine(p)));
        hub.registerAction("hotel.all", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelListGUI().open(p)));
        hub.registerAction("hotel.create", (p, s, raw) ->
                plugin.getAnvilInputGUI().openForHotelCreate(p));
        hub.registerAction("room.go", (p, s, raw) -> {
            var desk = plugin.getHotelDeskGUI();
            var hotel = desk.hotelOf(p);
            if (hotel == null) return;
            int i = idx(raw);
            if (i < 0 || i >= hotel.rooms.size()) return;
            var room = hotel.rooms.get(i);
            String err;
            if (room.guest != null && room.guest.equals(p.getUniqueId()))
                err = plugin.getHotelManager().checkout(p, room);
            else
                err = plugin.getHotelManager().checkin(p, room);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &7完成" : "&8[酒店] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getHotelDeskGUI().open(p, hotel));
        });
        hub.registerAction("sel.go", (p, s, raw) -> {
            var ctx = plugin.getPlayerSelectGUI().getContext(p);
            plugin.getPlayerSelectGUI().cleanup(p);
            if (raw == null || raw.isBlank() || ctx == null) return;
            Player online = Bukkit.getPlayerExact(raw);
            var off = online != null ? online : AnvilInputGUI.findPlayed(raw);
            if (off == null && online == null) return;
            switch (ctx) {
                case TPA -> {
                    if (online == null) return;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> p.performCommand("tpa " + online.getName()), 1L);
                }
                case TPA_HERE -> {
                    if (online == null) return;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> p.performCommand("tpahere " + online.getName()), 1L);
                }
                case PAY -> Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getAnvilInputGUI().openForPayAmount(p, off != null ? off : online));
                case MAIL -> plugin.getMailManager().giveDraftBook(p, off != null ? off : online);
            }
        });
        hub.registerAction("sel.all", (p, s, raw) -> {
            plugin.getPlayerSelectGUI().cleanup(p);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAnvilInputGUI().openForPayAllAmount(p));
        });
        hub.registerAction("act.whisper", (p, s, raw) -> {
            UUID id = plugin.getPlayerActionGUI().getTarget(p);
            if (id == null) return;
            plugin.getAnvilInputGUI().openForWhisper(p, id);
        });
        hub.registerAction("nb.tog", (p, s, raw) -> {
            try {
                plugin.getNewbieGuideManager().toggle(p.getUniqueId(),
                        com.etherstories.escore.managers.NewbieGuideManager.Step.valueOf(raw));
            } catch (Exception ignored) {}
            hub.refresh(p);
        });
        hub.registerAction("act.mail", (p, s, raw) -> {
            UUID id = plugin.getPlayerActionGUI().getTarget(p);
            if (id == null) return;
            plugin.getMailManager().giveDraftBook(p, Bukkit.getOfflinePlayer(id));
        });
        hub.registerAction("act.report", (p, s, raw) -> {
            if (!plugin.getReportManager().isEnabled()) return;
            UUID id = plugin.getPlayerActionGUI().getTarget(p);
            plugin.getAnvilInputGUI().openForReport(p, id);
        });
        hub.registerAction("act.friend", (p, s, raw) -> {
            UUID id = plugin.getPlayerActionGUI().getTarget(p);
            if (id == null) return;
            if (plugin.getFriendManager().areFriends(p.getUniqueId(), id)) {
                plugin.getFriendManager().removeFriend(p.getUniqueId(), id);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已删除好友"));
            } else {
                Player t = Bukkit.getPlayer(id);
                if (t == null) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &7对方离线，无法发请求"));
                    return;
                }
                plugin.getFriendManager().sendRequest(p, t);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已发送好友请求"));
            }
            hub.refresh(p);
        });
        hub.registerAction("act.pay", (p, s, raw) -> {
            UUID id = plugin.getPlayerActionGUI().getTarget(p);
            if (id == null) return;
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getAnvilInputGUI().openForPayAmount(p, Bukkit.getOfflinePlayer(id)));
        });
        hub.registerAction("pay.ok", (p, s, raw) -> {
            var pay = plugin.getPayConfirmGUI().getPending(p);
            plugin.getPayConfirmGUI().cleanup(p);
            if (pay == null) {
                hub.open(p, s.get("from") == null ? "overview" : s.get("from"));
                return;
            }
            if (pay.all()) {
                p.sendMessage(ColorUtil.colorize(
                        com.etherstories.escore.listeners.PayCommandTaxListener.payAll(plugin, p, pay.amount())));
            } else {
                var target = Bukkit.getOfflinePlayer(pay.targetUuid());
                double tax = plugin.getTaxManager().isPayTaxEnabled()
                        ? plugin.getTaxManager().calcTax(pay.amount(), plugin.getTaxManager().getPayTaxRate()) : 0;
                String error = plugin.getVaultHook().pay(p, target, pay.amount(), tax);
                p.sendMessage(ColorUtil.colorize(error != null ? "&8[ECOS] &7转账失败: " + error
                        : "&8[ECOS] &7已转账 &f" + plugin.getVaultHook().format(pay.amount())));
            }
            hub.open(p, s.get("from") == null ? "overview" : s.get("from"));
        });
        hub.registerAction("pay.no", (p, s, raw) -> {
            plugin.getPayConfirmGUI().cleanup(p);
            hub.open(p, s.get("from") == null ? "overview" : s.get("from"));
        });
        hub.registerAction("ck.do", (p, s, raw) -> {
            String msg = plugin.getCheckInManager().checkInWithReward(p);
            p.sendMessage(ColorUtil.colorize(msg == null ? "&8[ECOS] &a签到完成" : msg));
            hub.refresh(p);
        });
        hub.registerAction("ck.makeup", (p, s, raw) -> {
            java.time.LocalDate day = com.etherstories.escore.managers.CheckInManager.today().minusDays(1);
            if (raw != null && !raw.isBlank()) {
                try { day = java.time.LocalDate.parse(raw); } catch (Exception ignored) {}
            }
            String err = plugin.getCheckInManager().makeup(p.getUniqueId(), day);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[ECOS] &a已补签 " + day : "&8[ECOS] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("ck.prev", (p, s, raw) -> {
            var ym = checkinMonth(s).minusMonths(1);
            s.put("ck-ym", ym.toString());
            hub.refresh(p);
        });
        hub.registerAction("ck.next", (p, s, raw) -> {
            var ym = checkinMonth(s).plusMonths(1);
            s.put("ck-ym", ym.toString());
            hub.refresh(p);
        });
        hub.registerAction("muni.jobs", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getJobBoardGUI().open(p)));
        hub.registerAction("muni.recycle", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getRecycleBinGUI().open(p)));
        hub.registerAction("muni.ins", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getInsuranceGUI().open(p)));
        hub.registerAction("muni.transit", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitOfficeGUI().open(p)));
        hub.registerAction("muni.clean", (p, s, raw) -> {
            String err = plugin.getCleanupRequestManager().request(p);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[市政] &a已发起清理投票" : "&8[市政] &c" + err));
        });
        hub.registerAction("muni.pve", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            p.getInventory().addItem(com.etherstories.escore.items.PveItems.pack(),
                    com.etherstories.escore.items.PveItems.elite());
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已给刷怪棒 / 精英棒  &8右键刷、潜行换种类"));
        });
        hub.registerAction("kit.go", (p, s, raw) -> {
            if (raw == null) return;
            if (raw.startsWith("ess:")) {
                Bukkit.getScheduler().runTask(plugin, () -> p.performCommand("kit " + raw.substring(4)));
                return;
            }
            String err = plugin.getKitManager().claim(p, raw.startsWith("local:") ? raw.substring(6) : raw);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[ECOS] &a已领取" : "&8[ECOS] &c" + err));
        });
        hub.registerAction("aura.go", (p, s, raw) -> {
            try {
                AuraType t = AuraType.valueOf(raw);
                if (plugin.getAuraManager().owns(p.getUniqueId(), t)) {
                    if (plugin.getAuraManager().getEquipped(p.getUniqueId()) == t)
                        plugin.getAuraManager().unequip(p.getUniqueId());
                    else
                        plugin.getAuraManager().equip(p.getUniqueId(), t);
                    hub.refresh(p);
                    return;
                }
                if (!plugin.getVaultHook().isEnabled()) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &c经济不可用"));
                    return;
                }
                if (plugin.getVaultHook().getBalance(p) < t.price) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &c余额不足"));
                    return;
                }
                String err = plugin.getVaultHook().withdraw(p, t.price);
                if (err != null) {
                    p.sendMessage(ColorUtil.colorize("&8[ECOS] &c" + err));
                    return;
                }
                plugin.getAuraManager().giveAura(p.getUniqueId(), t);
                plugin.getAuraManager().equip(p.getUniqueId(), t);
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &a已购买并装备 &f" + t.displayName()));
            } catch (Exception ignored) {}
            hub.refresh(p);
        });
        hub.registerAction("aura.off", (p, s, raw) -> {
            plugin.getAuraManager().unequip(p.getUniqueId());
            hub.refresh(p);
        });
        hub.registerAction("aura.br", (p, s, raw) -> {
            var br = plugin.getAuraManager().cycleBrightness(p.getUniqueId());
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7粒子亮度: &f" + br.label));
            hub.refresh(p);
        });
        hub.registerAction("music.search", (p, s, raw) ->
                plugin.getAnvilInputGUI().openForMusicSearch(p));
        hub.registerAction("music.id", (p, s, raw) ->
                plugin.getAnvilInputGUI().openForMusicAddId(p));
        hub.registerAction("music.vote", (p, s, raw) ->
                plugin.getAllMusicHook().vote(plugin, p));
        hub.registerAction("music.queue", (p, s, raw) ->
                plugin.getAllMusicHook().list(plugin, p));
        hub.registerAction("music.listen", (p, s, raw) -> {
            if (!plugin.getAllMusicHook().ensureHooked(plugin.getLogger())) {
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &cAllMusic 未加载"));
                return;
            }
            boolean muted = plugin.getAllMusicHook().isMuted(p);
            plugin.getAllMusicHook().setListening(plugin, p, muted);
            p.sendMessage(ColorUtil.colorize(!muted
                    ? "&8[ECOS] &c已关闭听歌"
                    : "&8[ECOS] &a已开启听歌"));
            hub.refresh(p);
        });
        hub.registerAction("music.hist", (p, s, raw) -> {
            s.put("from", "music");
            hub.open(p, "music-history");
        });
        hub.registerAction("music.fav", (p, s, raw) -> {
            s.put("from", "music");
            hub.open(p, "music-fav");
        });
        hub.registerAction("music.plist", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            s.put("from", "music");
            hub.open(p, "music-playlist");
        });
        hub.registerAction("tax.pay.toggle", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            TaxManager tax = plugin.getTaxManager();
            tax.setPayEnabled(!tax.isPayTaxEnabled());
            plugin.getAuditLogManager().log(p, "TAX_PAY_TOGGLE", "enabled=" + tax.isPayTaxEnabled());
            hub.refresh(p);
        });
        hub.registerAction("tax.pay.down", (p, s, raw) -> taxPayRate(plugin, p, -0.01));
        hub.registerAction("tax.pay.up", (p, s, raw) -> taxPayRate(plugin, p, 0.01));
        hub.registerAction("tax.qs.toggle", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            TaxManager tax = plugin.getTaxManager();
            tax.setQuickShopEnabled(!tax.isQuickShopTaxEnabled());
            plugin.getAuditLogManager().log(p, "TAX_QS_TOGGLE", "enabled=" + tax.isQuickShopTaxEnabled());
            hub.refresh(p);
        });
        hub.registerAction("tax.qs.down", (p, s, raw) -> taxQsRate(plugin, p, -0.01));
        hub.registerAction("tax.qs.up", (p, s, raw) -> taxQsRate(plugin, p, 0.01));
        hub.registerAction("tax.link.toggle", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            var link = plugin.getESLinkHook();
            if (!link.present()) return;
            link.setTradeEnabled(!link.tradeEnabled());
            plugin.getAuditLogManager().log(p, "TAX_LINK_TOGGLE", "enabled=" + link.tradeEnabled());
            hub.refresh(p);
        });
        hub.registerAction("tax.link.down", (p, s, raw) -> taxLinkRate(plugin, p, -0.01));
        hub.registerAction("tax.link.up", (p, s, raw) -> taxLinkRate(plugin, p, 0.01));
        hub.registerAction("kitadm.create", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getAnvilInputGUI().openForKitName(p);
        });
        hub.registerAction("kitadm.edit", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null || raw.isBlank()) return;
            s.put("kit-armed", null);
            s.put("from", "admin-kit");
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getKitEditGUI().open(p, raw));
        });
        hub.registerAction("kitedit.mode", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getKitEditGUI().toggleMode(p);
            hub.refresh(p);
        });
        hub.registerAction("kitedit.cd", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getAnvilInputGUI().openForKitCooldown(p);
        });
        hub.registerAction("kitedit.import", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            int n = plugin.getKitEditGUI().importFromBackpack(p);
            p.sendMessage(ColorUtil.colorize(n < 0
                    ? "&8[ECOS] &7没有正在编辑的套件"
                    : "&8[ECOS] &7已从背包导入 &f" + n + " &7件（不含盔甲栏/副手）"));
            hub.refresh(p);
        });
        hub.registerAction("kitedit.del", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            String name = plugin.getKitEditGUI().getEditing(p);
            if (name == null) return;
            if (!"1".equals(s.get("kit-armed"))) {
                s.put("kit-armed", "1");
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &c再点一次确认删除 &f" + name));
                hub.refresh(p);
                return;
            }
            plugin.getKitManager().delete(name);
            plugin.getKitEditGUI().cleanup(p);
            s.put("kit-armed", null);
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已删除套件 &f" + name));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminKitGUI().open(p));
        });
        hub.registerAction("kitedit.back", (p, s, raw) -> {
            plugin.getKitEditGUI().cleanup(p);
            s.put("kit-armed", null);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getAdminKitGUI().open(p));
        });
        hub.registerAction("tad.st", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null || raw.isBlank()) return;
            s.put("from", "transit-admin");
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitStationGUI().open(p, raw));
        });
        hub.registerAction("tad.line", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null || raw.isBlank()) return;
            s.put("from", "transit-admin");
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitEdgesGUI().open(p, raw));
        });
        hub.registerAction("tad.types", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            s.put("from", "transit-admin");
            hub.open(p, "transit-types");
        });
        hub.registerAction("tad.announce", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            s.put("from", "transit-admin");
            hub.open(p, "transit-announce");
        });
        hub.registerAction("tad.claims", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            s.put("from", "transit-admin");
            hub.open(p, "transit-claims");
        });
        hub.registerAction("tad.riders", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            s.put("from", "transit-admin");
            hub.open(p, "transit-riders");
        });
        hub.registerAction("st.pos1", (p, s, raw) -> stationFeet(plugin, p, 1));
        hub.registerAction("st.pos2", (p, s, raw) -> stationFeet(plugin, p, 2));
        hub.registerAction("st.apply", (p, s, raw) -> {
            String sid = plugin.getTransitStationGUI().stationOf(p);
            if (sid == null) return;
            String err = plugin.getTransitManager().applyBox(p, sid);
            p.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &a范围已更新: &f" + plugin.getTransitManager().boxLabel(plugin.getTransitManager().getStation(sid))
                    : "&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("st.move", (p, s, raw) -> {
            String sid = plugin.getTransitStationGUI().stationOf(p);
            if (sid == null) return;
            var tm = plugin.getTransitManager();
            var st = tm.getStation(sid);
            String err = tm.moveStation(sid, p.getLocation(), tm.stationRadius(st));
            p.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &a判定区已移到脚下（半径 " + tm.stationRadius(tm.getStation(sid)) + "）"
                    : "&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("st.tp", (p, s, raw) -> {
            String sid = plugin.getTransitStationGUI().stationOf(p);
            if (sid == null) return;
            String err = plugin.getTransitManager().teleportToStation(p, sid);
            if (err != null) p.sendMessage(ColorUtil.colorize("&8[交通] &c" + err));
        });
        hub.registerAction("st.zh", (p, s, raw) -> {
            String sid = plugin.getTransitStationGUI().stationOf(p);
            if (sid == null) return;
            plugin.getAnvilInputGUI().openForStationRename(p, sid, false);
        });
        hub.registerAction("st.en", (p, s, raw) -> {
            String sid = plugin.getTransitStationGUI().stationOf(p);
            if (sid == null) return;
            plugin.getAnvilInputGUI().openForStationRename(p, sid, true);
        });
        hub.registerAction("st.skip", (p, s, raw) -> {
            String sid = plugin.getTransitStationGUI().stationOf(p);
            if (sid == null) return;
            var tm = plugin.getTransitManager();
            String err = tm.setSkipStop(sid, !tm.isSkipStop(sid));
            p.sendMessage(ColorUtil.colorize(err != null ? "&8[交通] &c" + err
                    : (tm.isSkipStop(sid) ? "&8[交通] &e已设为通过不停车" : "&8[交通] &a已恢复停车")));
            hub.refresh(p);
        });
        hub.registerAction("st.gatemode", (p, s, raw) -> {
            String m = plugin.getTransitStationGUI().cycleGateMode(p);
            p.sendMessage(ColorUtil.colorize("&8[交通] &e闸机模式: &f" + m
                    + " &7（BOTH 进出 / IN 仅进 / OUT 仅出）"));
            hub.refresh(p);
        });
        hub.registerAction("st.gate", (p, s, raw) -> {
            String sid = plugin.getTransitStationGUI().stationOf(p);
            if (sid == null) return;
            String mode = plugin.getTransitStationGUI().gateModeOf(p);
            p.getInventory().addItem(TransitItems.gate(sid, mode));
            p.sendMessage(ColorUtil.colorize("&8[交通] &a已给予闸机牌 &8[" + mode + "]"));
        });
        hub.registerAction("st.tvm", (p, s, raw) -> {
            String sid = plugin.getTransitStationGUI().stationOf(p);
            if (sid == null) return;
            p.getInventory().addItem(TransitItems.tvm(sid));
            p.sendMessage(ColorUtil.colorize("&8[交通] &a已给予原版售票机（磁石）"));
        });
        hub.registerAction("st.adjust", (p, s, raw) -> {
            String sid = plugin.getTransitStationGUI().stationOf(p);
            if (sid == null) return;
            p.getInventory().addItem(TransitItems.adjust(sid));
            p.sendMessage(ColorUtil.colorize("&8[交通] &a已给予补票处牌"));
        });
        hub.registerAction("st.bindtvm", (p, s, raw) -> {
            String sid = plugin.getTransitStationGUI().stationOf(p);
            if (sid == null) return;
            plugin.getTransitManager().beginTvmBind(p, sid);
            try { p.closeInventory(); } catch (Throwable ignored) {}
            p.sendMessage(ColorUtil.colorize("&8[交通] &e看向要当售票机的方块，右键绑定。"));
        });
        hub.registerAction("st.del", (p, s, raw) -> {
            var gui = plugin.getTransitStationGUI();
            String sid = gui.stationOf(p);
            if (sid == null) return;
            var tm = plugin.getTransitManager();
            var st = tm.getStation(sid);
            if (st == null) {
                gui.cleanup(p);
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(p));
                return;
            }
            if (!gui.isArmed(p)) {
                gui.armDelete(p, true);
                p.sendMessage(ColorUtil.colorize("&8[交通] &c再点一次确认删除"));
                hub.refresh(p);
                return;
            }
            String err = tm.deleteStation(sid);
            gui.cleanup(p);
            p.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &7已删除车站 &f" + st.displayName()
                    : "&8[交通] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(p));
        });
        hub.registerAction("st.line", (p, s, raw) -> {
            String sid = plugin.getTransitStationGUI().stationOf(p);
            if (sid == null || raw == null || raw.isBlank()) return;
            var tm = plugin.getTransitManager();
            var st = tm.getStation(sid);
            if (st == null) return;
            boolean on = st.lineIds().contains(raw);
            String err = on ? tm.removeStationLine(sid, raw) : tm.addStationLine(sid, raw);
            p.sendMessage(ColorUtil.colorize(err == null
                    ? (on ? "&8[交通] &7已从该站摘掉线路" : "&8[交通] &a已挂上线路，去该线连接页把邻站连上")
                    : "&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("st.back", (p, s, raw) -> {
            plugin.getTransitStationGUI().cleanup(p);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(p));
        });
        hub.registerAction("edge.st", (p, s, raw) -> {
            var gui = plugin.getTransitEdgesGUI();
            String line = gui.lineOf(p);
            if (line == null || raw == null || raw.isBlank()) return;
            String from = gui.pendingFrom(p);
            if (from == null || from.equals(raw)) {
                gui.setPendingFrom(p, from != null && from.equals(raw) ? null : raw);
                hub.refresh(p);
                return;
            }
            String err = plugin.getTransitManager().addEdge(from, raw, line, -1);
            gui.setPendingFrom(p, null);
            p.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &a已连接 &f" + plugin.getTransitManager().stationName(from)
                    + " ↔ " + plugin.getTransitManager().stationName(raw)
                    : "&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("edge.cut", (p, s, raw) -> {
            var gui = plugin.getTransitEdgesGUI();
            String line = gui.lineOf(p);
            if (line == null || raw == null || !raw.contains("|")) return;
            String[] parts = raw.split("\\|", 2);
            String err = plugin.getTransitManager().removeEdge(parts[0], parts[1], line);
            p.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &7已断开 &f" + plugin.getTransitManager().stationName(parts[0])
                    + " ↔ " + plugin.getTransitManager().stationName(parts[1])
                    : "&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("edge.del", (p, s, raw) -> {
            var gui = plugin.getTransitEdgesGUI();
            String line = gui.lineOf(p);
            if (line == null) return;
            if (!gui.isArmed(p)) {
                gui.armDelete(p, true);
                p.sendMessage(ColorUtil.colorize("&8[交通] &c再点一次确认删线"));
                hub.refresh(p);
                return;
            }
            String err = plugin.getTransitManager().deleteLine(line);
            gui.cleanup(p);
            p.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[交通] &7已删除线路 &f" + line : "&8[交通] &c" + err));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(p));
        });
        hub.registerAction("edge.back", (p, s, raw) -> {
            plugin.getTransitEdgesGUI().cleanup(p);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitAdminGUI().open(p));
        });
        hub.registerAction("ms.play", (p, s, raw) -> {
            int i = idx(raw);
            var songs = plugin.getMusicSearchResultGUI().cached(p);
            if (i < 0 || i >= songs.size()) return;
            var song = songs.get(i);
            plugin.getMusicSearchResultGUI().cleanup(p);
            plugin.getAllMusicHook().select(plugin, p, song.selectIndex());
            plugin.getMusicHistoryManager().recordManual(song.id(), song.name(), song.author(), p.getName());
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已点歌: &f" + song.name() + " &8- &7" + song.author()));
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getMusicMenuGUI().open(p), 20L);
        });
        hub.registerAction("ms.prev", (p, s, raw) -> {
            if (plugin.getAllMusicHook().prevSearchPage(p))
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicSearchResultGUI().open(p));
        });
        hub.registerAction("ms.next", (p, s, raw) -> {
            if (plugin.getAllMusicHook().nextSearchPage(p))
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMusicSearchResultGUI().open(p));
        });
        hub.registerAction("ms.retry", (p, s, raw) ->
                plugin.getAnvilInputGUI().openForMusicSearch(p));
        hub.registerAction("adev.create", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getAnvilInputGUI().openForEventName(p);
        });
        hub.registerAction("adev.del", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null || raw.isBlank()) return;
            var ev = plugin.getEventManager().getEvent(raw);
            if (ev == null) return;
            plugin.getEventManager().removeEvent(raw);
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7活动 &f" + ev.getName() + " &7已删除"));
            hub.refresh(p);
        });
        hub.registerAction("bc.add", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getAnvilInputGUI().openForBroadcastAdd(p);
        });
        hub.registerAction("bc.once", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getAnvilInputGUI().openForBroadcastOnce(p);
        });
        hub.registerAction("bc.edit", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            int i = idx(raw);
            if (i < 0) return;
            plugin.getAnvilInputGUI().openForBroadcastEdit(p, i);
        });
        hub.registerAction("bc.days", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            int i = idx(raw);
            plugin.getAdminBroadcastGUI().cycleDays(i);
            hub.refresh(p);
        });
        hub.registerAction("bc.del", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            int i = idx(raw);
            var entries = new ArrayList<>(plugin.getConfigManager().getBroadcastEntries());
            if (i < 0 || i >= entries.size()) return;
            entries.remove(i);
            plugin.getAdminBroadcastGUI().saveEntries(entries);
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已删除消息 #" + (i + 1)));
            hub.refresh(p);
        });
        hub.registerAction("bc.int.up", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            long cur = plugin.getConfigManager().getBroadcastInterval();
            plugin.getAdminBroadcastGUI().setInterval(cur + 30);
            hub.refresh(p);
        });
        hub.registerAction("bc.int.down", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            long cur = plugin.getConfigManager().getBroadcastInterval();
            plugin.getAdminBroadcastGUI().setInterval(cur - 30);
            hub.refresh(p);
        });
        hub.registerAction("grant.makeup", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getAnvilInputGUI().openForGrant(p, "makeup");
        });
        hub.registerAction("grant.lucky", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getAnvilInputGUI().openForGrant(p, "lucky");
        });
        hub.registerAction("music.stop", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> p.performCommand("music stop")));
        hub.registerAction("skill.buy", (p, s, raw) -> {
            if (raw == null || raw.isBlank() || plugin.getSkillShopManager() == null) return;
            var skill = com.etherstories.escore.weapons.SkillType.fromKey(raw);
            if (skill == null) return;
            String err = plugin.getSkillShopManager().buy(p, skill);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[技能店] &a已购买" : "&8[技能店] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("skill.price", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null) return;
            var skill = com.etherstories.escore.weapons.SkillType.fromKey(raw);
            if (skill == null) return;
            plugin.getAnvilInputGUI().openForSkillShopPrice(p, skill);
        });
        hub.registerAction("skill.toggle", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null) return;
            var skill = com.etherstories.escore.weapons.SkillType.fromKey(raw);
            if (skill == null) return;
            boolean on = plugin.getSkillShopManager().isForSale(skill);
            plugin.getSkillShopManager().setEnabled(skill, !on);
            hub.refresh(p);
        });
        hub.registerAction("estate.go", (p, s, raw) -> {
            if (raw == null) return;
            s.put("from", "estate");
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateRoomsGUI().open(p, raw));
        });
        hub.registerAction("estate.buy", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            var u = plugin.getEstateManager().byId(raw);
            if (u == null) return;
            var em = plugin.getEstateManager();
            if (em.hasPending(p.getUniqueId(), u.id())) {
                String err = em.confirmBuy(p);
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[房产] &a已购入 &f" + u.address()
                        : "&8[房产] &c" + err));
            } else {
                String err = em.beginBuy(p, u);
                if (err != null) p.sendMessage(ColorUtil.colorize("&8[房产] &c" + err));
                else p.sendMessage(ColorUtil.colorize("&8[房产] &e15秒内再点一次确认购买 &f" + u.address()));
            }
            hub.refresh(p);
        });
        hub.registerAction("estate.wand", (p, s, raw) -> {
            com.etherstories.escore.items.EstateWand.give(p);
            try { p.closeInventory(); } catch (Throwable ignored) {}
            p.sendMessage(ColorUtil.colorize("&8[房产] &a选区棒: &f左键一角  右键对角"));
        });
        hub.registerAction("estate.reg", (p, s, raw) -> {
            if (!plugin.getEstateManager().hasSelection(p.getUniqueId())) {
                p.sendMessage(ColorUtil.colorize("&8[房产] &c先用选区棒圈出房间"));
                return;
            }
            plugin.getAnvilInputGUI().openForEstateRegister(p);
        });
        hub.registerAction("estate.door", (p, s, raw) -> {
            var u = plugin.getEstateManager().at(p.getLocation());
            String err = plugin.getEstateManager().setDoor(p, u);
            p.sendMessage(ColorUtil.colorize(err == null
                    ? "&8[房产] &a门口已设 &f" + u.address() : "&8[房产] &c" + err));
        });
        hub.registerAction("estate.sign", (p, s, raw) -> {
            var u = plugin.getEstateManager().at(p.getLocation());
            if (u == null) {
                p.sendMessage(ColorUtil.colorize("&8[房产] &7站在房间里再绑牌"));
                return;
            }
            if (!p.hasPermission("es2uni.admin") && !p.getUniqueId().equals(u.owner())
                    && !p.getUniqueId().equals(u.createdBy())) {
                p.sendMessage(ColorUtil.colorize("&8[房产] &c这不是你的房间"));
                return;
            }
            plugin.getEstateManager().placeSign(p, u);
        });
        hub.registerAction("estate.help", (p, s, raw) -> {
            p.sendMessage(ColorUtil.colorize("&8[房产] &f选区棒圈房间 → 登记 → 站门口绑门 → 看墙绑牌"));
            p.sendMessage(ColorUtil.colorize("&8我的房产可上架/改价。待售页点两次确认购买。"));
        });
        hub.registerAction("estate.list", (p, s, raw) -> {
            if (raw == null) return;
            var u = plugin.getEstateManager().byId(raw);
            if (u == null) return;
            if (u.listed()) {
                String err = plugin.getEstateManager().setSale(p, u, u.price(), false);
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[房产] &7已下架" : "&8[房产] &c" + err));
                hub.refresh(p);
                return;
            }
            if (u.price() <= 0) {
                plugin.getAnvilInputGUI().openForEstatePrice(p, u);
                return;
            }
            String err = plugin.getEstateManager().setSale(p, u, u.price(), true);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[房产] &a已上架" : "&8[房产] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("estate.price", (p, s, raw) -> {
            if (raw == null) return;
            var u = plugin.getEstateManager().byId(raw);
            if (u == null) return;
            plugin.getAnvilInputGUI().openForEstatePrice(p, u);
        });
        hub.registerAction("estate.del", (p, s, raw) -> {
            if (raw == null) return;
            var u = plugin.getEstateManager().byId(raw);
            if (u == null) return;
            if (!"1".equals(s.get("est-armed"))) {
                s.put("est-armed", "1");
                p.sendMessage(ColorUtil.colorize("&8[房产] &c再点一次确认删除 &f" + u.address()));
                hub.refresh(p);
                return;
            }
            s.put("est-armed", null);
            String err = plugin.getEstateManager().delete(p, u);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[房产] &7已删除" : "&8[房产] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("estate.sale", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateSaleGUI().open(p)));
        hub.registerAction("estate.mine", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateMineGUI().openMine(p)));
        hub.registerAction("estate.tools", (p, s, raw) ->
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getEstateToolsGUI().open(p)));
        hub.registerAction("ev.join", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            var ev = plugin.getEventManager().getEvent(raw);
            if (ev == null) return;
            if (ev.hasJoined(p.getUniqueId())) {
                plugin.getEventManager().leave(raw, p.getUniqueId());
                p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已退出活动"));
            } else {
                boolean ok = plugin.getEventManager().join(raw, p.getUniqueId());
                p.sendMessage(ColorUtil.colorize(ok ? "&8[ECOS] &7已加入活动" : "&8[ECOS] &c无法加入（满员或不存在）"));
            }
            hub.refresh(p);
        });
        hub.registerAction("job.take", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            String err = plugin.getJobBoardManager().take(p, raw);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[招工] &a已接委托" : "&8[招工] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("job.done", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            String err = plugin.getJobBoardManager().complete(p, raw);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[招工] &a已完成" : "&8[招工] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("job.cancel", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            String err = plugin.getJobBoardManager().cancel(p, raw);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[招工] &7已取消" : "&8[招工] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("job.post", (p, s, raw) ->
                plugin.getAnvilInputGUI().openForJobPost(p, "1".equals(raw)));
        hub.registerAction("job.hist", (p, s, raw) -> {
            s.put("from", "jobs");
            hub.open(p, "job-history");
        });
        hub.registerAction("job.edit", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null || raw.isBlank()) return;
            plugin.getAnvilInputGUI().openForJobEditTitle(p, raw);
        });
        hub.registerAction("job.lock", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            var j = plugin.getJobBoardManager().get(raw);
            if (j == null) return;
            boolean lock = !j.locked;
            String err = plugin.getJobBoardManager().setLocked(p, raw, lock);
            p.sendMessage(ColorUtil.colorize(err == null
                    ? (lock ? "&8[招工] &7已锁定不再加人" : "&8[招工] &a已解锁加人")
                    : "&8[招工] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("job.kick", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            int cut = raw.indexOf('|');
            if (cut <= 0 || cut >= raw.length() - 1) return;
            String err = plugin.getJobBoardManager().removeWorker(p, raw.substring(0, cut), raw.substring(cut + 1));
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[招工] &7已踢出" : "&8[招工] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("job.force", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null || raw.isBlank()) return;
            if (!"1".equals(s.get("job-armed"))) {
                s.put("job-armed", "1");
                s.put("job-force", raw);
                p.sendMessage(ColorUtil.colorize("&8[招工] &c再点一次确认强制删除并退款"));
                hub.refresh(p);
                return;
            }
            if (!raw.equals(s.get("job-force"))) {
                s.put("job-armed", "1");
                s.put("job-force", raw);
                hub.refresh(p);
                return;
            }
            s.put("job-armed", null);
            s.put("job-force", null);
            String err = plugin.getJobBoardManager().forceRemove(p, raw);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[招工] &7已强制删除" : "&8[招工] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("estate.tp", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            var u = plugin.getEstateManager().byId(raw);
            if (u == null) return;
            var loc = u.teleportLoc();
            if (loc == null) {
                p.sendMessage(ColorUtil.colorize("&8[房产] &c无法传送"));
                return;
            }
            p.teleport(loc);
            p.sendMessage(ColorUtil.colorize("&8[房产] &7已到 &f" + u.address()));
        });
        hub.registerAction("skill.admin", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            s.put("from", "skillshop");
            hub.open(p, "skillshop-admin");
        });
        hub.registerAction("rec.take", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            String err = plugin.getRecycleBinManager().reclaim(p, raw);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[回收站] &a已取回" : "&8[回收站] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("ins.bak", (p, s, raw) -> {
            plugin.getInsuranceManager().backup(p);
            var b = plugin.getInsuranceManager().getBackup(p.getUniqueId());
            p.sendMessage(ColorUtil.colorize(b == null
                    ? "&8[保险] &7未写入（空包或物品骤减）"
                    : "&8[保险] &a已备份 &f" + b.timeLabel() + " &8(" + b.itemCount() + "件)"));
            hub.refresh(p);
        });
        hub.registerAction("ins.claim", (p, s, raw) -> {
            String err = plugin.getInsuranceManager().requestClaim(p);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[保险] &a已提交理赔" : "&8[保险] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("ins.open", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null || raw.isBlank()) return;
            s.put("from", "insurance");
            s.put("ins-claim", raw);
            hub.open(p, "insurance-bak");
        });
        hub.registerAction("ins.ok", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            String claimId = s.get("ins-claim");
            if (claimId == null) claimId = plugin.getInsuranceBackupGUI().getClaimId(p);
            int i = idx(raw);
            if (claimId == null || i < 0) return;
            String err = plugin.getInsuranceManager().approve(p, claimId, i);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[保险] &a已恢复并赔偿" : "&8[保险] &c" + err));
            hub.open(p, "insurance");
        });
        hub.registerAction("ins.no", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            String claimId = raw != null && !raw.isBlank() ? raw : s.get("ins-claim");
            if (claimId == null) claimId = plugin.getInsuranceBackupGUI().getClaimId(p);
            if (claimId == null) return;
            String err = plugin.getInsuranceManager().deny(p, claimId);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[保险] &7已拒绝" : "&8[保险] &c" + err));
            hub.open(p, "insurance");
        });
        hub.registerAction("sc.vote", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            String err = plugin.getShowcaseManager().vote(p, raw);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[展示] &a已投票" : "&8[展示] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("sc.tp", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            if (!plugin.getShowcaseManager().teleport(p, raw))
                p.sendMessage(ColorUtil.colorize("&8[展示] &c无法传送"));
        });
        hub.registerAction("sc.del", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            boolean ok = plugin.getShowcaseManager().remove(raw, p.getUniqueId(), p.hasPermission("es2uni.admin"));
            p.sendMessage(ColorUtil.colorize(ok ? "&8[展示] &7已删除" : "&8[展示] &c不能删这个点"));
            hub.refresh(p);
        });
        hub.registerAction("sc.reg", (p, s, raw) ->
                plugin.getAnvilInputGUI().openForShowcaseTitle(p));
        hub.registerAction("terr.tp", (p, s, raw) -> {
            var r = regionByName(plugin, raw);
            if (r == null) return;
            plugin.getTerritoryListGUI().teleport(p, r);
        });
        hub.registerAction("terr.vis", (p, s, raw) -> {
            if (raw == null) return;
            var r = plugin.getRegionManager().toggleVisible(raw, p.getUniqueId(),
                    p.hasPermission("es2uni.admin"));
            p.sendMessage(ColorUtil.colorize(r.isEmpty()
                    ? "&8[领地] &c找不到或无权"
                    : (r.get() ? "&8[领地] &a已公开" : "&8[领地] &7已隐藏")));
            hub.refresh(p);
        });
        hub.registerAction("mh.play", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            plugin.getAllMusicHook().addById(plugin, p, raw);
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已点歌"));
        });
        hub.registerAction("mh.fav", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            String[] parts = raw.split("\\|", 4);
            String id = parts[0];
            String name = parts.length > 1 ? parts[1] : id;
            String author = parts.length > 2 ? parts[2] : "";
            boolean on = plugin.getMusicFavoritesManager().toggle(p, id, name, author, "");
            p.sendMessage(ColorUtil.colorize(on ? "&8[ECOS] &a已收藏" : "&8[ECOS] &7已取消收藏"));
            hub.refresh(p);
        });
        hub.registerAction("mf.play", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            plugin.getAllMusicHook().addById(plugin, p, raw);
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &7已点歌"));
        });
        hub.registerAction("mf.del", (p, s, raw) -> {
            if (raw == null || raw.isBlank()) return;
            plugin.getMusicFavoritesManager().remove(p.getUniqueId(), raw);
            hub.refresh(p);
        });
        hub.registerAction("pl.start", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null || raw.isBlank()) return;
            boolean shuffle = raw.endsWith("|1");
            String key = shuffle ? raw.substring(0, raw.length() - 2) : raw;
            String err = plugin.getMusicPlaylistManager().start(p, key, shuffle);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[ECOS] &a歌单已开始" : "&8[ECOS] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("pl.stop", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getMusicPlaylistManager().stop(true);
            hub.refresh(p);
        });
        hub.registerAction("ty.create", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getAnvilInputGUI().openForTransitTypeCreate(p);
        });
        hub.registerAction("ty.edit", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null || raw.isBlank()) return;
            s.put("from", "transit-types");
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTransitTypeEditGUI().open(p, raw));
        });
        hub.registerAction("ty.rename", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            String id = plugin.getTransitTypeEditGUI().editing(p);
            if (id == null) return;
            plugin.getAnvilInputGUI().openForTransitTypeRename(p, id);
        });
        hub.registerAction("ty.color", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null) return;
            String id = plugin.getTransitTypeEditGUI().editing(p);
            if (id == null) return;
            String err = plugin.getTransitManager().setTypeColor(id, raw);
            if (err != null) p.sendMessage(ColorUtil.colorize("&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("ty.del", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            String id = plugin.getTransitTypeEditGUI().editing(p);
            if (id == null) return;
            if (!"1".equals(s.get("type-armed"))) {
                s.put("type-armed", "1");
                p.sendMessage(ColorUtil.colorize("&8[交通] &c再点一次确认删除"));
                hub.refresh(p);
                return;
            }
            s.put("type-armed", null);
            String err = plugin.getTransitManager().deleteType(id);
            plugin.getTransitTypeEditGUI().cleanup(p);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7已删除" : "&8[交通] &c" + err));
            hub.open(p, "transit-types");
        });
        hub.registerAction("an.arr", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            boolean on = plugin.getConfig().getBoolean("transit.announce.enabled", true);
            plugin.getTransitManager().setAnnounceEnabled(!on);
            hub.refresh(p);
        });
        hub.registerAction("an.dep", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            boolean on = plugin.getConfig().getBoolean("transit.announce.depart-enabled", true);
            plugin.getTransitManager().setDepartEnabled(!on);
            hub.refresh(p);
        });
        hub.registerAction("an.stack", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getTransitManager().setStackRepeats(!plugin.getTransitManager().stackRepeats());
            hub.refresh(p);
        });
        hub.registerAction("an.set.arr", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null) return;
            String err = plugin.getTransitManager().setAnnouncePreset(raw);
            if (err != null) p.sendMessage(ColorUtil.colorize("&8[交通] &c" + err));
            else plugin.getTransitManager().playArriveSound(p);
            hub.refresh(p);
        });
        hub.registerAction("an.set.dep", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null) return;
            String err = plugin.getTransitManager().setDepartPreset(raw);
            if (err != null) p.sendMessage(ColorUtil.colorize("&8[交通] &c" + err));
            else plugin.getTransitManager().playDepartSound(p);
            hub.refresh(p);
        });
        hub.registerAction("an.prev.arr", (p, s, raw) ->
                plugin.getTransitManager().playArriveSound(p));
        hub.registerAction("an.prev.dep", (p, s, raw) ->
                plugin.getTransitManager().playDepartSound(p));
        hub.registerAction("tc.ok", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null) return;
            String err = plugin.getTransitManager().resolveClaim(p, raw, true);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已通过申诉" : "&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("tc.no", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null) return;
            String err = plugin.getTransitManager().resolveClaim(p, raw, false);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7已驳回" : "&8[交通] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("tr.clear", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null) return;
            try {
                String err = plugin.getTransitManager().adminClearJourney(UUID.fromString(raw));
                p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &7已清除行程" : "&8[交通] &c" + err));
            } catch (Exception ignored) {}
            hub.refresh(p);
        });
        hub.registerAction("office.claim", (p, s, raw) -> {
            String err = plugin.getTransitManager().fileClaim(p);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[交通] &a已提交行程异常申报" : "&8[交通] &c" + err));
        });
        hub.registerAction("hotel.card", (p, s, raw) -> {
            var stay = plugin.getHotelManager().stayOf(p.getUniqueId());
            if (stay == null) {
                p.sendMessage(ColorUtil.colorize("&8[酒店] &7当前没有入住"));
                return;
            }
            var unit = plugin.getHotelManager().unitOf(stay);
            var hotel = plugin.getHotelDeskGUI().hotelOf(p);
            if (hotel == null) {
                for (var h : plugin.getHotelManager().all()) {
                    if (h.rooms.contains(stay)) { hotel = h; break; }
                }
            }
            if (hotel == null) return;
            String err = plugin.getHotelManager().giveCard(p, hotel, stay, unit);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &a已补发房卡" : "&8[酒店] &c" + err));
        });
        hub.registerAction("hotel.bind", (p, s, raw) -> {
            var hotel = plugin.getHotelDeskGUI().hotelOf(p);
            if (hotel == null || !plugin.getHotelManager().staff(p, hotel)) return;
            var u = plugin.getEstateManager().at(p.getLocation());
            String err = plugin.getHotelManager().bind(p, hotel, u);
            p.sendMessage(ColorUtil.colorize(err == null ? "&8[酒店] &a已绑房间" : "&8[酒店] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("hotel.mgr", (p, s, raw) -> {
            var hotel = plugin.getHotelDeskGUI().hotelOf(p);
            if (hotel == null || !plugin.getHotelManager().staff(p, hotel)) return;
            plugin.getAnvilInputGUI().openForHotelMgr(p, hotel.id);
        });
        hub.registerAction("hotel.xfer", (p, s, raw) -> {
            var hotel = plugin.getHotelDeskGUI().hotelOf(p);
            if (hotel == null || !plugin.getHotelManager().owner(p, hotel)) return;
            plugin.getAnvilInputGUI().openForHotelTransfer(p, hotel.id);
        });
        hub.registerAction("hotel.del", (p, s, raw) -> {
            var hotel = plugin.getHotelDeskGUI().hotelOf(p);
            if (hotel == null || !plugin.getHotelManager().owner(p, hotel)) return;
            plugin.getAnvilInputGUI().openForHotelDelete(p, hotel.id, hotel.name);
        });
        hub.registerAction("room.lock", (p, s, raw) -> {
            var hotel = plugin.getHotelDeskGUI().hotelOf(p);
            var room = hotelRoom(plugin, p, raw);
            if (hotel == null || room == null || !plugin.getHotelManager().staff(p, hotel)) return;
            String err = plugin.getHotelManager().setLocked(p, room, !room.locked);
            if (err != null) p.sendMessage(ColorUtil.colorize("&8[酒店] &c" + err));
            hub.refresh(p);
        });
        hub.registerAction("room.price", (p, s, raw) -> {
            var hotel = plugin.getHotelDeskGUI().hotelOf(p);
            var room = hotelRoom(plugin, p, raw);
            if (hotel == null || room == null || !plugin.getHotelManager().staff(p, hotel)) return;
            plugin.getAnvilInputGUI().openForHotelPrice(p, room.unitId, String.format("%.0f", room.price));
        });
        hub.registerAction("room.type", (p, s, raw) -> {
            var hotel = plugin.getHotelDeskGUI().hotelOf(p);
            var room = hotelRoom(plugin, p, raw);
            if (hotel == null || room == null || !plugin.getHotelManager().staff(p, hotel)) return;
            var types = com.etherstories.escore.hotel.HotelRoomType.values();
            int i = 0;
            for (; i < types.length; i++) if (types[i] == room.type) break;
            var next = types[(i + 1) % types.length];
            String err = plugin.getHotelManager().setType(p, room, next);
            if (err != null) p.sendMessage(ColorUtil.colorize("&8[酒店] &c" + err));
            else p.sendMessage(ColorUtil.colorize("&8[酒店] &7房型: &f" + next.label));
            hub.refresh(p);
        });
        hub.registerAction("reg.pos1", (p, s, raw) -> {
            var loc = p.getLocation();
            plugin.getRegionManager().setPos1(p.getUniqueId(), loc.getWorld().getName(),
                    loc.getBlockX(), loc.getBlockZ());
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &a角点1 &f" + loc.getBlockX() + "," + loc.getBlockZ()));
            hub.refresh(p);
        });
        hub.registerAction("reg.pos2", (p, s, raw) -> {
            var loc = p.getLocation();
            plugin.getRegionManager().setPos2(p.getUniqueId(), loc.getWorld().getName(),
                    loc.getBlockX(), loc.getBlockZ());
            p.sendMessage(ColorUtil.colorize("&8[ECOS] &6角点2 &f" + loc.getBlockX() + "," + loc.getBlockZ()));
            hub.refresh(p);
        });
        hub.registerAction("reg.create", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin")) return;
            plugin.getAnvilInputGUI().openForRegionCreate(p);
        });
        hub.registerAction("terr.create", (p, s, raw) ->
                plugin.getAnvilInputGUI().openForTerritoryCreate(p));
        hub.registerAction("terr.spawn", (p, s, raw) -> {
            if (raw == null) return;
            var loc = p.getLocation();
            var r = plugin.getRegionManager().setSpawn(raw, p.getUniqueId(),
                    p.hasPermission("es2uni.admin"), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            p.sendMessage(ColorUtil.colorize(r.isEmpty()
                    ? "&8[领地] &c不能设出生点"
                    : "&8[领地] &a出生点已设 &f" + raw));
        });
        hub.registerAction("reg.del", (p, s, raw) -> {
            if (!p.hasPermission("es2uni.admin") || raw == null) return;
            boolean ok = plugin.getRegionManager().deleteByName(raw, p.getUniqueId(), true);
            p.sendMessage(ColorUtil.colorize(ok ? "&8[ECOS] &7已删除 &f" + raw : "&8[ECOS] &c找不到"));
            hub.refresh(p);
        });
    }

    private interface Buttons {
        List<TerminalButton> get(Player player, TerminalSession session);
    }

    private interface Header {
        List<String> get(Player player, TerminalSession session);
    }

    private static TerminalPage hidden(String id, String label, Buttons buttons) {
        return hidden(id, label, (p, s) -> List.of(), buttons);
    }

    private static TerminalPage hidden(String id, String label, Header header, Buttons buttons) {
        return new TerminalPage() {
            @Override public String id() { return id; }
            @Override public String label() { return label; }
            @Override public boolean listed(Player player) { return false; }
            @Override public List<String> header(Player player, TerminalSession session) {
                return header.get(player, session);
            }
            @Override public List<TerminalButton> buttons(Player player, TerminalSession session) {
                return buttons.get(player, session);
            }
        };
    }

    private static TerminalButton back() {
        return TerminalButton.of("list.back", EcosStyle.BOOK_JIQING + "← 返回");
    }

    private static int idx(String raw) {
        try { return Integer.parseInt(raw.trim()); }
        catch (Exception e) { return -1; }
    }

    private static java.time.YearMonth checkinMonth(TerminalSession s) {
        String raw = s.get("ck-ym");
        if (raw != null) {
            try { return java.time.YearMonth.parse(raw); } catch (Exception ignored) {}
        }
        return java.time.YearMonth.from(com.etherstories.escore.managers.CheckInManager.today());
    }

    private static HotelManager.Room hotelRoom(ES2UniPlugin plugin, Player p, String unitId) {
        var hotel = plugin.getHotelDeskGUI().hotelOf(p);
        if (hotel == null || unitId == null) return null;
        for (var r : hotel.rooms) if (unitId.equals(r.unitId)) return r;
        return null;
    }

    private static List<TerminalButton> tvm(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        String st = plugin.getTransitTvmGUI().stationOf(p);
        TransitManager tm = plugin.getTransitManager();
        TransitManager.Station s = st == null ? null : tm.getStation(st);
        String name = s == null ? (st == null ? "?" : st) : s.displayName();
        out.add(TerminalButton.of("tvm.ticket", EcosStyle.BOOK_COBALT + "买单程 · " + name, "从本站出发"));
        out.add(TerminalButton.of("tvm.card", EcosStyle.BOOK_JIQING + "领交通卡", "工本费领卡"));
        boolean tap = tm.isTapPay(p.getUniqueId());
        out.add(TerminalButton.of("tvm.tappay",
                tap ? EcosStyle.BOOK_JIQING + "闪付已开" : EcosStyle.BOOK_ORANGE + "开通闪付",
                "空手刷闸"));
        out.add(TerminalButton.of("tvm.adjust", EcosStyle.BOOK_ORANGE + "补票", "无进站记录先补"));
        return out;
    }

    private static List<TerminalButton> adjust(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        String st = plugin.getTransitAdjustGUI().stationOf(p);
        if (st == null) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "未绑定车站"));
            return out;
        }
        var q = plugin.getTransitManager().quoteAdjust(p, st);
        out.add(TerminalButton.of("list.back", EcosStyle.BOOK_INK + q.title(), q.detail()));
        if (q.payable())
            out.add(TerminalButton.of("adjust.pay", EcosStyle.BOOK_ORANGE + "确认缴费", "缴完再出站"));
        else
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "无需补票"));
        return out;
    }

    private static List<TerminalButton> ticket(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(TerminalButton.of("ticket.back", EcosStyle.BOOK_JIQING + "← 返回"));
        var gui = plugin.getTransitTicketGUI();
        TransitManager tm = plugin.getTransitManager();
        if (gui.pickingCabin(p)) {
            for (var c : tm.allCabins()) {
                double fare = tm.quoteTicketFare(gui.originOf(p), gui.destOf(p), c.id());
                out.add(TerminalButton.of("ticket.cabin",
                        EcosStyle.BOOK_COBALT + c.displayName()
                                + (fare >= 0 ? "  " + EcosStyle.BOOK_ORANGE + String.format("%.0f", fare) : ""),
                        "购买此席别", c.id()));
            }
            return out;
        }
        String from = gui.originOf(p);
        for (TransitManager.Station st : tm.allStations()) {
            if (from != null && st.id().equals(from)) continue;
            String tip = from == null ? "选起点" : "选终点";
            if (from != null) {
                double fare = tm.quoteTicketFare(from, st.id(), TransitManager.Cabin.STD);
                tip = fare >= 0 ? "票价 " + String.format("%.0f", fare) : "无法直达";
            }
            out.add(TerminalButton.of("ticket.pick", EcosStyle.BOOK_INK + "▸ " + st.displayName(), tip, st.id()));
        }
        return out;
    }

    private static List<TerminalButton> office(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        TransitManager tm = plugin.getTransitManager();
        var acc = tm.getAccount(p.getUniqueId());
        out.add(TerminalButton.of("office.tappay",
                acc.tapPay() ? EcosStyle.BOOK_JIQING + "闪付已开" : EcosStyle.BOOK_ORANGE + "开通闪付"));
        out.add(TerminalButton.of("office.card", EcosStyle.BOOK_COBALT + "领卡 / 补办"));
        out.add(TerminalButton.of("office.lost", EcosStyle.BOOK_MUTED + "挂失"));
        out.add(TerminalButton.of("office.ticket", EcosStyle.BOOK_INK + "买单程"));
        out.add(TerminalButton.of("office.map", EcosStyle.BOOK_COBALT + "线路图"));
        out.add(TerminalButton.of("office.history", EcosStyle.BOOK_MUTED + "记录"));
        out.add(TerminalButton.of("office.board", EcosStyle.BOOK_ORANGE + "乘车榜"));
        out.add(TerminalButton.of("office.claim", EcosStyle.BOOK_MUTED + "行程异常申报"));
        if (p.hasPermission("es2uni.admin"))
            out.add(TerminalButton.of("office.admin", EcosStyle.BOOK_ORANGE + "管理"));
        return out;
    }

    private static List<TerminalButton> map(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        for (TransitManager.Station st : plugin.getTransitManager().allStations()) {
            out.add(TerminalButton.of("map.st", EcosStyle.BOOK_INK + "▸ " + st.displayName(), st.id(), st.id()));
        }
        return out;
    }

    private static List<TerminalButton> history(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var list = plugin.getTransitManager().ridesOf(p.getUniqueId());
        if (list == null || list.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "没有记录"));
        else {
            int n = 0;
            for (var row : list) {
                if (n++ > 10) break;
                out.add(TerminalButton.of("list.back",
                        EcosStyle.BOOK_MUTED + row.fromId() + " → " + row.toId()));
            }
        }
        return out;
    }

    private static List<TerminalButton> board(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        int rank = 0;
        for (var row : plugin.getTransitManager().topRiders(15)) {
            rank++;
            out.add(TerminalButton.of("list.back",
                    EcosStyle.BOOK_COBALT + "#" + rank + " " + row.name(),
                    row.rides() + " 次  " + money(plugin, row.fare())));
        }
        if (rank == 0) out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "还没有乘车记录"));
        return out;
    }

    private static List<TerminalButton> hotels(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        if (plugin.getHotelManager().stayOf(p.getUniqueId()) != null)
            out.add(TerminalButton.of("hotel.out", EcosStyle.BOOK_ORANGE + "退房"));
        if (plugin.getHotelListGUI().isMineView(p))
            out.add(TerminalButton.of("hotel.all", EcosStyle.BOOK_MUTED + "全部酒店"));
        else
            out.add(TerminalButton.of("hotel.mine", EcosStyle.BOOK_JIQING + "我管理的"));
        out.add(TerminalButton.of("hotel.create", EcosStyle.BOOK_MUTED + "创建"));
        if (plugin.getHotelManager().stayOf(p.getUniqueId()) != null)
            out.add(TerminalButton.of("hotel.card", EcosStyle.BOOK_COBALT + "补领房卡"));
        List<HotelManager.Hotel> list = plugin.getHotelListGUI().isMineView(p)
                ? plugin.getHotelManager().ownedOrManaged(p.getUniqueId())
                : new ArrayList<>(plugin.getHotelManager().all());
        for (HotelManager.Hotel h : list) {
            int vacant = 0;
            for (var r : h.rooms) if (r.vacant()) vacant++;
            out.add(TerminalButton.of("hotel.open", EcosStyle.BOOK_COBALT + "▸ " + h.name,
                    h.rooms.size() + " 间  空 " + vacant, h.id));
        }
        if (list.isEmpty())
            out.add(TerminalButton.of("hotel.create", EcosStyle.BOOK_MUTED + "还没有酒店"));
        return out;
    }

    private static List<TerminalButton> hotelDesk(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var hotel = plugin.getHotelDeskGUI().hotelOf(p);
        if (hotel == null) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "未打开酒店"));
            return out;
        }
        boolean staff = plugin.getHotelManager().staff(p, hotel);
        for (int i = 0; i < hotel.rooms.size(); i++) {
            var r = hotel.rooms.get(i);
            String label = r.vacant()
                    ? EcosStyle.BOOK_JIQING + "▸ 空房 " + r.type.label
                    : (r.guest != null && r.guest.equals(p.getUniqueId())
                    ? EcosStyle.BOOK_ORANGE + "▸ 我的房 退房"
                    : EcosStyle.BOOK_MUTED + "▸ 有人 " + (r.guestName == null ? "" : r.guestName));
            out.add(TerminalButton.of("room.go", label,
                    plugin.getHotelManager().money(r.price) + (r.locked ? " · 锁" : ""),
                    String.valueOf(i)));
            if (staff) {
                out.add(TerminalButton.of("room.lock",
                        r.locked ? EcosStyle.BOOK_MUTED + "开锁" : EcosStyle.BOOK_ORANGE + "上锁",
                        r.unitId, r.unitId));
                out.add(TerminalButton.of("room.price", EcosStyle.BOOK_ORANGE + "改价",
                        plugin.getHotelManager().money(r.price), r.unitId));
                out.add(TerminalButton.of("room.type", EcosStyle.BOOK_MUTED + r.type.label,
                        "切换房型", r.unitId));
            }
        }
        if (hotel.rooms.isEmpty())
            out.add(TerminalButton.of(staff ? "hotel.bind" : "list.back",
                    EcosStyle.BOOK_MUTED + "还没绑房间"));
        if (staff) {
            out.add(TerminalButton.of("hotel.bind", EcosStyle.BOOK_JIQING + "绑当前房", "站在房产里"));
            out.add(TerminalButton.of("hotel.mgr", EcosStyle.BOOK_ORANGE + "管理者", "聊天 add/remove"));
            if (plugin.getHotelManager().owner(p, hotel)) {
                out.add(TerminalButton.of("hotel.xfer", EcosStyle.BOOK_MUTED + "转让"));
                out.add(TerminalButton.of("hotel.del", EcosStyle.BOOK_ORANGE + "删除酒店"));
            }
        }
        return out;
    }

    private static List<TerminalButton> playerSelect(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var ctx = plugin.getPlayerSelectGUI().getContext(p);
        if (ctx == PlayerSelectGUI.SelectContext.PAY)
            out.add(TerminalButton.of("sel.all", EcosStyle.BOOK_ORANGE + "全体在线"));
        if (ctx == PlayerSelectGUI.SelectContext.MAIL)
            out.add(TerminalButton.of("mail.name", EcosStyle.BOOK_ORANGE + "输入名字", "离线也可写"));
        int n = 0;
        for (Player t : Bukkit.getOnlinePlayers()) {
            if (t.equals(p)) continue;
            out.add(TerminalButton.of("sel.go", EcosStyle.BOOK_COBALT + "▸ " + t.getName(),
                    t.getPing() + "ms", t.getName()));
            n++;
        }
        if (n == 0) out.add(TerminalButton.of(ctx == PlayerSelectGUI.SelectContext.MAIL
                ? "mail.name" : "list.back",
                EcosStyle.BOOK_MUTED + (ctx == PlayerSelectGUI.SelectContext.MAIL
                        ? "没有在线 · 输入名字" : "没有其他人")));
        return out;
    }

    private static List<TerminalButton> playerAction(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        UUID id = plugin.getPlayerActionGUI().getTarget(p);
        if (id == null) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "未选玩家"));
            return out;
        }
        var off = Bukkit.getOfflinePlayer(id);
        String name = off.getName() == null ? "?" : off.getName();
        Player t = off.getPlayer();
        if (t != null) {
            out.add(TerminalButton.of("act.whisper", EcosStyle.BOOK_INK + "私信 " + name));
        } else {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + name + " 离线"));
        }
        out.add(TerminalButton.of("act.mail", EcosStyle.BOOK_COBALT + "写信",
                t != null ? "书与笔留言" : "离线也能写"));
        out.add(TerminalButton.of("act.friend",
                plugin.getFriendManager().areFriends(p.getUniqueId(), id)
                        ? EcosStyle.BOOK_ORANGE + "删好友" : EcosStyle.BOOK_JIQING + "加好友"));
        if (plugin.getVaultHook().isEnabled())
            out.add(TerminalButton.of("act.pay", EcosStyle.BOOK_ORANGE + "转账"));
        if (plugin.getReportManager().isEnabled())
            out.add(TerminalButton.of("act.report", EcosStyle.BOOK_ORANGE + "悄悄话", "仅管理员可见"));
        return out;
    }

    private static List<TerminalButton> payConfirm(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        var pay = plugin.getPayConfirmGUI().getPending(p);
        out.add(TerminalButton.of("pay.no", EcosStyle.BOOK_MUTED + "取消"));
        if (pay == null) return out;
        String who = pay.all() ? "全体在线" : String.valueOf(pay.targetUuid());
        if (!pay.all()) {
            var off = Bukkit.getOfflinePlayer(pay.targetUuid());
            if (off.getName() != null) who = off.getName();
        }
        out.add(TerminalButton.of("pay.ok",
                EcosStyle.BOOK_ORANGE + "确认 " + plugin.getVaultHook().format(pay.amount()) + " → " + who));
        return out;
    }

    private static List<TerminalButton> checkin(ES2UniPlugin plugin, Player p, TerminalSession s) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        boolean done = plugin.getCheckInManager().hasCheckedInToday(p.getUniqueId());
        int streak = plugin.getCheckInManager().getStreak(p.getUniqueId());
        int mk = plugin.getCheckInManager().getMakeupTickets(p.getUniqueId());
        var ym = checkinMonth(s);
        var today = com.etherstories.escore.managers.CheckInManager.today();
        var checked = plugin.getCheckInManager().getCheckedDaysInMonth(p.getUniqueId(), ym);
        out.add(TerminalButton.of("ck.do",
                done ? EcosStyle.BOOK_JIQING + "今日已签 · 连" + streak
                        : EcosStyle.BOOK_ORANGE + "签到 · 连" + streak,
                done ? "已领取" : "点此签到"));
        out.add(TerminalButton.of("ck.prev", EcosStyle.BOOK_MUTED + "上月"));
        out.add(TerminalButton.of("list.back", EcosStyle.BOOK_INK + ym.toString(), "券 " + mk));
        out.add(TerminalButton.of("ck.next", EcosStyle.BOOK_MUTED + "下月"));
        int days = ym.lengthOfMonth();
        for (int d = 1; d <= days; d++) {
            var date = ym.atDay(d);
            boolean ok = checked.contains(d);
            boolean future = date.isAfter(today);
            boolean isToday = date.equals(today);
            if (future) {
                out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + String.valueOf(d)));
            } else if (ok) {
                out.add(TerminalButton.of("list.back",
                        EcosStyle.BOOK_JIQING + d + (isToday ? " 今" : "")));
            } else if (isToday) {
                out.add(TerminalButton.of("ck.do", EcosStyle.BOOK_ORANGE + d + " 签"));
            } else {
                out.add(TerminalButton.of(mk > 0 ? "ck.makeup" : "list.back",
                        EcosStyle.BOOK_MUTED + d + " 缺",
                        mk > 0 ? "补签券 " + mk : "没有补签券",
                        date.toString()));
            }
        }
        return out;
    }

    private static List<TerminalButton> municipal(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("muni.jobs", EcosStyle.BOOK_ORANGE + "招工"));
        out.add(TerminalButton.of("muni.clean", EcosStyle.BOOK_MUTED + "清理"));
        out.add(TerminalButton.of("muni.recycle", EcosStyle.BOOK_JIQING + "回收站"));
        out.add(TerminalButton.of("muni.ins", EcosStyle.BOOK_COBALT + "保险"));
        out.add(TerminalButton.of("muni.transit", EcosStyle.BOOK_COBALT + "交通"));
        if (p.hasPermission("es2uni.admin"))
            out.add(TerminalButton.of("muni.pve", EcosStyle.BOOK_ORANGE + "PVE 棒", "刷怪 / 精英"));
        return out;
    }

    private static List<TerminalButton> kits(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        if (plugin.getEssentialsHook().isEnabled()) {
            for (String name : plugin.getEssentialsHook().getKitNames())
                out.add(TerminalButton.of("kit.go", EcosStyle.BOOK_COBALT + "▸ " + name, "Essentials", "ess:" + name));
        }
        for (String name : plugin.getKitManager().names())
            out.add(TerminalButton.of("kit.go", EcosStyle.BOOK_ORANGE + "▸ " + name, "ECOS", "local:" + name));
        if (out.size() == 1)
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "没有 Kit"));
        return out;
    }

    private static List<TerminalButton> auras(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("aura.off", EcosStyle.BOOK_MUTED + "卸下"));
        var br = plugin.getAuraManager().getBrightness(p.getUniqueId());
        out.add(TerminalButton.of("aura.br", EcosStyle.BOOK_ORANGE + "亮度 " + br.label, "低/中/高"));
        var eq = plugin.getAuraManager().getEquipped(p.getUniqueId());
        for (AuraType t : AuraType.values()) {
            boolean own = plugin.getAuraManager().owns(p.getUniqueId(), t);
            boolean on = eq == t;
            String tip = own
                    ? (on ? "装备中 · 再点卸下" : "装备")
                    : "购买 " + money(plugin, t.price);
            out.add(TerminalButton.of("aura.go",
                    (on ? EcosStyle.BOOK_JIQING : own ? EcosStyle.BOOK_COBALT : EcosStyle.BOOK_MUTED)
                            + "▸ " + t.displayName(),
                    tip, t.name()));
        }
        return out;
    }

    private static List<TerminalButton> music(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("music.search", EcosStyle.BOOK_COBALT + "搜歌", "聊天输入歌名"));
        out.add(TerminalButton.of("music.id", EcosStyle.BOOK_JIQING + "ID 点歌", "聊天输入歌曲 ID"));
        out.add(TerminalButton.of("music.stop", EcosStyle.BOOK_ORANGE + "停止"));
        out.add(TerminalButton.of("music.vote", EcosStyle.BOOK_ORANGE + "投票切歌"));
        out.add(TerminalButton.of("music.queue", EcosStyle.BOOK_MUTED + "队列"));
        boolean muted = plugin.getAllMusicHook().isMuted(p);
        out.add(TerminalButton.of("music.listen",
                muted ? EcosStyle.BOOK_ORANGE + "听歌关" : EcosStyle.BOOK_JIQING + "听歌开"));
        out.add(TerminalButton.of("music.hist", EcosStyle.BOOK_MUTED + "点歌历史",
                plugin.getMusicHistoryManager().size() + " 条"));
        out.add(TerminalButton.of("music.fav", EcosStyle.BOOK_JIQING + "收藏",
                plugin.getMusicFavoritesManager().size(p.getUniqueId()) + " 首"));
        if (p.hasPermission("es2uni.admin"))
            out.add(TerminalButton.of("music.plist", EcosStyle.BOOK_ORANGE + "预设歌单",
                    plugin.getMusicPlaylistManager().statusLine()));
        return out;
    }

    private static List<TerminalButton> skillshop(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        if (p.hasPermission("es2uni.admin"))
            out.add(TerminalButton.of("skill.admin", EcosStyle.BOOK_ORANGE + "上架管理"));
        var shop = plugin.getSkillShopManager();
        if (shop == null || shop.forSale().isEmpty()) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "暂无在售技能"));
            return out;
        }
        for (var l : shop.forSale()) {
            out.add(TerminalButton.of("skill.buy",
                    EcosStyle.BOOK_COBALT + "▸ " + l.skill().chineseName,
                    l.skill().englishName + "  " + money(plugin, l.price()),
                    l.skill().configKey));
        }
        return out;
    }

    private static List<TerminalButton> estate(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("estate.sale", EcosStyle.BOOK_ORANGE + "待售"));
        out.add(TerminalButton.of("estate.mine", EcosStyle.BOOK_JIQING + "我的"));
        out.add(TerminalButton.of("estate.tools", EcosStyle.BOOK_MUTED + "工具"));
        for (String name : plugin.getEstateManager().buildings(null)) {
            int vacant = plugin.getEstateManager().vacantCount(name);
            out.add(TerminalButton.of("estate.go", EcosStyle.BOOK_COBALT + "▸ " + name,
                    "空闲 " + vacant, name));
        }
        return out;
    }

    private static List<TerminalButton> estateRooms(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        String b = plugin.getEstateRoomsGUI().buildingOf(p);
        if (b == null) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "未选楼"));
            return out;
        }
        var rooms = plugin.getEstateManager().inBuilding(b);
        for (var u : rooms) {
            boolean buy = u.vacant() && u.listed() && u.price() > 0;
            out.add(TerminalButton.of(buy ? "estate.buy" : "estate.tp",
                    (u.vacant() ? EcosStyle.BOOK_JIQING : EcosStyle.BOOK_MUTED)
                            + "▸ " + u.floor() + "-" + u.room() + "  " + u.address(),
                    buy ? "点两次确认 " + money(plugin, u.price())
                            : (u.vacant() ? "空闲" : (u.ownerName() == null ? "有人" : u.ownerName())),
                    u.id()));
        }
        return out;
    }

    private static List<TerminalButton> estateSale(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        for (var u : plugin.getEstateManager().listedForSale()) {
            out.add(TerminalButton.of("estate.buy", EcosStyle.BOOK_ORANGE + "▸ " + u.address(),
                    "点两次确认 " + money(plugin, u.price()), u.id()));
        }
        if (out.size() == 1)
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "没有待售"));
        return out;
    }

    private static List<TerminalButton> estateTools(ES2UniPlugin plugin, Player p) {
        return List.of(
                back(),
                TerminalButton.of("estate.wand", EcosStyle.BOOK_COBALT + "选区棒", "左键一角 右键对角"),
                TerminalButton.of("estate.reg", EcosStyle.BOOK_ORANGE + "登记房间", "圈好后聊天输入门牌"),
                TerminalButton.of("estate.door", EcosStyle.BOOK_JIQING + "绑门", "看向房门"),
                TerminalButton.of("estate.sign", EcosStyle.BOOK_MUTED + "绑牌", "看向木牌"),
                TerminalButton.of("estate.help", EcosStyle.BOOK_INK + "说明")
        );
    }

    private static List<TerminalButton> estateMine(ES2UniPlugin plugin, Player p, boolean admin) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var rooms = admin ? plugin.getEstateManager().all()
                : plugin.getEstateManager().ownedBy(p.getUniqueId());
        for (var u : rooms) {
            out.add(TerminalButton.of("estate.tp",
                    EcosStyle.BOOK_INK + "▸ " + u.address(),
                    (u.vacant() ? "空闲" : (u.ownerName() == null ? "" : u.ownerName()))
                            + (u.listed() ? " · 挂牌 " + money(plugin, u.price()) : ""),
                    u.id()));
            boolean mine = admin || p.getUniqueId().equals(u.owner());
            if (mine) {
                out.add(TerminalButton.of("estate.list",
                        u.listed() ? EcosStyle.BOOK_MUTED + "下架" : EcosStyle.BOOK_ORANGE + "上架",
                        u.listed() ? money(plugin, u.price()) : "没标价会问金额", u.id()));
                out.add(TerminalButton.of("estate.price", EcosStyle.BOOK_MUTED + "改价",
                        money(plugin, u.price()), u.id()));
                out.add(TerminalButton.of("estate.del", EcosStyle.BOOK_ORANGE + "删 " + u.address(),
                        "再点确认", u.id()));
            }
        }
        if (out.size() == 1)
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + (admin ? "还没有房间" : "你没有房产")));
        return out;
    }

    private static List<TerminalButton> events(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        for (var e : plugin.getEventManager().getAllEvents()) {
            boolean in = e.hasJoined(p.getUniqueId());
            out.add(TerminalButton.of("ev.join",
                    (in ? EcosStyle.BOOK_JIQING : EcosStyle.BOOK_ORANGE) + "▸ " + e.getName(),
                    (in ? "已参加 · 再点退出" : "参加") + " · " + e.getParticipantDisplay(),
                    e.getId()));
        }
        if (out.size() == 1)
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "没有活动"));
        return out;
    }

    private static List<TerminalButton> leaderboard(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        int rank = 0;
        for (var e : plugin.getPlaytimeManager().getTopPlayers(15)) {
            rank++;
            var off = Bukkit.getOfflinePlayer(e.getKey());
            String name = off.getName() == null ? "?" : off.getName();
            out.add(TerminalButton.of("list.back",
                    EcosStyle.BOOK_COBALT + "#" + rank + " " + name,
                    com.etherstories.escore.managers.PlaytimeManager.formatMillis(e.getValue())));
        }
        if (rank == 0) out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "还没有时长"));
        return out;
    }

    private static List<TerminalButton> newbie(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var ng = plugin.getNewbieGuideManager();
        out.add(TerminalButton.of("list.back",
                EcosStyle.BOOK_JIQING + "进度 " + ng.progress(p.getUniqueId()) + "%",
                "点条目勾选/取消"));
        for (var step : com.etherstories.escore.managers.NewbieGuideManager.Step.values()) {
            boolean done = ng.isDone(p.getUniqueId(), step);
            boolean optional = step == com.etherstories.escore.managers.NewbieGuideManager.Step.ADD_FRIEND
                    || step == com.etherstories.escore.managers.NewbieGuideManager.Step.SET_HOME;
            out.add(TerminalButton.of("nb.tog",
                    (done ? EcosStyle.BOOK_JIQING + "✔ " : EcosStyle.BOOK_ORANGE + "○ ")
                            + step.title + (optional ? " (可选)" : ""),
                    step.tip, step.name()));
        }
        return out;
    }

    private static List<TerminalButton> jobs(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("job.post", EcosStyle.BOOK_JIQING + "发布委托", "聊天输入标题"));
        if (p.hasPermission("es2uni.admin"))
            out.add(TerminalButton.of("job.post", EcosStyle.BOOK_ORANGE + "官方委托", "官方发布", "1"));
        out.add(TerminalButton.of("job.hist", EcosStyle.BOOK_MUTED + "历史记录"));
        var list = plugin.getJobBoardManager().listActive();
        if (list.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "暂无委托"));
        boolean admin = p.hasPermission("es2uni.admin");
        var hub = plugin.getTerminalHub();
        var session = hub == null ? null : hub.session(p);
        String armed = session == null ? null : session.get("job-force");
        for (var j : list) {
            boolean mine = j.poster.equals(p.getUniqueId()) || admin;
            boolean worker = j.workers.contains(p.getUniqueId());
            String tip = (j.official ? "官方 · " : "") + money(plugin, j.reward)
                    + " · " + j.slotsLabel() + " · " + j.posterName
                    + (j.locked ? " · 已锁定" : "")
                    + (j.hasWorkers() ? " · " + j.workersLabel() : "");
            String mod = j.adminModifiedLabel();
            if (mod != null) tip = tip + " · " + mod;
            if (j.hasRoom() && !worker && !mine)
                out.add(TerminalButton.of("job.take", EcosStyle.BOOK_INK + "▸ " + j.title, tip, j.id));
            else
                out.add(TerminalButton.of("list.back",
                        (mine ? EcosStyle.BOOK_COBALT : EcosStyle.BOOK_MUTED) + "▸ " + j.title,
                        tip + (worker ? " · 已接" : "")));
            if (mine && j.hasWorkers())
                out.add(TerminalButton.of("job.done", EcosStyle.BOOK_ORANGE + "完成 " + j.title, "发奖励", j.id));
            if (mine && (!j.hasWorkers() || admin))
                out.add(TerminalButton.of("job.cancel", EcosStyle.BOOK_MUTED + "取消 " + j.title, "退押金", j.id));
            if (mine)
                out.add(TerminalButton.of("job.lock",
                        j.locked ? EcosStyle.BOOK_JIQING + "解锁 " + j.title
                                : EcosStyle.BOOK_ORANGE + "锁定 " + j.title,
                        j.locked ? "再开放加人" : "不再加人", j.id));
            if (mine && j.hasWorkers()) {
                for (int i = 0; i < j.workers.size(); i++) {
                    String wn = i < j.workerNames.size() ? j.workerNames.get(i) : "?";
                    out.add(TerminalButton.of("job.kick",
                            EcosStyle.BOOK_MUTED + "踢 " + wn,
                            "移出 " + j.title, j.id + "|" + wn));
                }
            }
            if (admin) {
                out.add(TerminalButton.of("job.edit", EcosStyle.BOOK_ORANGE + "改标题 " + j.title,
                        "管理员改标题", j.id));
                boolean confirm = rawEquals(armed, j.id);
                out.add(TerminalButton.of("job.force",
                        confirm ? EcosStyle.BOOK_ORANGE + "再点确认删 #" + j.id
                                : EcosStyle.BOOK_MUTED + "强制删 #" + j.id,
                        "退款并删除", j.id));
            }
        }
        return out;
    }

    private static boolean rawEquals(String a, String b) {
        return a != null && a.equals(b);
    }

    private static List<TerminalButton> insurance(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var im = plugin.getInsuranceManager();
        var hist = im.getHistory(p.getUniqueId());
        var bak = im.getBackup(p.getUniqueId());
        var death = im.lastDeath(p.getUniqueId());
        String cover = death == null ? "最近死亡未记录"
                : (death.covered() ? "保障内 · " + death.label() : death.label());
        out.add(TerminalButton.of("list.back",
                EcosStyle.BOOK_INK + (bak == null ? "尚无备份" : "最新 " + bak.timeLabel()),
                cover + " · 历史 " + hist.size() + " 份"));
        out.add(TerminalButton.of("ins.bak", EcosStyle.BOOK_JIQING + "立即备份", "写入当前背包"));
        out.add(TerminalButton.of("ins.claim", EcosStyle.BOOK_ORANGE + "申请理赔",
                "通过后恢复 + " + (int) im.compensation()));
        if (p.hasPermission("es2uni.admin")) {
            var pending = im.pendingClaims();
            if (pending.isEmpty())
                out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "无待审理赔"));
            for (var c : pending) {
                String place = c.coverPlace == null || c.coverPlace.isBlank() ? "未记录" : c.coverPlace;
                out.add(TerminalButton.of("ins.open",
                        (c.covered ? EcosStyle.BOOK_JIQING : EcosStyle.BOOK_ORANGE)
                                + "▸ #" + c.id + " " + c.playerName,
                        (c.covered ? "范围内 · " : "范围外 · ") + place, c.id));
                out.add(TerminalButton.of("ins.no", EcosStyle.BOOK_MUTED + "拒 #" + c.id, "拒绝理赔", c.id));
            }
        }
        return out;
    }

    private static List<TerminalButton> recycle(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var list = plugin.getRecycleBinManager().listFor(p);
        if (list.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "回收站是空的"));
        for (var e : list) {
            String name = e.stack.getType().name();
            if (e.stack.hasItemMeta() && e.stack.getItemMeta().hasDisplayName())
                name = e.stack.getItemMeta().getDisplayName();
            out.add(TerminalButton.of("rec.take",
                    EcosStyle.BOOK_COBALT + "▸ " + name + " ×" + e.stack.getAmount(),
                    e.expireLabel() + " · " + e.storedLabel(), e.id));
        }
        return out;
    }

    private static List<TerminalButton> trade(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        int rank = 0;
        for (var e : plugin.getTradeStatsManager().getTopPlayers(15)) {
            rank++;
            out.add(TerminalButton.of("list.back",
                    EcosStyle.BOOK_ORANGE + "#" + rank + " " + e.name(),
                    money(plugin, e.volume())));
        }
        if (rank == 0) out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "今日还没有成交"));
        return out;
    }

    private static List<TerminalButton> showcase(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("sc.reg", EcosStyle.BOOK_JIQING + "登记脚下", "聊天输入标题"));
        var list = plugin.getShowcaseManager().topByVotes(30);
        if (list.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "还没有展示点"));
        for (var sc : list) {
            boolean own = sc.owner().equals(p.getUniqueId());
            out.add(TerminalButton.of("sc.tp",
                    EcosStyle.BOOK_COBALT + "▸ " + sc.title(),
                    sc.ownerName() + " · " + sc.votes() + " 票", sc.id()));
            out.add(TerminalButton.of("sc.vote",
                    EcosStyle.BOOK_ORANGE + "投 " + sc.title(), "每日每点一票", sc.id()));
            if (own || p.hasPermission("es2uni.admin"))
                out.add(TerminalButton.of("sc.del", EcosStyle.BOOK_MUTED + "删 " + sc.title(), "删除", sc.id()));
        }
        return out;
    }

    private static List<TerminalButton> territory(ES2UniPlugin plugin, Player p, boolean pub) {
        return territory(plugin, p, pub, false);
    }

    private static List<TerminalButton> territory(ES2UniPlugin plugin, Player p, boolean pub, boolean admin) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        if (admin || !pub) {
            String sel = plugin.getRegionManager().getSelectionInfo(p.getUniqueId());
            out.add(TerminalButton.of("reg.pos1", EcosStyle.BOOK_JIQING + "角点1", "记在脚下"));
            out.add(TerminalButton.of("reg.pos2", EcosStyle.BOOK_ORANGE + "角点2", "记在脚下"));
            out.add(TerminalButton.of("terr.create", EcosStyle.BOOK_COBALT + "创建领地",
                    sel == null ? "先点两个角点" : sel));
        }
        var list = admin ? plugin.getRegionManager().getAllTerritories()
                : (pub ? plugin.getRegionManager().getPublicTerritories()
                : plugin.getRegionManager().getByOwner(p.getUniqueId()));
        if (list.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + (pub ? "没有公开领地" : "你没有领地")));
        for (var r : list) {
            boolean owner = r.owner() != null && r.owner().equals(p.getUniqueId());
            out.add(TerminalButton.of("terr.tp",
                    (r.visible() ? EcosStyle.BOOK_INK : EcosStyle.BOOK_MUTED) + "▸ " + r.name(),
                    r.world() + (r.visible() ? " · 公开" : " · 隐藏"), r.name()));
            if (admin || owner)
                out.add(TerminalButton.of("terr.spawn", EcosStyle.BOOK_MUTED + "设出生 " + r.name(),
                        "脚下", r.name()));
            if (admin || owner)
                out.add(TerminalButton.of("terr.vis",
                        r.visible() ? EcosStyle.BOOK_MUTED + "隐藏 " + r.name()
                                : EcosStyle.BOOK_ORANGE + "公开 " + r.name(),
                        "切换可见", r.name()));
            if (admin)
                out.add(TerminalButton.of("reg.del", EcosStyle.BOOK_ORANGE + "删 " + r.name(),
                        "删除", r.name()));
        }
        return out;
    }

    private static List<TerminalButton> transitAdmin(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("tad.types", EcosStyle.BOOK_ORANGE + "车型"));
        out.add(TerminalButton.of("tad.announce", EcosStyle.BOOK_JIQING + "报站铃"));
        out.add(TerminalButton.of("tad.claims", EcosStyle.BOOK_MUTED + "申诉"));
        out.add(TerminalButton.of("tad.riders", EcosStyle.BOOK_COBALT + "在乘"));
        TransitManager tm = plugin.getTransitManager();
        for (TransitManager.Line l : tm.allLines()) {
            int n = tm.edgesOfLine(l.id()).size();
            out.add(TerminalButton.of("tad.line",
                    EcosStyle.BOOK_ORANGE + "线 " + l.displayName(),
                    l.id() + " · " + n + " 段", l.id()));
        }
        List<TransitManager.Station> all = new ArrayList<>(tm.allStations());
        if (all.isEmpty() && tm.allLines().isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "还没有车站"));
        for (TransitManager.Station st : all) {
            List<String> nb = tm.neighborLabels(st.id(), null);
            out.add(TerminalButton.of("tad.st",
                    EcosStyle.BOOK_COBALT + "▸ " + st.displayName(),
                    st.id() + (nb.isEmpty() ? " · 无连接" : " · " + String.join(", ", nb)),
                    st.id()));
        }
        return out;
    }

    private static List<String> taxHeader(ES2UniPlugin plugin, Player p) {
        var stats = plugin.getTradeStatsManager();
        TaxManager tax = plugin.getTaxManager();
        return List.of(
                EcosStyle.BOOK_INK + "今日税 " + money(plugin, stats.getTaxCollected()),
                EcosStyle.BOOK_MUTED + "成交 " + money(plugin, stats.getTotalVolume()),
                EcosStyle.BOOK_MUTED + "sink " + (tax.getSinkAccount().isBlank() ? "销毁" : tax.getSinkAccount())
        );
    }

    private static List<TerminalButton> adminTax(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        TaxManager tax = plugin.getTaxManager();
        out.add(TerminalButton.of("tax.pay.toggle",
                tax.isPayTaxEnabled() ? EcosStyle.BOOK_JIQING + "转账税开" : EcosStyle.BOOK_MUTED + "转账税关",
                "当前 " + tax.formatRate(tax.getPayTaxRate())));
        out.add(TerminalButton.of("tax.pay.down", EcosStyle.BOOK_ORANGE + "转账 -1%"));
        out.add(TerminalButton.of("tax.pay.up", EcosStyle.BOOK_JIQING + "转账 +1%"));
        out.add(TerminalButton.of("tax.qs.toggle",
                tax.isQuickShopTaxEnabled() ? EcosStyle.BOOK_JIQING + "商店税开" : EcosStyle.BOOK_MUTED + "商店税关",
                "当前 " + tax.formatRate(tax.getQuickShopTaxRate())));
        out.add(TerminalButton.of("tax.qs.down", EcosStyle.BOOK_ORANGE + "商店 -1%"));
        out.add(TerminalButton.of("tax.qs.up", EcosStyle.BOOK_JIQING + "商店 +1%"));
        var link = plugin.getESLinkHook();
        if (link.present()) {
            out.add(TerminalButton.of("tax.link.toggle",
                    link.tradeEnabled() ? EcosStyle.BOOK_JIQING + "互通开" : EcosStyle.BOOK_MUTED + "互通关",
                    "当前 " + link.formatRate()));
            out.add(TerminalButton.of("tax.link.down", EcosStyle.BOOK_ORANGE + "互通 -1%"));
            out.add(TerminalButton.of("tax.link.up", EcosStyle.BOOK_JIQING + "互通 +1%"));
        }
        return out;
    }

    private static List<TerminalButton> adminKit(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("kitadm.create", EcosStyle.BOOK_JIQING + "新建", "聊天输入套件 ID"));
        List<String> names = plugin.getKitManager().names();
        if (names.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "还没有套件"));
        for (String name : names) {
            KitManager.KitDef def = plugin.getKitManager().get(name);
            if (def == null) continue;
            out.add(TerminalButton.of("kitadm.edit",
                    EcosStyle.BOOK_COBALT + "▸ " + name,
                    plugin.getKitManager().describeRule(def) + " · " + def.items.size() + " 件",
                    name));
        }
        return out;
    }

    private static List<String> kitEditHeader(ES2UniPlugin plugin, Player p) {
        String name = plugin.getKitEditGUI().getEditing(p);
        KitManager.KitDef def = name == null ? null : plugin.getKitManager().get(name);
        if (def == null) return List.of(EcosStyle.BOOK_MUTED + "没有正在编辑的套件");
        return List.of(
                EcosStyle.BOOK_INK + def.name,
                EcosStyle.BOOK_MUTED + plugin.getKitManager().describeRule(def)
                        + " · " + def.items.size() + " 件"
        );
    }

    private static List<TerminalButton> kitEdit(ES2UniPlugin plugin, Player p, TerminalSession s) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(TerminalButton.of("kitedit.back", EcosStyle.BOOK_JIQING + "← 返回"));
        String name = plugin.getKitEditGUI().getEditing(p);
        if (name == null) {
            out.add(TerminalButton.of("kitedit.back", EcosStyle.BOOK_MUTED + "会话已失效"));
            return out;
        }
        KitManager.KitDef def = plugin.getKitManager().get(name);
        boolean once = def != null && def.claimMode == KitManager.ClaimMode.ONCE;
        out.add(TerminalButton.of("kitedit.mode",
                once ? EcosStyle.BOOK_ORANGE + "每人一次" : EcosStyle.BOOK_JIQING + "冷却重复",
                "点击切换"));
        out.add(TerminalButton.of("kitedit.cd", EcosStyle.BOOK_COBALT + "设冷却", "聊天输入秒数"));
        out.add(TerminalButton.of("kitedit.import", EcosStyle.BOOK_JIQING + "导入背包",
                "用当前背包覆盖套件物品"));
        boolean armed = "1".equals(s.get("kit-armed"));
        out.add(TerminalButton.of("kitedit.del",
                armed ? EcosStyle.BOOK_ORANGE + "再点确认删" : EcosStyle.BOOK_MUTED + "删除套件"));
        return out;
    }

    private static List<String> stationHeader(ES2UniPlugin plugin, Player p) {
        String sid = plugin.getTransitStationGUI().stationOf(p);
        TransitManager tm = plugin.getTransitManager();
        TransitManager.Station s = sid == null ? null : tm.getStation(sid);
        if (s == null) return List.of(EcosStyle.BOOK_MUTED + "未选车站");
        var c = s.center();
        String pos = c == null ? s.world() + " 未加载"
                : c.getBlockX() + " " + c.getBlockY() + " " + c.getBlockZ();
        String en = s.nameEn() == null || s.nameEn().isBlank() ? "未设" : s.nameEn();
        return List.of(
                EcosStyle.BOOK_INK + s.displayName() + "  &8" + s.id(),
                EcosStyle.BOOK_MUTED + "EN " + en,
                EcosStyle.BOOK_MUTED + tm.boxLabel(s),
                EcosStyle.BOOK_MUTED + pos
        );
    }

    private static List<TerminalButton> stationEdit(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(TerminalButton.of("st.back", EcosStyle.BOOK_JIQING + "← 返回"));
        String sid = plugin.getTransitStationGUI().stationOf(p);
        TransitManager tm = plugin.getTransitManager();
        TransitManager.Station s = sid == null ? null : tm.getStation(sid);
        if (s == null) {
            out.add(TerminalButton.of("st.back", EcosStyle.BOOK_MUTED + "车站不存在"));
            return out;
        }
        var p1 = tm.boxCorner(p, 1);
        var p2 = tm.boxCorner(p, 2);
        out.add(TerminalButton.of("st.pos1", EcosStyle.BOOK_JIQING + "角点1",
                p1 == null ? "记下脚下" : p1.getBlockX() + " " + p1.getBlockY() + " " + p1.getBlockZ()));
        out.add(TerminalButton.of("st.pos2", EcosStyle.BOOK_ORANGE + "角点2",
                p2 == null ? "记下对角" : p2.getBlockX() + " " + p2.getBlockY() + " " + p2.getBlockZ()));
        out.add(TerminalButton.of("st.apply", EcosStyle.BOOK_COBALT + "应用框选"));
        out.add(TerminalButton.of("st.move", EcosStyle.BOOK_MUTED + "移到脚下"));
        out.add(TerminalButton.of("st.tp", EcosStyle.BOOK_INK + "传送"));
        out.add(TerminalButton.of("st.zh", EcosStyle.BOOK_COBALT + "改中文名", "聊天输入"));
        out.add(TerminalButton.of("st.en", EcosStyle.BOOK_COBALT + "English", "type in chat"));
        boolean skip = tm.isSkipStop(sid);
        out.add(TerminalButton.of("st.skip",
                skip ? EcosStyle.BOOK_ORANGE + "通过不停车" : EcosStyle.BOOK_JIQING + "本站停车"));
        String mode = plugin.getTransitStationGUI().gateModeOf(p);
        out.add(TerminalButton.of("st.gatemode", EcosStyle.BOOK_MUTED + "闸机 " + mode));
        out.add(TerminalButton.of("st.gate", EcosStyle.BOOK_INK + "拿闸机牌"));
        out.add(TerminalButton.of("st.tvm", EcosStyle.BOOK_INK + "拿售票机"));
        out.add(TerminalButton.of("st.adjust", EcosStyle.BOOK_INK + "拿补票牌"));
        out.add(TerminalButton.of("st.bindtvm", EcosStyle.BOOK_ORANGE + "绑售票机", "合上书后右键方块"));
        boolean armed = plugin.getTransitStationGUI().isArmed(p);
        out.add(TerminalButton.of("st.del",
                armed ? EcosStyle.BOOK_ORANGE + "再点确认删" : EcosStyle.BOOK_MUTED + "删除车站"));
        for (TransitManager.Line l : tm.allLines()) {
            boolean on = s.lineIds().contains(l.id());
            out.add(TerminalButton.of("st.line",
                    (on ? EcosStyle.BOOK_JIQING : EcosStyle.BOOK_MUTED) + (on ? "● " : "○ ") + l.displayName(),
                    on ? "摘掉此线" : "挂上此线",
                    l.id()));
        }
        return out;
    }

    private static void taxPayRate(ES2UniPlugin plugin, Player p, double delta) {
        if (!p.hasPermission("es2uni.admin")) return;
        TaxManager tax = plugin.getTaxManager();
        tax.setPayRate(tax.getPayTaxRate() + delta);
        plugin.getAuditLogManager().log(p, "TAX_PAY_RATE", tax.formatRate(tax.getPayTaxRate()));
        plugin.getTerminalHub().refresh(p);
    }

    private static void taxQsRate(ES2UniPlugin plugin, Player p, double delta) {
        if (!p.hasPermission("es2uni.admin")) return;
        TaxManager tax = plugin.getTaxManager();
        tax.setQuickShopRate(tax.getQuickShopTaxRate() + delta);
        plugin.getAuditLogManager().log(p, "TAX_QS_RATE", tax.formatRate(tax.getQuickShopTaxRate()));
        plugin.getTerminalHub().refresh(p);
    }

    private static void taxLinkRate(ES2UniPlugin plugin, Player p, double delta) {
        if (!p.hasPermission("es2uni.admin")) return;
        var link = plugin.getESLinkHook();
        if (!link.present()) return;
        link.setTaxRate(link.taxRate() + delta);
        plugin.getAuditLogManager().log(p, "TAX_LINK_RATE", link.formatRate());
        plugin.getTerminalHub().refresh(p);
    }

    private static void stationFeet(ES2UniPlugin plugin, Player p, int corner) {
        String sid = plugin.getTransitStationGUI().stationOf(p);
        if (sid == null) return;
        plugin.getTransitManager().markBoxCorner(p, corner);
        p.sendMessage(ColorUtil.colorize(corner == 1
                ? "&8[交通] &a角点1已记在脚下。再去对角点开本页点角点2，然后点应用。"
                : "&8[交通] &6角点2已记在脚下。两点都有了就点「应用框选」。"));
        plugin.getTerminalHub().refresh(p);
    }

    private static List<TerminalButton> jobHistory(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var list = plugin.getJobBoardManager().listHistory();
        if (list.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "没有历史"));
        for (var j : list) {
            out.add(TerminalButton.of("list.back",
                    EcosStyle.BOOK_MUTED + "▸ " + j.title,
                    j.status.name() + " · " + j.completedLabel() + " · " + money(plugin, j.reward)));
        }
        return out;
    }

    private static List<TerminalButton> insuranceBak(ES2UniPlugin plugin, Player p, TerminalSession s) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        String claimId = s.get("ins-claim");
        if (claimId == null) claimId = plugin.getInsuranceBackupGUI().getClaimId(p);
        var c = claimId == null ? null : plugin.getInsuranceManager().getClaim(claimId);
        if (c == null) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "未选理赔"));
            return out;
        }
        out.add(TerminalButton.of("list.back",
                EcosStyle.BOOK_INK + c.playerName + " #" + c.id,
                "选一份备份恢复 · 赔偿 " + (int) plugin.getInsuranceManager().compensation()));
        out.add(TerminalButton.of("ins.no", EcosStyle.BOOK_ORANGE + "拒绝理赔", "驳回", c.id));
        var hist = plugin.getInsuranceManager().getHistory(c.player);
        if (hist.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "没有备份"));
        for (int i = hist.size() - 1; i >= 0; i--) {
            var b = hist.get(i);
            String sample = String.join(" · ", b.sampleNames(3));
            out.add(TerminalButton.of("ins.ok",
                    EcosStyle.BOOK_COBALT + "▸ 备份 #" + (i + 1) + " " + b.timeLabel(),
                    b.itemCount() + " 件" + (sample.isBlank() ? "" : " · " + sample),
                    String.valueOf(i)));
        }
        return out;
    }

    private static List<TerminalButton> districts(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        if (p.hasPermission("es2uni.admin")) {
            String sel = plugin.getRegionManager().getSelectionInfo(p.getUniqueId());
            out.add(TerminalButton.of("reg.pos1", EcosStyle.BOOK_JIQING + "角点1", "记在脚下"));
            out.add(TerminalButton.of("reg.pos2", EcosStyle.BOOK_ORANGE + "角点2", "记在脚下"));
            out.add(TerminalButton.of("reg.create", EcosStyle.BOOK_COBALT + "创建地区",
                    sel == null ? "先点两个角点" : sel));
        }
        var list = plugin.getRegionManager().getDistricts();
        if (list.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "没有地区"));
        for (var r : list) {
            out.add(TerminalButton.of("terr.tp",
                    EcosStyle.BOOK_INK + "▸ " + r.name(),
                    r.world() + (r.visible() ? " · 公开" : " · 隐藏"), r.name()));
            if (p.hasPermission("es2uni.admin")) {
                out.add(TerminalButton.of("terr.vis",
                        r.visible() ? EcosStyle.BOOK_MUTED + "隐藏 " + r.name()
                                : EcosStyle.BOOK_ORANGE + "公开 " + r.name(),
                        "切换可见", r.name()));
                out.add(TerminalButton.of("reg.del", EcosStyle.BOOK_ORANGE + "删 " + r.name(),
                        "删除", r.name()));
            }
        }
        return out;
    }

    private static List<TerminalButton> skillshopAdmin(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var shop = plugin.getSkillShopManager();
        if (shop == null) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "技能店未启用"));
            return out;
        }
        for (var t : com.etherstories.escore.weapons.SkillType.values()) {
            var l = shop.of(t);
            out.add(TerminalButton.of("skill.toggle",
                    (l.enabled() ? EcosStyle.BOOK_JIQING : EcosStyle.BOOK_MUTED)
                            + (l.enabled() ? "在售 " : "下架 ") + t.chineseName,
                    t.englishName + "  " + money(plugin, l.price()), t.configKey));
            out.add(TerminalButton.of("skill.price",
                    EcosStyle.BOOK_ORANGE + "定价 " + t.chineseName,
                    "聊天输入价格", t.configKey));
        }
        return out;
    }

    private static List<TerminalButton> musicHistory(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var list = plugin.getMusicHistoryManager().recent();
        if (list.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "本局还没有点歌"));
        for (var e : list) {
            boolean fav = plugin.getMusicFavoritesManager().has(p.getUniqueId(), e.id());
            String raw = e.id() + "|" + e.name() + "|" + (e.author() == null ? "" : e.author());
            out.add(TerminalButton.of("mh.play",
                    EcosStyle.BOOK_COBALT + "▸ " + e.name(),
                    e.author() + " · " + e.time() + (e.caller() == null ? "" : " · " + e.caller()),
                    e.id()));
            out.add(TerminalButton.of("mh.fav",
                    fav ? EcosStyle.BOOK_ORANGE + "取消收藏" : EcosStyle.BOOK_JIQING + "收藏",
                    e.name(), raw));
        }
        return out;
    }

    private static List<TerminalButton> musicFav(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var list = plugin.getMusicFavoritesManager().list(p.getUniqueId());
        if (list.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "还没有收藏"));
        for (var s : list) {
            out.add(TerminalButton.of("mf.play",
                    EcosStyle.BOOK_COBALT + "▸ " + s.name(),
                    s.author() + "  " + s.id(), s.id()));
            out.add(TerminalButton.of("mf.del", EcosStyle.BOOK_MUTED + "删 " + s.name(), "移出收藏", s.id()));
        }
        return out;
    }

    private static List<TerminalButton> musicPlaylist(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var pm = plugin.getMusicPlaylistManager();
        out.add(TerminalButton.of("list.back", EcosStyle.BOOK_INK + pm.statusLine()));
        if (pm.isRunning())
            out.add(TerminalButton.of("pl.stop", EcosStyle.BOOK_ORANGE + "停止歌单"));
        var presets = pm.listPresets();
        if (presets.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "config 里没有 music.presets"));
        for (var pr : presets) {
            out.add(TerminalButton.of("pl.start",
                    EcosStyle.BOOK_COBALT + "▸ " + pr.displayName(),
                    (pr.description() == null || pr.description().isBlank() ? "" : pr.description() + " · ")
                            + pr.songIds().size() + " 首 · 顺序",
                    pr.key()));
            out.add(TerminalButton.of("pl.start",
                    EcosStyle.BOOK_ORANGE + "打乱 " + pr.displayName(),
                    "随机顺序", pr.key() + "|1"));
        }
        return out;
    }

    private static List<TerminalButton> transitTypes(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("ty.create", EcosStyle.BOOK_JIQING + "新建车型", "聊天: id 显示名"));
        for (var t : plugin.getTransitManager().allTypes()) {
            out.add(TerminalButton.of("ty.edit",
                    t.color() + "▸ " + t.displayName(),
                    t.id(), t.id()));
        }
        return out;
    }

    private static List<String> typeEditHeader(ES2UniPlugin plugin, Player p) {
        String id = plugin.getTransitTypeEditGUI().editing(p);
        var t = id == null ? null : plugin.getTransitManager().getType(id);
        if (t == null) return List.of(EcosStyle.BOOK_MUTED + "未选车型");
        int used = 0;
        for (var l : plugin.getTransitManager().allLines())
            if (l.type().equals(t.id())) used++;
        return List.of(
                t.color() + t.displayName(),
                EcosStyle.BOOK_MUTED + t.id() + " · 线路 " + used
        );
    }

    private static List<TerminalButton> typeEdit(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        String id = plugin.getTransitTypeEditGUI().editing(p);
        var t = id == null ? null : plugin.getTransitManager().getType(id);
        if (t == null) {
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "未选车型"));
            return out;
        }
        out.add(TerminalButton.of("ty.rename", EcosStyle.BOOK_JIQING + "改名", "聊天输入"));
        for (String c : com.etherstories.escore.gui.TransitTypeEditGUI.COLORS) {
            boolean on = c.equals(t.color());
            out.add(TerminalButton.of("ty.color",
                    c + (on ? "当前颜色" : "选用此色"),
                    on ? "正在使用" : "点击换色", c));
        }
        out.add(TerminalButton.of("ty.del", EcosStyle.BOOK_ORANGE + "删除此车型", "再点一次确认"));
        return out;
    }

    private static List<TerminalButton> transitAnnounce(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        TransitManager tm = plugin.getTransitManager();
        boolean arr = plugin.getConfig().getBoolean("transit.announce.enabled", true);
        boolean dep = plugin.getConfig().getBoolean("transit.announce.depart-enabled", true);
        String arrP = plugin.getConfig().getString("transit.announce.preset", "jr");
        String depP = plugin.getConfig().getString("transit.announce.depart-preset", "jrgo");
        out.add(TerminalButton.of("an.arr",
                arr ? EcosStyle.BOOK_JIQING + "到站铃开" : EcosStyle.BOOK_MUTED + "到站铃关"));
        out.add(TerminalButton.of("an.prev.arr", EcosStyle.BOOK_COBALT + "试听到站", arrP));
        out.add(TerminalButton.of("an.dep",
                dep ? EcosStyle.BOOK_JIQING + "发车铃开" : EcosStyle.BOOK_MUTED + "发车铃关"));
        out.add(TerminalButton.of("an.prev.dep", EcosStyle.BOOK_COBALT + "试听发车", depP));
        out.add(TerminalButton.of("an.stack",
                tm.stackRepeats() ? EcosStyle.BOOK_ORANGE + "和声加厚" : EcosStyle.BOOK_MUTED + "单音旋律"));
        String[] arrIds = {"jr", "yamanote", "mtr", "tube", "osaka", "chime",
                "cr", "crh", "beijing", "shanghai", "guangzhou"};
        String[] arrNames = {"JR 到站", "山手线", "港铁", "伦敦地铁", "大阪地铁", "下行琶音",
                "国铁到站", "动车到站", "北京地铁", "上海地铁", "广州地铁"};
        for (int i = 0; i < arrIds.length; i++) {
            boolean on = arrIds[i].equalsIgnoreCase(arrP);
            out.add(TerminalButton.of("an.set.arr",
                    (on ? EcosStyle.BOOK_JIQING : EcosStyle.BOOK_MUTED) + arrNames[i],
                    on ? "当前" : "选用并试听", arrIds[i]));
        }
        String[] depIds = {"jrgo", "doors", "whistle", "shinkansen", "crgo", "crhgo"};
        String[] depNames = {"JR 发车", "车门关闭", "汽笛", "新干线", "国铁发车", "动车发车"};
        for (int i = 0; i < depIds.length; i++) {
            boolean on = depIds[i].equalsIgnoreCase(depP);
            out.add(TerminalButton.of("an.set.dep",
                    (on ? EcosStyle.BOOK_ORANGE : EcosStyle.BOOK_MUTED) + depNames[i],
                    on ? "当前" : "选用并试听", depIds[i]));
        }
        return out;
    }

    private static List<TerminalButton> transitClaims(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        var list = plugin.getTransitManager().pendingClaims();
        if (list.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "没有待审申诉"));
        for (var c : list) {
            out.add(TerminalButton.of("list.back",
                    EcosStyle.BOOK_INK + "▸ " + c.playerName() + " #" + c.id(),
                    "进站 " + c.originName()));
            out.add(TerminalButton.of("tc.ok", EcosStyle.BOOK_JIQING + "通过 #" + c.id(), "批准", c.id()));
            out.add(TerminalButton.of("tc.no", EcosStyle.BOOK_ORANGE + "驳回 #" + c.id(), "拒绝", c.id()));
        }
        return out;
    }

    private static List<TerminalButton> transitRiders(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        TransitManager tm = plugin.getTransitManager();
        var all = tm.activeJourneys();
        if (all.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "当前无人在乘"));
        long now = System.currentTimeMillis();
        for (var e : all) {
            var off = Bukkit.getOfflinePlayer(e.getKey());
            String name = off.getName() == null ? e.getKey().toString().substring(0, 8) : off.getName();
            var j = e.getValue();
            long min = Math.max(0, (now - j.tapInMs()) / 60000L);
            out.add(TerminalButton.of("tr.clear",
                    (off.isOnline() ? EcosStyle.BOOK_COBALT : EcosStyle.BOOK_MUTED) + "▸ " + name,
                    tm.stationName(j.originId()) + " · " + tm.cabinName(j.cabin())
                            + " · " + min + " 分钟 · 点此清除",
                    e.getKey().toString()));
        }
        return out;
    }

    private static com.etherstories.escore.managers.RegionManager.Region regionByName(ES2UniPlugin plugin, String name) {
        if (name == null || name.isBlank()) return null;
        for (var r : plugin.getRegionManager().getAll()) {
            if (r.name().equalsIgnoreCase(name)) return r;
        }
        return null;
    }

    private static String money(ES2UniPlugin plugin, double n) {
        return plugin.getVaultHook().isEnabled()
                ? plugin.getVaultHook().format(n)
                : String.format("%.2f", n);
    }

    private static List<String> musicSearchHeader(ES2UniPlugin plugin, Player p) {
        int page = plugin.getAllMusicHook().currentPage(p) + 1;
        int n = plugin.getMusicSearchResultGUI().cached(p).size();
        return List.of(EcosStyle.BOOK_MUTED + "第 " + page + " 页 · " + n + " 首");
    }

    private static List<TerminalButton> musicSearch(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("ms.retry", EcosStyle.BOOK_JIQING + "再搜", "聊天输入歌名"));
        var hook = plugin.getAllMusicHook();
        if (hook.canPrev(p))
            out.add(TerminalButton.of("ms.prev", EcosStyle.BOOK_MUTED + "上一页"));
        if (hook.canNext(p))
            out.add(TerminalButton.of("ms.next", EcosStyle.BOOK_MUTED + "下一页"));
        var songs = plugin.getMusicSearchResultGUI().cached(p);
        if (songs.isEmpty())
            out.add(TerminalButton.of("ms.retry", EcosStyle.BOOK_MUTED + "没有结果"));
        int i = 0;
        for (var song : songs) {
            if (i >= 12) break;
            out.add(TerminalButton.of("ms.play",
                    EcosStyle.BOOK_COBALT + "▸ " + song.name(),
                    song.author() + "  " + song.id(), String.valueOf(i)));
            i++;
        }
        return out;
    }

    private static List<TerminalButton> adminEvent(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("adev.create", EcosStyle.BOOK_JIQING + "创建", "聊天输入活动名"));
        var all = plugin.getEventManager().getAllEvents();
        if (all.isEmpty())
            out.add(TerminalButton.of("list.back", EcosStyle.BOOK_MUTED + "暂无活动"));
        for (var ev : all) {
            out.add(TerminalButton.of("adev.del",
                    EcosStyle.BOOK_ORANGE + "删 " + ev.getName(),
                    ev.getCreator() + " · " + ev.getParticipantDisplay(), ev.getId()));
        }
        return out;
    }

    private static List<String> adminBcHeader(ES2UniPlugin plugin, Player p) {
        return List.of(EcosStyle.BOOK_MUTED + "间隔 "
                + plugin.getConfigManager().getBroadcastInterval() + "s");
    }

    private static List<TerminalButton> adminBc(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(back());
        out.add(TerminalButton.of("bc.add", EcosStyle.BOOK_JIQING + "添加", "聊天输入"));
        out.add(TerminalButton.of("bc.once", EcosStyle.BOOK_ORANGE + "立刻播", "聊天输入后全服发"));
        out.add(TerminalButton.of("bc.int.down", EcosStyle.BOOK_MUTED + "间隔-30s"));
        out.add(TerminalButton.of("bc.int.up", EcosStyle.BOOK_MUTED + "间隔+30s"));
        var entries = plugin.getConfigManager().getBroadcastEntries();
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            String preview = e.text.length() > 16 ? e.text.substring(0, 16) + "…" : e.text;
            out.add(TerminalButton.of("bc.edit",
                    EcosStyle.BOOK_INK + "#" + (i + 1) + " " + preview,
                    e.daysLabel() + (e.activeToday() ? " · 今日会播" : " · 今日跳过"),
                    String.valueOf(i)));
            out.add(TerminalButton.of("bc.days",
                    EcosStyle.BOOK_MUTED + e.daysLabel(), "切换每天/工作日/周末", String.valueOf(i)));
            out.add(TerminalButton.of("bc.del", EcosStyle.BOOK_ORANGE + "删#" + (i + 1), "删除", String.valueOf(i)));
        }
        return out;
    }

    private static List<TerminalButton> adminGrant(ES2UniPlugin plugin, Player p) {
        return List.of(
                back(),
                TerminalButton.of("grant.makeup", EcosStyle.BOOK_ORANGE + "补签券", "聊天: 玩家 数量"),
                TerminalButton.of("grant.lucky", EcosStyle.BOOK_COBALT + plugin.getLuckyBlockManager().displayName(),
                        "聊天: 玩家 数量")
        );
    }

    private static List<String> edgesHeader(ES2UniPlugin plugin, Player p) {
        String lineId = plugin.getTransitEdgesGUI().lineOf(p);
        var line = lineId == null ? null : plugin.getTransitManager().getLine(lineId);
        if (line == null) return List.of(EcosStyle.BOOK_MUTED + "未选线路");
        String pending = plugin.getTransitEdgesGUI().pendingFrom(p);
        return List.of(
                EcosStyle.BOOK_INK + line.displayName(),
                pending == null
                        ? EcosStyle.BOOK_MUTED + "先点一站再点另一站"
                        : EcosStyle.BOOK_ORANGE + "已选 " + plugin.getTransitManager().stationName(pending)
        );
    }

    private static List<TerminalButton> transitEdges(ES2UniPlugin plugin, Player p) {
        List<TerminalButton> out = new ArrayList<>();
        out.add(TerminalButton.of("edge.back", EcosStyle.BOOK_JIQING + "← 返回"));
        String lineId = plugin.getTransitEdgesGUI().lineOf(p);
        TransitManager tm = plugin.getTransitManager();
        if (lineId == null || tm.getLine(lineId) == null) {
            out.add(TerminalButton.of("edge.back", EcosStyle.BOOK_MUTED + "线路不存在"));
            return out;
        }
        String pending = plugin.getTransitEdgesGUI().pendingFrom(p);
        for (var e : tm.edgesOfLine(lineId)) {
            out.add(TerminalButton.of("edge.cut",
                    EcosStyle.BOOK_MUTED + tm.stationName(e.from()) + " ↔ " + tm.stationName(e.to()),
                    "断开此段", e.from() + "|" + e.to()));
        }
        for (var st : tm.allStations()) {
            if (!st.lineIds().contains(lineId)) continue;
            boolean sel = st.id().equals(pending);
            out.add(TerminalButton.of("edge.st",
                    (sel ? EcosStyle.BOOK_ORANGE : EcosStyle.BOOK_COBALT) + (sel ? "● " : "▸ ") + st.displayName(),
                    sel ? "再点取消" : "点选连接", st.id()));
        }
        boolean armed = plugin.getTransitEdgesGUI().isArmed(p);
        out.add(TerminalButton.of("edge.del",
                armed ? EcosStyle.BOOK_ORANGE + "再点确认删线" : EcosStyle.BOOK_MUTED + "删除本线"));
        return out;
    }
}
