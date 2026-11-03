// Serves ../web over HTTP on :8081 and tunnels WebSocket upgrades to the
// Velocity + EaglerXServer listener (default 127.0.0.1:25565).
const http = require('http');
const net = require('net');
const fs = require('fs');
const path = require('path');

const PORT = parseInt(process.env.WEB_PORT || '8081', 10);
const BACKEND_HOST = process.env.EAGLER_HOST || '127.0.0.1';
const BACKEND_PORT = parseInt(process.env.EAGLER_PORT || '25565', 10);
const ROOT = path.resolve(process.env.WEB_ROOT || path.join(__dirname, '..', 'web'));
const INDEX = 'EaglerCraft-26.2.html';
const TYPES = {
  '.html': 'text/html; charset=utf-8', '.png': 'image/png', '.js': 'text/javascript',
  '.json': 'application/json', '.ico': 'image/x-icon', '.css': 'text/css',
};

const server = http.createServer((req, res) => {
  let p;
  try { p = decodeURIComponent(req.url.split('?')[0]); } catch (e) { res.writeHead(400); return res.end(); }
  if (p === '/') p = '/' + INDEX;
  const file = path.join(ROOT, path.normalize(p));
  if (file !== ROOT && !file.startsWith(ROOT + path.sep)) { res.writeHead(403); return res.end(); }
  fs.stat(file, (err, st) => {
    if (err || !st.isFile()) { res.writeHead(404); return res.end('Not found'); }
    res.writeHead(200, {
      'Content-Type': TYPES[path.extname(file)] || 'application/octet-stream',
      'Content-Length': st.size,
    });
    fs.createReadStream(file).pipe(res);
  });
});

server.on('upgrade', (req, socket, head) => {
  const upstream = net.connect(BACKEND_PORT, BACKEND_HOST, () => {
    let raw = `${req.method} ${req.url} HTTP/${req.httpVersion}\r\n`;
    for (let i = 0; i < req.rawHeaders.length; i += 2) raw += `${req.rawHeaders[i]}: ${req.rawHeaders[i + 1]}\r\n`;
    upstream.write(raw + '\r\n');
    if (head && head.length) upstream.write(head);
    socket.pipe(upstream);
    upstream.pipe(socket);
  });
  const close = () => { socket.destroy(); upstream.destroy(); };
  upstream.on('error', close);
  socket.on('error', close);
  upstream.on('close', close);
  socket.on('close', close);
});

server.listen(PORT, '0.0.0.0', () => console.log(`web gateway on :${PORT} -> ${BACKEND_HOST}:${BACKEND_PORT}`));
