# tunr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| tunr Unlock | `solutions.xapient.tunr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock tunr / tunr freischalten |
| tunr 1-Day Trial | `solutions.xapient.tunr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `tunr.storekit`.

## Berechtigungen
Mikrofon (`NSMicrophoneUsageDescription`, DE/EN in `InfoPlist.strings`): nur zum Stimmen, solange der Stimmer im Vordergrund offen ist; nichts wird aufgenommen oder gespeichert. Abgelehnt = Hinweis mit Knopf zur Einstellungen-App. Referenzton über dieselbe Audio-Sitzung (PlayAndRecord, Lautsprecher). `UIRequiredDeviceCapabilities`: nur arm64.

App Privacy: weiterhin „Data Not Collected“ (Audio verlässt das Gerät nie).

## Screenshots
- Szenen: `scenes.txt` (App Store), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots. Startargument `-aappsScene <NAME>`, siehe `apps/tunr/shared/src/iosMain/.../Scenes.kt`.
