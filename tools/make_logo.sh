#!/usr/bin/env bash
# Значок приложения и логотип раздела «О приложении» (нужен ImageMagick 7).
#
# Исходники — два круглых логотипа в images/: без надписи (значок приложения)
# и с надписью («О приложении»). Скрипт их только читает. Центр и радиус
# рисунка измерены по этим файлам; если их заменить, нужно измерить заново.
set -euo pipefail
cd "$(dirname "$0")/.."

ICON_SRC=images/logo-round.png        # без надписи
TEXT_SRC=images/logo-text-round.png   # с надписью
RES=app/src/main/res
BG='#FDFDFD'                          # цвет диска в исходниках

# центр и радиус описанной окружности рисунка, в пикселях исходника
ICON_CX=642.9; ICON_CY=647.5; ICON_R=524.1
TEXT_CX=643.4; TEXT_CY=647.0; TEXT_R=524.3

work=$(mktemp -d); trap 'rm -rf "$work"' EXIT

# clean <исходник> <выход>: рисунок на ровном фоне. Диск в исходнике чуть уже
# нужного нам круга, и его кайма легла бы внутрь круга тонкой серой дугой.
clean() {
  magick "$1" \( +clone -alpha extract -threshold 60% -morphology Erode Disk:8 \) \
    -alpha off -compose CopyOpacity -composite -compose Over -background "$BG" -flatten "$2"
}

# round_cut <вход> <cx> <cy> <радиус рисунка> <выход>: круг с полем 8 % вокруг рисунка
round_cut() {
  local r d x y
  r=$(awk "BEGIN { printf \"%d\", $4 * 1.08 + 0.5 }"); d=$((r * 2))
  x=$(awk "BEGIN { printf \"%d\", $2 - $r + 0.5 }"); y=$(awk "BEGIN { printf \"%d\", $3 - $r + 0.5 }")
  # -alpha set обязателен: без альфа-канала углы выходят чёрными
  magick "$1" -crop "${d}x${d}+${x}+${y}" +repage -alpha set \
    \( -size "${d}x${d}" xc:none -fill white -draw "circle $r,$r $r,0.5" \) \
    -compose DstIn -composite "$5"
}

clean "$ICON_SRC" "$work/icon.png"
clean "$TEXT_SRC" "$work/text.png"

# раздел «О приложении»: круг с надписью
mkdir -p "$RES/drawable-nodpi"
round_cut "$work/text.png" "$TEXT_CX" "$TEXT_CY" "$TEXT_R" "$work/about.png"
magick "$work/about.png" -resize 768x768 -quality 90 \
  -define webp:alpha-quality=100 "$RES/drawable-nodpi/logo_about.webp"

# Значок приложения — адаптивный: холст 108 dp, рисунок занимает 60 dp и целиком
# попадает в безопасную зону 66 dp. Форму (круг, сквиркл) вырезает сама система.
side=$(awk "BEGIN { printf \"%d\", $ICON_R * 2 * 1.02 + 0.5 }")
x=$(awk "BEGIN { printf \"%d\", $ICON_CX - $side / 2 + 0.5 }")
y=$(awk "BEGIN { printf \"%d\", $ICON_CY - $side / 2 + 0.5 }")
magick "$work/icon.png" -crop "${side}x${side}+${x}+${y}" +repage "$work/tight.png"
# силуэт для тематических значков Android 13+: система красит его по альфа-каналу
magick "$work/tight.png" -channel RGB -separate -evaluate-sequence min -negate -level 8%,45% \
  \( +clone -fill black -colorize 100 \) +swap -alpha off -compose CopyOpacity -composite "$work/mono.png"

for spec in mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432; do
  dpi=${spec%%:*}; canvas=${spec##*:}
  art=$(awk "BEGIN { printf \"%d\", $canvas * 60 / 108 * 1.02 + 0.5 }")
  mkdir -p "$RES/mipmap-$dpi"
  magick "$work/tight.png" -resize "${art}x${art}" -background "$BG" -gravity center \
    -extent "${canvas}x${canvas}" -define webp:lossless=true "$RES/mipmap-$dpi/ic_launcher_foreground.webp"
  magick "$work/mono.png" -resize "${art}x${art}" -background none -gravity center \
    -extent "${canvas}x${canvas}" -define webp:lossless=true "$RES/mipmap-$dpi/ic_launcher_monochrome.webp"
done
echo "готово"
