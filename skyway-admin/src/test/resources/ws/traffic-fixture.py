import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

class Echo(BaseHTTPRequestHandler):
    def do_GET(self):
        body = b'x' * (1024 * 1024) if self.path == '/download' else b'ok'
        self.send_response(200)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        size = int(self.headers['Content-Length'])
        self.send_response(200 if len(self.rfile.read(size)) == size else 400)
        self.end_headers()

server = ThreadingHTTPServer(('127.0.0.1', int(sys.argv[1]) if len(sys.argv) > 1 else 22420), Echo)
print(server.server_port, flush=True)
server.serve_forever()
