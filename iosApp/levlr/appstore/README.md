# levlr – App-Store-Material

Texte im Layout von fastlane `deliver` (`metadata/<locale>/*.txt`), damit sie später per App Store Connect API hochgeladen werden können.

| Datei | Feld in App Store Connect | Grenze |
|---|---|---|
| `name.txt` | Name | 30 |
| `subtitle.txt` | Subtitle | 30 |
| `description.txt` | Description | 4000 |
| `keywords.txt` | Keywords (Komma, ohne Leerzeichen) | 100 |
| `promotional_text.txt` | Promotional Text | 170 |
| `release_notes.txt` | What's New in This Version (erst ab dem 1. Update) | 4000 |
| `support_url.txt`, `privacy_url.txt`, `marketing_url.txt` | Support URL, Privacy Policy URL, Marketing URL | – |
| `review_information/notes.txt` | App Review Information → Notes | 4000 |
| `primary_category.txt` | Category: Utilities | – |

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| levlr Unlock | `solutions.xapient.levlr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock levlr / levlr freischalten |
| levlr 1-Day Trial | `solutions.xapient.levlr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Product IDs mit Bundle-ID-Präfix: Apple lässt eine Product ID nach dem Einreichen nicht mehr für eine andere App zu, `full_unlock` allein ginge also nur in einer App. Der Code leitet die IDs aus der Bundle-ID ab (`iosApp/common/StoreKitBridge.swift`). Der Reference Name der Testphase folgt Apples Vorgabe „XX-day Trial“ (Guideline 3.1.1).

Beschreibungen für die IAPs (max. 55 Zeichen): wie in `levlr.storekit`. Für die Prüfung braucht jeder IAP einen Review-Screenshot: Angebots-Bildschirm (Szene `OFFER`) bzw. Dankes-Bildschirm (`THANKS`).

## App Privacy (App Store Connect → App Privacy)
- „Do you or your third-party partners collect data from this app?“ → **No, we do not collect data from this app**. Ergebnis: „Data Not Collected“.
- Privacy Policy URL: `https://valueerrorx.github.io/aapps/privacy/`
- Begründung: keine Netzwerkzugriffe, keine SDKs, Einstellungen und Kauf-Cache nur lokal (UserDefaults). Der Kauf läuft über Apple, das zählt nicht als Datenerhebung durch die App.

## Age Rating (Fragebogen in App Store Connect)
Alle Inhaltsfragen (Gewalt, Sexualität, Glücksspiel, Drogen, Horror, Schimpfwörter, medizinische Inhalte usw.) → **None** bzw. **No**. Weitere Fragen: Unrestricted Web Access **No**, User-Generated Content **No**, Messaging/Chat **No**, Advertising **No**, Contests **No**, Loot boxes **No**, In-App Purchases **Yes**. Erwartetes Ergebnis: **4+**. (Die genauen Fragen ändern sich bei Apple gelegentlich; immer die „keine/nein“-Antwort wählen, außer bei In-App-Käufen.)

## Screenshots
- Pflichtgrößen: iPhone 6.9" (1320 × 2868) und iPad 13" (2064 × 2752), je 1–10 Bilder pro Sprache.
- Zustände: `EDGE_TILTED`, `EDGE_LEVEL`, `FLAT_TILTED`, `HELD`, `CALIBRATE`, `SETTINGS`, `OFFER`, `THANKS` (Startargument `-aappsScene <NAME>`, siehe `apps/levlr/shared/src/iosMain/.../Scenes.kt`).
- Der UI-Test `LevlrUITests/ScreenshotTests` startet alle Zustände in EN und DE und hängt die Bilder an das Testergebnis (`.xcresult`). Im CI bleiben sie auf dem Runner (öffentliches Repo, keine Artefakte); Die Review-Screenshots der IAPs (`OFFER` für die Testphase, `THANKS` für die Freischaltung) lädt die CI direkt per API hoch (`iosApp/tools/asc-iap-screenshot.mjs`, nur wenn noch keiner da ist). Die App-Screenshots (6.9" `APP_IPHONE_67` und 13" `APP_IPAD_PRO_3GEN_129`, EN + DE, Szenen wie im Play-Listing ohne Querformat) macht die CI per `iosApp/tools/sim-shots.sh` und lädt sie mit `asc-screenshots.mjs` hoch; ein Satz wird nur ersetzt, wenn sich Bilder geändert haben.
- Von Hand am Mac: `xcrun simctl boot "<iPhone … Pro Max>"`, App installieren, dann `xcrun simctl launch booted solutions.xapient.levlr -aappsScene EDGE_TILTED -AppleLanguages "(de)"` und `xcrun simctl io booted screenshot edge_tilted_de.png`.
