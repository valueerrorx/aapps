# pulsr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| pulsr Unlock | `solutions.xapient.pulsr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock pulsr / pulsr freischalten |
| pulsr 1-Day Trial | `solutions.xapient.pulsr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `pulsr.storekit`.

## Kategorie
Music.

## Berechtigungen
Keine Abfrage. `UIBackgroundModes` = `audio`: spielt bei gesperrtem Bildschirm weiter (wie der Vordergrunddienst auf Android), nur solange der Nutzer das Metronom gestartet hat; Anruf/Unterbrechung oder abgesteckter Kopfhörer stoppt. Audiosession Playback mit „Mix with others“ (Üben zu laufender Musik).

## App Privacy
„Data Not Collected“: Einstellungen liegen nur auf dem Gerät.

## Screenshots
- Szenen: `scenes.txt` (App Store), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots, `ABOUT` nur UI-Test. Startargument `-aappsScene <NAME>`, siehe `apps/pulsr/shared/src/iosMain/.../Scenes.kt`.
