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
download "1G3lhqsp-K4_WYRwQ3YJ9dfNEhkDm0kG6" "$RAW/boo_laugh.mp3"
download "149Tk0WObWGZnB6uJA6SDj7Jco6hULTGf" "$RAW/evil_laugh.mp3"
download "1bGIhfMW0XrVeGDPyHz-LzGL4rqWmJkSI" "$RAW/intro.mp4"

# New layered map
download "11pN8itBK1-8ONtJ0KIAPigTcVH_YmEeu" "$DRAW/sky_bg.jpg"
download "1S1IWvW9Xz5Gyct9kDDW5_TN5r-XoX7si" "$DRAW/city_layer.png"
download "1bOAN8gq4_bypwR-W-2DBmUzxTBnQkVMD" "$DRAW/sidewalk.jpg"

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

download "1VGJBp6W3mFw9Gd_UgDba_L4yYP0Ov3Ph" "$DRAW/ghost_front.png"
download "17oYXw4OVG92zCx6FTu02OxG-Ti30gFBq" "$DRAW/ghost_side.png"

download "1T5iSFRCwZ6WrEV_xXQbcz17WkMSANCWN" "$DRAW/mummy_front.png"
download "11zFm9z3udt8AG2E07SODyAZt7B_s5K9k" "$DRAW/mummy_side.png"

download "1zZ3ih0mL6THlgyjxi-FPh5qz-vCmXbXo" "$DRAW/wizard_front.png"
download "1WtehSxAoF_Bt17AbmkE1e4zvUbl_Fljo" "$DRAW/wizard_side.png"

download "1IHRtDhXhtWDF1a_onZvlFxclNWGtkfNG" "$DRAW/little_witch_front.png"
download "16zlCLmp7QJ2nX0yHbrVczSrp8SHeTOpd" "$DRAW/little_witch_side.png"

download "1fFBbH4MLhamZ6mBPAiV00Kj0cqzD-wHB" "$DRAW/pumpkin_front.png"
download "17rX16oKDf1SO1tyrszgw85AUVurHWw5u" "$DRAW/pumpkin_side.png"

download "1bYavWhIMO8EQ-96GXC4MviB2oQD62fV9" "$DRAW/wolf_front.png"
download "1w9pkqhlrE8I4aVPq2gu5iRRWXMPrm4d0" "$DRAW/wolf_side.png"

echo "All v2.0 assets downloaded."
