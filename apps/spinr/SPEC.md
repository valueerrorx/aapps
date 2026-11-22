# spinr – SPEC

## Aufgabe
Ein Glücksrad: dreht sich wie ein echtes, bleibt fair irgendwo stehen und zeigt groß, was gezogen wurde.

## Muss
- Rad aus einer Liste (2–50 Einträge, je max. 40 Zeichen). Leere Einträge zählen nicht.
- Drehen per Tippen aufs Rad, per Hauptaktion „Drehen" oder per Schwung (Wischen im Kreis). Langsames Ziehen dreht das Rad nur von Hand, ohne Ergebnis.
- Physik: Anfangsschwung, Abbremsen aus Gleitreibung plus geschwindigkeitsabhängiger Reibung (geschlossene Lösung, keine Integrationsfehler). Kein Einrasten am Ende: Ergebnis ist das Segment, auf dem der Zeiger stehen bleibt.
- Fairness: die Endstellung ist gleichverteilt über 360°. Tippen: Weg = 4–5 volle Umdrehungen + gleichverteilter Rest. Schwung: natürlicher Weg aus der Wischgeschwindigkeit (2,5–10 Umdrehungen), auf ±180° so angepasst, dass der Rest gleichverteilt ist. Während des Drehens ist das Rad gesperrt.
- Stifte an den Segmentgrenzen, eine Zunge am Zeiger wird mitgenommen und schnellt zurück. Jeder Stift = Haptik-Tick (max. ~35/s), optional Klick-Ton (im Code erzeugt, ohne Datei). Bewegungsunschärfe bei hoher Geschwindigkeit.
- Ergebnis: großer Text in der Segmentfarbe, Gewinner-Segment hebt sich, die anderen treten zurück, Konfetti aus dem Zeiger, Haptik `confirm`, Ansage für TalkBack.
- „Entfernen": gezogenen Eintrag für diese Runde herausnehmen (Reihenfolge auslosen). „Alle zurück" stellt die Runde wieder her. Nicht gespeichert.
- Mehrere Räder gespeichert, eines aktiv. Vorlagen beim ersten Start und jederzeit hinzufügbar: Was essen wir?, Ja/Nein, Würfel 1–6, Wer zahlt?.
- Rad bearbeiten: Name, Einträge ändern/hinzufügen/löschen, Rad löschen (das letzte bleibt).
- Farben: Pastell-Palette, Nachbarn nie gleich (auch über den Kreis).
- Hoch- und Querformat, Englisch und Deutsch. Keine Berechtigungen.

## Nicht
- Keine Gewichte, kein Münz-/Würfelmodus, kein Teilen, keine Historie, kein INTERNET.
- Kein Glücksspiel-Wording (keine Einsätze, kein „Gewinn").

## Screens
1. **Rad** (Hero ≥ 60 %): oben Rad-Name (öffnet Räder) und Zahnrad, darunter Ergebnis bzw. Hinweis, Rad, unten „Drehen" und ggf. „Entfernen"/„Alle zurück".
2. **Räder**: Liste (Auswahl, Bearbeiten), neues Rad, Vorlagen.
3. **Bearbeiten**: Name, Einträge mit Farbpunkt, Eintrag hinzufügen, Rad löschen.
4. **Einstellungen**: Haptik, Ton, Räder, Info.
5. **Info**: wie alle aapps.

## Screenshot-Zustände (Roborazzi, `apps/spinr/screenshots/`)
| Name | Inhalt |
|---|---|
| `wheel` | Essen-Rad (8), Ruhe, Hinweis |
| `spinning` | schnelle Drehung, Unschärfe, Zunge gebogen |
| `result` | Ergebnis „Sushi", Gewinner hervorgehoben, Konfetti |
| `many` | 30 Einträge, Ergebnis |
| `removed` | Runde mit 2 entfernten Einträgen |
| `wheels`, `editor`, `settings`, `about` | Räder, Bearbeiten, Einstellungen, Info |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard: Pixel 7, en): `wheel_de`, `_360dp`, `_land`, `_font130`, `_font200`; `result_de`, `result_land`; `editor_de`; `settings_de`, `settings_font200`; `about_de`.

## Abnahmekriterien
- Domain ohne Android-Imports, Unit-Tests für Reibung/Umkehrung, Drehverlauf, Segment unter dem Zeiger, Fairness (Chi-Quadrat über viele Drehungen, Tippen und Schwung), Stifte/Zunge, Palette, Speicherformat, Runde, Klick-Ton.
- Ruht das Rad, laufen keine Animations-Frames mehr.
- `:apps:spinr:check`, `verifyRoborazziDebug`, `assembleRelease` grün, Manifest ohne Berechtigungen, Release-APK < 4 MB.
- Lesbar bei Schriftgröße 200 %.
