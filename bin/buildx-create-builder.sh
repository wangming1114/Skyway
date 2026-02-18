#!/usr/bin/env bash
# 创建 buildx builder（无代理）
# 请用 bash 执行：bash bin/buildx-create-builder.sh

set -e
docker buildx rm skyway-builder 2>/dev/null || true
docker buildx create --name skyway-builder --use
echo "Done. Use: docker buildx build ..."
