# thinkr – SPEC (Phase 1: Gerüst + Echo)

## Aufgabe
Kurze Denkspiele für zwischendurch, je etwa eine Minute. Offline, ohne Werbung, ohne Abo, ohne Konto. Jeden Tag stellt thinkr bis zu drei Spiele als Tagestraining zusammen. Unterhaltung, keine Gesundheitsversprechen.

Erster Release: 5 Spiele (Echo, Weichen, Sternbild, Tinte, Schwarm), die übrigen 4 (Bande, Tropfen, Rundblick, Slalom) danach. Phase 1 enthält das Gerüst und Echo.

## Rechtliche Linie
- Nur die Spiel-Prinzipien sind übernommen (klassische Psychologie-Aufgaben wie n-back). Eigene Namen, Figuren, Optik, Texte und Töne.
- Nirgends fremde Marken- oder Spielnamen, auch nicht in Store-Texten oder als Vergleich.
- Keine Wirkversprechen („trainiert", „verbessert", „wissenschaftlich"), kein Gehirn-Index, kein Altersvergleich.
- Info: „Unterhaltung. Kein Medizinprodukt, keine Gesundheitsversprechen."

## Rahmen (Muss)
- Spiele-Registry: ein Spiel = Domain-Ordner + Board + Registry-Eintrag (`Games.kt`).
- Runde: 3-2-1-Einzählen, 60 s Spielzeit, Ergebnis. Die Spielzeit läuft nur in der Phase „Spielen"; Pause hält sie an.
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
| `settings`, `about` | Einstellungen, Info |
| `icon_masks`, `icon_store_512` | App-Icon |

Varianten: `_de`, `_land`, `_font200` für home, games, echo_play, result_best; dazu `home_360dp`, `settings_de`, `about_de`.

## Abnahme
- Domain ohne Android-Imports; Unit-Tests für Kette, Treppe, Runde, Echo (Kartenfolge, Frist, Ablauf, Wechsel-Sperre, Punkte, Stufen), Tagestraining, Serie, Fortschritt und Codec (inkl. kaputter Daten).
- Pause bei `ON_STOP` (Robolectric-Test).
- `:apps:thinkr:check`, `assembleDebug`, `verifyRoborazziDebug` grün. Manifest nur mit VIBRATE.
