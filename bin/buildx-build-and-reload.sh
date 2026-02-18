#!/usr/bin/env bash
# 构建 skyway-admin 镜像并推送，然后停止并删除旧容器，拉取新镜像并启动
# 在项目根目录执行：bash bin/buildx-build-and-reload.sh

set -e
SCRIPT_DIR="$(dirname "$0")"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DOCKER_DIR="${ROOT}/docker"
CONTAINER_NAME="skyway-admin"

# 1. 构建并推送
bash "${SCRIPT_DIR}/buildx-build.sh"

# 2. 在 docker 目录下操作 compose
cd "${DOCKER_DIR}"
echo "[reload] 拉取新镜像 ..."
docker compose pull skyway-admin

echo "[reload] 停止并删除旧容器 ${CONTAINER_NAME} ..."
docker compose stop skyway-admin 2>/dev/null || true
docker rm -f "${CONTAINER_NAME}" 2>/dev/null || true

echo "[reload] 启动新容器 ..."
docker compose up -d skyway-admin

echo "Done. 容器 ${CONTAINER_NAME} 已使用新镜像运行。"
