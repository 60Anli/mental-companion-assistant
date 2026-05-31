# Python Backend

这是心理陪伴助手的 Python/FastAPI 实现，复用现有前端 `/api` 路径和 MySQL 表结构，并使用 LangGraph 实现 ReAct 风格智能体。

Agent 执行模式：

```text
reason: 判断下一步需要做什么
-> act: 调用知识检索、分类、回复生成、记录写入、邮件预警等工具
-> observe: 写入观察结果并回到 reason
-> finish: 生成最终响应
```

检索链路升级为：

```text
用户问题
-> Qdrant 向量召回
-> BM25 关键词召回
-> RRF 融合
-> Cross-Encoder 重排序
-> 拼接 RAG 上下文
-> LLM 意图/风险识别和回复生成
```

## 启动

```powershell
cd backend-python
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
```

编辑 `.env`，至少填好：

```env
MYSQL_URL=mysql+pymysql://root:1111@127.0.0.1:3307/mental_companion?charset=utf8mb4
REDIS_URL=redis://127.0.0.1:6379/0
QDRANT_URL=http://127.0.0.1:6333
QDRANT_VECTOR_SIZE=1536
LLM_API_KEY=你的在线大模型Key
```

`QDRANT_VECTOR_SIZE` 必须和嵌入模型输出维度一致。比如在线 `text-embedding-v3` 常用 1536 维；如果切到 Ollama 的 `nomic-embed-text`，需要按实际维度调整，并在新 collection 上重建知识库。

启动基础服务：

```powershell
cd ..
docker compose up -d mysql redis qdrant
```

启动 Python 后端：

```powershell
cd backend-python
uvicorn app.main:app --host 0.0.0.0 --port 8080 --reload
```

前端仍然启动：

```powershell
cd frontend
npm run dev
```

## 检索配置

```env
RAG_DENSE_TOP_K=12
RAG_KEYWORD_TOP_K=12
RAG_RRF_K=60
RAG_FINAL_TOP_K=3
RERANKER_ENABLED=true
CROSS_ENCODER_MODEL=BAAI/bge-reranker-base
```

第一次使用 Cross-Encoder 会下载模型。服务器网络不方便时，可以提前把模型下载到本地目录，然后把 `CROSS_ENCODER_MODEL` 改成本地路径。
