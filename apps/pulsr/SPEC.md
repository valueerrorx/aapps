# pulsr – SPEC

## Aufgabe
Metronom zum Üben: Tempo einstellen, Start drücken, sauberer Klick ohne Schwanken. Hübsch, ruhig, ohne Werbung.

## Muss
- Tempo 20–300 BPM. Ändern per Drehen am Rad (Ziehen im Kreis um das Hero, 6° = 1 BPM), per −/+ (Halten wiederholt) und per Tap-Tempo.
- Tap-Tempo: Mittelwert der letzten bis zu 6 Abstände, Ausreißer (> 35 % vom Median) fallen weg, nach 3,2 s Pause (länger als ein Schlag bei 20 BPM) beginnt die Messung neu. Ab dem 2. Tipp wird das Tempo gesetzt.
- Taktart 1–12 Schläge (x/4), erster Schlag betont (abschaltbar). Unterteilung: Viertel, Achtel, Triolen, Sechzehntel.
- Klang: Klick, Holz, Piep; jeweils eigene Töne für Betonung, Schlag und Unterteilung, vorab als PCM erzeugt.
- Timing sample-genau: ein durchgehender `AudioTrack` (MODE_STREAM, PCM 16 bit, native Abtastrate) auf eigenem Thread mit Audio-Priorität. Die Klicks werden auf das Sample genau in den Puffer gemischt, kein Handler/Timer/delay. Kein Drift: die Beat-Position läuft kontinuierlich mit, Fehler < 1 Sample je Schlag, ohne Aufsummieren.
- Tempo-, Takt- und Unterteilungswechsel im Lauf ohne Stolpern: der laufende Schlag wird mit dem neuen Tempo zu Ende gezählt.
- Visueller Puls synchron zum Ton: Die Animation leitet ihre Phase aus der Audio-Position ab (`AudioTrack.getTimestamp`, sonst `playbackHeadPosition`), nicht aus der Wanduhr. Aktueller Schlag leuchtet auf, ein Komet läuft im Takt um den Ring, der betonte Schlag schickt eine Ring-Welle aus, die BPM-Zahl pulsiert leicht.
- Hintergrund: Foreground-Service `mediaPlayback` mit Benachrichtigung (Tempo, Takt, Stopp). Kopfhörer ab = Stopp.
- Bildschirm bleibt beim Spielen an (abschaltbar). Klang über die Medienlautstärke, mischt sich in Musik (kein Audio-Fokus, damit man zu Aufnahmen üben kann).
- Einstellungen und Tempo überstehen Drehung und Prozess-Tod. Nach Prozess-Tod spielt nichts von selbst.
- Hoch- und Querformat, Englisch und Deutsch, lesbar bei 200 % Schrift.

## Berechtigungen
| Berechtigung | Grund |
|---|---|
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Das Metronom spielt bei gesperrtem Bildschirm weiter. Es ist Audiowiedergabe, also der passende Standardtyp. |
| `POST_NOTIFICATIONS` | Stopp in der Benachrichtigung (Android 13+, einmal vor dem ersten Start gefragt; abgelehnt spielt es trotzdem). |
| `WAKE_LOCK` | CPU bleibt wach, damit der Audio-Thread den Puffer rechtzeitig füllt. Nur während des Spielens. |

Kein INTERNET, kein VIBRATE (Haptik über die View).

## Nicht
- Keine Presets/Setlists, kein Trainer (Tempo-Rampen), keine Pendel-Physik, kein Blitz-Modus, keine Betonung pro Schlag, keine ungeraden Nenner (x/8).

## Screens
1. **Metronom**: oben Taktart-Stepper und Zahnrad; Hero = Rad mit Schlag-Segmenten, Skala, Komet und großer BPM-Zahl mit Tempobezeichnung (Largo … Prestissimo); darunter Schlagzähler 1…n (laufender Schlag als Akzent-Kreis, Punkt unter der betonten Eins), −, TAP, +, Unterteilung; Hauptaktion Start/Stopp. Querformat: Bedienung links, Rad rechts (ohne Zähler).
2. **Einstellungen**: Klang, Betonung, Bildschirm an, Info.
3. **Info**: wie alle aapps.

## Screenshot-Zustände (Roborazzi, `apps/pulsr/screenshots/`)
| Name | Inhalt |
|---|---|
| `idle` | 120 BPM, 4/4, gestoppt |
| `play_accent` | 4/4 Achtel, 96 BPM, kurz nach Schlag 1 (Welle) |
| `play_beat3` | 3/4 Triolen, 72 BPM, Schlag 3 |
| `play_fast` | 7/4 Sechzehntel, 220 BPM |
| `settings`, `about` | |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard Pixel 7, en): `_de`, `_land`, `_font200`, `_land_font200` für `idle`, `play_accent`, `settings`; dazu `idle_360dp`, `about_de`.

## Abnahmekriterien
- Domain ohne Android-Imports, Unit-Tests für Sample-Positionen (Abstand, Unterteilung, Betonung, kein Drift über 10 min, Tempowechsel mitten im Schlag), Mischen (Onset auf dem Sample), Audio-Uhr (Schlag/Phase aus Frame-Position), Tap-Tempo (Mittelwert, Ausreißer, Neustart nach Pause), Rad-Winkel, Tempobezeichnung, Speicher, Kontrast (WCAG AA gemessen, auch inaktive Segmente ≥ 3:1).
- Gesten (Robolectric): Rad drehen, −/+ und Halten, Tap, Start/Stopp. Im Leerlauf keine Animations-Frames.
- `:apps:pulsr:check`, `verifyRoborazziDebug`, `assembleRelease` grün; nur die 4 Berechtigungen oben; Release-APK < 4 MB.
