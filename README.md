# 🧠 MindCare — 心理陪伴助手

基于 **Python / FastAPI / LangGraph** 的 AI 心理陪伴助手。采用 **ReAct Agent** 编排完整对话工作流：意图识别 → 混合 RAG 检索 → 风险分级 → LLM 回复 → 记忆更新 → 高危预警。

> ⚠️ 本系统为非诊断型心理陪伴工具，不提供医疗诊断，不替代医生、心理治疗师或紧急服务。

---

## 🏗 架构

```
┌──────────────┐     ┌────────────────────────────────────┐     ┌───────────┐
│  Vue 3 前端   │────▶│  FastAPI + LangGraph ReAct Agent   │────▶│  MySQL    │
│  Element Plus │     │                                    │     │  Redis    │
│  :5173        │     │  reason → act → observe → finish   │     │  Qdrant   │
└──────────────┘     └────────────────────────────────────┘     └───────────┘
```

**ReAct Agent 执行流程：**

```
用户消息
 → 保存用户消息
 → 知识库检索     ← Qdrant 向量 + BM25 关键词 + RRF 融合 + Cross-Encoder 重排
 → 加载记忆       ← Redis 短期(10轮) + MySQL 长期
 → 意图/风险识别  ← LLM 分类 + 规则命中
 → 生成回复       ← 按意图选择不同 System Prompt
 → 保存回复 & 记忆
 → 工作流记录     ← 非闲聊写入
 → 风险记录/邮件  ← 高风险触发
 → 返回响应
```

---

## 🛠 技术栈

| 层 | 技术 |
|----|------|
| **后端** | Python 3.10+ / FastAPI / LangGraph |
| **Agent** | ReAct StateGraph（reason → act → observe 循环） |
| **LLM** | 阿里云百炼 DashScope / 兼容 OpenAI / 支持 Ollama |
| **向量库** | Qdrant |
| **关系库** | MySQL 8.0 |
| **缓存** | Redis（短期记忆） |
| **前端** | Vue 3 / Element Plus / Vite |
| **分词** | jieba（中文 BM25） |
| **重排序** | BGE-Reranker (Cross-Encoder) |

---

## 🚀 快速启动

### 1. Clone

```bash
git clone https://github.com/60Anli/mental-companion-assistant.git
cd mental-companion-assistant
```

### 2. 启动基础服务

```bash
docker compose up -d mysql redis qdrant
```

### 3. 配置并启动后端

```bash
cd backend-python
pip install -r requirements.txt
cp .env.example .env
# 编辑 .env 至少填入 LLM_API_KEY

uvicorn app.main:app --host 0.0.0.0 --port 8080 --reload
```

### 4. 启动前端

```bash
cd ../frontend
npm install && npm run dev
```

浏览器打开 **http://localhost:5173**

### 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | 管理员 |
| `user` | `user123` | 学生 |

---

## 📦 依赖服务（Docker）

| 服务 | 端口映射 | 用途 |
|------|----------|------|
| MySQL | `3307:3306` | 用户、会话、记录、长期记忆 |
| Redis | `6379:6379` | 短期记忆（最近 10 轮） |
| Qdrant | `6335:6333` | 知识库向量存储与检索 |

---

## 🔧 配置（.env）

```env
# LLM（阿里云百炼）
LLM_API_KEY=sk-xxx
LLM_MODEL=qwen-plus
LLM_EMBEDDING_MODEL=text-embedding-v3   # 1024 维

# 数据库
MYSQL_URL=mysql+pymysql://root:1111@127.0.0.1:3307/mental_companion?charset=utf8mb4
REDIS_URL=redis://127.0.0.1:6379/0
QDRANT_URL=http://127.0.0.1:6335
QDRANT_VECTOR_SIZE=1024

# 邮件预警（QQ 邮箱）
MAIL_ENABLED=true
MAIL_USERNAME=your@qq.com
MAIL_PASSWORD=授权码
MAIL_ALERT_RECEIVER=admin@example.com
```

完整配置见 [`backend-python/.env.example`](backend-python/.env.example)

---

## 📡 API

### 公开

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/health` | 健康检查 |
| `POST` | `/api/auth/login` | 登录 |
| `POST` | `/api/auth/register` | 注册 |

### 需认证

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/chat/send` | 发送消息 |
| `GET` | `/api/chat/sessions` | 会话列表 |
| `GET` | `/api/chat/sessions/{id}/messages` | 消息历史 |
| `DELETE` | `/api/chat/sessions/{id}` | 删除会话 |

### 管理员

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/admin/users` | 用户列表 |
| `POST` | `/api/admin/knowledge/upload` | 上传知识文档 |
| `GET` | `/api/admin/knowledge/list` | 知识库列表 |
| `GET` | `/api/admin/workflow-records` | 工作流记录 |
| `GET` | `/api/admin/workflow-records/export` | 导出 Excel |
| `GET` | `/api/admin/risk-records` | 风险记录 |
| `GET` | `/api/admin/email/logs` | 邮件日志 |
| `POST` | `/api/admin/email/test` | 测试邮件 |

---

## ☁️ 部署到阿里云 ECS

### 环境准备

```bash
# Docker
curl -fsSL https://get.docker.com | bash

# Python (Miniconda)
wget https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh
bash Miniconda3-latest-Linux-x86_64.sh -b -p /opt/mindcare/miniconda3
/opt/mindcare/miniconda3/bin/conda create -n mindcare python=3.10 -y
```

### 部署

```bash
git clone https://github.com/60Anli/mental-companion-assistant.git /opt/mindcare/mental-companion-assistant
cd /opt/mindcare/mental-companion-assistant

# 基础服务
docker compose -f deploy/docker-compose.ecs.yml \
  --env-file /etc/mental-companion/mental-companion.env up -d

# Python 依赖
/opt/mindcare/miniconda3/envs/mindcare/bin/pip install -r backend-python/requirements.txt

# 配置 & 启动
cp backend-python/.env.example backend-python/.env
# 编辑 .env 填入生产环境值

sudo cp deploy/mental-companion.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now mental-companion
```

### Nginx

```bash
# 构建前端
cd frontend && npm install && npm run build

# Nginx 配置参考 deploy/nginx.mental-companion.conf
# 前端静态文件: frontend/dist/
# API 反代: 127.0.0.1:8080
```

---

## 📂 项目结构

```
mental-companion-assistant/
├── backend-python/          # Python FastAPI 后端
│   ├── app/
│   │   ├── main.py          # FastAPI 路由
│   │   ├── agent.py         # LangGraph ReAct Agent
│   │   ├── config.py        # 配置（.env 加载）
│   │   ├── database.py      # SQLAlchemy & 建表
│   │   ├── models.py        # ORM 模型
│   │   ├── schemas.py       # 请求/响应 Schema
│   │   ├── security.py      # JWT 认证
│   │   ├── services.py      # 业务服务
│   │   ├── llm.py           # LLM 客户端
│   │   ├── prompts.py       # Prompt 模板
│   │   ├── retrieval.py     # Qdrant + BM25 + RRF + 重排
│   │   └── utils.py         # 工具函数
│   ├── requirements.txt
│   └── .env.example
├── frontend/                # Vue 3 + Element Plus
│   ├── src/views/           # ChatView, LoginView, AdminView
│   └── vite.config.js
├── pretrain/                # QLoRA 微调
│   ├── scripts/             # train, evaluate, merge
│   └── configs/
├── deploy/                  # ECS 部署
│   ├── docker-compose.ecs.yml
│   ├── mental-companion.service
│   └── nginx.mental-companion.conf
├── docker/mysql/init.sql    # MySQL 初始化
├── docker-compose.yml       # 开发环境
└── README.md
```

---

## 📄 License

MIT
