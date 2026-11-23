# sleepr – Store-Grafiken

Quelle: `apps/sleepr/screenshots/`. Achtung: bei sleepr sind Dateien ohne Suffix deutsch, `_en` englisch.
Ziel für fastlane supply: `metadata/android/<locale>/images/` (`icon.png`, `featureGraphic.png`, `phoneScreenshots/`, `sevenInchScreenshots/`). Noch nicht kopiert.

## Phone-Screenshots
de-DE:
1. `idle.png` – Mixer mit Standardmischung
2. `playing.png` – läuft, Timer mit Restzeit
3. `idle_bottom.png` – Liste unten mit Timer
4. `settings.png` – Einstellungen mit Mischungen
5. `idle_land.png` – Querformat

en-US:
1. `idle_en.png` – Mixer
2. `settings_en.png` – Einstellungen

Tablet (optional, nur DE): `idle_tablet.png`, `settings_tablet.png`.

## Icon und Feature-Grafik
- Icon 512: ja, `screenshots/icon_store_512.png`
- Feature-Grafik 1024×500: fehlt

## Offen
- EN hat nur 2 Bilder: `playing_en`, `idle_land_en` und ein EN-Tablet-Bild aufnehmen.
- Seitenverhältnis 1078×2399 ist über 2:1, vor dem Upload anpassen (siehe `docs/PLAY-CHECKLIST.md`).
