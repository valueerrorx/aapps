# levlr – SPEC

## Aufgabe
Eine Wasserwaage: zeigt, wie schief etwas ist, und rastet hörbar-fühlbar ein, wenn es gerade ist.

## Muss
- Zwei Modi, automatisch nach Lage gewählt (mit Hysterese):
  - **Kante**: Handy steht auf einer Kante (hoch oder quer). Röhrenlibelle, 1 Achse.
  - **Flach**: Handy liegt auf dem Rücken. Dosenlibelle, 2 Achsen.
- Große Anzeige des Neigungswinkels, eine Nachkommastelle. Einheit wählbar: Grad, Prozent, mm/m.
- Flüssigkeit und Blase folgen der Neigung mit Federphysik (Nachschwappen, Blase verformt sich bei Bewegung).
- Einrasten bei 0,0°: Farbwechsel auf den Akzent, Glanz-Effekt, Vibration (`confirm`, einmal pro Einrasten, Hysterese gegen Flattern).
- Haptik: Einrasten und Kalibrier-Schritte sind Ereignisse (`tick`/`confirm` über den Vibrator, Usage Alarm), Einheiten-Wahl ist Touch-Feedback (`step`).
- Halten: Wert und Blase einfrieren (Hauptaktion, Tipp auf den Knopf oder auf die Zahl).
- Zwei-Punkt-Kalibrierung pro Modus: messen, Handy um 180° drehen (Flach: auf der Stelle drehen; Kante: auf die gegenüberliegende Kante stellen), messen. Ergebnis gespeichert, zurücksetzbar.
- Bildschirm bleibt an, solange die Waage sichtbar ist.
- Hoch- und Querformat, Englisch und Deutsch.
- Sensor: Gravity-Sensor, sonst Beschleunigungssensor. Listener nur zwischen onResume und onPause. Kein Sensor: Hinweis statt Absturz.
- Nur VIBRATE (normal, ohne Rückfrage, kommt aus `core/ui`).

## Nicht
- Kein Ton, keine Kamera, kein AR-Messen, keine Historie, kein Werbe-/Tracking-Code, kein INTERNET.
- Keine Vermessungs-Genauigkeit: Hinweis „nicht für Vermessungsarbeiten" im Info-Screen.

## Screens
1. **Waage** (Hero, ≥ 60 % Fläche): Zahl oben, Libelle/Flüssigkeit füllt den Rest, unten „Halten". Zahnrad oben rechts.
2. **Kalibrieren** (Overlay auf der Waage): Schritt 1/2, Schritt 2/2, Fertig. Abbrechen jederzeit.
3. **Einstellungen**: Einheit, Kalibrieren, Kalibrierung zurücksetzen, Haptik an/aus, Info. Debug-Build: Sensor-Aufzeichnung (10 s CSV, Teilen-Menü).
4. **Info**: Version, GPLv3, Quellcode-Link, „keine Werbung, kein Tracking, keine Internet-Berechtigung", Schriften-Lizenz, Vermessungs-Hinweis.

## Screenshot-Zustände (Roborazzi, `apps/levlr/screenshots/`)
| Name | Inhalt |
|---|---|
| `edge_tilted` | Kante, 3,4°, Flüssigkeit schwappt nach |
| `edge_mm` | wie `edge_tilted`, Einheit mm/m |
| `edge_level` | Kante, 0,0°, eingerastet, Glanz |
| `flat_tilted` | Flach, Blase außermittig (2,1° / −1,3°) |
| `flat_level` | Flach, eingerastet, Ring-Puls |
| `held` | Kante, 12,5°, gehalten |
| `calibrate_step1` | Kalibrieren, Schritt 1 |
| `calibrate_step2` | Kalibrieren, Schritt 2, Messung läuft |
| `settings`, `about` | Einstellungen, Info |
| `icon_masks`, `icon_store_512` | App-Icon: Kreis, Squircle, Rund-Quadrat, Monochrom; Store-Icon |

Varianten (Standard: Pixel 7, en): `edge_tilted_de`, `_360dp`, `_land`, `_font130`, `_font200`; `flat_tilted_land`; `calibrate_step2_de`; `settings_de`, `settings_font200`; `about_de`.

## Abnahmekriterien
- Domain ohne Android-Imports, Unit-Tests für Winkel, Einheiten, Glättung, Modus-Hysterese, Einrast-Hysterese, Kalibrierung.
- Liegt das Handy still, laufen keine Animations-Frames mehr (Totband auf Messwert, Feder schläft ein).
- Kalibrierung: Bias von ±1° wird nach zwei Messungen auf < 0,05° korrigiert (Unit-Test).
- `./gradlew :apps:levlr:check :apps:levlr:assembleDebug` grün, Screenshots verifiziert, Manifest nur mit VIBRATE.
- Release-APK < 4 MB.
- Lesbar bei Schriftgröße 200 %, nichts abgeschnitten.
