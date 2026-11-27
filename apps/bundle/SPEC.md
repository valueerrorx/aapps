# xapient bundle – Spezifikation

## Zweck
Schlüssel-App: Bezahl-App in Google Play (Preis in der Console, kein In-App-Kauf). Ist sie aus Google Play installiert und mit dem Play-Schlüssel signiert, schalten alle xapient-Apps frei (`core/billing/BundleKey`, live bei jedem Start, kein Cache).

## Funktion
- Übersicht aller xapient-Apps (`Catalog`, Liste = `include(":apps:…")` in `settings.gradle.kts`, Test prüft das): Icon, Name, Einzeiler DE/EN, Status, Aktion.
- Gruppen „Installiert" (Aktion Öffnen) und „Weitere Apps" (Aktion Installieren = Play-Store-Seite, sonst Browser).
- Status: bezahlte Apps installiert = „Freigeschaltet", nicht installiert = „Enthalten"; moovr/glidr = „Gratis".
- Zähler „x von y installiert", Info-Seite (AboutScreen) mit Hinweis auf Prüfung und Gerätewechsel.
- Status wird bei jedem Fortsetzen neu gelesen (Rückkehr aus Google Play).

## Technik
- Nur `:core:ui`, minSdk 28 (darunter wirkt das Bundle in den Apps nicht). Keine Berechtigungen, BILLING aus core/ui per `tools:node="remove"` entfernt.
- `<queries>` mit allen App-Paketen (Paket-Sichtbarkeit ab Android 11).
- Akzent Champagner `#F3C98B`, Icon: Schlüssel mit Ring aus den App-Farben.

## Screenshots (Roborazzi)
`home`, `home_de`, `home_more`, `home_more_de`, `home_empty`, `home_land`, `home_font200`, `about`, `about_de`, Icons (`icon_masks`, `icon_store_512`).

## Play Console (Thomas)
1. App als PAID anlegen (später nicht änderbar), Preis 9,99 €.
2. Erstes AAB in Internal testing, Play App Signing (Google-Schlüssel).
3. App integrity > App signing key > SHA-256 in `BundleKey.BUNDLE_CERT_SHA256` eintragen, Einzel-Apps neu veröffentlichen.
4. Test: Bundle aus Internal Track, Einzel-App beliebig. Internal app sharing nutzt einen anderen Schlüssel, dort passt der Hash nicht.
