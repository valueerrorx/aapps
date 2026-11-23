# magnr – SPEC

## Aufgabe
Metalldetektor und Magnetfeld-Messer: Schrauben, Nägel, Stahlträger und Magnete in der Nähe finden und die Feldstärke in µT zeigen. Hübsch und ehrlich: findet nur, was magnetisch ist.

## Muss
- Sensor `TYPE_MAGNETIC_FIELD` (kalibriert), 50 Hz (`samplingPeriodUs = 20 000`, ≙ `SENSOR_DELAY_GAME`): flott genug für Ton und Ausschlag, UI-Rate (~16 Hz) wäre zu träge.
  - Warum nicht `UNCALIBRATED`: dort steckt der Hart-Eisen-Versatz des Handys selbst (Lautsprecher, Kamera, oft 20–200 µT) im Wert. Der Betrag ändert sich dann schon beim Drehen des Handys, jeder Schwenk wäre ein Fehlalarm. Kalibriert ist der Betrag weitgehend lageunabhängig, bis auf einen Rest-Versatz (siehe unten). Nachteil: Die Online-Kalibrierung des Systems springt gelegentlich. Das fangen Nullen und der automatische Grundwert ab.
- Rest-Versatz (Feldtest Pixel: Fächer füllt sich beim Kippen ohne Metall, Acht hilft nicht):
  - Ursache: Die Systemkalibrierung lässt einen Rest-Versatz o (Hart-Eisen) übrig. Dann ändert sich |B| beim Kippen um bis zu 2·|o|, bei 10–15 µT also mehrere Bögen.
  - Abhilfe: zusätzlich `TYPE_GAME_ROTATION_VECTOR` (Lage aus Gyro und Beschleunigung, ohne Magnetometer), 50 Hz, auf den Zeitpunkt des Magnetwerts interpoliert.
  - Ohne Metall dreht sich B − o starr mit dem Handy: (I − ΔR)·o = B_t − ΔR·B_k, ΔR = R_tᵀ·R_k. Eine Gleichung je ≥ 15° Drehung innerhalb 1 s, kleinste Quadrate mit Vergessen (0,98 je Gleichung) und Ridge 0,05 (nicht beobachtete Richtungen bleiben 0).
  - Reines Verschieben (Überstreichen einer Wand) liefert keine Gleichung, Metall stört die Schätzung also kaum. Gleichungen mit Rest > 15 µT (Metall im Gefälle, Kalibriersprung) fallen weg, |o| höchstens 100 µT.
  - Neuer Versatz: Glättung und Grundwert werden in ihrer Lage neu gerechnet (Bezugsrohwert beim Einschwingen/Nullen), kein Sprung.
  - Ohne Lagesensor: wie bisher (o = 0).
- Messgröße ist der Betrag |B − o| (lageunabhängig), geglättet exponentiell mit τ = 0,12 s.
- Grundwert (Erdfeld am Ort):
  - automatisch: Mittel der ersten 0,5 s nach dem Start.
  - Kurze Pause (≤ 60 s, auch Einstellungen oder App-Wechsel): Er bleibt, sonst würde ein gerade gehaltener Fund zum Grundwert. Längere Pause, Prozess-Tod über 60 s oder Neustart des Geräts: Er schwingt neu ein, weil das Handy vermutlich woanders steht. Der Pausenbeginn (elapsedRealtime) wird mitgesichert.
  - „Nullen“ (Hauptaktion): Grundwert = aktueller Betrag, mit Haptik. Pausiert: misst weiter und schwingt neu ein.
  - Driftausgleich (abschaltbar, Standard an): nur in Stufe „ruhig“ und solange |Δ| unter der doppelten Rauschschwelle der Empfindlichkeit liegt (6 / 1,8 / 1,2 µT). Dann folgt der Grundwert langsam (τ = 30 s). Ein gehaltener Fund verschwindet so nicht.
- Abweichung Δ = Betrag − Grundwert (mit Vorzeichen). Ausschlag 0…1 = (|Δ| − Rauschen) / (Vollausschlag − Rauschen).
- Empfindlichkeit: Vollausschlag 100 / 40 / 12 µT (niedrig / mittel / hoch, durch 4 teilbar für runde Skalenwerte), Rauschschwelle 3 / 0,9 / 0,6 µT.
- Stufen mit Hysterese (0,04): ruhig < 0,12 ≤ schwach < 0,45 ≤ nah < 0,8 ≤ sehr nah. Haptik-Tick beim Steigen der Stufe, „sehr nah“ mit Bestätigungs-Haptik. Abschaltbar. Ein Wechsel der Empfindlichkeit setzt die Stufe aus dem aktuellen Δ, ohne Haptik.
- Spitze: größte |Δ| bleibt 2 s stehen, dann folgt sie dem aktuellen Wert.
- Verlauf der letzten 20 s: Ausschlag je 100 ms, Pausen als Lücke.
- Ton (Standard aus, Schalter auf dem Hauptschirm und in den Einstellungen): kurze Pieptöne ab „schwach“.
  - Tonhöhe 520 → 2080 Hz (zwei Oktaven).
  - Abstand 600 → 60 ms, ab 0,95 Dauerton.
  - Sinus mit 3 ms Ein- und Ausblendung (kein Knacksen), Medienlautstärke.
  - Höchstens ein Audio-Thread. Stopp blockiert den Main-Thread nicht.
- Sensor am Anschlag: Hinweis „Am Anschlag“ statt Stufe.
  - Annahme: Kalibrierte Werte sind um den Hart-Eisen-Versatz (bis einige 100 µT) verschoben, die Rohgrenze `maximumRange` liegt also nicht genau bei |Achse| = maxRange.
  - Darum zwei Zeichen: eine Achse ≥ 90 % von `maximumRange`, oder eine Achse mit |Wert| ≥ 200 µT, die 10 Werte lang exakt gleich bleibt (geclippt, kein Rauschen).
- Kalibrierungshinweis (Acht-Bewegung), wenn der Sensor ≥ 1 s Genauigkeit „niedrig/unzuverlässig“ meldet. Ausblendbar bis zur nächsten Verschlechterung, manuell über die Einstellungen.
- Ehrlichkeit:
  - Einführungskarte beim ersten Start: findet Eisen, Stahl, Nickel und Magnete, kein Gold/Silber/Kupfer/Alu. Sensor sitzt meist oben am Handy, mit der Oberkante langsam über die Wand fahren.
  - Fußzeile dauerhaft. Wieder aufrufbar in den Einstellungen.
- Pause/Weiter (Nebenaktion): pausiert sind Sensor und Ton aus, Anzeige gedimmt, keine Frames.
- Sensor, Ton und Haptik nur im Vordergrund und nur auf dem Hauptschirm: ab ON_STOP oder in Einstellungen/Info aus, danach wieder an (außer pausiert). Kein Dienst.
- TalkBack: Fächer und Werte haben einen Zustandstext (Δ, Stufe, Feld, Spitze), neu höchstens einmal pro Sekunde.
- Kein Magnetometer: klarer Hinweis statt Messung, kein Absturz.
- Zustand übersteht Drehung und Prozess-Tod:
  - per SavedState: Grundwert, Bezugsrohwert, Versatz-Schätzung, Verlauf, Pause.
  - per SharedPreferences: Einstellungen, Einführung gesehen.
- Bildschirm bleibt beim Messen an (abschaltbar). Hoch- und Querformat, Englisch und Deutsch, lesbar bei 200 % Schrift.

## Berechtigungen
Keine (Lagesensor braucht keine). Haptik über die View, Ton über AudioTrack. Kein INTERNET, kein Dienst.

## Nicht
- Keine „Gold-/Schatzsuche“, keine EMF-/Strahlungs- oder Gesundheitsaussagen, keine Richtungs-/Vektoranzeige, kein Export, keine eigene volle Kalibrierung (macht das System, magnr schätzt nur den Rest-Versatz nach).

## Screens
1. **Detektor** (Hero ≥ 60 %):
   - oben Status (Misst/Pausiert) und Zahnrad.
   - Hero = Fächer, bewusst kein Kreis- oder Ring-Gauge (unterscheidbar von loudr/nordr/tunr): 8 konzentrische Bögen mit 100° Öffnung über einem Sensorpunkt unten in der Mitte, nach oben immer breiter wie ein WLAN-Zeichen.
     - Je Bogen ein Achtel des Vollausschlags. Die Bögen leuchten von innen nach außen, der äußerste aktive wächst von der Mitte zu den Seiten.
     - Farbverlauf violett → hell, Schein auf aktiven Bögen, dünner Bogen als Spitzen-Marke.
     - Skala am rechten Bogenende als Abweichung („±20“, „±40 µT“).
   - Unter dem Fächer groß die Abweichung mit Vorzeichen und Einheit („+21.3 µT“), darunter die Stufe mit Punkt.
   - darunter Feld (Betrag) · Grundwert · Spitze, Verlauf 20 s, Fußzeile.
   - unten Pause, „Nullen“, Ton.
   - Querformat: Werte links, Fächer rechts.
2. **Karten** über dem Fächer: Einführung (Handy mit Sensorpunkt oben), Kalibrieren (Acht).
3. **Kein Sensor**: leerer Fächer, Erklärung.
4. **Einstellungen**: Empfindlichkeit, Driftausgleich, Ton, Vibration, Bildschirm an, „Was magnr findet“, Kalibrierhilfe, Info.
5. **Info**: wie alle aapps, plus Ehrlichkeitshinweis.

## Screenshot-Zustände (Roborazzi, `apps/magnr/screenshots/`)
| Name | Inhalt |
|---|---|
| `near` | Betrag 69,2 µT, Δ +21,3, mittel, Stufe nah, Spitze +24,6 |
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
  - Rest-Versatz: Kippen ohne Metall (Erdfeld ~49 µT + 15 µT Versatz) bleibt nach dem ersten Kippen je Achse ruhig, Metall danach klar „nah“, Verschieben ohne Drehen liefert keine Gleichung, Schätzung übersteht Sichern
  - Ausschlag/Empfindlichkeit, Stufen mit Hysterese, Spitze halten
  - Verlauf mit Lücke, Anschlag (Grenze und klebende Achse), Genauigkeit mit Wartezeit
  - Sichern/Wiederherstellen inklusive veraltetem Grundwert nach langer Pause, Tonabbildung (Tonhöhe, Abstand, Stille), Piep-Synthese (Frequenz, keine Sprünge)
  - Kontrast (WCAG AA gemessen, auch pausiert, jeder Fächerbogen).
- Gesten (Robolectric): Nullen, Pause/Weiter, Ton, Empfindlichkeit, Einführung schließen, Einstellungen stoppen die Messung. Pausiert keine Frames. Sensor und Ton folgen ON_START/ON_STOP. Ein Ton-Thread, nicht blockierend.
- `:apps:magnr:check`, `verifyRoborazziDebug`, `assembleRelease` grün; Manifest ohne Berechtigungen; Release-APK < 4 MB.
