#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Lay out original keyboard pixels; never redraw or synthesize the interfaces."""
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
    parser.add_argument('--font', type=Path, required=True, help='Local CJK font; not copied or bundled')
    args = parser.parse_args()
    fonts = {size: ImageFont.truetype(str(args.font), size) for size in (23, 26, 38, 46)}
    output = IMAGES / 'android-ios-comparison.png'
    left_source = IMAGES / 'android-nine-key.png'
    right_source = IMAGES / 'ios-nine-key-area.png'
    before = {p.name: digest(p) for p in (left_source, right_source)}
    left = Image.open(left_source).convert('RGB').crop((0, 219, 960, 920))
    right = Image.open(right_source).convert('RGB')
    canvas = Image.new('RGB', (1440, 748), '#f3f5f7')
    draw = ImageDraw.Draw(canvas)
    draw.text((720, 42), '真实截图对照', font=fonts[38], fill='#18212b', anchor='mm')
    panels = []
    for x, platform, subtitle, color, image in (
        (48, 'Android', '本项目 · 实机截图', '#13834b', left),
        (744, 'iOS', '原生键盘 · 原图裁切', '#0078d4', right),
    ):
        width = 648
        height = round(image.height * width / image.width)
        scaled = image.resize((width, height), Image.Resampling.LANCZOS)
        box = (x, 170, x + width, 170 + height)
        draw.text((x + width / 2, 103), platform, font=fonts[46], fill=color, anchor='mm')
        draw.text((x + width / 2, 143), subtitle, font=fonts[26], fill='#515c68', anchor='mm')
        canvas.paste(scaled, box[:2])
        assert ImageChops.difference(canvas.crop(box), scaled).getbbox() is None
        panels.append({'platform': platform, 'output_box': list(box), 'scaled_image': scaled})
    draw.text((720, 699), '仅裁剪与等比例缩放，不重绘界面，不生成机身或编辑区',
              font=fonts[23], fill='#515c68', anchor='mm')
    canvas.save(output, optimize=True)
    saved = Image.open(output).convert('RGB')
    for panel in panels:
        assert ImageChops.difference(saved.crop(tuple(panel['output_box'])), panel.pop('scaled_image')).getbbox() is None
    assert before == {p.name: digest(p) for p in (left_source, right_source)}
    metadata = {
        'kind': 'deterministic composition of original screenshot key areas',
        'generator': 'Pillow; no generative model',
        'output': {'file': output.name, 'width': canvas.width, 'height': canvas.height,
                   'bytes': output.stat().st_size, 'sha256': digest(output)},
        'left': {'platform': 'Android', 'source_file': left_source.name, 'source_sha256': before[left_source.name],
                 'crop_box': [0, 219, 960, 920],
                 'ime_apk_sha256': '91350a65112bebb3d7a92db11d1d3830fdefe2bc71da831fb1e59ab83b6d7ec1'},
        'right': {'platform': 'iOS', 'source_file': right_source.name, 'source_sha256': before[right_source.name],
                  'reference_keyboard_sha256': 'd5648611a34372f35ef9a9c130bd1fe26e8ca942de1f6bed5a60d269bea4ec80',
                  'crop_from_reference_keyboard': [0, 134, 591, 566],
                  'private_original_in_git': False, 'prediction_row_omitted': True},
        'operations': ['crop to key areas', 'equal width with preserved aspect ratio', 'add labels outside images'],
        'synthetic_phone_frames': False, 'synthetic_editor': False, 'ui_redrawn': False,
        'pixel_checks': {'saved_panels_equal_resized_source_pixels': True, 'sources_unchanged': True},
        'panels': panels, 'not_full_screen_or_pixel_parity_acceptance': True,
    }
    (IMAGES / 'comparison.json').write_text(json.dumps(metadata, indent=2) + '\n')
    print(json.dumps({'file': output.relative_to(ROOT).as_posix(), 'size': list(canvas.size),
                      'panels_match_sources': True, 'sha256': digest(output)}))


if __name__ == '__main__':
    main()
