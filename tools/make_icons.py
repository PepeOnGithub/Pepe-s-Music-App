"""Generates the launcher icon from the Font Awesome Free "music" glyph (CC BY 4.0): a
green-gradient music-note glyph on a cream disc, on the same black rim-lit badge as the sibling
apps (Clock/Notes/Weather/Calculator).

Badge recipe (same as Clock's tools/make_icons.py):
  - one light source, from directly above, applied consistently to every surface and shadow;
  - the badge is a top-to-bottom linear gradient (a lit dome), not a gradient from a corner;
  - the bevel is a thin rim light traced along the actual visible edge of the badge shape,
    bright at the top and fading to nothing by mid-height;
  - the face is its own cream disc so the glyph has something to sit on, with a soft drop
    shadow cast straight down (matching the overhead light).

Writes: adaptive background/foreground/monochrome vectors, legacy WebP mipmaps (API 24-25) and
the 512 px Play Store PNG. Run from anywhere: python tools/make_icons.py
"""
import io
import os
import re

import cairosvg
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "app", "src", "main", "res")


def fa_path(name):
    with open(os.path.join(HERE, f"fa-{name}.svg"), encoding="utf-8") as f:
        return re.search(r'<path d="([^"]+)"', f.read()).group(1)


PATH = fa_path("music")  # viewBox 512 x 512
GX, GY, GS = 34.8, 34.8, 0.075
FACE_R = 20.25

SHADOW = [(0.6, 1.6, 0.30), (1.2, 3.0, 0.20), (2.0, 4.6, 0.12)]

BG_HI, BG_MID, BG_LO = "FF2A2A2A", "FF141414", "FF000000"
BEVEL_DARK = "26003300"
FACE_HI, FACE_LO = "FFFFFDF6", "FFF3E7C9"
GLYPH_HI, GLYPH_LO = "FF7ACB7B", "FF2E7D32"


def write(rel, text):
    p = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        f.write(text)


HEADER = ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
          '    xmlns:aapt="http://schemas.android.com/aapt"\n'
          '    android:width="108dp" android:height="108dp"\n'
          '    android:viewportWidth="108" android:viewportHeight="108">\n')

bg = HEADER
bg += (
    '    <path android:pathData="M0,0h108v108h-108z">\n'
    '        <aapt:attr name="android:fillColor">\n'
    '            <gradient android:startX="54" android:startY="0" android:endX="54" android:endY="108" android:type="linear">\n'
    f'                <item android:color="#{BG_HI}" android:offset="0.0" />\n'
    f'                <item android:color="#{BG_MID}" android:offset="0.5" />\n'
    f'                <item android:color="#{BG_LO}" android:offset="1.0" />\n'
    '            </gradient>\n'
    '        </aapt:attr>\n'
    '    </path>\n'
    f'    <path android:fillColor="#{BEVEL_DARK}" android:pathData="M0,104h108v4h-108z" />\n'
)
write("drawable/ic_launcher_background.xml", bg + "</vector>\n")

fg = ('<!-- Music glyph: Font Awesome Free (CC BY 4.0), https://fontawesome.com -->\n' + HEADER)
fg += (
    f'    <path android:pathData="M54,{54 - FACE_R}a{FACE_R},{FACE_R} 0 1 1 0,{FACE_R * 2}a{FACE_R},{FACE_R} 0 1 1 0,-{FACE_R * 2}z">\n'
    '        <aapt:attr name="android:fillColor">\n'
    '            <gradient android:startX="54" android:startY="' + str(54 - FACE_R) + '" android:endX="54" android:endY="' + str(54 + FACE_R) + '" android:type="linear">\n'
    f'                <item android:color="#{FACE_HI}" android:offset="0.0" />\n'
    f'                <item android:color="#{FACE_LO}" android:offset="1.0" />\n'
    '            </gradient>\n'
    '        </aapt:attr>\n'
    '    </path>\n'
)
for dx, dy, a in SHADOW:
    alpha_hex = f"{int(round(a * 255)):02X}"
    fg += (
        f'    <group android:scaleX="{GS}" android:scaleY="{GS}" android:translateX="{GX + dx:.2f}" android:translateY="{GY + dy:.2f}">\n'
        f'        <path android:fillColor="#{alpha_hex}000000" android:fillType="evenOdd" android:pathData="{PATH}" />\n'
        '    </group>\n'
    )
fg += (
    f'    <group android:scaleX="{GS}" android:scaleY="{GS}" android:translateX="{GX}" android:translateY="{GY}">\n'
    f'        <path android:fillType="evenOdd" android:pathData="{PATH}">\n'
    '            <aapt:attr name="android:fillColor">\n'
    '                <gradient android:startX="60" android:startY="40" android:endX="450" android:endY="480" android:type="linear">\n'
    f'                    <item android:color="#{GLYPH_HI}" android:offset="0.0" />\n'
    f'                    <item android:color="#{GLYPH_LO}" android:offset="1.0" />\n'
    '                </gradient>\n'
    '            </aapt:attr>\n'
    '        </path>\n'
    '    </group>\n'
)
write("drawable/ic_launcher_foreground.xml", fg + "</vector>\n")

mono = HEADER
mono += (
    f'    <group android:scaleX="{GS}" android:scaleY="{GS}" android:translateX="{GX}" android:translateY="{GY}">\n'
    f'        <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd" android:pathData="{PATH}" />\n'
    '    </group>\n'
)
write("drawable/ic_launcher_monochrome.xml", mono + "</vector>\n")


def svg(size, shape):
    if shape == "round":
        clip, rim = '<circle cx="54" cy="54" r="54"/>', '<circle cx="54" cy="54" r="52.5"/>'
    else:
        clip, rim = '<rect width="108" height="108" rx="24" ry="24"/>', '<rect x="1.5" y="1.5" width="105" height="105" rx="22.5" ry="22.5"/>'
    shadows = "".join(
        f'<g transform="translate({GX + dx} {GY + dy}) scale({GS})"><path fill-rule="evenodd" d="{PATH}" fill="#000" fill-opacity="{a}"/></g>'
        for dx, dy, a in SHADOW
    )
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 108 108">
<defs>
<linearGradient id="bg" x1="54" y1="0" x2="54" y2="108" gradientUnits="userSpaceOnUse">
<stop offset="0" stop-color="#{BG_HI[2:]}"/><stop offset="0.5" stop-color="#{BG_MID[2:]}"/><stop offset="1" stop-color="#{BG_LO[2:]}"/>
</linearGradient>
<linearGradient id="rim" x1="54" y1="0" x2="54" y2="100" gradientUnits="userSpaceOnUse">
<stop offset="0" stop-color="#fff" stop-opacity="0.9"/><stop offset="0.45" stop-color="#fff" stop-opacity="0.4"/><stop offset="1" stop-color="#fff" stop-opacity="0"/>
</linearGradient>
<linearGradient id="face" x1="54" y1="{54 - FACE_R}" x2="54" y2="{54 + FACE_R}" gradientUnits="userSpaceOnUse">
<stop offset="0" stop-color="#{FACE_HI[2:]}"/><stop offset="1" stop-color="#{FACE_LO[2:]}"/>
</linearGradient>
<linearGradient id="glyph" x1="60" y1="40" x2="450" y2="480" gradientUnits="userSpaceOnUse">
<stop offset="0" stop-color="#{GLYPH_HI[2:]}"/><stop offset="1" stop-color="#{GLYPH_LO[2:]}"/>
</linearGradient>
<clipPath id="c">{clip}</clipPath></defs>
<g clip-path="url(#c)">
<rect width="108" height="108" fill="url(#bg)"/>
<circle cx="54" cy="54" r="{FACE_R}" fill="url(#face)"/>
{shadows}
<g transform="translate({GX} {GY}) scale({GS})"><path fill-rule="evenodd" d="{PATH}" fill="url(#glyph)"/></g>
{rim.replace("/>", ' fill="none" stroke="url(#rim)" stroke-width="6"/>')}
</g></svg>"""


def render(size, shape="square"):
    png = cairosvg.svg2png(bytestring=svg(size, shape).encode())
    return Image.open(io.BytesIO(png)).convert("RGBA")


for name, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    d = os.path.join(RES, f"mipmap-{name}")
    os.makedirs(d, exist_ok=True)
    render(px, "square").save(os.path.join(d, "ic_launcher.webp"), "WEBP", quality=95)
    render(px, "round").save(os.path.join(d, "ic_launcher_round.webp"), "WEBP", quality=95)

store_dir = os.path.join(HERE, "..", "store")
os.makedirs(store_dir, exist_ok=True)
render(512, "square").convert("RGB").save(os.path.join(store_dir, "play_store_icon_512.png"))
print("icons written")
