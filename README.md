# mental-companion-assistant

心理陪伴助手 MVP，面向 AI Agent 工作流展示场景，覆盖心理陪伴对话、心理知识问答、咨询分流、高风险识别、邮件预警、Excel 留痕与后台管理。

系统定位为非诊断型心理陪伴与咨询分流工具，不提供医疗诊断，不替代医生、心理治疗师或紧急服务。

## 核心能力

- 大模型接入：支持 Ollama 本地模型与 OpenAI-compatible API，可通过配置切换模型提供方。
- Agent 工作流编排：Python 版使用 LangGraph 实现 ReAct 风格智能体，以 Thought -> Action -> Observation 循环执行检索、分类、风险判断、回复生成和工具调用。
- Hybrid RAG：Java 版集成 Chroma + Lucene BM25 + RRF；Python 版升级为 Qdrant 向量召回 + BM25 关键词召回 + RRF 融合 + Cross-Encoder 重排序。
- 记忆机制：Redis 保存最近 10 轮短期记忆，MySQL 保存长期记忆摘要，增强多轮对话连续性。
- 风险与情绪识别：意图识别分为闲聊、咨询、高风险三类；咨询场景继续识别正常、焦虑、失落、高风险等情绪状态，高风险结果会触发邮件预警。
- 标准 MCP 工具调用：Java 版后端作为 Agent Host，通过 MCP Client 调用 `/mcp` 暴露的工具；Python 版后端以 FastAPI 服务实现同等 Agent 工作流。
- 后台管理：支持知识库上传、工作流记录查看、风险记录查看、邮件日志查看和 Excel 导出。
- 普通用户注册：支持学生自助注册普通账号，登记姓名、学院和邮箱，管理员账号仍由系统初始化或后台维护。
- 微调工程脚手架：`pretrain/` 提供 Qwen2.5-7B QLoRA 训练、评估、LoRA 合并与 Ollama 部署示例。

## 技术栈

- 后端：Java 17 / Spring Boot 3，Python / FastAPI / LangGraph
- 数据：MySQL, Redis, Qdrant, Chroma, Lucene
- AI：Ollama, OpenAI-compatible API, Prompt Engineering, Hybrid RAG, Cross-Encoder Reranker
- 工具：标准 MCP JSON-RPC, Spring Mail, EasyExcel, Docker Compose
- 前端：Vue3, Element Plus, Vite

## 工作流

```mermaid
flowchart TD
    A[用户输入] --> B[保存用户消息]
    B --> R[LangGraph ReAct Agent]
    R --> C[Hybrid RAG 检索]
    C --> C1[Qdrant 向量召回]
    C --> C2[BM25 关键词召回]
    C1 --> C3[RRF 融合排序]
    C2 --> C3
    C3 --> C4[Cross-Encoder 重排序]
    C4 --> D[加载短期记忆和长期记忆]
    D --> E[LLM 意图识别: 闲聊/咨询/高风险]
    E --> F{意图分流}
    F -->|闲聊| G[自然聊天回复]
    F -->|咨询| H[情绪识别: 正常/焦虑/失落/高风险]
    H -->|正常/焦虑/失落| I[心理陪伴与知识库增强回复]
    H -->|高风险| J[安全危机回复]
    F -->|高风险| J
    I --> K[写入 workflow_record]
    J --> K
    K --> L[追加写入 Excel]
    J --> M[写入 risk_record]
    M --> N[发送邮件预警]
    G --> O[返回响应]
    L --> O
    N --> O
```

## MCP 工具调用

Java 版项目已将原来的本地工具注册调用升级为标准 MCP JSON-RPC 工具调用。主后端既是 Agent Host，也内置 MCP Server 端点 `/mcp`；`ChatWorkflowService` 不再直接调用工具 Bean，而是通过 MCP Client 调用标准工具名：

- `knowledge_search`：执行 Hybrid RAG 检索。
- `save_workflow_record`：写入非闲聊工作流记录。
- `append_workflow_excel`：追加写入 Excel。
- `save_risk_record`：写入高风险记录。
- `send_email_alert`：发送高风险邮件预警并写日志。

上线时可通过 `MCP_ACCESS_TOKEN` 为 `/mcp` 调用增加访问令牌；如果后续要拆成独立 MCP Server，只需要把 `MCP_CLIENT_URL` 改成独立服务地址。

```yaml
mcp:
  enabled: true
  client:
    url: http://127.0.0.1:8080/mcp
    access-token: ${MCP_ACCESS_TOKEN:}
```

## 混合检索设计

Python 版聊天入口由 LangGraph ReAct Agent 编排。Agent 状态图包含 `reason`、`act`、`observe` 三类节点：`reason` 根据当前状态决定下一步动作，`act` 执行对应工具，`observe` 记录结果并回到 `reason`，直到构造最终响应。返回结果中的 `reactTrace` 会记录每一步 thought/action/observation，便于演示 Agent 推理链路。

Python 版知识库文档上传后会被切片，并同时写入 MySQL 和 Qdrant：

- MySQL `knowledge_document` 保存原始文档。
- MySQL `knowledge_chunk` 保存文档切片。
- Qdrant 保存切片向量，用于 Dense Retrieval。
- BM25 从 MySQL 切片构建关键词召回候选。

查询时系统会同时执行 Qdrant 向量召回和 BM25 关键词召回，先用 RRF 融合候选，再使用 Cross-Encoder 对候选片段进行 query-document 相关性重排序，默认取 3 个片段拼入 Prompt 生成回答。相比单纯向量检索，该 pipeline 对心理学术语、关键词明确的问题和语义表达变化更稳定。

相关配置：

```env
QDRANT_URL=http://127.0.0.1:6333
QDRANT_VECTOR_SIZE=1536
RAG_DENSE_TOP_K=12
RAG_KEYWORD_TOP_K=12
RAG_RRF_K=60
RAG_FINAL_TOP_K=3
RERANKER_ENABLED=true
CROSS_ENCODER_MODEL=BAAI/bge-reranker-base
```

## 模型配置

默认使用 OpenAI-compatible API，也可以切换为 Ollama 本地模型。

```yaml
llm:
  provider: openai
  base-url: https://dashscope.aliyuncs.com/compatible-mode
  api-key: ${LLM_API_KEY:}
  model: qwen-plus
  embedding-model: text-embedding-v3
```

Ollama 示例：

```yaml
llm:
  provider: ollama
  base-url: http://localhost:11434
  model: qwen2.5-mental:latest
  embedding-model: nomic-embed-text
```

## 快速启动

### 1. 启动基础服务

```powershell
docker compose up -d mysql redis qdrant
```

首次运行会拉取 MySQL、Redis 和 Qdrant 镜像。如果 Docker Hub 网络超时，重复执行同一条命令即可继续复用已下载的镜像层。

### 2. 配置本地密钥

Python 后端在 `backend-python/.env` 中配置数据库、Redis、Qdrant、模型 API Key 和邮件信息。该文件已加入 `.gitignore`，不会提交到 GitHub。

最小配置示例：

```env
MYSQL_URL=mysql+pymysql://root:1111@127.0.0.1:3307/mental_companion?charset=utf8mb4
REDIS_URL=redis://127.0.0.1:6379/0
QDRANT_URL=http://127.0.0.1:6333
QDRANT_VECTOR_SIZE=1536
LLM_PROVIDER=openai
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
LLM_API_KEY=your_api_key
LLM_MODEL=qwen-plus
LLM_EMBEDDING_MODEL=text-embedding-v3
```

如果启动 Java 后端，则继续使用 `backend/src/main/resources/application-local.yml`。

### 3. 启动 Python 后端

```powershell
cd backend-python
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8080 --reload
```

后端地址：

```text
http://localhost:8080
```

### 4. 启动 Java 后端

```powershell
C:\Users\83848\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd -f backend\pom.xml -DskipTests package
D:\JAVA\jdk21\bin\java.exe -jar backend\target\mental-companion-assistant-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

后端地址：

```text
http://localhost:8080
```

Python 后端和 Java 后端都使用 8080 端口，二选一启动即可。

### 5. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173
```

## 测试账号

- 管理员：`admin / admin123`
- 普通用户：`user / user123`

登录页支持普通用户注册。注册用户默认角色为 `USER`，不会获得管理员权限。

## 演示流程

1. 管理员登录后台。
2. 上传 `data/sample-knowledge.md` 到知识库。
3. 进入聊天页，依次测试闲聊、心理咨询、知识问答和高风险输入。
4. 在右侧工作流面板观察意图类型、风险等级、RAG 命中、Excel 写入和邮件动作。
5. 回到后台查看工作流记录、风险记录、邮件日志，并导出 Excel。

## 典型用例

```text
你好，今天有点无聊。
```

预期：闲聊，自然回复，不写 Excel，不发邮件。

```text
我最近压力很大，经常睡不着，感觉很累。
```

预期：咨询，并进一步识别情绪状态为焦虑或失落；写入 workflow_record 和 Excel，不发邮件。

```text
长期焦虑时可以用哪些放松方法？
```

预期：咨询类知识问答，基于 Hybrid RAG 返回知识库增强回答，情绪状态为正常。

```text
我真的活不下去了，想结束这一切。
```

预期：高风险，写入 workflow_record、risk_record 和 Excel，并触发邮件预警。

## 主要接口

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/chat/send`
- `POST /api/admin/knowledge/upload`
- `GET /api/admin/knowledge/list`
- `GET /api/admin/workflow-records`
- `GET /api/admin/workflow-records/export`
- `GET /api/admin/risk-records`
- `GET /api/admin/email/logs`
