#!/usr/bin/env bash
# Startet Punktjäger im Emulator, spielt eine Runde und prüft, dass nichts abstürzt.
# Aufruf: smoke-test.sh <apk> [ausgabe-ordner]
set -euo pipefail

APK="$1"
OUT="${2:-smoke-test}"
PKG="io.github.sembe123.punktjaeger"
mkdir -p "$OUT"

shot() { adb exec-out screencap -p > "$OUT/$1.png"; }

# Speichert die aktuelle Bedienoberfläche als XML. uiautomator braucht manchmal mehrere Versuche.
dump_ui() {
  for _ in 1 2 3 4 5; do
    if adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1; then
      adb shell cat /sdcard/ui.xml > "$OUT/$1.xml"
      return 0
    fi
    sleep 1
  done
  return 1
}

# Tippt auf das Element mit dem angegebenen Text (aus dem letzten Dump).
tap_text() {
  local text="$1" file="$OUT/$2.xml" node x1 y1 x2 y2
  node=$(grep -o "<node [^>]*text=\"$text\"[^>]*>" "$file" | head -1 || true)
  if [ -z "$node" ]; then
    echo "::error::Element '$text' nicht gefunden"
    exit 1
  fi
  read -r x1 y1 x2 y2 <<< "$(echo "$node" | sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/')"
  adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
}

expect_text() {
  if ! grep -q "$1" "$OUT/$2.xml"; then
    echo "::error::Text '$1' nicht auf dem Bildschirm gefunden"
    exit 1
  fi
}

check_alive() {
  if adb logcat -d -b crash | grep -q "$PKG"; then
    echo "::error::Die App ist abgestürzt"
    adb logcat -d -b crash
    exit 1
  fi
  if ! adb shell pidof "$PKG" > /dev/null; then
    echo "::error::Die App läuft nicht mehr"
    adb logcat -d | tail -200
    exit 1
  fi
}

adb install -r "$APK"
adb logcat -b all -c

echo "== Menü"
adb shell am start -W -n "$PKG/.MainActivity"
sleep 4
shot 01-menu
dump_ui menu
expect_text "Spielen" menu
check_alive

echo "== Spiel starten"
tap_text "Spielen" menu
sleep 3
shot 02-spiel
for _ in $(seq 1 12); do
  adb shell input tap $(( 100 + RANDOM % 880 )) $(( 500 + RANDOM % 1700 ))
done
shot 03-spiel
check_alive

echo "== Auf Spielende warten"
over=0
for _ in $(seq 1 20); do
  if dump_ui spielende && grep -q "Spiel vorbei" "$OUT/spielende.xml"; then
    over=1
    break
  fi
  sleep 2
done
if [ "$over" != 1 ]; then
  shot spielende-fehlt
  echo "::error::Spielende-Bildschirm nicht erschienen"
  exit 1
fi
shot 04-spielende
check_alive

echo "== Nochmal spielen, dann pausieren"
tap_text "Nochmal" spielende
sleep 2
adb shell input keyevent KEYCODE_BACK
sleep 1
dump_ui pause
expect_text "Weiter" pause
shot 05-pause

echo "== Zurück ins Menü"
tap_text "Spiel beenden" pause
sleep 2
dump_ui menu2
expect_text "Spielen" menu2
expect_text "REKORD" menu2
shot 06-menu
check_alive

echo "Smoke-Test erfolgreich."
