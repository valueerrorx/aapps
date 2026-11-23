# thinkr – SPEC (Phase 1: Gerüst + Echo, Phase 2: Weichen)

## Aufgabe
Kurze Denkspiele für zwischendurch, je etwa eine Minute. Offline, ohne Werbung, ohne Abo, ohne Konto. Jeden Tag stellt thinkr bis zu drei Spiele als Tagestraining zusammen. Unterhaltung, keine Gesundheitsversprechen.

Erster Release: 5 Spiele (Echo, Weichen, Sternbild, Tinte, Schwarm), die übrigen 4 (Bande, Tropfen, Rundblick, Slalom) danach. Phase 1 enthält das Gerüst und Echo, Phase 2 Weichen.

## Rechtliche Linie
- Nur die Spiel-Prinzipien sind übernommen (klassische Psychologie-Aufgaben wie n-back). Eigene Namen, Figuren, Optik, Texte und Töne.
- Nirgends fremde Marken- oder Spielnamen, auch nicht in Store-Texten oder als Vergleich.
- Keine Wirkversprechen („trainiert", „verbessert", „wissenschaftlich"), kein Gehirn-Index, kein Altersvergleich.
- Info: „Unterhaltung. Kein Medizinprodukt, keine Gesundheitsversprechen."

## Rahmen (Muss)
- Spiele-Registry: ein Spiel = Domain-Ordner + Board + Registry-Eintrag (`Games.kt`).
- Runde: 3-2-1-Einzählen, 60 s Spielzeit (Weichen 90 s), Ergebnis. Die Spielzeit läuft nur in der Phase „Spielen"; Pause hält sie an.
- Pause per Knopf oben links, per Zurück und automatisch bei `ON_STOP`. Zurück in der Pause beendet die Runde ohne Wertung. Ein laufendes Spiel wird nicht gespeichert (Prozess-Tod = Runde verfällt).
- Punkte: richtig = Grundpunkte × Multiplikator. Multiplikator +1 nach je 4 Richtigen in Folge (max. 5), Fehler setzt auf 1.
- Stufe je Spiel (1…N) wird gespeichert. Start auf gespeicherter Stufe minus 2 (Aufwärmen), aber nicht unter den Beginn der aktuellen Spielart (Echo: 1, 10, 14).
- Gemächlich (Einstellung): +50 % Zeit je Aufgabe, eigene Stufe und eigener Bestwert.
- Tagestraining: deterministisch aus dem lokalen Datum, min(3, Spielzahl) verschiedene Spiele. Wochenrotation (Wochen-Seed): Tag w der Woche nimmt die Plätze 3w…3w+2 einer gemischten Spieleliste. Bei 9 Spielen spielt jedes ≥ 2× pro Woche und kein Spiel an zwei Tagen hintereinander (Wochenwechsel per Tausch-Regel, verankert am 5.1.2026). Ein Spiel zählt für heute, sobald heute eine Runde davon gespielt wurde (auch aus der Spieleliste).
- Tageswechsel wird beim Öffnen (`ON_START`) übernommen, nicht mitten im Spiel.
- Serie: Tage mit erledigtem Tagestraining. Ein verpasster Tag wird still überbrückt, wenn der letzte überbrückte mindestens 7 Tage zurückliegt. Zwei Lücken in Folge beenden die Serie, ohne Schuld-Text („Neue Serie beginnt heute"). Heute offen zählt nicht als Lücke.
- Speicher: SharedPreferences, ein Text-Blob `thinkr1` (Zeilen mit `|`): Fortschritt `P`, Runden `R` (max. 1000, max. 400 Tage), erledigte Tage `D`. Kaputte Zeilen werden übersprungen.
- Haptik: Tipp/Wisch `press()`; Multiplikator-Aufstieg `tick()`; Rundenende `confirm()`; neuer Bestwert `tick()`. Fehler nur optisch (Wackeln, Bernstein-Rand), kein Vibrieren.
- Töne (Standard aus): im Code erzeugt, richtig / falsch / Rundenende, dezent.
- Keine Benachrichtigungen, keine Berechtigung außer VIBRATE (aus `core/ui`).

## Echo (Pilot)
- Glas-Karten mit Leucht-Glyphen (Kreis, Dreieck, Raute, Stern, Welle, Ring) gleiten nacheinander in die Mitte. Frage: gleich wie die vorige? Zwei große Tasten unten: links Nein (✕), rechts Ja (✓). Wisch links/rechts zusätzlich.
- Die erste Karte (bzw. die ersten zwei bei 2-back) nur merken: sie bleibt 1,2 s stehen und wechselt von selbst.
- Ab Stufe 10: 2-back (vergleichen mit der Karte vor zwei), Plakette „2" über der Karte. Ab Stufe 14: ähnliche Glyphen als Köder (Kreis/Ring, Dreieck/Raute). Max. Stufe 20.
- Frist je Karte. Abgelaufen = Fehler. Treppe auf die Frist: 3 richtig in Folge −8 %, 1 Fehler +12 %. Stufe = Frist-Band: Spielart 1 (Stufe 1–9) ab 2400 ms, Spielart 2 (10–13) ab 3000 ms, Spielart 3 (14–20) ab 2800 ms, je Stufe ×0,88. Am Rundenende zählt das Band der aktuellen Frist; innerhalb einer Runde höchstens eine Spielart hinauf oder hinunter.
- Kartenfolge: Anteil „gleich" 40–60 %, nie mehr als 4 gleiche Antworten in Folge. 2-back nutzt auch die Karte direkt davor als Köder.
- Nach jeder Antwort 120 ms Wechsel, Eingaben darin zählen nicht.
- Punkte: 50 × Multiplikator, +15 × Multiplikator bei Antwort unter der halben Frist.
- Optik: Karte kommt mit Feder-Überschwinger, Glasreflex, Akzent-Glow. Richtig: Karte fliegt in Antwortrichtung mit Drehung weg. Falsch: Wackeln, Bernstein-Kante. Fristlinie unter der Karte.
- Barrierefreiheit: nur die Form zählt, alle Glyphen gleiche Farbe.

## Weichen
- Schienennetz aus Leuchtlinien (nur waagrecht, senkrecht, 45°, runde Ecken) auf dunklem Grund. Aus dem Portal rollen nacheinander Lichtperlen. Jede Perle hat Farbe und Form ihres Tors. Tipp auf eine Weiche legt sie um (Tippradius ≥ 26 dp bzw. 1,15 Rasterfelder, nächste Weiche gewinnt, löst beim Aufsetzen aus). Perlen fahren gleichmäßig und blockieren sich nicht.
- Runde 90 s. Richtige Ankunft: 40 × Multiplikator. Falsche: 0 Punkte, Multiplikator auf 1. Ergebnis zeigt „Treffer 27/31".
- Stufe = Netzgröße: Stufe 1 = 3 Tore, je Stufe ein Tor mehr bis 10 Tore (Stufe 8), Stufe 9–12 nur schneller. Tempo 2,2 Felder/s × 1,035 je Stufe.
- Takt adaptiv in der Runde: Startabstand 3,8 s × 0,97 je Stufe; nach 3 richtigen Ankünften in Folge −8 %, nach jeder Fehlleitung +15 % (0,9–6 s). Erste Abfahrt nach 0,6 s, keine Abfahrt in den letzten 2 s. Höchstens 2 gleiche Ziele hintereinander.
- Stufe nach der Runde: Trefferquote ≥ 90 % +1, ≥ 70 % gleich, sonst −1; unter 6 Ankünften bleibt sie. Kein Aufwärm-Abschlag (die Stufe ändert sich nur um 1 je Runde), aufgewärmt wird über den lockeren Takt am Rundenbeginn.
- Gemächlich: Perlen ⅔ so schnell, Takt × 1,5.
- Netze: handgebaute Sammlung als Zeichen-Raster (`NetBook`), 3 je Torzahl 3–6, 2 je Torzahl 7–10, je 4 Varianten (gespiegelt, gekippt). Im Querformat wird das Netz gedreht, wenn es dadurch größer wird. Farben und Weichenstellung je Runde zufällig.
- Tests: jedes Netz ist ein Baum (keine Sackgasse, keine Schleife), jedes Tor über genau eine Stellung erreichbar, nur 0/45/90°, Knoten ≥ 2,8 Felder auseinander, Tore ≥ 1,9 Felder von fremden Schienen. Ereignisse laufen streng nach Zeit (Ergebnis unabhängig von der Bildrate). Ein perfekter Spieler bringt auf jeder Stufe alle Perlen heim.
- Optik: gestellter Abgang hell, der andere gedimmt; Weichenpfeil und Schienen schnappen mit Feder um. Tor = Ring mit Öffnung zur Schiene und Form innen; richtig schluckt es die Perle mit Ring-Welle, falsch wackelt es mit Bernstein-Ring und die Perle verpufft grau. Perlen mit Lichtschweif und kurzem Federn nach einer Weiche. Portal pulst bei jeder Abfahrt. Keine Züge, Bahnhöfe, Schwellen oder Bäume.
- Alle Bewegungen hängen nur an der Spielzeit: in der Pause steht alles still, auch die Federn.
- Haptik: Weiche `press()`, richtige Ankunft und Kettenaufstieg `tick()` (entprellt), Rundenende `confirm()`. Fehler nur optisch.
- Barrierefreiheit: jede Farbe hat ihre Form (Kreis, Dreieck, Quadrat, Raute, Stern, Sechseck, Kreuz, Halbmond, Herz, Ring), Palette Okabe-Ito mit Helligkeitsstufen.

## Screens
1. **Heute**: Datum, oben Spiele und Zahnrad. Hero: Tagesbild aus bis zu 3 Lichtkugeln (Spielsymbol, Name, Stufe), verbunden durch Linien, erledigt = Akzent mit Haken. Darunter Serie und Woche Mo–So. Pille „Start" / „Weiter" / „Fertig für heute".
2. **Spiele**: alle Spiele mit Stufe und Bestwert, Tipp startet.
3. **Spiel**: Zeitleiste, Punkte, Multiplikator mit 4 Punkten, Pause.
4. **Ergebnis**: Punkte groß, Stufe mit Pfeil, Treffer in %, Bestwert, „Neuer Bestwert" mit einmaligem Glanz, „Tagestraining erledigt". Pille „Weiter" (nächstes Tagesspiel) bzw. „Nochmal", Text-Knopf „Fertig".
5. **Einstellungen**: Gemächlich, Haptik, Töne, Stufe zurücksetzen je Spiel (zweimal tippen), Info.
6. **Info**: wie alle aapps, plus Spielregeln, Tagestraining, Hinweis zu TalkBack, Gesundheits-Satz.

Verlauf-Screen kommt in einer späteren Phase.

## Akzent und Icon
- Akzent Orchidee `#F27BE8`.
- Icon: drei Punkte als Mini-Sternbild, zwei Linien, der dritte Punkt größer und leuchtend.

## Screenshot-Zustände (`apps/thinkr/screenshots/`)
| Name | Inhalt |
|---|---|
| `home` | Tagesbild, nichts erledigt, Serie 4 |
| `home_done` | alles erledigt, Pille „Fertig für heute" |
| `games` | Spieleliste |
| `echo_countdown` | Einzählen mit Frage |
| `echo_play` | Echo 1-back mitten im Spiel, Multiplikator ×3 |
| `echo_2back` | Echo 2-back mit Plakette |
| `echo_paused` | Pause |
| `result_best` | Ergebnis mit neuem Bestwert und Stufe hinauf |
| `result_daily` | Ergebnis, Tagestraining erledigt |
| `switches_countdown` | Weichen: Einzählen mit Frage über dem Netz |
| `switches_play` | Weichen Stufe 1 (3 Tore) mitten im Spiel |
| `switches_eight` | 8 Tore, kurz nach einer Fehlleitung |
| `switches_ten` | 10 Tore |
| `switches_paused` | Pause |
| `switches_nets` | alle Netze der Sammlung (Grundvariante) |
| `result_switches` | Ergebnis mit Treffer 27/29 |
| `settings`, `about` | Einstellungen, Info |
| `icon_masks`, `icon_store_512` | App-Icon |

Varianten: `_de`, `_land`, `_font200` für home, games, echo_play, result_best; dazu `home_360dp`, `echo_countdown_de_font200`, `result_best_shine`, `settings_de`, `settings_font200`, `about_de`.

Weichen-Varianten: `switches_play_de`, `_land`, `_font200`, `switches_play_360dp` (8 Tore), `switches_eight_land`, `switches_ten_land`, `switches_countdown_de_font200`, `result_switches_de`.

`preview_home3`, `preview_home3_land`: Layout-Vorschau des Tagesbilds mit drei Kugeln (Echo, Weichen, Platzhalter), bis das dritte Spiel da ist.

## Abnahme
- Domain ohne Android-Imports; Unit-Tests für Kette, Treppe, Runde, Echo (Kartenfolge, Frist, Ablauf, Wechsel-Sperre, Punkte, Stufen), Tagestraining, Serie, Fortschritt und Codec (inkl. kaputter Daten).
- Pause bei `ON_STOP` (Robolectric-Test).
- `:apps:thinkr:check`, `assembleDebug`, `verifyRoborazziDebug` grün. Manifest nur mit VIBRATE.
