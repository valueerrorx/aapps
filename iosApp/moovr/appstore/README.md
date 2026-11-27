# moovr – App-Store-Material

Aufbau, Grenzen der Textfelder und App Privacy wie bei levlr: siehe `../../levlr/appstore/README.md`.

## Gratis
Kein In-App-Kauf, keine Testphase, kein Store-Gate, kein `.storekit`. Preis 0, Kategorie Games.

## Aufbau der App
Reines Swift (kein Kotlin): `moovr/WebGame.swift` zeigt das Spiel aus `apps/moovr/src/main/assets` (dieselben Dateien wie Android) in einer WKWebView über das eigene Schema `aapps://game/`. Fremde Adressen werden nicht geladen. WebKit auf iOS spielt kein Ogg, darum liegen dieselben Klänge als AAC in `../ios-assets/assets/sounds/*.m4a` (erzeugt mit ffmpeg, 64 kbit/s) und ein kleines Skript lädt `.m4a` statt `.ogg`; `game.js` bleibt unverändert. Ändern sich Klänge, die `.m4a` neu erzeugen.

## Berechtigungen
Keine. `UIRequiredDeviceCapabilities`: arm64. Nur Querformat, iPad im Vollbild (`UIRequiresFullScreen`).

## Screenshots
Szenen `scenes.txt` (`MENU`, `PLAY`), Startargument `-aappsScene <NAME>` (nur Debug). Das Spiel läuft quer, der Simulator liefert das Bild hochkant: `rotate.txt` = Grad im Uhrzeigersinn, um die `sim-shots.sh` dreht (bei Querlage „Home rechts“ = 270).
