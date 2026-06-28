#!/usr/bin/env bash
# 构建 skyway-admin 镜像到本地 Docker，然后停止并删除旧容器，使用本地新镜像启动
# 在项目根目录执行：bash bin/buildx-build-and-reload-local.sh

set -e
SCRIPT_DIR="$(dirname "$0")"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DOCKER_DIR="${ROOT}/docker"
CONTAINER_NAME="skyway-admin"

REGISTRY="${DOCKER_REGISTRY:-crpi-kf13onfj0v8f6jax.cn-shanghai.personal.cr.aliyuncs.com}"
NAMESPACE="${DOCKER_NAMESPACE:-ming121}"
IMAGE="${REGISTRY}/${NAMESPACE}/skyway-admin:latest"

# 1. 构建并加载到本地 Docker，不推送
cd "${ROOT}"
echo "Buildx build and load: skyway-admin -> ${IMAGE}"
docker buildx build --platform linux/amd64 \
  -t "${IMAGE}" \
  --load \
  -f "${DOCKER_DIR}/Dockerfile.skyway" .

# 2. 在 docker 目录下操作 compose
cd "${DOCKER_DIR}"
echo "[reload] 停止并删除旧容器 ${CONTAINER_NAME} ..."
docker compose stop skyway-admin 2>/dev/null || true
docker rm -f "${CONTAINER_NAME}" 2>/dev/null || true

echo "[reload] 启动新容器 ..."
docker compose up -d skyway-admin

echo "Done. 容器 ${CONTAINER_NAME} 已使用本地新镜像运行。"
