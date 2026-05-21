import argparse
import json
import re
from pathlib import Path

import torch
from peft import PeftModel
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from transformers import AutoModelForCausalLM, AutoTokenizer


SYSTEM_PROMPT = """你是心理陪伴助手的情绪与风险识别模块。
请只输出 JSON：
{"emotion":"anxiety","riskLevel":"LOW","riskType":"stress"}
"""


def extract_json(text: str) -> dict:
    match = re.search(r"\{.*\}", text, re.S)
    if not match:
        return {}
    try:
        return json.loads(match.group(0))
    except json.JSONDecodeError:
        return {}


def build_prompt(tokenizer, user_text: str) -> str:
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": user_text},
    ]
    if hasattr(tokenizer, "apply_chat_template"):
        return tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    return f"<system>{SYSTEM_PROMPT}</system>\n<user>{user_text}</user>\n<assistant>"


def main():
    parser = argparse.ArgumentParser(description="Evaluate emotion/risk classification of fine-tuned adapter.")
    parser.add_argument("--model-path", required=True, help="Base model path or HF id.")
    parser.add_argument("--adapter-path", required=True, help="LoRA adapter path.")
    parser.add_argument("--eval-file", required=True, help="JSONL eval file with input/meta labels.")
    parser.add_argument("--report-file", required=True, help="Output JSON report.")
    parser.add_argument("--max-new-tokens", type=int, default=128)
    args = parser.parse_args()

    tokenizer = AutoTokenizer.from_pretrained(args.model_path, trust_remote_code=True)
    model = AutoModelForCausalLM.from_pretrained(
        args.model_path,
        torch_dtype=torch.bfloat16,
        device_map="auto",
        trust_remote_code=True,
    )
    model = PeftModel.from_pretrained(model, args.adapter_path)
    model.eval()

    gold_emotions, pred_emotions = [], []
    gold_risks, pred_risks = [], []

    with open(args.eval_file, "r", encoding="utf-8") as file:
        for line in file:
            item = json.loads(line)
            user_text = item.get("input") or item.get("question") or ""
            meta = item.get("meta", item)
            prompt = build_prompt(tokenizer, user_text)
            inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
            with torch.no_grad():
                output = model.generate(
                    **inputs,
                    max_new_tokens=args.max_new_tokens,
                    do_sample=False,
                    pad_token_id=tokenizer.eos_token_id,
                )
            generated = tokenizer.decode(output[0][inputs["input_ids"].shape[1]:], skip_special_tokens=True)
            pred = extract_json(generated)

            gold_emotions.append(str(meta.get("emotion", "unknown")))
            pred_emotions.append(str(pred.get("emotion", "unknown")))
            gold_risks.append(str(meta.get("riskLevel") or meta.get("risk_level") or "LOW").upper())
            pred_risks.append(str(pred.get("riskLevel", "LOW")).upper())

    report = {
        "emotion_accuracy": accuracy_score(gold_emotions, pred_emotions),
        "risk_accuracy": accuracy_score(gold_risks, pred_risks),
        "emotion_report": classification_report(gold_emotions, pred_emotions, output_dict=True, zero_division=0),
        "risk_report": classification_report(gold_risks, pred_risks, output_dict=True, zero_division=0),
        "emotion_confusion_matrix": confusion_matrix(gold_emotions, pred_emotions).tolist(),
        "risk_confusion_matrix": confusion_matrix(gold_risks, pred_risks).tolist(),
    }
    Path(args.report_file).parent.mkdir(parents=True, exist_ok=True)
    Path(args.report_file).write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

