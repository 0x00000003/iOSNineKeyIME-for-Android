#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Frame and label complete screenshots without changing screenshot pixels."""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
IMAGES = ROOT / 'docs/images'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--font', type=Path, required=True, help='Local Chinese-capable font; not bundled')
    args = parser.parse_args()
    title_font = ImageFont.truetype(str(args.font), 38)
    subtitle_font = ImageFont.truetype(str(args.font), 26)
    left_path = IMAGES / 'android-fullscreen.png'
    right_path = IMAGES / 'ios-fullscreen.png'
    left = Image.open(left_path).convert('RGB')
    right = Image.open(right_path).convert('RGB')
    assert left.height == right.height, 'Original screenshots must have equal height; never resize silently'
    hashes = {p.name: digest(p) for p in (left_path, right_path)}
    margin, gap, border, header = 32, 32, 4, 116
    width = margin * 2 + gap + left.width + right.width + border * 4
    height = margin * 2 + header + left.height + border * 2
    canvas = Image.new('RGB', (width, height), '#eef1f5')
    draw = ImageDraw.Draw(canvas)
    boxes = []
    x = margin
    for source, platform, subtitle, color in (
        (left, 'Android', '本项目输入法', '#148650'),
        (right, 'iOS', 'iPhone 原生键盘 · 参考', '#2764c5'),
    ):
        card_width = source.width + border * 2
        draw.rounded_rectangle((x, margin, x + card_width - 1, height - margin - 1),
                               radius=18, fill=color)
        draw.rounded_rectangle((x + border, margin + border, x + card_width - border - 1,
                                margin + header + 24), radius=14, fill='#ffffff')
        draw.text((x + 24, margin + 17), platform, font=title_font, fill=color)
        draw.text((x + 24, margin + 67), subtitle, font=subtitle_font, fill='#374151')
        image_x, image_y = x + border, margin + header + border
        canvas.paste(source, (image_x, image_y))
        boxes.append((image_x, image_y, image_x + source.width, image_y + source.height))
        x += card_width + gap
    output = IMAGES / 'android-ios-comparison.png'
    canvas.save(output, optimize=True)
    saved = Image.open(output).convert('RGB')
    left_box, right_box = boxes
    assert ImageChops.difference(saved.crop(left_box), left).getbbox() is None
    assert ImageChops.difference(saved.crop(right_box), right).getbbox() is None
    assert hashes == {p.name: digest(p) for p in (left_path, right_path)}
    metadata = {
        'kind': 'two complete original screenshots with external platform labels and borders',
        'generator': 'Pillow; no generative model',
        'output': {'file': output.name, 'width': canvas.width, 'height': canvas.height,
                   'bytes': output.stat().st_size, 'sha256': digest(output)},
        'left': {'platform': 'Android', 'source_file': left_path.name,
                 'source_sha256': hashes[left_path.name], 'source_dimensions': list(left.size),
                 'original_attachment_sha256': '8023ac4f55be7304f5a7a990efefdac231d30e79c17c7a61896822d44ab73dbc',
                 'output_box': list(left_box)},
        'right': {'platform': 'iOS', 'source_file': right_path.name,
                  'source_sha256': hashes[right_path.name], 'source_dimensions': list(right.size),
                  'original_attachment_sha256': '7940553c771e275872c1cfd51d151e90d77a5982abe484f18dc4d48c77b684e5',
                  'output_box': list(right_box)},
        'cropped': False, 'resized': False, 'labels_added': True, 'ui_redrawn': False,
        'layout': {'margin': margin, 'gap': gap, 'border': border, 'header': header,
                   'left_label': 'Android / 本项目输入法',
                   'right_label': 'iOS / iPhone 原生键盘 · 参考',
                   'left_border': '#148650', 'right_border': '#2764c5',
                   'font_name': args.font.name},
        'pixel_checks': {'saved_halves_equal_original_pixels': True, 'sources_unchanged': True},
    }
    (IMAGES / 'comparison.json').write_text(json.dumps(metadata, indent=2) + '\n')
    print(json.dumps({'file': output.relative_to(ROOT).as_posix(), 'size': list(canvas.size),
                      'halves_match_originals': True, 'sha256': digest(output)}))


if __name__ == '__main__':
    main()
