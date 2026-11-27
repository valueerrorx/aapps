#!/usr/bin/env bash
# Store-Screenshots im Simulator: App in festen Szenen starten (Debug-Build, -aappsScene) und abfotografieren.
# sim-shots.sh <app.app> <bundleId> <ordner> <displayType> <Geraete-Regex> <BxH> <Szene>...
# Ergebnis: <ordner>/<locale>/<displayType>/NN_<szene>.png (en-US und de-DE)
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

for spec in en-US:en:en_US de-DE:de:de_AT; do
  IFS=: read -r LOCALE LANG_ CC <<< "$spec"
  mkdir -p "$OUT/$LOCALE/$TYPE"
  n=1
  for scene in "$@"; do
    xcrun simctl launch --terminate-running-process "$UDID" "$BUNDLE" -aappsScene "$scene" -AppleLanguages "($LANG_)" -AppleLocale "$CC" > /dev/null
    sleep 6
    f="$OUT/$LOCALE/$TYPE/$(printf %02d "$n")_$(echo "$scene" | tr 'A-Z' 'a-z').png"
    xcrun simctl io "$UDID" screenshot "$f" > /dev/null 2>&1
    got="$(sips -g pixelWidth -g pixelHeight "$f" | awk '/pixelWidth/{w=$2}/pixelHeight/{h=$2}END{print w"x"h}')"
    if [ "$got" != "$SIZE" ]; then echo "::error::$f ist $got statt $SIZE"; exit 1; fi
    # Querformat-App: Simulator liefert das Bild hochkant, gerade drehen (SHOT_ROTATE = Grad im Uhrzeigersinn)
    if [ -n "${SHOT_ROTATE:-}" ]; then sips -r "$SHOT_ROTATE" "$f" > /dev/null; fi
    n=$((n + 1))
  done
done
xcrun simctl terminate "$UDID" "$BUNDLE" || true
xcrun simctl shutdown "$UDID" || true
echo "$TYPE: $(( $# * 2 )) Bilder"
