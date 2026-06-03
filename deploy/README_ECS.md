# 阿里云 ECS 部署说明

适用环境：Ubuntu 24.04、2C2G、在线 LLM API（阿里云百炼）。

## 环境准备

```bash
# Docker
curl -fsSL https://get.docker.com | bash

# Miniconda (Python 3.10)
wget https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh
bash Miniconda3-latest-Linux-x86_64.sh -b -p /opt/mindcare/miniconda3
/opt/mindcare/miniconda3/bin/conda create -n mindcare python=3.10 -y
```

## 部署步骤

```bash
# 1. Clone
git clone https://github.com/60Anli/mental-companion-assistant.git /opt/mindcare/mental-companion-assistant
cd /opt/mindcare/mental-companion-assistant

# 2. 创建环境变量文件
sudo mkdir -p /etc/mental-companion
sudo tee /etc/mental-companion/mental-companion.env << 'EOF'
MYSQL_ROOT_PASSWORD=<强密码>
REDIS_PASSWORD=<强密码>
EOF

# 3. 启动基础服务
docker compose -f deploy/docker-compose.ecs.yml \
  --env-file /etc/mental-companion/mental-companion.env up -d

# 4. 安装 Python 依赖
/opt/mindcare/miniconda3/envs/mindcare/bin/pip install -r backend-python/requirements.txt

# 5. 配置后端
cp backend-python/.env.example backend-python/.env
# 编辑 backend-python/.env 填入 LLM_API_KEY、MYSQL_URL、REDIS_URL 等

# 6. 启动后端 (systemd)
sudo cp deploy/mental-companion.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now mental-companion
```

## Nginx

```bash
# 构建前端
cd /opt/mindcare/mental-companion-assistant/frontend
npm install && npm run build

# 安装 Nginx
sudo apt install nginx -y
sudo cp /opt/mindcare/mental-companion-assistant/deploy/nginx.mental-companion.conf \
  /etc/nginx/sites-available/mental-companion
sudo ln -s /etc/nginx/sites-available/mental-companion /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

# HTTPS (Certbot)
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your-domain.com
```

## 验证

```bash
# 后端
curl http://127.0.0.1:8080/api/health

# 前端（Nginx）
curl https://your-domain.com

# 日志
sudo journalctl -u mental-companion -f
```
