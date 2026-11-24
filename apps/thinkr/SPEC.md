# thinkr – SPEC (Phase 1: Gerüst + Echo, Phase 2: Weichen, Phase 3: Sternbild, Tinte, Schwarm = Release 1, Phase 4: Bande, Tropfen, Rundblick, Slalom)

## Aufgabe
Kurze Denkspiele für zwischendurch, je etwa eine Minute. Offline, ohne Werbung, ohne Abo, ohne Konto. Jeden Tag stellt thinkr bis zu drei Spiele als Tagestraining zusammen. Unterhaltung, keine Gesundheitsversprechen.

Erster Release: 5 Spiele (Echo, Weichen, Sternbild, Tinte, Schwarm), die übrigen 4 (Bande, Tropfen, Rundblick, Slalom) danach (Phase 4). Phase 1 enthält das Gerüst und Echo, Phase 2 Weichen.

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
- Haptik: Tipp/Wisch `press()`; Multiplikator-Aufstieg `tick()`; Rundenende `confirm()`; neuer Bestwert `tick()`. Fehler nur optisch (Wackeln, Bernstein-Rand), kein Vibrieren. Spiele melden zusätzlich `Play.steps` (Raster, z. B. Abprall → `step()`) und `Play.jolts` (harter Stoß, z. B. Zusammenstoß → `confirm()`).
- Runde vor der Zeit beenden: `Round.end()` (Becken voll, Leben weg). `GameDef.timed = false`: keine Zeitleiste, das Spiel zeigt seinen Stand selbst (Tropfen).
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
- Netze: handgebaute Sammlung als Zeichen-Raster (`NetBook`), 3 je Torzahl 3–6, 2 je Torzahl 7–10, je 4 Varianten (gespiegelt, gekippt). Anlaufstrecke Portal → erste Weiche ≥ 6 Felder (Stufe 1 ≥ 2,5 s, jede Stufe ≥ 1,8 s Zeit bis zur ersten Weiche). Im Querformat wird das Netz gedreht, wenn es dadurch größer wird. Farben und Weichenstellung je Runde zufällig.
- Tests: jedes Netz ist ein Baum (keine Sackgasse, keine Schleife), jedes Tor über genau eine Stellung erreichbar, nur 0/45/90°, Knoten ≥ 2,8 Felder auseinander, Tore ≥ 1,9 Felder von fremden Schienen, Zeit bis zur ersten Weiche (s. o.). Ereignisse laufen streng nach Zeit (Ergebnis unabhängig von der Bildrate). Ein perfekter Spieler bringt auf jeder Stufe alle Perlen heim.
- Optik: gestellter Abgang hell, der andere gedimmt; Weichenpfeil und Schienen schnappen mit Feder um. Tor = Ring mit Öffnung zur Schiene und Form innen; richtig schluckt es die Perle mit Ring-Welle, falsch wackelt es mit Bernstein-Ring und die Perle verpufft grau. Perlen mit Lichtschweif und kurzem Federn nach einer Weiche. Portal pulst bei jeder Abfahrt. Keine Züge, Bahnhöfe, Schwellen oder Bäume.
- Alle Bewegungen hängen nur an der Spielzeit: in der Pause steht alles still, auch die Federn.
- Haptik: Weiche `press()`, richtige Ankunft und Kettenaufstieg `tick()` (entprellt), Rundenende `confirm()`. Fehler nur optisch.
- Barrierefreiheit: jede Farbe hat ihre Form (Kreis, Dreieck, Quadrat, Raute, Stern, Sechseck, Kreuz, Halbmond, Herz, Ring), Palette Okabe-Ito mit Helligkeitsstufen.

## Gemeinsam für Sternbild, Tinte, Schwarm
- Alle Bewegungen (Federn, Glühen, Staub, Abgänge) hängen nur an der Spielzeit `round.now`: in Pause und Einzählen steht alles still. Federn als reine Zeitfunktion (`Spring`).
- Frist-Spiele (Tinte, Schwarm) nutzen dieselben Frist-Bänder wie Echo (`Bands`): Treppe 3 richtig −8 %, Fehler +12 %, Stufe = Band der aktuellen Frist, höchstens eine Spielart hinauf/hinunter je Runde, Aufwärmen −2 aber nicht unter den Beginn der Spielart.
- Haptik: Tipp/Wisch `press()` (Rahmen), Kettenaufstieg `tick()`, Spiel-Ereignis (`Play.cheers`, z. B. perfekter Durchgang) `tick()`, Rundenende `confirm()`. Fehler nur optisch.
- Gemeinsame Bausteine in `ui/GameParts.kt`: Einzähl-Overlay, Ja/Nein-Taste, Fristlinie.

## Sternbild
- Raster aus dunklen Punkten (4×4 bis 7×7) auf Nachthimmel mit langsam ziehendem Staub. N Sterne (Stufe + 2) leuchten nach 0,5 s auf, stehen 1,2 s (−20 ms je Stufe, min. 0,95 s, Gemächlich ×1,5) und verlöschen. Dann aus dem Gedächtnis antippen (löst beim Aufsetzen aus).
- Treffer: Stern mit Ring. Fehler: Bernstein-X. Zwei Fehler beenden den Durchgang, verpasste Sterne erscheinen gestrichelt. Perfekt: Sternbild-Linien (kürzester Baum) wachsen nacheinander mit Feder, `cheers` +1.
- Treppe je Durchgang: perfekt +1, ein Fehler gleich, zwei −1 (Stufe 1–14). Raster: ≤4 Sterne 4×4, ≤7 5×5, ≤11 6×6, sonst 7×7 (höchstens ein Drittel leuchtet).
- Punkte: 30 je Stern × Multiplikator, perfekt +10 je Stern × Multiplikator. Treffer % = richtige Tipps / alle Tipps.
- Oben: „Merken" bzw. „gefunden / gesamt", rechts zwei Leben als Rauten.
- Raster-Drehung ab Stufe 12: bewusst noch nicht (Plan: erst nach Review).

## Tinte
- Zwei Glas-Karten: links „Bedeutung" (Wort in neutraler Schrift), rechts „Schriftfarbe" (Wort in Farbe). Frage: passt die Bedeutung links zur Schriftfarbe rechts? Ja/Nein-Tasten wie Echo.
- Farben: Blau, Orange, Gelb, Lila (sicher), ab Stufe 6 dazu Rot und Grün. Ab Stufe 10 ist auch das linke Wort farbig (Ablenkung, 70 % in fremder Farbe). Anteil widersprüchlicher Karten rechts 30 % + 5 % je Stufe bis 75 %; bei „Nein"-Karten oft der Köder „Wort rechts = Bedeutung links".
- Farbenblind-Modus (Einstellung, Standard aus): nur die vier sicheren Farben. Test: paarweise ΔE ≥ 22 unter Protanopie und Deuteranopie (Machado 2009), ohne Sehschwäche alle sechs ≥ 25.
- Kartenfolge: Anteil „passt" 40–60 %, nie mehr als 4 gleiche Antworten in Folge, nie zweimal dieselbe Karte.
- Frist-Bänder: 1–5 ab 2600 ms, 6–9 ab 2900 ms, 10–16 ab 3100 ms, je Stufe ×0,9. Nach jeder Antwort 140 ms Wechsel.
- Punkte: 50 × Multiplikator.
- Optik: alte Karte gleitet nach oben (richtig) bzw. wackelt bernstein nach unten (falsch), neue klappt mit Feder herein. Wörter per Auto-Size einzeilig, auch bei 200 % Schrift. Farbwörter als Ressourcen (DE/EN, Test).
- Info sagt ehrlich: bei starker Farbsehschwäche nur eingeschränkt spielbar.

## Schwarm
- Ein Schwarm Kometen (Pfeilkopf mit Lichtschweif) in wechselnder Formation (Linie, Kreuz, V, Raute) an wechselnder Stelle. Der Mittlere ist größer und trägt einen Ring. Wisch in seine Zeigerichtung (löst aus, sobald der Finger 28 dp gezogen hat).
- Ab Stufe 10 fliegt der Schwarm langsam (0,55 Abstände/s). Ist der Mittlere hohl (40 %), zählt die Flugrichtung statt der Zeigerichtung; meist weichen beide ab.
- Größe 5 (1–3), 7 (4–7), 9 (ab 8). Anteil gemischter Schwärme 30 % + 5 % je Stufe bis 70 %. Zittern wächst mit der Stufe.
- Antworten gleichverteilt, höchstens 3 gleiche in Folge, nie zweimal dieselbe Formation oder fast dieselbe Stelle.
- Frist-Bänder: 1–9 ab 2600 ms, 10–16 ab 3200 ms, je Stufe ×0,9. Nach jeder Antwort 220 ms Wechsel.
- Punkte: 40 × Multiplikator.
- Optik: richtig zieht der Schwarm in Wischrichtung mit langen Schweifen ab, falsch wackelt er bernstein und verblasst. Keine Vögel, nur abstrakte Kometen. TalkBack-Aktionen für die vier Richtungen.

## Bande
- Feld 5×5 bis 8×8 aus dunklem Glas, ringsum Randlaschen (Pfeile nach außen). Glas-Prismen („/" oder „\") leuchten nach 0,45 s auf, stehen 3 s (−100 ms je Stufe, min. 1,8 s, Gemächlich ×1,5) und verschwinden. Dann sitzt eine Lichtkugel an einem Randfeld; Tipp auf die Lasche, an der das Licht austritt (löst beim Aufsetzen aus; knapp im Feld zählt die nächste Kante, Ecken nicht).
- Danach läuft die Kugel sichtbar (9 Felder/s), getroffene Prismen leuchten beim Abprall auf (`steps` → `step()`), der Weg gestrichelt in Akzent. Gewertet wird, wenn das Licht draußen ist (endet die Zeit vorher, trotzdem). Richtig: Weg leuchtet, Lasche pulst. Falsch: Bernstein-X an der getippten Lasche, alle Prismen gedimmt sichtbar.
- Stufe 1–14: Raster 5 (1–3), 6 (4–7), 7 (8–11), 8 (12–14); Prismen 2 + (Stufe−1)·2/3; der Generator verlangt, dass das Licht mindestens min(Prismen, 1 + Stufe/3) verschiedene trifft. Treppe: richtig +1, falsch gleich, zwei falsch in Folge −1.
- Punkte: 20 je getroffenem Prisma × Multiplikator. Farbfilter-Prismen (Stufe 12+) bewusst nicht (Plan: erst nach Review).

## Tropfen
- Rechentropfen (Glas, Rechnung in Space Grotesk mit tnum) fallen in ein Becken. Ziffernfeld 3×4 (1–9, Löschen, 0, Eingabe), Tasten ≥ 52 dp, lösen beim Aufsetzen aus. Ergebnis löst automatisch aus, sobald es eindeutig ist (kein längeres Ergebnis beginnt so), sonst mit Eingabe. Falsche Eingabe: Fehler, Anzeige wackelt bernstein.
- Gleiche Ergebnisse platzen gemeinsam (+10 je weiterem Tropfen), goldener Leuchttropfen (ab Stufe 3, 6 %) räumt den Himmel ab. Jeder Aufschlag hebt das Wasser (Feder, Schwappen) und zählt als Fehler; beim dritten ist das Becken voll und die Runde aus. Keine Uhr, keine Zeitleiste (Sicherheitsnetz 15 min).
- Stufe 1–14: + bis 10 → + bis 20 → +/− → bis 50 → × (≤5) → × (≤10) → ÷ → bis 100 → zweistellig × einstellig (ab 10) → bis 200 (ab 12). Fallzeit 9 s × 0,94 je Stufe, gleichzeitig 2 / 3 (ab 4) / 4 (ab 8) / 5 (ab 12). In der Runde: 4 gelöste Tropfen → Stufe +1 (auf 14: 4 % schneller), Aufschlag → −1. Gemächlich: Fallen und Takt ×1,5.
- Punkte: 10 (klein) / 15 (größer) / 20 (×, ÷) / 30 (zweistellig ×) × Multiplikator. Jeder geplatzte Tropfen ist ein Treffer (`tick()`).
- Tropfen-Text wächst mit der Systemschrift bis 160 %, darüber wird der Tropfen größer; breite Rechnungen werden kleiner statt umzubrechen. Ereignisse laufen streng nach Zeit (Ergebnis unabhängig von der Bildrate).

## Rundblick
- Sternenfeld mit 1–3 Positionsringen (8 Stellen je Ring, Ring 2 versetzt; Radien am kürzeren Maß). Fixierkreuz 0,6–0,95 s, dann blitzen gleichzeitig in der Mitte Kreis oder Raute und am Ring ein Stern auf, danach 0,3 s Rauschmaske an Mitte und allen Stellen. Antwort: Form über zwei Tasten (Kreis/Raute), Stelle per Tipp, beliebige Reihenfolge, je einmal. Nur beides richtig zählt.
- Anzeigezeit in Frames (60 Hz): Leiter 30, 26, 22, 19, 16, 14, 12, 10, 9, 8, 7, 6, 5, 4, 3. Zwei richtig in Folge eine Sprosse kürzer, falsch eine länger. Die Maske kommt im ersten Frame, in dem die Zielzeit bis auf einen halben Frame erreicht ist (gemessene Frame-Dauer); bei 120 Hz also doppelt so viele Frames. Weicht die gemessene Anzeigedauer um mehr als ¾ Frame ab, zählt die Aufgabe nicht („Blitz war ungenau"). Ebenso, wenn mitten im Blitz pausiert wurde (erster Frame danach ohne Zeitfortschritt). Tipps während des Blitzes werden ignoriert.
- Stufe = Band der Anzeigezeit (500 ms × 0,72 je Stufe): 1 Ring (1–6), 2 Ringe (7–12), 3 Ringe mit Ködern (vierzackige Funken, 1–3) ab 13, max. 18; höchstens eine Spielart je Runde hinauf. Gemächlich: Frames ×1,5, mindestens 9 (150 ms).
- Punkte: 60 × Multiplikator + 15 je Ring nach außen. Auflösung: Treffer pulst und zieht eine Linie zur Mitte; sonst Bernstein-X und das echte Ziel gestrichelt.

## Slalom
- Gleiter (Deltaflügel in Akzent, Leuchtschweif) in einer Kristallschlucht bei Nacht: Pseudo-3D aus flachen Ebenen (Fluchtpunkt, Spurstriche und Querlinien wandern mit der Welt, gezackte Kristallwände, Mond). Hindernisse: Kristallsäulen (kühles Blau) und Felsbrocken, ab Stufe 6 rutschen manche seitlich in ihre Spur. Keine Autos, keine Wüste.
- Tipp auf linke/rechte Hälfte wechselt die Spur (160 ms, Feder-Neigung); gezählt wird ab der Hälfte des Wechsels. Zusammenstoß: Leben −1 (3), Tempo ×0,9, `confirm()`, Bernstein-Ring, kurzer Rückstoß, 0,9 s Schonzeit. Nach dem dritten ist die Runde aus, sonst nach 60 s.
- Tempo 14 Einheiten/s × 1,06 je Stufe; je 10 s ohne Treffer ×1,06. Spuren 3 (1–4), 4 (5–9), 5 (10–14). Reihen: Abstand ≥ Tempo × (0,62 s + 0,34 s je Spur Versatz) zur garantiert freien Spur; mehr gesperrte Spuren mit der Stufe, enge Tore ab 10. Stufe nach der Runde = Tempo-Band, je Runde höchstens +3 bzw. −2. Gemächlich: Tempo ⅔, gleich bewertet.
- Punkte: je sauber passierter Reihe 10 × Tempo-Faktor × Multiplikator; knapp ausgewichen (gesperrte Nachbarspur, Wechsel in den letzten 0,7 s) +15 und `tick()`. Ereignisse (Durchfahrten, Tempostufen, neue Reihen) laufen streng nach Zeit.

## Screens
1. **Heute**: Datum, oben Spiele und Zahnrad. Hero: Tagesbild aus bis zu 3 Lichtkugeln (Spielsymbol, Name, Stufe), verbunden durch Linien, erledigt = Akzent mit Haken. Darunter Serie und Woche Mo–So. Pille „Start" / „Weiter" / „Fertig für heute".
2. **Spiele**: alle Spiele mit Stufe und Bestwert, Tipp startet.
3. **Spiel**: Zeitleiste, Punkte, Multiplikator mit 4 Punkten, Pause.
4. **Ergebnis**: Punkte groß, Stufe mit Pfeil, Treffer in %, Bestwert, „Neuer Bestwert" mit einmaligem Glanz, „Tagestraining erledigt". Pille „Weiter" (nächstes Tagesspiel) bzw. „Nochmal", Text-Knopf „Fertig".
5. **Einstellungen**: Gemächlich, Farbenblind-Modus, Haptik, Töne, Stufe zurücksetzen je Spiel (zweimal tippen), Info.
6. **Info**: wie alle aapps, plus Spielregeln, Tagestraining, Hinweis zu TalkBack, Gesundheits-Satz.

Verlauf-Screen kommt in einer späteren Phase. Tagestraining und Listen enthalten alle neun Spiele (Wochenrotation: jedes ≥ 2× je Woche, keines an zwei Tagen in Folge).

## Akzent und Icon
- Akzent Orchidee `#F27BE8`.
- Icon: drei Punkte als Mini-Sternbild, zwei Linien, der dritte Punkt größer und leuchtend.

## Screenshot-Zustände (`apps/thinkr/screenshots/`)
| Name | Inhalt |
|---|---|
| `home` | Tagesbild mit den drei echten Tagesspielen, nichts erledigt, Serie 4 |
| `home_partly` | eines erledigt (`home_partly_land`: zwei) |
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

Sternbild: `stars_show` (Sterne leuchten), `stars_recall` (2 gefunden, 1 Fehler), `stars_perfect` (Sternbild-Linien), `stars_failed` (zwei Fehler, verpasste gestrichelt), `stars_big` (7×7), `stars_countdown`, `stars_paused`; Varianten `stars_show_land`, `stars_show_360dp`, `stars_recall_de`, `stars_recall_font200`, `stars_countdown_de_font200`.

Tinte: `ink_play` (4 Farben), `ink_wrong` (falsch, alte Karte wackelt hinaus), `ink_tinted` (Stufe 12, linkes Wort farbig), `ink_colorblind` (Stufe 12 im Farbenblind-Modus), `ink_countdown`, `ink_paused`; Varianten `ink_play_de`, `_land`, `_font200`, `_360dp`, `ink_tinted_de`, `ink_countdown_de_font200`.

Schwarm: `swarm_play` (7 Kometen, gemischt), `swarm_hollow` (Stufe 12, hohl, fliegt), `swarm_leaving` (richtig gewischt, zieht ab), `swarm_countdown`, `swarm_paused`; Varianten `swarm_play_de`, `_land`, `_360dp` (9 Kometen), `swarm_hollow_land`, `swarm_countdown_hollow_de_font200`.

Bande: `bounce_show`, `bounce_answer`, `bounce_solve` (Licht unterwegs), `bounce_wrong`, `bounce_big` (8×8), `bounce_countdown`, `bounce_paused`; Varianten `bounce_show_land`, `bounce_show_360dp`, `bounce_answer_de`, `bounce_countdown_de_font200`.

Tropfen: `drops_play`, `drops_busy` (Stufe 12, zwei Aufschläge), `drops_countdown`, `drops_paused`; Varianten `drops_play_de`, `_land`, `_font200`, `_360dp`, `drops_countdown_de_font200`.

Rundblick: `glance_fix`, `glance_flash`, `glance_flash_decoys`, `glance_mask`, `glance_answer`, `glance_solved`, `glance_missed`, `glance_countdown`, `glance_paused`; Varianten `glance_flash_land`, `glance_answer_de`, `_360dp`, `_font200`, `glance_countdown_de_font200`.

Slalom: `slalom_play` (Neigung im Wechsel), `slalom_fast` (Stufe 12, 5 Spuren), `slalom_crash`, `slalom_countdown`, `slalom_paused`; Varianten `slalom_play_de`, `_land`, `_360dp`, `slalom_countdown_de_font200`.

## Abnahme
- Domain ohne Android-Imports; Unit-Tests für Kette, Treppe, Frist-Bänder, Runde, Echo (Kartenfolge, Frist, Ablauf, Wechsel-Sperre, Punkte, Stufen), Sternbild (Muster, Linien-Baum, Phasen, Treppe, Punkte), Tinte (Kartenfolge, Anteile, Farbenblind-Palette, Frist), Schwarm (Formationen, Verteilung, Hohl-Regel, Frist), Bande (Strahlverfolgung, Umkehrbarkeit, Mindesttreffer, Treppe, Wertung erst nach dem Lauf), Tropfen (Rechnungen ganzzahlig, Eindeutigkeit, Ketten, Leuchttropfen, Becken, Bildraten-Unabhängigkeit), Rundblick (Frames bei 60/120 Hz, ungültige Blitze, Phasen, Treppe, Ringe/Köder), Slalom (freie Spur erreichbar, perfekter Fahrer ohne Treffer, Tempo-Treppe, Leben, Bildraten-Unabhängigkeit), Tagestraining, Serie, Fortschritt und Codec (inkl. kaputter Daten). Farbwörter DE/EN per Robolectric.
- Pause bei `ON_STOP` (Robolectric-Test).
- `:apps:thinkr:check`, `assembleDebug`, `verifyRoborazziDebug` grün. Manifest nur mit VIBRATE.
