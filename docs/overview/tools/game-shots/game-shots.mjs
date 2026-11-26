// Echte Spiel-Screenshots von moovr/glidr im Headless-Chromium (die WebView ist auch Chromium)
// Aufruf: node game-shots.mjs <moovr|glidr> <ausgabeordner> <szenario> [seed]
// Braucht playwright-core + chromium-headless-shell (siehe ../README.md)
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright-core';

const [app, out, scen] = process.argv.slice(2);
const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '../../../../apps', app, 'src/main/assets');
const MIME = { '.html': 'text/html', '.js': 'text/javascript', '.json': 'application/json', '.jsona': 'application/json',
  '.png': 'image/png', '.jpg': 'image/jpeg', '.ogg': 'audio/ogg', '.xml': 'text/xml' };

// Static-Server nur auf localhost, XHR geht nicht über file://
const server = http.createServer((req, res) => {
  const p = path.normalize(decodeURIComponent(req.url.split('?')[0])).replace(/^\/+/, '');
  const f = path.join(root, p || 'index.html');
  if (!f.startsWith(root) || !fs.existsSync(f) || fs.statSync(f).isDirectory()) { res.writeHead(404); return res.end(); }
  res.writeHead(200, { 'content-type': MIME[path.extname(f)] || 'application/octet-stream' });
  fs.createReadStream(f).pipe(res);
});
await new Promise((r) => server.listen(0, '127.0.0.1', r));
const base = `http://127.0.0.1:${server.address().port}/`;

fs.mkdirSync(out, { recursive: true });
// Handy quer 2:1, 2400×1200 Pixel (Play-Maximum, store-images.js braucht keinen Rand)
const browser = await chromium.launch({ args: ['--mute-audio', '--autoplay-policy=no-user-gesture-required'] });
const page = await browser.newPage({ viewport: { width: 960, height: 480 }, deviceScaleFactor: 2.5 });
page.on('pageerror', (e) => console.log('pageerror', e.message));
const ctx = {
  page, base,
  sleep: (ms) => page.waitForTimeout(ms),
  shot: async (name) => { await page.screenshot({ path: path.join(out, name + '.png') }); console.log('shot', name); },
  ev: (fn, arg) => page.evaluate(fn, arg),
  // wartet auf einen Phaser-State ohne laufende Überblendung
  waitState: (s, t = 20000) => page.waitForFunction((s) => window.game && game.state && game.state.current === s && !(window.cover && cover.game), s, { timeout: t }),
  seed: Number(process.argv[5] || 7),
};
try {
  const scenarios = (await import(`./scen-${app}.mjs`)).default;
  await scenarios[scen](ctx);
} finally {
  await browser.close();
  server.close();
}
