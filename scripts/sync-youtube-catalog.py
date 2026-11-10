#!/usr/bin/env python3
"""Synchronise public Mazsola Klub videos with the website catalogue.

Only updates the catalogue after a full, validated YouTube extraction.
No credentials or YouTube API key required. Run from the repo root.
"""
from __future__ import annotations
import json
import re
from pathlib import Path

from yt_dlp import YoutubeDL

ROOT = Path(__file__).resolve().parents[1]
SONGS = ROOT / "songs.json"
TITLES = ROOT / "song-titles.json"
CHANNEL = "https://www.youtube.com/@mazsola-klub/videos"
KNOWN_DURATION = {"3s9KkbnC5sc": "2:34", "C50N4HcMsbw": "6:31"}

# These IDs are served as translated English titles to some unauthenticated clients.
# The Hungarian originals were verified against the channel owner's 2026-11-10 list.
HUNGARIAN_TITLES = {
    "bKq6zXh33OM": "🍇 Lipem, lopom a szőlőt | Magyar vicces mondókadal gyerekeknek",
    "tr2Xj94C7Pg": "🌷 Már megjöttünk ez helyre 🌸 Anyák napi dal | Megható gyerekdal anyukáknak 💖",
    "1O8nMmfTJDQ": "🌸 Tavaszi szél vizet áraszt | Magyar népdal gyerekeknek 🌿🐦",
}



def duration_label(value):
    if isinstance(value, (int, float)) and value > 0:
        seconds = int(value)
        return f"{seconds // 60}:{seconds % 60:02d}"
    if isinstance(value, str) and re.fullmatch(r"\d+:\d{2}", value):
        return value
    return ""


def main():
    previous = json.loads(SONGS.read_text(encoding="utf-8"))
    with YoutubeDL({
        "quiet": True,
        "no_warnings": True,
        "extract_flat": "in_playlist",
        "ignoreerrors": False,
    }) as ydl:
        info = ydl.extract_info(CHANNEL, download=False)
    extracted = [x for x in (info or {}).get("entries", []) if x]
    ids = [x.get("id") for x in extracted]
    if len(extracted) < max(200, len(previous) - 5):
        raise RuntimeError(f"Truncated extraction: {len(extracted)} vs existing {len(previous)}")
    if len(ids) != len(set(ids)) or any(not re.fullmatch(r"[A-Za-z0-9_-]{11}", x or "") for x in ids):
        raise RuntimeError("Duplicate or malformed YouTube video IDs")
    if any(not x.get("title") for x in extracted):
        raise RuntimeError("Missing YouTube title")
    old_by_id = {x["id"]: x for x in previous}
    current_id_set = set(ids)
    removed = [x for x in previous if x["id"] not in current_id_set]
    if len(removed) > 5:
        raise RuntimeError(f"Unexpected removal of {len(removed)} videos; aborting")
    rows = []
    changed_titles = []
    for x in extracted:
        id_ = x["id"]
        title = HUNGARIAN_TITLES.get(id_, x["title"])
        old = old_by_id.get(id_, {})
        if old and old.get("title") != title:
            changed_titles.append((id_, old["title"], title))
        row = dict(old)
        row.update({
            "id": id_,
            "title": title,
            "duration": duration_label(x.get("duration")) or old.get("duration") or KNOWN_DURATION.get(id_, ""),
            "thumbnail": old.get("thumbnail") or f"https://i.ytimg.com/vi/{id_}/hqdefault.jpg",
        })
        rows.append(row)
    added = [x for x in rows if x["id"] not in old_by_id]
    print(f"YouTube: {len(rows)} videos; added {len(added)}; removed {len(removed)}; renamed {len(changed_titles)}")
    for x in added:
        print(f"ADDED {x['id']} {x['title']}")
    for x in removed:
        print(f"REMOVED {x['id']} {x['title']}")
    for id_, old_title, new_title in changed_titles:
        print(f"RENAMED {id_}: {old_title!r} -> {new_title!r}")
    SONGS.write_text(json.dumps(rows, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    TITLES.write_text(json.dumps([x["title"] for x in rows], ensure_ascii=False) + "\n", encoding="utf-8")
    # Update static counts on homepage and catalogue banner; keep playlist-specific counts untouched.
    old_count = str(len(previous))
    new_count = str(len(rows))
    homepage = ROOT / "index.html"
    source = homepage.read_text(encoding="utf-8")
    source = source.replace(old_count + " videó", new_count + " videó")
    homepage.write_text(source, encoding="utf-8")
    catalog = ROOT / "catalog.js"
    source = catalog.read_text(encoding="utf-8")
    source = source.replace('id="video-count-banner">' + old_count, 'id="video-count-banner">' + new_count)
    if "bannerCount.textContent=songs.length" not in source:
        source = source.replace(
            "songs=data;renderPopular();applyFilters(false)",
            "songs=data;const bannerCount=document.querySelector('#video-count-banner');if(bannerCount)bannerCount.textContent=songs.length;renderPopular();applyFilters(false)",
        )
    catalog.write_text(source, encoding="utf-8")
    # Show any remaining references to removed IDs (other than immutable logs).
    for item in removed:
        for path in [ROOT / "index.html", ROOT / "catalog.js", ROOT / "halloween/index.html"]:
            if item["id"] in path.read_text(encoding="utf-8"):
                raise RuntimeError(f"Deleted video {item['id']} still linked in {path}")


if __name__ == "__main__":
    main()
