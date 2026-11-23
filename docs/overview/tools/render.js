// Rendert die PNG-Karten (1080×1600) nach docs/overview/cards aus apps.json + img/
// Aufruf: node render.js [app,...]
const fs = require('fs');
const path = require('path');
const { OV, canvas, apps } = require('./lib.js');
const { createCanvas, loadImage, GlobalFonts } = canvas();

GlobalFonts.registerFromPath(path.join(OV, 'fonts/Inter-Regular.ttf'), 'InterR');
GlobalFonts.registerFromPath(path.join(OV, 'fonts/Inter-SemiBold.ttf'), 'InterSB');
GlobalFonts.registerFromPath(path.join(OV, 'fonts/SpaceGrotesk-Bold.ttf'), 'SG');

const W = 1080, H = 1600;
const BG = '#10161B', FG = '#DCE3E8', MUTED = '#8A97A2';
const TX = 112, TW = W - TX - 72, FS = 34;

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
// Umbruch je Satz mit eigener Schrift (Satz 1 halbfett)
function textLayout(g, app, fs_) {
  const lh = Math.round(fs_ * 1.36), gap = Math.round(fs_ * 0.55);
  const lines = app.text.map((t, k) => { g.font = `${fs_}px ${k === 0 ? 'InterSB' : 'InterR'}`; return wrap(g, t, TW); });
  return { lines, lh, gap, total: lines.reduce((n, l) => n + l.length * lh, 0) + gap * 2 };
}

async function card(app) {
  const c = createCanvas(W, H); const g = c.getContext('2d');
  const A = app.accent;
  g.fillStyle = BG; g.fillRect(0, 0, W, H);
  // weicher Akzent-Schein hinter dem Bild und am Kopf
  let rg = g.createRadialGradient(W / 2, 700, 40, W / 2, 700, 720);
  rg.addColorStop(0, hexA(A, 0.20)); rg.addColorStop(1, hexA(A, 0));
  g.fillStyle = rg; g.fillRect(0, 0, W, H);
  rg = g.createLinearGradient(0, 0, 0, 260);
  rg.addColorStop(0, hexA(A, 0.10)); rg.addColorStop(1, hexA(A, 0));
  g.fillStyle = rg; g.fillRect(0, 0, W, 260);

  // Kopf: Icon, Name, Kategorie, Nummer
  const icon = await loadImage(fs.readFileSync(path.join(OV, app.icon)));
  const IS = 156, IX = 72, IY = 72;
  g.save(); g.shadowColor = hexA(A, 0.45); g.shadowBlur = 40; rr(g, IX, IY, IS, IS, 38); g.fillStyle = BG; g.fill(); g.restore();
  g.save(); rr(g, IX, IY, IS, IS, 38); g.clip(); g.drawImage(icon, IX, IY, IS, IS); g.restore();
  g.save(); rr(g, IX + 0.5, IY + 0.5, IS - 1, IS - 1, 38); g.strokeStyle = 'rgba(255,255,255,0.10)'; g.lineWidth = 2; g.stroke(); g.restore();
  g.fillStyle = FG; g.font = '104px SG'; g.fillText(app.id, 262, 168);
  g.fillStyle = A; g.font = '30px InterSB'; g.letterSpacing = '4px'; g.fillText(app.cat.toUpperCase(), 266, 216); g.letterSpacing = '0px';
  g.fillStyle = MUTED; g.font = '28px InterSB'; g.textAlign = 'right'; g.fillText(app.nr, W - 72, 108); g.textAlign = 'left';

  // Bild; Querformat: Bild + Text gemeinsam vertikal zentriert
  const img = await loadImage(fs.readFileSync(path.join(OV, app.shot)));
  const BY = 280, BW = W - 140, BH = 830;
  const s = Math.min(BW / img.width, BH / img.height);
  const w = Math.round(img.width * s), h = Math.round(img.height * s), x = Math.round((W - w) / 2);
  const tl = textLayout(g, app, FS);
  const y = app.landscape ? Math.round(BY + (H - 56 - BY - (h + 76 + tl.total - tl.lh * 0.3)) / 2) : Math.round(BY + (BH - h) / 2);
  const r = app.landscape ? 32 : 48;
  g.save(); g.shadowColor = hexA(A, 0.35); g.shadowBlur = 70; rr(g, x, y, w, h, r); g.fillStyle = BG; g.fill(); g.restore();
  g.save(); rr(g, x, y, w, h, r); g.clip(); g.drawImage(img, x, y, w, h); g.restore();
  g.save(); rr(g, x + 1, y + 1, w - 2, h - 2, r); g.strokeStyle = 'rgba(255,255,255,0.12)'; g.lineWidth = 2; g.stroke(); g.restore();

  // 3 Sätze mit Akzent-Punkt
  let ty = (app.landscape ? y + h : BY + BH) + 76;
  if (ty + tl.total - tl.lh * 0.3 > H - 56) console.warn(`WARN Text zu lang: ${app.id}`);
  tl.lines.forEach((ls, k) => {
    g.fillStyle = A; g.beginPath(); g.arc(TX - 30, ty - FS * 0.33, 7, 0, Math.PI * 2); g.fill();
    g.fillStyle = k === 0 ? FG : '#C3CCD3'; g.font = `${FS}px ${k === 0 ? 'InterSB' : 'InterR'}`;
    for (const l of ls) { g.fillText(l, TX, ty); ty += tl.lh; }
    ty += tl.gap;
  });

  fs.writeFileSync(path.join(OV, app.card), c.toBuffer('image/png'));
  console.log(app.card);
}

(async () => {
  fs.mkdirSync(path.join(OV, 'cards'), { recursive: true });
  const only = process.argv[2] && process.argv[2].split(',');
  const list = apps();
  for (const a of list) if (!only || only.includes(a.id)) await card(a);
  fs.writeFileSync(path.join(OV, 'captions.txt'), list.map(a => `${a.id}: ${a.caption}`).join('\n') + '\n');
})();
