# hudr – SPEC

## Aufgabe
GPS-Tacho: App öffnen, Geschwindigkeit riesig ablesen. Fürs Rad, Auto, Boot, Laufen. HUD-Modus spiegelt die Zahl für die Windschutzscheibe. Kernversprechen: hudr hat keine INTERNET-Berechtigung und kann den Standort nachweislich nicht senden. Ein Richtwert, kein geeichtes Messgerät.

## Farbe
Akzent Eis-Cyan `#2EE6E6` (Tacho, Icon), HUD-Ziffern `#3DFFFF` auf Schwarz; Bernstein/Rot nur als Warnfarben. Eigene Farbe, von tunr-Grün und nordr-Hellblau abgesetzt.

## Muss
- Standort über `LocationManager.GPS_PROVIDER` (keine Play Services), 2 Updates/s erbeten, Zeit aus `elapsedRealtimeNanos`.
- Geschwindigkeit aus `Location.speed` (Doppler). Fehlt sie (`hasSpeed` false): Fallback aus Positionsdifferenzen über ≥ 2 s Basis (älteste Position der letzten 5 s); Strecke innerhalb des Rauschens (mittlere Genauigkeit) zählt als Stillstand.
- Glättung: exponentiell, Zeitkonstante 0,8 s, pro Fix nach Zeitabstand.
- Stillstand: Anzeige 0, kein Zittern bei 0–3 km/h. Rauschgrenze = max(0,7 m/s, Speed-Genauigkeit bis 2 m/s). Stillstand ab zwei Fixes unter der Grenze. Fahrt erst, wenn ≥ 1,1 m/s (≈ 4 km/h) über der Grenze ≥ 2 s anhält, die Positionsgenauigkeit ≤ 25 m ist und die Position seit dem Halt um ≥ max(Genauigkeit, 5 m) gewandert ist (gegen Mehrwege-Echos im Stand).
- Ausreißer: Sprung > 10 m/s² zum letzten Wert wird verworfen (nach drei in Folge gilt er). Fixes mit Speed-Genauigkeit > 2 m/s zählen nicht für Max.
- Trend: Beschleunigung aus dem geglätteten Wert (Zeitkonstante 1 s), Vorhersage = v + 3 s × a.
- Einheiten km/h, mph, kn; Standard nach Land der Locale (US, GB, LR, MM → mph, sonst km/h). Strecke in km, mi bzw. sm.
- GPS-Status: „Suche GPS“ bis zum ersten Fix, „Signal verloren“ nach 5 s ohne Fix (Zahl gedimmt als „–“), sonst ±Genauigkeit in m und genutzte/sichtbare Satelliten (`GnssStatus`). GPS im System aus: Hinweis + „Standort einschalten“.
- Fahrt: Max (geglättet, nur in Fahrt, nur was ≥ 2 s anlag = Minimum über ein 2-s-Fenster), Ø (Strecke / Fahrzeit), Strecke. Strecke = Trapez aus Geschwindigkeit × Zeit in Fahrt inkl. letztem Stück bis 0 (Lücken ≤ 3 s), bei längerer Lücke Luftlinie, wenn beide Fixes ≤ 30 m genau. Zurücksetzen per Knopf.
- Warnschwelle (optional, aus): Wert in der gewählten Einheit, 5er-Schritte 10–300. Zahl: Akzent → Bernstein ab angezeigt 90 % (gerundet, ab 0,9 × Schwelle − 0,5) → Rot über der Schwelle (angezeigte Zahl > Schwelle). Am Band: Bernstein-Streifen 90–100 %, Rot darüber. Hysterese 2 Einheiten (Rot) bzw. 1 Einheit (Bernstein). Optional ein Ton beim Überschreiten (`ToneGenerator`, keine Berechtigung), erneut erst nach Rückkehr unter die Hysterese.
- Geschwindigkeitsband (Speed-Tape wie im Flugzeug-HUD): zeigt 60 Einheiten um den Wert, läuft unter einer festen Zeiger-Box; Striche je 5, Zahlen je 10 (bei Platzmangel je 20). Max als kleines Dreieck, Trend als Pfeil bis zur 3-s-Vorhersage. Enden blenden aus. Ohne Wert gleitet es gedimmt auf 0.
- Ziffern ändern sich nur mit neuem Fix; das Band folgt per kritisch gedämpfter Feder und schläft danach ein (keine Bilder im Stillstand der Anzeige).
- HUD-Modus (Hauptaktion): reines Schwarz, gleiches Layout wie der Tacho, reduziert auf Ziffern + Band in HUD-Cyan (Bernstein/Rot an der Schwelle), horizontal gespiegelt (abschaltbar), Systemleisten ausgeblendet, Bildschirm bleibt an, optional maximale Helligkeit. Ausrichtung gesperrt (`SCREEN_ORIENTATION_LOCKED`, flach auf dem Armaturenbrett kein Kippen), beim Verlassen wiederhergestellt. Tippen zeigt 3 s lang „HUD beenden“ (TalkBack: „Beenden-Knopf zeigen“); Zurück beendet ebenfalls.
- Ziffern: eigene Strichschrift (HUD-Stil, „1“ mit Fuß, nie wie „l“), rechtsbündig in drei festen Zellen, Größe aus der verfügbaren Fläche (Hochformat: Höhe). Ohne Wert „– – –“.
- Standort nur im Vordergrund: ab ON_STOP aus, ab ON_START wieder an. Von ON_START bis ON_STOP hört ein Receiver auf `PROVIDERS_CHANGED_ACTION`: Standort im System wieder an = Messung startet sofort. Kein Dienst, kein Hintergrund-Standort, keine Routen-Speicherung (nur die drei Fahrtwerte).
- Rechte: `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` zusammen erfragt (Android 12+ verlangt beide). Beim ersten Start einmal automatisch. Weggetippt: weiter fragbar. Nur ungefähr erteilt: Hinweis „Genauer Standort nötig“ und erneut fragen (System bietet Upgrade an); bleibt so, auch wenn früher eine Begründung verlangt wurde. Gesperrt erst, wenn die Upgrade-Anfrage ohne Dialog sofort (< 300 ms) zurückkommt. Erst nach belegter Ablehnung (Begründung war schon verlangt und wird nicht mehr) Weg in die App-Einstellungen.
- Zustand übersteht Drehung und Prozess-Tod (Fahrtwerte, HUD per SavedState; Einstellungen per SharedPreferences).
- Hinweis auf Hauptschirm, in den Einstellungen und auf der Info-Seite: Anzeige kann abweichen, nicht für verkehrsrechtliche Zwecke, nicht während der Fahrt bedienen.
- TalkBack: Ziffern als Live-Region, angesagt nur bei Wechsel von Warnzustand, Zehnerstelle oder GPS-Phase. GPS-Satellitenzahl löst nur bei Änderung ein neues Bild aus.
- Hoch- und Querformat, Englisch und Deutsch, lesbar bei 200 % Schrift.

## Berechtigungen
| Berechtigung | Grund |
|---|---|
| `ACCESS_FINE_LOCATION` | GPS-Geschwindigkeit. Nur bei Nutzung der App, nichts wird gespeichert oder gesendet. |
| `ACCESS_COARSE_LOCATION` | Android 12+ verlangt sie zusammen mit FINE. |

Kein INTERNET, kein Hintergrund-Standort, kein Dienst. `android.hardware.location.gps` nicht Pflicht.

## Nicht
- Keine Karte, keine Routen, kein Export, keine Blitzer-/Limitdaten, kein Höhenmesser, kein Kompass, keine Benachrichtigung.

## Screens
1. **Tacho**: kein Ring, keine Glaskarte. Hochformat: oben GPS-Status und Zahnrad, riesige Ziffern (füllen den Platz), Einheit darunter, waagrechtes Band, Max · Ø · Strecke in vier Eck-Klammern (Reticle), Hinweiszeile, unten Zurücksetzen und „HUD“. Querformat: links ~60 % Ziffern mit Werten als schmaler Zeile darunter, rechts senkrechtes Band.
2. **Standort fehlt** (fragen, ungefähr, gesperrt, GPS aus): Pin im Reticle, Erklärung und Aktion als Gruppe.
3. **HUD**: schwarz, gespiegelt, Ziffern + Band wie der Tacho, ohne Werte und Knöpfe.
4. **Einstellungen**: Einheit, Warnschwelle (an/aus, Wert, Ton), Bildschirm an, HUD spiegeln, HUD max. Helligkeit, Hinweis, Info.
5. **Info**: wie alle aapps.

## Screenshot-Zustände (Roborazzi, `apps/hudr/screenshots/`)
| Name | Inhalt |
|---|---|
| `drive` | 87 km/h, Max 112, Ø 64, 23,4 km, ±4 m, 11/24 Sat |
| `near` | 47 km/h bei Schwelle 50 (Bernstein) |
| `over` | 58 km/h bei Schwelle 50 (Rot) |
| `stopped` | 0 km/h, Fahrtwerte stehen |
| `searching` | Suche GPS, noch kein Fix |
| `lost` | Signal verloren |
| `mph` | 54 mph |
| `hud`, `hud_over`, `hud_unmirrored`, `hud_searching` | HUD gespiegelt, über Schwelle, ungespiegelt, ohne Fix |
| `loc_ask`, `loc_coarse`, `loc_blocked`, `gps_off` | Standort fehlt |
| `settings`, `about` | |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard Pixel 7, en): `_de`, `_land`, `_font200`, `_land_font200` für `drive`, `loc_ask`, `settings`; dazu `drive_360dp`, `hud_land`, `hud_land_font200`, `about_de`, `about_font200`.

## Abnahmekriterien
- Domain ohne Android-Imports, Unit-Tests für Glättung, Stillstand (kein Zittern), Einheiten/Locale, Strecke/Ø/Max/Reset, Lückenbrücke, Fallback aus Positionen, Warnschwelle mit Hysterese, Rundung und Ton-Auslösung, Fahrtbeginn (Zeit, Genauigkeit, Verschiebung), Ausreißer, Max-Haltezeit, Trend, GPS-Status/Veralten, Band, Ansage-Schlüssel, Feder, Sichern/Wiederherstellen, Kontrast (WCAG AA gemessen, auch HUD).
- Gesten (Robolectric): HUD an/aus, Zurücksetzen, Standort-Aktion je Zustand, Schwellen-Schritte, Rechte-Fluss (weggetippt, einmal abgelehnt, gesperrt, nur ungefähr auch nach Begründung), GPS-an per Broadcast, HUD-Ausrichtung gesperrt/wiederhergestellt, Tipp-Bezeichnung. Ohne neuen Fix keine Frames.
- `:apps:hudr:check`, `verifyRoborazziDebug`, `assembleRelease` grün; nur die zwei Standort-Rechte; Release-APK < 4 MB.
