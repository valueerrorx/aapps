# tunr – SPEC

## Aufgabe
Chromatisches Stimmgerät: App öffnen, Ton spielen, Note und Abweichung sofort ablesen. Hübsch, ruhig, ohne Werbung, ohne Internet.

## Muss
- Mikrofon über `AudioRecord` (Mono, PCM 16 bit, 48 kHz, sonst 44,1 kHz). Quelle `UNPROCESSED`, wenn das Gerät es meldet (`PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED`), dann `VOICE_RECOGNITION` (ohne AGC/Rauschfilter), zuletzt `MIC`. Je Quelle beide Raten; ein Versuch gilt erst, wenn `startRecording` läuft.
- Tonhöhe per YIN: Differenzfunktion über FFT (Kreuzkorrelation + laufende Energiesummen), kumulativ normiert, absolute Schwelle 0,15, lokales Minimum, parabolische Interpolation auf der Differenzfunktion. Fenster ≈ 43 ms, Analyse alle 25 ms. Bereich 38 Hz (unter E1) bis 4,5 kHz (über C8).
- Oktavsicherheit: Liegt bei 2τ bzw. 3τ ein deutlich tieferes Minimum (< 20 % des gewählten), gilt die längere Periode (schwacher Grundton, schwache ungerade Obertöne).
- Stille: Pegel-Tor mit Hysterese (auf ab −50 dBFS, zu unter −56 dBFS; bei `UNPROCESSED` ohne Vorverstärkung 10 dB tiefer, −60/−66 dBFS – konservative Annahme, **am Gerät prüfen**), gemessen über dasselbe jüngste Fenster, das YIN auswertet; ohne Ton über 150 ms gilt „kein Ton“, die letzte Note bleibt gedimmt stehen.
- Glättung in Cent: Median der letzten 5 Werte, dann exponentiell (α 0,4). Ausreißer (> 60 Cent Sprung) zählen erst, wenn der nächste Wert sie bestätigt; dann rastet die Anzeige sofort auf den neuen Ton ein (kein Nachziehen). Ebenso gilt der erste Wert nach Stille erst mit einem passenden zweiten (kein falsches Einrasten durch Anschlag oder Geräusch).
- Anzeige: Notenname groß mit Vorzeichen und Oktave (wissenschaftlich, C4 = eingestrichenes c), Abweichung ±50 Cent auf einer Bogenskala mit Federzeiger, Frequenz in Hz (0,1), Cent als Zahl. Zone „gestimmt“ ±5 Cent (raus erst über 7 Cent): Zone leuchtet grün, einmaliger Glanz, Vibration `confirm` (Ereignis über den Vibrator, Usage Alarm). Saite und Instrument wählen: Touch-Feedback `press`.
- Strobe-Ring wie bei Hardware-Stimmern: dreht mit der Abweichung (zu hoch im Uhrzeigersinn), steht bei 0 Cent still. Tempo nahe 0 linear (6°/s je Cent), darüber gestaucht auf höchstens 120°/s, damit je Bild (60 Hz) unter 0,4 Strichabstand: kein scheinbares Rückwärtsdrehen. Farbe stufenlos grün (±5) → bernstein → koralle (≥ 35 Cent).
- Kammerton A4 415–466 Hz in 1-Hz-Schritten (Standard 440), gespeichert.
- Schreibweise: C♯ (Kreuze), D♭ (Bes), deutsch (H, B = B♭). Standard nach Sprache: Deutsch → deutsch, sonst Kreuze.
- Instrumente: Chromatisch (Standard), Gitarre Standard (E2 A2 D3 G3 B3 E4), Gitarre Drop D (D2 …), Bass 4 (E1 A1 D2 G2), Ukulele (G4 C4 E4 A4), Violine (G3 D4 A4 E5). Mit Instrument zeigt eine Saitenreihe die nächste Saite; Cent beziehen sich auf diese Saite (auch > 50, der Zeiger schlägt dann am Rand an). Saitenwechsel erst, wenn eine andere Saite um 30 Cent näher liegt. Tippen auf eine Saite spielt ihren Referenzton und hält sie fest, erneutes Tippen spielt ihn wieder und gibt sie frei.
- Referenzton: gezupfte Saite per Karplus-Strong (Allpass für Bruchteil-Verzögerung, Frequenz < 0,1 Cent genau), selbst erzeugt, keine Audiodateien. Zielfrequenz der Saite mit eingestelltem Kammerton. 2,8 s: 3-ms-Einsatz, Grundton fällt in 3,2 s um 60 dB, letzte 0,8 s sanft auf null, Spitze −6 dBFS. Ausgabe über `AudioTrack` (Medien/Musik, Float, Geräte-Rate), Synthese auf eigenem Thread. Erneutes Tippen blendet den alten Ton in 30 ms aus. Solange er klingt plus 0,3 s Nachlauf (124 Analysen) zählt das Mikrofon nicht, die Anzeige ist aus und läuft danach von selbst weiter. Die Saite leuchtet beim Anschlag in Grün auf und klingt mit dem Ton ab. Verlassen des Stimm-Bildschirms oder ON_STOP blendet aus. TalkBack: Aktion „Ton A2 abspielen“.
- RECORD_AUDIO zur Laufzeit: beim ersten Öffnen einmal fragen. Abgelehnt oder Dialog weggetippt: Hinweis + „Mikrofon erlauben“. Dauerhaft abgelehnt gilt erst mit Beleg: eine Anfrage kommt ohne Dialog sofort (< 0,5 s, keine Pause) abgelehnt zurück; dann „App-Infos öffnen“. Auch dieser Knopf fragt zuerst; kommt wieder sofort ein Nein, öffnen sich die App-Infos. Beim Zurückkehren neu prüfen.
- Mikrofon nur im Vordergrund und nur am Stimm-Bildschirm (ON_START … ON_STOP), kein Dienst. Bildschirm bleibt an, solange gehört wird. Fehler beim Öffnen: Hinweis + „Erneut versuchen“. Liefert das Mikrofon 2 s lang nur Nullen (systemweit gesperrt oder belegt), steht mittig ein Hinweis.
- Einstellungen und Instrument sofort gespeichert; festgehaltene Saite und Bildschirm überstehen Drehung und Prozess-Tod (`rememberSaveable`).
- Leerlauf (kein Ton, Zeiger in Ruhe): keine Frames. Frames nur, solange ein Ton klingt oder eine Feder schwingt.
- Hoch- und Querformat, Englisch und Deutsch, lesbar bei 200 % Schrift.

## Berechtigungen
| Berechtigung | Grund |
|---|---|
| `RECORD_AUDIO` | Das Stimmgerät hört den Ton des Instruments. Nur im Vordergrund, nichts wird gespeichert oder übertragen. |

Dazu VIBRATE aus `core/ui` (normal, ohne Rückfrage) für die Gestimmt-Vibration. Kein INTERNET, kein Dienst.

## Nicht
- Kein Referenzton im chromatischen Modus, keine eigenen Stimmungen, kein Verlauf/Aufnahme, keine Transposition, kein Diagnose-WAV (später).

## Screens
1. **Stimmen**: oben Kammerton (A4 · 440 Hz) und Zahnrad; darüber dem Hero groß der Zustand („GESTIMMT ✓“ / „ZU TIEF“ / „ZU HOCH“, gestimmt nicht nur über Farbe; ohne Ton leer); Hero = Kreis mit Strobe-Ring, Bogenskala ±50 Cent, Zeiger und mittig Note, Cent und Hz als eine Gruppe; darunter Saitenreihe (nur mit Instrument); Hauptaktion = Instrument-Pille. Ohne Mikrofon ersetzt Hinweis + Pille (Erlauben/Einstellungen/Erneut) die Instrument-Pille. Hochformat: Gruppe aus Zustand, Hero und Saiten leicht über der Mitte (freier Platz 30 % oben). Querformat: Zustand und Bedienung links, Hero rechts.
2. **Instrument**: Liste mit Saiten, Auswahl springt zurück.
3. **Einstellungen**: Kammerton (−/+), Schreibweise, Haptik, Info.
4. **Info**: wie alle aapps.

## Screenshot-Zustände (Roborazzi, `apps/tunr/screenshots/`)
| Name | Inhalt |
|---|---|
| `listen` | chromatisch, noch kein Ton („Spiel einen Ton“) |
| `in_tune` | chromatisch, A4 +1 Cent, eingerastet |
| `flat_guitar` | Gitarre, A-Saite (A2) −23 Cent |
| `sharp` | chromatisch, F♯3 +38 Cent |
| `reference_tone` | Gitarre, Referenzton A2 klingt (Saite leuchtet, festgehalten) |
| `mic_ask`, `mic_blocked` | Mikrofon fehlt / dauerhaft abgelehnt |
| `mic_silent` | Mikrofon liefert nur Nullen |
| `instruments`, `settings`, `about` | |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard Pixel 7, en): `_de`, `_land`, `_font200`, `_land_font200` für `in_tune`, `flat_guitar`, `settings`; dazu `listen_360dp`, `in_tune_360dp`, `in_tune_360dp_font200`, `about_de`, `instruments_de`.

## Abnahmekriterien
- Domain ohne Android-Imports. Unit-Tests mit synthetischen Signalen bei 48 und 44,1 kHz: Sinus E1…C8 (< 1 Cent), Sägezahn, schwacher Grundton (Oktave stimmt), Rauschen/Stille (kein Ton), Sinus mit Rauschen, ±Cent-Verstimmung; FFT gegen DFT; Noten/Schreibweise/Kammerton; Saitenwahl; Tracker (Median, Ausreißer, Einrasten, Stille, Hysterese); Federn (schlafen ein, Strobe steht bei 0); Referenzton (YIN ±2 Cent für alle Saiten bei 415/440/466 Hz, Hüllkurve fällt, kein Clipping, Start/Ende ohne Sprung, Ausblenden beim erneuten Tippen stetig); Pause der Erkennung; Mikrofon-Zustand; Speicher; Kontrast (WCAG AA gemessen, inaktive Elemente ≥ 3:1).
- Gesten (Robolectric): Saite festhalten/lösen, Saite tippen spielt den Ton (Kammerton) und pausiert die Messung, Instrument wählen, Kammerton −/+, Mikrofon-Knöpfe. Im Leerlauf keine Frames.
- `:apps:tunr:check`, `verifyRoborazziDebug`, `assembleRelease` grün; nur `RECORD_AUDIO` und VIBRATE; Release-APK < 4 MB.
