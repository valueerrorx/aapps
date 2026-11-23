# scorr – SPEC

## Aufgabe
Punkte zählen beim Spieleabend, Kartenspiel oder Tischtennis: große Farbflächen pro Spieler, ein Tipp zählt.

## Muss
- 1–8 Spieler oder Teams, jeder mit Name (max. 24 Zeichen) und einer von 8 Farben. Leerer Name zeigt „Spieler n".
- Kachel pro Spieler (Hero, zusammen ≥ 60 %): Tippen obere Hälfte +1, untere Hälfte −1. Senkrecht ziehen zählt in Schritten (je 24 dp ein Punkt, nach oben plus) mit Vorschau „+7" und Haptik je Schritt (`step`, Tippen `press`), Loslassen bucht. Lange drücken öffnet die Eingabe (beliebige Zahl addieren oder abziehen).
- Schnelle Änderungen am selben Spieler (< 2 s) werden zu einem Zug zusammengefasst; die Kachel zeigt den Zug kurz als „+3". Ein Zug, der sich auf 0 aufhebt, verschwindet.
- Rückgängig: nimmt den letzten Zug bzw. das letzte Rundenende zurück, beliebig oft.
- Runden (abschaltbar): Hauptaktion „Nächste Runde". Verlauf als Tabelle: Summen oben (am größten), darunter Runden absteigend mit dem Punkteunterschied je Spieler.
- Ziel (optional, beliebige Zahl): erreicht ein Spieler das Ziel, ist das Spiel aus. Gewinner nach Einstellung „meiste" oder „wenigste Punkte" (z. B. Uno). Gleichstand = mehrere Gewinner.
- Gewinner: Krone, Glanz einmalig, Lichthof, die anderen treten zurück, Vibration `confirm` (Ereignis über den Vibrator, Usage Alarm), Ansage für TalkBack. Ohne Ende trägt die Führung eine kleine Krone.
- Neues Spiel: Punkte und Verlauf löschen, Spieler bleiben (zweimal tippen zum Bestätigen).
- Zustand übersteht Drehung und Prozess-Tod (bei jeder Änderung gespeichert). Bildschirm bleibt an (abschaltbar).
- Punkte −999 999 … 999 999, Minus als echtes „−". Zahl füllt die Kachel und wird nie umgebrochen.
- Hoch- und Querformat, Englisch und Deutsch. Nur VIBRATE (normal, ohne Rückfrage, kommt aus `core/ui`).

## Nicht
- Keine Spielvorlagen/Regelwerke, kein Timer, kein Teilen, keine Statistik über Spiele hinweg, kein INTERNET.
- Keine Spieler-Reihenfolge per Ziehen, keine gedrehte Kachel für Gegenüber.

## Raster
Hochformat: 1–3 untereinander, 4 = 2×2, 5–6 = 2×3, 7–8 = 2×4. Querformat: 1–4 nebeneinander, 5–6 = 3×2, 7–8 = 4×2. Querformat: Bedienung links, Kacheln rechts.

## Screens
1. **Tafel**: oben Runden-Chip (öffnet Verlauf), Rückgängig, Zahnrad; Kacheln; unten „Nächste Runde" bzw. „Neues Spiel" nach Spielende.
2. **Eingabe** (über der Tafel): Spielername in seiner Farbe, Zahl, „Abziehen" / „Addieren".
3. **Verlauf**: Summen und Runden-Tabelle, waagrecht scrollbar bei vielen Spielern.
4. **Spieler**: Name, Farbe, entfernen (mit Punkten: zweimal tippen), hinzufügen.
5. **Einstellungen**: Spieler, Ziel, Gewinner, Runden, Haptik, Bildschirm an, Neues Spiel, Info.
6. **Info**: wie alle aapps.

## Screenshot-Zustände (Roborazzi, `apps/scorr/screenshots/`)
| Name | Inhalt |
|---|---|
| `board` | 2 Spieler, Runde 3, Führung |
| `board4` | 4 Spieler, Zug-Anzeige „+3" |
| `board8` | 8 Spieler, ein Wert negativ |
| `scrub` | Ziehen mit Vorschau „+7" |
| `winner` | Ziel 100 erreicht, Gewinner hervorgehoben (Ruhezustand), `winner_shine` mit Glanz |
| `board8_land_extreme` | 8 Spieler quer, Grenzwerte −999 990 und 999 999 |
| `entry` | Eingabe über der Tafel |
| `history`, `players`, `settings`, `about` | Verlauf, Spieler, Einstellungen, Info |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard Pixel 7, en): `_de`, `_land`, `_font200`, `_land_font200` für alle Hauptansichten (Tafel, Gewinner, Eingabe, Verlauf, Spieler, Einstellungen); dazu `board_360dp`, `board_font130`, `board4_land`, `board8_land`, `board_land_font200_de`, `winner_again_land_font200_de` (Bestätigung „Neues Spiel").

## Abnahmekriterien
- Domain ohne Android-Imports, Unit-Tests für Zählen/Zusammenfassen, Rückgängig, Runden-Tabelle, Ziel/Gewinner (beide Richtungen, Gleichstand), Spieler ändern/entfernen, Grenzen, Speicherformat (inkl. kaputter Daten), Raster, Zahlformat.
- Im Leerlauf keine Animations-Frames.
- `:apps:scorr:check`, `verifyRoborazziDebug`, `assembleRelease` grün, Manifest nur mit VIBRATE, Release-APK < 4 MB.
- Lesbar bei Schriftgröße 200 %.
