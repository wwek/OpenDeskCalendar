#!/usr/bin/env python3
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
OUTPUTS = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/System/Library/Fonts/Supplemental/Helvetica Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Helvetica.ttf",
        "/Library/Fonts/Arial Unicode.ttf",
    ]
    for candidate in candidates:
        path = Path(candidate)
        if path.exists():
            return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def shaped_gradient(size: int, round_shape: bool) -> Image.Image:
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gradient = Image.new("RGBA", (size, size))
    pixels = gradient.load()
    top = (19, 102, 129)
    bottom = (37, 99, 235)
    for y in range(size):
        t = y / float(size - 1)
        for x in range(size):
            edge = x / float(size - 1)
            mix = min(1.0, max(0.0, t * 0.82 + edge * 0.18))
            color = tuple(int(top[i] * (1 - mix) + bottom[i] * mix) for i in range(3))
            pixels[x, y] = (*color, 255)
    mask = Image.new("L", (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    if round_shape:
        mask_draw.ellipse((24, 24, size - 24, size - 24), fill=255)
    else:
        mask_draw.rounded_rectangle((28, 28, size - 28, size - 28), radius=220, fill=255)
    image.paste(gradient, (0, 0), mask)
    return image


def draw_icon(round_shape: bool = False) -> Image.Image:
    size = 1024
    image = shaped_gradient(size, round_shape)
    draw = ImageDraw.Draw(image)

    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.rounded_rectangle((176, 214, 848, 824), radius=92, fill=(0, 0, 0, 92))
    shadow = shadow.filter(ImageFilter.GaussianBlur(28))
    image.alpha_composite(shadow)

    draw.rounded_rectangle((168, 196, 856, 800), radius=92, fill=(248, 250, 252, 255))
    draw.rounded_rectangle((168, 196, 856, 800), radius=92, outline=(226, 232, 240, 255), width=10)

    draw.rounded_rectangle((168, 196, 856, 342), radius=92, fill=(17, 24, 39, 255))
    draw.rectangle((168, 270, 856, 342), fill=(17, 24, 39, 255))

    for x in (306, 718):
        draw.rounded_rectangle((x - 34, 142, x + 34, 248), radius=28, fill=(15, 23, 42, 255))
        draw.rounded_rectangle((x - 20, 166, x + 20, 238), radius=18, fill=(94, 234, 212, 255))

    draw.ellipse((246, 404, 360, 518), fill=(250, 204, 21, 255))
    draw.arc((222, 380, 384, 542), start=210, end=330, fill=(14, 116, 144, 255), width=22)
    draw.ellipse((334, 472, 486, 572), fill=(203, 213, 225, 255))
    draw.ellipse((260, 494, 392, 594), fill=(226, 232, 240, 255))
    draw.rounded_rectangle((266, 540, 520, 616), radius=38, fill=(226, 232, 240, 255))

    day_font = font(248, bold=True)
    label_font = font(88, bold=True)
    draw.text((620, 440), "08", font=day_font, fill=(15, 23, 42, 255), anchor="mm")
    draw.text((620, 646), "CAL", font=label_font, fill=(37, 99, 235, 255), anchor="mm")

    grid_color = (148, 163, 184, 255)
    for x in (270, 356, 442, 528, 614, 700):
        draw.line((x, 682, x, 742), fill=grid_color, width=8)
    for y in (682, 742):
        draw.line((230, y, 742, y), fill=grid_color, width=8)

    return image


def main() -> None:
    icon = draw_icon(False)
    round_icon = draw_icon(True)
    docs = ROOT / "docs" / "assets"
    docs.mkdir(parents=True, exist_ok=True)
    icon.save(docs / "app-icon.png")

    for folder, size in OUTPUTS.items():
        out_dir = ROOT / "app" / "src" / "main" / "res" / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        icon.resize((size, size), Image.Resampling.LANCZOS).save(out_dir / "ic_launcher.png")
        round_icon.resize((size, size), Image.Resampling.LANCZOS).save(out_dir / "ic_launcher_round.png")


if __name__ == "__main__":
    main()
