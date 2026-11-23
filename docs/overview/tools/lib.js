// Gemeinsame Helfer: Pfade, Canvas-Laden, Dateien aus Git-Branches lesen
const path = require('path');
const { execFileSync } = require('child_process');

// docs/overview und Repo-Wurzel relativ zu diesem Skript
const OV = path.resolve(__dirname, '..');
const REPO = process.env.AAPPS_REPO || path.resolve(__dirname, '../../..');

// @napi-rs/canvas: normal per npm, alternativ Pfad über NAPI_CANVAS
function canvas() {
  try { return require(process.env.NAPI_CANVAS || '@napi-rs/canvas'); }
  catch (e) {
    console.error('@napi-rs/canvas fehlt: `npm i @napi-rs/canvas` in docs/overview/tools oder NAPI_CANVAS=/pfad/zu/@napi-rs/canvas setzen.');
    process.exit(1);
  }
}

// Datei aus einem Branch lesen, ohne ihn auszuchecken
function gitFile(ref, file) {
  return execFileSync('git', ['-C', REPO, 'show', `${ref}:${file}`], { maxBuffer: 64 << 20 });
}

const apps = () => JSON.parse(require('fs').readFileSync(path.join(OV, 'apps.json'), 'utf8'));

module.exports = { OV, REPO, canvas, gitFile, apps };
