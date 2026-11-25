# glidr – Store-Grafiken

Quelle: `apps/glidr/screenshots/`. Dort liegen nur `icon_masks.png`, `icon_store_512.png` und `splash_pre31.png`.
Icon und Feature-Grafik erzeugt mit `docs/overview/tools/store-images.js` nach `metadata/android/<locale>/images/`.

## Phone-Screenshots
Fehlen. Das Spiel läuft in einer WebView, Robolectric kann es nicht rendern.
- Nötig: mind. 2, besser 4 bis 6 Bilder im Querformat vom Gerät (Levelauswahl mit Sternen, 2 bis 3 Level mit Schanzen/Wasserlöchern, Levelende).
- Die Spieltexte sind englisch, dieselben Bilder passen für de-DE und en-US.
- Notlösung: `docs/overview/tools/glidr-level.js` rendert Level im Startzustand (ohne Spiel-UI), z. B. `node glidr-level.js 23 --out /tmp`. Echte Gerätebilder sind besser.

## Icon und Feature-Grafik
- Icon 512: `screenshots/icon_store_512.png`
- Feature-Grafik 1024×500: erzeugt
