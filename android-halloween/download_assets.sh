#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
DRAW="$ROOT/app/src/main/res/drawable-nodpi"
RAW="$ROOT/app/src/main/res/raw"
mkdir -p "$DRAW" "$RAW"

download() {
  local id="$1"
  local out="$2"
  echo "Downloading $out"
  curl -L --fail --retry 3 --retry-delay 2 "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=t" -o "$out"
  test -s "$out"
}

download "1Q4Z5PknJsVKnjJbpaVKPaBpLZaYKBnel" "$DRAW/menu_bg.png"
download "1b0TH2B27xeer-VOdMkHowq_2lMpDlFEc" "$DRAW/logo.png"
download "1CCHS428ju-euTDxe3Rm0UZLML3rPU1YO" "$DRAW/map_tile.png"
download "1l2TFtZM5MU-L9XVeGXuNzzHDC6RmAeFP" "$DRAW/sweet_csoki.png"
download "1d2M2NlTafuCE2YmF5wNH5S_QpYj6pWfX" "$DRAW/sweet_nyaloka.png"
download "1yH6_sC8zkyJgj9jwew9xNbWoo56yaKuE" "$DRAW/sweet_cukorka.png"
download "1B4kgyG-1Zk4ke01md35c3KrCyzHPsbNA" "$DRAW/frog.png"
download "1YfvSKBywaMZeFozfJL7rAKNE2XcQJsia" "$DRAW/witch.png"
download "1fPfQMnPlhYNzNse8k4OVsqACjtgxtYOe" "$DRAW/vampire_front.png"
download "1oscIOXdUe7ARlAowJmgSgkPXQJGnEBD9" "$DRAW/vampire_side.png"
download "1R10Oi-UFy67YHvVfg6khN-mhcRTJiH76" "$DRAW/skeleton_front.png"
download "1zI-YkUl4sRPaQmxhhyC9cyjXwaBpP3gH" "$DRAW/skeleton_side.png"
download "1VGZlV5j8REv1HmWtWCfe8jiVM3daJ2xM" "$RAW/spooky_loop.mp3"

echo "All assets downloaded."
