# aapps – Plan (Entwurf 21.11.2026, wartet auf Thomas' OK)

9 Geschwister-Apps von sleepr: tunr, pulsr, levlr, nordr, loudr, roundr, spinr, scorr, focusr.
Ideen und Recherche: `research/ideen.md`.

## Repo
- Monorepo `valueerrorx/aapps`, Gradle-Multimodul:
  - `build-logic/`: Convention-Plugins `aapps.app` und `aapps.lib`
  - `core/ui`: Theme, Glow, Federn, Haptik, Dial, BigNumber, Settings/About, Permission-Dialog
  - `core/audio`: kommt erst ab roundr dazu
  - `apps/<name>/`: SPEC.md, CHECKLIST.md, fastlane-Metadaten, version.txt
  - `docs/`: DESIGN.md, AGENTS.md, PRIVACY.md
- Jede App bekommt eine eigene applicationId `solutions.xapient.<name>`. Tags heißen `<name>-vX.Y.Z`, Releases `<name>-latest`.
- Empfehlung: das Repo öffentlich machen (GPLv3 wie sleepr). Dann ist CI kostenlos, privat wären die Actions-Minuten schnell aufgebraucht.

## Stack
- Kotlin + Jetpack Compose. sleepr nutzt dagegen Views/Canvas.
- Aktuelle stabile Versionen: AGP/Gradle/Kotlin, compileSdk 37, targetSdk 36, minSdk 26, JDK 17.
- Erlaubte Abhängigkeiten: Compose BOM, activity/lifecycle, core-ktx, kotlinx-serialization (nur wo nötig). Ausnahme: androidx.webkit nur für moovr und glidr.
- Verboten: Hilt, Navigation, Room, Firebase, Play Services, INTERNET.
- Release-Build mit R8. Die APK soll unter 4 MB bleiben.
- Sprachen: Englisch und Deutsch.

## Design
- Dunkle Basis wie sleepr (BG #10161B, SURFACE #172028, FG #DCE3E8), pro App eine Akzentfarbe mit Glow.
- Schriften: Space Grotesk für Zahlen, Inter für Text, beide offline eingebettet.
- Ein Hero-Element nimmt ≥ 60 % des Bildschirms ein. Höchstens eine Hauptaktion, Einstellungen hinter dem Zahnrad.
- Animationen über Federn statt linear, jede Rastung mit Haptik. Kein Dauer-Animieren im Leerlauf.
- Icons als VectorDrawable, adaptiv und mit Monochrom-Variante. Sie werden per Roborazzi in allen Masken gerendert. Daraus entstehen auch das Store-Icon (512) und die Feature-Grafik.

## Build und Qualität
- Lokal am NAS (JDK17 + SDK in ~, `source ~/android-env.sh`): Unit-Tests, Roborazzi-Screenshots, Lint, Debug-APK.
- GitHub Actions wie bei sleepr: ein Pfadfilter wählt nur die betroffenen Apps aus. Der Workflow liefert ein Artifact pro Push und `<name>-latest` auf main.
- Auf Thomas' Handy kommen nur CI-APKs, weil die Signatur sonst nicht passt.
- Signierung: Keystore nur als GitHub-Secret, mit einem Alias pro App. Er kommt nie ins Repo. Backup offline.
- Fachlogik ohne Android-Imports, damit sie per JVM testbar ist. Sensoren, Audio und Uhr laufen über Interfaces.
- Screenshot-Zustände werden in der SPEC festgelegt. Die PNGs liegen im Repo.
- Ein Build-Check bricht ab bei INTERNET oder nicht freigegebenen Berechtigungen.
- Debug-Diagnosemodus: Sensor-CSV bzw. Mikrofon-WAV per Teilen-Menü. Die Dateien landen als Test-Fixtures im Repo.

## Workflow pro App
1. SPEC (Architekt).
2. Critic: Gate A.
3. Dev arbeitet test-first: Gate B = `check` grün.
4. UI-Polish anhand der Screenshots: Gate C = Critic prüft gegen DESIGN.md.
5. Tester schreibt fiese Tests und CHECKLIST.md.
6. Review, Merge, CI grün.
7. APK-Link + Checkliste an Thomas: Gate D = Thomas gibt frei, dann Tag.

## Reihenfolge (Schätzung in Agenten-Tagen)
1. levlr (Pilot, baut Core/CI mit) 4–5
2. nordr 1,5
3. spinr 1,5
4. scorr 1,5
5. focusr 2
6. roundr 2,5
7. pulsr 3
8. loudr 2,5
9. tunr 3–4

Summe ~22–25 Tage. In Kalenderzeit eher 5–7 Wochen, weil Thomas' Testrunden der Engpass sind.

## Risiken
- Sensor-Kalibrierung: levlr mit Zwei-Punkt-Kalibrierung, nordr mit Genauigkeitsanzeige.
- Audio-Timing bei pulsr: Klicks werden sample-genau in den Stream gemischt.
- Tiefe Saiten bei tunr: YIN/MPM mit großem Puffer.
- Mikrofon-Quelle: UNPROCESSED, wo verfügbar.
- Foreground-Service-Policy:
  - pulsr: mediaPlayback.
  - focusr: kein Service, stattdessen USE_EXACT_ALARM.
  - roundr: specialUse nur bei gesperrtem Bildschirm.
- Play-Store: privates Konto braucht 12 Tester / 14 Tage pro App, daher ein gemeinsamer Tester-Pool.
- Ab 2027 Entwickler-Verifizierung auch für Sideloading.
- Gefahr von Overengineering: Allowlist, Code erst beim zweiten Nutzer in den Core, der Critic darf streichen.
