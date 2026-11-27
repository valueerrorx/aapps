# loudr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| loudr Unlock | `solutions.xapient.loudr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock loudr / loudr freischalten |
| loudr 1-Day Trial | `solutions.xapient.loudr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `loudr.storekit`.

## Berechtigungen
Mikrofon (`NSMicrophoneUsageDescription`, DE/EN in `InfoPlist.strings`): nur zum Messen, solange die App im Vordergrund ist; nichts wird aufgenommen oder gespeichert. Abgelehnt = Hinweis mit Knopf zur Einstellungen-App. `UIRequiredDeviceCapabilities`: nur arm64.

App Privacy: weiterhin „Data Not Collected“ (Audio verlässt das Gerät nie).

## Screenshots
- Szenen: `scenes.txt` (App Store), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots. Startargument `-aappsScene <NAME>`, siehe `apps/loudr/shared/src/iosMain/.../Scenes.kt`.
