import json
from functools import lru_cache
from pathlib import Path
from typing import TypedDict


class TopicMeta(TypedDict):
    id: str
    subject: str
    grade: int | None
    term: str
    unit: str
    title: str


@lru_cache(maxsize=1)
def load_topic_meta_map() -> dict[str, TopicMeta]:
    path = Path(__file__).resolve().parent / "curriculum_meta.json"
    if not path.exists():
        return {}
    raw = json.loads(path.read_text(encoding="utf-8"))
    out: dict[str, TopicMeta] = {}
    for item in raw.get("topics", []):
        tid = str(item.get("id", "")).strip()
        if not tid:
            continue
        out[tid] = TopicMeta(
            id=tid,
            subject=str(item.get("subject", "")),
            grade=item.get("grade"),
            term=str(item.get("term", "")),
            unit=str(item.get("unit", "")),
            title=str(item.get("title", tid)),
        )
    return out
