# nordr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| nordr Unlock | `solutions.xapient.nordr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock nordr / nordr freischalten |
| nordr 1-Day Trial | `solutions.xapient.nordr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `nordr.storekit`.

## Berechtigungen
Keine. Die Richtung kommt aus CoreMotion (Device Motion, Bezug magnetisch Nord), das braucht weder Standort noch Rückfrage. Rechtweisend rechnet die App mit dem eingebauten Weltmagnetfeld-Modell (WMM2025) für die gewählte Region. `UIRequiredDeviceCapabilities`: arm64, accelerometer, magnetometer (haben alle iPhones und iPads).

## Screenshots
- Szenen: `scenes.txt` (App Store), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots. Startargument `-aappsScene <NAME>`, siehe `apps/nordr/shared/src/iosMain/.../Scenes.kt`.
