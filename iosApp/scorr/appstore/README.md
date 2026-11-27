# scorr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| scorr Unlock | `solutions.xapient.scorr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock scorr / scorr freischalten |
| scorr 1-Day Trial | `solutions.xapient.scorr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `scorr.storekit`.

## Berechtigungen
Keine. Haptik über UIKit (folgt der Systemhaptik), Bildschirm bleibt an über `idleTimerDisabled` (abschaltbar). `UIRequiredDeviceCapabilities`: nur arm64.

## Screenshots
- Szenen: `scenes.txt` (App Store, wie das Play-Listing), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots. Startargument `-aappsScene <NAME>`, siehe `apps/scorr/shared/src/iosMain/.../Scenes.kt`.
