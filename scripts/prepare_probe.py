#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Prepare a fresh no-model HOST probe. This is not the application's resource pack."""
import hashlib
import json
from pathlib import Path
import shutil
import re
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from artifact_utils import extract_zip, verify

ROOT = Path(__file__).resolve().parents[1]


def main():
    archive = ROOT / '.cache/upstream/wanxiang-lite.zip'
    verify(archive, '4f76c49b70f711c10b6abdf142249d4f0e88dd5b00ffe380fd45b0cd1b39d42a', 23403719)
    stage = ROOT / '.cache/probe-shared'
    extract_zip(archive, stage)
    modifications = []
    for p in stage.glob('*.schema.yaml'):
        before = p.read_bytes()
        lines = before.decode('utf-8-sig').splitlines(keepends=True)
        out = []
        in_grammar = False
        for line in lines:
            if line.startswith('grammar:'):
                in_grammar = True
                continue
            if in_grammar:
                if line and not line[0].isspace() and not line.startswith('#') and line.strip():
                    in_grammar = False
                else:
                    continue
            if 'wanxiang.input_statistics' not in line:
                out.append(line)
        after = ''.join(out).encode()
        if before != after:
            p.write_bytes(after)
            modifications.append({'path':p.name,'before':hashlib.sha256(before).hexdigest(),
                                  'after':hashlib.sha256(after).hexdigest(),
                                  'changes':['remove grammar block for explicitly no-model probe',
                                             'remove input_statistics component']})
    (stage / 'lua/wanxiang/input_statistics.lua').unlink()
    schema = stage / 'wanxiang_t9.schema.yaml'
    schema.write_text(schema.read_text().replace('  filters:\n', '  filters:\n    - lua_filter@*ime_probe\n'))
    shutil.copy2(ROOT / 'tests/native/ime_probe.lua', stage / 'lua/ime_probe.lua')
    default = (stage / 'default.yaml').read_text()
    default = re.sub(r'schema_list:\n(?:[ \t#].*\n)*', 'schema_list:\n  - schema: wanxiang_t9\n', default)
    default = re.sub(r'page_size: \d+', 'page_size: 20', default)
    (stage / 'default.yaml').write_text(default)
    shutil.copy2(ROOT / 'docs/reference/upstream/wanxiang/LICENSE', stage / 'LICENSE.wanxiang')
    manifest = {'scope':'HOST ONLY; no model; no Android/noBackup/sensitive-mode evidence',
                'source':'amzxyz/rime-wanxiang v18.1.7 lite asset 659385471',
                'modifications':modifications,
                'additional_changes':['remove unused input_statistics.lua',
                                      'add test-only Lua execution marker filter; no persistent writes',
                                      'preserve default.yaml includes; select only wanxiang_t9; page size 20',
                                      'copy pinned CC-BY-4.0 LICENSE from source commit'],
                'files':[{'path':p.relative_to(stage).as_posix(),'bytes':p.stat().st_size,
                          'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
                         for p in sorted(stage.rglob('*')) if p.is_file()]}
    (ROOT / 'third_party/probe-resources.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n')
    print('Prepared fresh no-model host resource pack; statistics module removed.')


if __name__ == '__main__':
    main()
