// Play-Store-Grafiken je App und Sprache nach apps/<app>/fastlane/metadata/android/<locale>/images/
// Aufruf: node store-images.js [app,...] [--collage datei.png] | node store-images.js --check
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { OV, REPO, canvas, apps } = require('./lib.js');

const LOCALES = ['de-DE', 'en-US'];
const GAMES = ['moovr', 'glidr']; // Spiele: Feature-Grafik mit Spielgrafik, Screenshots quer aus game-shots.mjs
const BG = '#10161B', FG = '#DCE3E8', SUB = '#C3CCD3';
const FW = 1024, FH = 500;

const appDir = (id) => path.join(REPO, 'apps', id);
const imgDir = (id, loc) => path.join(appDir(id), 'fastlane/metadata/android', loc, 'images');

function hexA(hex, a) {
  const n = parseInt(hex.slice(1), 16);
  return `rgba(${n >> 16},${(n >> 8) & 255},${n & 255},${a})`;
}
function rr(g, x, y, w, h, r) {
  g.beginPath(); g.moveTo(x + r, y); g.arcTo(x + w, y, x + w, y + h, r); g.arcTo(x + w, y + h, x, y + h, r);
  g.arcTo(x, y + h, x, y, r); g.arcTo(x, y, x + w, y, r); g.closePath();
}
function wrap(g, text, maxW) {
  const lines = []; let cur = '';
  for (const w of text.split(' ')) {
    const t = cur ? cur + ' ' + w : w;
    if (g.measureText(t).width > maxW && cur) { lines.push(cur); cur = w; } else cur = t;
  }
  if (cur) lines.push(cur);
  return lines;
}

// CRC32 für PNG-Chunks (zlib.crc32 erst ab Node 20.15)
const CRC = Array.from({ length: 256 }, (_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
function crc32(buf) { let c = ~0; for (const b of buf) c = CRC[(c ^ b) & 255] ^ (c >>> 8); return ~c >>> 0; }

// PNG-Encoder mit adaptivem Zeilenfilter und zlib 9; ch = 3 (RGB) oder 4 (RGBA)
function encodePNG(w, h, px, ch) {
  const bpl = w * ch, raw = Buffer.alloc((bpl + 1) * h), cand = Buffer.alloc(bpl);
  let prev = Buffer.alloc(bpl);
  for (let y = 0; y < h; y++) {
    const row = px.subarray(y * bpl, (y + 1) * bpl);
    let best = 0, bestSum = Infinity;
    for (let f = 0; f < 5; f++) {
      let sum = 0;
      for (let i = 0; i < bpl; i++) {
        const a = i >= ch ? row[i - ch] : 0, b = prev[i], c = i >= ch ? prev[i - ch] : 0;
        let p;
        if (f === 0) p = 0; else if (f === 1) p = a; else if (f === 2) p = b; else if (f === 3) p = (a + b) >> 1;
        else { const q = a + b - c, pa = Math.abs(q - a), pb = Math.abs(q - b), pc = Math.abs(q - c); p = pa <= pb && pa <= pc ? a : pb <= pc ? b : c; }
        const v = (row[i] - p) & 255; sum += v < 128 ? v : 256 - v;
        if (sum >= bestSum) break;
      }
      if (sum < bestSum) { bestSum = sum; best = f; }
    }
    const o = y * (bpl + 1); raw[o] = best;
    for (let i = 0; i < bpl; i++) {
      const a = i >= ch ? row[i - ch] : 0, b = prev[i], c = i >= ch ? prev[i - ch] : 0;
      let p;
      if (best === 0) p = 0; else if (best === 1) p = a; else if (best === 2) p = b; else if (best === 3) p = (a + b) >> 1;
      else { const q = a + b - c, pa = Math.abs(q - a), pb = Math.abs(q - b), pc = Math.abs(q - c); p = pa <= pb && pa <= pc ? a : pb <= pc ? b : c; }
      cand[i] = (row[i] - p) & 255;
    }
    cand.copy(raw, o + 1);
    prev = row;
  }
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]);
    const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td));
    return Buffer.concat([len, td, crc]);
  };
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = ch === 4 ? 6 : 2;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9, memLevel: 9 })), chunk('IEND', Buffer.alloc(0))]);
}

// RGBA aus Canvas in RGB bzw. RGBA-Puffer
function pixels(g, w, h, ch) {
  const d = g.getImageData(0, 0, w, h).data;
  if (ch === 4) return Buffer.from(d.buffer, d.byteOffset, d.length);
  const out = Buffer.alloc(w * h * 3);
  for (let i = 0, j = 0; i < d.length; i += 4, j += 3) { out[j] = d[i]; out[j + 1] = d[i + 1]; out[j + 2] = d[i + 2]; }
  return out;
}

// Screenshot-Listen aus fastlane/README.md (Abschnitt „Phone-Screenshots“, Blöcke „de-DE:“/„en-US:“)
function shotLists(id) {
  const md = fs.readFileSync(path.join(appDir(id), 'fastlane/README.md'), 'utf8');
  const sec = (md.split('## Phone-Screenshots')[1] || '').split('\n## ')[0];
  const res = {}; let cur = null;
  for (const line of sec.split('\n')) {
    const loc = line.match(/^([a-z]{2}-[A-Z]{2}):\s*$/);
    if (loc) { cur = res[loc[1]] = []; continue; }
    const m = line.match(/^\d+\.\s+`([^`]+)`/);
    if (m && cur) cur.push(m[1]); else if (!line.trim()) cur = null;
  }
  return res;
}

module.exports = { encodePNG, shotLists };
if (require.main !== module) return;

const { createCanvas, loadImage, GlobalFonts } = canvas();
GlobalFonts.registerFromPath(path.join(OV, 'fonts/Inter-Regular.ttf'), 'InterR');
GlobalFonts.registerFromPath(path.join(OV, 'fonts/Inter-SemiBold.ttf'), 'InterSB');
GlobalFonts.registerFromPath(path.join(OV, 'fonts/SpaceGrotesk-Bold.ttf'), 'SG');
const load = (f) => loadImage(fs.readFileSync(f));

// Screenshot ohne Beschnitt auf 2:1 (1200×2400 bzw. 2400×1200), Rand mit Randfarbe des Bildes fortgesetzt
async function playShot(file) {
  const img = await load(file);
  const land = img.width > img.height, TW = land ? 2400 : 1200, TH = land ? 1200 : 2400;
  const s = Math.min(1, TW / img.width, TH / img.height);
  const w = Math.round(img.width * s), h = Math.round(img.height * s);
  const c = createCanvas(w, h), g = c.getContext('2d');
  g.drawImage(img, 0, 0, w, h);
  const src = g.getImageData(0, 0, w, h).data;
  const ox = (TW - w) >> 1, oy = (TH - h) >> 1, out = Buffer.alloc(TW * TH * 3);
  for (let y = 0; y < TH; y++) {
    const sy = Math.min(h - 1, Math.max(0, y - oy));
    for (let x = 0; x < TW; x++) {
      const sx = Math.min(w - 1, Math.max(0, x - ox)), i = (sy * w + sx) * 4, j = (y * TW + x) * 3;
      out[j] = src[i]; out[j + 1] = src[i + 1]; out[j + 2] = src[i + 2];
    }
  }
  return encodePNG(TW, TH, out, 3);
}

function meta(id, loc) {
  const d = path.join(appDir(id), 'fastlane/metadata/android', loc);
  const title = fs.readFileSync(path.join(d, 'title.txt'), 'utf8').trim();
  return { tag: (title.split(' – ')[1] || '').trim(), claim: fs.readFileSync(path.join(d, 'short_description.txt'), 'utf8').trim() };
}

// Feature-Grafik 1024×500: links Icon, Name, Kategorie, Claim; rechts Handy mit Screenshot bzw. Spielgrafik
async function feature(app, loc, shots) {
  const A = app.accent, { tag, claim } = meta(app.id, loc);
  const c = createCanvas(FW, FH), g = c.getContext('2d');
  g.fillStyle = BG; g.fillRect(0, 0, FW, FH);
  let gr = g.createLinearGradient(0, 0, FW * 0.75, FH);
  gr.addColorStop(0, hexA(A, 0.16)); gr.addColorStop(0.55, hexA(A, 0.03)); gr.addColorStop(1, hexA(A, 0));
  g.fillStyle = gr; g.fillRect(0, 0, FW, FH);

  // Handy: hochkant unten angeschnitten, Spiele quer rechts angeschnitten
  const game = GAMES.includes(app.id);
  const B = 9; // Rahmenbreite
  let px, py, sw, sh, src;
  if (game) {
    src = await load(path.join(OV, app.shot));
    sh = 280; sw = Math.round(sh * 1.6); px = 606; py = Math.round((FH - sh) / 2) - B;
  } else {
    const list = shots[loc];
    const first = list.find((f) => !/_land\.png$/.test(f)) || list[0];
    src = await load(path.join(appDir(app.id), 'screenshots', first));
    sw = 252; sh = Math.round(src.height * sw / src.width); px = 700; py = 54;
  }
  const pw = sw + 2 * B, ph = sh + 2 * B, R = 40, r = R - B;
  gr = g.createRadialGradient(px + pw / 2, Math.min(py + ph / 2, FH * 0.6), 30, px + pw / 2, Math.min(py + ph / 2, FH * 0.6), 400);
  gr.addColorStop(0, hexA(A, 0.30)); gr.addColorStop(1, hexA(A, 0));
  g.fillStyle = gr; g.fillRect(0, 0, FW, FH);
  g.save(); g.shadowColor = hexA(A, 0.55); g.shadowBlur = 70; rr(g, px, py, pw, ph, R); g.fillStyle = '#07090B'; g.fill(); g.restore();
  g.save(); rr(g, px + B, py + B, sw, sh, r); g.clip();
  if (game) { // cover-fit
    const s = Math.max(sw / src.width, sh / src.height), w = src.width * s, h = src.height * s;
    g.drawImage(src, px + B + (sw - w) / 2, py + B + (sh - h) / 2, w, h);
  } else g.drawImage(src, px + B, py + B, sw, sh);
  g.restore();
  g.save(); rr(g, px + 1, py + 1, pw - 2, ph - 2, R - 1); g.strokeStyle = 'rgba(255,255,255,0.16)'; g.lineWidth = 2; g.stroke(); g.restore();
  g.save(); rr(g, px + B - 0.5, py + B - 0.5, sw + 1, sh + 1, r); g.strokeStyle = 'rgba(0,0,0,0.6)'; g.lineWidth = 1; g.stroke(); g.restore();

  // Text links, vertikal zentriert
  const TX = 72, TW = px - TX - 48, IS = 112;
  g.font = '25px InterR';
  const lines = wrap(g, claim, Math.min(TW, 500)).slice(0, 3);
  const LH = 35, blockH = IS + 34 + lines.length * LH - 10;
  const y0 = Math.round((FH - blockH) / 2);
  const icon = await load(path.join(appDir(app.id), 'screenshots/icon_store_512.png'));
  g.save(); g.shadowColor = hexA(A, 0.45); g.shadowBlur = 34; rr(g, TX, y0, IS, IS, 28); g.fillStyle = BG; g.fill(); g.restore();
  g.save(); rr(g, TX, y0, IS, IS, 28); g.clip(); g.drawImage(icon, TX, y0, IS, IS); g.restore();
  g.save(); rr(g, TX + 0.5, y0 + 0.5, IS - 1, IS - 1, 28); g.strokeStyle = 'rgba(255,255,255,0.10)'; g.lineWidth = 2; g.stroke(); g.restore();
  const NX = TX + IS + 28;
  g.fillStyle = FG; g.font = '84px SG'; g.fillText(app.id, NX - 3, y0 + 70);
  g.font = '19px InterSB'; g.letterSpacing = '3px';
  let t = tag.toUpperCase();
  while (g.measureText(t).width > TW - (NX - TX) && t.length > 4) t = t.slice(0, -2) + '…';
  g.fillStyle = A; g.fillText(t, NX, y0 + 104); g.letterSpacing = '0px';
  g.fillStyle = SUB; g.font = '25px InterR';
  let ty = y0 + IS + 34 + 18;
  for (const l of lines) { g.fillText(l, TX, ty); ty += LH; }
  if (wrap(g, claim, Math.min(TW, 500)).length > 3) console.warn(`WARN Claim zu lang: ${app.id} ${loc}`);
  return c;
}

async function icon(id) {
  const img = await load(path.join(appDir(id), 'screenshots/icon_store_512.png'));
  const c = createCanvas(512, 512), g = c.getContext('2d');
  g.drawImage(img, 0, 0, 512, 512);
  return encodePNG(512, 512, pixels(g, 512, 512, 4), 4);
}

// Prüft alle erzeugten Dateien gegen die Play-Vorgaben
function check() {
  let bad = 0, n = 0;
  const hdr = (f) => { const b = fs.readFileSync(f); return { w: b.readUInt32BE(16), h: b.readUInt32BE(20), ct: b[25], size: b.length }; };
  const fail = (f, msg) => { bad++; console.log(`FEHLER ${path.relative(REPO, f)}: ${msg}`); };
  for (const app of apps()) for (const loc of LOCALES) {
    const d = imgDir(app.id, loc);
    const fg = path.join(d, 'featureGraphic.png'), ic = path.join(d, 'icon.png');
    if (!fs.existsSync(fg)) fail(fg, 'fehlt'); else { n++; const i = hdr(fg); if (i.w !== FW || i.h !== FH || i.ct !== 2 || i.size > 15 << 20) fail(fg, JSON.stringify(i)); }
    if (!fs.existsSync(ic)) fail(ic, 'fehlt'); else { n++; const i = hdr(ic); if (i.w !== 512 || i.h !== 512 || i.ct !== 6 || i.size >= 1 << 20) fail(ic, JSON.stringify(i)); }
    const sd = path.join(d, 'phoneScreenshots');
    const files = fs.existsSync(sd) ? fs.readdirSync(sd) : [];
    if (files.length < 2) fail(sd, `nur ${files.length} Screenshots`);
    for (const f of files) {
      n++; const i = hdr(path.join(sd, f)), lo = Math.min(i.w, i.h), hi = Math.max(i.w, i.h);
      if (lo < 320 || hi > 3840 || hi > 2 * lo || i.ct !== 2 || i.size >= 8 << 20) fail(path.join(sd, f), JSON.stringify(i));
    }
  }
  console.log(`${n} Dateien geprüft, ${bad} Fehler`);
  process.exit(bad ? 1 : 0);
}

(async () => {
  const args = process.argv.slice(2);
  if (args.includes('--check')) return check();
  const ci = args.indexOf('--collage'), collage = ci >= 0 ? args.splice(ci, 2)[1] : null;
  const only = args[0] && args[0].split(',');
  const list = apps().filter((a) => !only || only.includes(a.id));
  const tiles = [];
  for (const app of list) {
    const shots = shotLists(app.id);
    const iconPng = await icon(app.id);
    for (const loc of LOCALES) {
      const d = imgDir(app.id, loc);
      fs.mkdirSync(d, { recursive: true });
      fs.writeFileSync(path.join(d, 'icon.png'), iconPng);
      const c = await feature(app, loc, shots);
      fs.writeFileSync(path.join(d, 'featureGraphic.png'), encodePNG(FW, FH, pixels(c.getContext('2d'), FW, FH, 3), 3));
      if (loc === 'de-DE') tiles.push(c);
      if (!shots[loc] || !shots[loc].length) { console.warn(`WARN keine Screenshot-Liste: ${app.id} ${loc}`); continue; }
      const sd = path.join(d, 'phoneScreenshots');
      fs.rmSync(sd, { recursive: true, force: true }); fs.mkdirSync(sd);
      for (const [k, f] of shots[loc].entries())
        fs.writeFileSync(path.join(sd, `${k + 1}.png`), await playShot(path.join(appDir(app.id), 'screenshots', f)));
    }
    console.log(app.id);
  }
  if (collage && tiles.length) { // Vorschau: 2 Spalten, halbe Größe
    const S = 0.5, G = 20, tw = FW * S, th = FH * S, rows = Math.ceil(tiles.length / 2);
    const c = createCanvas(G + 2 * (tw + G), G + rows * (th + G)), g = c.getContext('2d');
    g.fillStyle = '#05080A'; g.fillRect(0, 0, c.width, c.height);
    tiles.forEach((t, k) => g.drawImage(t, G + (k % 2) * (tw + G), G + Math.floor(k / 2) * (th + G), tw, th));
    fs.writeFileSync(collage, c.toBuffer('image/png'));
    console.log(collage);
  }
})();
