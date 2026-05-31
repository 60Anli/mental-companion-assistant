import json
from datetime import date, datetime
from pathlib import Path
from typing import Any


def camelize(name: str) -> str:
    parts = name.split("_")
    return parts[0] + "".join(part.capitalize() for part in parts[1:])


def model_to_dict(model: Any) -> dict:
    data = {}
    for column in model.__table__.columns:
        value = getattr(model, column.name)
        if isinstance(value, (datetime, date)):
            value = value.isoformat(sep=" ")
        data[camelize(column.name)] = value
    return data


def safe_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, default=str)


def read_json(text: str | None, default: Any) -> Any:
    if not text:
        return default
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return default


def ensure_parent(path: str | Path) -> Path:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    return target
