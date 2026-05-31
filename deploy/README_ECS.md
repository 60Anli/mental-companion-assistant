# 阿里云 ECS 上线部署说明

适用环境：Ubuntu 24.04、2C2G、在线大模型 API。

推荐部署方式：

- MySQL、Redis、Chroma 使用 Docker Compose。
- Spring Boot 后端使用 jar + systemd 常驻。
- Vue 前端使用 Nginx 静态托管，并通过 `/api` 反向代理到后端。
- 线上更新通过 GitHub `main` 分支同步。

## 1. 阿里云控制台准备

1. 给 ECS 绑定公网 IP 或弹性公网 IP。截图里如果只看到 `172.x.x.x` 私网 IP，外网无法访问。
2. 公网带宽至少设置 1 Mbps。
3. 安全组入方向开放：
   - `22`：SSH
   - `80`：HTTP
   - `443`：HTTPS，后续配置域名证书时使用
4. 不要开放 MySQL、Redis、Chroma 端口。生产 compose 已绑定到 `127.0.0.1`。

## 2. 登录服务器

```bash
ssh root@你的公网IP
```

## 3. 安装依赖

```bash
apt update
apt install -y git curl rsync nginx openjdk-21-jdk maven ca-certificates

curl -fsSL https://get.docker.com | bash
systemctl enable --now docker
apt install -y docker-compose-plugin

curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs
```

## 4. 拉取项目

```bash
mkdir -p /opt/mindcare
git clone https://github.com/60Anli/mental-companion-assistant.git /opt/mindcare/mental-companion-assistant
cd /opt/mindcare/mental-companion-assistant
```

如果仓库已经存在：

```bash
cd /opt/mindcare/mental-companion-assistant
git pull --ff-only origin main
```

## 5. 配置基础服务密钥

创建 Docker 基础服务环境变量文件：

```bash
mkdir -p /etc/mental-companion
nano /etc/mental-companion/infra.env
```

示例：

```env
MYSQL_ROOT_PASSWORD=请换成强密码
REDIS_PASSWORD=请换成强密码
```

启动 MySQL、Redis、Chroma、Qdrant：

```bash
cd /opt/mindcare/mental-companion-assistant
docker compose --env-file /etc/mental-companion/infra.env -f deploy/docker-compose.ecs.yml up -d
docker compose --env-file /etc/mental-companion/infra.env -f deploy/docker-compose.ecs.yml ps
```

## 6. 配置后端环境变量

```bash
nano /etc/mental-companion/mental-companion.env
```

示例：

```env
SPRING_PROFILES_ACTIVE=prod
MYSQL_URL=jdbc:mysql://127.0.0.1:3307/mental_companion?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=和 infra.env 中 MYSQL_ROOT_PASSWORD 一致

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=和 infra.env 中 REDIS_PASSWORD 一致
REDIS_DATABASE=0

CHROMA_BASE_URL=http://127.0.0.1:8000
QDRANT_URL=http://127.0.0.1:6333

# MCP 工具调用令牌。后端会用它调用本机 /mcp 端点；Nginx 默认不暴露 /mcp。
MCP_ACCESS_TOKEN=请换成随机长字符串

LLM_API_KEY=你的在线大模型APIKey
LLM_MODEL=qwen-plus
LLM_EMBEDDING_MODEL=text-embedding-v3

MAIL_ENABLED=true
MAIL_HOST=smtp.qq.com
MAIL_PORT=465
MAIL_USERNAME=你的发件邮箱
MAIL_PASSWORD=你的SMTP授权码
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=false
MAIL_SMTP_SSL_ENABLE=true
MAIL_FROM=你的发件邮箱
MAIL_ALERT_RECEIVER=你的接收邮箱
MAIL_TEACHER_NAME=李老师
MAIL_TEACHER_DEPARTMENT=学生工作部心理健康中心
```

## 7. 构建后端和前端

```bash
cd /opt/mindcare/mental-companion-assistant
mvn -f backend/pom.xml -DskipTests package
npm --prefix frontend ci
npm --prefix frontend run build
```

## 8. 配置 systemd 后端服务

```bash
cp deploy/mental-companion.service /etc/systemd/system/mental-companion.service
systemctl daemon-reload
systemctl enable --now mental-companion
journalctl -u mental-companion -f
```

看到 `Started MentalCompanionAssistantApplication` 类似日志后，后端启动成功。

## 9. 配置 Nginx 前端

```bash
mkdir -p /var/www/mental-companion
rsync -a --delete frontend/dist/ /var/www/mental-companion/

cp deploy/nginx.mental-companion.conf /etc/nginx/sites-available/mental-companion.conf
ln -sf /etc/nginx/sites-available/mental-companion.conf /etc/nginx/sites-enabled/mental-companion.conf
rm -f /etc/nginx/sites-enabled/default

nginx -t
systemctl reload nginx
```

浏览器访问：

```text
http://你的公网IP
```

## 10. 验证功能

1. 登录 `admin / admin123`。
2. 后台上传 `docs/*.md`。
3. 注册一个普通用户。
4. 使用普通用户测试知识库问答。
5. 输入高风险语句，检查 `email_alert_log` 和收件箱。
6. 查看后端日志中是否出现 `MCP tool call: knowledge_search`，用于确认工具链已经走标准 MCP 调用。

## 11. 本地更新后如何同步到线上

本地开发完成后：

```powershell
git add .
git commit -m "你的提交说明"
git push origin main
```

服务器上执行：

```bash
ssh root@你的公网IP
cd /opt/mindcare/mental-companion-assistant
bash deploy/update-ecs.sh
```

脚本会自动：

1. `git pull --ff-only origin main`
2. 重新打包后端 jar
3. 重新构建前端 dist
4. 同步前端到 `/var/www/mental-companion`
5. 重启后端 systemd 服务
6. reload Nginx

## 12. 常用排错命令

查看后端日志：

```bash
journalctl -u mental-companion -f
```

查看基础服务：

```bash
docker ps
docker logs mental-companion-mysql --tail 100
docker logs mental-companion-redis --tail 100
docker logs mental-companion-chroma --tail 100
```

查看端口：

```bash
ss -lntp
```

重启后端：

```bash
systemctl restart mental-companion
```

重启基础服务：

```bash
cd /opt/mindcare/mental-companion-assistant
docker compose --env-file /etc/mental-companion/infra.env -f deploy/docker-compose.ecs.yml restart
```
