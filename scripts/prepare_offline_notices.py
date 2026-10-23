#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Bundle pinned public source license texts. Not a complete compliance audit."""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def build_entries(manifest, root=ROOT):
    root = root.resolve()
    entries = []
    if len(manifest['files']) > 256:
        raise ValueError('E_NOTICE_BUDGET')
    for row in manifest['files']:
        file = (root / row['path']).resolve()
        try:
            file.relative_to(root)
        except ValueError:
            raise ValueError('E_NOTICE_PATH') from None
        raw = file.read_bytes()
        if len(raw) > 2 * 1024 * 1024:
            raise ValueError('E_NOTICE_BUDGET')
        if hashlib.sha256(raw).hexdigest() != row['sha256']:
            raise ValueError('E_NOTICE_HASH')
        entries.append(dict(title=row['title'], source_path=row['path'],
                            sha256=row['sha256'], text=raw.decode('utf-8')))
    return entries


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    try:
        output.relative_to(ROOT)
    except ValueError:
        raise ValueError('E_NOTICE_OUTPUT') from None
    manifest = json.loads((ROOT / 'third_party/offline-notice-sources.json').read_text())
    entries = build_entries(manifest)
    payload = json.dumps(dict(scope=manifest['scope'], entries=entries), ensure_ascii=False, indent=2).encode() + b'\n'
    if hashlib.sha256(payload).hexdigest() != manifest['sha256'] or len(payload) != manifest['bytes']:
        raise ValueError('E_NOTICE_BUNDLE_PIN')
    if len(payload) > 4 * 1024 * 1024:
        raise ValueError('E_NOTICE_BUDGET')
    output.mkdir(parents=True, exist_ok=True)
    file = output / 'offline-notices.json'
    tmp = output / 'offline-notices.json.tmp'
    tmp.write_bytes(payload)
    tmp.replace(file)
    print(json.dumps(dict(entries=len(entries), bytes=len(payload), sha256=hashlib.sha256(payload).hexdigest(),
                          scope='pinned public notice bundle ONLY; not full runtime audit')))


if __name__ == '__main__':
    main()
