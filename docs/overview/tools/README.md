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
- sleepr liegt wie alle anderen unter `apps/sleepr`: Screenshot `idle.png` und Store-Icon aus `apps/sleepr/screenshots/` (bei sleepr sind Screenshots ohne Suffix deutsch, englische heißen `_en`).
- moovr: Spielgrafik `clumsy-cheatsheet.jpg` aus den Assets (die App hat keine Spiel-Screenshots).
- glidr: `source.shot = "level:23"` rendert Level 23 mit `glidr-level.js` nach.

## glidr-Level
- `glidr-level.js` baut ein Level im Startzustand so auf wie `game.js`: Eis-Kachel + Level-Hintergrund, Objekte aus `allsprites.png` mit Frame-Zuordnung, Zeichenreihenfolge und Physik-Versätzen des Spiels. Es fehlen Animationen, Partikel und Spiel-UI.
- Einzelne Level als PNG: `node glidr-level.js 23 21 --out /tmp`.
- Alle 48 Level zum Sichten: `node glidr-all.js /tmp` (3 Blätter à 16).
- Ref abweichend von `feat/glidr`: `GLIDR_REF=main`.

## Play-Store-Grafiken
`node store-images.js [app,…] [--collage vorschau.png]` schreibt je App und Sprache (de-DE, en-US) nach `apps/<app>/fastlane/metadata/android/<locale>/images/`:
- `phoneScreenshots/1.png …`: Auswahl und Reihenfolge aus `apps/<app>/fastlane/README.md` (Abschnitt „Phone-Screenshots“), Quelle `apps/<app>/screenshots/` im Arbeitsbaum. Ohne Beschnitt auf 1200×2400 bzw. 2400×1200 (2:1) gebracht, der Rand setzt die Randpixel des Bildes fort. moovr und glidr bekommen keine (nur vom Gerät).
- `featureGraphic.png`: 1024×500, RGB ohne Alpha. Icon, Name, Kategorie aus `title.txt`, Claim aus `short_description.txt`, rechts das erste Hochformat-Bild der Liste im Handy-Rahmen (moovr/glidr: Spielgrafik aus `img/`).
- `icon.png`: 512×512 RGBA aus `screenshots/icon_store_512.png`.
- `--collage`: alle de-DE-Feature-Grafiken als Vorschau in 2 Spalten.

`node store-images.js --check` prüft alle Dateien gegen die Play-Vorgaben (Maße, max. 2:1, Alpha, Dateigröße).

## Schriften
Inter und Space Grotesk (SIL Open Font License 1.1, Lizenztexte in `../fonts/`) als Teilmengen (Latein inkl. Umlaute), dieselben wie in den Apps.
