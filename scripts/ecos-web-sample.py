#!/usr/bin/env python3
"""Local ECOS terminal sample. Serves the real index.html + mock /v1 APIs."""
from __future__ import annotations

import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HTML = (ROOT / "src/main/resources/web/index.html").read_bytes()
PHONE = (ROOT / "scripts/phone.html").read_bytes()
PORT = 8766
PAIR = "204800"
TOKEN = "sample-token"
MAP = "http://172.16.0.43:8100/"

NOW = int(time.time() * 1000)
DESK = {
    "ok": True,
    "name": "node0",
    "version": "1.20.39",
    "online": True,
    "admin": True,
    "tps": 19.94,
    "mspt": 8.2,
    "players": 7,
    "mail": 2,
    "checked": True,
    "streak": 14,
    "muted": False,
    "hasLink": True,
    "link": False,
    "url": "",
    "status": "在绿野修路",
    "tape": [
        "Etharia Central OS · node0 · 终端在线",
        "在线 7 人",
        "余额 12,480 EP",
        "邮件未读 2 · 签到已签 · 连签 14",
        "签名 「在绿野修路」",
        "点播 HOYO-MiX / 夜航星 · node0 点的 · 队列 2",
        "[Web] <node0> 我在路上",
    ],
    "balance": "12,480 EP",
    "map": MAP,
    "skin": "https://mc-heads.net/skin/jeb_",
    "who": [
        {"name": "node0", "world": "world_greenfield", "x": 128, "y": 72, "z": -40, "ip": "172.16.0.44",
         "head": "https://mc-heads.net/head/jeb_/80"},
        {"name": "Lumen", "world": "world", "x": -12, "y": 64, "z": 88,
         "head": "https://mc-heads.net/head/Alex/80"},
        {"name": "Kite", "world": "world", "x": 4, "y": 71, "z": 16,
         "head": "https://mc-heads.net/head/Steve/80"},
    ],
    "now": {
        "id": "1869325773",
        "name": "夜航星",
        "author": "HOYO-MiX",
        "album": "原神",
        "pic": "https://p2.music.126.net/6y-USeQw5-fK1Y4c4XJzHg==/109951165911211692.jpg",
        "caller": "node0",
        "queue": 2,
        "lyric": "穿过夜的星河",
        "tlyric": "",
        "prev": "风把路灯吹亮",
        "next": "我还在路上",
        "now": 42000,
        "all": 210000,
    },
    "queue": [
        {"id": "1", "name": "夜航星", "author": "HOYO-MiX", "album": "原神", "caller": "node0"},
        {"id": "2", "name": "Take Me Hand", "author": "DAISHI DANCE", "album": "借物少女", "caller": "Lumen"},
        {"id": "3", "name": "City Lights", "author": "ES Radio", "album": "", "caller": "Kite"},
    ],
    "chat": [
        {"ts": NOW - 120000, "name": "系统", "text": "Lumen 加入了游戏", "web": False, "kind": "join"},
        {"ts": NOW - 90000, "name": "Lumen", "text": "绿野南门集合", "web": False, "kind": "chat"},
        {"ts": NOW - 60000, "name": "Kite", "text": "谁带鞘翅", "web": False, "kind": "chat"},
        {"ts": NOW - 45000, "name": "系统", "text": "[广播] 晚 8 点绿野南门集市", "web": False, "kind": "sys"},
        {"ts": NOW - 30000, "name": "node0", "text": "我在路上", "web": True, "kind": "web"},
        {"ts": NOW - 8000, "name": "系统", "text": "Kite 离开了游戏", "web": False, "kind": "quit"},
        {"ts": NOW - 4000, "name": "系统", "text": "node0 登录  IP 172.16.0.44", "web": False, "kind": "admin"},
    ],
    "logins": [
        {"ts": NOW - 4000, "name": "node0", "ip": "172.16.0.44", "kind": "NEW_IP", "note": "上次 172.16.0.18"},
        {"ts": NOW - 180000, "name": "Steve", "ip": "198.51.100.7", "kind": "PROBE", "note": "握手后未进服"},
        {"ts": NOW - 3600000, "name": "Kite", "ip": "10.0.0.12", "kind": "FAIL", "note": "KICK_BANNED"},
    ],
    "logs": [
        "2026-08-29 15:20:01 | node0 | WEB_CMD | ecos tps",
        "2026-08-29 12:00:00 | CONSOLE | RELOAD | command",
    ],
}

CATALOG = [
    {"id": "1869325773", "name": "夜航星", "author": "HOYO-MiX", "album": "原神"},
    {"id": "29732210", "name": "Take Me Hand", "author": "DAISHI DANCE / Cécile Corbel", "album": "借物少女"},
    {"id": "447925558", "name": "City of Stars", "author": "Ryan Gosling", "album": "La La Land"},
    {"id": "1297496909", "name": "Past Lives", "author": "BØRNS", "album": "Dopamine"},
    {"id": "139774", "name": "晴天", "author": "周杰伦", "album": "叶惠美"},
    {"id": "185811", "name": "稻香", "author": "周杰伦", "album": "魔杰座"},
    {"id": "186016", "name": "七里香", "author": "周杰伦", "album": "七里香"},
    {"id": "186001", "name": "青花瓷", "author": "周杰伦", "album": "我很忙"},
    {"id": "108242", "name": "江南", "author": "林俊杰", "album": "第二天堂"},
    {"id": "108139", "name": "曹操", "author": "林俊杰", "album": "曹操"},
    {"id": "25706282", "name": "起风了", "author": "买辣椒也用券", "album": ""},
    {"id": "447925559", "name": "City Lights", "author": "ES Radio", "album": ""},
    {"id": "26547866", "name": "光年之外", "author": "邓紫棋", "album": ""},
]
SEARCH = {"q": "", "page": 0, "songs": []}
LYRICS = [
    ("风把路灯吹亮", "The lamps wake in the wind"),
    ("穿过夜的星河", "Through the river of night"),
    ("我还在路上", "I am still on the road"),
    ("绿野的南门开着", "South gate of Greenfield stays open"),
]


def apply_lyric():
    i = int(time.time() * 2) % len(LYRICS)
    prev = LYRICS[(i - 1) % len(LYRICS)]
    cur = LYRICS[i]
    nxt = LYRICS[(i + 1) % len(LYRICS)]
    DESK["now"].update(
        lyric=cur[0],
        tlyric=cur[1],
        prev=prev[0],
        next=nxt[0],
        now=int(time.time() * 1000) % 210000,
        all=210000,
    )


def search_page():
    songs = SEARCH["songs"]
    pages = max(1, (len(songs) + 9) // 10)
    page = max(0, min(SEARCH["page"], pages - 1))
    SEARCH["page"] = page
    return {
        "ok": True,
        "q": SEARCH["q"],
        "page": page + 1,
        "pages": pages,
        "prev": page > 0,
        "next": page < pages - 1,
        "songs": songs[page * 10 : (page + 1) * 10],
    }


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        print(f"{self.address_string()} {fmt % args}")

    def _send(self, code: int, ctype: str, body: bytes):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _json(self, code: int, obj):
        self._send(code, "application/json; charset=utf-8", json.dumps(obj, ensure_ascii=False).encode())

    def _auth(self) -> bool:
        auth = self.headers.get("Authorization", "")
        return auth == f"Bearer {TOKEN}" or "sample-token" in (self.headers.get("Cookie") or "")

    def do_GET(self):
        path = self.path.split("?", 1)[0]
        if path in ("/", "/index.html"):
            self._send(200, "text/html; charset=utf-8", HTML)
            return
        if path in ("/phone", "/phone.html"):
            self._send(200, "text/html; charset=utf-8", PHONE)
            return
        if path == "/v1/info":
            self._json(200, {"ok": True, "name": "ECOS", "version": "1.20.39", "online": 7, "song": "夜航星"})
            return
        if path == "/v1/desk":
            if not self._auth():
                self._json(401, {"ok": False, "error": "未连接"})
                return
            apply_lyric()
            self._json(200, DESK)
            return
        if path == "/v1/now":
            if not self._auth():
                self._json(401, {"ok": False, "error": "未连接"})
                return
            apply_lyric()
            self._json(200, {"ok": True, "now": DESK["now"]})
            return
        self._send(404, "text/plain", b"not found")

    def do_POST(self):
        path = self.path.split("?", 1)[0]
        n = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(n).decode("utf-8", "replace") if n else ""
        try:
            body = json.loads(raw) if raw.strip().startswith("{") else {}
        except json.JSONDecodeError:
            body = {}
        if path == "/v1/pair":
            code = str(body.get("code") or "").strip()
            if code != PAIR:
                self._json(401, {"ok": False, "error": f"样例码是 {PAIR}"})
                return
            self._json(200, {"ok": True, "token": TOKEN, "name": "node0"})
            return
        if path == "/v1/logout":
            self._json(200, {"ok": True})
            return
        if path == "/v1/search":
            if not self._auth():
                self._json(401, {"ok": False, "error": "未连接"})
                return
            direction = str(body.get("dir") or "").strip().lower()
            if direction in ("next", "prev"):
                if not SEARCH["songs"]:
                    self._json(400, {"ok": False, "error": "先搜索"})
                    return
                SEARCH["page"] += 1 if direction == "next" else -1
                self._json(200, search_page())
                return
            q = str(body.get("q") or body.get("query") or "").strip()
            if not q:
                self._json(400, {"ok": False, "error": "输入歌名"})
                return
            songs = [s for s in CATALOG if q.lower() in (s["name"] + s["author"] + s["album"]).lower()]
            if not songs:
                songs = list(CATALOG)
            SEARCH.update(q=q, page=0, songs=songs)
            self._json(200, search_page())
            return
        if path == "/v1/chat":
            if not self._auth():
                self._json(401, {"ok": False, "error": "未连接"})
                return
            text = str(body.get("text") or "").strip()
            if not text:
                self._json(400, {"ok": False, "error": "空消息"})
                return
            if text.startswith("/"):
                cmd = text[1:].strip()
                if not DESK.get("admin") and cmd.startswith(("ecos broadcast", "broadcast", "op ", "stop")):
                    self._json(403, {"ok": False, "error": "没有权限"})
                    return
                reply = f"样例已执行 /{cmd}" if cmd else "空指令"
                DESK["chat"].append({
                    "ts": int(time.time() * 1000),
                    "name": "node0",
                    "text": text[:200],
                    "web": False,
                    "kind": "cmd",
                })
                DESK["chat"].append({
                    "ts": int(time.time() * 1000),
                    "name": "系统",
                    "text": reply,
                    "web": False,
                    "kind": "sys",
                })
                self._json(200, {"ok": True, "command": True, "replies": [reply]})
                return
            DESK["chat"].append({
                "ts": int(time.time() * 1000),
                "name": "node0",
                "text": text[:200],
                "web": True,
                "kind": "web",
            })
            self._json(200, {"ok": True})
            return
        if path == "/v1/complete":
            if not self._auth():
                self._json(401, {"ok": False, "error": "未连接"})
                return
            text = str(body.get("text") or "").lstrip("/")
            hints = [w for w in ("ecos", "es2", "music", "list", "help") if w.startswith(text.split()[-1] if text else "")]
            self._json(200, {"ok": True, "hints": hints})
            return
        if path == "/v1/link":
            if not self._auth():
                self._json(401, {"ok": False, "error": "未连接"})
                return
            if "all" in body:
                DESK["link"] = bool(body.get("all"))
            self._json(200, {"ok": True, "hasLink": True, "link": bool(DESK.get("link"))})
            return
        if path == "/v1/mute":
            if not self._auth():
                self._json(401, {"ok": False, "error": "未连接"})
                return
            muted = bool(body.get("muted"))
            DESK["muted"] = muted
            self._json(200, {"ok": True, "muted": muted})
            return
        if path == "/v1/music":
            if not self._auth():
                self._json(401, {"ok": False, "error": "未连接"})
                return
            act = str(body.get("action") or "").strip().lower()
            if act == "vote":
                self._json(200, {"ok": True, "msg": "已投下切歌一票"})
                return
            if act == "next":
                if not DESK.get("admin"):
                    self._json(403, {"ok": False, "error": "切歌需要管理权限"})
                    return
                if DESK["queue"]:
                    DESK["queue"].pop(0)
                self._json(200, {"ok": True, "msg": "已切到下一首"})
                return
            sid = str(body.get("id") or "").strip()
            if not sid:
                self._json(400, {"ok": False, "error": "先搜索再点结果"})
                return
            hit = next((s for s in CATALOG if s["id"] == sid), None)
            name = hit["name"] if hit else f"#{sid}"
            author = hit["author"] if hit else "网易云"
            DESK["queue"].append({"id": sid, "name": name, "author": author, "caller": "node0"})
            self._json(200, {"ok": True, "via": "sample"})
            return
        self._send(404, "text/plain", b"not found")


if __name__ == "__main__":
    httpd = ThreadingHTTPServer(("127.0.0.1", PORT), Handler)
    print(f"ECOS sample  http://127.0.0.1:{PORT}/")
    print(f"游戏终端    http://127.0.0.1:{PORT}/phone")
    print(f"连接码  {PAIR}")
    httpd.serve_forever()
