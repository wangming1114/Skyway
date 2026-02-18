#!/bin/bash
# skyway-admin 使用 buildx 构建并推送到阿里云
# 在项目根目录执行：./bin/buildx-build.sh
# 首次：复制 docker/.example 为 docker/.registry.env 并填入密码

set -e
SCRIPT_DIR="$(dirname "$0")"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DOCKER_DIR="${ROOT}/docker"
cd "${ROOT}"

REGISTRY="${DOCKER_REGISTRY:-crpi-kf13onfj0v8f6jax.cn-shanghai.personal.cr.aliyuncs.com}"
NAMESPACE="${DOCKER_NAMESPACE:-ming121}"
IMAGE="${REGISTRY}/${NAMESPACE}/skyway-admin:latest"

# 使用本地凭证登录（.registry.env 在 docker/ 下，勿提交）
if [ -f "${DOCKER_DIR}/.registry.env" ]; then
  set -a
  source "${DOCKER_DIR}/.registry.env"
  set +a
  echo "${DOCKER_REGISTRY_PASSWORD}" | docker login --username="${DOCKER_REGISTRY_USER}" --password-stdin "${REGISTRY}" 2>/dev/null || true
fi

echo "Buildx build and push: skyway-admin -> ${IMAGE}"
docker buildx build --platform linux/amd64 \
  -t "${IMAGE}" \
  --push \
  -f "${DOCKER_DIR}/Dockerfile.skyway" .

echo "Done. Image: ${IMAGE}"
