#!/usr/bin/env python3
"""Serve the custom TNT pack (with CORS for browser-based Eaglercraft clients) and the Lston Packs site."""

import hashlib
import json
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parent
PACK_PATH = ROOT.parent / "web" / "custom-tnt-pack.zip"
SITE = ROOT / "site" / "index.html"
TEXTURES = ROOT / "assets" / "lston" / "textures" / "item"
VANILLA_ITEMS = ROOT / "assets" / "minecraft" / "textures" / "items"
EXTRA_TEXTURES = {
    "tnt_boss_bar": ROOT / "site" / "bossbar.png",
}


REPO = ROOT.parent
SRC = {
    "tnt": REPO / "custom-tnt-plugin/src/main/java/net/lstonstudios/customtnt/CustomTNTPlugin.java",
    "gun": REPO / "custom-gun-plugin/src/main/java/net/lstonstudios/customgun/CustomGunPlugin.java",
    "ability": REPO / "custom-ability-plugin/src/main/java/net/lstonstudios/customability/CustomAbilityPlugin.java",
    "sword": REPO / "custom-items-plugin/src/main/java/net/lstonstudios/customitems/CustomItemsPlugin.java",
}
STR = r'"((?:[^"\\]|\\.)*)"'
CLEAN = lambda t: t.replace('\\"', '"').replace("\\'", "'")


def _read(key):
    try:
        return SRC[key].read_text(errors="replace")
    except OSError:
        return ""


def tnt_sizes():
    """Effect radius (blocks) and blast power per TNT key, from the live plugin config."""
    for path in (REPO / "server/plugins/CustomTNT/config.yml", REPO / "custom-tnt-plugin/src/main/resources/config.yml"):
        try:
            lines = path.read_text().splitlines()
        except OSError:
            continue
        out, key, inside = {}, None, False
        for line in lines:
            if re.match(r"^\S", line):
                inside = line.startswith("explosions:")
            elif inside:
                m = re.match(r"^  ([\w-]+):\s*$", line)
                if m:
                    key = m.group(1)
                    out[key] = {}
                    continue
                m = re.match(r"^    (size|power):\s*([\d.]+)", line)
                if m and key:
                    out[key][m.group(1)] = float(m.group(2))
        if out:
            return out
    return {}


def item_details():
    """Item details parsed from the plugin sources, keyed by texture name, so the site tracks the plugins."""
    d = {}
    sizes = tnt_sizes()
    tnt = _read("tnt")
    cooldowns = {}
    for m in re.finditer(r'^\s+([A-Z_]+)\("([a-z-]+)",\s*(?:ChatColor\.\w+ \+ )?' + STR + r',(.*?)' + STR + r',\s*(0x[0-9A-Fa-f]+|\d+),\s*(\d+)\)',
                         tnt, re.S | re.M):
        _, key, name, _, desc, _, cd = m.groups()
        tex = key.replace("-tnt", "").replace("-", "_") + "_tnt"
        tex = "xray_tnt" if key == "x-ray" else tex
        cfg = sizes.get(key) or sizes.get(key.replace("-tnt", ""), {})
        stats = []
        if "size" in cfg:
            radius = min(max(int(cfg["size"]), 4), 10) if key == "mining" else int(cfg["size"])
            stats.append(["Radius", f"{radius} blocks"])
        if key == "mining":
            stats.append(["Tunnel length", "12 blocks"])
        if cfg.get("power"):
            stats.append(["Blast power", f"{cfg['power']:g}"])
        stats.append(["Cooldown", "none" if int(cd) == 0 else f"{cd}s"])
        d[tex] = {"name": CLEAN(name), "type": "TNT", "desc": CLEAN(desc), "cmd": f"/customtnt <player> {key}", "stats": stats}
    for m in re.finditer(r'^\s+[A-Z_]+\("(\w+)",\s*(?:Material\.\w+,\s*)?ChatColor\.\w+,\s*' + STR + r',\s*' + STR +
                         r',\s*([\d.]+),\s*(\d+),\s*([\d.]+),\s*(\d+),\s*(\d+),\s*(\d+)', _read("gun"), re.M):
        key, name, desc, dmg, pel, spr, rng, cd, expl = m.groups()
        st = [["Damage", f"{float(dmg) / 2:g} hearts" if float(dmg) else "explosion only"], ["Range", f"{rng} blocks"],
              ["Reload", f"{int(cd) / 20:g}s"]]
        if int(pel) > 1:
            st.insert(1, ["Pellets", pel])
        if int(expl):
            st.append(["Blast size", expl])
        d["gun_" + key] = {"name": CLEAN(name), "type": "Gun", "desc": CLEAN(desc), "cmd": f"/cgun <player> {key}", "stats": st}
    for m in re.finditer(r'^\s+[A-Z_]+\("(\w+)",\s*Material\.\w+,\s*ChatColor\.\w+,\s*' + STR + r',\s*' + STR + r',\s*(\d+)\)',
                         _read("ability"), re.M | re.S):
        key, name, desc, cd = m.groups()
        d["ability_" + key] = {"name": CLEAN(name), "type": "Ability", "desc": CLEAN(desc),
                               "cmd": f"/customability <player> {key}", "stats": [["Cooldown", f"{cd}s"]]}
    for m in re.finditer(r'^\s+[A-Z_]+\("(\w+)",\s*(?:Material\.\w+,\s*)?ChatColor\.\w+,\s*ChatColor\.\w+,\s*' + STR + r',\s*' + STR + r'\)', _read("sword"), re.M):
        key, name, desc = m.groups()
        d["sword_" + key] = {"name": CLEAN(name) + " Sword", "type": "Sword", "desc": CLEAN(desc), "cmd": f"/customsword <player> {key}", "stats": []}
    d["redstone_pass"] = {"name": "One-Time Redstone Pass", "type": "Pass", "cmd": "/pass <player> [amount]",
                          "desc": "Right-click a special pass button with it to press the button. The pass is used up.", "stats": [["Uses", "1"]]}
    d["pass_button"] = {"name": "Pass Button", "type": "Pass", "cmd": "/passbutton <player> [amount]",
                        "desc": "A stone-style button with a slit for a pass. Only works when a redstone pass is used on it.", "stats": []}
    return d


class PackHandler(BaseHTTPRequestHandler):
    def _send_headers(self, status, content_length=0):
        self.send_response(status)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Range")
        self.send_header("Access-Control-Expose-Headers", "Content-Length")
        self.send_header("Cache-Control", "no-store")
        if status == 200:
            self.send_header("Content-Type", "application/zip")
            self.send_header("Content-Length", str(content_length))
        self.end_headers()

    def _send(self, body, content_type, include_body, cache="no-store"):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", cache)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        if include_body:
            self.wfile.write(body)

    def _serve(self, include_body):
        path = urlsplit(self.path).path
        if path == "/custom-tnt-pack.zip":
            self._send(PACK_PATH.read_bytes(), "application/zip", include_body)
        elif path in ("/", "/index.html"):
            self._send(SITE.read_bytes(), "text/html; charset=utf-8", include_body)
        elif path in ("/favicon.png", "/favicon.ico"):
            self._send((TEXTURES / "anti_tnt.png").read_bytes(), "image/png", include_body, "public, max-age=300")
        elif path == "/bossbar.png":
            self._send((ROOT / "site" / "bossbar.png").read_bytes(), "image/png", include_body, "public, max-age=300")
        elif path == "/api/info":
            pack = PACK_PATH.read_bytes()
            info = {
                "items": sorted(p.stem for p in TEXTURES.glob("*.png")) + sorted(EXTRA_TEXTURES),
                "size": len(pack),
                "sha1": hashlib.sha1(pack).hexdigest(),
                "details": item_details(),
            }
            self._send(json.dumps(info).encode(), "application/json", include_body)
        elif path.startswith("/textures/") and path.endswith(".png"):
            name = path[len("/textures/"):-len(".png")]
            file = EXTRA_TEXTURES.get(name, TEXTURES / (name + ".png"))
            if "/" not in name and file.is_file():
                self._send(file.read_bytes(), "image/png", include_body, "public, max-age=300")
            else:
                self._send_headers(404)
        else:
            self._send_headers(404)

    def do_GET(self):
        self._serve(include_body=True)

    def do_HEAD(self):
        self._serve(include_body=False)

    def do_OPTIONS(self):
        self._send_headers(204)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8082), PackHandler)
    print("Lston Packs site + CORS-enabled pack server listening on 0.0.0.0:8082", flush=True)
    server.serve_forever()
