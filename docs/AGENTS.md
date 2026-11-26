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
   Ausnahme: VIBRATE kommt aus `core/ui` in jede App und ist immer frei (normale Berechtigung, keine Rückfrage).
   Ebenso `com.android.vending.BILLING` aus `core/billing`. INTERNET und ACCESS_NETWORK_STATE, die Play Billing über datatransport mitbringt, entfernt `core/billing` per `tools:node="remove"`; der Check bricht ab, falls sie wieder auftauchen.
5. Testphase + Kauf: `setContent { TrialGated(<App>Accent) { <App>App() } }` (kommt über `core/ui`). Einstiege ohne Activity (Kachel, Widget) vorher `TrialGate.expired(context)` prüfen.

## Testphase und Kauf (`core/billing`)
- Alle Werkzeug-Apps: 24 h ab erstem Start frei, danach Dankes-Bildschirm (`core/ui/ThanksScreen`) mit einmaligem Kauf, Produkt-ID `full_unlock` (nicht verbrauchbar). moovr und glidr: kein Billing.
- Entscheidung als reine Funktion `TrialPolicy.decide` (Unit-Tests), Play Billing nur in `PlayPurchases`, Zustand lokal in SharedPreferences `aapps_billing`.
- Tests: `TrialGate.factory` durch einen Fake (`PurchaseSource`) ersetzen, siehe `levlr/TrialGatedTest`.
- Nur Debug-Builds: `adb shell am start -S -n solutions.xapient.<app>/.MainActivity --es aapps_trial expire` (Testphase vorbei) bzw. `reset`. Im Release fehlt der Schalter (`core/billing/src/release`).
- sleepr (Views): Dankes-Bildschirm als Views in `Pages.thanks`, gleiche Texte aus `core/billing`.

## Haptik (`core/ui/Haptics`)
- `tick()`/`confirm()`: Ereignis (Fund, Einrasten, gestimmt, Ablauf). Läuft über den Vibrator (Usage Alarm), spürbar auch bei ausgeschalteter Touch-Haptik im System, entprellt auf 120 ms.
- `step()`: Raster beim Ziehen, Wechsel im Segment. `press()`: Tasten. Beide über die View, folgen der System-Touch-Haptik.
- Faustregel: Folgt die Rückmeldung direkt auf einen Finger, dann `step`/`press`.

## iOS (KMP, erst levlr)
- Aufbau, CI und Store-Schritte: `iosApp/README.md`. Gebaut wird nur in GitHub Actions (`.github/workflows/ios-<app>.yml`), am NAS nur die Android- und JVM-Seite.
- App mit iOS: Code nach `apps/<app>/shared` (Plugin `aapps.kmp`, Framework `AappsKit`), `apps/<app>` bleibt dünner Android-Einstieg (AGP 9 erlaubt KMP nicht im App-Modul). Texte in `shared/src/commonMain/composeResources`.
- `core/ui` ist KMP; Android-Ressourcen in `src/androidMain/res` sind zugleich die CMP-Ressourcen (`Res`), andere Apps nutzen weiter `R.string.aapps_*`.
- Kauf auf iOS: `core/store` (StoreKit 2 über `iosApp/common/StoreKitBridge.swift`), Testphase = Gratis-Kauf `<bundle>.trial`, Freischalten `<bundle>.full_unlock`. Tests `:core:store:jvmTest`.
- Plattformteile per `expect`/`actual` nur wo nötig (Sensor, Haptik, Drehung, Schriften, Speicher).

## Regeln
- sleepr ist Views/Canvas statt Compose und nutzt `core/ui` nicht; nicht auf Compose umbauen. Haptik dort direkt über die View (`KEYBOARD_TAP` wie `press()`).
- Erlaubt: Compose BOM, activity, lifecycle, core-ktx, kotlinx-serialization (nur wo nötig).
- Ausnahme: androidx.webkit nur für die Spiele moovr und glidr (WebViewAssetLoader für die Spiel-Assets).
- Ausnahme: CameraX (camera-camera2, -lifecycle, -view) und ZXing core nur für dividr (QR erzeugen und lesen), ohne ML Kit/Play Services.
- Ausnahme: Google Play Billing Library (`com.android.billingclient:billing`) nur in `core/billing`, von Thomas freigegeben. Bringt transitiv Play-Services-Teile und datatransport mit; nichts davon direkt nutzen.
- Verboten: Hilt, Navigation, Room, Firebase, Play Services (außer transitiv über Billing), INTERNET.
- Code erst beim zweiten Nutzer in `core/` verschieben.
- Keystore nie ins Repo. Signierung nur über Umgebungsvariablen (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD_<APP>`, Alias = App-Name), sonst Debug-Schlüssel. Mit `-Paapps.requireReleaseKey=true` (CI bei Push auf main/Tag) bricht der Build ohne Schlüssel ab.
- Kommentare einzeilig und kurz, Commits klein, Identität `valueerrorx`.
- Design-Regeln: `docs/DESIGN.md`.
