// App-Screenshots der bearbeitbaren App-Store-Version hochladen; ersetzt einen Satz nur, wenn sich Bilder geaendert haben.
// node asc-screenshots.mjs <bundleId> <ordner>   Ordner: <locale>/<displayType>/NN_name.png
//   displayType z. B. APP_IPHONE_67 (6.9"), APP_IPAD_PRO_3GEN_129 (13")
// Umgebung: ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH (.p8). Gibt keine Schluessel aus.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';

const [bundleId, dir] = process.argv.slice(2);
const { ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH } = process.env;
if (!bundleId || !dir || !ASC_KEY_ID || !ASC_ISSUER_ID || !ASC_KEY_PATH) {
  console.error('usage: ASC_KEY_ID=… ASC_ISSUER_ID=… ASC_KEY_PATH=… asc-screenshots.mjs <bundleId> <ordner>');
  process.exit(2);
}
const key = fs.readFileSync(ASC_KEY_PATH, 'utf8');
function jwt() {
  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');
  const now = Math.floor(Date.now() / 1000);
  const head = b64({ alg: 'ES256', kid: ASC_KEY_ID, typ: 'JWT' });
  const body = b64({ iss: ASC_ISSUER_ID, iat: now, exp: now + 600, aud: 'appstoreconnect-v1' });
  const sig = crypto.sign('sha256', Buffer.from(`${head}.${body}`), { key, dsaEncoding: 'ieee-p1363' }).toString('base64url');
  return `${head}.${body}.${sig}`;
}
async function api(method, p, body) {
  const r = await fetch(`https://api.appstoreconnect.apple.com${p}`, {
    method,
    headers: { Authorization: `Bearer ${jwt()}`, 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await r.text();
  const j = text ? JSON.parse(text) : {};
  if (!r.ok) throw new Error(`${method} ${p.split('?')[0]} -> ${r.status} ${JSON.stringify((j.errors || []).map((e) => e.detail || e.title))}`);
  return j;
}
const md5 = (b) => crypto.createHash('md5').update(b).digest('hex');

const app = (await api('GET', `/v1/apps?filter[bundleId]=${bundleId}`)).data[0];
const ver = (await api('GET', `/v1/apps/${app.id}/appStoreVersions?filter[platform]=IOS&filter[appStoreState]=PREPARE_FOR_SUBMISSION,DEVELOPER_REJECTED,REJECTED,METADATA_REJECTED`)).data[0];
if (!ver) { console.log('keine bearbeitbare Version, nichts zu tun'); process.exit(0); }
const locs = (await api('GET', `/v1/appStoreVersions/${ver.id}/appStoreVersionLocalizations`)).data;

for (const locale of fs.readdirSync(dir).sort()) {
  const loc = locs.find((l) => l.attributes.locale === locale);
  if (!loc) { console.log(`${locale}: keine Lokalisierung in ASC, uebersprungen`); continue; }
  const sets = (await api('GET', `/v1/appStoreVersionLocalizations/${loc.id}/appScreenshotSets?include=appScreenshots&limit[appScreenshots]=10`));
  for (const type of fs.readdirSync(path.join(dir, locale)).sort()) {
    const files = fs.readdirSync(path.join(dir, locale, type)).filter((f) => f.endsWith('.png')).sort();
    const bytes = files.map((f) => fs.readFileSync(path.join(dir, locale, type, f)));
    let set = sets.data.find((s) => s.attributes.screenshotDisplayType === type);
    if (set) {
      const ids = (set.relationships?.appScreenshots?.data || []).map((d) => d.id);
      const cur = ids.map((id) => (sets.included || []).find((x) => x.id === id)?.attributes?.sourceFileChecksum);
      if (cur.length === bytes.length && cur.every((c, i) => c === md5(bytes[i]))) {
        console.log(`${locale} ${type}: unveraendert (${files.length})`);
        continue;
      }
      for (const id of ids) await api('DELETE', `/v1/appScreenshots/${id}`);
    } else {
      set = (await api('POST', '/v1/appScreenshotSets', {
        data: {
          type: 'appScreenshotSets',
          attributes: { screenshotDisplayType: type },
          relationships: { appStoreVersionLocalization: { data: { type: 'appStoreVersionLocalizations', id: loc.id } } },
        },
      })).data;
    }
    // nacheinander hochladen, die Reihenfolge bleibt erhalten
    for (let i = 0; i < files.length; i++) {
      const shot = (await api('POST', '/v1/appScreenshots', {
        data: {
          type: 'appScreenshots',
          attributes: { fileName: files[i], fileSize: bytes[i].length },
          relationships: { appScreenshotSet: { data: { type: 'appScreenshotSets', id: set.id } } },
        },
      })).data;
      for (const op of shot.attributes.uploadOperations) {
        const headers = Object.fromEntries((op.requestHeaders || []).map((h) => [h.name, h.value]));
        const r = await fetch(op.url, { method: op.method, headers, body: bytes[i].subarray(op.offset, op.offset + op.length) });
        if (!r.ok) throw new Error(`${files[i]}: Upload-Teil ${op.offset} -> ${r.status}`);
      }
      await api('PATCH', `/v1/appScreenshots/${shot.id}`, {
        data: { type: 'appScreenshots', id: shot.id, attributes: { uploaded: true, sourceFileChecksum: md5(bytes[i]) } },
      });
    }
    console.log(`${locale} ${type}: ${files.length} hochgeladen`);
  }
}
