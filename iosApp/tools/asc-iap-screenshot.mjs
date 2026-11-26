// Review-Screenshot je In-App-Kauf nach App Store Connect laden (nur wenn noch keiner da ist).
// node asc-iap-screenshot.mjs <bundleId> <suffix>=<png> ...   z. B. trial=offer.png full_unlock=thanks.png
// Umgebung: ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH (.p8). Gibt keine Schluessel aus.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';

const [bundleId, ...pairs] = process.argv.slice(2);
const { ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH } = process.env;
if (!bundleId || !pairs.length || !ASC_KEY_ID || !ASC_ISSUER_ID || !ASC_KEY_PATH) {
  console.error('usage: ASC_KEY_ID=… ASC_ISSUER_ID=… ASC_KEY_PATH=… asc-iap-screenshot.mjs <bundleId> <suffix>=<png> ...');
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

const app = (await api('GET', `/v1/apps?filter[bundleId]=${bundleId}`)).data[0];
if (!app) throw new Error(`App ${bundleId} nicht gefunden`);
const iaps = (await api('GET', `/v1/apps/${app.id}/inAppPurchasesV2?limit=200`)).data;

for (const pair of pairs) {
  const [suffix, file] = pair.split('=');
  const productId = `${bundleId}.${suffix}`;
  const iap = iaps.find((x) => x.attributes.productId === productId);
  if (!iap) throw new Error(`IAP ${productId} fehlt`);
  const cur = await api('GET', `/v2/inAppPurchases/${iap.id}/appStoreReviewScreenshot`).catch(() => ({ data: null }));
  const state = cur.data?.attributes?.assetDeliveryState?.state;
  if (cur.data && state !== 'FAILED') {
    console.log(`${productId}: Screenshot vorhanden (${state})`);
    continue;
  }
  if (cur.data) await api('DELETE', `/v1/inAppPurchaseAppStoreReviewScreenshots/${cur.data.id}`);
  const bytes = fs.readFileSync(file);
  const shot = (await api('POST', '/v1/inAppPurchaseAppStoreReviewScreenshots', {
    data: {
      type: 'inAppPurchaseAppStoreReviewScreenshots',
      attributes: { fileName: path.basename(file), fileSize: bytes.length },
      relationships: { inAppPurchaseV2: { data: { type: 'inAppPurchases', id: iap.id } } },
    },
  })).data;
  for (const op of shot.attributes.uploadOperations) {
    const headers = Object.fromEntries((op.requestHeaders || []).map((h) => [h.name, h.value]));
    const r = await fetch(op.url, { method: op.method, headers, body: bytes.subarray(op.offset, op.offset + op.length) });
    if (!r.ok) throw new Error(`Upload-Teil ${op.offset} -> ${r.status}`);
  }
  await api('PATCH', `/v1/inAppPurchaseAppStoreReviewScreenshots/${shot.id}`, {
    data: {
      type: 'inAppPurchaseAppStoreReviewScreenshots',
      id: shot.id,
      attributes: { uploaded: true, sourceFileChecksum: crypto.createHash('md5').update(bytes).digest('hex') },
    },
  });
  console.log(`${productId}: Screenshot hochgeladen (${bytes.length} Bytes)`);
}
