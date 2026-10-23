# SPDX-License-Identifier: GPL-3.0-or-later
import importlib.util
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location('audit', Path(__file__).resolve().parents[2] / 'scripts/audit_wanxiang.py')
audit = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(audit)


class AuditTests(unittest.TestCase):
    def test_dictionary_body_is_not_interpreted_as_yaml(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)
            (p/'main.dict.yaml').write_text('import_tables:\n  - words\n...\nlanguage: fake\n')
            refs=audit.audit(p)['references']
            self.assertEqual([(r['kind'],r['target']) for r in refs],[('import_table','words.dict.yaml')])

    def test_lua_slash_require_and_protected_require_are_scanned(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)
            (p/'code.lua').write_text('local x = require("wanxiang/userdb")\nlocal ok, y = pcall(require, "wanxiang/charset_filter")\n')
            refs=audit.audit(p)['references']
            self.assertEqual([r['target'] for r in refs],['lua/wanxiang/userdb.lua','lua/wanxiang/charset_filter.lua'])

    def test_missing_model_and_real_reference(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)
            (p/'main.schema.yaml').write_text('grammar:\n  language: fixed-model\nkey_binder:\n  __include: default:/key_binder?\n')
            (p/'default.yaml').write_text('key_binder: {}\n')
            result=audit.audit(p)
            self.assertEqual([r['target'] for r in result['missing_references']],['fixed-model.gram'])
            self.assertTrue(next(r for r in result['references'] if r['target']=='default.yaml')['optional'])
