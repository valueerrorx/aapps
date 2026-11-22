# nordr – SPEC

## Aufgabe
Ein Kompass: zeigt ruhig und genau, wohin das Handy zeigt, und hält auf Wunsch einen Kurs.

## Muss
- Richtung aus dem Rotation-Vector-Sensor (Fusion aus Beschleunigung, Magnetfeld, Gyro). Fallback ohne Rotation-Vector: Beschleunigungssensor + Magnetometer, eigene Rechnung im Domain.
- Funktioniert flach liegend und aufrecht gehalten (Blickrichtung = Rückseite), ohne Sprung dazwischen. Hoch- und Querformat (Display-Rotation wird eingerechnet).
- Glättung im Kreis: kürzester Weg über 0°/360°, schneller bei großer Drehung, Totband gegen Zittern im Stillstand.
- Kompassrose mit Trägheit und Nachschwingen (Feder), Glas-Spiegelung bewegt sich mit der Neigung.
- Große Gradzahl (ganze Grad) und Himmelsrichtung (16 Striche, DE mit O für Ost).
- Kalibrierungshinweis (Acht-Bewegung), wenn das Magnetometer niedrige Genauigkeit meldet. Eigener Hinweis bei magnetischer Störung (Feldstärke außerhalb 20–70 µT). Ausblendbar, kommt bei der nächsten Verschlechterung wieder; manuell über die Einstellungen.
- Kurs setzen (Hauptaktion): Marke auf der Rose, Abweichung „12° rechts", Haptik und Glanz beim Zurückkommen auf Kurs (Hysterese 2°/4°). Kurs bleibt gespeichert.
- Haptik-Tick beim Überstreichen von N/O/S/W (mit Hysterese), abschaltbar.
- Missweisend (magnetisch) oder rechtweisend (geografisch). Deklination über `GeomagneticField` für eine manuell gewählte Region, ohne Standort-Berechtigung. Startwert: Region passend zur Zeitzone, sonst magnetisch.
- Bildschirm bleibt an. Sensoren nur zwischen onResume und onPause. Kein Magnetometer: Hinweis statt Absturz.
- Englisch und Deutsch. Keine Berechtigungen.

## Nicht
- Kein Standort, keine Karte, keine Wegpunkte, kein Kamera-/AR-Modus, keine Neigungsmessung (das ist levlr), kein INTERNET.
- Hinweis im Info-Screen: „nicht für Navigation in kritischen Situationen".

## Screens
1. **Kompass** (Hero ≥ 60 %): oben Nordbezug + Genauigkeitspunkt und Zahnrad, Zahl und Richtung, Rose, unten „Kurs setzen"/„Kurs löschen".
2. **Kalibrieren** (Karte über dem Kompass): animierte Acht, Text, „Ausblenden".
3. **Einstellungen**: Nordbezug, Region, Haptik, Kalibrierhinweis zeigen, Info. Debug-Build: Sensor-Aufzeichnung (10 s CSV).
4. **Region**: Liste mit Deklination je Region.
5. **Info**: wie alle aapps, plus Navigations-Hinweis.

## Screenshot-Zustände (Roborazzi, `apps/nordr/screenshots/`)
| Name | Inhalt |
|---|---|
| `heading` | 247° WSW, Rose schwingt nach |
| `true_north` | rechtweisend, 4° N |
| `course` | Kurs 250°, Richtung 232°, Abweichungsbogen |
| `on_course` | auf Kurs, Glanz |
| `calibrate` | Kalibrierhinweis (Acht) |
| `interference` | Störungshinweis |
| `settings`, `region`, `about` | Einstellungen, Regionsliste, Info |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard: Pixel 7, en): `heading_de`, `_360dp`, `_land`, `_font130`, `_font200`; `course_land`; `calibrate_de`; `settings_de`, `settings_font200`; `about_de`.

## Abnahmekriterien
- Domain ohne Android-Imports, Unit-Tests für Winkel-Wraparound, Orientierung (flach, aufrecht, quer, Rotation-Vector = Fallback), Glättung, Genauigkeit/Störung, Kurs, Kardinal-Ticks, Deklination, Federphysik.
- Liegt das Handy still, laufen keine Animations-Frames mehr.
- `:apps:nordr:check`, `verifyRoborazziDebug`, `assembleRelease` grün, Manifest ohne Berechtigungen, Release-APK < 4 MB.
- Lesbar bei Schriftgröße 200 %.
