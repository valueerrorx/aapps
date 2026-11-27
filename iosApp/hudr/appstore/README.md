# hudr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| hudr Unlock | `solutions.xapient.hudr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock hudr / hudr freischalten |
| hudr 1-Day Trial | `solutions.xapient.hudr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `hudr.storekit`.

## Berechtigungen
Nur Standort „Beim Verwenden der App“ (`NSLocationWhenInUseUsageDescription`), kein Hintergrund-Standort. Bei „ungefährem Standort“ fragt die App einmal pro Sitzung nach dem genauen (`NSLocationTemporaryUsageDescriptionDictionary`, Schlüssel `speed`). Der Standort verlässt das Gerät nie, App Privacy bleibt „Data Not Collected“. `UIRequiredDeviceCapabilities`: arm64, location-services, gps (iPads nur mit Mobilfunk, ein WLAN-iPad kann keine Geschwindigkeit messen).

## Screenshots
- Szenen: `scenes.txt` (App Store), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots. Startargument `-aappsScene <NAME>`, siehe `apps/hudr/shared/src/iosMain/.../Scenes.kt`.
