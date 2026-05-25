#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/mindcare/mental-companion-assistant}"
WEB_DIR="${WEB_DIR:-/var/www/mental-companion}"
BRANCH="${BRANCH:-main}"

cd "$APP_DIR"
git pull --ff-only origin "$BRANCH"

mvn -f backend/pom.xml -DskipTests package

if [ -f frontend/package-lock.json ]; then
  npm --prefix frontend ci
else
  npm --prefix frontend install
fi
npm --prefix frontend run build

mkdir -p "$WEB_DIR"
rsync -a --delete frontend/dist/ "$WEB_DIR"/

systemctl restart mental-companion
nginx -t
systemctl reload nginx

echo "Deployment updated successfully."
