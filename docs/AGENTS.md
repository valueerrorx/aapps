# aapps – Hinweise für Agenten

## Bauen am NAS
```sh
source ~/android-env.sh
./gradlew :apps:<app>:check :apps:<app>:assembleDebug     # Unit-Tests, Lint, Berechtigungs-Check
./gradlew :apps:<app>:recordRoborazziDebug                # Screenshots neu aufnehmen
./gradlew :apps:<app>:verifyRoborazziDebug                # gegen apps/<app>/screenshots prüfen
```
Läufe über 8 min per `xapient-job` starten. Speicher am NAS ist knapp: Gradle-Heap 1,5 GB, Test-Heap 1 GB, ein Test-Fork, 2 Worker.
Beim Feinschliff nur einzelne Zustände aufnehmen: `recordRoborazziDebug --tests '*LevelScreenshotTest.edge_tilted'`. Volle Matrix nur am Schluss.
Robolectric läuft mit SDK 34 (`src/test/resources/robolectric.properties`), weil SDK 35+ Java 21 braucht.

## Neue App
1. `apps/<name>/` mit `SPEC.md` (zuerst), `version.txt`, `build.gradle.kts` (`aapps.app` + Roborazzi), Eintrag in `settings.gradle.kts`.
2. Fachlogik in `domain/` ohne Android-Imports; Sensoren/Audio/Uhr über Interfaces.
3. Screenshot-Zustände aus der SPEC als Roborazzi-Tests, PNGs in `apps/<name>/screenshots/` committen.
4. Berechtigungen: Standard ist keine. Freigaben pro App über `aapps { allowedPermissions.add("…") }`, INTERNET ist immer verboten (`check<Variant>ManifestPermissions`).

## Regeln
- Erlaubt: Compose BOM, activity, lifecycle, core-ktx, kotlinx-serialization (nur wo nötig).
- Ausnahme: androidx.webkit nur für moovr (WebViewAssetLoader für die Spiel-Assets).
- Verboten: Hilt, Navigation, Room, Firebase, Play Services, INTERNET.
- Code erst beim zweiten Nutzer in `core/` verschieben.
- Keystore nie ins Repo. Signierung nur über Umgebungsvariablen (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD_<APP>`, Alias = App-Name), sonst Debug-Schlüssel. Mit `-Paapps.requireReleaseKey=true` (CI bei Push auf main/Tag) bricht der Build ohne Schlüssel ab.
- Kommentare einzeilig und kurz, Commits klein, Identität `valueerrorx`.
- Design-Regeln: `docs/DESIGN.md`.
