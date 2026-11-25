# dividr – SPEC

## Aufgabe
Gruppenausgaben teilen (Törn, Urlaub, WG) ganz ohne Server, Konto oder INTERNET. Die Daten wandern nur per QR-Code von Handy zu Handy.

## Muss
- Gruppe einmal anlegen (Name, eigener Name, Währung), die anderen treten per Scan bei („Wer bist du?": vorhandene Person wählen oder neu). Alle teilen dieselbe Gruppen-ID.
- Personen ≠ Geräte: Gäste ohne Handy möglich. Name max. 24 Zeichen.
- Ausgabe: Betrag, Titel (optional, max. 40), bezahlt von (eine Person), für „Alle" oder feste Auswahl, Datum. Nur gleichmäßige Anteile.
- Zahlung: „Anna zahlte Ben 50 €" als eigener Eintrag (Ausgleich).
- Fremdwährung mit Kurs „1 HUF = 0,00253 €", vorbelegt mit dem zuletzt benutzten Kurs.
- „Für alle" ist dynamisch: zählt alle aktiven Personen. Später aufgenommene Personen zählen standardmäßig von Anfang an mit; Option „ab jetzt" (zählt nur bei danach erstellten „alle"-Ausgaben). Umschaltbar in Personen und im Import.
- Jeder darf alle Einträge ändern und löschen (Löschen zweimal tippen).
- Hauptscreen: eigener Saldo groß, Ausgabenliste nach Tag, unten links der eigene Live-QR (weiße Karte), rechts „Scannen", darüber die Pille „+ Ausgabe". Tipp auf den QR = Vollbild, Code so groß wie möglich, weißer Rand, volle Helligkeit.
- Scan (CameraX 1920×1080 + ZXing TRY_HARDER), bei Notfall-Folge Fortschritt „Teil 2 von 4"; danach Import-Dialog mit Klartext: neue Personen (mit „zählt bei n Ausgaben mit" und Wahl „von Anfang an / ab jetzt"), Namensgleichheit („dieselbe Person?"), neue, geänderte und gelöschte Einträge einzeln. „Übernehmen" / „Abbrechen". Nichts Neues = kurzer Hinweis.
- Nach Übernehmen sofort der eigene Vollbild-QR („Jetzt scannt das andere Handy dich"), damit beide gleich stehen.
- Abrechnung: Saldo je Person als Balken, minimale Überweisungen (exakt bis 14 Personen, darüber Greedy), deterministisch auf allen Geräten. „Erledigt" legt eine Zahlung an. Teilen als Text über Android-Teilen.
- Personen: umbenennen, Gast hinzufügen, mit anderer Person zusammenlegen (und zurück), entfernen nur ohne Bezüge.
- Einstellungen: Gruppenname, Währung (nur ohne Ausgaben), Haptik, Gruppen, neue Gruppe, Gruppe verlassen (zweimal tippen), Info.
- Mehrere Gruppen; die zuletzt offene startet direkt.
- Hoch- und Querformat, Englisch und Deutsch. Berechtigungen: CAMERA (erst beim ersten Scan), VIBRATE aus `core/ui`.

## Nicht
- Keine ungleichen Anteile, keine Kategorien, Fotos, Uhrzeiten, Konten, Signaturen.
- Kein CSV-Export, kein „Anstoßen" mit Frontkamera, kein Code-als-Text (Nachzüge nur auf Wunsch).

## Merge
- Jeder Datensatz (Person, Eintrag, Gruppenkopf) hat eine ID (Gerät 32 bit + laufende Nummer) und einen Stempel (v, Gerät). Merge = Vereinigung, pro ID gewinnt der größere Stempel: zuerst v, dann „nicht gelöscht" vor „gelöscht", dann die größere Geräte-ID. Ändern = v + 1. Keine Uhrzeit.
- Löschen hinterlässt einen Grabstein (ID + Stempel), nie endgültig entfernt.
- Zusammenlegen von Personen über einen Alias (`mergedInto`, immer größere → kleinere ID), Auflösung max. 8 Sprünge, Zyklus → kleinste ID.
- „Ab jetzt" merkt sich den Wissensstand beim Aufnehmen (höchste bekannte Nummer je Gerät). Eine „alle"-Ausgabe zählt die Person nicht, wenn ihre ID davon abgedeckt ist.
- Gruppen zusammenlegen (getrennt angelegt): ID-Mengen vereinigen, nur bei gleicher Währung.

## QR-Format
Binärnachricht (Format 1) → Deflate (raw) → Rahmen `[magic 'D' | Version 1 | CRC32 4 B | Teil | Teile | Daten]` → Base45 → QR alphanumerisch, ECC M, Normalfall genau EIN Code, Version dynamisch bis 40 (Rahmen ≤ 2 258 Byte = 3 387 Zeichen). Base45 statt Byte-Modus: nur ~3 % weniger Kapazität, dafür keine Zeichensatz-Probleme beim Lesen roher Bytes. Nur wenn selbst das nicht reicht: Notfall-Folge mehrerer Codes (4 Bilder/s, max. 64 Teile). Lesegrenzen: 256 KB entpackt, 100 Personen, 5 000 Einträge, Beträge ≤ 10^10, v ≤ 10^6; Verletzung = „Code nicht lesbar". Speicherung am Gerät mit demselben Codec (unkomprimiert, Datei je Gruppe, Temp-Datei + Sicherung).

## Rundung
Fremdwährung zuerst in Gruppen-Cent umrechnen (Nachkommastellen je Währung), dann gleich teilen; Rest-Cent einzeln an die Beteiligten in aufsteigender ID-Reihenfolge. Summe aller Salden = 0.

## Screens
1. **Gruppen** (über den Titel erreichbar, leer = Start mit „Neue Gruppe" und „Code scannen").
2. **Neue Gruppe**: Gruppenname, dein Name, Währung.
3. **Gruppe**: Saldo, Liste, QR + Scannen, „+ Ausgabe".
4. **Eintrag**: Ausgabe | Zahlung, Betrag, Währung/Kurs, Titel, bezahlt von, für, Datum, Löschen.
5. **QR-Vollbild**, 6. **Scannen**, 7. **Import**, 8. **Beitreten**, 9. **Abrechnung**, 10. **Personen**, 11. **Einstellungen**, 12. **Info**.

## Screenshot-Zustände (Roborazzi, `apps/dividr/screenshots/`)
`groups`, `start`, `new_group`, `group`, `group_multi`, `group_empty`, `entry`, `entry_some`, `entry_foreign`, `entry_payment`, `qr_fullscreen`, `scan`, `import_new_person`, `import_update`, `import_same_name`, `join`, `settle`, `people`, `settings`, `about`, `icon_masks`, `icon_store_512`. Varianten `_de`, `_land`, `_font200` für group, entry, import, settle.

## Abnahmekriterien
- Domain ohne Android-Imports. Unit-Tests: Merge-Gesetze (kommutativ, assoziativ, idempotent, Gossip-Simulation), Edge Cases (Löschen vs. Ändern, Grabstein, Alias, Zyklus, Gast → Handy, doppelte Namen, „ab jetzt"), Salden (Summe 0, Rest-Cent, Fremdwährung 0/2 Stellen), Abrechnung (Optimum, Brute Force, Determinismus, Greedy > 14), Codec (Rundlauf, Grenzen, Version, Fuzz), Rahmen/Base45 (RFC 9285, vertauschte/doppelte/fremde Teile, CRC-Wechsel), Import-Vorschau, QR-Rundlauf mit ZXing, Store.
- `:apps:dividr:check`, `verifyRoborazziDebug`, `assembleDebug` grün, Manifest nur CAMERA + VIBRATE.
