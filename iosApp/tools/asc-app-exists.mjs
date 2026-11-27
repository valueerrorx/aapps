// Gibt "true" aus, wenn die App mit dieser Bundle-ID in App Store Connect angelegt ist, sonst "false".
// node asc-app-exists.mjs <bundleId>   Umgebung: ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH (.p8). Gibt keine Schluessel aus.
import fs from 'node:fs';
import crypto from 'node:crypto';

const [bundleId] = process.argv.slice(2);
const { ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH } = process.env;
if (!bundleId || !ASC_KEY_ID || !ASC_ISSUER_ID || !ASC_KEY_PATH) {
  console.error('usage: ASC_KEY_ID=… ASC_ISSUER_ID=… ASC_KEY_PATH=… asc-app-exists.mjs <bundleId>');
  process.exit(2);
}
const key = fs.readFileSync(ASC_KEY_PATH, 'utf8');
const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');
const now = Math.floor(Date.now() / 1000);
const head = b64({ alg: 'ES256', kid: ASC_KEY_ID, typ: 'JWT' });
const body = b64({ iss: ASC_ISSUER_ID, iat: now, exp: now + 600, aud: 'appstoreconnect-v1' });
const sig = crypto.sign('sha256', Buffer.from(`${head}.${body}`), { key, dsaEncoding: 'ieee-p1363' }).toString('base64url');
const r = await fetch(`https://api.appstoreconnect.apple.com/v1/apps?filter[bundleId]=${bundleId}&fields[apps]=bundleId`, {
  headers: { Authorization: `Bearer ${head}.${body}.${sig}` },
});
if (!r.ok) {
  console.error(`App-Abfrage -> ${r.status}`);
  process.exit(1);
}
const j = await r.json();
console.log((j.data || []).some((a) => a.attributes.bundleId === bundleId) ? 'true' : 'false');
