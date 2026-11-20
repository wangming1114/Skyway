set -euo pipefail
bytes() {
    iptables -L "NODE_TRAFFIC_$1" -nxv | awk -v key="$2:$3" '$NF==key { print $2; found=1 } END { if (!found) exit 1 }'
}
transfer() {
    local port=$1
    curl --noproxy '' -fsS --max-time 35 -x "socks5h://127.0.0.1:$port" -o "/qa/download-$port" http://127.0.0.1:22420/download
    test "$(wc -c < "/qa/download-$port")" -eq 1048576
    curl --noproxy '' -fsS --max-time 35 -x "socks5h://127.0.0.1:$port" --data-binary "@/qa/download-$port" http://127.0.0.1:22420/upload
}
before_a=$(bytes IN dpt 22401)
before_b=$(bytes IN dpt 22402)
out_a=$(bytes OUT spt 22401)
out_b=$(bytes OUT spt 22402)
TERM=xterm bash /qa/speed.sh >/qa/speed.out 2>/qa/speed.err & monitor=$!
trap 'kill "$monitor" 2>/dev/null || :' EXIT
sleep 1.2
transfer 22411 & a=$!
transfer 22412 & b=$!
wait "$a"
wait "$b"
sleep 1.5
for port in 22401 22402; do
    awk -F'|' -v port="$port" '$1+0==port && ($2+0>0 || $3+0>0) {seen=1} END {exit !seen}' /qa/speed.out
done
test $(( $(bytes IN dpt 22401) - before_a )) -ge 1048576
test $(( $(bytes IN dpt 22402) - before_b )) -ge 1048576
test $(( $(bytes OUT spt 22401) - out_a )) -ge 1048576
test $(( $(bytes OUT spt 22402) - out_b )) -ge 1048576
for pref in 57310 57311; do
    filters=$(tc -s filter show dev lo egress pref "$pref")
    test "$(grep -cE 'Sent [1-9][0-9]* bytes' <<<"$filters")" -ge 2
done
