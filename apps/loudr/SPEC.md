# loudr – SPEC

## Aufgabe
Lautstärke-Messer: App öffnen, sofort sehen, wie laut es ist, mit Alltagsvergleich. Hübsch, ruhig, ohne Werbung. Ein Schätzwert, kein geeichtes Messgerät.

## Muss
- Mikrofon über `AudioRecord` (PCM 16 bit mono, 48 kHz, sonst 44,1 kHz) auf eigenem Thread. Quelle `UNPROCESSED`, wenn das Gerät sie meldet (`PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED`), sonst `VOICE_RECOGNITION`. Beide Quellen sind laut CDD ohne AGC/Rauschunterdrückung.
- A-Bewertung als IIR (drei Biquads, bilineare Transformation der Pole aus IEC 61672-1), bei 1 kHz auf 0 dB normiert. Abweichung zur Sollkurve: ≤ 0,1 dB von 10 Hz bis 4 kHz, darüber innerhalb Klasse-1-Toleranz (44,1 und 48 kHz).
- Pegel: dB = 10·log10(mittleres Quadrat) + 112,35. Bezug laut Android-CDD: 90 dB SPL bei 1 kHz ergeben RMS 2500 (16 bit). Dazu der Kalibrier-Offset.
- Zeitbewertung Fast (125 ms) und Slow (1 s), exponentiell pro Sample; Wahl in den Einstellungen.
- Anzeige: aktueller Pegel, Min und Max (der gewählten Zeitbewertung), Mittelwert als Leq (energetisch über die Messzeit). Beim Start 0,3 s Einschwingen, die letzten 0,1 davon setzen den Startwert der Zeitbewertung.
- Verlauf der letzten 60 s: Leq je 100 ms.
- Ziffern wechseln höchstens alle 250 ms (lesbar); der Bogen folgt flüssig per kritisch gedämpfter Feder.
- Alltagsvergleich je 10 dB (Flüstern, ruhiges Zimmer, Büro, Gespräch, Staubsauger, Straßenverkehr, Rasenmäher, Motorrad, Rockkonzert, Sirene) und vier Farbzonen (ruhig < 55, mittel < 70, laut < 85, sehr laut).
- Kalibrier-Offset −20 … +20 dB in 0,5-dB-Schritten, gespeichert. In den Einstellungen mit Live-Wert zum Abgleich.
- Pause/Weiter (Hauptaktion) und Zurücksetzen (Min/Max/Leq/Verlauf).
- Mikrofon nur im Vordergrund: ab ON_STOP aus, ab ON_START wieder an (außer pausiert). Kein Dienst, kein Hintergrund-Messen.
- `RECORD_AUDIO` zur Laufzeit; beim ersten Start einmal automatisch gefragt. Abgelehnt: Erklärung und „Mikrofon erlauben“. „Nie wieder fragen“: Weg in die App-Einstellungen. Mikrofon belegt/Fehler: „Erneut versuchen“.
- Hinweis auf dem Hauptschirm, in den Einstellungen und auf der Info-Seite: Handy-Mikrofone sind nicht geeicht. Keine Gesundheitsversprechen.
- Zustand übersteht Drehung und Prozess-Tod (Min/Max/Leq/Verlauf/Pause per SavedState, Einstellungen per SharedPreferences).
- Hoch- und Querformat, Englisch und Deutsch, lesbar bei 200 % Schrift.

## Berechtigungen
| Berechtigung | Grund |
|---|---|
| `RECORD_AUDIO` | Pegel messen. Nur im Vordergrund, nichts wird gespeichert oder gesendet. |

Kein INTERNET, kein Dienst.

## Nicht
- Kein Spektrum/Wasserfall, kein Export (PNG/CSV), keine C-/Z-Bewertung, kein Peak-Pegel, keine Aufnahme.

## Screens
1. **Messer**: oben Status (Messung/Pausiert) und Zahnrad; Hero = 270°-Bogen 20–120 dB mit Farbzonen-Rand, Skala, leuchtendem Pegelbogen, Max-Marke; innen große Zahl, „dB(A) · Fast“, Alltagsvergleich. Darunter Min · Leq · Max, Verlauf 60 s, Hinweis, unten Zurücksetzen und Pause/Weiter. Querformat: Werte links, Bogen rechts.
2. **Mikrofon fehlt**: leerer Bogen mit Mikrofon, Erklärung, Hauptaktion je Zustand.
3. **Einstellungen**: Zeitbewertung, Kalibrierung, Bildschirm an, Hinweis, Info.
4. **Info**: wie alle aapps.

## Screenshot-Zustände (Roborazzi, `apps/loudr/screenshots/`)
| Name | Inhalt |
|---|---|
| `talk` | 63 dB Gespräch, Fast, misst |
| `quiet` | 33 dB, misst |
| `loud` | 92 dB, Max 101, Slow, Offset +2 |
| `paused` | 71 dB, pausiert |
| `mic_ask`, `mic_blocked`, `mic_failed` | Mikrofon fehlt |
| `settings`, `about` | |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard Pixel 7, en): `_de`, `_land`, `_font200`, `_land_font200` für `talk`, `mic_ask`, `settings`; dazu `talk_360dp`, `loud_land`, `about_de`, `about_font200`.

## Abnahmekriterien
- Domain ohne Android-Imports, Unit-Tests für A-Filter (Frequenzgang gegen Sollkurve, Sinus im Zeitbereich), dB-Rechnung (Bezugspunkt), Zeitbewertung (Zeitkonstanten), Leq, Min/Max/Reset, Einschwingen, Verlauf, Sichern/Wiederherstellen, Kalibrierung, Vergleich/Zonen, Feder, Halten, Speicher, Kontrast (WCAG AA gemessen, auch pausiert und Zonen-Rand ≥ 3:1).
- Gesten (Robolectric): Pause/Weiter, Zurücksetzen, Mikrofon-Aktion, Kalibrier-Schritte. Pausiert keine Frames.
- `:apps:loudr:check`, `verifyRoborazziDebug`, `assembleRelease` grün; nur `RECORD_AUDIO`; Release-APK < 4 MB.
