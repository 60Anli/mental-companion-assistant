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

# 确保上传目录存在
mkdir -p "$APP_DIR/backend-python/data/uploads"

# 数据库迁移（增列容错）
$CONDA_PYTHON -c "
from app.database import engine
from sqlalchemy import text
try:
    with engine.begin() as conn:
        conn.execute(text('ALTER TABLE chat_message ADD COLUMN media_urls TEXT NULL'))
    print('Migration OK: media_urls column added')
except Exception as e:
    err = str(e).lower()
    if '1060' in err or 'duplicate' in err:
        print('Migration SKIP: column already exists')
    else:
        print(f'Migration WARNING: {e}')
"

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
