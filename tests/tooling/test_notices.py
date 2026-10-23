# SPDX-License-Identifier: GPL-3.0-or-later
import hashlib
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'scripts'))
from prepare_offline_notices import build_entries, ROOT


class NoticeBoundary(unittest.TestCase):
    def test_changed_license_is_rejected_before_bundling(self):
        with tempfile.TemporaryDirectory(dir=ROOT / ".cache") as temp:
            root=Path(temp)
            (root/'LICENSE').write_text('changed')
            row=dict(path='LICENSE',title='synthetic',sha256=hashlib.sha256(b'expected').hexdigest())
            with self.assertRaisesRegex(ValueError,'E_NOTICE_HASH'):
                build_entries(dict(files=[row]),root)

    def test_reference_escape_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=ROOT / ".cache") as temp:
            with self.assertRaisesRegex(ValueError,'E_NOTICE_PATH'):
                build_entries(dict(files=[dict(path='../private',title='escape',sha256='0'*64)]),Path(temp))
