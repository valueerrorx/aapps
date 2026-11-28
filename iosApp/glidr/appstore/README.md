# glidr – App-Store-Material

Aufbau, Grenzen der Textfelder und App Privacy wie bei levlr: siehe `../../levlr/appstore/README.md`.

## Gratis
Kein In-App-Kauf, keine Testphase, kein Store-Gate, kein `.storekit`. Preis 0, Kategorie Games.

## Aufbau der App
Reines Swift (kein Kotlin): `glidr/WebGame.swift` zeigt das Spiel aus `apps/glidr/src/main/assets` (dieselben Dateien wie Android) in einer WKWebView über das eigene Schema `aapps://game/`. Fremde Adressen werden nicht geladen. WebKit auf iOS spielt kein Ogg, darum liegen dieselben Klänge als AAC in `../ios-assets/assets/sounds/*.m4a` (erzeugt mit ffmpeg, 64 kbit/s) und ein kleines Skript lädt `.m4a` statt `.ogg`; `game.js` bleibt unverändert. Ändern sich Klänge, die `.m4a` neu erzeugen. Fortschritt und Sterne liegen im localStorage der WebView.

## iPad
Spielfläche im Seitenverhältnis 16:10 (768×480) eingepasst, Rest schwarz (Phaser `EXACT_FIT` würde sonst auf 4:3 strecken); am iPhone füllt sie wie auf Android den Bereich ohne Notch.

## Berechtigungen
Keine. `UIRequiredDeviceCapabilities`: arm64. Nur Querformat, iPad im Vollbild (`UIRequiresFullScreen`).

## Screenshots
Szenen `scenes.txt` (`MENU`, `LEVEL11`, `LEVEL16`, `LEVEL23`, `CLEARED`), Startargument `-aappsScene <NAME>` (nur Debug). Das Menü zeigt einen Spielstand mit Sternen. Im Screenshot-Modus bleibt die App hochkant und `WebGame.swift` dreht das Spiel selbst quer auf den ganzen Bildschirm (ohne Balken, gestreckt wie auf Android), so hängt nichts von einer Simulator-Drehung ab. `rotate.txt` = Grad im Uhrzeigersinn, um die `sim-shots.sh` das Foto (iPhone und iPad) gerade dreht (270).
