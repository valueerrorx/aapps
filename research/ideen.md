# aapps – 10 kleine Android-Apps im Stil von sleepr

Stand: 2026-11-21. Recherche: Websuche + Tracker-Seiten (AppGoblin, AppBrain, Exodus, Play-Listings). Installzahlen sind Schätzungen dieser Drittanbieter, keine offiziellen Google-Zahlen, aber in der Größenordnung belastbar. Reddit war für die Recherche nicht abrufbar (Crawler-Sperre), Reddit-Belege fehlen daher. Nachfrage wird stattdessen über Installzahlen, Bewertungsanzahl und Monetarisierungsart der Marktführer belegt.

Referenz sleepr: ein Zweck (Rauschgenerator), live am Gerät berechnet, offline, keine Abhängigkeiten, fast keine Berechtigungen, schöne Echtzeit-Visualisierung (Spektrum, Wellen-Regler), GPLv3, APK über GitHub Releases.

## 1. Kurzfazit

- Die gefragtesten Einzweck-Werkzeuge (Tuner, Metronom, Kompass, Wasserwaage, Schallpegel, Intervall-Timer, Glücksrad, Scoreboard) haben 3 bis 40 Mio. Installationen pro Marktführer und finanzieren sich fast vollständig über Werbung (oft „Ad 100 %“).
- Genau dort liegt die Chance: gleiche Funktion, aber schön, werbefrei, ohne Internet-Berechtigung, also nachprüfbar privat. Es gibt FOSS-Alternativen (F-Droid), die sind aber meist optisch schlicht.
- Alle 10 Ideen laufen komplett offline, ohne Account und ohne API-Key. Höchstens Mikrofon oder Kamera ist nötig, keine heiklen Berechtigungen.
- Jede App hat einen „Hero-Moment“ (Animation/Physik/Sensor-Visualisierung), der sie in Screenshots sofort von der Werbe-Konkurrenz abhebt.
- Hürden: Play-Store-Neukonto braucht 12 Tester × 14 Tage Closed Test pro App. Für reine APK-Verteilung kommt ab 2027 weltweit die Entwickler-Verifizierung beim Sideloading (siehe Risiken).

## 2. Longlist (~25 Kandidaten)

Format: Zweck – Nachfrage-Beleg [Quelle-Nr.] – Schwäche der Konkurrenz – Scope

1. **Instrumenten-Tuner (chromatisch)**: DaTuner 39,9 Mio. Installs, 456k Bewertungen [1], GuitarTuna 100 Mio.+ [2]. Konkurrenz: DaTuner 59 % Werbung, GuitarTuna drängt aufs Abo, Alt-Stimmungen nur im Abo [3]. Scope M
2. **Metronom**: Metronome Beats ~28 Mio. [4]. Konkurrenz: AppLovin+AdMob-SDKs [5], Beschwerden über störende Werbung bei Metronomen [6]. Scope S
3. **Wasserwaage/Neigungsmesser**: NixGame 8,5 Mio. (Ad 100 %) [7], Gamma Play 10 Mio.+ [8]. Konkurrenz: reine Werbe-Apps. Scope S
4. **Kompass**: Smart Compass 35 Mio. (Ad 100 %) [9]. Konkurrenz: Werbung, oft unnötige Standort-Berechtigung. Scope S
5. **Schallpegelmesser (dB)**: Abc Apps Sound Meter 24,5 Mio., Werbung + IAP [10]. Konkurrenz: Werbung und Kauf-Upsell. Scope S
6. **Intervall-/HIIT-Timer**: Interval Timer 12,1 Mio., Bewertung 4,93, Werbung + IAP [11]. FOSS-Alternativen gibt es, aber spartanisch [12]. Scope S–M
7. **Glücksrad/Entscheidungsrad**: Spin The Wheel 13,8 Mio., Werbung + IAP [13], AppBrain stuft es mit „High Maturity“ ein (wegen Werbung) [14]. Scope S
8. **Scoreboard/Punktezähler**: Scoreboard – Track Score 3 Mio., Werbung + IAP [15], Score Counter 765k [16]. Scope S
9. **Visueller Timer (Time-Timer-Stil)/Fokus-Timer**: Focus To-Do 11,8 Mio. (IAP) [17]. Time Timer ist auf Abo umgestiegen, Nutzer beschweren sich [18]. Scope S
10. **QR-/Barcode-Scanner**: Gamma QR & Barcode Scanner 861,6 Mio. Installs, Werbung [19]. Kategorie mit Malware- und Adware-Vorgeschichte [20][21]. Scope S–M
11. **Countdown „Tage bis …“ mit Widget**: Days Until ~1,58 Mio., „ads can get annoying“ [22], Days Matter 1 Mio.+, Widget aktualisiert nicht [22]. Scope S–M
12. **Lupe (Kamera + Licht)**: App2U Magnifier + Flashlight 10 Mio.+ [23]. Konkurrenz: Werbung. Scope S
13. **AR-/Lineal-Messen**: Smart Measure 44,5 Mio., aber nur 3,15 Sterne (Ad 100 %) [24]. Präzise Messung ist schwierig. Scope L
14. **Habit-Tracker**: Loop 5–7,6 Mio., FOSS, werbefrei [25]. Starke FOSS-Konkurrenz existiert schon. Scope M
15. **Pomodoro-Timer**: Focus To-Do 11,8 Mio. [17], viele Abo-Apps. Scope S (in Nr. 9 integrierbar)
16. **Flip-Clock/Nachttischuhr**: FlipClock (FOSS) ~1 Mio. [26], Flip Clock Timer ~636k, Ad 100 % [26]. Scope S
17. **Teleprompter**: Teleprompter.com ~572k Android, iOS 5 Mio.+, Wasserzeichen ohne Abo, $89/Jahr [27][28]. Scope M
18. **Finger-Chooser (Wer fängt an?)**: Chwazi „10M+ people“ laut Hersteller (plattformübergreifend, unbelegt) [29]. Scope S
19. **Tally-Counter/Zähler**: Marcel Bochtler 500k+ [30], viele Klone mit Werbung. Nachfrage eher mittel. Scope S
20. **Atemübungen (Box/4-7-8)**: viele Abo-Apps, Beschwerden über Abo und Paywall [31]. Android-Zahlen nicht belegt. Gesundheits-Claims vermeiden. Scope S
21. **Mondphasen**: Moonly ~4 Mio. (Entwickler gesamt) [32], Phases of the Moon mit Werbung [33]. Offline berechenbar. Scope S
22. **Lichtmesser (Lux/Belichtung)**: Lux Light Meter PRO ~514k [34]. Nische. Scope S
23. **Schachuhr/Spieluhr**: beste Treffer 500k+ [35]. Nische. Scope S
24. **Würfel/Zufallsgenerator**: Teil der Glücksrad-Kategorie, einzeln schwächer belegt. Scope S
25. **Taschenlampe**: in Android eingebaut, sinkender Bedarf, FOSS vorhanden [36]. Nicht empfohlen.

## 3. Top 10 (Auswahl und Begründung)

Kriterien-Gewichtung: belegte Nachfrage (Installs) × werbe-/abo-verseuchte Konkurrenz × grafisches Potenzial × kleiner Scope × null Policy-Risiko. Ausgeschlossen bzw. zurückgestellt wurden: AR-Messen (Scope L, schlechte Präzision), Habit-Tracker (Loop ist FOSS und gut), Teleprompter (geringere Android-Nachfrage), Taschenlampe (eingebaut), Atem-App (Gesundheits-Claim-Nähe). Countdown-Widget (Nr. 11) ist Ersatzkandidat Nr. 1.

Namensschema: wie sleepr, kleingeschrieben mit „-r“ statt „-er“ (Tumblr/Flickr-Stil). Namensprüfung = grobe Websuche, kein Markenrecherche-Ersatz.

---

### 1. Tuner: chromatischer Instrumenten-Stimmer
- **Problem/Nutzer:** Gitarre, Ukulele, Geige, Bass schnell stimmen. Hobby- und Schulmusiker. Riesige Nachfrage (DaTuner 39,9 Mio., GuitarTuna 100 Mio.+) [1][2]. Marktführer leben von Werbung oder drängen ins Abo [1][3].
- **MVP:** Live-Tonhöhe (YIN/MPM-Pitch-Detection, rein Kotlin), Note + Cent-Abweichung; Instrument-Presets (Gitarre Standard/Drop D/Ukulele/Violine/Bass), alle Stimmungen gratis; Kammerton A4 einstellbar (415–466 Hz); „gestimmt“-Feedback (Farbe + Haptik); optional Referenzton abspielen.
- **Grafik:** Strobe-Tuner-Look: ein rotierendes Ringmuster, das bei korrekter Stimmung stillsteht (wie Hardware-Strobe-Tuner). Darunter eine weiche Nadel mit Federphysik. Hintergrund-Glow wechselt von Rot über Bernstein zu Grün.
- **Permissions:** RECORD_AUDIO (nur im Vordergrund, kein Dienst).
- **Risiken/Policy:** keine. Mikrofon-Nutzung im Data-Safety-Formular deklarieren („nicht gespeichert, nicht übertragen“).
- **Komplexität:** M (Pitch-Detection robust für tiefe Bass-Saiten).
- **Namen:** `tunr` (Achtung: „Tunr“ ist ein iOS-Musikvisualizer von SoundSpectrum [37], kein Tuner), `pitchr`, `stringr`. **Favorit: tunr** (alternativ pitchr, falls die Kollision stört).
- **Icon:** Stimmgabel als zwei Linien, die in einem grünen Kreis zu einem Punkt zusammenlaufen.

### 2. Metronom
- **Problem/Nutzer:** Üben im Takt, Musikschüler und Bands. Metronome Beats ~28 Mio. [4], viele Werbe-SDKs [5], Werbung stört beim Üben [6].
- **MVP:** BPM 20–300 (Drehrad + Tap-Tempo); Taktarten, Betonung, Unterteilungen (Achtel/Triolen/Sechzehntel); sample-genaues Timing (Klicks direkt in den AudioTrack-Puffer gemischt, ohne Oboe-Abhängigkeit, wie die Engine von sleepr); Klangauswahl (Klick, Holz, Beep); läuft bei gesperrtem Bildschirm weiter (Foreground-Service mediaPlayback, wie sleepr); Presets/Setlist.
- **Grafik:** Pendel mit echter Schwingungsphysik und Bewegungsunschärfe, Beat-Punkte pulsieren, betonter Schlag als Ring-Welle. Optionaler Vollbild-„Blitz“-Modus für Drummer.
- **Permissions:** POST_NOTIFICATIONS, FOREGROUND_SERVICE(_MEDIA_PLAYBACK).
- **Risiken/Policy:** FGS-Typ deklarieren (mediaPlayback ist hier legitim) [38].
- **Komplexität:** S–M (Timing-Präzision ist der Knackpunkt, Audio-Engine aus sleepr wiederverwendbar).
- **Namen:** `pulsr`, `beatr`, `tempr`. Keine bekannte gleichnamige Android-App gefunden [39]. **Favorit: pulsr**.
- **Icon:** Pendel-Dreieck, dessen Spitze als Punkt nachleuchtet.

### 3. Wasserwaage / Neigungsmesser
- **Problem/Nutzer:** Bild aufhängen, Möbel ausrichten, Heimwerken. NixGame 8,5 Mio. mit Ad 100 % [7], Gamma Play 10 Mio.+ [8].
- **MVP:** Röhrenlibelle (1 Achse) + Dosenlibelle (2 Achsen); Gradanzeige mit Kalibrierung (Nullpunkt auf beliebiger Fläche); Hold/Lock-Taste; Signalton/Haptik bei 0,0°; Einheiten Grad/%/mm pro m.
- **Grafik:** Flüssigkeit mit echter Physik: Blase verformt sich, Flüssigkeit schwappt (Shader/Canvas-Spring-Simulation), Glas-Lichtreflexe. Bei 0° ein „Einrasten“-Glanzeffekt.
- **Permissions:** keine (Beschleunigungssensor).
- **Risiken/Policy:** keine. Hinweis „nicht für Vermessungsarbeiten“.
- **Komplexität:** S.
- **Namen:** `bubblr` (Achtung: ein neuer Play-Entwickler heißt „Bubblr“, eine Augen-Timer-App [40]), `levlr`, `flatr`. **Favorit: levlr**.
- **Icon:** grüne Glasröhre mit einer leuchtenden Blase exakt in der Mitte.

### 4. Kompass
- **Problem/Nutzer:** Wandern, Orientierung, Sonnenstand/Balkon-Ausrichtung prüfen. Smart Compass 35 Mio., Ad 100 % [9].
- **MVP:** Magnetischer Nordpfeil (Rotation-Vector-Sensor) mit Glättung; Kalibrierungs-Hinweis (Achter-Bewegung) bei schlechter Genauigkeit; Kursmarkierung festlegen (Peilung halten); wahrer Norden über manuell gewählte Region (Deklination via `GeomagneticField`, ohne Standort-Berechtigung); Hochformat und flach liegend.
- **Grafik:** physisch anmutende Kompassrose mit Trägheit und Nachschwingen (Feder-Dämpfung), Tiefenschatten, Glas-Spiegelung, die sich mit der Neigung bewegt.
- **Permissions:** keine (Standort bewusst weglassen, das ist ein USP).
- **Risiken/Policy:** keine. Hinweis „nicht für Navigation in kritischen Situationen“.
- **Komplexität:** S.
- **Namen:** `nordr`, `headr`, `bearr`. Keine Kollision gefunden [41]. **Favorit: nordr**.
- **Icon:** schlanke Kompassnadel, Nordhälfte in Markenfarbe, Südhälfte grau, auf dunklem Kreis.

### 5. Schallpegelmesser (dB)
- **Problem/Nutzer:** Lärm messen (Nachbarn, Baustelle, Kinderzimmer, Konzert, Klassenzimmer). Sound Meter 24,5 Mio. mit Werbung + IAP [10].
- **MVP:** Live-dB(A)-Schätzung mit A-Bewertung; Min/Ø/Max + Zeitverlauf (60 s); Referenz-Skala (Flüstern … Rockkonzert); Kalibrierungs-Offset; Sitzung als PNG/CSV teilen.
- **Grafik:** analoges VU-Meter mit Nadelphysik + scrollendes Spektrogramm (Wasserfall, aus der Spektrum-Darstellung von sleepr ableitbar), Farbskala grün → rot.
- **Permissions:** RECORD_AUDIO.
- **Risiken/Policy:** keine Gesundheitsversprechen („schützt dein Gehör“ vermeiden). Hinweis, dass Handy-Mikros nicht geeicht sind.
- **Komplexität:** S.
- **Namen:** `loudr` (Achtung: Loudr war ein Musik-Lizenzdienst, 2016 von Spotify übernommen, als App nicht aktiv; keine Sound-Meter-Kollision gefunden [42]), `decibr`, `noisr`. **Favorit: loudr**.
- **Icon:** drei konzentrische Schallbögen, der äußerste in Warnorange.

### 6. Intervall-Timer (HIIT/Tabata/Boxrunden)
- **Problem/Nutzer:** Training mit Arbeits- und Pausenphasen. Interval Timer 12,1 Mio., Werbung + IAP [11]. FOSS-Alternativen sind „no frills“ [12].
- **MVP:** Vorlagen (Tabata 20/10×8, EMOM, Boxen 3:00/1:00); eigene Sequenzen (Aufwärmen/Arbeit/Pause/Runden/Sätze); große Zahlen, Sprach-/Tonsignale, Countdown-Pieps; läuft gesperrt weiter mit Benachrichtigungs-Steuerung; Bildschirm an halten.
- **Grafik:** Vollbild-Farbfläche je Phase (Arbeit = Glut-Orange, Pause = Eis-Blau), die sich als Ring/Füllstand leert. Phasenwechsel als flüssiger Farb-Morph. Lesbar aus 3 m Entfernung.
- **Permissions:** POST_NOTIFICATIONS, FOREGROUND_SERVICE (Typ: mediaPlayback für Tonsignale oder specialUse). Kein Exact-Alarm nötig, solange der Dienst läuft.
- **Risiken/Policy:** FGS-Typ sauber begründen [38]; keinen „health“-Typ (der braucht Sensor-Berechtigungen). Keine Fitness- oder Gesundheitsversprechen.
- **Komplexität:** S–M.
- **Namen:** `roundr`, `intervalr`, `sweatr`. Keine Android-Kollision gefunden [43]. **Favorit: roundr**.
- **Icon:** Kreis, halb orange, halb blau, mit kleinem Spalt wie ein Timer-Ring.

### 7. Glücksrad / Entscheidungs-Spinner
- **Problem/Nutzer:** „Wer zahlt / was essen wir / wer ist dran“, Lehrer (Schüler aufrufen), Partys. Spin The Wheel 13,8 Mio., Werbung + IAP [13]. Werbung führt zu „High Maturity“-Einstufung [14].
- **MVP:** Rad aus Liste bauen (2–50 Einträge), mehrere Räder speichern; realistische Drehphysik (Wisch-Geschwindigkeit → Schwung → Reibung → Ticken an Stiften); Option „Gewinner entfernen“ (Reihenfolge auslosen); Ergebnis groß mit Konfetti; Zusatzmodus Münze/Würfel (optional v1.1).
- **Grafik:** Rad mit Tiefe, Ticker-Zunge biegt sich an jedem Segment, Haptik-Tick synchron, Motion Blur bei Speed. Palette aus der Markenfarbwelt automatisch.
- **Permissions:** keine.
- **Risiken/Policy:** nicht als Glücksspiel vermarkten (keine Einsätze, kein „Gewinn“-Wording mit Geld).
- **Komplexität:** S.
- **Namen:** `spinr`, `pickr`, `decidr` (Decidr existiert als kleine Android-App, 2026 [44]). Keine bekannte „Spinr“-App gefunden [44]. **Favorit: spinr**.
- **Icon:** buntes Tortenrad, leicht gekippt, mit kleinem Zeiger oben.

### 8. Scoreboard / Punktezähler für Spiele
- **Problem/Nutzer:** Spieleabend, Kartenspiele (Schnapsen, Uno, Rommé), Tischtennis, Kinderspiele. Scoreboard – Track Score 3 Mio., Werbung + IAP [15], Score Counter 765k [16].
- **MVP:** 2-Spieler-Vollbild (Tippen +1, Wischen −1) und Mehrspieler-Liste mit Runden-Eingabe; Spielerfarben/-namen merken; Rundenverlauf + Rückgängig; Zielpunktzahl mit Sieger-Animation; bleibt wach.
- **Grafik:** Flip-Ziffern wie bei einer Fallblattanzeige (Split-Flap) beim Zählen, satte Farbflächen pro Spieler, Sieger-Feuerwerk.
- **Permissions:** keine.
- **Risiken/Policy:** keine.
- **Komplexität:** S.
- **Namen:** `scorr`, `tallyr` (existiert als iOS-Scorekeeper [45]), `pointr`. **Favorit: scorr** (keine Kollision gefunden [45]).
- **Icon:** zwei Split-Flap-Ziffern „2:1“ in zwei Farben.

### 9. Visueller Fokus-Timer (Time-Timer-Prinzip)
- **Problem/Nutzer:** verbleibende Zeit „sehen“ statt Zahlen lesen: Lernen, Hausaufgaben, Meetings, Kochen, ADHS-Nutzer, Unterricht. Time Timer hat auf Abo umgestellt, Beschwerden in Reviews [18]. Pomodoro-Marktführer 11,8 Mio. [17].
- **MVP:** schrumpfende farbige Scheibe (bis 60/120 min), per Finger aufziehen; optionaler Pomodoro-Zyklus (25/5 ×4); sanfter Endton + Vibration; Vollbild-/Querformat-Modus fürs Klassenzimmer bzw. den Beamer; läuft gesperrt weiter.
- **Grafik:** die Scheibe als „Licht, das abnimmt“ mit weichem Verlauf, Rand-Glow und leichtem Pulsieren in der letzten Minute. Aufziehen per Drehgeste mit Raster-Haptik.
- **Permissions:** POST_NOTIFICATIONS, FOREGROUND_SERVICE (specialUse/shortService) oder AlarmManager-Ende über SCHEDULE_EXACT_ALARM bzw. USE_EXACT_ALARM.
- **Risiken/Policy:** Exact-Alarm-Permission ist deklarationspflichtig. Lieber laufenden FGS nutzen. Nicht als App „für Kinder“ oder „für ADHS“ einstufen: die Families-Policy und Gesundheits-Claims würden dann greifen. Zielgruppe „alle“.
- **Komplexität:** S.
- **Namen:** `focusr`, `slicr`, `dialr`. Keine Kollision gefunden [43]. **Favorit: focusr**.
- **Icon:** Kreis mit rotem Dreiviertel-Segment, wie eine Tortenuhr.

### 10. QR- & Barcode-Scanner ohne Internet
- **Problem/Nutzer:** QR-Codes (Speisekarten, WLAN, Tickets, Produkte) scannen und erzeugen. Größte Kategorie überhaupt: Gamma 861,6 Mio. Installs mit Werbung [19]. Lange Geschichte von Adware/Malware-Scannern [20][21]. Ein Scanner ohne INTERNET-Permission ist ein nachprüfbares Vertrauensargument.
- **MVP:** CameraX + ZXing (rein offline; ML Kit bundled wäre Alternative, aber Google-Abhängigkeit); Typ-Erkennung (URL/WLAN/Kontakt/Text) mit Vorschau vor dem Öffnen, URL-Warnung bei verdächtigen Domains (lokale Heuristik); Verlauf lokal; QR erzeugen (Text/WLAN) + teilen; aus Bild scannen.
- **Grafik:** animierter Sucher, der sich beim Erkennen um den Code „zusammenzieht“ (Ecken-Morph), Ergebnis als Karte, die aus dem Code herausfliegt.
- **Permissions:** CAMERA. Bewusst KEIN INTERNET (Links werden an den Browser übergeben).
- **Risiken/Policy:** unkritisch, aber die Kategorie ist extrem umkämpft und Android-Kameras können QR schon nativ. Die Nachfrage pro neuer App ist deshalb unsicherer als die Installzahl suggeriert. Grafisch weniger spektakulär, alternativ durch das Countdown-Widget ersetzen.
- **Komplexität:** S–M.
- **Namen:** `scanr` (kleine Namensvettern wie „QRScanr“ [46]), `codr`, `qrr`. **Favorit: scanr**.
- **Icon:** vier Sucher-Ecken, in der Mitte ein einzelner leuchtender Punkt.

**Ersatz Nr. 1: Countdown-Widget („Tage bis …“)**. Days Until ~1,58 Mio. mit Werbung, Konkurrenz-Widgets aktualisieren nicht zuverlässig [22]. Glance-Widgets mit Verlauf/Foto, keine Permissions, Scope S–M. Name: `countr`/`untilr`.

## 4. Gemeinsame Designsprache / Marke (Vorschlag)

- **Name/Schema:** alle Apps kleingeschrieben, „-r“-Endung wie sleepr (tunr, pulsr, levlr, nordr, loudr, roundr, spinr, scorr, focusr, scanr). Dachmarke z. B. „the -r apps“ oder ein Entwicklername wie „xapients“.
- **Versprechen (in jedem Store-Text gleich):** keine Werbung, kein Tracking, kein Account, und wo möglich keine INTERNET-Berechtigung, also nachprüfbar offline. Open Source (GPLv3) wie sleepr.
- **Farbwelt:** dunkler, fast schwarzer Grund (#0E1116) mit jeweils EINER leuchtenden Signaturfarbe pro App (tunr grün, pulsr magenta, levlr limette, nordr eisblau, loudr orange, roundr glut/eis, spinr bunt-pastell, scorr gelb, focusr koralle, scanr cyan). Material 3 Dynamic Color als Option, Standard ist aber die Signaturfarbe.
- **Icon-Stil:** einheitliche Form (Squircle, dunkler Grund), ein einziges geometrisches Linien-Symbol in der Signaturfarbe mit dezentem Glow. Themed-Icon (monochrom) für Android 13+ mitliefern.
- **UI-Bausteine (gemeinsames Compose-Modul):** große Ziffern-Typo (z. B. Inter/Space Grotesk tabular), Feder-Physik für alle Zeiger/Nadeln, Haptik-Raster, Glas-/Glow-Effekte, einheitliche Einstellungs- und Info-Seite. Das spart Agentenarbeit und gibt allen Apps dasselbe Gefühl.
- **Screenshots:** gleiches Template (dunkel, App-Hero-Animation als Standbild, eine Zeile Claim „Nur das. Schön. Ohne Werbung.“).

## Risiken/Hinweise für den Vertrieb

- **Play Store:** private Entwicklerkonten (nach 13.11.2023 angelegt) brauchen pro App einen Closed Test mit mindestens 12 Testern, die 14 Tage am Stück dabei sind [47]. Organisationskonten sind laut Drittquellen ausgenommen. Bei 10 Apps lohnt sich ein Organisationskonto oder ein fester Tester-Pool.
- **APK-Direktvertrieb:** Google führt die Entwickler-Verifizierung auch für sideloaded Apps ein. Ab 30.09.2026 gilt sie in BR/ID/SG/TH, 2027 weltweit. Unregistrierte APKs gehen dann nur noch über einen „advanced flow“ bzw. ADB [48]. Für Österreich also bis 2027 unkritisch, danach sollte die Entwickler-ID registriert sein (Hobbyist-Konten sind angekündigt).
- **Foreground-Services:** Seit Android 14 muss jeder FGS-Typ im Manifest stehen und in der Play Console begründet werden [38]. Das betrifft Metronom, Intervall- und Fokus-Timer.
- **F-Droid** wäre ein natürlicher Zusatzkanal (GPLv3, keine Tracker), die Zielgruppe dort schätzt genau dieses Versprechen.

## 5. Quellen

1. AppGoblin DaTuner: https://appgoblin.info/apps/com.bork.dsp.datuna
2. GuitarTuna 100 Mio.+ (Websuche-Zusammenfassung; App Store + Yousician): https://apps.apple.com/app/id527588389, https://yousician.com/guitar-tuner
3. GuitarTuna-Reviews (Abo-Druck, Werbung): https://justuseapp.com/en/app/527588389/guitartuna-tune-play-guitar/reviews, https://android-stats.com/d/com.ovelin.guitartuna
4. Metronome Beats Installs: https://appgoblin.info/apps/com.andymstone.metronome, https://www.appbrain.com/app/metronome-beats/com.andymstone.metronome
5. Exodus-Report Metronome Beats (Tracker/Ad-SDKs): https://reports.exodus-privacy.eu.org/en/reports/503042
6. Metronom-Reviews „ads are distracting“: https://justuseapp.com/en/app/297704514/metronome-reloaded/reviews
7. AppGoblin Bubble Level (NixGame): https://appgoblin.info/apps/org.nixgame.bubblelevel
8. AppBrain Bubble-Level-Listen (Gamma Play 10M+): https://www.appbrain.com/app/org.nixgame.bubblelevel
9. AppGoblin Smart Compass: https://appgoblin.info/apps/kr.sira.compass
10. AppGoblin Sound Meter (Abc Apps): https://appgoblin.info/apps/com.gamebasic.decibel, Exodus: https://reports.exodus-privacy.eu.org/en/reports/3736
11. AppGoblin Interval Timer: https://appgoblin.info/apps/cc.dreamspark.intervaltimer
12. AlternativeTo HIIT/OpenHIIT (FOSS): https://alternativeto.net/software/hiit/about, https://alternativeto.net/software/openhiit/about
13. AppGoblin Spin The Wheel: https://appgoblin.info/apps/com.randompicker.wheeldecides.randomnamepicker.spinthewheel
14. AppBrain Spin The Wheel (High Maturity, contains ads): https://www.appbrain.com/app/spin-the-wheel-picker-decides/com.randompicker.wheeldecides.randomnamepicker.spinthewheel
15. AppGoblin Scoreboard – Track Score: https://appgoblin.info/apps/it.censa.scoreboardsimple
16. AppGoblin Score Counter: https://appgoblin.info/apps/ua.napps.scorekeeper
17. AppGoblin Focus To-Do: https://appgoblin.info/apps/com.superelement.pomodoro
18. Time Timer (Abo-Umstellung, Reviews): https://apps.apple.com/us/app/time-timer/id332520417, https://www.timetimer.com/products/time-timer-ios-app
19. AppGoblin QR & Barcode Scanner (Gamma): https://appgoblin.info/apps/com.gamma.scan
20. Sophos 2018 QR-Adware: https://news.sophos.com/en-us/2018/03/23/crooks-infiltrate-google-play-with-malware-lurking-in-qr-reading-utilities/
21. Barcode Scanner Adware 2021: https://www.theregister.com/2021/02/08/barcode_scan_app_malwarebytes_update/ ; Android Police QR-Abo-Abzocke: https://www.androidpolice.com/2019/06/18/qr-code-app-on-play-store-ripping-people-off-for-100-through-shady-trial-scheme/
22. Countdown Days Until: https://grand-screen.com/apps/countdown-days-until/ ; Days Matter: https://producthunt.com/r/e2b9e5a28cd66b
23. AppBrain App2U Magnifier: https://www.appbrain.com/dev/App2U/
24. AppGoblin Smart Measure: https://appgoblin.info/apps/kr.sira.measure
25. Loop Habit Tracker: https://github.com/iSoron/uhabits, https://www.appbrain.com/app/loop-habit-tracker/org.isoron.uhabits
26. Flip-Clock-Apps: https://chrome-stats.com/d/one.alynx.flipclock, https://appgoblin.info/apps/flip.clock.pomodoro.studytimer.studyclock.desktopclock.fliqlo.flipclock
27. AppBrain Teleprompter LLC: https://www.appbrain.com/dev/Teleprompter+LLC/
28. Teleprompter.com Reviews (Abo, Wasserzeichen): https://apps.apple.com/app/id1420515755
29. Chwazi (Herstellerangabe): https://apps.apple.com/app/id689674978
30. AppBrain Tally Counter (Marcel Bochtler): https://www.appbrain.com/dev/Marcel+Bochtler/
31. Atem-App-Reviews (Abo/Paywall): https://mwm.ai/apps/breathing-exercises-breathe/1106998959, https://apps.apple.com/us/app/id6444422222
32. AppBrain Cosmic Vibrations (Moonly): https://www.appbrain.com/dev/Cosmic+Vibrations+Inc/
33. Android Authority Mondphasen-Apps: https://androidauthority.com/best-moon-phase-apps-moon-calendar-apps-android-815919
34. AppGoblin Lux Light Meter PRO: https://appgoblin.info/apps/com.pardel.photometer
35. Schachuhr-Apps: https://www.appbrain.com/app/chessis-clock-game-timer/com.chessimprovement.chessclock
36. F-Droid/AlternativeTo Taschenlampen: https://alternativeto.net/platform/f-droid/
37. Tunr (iOS-Visualizer): https://apps.apple.com/us/app/id948831179
38. Play-Policy Foreground-Service-Typen: https://support.google.com/googleplay/android-developer/answer/13392821, https://developer.android.com/develop/background-work/services/fgs/declare
39. Metronom-Namensprüfung (Websuche, keine „pulsr“-App gefunden): https://www.soundbrenner.com/blog/best-metronome-apps-for-android
40. AppBrain Entwickler „Bubblr“: https://www.appbrain.com/dev/Bubblr/
41. Kompass-Namensprüfung (keine „nordr“-App): https://www.androidauthority.com/best-compass-apps-android-103888
42. Sound-Meter-Namensprüfung (keine „loudr“-App): https://play.google.com/store/apps/details?id=com.proaudiotools
43. Timer-Namensprüfung (keine „roundr“/„focusr“-App): https://apps.apple.com/us/app/roundready-timer/id6777734375
44. Decidr (Android, 2026): https://appgoblin.info/apps/anini.decido
45. Tallyr (iOS): https://apps.apple.com/it/app/tallyr/id1568802212
46. QRScanr: https://chrome-stats.com/d/com.domelabs.scanapp
47. Play Closed-Test-Pflicht: https://support.google.com/googleplay/android-developer/answer/14151465
48. Android Developer Verification: https://support.google.com/android-developer-console/answer/16561738, https://www.androidauthority.com/android-sideloading-changes-timeline-3679204/
49. sleepr README: https://github.com/valueerrorx/sleepr
