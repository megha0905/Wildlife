// Tiny development server for the plain HTML/CSS/JS frontend. Uses only Node's built-in modules.
//  - serves index.html, src/ and public/ on port 5173
//  - forwards every /api/... request to the API gateway, so the browser only ever talks to one address
//    (no CORS problems, and it works the same on a laptop and in GitHub Codespaces)
// Settings (optional): PORT (default 5173), API_TARGET (default http://localhost:8080)
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.dirname(fileURLToPath(import.meta.url));
const PORT = Number(process.env.PORT || 5173);
const API_TARGET = new URL(process.env.API_TARGET || 'http://localhost:8080');

const TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.ico': 'image/x-icon',
  '.json': 'application/json',
};

function proxyToGateway(req, res) {
  const upstream = http.request(
    {
      hostname: API_TARGET.hostname,
      port: API_TARGET.port || 80,
      path: req.url,
      method: req.method,
      headers: { ...req.headers, host: API_TARGET.host },
    },
    (up) => {
      res.writeHead(up.statusCode || 502, up.headers);
      up.pipe(res);
    },
  );
  upstream.on('error', () => {
    res.writeHead(503, { 'content-type': 'application/json' });
    res.end(JSON.stringify({
      timestamp: new Date().toISOString(), status: 503, error: 'Service Unavailable',
      message: `Can't reach the API gateway at ${API_TARGET.origin}. Is it running?`, path: req.url, details: [],
    }));
  });
  req.pipe(upstream);
}

function serveFile(req, res) {
  const urlPath = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  // /favicon.svg and other images live in public/; everything else is relative to the frontend folder
  const candidates = urlPath === '/' ? ['index.html'] : [urlPath.slice(1), path.join('public', urlPath.slice(1))];
  for (const rel of candidates) {
    const file = path.normalize(path.join(ROOT, rel));
    if (!file.startsWith(ROOT + path.sep)) break; // refuse anything outside the frontend folder
    if (fs.existsSync(file) && fs.statSync(file).isFile()) {
      res.writeHead(200, { 'content-type': TYPES[path.extname(file)] || 'application/octet-stream', 'cache-control': 'no-cache' });
      fs.createReadStream(file).pipe(res);
      return;
    }
  }
  // unknown path: send the app itself (screens are chosen by the #/route in the address)
  res.writeHead(200, { 'content-type': TYPES['.html'] });
  fs.createReadStream(path.join(ROOT, 'index.html')).pipe(res);
}

http.createServer((req, res) => {
  if (req.url.startsWith('/api/')) proxyToGateway(req, res);
  else serveFile(req, res);
}).listen(PORT, '0.0.0.0', () => {
  console.log(`WildGuard frontend running at http://localhost:${PORT}  (API calls forwarded to ${API_TARGET.origin})`);
});
