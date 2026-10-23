#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Refuse SDK package revision drift; does not certify full payload provenance or a clean rebuild."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    inventory = json.loads((ROOT / 'third_party/toolchain-inventory.json').read_text())
    for row in inventory['sdk_packages']:
        source = ROOT / '.cache/android-sdk' / row['path'] / 'source.properties'
        if not source.is_file() or hashlib.sha256(source.read_bytes()).hexdigest() != row['source_properties_sha256']:
            raise SystemExit('E_SDK_PIN: missing or changed package revision receipt: ' + row['path'])
    print('SDK pinned revision receipts match; full installer/payload and clean rebuild audit is separate.')


if __name__ == '__main__':
    main()
