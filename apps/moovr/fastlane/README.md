# moovr – Store-Grafiken

Quelle: `apps/moovr/screenshots/`. Dort liegen nur `icon_masks.png`, `icon_store_512.png` und `splash_pre31.png`.
Ziel für fastlane supply: `metadata/android/<locale>/images/`. Noch nicht kopiert.

## Phone-Screenshots
Fehlen. Das Spiel läuft in einer WebView, Robolectric kann es nicht rendern.
- Nötig: mind. 2, besser 4 bis 6 Bilder im Querformat vom Gerät (Hauptmenü, Sprung über Schollen, Superkraft, Ergebnis mit Metern).
- Die Spieltexte sind englisch, dieselben Bilder passen für de-DE und en-US.
- `splash_pre31.png` (800×480, nur Startbild) eignet sich nicht als Screenshot.
- Notlösung bis dahin: `src/main/assets/assets/sprites/clumsy-cheatsheet.jpg` zeigt Spielgrafik, ist aber kein echter Screenshot.

## Icon und Feature-Grafik
- Icon 512: ja, `screenshots/icon_store_512.png`
- Feature-Grafik 1024×500: fehlt
