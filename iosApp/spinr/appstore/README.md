# spinr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| spinr Unlock | `solutions.xapient.spinr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock spinr / spinr freischalten |
| spinr 1-Day Trial | `solutions.xapient.spinr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `spinr.storekit`.

## Berechtigungen
Keine. Klick-Ton über AVAudioPlayer in der Kategorie Ambient (Stummschalter gilt, andere Audio-Apps laufen weiter). `UIRequiredDeviceCapabilities`: nur arm64.

## Screenshots
- Szenen: `scenes.txt` (App Store), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots. Startargument `-aappsScene <NAME>`, siehe `apps/spinr/shared/src/iosMain/.../Scenes.kt`.
