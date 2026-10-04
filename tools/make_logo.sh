#!/usr/bin/env bash
# Логотип и значок приложения из images/logos-orig.png (нужен ImageMagick 7).
#
# В исходнике два логотипа рядом на почти белом фоне: слева без надписи (значок
# приложения), справа с надписью (раздел «О приложении»). Центры и радиусы
# рисунков измерены по исходнику; если его заменить, их нужно измерить заново.
set -euo pipefail
cd "$(dirname "$0")/.."

SRC=images/logos-orig.png
RES=app/src/main/res
BG='#FEFEFE'            # фон исходника

# центр и радиус описанной окружности рисунка, в пикселях исходника
ICON_CX=506.4; ICON_CY=478.0; ICON_R=318.7   # без надписи
TEXT_CX=1265.4; TEXT_CY=478.5; TEXT_R=329.4  # с надписью

# round_cut <cx> <cy> <радиус рисунка> <выход>: круг с полем 8 % вокруг рисунка
round_cut() {
  local r d x y
  r=$(awk "BEGIN { printf \"%d\", $3 * 1.08 + 0.5 }"); d=$((r * 2))
  x=$(awk "BEGIN { printf \"%d\", $1 - $r + 0.5 }"); y=$(awk "BEGIN { printf \"%d\", $2 - $r + 0.5 }")
  # -alpha set обязателен: без альфа-канала у исходника углы выходят чёрными
  magick "$SRC" -crop "${d}x${d}+${x}+${y}" +repage -alpha set \
    \( -size "${d}x${d}" xc:none -fill white -draw "circle $r,$r $r,0.5" \) \
    -compose DstIn -composite "$4"
}

round_cut "$ICON_CX" "$ICON_CY" "$ICON_R" images/logo-round.png
round_cut "$TEXT_CX" "$TEXT_CY" "$TEXT_R" images/logo-text-round.png

# раздел «О приложении»: круг с надписью
mkdir -p "$RES/drawable-nodpi"
magick images/logo-text-round.png -resize 512x512 -quality 92 \
  -define webp:alpha-quality=100 "$RES/drawable-nodpi/logo_about.webp"

# Значок приложения — адаптивный: холст 108 dp, рисунок занимает 60 dp и целиком
# попадает в безопасную зону 66 dp. Форму (круг, сквиркл) вырезает сама система.
side=$(awk "BEGIN { printf \"%d\", $ICON_R * 2 * 1.02 + 0.5 }")
x=$(awk "BEGIN { printf \"%d\", $ICON_CX - $side / 2 + 0.5 }")
y=$(awk "BEGIN { printf \"%d\", $ICON_CY - $side / 2 + 0.5 }")
tight=$(mktemp --suffix=.png); mono=$(mktemp --suffix=.png)
trap 'rm -f "$tight" "$mono"' EXIT
magick "$SRC" -alpha off -crop "${side}x${side}+${x}+${y}" +repage "$tight"
# силуэт для тематических значков Android 13+: система красит его по альфа-каналу
magick "$tight" -channel RGB -separate -evaluate-sequence min -negate -level 8%,45% \
  \( +clone -fill black -colorize 100 \) +swap -alpha off -compose CopyOpacity -composite "$mono"

for spec in mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432; do
  dpi=${spec%%:*}; canvas=${spec##*:}
  art=$(awk "BEGIN { printf \"%d\", $canvas * 60 / 108 * 1.02 + 0.5 }")
  mkdir -p "$RES/mipmap-$dpi"
  magick "$tight" -resize "${art}x${art}" -background "$BG" -gravity center \
    -extent "${canvas}x${canvas}" -define webp:lossless=true "$RES/mipmap-$dpi/ic_launcher_foreground.webp"
  magick "$mono" -resize "${art}x${art}" -background none -gravity center \
    -extent "${canvas}x${canvas}" -define webp:lossless=true "$RES/mipmap-$dpi/ic_launcher_monochrome.webp"
done
echo "готово"
