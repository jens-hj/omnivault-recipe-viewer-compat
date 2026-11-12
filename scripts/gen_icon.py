"""Draws the mod icon: a terminal screen showing a recipe arrow, as 16x16 pixel art, scaled up.

Usage: gen_icon.py <out.png> [modrinth_out.png]
The first output is 128px on a transparent background; the optional second is a 512px
rounded dark tile for the Modrinth project page.
"""
import sys
from PIL import Image, ImageDraw

# . empty, o outline, f frame, s screen, c cyan, w wood, b wood dark, g gold, y highlight
ART = [
    "................",
    ".oooooooooooooo.",
    ".offffffffffffo.",
    ".ofssssssssssfo.",
    ".ofswbsssssssfo.",
    ".ofsbwsscssssfo.",
    ".ofsssssccsggfo.",
    ".ofswbcccccgyfo.",
    ".ofsbwssccsggfo.",
    ".ofssssscssssfo.",
    ".ofswbsssssssfo.",
    ".ofsbwsssssssfo.",
    ".ofssssssssssfo.",
    ".offffffffffffo.",
    ".oooooooooooooo.",
    "................",
]
COLORS = {
    "o": (14, 18, 26, 255),
    "f": (58, 68, 86, 255),
    "s": (22, 28, 40, 255),
    "c": (56, 225, 255, 255),
    "w": (190, 140, 82, 255),
    "b": (140, 96, 52, 255),
    "g": (214, 160, 40, 255),
    "y": (255, 232, 120, 255),
    ".": None,
}


def pixels():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(ART):
        for x, c in enumerate(row):
            if COLORS.get(c):
                img.putpixel((x, y), COLORS[c])
    return img


def main():
    art = pixels()
    art.resize((128, 128), Image.NEAREST).save(sys.argv[1])
    if len(sys.argv) > 2:
        tile = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
        ImageDraw.Draw(tile).rounded_rectangle((0, 0, 511, 511), radius=96, fill=(28, 24, 44, 255))
        tile.alpha_composite(art.resize((448, 448), Image.NEAREST), (32, 32))
        tile.save(sys.argv[2])


if __name__ == "__main__":
    main()
