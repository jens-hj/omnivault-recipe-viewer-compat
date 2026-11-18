"""Original compatibility icon, composed on one absolute pixel grid."""
from pathlib import Path
from PIL import Image, ImageDraw
from compat_symbol import BACKGROUND, draw_compat_symbol

root = Path(__file__).resolve().parents[1]
image = Image.new("RGB", (32,32), BACKGROUND)
d = ImageDraw.Draw(image)
# Recipe terminal, with the links occupying its lower-right connection area.
d.rectangle((3,4,26,17), fill="#587680")
d.rectangle((5,6,24,15), fill="#0b1c27")
for y in (7,12):
    d.rectangle((7,y,10,y+2), fill="#c89459")
    d.point((7,y), fill="#f0c087")
d.rectangle((12,10,17,11), fill="#82e3cf")
d.rectangle((16,9,17,12), fill="#82e3cf")
d.rectangle((18,10,18,11), fill="#82e3cf")
d.rectangle((20,9,22,12), fill="#f1c767")
d.rectangle((11,18,14,22), fill="#587680")
d.rectangle((7,23,16,24), fill="#7f9ca3")

draw_compat_symbol(image)
for path, size in ((root / "common/src/main/resources/assets/omnivault_recipe_viewer_compat/icon.png", 128), (root / "docs/icon.png", 512)):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.resize((size,size), Image.Resampling.NEAREST).save(path)
