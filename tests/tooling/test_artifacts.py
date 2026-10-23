# SPDX-License-Identifier: GPL-3.0-or-later
import hashlib
import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile

SPEC = importlib.util.spec_from_file_location('artifacts', Path(__file__).resolve().parents[2] / 'scripts/artifact_utils.py')
artifacts = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(artifacts)


class ExtractionTests(unittest.TestCase):
    def extract(self, names, **kwargs):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            archive = root / 'input.zip'
            with zipfile.ZipFile(archive, 'w') as z:
                for name in names:
                    z.writestr(name, b'synthetic')
            artifacts.extract_zip(archive, root / 'out', **kwargs)
            return (root / 'out/valid/file.txt').read_bytes()

    def test_valid(self):
        self.assertEqual(self.extract(['valid/file.txt']), b'synthetic')

    def test_traversal_rejected_before_any_write(self):
        for name in ['../escape', '/absolute', 'x/../../escape', 'C:/escape', 'x\\..\\escape']:
            with self.subTest(name=name), self.assertRaises(ValueError):
                self.extract(['valid/file.txt', name])

    def test_symlink_and_duplicate_rejected(self):
        entry = zipfile.ZipInfo('link')
        entry.create_system = 3
        entry.external_attr = 0o120777 << 16
        with self.assertRaises(ValueError):
            self.extract([entry])
        with self.assertRaises(ValueError):
            self.extract(['valid/file.txt', 'valid/file.txt'])

    def test_size_limit(self):
        with self.assertRaises(ValueError):
            self.extract(['valid/file.txt'], max_bytes=3)

    def test_hash_rejects_tampering(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'a'
            p.write_bytes(b'a')
            artifacts.verify(p, hashlib.sha256(b'a').hexdigest(), 1)
            p.write_bytes(b'b')
            with self.assertRaises(ValueError):
                artifacts.verify(p, hashlib.sha256(b'a').hexdigest(), 1)

    def test_existing_destination_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d)
            with zipfile.ZipFile(p / 'a.zip', 'w') as z:
                z.writestr('ok', b'a')
            (p / 'out').mkdir()
            with self.assertRaises(FileExistsError):
                artifacts.extract_zip(p / 'a.zip', p / 'out')
