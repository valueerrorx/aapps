# magnr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| magnr Unlock | `solutions.xapient.magnr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock magnr / magnr freischalten |
| magnr 1-Day Trial | `solutions.xapient.magnr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `magnr.storekit`.

## Berechtigungen
Keine. Das Magnetfeld kommt aus CoreMotion (Device Motion, Bezug „beliebig, korrigiert“, damit iOS das Feld kalibriert), das braucht weder Standort noch Rückfrage. Der Ton läuft über AVAudioEngine, kein Mikrofon. `UIRequiredDeviceCapabilities`: arm64, accelerometer, magnetometer (haben alle iPhones und iPads).

## Screenshots
- Szenen: `scenes.txt` (App Store), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots. Startargument `-aappsScene <NAME>`, siehe `apps/magnr/shared/src/iosMain/.../Scenes.kt`.
