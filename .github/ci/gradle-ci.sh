#!/usr/bin/env bash
# Runner (4 Kerne, 16 GB): Benutzer-Properties ueberschreiben die NAS-Sparwerte aus gradle.properties
set -euo pipefail
dir="${GRADLE_USER_HOME:-$HOME/.gradle}"
mkdir -p "$dir"
printf '%s\n' \
  "org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8" \
  "org.gradle.workers.max=4" >> "$dir/gradle.properties"
