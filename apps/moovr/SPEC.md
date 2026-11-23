# moovr – Clumsy Cow

Erstes Spiel der Familie: Thomas' Phaser-2-Spiel „Clumsy Cow" (2015) als native Hülle ohne Capacitor.
Store-Untertitel: „Clumsy Cow". Paket `solutions.xapient.moovr`, minSdk 26.

## Spiel
- Quelle: `clumsycow/www` (Phaser 2.3.0 + P2-Physik, Canvas-Renderer, Welt 768×480, `EXACT_FIT`).
- Grafik, Sound, Musik und `game.js` bleiben unverändert (GPLv3, Thomas). Phaser/Pixi/p2 und Gray.js: MIT, Hinweise in `assets/THIRD-PARTY-NOTICES.txt`.
- Rekord in `localStorage` (Schlüssel `distance`), bleibt über Neustarts und Updates.

## Assets (`src/main/assets/`)
- Übernommen: `index.html` (+ `moovr.js`, schwarzer Hintergrund, ohne toten Cordova-`backbutton`-Listener), `game.js`, `phaser.min.js`, `Gray.js`, `assets/fonts/font1.{png,xml}`, `assets/polygons/border.json`, `assets/sprites/allsprites.{png,jsona}`, `assets/sprites/clumsy-cheatsheet.jpg`, alle 18 OGG.
- Weggelassen: Capacitor-Projekt, `font1.json`, `fonts/convert.js` (Werkzeug), alte XDK-Icons, Cover, Splash-PNGs, Git-History.

## Hülle (`MainActivity`)
- WebView vollflächig, immersiv (Leisten per Wischen kurz sichtbar), Querformat (`sensorLandscape`), Bildschirm bleibt an.
- Notch: Spiel bleibt außerhalb der Display-Aussparung (schwarzer Rand).
- `android:appCategory="game"`: Android 16 lässt Querformat-Sperre auf Tablets bestehen.
- Assets über `WebViewAssetLoader` unter `https://appassets.androidplatform.net/assets/`.
- Härtung: kein Datei-/Content-Zugriff, keine Zoom-/Fenster-Funktionen, Safe Browsing und Metrics aus (Manifest), jede Anfrage außerhalb der eigenen Assets wird mit 403 beantwortet, fremde Navigationen blockiert; http(s)-Links nur nach Tipp im Browser. JS läuft nur in den eigenen Seiten. Remote-Debugging nur im Debug-Build.
- Fehlendes eigenes Asset: 404.
- Renderer abgestürzt: Activity startet neu statt App-Absturz, höchstens einmal pro 30 s; ein zweiter Absturz im Fenster beendet die App (keine Neustart-Schleife).

## Zurück-Taste (`moovr.js` → `moovrBack()`)
- Im Spiel: Menü öffnen (pausiert); ist es offen: schließen und weiterspielen. Während Blende oder Verlust-Animation: nichts.
- Ergebnis-Bildschirm: zurück ins Hauptmenü.
- Hauptmenü: Hilfe offen → schließen, sonst App beenden.
- Antwortet das Spiel nicht (`null`), wird beendet, damit niemand festhängt.
- Wirft `moovrBack()` eine Exception: im Spiel und während der Blende „handled", sonst „exit".

## Pause
- `onPause`: `moovrPause()` (Phaser-Zeit stoppt, AudioContext `suspend`) + `WebView.onPause()`.
- `onResume`: `WebView.onResume()` + `moovrResume()`; eine Pause über das Spielmenü bleibt bestehen.
- Musik startet ohne Tipp (`mediaPlaybackRequiresUserGesture = false`), Fallback: erster Tipp gibt den AudioContext frei.

## Look
- Akzent Wiesengrün `#8FDC5C` (Kuh auf der Wiese), Icon: Kuhkopf mit Hörnern, schielenden Glubschaugen und Maul, adaptiv + Monochrom.
- Splash: ab Android 12 SplashScreen-API mit Vektor-Icon, darunter `windowBackground`-Layer-List mit demselben Vektor. Splash bleibt, bis die Seite geladen ist (max. 2 s).

## Berechtigungen
Keine. Kein INTERNET (Assets kommen aus dem APK, Anfragen werden lokal abgefangen).

## Tests
- JVM: `UrlPolicy` (eigene Assets, Fremd-Hosts, Pfad-Tricks), `BackBridge` (Antwort-Auswertung).
- Robolectric: WebView-Einstellungen, Start-URL, Asset-Routing über den Client (index.html, OGG, 404 für Fehlendes, 403 für Fremdes), Zurück-Taste, Pause/Resume, Bildschirm-an, Renderer-Absturz (Neustart beim 1., Beenden beim 2. im Fenster, Pause/Resume fassen die tote WebView nicht an).
- Roborazzi: `icon_masks`, `icon_store_512`, `splash_pre31`.
- Nur am Gerät prüfbar (Robolectric hat keine echte WebView): Spiel lädt und läuft flüssig, WebAudio/OGG spielt ohne INTERNET, Musik stoppt im Hintergrund und kommt zurück, Rekord bleibt nach Neustart, Zurück-Taste in allen Zuständen, Notch, Splash-Übergang, Tablet-Querformat.
