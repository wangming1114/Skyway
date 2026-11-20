# Test service supervisor for a disposable container without systemd or OpenRC.
set -euo pipefail
install -d /etc/sing-box/bin /etc/sing-box/conf /var/log/sing-box /etc/init.d
install /qa/bin/sing-box /etc/sing-box/bin/sing-box
install /qa/bin/caddy /usr/local/bin/caddy
chmod +x /etc/sing-box/sh/sing-box.sh
ln -sf /etc/sing-box/sh/sing-box.sh /usr/local/bin/sb
touch /etc/init.d/caddy
chmod +x /etc/init.d/caddy
echo '{"log":{"level":"info","timestamp":true}}' >/etc/sing-box/config.json
cat >/usr/local/bin/rc-service <<'SH'
#!/bin/bash
set -eu
pid=/run/$1.pid
running() { [ -f "$pid" ] && grep -q ') [SRD]' /proc/"$(cat "$pid")"/stat 2>/dev/null; }
if [ "$2" = status ]; then running; exit; fi
if running; then
    if [ "$2" = start ]; then exit 0; fi
    kill "$(cat "$pid")"
    for n in {1..40}; do running || break; sleep .1; done
fi
case "$1" in
    sing-box) /etc/sing-box/bin/sing-box run -c /etc/sing-box/config.json -C /etc/sing-box/conf >>/qa/sing-box.log 2>&1 </dev/null & ;;
    caddy) /usr/local/bin/caddy run --config /etc/caddy/Caddyfile --adapter caddyfile >>/qa/caddy.log 2>&1 </dev/null & ;;
    *) exit 1 ;;
esac
echo $! >"$pid"
sleep .3
running
SH
chmod +x /usr/local/bin/rc-service
