# thinkr – App-Store-Material

Aufbau, Grenzen der Textfelder, App Privacy und Age Rating wie bei levlr: siehe `../../levlr/appstore/README.md`.

## In-App-Käufe (beide Non-Consumable)

| Reference Name | Product ID | Preis | Display Name EN / DE |
|---|---|---|---|
| thinkr Unlock | `solutions.xapient.thinkr.full_unlock` | 1,49 € (Basisland Österreich) | Unlock thinkr / thinkr freischalten |
| thinkr 1-Day Trial | `solutions.xapient.thinkr.trial` | 0 (Free) | 1-Day Trial / 1 Tag testen |

Beschreibungen (max. 55 Zeichen) wie in `thinkr.storekit`.

## Kategorie
Games, Unterkategorien Puzzle und Casual (`metadata/primary_subcategories.txt`). Unterhaltung, keine Gesundheitsversprechen.

## Berechtigungen
Keine. Töne über die Ambient-Audiosession (Stummschalter gilt), „Bewegung reduzieren“ hält die Deko-Animationen an.

## App Privacy
„Data Not Collected“: Fortschritt liegt nur auf dem Gerät.

## Screenshots
- Szenen: `scenes.txt` (App Store), dazu `OFFER`/`THANKS` für die IAP-Review-Screenshots, `GAMES`/`SETTINGS`/`ABOUT` nur UI-Test. Startargument `-aappsScene <NAME>`, siehe `apps/thinkr/shared/src/iosMain/.../Scenes.kt`.
