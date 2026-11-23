// Prüft index.html: Tag-Balance, alle src/href-Ziele vorhanden, alle Karten/Bilder aus apps.json da
const fs = require('fs');
const path = require('path');
const { OV, apps } = require('./lib.js');

let bad = 0;
const fail = m => { console.error('FEHLER', m); bad++; };
const html = fs.readFileSync(path.join(OV, 'index.html'), 'utf8');

// Tag-Balance (void-Elemente ausgenommen)
const VOID = new Set(['meta', 'link', 'img', 'br', 'hr', 'input', 'source']);
const stack = [];
for (const m of html.replace(/<!--[\s\S]*?-->/g, '').matchAll(/<(\/?)([a-zA-Z][\w-]*)[^>]*?>/g)) {
  const [, close, tag] = m; const t = tag.toLowerCase();
  if (VOID.has(t) || t === '!doctype') continue;
  if (!close) stack.push(t);
  else if (stack.pop() !== t) fail(`Tag </${t}> passt nicht`);
}
if (stack.length) fail(`offene Tags: ${stack.join(',')}`);
if (!/^<!doctype html>/i.test(html)) fail('doctype fehlt');

// Lokale Verweise in HTML und CSS
const refs = [...html.matchAll(/(?:src|href)="([^"#]+)"/g)].map(m => m[1]);
const css = fs.readFileSync(path.join(OV, 'style.css'), 'utf8');
refs.push(...[...css.matchAll(/url\("([^"]+)"\)/g)].map(m => m[1]));
for (const r of refs) {
  if (/^[a-z]+:/i.test(r)) fail(`externer Verweis: ${r}`);
  else if (!fs.existsSync(path.join(OV, r))) fail(`fehlt: ${r}`);
}
for (const a of apps()) for (const k of ['icon', 'shot', 'card']) if (!fs.existsSync(path.join(OV, a[k]))) fail(`fehlt: ${a[k]}`);

console.log(`${refs.length} Verweise geprüft, ${bad} Fehler`);
process.exit(bad ? 1 : 0);
