# pretrain：Qwen2.5-7B 心理场景 QLoRA 微调工程

这个目录提供一个可落地的离线微调流程，用于把心理问答/陪伴对话数据整理成 SFT 格式，对 `Qwen2.5-7B-Instruct` 做 QLoRA 微调，并将微调后的模型导出给 Ollama 使用。

注意：仓库不包含真实训练数据、模型权重、LoRA adapter 或 GGUF 文件。请把数据和训练产物放在被 `.gitignore` 忽略的目录中。

## 目标

- 提升心理场景下的情绪识别、风险表达识别和陪伴式回复稳定性。
- 保留主项目中的规则识别、RAG、短期记忆和长期记忆，不把安全判断完全交给微调模型。
- 通过评估脚本统计 emotion / risk 标签的 accuracy、macro-F1，并输出混淆矩阵。

## 目录结构

```text
pretrain
├─ configs
│  └─ qlora_qwen2_5_7b.yaml
├─ data
│  └─ sample_psychqa.jsonl
├─ scripts
│  ├─ clean_psychqa.py
│  ├─ train_qlora.py
│  ├─ evaluate_emotion.py
│  └─ merge_lora.py
├─ ollama
│  ├─ Modelfile.qwen2.5-mental
│  └─ export_to_ollama.md
├─ requirements.txt
└─ README.md
```

## 数据集假设

假设你有一个 `PsychQA` 校园心理对话数据集，原始数据大约 2w 条。经过脱敏、清洗、去重、标签修订后得到 2000 到 3000 条高质量样本。

推荐原始字段：

```json
{
  "question": "我最近压力很大，经常睡不着，怎么办？",
  "answer": "听起来你最近承受了不少压力...",
  "emotion": "anxiety",
  "risk_level": "LOW",
  "risk_type": "stress"
}
```

清洗后训练格式为 JSONL，每行一条：

```json
{
  "instruction": "识别用户情绪与风险，并给出非诊断性的心理陪伴回复。",
  "input": "我最近压力很大，经常睡不着，怎么办？",
  "output": "{\"emotion\":\"anxiety\",\"riskLevel\":\"LOW\",\"riskType\":\"stress\",\"reply\":\"听起来你最近承受了不少压力...\"}"
}
```

## 安装环境

建议 Linux + CUDA 环境。QLoRA 4bit 训练需要 NVIDIA GPU，24GB 显存通常可以跑小批量训练。

```bash
cd pretrain
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Windows 也可以准备脚本和数据，但实际 QLoRA 训练建议放到 Linux GPU 机器上跑。

## 1. 数据清洗

把原始 JSON/JSONL/CSV 转成 SFT JSONL：

```bash
python scripts/clean_psychqa.py \
  --input data/raw/psychqa_raw.jsonl \
  --output data/processed/psychqa_sft.jsonl
```

脚本会做：

- 去除空问题/空回答。
- 去重。
- 基础脱敏：手机号、邮箱、身份证号替换为占位符。
- 统一情绪标签、风险等级字段。
- 输出 `instruction/input/output` 格式。

## 2. QLoRA 训练

默认参数参考图中方案：

- 基座模型：`Qwen/Qwen2.5-7B-Instruct`
- 量化：4bit NF4
- LoRA rank：8
- LoRA alpha：16
- 学习率：2e-4
- 训练目标：只训练 LoRA adapter，不改基座模型权重

```bash
python scripts/train_qlora.py \
  --config configs/qlora_qwen2_5_7b.yaml \
  --train-file data/processed/psychqa_sft.jsonl \
  --output-dir outputs/qwen2.5-mental-lora
```

训练产物是几十 MB 到几百 MB 的 LoRA adapter，默认保存到：

```text
pretrain/outputs/qwen2.5-mental-lora
```

## 3. 评估

准备带标签的评估集：

```bash
python scripts/evaluate_emotion.py \
  --model-path Qwen/Qwen2.5-7B-Instruct \
  --adapter-path outputs/qwen2.5-mental-lora \
  --eval-file data/eval/psychqa_eval.jsonl \
  --report-file outputs/eval_report.json
```

报告包含：

- emotion accuracy
- emotion macro-F1
- riskLevel accuracy
- riskLevel macro-F1
- 混淆矩阵

不要在没有独立测试集的情况下宣称准确率提升。

## 4. 合并 LoRA adapter

将 LoRA adapter 合并到 HuggingFace 模型目录：

```bash
python scripts/merge_lora.py \
  --base-model Qwen/Qwen2.5-7B-Instruct \
  --adapter-path outputs/qwen2.5-mental-lora \
  --output-dir models/qwen2.5-mental-merged
```

## 5. 导出到 Ollama

见：

```text
ollama/export_to_ollama.md
```

最终在主项目中切换：

```yaml
llm:
  provider: ollama
  base-url: http://localhost:11434
  model: qwen2.5-mental:latest
  embedding-model: nomic-embed-text

fine-tune:
  enabled: true
  base-model: Qwen2.5
  adapter-type: QLoRA
  adapter-path: ./pretrain/outputs/qwen2.5-mental-lora
  ollama-model: qwen2.5-mental:latest
  training-profile: emotion-dialog-sft
```

