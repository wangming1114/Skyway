"""Authoritative qa.test DNS and the Cloudflare TXT API used by lego, confined to loopback."""
import json
import threading
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit
from dnslib import A, NS, QTYPE, RR, SOA, TXT
from dnslib.server import DNSServer

ZONE = 'qa.test.'
ZONE_ID = 'a' * 32
records = {}
lock = threading.Lock()

class Resolver:
    def resolve(self, request, handler):
        reply = request.reply()
        name, kind = str(request.q.qname), QTYPE[request.q.qtype]
        data = []
        if name == ZONE and kind == 'SOA':
            data = [SOA('ns.' + ZONE, 'hostmaster.' + ZONE, (1, 60, 60, 60, 1))]
        elif name == ZONE and kind == 'NS':
            data = [NS('ns.' + ZONE)]
        elif kind == 'A' and name == 'ns.' + ZONE:
            data = [A('127.0.0.1')]
        elif kind == 'TXT':
            with lock:
                data = [TXT(value) for host, value in records.values() if host + '.' == name]
        for value in data:
            reply.add_answer(RR(name, request.q.qtype, ttl=1, rdata=value))
        return reply

class Cloudflare(BaseHTTPRequestHandler):
    def respond(self, result, status=200):
        if self.headers.get('Authorization') != 'Bearer qa-dns-token':
            status, result = 403, None
        body = json.dumps({'success': status == 200, 'result': result, 'errors': [],
                           'result_info': {'page': 1, 'total_pages': 1}}).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if urlsplit(self.path).path.endswith('/zones'):
            self.respond([{'id': ZONE_ID, 'name': ZONE.rstrip('.')}])
        else:
            self.respond(None, 404)

    def do_POST(self):
        entry = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
        if entry.get('type') != 'TXT' or not entry['name'].endswith('.' + ZONE.rstrip('.')):
            return self.respond(None, 400)
        record = uuid.uuid4().hex
        with lock:
            records[record] = entry['name'], entry['content'].strip('"')
        self.respond(dict(entry, id=record))

    def do_DELETE(self):
        with lock:
            existing = records.pop(self.path.rsplit('/', 1)[-1], None)
        self.respond({}, 200 if existing else 404)

for tcp in [False, True]:
    DNSServer(Resolver(), address='127.0.0.1', port=53, tcp=tcp).start_thread()
ThreadingHTTPServer(('127.0.0.1', 22450), Cloudflare).serve_forever()
