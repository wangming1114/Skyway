#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
image=skyway-ws-qa:local
container=skyway-ws-qa-$$
docker build -t "$image" skyway-admin/src/test/resources/ws
docker run --rm \
    -v "$PWD/skyway-admin/src/main/resources/ws/vless-ws.sh:/qa/vless-ws.sh:ro" \
    -v "$PWD/skyway-admin/src/test/resources/ws/check-caddy-install.sh:/qa/check-caddy-install.sh:ro" \
    alpine:3.22 sh -ec 'apk add --no-cache bash curl ca-certificates openssl openrc procps-ng python3 && exec bash /qa/check-caddy-install.sh'
docker run -d --name "$container" --network none --cap-add NET_ADMIN "$image" >/dev/null
trap 'docker rm -f "$container" >/dev/null' EXIT
SKYWAY_WS_TEST_CONTAINER=$container mvn -pl skyway-admin -am test
