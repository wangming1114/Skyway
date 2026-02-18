#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ADMIN_DIST="$ROOT/skyway-ui/dist"
NGINX_ADMIN="$ROOT/docker/volumes/nginx/admin"

echo "[管理端] 正在打包 skyway-ui ..."
cd "$ROOT/skyway-ui"
npm install
npm run build:prod

echo "[管理端] 复制到 docker/volumes/nginx/admin ..."
mkdir -p "$NGINX_ADMIN"
rm -rf "${NGINX_ADMIN:?}"/*
cp -R "$ADMIN_DIST"/* "$NGINX_ADMIN/"

echo "[管理端] 完成：$NGINX_ADMIN"
