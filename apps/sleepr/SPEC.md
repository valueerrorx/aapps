# sleepr – SPEC

## Aufgabe
Rauschgenerator zum Abschirmen von Störgeräuschen, Konzentrieren und Einschlafen: Farben mischen, Timer setzen, laufen lassen. Ruhig, offline, ohne Werbung.

## Muss
- Fünf Rauschfarben, live am Gerät berechnet (keine Audiodateien, keine Schleifen): Weiß (0 dB/Okt), Rosa (−3 dB/Okt), Braun (−6 dB/Okt), Grau (gehörangepasst: Weiß durch Low-Shelf 250 Hz +10 dB, Peak 3,5 kHz −6 dB, High-Shelf 10 kHz +4 dB), Grün (Rosa durch Hochpass 60 Hz, Peak 500 Hz +10 dB, Tiefpass 5 kHz). Jede Farbe hat einen eigenen Regler 0–100, alle Farben werden auf denselben RMS-Pegel normiert.
- Wellen-Modus je Farbe: zufällige Meereswellen (Anschwellen, Brechen, Auslaufen; Tal 40 %, Kamm bis 130 % des Reglerwerts), mittlere Wellenlänge 3–20 s, Länge und Höhe jeder Welle zufällig. Ein-/Ausschalten blendet über 1,5 s weich über. Die Regler folgen den Wellen sichtbar, solange niemand sie anfasst.
- Mischungen (Presets): Meer, Fokus, Einschlafen, Abschirmen, Natur, Klassisch weiß.
- Lautstärke 0–100 (quadratische Kurve), Höhen = Tiefpass 400 Hz – 20 kHz logarithmisch („offen“ ab 19 kHz).
- Spektrumanzeige: theoretisches Leistungsspektrum der Mischung in dB über log-Frequenz (20 Hz – 20 kHz), Summe gefüllt, bei mehreren Farben die Anteile gestrichelt in ihrer Farbe.
- Timer Aus/15/30/45/60/90 min mit sanftem Ausblenden (Dauer t × 12 s, höchstens 2 min). Timer-Wechsel im Lauf startet ihn neu.
- Audio: eigener Thread mit Audio-Priorität, `AudioTrack` MODE_STREAM, PCM float stereo 48 kHz, ≥ 300 ms Puffer, zwei unabhängige Kanäle. Pegeländerungen geglättet (60 ms), Einblenden 1,5 s, Soft-Clip ab 0,7. Stopp ohne Knacksen: 350 ms Ausblenden, Puffer mit Stille auslaufen lassen.
- Hintergrund: Foreground-Service `mediaPlayback` mit MediaSession und Benachrichtigung (Mischung, Timer, Stopp; auch am Sperrbildschirm). Audio-Fokus: Verlust (auch kurzzeitig, z. B. Anruf) = Stopp.
- Mischung, Lautstärke, Höhen und Timer überstehen Neustart (SharedPreferences). Nach Prozess-Tod spielt nichts von selbst.
- Hoch- und Querformat, lesbar bei 200 % Schrift. Edge-to-edge, Inhalt im sicheren Bereich.

## Berechtigungen
| Berechtigung | Grund |
|---|---|
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Das Rauschen läuft bei gesperrtem Bildschirm weiter. Es ist Audiowiedergabe, also der passende Standardtyp. |
| `POST_NOTIFICATIONS` | Stopp in der Benachrichtigung bzw. am Sperrbildschirm (Android 13+, einmal vor dem ersten Start mit Erklärung gefragt; abgelehnt läuft es trotzdem). |
| `WAKE_LOCK` | CPU bleibt wach, damit der Audio-Thread den Puffer rechtzeitig füllt. Nur während der Wiedergabe, höchstens 12 h. |

Kein INTERNET, kein VIBRATE (sleepr nutzt `core/ui` nicht). Haptik wie `press()` aus `core/ui`: Touch-Feedback über die View (`KEYBOARD_TAP`) für Start/Stopp, Mischungen, Timer und Wellen-Schalter; folgt der System-Touch-Haptik.

## Technik
- Views und Canvas (`SpectrumView`), kein Compose und kein `core/ui`. Das gehört so, nicht umbauen.
- Signalverarbeitung in `domain/Dsp.kt` ohne Android-Imports (RBJ-Biquads, Rauschfarben je Kanal, RMS-Normierung).
- Oberfläche derzeit nur Deutsch.

## Nicht
- Keine Audiodateien oder Schleifen, alles wird live berechnet.
- Kein Ausblenden fremder Apps (das macht dozr).

## Screens
1. **Mixer**: Titel und Kurztext; Karte „Spektrum der Mischung“; Mischungen als Chips; Karte Rauschfarben (Farbpunkt, Name, Steigung, Hinweis, Wellen-Schalter, Wert, Regler, bei aktiven Wellen Regler für die Wellenlänge); Karte Lautstärke und Höhen; Timer-Chips; Hinweistext. Hauptaktion START/STOP als Pille unten mittig mit Glow, Status („Bereit“, „Läuft · noch m:ss“, „Blendet aus · …“) und Mischung darüber. Hochformat: Leiste unten fest, Inhalt scrollt darüber und blendet weich aus. Querformat: Titel, Status und Pille links, Inhalt rechts.

## Screenshot-Zustände (Roborazzi, `apps/sleepr/screenshots/`)
| Name | Inhalt |
|---|---|
| `idle` | Standardmischung (Rosa 20, Braun 60), gestoppt |
| `playing` | läuft, Timer mit Restzeit |
| `idle_land`, `idle_font200`, `idle_tablet` | Querformat, 200 % Schrift, Tablet 800 × 1280 dp |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Standard Pixel 7.

## Abnahmekriterien
- Unit-Tests für die Signalverarbeitung: Tiefpass (Durchlass, −3 dB an der Grenze, Sperrbereich), Peak-Filter-Verstärkung, Weiß im Bereich ±1, gleiche RMS aller Farben nach Normierung, deterministisch bei gleichem Seed.
- `:apps:sleepr:check`, `verifyRoborazziDebug`, `assembleRelease` grün; nur die 4 Berechtigungen oben; Release-APK < 4 MB.
