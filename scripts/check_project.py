#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Offline preparation checks only. Never certifies an Android app or APK."""
import hashlib
import json
from pathlib import Path
import re
import string
import sys
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = (
    'AGENTS.md', 'README.md', 'LICENSE', 'NOTICE.md', '.gitignore', 'upstream-lock.json',
    'docs/STATUS.md', 'docs/PRODUCT.md', 'docs/ARCHITECTURE.md', 'docs/UPSTREAM.md',
    'docs/COMPLIANCE.md', 'docs/SECURITY.md', 'docs/UI_SPEC.md', 'docs/PLAN.md',
    'docs/DEVELOPMENT.md', 'docs/TESTING.md', 'docs/PROGRESS.md', 'docs/DECISIONS.md',
    'docs/CODEX_START.md', 'docs/FILE_MANIFEST.md', 'docs/reference/README.md',
    'docs/testing/RUN_TEMPLATE.md', 'docs/testing/INIT-20261023.md',
    'spec/keyboard.json', 'spec/security.json', 'tests/fixtures/seed_cases.json',
    'scripts/doctor.py', 'tests/tooling/test_checks.py', 'android/README.md',
)
SKIP_DIRS = {'.git', '.cache', '.private', 'artifacts', '__pycache__', '.gradle', 'build', '.cxx', '.externalNativeBuild'}

# These are CMake-generated copies/links, never imported source files.
GENERATED_TREES = {
    'android/trime/app/src/main/jni/boost',
    'android/trime/app/src/main/jni/librime/plugins/librime-lua',
    'android/trime/app/src/main/jni/librime/plugins/librime-octagram',
    'android/trime/app/src/main/jni/librime/plugins/librime-predict',
    'android/trime/app/src/main/jni/librime-lua/thirdparty',
}


def within(root, path):
    try:
        path.resolve().relative_to(root.resolve())
        return True
    except ValueError:
        return False


def project_files(root):
    # Do not descend ignored build trees or follow symlinks.
    import os
    for base, dirs, files in os.walk(root, followlinks=False):
        dirs[:] = sorted(d for d in dirs if d not in SKIP_DIRS and (Path(base) / d).relative_to(root).as_posix() not in GENERATED_TREES and not (Path(base) / d).is_symlink())
        for name in sorted(files):
            if (Path(base) / name).relative_to(root).as_posix() in GENERATED_TREES:
                continue
            if name == '.DS_Store' or name.endswith(('.pyc', '.pyo')):
                continue
            yield Path(base) / name


def local_link_errors(root, source, content):
    errors = []
    # The authored documents use inline Markdown links. No network access occurs.
    content = re.sub(r'```.*?```', '', content, flags=re.S)
    for target in re.findall(r'!?\[[^\]\n]*\]\(([^)\n]+)\)', content):
        target = target.strip().split(' "', 1)[0].strip('<>')
        if urlsplit(target).scheme or target.startswith(('#', '//')):
            continue
        relative = unquote(target.split('#', 1)[0].split('?', 1)[0])
        dest = source.parent / relative
        if not within(root, dest):
            errors.append(f'{source.relative_to(root)}: link escapes project: {target}')
        elif not dest.exists():
            errors.append(f'{source.relative_to(root)}: broken link: {target}')
    return errors


def validate_keyboard(k):
    errors = []
    mapping = k.get('mapping', {})
    expected = dict(zip('23456789', ['ABC', 'DEF', 'GHI', 'JKL', 'MNO', 'PQRS', 'TUV', 'WXYZ']))
    if mapping != expected:
        errors.append('keyboard mapping must match the reviewed numeric nine-key mapping')
    letters = ''.join(mapping.values())
    if sorted(letters) != list(string.ascii_uppercase):
        errors.append('keyboard mapping must cover each of the 26 letters exactly once')
    policy = k.get('bar_policy', {})
    for key in ('chinese_idle', 'full_commit', 'delete_to_empty'):
        if policy.get(key) != 'punctuation':
            errors.append('idle/full-commit/delete-empty must show punctuation: ' + key)
    if policy.get('partial_commit') != 'candidates':
        errors.append('partial commits must keep candidates')
    if policy.get('post_commit_prediction_default') is not False:
        errors.append('post-commit prediction must not override default punctuation')
    return errors


def validate_seed_cases(k, cases):
    errors = []
    mapping = {letter.lower(): digit for digit, letters in k['mapping'].items() for letter in letters}
    if cases.get('synthetic_only') is not True:
        errors.append('fixtures must use synthetic data only')
    for case in cases.get('encoding_cases', []):
        expected = ''.join(mapping.get(ch.lower(), '?') for ch in case['pinyin'])
        if case['digits'] != expected:
            errors.append(f"fixture encoding mismatch: {case['pinyin']}: {case['digits']} != {expected}")
    return errors


def validate_lock(root, lock):
    errors = []
    for key, project in lock.get('projects', {}).items():
        sha = project.get('commit', '')
        if not re.fullmatch(r'[0-9a-f]{40}', sha):
            errors.append(key + ': invalid immutable commit')
        tree_file = root / 'docs/reference/upstream' / (key + '-tree.json')
        if not tree_file.is_file():
            errors.append(key + ': missing tree snapshot')
            continue
        tree = json.loads(tree_file.read_text())
        if tree.get('sha') != sha or tree.get('truncated') is not False:
            errors.append(key + ': tree SHA mismatch or truncated tree')
        blobs = {x['path']: x['sha'] for x in tree.get('tree', []) if x['type'] == 'blob'}
        gitlinks = {x['path']: x['sha'] for x in tree.get('tree', []) if x['type'] == 'commit'}
        for sub in project.get('submodules', []):
            if gitlinks.get(sub['path']) != sub['commit']:
                errors.append(key + ': submodule pin mismatch: ' + sub['path'])
        for file in project.get('files', []):
            p = root / file['local_path']
            if not within(root, p) or p.is_symlink() or not p.is_file():
                errors.append(key + ': missing/unsafe snapshot: ' + file['local_path'])
                continue
            data = p.read_bytes()
            if hashlib.sha256(data).hexdigest() != file['sha256'] or len(data) != file['bytes']:
                errors.append(key + ': snapshot SHA256/size mismatch: ' + file['path'])
            git_blob_sha = hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()
            if blobs.get(file['path']) != git_blob_sha:
                errors.append(key + ': bytes do not match pinned Git blob: ' + file['path'])
            expected = f"https://raw.githubusercontent.com/{project['repository']}/{sha}/{file['path']}"
            if file.get('url') != expected:
                errors.append(key + ': source URL must use canonical repo and pinned SHA')
    if lock.get('runtime_assets'):
        for a in lock['runtime_assets']:
            if not all(a.get(k) for k in ('sha256', 'source', 'license', 'version')):
                errors.append('runtime asset lacks digest/source/license/version')
    return errors


def check(root=ROOT):
    errors = [f'missing required file: {p}' for p in REQUIRED if not (root / p).is_file()]
    files = list(project_files(root))
    for path in files:
        rel = path.relative_to(root)
        if path.is_symlink():
            errors.append('unexpected symlink: ' + str(rel))
            continue
        if path.suffix in {'.json', '.md', '.py'}:
            try:
                data = path.read_text(encoding='utf-8')
                if path.suffix == '.json':
                    json.loads(data)
                elif path.suffix == '.md' and 'docs/reference/upstream/' not in rel.as_posix():
                    errors.extend(local_link_errors(root, path, data))
            except (UnicodeError, ValueError) as exc:
                errors.append(str(rel) + ': ' + str(exc))
        # Avoid printing matching secrets. Research files can contain upstream examples.
        if 'docs/reference/upstream/' not in rel.as_posix() and path.stat().st_size < 1000000:
            data = path.read_bytes()
            if re.search(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----', data):
                errors.append('private-key marker found (content suppressed): ' + str(rel))
        if path.suffix.lower() in {'.jks', '.keystore', '.apk', '.aab', '.hprof'} or path.name in {'local.properties', 'keystore.properties', '.env'}:
            errors.append('sensitive/generated file outside ignored storage: ' + str(rel))
    checksum_file = root / 'docs/reference/upstream/SHA256SUMS'
    if not checksum_file.is_file():
        errors.append('missing reference SHA256SUMS')
    else:
        for line in checksum_file.read_text().splitlines():
            try:
                digest, relative = line.split('  ', 1)
                p = checksum_file.parent / relative
                if not within(checksum_file.parent, p) or p.is_symlink() or not p.is_file():
                    errors.append('unsafe/missing reference checksum target')
                elif hashlib.sha256(p.read_bytes()).hexdigest() != digest:
                    errors.append('reference checksum mismatch: ' + relative)
            except ValueError:
                errors.append('invalid reference checksum line')
    try:
        k = json.loads((root / 'spec/keyboard.json').read_text())
        cases = json.loads((root / 'tests/fixtures/seed_cases.json').read_text())
        errors.extend(validate_keyboard(k))
        errors.extend(validate_seed_cases(k, cases))
        sec = json.loads((root / 'spec/security.json').read_text())
        if 'android.permission.INTERNET' not in sec.get('forbidden_permissions', []):
            errors.append('security spec must forbid INTERNET')
        for field in ('allow_backup', 'allow_shared_uid', 'allow_backup_agent', 'allow_runtime_network', 'sensitive_learning', 'sensitive_context_reads', 'telemetry', 'advertising', 'runtime_online_dependencies'):
            if sec.get(field) is not False:
                errors.append('security requirement is not disabled: ' + field)
        errors.extend(validate_lock(root, json.loads((root / 'upstream-lock.json').read_text())))
    except (OSError, KeyError, ValueError, TypeError) as exc:
        errors.append('spec/lock error: ' + str(exc))
    print(f'Checked {len(files)} files; {len(errors)} error(s). Scope: preparation only; no Android/APK validation.')
    for e in errors:
        print('FAIL:', e)
    return 1 if errors else 0


if __name__ == '__main__':
    sys.exit(check())
