# SPDX-License-Identifier: GPL-3.0-or-later
from pathlib import Path
import sys,tempfile,unittest,json,hashlib
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'scripts'))
from ensure_runtime_assets import ROOT,validate_assets

class RuntimeAssetBoundary(unittest.TestCase):
    def test_incomplete_clean_output_and_unknown_file_fail_closed(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.cache') as temp:
            root=Path(temp);(root/'third_party').mkdir();out=root/'output';out.mkdir()
            manifest=dict(archive_sha256=hashlib.sha256(b'asset').hexdigest(),archive_bytes=5,model=False)
            raw=(json.dumps(manifest)+'\n').encode();(root/'third_party/android-probe-resources.json').write_bytes(raw)
            with self.assertRaisesRegex(ValueError,'E_ASSET_OUTPUT_CONTENT'):validate_assets(out,root)
            (out/'ime-probe.zip').write_bytes(b'asset');(out/'ime-probe-manifest.json').write_bytes(raw)
            (out/'ime-probe.sha256').write_text(manifest['archive_sha256']+'\n')
            validate_assets(out,root)
            (out/'unknown.lua').write_text('return {}')
            with self.assertRaisesRegex(ValueError,'E_ASSET_OUTPUT_CONTENT'):validate_assets(out,root)
