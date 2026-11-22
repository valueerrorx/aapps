# roundr – SPEC

## Aufgabe
Intervall-Timer für HIIT, Tabata und Boxrunden: Arbeit und Pause wechseln sich ab, groß lesbar aus 3 m, mit Ton und Vibration, auch bei gesperrtem Bildschirm.

## Muss
- Training = Vorbereitung (0–60 s), Arbeit (5 s–60 min), Pause (0–60 min), Runden (1–99), Sätze (1–10), Satzpause (0–10 min). Pause 0 = direkt weiter (EMOM). Nach der letzten Runde keine Pause; zwischen Sätzen ersetzt die Satzpause die Pause (0 = normale Pause).
- Vorlagen: Tabata 20/10 × 8, Tabata 3 Sätze, HIIT 40/20 × 10, EMOM 1:00 × 10, Boxen 3:00/1:00 × 12. Eigene Trainings unter Namen speichern (max. 20, Name max. 24 Zeichen, gleicher Name überschreibt), löschen mit zweitem Tipp.
- Werte per −/+ ändern, Gedrückthalten wiederholt. Zeit-Schritte 5 s (< 1 min), 15 s (< 5 min), sonst 30 s, immer auf das Raster.
- Lauf: ganze Fläche in der Phasenfarbe (Arbeit Glut-Orange, Pause Eis-Blau, Vorbereitung Bernstein), die sich wie ein Füllstand von oben nach unten leert; Phasenwechsel füllt per Feder neu und morpht die Farbe. Text und Symbole wechseln an der Füllkante die Farbe (dunkel auf Farbe, Farbe auf Dunkel).
- Restzeit riesig (Sekunden unter 1 min, sonst m:ss, aufgerundet), Phase, Runde/Satz, nächste Phase. Letzte 3 s pulsieren.
- Steuerung: Anhalten/Weiter, zur nächsten Phase, zurück (zum Phasenanfang, innerhalb 2 s zur vorigen Phase), Beenden mit zweitem Tipp.
- Ansagen: Piep bei 3-2-1 vor jedem Phasenende, eigener Ton für Arbeit, Pause, Fertig; Vibration dazu. Beides abschaltbar. Töne über die Medienlautstärke.
- Hintergrund: Foreground-Service mit Benachrichtigung (Phase, Runde, herunterzählende Uhr, Anhalten/Weiter, Beenden), Partial-Wakelock nur während der Lauf läuft. Bildschirm bleibt im Lauf an (abschaltbar).
- Zeit nur aus der monotonen Uhr (`elapsedRealtime`): kein Driften, Anhalten/Weiter verschiebt nur den Versatz. Lauf, gewähltes Training, Vorlagen und Einstellungen überstehen Drehung und Prozess-Tod; nach Neustart des Geräts ist ein Lauf angehalten.
- Fertig-Ansicht: Ring des ganzen Trainings, Gesamtzeit, Runden; „Nochmal" oder schließen.
- Hoch- und Querformat, Englisch und Deutsch, lesbar bei 200 % Schrift.

## Berechtigungen
| Berechtigung | Grund |
|---|---|
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Lauf geht bei gesperrtem Bildschirm weiter. Kein passender Standardtyp: `health` braucht Sensor-Rechte, `mediaPlayback` passt nicht zu kurzen Signaltönen. Untertyp im Manifest begründet. |
| `POST_NOTIFICATIONS` | Steuerung in der Benachrichtigung (Android 13+, einmal beim ersten Start gefragt; abgelehnt läuft der Timer trotzdem). |
| `WAKE_LOCK` | Ohne ihn schläft die CPU bei gesperrtem Bildschirm ein und die Ansagen kommen zu spät. Nur während des Laufs. |
| `VIBRATE` | Vibration bei Phasenwechsel, wenn das Handy gesperrt in der Tasche ist (View-Haptik geht dann nicht). |

Kein INTERNET, kein Exact-Alarm (der Dienst läuft ja).

## Nicht
- Keine Sprachansage, keine Musiksteuerung, kein Verlauf/Statistik, keine Übungsnamen pro Runde, keine Gesundheits- oder Fitnessversprechen.

## Screens
1. **Start**: Name des Trainings (öffnet Vorlagen), Zahnrad; Hero-Ring mit allen Phasen und Gesamtzeit; Arbeit/Pause/Runden (öffnet Bearbeiten); Hauptaktion „Start".
2. **Vorlagen**: eingebaute und eigene Trainings mit Mini-Ring und Kurzform.
3. **Bearbeiten**: −/+ je Wert, Satzpause nur bei mehr als einem Satz, Name + „Speichern".
4. **Lauf**: siehe oben.
5. **Fertig**.
6. **Einstellungen**: Töne, Vibration, Bildschirm an, Info.
7. **Info**: wie alle aapps.

## Screenshot-Zustände (Roborazzi, `apps/roundr/screenshots/`)
| Name | Inhalt |
|---|---|
| `home` | Tabata gewählt |
| `run_work` | Arbeit, Runde 3/8, 60 % übrig |
| `run_rest`, `run_prep`, `run_setrest` | Pause fast leer, Vorbereitung, Satzpause |
| `run_last3` | Arbeit, 2 s übrig |
| `run_paused` | angehalten, gedimmt |
| `run_long` | Boxen, 2:41 übrig |
| `done` | Fertig |
| `library`, `editor`, `settings`, `about` | |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard Pixel 7, en): `_de`, `_land`, `_font200`, `_land_font200` für Start, Lauf (Arbeit), Fertig, Vorlagen, Bearbeiten, Einstellungen; dazu `run_rest_land`, `run_paused_de`, `home_360dp`.

## Abnahmekriterien
- Domain ohne Android-Imports, Unit-Tests für Phasenfolge (Pause 0, Sätze, Satzpause), Position/Restzeit, Anhalten/Weiter/Springen, Neustart-Erkennung, Ansagen (3-2-1, kurze Phasen, verspätete Ticks), Schritte/Grenzen, Speicherformat (inkl. kaputter Daten), Zeitformat, Kontrast (WCAG AA gemessen).
- Gesten: −/+ und Gedrückthalten, Anhalten/Weiter im Lauf (Robolectric).
- Im Leerlauf (Start, angehalten, fertig) keine Animations-Frames.
- `:apps:roundr:check`, `verifyRoborazziDebug`, `assembleRelease` grün; nur die 5 Berechtigungen oben; Release-APK < 4 MB.
