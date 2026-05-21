# 合并模型导出到 Ollama

下面步骤假设你已经完成 QLoRA 训练，并用 `scripts/merge_lora.py` 得到了合并后的 HuggingFace 模型目录：

```text
pretrain/models/qwen2.5-mental-merged
```

## 1. 准备 llama.cpp

```bash
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp
cmake -B build
cmake --build build --config Release
```

## 2. 转 GGUF

不同版本 llama.cpp 脚本名称可能略有变化，常见命令如下：

```bash
python convert_hf_to_gguf.py \
  ../pretrain/models/qwen2.5-mental-merged \
  --outfile ../pretrain/models/qwen2.5-mental-f16.gguf \
  --outtype f16
```

## 3. 量化为 Q4_K_M

```bash
./build/bin/llama-quantize \
  ../pretrain/models/qwen2.5-mental-f16.gguf \
  ../pretrain/models/qwen2.5-mental-q4_k_m.gguf \
  Q4_K_M
```

## 4. 创建 Ollama 模型

把 `pretrain/ollama/Modelfile.qwen2.5-mental` 里的 `FROM` 改成 GGUF 文件路径，然后执行：

```bash
ollama create qwen2.5-mental:latest -f pretrain/ollama/Modelfile.qwen2.5-mental
ollama run qwen2.5-mental:latest
```

## 5. 主项目切换配置

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

