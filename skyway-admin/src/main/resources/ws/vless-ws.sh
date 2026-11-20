#!/usr/bin/env bash
# Invoked over the existing VPS SSH connection. Arguments are validated and shell-quoted by WsRemote.
set -euo pipefail
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/sbin:$PATH"
action=$1
shift
core=/etc/sing-box/bin/sing-box
conf=/etc/sing-box/conf
caddyfile=/etc/caddy/Caddyfile
sites=/etc/caddy/skyway
certs=/etc/caddy/skyway-certs
work=$(mktemp -d)
trap 'rm -rf -- "$work"' EXIT
umask 077
service_action() {
    if command -v systemctl >/dev/null && [ -d /run/systemd/system ]; then systemctl "$2" "$1"
    else rc-service "$1" "$2"; fi
}
caddy_bin() {
    if command -v caddy >/dev/null; then command -v caddy
    elif [ -x /usr/local/bin/caddy ]; then echo /usr/local/bin/caddy
    else return 1; fi
}
reload_caddy() {
    local bin
    bin=$(caddy_bin)
    "$bin" validate --config "$caddyfile" --adapter caddyfile >/dev/null
    if ! pgrep -x caddy >/dev/null; then service_action caddy start
    elif "$bin" reload --force --config "$caddyfile" --adapter caddyfile >"$work/reload" 2>&1; then return
    elif grep -Eq '^[[:space:]]*admin[[:space:]]+off([[:space:]]|$)' "$caddyfile"; then
        # 233boy runs caddy run --config. Caddy 2.11 supports graceful file reload with admin off.
        case "$("$bin" version)" in
          v2.[0-9].*|v2.10.*) service_action caddy restart ;;
          *) if [ -d /run/systemd/system ]; then systemctl kill --kill-who=main --signal=USR1 caddy
             else kill -USR1 "$(cat /run/caddy.pid)"; fi ;;
        esac
    else cat "$work/reload" >&2; return 1; fi
}
install_caddy() {
    if ! caddy_bin >/dev/null; then
        local arch version=2.11.9
        case "$(uname -m)" in x86_64) arch=amd64;; aarch64|arm64) arch=arm64;; *) echo 'Caddy 不支持此 CPU 架构' >&2; return 1;; esac
        local asset="caddy_${version}_linux_${arch}.tar.gz" release="https://github.com/caddyserver/caddy/releases/download/v$version"
        (cd "$work"
         curl -fsSL --retry 2 "$release/$asset" -o "$asset"
         curl -fsSL --retry 2 "$release/caddy_${version}_checksums.txt" -o sums
         awk -v name="$asset" '
             ($2 == name || $2 == "*" name) && length($1) == 128 && $1 ~ /^[[:xdigit:]]+$/ {
                 print
                 matches++
             }
             END { if (matches != 1) exit 1 }
         ' sums > expected
         sha512sum -c expected
         tar -xzf "$asset" caddy
         install -m 0755 caddy /usr/local/bin/caddy)
    fi
    install -d -m 0755 /etc/caddy "$sites"
    if [ ! -f "$caddyfile" ]; then
        printf '{\n auto_https disable_redirects\n}\nimport %s/*.conf\n' "$sites" > "$work/Caddyfile"
        install -m 0644 "$work/Caddyfile" "$caddyfile"
    elif ! grep -Eq '^[[:space:]]*import[[:space:]]+/etc/caddy/skyway/\*\.conf[[:space:]]*$' "$caddyfile"; then
        # Keep relative imports and permissions of the original Caddyfile.
        local candidate
        candidate=$(mktemp /etc/caddy/.skyway-Caddyfile.XXXXXX)
        cp -p "$caddyfile" "$candidate"
        printf '\nimport %s/*.conf\n' "$sites" >> "$candidate"
        if ! "$(caddy_bin)" validate --adapter caddyfile --config "$candidate"; then rm -f "$candidate"; return 1; fi
        mv -f "$candidate" "$caddyfile"
    fi
    if [ -d /run/systemd/system ]; then
        if ! systemctl cat caddy.service >/dev/null 2>&1; then
            cat > /etc/systemd/system/caddy.service <<UNIT
[Unit]
Description=Caddy
After=network-online.target
[Service]
ExecStart=$(caddy_bin) run --config $caddyfile --adapter caddyfile
Restart=on-failure
[Install]
WantedBy=multi-user.target
UNIT
            systemctl daemon-reload
            systemctl enable caddy
        fi
    elif [ ! -x /etc/init.d/caddy ]; then
        cat > /etc/init.d/caddy <<UNIT
#!/sbin/openrc-run
command="$(caddy_bin)"
command_args="run --config $caddyfile --adapter caddyfile"
command_background=true
pidfile=/run/caddy.pid
UNIT
        chmod 0755 /etc/init.d/caddy
        rc-update add caddy default
    fi
}
rate_filters() {
    local mode=$1 port=$2 download=$3 upload=$4 group slot dir pref rate handle burst
    # u32 is also available on kernels without cls_flower. Each priority owns 4095 node handles.
    group=$(((port - 1) / 4095))
    slot=$(printf '%x' "$(((port - 1) % 4095 + 1))")
    if ! tc qdisc show dev lo | grep -qw clsact; then
        [ "$mode" != unlimit ] || return 0
        tc qdisc add dev lo clsact || return 1
    fi
    for dir in sport dport; do
        if [ "$dir" = sport ]; then pref=$((57300 + group * 2)); rate=$download
        else pref=$((57301 + group * 2)); rate=$upload; fi
        if [ "$mode" != limit ]; then
            handle=$(tc filter show dev lo egress pref "$pref" | awk -v suffix="::$slot" '{for(i=1;i<NF;i++) if ($i=="fh" && $(i+1) ~ (suffix "$")) print $(i+1)}')
            if [ "$mode" = unlimit ]; then
                if [ -n "$handle" ]; then tc filter del dev lo egress protocol ip pref "$pref" handle "$handle" u32 || return 1; fi
                continue
            fi
            [ -z "$handle" ] || continue
        fi
        burst=$((rate * 8 + 256))
        tc filter replace dev lo egress protocol ip pref "$pref" handle "::$slot" u32 \
                match ip protocol 6 0xff match ip "$dir" "$port" 0xffff \
                action police rate "${rate}mbit" burst "${burst}kb" mtu 128kb conform-exceed drop/ok || return 1
    done
}
case "$action" in
preflight)
    command -v sb >/dev/null
    listeners=$(ss -Hltnp "sport = :$1")
    if [ -n "$listeners" ] && grep -v '"caddy"' <<<"$listeners" | grep -q .; then echo "公网端口 $1 已被其他服务占用" >&2; exit 1; fi
    ;;
generate) sb gen vws "$1" "$2" "$3" ;;
csr)
    dir="$certs/$1"; host=$2
    install -d -m 0755 "$certs"; [ -d "$dir" ] || install -d -m 0700 "$dir"
    if [ ! -s "$dir/key.pem" ]; then
        openssl ecparam -name prime256v1 -genkey -noout -out "$dir/key.new"
        mv "$dir/key.new" "$dir/key.pem"
    fi
    printf '[req]\ndistinguished_name=dn\nreq_extensions=ext\nprompt=no\n[dn]\nCN=%s\n[ext]\nsubjectAltName=DNS:%s\n' "$host" "$host" > "$work/req.cnf"
    openssl req -new -key "$dir/key.pem" -config "$work/req.cnf"
    ;;
public-key)
    openssl pkey -in "$certs/$1/key.pem" -pubout -outform DER | base64
    ;;
certificate)
    dir="$certs/$1"; mkdir -p "$dir"
    if [ -s "$dir/chain.pem" ]; then cat "$dir/chain.pem"; fi
    ;;
put-cert)
    dir="$certs/$1"; printf %s "$2" | base64 -d > "$dir/chain.new"
    openssl x509 -in "$dir/chain.new" -pubkey -noout > "$work/pub"
    openssl pkey -in "$dir/key.pem" -pubout | cmp - "$work/pub"
    chmod 0644 "$dir/chain.new"; mv -f "$dir/chain.new" "$dir/chain.pem"
    ;;
inbound)
    name=$1; disabled=$2
    dest="$conf/$name.json"; other="$dest.disabled"
    if [ "$disabled" = 1 ]; then dest="$conf/$name.json.disabled"; other="$conf/$name.json"; fi
    printf %s "$3" | base64 -d > "$work/node"
    if [ -f "$dest" ] && cmp -s "$work/node" "$dest" && [ ! -e "$other" ]; then exit 0; fi
    mkdir "$work/check"
    for file in "$conf"/*.json; do
        [ -f "$file" ] || continue
        [ "$file" = "$conf/$name.json" ] || ln -s "$file" "$work/check/$(basename "$file")"
    done
    cp "$work/node" "$work/check/$name.json"
    "$core" check -c /etc/sing-box/config.json -C "$work/check" >/dev/null
    [ ! -e "$dest" ] || cp -p "$dest" "$work/old"
    [ ! -e "$other" ] || cp -p "$other" "$work/other"
    install -m 0600 "$work/node" "$dest.new"; mv -f "$dest.new" "$dest"; rm -f "$other"
    if ! service_action sing-box restart; then
        rm -f "$dest"; [ ! -f "$work/old" ] || cp -p "$work/old" "$dest"
        [ ! -f "$work/other" ] || cp -p "$work/other" "$other"
        service_action sing-box restart; exit 1
    fi
    ;;
site)
    id=$1; host=$2; port=$3; upstream=$4; route=$5
    install_caddy
    # Distribution packages often run Caddy under a dedicated service account.
    # The VPS root and that group need access to this node's certificate and key.
    reader=$(ps -o egid= -p "$(pgrep -xo caddy || echo 0)" 2>/dev/null | tr -d ' ') || reader=
    if [ -z "$reader" ] && [ -d /run/systemd/system ]; then
        user=$(systemctl show caddy -p User --value)
        group=$(systemctl show caddy -p Group --value)
        if [ -n "$group" ]; then reader=$(getent group "$group" | cut -d: -f3)
        elif [ -n "$user" ]; then reader=$(id -g "$user"); fi
    elif [ -z "$reader" ] && getent group caddy >/dev/null; then
        reader=$(getent group caddy | cut -d: -f3)
    fi
    if [[ "$reader" =~ ^[0-9]+$ ]] && [ "$reader" != 0 ]; then
        chgrp "$reader" "$certs/$id" "$certs/$id/chain.pem" "$certs/$id/key.pem"
        chmod 0750 "$certs/$id"
        chmod 0640 "$certs/$id/chain.pem" "$certs/$id/key.pem"
    fi
    file="$sites/$id.conf"
    cat > "$work/site" <<SITE
https://$host:$port {
    tls $certs/$id/chain.pem $certs/$id/key.pem
    reverse_proxy $route 127.0.0.1:$upstream {
        header_up Host localhost
    }
}
SITE
    [ ! -e "$file" ] || cp -p "$file" "$work/old"
    install -m 0644 "$work/site" "$file.new"; mv -f "$file.new" "$file"
    if ! reload_caddy; then
        if [ -f "$work/old" ]; then cp -p "$work/old" "$file"; else rm -f "$file"; fi
        reload_caddy; exit 1
    fi
    ;;
probe)
    host=$1; port=$2; route=$3; enabled=$4
    expected=$(openssl x509 -in "$certs/$5/chain.pem" -noout -fingerprint -sha256)
    # Verify certificate chain, hostname and the WebSocket accept header on the actual Caddy listener.
    for n in 1 2 3 4 5; do
        timeout 4 openssl s_client -connect "127.0.0.1:$port" -servername "$host" </dev/null >"$work/tls" 2>/dev/null || :
        served=$(openssl x509 -in "$work/tls" -noout -fingerprint -sha256 2>/dev/null) || served=
        if [ "$served" != "$expected" ]; then sleep 1; continue; fi
        : > "$work/headers"
        curl --noproxy '*' --http1.1 --silent --max-time 2 --resolve "$host:$port:127.0.0.1" \
            -D "$work/headers" -o /dev/null -H 'Connection: Upgrade' -H 'Upgrade: websocket' \
            -H 'Sec-WebSocket-Version: 13' -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' "https://$host:$port$route" || :
        if [ "$enabled" = 0 ]; then grep -q '^HTTP/' "$work/headers" && exit 0
        elif grep -q '^HTTP/1.1 101' "$work/headers" && grep -iq '^Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=' "$work/headers"; then exit 0; fi
        sleep 1
    done
    echo '源站 TLS 或 WebSocket 握手未通过' >&2; exit 1
    ;;
remove-site)
    file="$sites/$1.conf"
    if [ -f "$file" ]; then
        mv "$file" "$file.retiring"
    fi
    if [ -f "$file.retiring" ]; then
        reload_caddy
        rm -f "$file.retiring"
    fi
    rm -f -- "$certs/$1/key.pem" "$certs/$1/chain.pem" "$certs/$1/chain.new" "$certs/$1/key.new"
    if [ -d "$certs/$1" ]; then rmdir -- "$certs/$1"; fi
    ;;
counters|uncount)
    port=$1
    if [ "$action" = counters ] && [ -f "/etc/sing-box/skyway-rates/$port" ]; then
        read -r download upload < "/etc/sing-box/skyway-rates/$port"
        rate_filters restore "$port" "$download" "$upload"
    fi
    if command -v iptables >/dev/null; then
        for direction in in out; do
            if [ "$direction" = in ]; then chain=NODE_TRAFFIC_IN; hook=INPUT; iface=-i; field=--dport
            else chain=NODE_TRAFFIC_OUT; hook=OUTPUT; iface=-o; field=--sport; fi
            rule=("$iface" lo -p tcp "$field" "$port" -j RETURN)
            if [ "$action" = counters ]; then
                iptables -w -S "$chain" >/dev/null 2>&1 || iptables -w -N "$chain"
                iptables -w -C "$hook" -p tcp -j "$chain" 2>/dev/null || iptables -w -I "$hook" 1 -p tcp -j "$chain"
                iptables -w -C "$chain" "${rule[@]}" 2>/dev/null || iptables -w -A "$chain" "${rule[@]}"
            elif iptables -w -C "$chain" "${rule[@]}" 2>/dev/null; then
                iptables -w -D "$chain" "${rule[@]}"
            fi
        done
    else
        for direction in in out; do
            if [ "$direction" = in ]; then hook=input; iface=iifname; field=dport
            else hook=output; iface=oifname; field=sport; fi
            if [ "$action" = counters ]; then
                nft list table inet node_traffic >/dev/null 2>&1 || nft add table inet node_traffic
                nft list chain inet node_traffic "$direction" >/dev/null 2>&1 || nft add chain inet node_traffic "$direction" "{ type filter hook $hook priority 0; policy accept; }"
                nft list chain inet node_traffic "$direction" | grep -Eq "tcp $field $port counter" || nft add rule inet node_traffic "$direction" "$iface" lo tcp "$field" "$port" counter return
            elif nft list chain inet node_traffic "$direction" >/dev/null 2>&1; then
                nft -a list chain inet node_traffic "$direction" | awk "/$iface \"lo\" tcp $field $port counter/ {print \$NF}" | while read -r handle; do
                    nft delete rule inet node_traffic "$direction" handle "$handle"
                done
            fi
        done
    fi
    ;;
limit|unlimit)
    port=$1
    install -d -m 0700 /etc/sing-box/skyway-rates
    rule=/etc/sing-box/skyway-rates/$port
    if [ "$action" = unlimit ]; then
        if [ ! -f "$rule" ] && ! command -v tc >/dev/null; then exit 0; fi
        rate_filters unlimit "$port" 0 0
        rm -f -- "$rule"
    else
        if ! rate_filters limit "$port" "$2" "$3"; then
            if [ -f "$rule" ]; then
                read -r previous_down previous_up < "$rule"
                rate_filters limit "$port" "$previous_down" "$previous_up"
            else rate_filters unlimit "$port" 0 0; fi
            exit 1
        fi
        printf '%s %s\n' "$2" "$3" > "$rule.new"; mv -f "$rule.new" "$rule"
    fi
    ;;
remove-inbound)
    file="$conf/$1.json"
    if [ -f "$file" ]; then mv "$file" "$file.retiring"; fi
    if [ -e "$file.retiring" ]; then service_action sing-box restart; rm -f "$file.retiring"; fi
    rm -f -- "$file.disabled"
    ;;
*) exit 2 ;;
esac
