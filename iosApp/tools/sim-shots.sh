#!/usr/bin/env bash
# Store-Screenshots im Simulator: App in festen Szenen starten (Debug-Build, -aappsScene) und abfotografieren.
# sim-shots.sh <app.app> <bundleId> <ordner> <displayType> <Geraete-Regex> <BxH> <Szene>...
# Ergebnis: <ordner>/<locale>/<displayType>/NN_<szene>.png (en-US und de-DE)
# Umgebung: SHOT_ROTATE (Grad im Uhrzeigersinn), SHOT_WAIT (Sekunden bis zum ersten Foto, Standard 6)
set -euo pipefail
APP=$1 BUNDLE=$2 OUT=$3 TYPE=$4 DEVICE=$5 SIZE=$6
shift 6

UDID=$(xcrun simctl list devices available -j | python3 -c '
import json, re, sys
d = json.load(sys.stdin)["devices"]
hits = [x for r, l in sorted(d.items()) if "iOS" in r for x in l if re.match(sys.argv[1], x["name"])]
print(hits[-1]["udid"] if hits else "")' "$DEVICE")
if [ -z "$UDID" ]; then echo "::error::kein Simulator fuer $DEVICE"; exit 1; fi
echo "$TYPE: $(xcrun simctl list devices | grep "$UDID" | sed 's/ (.*//' | xargs)"

xcrun simctl boot "$UDID" || true
xcrun simctl bootstatus "$UDID" -b
xcrun simctl status_bar "$UDID" override --time 9:41 --batteryState charged --batteryLevel 100 --wifiBars 3 --cellularBars 4 || true
xcrun simctl install "$UDID" "$APP"

# Bild hat Inhalt? (Debug-Build startet manchmal >6 s, dann zeigt das Foto nur den leeren Hintergrund)
TOOLS=$(cd "$(dirname "$0")" && pwd)
SMALL=$(mktemp -t shot).png
has_content() {
  rm -f "$SMALL"
  # sips meldet bei fehlender Datei nur eine Warnung (Exit 0), darum Datei selbst pruefen
  [ -s "$1" ] && sips -Z 320 "$1" --out "$SMALL" > /dev/null 2>&1 && [ -s "$SMALL" ] &&
    python3 -I "$TOOLS/shot-content.py" "$SMALL" | awk 'END { exit !($1 > 0.005) }'
}

# simctl mit Zeitlimit (haengt sonst ggf. bis zum Job-Abbruch); <sekunden> <befehl...>
limit() { perl -e 'alarm shift; exec @ARGV' "$@"; }
snap() { rm -f "$1"; limit 60 xcrun simctl io "$UDID" screenshot "$1" > /dev/null 2>&1 || echo "::warning::$TYPE $LOCALE: Foto fehlgeschlagen"; }

# Szene starten und fotografieren, sobald Inhalt da ist (+3 s fuer Animationen); bis zu 3 Starts
shoot() {
  local scene=$1 f=$2 try wait
  for try in 1 2 3; do
    echo "$(date +%T) $TYPE $LOCALE $scene"
    limit 120 xcrun simctl launch --terminate-running-process "$UDID" "$BUNDLE" -aappsScene "$scene" -AppleLanguages "($LANG_)" -AppleLocale "$CC" > /dev/null ||
      echo "::warning::$TYPE $LOCALE $scene: Start fehlgeschlagen oder Zeitlimit"
    sleep "${SHOT_WAIT:-6}"
    for wait in 1 2 3 4 5 6 7 8 9 10; do
      snap "$f"
      if has_content "$f"; then
        sleep 3
        snap "$f"
        if has_content "$f"; then return 0; fi
      fi
      sleep 2
    done
    echo "::warning::$TYPE $LOCALE $scene: noch leer, Start $try wiederholen"
  done
  echo "::error::$TYPE $LOCALE $scene bleibt leer"
  exit 1
}

for spec in en-US:en:en_US de-DE:de:de_AT; do
  IFS=: read -r LOCALE LANG_ CC <<< "$spec"
  mkdir -p "$OUT/$LOCALE/$TYPE"
  n=1
  for scene in "$@"; do
    f="$OUT/$LOCALE/$TYPE/$(printf %02d "$n")_$(echo "$scene" | tr 'A-Z' 'a-z').png"
    shoot "$scene" "$f"
    # Spiele schreiben ihre Fenster-/Bildschirmgroesse mit (Diagnose)
    DATA=$(xcrun simctl get_app_container "$UDID" "$BUNDLE" data 2>/dev/null || true)
    if [ -n "$DATA" ] && [ -f "$DATA/Documents/shot-layout.txt" ]; then echo "layout $scene: $(cat "$DATA/Documents/shot-layout.txt")"; fi
    got="$(sips -g pixelWidth -g pixelHeight "$f" | awk '/pixelWidth/{w=$2}/pixelHeight/{h=$2}END{print w"x"h}')"
    if [ "$got" != "$SIZE" ]; then echo "::error::$f ist $got statt $SIZE"; exit 1; fi
    # Querformat-App: Simulator liefert das Bild hochkant, gerade drehen (SHOT_ROTATE = Grad im Uhrzeigersinn)
    if [ -n "${SHOT_ROTATE:-}" ]; then sips -r "$SHOT_ROTATE" "$f" > /dev/null; fi
    n=$((n + 1))
  done
done
rm -f "$SMALL"
xcrun simctl terminate "$UDID" "$BUNDLE" || true
xcrun simctl shutdown "$UDID" || true
echo "$TYPE: $(( $# * 2 )) Bilder"
