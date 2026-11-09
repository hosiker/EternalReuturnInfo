#!/usr/bin/env python3
"""
게임 방송(스트리머) 영상 목록을 YouTube 에서 받아 JSON 파일로 만든다.
GitHub Actions(.github/workflows/update-videos.yml)가 6시간마다 실행해서 data 브랜치에 올리고,
앱은 YouTube 를 직접 검색하지 않고 이 JSON 을 읽는다. (검색은 호출당 한도 100 이라 이용자 수에 비례해 한도가 닳는 문제를 없애려는 것)

사용법:  YOUTUBE_API_KEY=... python tools/fetch_videos.py [출력 폴더]   (기본 out)
출력:    streamer_date.json (최신순), streamer_viewCount.json (최근 30일 조회순)
실패하면(키 오류, 한도 초과 등) 아무 파일도 쓰지 않고 종료 코드 1 로 끝난다 → 이전 목록이 그대로 남는다.

키워드·쇼츠 판정은 앱의 VideoFilter.kt / VideoKind.kt 와 같은 규칙이다. 바꿀 때는 함께 바꿀 것.
표준 라이브러리만 쓴다.
"""
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone

API = "https://www.googleapis.com/youtube/v3"
SEARCH_QUERY = "이터널리턴|이터널 리턴|eternal return|블서|영회"
KEYWORDS = ["이터널리턴", "eternalreturn", "이터널", "리턴", "블서", "블랙서바이벌", "영회", "영원회귀", "이리"]
MAX_ITEMS = 20


def normalize(text):
    return "".join(ch for ch in text.lower() if not ch.isspace())


def matches(title, tags):
    text = normalize((title or "") + " " + " ".join(tags or []))
    return any(k in text for k in KEYWORDS)


def duration_seconds(iso):
    m = re.fullmatch(r"P(?:(\d+)D)?T?(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?", iso or "")
    if not m or not any(m.groups()):
        return None
    d, h, mi, s = (int(g) if g else 0 for g in m.groups())
    return d * 86400 + h * 3600 + mi * 60 + s


def is_short(duration, title, tags):
    sec = duration_seconds(duration)
    if sec is None or sec <= 0:
        return False
    if sec <= 60:
        return True
    if sec > 180:
        return False
    text = ((title or "") + " " + " ".join(tags or [])).lower()
    return "#short" in text or any(t.lower() == "shorts" for t in (tags or []))


def call(path, params, key):
    query = urllib.parse.urlencode({**params, "key": key})
    req = urllib.request.Request(f"{API}/{path}?{query}", headers={"User-Agent": "er-info-video-feed"})
    try:
        with urllib.request.urlopen(req, timeout=30) as res:
            return json.load(res)
    except urllib.error.HTTPError as e:
        # 키는 주소에 들어 있으므로 오류 본문만 출력한다
        raise SystemExit(f"YouTube {path} 실패 HTTP {e.code}: {e.read().decode('utf-8', 'replace')[:300]}")


def build(sort, key, now):
    params = {
        "part": "snippet", "type": "video", "q": SEARCH_QUERY, "order": sort,
        "maxResults": 50, "relevanceLanguage": "ko",
    }
    if sort == "viewCount":  # 오래된 인기 영상만 나오지 않도록 최근 30일
        params["publishedAfter"] = (now - timedelta(days=30)).strftime("%Y-%m-%dT%H:%M:%SZ")
    found = call("search", params, key)
    ids = [i["id"]["videoId"] for i in found.get("items", []) if i.get("id", {}).get("videoId")]
    if not ids:
        return []
    details = call("videos", {"part": "snippet,contentDetails", "id": ",".join(ids)}, key)
    by_id = {d["id"]: d for d in details.get("items", []) if d.get("id")}
    items = []
    for vid in ids:
        d = by_id.get(vid)
        if not d:
            continue
        sn = d.get("snippet", {})
        if not matches(sn.get("title"), sn.get("tags")):
            continue
        thumbs = sn.get("thumbnails", {})
        thumb = (thumbs.get("medium") or thumbs.get("high") or thumbs.get("default") or {}).get("url")
        items.append({
            "id": vid,
            "title": sn.get("title"),
            "thumbnail": thumb,
            "isShort": is_short(d.get("contentDetails", {}).get("duration"), sn.get("title"), sn.get("tags")),
        })
        if len(items) >= MAX_ITEMS:
            break
    print(f"{sort}: 검색 {len(ids)}개 중 {len(items)}개 통과")
    return items


def main():
    key = os.environ.get("YOUTUBE_API_KEY", "").strip()
    if not key:
        raise SystemExit("YOUTUBE_API_KEY 가 없습니다.")
    out = sys.argv[1] if len(sys.argv) > 1 else "out"
    now = datetime.now(timezone.utc)
    feeds = {sort: build(sort, key, now) for sort in ("date", "viewCount")}
    # 둘 중 하나라도 비어 있으면 잘못된 응답일 수 있으니 덮어쓰지 않는다
    if any(not items for items in feeds.values()):
        raise SystemExit("빈 목록이 있어 저장하지 않습니다.")
    os.makedirs(out, exist_ok=True)
    stamp = int(time.time() * 1000)
    for sort, items in feeds.items():
        with open(os.path.join(out, f"streamer_{sort}.json"), "w", encoding="utf-8") as f:
            json.dump({"generatedAt": stamp, "items": items}, f, ensure_ascii=False, indent=1)
    print("저장 완료:", out)


if __name__ == "__main__":
    main()
