# dividr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| dividr Unlock | `solutions.xapient.dividr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock dividr / dividr freischalten |
| dividr 1-Day Trial | `solutions.xapient.dividr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `dividr.storekit`.

## Berechtigungen
Kamera (`NSCameraUsageDescription`, DE/EN), nur zum Lesen der QR-Codes; ohne Erlaubnis bleibt alles außer Scannen nutzbar. `UIRequiredDeviceCapabilities`: nur arm64 (Kamera nicht Pflicht).

## App Privacy
„Data Not Collected“: Gruppen liegen nur auf dem Gerät, Austausch nur per QR-Code von Gerät zu Gerät.

## Screenshots
- Szenen: `scenes.txt` (App Store), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots, `SCAN`/`ABOUT` nur UI-Test. Startargument `-aappsScene <NAME>`, siehe `apps/dividr/shared/src/iosMain/.../Scenes.kt`.
