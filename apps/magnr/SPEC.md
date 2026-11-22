# magnr – SPEC

## Aufgabe
Metalldetektor und Magnetfeld-Messer: Schrauben, Nägel, Stahlträger und Magnete in der Nähe finden und die Feldstärke in µT zeigen. Hübsch und ehrlich: findet nur, was magnetisch ist.

## Muss
- Sensor `TYPE_MAGNETIC_FIELD` (kalibriert), 50 Hz (`samplingPeriodUs = 20 000`, ≙ `SENSOR_DELAY_GAME`): flott genug für Ton und Ausschlag, UI-Rate (~16 Hz) wäre zu träge.
  - Warum nicht `UNCALIBRATED`: dort steckt der Hart-Eisen-Versatz des Handys selbst (Lautsprecher, Kamera, oft 20–200 µT) im Wert. Der Betrag ändert sich dann schon beim Drehen des Handys, jeder Schwenk wäre ein Fehlalarm. Kalibriert ist der Betrag lageunabhängig, nur Metall in der Nähe ändert ihn. Nachteil: Die Online-Kalibrierung des Systems springt gelegentlich. Das fangen Nullen und der automatische Grundwert ab.
- Messgröße ist der Betrag |B| (lageunabhängig), geglättet exponentiell mit τ = 0,12 s.
- Grundwert (Erdfeld am Ort):
  - automatisch: Mittel der ersten 0,5 s nach dem Start bzw. Weiter.
  - „Nullen“ (Hauptaktion): Grundwert = aktueller Betrag, mit Haptik.
  - Driftausgleich (abschaltbar, Standard an): Liegt die Abweichung unter 2 µT, folgt der Grundwert langsam (τ = 30 s). Ein gehaltener Fund verschwindet so nicht.
- Abweichung Δ = Betrag − Grundwert (mit Vorzeichen). Ausschlag 0…1 = (|Δ| − Rauschen) / (Vollausschlag − Rauschen).
- Empfindlichkeit: Vollausschlag 100 / 30 / 10 µT (niedrig / mittel / hoch), Rauschschwelle 3 / 0,9 / 0,6 µT.
- Stufen mit Hysterese (0,04): ruhig < 0,12 ≤ schwach < 0,45 ≤ nah < 0,8 ≤ sehr nah. Haptik-Tick beim Steigen der Stufe, „sehr nah“ mit Bestätigungs-Haptik. Abschaltbar.
- Spitze: größte |Δ| bleibt 2 s stehen, dann folgt sie dem aktuellen Wert.
- Verlauf der letzten 20 s: Ausschlag je 100 ms, Pausen als Lücke.
- Ton (Standard aus, Schalter auf dem Hauptschirm und in den Einstellungen): kurze Pieptöne ab „schwach“.
  - Tonhöhe 520 → 2080 Hz (zwei Oktaven).
  - Abstand 600 → 60 ms, ab 0,95 Dauerton.
  - Sinus mit 3 ms Ein- und Ausblendung (kein Knacksen), Medienlautstärke.
- Sensor am Anschlag (eine Achse ≥ 98 % von `maximumRange`): Hinweis „Am Anschlag“ statt Stufe.
- Kalibrierungshinweis (Acht-Bewegung), wenn der Sensor ≥ 1 s Genauigkeit „niedrig/unzuverlässig“ meldet. Ausblendbar bis zur nächsten Verschlechterung, manuell über die Einstellungen.
- Ehrlichkeit:
  - Einführungskarte beim ersten Start: findet Eisen, Stahl, Nickel und Magnete, kein Gold/Silber/Kupfer/Alu. Sensor sitzt meist oben am Handy, mit der Oberkante langsam über die Wand fahren.
  - Fußzeile dauerhaft. Wieder aufrufbar in den Einstellungen.
- Pause/Weiter (Nebenaktion): pausiert sind Sensor und Ton aus, Anzeige gedimmt, keine Frames.
- Sensor und Ton nur im Vordergrund: ab ON_STOP aus, ab ON_START wieder an (außer pausiert). Kein Dienst.
- Kein Magnetometer: klarer Hinweis statt Messung, kein Absturz.
- Zustand übersteht Drehung und Prozess-Tod:
  - per SavedState: Grundwert, Verlauf, Pause.
  - per SharedPreferences: Einstellungen, Einführung gesehen.
- Bildschirm bleibt beim Messen an (abschaltbar). Hoch- und Querformat, Englisch und Deutsch, lesbar bei 200 % Schrift.

## Berechtigungen
Keine. Haptik über die View, Ton über AudioTrack. Kein INTERNET, kein Dienst.

## Nicht
- Keine „Gold-/Schatzsuche“, keine EMF-/Strahlungs- oder Gesundheitsaussagen, keine Richtungs-/Vektoranzeige, kein Export, keine Kalibrierung im eigenen Code (macht das System).

## Screens
1. **Detektor** (Hero ≥ 60 %):
   - oben Status (Misst/Pausiert) und Zahnrad.
   - Hero = Radar: 270°-Bogen mit Skala in µT der Empfindlichkeit, leuchtendem Ausschlag und Spitzen-Marke. Innen Ringe und ein Lichthof, der mit der Nähe anschwillt, Wellen ab „schwach“. Mitte: großer Betrag in µT, Stufe mit Punkt.
   - darunter Grundwert · Abweichung · Spitze, Verlauf 20 s, Fußzeile.
   - unten Pause, „Nullen“, Ton.
   - Querformat: Werte links, Radar rechts.
2. **Karten** über dem Radar: Einführung (Handy mit Sensorpunkt oben), Kalibrieren (Acht).
3. **Kein Sensor**: leeres Radar mit Magnet, Erklärung.
4. **Einstellungen**: Empfindlichkeit, Driftausgleich, Ton, Vibration, Bildschirm an, „Was magnr findet“, Kalibrierhilfe, Info.
5. **Info**: wie alle aapps, plus Ehrlichkeitshinweis.

## Screenshot-Zustände (Roborazzi, `apps/magnr/screenshots/`)
| Name | Inhalt |
|---|---|
| `near` | Betrag 63,1 µT, Δ +15,2, mittel, Stufe nah, Spitze +18,4 |
| `quiet` | 48,2 µT, Δ +0,3, ruhig |
| `strong` | hoch, sehr nah, Ton an |
| `saturated` | Sensor am Anschlag |
| `paused` | pausiert, Verlauf mit Lücke |
| `intro`, `calibrate` | Karten |
| `no_sensor` | kein Magnetometer |
| `settings`, `about` | |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard Pixel 7, en): `near_de`, `near_land`, `near_font200`, `near_land_font200`, `near_360dp`; `intro_de`, `intro_land`, `intro_font200`; `paused_land`, `no_sensor_land`; `settings_de`, `settings_font200`; `about_de`, `about_font200`.

## Abnahmekriterien
- Domain ohne Android-Imports. Unit-Tests für:
  - Glättung (Zeitkonstante), Grundwert (Einschwingen, Nullen, Drift nur im Ruhebereich)
  - Ausschlag/Empfindlichkeit, Stufen mit Hysterese, Spitze halten
  - Verlauf mit Lücke, Anschlag, Genauigkeit mit Wartezeit
  - Sichern/Wiederherstellen, Tonabbildung (Tonhöhe, Abstand, Stille), Piep-Synthese (Frequenz, keine Sprünge)
  - Kontrast (WCAG AA gemessen, auch pausiert).
- Gesten (Robolectric): Nullen, Pause/Weiter, Ton, Empfindlichkeit, Einführung schließen. Pausiert keine Frames. Sensor und Ton folgen ON_START/ON_STOP.
- `:apps:magnr:check`, `verifyRoborazziDebug`, `assembleRelease` grün; Manifest ohne Berechtigungen; Release-APK < 4 MB.
