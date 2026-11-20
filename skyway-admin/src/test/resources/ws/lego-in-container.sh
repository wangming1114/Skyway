#!/usr/bin/env bash
# Test adapter for the Linux lego binary. Only public CSRs and chains cross the container boundary.
set -euo pipefail
args=()
while [ "$#" -gt 0 ]; do
    case "$1" in
        --path) local_state=$2; shift 2 ;;
        --csr) csr=$2; shift 2 ;;
        *) args+=("$1"); shift ;;
    esac
done
container=$SKYWAY_WS_TEST_CONTAINER
dir=/qa/orders
target="$dir/$(basename "$csr")"
docker exec "$container" mkdir -p "$dir"
docker exec -i "$container" sh -c 'cat > "$1"' sh "$target" < "$csr"
host=$(docker exec "$container" openssl req -in "$target" -noout -subject -nameopt RFC2253 | sed -n 's/^subject=CN=//p')
[[ "$host" == *.qa.test ]]
docker exec -e CLOUDFLARE_DNS_API_TOKEN -e CLOUDFLARE_ZONE_API_TOKEN \
    -e CLOUDFLARE_BASE_URL=http://127.0.0.1:22450/client/v4 -e CLOUDFLARE_POLLING_INTERVAL=1 \
    "$container" /qa/bin/lego --path "$dir" --csr "$target" --dns.resolvers 127.0.0.1:53 "${args[@]}"
mkdir -p "$local_state/certificates"
docker exec "$container" cat "$dir/certificates/$host.crt" > "$local_state/certificates/$host.crt"
