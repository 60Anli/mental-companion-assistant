#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/mindcare/mental-companion-assistant}"
WEB_DIR="${WEB_DIR:-/var/www/mental-companion}"
BRANCH="${BRANCH:-main}"
CONDA_PYTHON="/opt/mindcare/miniconda3/envs/mindcare/bin/python"
CONDA_PIP="/opt/mindcare/miniconda3/envs/mindcare/bin/pip"

cd "$APP_DIR"
git pull --ff-only origin "$BRANCH"

# Python 依赖更新
$CONDA_PIP install -r backend-python/requirements.txt

# 前端构建
if [ -f frontend/package-lock.json ]; then
  npm --prefix frontend ci
else
  npm --prefix frontend install
fi
npm --prefix frontend run build

# 部署前端静态文件
mkdir -p "$WEB_DIR"
rsync -a --delete frontend/dist/ "$WEB_DIR"/

# 重启后端
systemctl restart mental-companion
nginx -t
systemctl reload nginx

echo "Deployment updated successfully."
