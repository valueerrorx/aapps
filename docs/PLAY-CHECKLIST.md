# Play-Store-Checkliste (aapps)

Für alle 18 Apps. [x] = vorbereitet, [ ] = musst du tun, [?] = deine Entscheidung.
Store-Texte: `apps/<app>/fastlane/metadata/android/{de-DE,en-US}/`, Grafik-Listen: `apps/<app>/fastlane/README.md`.

## 1. Konto und Grundlagen
- [x] Entwickler-Verifizierung (erledigt 25.11.2026).
- [ ] Testpflicht für neue private Konten: geschlossener Test mit mindestens 12 Testern, 14 Tage am Stück, **je App**, bevor Produktion freigeschaltet wird. Tipp: eine Google-Gruppe als gemeinsamer Tester-Pool für alle 18 Apps, alle Tests parallel starten. Entfällt nur, falls dein Konto vor 13.11.2023 angelegt wurde.
- [ ] Zahlungsprofil (Händlerkonto) für In-App-Käufe einrichten. Modell: alle Apps gratis im Store, die 16 Apps außer den Spielen mit 24-h-Testphase und einmaligem In-App-Kauf `full_unlock` (siehe 2a), moovr und glidr komplett gratis. Achtung: Eine App, die einmal gratis war, kann nie mehr kostenpflichtig werden (In-App-Kauf geht trotzdem).
- [ ] EU-Händlerstatus (DSA) angeben; als Händler werden Kontaktdaten im Store öffentlich angezeigt.
- [ ] Öffentliche Kontakt-E-Mail im Store eintragen (Pflicht).

## 2. Store-Eintrag je App
- [x] Titel, Kurz- und Langbeschreibung, Changelog DE/EN (Limits geprüft: Titel ≤ 30, kurz ≤ 80, lang ≤ 4000, Changelog ≤ 500 Zeichen).
- [x] Changelog-Datei = versionCode aus `version.txt` (X·10000 + Y·100 + Z): `100.txt` für 0.1.0, `10000.txt` für sleepr, moovr, glidr (1.0.0). Änderst du die Version vor dem Upload, Datei umbenennen.
- [x] Icon 512×512 (RGBA), Feature-Grafik 1024×500 und Screenshots (auf 2:1 gebracht) bei allen 18 unter `metadata/android/<locale>/images/`, erzeugt mit `docs/overview/tools/store-images.js`, geprüft mit `--check`.
- [x] Screenshot-Sprachvarianten angeglichen (sleepr, loudr, levlr, nordr, hertzr, magnr, pulsr, hudr, tunr: DE und EN gleiche Szenen).
- [x] moovr (5) und glidr (6): echte Spiel-Screenshots quer, erzeugt mit `docs/overview/tools/game-shots/`.
- [ ] Datenschutzerklärung `docs/privacy/index.html` hosten (Vorschlag: https://xapient.solutions/privacy/) und in jeder App als URL eintragen. Kontakt eingetragen (25.11).

## 2a. In-App-Produkt `full_unlock` (alle außer moovr, glidr)
Apps: sleepr, levlr, nordr, spinr, scorr, focusr, roundr, pulsr, loudr, tunr, magnr, dozr, hertzr, hudr, thinkr, dividr.
- [ ] Je App zuerst ein AAB mit Billing hochladen (interner Test genügt), erst dann lässt die Console In-App-Produkte zu.
- [ ] Monetarisieren > Produkte > In-App-Produkte (Einmalkauf): Produkt-ID exakt `full_unlock`, aktiv, nicht verbrauchbar (die App bestätigt den Kauf, verbraucht ihn nie).
- [ ] Name/Beschreibung DE/EN, z. B. „Dauerhaft freischalten“ / „Unlock for good“, „Einmaliger Kauf, alle künftigen Updates inklusive“.
- [ ] Preis: 1,49 € für alle Apps (Thomas), Play rechnet Landeswährungen um. Im Code nicht hartcodiert, die App zeigt den Preis aus ProductDetails.
- [ ] Lizenztester eintragen (Einstellungen > Lizenztests, Gmail-Konten): Testkäufe ohne echte Zahlung, inkl. „langsame Testkarte“ für den ausstehenden Kauf.
- [ ] Auf dem Gerät testen: Testphase ablaufen lassen (Debug-Build: `adb shell am start -S -n solutions.xapient.<app>/.MainActivity --es aapps_trial expire`), Preis erscheint, Kauf, App frei, Flugmodus bleibt frei, „Kauf wiederherstellen“ nach Neuinstallation, Erstattung in der Console → beim nächsten Start wieder Dankes-Bildschirm.
- Hinweis: Die Billing-Library bringt INTERNET und ACCESS_NETWORK_STATE (über Googles datatransport) mit; beide werden im Manifest entfernt, die Apps bleiben ohne Internet-Berechtigung. Der Kauf läuft per IPC über die Play-Store-App. Darum unbedingt auf einem echten Gerät mit Lizenztester testen, bevor eine App in Produktion geht.

## 3. Kategorie
| App | Kategorie (Vorschlag) |
|---|---|
| sleepr | Musik & Audio |
| levlr | Tools |
| nordr | Tools |
| spinr | Unterhaltung |
| scorr | Unterhaltung |
| focusr | Produktivität |
| roundr | Gesundheit & Fitness [?] (alternativ Sport) |
| pulsr | Musik & Audio |
| loudr | Tools |
| tunr | Musik & Audio |
| magnr | Tools |
| dozr | Musik & Audio |
| hertzr | Tools |
| hudr | Karten & Navigation [?] (alternativ Auto & Fahrzeuge) |
| moovr | Spiel: Arcade |
| glidr | Spiel: Denkspiele (Puzzle) |
| thinkr | Spiel: Denkspiele (Puzzle) [?] (alternativ Lernen) |
| dividr | Finanzen [?] (alternativ Reisen & Lokales) |

## 4. App-Inhalte (Policy-Formulare)
### Datensicherheit (Data safety), alle 18 gleich
- Erhebt oder teilt die App Nutzerdaten? **Nein.**
- Begründung: Keine App hat die INTERNET-Berechtigung (Build-Check `check<Variant>ManifestPermissions` bricht sonst ab; die von Play Billing mitgebrachte wird entfernt). Kein SDK für Werbung, Analyse oder Absturzberichte, kein Firebase. Einzige Bibliothek von Google ist Play Billing (Kauf per IPC über die Play-Store-App). Daten, die nur auf dem Gerät verarbeitet werden, gelten laut Play nicht als „erhoben“.
  - Kaufstatus: Die App erfährt nur „gekauft ja/nein“ und speichert das lokal; Zahlungsdaten verarbeitet Google selbst. [?] Vor dem Ausfüllen Googles aktuelle Angaben zur Play Billing Library im Data-safety-Leitfaden prüfen.
- Je App zur Kontrolle:
  - loudr, tunr: Mikrofon nur live, nichts gespeichert oder gesendet → nicht erhoben.
  - hudr: genauer Standort nur im Vordergrund, nicht gespeichert, kann nicht gesendet werden → nicht erhoben.
  - scorr: Spielernamen nur lokal → nicht erhoben.
  - dividr: Kamera nur zum QR-Scannen; Namen und Ausgaben bleiben lokal und gehen nur per QR von Handy zu Handy, Teilen als Text nur auf Tippen des Nutzers über das System-Teilen → nicht erhoben.
  - thinkr: nur lokale Spielstände.
  - sleepr: `allowBackup=true`, Einstellungen können in die System-Sicherung des Nutzers gehen; das ist das Android-Backup, keine Erhebung durch die App.
  - alle anderen: nur Sensoren und lokale Einstellungen.
- Sicherheit: Verschlüsselung bei Übertragung entfällt (keine Übertragung), Löschanfrage entfällt (nichts erhoben).

### Weitere Formulare
- Werbung: **Nein**, bei allen.
- App-Zugriff: alle Funktionen ohne Anmeldung zugänglich.
- Gesundheits-Apps: alle „keine Gesundheitsfunktionen“. [?] roundr: falls Kategorie Gesundheit & Fitness, ehrlich „Trainings-Timer, erfasst keine Gesundheitsdaten“.
- Finanzfunktionen, Behörden-App, Nachrichten: Nein. [?] dividr: rechnet nur Ausgaben einer Gruppe auf, keine Zahlungen oder Finanzdienste → „keine Finanzfunktionen“.

### Altersfreigabe (IARC-Fragebogen)
- Apps: Kategorie „Dienstprogramm, Produktivität, Kommunikation oder andere“. Alles Nein: keine Gewalt, keine Sexualität, keine Sprache, keine Drogen, kein Glücksspiel, keine Nutzer-Interaktion, kein Teilen von Standort mit anderen. Digitale Käufe: **Ja** (Einmalkauf `full_unlock`), außer moovr und glidr.
  - spinr: kein Glücksspiel und kein simuliertes Glücksspiel (keine Einsätze, keine Gewinne).
  - hudr: nutzt Standort, teilt ihn aber nicht mit anderen Nutzern → Nein.
- Spiele (moovr, glidr): Kategorie „Spiel“. Keine Gewalt gegen Menschen oder Tiere im eigentlichen Sinn; die Kuh kann comichaft an Stacheln oder im Wasserloch scheitern. [?] Je nach Wortlaut „keine Gewalt“ oder höchstens „Comic/Fantasy, mild“. Kein Blut, keine Käufe, keine Werbung, kein Online (kein Billing).
- thinkr: Kategorie „Spiel“, keine Gewalt, Digitale Käufe: Ja.
- Erwartung: niedrigste Stufe (USK 0 / PEGI 3 / Everyone).

### Zielgruppe
- [x] Entschieden: alle Apps **13+**, nicht für Kinder gezielt. Wählst du Altersgruppen unter 13 (z. B. für die Spiele oder focusr im Unterricht), gilt das Familien-Programm mit strengeren Regeln (Inhalte, Links nach außen, Prüfung).

## 5. Sonderberechtigungen (Play-Console-Erklärungen)
Für jeden Vordergrunddienst-Typ verlangt die Console eine Beschreibung und einen **Video-Link** (z. B. nicht gelistetes Video, 30 s, Funktion bei gesperrtem Bildschirm). [ ] Videos aufnehmen.

| App | Erklärung nötig |
|---|---|
| sleepr, pulsr, hertzr | FGS `mediaPlayback` |
| roundr, dozr | FGS `specialUse` (Untertyp steht schon im Manifest) |
| focusr | `USE_EXACT_ALARM` (+ `SCHEDULE_EXACT_ALARM` nur bis Android 12L) |
| hudr | keine Erklärung (nur Vordergrund-Standort), aber Datenschutz-URL Pflicht |
| loudr, tunr | keine Erklärung, aber Datenschutz-URL Pflicht (Mikrofon) |
| dividr | keine Erklärung, aber Datenschutz-URL Pflicht (Kamera) |

### mediaPlayback – sleepr
- DE: sleepr erzeugt Rauschen, das der Nutzer zum Einschlafen oder Abschirmen von Geräuschen startet. Die Wiedergabe muss bei gesperrtem Bildschirm weiterlaufen. Der Dienst läuft nur, solange der Nutzer abspielt, zeigt eine Benachrichtigung mit Stopp und endet bei Stopp, Timer-Ende oder Audio-Fokus-Verlust. Wird er unterbrochen, verstummt das Rauschen mitten in der Nutzung.
- EN: sleepr generates noise that the user starts to fall asleep or mask sounds. Playback must continue while the screen is locked. The service runs only while the user is playing audio, shows a notification with a stop button, and ends on stop, timer end or loss of audio focus. If it were interrupted, the noise would stop while the user is relying on it.

### mediaPlayback – pulsr
- DE: pulsr ist ein Metronom. Der Klick wird als durchgehender Audiostream abgespielt und muss bei gesperrtem Bildschirm weiterlaufen, während der Nutzer ein Instrument übt. Der Dienst läuft nur während der Wiedergabe, zeigt Tempo, Takt und Stopp in der Benachrichtigung und endet bei Stopp, abgestecktem Kopfhörer oder Anruf.
- EN: pulsr is a metronome. The click is played as a continuous audio stream and must keep playing with the screen locked while the user practises an instrument. The service runs only during playback, shows tempo, time signature and a stop button in the notification, and ends on stop, headphone unplug or an incoming call.

### mediaPlayback – hertzr
- DE: hertzr ist ein Tongenerator für Lautsprecher- und Raumtests. Ein Testton oder ein Sweep bis 60 s muss weiterlaufen, wenn der Bildschirm ausgeht, weil das Handy dabei oft abgelegt wird. Der Dienst läuft nur während der Wiedergabe, zeigt Frequenz und Stopp in der Benachrichtigung und endet bei Stopp, abgestecktem Kopfhörer oder Anruf.
- EN: hertzr is a tone generator for speaker and room tests. A test tone or a sweep of up to 60 s must keep playing when the screen turns off, because the phone is often set down during the test. The service runs only during playback, shows the frequency and a stop button in the notification, and ends on stop, headphone unplug or an incoming call.

### specialUse – roundr
- DE: roundr ist ein Intervall-Timer für Training. Während eines vom Nutzer gestarteten Trainings zählt der Dienst Arbeits- und Pausenphasen und spielt 3-2-1-Signale und Vibration, auch wenn das Handy gesperrt in der Tasche ist. Kein Standardtyp passt: `mediaPlayback` passt nicht zu kurzen Signaltönen, `health` würde Sensor-Rechte verlangen, die roundr nicht braucht. Der Dienst läuft nur während eines aktiven Trainings und endet bei Fertig oder Beenden. Würde er verzögert, kämen die Signale zu spät.
- EN: roundr is an interval workout timer. During a workout started by the user, the service counts work and rest phases and plays 3-2-1 signals and vibration, even when the phone is locked in a pocket. No standard type fits: `mediaPlayback` does not match short signal tones, and `health` would require sensor permissions roundr does not need. The service runs only while a workout is active and ends when it is finished or stopped. If it were deferred, the signals would arrive late.

### specialUse – dozr
- DE: dozr ist ein Einschlaf-Timer für andere Apps. Nach einem vom Nutzer gestarteten Countdown senkt der Dienst die Medienlautstärke schrittweise, bittet andere Player per Audio-Fokus um Pause und stellt die Lautstärke danach wieder her. dozr spielt selbst nichts ab, daher passt `mediaPlayback` nicht. Der Dienst läuft nur, solange ein vom Nutzer gestarteter Timer aktiv ist, zeigt Restzeit, „+10 min“ und Stopp in der Benachrichtigung. Würde er verzögert, liefe die Wiedergabe die ganze Nacht weiter oder die Lautstärke bliebe auf null.
- EN: dozr is a sleep timer for other apps. After a countdown started by the user, the service gradually lowers the media volume, asks other players to pause via audio focus and then restores the volume. dozr does not play audio itself, so `mediaPlayback` does not fit. The service runs only while a timer started by the user is active and shows the remaining time, "+10 min" and stop in the notification. If it were deferred, playback would continue all night or the volume would stay at zero.

### USE_EXACT_ALARM – focusr
- DE: focusr ist eine Timer-App; der Timer ist die Kernfunktion. Der Nutzer stellt eine Dauer ein und erwartet Ton und Vibration genau zum Ende, auch bei gesperrtem Bildschirm und im Ruhezustand des Geräts. Dafür wird genau ein exakter Wecker für das Timer-Ende gesetzt und bei Pause oder Zurücksetzen gelöscht. Kein Vordergrunddienst.
- EN: focusr is a timer app; the countdown timer is its core function. The user sets a duration and expects sound and vibration exactly at the end, even with the screen locked and the device idle. For this, exactly one exact alarm is set for the end of the timer and cancelled on pause or reset. No foreground service is used.

## 6. Signierung
- [x] Release-Keystore mit einem Alias pro App (Alias = App-Name). Alle 18 Aliase angelegt (thinkr, dividr am 25.11) und ein Backup existiert (Ort steht bewusst nicht im Repo).
- [ ] Zusätzlich ein Backup außer Haus bzw. offline (laut PLAN), getrennt vom NAS.
- [ ] GitHub-Secrets setzen (`KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD_<APP>`), damit die CI signiert. Wartet auf Token-Rechte.
- [?] Play App Signing (Pflicht bei AAB): Standard ist, dass Google den App-Signaturschlüssel erzeugt und unser Key nur Upload-Key ist. Folge: Play-Installationen und GitHub-APKs haben verschiedene Signaturen, ein Wechsel zwischen beiden geht nur mit Neuinstallation. Alternative: eigenen Key als App-Signaturschlüssel hochladen (PEPK), dann bleibt die Signatur gleich.
- [ ] sleepr: alter Key aus dem früheren sleepr-Repo nicht mehr verwenden; Play bekommt den neuen.

## 7. Build und Upload
- Play verlangt AAB statt APK. Je App:
  `./gradlew :apps:<app>:bundleRelease -Paapps.requireReleaseKey=true` mit `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD_<APP>` → `apps/<app>/build/outputs/bundle/release/<app>-release.aab`
- [x] Builds am NAS per `xapient-job` (Speicher knapp, eine App nach der anderen); signierte AABs aller 18 Apps liegen außerhalb des Repos bereit.
- [ ] R8-Mapping (`build/outputs/mapping/release/mapping.txt`) mit hochladen, damit Absturzberichte lesbar sind.
- [ ] Jeder weitere Upload braucht einen höheren versionCode, also `version.txt` erhöhen.
- Optional: `fastlane supply` liest die Texte direkt aus `apps/<app>/fastlane/metadata/android`. Sonst Texte per Copy-and-paste in die Console.
