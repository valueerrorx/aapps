// Holt Icons und Screenshots aus den App-Branches nach docs/overview/img
// Aufruf: node collect.js [app,...]   (sleepr braucht SLEEPR_SHOT = Pfad zum Screenshot, siehe apps.json)
const fs = require('fs');
const path = require('path');
const { OV, canvas, gitFile, apps } = require('./lib.js');
const { createCanvas, loadImage } = canvas();
const { renderLevel } = require('./glidr-level.js');

const ICON = 256, SHOT_H = 1080, SHOT_W = 1600;

// sleepr-Icon aus seinem VectorDrawable (ic_launcher_fg + launcher_bg) nachgebaut
const SLEEPR_ICON = `<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108">
<defs><radialGradient id="g" cx="50%" cy="45%" r="70%"><stop offset="0" stop-color="#1D2831"/><stop offset="1" stop-color="#10161B"/></radialGradient></defs>
<rect width="108" height="108" fill="url(#g)"/>
<g transform="translate(54 54) scale(1.25) translate(-54 -51)">
<path fill="#86B7CC" d="M34,51h4v6h-4zM42,44h4v20h-4zM50,36h4v36h-4zM58,42h4v24h-4zM66,48h4v12h-4z"/>
<path fill="#DCE3E8" d="M70,30a9,9 0,1 0,8 13a7,7 0,1 1,-8 -13z"/></g></svg>`;

// Auf Maximalgröße verkleinern, nie vergrößern
function fit(src, maxW, maxH) {
  const s = Math.min(1, maxW / src.width, maxH / src.height);
  const c = createCanvas(Math.round(src.width * s), Math.round(src.height * s));
  c.getContext('2d').drawImage(src, 0, 0, c.width, c.height);
  return c;
}

async function collect(a) {
  const s = a.source;
  let icon, shot;
  if (s.repo === 'SLEEPR_DIR') {
    // "env:NAME" = Bilddatei aus Umgebungsvariable, sonst Pfad im sleepr-Checkout
    const file = s.shot.startsWith('env:') ? process.env[s.shot.slice(4)] : process.env.SLEEPR_DIR && path.join(process.env.SLEEPR_DIR, s.shot);
    if (!file) { console.warn(`sleepr übersprungen: ${s.shot.startsWith('env:') ? s.shot.slice(4) : 'SLEEPR_DIR'} nicht gesetzt`); return; }
    icon = await loadImage(Buffer.from(SLEEPR_ICON));
    shot = await loadImage(fs.readFileSync(file));
  } else {
    icon = await loadImage(gitFile(s.ref, s.icon));
    shot = s.shot.startsWith('level:') ? (await renderLevel(s.shot.slice(6))).canvas : await loadImage(gitFile(s.ref, s.shot));
  }
  fs.writeFileSync(path.join(OV, a.icon), fit(icon, ICON, ICON).toBuffer('image/png'));
  fs.writeFileSync(path.join(OV, a.shot), fit(shot, SHOT_W, SHOT_H).toBuffer('image/webp', 90));
  console.log(a.id, 'ok');
}

(async () => {
  fs.mkdirSync(path.join(OV, 'img'), { recursive: true });
  const only = process.argv[2] && process.argv[2].split(',');
  for (const a of apps()) if (!only || only.includes(a.id)) await collect(a);
})();
