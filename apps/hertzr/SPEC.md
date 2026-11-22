# hertzr – SPEC

## Aufgabe
Sauberer Tongenerator ohne Esoterik: einen reinen Ton mit genau einstellbarer Frequenz und Wellenform erzeugen. Für Lautsprecher- und Subwoofer-Tests, Raumresonanzen, Hörbereich-Demo und Physikunterricht. Hübsch, ruhig, ohne Werbung.

## Muss
- Frequenz 1 Hz bis 22 kHz, oben begrenzt auf 49 % der Abtastrate (bei 44,1 kHz also 21,6 kHz). Anzeige groß, unter 100 Hz mit einer Nachkommastelle, sonst ganze Hertz; dazu der nächste Notenname mit Cent-Abweichung (A4 = 440 Hz; im Deutschen H statt B).
- Einstellen:
  - Rad: Ziehen im Kreis um das Hero, eine volle Umdrehung = eine Oktave (logarithmisch), Haptik bei jedem Halbton.
  - Feineinstellung −/+ (Halten wiederholt immer schneller): Schritt 0,1 Hz unter 100 Hz, 1 Hz bis 10 kHz, 10 Hz darüber.
  - Oktave ÷2 und ×2.
  - Direkte Eingabe: Tippen auf die Zahl öffnet ein Zahlenfeld („440", „1,5k", „12 kHz").
- Wellenformen Sinus, Rechteck, Dreieck, Sägezahn. Rechteck und Sägezahn mit PolyBLEP, Dreieck mit PolyBLAMP. Gemessen bei 48 kHz (stärkste Spiegelfrequenz relativ zum Grundton, 1,2/3,1/4,7 kHz): Sägezahn bis 16 kHz ≤ −32,6 dB (naiv −16,4), Rechteck ≤ −32,6 dB, Dreieck ≤ −49,5 dB; ab 8 kHz reiner Sinus (< −150 dB). Bewusst kein Oversampling (Aufwand), PolyBLEP ist der übliche Kompromiss. Ab 1/10 der Abtastrate wird die Form gleitend zum Sinus überblendet, bei 1/6 ist es ein reiner Sinus (die 3. Harmonische läge dann über Nyquist). Umschalten im Lauf mit 20 ms Überblendung.
- Phasenkontinuierlich: ein Phasen-Akkumulator (Double), Frequenzwechsel gleitet in 8 ms logarithmisch, kein Sprung im Signal.
- Pegel −60 bis 0 dBFS in 1-dB-Schritten (Schieber). Schutz:
  - Standard −24 dB; nach einem App-Neustart höchstens −12 dB (bewusste Ausnahme vom Wiederherstellen).
  - Start blendet in 40 ms ein, Stopp blendet in 40 ms aus, Pegeländerungen gleiten (10 ms).
  - Über −10 dB: Schieber-Bereich und Wert in Bernstein, Hinweis auf Gehör und Lautsprecher.
  - Kopfhörer angeschlossen und über −30 dB: Hinweis „leise beginnen".
- Sweep: logarithmisch von Start bis Ende (je 1 Hz–22 kHz, auch abwärts), Dauer 5/10/20/30/60 s. Läuft einmal, blendet am Ende aus und stoppt. Rad und Zahl zeigen die laufende Frequenz (nur in der Zeichenphase gelesen).
- Ausgang (Einstellungen): beide Kanäle, nur links, nur rechts, rechts gegenphasig (Polaritätstest).
- Audio: ein `AudioTrack` (MODE_STREAM, PCM Float, Stereo, native Rate) auf eigenem Thread mit Audio-Priorität, 5-ms-Blöcke, Puffer wächst bei Aussetzern (bis 80 ms). Fällt die Ausgabe weg, einmal mit neuem Track weiter, sonst Stopp. Übergaben an den Thread atomar (`AtomicReference`), die laufende Frequenz für die Anzeige als `@Volatile`.
- Hintergrund: Foreground-Service `mediaPlayback` wie pulsr. Begründung: Lautsprecher- und Raumtests macht man mit dem Handy in der Hand oder abgelegt, oft länger als das Display-Timeout, und ein 60-s-Sweep soll nicht abbrechen, wenn der Bildschirm ausgeht. Benachrichtigung mit Frequenz und Stopp. Kopfhörer ab = Stopp, Telefonat beginnt = Stopp. Kein Audio-Fokus (Testton darf über Musik laufen; Stopp per Benachrichtigung).
- Bildschirm bleibt beim Spielen an (abschaltbar).
- Einstellungen überstehen Drehung und Prozess-Tod; nach Prozess-Tod spielt nichts von selbst.
- Hoch- und Querformat, Englisch und Deutsch, lesbar bei 200 % Schrift.

## Berechtigungen
| Berechtigung | Grund |
|---|---|
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Ton bzw. Sweep läuft bei ausgeschaltetem Bildschirm weiter (siehe oben). Audiowiedergabe = passender Standardtyp. |
| `POST_NOTIFICATIONS` | Stopp in der Benachrichtigung (Android 13+, einmal vor dem ersten Start gefragt; abgelehnt spielt es trotzdem). |
| `WAKE_LOCK` | CPU bleibt wach, damit der Audio-Thread den Puffer füllt. Nur während des Spielens. |

Kein INTERNET, kein VIBRATE.

## Nicht
- Keine „Heilfrequenzen", Solfeggio, binaurale Beats, Mückenschutz- oder „Speaker Cleaner"-Presets, keine Gesundheitsversprechen.
- Kein Rauschen (macht sleepr), keine Mehrfach-Töne, kein Speichern von Presets, kein linearer Sweep.

## Screens
1. **Generator**: oben Modus TON | SWEEP und Zahnrad. Hero = Rad: 270°-Bogen mit logarithmischer Skala (1, 10, 100, 1k, 10k, 20k), Füllung bis zur Frequenz, Marke mit Lichthof; innen Notenname, große Zahl mit Einheit, darunter ein Oszilloskop-Streifen der Wellenform (Zahl der Perioden wächst mit der Frequenz, läuft beim Spielen, morpht beim Wechsel). Im Sweep-Modus ist der Bereich Start–Ende auf dem Bogen markiert. Darunter ein Bedienfeld fester Höhe (Ton: ÷2 − + ×2; Sweep: Start, Ende, Dauer), Wellenform-Wahl, Pegel-Schieber mit Hinweiszeile fester Höhe, Hauptaktion Start/Stopp. Querformat: Bedienung links, Rad rechts.
2. **Eingabe** (Overlay): Zahlenfeld mit Einheit, Übernehmen/Abbrechen.
3. **Einstellungen**: Ausgang, Bildschirm an, Info.
4. **Info**: wie alle aapps, mit Hinweis auf Gehörschutz.

## Screenshot-Zustände (Roborazzi, `apps/hertzr/screenshots/`)
| Name | Inhalt |
|---|---|
| `idle` | 440 Hz Sinus, −24 dB, gestoppt |
| `play_square` | 55 Hz Rechteck, −14 dB, spielt |
| `play_loud` | 12 kHz Sägezahn, −6 dB, Kopfhörer, Warnung |
| `sweep` | Sweep 20 Hz → 20 kHz, 10 s, bei 40 %, Dreieck |
| `input` | Eingabe „1,5k" |
| `settings`, `about` | |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard Pixel 7, en): `_de`, `_land`, `_font200`, `_land_font200` für `idle`, `sweep`, `settings`; dazu `idle_360dp`, `about_de`.

## Abnahmekriterien
- Domain ohne Android-Imports. Unit-Tests: Frequenzgenauigkeit (gemessen über Nulldurchgänge, 1 Hz bis 20 kHz), kein Phasendrift über 10 min, Phasenkontinuität bei Frequenz-, Wellen- und Kanalwechsel (größter Sample-Sprung begrenzt), Sweep-Kurve (gemessene Frequenz entlang der Zeit = Exponentialkurve), Aliasing (gemessen per DFT, Grenzwerte in den Tests), Pegel (RMS in dB), Ein-/Ausblenden, Kanäle, Log-Skala, Schritte, Eingabe-Parser, Notenname, Speicher, Kontrast (WCAG gemessen).
- Gesten (Robolectric): Rad drehen, −/+ und Halten, Oktave, Wellenform, Pegel ziehen, Eingabe, Modus, Start/Stopp. Im Leerlauf keine Animations-Frames.
- `:apps:hertzr:check`, `verifyRoborazziDebug`, `assembleRelease` grün; nur die 4 Berechtigungen oben; Release-APK < 4 MB.
