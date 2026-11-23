// Erzeugt docs/overview/index.html aus apps.json (statisch, ohne JS)
// Aufruf: node build-html.js
const fs = require('fs');
const path = require('path');
const { OV, apps } = require('./lib.js');

const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

// Bildgröße aus dem Datei-Header (WebP/PNG), für width/height gegen Layout-Sprünge
function imgSize(rel) {
  const b = fs.readFileSync(path.join(OV, rel));
  if (b.toString('ascii', 1, 4) === 'PNG') return [b.readUInt32BE(16), b.readUInt32BE(20)];
  const t = b.toString('ascii', 12, 16);
  if (t === 'VP8X') return [1 + b.readUIntLE(24, 3), 1 + b.readUIntLE(27, 3)];
  if (t === 'VP8L') { const v = b.readUInt32LE(21); return [(v & 0x3fff) + 1, ((v >> 14) & 0x3fff) + 1]; }
  if (t === 'VP8 ') return [b.readUInt16LE(26) & 0x3fff, b.readUInt16LE(28) & 0x3fff];
  throw new Error(`Bildformat unbekannt: ${rel}`);
}

const cards = apps().map(a => {
  const [sw, sh] = imgSize(a.shot);
  return `    <article class="card${a.landscape ? ' wide' : ''}" id="${esc(a.id)}" style="--a:${esc(a.accent)}">
      <header>
        <img class="icon" src="${esc(a.icon)}" alt="" width="96" height="96">
        <div class="title">
          <h2>${esc(a.id)}</h2>
          <p class="cat">${esc(a.cat)}</p>
        </div>
        <span class="nr">${esc(a.nr)}</span>
      </header>
      <figure class="shot"><img src="${esc(a.shot)}" alt="${esc(a.id)}: ${esc(a.cat)}" width="${sw}" height="${sh}" loading="lazy"></figure>
      <ul>
${a.text.map(t => `        <li>${esc(t)}</li>`).join('\n')}
      </ul>
      <a class="png" href="${esc(a.card)}">Karte als Bild</a>
    </article>`;
}).join('\n');

const html = `<!doctype html>
<html lang="de">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>aapps – Übersicht</title>
  <meta name="description" content="aapps: eine Familie kleiner, werbefreier Android-Apps.">
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <!-- Generiert von tools/build-html.js aus apps.json, nicht von Hand ändern -->
  <header class="top">
    <h1>aapps</h1>
    <p>Eine Familie kleiner, werbefreier Android-Apps: ruhig, hübsch, ohne Tracking und ohne Internet.</p>
  </header>
  <main class="grid">
${cards}
  </main>
  <footer class="bottom">
    <p>Freie Software unter GPLv3.</p>
  </footer>
</body>
</html>
`;
fs.writeFileSync(path.join(OV, 'index.html'), html);
console.log('index.html');
