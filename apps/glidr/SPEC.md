# glidr – Cool Cows on Ice

Zweites Spiel der Familie: Thomas' Phaser-2-Physikrätsel „Cool Cows on Ice" (2015) als native Hülle ohne Capacitor, gleiche Bauart wie moovr.
Store-Untertitel: „Cool Cows on Ice". Paket `solutions.xapient.glidr`, minSdk 26.

## Spiel
- Quelle: `cowsonice/www` (Phaser 2.3.0 + P2-Physik, Canvas-Renderer, Welt 768×480, `EXACT_FIT`, 48 Level inkl. Intro).
- Grafik, Sound, Musik, Level und `game.js` bleiben unverändert (GPLv3, Thomas). Phaser/Pixi/p2 und Gray.js: MIT, Hinweise in `assets/THIRD-PARTY-NOTICES.txt`.
- Werbung/Kauf: schon in der Quelle entfernt (AdMob, In-App-Kauf, XDK/Cordova-Plugins). Assets geprüft auf admob, ca-app-pub, billing, gtag, firebase, analytics, purchase: keine Treffer (nur Phasers Geräteerkennung `window.cordova`).
- Sterne pro Level in `localStorage` (Schlüssel = Levelnummer), bleiben über Neustarts und Updates.

## Assets (`src/main/assets/`)
- Übernommen: `index.html` (+ `glidr.js`, schwarzer Hintergrund, ohne Mailadresse im Kommentar), `game.js`, `phaser.min.js`, `Gray.js`, `assets/` komplett (Level-JSON/PNG, `levels.js`, Sprites, Font, Polygone, Ladebalken, 25 OGG).
- Weggelassen: Capacitor-Projekt, `resources/`, alte XDK-Icons und Cover (`cowICON*.png`, `*.jpg`), Git-History.

## Hülle (`MainActivity`)
- WebView vollflächig, immersiv (Leisten per Wischen kurz sichtbar), Querformat (`sensorLandscape`), Bildschirm bleibt an.
- Notch: Spiel bleibt außerhalb der Display-Aussparung (schwarzer Rand).
- `android:appCategory="game"`: Android 16 lässt Querformat-Sperre auf Tablets bestehen.
- Assets über `WebViewAssetLoader` unter `https://appassets.androidplatform.net/assets/`.
- Härtung: kein Datei-/Content-Zugriff, keine Zoom-/Fenster-Funktionen, Safe Browsing und Metrics aus (Manifest), jede Anfrage außerhalb der eigenen Assets wird mit 403 beantwortet, fremde Navigationen blockiert; http(s)-Links nur nach Tipp im Browser. Remote-Debugging nur im Debug-Build.
- Renderer abgestürzt: Activity startet neu statt App-Absturz.

## Zurück-Taste (`glidr.js` → `glidrBack()`)
- Im Level: Spielmenü (Neustart/Levelauswahl) rechts ein- bzw. ausfahren; nicht doppelt, solange es fährt. Nach Verlust (Blende zum Ergebnis läuft): nichts.
- Während Blende: nichts.
- Ergebnis- und Schlussbildschirm: zurück zur Levelauswahl.
- Levelauswahl: weiter unten geblättert → eine Seite hoch, sonst App beenden.
- Antwortet das Spiel nicht (`null`), wird beendet, damit niemand festhängt.

## Pause
- `onPause`: `glidrPause()` (Phaser-Zeit stoppt, AudioContext `suspend`) + `WebView.onPause()`.
- `onResume`: `WebView.onResume()` + `glidrResume()`.
- Musik startet ohne Tipp (`mediaPlaybackRequiresUserGesture = false`), Fallback: erster Tipp gibt den AudioContext frei.

## Look
- Akzent Gletscherweiß `#CDE6FF` (Eis, helle Kuh), Icon: Kuhkopf mit großen Augen schlittert schräg über eine Eisscholle, Fahrtstriche links, adaptiv + Monochrom.
- Splash: ab Android 12 SplashScreen-API mit Vektor-Icon, darunter `windowBackground`-Layer-List mit demselben Vektor. Splash bleibt, bis die Seite geladen ist (max. 2 s).

## Berechtigungen
Keine. Kein INTERNET (Assets kommen aus dem APK, Anfragen werden lokal abgefangen).

## Tests
- JVM: `UrlPolicy` (eigene Assets, Fremd-Hosts, Pfad-Tricks), `BackBridge` (Antwort-Auswertung).
- Robolectric: WebView-Einstellungen, Start-URL, Asset-Routing über den Client (index.html, Level-JSON, OGG, 403 für Fremdes), Zurück-Taste, Pause/Resume, Bildschirm-an.
- Roborazzi: `icon_masks`, `icon_store_512`, `splash_pre31`.
- Nur am Gerät prüfbar (Robolectric hat keine echte WebView): Spiel lädt (Ladebalken) und läuft flüssig, WebAudio/OGG spielt ohne INTERNET, Musik stoppt im Hintergrund und kommt zurück, Sterne bleiben nach Neustart, Zurück-Taste in allen Zuständen, Notch, Splash-Übergang, Tablet-Querformat.
