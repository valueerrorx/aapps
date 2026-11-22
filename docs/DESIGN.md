# aapps – Design

Gilt für alle Apps. Critic prüft Screenshots dagegen (Gate C).

## Grundsatz
Ein Werkzeug, ein Hero. Das Hero-Element nimmt mindestens 60 % der Fläche ein, alles andere tritt zurück. Kein Material-Standardlook: keine Ripples, keine Cards mit Schatten-Elevation, keine Material-Icons.

## Farben (`core/ui/Theme.kt`)
- BG `#10161B`, SURFACE `#172028`, SURFACE_HIGH `#1E2932`, LINE `#26323C`
- FG `#DCE3E8`, FG_MUTED `#8A97A2`, FG_FAINT `#55626D`
- Pro App genau eine Akzentfarbe (levlr `#C6F24E`). Akzent nur für Zustand („eingerastet", aktiv) und das Hero, nie für Fließtext.
- Warnung: Bernstein `#F2B84E`, sparsam.

## Schrift
- Space Grotesk (Medium/Bold) für Zahlen und Überschriften, Ziffern immer `tnum`.
- Inter (Regular/SemiBold) für Text. Labels in Versalien, SemiBold, Laufweite +0,14 em.
- Große Zahl mit Auto-Size, damit sie bei 200 % Schrift nicht umbricht.

## Licht und Tiefe
- Glow = weicher radialer Verlauf (`Modifier.glow`, `drawGlow`), nie harte Schatten.
- Glas: dunkle Füllung, 1–1,5 dp Kante, ein heller Reflexstreifen oben.
- Eingerastet: Akzent-Kante, stärkerer Halo, einmaliger Glanz.

## Bewegung
- Nur Federn (`Springs` für Compose, eigene Physik als reines Kotlin im Domain), nie linear. Physik-Schleifen schlafen ein, sobald alles ruht.
- Jede Rastung = Haptik (`Haptics.confirm`/`tick`), ohne VIBRATE-Berechtigung.

## Layout
- Edge-to-edge (`AappsScaffold`), Inhalt in den sicheren Bereich.
- Höchstens eine Hauptaktion (Pille unten), Einstellungen hinter dem Zahnrad oben rechts.
- Hoch- und Querformat; im Querformat Text links, Hero rechts.
- Touch-Ziele ≥ 48 dp.

## Icons
Adaptive VectorDrawables: dunkler Hintergrund mit leichtem Verlauf, ein Linien-Symbol in der Akzentfarbe mit dezentem Glow, Monochrom-Ebene. Sichtbarer Bereich = mittlere 72 dp von 108 dp. Gerendert per Roborazzi (`IconRenderTest`).
