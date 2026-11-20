set -euo pipefail
cd /qa
for file in dns.pid authority.pid; do
    if [ -f "$file" ]; then kill "$(cat "$file")" 2>/dev/null || :; fi
done
grep -qw 'ns.qa.test' /etc/hosts || echo '127.0.0.1 ns.qa.test' >> /etc/hosts
python3 -I -u /qa/acme-dns-fixture.py >dns.log 2>&1 </dev/null & echo $! >dns.pid
cat >authority.Caddyfile <<'CONFIG'
{
    admin 127.0.0.1:22452
    auto_https disable_redirects
    skip_install_trust
}
https://localhost:22451 {
    bind 127.0.0.1
    tls internal
    acme_server {
        challenges dns-01
        lifetime 3h
        resolvers 127.0.0.1:53
    }
}
CONFIG
XDG_DATA_HOME=/qa/authority XDG_CONFIG_HOME=/qa/authority ./bin/caddy run --config authority.Caddyfile --adapter caddyfile >authority.log 2>&1 </dev/null & echo $! >authority.pid
for n in {1..40}; do
    if curl --silent --fail --insecure https://localhost:22451/acme/local/directory -o /dev/null; then break; fi
    sleep .25
done
install -m 0644 /qa/authority/caddy/pki/authorities/local/root.crt /usr/local/share/ca-certificates/skyway-qa-acme.crt
update-ca-certificates >/dev/null
curl -fsS https://localhost:22451/acme/local/directory >/dev/null
