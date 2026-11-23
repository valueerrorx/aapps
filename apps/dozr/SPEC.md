# dozr – SPEC

## Aufgabe
Einschlaf-Timer für jede App: Musik, Podcast, Hörbuch oder YouTube laufen lassen, dozr blendet nach X Minuten die Medienlautstärke sanft aus und stoppt die Wiedergabe. Danach ist die Lautstärke wieder wie vorher. Schwester von sleepr: sleepr stoppt nur die eigenen Geräusche, dozr alles.

## Muss
- **Hero: Mond-Dial.** Ein runder Nachthimmel mit Horizont und See. Der Mond liegt auf einem Halbbogen über dem Horizont: rechts am Horizont = 0, im Zenit = 60 min, links am Horizont = 120 min. Ziehen am Himmel zieht den Mond hoch (mehr Zeit) oder runter, in Rasten zu 5 min (5–120 min), jede Raste mit Haptik (`step`, Touch-Feedback; Schnellwahl-Chips `press`). Unter dem Horizont bleibt er am nächsten Ende stehen und springt nicht auf die andere Seite. Tippen ändert nichts. Einstellen nur, wenn der Timer nicht läuft.
- **Eigenes Motiv, kein Fortschrittsring:** Nachthimmel im runden Fenster mit Sternen, Hügeln, See und Spiegelung; die Zeit liest man an der Höhe des Monds. Akzent „Mondrose“ `#FF9EC7` (Rosa nutzt keine andere aapps-App), Mond Elfenbein, Himmel Tiefblau.
- **Untergehen:** Läuft der Timer, sinkt der Mond mit der Restzeit nach rechts zum Horizont, der Bogen vom Horizont zum Mond leuchtet. Beim Ausblenden taucht er ins Wasser, sein Schein wird schwächer. Danach ist er untergegangen, nur die Sterne bleiben.
- **Anzeige:** große Minuten „23“ mit kleinem „min“, beim Lauf dahinter klein und gedimmt die Sekunden („41 s“). Darunter der Zustand: Uhr-Symbol + ENDE 23:45 (auch im Leerlauf als Vorschau), BLENDET AUS, GESTOPPT UM 23:45. TalkBack liest „noch 23 Minuten 41 Sekunden“.
- **Ausblend-Kurve:** Unter dem Hero eine flache Kurve: Plateau, dann eine weiche Cosinus-Kurve auf 0. Beschriftet mit Beginn und Ende des Ausblendens (Uhrzeit). Ein Punkt zeigt nur beim Ausblenden die aktuelle Stelle.
- **Schnellwahl** 15 · 30 · 45 · 60 im Leerlauf. Beim Lauf steht an derselben Stelle „+10 MIN“ (Zeile behält ihre Höhe, nichts springt).
- **Hauptaktion** (Pille): START bzw. STOPP.
- **Ende** (auch im Hintergrund, bei gesperrtem Bildschirm, nach Prozess-Tod):
  1. Die letzten N Minuten (Einstellung: 1, 3, 5 min; Standard 1 min; kürzer wären bei wenigen Stufen hörbare Sprünge) wird `STREAM_MUSIC` in Indexschritten entlang der Kurve von der Ausgangslautstärke auf 0 gesenkt (ohne Lautstärke-Einblendung).
  2. Bei 0: Audio-Fokus `AUDIOFOCUS_GAIN` anfordern. Gut erzogene Player (Spotify, YouTube, Podcast-Apps) pausieren bei Fokusverlust.
  3. Läuft nach 1 s noch Musik (`isMusicActive`), einmal `KEYCODE_MEDIA_PAUSE` per `AudioManager.dispatchMediaKeyEvent`. Rückfall für Player, die den Fokus ignorieren, aber eine Media-Session haben. Bewusst PAUSE statt PLAY_PAUSE: ist nichts aktiv, startet dadurch nichts.
  4. Sobald nichts mehr läuft (höchstens 10 s warten): Fokus abgeben, dann die Ausgangslautstärke wiederherstellen. Läuft nach 10 s immer noch etwas, bleibt sie auf 0 (lieber still als laut mitten in der Nacht). Die Fertig-Benachrichtigung sagt das; beim nächsten Öffnen von dozr wird sie wiederhergestellt.
- **Lautstärke gehört dem Nutzer:** dozr merkt sich die zuletzt selbst gesetzte Stufe. Weicht die aktuelle davon ab, hat jemand anderer gedreht: dozr fasst die Lautstärke in diesem Lauf nicht mehr an und stellt am Ende auch nichts zurück (seine Wahl gilt). Gestoppt wird trotzdem. Verlängern oder Stoppen während des Ausblendens stellt die Ausgangslautstärke sofort wieder her (sofern unberührt).
- **Ausgabegerät:** Der Merker kennt das Gerät (Lautsprecher, Bluetooth, Kabel, USB), jedes hat eigene Stufen. Wechselt die Ausgabe, wird weder weiter ausgeblendet noch zurückgestellt; der Merker bleibt, bis dasselbe Gerät wieder aktiv ist (nächstes Öffnen). Begrenzt das System beim Zurückstellen (Gehörschutz bei Kopfhörern), wird die tatsächliche Stufe neu gelesen und der Merker behalten.
- **Verlängern:** +10 min über App, Benachrichtigung oder Schütteln; Restzeit höchstens 120 min.
- **Schütteln** (abschaltbar, Standard an): Beschleunigungssensor (`SENSOR_DELAY_UI`) erst ab 5 min vor dem Ausblenden bis zum Ende, sonst aus (Akku). Drei kräftige Stöße (> 2,4 g) innerhalb 1 s verlängern um 10 min, danach 3 s Pause. Bestätigung: kurzes Vibrieren.
- **Laufende Benachrichtigung** (Foreground-Service): Countdown (Chronometer), „Stoppt um 23:45“, Aktionen „+10 min“ und „Stopp“. Tippen öffnet die App.
- **Quick-Settings-Kachel:** Tippen startet mit dem zuletzt gewählten Wert bzw. stoppt einen laufenden Timer. Untertitel zeigt Wert bzw. Endzeit. In den Einstellungen „Kachel hinzufügen“ (ab Android 13).
- Lauf hängt an `elapsedRealtime`. Zustand (Lauf, Ausgangslautstärke, zuletzt gesetzte Stufe) wird bei jeder Änderung synchron gespeichert und übersteht Prozess-Tod; der Dienst ist `START_STICKY` und macht nahtlos weiter. War das Ende beim Wiederaufnehmen schon über 2 min vorbei (oder Neustart des Geräts), wird nichts mehr pausiert, nur die Lautstärke zurückgestellt.
- Verweigert das System dem Dienst den Vordergrund (z. B. Neustart aus dem Hintergrund): Ende schon erreicht → abschließen und Lautstärke zurück; läuft der Timer noch → Hinweis „Einschlaf-Timer läuft noch“, Tippen öffnet die App, die den Dienst neu startet. Ohne Benachrichtigungsrecht wird der Timer gestoppt (lieber ehrlich als ein Timer, der nie ausblendet).
- Hoch- und Querformat (quer: Text, Kurve und Bedienung links, Hero rechts). Englisch und Deutsch. TalkBack: im Leerlauf ist der Himmel ein Regler „30 Minuten“ in 5-min-Schritten, beim Lauf nur Anzeige (kein Regler).

## Timer-Mechanismus (ohne Exact-Alarm)
Ein Foreground-Service mit `Handler` auf dem Main-Looper rechnet jeden Schritt aus `elapsedRealtime` neu (vor dem Ausblenden höchstens alle 30 s, beim Ausblenden alle 250 ms). Ein `PARTIAL_WAKE_LOCK` (Zeitlimit = Restzeit + 1 min) hält die CPU erst ab 10 min vor dem Ausblenden wach; ohne ihn kann der Prozessor bei Offload-Wiedergabe schlafen und `Handler`-Zeiten laufen nicht weiter. Bis dahin weckt ein ungenauer `setAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP)`-Wecker den Dienst (keine Berechtigung nötig); die 10 min Vorlauf decken die Doze-Verzögerung ab, ein verpasstes Ende fängt die Überfällig-Regel. Ab der Sperre ist das Ende auf ±1 s genau, `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` sind unnötig (und für einen Einschlaf-Timer bei Play nicht begründbar).

## Berechtigungen (Allowlist in `apps/dozr/build.gradle.kts`)
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`: Der Dienst spielt nichts ab (also nicht `mediaPlayback`), er steuert fremde Wiedergabe; Begründung in `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`. Start nur aus dem Vordergrund (App, Benachrichtigungs-Aktion, Kachel-Tipp). Lehnt das System den Start aus der Kachel ab, öffnet die Kachel die App, die dann startet.
- `WAKE_LOCK`: siehe Timer-Mechanismus.
- `POST_NOTIFICATIONS` (ab 33): laufende Benachrichtigung. Abfrage vor dem Start, solange nicht erteilt und das System sie noch anzeigen darf. Weggetippt = beim nächsten Start erneut. Nach der ersten Ablehnung einmal eine eigene Erklärung, dann erneut fragen. Nach der zweiten Ablehnung nur noch über die Einstellungen (Zeile „Benachrichtigungen erlauben“, ggf. Systemeinstellung). Der Timer läuft auch ohne.
- `VIBRATE`: Bestätigung beim Schütteln bei gesperrtem Bildschirm (ohne Fenster keine View-Haptik).
- Keine: Lautstärke (`setStreamVolume`), Audio-Fokus und Medientaste brauchen keine Berechtigung. Kein INTERNET, kein Device-Admin, keine Bildschirm-aus-Funktion, kein Exact-Alarm, kein Boot-Receiver.

## Nicht
- Kein Abschalten von WLAN/Bluetooth/Bildschirm, keine Statistik, keine Wochentags-Automatik, kein Widget, keine eigenen Geräusche (das macht sleepr).

## Screens
1. **Timer**: Zahnrad oben rechts, Restzeit + Zustand, Mond-Dial (Hero), Ausblend-Kurve, Schnellwahl bzw. +10, Pille.
2. **Einstellungen**: Ausblenden (1 / 3 / 5 min), Schütteln verlängert, Haptik beim Einstellen, Benachrichtigungen erlauben (nur wenn fehlend), Kachel hinzufügen (ab 33), Info.
3. **Info**: wie alle aapps.

## Screenshot-Zustände (Roborazzi, `apps/dozr/screenshots/`)
| Name | Inhalt |
|---|---|
| `idle` | 30 min eingestellt |
| `running` | 23 min 41 s von 45 min |
| `fading` | 0:24 von 30:00, Ausblenden zu 60 % |
| `done` | untergegangen, „Gestoppt um“ |
| `drag` | Ziehen bei 90 min |
| `long` | 2:00:00 eingestellt |
| `settings`, `about` | Einstellungen, Info |
| `icon_masks`, `icon_store_512` | App-Icon |

Varianten (Pixel 7, en): `_de`, `_land`, `_font200`, `_land_font200` für `idle`, `running`, `fading`, `done`, `settings`. Dazu `running_360dp`.

## Abnahmekriterien
- Domain ohne Android-Imports. Unit-Tests: Restzeit/Zustände, Verlängern (Deckel), Fade-Kurve (monoton, Endpunkte), Fader (Wiederherstellen, Nutzer dreht während des Ausblendens, Verlängern/Stoppen während des Ausblendens, Prozess-Tod mitten im Ausblenden), Mond-Geometrie (Rasten, Horizont-Hysterese), Schütteln, Zeitformat, Laden kaputter/alter Daten, Kontrast (WCAG, gemessen).
- Robolectric: Gesten (Ziehen stellt ein, Tippen nicht, unter dem Horizont kein Sprung), Start/Stopp/+10, Dienst-Ende (Fokus, Lautstärke zurück), Sensor nur während des Laufs.
- Im Leerlauf und nach dem Ende keine Frames. Während des Laufs neu zeichnen nur, wenn sich der Mond um ~1 px bewegt oder die Sekunde wechselt (Zeichnen-Phase, kein Neu-Komponieren des Hero).
- `:apps:dozr:check`, `verifyRoborazziDebug`, `assembleRelease` grün, Release-APK < 4 MB.
