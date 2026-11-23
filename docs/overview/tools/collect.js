// Holt Icons und Screenshots aus den App-Branches nach docs/overview/img
// Aufruf: node collect.js [app,...]
const fs = require('fs');
const path = require('path');
const { OV, canvas, gitFile, apps } = require('./lib.js');
const { createCanvas, loadImage } = canvas();
const { renderLevel } = require('./glidr-level.js');

const ICON = 256, SHOT_H = 1080, SHOT_W = 1600;

// Auf Maximalgröße verkleinern, nie vergrößern
function fit(src, maxW, maxH) {
  const s = Math.min(1, maxW / src.width, maxH / src.height);
  const c = createCanvas(Math.round(src.width * s), Math.round(src.height * s));
  c.getContext('2d').drawImage(src, 0, 0, c.width, c.height);
  return c;
}

async function collect(a) {
  const s = a.source;
  const icon = await loadImage(gitFile(s.ref, s.icon));
  const shot = s.shot.startsWith('level:') ? (await renderLevel(s.shot.slice(6))).canvas : await loadImage(gitFile(s.ref, s.shot));
  fs.writeFileSync(path.join(OV, a.icon), fit(icon, ICON, ICON).toBuffer('image/png'));
  fs.writeFileSync(path.join(OV, a.shot), fit(shot, SHOT_W, SHOT_H).toBuffer('image/webp', 90));
  console.log(a.id, 'ok');
}

(async () => {
  fs.mkdirSync(path.join(OV, 'img'), { recursive: true });
  const only = process.argv[2] && process.argv[2].split(',');
  for (const a of apps()) if (!only || only.includes(a.id)) await collect(a);
})();
