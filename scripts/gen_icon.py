"""Original pixel art. Each character is exactly one cell of a 16x16 grid."""
from pathlib import Path
from PIL import Image
from compat_symbol import compose

root = Path(__file__).resolve().parents[1]
ART = (
    '................',
    '................',
    '..FFFFFFFFFFFF..',
    '..FSSSSSSSSSSF..',
    '..FSBSSSSSSSSF..',
    '..FSSSSSSCSSYF..',
    '..FSBSSCCCCSYF..',
    '..FSSSSSSCSSYF..',
    '..FFFFFFFFFFFF..',
    '......FF........',
    '......FF........',
    '....FFFFFF......',
    '................',
    '................',
    '................',
    '................',
)
COLORS = {'F': '#7799a1', 'S': '#0b1c27', 'B': '#dda66b', 'C': '#82e3cf', 'Y': '#ffe39b'}
image = compose(ART, COLORS)
for path, size in ((root / "common/src/main/resources/assets/omnivault_recipe_viewer_compat/icon.png", 128), (root / "docs/icon.png", 512)):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.resize((size,size), Image.Resampling.NEAREST).save(path)
