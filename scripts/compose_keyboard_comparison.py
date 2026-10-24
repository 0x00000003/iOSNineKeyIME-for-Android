#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Join the two complete supplied screenshots, Android left and iOS right."""
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[1]
IMAGES = ROOT / 'docs/images'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    left_path = IMAGES / 'android-fullscreen.png'
    right_path = IMAGES / 'ios-fullscreen.png'
    left = Image.open(left_path).convert('RGB')
    right = Image.open(right_path).convert('RGB')
    assert left.height == right.height, 'Original screenshots must have equal height; never resize silently'
    hashes = {p.name: digest(p) for p in (left_path, right_path)}
    canvas = Image.new('RGB', (left.width + right.width, left.height))
    canvas.paste(left, (0, 0))
    canvas.paste(right, (left.width, 0))
    output = IMAGES / 'android-ios-comparison.png'
    canvas.save(output, optimize=True)
    saved = Image.open(output).convert('RGB')
    left_box = (0, 0, left.width, left.height)
    right_box = (left.width, 0, canvas.width, right.height)
    assert ImageChops.difference(saved.crop(left_box), left).getbbox() is None
    assert ImageChops.difference(saved.crop(right_box), right).getbbox() is None
    assert hashes == {p.name: digest(p) for p in (left_path, right_path)}
    metadata = {
        'kind': 'two complete original screenshots joined side by side',
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
        'cropped': False, 'resized': False, 'labels_added': False, 'ui_redrawn': False,
        'pixel_checks': {'saved_halves_equal_original_pixels': True, 'sources_unchanged': True},
    }
    (IMAGES / 'comparison.json').write_text(json.dumps(metadata, indent=2) + '\n')
    print(json.dumps({'file': output.relative_to(ROOT).as_posix(), 'size': list(canvas.size),
                      'halves_match_originals': True, 'sha256': digest(output)}))


if __name__ == '__main__':
    main()
