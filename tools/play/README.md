# Play-Upload (Node, androidpublisher v3)

Liest die fastlane-Metadaten aus `apps/<app>/fastlane/metadata/android/`. Paketname: `solutions.xapient.<app>`.
Wird vom Workflow `android-release.yml` genutzt, laeuft aber auch lokal.

    npm ci --prefix tools/play
    PLAY_KEY_FILE=/pfad/dienstkonto.json node tools/play/play.mjs check <app>

Das Dienstkonto-JSON liegt nie im Repo (CI: Secret `PLAY_SERVICE_ACCOUNT_JSON`, nur in `$RUNNER_TEMP`).

## Befehle
    node play.mjs check   <app>            # Auth + Lesezugriff (Edit anlegen, lesen, verwerfen)
    node play.mjs listing <app> [--dry-run] # Texte aller Locales + Bilder, defaultLanguage=en-US
    node play.mjs upload  <app> --aab=<datei> [--track=internal|production] [--status=draft|completed] [--dry-run]
    node play.mjs iap     <app> [--dry-run] # Einmalprodukt full_unlock, 1,49 EUR (nicht moovr/glidr)

- upload: Track `internal` (Standard) oder `production`. Status `draft`, solange die App nie veroeffentlicht war, sonst `completed`.
  Release-Notes aus `changelogs/<versionCode>.txt`.
- Pfade per Umgebung: `AAPPS_REPO` (Standard: Repo-Wurzel), `AAPPS_AAB_DIR` (statt `--aab`), `PLAY_KEY_FILE`.
