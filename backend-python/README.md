# Python Backend — LangGraph ReAct Agent

心理陪伴助手 Python/FastAPI 后端，使用 LangGraph 实现 ReAct 智能体。

## Agent 执行模式

```
reason: 判断下一步需要做什么
→ act: 执行工具（知识检索、分类、回复生成、记录写入、邮件预警）
→ observe: 记录观察结果，回到 reason
→ finish: 生成最终响应
```

## 检索链路

```
用户问题
→ Qdrant 向量召回 (top 12)
→ BM25 关键词召回 (top 12)
→ RRF 融合排序
→ Cross-Encoder 重排序
→ 取 top 3 拼接 RAG 上下文
→ LLM 意图/风险识别 & 回复生成
```

## 本地启动

```bash
cd backend-python
pip install -r requirements.txt
cp .env.example .env
# 编辑 .env，至少填入 LLM_API_KEY

uvicorn app.main:app --host 0.0.0.0 --port 8080 --reload
```

## 依赖服务

确保以下 Docker 服务已启动：

```bash
cd ..
docker compose up -d mysql redis qdrant
```

## 检索配置

```env
RAG_DENSE_TOP_K=12       # 向量召回候选数
RAG_KEYWORD_TOP_K=12     # BM25 关键词召回候选数
RAG_RRF_K=60             # RRF 融合常数
RAG_FINAL_TOP_K=3        # 最终返回给 LLM 的片段数
RERANKER_ENABLED=false   # 生产建议关闭以节省显存
```

`QDRANT_VECTOR_SIZE` 必须与嵌入模型输出维度一致：
- 阿里云 `text-embedding-v3` → **1024**
- OpenAI `text-embedding-ada-002` → **1536**
- Ollama `nomic-embed-text` → **768**
