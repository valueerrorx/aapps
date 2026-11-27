# focusr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| focusr Unlock | `solutions.xapient.focusr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock focusr / focusr freischalten |
| focusr 1-Day Trial | `solutions.xapient.focusr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `focusr.storekit`.

## Berechtigungen
- Mitteilungen (UserNotifications, Alert + Ton): Abfrage beim ersten Start des Timers, danach nur über die Einstellungen. Eine lokale Mitteilung „Zeit um“ mit dem Glockenton (`chime.wav` aus `apps/focusr/src/main/res/raw`, im Bundle) kommt auch bei gesperrtem Bildschirm oder beendeter App. Kein Info.plist-Text nötig.
- Im Vordergrund spielt focusr Glocke (AVAudioPlayer, Kategorie Playback mit Ducking, klingt auch bei Stummschalter) und Vibration selbst.
- `UIRequiredDeviceCapabilities`: nur arm64. Keine Hintergrund-Modi.

## Screenshots
- Szenen: `scenes.txt` (App Store, Play-Listing ohne Querformat), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots. Startargument `-aappsScene <NAME>`, siehe `apps/focusr/shared/src/iosMain/.../Scenes.kt`.
