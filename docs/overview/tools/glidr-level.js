// Rendert ein glidr-Level (Startzustand) so, wie game.js es aufbaut
// Aufruf: node glidr-level.js <level...> [--out DIR]
const fs = require('fs');
const path = require('path');
const { canvas, gitFile } = require('./lib.js');
const { createCanvas, loadImage } = canvas();

const REF = process.env.GLIDR_REF || 'feat/glidr';
const A = 'apps/glidr/src/main/assets/assets';
const read = rel => gitFile(REF, `${A}/${rel}`);

// Gruppen in Zeichenreihenfolge (setupDisplayGroups)
const ORDER = ['ground', 'water', 'freeze', 'mines', 'ramp', 'block', 'withoutbody', 'finish', 'ice', 'enemy', 'wood', 'powerup', 'asset', 'player', 'plant'];

// gid -> {frame, group} wie setupObjects/setupPlants
function mapGid(gid) {
  const plants = { 1: 'bush-tileset-0', 11: 'grass-tileset-0', 21: 'longplants-tileset-0' };
  for (const [b, p] of Object.entries(plants)) {
    const d = gid - b;
    if (d >= 0 && d < (b == 1 ? 7 : 8)) return { frame: `${p}${d + 1}.png`, group: 'plant' };
  }
  const g31 = { 31: ['ground-tileset-01.png', 'ground'], 32: ['ground-tileset-02.png', 'ground'], 33: ['ground-tileset-03.png', 'water'],
    34: ['ground-tileset-04.png', 'water'], 35: ['waterhole-1.png', 'water'], 36: ['waterhole-2.png', 'water'],
    37: ['ground-tileset-07.png', 'ramp'], 38: ['ground-tileset-07.png', 'ramp'] };
  if (g31[gid]) return { frame: g31[gid][0], group: g31[gid][1] };
  if (gid >= 41 && gid < 82) {
    const a = `cowsonice-tileset-64-${gid - 40}.png`;
    const special = { 41: [a, 'wood'], 48: ['help1.png', 'help'], 49: ['windleft.png', 'wind'], 70: ['tuxbomb-spritesheet-1.png', 'asset'],
      71: ['cow-spritesheet-07.png', 'player'], 72: ['tuxminibomb-spritesheet-1.png', 'asset'], 73: ['tuxice-spritesheet-1.png', 'asset'],
      74: ['tuxanchor-spritesheet-1.png', 'asset'], 75: ['watermine.png', 'mines'], 76: ['clover3.png', 'powerup'],
      77: ['bumper-spritesheet-1.png', 'mines'], 78: ['tuxtimebomb-spritesheet-1.png', 'asset'] };
    if (special[gid]) return { frame: special[gid][0], group: special[gid][1] };
    if ([42, 65, 69].includes(gid)) return { frame: a, group: 'ice' };
    if (gid === 53 || gid === 54) return { frame: a, group: 'withoutbody' };
    if (gid === 55 || gid === 56) return { frame: a, group: 'enemy' };
    return { frame: a, group: 'block' };
  }
  if (gid >= 82 && gid < 121) {
    const a = `cowsonice-tileset-32-${gid - 81}.png`;
    if (gid <= 85) return { frame: a, group: 'finish' };
    if (gid >= 106 && gid <= 108) return { frame: a, group: 'wood' };
    return { frame: a, group: 'withoutbody' };
  }
  return null; // ice-terrain usw. erzeugt das Spiel nicht als Objekt
}

let atlasCache;
async function atlas() {
  if (!atlasCache) {
    const img = await loadImage(read('sprites/allsprites.png'));
    const fr = {}; for (const v of JSON.parse(read('sprites/allsprites.jsona').toString()).frames) fr[v.filename] = v.frame;
    atlasCache = { img, fr };
  }
  return atlasCache;
}

async function renderLevel(n) {
  const { img: sheet, fr } = await atlas();
  const lv = JSON.parse(read(`levels/${n}.json`).toString());
  const W = lv.width * lv.tilewidth, H = lv.height * lv.tileheight;
  const c = createCanvas(W, H); const g = c.getContext('2d');
  const missing = new Set();

  // Hintergrund: Eis gekachelt + Level-Bild
  const ice = fr['ice.png'];
  for (let x = 0; x < W; x += ice.w) for (let y = 0; y < H; y += ice.h) g.drawImage(sheet, ice.x, ice.y, ice.w, ice.h, x, y, ice.w, ice.h);
  g.drawImage(await loadImage(read(`levels/${n}.png`)), 0, 0);

  const groups = Object.fromEntries(ORDER.map(k => [k, []]));
  const objs = lv.layers.find(l => l.name === 'objects').objects.filter(o => o.gid && o.visible !== false);
  // Erzeugung wie im Spiel: Schleife über gid, innerhalb Dateireihenfolge
  const gids = [...new Set(objs.map(o => o.gid))].sort((a, b) => a - b);
  let rnd = 7;
  for (const gid of gids) for (const o of objs.filter(x => x.gid === gid)) {
    const m = mapGid(gid); if (!m) continue;
    const f = fr[m.frame]; if (!f) { missing.add(m.frame); continue; }
    const top = o.y - f.h; // createFromObjects: y -= height
    if (m.group === 'help') { const hf = fr[o.name + '.png']; if (hf) groups.ground.push({ f: hf, x: o.x, y: top }); continue; }
    if (m.group === 'wind') { const wf = fr[`wind${o.name}.png`]; if (wf) groups.ground.push({ f: wf, x: o.x, y: top, alpha: 0.8 }); continue; }
    if (m.group === 'plant' || m.group === 'withoutbody') { groups[m.group].push({ f, x: o.x, y: top }); continue; }
    // Physik-Objekte: Anker 0.5, Körper um halbe Größe + Offsets verschoben
    let s = 1, cx = o.x + f.w / 2, cy = top + f.h / 2, ang = 0, alpha = 1;
    if (m.group === 'asset') { s = 0.7; cx = o.x + f.w * s / 2 + 6; cy = top + f.h * s / 2 + 6; }
    if (m.group === 'player') { cx -= 24; cy += 24; }
    if (m.frame.startsWith('bumper')) { cx -= 12; cy += 12; }
    if (m.group === 'ramp' && o.name === 'rampfromright') ang = 180;
    if (m.group === 'enemy') { rnd = (rnd * 9301 + 49297) % 233280; ang = Math.floor(rnd / 233280 * 51); } // im Spiel zufällig 0–50°
    if (m.group === 'water') {
      alpha = 0.8;
      if (m.frame === 'waterhole-1.png') cx -= 24; // pivot.x = 24
      else cx -= 12; // Körper y +24, pivot (12,24)
      groups.freeze.push({ f: fr['freeze1.png'], x: o.x - 15, y: top });
    }
    groups[m.group].push({ f, cx, cy, s, ang, alpha });
  }
  for (const k of ORDER) for (const sp of groups[k]) {
    g.save(); g.globalAlpha = sp.alpha || 1;
    const f = sp.f;
    if (sp.cx === undefined) g.drawImage(sheet, f.x, f.y, f.w, f.h, sp.x, sp.y, f.w, f.h);
    else {
      g.translate(sp.cx, sp.cy); g.rotate((sp.ang || 0) * Math.PI / 180); g.scale(sp.s, sp.s);
      g.drawImage(sheet, f.x, f.y, f.w, f.h, -f.w / 2, -f.h / 2, f.w, f.h);
    }
    g.restore();
  }
  return { canvas: c, missing: [...missing], count: objs.length };
}
module.exports = { renderLevel };

if (require.main === module) {
  (async () => {
    const args = process.argv.slice(2);
    const oi = args.indexOf('--out');
    const out = oi >= 0 ? args.splice(oi, 2)[1] : process.cwd();
    for (const n of args) {
      const r = await renderLevel(n);
      fs.writeFileSync(path.join(out, `level-${n}.png`), r.canvas.toBuffer('image/png'));
      console.log(n, r.canvas.width, r.canvas.height, 'obj', r.count, 'missing', r.missing.join(','));
    }
  })();
}
