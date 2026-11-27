# sleepr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| sleepr Unlock | `solutions.xapient.sleepr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock sleepr / sleepr freischalten |
| sleepr 1-Day Trial | `solutions.xapient.sleepr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `sleepr.storekit`.

## Berechtigungen
Keine. Hintergrund-Audio (`UIBackgroundModes` = `audio`): das Rauschen läuft bei gesperrtem Bildschirm weiter, wie der Vordergrunddienst auf Android. Steuerung am Sperrbildschirm über `MPRemoteCommandCenter`. `UIRequiredDeviceCapabilities`: nur arm64.

## Screenshots
- Szenen: `scenes.txt` (App Store), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots. Startargument `-aappsScene <NAME>`, siehe `apps/sleepr/shared/src/iosMain/.../Scenes.kt`.
