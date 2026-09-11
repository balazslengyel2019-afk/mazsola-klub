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

# Menu
download "1Q4Z5PknJsVKnjJbpaVKPaBpLZaYKBnel" "$DRAW/menu_bg.png"
download "1atvJgPM_Yvqb4CqkGxeoj4ZunDXRnmgx" "$DRAW/menu_logo.png"
download "1KjaofVk3_hFGyqcoZdfqWa47SSzZTL04" "$DRAW/app_icon.png"
download "1VGZlV5j8REv1HmWtWCfe8jiVM3daJ2xM" "$RAW/spooky_loop.mp3"

# New layered map
download "11pN8itBK1-8ONtJ0KIAPigTcVH_YmEeu" "$DRAW/sky_bg.jpg"
download "1S1IWvW9Xz5Gyct9kDDW5_TN5r-XoX7si" "$DRAW/city_layer.png"
download "1uPRYnk7izhNiuTjCnb16iqrQBAn6cs4A" "$DRAW/sidewalk.png"

# Sweets
download "1l2TFtZM5MU-L9XVeGXuNzzHDC6RmAeFP" "$DRAW/sweet_csoki.png"
download "1d2M2NlTafuCE2YmF5wNH5S_QpYj6pWfX" "$DRAW/sweet_nyaloka.png"
download "1yH6_sC8zkyJgj9jwew9xNbWoo56yaKuE" "$DRAW/sweet_cukorka.png"

# Characters
download "1B4kgyG-1Zk4ke01md35c3KrCyzHPsbNA" "$DRAW/frog.png"
download "1YfvSKBywaMZeFozfJL7rAKNE2XcQJsia" "$DRAW/witch.png"
download "1fPfQMnPlhYNzNse8k4OVsqACjtgxtYOe" "$DRAW/vampire_front.png"
download "1oscIOXdUe7ARlAowJmgSgkPXQJGnEBD9" "$DRAW/vampire_side.png"
download "1R10Oi-UFy67YHvVfg6khN-mhcRTJiH76" "$DRAW/skeleton_front.png"
download "1zI-YkUl4sRPaQmxhhyC9cyjXwaBpP3gH" "$DRAW/skeleton_side.png"

echo "All v1.2 assets downloaded."
