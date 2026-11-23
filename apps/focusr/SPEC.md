# focusr – SPEC

## Aufgabe
Ein visueller Timer nach dem Time-Timer-Prinzip: Die verbleibende Zeit sieht man als farbige Scheibe, die langsam schrumpft. Gedacht für Lernen, Meetings, Kochen und den Unterricht (Beamer im Querformat). Zielgruppe sind alle, also ausdrücklich keine Kinder- oder Gesundheits-App.

## Muss
- **Hero-Scheibe**: Ein Sektor in Koralle beginnt bei 12 Uhr und läuft im Uhrzeigersinn (bewusste Entscheidung: wie eine Uhr, im Uhrzeigersinn ziehen = mehr Zeit). Seine Größe ist die Restzeit geteilt durch die Skala. Läuft der Timer, schrumpft er zurück Richtung 12 Uhr. Die ursprünglich eingestellte Zeit bleibt als blasser Schatten stehen. Der Lichthof um die Scheibe wird mit der Restzeit schwächer. In der letzten Minute pulsiert der Sektor leicht.
- **Skala** (in den Einstellungen): 5, 20, 60 oder 120 min, Standard ist 60. Es gibt 60 Rasten pro Umdrehung, also 5 s, 20 s, 1 min bzw. 2 min pro Raste. Beim Wechsel der Skala wird die eingestellte Zeit auf das Maximum begrenzt. Ein laufender Timer bleibt unverändert, die Scheibe zeigt dann höchstens „voll“.
- **Einstellen per Ziehen** auf der Scheibe, und zwar in jedem Zustand. Der Sektorrand folgt dem Finger und rastet ein, jede Raste gibt Haptik (`step`, Touch-Feedback; Tasten `press`). Bei 12 Uhr bleibt er voll bzw. leer stehen und springt nicht über, auch nicht beim Überdrehen um mehrere Umdrehungen. Er greift erst wieder, wenn der Finger 12 Uhr zurück überquert. Während der Timer läuft, nimmt die Scheibe Ziehen nur nahe der Kante an (±20°), damit ein versehentliches Wischen die Restzeit nicht verstellt. Ein bloßes Tippen ändert nichts. Was beim Loslassen eingestellt ist, gilt als neue Zeit:
  - im Leerlauf: neue Zeit
  - wenn der Timer läuft: läuft mit der neuen Restzeit weiter
  - pausiert: neue Restzeit
  - nach dem Ende: neue Zeit im Leerlauf
  - bei 0: Leerlauf
- **Hauptaktion** (Pille): START, PAUSE, WEITER, NOCHMAL (nach dem Ende). Daneben „Zurücksetzen“ als Symbol, sichtbar sobald nicht Leerlauf. Wer mit 0 startet, bekommt eine abweisende Haptik und den Hinweis „Scheibe ziehen“.
- **Anzeige**: große Restzeit `m:ss` bzw. `h:mm:ss` (aufgerundet auf volle Sekunden, 0:00 erst am Ende). Darunter steht der Zustand: BEREIT, ENDET UM 14:35, PAUSIERT oder ZEIT UM.
- **Ende**, auch im Hintergrund, bei gesperrtem Bildschirm und nach Prozess-Tod:
  - Ein exakter Alarm (`AlarmManager.setExactAndAllowWhileIdle`) weckt einen Receiver.
  - Der Receiver spielt einen sanften, selbst erzeugten Glockenton (`res/raw/chime.wav`, ~3 s, Audio-Nutzung Alarm), vibriert (Muster ~2 s) und zeigt die Benachrichtigung „Zeit um“ mit der tatsächlich gelaufenen Dauer, wenn die App nicht sichtbar ist. Während der Glocke holt sich focusr den Audio-Fokus mit Ducken (andere Wiedergabe wird nur leiser).
  - Ton und Vibration sind einzeln abschaltbar.
- **Benachrichtigung während des Laufs**: Ist die App im Hintergrund, zeigt eine stille, laufende Benachrichtigung einen Countdown (Chronometer). Sie verschwindet 2 s nach dem Ende von selbst (`setTimeoutAfter`), es gibt also keinen negativen Countdown. Kein Foreground-Service.
- Lauf und Alarm hängen an der Laufzeituhr (`elapsedRealtime`, `ELAPSED_REALTIME_WAKEUP`), ein Uhrzeitwechsel ändert nichts. Die Wanduhr dient nur der Anzeige („Endet um“) und zur Umrechnung nach einem Neustart (Erkennung über `BOOT_COUNT`).
- Der Zustand wird bei jeder Änderung gespeichert und übersteht Drehen und Prozess-Tod. Ein abgelaufener Timer zeigt beim Öffnen „Zeit um“. Beim App-Start wird der Alarm für einen laufenden Timer neu gesetzt.
- Der Bildschirm bleibt an, solange der Timer läuft (abschaltbar).
- Hoch- und Querformat (quer: Text und Bedienung links, Scheibe rechts). Englisch und Deutsch. TalkBack: Die Scheibe ist ein Regler mit Zustand „25 Minuten“ und lässt sich über Lautstärke/Wischen in Rasten verstellen.

## Berechtigungen (Allowlist in `apps/focusr/build.gradle.kts`)
- `USE_EXACT_ALARM` (ab API 33) und `SCHEDULE_EXACT_ALARM` mit `maxSdkVersion=32`: Das Ende kommt auch im Doze-Modus pünktlich. Die Play-Richtlinie erlaubt das für Timer-Apps, ein Dialog ist nicht nötig. Ohne Recht (vom Nutzer entzogen auf API 31/32) gibt es den Fallback `setAndAllowWhileIdle`.
- `POST_NOTIFICATIONS` (ab API 33, Laufzeit): für die Benachrichtigungen „Zeit um“ und „läuft“. Die Abfrage kommt beim ersten Start. Der Timer funktioniert auch ohne diese Berechtigung, Ton und Vibration kommen trotzdem.
- `VIBRATE`: Vibration am Ende bei gesperrtem Bildschirm. Ohne Fenster gibt es keine View-Haptik, daher ist die Berechtigung nötig. Sie ist normal und wird ohne Dialog erteilt.
- Kein Foreground-Service, kein INTERNET, kein Boot-Receiver (nach einem Neustart zeigt die App beim Öffnen „Zeit um“, aber ohne Ton).

## Nicht
- Kein Pomodoro-Zyklus, keine Vorlagen oder Listen, nicht mehrere Timer, keine Statistik, kein Widget.
- Keine Aktionen in der Benachrichtigung (Tippen öffnet die App).

## Screens
1. **Timer**: Zahnrad oben rechts, Restzeit und Zustand, die Scheibe (Hero), unten Zurücksetzen plus Hauptaktion.
2. **Einstellungen**: Skala, Ton am Ende, Vibration am Ende, Haptik beim Einstellen, Bildschirm an, Benachrichtigungen erlauben (nur wenn verweigert), Info.
3. **Info**: wie alle aapps.

## Screenshot-Zustände (Roborazzi, `apps/focusr/screenshots/`)
| Name | Inhalt |
|---|---|
| `idle` | 25:00 eingestellt, Skala 60 |
| `running` | 17:42 von 25:00, Schatten sichtbar, „Endet um“ |
| `paused` | 9:15 pausiert |
| `last_minute` | 0:42, Puls eingefroren |
| `done` | Zeit um |
| `drag` | Ziehen bei 40 min |
| `zero` | 0:00 im Leerlauf, Hinweis |
| `scale5`, `scale20`, `scale120` | 3:20 / 12:40 / 1:35:00 laufend |
| `settings`, `about` | Einstellungen, Info |
| `icon_masks`, `icon_store_512` | App-Icon in Masken, Store-Icon |

Varianten (Standard Pixel 7, en): `_de`, `_land`, `_font200` und `_land_font200` für `idle`, `running`, `paused`, `done` und `settings`. Dazu `running_360dp`.

## Abnahmekriterien
- Domain ohne Android-Imports. Unit-Tests für:
  - Zustände und Übergänge, Restzeit/Ende, Ziehen in allen Zuständen
  - Folgen am 12-Uhr-Übergang, Rasten, Skalen
  - Zeitformat, Ticker-Intervall, Laden kaputter Daten, Kontrast (WCAG AA, gemessen)
- Robolectric:
  - Gesten: Ziehen stellt ein, Tippen nicht
  - Start/Pause/Reset
  - Alarm gesetzt bzw. gelöscht
  - Receiver meldet das Ende
- Im Leerlauf, pausiert und nach dem Ende keine Animations-Frames. Während des Laufs nur so oft neu zeichnen, wie sich der Rand um ~1 px bewegt (höchstens 60/s, mindestens 1/s).
- `:apps:focusr:check`, `verifyRoborazziDebug` und `assembleRelease` grün. Im Manifest nur die freigegebenen Berechtigungen, Release-APK < 4 MB.
- Lesbar bei Schriftgröße 200 %.
