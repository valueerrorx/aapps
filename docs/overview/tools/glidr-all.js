// Kontaktabzug aller 48 glidr-Level zum Sichten (3 Blätter à 16)
// Aufruf: node glidr-all.js [AUSGABEORDNER]
const fs = require('fs');
const path = require('path');
const { OV, canvas } = require('./lib.js');
const { renderLevel } = require('./glidr-level.js');
const { createCanvas, GlobalFonts } = canvas();
GlobalFonts.registerFromPath(path.join(OV, 'fonts/Inter-SemiBold.ttf'), 'InterSB');
const out = process.argv[2] || process.cwd();
(async () => {
  const TW = 480, TH = 300, cols = 4, per = 16;
  for (let s = 0; s < 3; s++) {
    const c = createCanvas(cols * TW, (per / cols) * TH); const g = c.getContext('2d');
    g.fillStyle = '#222'; g.fillRect(0, 0, c.width, c.height);
    for (let k = 0; k < per; k++) {
      const n = s * per + k; const { canvas: lv } = await renderLevel(n);
      const sc = Math.min((TW - 8) / lv.width, (TH - 36) / lv.height);
      const x = (k % cols) * TW + 4, y = Math.floor(k / cols) * TH + 30;
      g.drawImage(lv, x, y, lv.width * sc, lv.height * sc);
      g.fillStyle = '#fff'; g.font = '22px InterSB'; g.fillText(`${n}  ${lv.width}x${lv.height}`, x, y - 6);
    }
    fs.writeFileSync(path.join(out, `glidr-sheet-${s}.png`), c.toBuffer('image/png'));
  }
})();
