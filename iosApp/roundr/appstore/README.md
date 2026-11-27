# roundr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| roundr Unlock | `solutions.xapient.roundr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock roundr / roundr freischalten |
| roundr 1-Day Trial | `solutions.xapient.roundr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `roundr.storekit`.

## Berechtigungen
- Mitteilungen (UserNotifications, Alert + Ton): Abfrage beim ersten Start eines Timers. Jeder Abschnittswechsel ist als lokale Mitteilung mit eigenem Ton vorgemerkt (höchstens 60), so kommen die Phasen auch bei gesperrtem Bildschirm. Die Töne erzeugt die App beim Start als WAV in `Library/Sounds`. Kein Info.plist-Text nötig.
- Im Vordergrund spielt roundr alle Signale selbst (AVAudioPlayer, Kategorie Playback mischend: klingt auch bei Stummschalter, Musik läuft weiter) samt 3-2-1 und Vibration.
- `UIRequiredDeviceCapabilities`: nur arm64. Keine Hintergrund-Modi.

## Screenshots
- Szenen: `scenes.txt` (App Store, Play-Listing ohne Querformat), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots. Startargument `-aappsScene <NAME>`, siehe `apps/roundr/shared/src/iosMain/.../Scenes.kt`.
