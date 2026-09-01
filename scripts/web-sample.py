#!/usr/bin/env python3
"""Serve the real ECOS index.html with mock /v1 APIs for local preview."""
from __future__ import annotations

import json
import mimetypes
import os
import threading
import time
import urllib.parse
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HTML = os.path.join(ROOT, "src/main/resources/web/index.html")
PORT = int(os.environ.get("ECOS_SAMPLE_PORT", "8767"))

NOW = {
    "name": "Midnight City",
    "author": "M83",
    "album": "Hurry Up, We're Dreaming",
    "caller": "node0",
    "time": "4:03",
    "pic": "",
    "lyric": "The city is my church",
    "tlyric": "这座城市是我的教堂",
    "prev": "Waiting in a car",
    "next": "Waiting for a ride",
    "id": "sample",
}

STATE = {
    "checked": False,
    "streak": 12,
    "makeup": 2,
    "status": "在画外壳图纸",
    "share": True,
    "muted": False,
    "mail": [
        {"i": 0, "from": "Lumen", "date": "08-30 10:12", "read": False, "text": "今晚车站见，带上图纸。"},
        {"i": 1, "from": "Hub", "date": "08-29 21:04", "read": True, "text": "天气同步完成，注意夜间静音。"},
    ],
    "reqs": [{"id": "11111111-1111-1111-1111-111111111111", "name": "Kite"}],
    "friends": [
        {"id": "22222222-2222-2222-2222-222222222222", "name": "Lumen", "online": True, "share": True, "loc": "world 128 64 -40"},
        {"id": "33333333-3333-3333-3333-333333333333", "name": "Iris", "online": False, "share": False},
    ],
}


def desk():
    return {
        "ok": True,
        "name": "node0",
        "version": "1.20.75-sample",
        "online": False,
        "admin": True,
        "uuid": "00000000-0000-0000-0000-000000000001",
        "skin": "/v1/skin?n=node0",
        "players": 3,
        "mail": sum(1 for m in STATE["mail"] if not m["read"]),
        "checked": STATE["checked"],
        "streak": STATE["streak"],
        "status": STATE["status"],
        "balance": "12,480 ES",
        "stay": "星港酒店 · 7-1203",
        "makeup": STATE["makeup"],
        "reqs": len(STATE["reqs"]),
        "played": "186h 40m",
        "map": "",
        "muted": STATE["muted"],
        "hasLink": True,
        "link": False,
        "url": "",
        "tps": 19.94,
        "mspt": 18.2,
        "tape": [
            "样例数据，不是游戏服",
            "未读来信 %d · 签到%s · 连签 %d 日"
            % (sum(1 for m in STATE["mail"] if not m["read"]),
               "今日已签" if STATE["checked"] else "今日尚未签到", STATE["streak"]),
            "签名 「%s」" % STATE["status"],
            "正在播放 M83 / Midnight City",
        ],
        "who": [
            {"name": "Lumen", "uuid": "2", "world": "world", "x": 128, "y": 64, "z": -40, "head": "", "skin": ""},
            {"name": "Kite", "uuid": "3", "world": "world", "x": -12, "y": 71, "z": 88, "head": "", "skin": ""},
            {"name": "Iris", "uuid": "4", "world": "nether", "x": 20, "y": 48, "z": 6, "head": "", "skin": ""},
        ],
        "now": NOW,
        "queue": [
            {"name": "Midnight City", "author": "M83", "album": "Hurry Up, We're Dreaming", "caller": "node0"},
            {"name": "Nightcall", "author": "Kavinsky", "album": "OutRun", "caller": "Lumen"},
        ],
        "chat": [
            {"kind": "sys", "text": "样例终端已接入。操作会改这份假数据，不会进服。"},
            {"kind": "join", "text": "Lumen 进入了服务器"},
            {"kind": "chat", "name": "Lumen", "text": "车站图纸我放前台了"},
            {"kind": "web", "name": "node0", "text": "收到，晚上过去看"},
        ],
        "logins": [
            {"name": "node0", "kind": "WEB_NEW", "ip": "127.0.0.1", "note": "样例接入"},
        ],
        "logs": ["web pair node0 · sample"],
    }


def life():
    today = time.localtime()
    return {
        "ok": True,
        "checked": STATE["checked"],
        "streak": STATE["streak"],
        "makeup": STATE["makeup"],
        "status": STATE["status"],
        "balance": "12,480 ES",
        "stay": "星港酒店 · 7-1203",
        "share": STATE["share"],
        "played": "186h 40m",
        "taxOn": True,
        "taxRate": 0.05,
        "recycle": 2,
        "rideHint": "在乘: 中央站 → ? · 普通",
        "tap": True,
        "mail": STATE["mail"],
        "friends": STATE["friends"],
        "reqs": STATE["reqs"],
        "notices": [
            {"from": "市政", "date": "08-30", "text": "本周车站连边施工，末班提前 20 分钟。"},
            {"from": "ECOS", "date": "08-28", "text": "网页终端开放生活页，离线可签到转账写信。"},
        ],
        "jobs": [
            {"id": "job_1", "title": "车站指示牌重绘", "who": "市政", "slots": "1/3", "pay": "800 ES", "mine": False},
            {"id": "job_2", "title": "酒店大堂绿植", "who": "Lumen", "slots": "2/2", "pay": "350 ES", "mine": True},
        ],
        "events": [
            {"id": "event_1", "name": "周末跳蚤市场", "n": 6, "max": 20, "in": False},
            {"id": "event_2", "name": "夜间观星", "n": 4, "max": 8, "in": True},
        ],
        "show": [
            {"id": "sc_1", "title": "中央站天窗", "who": "Iris", "votes": 18},
            {"id": "sc_2", "title": "港口起重机", "who": "Kite", "votes": 11},
        ],
        "play": [{"name": "node0", "v": "186h 40m"}, {"name": "Lumen", "v": "142h 10m"}, {"name": "Iris", "v": "98h 05m"}],
        "trade": [{"name": "Lumen", "v": "24,100 ES"}, {"name": "node0", "v": "9,820 ES"}],
        "rides": [{"name": "Iris", "v": "86 次"}, {"name": "node0", "v": "54 次"}],
        "deaths": [
            {"w": "world", "x": 12, "y": 63, "z": -8, "cause": "坠落", "date": "08-29 19:22"},
        ],
        "wps": [
            {"name": "工作室", "w": "world", "x": 80, "y": 70, "z": 16},
            {"name": "中央站", "w": "world", "x": 0, "y": 64, "z": 0},
        ],
        "ins": [{"when": "08-29 19:22", "n": 14}, {"when": "08-20 08:01", "n": 9}],
        "cover": {"ok": True, "kind": "transit", "place": "中央站"},
        "histRide": [
            {"from": "中央站", "to": "港口", "fare": "6 ES"},
            {"from": "港口", "to": "星港酒店", "fare": "4 ES"},
        ],
        "stops": [{"id": "central", "name": "中央站"}, {"id": "port", "name": "港口"}, {"id": "hotel", "name": "星港酒店"}],
        "homes": [{"id": "u1", "addr": "星港 7-1203", "sale": False, "price": ""}],
        "sale": [{"addr": "港湾 2-501", "who": "Kite", "price": "6,400 ES"}],
        "kits": [{"name": "starter", "rule": "每人一次", "n": 8}, {"name": "builder", "rule": "冷却 24 小时", "n": 12}],
        "lands": [{"name": "工作室", "vis": False, "area": 1600}],
        "pubs": [{"name": "中央广场", "who": "市政"}],
        "auras": [{"id": "stardust", "name": "星尘", "on": True}, {"id": "note_circle", "name": "音符", "on": False}],
        "bright": "中",
        "miles": ["百小时", "通勤达人"],
        "skills": [{"name": "霁弧", "price": "1,200 ES"}],
        "cal": {
            "y": today.tm_year,
            "m": today.tm_mon,
            "today": today.tm_mday,
            "days": [1, 2, 4, 8, 12, 15, 20, 24, 28],
        },
    }


def act(body: dict) -> dict:
    do = body.get("do") or body.get("action") or ""
    ident = str(body.get("id") or "")
    name = str(body.get("name") or "")
    text = str(body.get("text") or "")
    amount = str(body.get("amount") or "")
    if do == "checkin":
        if STATE["checked"]:
            return {"ok": False, "error": "今日已签到"}
        STATE["checked"] = True
        STATE["streak"] += 1
        return {"ok": True, "msg": "签到成功 · 连签 %d 日 · 40 ES" % STATE["streak"]}
    if do == "makeup":
        if STATE["makeup"] <= 0:
            return {"ok": False, "error": "没有补签券"}
        STATE["makeup"] -= 1
        return {"ok": True, "msg": "已补签 " + ident}
    if do == "status":
        STATE["status"] = text.strip()
        return {"ok": True, "msg": "已清除签名" if not STATE["status"] else "签名已更新"}
    if do == "mail.read":
        for m in STATE["mail"]:
            if str(m["i"]) == ident:
                m["read"] = True
        return {"ok": True, "msg": "已读"}
    if do == "mail.del":
        STATE["mail"] = [m for m in STATE["mail"] if str(m["i"]) != ident]
        return {"ok": True, "msg": "已删除"}
    if do == "mail.send":
        return {"ok": True, "msg": "已送达 " + name}
    if do == "friend.add":
        return {"ok": True, "msg": "已发出好友请求"}
    if do == "friend.acc":
        keep = [r for r in STATE["reqs"] if r["id"] != ident]
        found = next((r for r in STATE["reqs"] if r["id"] == ident), None)
        STATE["reqs"] = keep
        if found:
            STATE["friends"].append({"id": found["id"], "name": found["name"], "online": False, "share": False})
        return {"ok": True, "msg": "已接受"}
    if do == "friend.den":
        STATE["reqs"] = [r for r in STATE["reqs"] if r["id"] != ident]
        return {"ok": True, "msg": "已拒绝"}
    if do == "friend.del":
        STATE["friends"] = [f for f in STATE["friends"] if f["id"] != ident]
        return {"ok": True, "msg": "已删除好友"}
    if do == "friend.share":
        STATE["share"] = not STATE["share"]
        return {"ok": True, "msg": "已开启位置分享" if STATE["share"] else "已关闭位置分享"}
    if do == "pay":
        return {"ok": True, "msg": "已转 %s → %s（税另计）" % (amount, name)}
    if do == "job.take":
        return {"ok": True, "msg": "已接委托"}
    if do == "show.vote":
        return {"ok": True, "msg": "已投票"}
    if do == "ev.toggle":
        return {"ok": True, "msg": "已切换参加状态"}
    if do == "wp.del":
        return {"ok": True, "msg": "已删除坐标"}
    if do == "aura.eq":
        return {"ok": True, "msg": "已装备（需在游戏中可见）"}
    if do == "aura.off":
        return {"ok": True, "msg": "已卸下特效"}
    if do == "aura.bright":
        return {"ok": True, "msg": "亮度：高"}
    if do == "land.vis":
        return {"ok": True, "msg": "已公开"}
    if do == "route":
        return {"ok": True, "msg": "6 ES · %s → %s" % (name, text)}
    return {"ok": False, "error": "样例未实现 " + do}


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        print("[%s] %s" % (self.address_string(), fmt % args))

    def _json(self, code, obj):
        raw = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(raw)

    def _bytes(self, code, body, mime):
        self.send_response(code)
        self.send_header("Content-Type", mime)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _body(self):
        n = int(self.headers.get("Content-Length") or 0)
        if n <= 0:
            return {}
        raw = self.rfile.read(n)
        try:
            return json.loads(raw.decode("utf-8"))
        except Exception:
            return {}

    def do_GET(self):
        path = urllib.parse.urlparse(self.path).path
        if path in ("/", "/index.html"):
            with open(HTML, "rb") as f:
                self._bytes(200, f.read(), "text/html; charset=utf-8")
            return
        if path == "/v1/info":
            return self._json(200, {"ok": True, "version": "1.20.75-sample"})
        if path == "/v1/desk":
            return self._json(200, desk())
        if path == "/v1/life":
            return self._json(200, life())
        if path == "/v1/now":
            return self._json(200, {"ok": True, "online": False, "now": NOW})
        if path == "/v1/library":
            return self._json(200, {
                "ok": True,
                "favorites": [{"id": "1", "name": "Midnight City", "author": "M83", "album": "Hurry Up, We're Dreaming", "fav": True}],
                "history": [{"id": "2", "name": "Nightcall", "author": "Kavinsky", "album": "OutRun"}],
            })
        if path == "/v1/skin":
            self.send_response(302)
            self.send_header("Location", "https://mc-heads.net/avatar/Steve/40")
            self.end_headers()
            return
        self._json(404, {"ok": False, "error": "没有这个接口"})

    def do_POST(self):
        path = urllib.parse.urlparse(self.path).path
        body = self._body()
        if path == "/v1/pair":
            return self._json(200, {"ok": True, "token": "sample-token"})
        if path == "/v1/act":
            return self._json(200, act(body))
        if path == "/v1/search":
            return self._json(200, {
                "ok": True, "page": 1, "pages": 1, "prev": False, "next": False,
                "songs": [
                    {"id": "s1", "name": "Midnight City", "author": "M83", "album": "Hurry Up, We're Dreaming"},
                    {"id": "s2", "name": "Nightcall", "author": "Kavinsky", "album": "OutRun"},
                ],
            })
        if path == "/v1/music":
            return self._json(200, {"ok": True, "msg": "样例已记下"})
        if path == "/v1/chat":
            return self._json(200, {"ok": True})
        if path == "/v1/complete":
            return self._json(200, {"ok": True, "hints": ["msg", "pay", "ecos"]})
        if path == "/v1/link":
            return self._json(200, {"ok": True, "link": bool(body.get("all"))})
        if path == "/v1/mute":
            STATE["muted"] = bool(body.get("muted"))
            return self._json(200, {"ok": True})
        if path == "/v1/logout":
            return self._json(200, {"ok": True})
        self._json(404, {"ok": False})


def main():
    if not os.path.isfile(HTML):
        raise SystemExit("找不到 " + HTML)
    httpd = ThreadingHTTPServer(("127.0.0.1", PORT), Handler)
    url = "http://127.0.0.1:%d/?code=000000" % PORT
    print("ECOS sample  " + url)
    threading.Timer(0.4, lambda: webbrowser.open(url)).start()
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        httpd.server_close()


if __name__ == "__main__":
    main()
