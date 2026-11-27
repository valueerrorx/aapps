#!/usr/bin/env node
// Google Play Upload-Werkzeug (androidpublisher v3) fuer das aapps-Monorepo
// Dienstkonto-JSON nur per PLAY_KEY_FILE (nie im Repo), AAB per --aab=<datei> oder AAPPS_AAB_DIR
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { androidpublisher } from '@googleapis/androidpublisher';
import { JWT } from 'google-auth-library';

const REPO = process.env.AAPPS_REPO || path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const AAB_DIR = process.env.AAPPS_AAB_DIR || '';
const KEY_FILE = process.env.PLAY_KEY_FILE || '';
const TRACKS = ['internal', 'production'];
const DEFAULT_LANG = 'en-US';
const PRODUCT_ID = 'full_unlock';
const PURCHASE_OPTION_ID = 'buy';
const PRICE_EUR = 1.49;
// ohne In-App-Kauf: Gratis-Spiele und die Bezahl-App bundle (Preis in der Console)
const NO_IAP = new Set(['moovr', 'glidr', 'bundle']);

// Kurz-Locales auf Play-Codes abbilden
const LOCALE_MAP = { de: 'de-DE', en: 'en-US', fr: 'fr-FR', es: 'es-ES', it: 'it-IT', nl: 'nl-NL', pt: 'pt-PT', sl: 'sl' };
// fastlane-Bildtypen -> Play imageType
const SINGLE_IMAGES = ['icon', 'featureGraphic', 'promoGraphic', 'tvBanner'];
const MULTI_IMAGES = ['phoneScreenshots', 'sevenInchScreenshots', 'tenInchScreenshots', 'tvScreenshots', 'wearScreenshots'];
const LIMITS = { title: 30, short: 80, full: 4000 };

const IAP_TEXT = {
  'en-US': (n) => ({ title: 'Unlock for good', description: `Unlock all features of ${n} for good. One-time purchase, no subscription, no ads.` }),
  'de-DE': (n) => ({ title: 'Dauerhaft freischalten', description: `Alle Funktionen von ${n} dauerhaft freischalten. Einmalkauf, kein Abo, keine Werbung.` }),
};

const args = process.argv.slice(2);
const DRY = args.includes('--dry-run');
const opt = (name) => (args.find((a) => a.startsWith(`--${name}=`)) || '').slice(name.length + 3) || undefined;
const statusArg = opt('status');
const TRACK = opt('track') || 'internal';
const AAB_ARG = opt('aab');
const [cmd, app] = args.filter((a) => !a.startsWith('--'));

function usage(code = 1) {
  console.log('Nutzung: node play.mjs <check|listing|upload|iap|all> <app> [--dry-run] [--status=draft|completed] [--track=internal|production] [--aab=datei]');
  process.exit(code);
}

// API-Fehler ohne Request-Config (enthaelt Auth-Header) formatieren
function apiErr(e) {
  const status = e?.response?.status ?? e?.status ?? e?.code ?? '';
  const msg = e?.response?.data?.error?.message ?? e?.errors?.[0]?.message ?? e?.message ?? String(e);
  return `${status ? `HTTP ${status}: ` : ''}${msg}`;
}

function money(currencyCode, value) {
  const units = Math.floor(value);
  const nanos = Math.round((value - units) * 1e9);
  return { currencyCode, units: String(units), nanos };
}
const fmtMoney = (m) => (m ? `${(Number(m.units || 0) + (m.nanos || 0) / 1e9).toFixed(2)} ${m.currencyCode}` : '-');

// PNG/JPEG-Abmessungen ohne Zusatzpakete lesen
function imageSize(file) {
  const b = fs.readFileSync(file);
  if (b.length > 24 && b.readUInt32BE(0) === 0x89504e47) return { w: b.readUInt32BE(16), h: b.readUInt32BE(20), type: 'png' };
  if (b[0] === 0xff && b[1] === 0xd8) {
    let i = 2;
    while (i < b.length) {
      if (b[i] !== 0xff) { i++; continue; }
      const m = b[i + 1];
      if (m >= 0xc0 && m <= 0xcf && ![0xc4, 0xc8, 0xcc].includes(m)) return { w: b.readUInt16BE(i + 7), h: b.readUInt16BE(i + 5), type: 'jpeg' };
      i += 2 + b.readUInt16BE(i + 2);
    }
  }
  return { w: 0, h: 0, type: 'unknown' };
}

// Play-Vorgaben grob pruefen, gibt Warnungen zurueck
function checkImage(kind, file) {
  const { w, h, type } = imageSize(file);
  const size = fs.statSync(file).size;
  const warn = [];
  if (type === 'unknown') warn.push('kein PNG/JPEG');
  if (kind === 'icon') {
    if (w !== 512 || h !== 512) warn.push(`icon muss 512x512 sein (${w}x${h})`);
    if (size > 1024 * 1024) warn.push('icon > 1 MB');
  } else if (kind === 'featureGraphic') {
    if (w !== 1024 || h !== 500) warn.push(`featureGraphic muss 1024x500 sein (${w}x${h})`);
  } else if (kind.endsWith('Screenshots')) {
    const lo = Math.min(w, h), hi = Math.max(w, h);
    if (lo < 320 || hi > 3840) warn.push(`Seiten 320..3840 px verlangt (${w}x${h})`);
    if (hi > 2 * lo) warn.push(`Seitenverhaeltnis > 2:1 (${w}x${h})`);
    if (size > 8 * 1024 * 1024) warn.push('> 8 MB');
  }
  return { w, h, size, type, warn };
}

const mime = (f) => (/\.jpe?g$/i.test(f) ? 'image/jpeg' : 'image/png');
const readTxt = (f) => (fs.existsSync(f) ? fs.readFileSync(f, 'utf8').trim() : null);
const numSort = (a, b) => (parseInt(a, 10) || 0) - (parseInt(b, 10) || 0) || a.localeCompare(b);

function metaDir(appName) {
  return path.join(REPO, 'apps', appName, 'fastlane', 'metadata', 'android');
}

// Alle Locales samt Texten und Bildern aus fastlane-metadata lesen
function readMetadata(appName) {
  const dir = metaDir(appName);
  if (!fs.existsSync(dir)) throw new Error(`keine fastlane-Metadaten: ${dir}`);
  const locales = [];
  for (const folder of fs.readdirSync(dir).sort()) {
    const ld = path.join(dir, folder);
    if (!fs.statSync(ld).isDirectory()) continue;
    const language = LOCALE_MAP[folder] || folder;
    const images = {};
    const imgDir = path.join(ld, 'images');
    for (const k of SINGLE_IMAGES) {
      const f = ['png', 'jpg', 'jpeg'].map((e) => path.join(imgDir, `${k}.${e}`)).find((p) => fs.existsSync(p));
      if (f) images[k] = [f];
    }
    for (const k of MULTI_IMAGES) {
      const d = path.join(imgDir, k);
      if (!fs.existsSync(d)) continue;
      const files = fs.readdirSync(d).filter((f) => /\.(png|jpe?g)$/i.test(f)).sort(numSort).map((f) => path.join(d, f));
      if (files.length) images[k] = files;
    }
    locales.push({
      folder, language, dir: ld, images,
      title: readTxt(path.join(ld, 'title.txt')),
      short: readTxt(path.join(ld, 'short_description.txt')),
      full: readTxt(path.join(ld, 'full_description.txt')),
    });
  }
  return locales;
}

// versionCode wie im Gradle-Plugin: X*10000 + Y*100 + Z
function versionCodeFromRepo(appName) {
  const v = readTxt(path.join(REPO, 'apps', appName, 'version.txt'));
  if (!v) return null;
  const p = v.split('.').map(Number);
  if (p.length !== 3) return null;
  const base = p[0] * 10000 + p[1] * 100 + p[2];
  // optionale versioncode.txt wie im Gradle-Plugin
  const o = Number(readTxt(path.join(REPO, 'apps', appName, 'versioncode.txt')));
  return o > base ? o : base;
}

function releaseNotes(appName, versionCode) {
  const notes = [];
  for (const l of readMetadata(appName)) {
    const t = readTxt(path.join(l.dir, 'changelogs', `${versionCode}.txt`));
    if (t) notes.push({ language: l.language, text: t.slice(0, 500) });
  }
  return notes;
}

function appTitle(appName) {
  const t = readTxt(path.join(metaDir(appName), DEFAULT_LANG, 'title.txt'));
  return t ? t.split(/\s+[–-]\s+/)[0] : appName;
}

// Dienstkonto nur im Speicher, nie ausgeben
function makeAuth() {
  if (!KEY_FILE || !fs.existsSync(KEY_FILE)) throw new Error('Dienstkonto-Datei fehlt (PLAY_KEY_FILE)');
  const c = JSON.parse(fs.readFileSync(KEY_FILE, 'utf8'));
  return new JWT({ email: c.client_email, key: c.private_key, scopes: ['https://www.googleapis.com/auth/androidpublisher'] });
}

let apiClient;
async function api() {
  if (apiClient) return apiClient;
  const auth = makeAuth();
  apiClient = androidpublisher({ version: 'v3', auth });
  return apiClient;
}

// Schreibvorgaenge in einem Edit; commit am Ende, bei Fehler Edit loeschen
async function withEdit(packageName, fn, { commit = true } = {}) {
  const ap = await api();
  const { data } = await ap.edits.insert({ packageName, requestBody: {} });
  const editId = data.id;
  try {
    const res = await fn(ap, editId);
    if (commit) {
      await ap.edits.commit({ packageName, editId });
      console.log(`  Edit ${editId} committet.`);
    } else {
      await ap.edits.delete({ packageName, editId });
    }
    return res;
  } catch (e) {
    try { await ap.edits.delete({ packageName, editId }); console.log(`  Edit ${editId} verworfen.`); } catch { /* schon weg */ }
    throw e;
  }
}

// ---- check ----
async function cmdCheck(appName, pkg) {
  console.log(`check ${pkg}`);
  const ap = await api();
  let edit;
  try {
    edit = (await ap.edits.insert({ packageName: pkg, requestBody: {} })).data;
    console.log(`  Auth ok, Edit angelegt (${edit.id}).`);
  } catch (e) {
    const s = e?.response?.status;
    console.log(`  edits.insert fehlgeschlagen: ${apiErr(e)}`);
    if (s === 401) console.log('  -> Auth abgelehnt: Dienstkonto/Schluessel pruefen.');
    else if (s === 403) console.log('  -> Kein Zugriff: Dienstkonto in der Play Console unter Nutzer & Berechtigungen fuer diese App freigeben, Android Publisher API im GCP-Projekt aktivieren.');
    else if (s === 404) console.log('  -> App nicht gefunden/zugreifbar. Bei frisch angelegten Apps ohne erstes AAB normal: erstes AAB einmal manuell in der Console hochladen, danach geht die API.');
    process.exitCode = 2;
    await probeToken();
    return;
  }
  try {
    const d = (await ap.edits.details.get({ packageName: pkg, editId: edit.id })).data;
    console.log(`  details: defaultLanguage=${d.defaultLanguage || '-'} contactEmail=${d.contactEmail ? 'gesetzt' : '-'} website=${d.contactWebsite || '-'}`);
  } catch (e) { console.log(`  details.get: ${apiErr(e)}`); }
  try {
    const l = (await ap.edits.listings.list({ packageName: pkg, editId: edit.id })).data.listings || [];
    console.log(`  listings: ${l.length ? l.map((x) => `${x.language} "${x.title}"`).join(', ') : 'keine'}`);
  } catch (e) { console.log(`  listings.list: ${apiErr(e)}`); }
  try {
    const t = (await ap.edits.tracks.list({ packageName: pkg, editId: edit.id })).data.tracks || [];
    const desc = t.map((x) => `${x.track}[${(x.releases || []).map((r) => `${r.status}:${(r.versionCodes || []).join('/')}`).join(' ') || 'leer'}]`);
    console.log(`  tracks: ${desc.join(', ') || 'keine'}`);
  } catch (e) { console.log(`  tracks.list: ${apiErr(e)}`); }
  try {
    const p = (await ap.monetization.onetimeproducts.list({ packageName: pkg })).data.oneTimeProducts || [];
    console.log(`  Einmalprodukte: ${p.map((x) => x.productId).join(', ') || 'keine'}`);
  } catch (e) { console.log(`  onetimeproducts.list: ${apiErr(e)}`); }
  try {
    await ap.edits.delete({ packageName: pkg, editId: edit.id });
    console.log('  Edit verworfen (nichts geaendert).');
  } catch (e) { console.log(`  edits.delete: ${apiErr(e)}`); }
}

// Prueft nur, ob ein Access-Token ausgestellt wird (Token selbst wird nie ausgegeben)
async function probeToken() {
  try {
    const t = await makeAuth().getAccessToken();
    console.log(`  Token-Test: ${t?.token ? 'Access-Token erhalten (Dienstkonto-Login ok)' : 'kein Token'}`);
  } catch (e) {
    console.log(`  Token-Test fehlgeschlagen: ${apiErr(e)}`);
  }
}

// ---- listing ----
async function cmdListing(appName, pkg) {
  const locales = readMetadata(appName);
  console.log(`listing ${pkg}${DRY ? ' (dry-run)' : ''}: ${locales.length} Locale(s), Default ${DEFAULT_LANG}`);
  let problems = 0;
  if (!locales.some((l) => l.language === DEFAULT_LANG)) { console.log(`  FEHLER: Default-Locale ${DEFAULT_LANG} fehlt`); problems++; }
  for (const l of locales) {
    const tl = l.title?.length ?? 0, sl = l.short?.length ?? 0, fl = l.full?.length ?? 0;
    const lim = [];
    if (!l.title) lim.push('title fehlt'); else if (tl > LIMITS.title) lim.push(`title ${tl}>${LIMITS.title}`);
    if (!l.short) lim.push('short fehlt'); else if (sl > LIMITS.short) lim.push(`short ${sl}>${LIMITS.short}`);
    if (!l.full) lim.push('full fehlt'); else if (fl > LIMITS.full) lim.push(`full ${fl}>${LIMITS.full}`);
    problems += lim.length;
    console.log(`  ${l.folder}${l.folder !== l.language ? ` -> ${l.language}` : ''}: title ${tl}/${LIMITS.title}, short ${sl}/${LIMITS.short}, full ${fl}/${LIMITS.full}${lim.length ? `  !! ${lim.join(', ')}` : ''}`);
    for (const [kind, files] of Object.entries(l.images)) {
      const infos = files.map((f) => ({ f, ...checkImage(kind, f) }));
      const dims = [...new Set(infos.map((i) => `${i.w}x${i.h}`))].join(',');
      const warns = infos.flatMap((i) => i.warn.map((w) => `${path.basename(i.f)}: ${w}`));
      let extra = '';
      if (kind.endsWith('Screenshots') && (files.length < 2 || files.length > 8)) extra = ` !! ${files.length} Stueck (Play: 2..8)`;
      problems += warns.length + (extra ? 1 : 0);
      console.log(`    ${kind}: ${files.length} x ${dims}${extra}${warns.length ? `  !! ${warns.join('; ')}` : ''}`);
    }
    for (const req of ['icon', 'featureGraphic', 'phoneScreenshots']) {
      if (!l.images[req]) console.log(`    (${req} fehlt in ${l.folder}${l.language === DEFAULT_LANG ? ' – Pflicht fuer Default-Sprache' : ', Play nimmt dann Default-Sprache'})`);
    }
  }
  if (problems) console.log(`  ${problems} Problem(e) gefunden.`);
  if (DRY) {
    console.log('  dry-run: wuerde pro Locale listings.update + pro Bildtyp images.deleteall + images.upload, dann details.patch defaultLanguage=en-US, Edit committen.');
    return;
  }
  if (locales.some((l) => !l.title || !l.short || !l.full)) throw new Error('Texte unvollstaendig, Abbruch');
  await withEdit(pkg, async (ap, editId) => {
    for (const l of locales) {
      await ap.edits.listings.update({
        packageName: pkg, editId, language: l.language,
        requestBody: { language: l.language, title: l.title, shortDescription: l.short, fullDescription: l.full },
      });
      console.log(`  ${l.language}: Texte aktualisiert`);
      for (const [kind, files] of Object.entries(l.images)) {
        await ap.edits.images.deleteall({ packageName: pkg, editId, language: l.language, imageType: kind });
        for (const f of files) {
          await ap.edits.images.upload({
            packageName: pkg, editId, language: l.language, imageType: kind,
            media: { mimeType: mime(f), body: fs.createReadStream(f) },
          });
        }
        console.log(`  ${l.language}: ${kind} ${files.length} hochgeladen`);
      }
    }
    await ap.edits.details.patch({ packageName: pkg, editId, requestBody: { defaultLanguage: DEFAULT_LANG, contactEmail: 'valueerror@gmail.com', contactWebsite: 'https://valueerrorx.github.io/aapps/overview/' } });
    console.log(`  defaultLanguage=${DEFAULT_LANG}`);
  });
}

// ---- upload ----
async function cmdUpload(appName, pkg) {
  if (!TRACKS.includes(TRACK)) throw new Error(`Track ${TRACK} nicht erlaubt (${TRACKS.join('|')})`);
  const aab = AAB_ARG || (AAB_DIR && path.join(AAB_DIR, `${appName}-release.aab`));
  if (!aab || !fs.existsSync(aab)) throw new Error(`AAB fehlt: ${aab || '--aab oder AAPPS_AAB_DIR setzen'}`);
  const st = fs.statSync(aab);
  const vcRepo = versionCodeFromRepo(appName);
  console.log(`upload ${pkg}${DRY ? ' (dry-run)' : ''}: ${path.basename(aab)} (${(st.size / 1e6).toFixed(1)} MB, ${st.mtime.toISOString().slice(0, 16)}), versionCode laut version.txt ${vcRepo ?? '?'}`);
  if (DRY) {
    const notes = vcRepo ? releaseNotes(appName, vcRepo) : [];
    console.log(`  Release-Notes fuer ${vcRepo}: ${notes.map((n) => `${n.language} (${n.text.length} Z.)`).join(', ') || 'keine'}`);
    console.log(`  dry-run: wuerde bundles.upload, Track ${TRACK}, Status ${statusArg || 'draft falls App nie veroeffentlicht, sonst completed'}, Edit committen.`);
    return;
  }
  await withEdit(pkg, async (ap, editId) => {
    let status = statusArg;
    if (!status) {
      const tracks = (await ap.edits.tracks.list({ packageName: pkg, editId })).data.tracks || [];
      const published = tracks.some((t) => (t.releases || []).some((r) => ['completed', 'inProgress', 'halted'].includes(r.status)));
      status = published ? 'completed' : 'draft';
    }
    console.log('  lade AAB hoch ...');
    const b = (await ap.edits.bundles.upload(
      { packageName: pkg, editId, media: { mimeType: 'application/octet-stream', body: fs.createReadStream(aab) } },
      { timeout: 10 * 60 * 1000 },
    )).data;
    const vc = b.versionCode;
    console.log(`  Bundle hochgeladen: versionCode ${vc}`);
    if (vcRepo && vc !== vcRepo) console.log(`  Hinweis: versionCode ${vc} != version.txt (${vcRepo})`);
    const notes = releaseNotes(appName, vc);
    await ap.edits.tracks.update({
      packageName: pkg, editId, track: TRACK,
      requestBody: { track: TRACK, releases: [{ versionCodes: [String(vc)], status, releaseNotes: notes.length ? notes : undefined }] },
    });
    console.log(`  Track ${TRACK}: ${vc} Status ${status}, Notes ${notes.map((n) => n.language).join(',') || '-'}`);
  }).catch((e) => {
    if (/draft/i.test(apiErr(e))) console.log('  Hinweis: App ist noch Entwurf -> mit --status=draft erneut versuchen.');
    throw e;
  });
}

// ---- iap ----
async function buildProduct(appName, pkg) {
  const ap = await api();
  const name = appTitle(appName);
  const listings = Object.entries(IAP_TEXT).map(([languageCode, f]) => ({ languageCode, ...f(name) }));
  // Netto-Basis so, dass AT (20 % USt) bei 1,49 brutto landet; Euro-Laender danach fix 1,49
  const net = Math.round((PRICE_EUR / 1.2) * 100) / 100;
  const conv = (await ap.monetization.convertRegionPrices({
    packageName: pkg, requestBody: { price: money('EUR', net) },
  })).data;
  const regions = Object.values(conv.convertedRegionPrices || {}).map((r) => ({
    regionCode: r.regionCode,
    availability: 'AVAILABLE',
    price: r.price.currencyCode === 'EUR' ? money('EUR', PRICE_EUR) : r.price,
  }));
  const product = {
    packageName: pkg,
    productId: PRODUCT_ID,
    listings,
    purchaseOptions: [{
      purchaseOptionId: PURCHASE_OPTION_ID,
      buyOption: { legacyCompatible: true, multiQuantityEnabled: false },
      regionalPricingAndAvailabilityConfigs: regions,
      newRegionsConfig: {
        availability: 'AVAILABLE',
        eurPrice: conv.convertedOtherRegionsPrice?.eurPrice,
        usdPrice: conv.convertedOtherRegionsPrice?.usdPrice,
      },
    }],
  };
  return { product, regionsVersion: conv.regionVersion?.version, regions, conv, net };
}

async function cmdIap(appName, pkg) {
  if (NO_IAP.has(appName)) { console.log(`iap ${pkg}: uebersprungen (kein In-App-Kauf)`); return; }
  const name = appTitle(appName);
  console.log(`iap ${pkg}${DRY ? ' (dry-run)' : ''}: ${PRODUCT_ID}, Einmalkauf ${PRICE_EUR.toFixed(2)} EUR`);
  for (const [lc, f] of Object.entries(IAP_TEXT)) {
    const t = f(name);
    console.log(`  ${lc}: "${t.title}" – ${t.description} (${t.description.length}/200)`);
  }
  let built;
  try {
    built = await buildProduct(appName, pkg);
  } catch (e) {
    console.log(`  convertRegionPrices fehlgeschlagen: ${apiErr(e)}`);
    if (DRY) { console.log('  dry-run: Laenderpreise nicht ermittelbar; wuerde onetimeproducts.patch (allowMissing) + Kaufoption aktivieren.'); return; }
    throw e;
  }
  const { regions, regionsVersion, conv, net } = built;
  const pick = ['AT', 'DE', 'CH', 'US', 'GB', 'PL', 'IN'];
  const sample = pick.map((c) => regions.find((r) => r.regionCode === c)).filter(Boolean).map((r) => `${r.regionCode} ${fmtMoney(r.price)}`);
  console.log(`  Basis netto ${net} EUR, regionsVersion ${regionsVersion}, ${regions.length} Regionen; z. B. ${sample.join(', ')}`);
  console.log(`  neue Regionen: ${fmtMoney(conv.convertedOtherRegionsPrice?.eurPrice)} / ${fmtMoney(conv.convertedOtherRegionsPrice?.usdPrice)}`);
  if (DRY) { console.log('  dry-run: wuerde onetimeproducts.patch (allowMissing) + Kaufoption "buy" aktivieren.'); return; }
  const ap = await api();
  await ap.monetization.onetimeproducts.patch({
    packageName: pkg, productId: PRODUCT_ID, allowMissing: true,
    'regionsVersion.version': regionsVersion, updateMask: 'listings,purchaseOptions',
    requestBody: built.product,
  });
  console.log('  Produkt angelegt/aktualisiert');
  await ap.monetization.onetimeproducts.purchaseOptions.batchUpdateStates({
    packageName: pkg, productId: PRODUCT_ID,
    requestBody: { requests: [{ activatePurchaseOptionRequest: { packageName: pkg, productId: PRODUCT_ID, purchaseOptionId: PURCHASE_OPTION_ID } }] },
  });
  console.log('  Kaufoption aktiviert');
}

async function main() {
  if (!cmd || !app || !/^[a-z][a-z0-9]*$/.test(app)) usage();
  if (!fs.existsSync(path.join(REPO, 'apps', app))) throw new Error(`unbekannte App: ${app}`);
  const pkg = `solutions.xapient.${app}`;
  switch (cmd) {
    case 'check': return cmdCheck(app, pkg);
    case 'listing': return cmdListing(app, pkg);
    case 'upload': return cmdUpload(app, pkg);
    case 'iap': return cmdIap(app, pkg);
    case 'all':
      await cmdListing(app, pkg);
      await cmdUpload(app, pkg);
      return cmdIap(app, pkg);
    default: usage();
  }
}

main().catch((e) => {
  console.error(`FEHLER: ${apiErr(e)}`);
  process.exit(1);
});
