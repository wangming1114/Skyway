"""Network-namespace-only probe for the opt-in live VPS test; no external dependencies."""
import argparse
import json
import socket
import struct
import time


def read_exact(stream, count):
    result = b""
    while len(result) < count:
        chunk = stream.recv(count - len(result))
        if not chunk:
            raise OSError("SOCKS connection closed")
        result += chunk
    return result


def socks(port, command, destination):
    control = socket.create_connection(("127.0.0.1", port), timeout=20)
    control.sendall(b"\x05\x01\x00")
    if read_exact(control, 2) != b"\x05\x00":
        raise OSError("SOCKS authentication failed")
    control.sendall(b"\x05" + bytes([command]) + b"\x00\x01" + socket.inet_aton("127.0.0.1") + struct.pack("!H", destination))
    version, reply, _, atyp = read_exact(control, 4)
    if version != 5 or reply != 0:
        raise OSError("SOCKS request failed: " + str(reply))
    if atyp == 1:
        read_exact(control, 4)
    elif atyp == 4:
        read_exact(control, 16)
    else:
        read_exact(control, read_exact(control, 1)[0])
    endpoint = struct.unpack("!H", read_exact(control, 2))[0]
    return control, endpoint


def http(port, host, count, upload=False):
    started = time.monotonic()
    control, _ = socks(port, 1, 29090)
    with control:
        path = "/health" if count == 0 or upload else "/blob?bytes=" + str(count)
        headers = ("POST" if upload else "GET") + " " + path + " HTTP/1.1\r\nHost: " + host + "\r\nConnection: close\r\n"
        if upload:
            headers += "Content-Length: " + str(count) + "\r\n"
        control.sendall((headers + "\r\n").encode("ascii"))
        if upload:
            control.sendall(b"u" * count)
        response = b""
        while True:
            data = control.recv(65536)
            if not data:
                break
            response += data
    header, _, body = response.partition(b"\r\n\r\n")
    return {"ok": b" 200 " in header.split(b"\r\n")[0], "bytes": len(body),
            "health": b"skyway-live-ok" in body, "seconds": time.monotonic() - started}


def udp(port):
    control, endpoint = socks(port, 3, 0)
    with control, socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as stream:
        stream.settimeout(15)
        payload = b"skyway-live-udp"
        frame = b"\x00\x00\x00\x01" + socket.inet_aton("127.0.0.1") + struct.pack("!H", 29092) + payload
        stream.sendto(frame, ("127.0.0.1", endpoint))
        response, _ = stream.recvfrom(65535)
        return {"ok": response.endswith(payload)}


parser = argparse.ArgumentParser()
parser.add_argument("port", type=int)
parser.add_argument("operation")
parser.add_argument("--host", default="allowed.example")
parser.add_argument("--bytes", type=int, default=0)
parser.add_argument("--seconds", type=int, default=6)
args = parser.parse_args()
try:
    if args.operation == "udp":
        result = udp(args.port)
    elif args.operation == "load":
        until = time.monotonic() + args.seconds
        transfers = 0
        while time.monotonic() < until:
            sample = http(args.port, args.host, 1024 * 1024)
            if not sample["ok"]:
                raise OSError("HTTP load transfer failed")
            transfers += 1
        result = {"ok": True, "transfers": transfers}
    else:
        result = http(args.port, args.host, args.bytes, args.operation == "upload")
    print(json.dumps(result))
except Exception as error:
    print(json.dumps({"ok": False, "error": str(error)}))
