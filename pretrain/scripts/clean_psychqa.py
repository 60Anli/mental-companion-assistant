import argparse
import csv
import hashlib
import json
import re
from pathlib import Path


DEFAULT_INSTRUCTION = "识别用户情绪与风险，并给出非诊断性的心理陪伴回复。"

EMOTION_ALIASES = {
    "焦虑": "anxiety",
    "anxiety": "anxiety",
    "压力": "stress",
    "stress": "stress",
    "悲伤": "sadness",
    "sadness": "sadness",
    "绝望": "despair",
    "despair": "despair",
    "愤怒": "anger",
    "anger": "anger",
    "孤独": "loneliness",
    "loneliness": "loneliness",
}

RISK_LEVELS = {"LOW", "MEDIUM", "HIGH"}


def mask_sensitive(text: str) -> str:
    text = re.sub(r"1[3-9]\d{9}", "[PHONE]", text)
    text = re.sub(r"[\w.\-+]+@[\w.\-]+\.\w+", "[EMAIL]", text)
    text = re.sub(r"\b\d{17}[\dXx]\b", "[ID_CARD]", text)
    return text.strip()


def normalize_emotion(value: str) -> str:
    if not value:
        return "unknown"
    return EMOTION_ALIASES.get(value.strip().lower(), value.strip().lower())


def normalize_risk_level(value: str) -> str:
    if not value:
        return "LOW"
    normalized = value.strip().upper()
    return normalized if normalized in RISK_LEVELS else "LOW"


def read_rows(path: Path):
    suffix = path.suffix.lower()
    if suffix == ".jsonl":
        with path.open("r", encoding="utf-8") as file:
            for line in file:
                line = line.strip()
                if line:
                    yield json.loads(line)
    elif suffix == ".json":
        data = json.loads(path.read_text(encoding="utf-8"))
        if isinstance(data, list):
            yield from data
        else:
            yield data
    elif suffix == ".csv":
        with path.open("r", encoding="utf-8-sig", newline="") as file:
            yield from csv.DictReader(file)
    else:
        raise ValueError(f"Unsupported input suffix: {suffix}")


def pick(row: dict, *keys: str) -> str:
    for key in keys:
        value = row.get(key)
        if value is not None and str(value).strip():
            return str(value).strip()
    return ""


def fingerprint(question: str, answer: str) -> str:
    return hashlib.sha256(f"{question}\n{answer}".encode("utf-8")).hexdigest()


def convert_row(row: dict) -> dict | None:
    question = mask_sensitive(pick(row, "question", "query", "input", "user", "prompt"))
    answer = mask_sensitive(pick(row, "answer", "response", "output", "assistant", "completion"))
    if not question or not answer:
        return None

    emotion = normalize_emotion(pick(row, "emotion", "emotion_label", "label"))
    risk_level = normalize_risk_level(pick(row, "risk_level", "riskLevel", "risk"))
    risk_type = pick(row, "risk_type", "riskType") or "none"

    output = {
        "emotion": emotion,
        "riskLevel": risk_level,
        "riskType": risk_type,
        "reply": answer,
    }
    return {
        "instruction": DEFAULT_INSTRUCTION,
        "input": question,
        "output": json.dumps(output, ensure_ascii=False),
        "meta": {
            "emotion": emotion,
            "riskLevel": risk_level,
            "riskType": risk_type,
        },
    }


def main():
    parser = argparse.ArgumentParser(description="Clean PsychQA-like data into SFT JSONL.")
    parser.add_argument("--input", required=True, help="Raw JSON/JSONL/CSV file.")
    parser.add_argument("--output", required=True, help="Output SFT JSONL file.")
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    seen = set()
    kept = 0
    with output_path.open("w", encoding="utf-8") as out:
        for row in read_rows(input_path):
            item = convert_row(row)
            if item is None:
                continue
            fp = fingerprint(item["input"], item["output"])
            if fp in seen:
                continue
            seen.add(fp)
            out.write(json.dumps(item, ensure_ascii=False) + "\n")
            kept += 1

    print(f"saved={kept} output={output_path}")


if __name__ == "__main__":
    main()

