# SPDX-License-Identifier: GPL-3.0-or-later
"""Negative tests of the repository checker; not Android behavior tests."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('project_checks', ROOT / 'scripts/check_project.py')
checks = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(checks)
TEMP_ROOT = ROOT / '.cache/testing'
TEMP_ROOT.mkdir(parents=True, exist_ok=True)


class SpecificationChecks(unittest.TestCase):
    def setUp(self):
        self.k = json.loads((ROOT / 'spec/keyboard.json').read_text())

    def test_valid_spec(self):
        self.assertEqual(checks.validate_keyboard(self.k), [])

    def test_duplicate_letter_is_rejected(self):
        self.k['mapping']['9'] = 'WXYA'
        self.assertTrue(checks.validate_keyboard(self.k))

    def test_swapped_digit_is_rejected(self):
        self.k['mapping']['2'], self.k['mapping']['3'] = self.k['mapping']['3'], self.k['mapping']['2']
        self.assertTrue(checks.validate_keyboard(self.k))

    def test_idle_prediction_cannot_override_punctuation(self):
        self.k['bar_policy']['post_commit_prediction_default'] = True
        self.assertTrue(checks.validate_keyboard(self.k))

    def test_partial_commit_cannot_clear_candidates(self):
        self.k['bar_policy']['partial_commit'] = 'punctuation'
        self.assertTrue(checks.validate_keyboard(self.k))

    def test_mistyped_fixture_is_rejected(self):
        cases = {'synthetic_only': True, 'encoding_cases': [{'pinyin': 'nihao', 'digits': '64420'}]}
        self.assertTrue(checks.validate_seed_cases(self.k, cases))


class LinkChecks(unittest.TestCase):
    def test_missing_link_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as d:
            r = Path(d)
            self.assertTrue(checks.local_link_errors(r, r / 'README.md', '[missing](absent.md)'))

    def test_external_and_anchor_links_are_ignored(self):
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as d:
            r = Path(d)
            self.assertEqual(checks.local_link_errors(r, r / 'README.md', '[web](https://example.invalid/) [local](#a)'), [])

    def test_escape_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as d:
            r = Path(d)
            self.assertTrue(checks.local_link_errors(r, r / 'README.md', '[outside](../README.md)'))


class IntegrityChecks(unittest.TestCase):
    def make_case(self, r):
        blob = b'research data\n'
        commit = 'a' * 40
        p = r / 'docs/reference/upstream/example/LICENSE'
        p.parent.mkdir(parents=True)
        p.write_bytes(blob)
        tree = {'sha': commit, 'truncated': False, 'tree': [{'path': 'LICENSE', 'type': 'blob', 'sha': hashlib.sha1(b'blob ' + str(len(blob)).encode() + b'\0' + blob).hexdigest()}]}
        (p.parent.parent / 'example-tree.json').write_text(json.dumps(tree))
        project = {'commit': commit, 'repository': 'owner/example', 'files': [{'path': 'LICENSE', 'local_path': p.relative_to(r).as_posix(), 'sha256': hashlib.sha256(blob).hexdigest(), 'bytes': len(blob), 'url': f'https://raw.githubusercontent.com/owner/example/{commit}/LICENSE'}]}
        return {'projects': {'example': project}, 'runtime_assets': []}, p

    def test_valid_snapshot(self):
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as d:
            r = Path(d)
            lock, _ = self.make_case(r)
            self.assertEqual(checks.validate_lock(r, lock), [])

    def test_tampered_snapshot_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as d:
            r = Path(d)
            lock, p = self.make_case(r)
            p.write_text('changed\n')
            self.assertTrue(checks.validate_lock(r, lock))

    def test_mutable_source_url_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as d:
            r = Path(d)
            lock, _ = self.make_case(r)
            lock['projects']['example']['files'][0]['url'] = 'https://raw.githubusercontent.com/owner/example/main/LICENSE'
            self.assertTrue(checks.validate_lock(r, lock))

    def test_lock_path_escape_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as d:
            r = Path(d)
            lock, _ = self.make_case(r)
            lock['projects']['example']['files'][0]['local_path'] = '../private-file'
            self.assertTrue(checks.validate_lock(r, lock))


if __name__ == '__main__':
    unittest.main()
