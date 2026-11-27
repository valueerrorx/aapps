# hertzr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| hertzr Unlock | `solutions.xapient.hertzr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock hertzr / hertzr freischalten |
| hertzr 1-Day Trial | `solutions.xapient.hertzr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `hertzr.storekit`.

## Berechtigungen
Keine. Hintergrund-Audio (`UIBackgroundModes` = `audio`): Ton und Sweep laufen bei gesperrtem Bildschirm weiter, wie der Vordergrunddienst auf Android. Anzeige und Stopp am Sperrbildschirm statt der Android-Benachrichtigung. Kopfhörer-Erkennung über die Audio-Route (Pegelgrenze), Stopp bei abgezogenem Kopfhörer und bei Unterbrechung. `UIRequiredDeviceCapabilities`: nur arm64.

## Screenshots
- Szenen: `scenes.txt` (App Store), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots. Startargument `-aappsScene <NAME>`, siehe `apps/hertzr/shared/src/iosMain/.../Scenes.kt`.
