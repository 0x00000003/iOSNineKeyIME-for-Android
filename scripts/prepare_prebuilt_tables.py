#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Copy only deterministic public dictionary tables, never a user database/profile."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
NAMES = [base + suffix for base in ['custom_phrase', 'wanxiang_abbrev_t9', 'wanxiang_lite']
         for suffix in ['.table.bin', '.reverse.bin']]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    first = ROOT / '.cache/host-compat-profile-1/build'
    second = ROOT / '.cache/host-compat-profile-2/build'
    stage = ROOT / '.cache/probe-prebuilt'
    if stage.exists():
        raise FileExistsError('E_PREBUILT_STAGE_EXISTS')
    rows = []
    for name in NAMES:
        source = second / name
        if digest(first / name) != digest(source):
            raise ValueError('E_NONDETERMINISTIC_TABLE: ' + name)
        rows.append(dict(path='build/' + name, sha256=digest(source), bytes=source.stat().st_size,
                         license='CC-BY-4.0, generated from the pinned original public Wanxiang dictionaries'))
    stage.mkdir()
    for name in NAMES:
        shutil.copy2(second / name, stage / name)
    manifest = dict(scope='Six public static dictionary binaries only; NO userdb, learned data, schema timestamps, prism or model',
                    generator='tests/native/compatibility.cc via pinned Trime librime 1.17.0 host build',
                    generator_source_sha256=digest(ROOT / 'tests/native/compatibility.cc'),
                    baseline_commit=subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
                    data_source='Wanxiang lite v18.1.7 asset 659385471; original source archive SHA in upstream-lock.json',
                    verification='Byte-identical in two independently deployed host profiles. Not a clean-toolchain reproducibility or Android compatibility claim.',
                    excluded='Prism/schema builds contain source timestamps; user dictionaries and installation/user YAML are never selected.',
                    files=rows)
    (ROOT / 'third_party/prebuilt-tables.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('Prepared six matching static data tables:', sum(row['bytes'] for row in rows), 'bytes')


if __name__ == '__main__':
    main()
