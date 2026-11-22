# hudr – SPEC

## Aufgabe
GPS-Tacho: App öffnen, Geschwindigkeit riesig ablesen. Fürs Rad, Auto, Boot, Laufen. HUD-Modus spiegelt die Zahl für die Windschutzscheibe. Kernversprechen: hudr hat keine INTERNET-Berechtigung und kann den Standort nachweislich nicht senden. Ein Richtwert, kein geeichtes Messgerät.

## Muss
- Standort über `LocationManager.GPS_PROVIDER` (keine Play Services), 2 Updates/s erbeten, Zeit aus `elapsedRealtimeNanos`.
- Geschwindigkeit aus `Location.speed` (Doppler). Fehlt sie (`hasSpeed` false): Fallback aus Positionsdifferenzen über ≥ 2 s Basis (älteste Position der letzten 5 s); Strecke innerhalb des Rauschens (mittlere Genauigkeit) zählt als Stillstand.
- Glättung: exponentiell, Zeitkonstante 0,8 s, pro Fix nach Zeitabstand.
- Stillstand: Anzeige 0, kein Zittern bei 0–3 km/h. Rauschgrenze = max(0,7 m/s, Speed-Genauigkeit bis 2 m/s). Stillstand ab zwei Fixes unter der Grenze, Fahrt erst ab zwei Fixes ≥ 1,1 m/s (≈ 4 km/h) und über der Grenze.
- Einheiten km/h, mph, kn; Standard nach Land der Locale (US, GB, LR, MM → mph, sonst km/h). Strecke in km, mi bzw. sm.
- GPS-Status: „Suche GPS“ bis zum ersten Fix, „Signal verloren“ nach 5 s ohne Fix (Zahl gedimmt als „–“), sonst ±Genauigkeit in m und genutzte/sichtbare Satelliten (`GnssStatus`). GPS im System aus: Hinweis + „Standort einschalten“.
- Fahrt: Max (geglättet, nur in Fahrt), Ø (Strecke / Fahrzeit), Strecke. Strecke = Trapez aus Geschwindigkeit × Zeit in Fahrt (Lücken ≤ 3 s), bei längerer Lücke Luftlinie, wenn beide Fixes ≤ 30 m genau. Zurücksetzen per Knopf.
- Warnschwelle (optional, aus): Wert in der gewählten Einheit, 5er-Schritte 10–300. Bogen und Zahl: Akzent → Bernstein ab 90 % → Rot über der Schwelle (angezeigte Zahl > Schwelle). Hysterese 2 Einheiten (Rot) bzw. 1 Einheit (Bernstein). Optional ein Ton beim Überschreiten (`ToneGenerator`, keine Berechtigung), erneut erst nach Rückkehr unter die Hysterese.
- Skala des Bogens: kleinste aus 40/80/160/240 km/h (30/60/120/180 mph, 20/40/80/120 kn), die Max der Fahrt und Schwelle fasst; wächst nur, Zurücksetzen fängt neu an.
- Ziffern ändern sich nur mit neuem Fix; der Bogen folgt per kritisch gedämpfter Feder und schläft danach ein (keine Bilder im Stillstand der Anzeige).
- HUD-Modus (Hauptaktion): reines Schwarz, Zahl in HUD-Grün (über Schwelle Rot), horizontal gespiegelt (abschaltbar), Systemleisten ausgeblendet, Bildschirm bleibt an, optional maximale Helligkeit. Tippen zeigt 3 s lang „HUD beenden“; Zurück beendet ebenfalls. Zahlgröße fest aus „188“, damit nichts springt.
- Standort nur im Vordergrund: ab ON_STOP aus, ab ON_START wieder an. Kein Dienst, kein Hintergrund-Standort, keine Routen-Speicherung (nur die drei Fahrtwerte).
- Rechte: `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` zusammen erfragt (Android 12+ verlangt beide). Beim ersten Start einmal automatisch. Weggetippt: weiter fragbar. Nur ungefähr erteilt: Hinweis „Genauer Standort nötig“ und erneut fragen (System bietet Upgrade an). Erst nach belegter Ablehnung (Begründung war schon verlangt und wird nicht mehr) Weg in die App-Einstellungen.
- Zustand übersteht Drehung und Prozess-Tod (Fahrtwerte, Skala, HUD per SavedState; Einstellungen per SharedPreferences).
- Hinweis auf Hauptschirm, in den Einstellungen und auf der Info-Seite: Anzeige kann abweichen, nicht für verkehrsrechtliche Zwecke, nicht während der Fahrt bedienen.
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
1. **Tacho**: oben GPS-Status und Zahnrad; Hero = 270°-Bogen mit Skala, Schwellenmarke und leuchtendem Bogen, innen riesige Zahl und Einheit. Darunter Max · Ø · Strecke, Hinweiszeile, unten Zurücksetzen und „HUD“. Querformat: Werte links, Bogen rechts.
2. **Standort fehlt** (fragen, ungefähr, gesperrt, GPS aus): leerer kleinerer Bogen mit Pin, Erklärung und Aktion als Gruppe.
3. **HUD**: schwarz, gespiegelt, nur Zahl + Einheit (+ Schwellen-Rot).
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
| `hud`, `hud_over`, `hud_unmirrored` | HUD gespiegelt, über Schwelle, ungespiegelt |
| `loc_ask`, `loc_coarse`, `loc_blocked`, `gps_off` | Standort fehlt |
| `settings`, `about` | |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard Pixel 7, en): `_de`, `_land`, `_font200`, `_land_font200` für `drive`, `loc_ask`, `settings`; dazu `drive_360dp`, `hud_land`, `about_de`, `about_font200`.

## Abnahmekriterien
- Domain ohne Android-Imports, Unit-Tests für Glättung, Stillstand (kein Zittern), Einheiten/Locale, Strecke/Ø/Max/Reset, Lückenbrücke, Fallback aus Positionen, Warnschwelle mit Hysterese und Ton-Auslösung, GPS-Status/Veralten, Skala, Feder, Sichern/Wiederherstellen, Kontrast (WCAG AA gemessen, auch HUD).
- Gesten (Robolectric): HUD an/aus, Zurücksetzen, Standort-Aktion je Zustand, Schwellen-Schritte, Rechte-Fluss (weggetippt, einmal abgelehnt, gesperrt, nur ungefähr). Ohne neuen Fix keine Frames.
- `:apps:hudr:check`, `verifyRoborazziDebug`, `assembleRelease` grün; nur die zwei Standort-Rechte; Release-APK < 4 MB.
