#!/usr/bin/env bash
# Runs inside the fresh Alpine container created by bin/test-vless-ws-local.sh.
set -euo pipefail
test ! -f /etc/caddy/Caddyfile
if command -v caddy >/dev/null; then exit 1; fi
mkdir -p /run/openrc
touch /run/openrc/softlevel

id=00000000-0000-4000-8000-000000000001
host=installation.example.test
cert=/etc/caddy/skyway-certs/$id
script=/qa/vless-ws.sh
bash "$script" csr "$id" "$host" >/dev/null
openssl req -x509 -new -key "$cert/key.pem" -subj "/CN=$host" \
    -addext "subjectAltName=DNS:$host" -days 1 -out "$cert/chain.pem"

web_root=$(mktemp -d)
echo skyway-fresh-origin > "$web_root/check"
python3 -m http.server 22101 --bind 127.0.0.1 --directory "$web_root" >/dev/null 2>&1 &
origin_pid=$!
trap 'kill "$origin_pid" 2>/dev/null || :; rc-service caddy stop >/dev/null 2>&1 || :' EXIT
bash "$script" site "$id" "$host" 443 22101 /check
test -x /usr/local/bin/caddy
test -L /etc/runlevels/default/caddy
rc-service caddy status
reply=$(curl --noproxy '*' -fsS --retry 5 --retry-connrefused --retry-delay 1 --max-time 5 \
    --cacert "$cert/chain.pem" --resolve "$host:443:127.0.0.1" "https://$host/check")
test "$reply" = skyway-fresh-origin
bash "$script" remove-site "$id"
test ! -e "/etc/caddy/skyway/$id.conf"
