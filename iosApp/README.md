# aapps für iPhone und iPad

levlr ist die erste App und die Vorlage für alle weiteren, danach nordr und spinr. Gebaut wird in GitHub Actions auf macOS (`.github/workflows/ios.yml`, eine Matrix über alle `iosApp/<app>/project.yml`), ein eigener Mac ist nicht nötig.

## Aufbau

```
core/ui            KMP: Theme, Bausteine, Einstellungen, Info, Dankes-Layout (Android + iOS)
                   androidMain: Haptik/Vibrator, TrialGated (Google Play), ThanksScreen
                   iosMain:     Haptik (UIKit), StoreGated + StoreScreen (App Store), Schriften, UserDefaults
                   alle: Display-Rotation, Zurück-Taste, Dezimaltrenner (DisplayRotationEffect, BackEffect, decimalSeparator)
core/store         KMP (iOS + JVM): Testphase/Kauf-Logik für den App Store (StorePolicy, StoreGate), Tests auf der JVM
core/billing       nur Android (Google Play), unverändert
apps/<app>/shared  KMP: Domain + komplette Oberfläche, Texte in composeResources (DE/EN)
                   iosMain: Sensor (CoreMotion), MainViewController, Screenshot-Szenen
apps/levlr         Android-App, nur noch Einstieg (MainActivity, Sensor, Debug-Diagnose)
iosApp/common      Swift, für alle Apps gleich: StoreKitBridge (StoreKit 2)
iosApp/<app>       XcodeGen-Projekt (project.yml), SwiftUI-Hülle, Info.plist, PrivacyInfo, Icons, .storekit, UI-Tests, appstore/
```

- Kotlin-Framework heißt in jeder App `AappsKit` (statisch, per `embedAndSignAppleFrameworkForXcode`, ohne CocoaPods).
- StoreKit 2 ist nur aus Swift erreichbar: `StoreKitBridge.swift` implementiert das Kotlin-Interface `StoreBridge`, die Entscheidung (Angebot/Testphase/frei/abgelaufen) trifft `StoreGate` in Kotlin.
- Xcode-Projekt per XcodeGen statt handgeschriebener `.pbxproj`: lesbar, diff-bar, ohne Mac nicht fehleranfällig zu pflegen. Das erzeugte `levlr.xcodeproj` ist nicht im Repo.
- Version wie Android aus `apps/<app>/version.txt`, Build-Nummer = `github.run_number + 100` (steigt je Lauf, damit je App monoton; alte Builds gingen bis 10).
- CI: Push mit eigenen Aenderungen (`apps/<app>/`, `iosApp/<app>/`) = Kotlin-Tests + TestFlight-Upload; Aenderungen an core/Build/iosApp/common = nur Kotlin-Tests aller Apps, kein Upload. UI-Tests und Store-Screenshots nur per Handstart (`workflow_dispatch`, screenshots=true).

## Testphase und Kauf auf iOS (Apple 3.1.1)
- Zwei Non-Consumables: `solutions.xapient.levlr.trial` (Preis 0, „1-Day Trial“) und `solutions.xapient.levlr.full_unlock` (1,49 €).
- Erster Start: Angebots-Bildschirm mit Dauer (1 Tag), was danach gesperrt ist, und Preis. „Testphase starten“ kauft das Gratis-Produkt; Startzeit wird lokal gespeichert (und aus der Transaktion übernommen).
- Bei jedem Start und jeder Rückkehr in die App: `Transaction.currentEntitlements`. Gekauft → lokal gecacht, auch offline frei.
- Nach 24 h ohne Kauf: Dankes-Bildschirm (Texte wie Android) mit lokalisiertem StoreKit-Preis, „Kauf wiederherstellen“ (`AppStore.sync()`), Open-Source-Link.
- `Transaction.updates` wird die ganze Laufzeit gehört, jede Transaktion mit `finish()` abgeschlossen.
- Debug-Builds: Startargument `-aappsTrial expire` bzw. `reset` (Release ignoriert es). Lokal im Simulator: Scheme nutzt `levlr.storekit`.

## CI (GitHub Actions)
Bei jedem Push auf `feat/ios` je betroffener App (eigene Dateien unter `apps/<app>` bzw. `iosApp/<app>`; core/ui, core/store, iosApp/common, Build-Dateien betreffen alle). Von Hand: workflow_dispatch mit `app` (leer = alle).
1. Kotlin-Tests auf JVM und iOS-Simulator
2. `xcodegen generate`, Simulator-Build, UI-Tests (Start + alle Screenshot-Szenen)
3. Nur wenn die Secrets gesetzt sind und die App in App Store Connect existiert: IAP-Review-Screenshots, App-Screenshots, Archiv, Signierung (automatisch, per API-Key), Upload zu TestFlight

Keine Build-Artefakte als GitHub-Artifact (öffentliches Repo). Der API-Key liegt nur während des Schritts in `$RUNNER_TEMP`.
Secrets: `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8` (Inhalt der .p8-Datei), `APPLE_TEAM_ID`.

## Deine Schritte (Thomas)

Begriffe aus App Store Connect bzw. developer.apple.com auf Englisch. Mit [prüfen] markierte Menüpfade sind nicht sicher belegt, die Namen können leicht abweichen.

1. **Paid Apps Agreement** (Pflicht für In-App-Käufe): App Store Connect → **Business** (früher „Agreements, Tax, and Banking“) → **Paid Apps** → Bankkonto, Steuerformulare (als Privatperson außerhalb der USA: W-8BEN) und Kontakte ausfüllen. Bis der Status „Active“ ist, liefert StoreKit keine Produkte (Preis fehlt in der App).
2. **API-Key** (damit CI hochladen und ich vieles per API erledigen kann): App Store Connect → **Users and Access** → **Integrations** → **App Store Connect API** → **Team Keys** → **+** (Generate API Key). Name z. B. `aapps-ci`, Access **Admin** (automatisches Signieren in der CI legt Zertifikate/Profile an; dafür reicht „App Manager“ nicht sicher [prüfen]). Die `.p8` lässt sich nur einmal laden. Notieren: **Key ID**, **Issuer ID** (oben auf der Seite). Team ID: developer.apple.com → **Account** → **Membership details** → Team ID.
   Ablage am NAS: `~/secrets/apple/AuthKey_<KEYID>.p8` und `~/secrets/apple/asc.env` mit `ASC_KEY_ID=…`, `ASC_ISSUER_ID=…`, `APPLE_TEAM_ID=…` (Rechte 600). Ich gebe die Werte nie aus und setze daraus die GitHub-Secrets.
3. **Bundle ID registrieren** (kann ich mit dem API-Key übernehmen): developer.apple.com → **Certificates, Identifiers & Profiles** → **Identifiers** → **+** → **App IDs** → **App** → Description `levlr`, **Explicit** Bundle ID `solutions.xapient.levlr`. In-App Purchase ist standardmäßig aktiv.
4. **App anlegen** (erledigt für levlr: „levlr – Spirit Level“; geht nicht per API): App Store Connect → **Apps** → **+** → **New App**: Platform **iOS**, Name (im ganzen App Store eindeutig), Primary Language **English (U.S.)**, Bundle ID `solutions.xapient.levlr`, SKU `levlr`, User Access **Full Access**.
5. **In-App-Käufe** (für levlr per API angelegt: Texte, Preis, alle Länder; fehlt nur der Review-Screenshot): in der App → **Monetization** → **In-App Purchases** → **+** [prüfen] → Type **Non-Consumable**:
   - Reference Name `levlr Unlock`, Product ID `solutions.xapient.levlr.full_unlock`, **Price Schedule**: Base Country/Region **Austria**, Preis **1,49 €** (Apple hat seit 2023 keine „Tiers“ mehr, sondern Preispunkte; 1,49 € sollte es geben, sonst den nächsten wählen [prüfen]).
   - Reference Name `levlr 1-Day Trial`, Product ID `solutions.xapient.levlr.trial`, Preis **Free** (0,00).
   - Je **App Store Localization** EN + DE (Texte in `levlr/appstore/README.md`), **Availability** alle Länder, **Review Information** mit Screenshot (Angebots- bzw. Dankes-Bildschirm).
6. **App Privacy**: „No, we do not collect data“ → „Data Not Collected“; Privacy Policy URL siehe `levlr/appstore/README.md`. **Age Rating**: alles „None/No“, nur In-App Purchases „Yes“ → 4+. **Pricing and Availability**: Price **Free**.
7. **TestFlight**: Nach dem nächsten CI-Lauf mit Secrets erscheint der Build in App Store Connect → **TestFlight**. Unter **Internal Testing** eine Gruppe anlegen, dich hinzufügen, auf iPhone/iPad die App **TestFlight** installieren und levlr laden. Käufe in TestFlight sind Sandbox-Käufe und kosten nichts. Exportverschlüsselung ist per `ITSAppUsesNonExemptEncryption = NO` schon beantwortet.
8. **Einreichen**: App Store Connect → App → **iOS App** Version `0.1.0` → Texte (aus `levlr/appstore/metadata`, kann ich per API füllen), Screenshots 6.9" + 13", **Build** auswählen, unter **In-App Purchases** beide Käufe hinzufügen (die ersten IAPs gehen zusammen mit einer App-Version in die Prüfung), **App Review Information** → Notes aus `metadata/review_information/notes.txt`, dann **Add for Review** → **Submit for Review**.

Stand levlr: Texte, Kategorie, Altersfreigabe (4+), Preis (gratis), Verfügbarkeit, Build, IAPs und Screenshots sind per API gesetzt. Noch von Hand in App Store Connect: **App Privacy** (die API kennt diesen Fragebogen nicht) und eine **Telefonnummer** für den Review-Kontakt (Apple-Pflicht).

## Was ich mit dem API-Key automatisieren kann
- Bundle ID registrieren, In-App-Käufe anlegen (inkl. Preis, Lokalisierung, Verfügbarkeit, Review-Screenshot)
- Store-Texte DE/EN, Keywords, URLs, Altersfreigabe-Fragebogen
- Screenshots hochladen, Build der Version zuweisen, Einreichung anstoßen
- Nicht per API: App anlegen, Paid Apps Agreement/Steuer/Bank, App-Privacy-Fragebogen [prüfen]

## Optional: lokal am Mac
JDK 17, Xcode (neueste), Android SDK (Gradle konfiguriert das ganze Repo; `ANDROID_HOME` oder `local.properties`), `brew install xcodegen`. Dann
```sh
cd iosApp/levlr
AAPPS_VERSION=$(cat ../../apps/levlr/version.txt) AAPPS_BUILD=1 xcodegen generate
open levlr.xcodeproj   # Scheme levlr, Simulator wählen, Run (nutzt levlr.storekit)
```
Signieren: Target **levlr** → **Signing & Capabilities** → **Automatically manage signing**, Team wählen.

## Einschränkungen auf iOS (levlr)
- **Haptik**: iOS folgt immer der Systemeinstellung „Systemhaptik“. Auf Android vibriert das Einrasten auch bei ausgeschalteter Touch-Haptik (Wecker-Kategorie); das lässt iOS nicht zu. iPads haben keine Taptic Engine, dort gibt es kein Einrast-Feedback, nur Farbe und Glanz.
- **Sensor**: CoreMotion Device Motion (fusionierte Schwerkraft, 60 Hz) entspricht dem Android-Gravity-Sensor; ohne Device Motion der Beschleunigungssensor. Keine Berechtigung nötig, daher kein Hinweistext in der Info.plist. Der Simulator hat keinen Sensor (zeigt „kein Lagesensor“, Screenshots über feste Szenen).
- **Drehung**: Die Bildschirm-Ausrichtung wird alle 250 ms gelesen (180°-Drehungen ändern die Fenstergröße nicht). iPhones mit Face ID drehen die Oberfläche nicht auf den Kopf; die Kante-Messung stimmt trotzdem, weil sie aus der Schwerkraft rechnet. Zuordnung der Ausrichtung am Gerät prüfen (offener Punkt).
- **Testphase**: Muss auf iOS aktiv gestartet werden (Gratis-Kauf), nicht automatisch ab dem ersten Start. Sie hängt am Apple Account: Neuinstallation setzt sie nicht zurück, ein anderes Gerät mit demselben Account sieht denselben Stand. Wer die Geräteuhr zurückstellt, verlängert die Testphase; bewusst kein Schutz dagegen (wie Android). Ein Kauf geht nur bei belegter Erstattung verloren, nicht offline oder nach einem Gerätewechsel.
- **Zurück**: Keine System-Zurück-Taste, zurück geht es über den Pfeil oben links.
- **Diagnose** (Sensor-CSV aufzeichnen) gibt es nur im Android-Debug-Build.
- **Sicherung**: Einstellungen und Kalibrierung liegen in den UserDefaults und können über iCloud-/Gerätesicherung auf ein neues Gerät wandern (Android: Sicherung aus). Nach einem Gerätewechsel neu kalibrieren (bewusst nicht ausgeschlossen).

## Einschränkungen auf iOS (nordr)
- **Sensor**: CoreMotion Device Motion mit Bezug „magnetisch Nord“ (Gyro + Magnetometer + Schwerkraft fusioniert, wie der Android-Rotation-Vector), ohne Standort und ohne Berechtigung. Genauigkeit aus der Magnetometer-Kalibrierung von iOS (unkalibriert/gering → Kalibrierhinweis), Störung aus der Feldstärke wie auf Android. Vor der ersten Kalibrierung liefert iOS teils kein Feld, dann fehlt der Hinweis kurz. Der Simulator hat keinen Kompass (zeigt „kein Kompass-Sensor“, Screenshots über feste Szenen).
- **Deklination**: Android nutzt `GeomagneticField`; iOS hat kein Gegenstück ohne Standort, deshalb rechnet nordr dort mit dem eingebauten Weltmagnetfeld-Modell WMM2025 (NOAA, gemeinfrei, gültig bis Ende 2029) für die gewählte Region. Danach braucht es ein Update mit dem Nachfolgemodell.
- **Haptik**: wie levlr, folgt der Systemhaptik; iPads ohne Taptic Engine haben kein Tippen bei N/O/S/W und auf Kurs.
- **Drehung, Zurück, Sicherung**: wie levlr.

## Einschränkungen auf iOS (spinr)
- **Klick-Ton**: AVAudioPlayer (drei Player im Wechsel, leicht wechselnde Tonhöhe wie Android), Audio-Kategorie Ambient: der Stummschalter schaltet den Ton ab, andere Audio-Apps laufen weiter.
- **Bedienungshilfen**: Android sagt das Ergebnis über TalkBack an; ob VoiceOver das auf iOS ebenso tut, ist am Gerät zu prüfen (in der Store-Beschreibung deshalb nicht erwähnt).
- **Zeichen kürzen**: Einträge werden an Graphem-Grenzen gekürzt (Android BreakIterator, iOS NSString composed character sequences).
- **Drehung, Zurück, Sicherung**: wie levlr.

## Einschränkungen auf iOS (magnr)
- **Sensor**: CoreMotion Device Motion (kalibriertes Magnetfeld in µT, 50 Hz) statt des Android-Magnetsensors; die Lage kommt aus Gyro + Schwerkraft mit Magnetometer-Korrektur und wird für den Feld-Ausgleich beim Drehen genutzt wie auf Android. iOS nennt keinen Messbereich, die Sättigung wird daher nur an einer stehen bleibenden Achse erkannt. Keine Berechtigung nötig. Der Simulator hat kein Magnetometer (zeigt „kein Sensor“, Screenshots über feste Szenen).
- **Ton**: AVAudioEngine mit derselben Ton-Synthese wie Android, Audio-Kategorie Ambient (Stummschalter gilt, Musik läuft weiter).
- **Haptik, Drehung, Zurück, Sicherung**: wie levlr.

## Einschränkungen auf iOS (sleepr)
- **Oberfläche**: Auf Android ist sleepr bewusst mit Views gebaut (SPEC.md). Für iOS gibt es dieselbe Oberfläche in Compose (`apps/sleepr/shared`), die Signalverarbeitung (`domain/Dsp.kt`) ist dieselbe Datei. Die Android-App hängt nicht am KMP-Modul.
- **Audio**: AVAudioEngine mit vier Puffern zu je 4096 Frames (rund 340 ms, Android ≥ 300 ms), Rechenweg wie `NoiseEngine` auf Android. Kategorie Playback mit Hintergrund-Audio (`UIBackgroundModes` audio) statt Vordergrunddienst; Stopp am Sperrbildschirm über die Mediensteuerung statt Benachrichtigung, darum keine Berechtigung.
- **Audio-Fokus**: Unterbrechung (Anruf, Wecker, Siri) stoppt wie auf Android; abgezogene Kopfhörer stoppen weich (Apple-Vorgabe).
- **Lautstärke**: Die Hardware-Tasten regeln die Systemlautstärke, der Regler in der App die Mischung (wie Android).
- **Zurück, Sicherung**: wie levlr.

## Einschränkungen auf iOS (scorr)
- **Fehler-Haptik** (Tippen, wenn nichts geht): UINotificationFeedbackGenerator „Error“, folgt der Systemhaptik; iPads ohne Taptic Engine bleiben still.
- **Zurück, Sicherung**: wie levlr.

## Einschränkungen auf iOS (focusr)
- **Uhr**: iOS hat keine öffentliche Laufzeituhr, die im Ruhezustand weiterläuft; focusr rechnet mit der Wanduhr. Wer während eines Timers die Uhrzeit ändert, verschiebt das Ende (dafür übersteht ein Timer einen Neustart).
- **Hintergrund**: Kein Dauer-Hinweis mit Restzeit (Android: Countdown-Benachrichtigung). Das Ende kommt als lokale Mitteilung mit Glocke, aber nur mit Mitteilungsrecht; ohne Recht klingelt focusr nur bei offener App. Ob die Mitteilung vibriert, bestimmen die iOS-Einstellungen.
- **Ton**: Bei offener App spielt focusr die Glocke selbst (Kategorie Playback mit Ducking, klingt auch bei Stummschalter).
- **Zurück, Sicherung**: wie levlr.

## Einschränkungen auf iOS (hudr)
- **Standort**: CoreLocation nur „Beim Verwenden der App“, kein Hintergrund. Ist nur der ungefähre Standort erlaubt, fragt der Knopf einmal pro Sitzung nach dem genauen (temporäre volle Genauigkeit); gesperrt oder Ortungsdienste aus → Knopf öffnet die App-Einstellungen (zu den Ortungsdiensten selbst lässt iOS nicht springen, der Hinweistext sagt das).
- **Satelliten**: iOS nennt keine Satellitenzahl, der GPS-Status zeigt nur die Genauigkeit.
- **HUD-Modus**: volle Helligkeit auf Wunsch (danach der vorige Wert); Statusleiste und Drehung bleiben wie im normalen Modus.
- **Geräte**: `UIRequiredDeviceCapabilities` gps, also iPhones und iPads mit Mobilfunk.
- **Ton**: Doppelpiep über AVAudioPlayer, Audio-Kategorie Ambient (Stummschalter gilt).
- **Haptik, Zurück, Sicherung**: wie levlr.

## Spiele (moovr, glidr)
Gratis, ohne Kauf und ohne Kotlin: je App eine eigene Swift-Hülle `WebGame.swift` (WKWebView, eigenes Schema `aapps://game/`, nur Dateien aus dem Bundle) um dieselben Phaser-Dateien wie Android (`apps/<app>/src/main/assets`). Kein `apps/<app>/shared`, der Workflow überspringt dann den Kotlin-Test der App und die IAP-Screenshots (kein `.storekit`).
- **Ton**: WebKit auf iOS spielt kein Ogg; dieselben Klänge liegen als AAC in `iosApp/<app>/ios-assets`, ein Skript lädt `.m4a` statt `.ogg`. Phaser sperrt Audio auf dem iPhone bis zum ersten Tippen, die Musik kann also erst dann einsetzen; der Stummschalter schaltet das Spiel stumm.
- **Zurück**: iOS hat keine Zurück-Taste, die Spiele haben eigene Menü-Knöpfe.
- **Pause**: beim Wechsel in den Hintergrund `<app>Pause()`, danach `<app>Resume()` wie auf Android.
- **Absturz des Web-Prozesses**: neu laden, höchstens einmal pro 30 s.
- **Spielstand**: localStorage der WebView (bleibt bei Updates, geht beim Löschen der App verloren).

## Einschränkungen auf iOS (hertzr)
- **Audio**: AVAudioEngine mit vier Puffern zu je 512 Frames bei fester Rate 48 kHz (rund 43 ms, der Mixer passt an die Hardware an); derselbe `ToneRenderer` wie auf Android, Obergrenze damit immer 22 kHz. Hintergrund-Audio statt Vordergrunddienst, Anzeige und Stopp am Sperrbildschirm statt Benachrichtigung, darum keine Berechtigung.
- **Schutz**: Kopfhörer über die Audio-Route (kabelgebunden, Bluetooth), Stopp bei abgezogenem Kopfhörer und bei Unterbrechung wie `AudioGuard` auf Android.
- **Haptik**: Anschlag am Bandende = Fehler-Haptik (`UINotificationFeedbackGenerator`), sonst wie levlr.
- **Zurück, Sicherung**: wie levlr.

## Einschränkungen auf iOS (dividr)
- **QR-Code erzeugen**: Android nutzt ZXing, iOS einen eigenen Encoder in Kotlin (`apps/dividr/shared/.../qr/QrEncoder.kt`, ECC M, Strafpunkte nach Norm). Auf der JVM gegen ZXing geprüft: gleiche Version, bei gleicher Maske Bit für Bit gleiche Matrix, zurücklesbar bis Version 40. Die gewählte Maske kann vereinzelt von ZXing abweichen (beide gültig).
- **QR-Code lesen**: AVFoundation (`AVCaptureMetadataOutput`, 1080p) in Swift (`iosApp/dividr/dividr/DividrHost.swift`), eingebettet per `UIKitView`. Ob sehr dichte Codes (große Gruppen, Version 30–40) so zuverlässig gelesen werden wie mit ZXing auf Android, ist am Gerät zu prüfen.
- **Kamera-Erlaubnis**: Systemdialog beim ersten Scannen; nach Ablehnung führt „Einstellungen öffnen“ in die App-Einstellungen.
- **Komprimieren**: System-zlib (raw Deflate, Stufe 9) wie `java.util.zip` auf Android; die Codes sind zwischen beiden Plattformen austauschbar.
- **Zahlen/Währungen/Datum**: NSNumberFormatter/NSDateFormatter statt java.text; Nachkommastellen je Währung nach ISO 4217 wie `java.util.Currency` (Apples Formatter weicht teils ab, z. B. Bargeld-Stellen). Darstellung kann sich minimal unterscheiden (z. B. Leerzeichen vor €).
- **Teilen**: Systemblatt (`UIActivityViewController`), auf dem iPad als Popover in der Bildschirmmitte.
- **Helligkeit**: Solange der eigene QR-Code angezeigt wird, volle Bildschirmhelligkeit; beim Verlassen zurück.
- **Sicherung**: Gruppen (Application Support) und Geräte-ID (UserDefaults) wandern mit iCloud-/Gerätesicherung (Android: Sicherung aus). Wer eine Sicherung auf ein zweites Gerät spielt und beide weiter nutzt, hat zweimal dieselbe Geräte-ID; dann können neue Einträge der beiden Geräte kollidieren. Bewusst nicht ausgeschlossen (Normalfall: altes Gerät wird ersetzt).
- **Drehung, Zurück**: wie levlr.

## Einschränkungen auf iOS (thinkr)
- **Töne**: AVAudioPlayer je Klang über die Ambient-Audiosession (Stummschalter gilt, andere Musik läuft weiter); Android SoundPool.
- **Bewegung reduzieren**: „Bewegung reduzieren“ in den Bedienungshilfen hält die Deko-Animationen an (Android: Animationsskala 0).
- **Datum**: Tageswechsel nach lokaler Zeitzone wie Android; Datumszeile und Wochentagsbuchstaben über NSDateFormatter (gleiches Muster wie Android).
- **Drehung, Zurück, Sicherung**: wie levlr.
