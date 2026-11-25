# aapps

Kleine Android-Werkzeuge, jedes mit genau einer Aufgabe: schön, schnell, offline.

## Apps
- **sleepr** – Mischt weißes, rosa, braunes, graues und grünes Rauschen live zum Abschirmen, Konzentrieren und Einschlafen.
- **levlr** – Wasserwaage, die bei 0,0° spürbar einrastet, auf der Kante oder flach liegend.
- **nordr** – Ruhiger, genauer Kompass, der auf Wunsch einen Kurs hält.
- **spinr** – Glücksrad, das sich wie ein echtes dreht und fair entscheidet.
- **scorr** – Punktezähler für Spieleabende mit großen Farbflächen pro Spieler.
- **focusr** – Visueller Timer: die Restzeit als farbige Scheibe, die langsam schrumpft.
- **roundr** – Intervall-Timer für HIIT, Tabata und Boxrunden, aus drei Metern lesbar.
- **pulsr** – Metronom mit sauberem, sample-genauem Klick zum Üben.
- **loudr** – Lautstärke-Messer mit Alltagsvergleich und Verlauf.
- **tunr** – Chromatisches Stimmgerät: Ton spielen, Note und Abweichung sofort ablesen.
- **magnr** – Metalldetektor und Magnetfeld-Messer in Mikrotesla.
- **dozr** – Einschlaf-Timer für jede App, der Musik und Podcasts sanft ausblendet.
- **hertzr** – Sauberer Tongenerator mit genau einstellbarer Frequenz und Wellenform.
- **hudr** – GPS-Tacho mit riesiger Anzeige und HUD-Modus, ganz ohne Internet.
- **moovr** – Clumsy Cow: Geschicklichkeitsspiel mit einer tollpatschigen Kuh auf Eisschollen.
- **glidr** – Cool Cows on Ice: Physikrätsel mit 48 Leveln und schlitternden Kühen.
- **thinkr** – Kurze Denkspiele für zwischendurch, je etwa eine Minute, mit Tagestraining.
- **dividr** – Gruppenkasse ohne Konto und Internet: Ausgaben teilen, Handys gleichen per QR-Code ab.

Jede App liegt unter `apps/<name>/` mit `SPEC.md`. Übersicht mit Bildern: [docs/overview/index.html](docs/overview/index.html), Plan: [docs/PLAN.md](docs/PLAN.md).

## Bauen
Siehe [docs/AGENTS.md](docs/AGENTS.md). Die CI (GitHub Actions) läuft vorerst nur manuell, weil die Signing-Secrets noch fehlen.

## Prinzipien
- Keine Werbung, kein Tracking, kein Account.
- Keine Internet-Berechtigung: was die Apps messen, bleibt am Gerät.
- Open Source unter GPLv3.
- Alle Apps außer den Spielen moovr und glidr: 24 Stunden voll testen, danach einmaliger Kauf über Google Play (`core/billing`, Produkt `full_unlock`).

## Lizenz
GPLv3, siehe [LICENSE](LICENSE).
