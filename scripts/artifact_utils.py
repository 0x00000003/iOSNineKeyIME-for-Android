#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Verified build-host artifacts only; never operates on IME user data."""
import hashlib
from pathlib import Path, PurePosixPath
import re
import shutil
import stat
import zipfile


def verify(path, sha256, size=None):
    path = Path(path)
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    if digest.hexdigest() != sha256 or (size is not None and path.stat().st_size != size):
        raise ValueError('artifact digest/size mismatch: ' + path.name)
    return digest.hexdigest()


def extract_zip(archive, destination, max_bytes=2 * 1024**3, max_files=100000):
    """Validate the whole central directory before writing a fresh staging tree.

    Reject links, special files, duplicate/case-alias paths and traversal. Nothing
    is merged into an existing tree. Callers atomically rename staging on success.
    """
    destination = Path(destination)
    if destination.exists() or destination.is_symlink():
        raise FileExistsError(destination)
    with zipfile.ZipFile(archive) as z:
        entries = z.infolist()
        if len(entries) > max_files or sum(i.file_size for i in entries) > max_bytes:
            raise ValueError('archive exceeds extraction budget')
        seen = set()
        files = set()
        for item in entries:
            raw = item.filename
            parts = PurePosixPath(raw).parts
            mode = item.external_attr >> 16
            if (not parts or raw.startswith('/') or '\\' in raw or
                    any(p in ('.', '..') for p in raw.rstrip('/').split('/')) or
                    re.match(r'^[A-Za-z]:', raw) or
                    stat.S_IFMT(mode) not in (0, stat.S_IFREG, stat.S_IFDIR)):
                raise ValueError('unsafe archive member')
            key = '/'.join(parts).casefold()
            if key in seen:
                raise ValueError('duplicate archive member')
            seen.add(key)
            if not item.is_dir():
                files.add(key)
        for key in seen:
            if any('/'.join(key.split('/')[:n]) in files for n in range(1, len(key.split('/')))):
                raise ValueError('archive parent is a file')
        destination.mkdir(parents=True)
        try:
            for item in entries:
                target = destination.joinpath(*PurePosixPath(item.filename).parts)
                if item.is_dir():
                    target.mkdir(parents=True, exist_ok=True)
                    continue
                target.parent.mkdir(parents=True, exist_ok=True)
                with z.open(item) as src, target.open('xb') as dst:
                    shutil.copyfileobj(src, dst)
                target.chmod(0o755 if (item.external_attr >> 16) & 0o111 else 0o644)
        except Exception:
            # Only our newly created staging directory, never an existing tree.
            shutil.rmtree(destination)
            raise
