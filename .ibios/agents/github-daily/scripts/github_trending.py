import json, sys
from datetime import date, timedelta
from urllib.request import Request, urlopen
from urllib.parse import quote

API = "https://api.github.com/search/repositories"
HDR = {"User-Agent": "ibios-github-digest", "Accept": "application/vnd.github+json"}

def search(q, n=5):
    url = f"{API}?q={quote(q)}&sort=stars&order=desc&per_page={n}"
    with urlopen(Request(url, headers=HDR), timeout=15) as r:
        items = json.load(r).get("items", [])
    return [{"name": i["full_name"], "stars": i["stargazers_count"],
             "desc": (i.get("description") or "")[:120], "url": i["html_url"]} for i in items]

today = date.today()
yday = (today - timedelta(days=1)).isoformat()
month0 = today.replace(day=1).isoformat()
out = {
    "today": search(f"created:>{yday}"),
    "month": search(f"created:>{month0}"),
    "ai":    search(f"created:>{month0} topic:ai"),
}
json.dump(out, sys.stdout, ensure_ascii=False)
