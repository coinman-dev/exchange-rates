#!/usr/bin/env bash
# Значок приложения и логотип раздела «О приложении» (нужен ImageMagick 7).
#
# Исходники — два круглых логотипа в images/: без надписи (значок приложения)
# и с надписью («О приложении»). Скрипт их только читает и ничего не
# центрирует сам: круг берётся ровно по белому диску исходника, поэтому
# рисунок стоит в круге так же, как в файле.
set -euo pipefail
cd "$(dirname "$0")/.."

ICON_SRC=images/logo-round.png        # без надписи
TEXT_SRC=images/logo-text-round.png   # с надписью
RES=app/src/main/res
BG='#FDFDFD'                          # цвет диска в исходниках

work=$(mktemp -d); trap 'rm -rf "$work"' EXIT

# disc <исходник> <выход>: круг по диску исходника, прозрачный снаружи.
# Диск находится по альфа-каналу; радиус на 4 пикселя меньше, чтобы не захватить
# сглаженную кайму по его краю.
disc() {
  local box w h x y r d
  box=$(magick "$1" -alpha extract -threshold 50% -format '%@' info:)   # ШxВ+X+Y
  w=${box%%x*}; box=${box#*x}; h=${box%%+*}; box=${box#*+}; x=${box%%+*}; y=${box#*+}
  r=$(( (w < h ? w : h) / 2 - 4 )); d=$((r * 2))
  x=$(awk "BEGIN { printf \"%d\", $x + $w / 2 - $r + 0.5 }")
  y=$(awk "BEGIN { printf \"%d\", $y + $h / 2 - $r + 0.5 }")
  # фон диска выравнивается до одного цвета, иначе он чуть «шумит» (252–255)
  magick "$1" -crop "${d}x${d}+${x}+${y}" +repage -compose Over -background "$BG" -flatten \
    -alpha set \( -size "${d}x${d}" xc:none -fill white -draw "circle $r,$r $r,0.5" \) \
    -compose DstIn -composite "$2"
}

disc "$ICON_SRC" "$work/icon.png"
disc "$TEXT_SRC" "$work/text.png"

# раздел «О приложении»: круг с надписью
mkdir -p "$RES/drawable-nodpi"
magick "$work/text.png" -resize 768x768 -quality 90 \
  -define webp:alpha-quality=100 "$RES/drawable-nodpi/logo_about.webp"

# Значок приложения — адаптивный: холст 108 dp, диск исходника занимает 66 dp
# (безопасная зона), фон того же цвета. Форму (круг, сквиркл) вырезает система.
magick "$work/icon.png" -compose Over -background "$BG" -flatten "$work/flat.png"
# силуэт для тематических значков Android 13+: система красит его по альфа-каналу
magick "$work/flat.png" -channel RGB -separate -evaluate-sequence min -negate -level 8%,45% \
  \( +clone -fill black -colorize 100 \) +swap -alpha off -compose CopyOpacity -composite "$work/mono.png"

for spec in mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432; do
  dpi=${spec%%:*}; canvas=${spec##*:}
  size=$(awk "BEGIN { printf \"%d\", $canvas * 66 / 108 + 0.5 }")
  mkdir -p "$RES/mipmap-$dpi"
  magick "$work/flat.png" -resize "${size}x${size}" -background "$BG" -gravity center \
    -extent "${canvas}x${canvas}" -define webp:lossless=true "$RES/mipmap-$dpi/ic_launcher_foreground.webp"
  magick "$work/mono.png" -resize "${size}x${size}" -background none -gravity center \
    -extent "${canvas}x${canvas}" -define webp:lossless=true "$RES/mipmap-$dpi/ic_launcher_monochrome.webp"
done
echo "готово"
