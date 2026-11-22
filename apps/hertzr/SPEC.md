# hertzr – SPEC

## Aufgabe
Sauberer Tongenerator ohne Esoterik: einen reinen Ton mit genau einstellbarer Frequenz und Wellenform erzeugen. Für Lautsprecher- und Subwoofer-Tests, Raumresonanzen, Hörbereich-Demo und Physikunterricht. Hübsch, ruhig, ohne Werbung.

## Muss
- Frequenz 1 Hz bis 22 kHz, oben begrenzt auf 49 % der Abtastrate (bei 44,1 kHz also 21,6 kHz). Anzeige groß, unter 100 Hz mit einer Nachkommastelle, sonst ganze Hertz; dazu der nächste Notenname mit Cent-Abweichung (A4 = 440 Hz; im Deutschen H statt B).
- Einstellen:
  - Frequenzband: horizontal, logarithmisch, feste Mittel-Nadel, das Band läuft. Wischen grob (Bildschirmbreite = 2 Oktaven, Berührungsschwelle zählt mit), Schwung läuft nach dem Loslassen aus (exponentiell, stoppt am Rand mit Abweis-Haptik), Haptik bei jedem Halbton. TalkBack: eigene Aktionen Schritt ±, Halbton ±, Oktave ±.
  - Feineinstellung −/+ (Halten wiederholt immer schneller): Schritt 0,1 Hz unter 100 Hz, 1 Hz bis 10 kHz, 10 Hz darüber.
  - Oktave ÷2 und ×2.
  - Direkte Eingabe: Tippen auf die Anzeige öffnet ein Zahlenfeld mit Umschalter Hz | kHz (die Zifferntastatur hat kein „k"; „1,5k" geht weiter per Hardware-Tastatur). Drei Stellen nach Punkt/Komma ohne „k" sind Tausender („1.500" = 1500 Hz), weil das Raster keine Tausendstel kennt.
- Wellenformen Sinus, Rechteck, Dreieck, Sägezahn. Rechteck und Sägezahn mit PolyBLEP, Dreieck mit PolyBLAMP. Gemessen bei 48 kHz (stärkste Spiegelfrequenz relativ zum Grundton, 1,2/3,1/4,7 kHz): Sägezahn bis 16 kHz ≤ −32,6 dB (naiv −16,4), Rechteck ≤ −32,6 dB, Dreieck ≤ −49,5 dB; ab 8 kHz reiner Sinus (< −150 dB). Bewusst kein Oversampling (Aufwand), PolyBLEP ist der übliche Kompromiss. Ab 1/10 der Abtastrate wird die Form gleitend zum Sinus überblendet, bei 1/6 ist es ein reiner Sinus (die 3. Harmonische läge dann über Nyquist). Umschalten im Lauf mit 20 ms Überblendung; ein zweiter Wechsel mitten in der Blende blendet vom aktuellen Gemisch weiter (Anteile je Form), kein Klick.
- Phasenkontinuierlich: ein Phasen-Akkumulator (Double), Frequenzwechsel gleitet in 8 ms logarithmisch, kein Sprung im Signal.
- Pegel −60 bis 0 dBFS in 1-dB-Schritten (Schieber). Schutz:
  - Standard −24 dB; nach einem App-Neustart höchstens −12 dB (bewusste Ausnahme vom Wiederherstellen).
  - Start blendet in 40 ms ein, Stopp blendet in 40 ms aus, Pegeländerungen gleiten (10 ms).
  - Über −10 dB: Schieber-Bereich und Wert in Bernstein, Hinweis auf Gehör und Lautsprecher.
  - Kopfhörer angeschlossen und über −30 dB: Hinweis „leise beginnen". Kommt ein Kopfhörer (Kabel, USB, Bluetooth, BLE, Hörgerät) während des Spielens dazu, gleitet der Pegel auf −30 dB.
- Sweep: logarithmisch von Start bis Ende (je 1 Hz–22 kHz, auch abwärts), Dauer 5/10/20/30/60 s. Läuft einmal, blendet am Ende aus und stoppt. Start, Ende und Dauer werden beim Start festgelegt und sind während des Laufs gesperrt. Anzeige und Band zeigen die laufende Frequenz (nur in der Zeichenphase gelesen).
- Ausgang (Einstellungen): beide Kanäle, nur links, nur rechts, rechts gegenphasig (Polaritätstest).
- Audio: ein `AudioTrack` (MODE_STREAM, PCM Float, Stereo, native Rate) auf eigenem Thread mit Audio-Priorität, 5-ms-Blöcke, Puffer wächst bei Aussetzern (bis 80 ms). Fällt die Ausgabe weg, einmal mit neuem Track weiter, sonst Stopp. Übergaben an den Thread atomar (`AtomicReference`), die laufende Frequenz für die Anzeige als `@Volatile`.
- Hintergrund: Foreground-Service `mediaPlayback` wie pulsr. Begründung: Lautsprecher- und Raumtests macht man mit dem Handy in der Hand oder abgelegt, oft länger als das Display-Timeout, und ein 60-s-Sweep soll nicht abbrechen, wenn der Bildschirm ausgeht. Benachrichtigung mit Frequenz und Stopp. Kopfhörer ab = Stopp, Telefonat beginnt = Stopp; dieser Schutz hängt am Store (`AudioGuard`), nicht am Dienst, und greift deshalb auch, wenn der Vordergrund-Start scheitert. Wakelock mit 10-min-Frist, alle 5 min erneuert, solange gespielt wird. Kein Audio-Fokus (Testton darf über Musik laufen; Stopp per Benachrichtigung).
- Bildschirm bleibt beim Spielen an (abschaltbar).
- Einstellungen überstehen Drehung und Prozess-Tod (gespeichert 400 ms nach der letzten Änderung, nicht je Zieh-Schritt); nach Prozess-Tod spielt nichts von selbst.
- Hoch- und Querformat, Englisch und Deutsch, lesbar bei 200 % Schrift.

## Berechtigungen
| Berechtigung | Grund |
|---|---|
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Ton bzw. Sweep läuft bei ausgeschaltetem Bildschirm weiter (siehe oben). Audiowiedergabe = passender Standardtyp. |
| `POST_NOTIFICATIONS` | Stopp in der Benachrichtigung (Android 13+, vor dem Start gefragt; weggetippt = beim nächsten Start wieder, nur endgültige Ablehnung beendet das Fragen; abgelehnt spielt es trotzdem). |
| `WAKE_LOCK` | CPU bleibt wach, damit der Audio-Thread den Puffer füllt. Nur während des Spielens. |

Kein INTERNET, kein VIBRATE.

## Nicht
- Keine „Heilfrequenzen", Solfeggio, binaurale Beats, Mückenschutz- oder „Speaker Cleaner"-Presets, keine Gesundheitsversprechen.
- Kein Rauschen (macht sleepr), keine Mehrfach-Töne, kein Speichern von Presets, kein linearer Sweep.

## Screens
1. **Generator** (Akzent Kobaltblau #5B8DFF, kein Ring/Bogen): oben Modus TON | SWEEP und Zahnrad. Hero = Phosphor-Oszilloskop über die volle Breite (Hochformat ~45 % der Höhe): feines 8×6-Raster mit Feinstrichen auf den Mittelachsen, leichte Vignette, Spur mit Schein und hellem Kern, beim Spielen 3 verblassende Nachleucht-Kopien. Zahl der Perioden wächst mit der Frequenz, die Form zeigt mit der echten Abtastrate, was klingt (hohe Töne als Sinus), Wechsel morpht. Links oben die Messgeräte-Anzeige: Notenname, große Zahl, Einheit; Tippen = Eingabe. Im Sweep eine dünne Fortschritts-Zeitachse unten im Schirm. Darunter das Frequenzband (88 dp): Dekaden-Skala 1–9 mit Feinstrichen oben, Klaviatur-Leiste unten (C mit Oktavzahl, aktuelle Taste markiert), Bereich außerhalb 1 Hz…max abgedunkelt; im Sweep ist Start–Ende getönt und das Band läuft live. Darunter Bedienfeld fester Höhe (Ton: ÷2 − + ×2; Sweep: Start, Ende, Dauer), Wellenform-Wahl, Pegel-Schieber (ab 1,5-facher Schrift Beschriftung über der Spur) mit Hinweiszeile fester Höhe, Hauptaktion Start/Stopp. Querformat: links Oszilloskop volle Höhe mit Band darunter, rechts die Bedienspalte.
2. **Eingabe** (Overlay): Zahlenfeld, Umschalter Hz | kHz, Vorschau, Übernehmen/Abbrechen.
3. **Einstellungen**: Ausgang, Bildschirm an, Info.
4. **Info**: wie alle aapps, mit Hinweis auf Gehörschutz.

## Screenshot-Zustände (Roborazzi, `apps/hertzr/screenshots/`)
| Name | Inhalt |
|---|---|
| `idle` | 440 Hz Sinus, −24 dB, gestoppt |
| `play_square` | 55 Hz Rechteck, −14 dB, spielt |
| `play_loud` | 12 kHz Sägezahn, −6 dB, Kopfhörer, Warnung |
| `sweep` | Sweep 20 Hz → 20 kHz, 10 s, bei 40 %, Dreieck |
| `input` | Eingabe „1500" |
| `settings`, `about` | |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard Pixel 7, en): `_de`, `_land`, `_font200`, `_land_font200` für `idle`, `sweep`, `settings`; dazu `idle_360dp`, `about_de`.

## Abnahmekriterien
- Domain ohne Android-Imports. Unit-Tests: Frequenzgenauigkeit (gemessen über Nulldurchgänge, 1 Hz bis 20 kHz), kein Phasendrift über 10 min, Phasenkontinuität bei Frequenz-, Wellen- und Kanalwechsel (größter Sample-Sprung begrenzt), Sweep-Kurve (gemessene Frequenz entlang der Zeit = Exponentialkurve), Aliasing (gemessen per DFT, Grenzwerte in den Tests), Pegel (RMS in dB), Ein-/Ausblenden, Kanäle, Log-Skala, Schritte, Eingabe-Parser, Notenname, Speicher, Kontrast (WCAG gemessen).
- Gesten (Robolectric): Band ziehen und Schwung, Tippen aufs Band ändert nichts, TalkBack-Aktionen, −/+ und Halten, Oktave, Wellenform, Pegel ziehen, Eingabe (auch kHz-Umschalter), Modus, Sweep-Sperre im Lauf, Start/Stopp. Schutz (Robolectric): Kopfhörer ab und Telefonat stoppen ohne Dienst, Speichern entprellt. Im Leerlauf keine Animations-Frames.
- `:apps:hertzr:check`, `verifyRoborazziDebug`, `assembleRelease` grün; nur die 4 Berechtigungen oben; Release-APK < 4 MB.
