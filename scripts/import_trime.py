#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Import the pinned recursive Git trees as ordinary files, with provenance.

Run once against a fully initialized cache. Refuses an existing destination.
Symlink assets are materialized after ensuring their target stays in the checkout.
"""
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def git(repo, *args):
    return subprocess.check_output(['git', '-C', str(repo), *args])


def main():
    source = ROOT / '.cache/upstream/trime'
    destination = ROOT / 'android/trime'
    lock = json.loads((ROOT / 'upstream-lock.json').read_text())['projects']['trime']
    if git(source, 'rev-parse', 'HEAD').decode().strip() != lock['commit']:
        raise ValueError('Trime checkout differs from lock')
    if destination.exists():
        raise FileExistsError(destination)
    status = git(source, 'submodule', 'status', '--recursive').decode().splitlines()
    if any(line[0] != ' ' for line in status):
        raise ValueError('submodule not at its locked gitlink')
    repos = [source] + [source / line.split()[1] for line in status]
    records = []
    manifests = []
    for repo in repos:
        rel = repo.relative_to(source)
        entries = git(repo, 'ls-tree', '-rz', 'HEAD').split(b'\0')
        files = []
        for entry in filter(None, entries):
            meta, raw = entry.split(b'\t', 1)
            mode, kind, blob = meta.decode().split()
            if kind == 'commit':
                continue
            path = raw.decode()
            target = repo / path
            resolved = target.resolve()
            if not resolved.is_relative_to(source.resolve()) or not resolved.is_file():
                raise ValueError('unsafe/missing tracked source: ' + str(rel / path))
            data = target.read_bytes()
            out = destination / rel / path
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(data)
            out.chmod(0o755 if mode == '100755' else 0o644)
            files.append({'path': path, 'git_blob': blob, 'git_mode': mode,
                          'materialized_link': target.readlink().as_posix() if target.is_symlink() else None,
                          'sha256': hashlib.sha256(data).hexdigest(), 'bytes': len(data)})
        repo_record = {'path': rel.as_posix(),
                       'commit': git(repo, 'rev-parse', 'HEAD').decode().strip(),
                       'source': git(repo, 'remote', 'get-url', 'origin').decode().strip(),
                       'licenses': [f['path'] for f in files if any(w in Path(f['path']).name.lower()
                                    for w in ('license', 'copying', 'notice', 'copyright'))]}
        records.append(repo_record)
        manifests.append({**repo_record, 'files': files})
    provenance = ROOT / 'third_party'
    provenance.mkdir(exist_ok=True)
    (provenance / 'trime-sources.json').write_text(json.dumps(manifests, indent=2, ensure_ascii=False) + '\n')
    (provenance / 'trime-repositories.json').write_text(json.dumps(records, indent=2) + '\n')
    print('Imported', sum(len(m['files']) for m in manifests), 'files from', len(records), 'fixed repositories; no nested Git metadata.')


if __name__ == '__main__':
    main()
