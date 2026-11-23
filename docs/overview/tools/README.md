# Übersicht neu erzeugen

`docs/overview/apps.json` ist die einzige Datenquelle (Name, Kategorie, Akzentfarbe, 3 Sätze, Bildpfade, Herkunft der Bilder). Alles andere wird daraus erzeugt:

| Schritt | Befehl | Ergebnis |
|---|---|---|
| 1. Bilder holen | `node collect.js [app,…]` | `img/<app>-icon.png` (256 px), `img/<app>-shot.webp` (max. 1600×1080) |
| 2. Karten rendern | `node render.js [app,…]` | `cards/NN-<app>.png` (1080×1600), `captions.txt` |
| 3. HTML bauen | `node build-html.js` | `index.html` (statisch, ohne JS) |

`index.html` und `style.css` brauchen keinen Server: Datei im Browser öffnen genügt. `index.html` nicht von Hand ändern, sondern `apps.json` bzw. `build-html.js`.

## Voraussetzungen
- Node.js ≥ 18.
- `@napi-rs/canvas` für Schritt 1 und 2: in diesem Ordner `npm i @napi-rs/canvas` ausführen (landet in `tools/node_modules`, nicht committen). Alternativ eine vorhandene Installation per Umgebungsvariable nutzen: `NAPI_CANVAS=/pfad/zu/node_modules/@napi-rs/canvas`.
- Schritt 3 braucht nichts außer Node.

## Woher die Bilder kommen
- `collect.js` liest Icons und Screenshots per `git show <ref>:<pfad>` direkt aus den App-Branches (`source.ref` in `apps.json`, z. B. `feat/levlr`). Kein Auschecken und keine Worktree-Pfade nötig. Das Repo wird relativ zu diesem Ordner gefunden (`../../..`), abweichend per `AAPPS_REPO=/pfad/zum/aapps`.
- Sind die Apps später nach `main` gemergt, in `apps.json` einfach `source.ref` auf `main` ändern.
- sleepr liegt in einem eigenen Repo: `SLEEPR_DIR=/pfad/zum/sleepr-checkout node collect.js sleepr`. Das Icon wird aus seinem VectorDrawable nachgebaut. Ohne `SLEEPR_DIR` überspringt `collect.js` sleepr und die vorhandenen Bilder bleiben.
- moovr: Spielgrafik `clumsy-cheatsheet.jpg` aus den Assets (die App hat keine Spiel-Screenshots).
- glidr: `source.shot = "level:23"` rendert Level 23 mit `glidr-level.js` nach.

## glidr-Level
- `glidr-level.js` baut ein Level im Startzustand so auf wie `game.js`: Eis-Kachel + Level-Hintergrund, Objekte aus `allsprites.png` mit Frame-Zuordnung, Zeichenreihenfolge und Physik-Versätzen des Spiels. Es fehlen Animationen, Partikel und Spiel-UI.
- Einzelne Level als PNG: `node glidr-level.js 23 21 --out /tmp`.
- Alle 48 Level zum Sichten: `node glidr-all.js /tmp` (3 Blätter à 16).
- Ref abweichend von `feat/glidr`: `GLIDR_REF=main`.

## Schriften
Inter und Space Grotesk (SIL Open Font License 1.1, Lizenztexte in `../fonts/`) als Teilmengen (Latein inkl. Umlaute), dieselben wie in den Apps.
