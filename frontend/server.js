import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import httpProxy from 'http-proxy';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const app = express();
const PORT = process.env.PORT || 3000;

const BACKEND_URL = (process.env.BACKEND_URL || process.env.VITE_BACKEND_URL || 'http://127.0.0.1:8081').replace(/\/$/, '');

const { createProxyServer } = httpProxy;

const proxy = createProxyServer({
  target: BACKEND_URL,
  changeOrigin: true,
  ws: true,
  secure: BACKEND_URL.startsWith('https://'),
});

proxy.on('error', (error, req, res) => {
  console.error(`Proxy error for ${req?.method ?? 'UNKNOWN'} ${req?.url ?? ''}:`, error.message);

  if (res && !res.headersSent) {
    res.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Backend service is unavailable');
  }
});

app.use((req, res, next) => {
  if (!req.url.startsWith('/api/')) return next();
  proxy.web(req, res);
});

app.use(express.static(path.join(__dirname, 'dist')));

app.get(/.*/, (req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

const server = app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
  console.log(`Proxying API requests to ${BACKEND_URL}`);
});

server.on('upgrade', (req, socket, head) => {
  if (req.url?.startsWith('/api/')) {
    proxy.ws(req, socket, head);
    return;
  }

  socket.destroy();
});
