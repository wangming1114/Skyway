#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
C_DIST="$ROOT/skyway-c-ui/dist"
NGINX_C="$ROOT/docker/volumes/nginx/c"

echo "[C端] 正在打包 skyway-c-ui ..."
cd "$ROOT/skyway-c-ui"
npm install
npm run build

echo "[C端] 复制到 docker/volumes/nginx/c ..."
mkdir -p "$NGINX_C"
rm -rf "${NGINX_C:?}"/*
cp -R "$C_DIST"/* "$NGINX_C/"

echo "[C端] 完成：$NGINX_C"
