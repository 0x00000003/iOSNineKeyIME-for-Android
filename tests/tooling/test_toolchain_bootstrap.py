# SPDX-License-Identifier: GPL-3.0-or-later
from pathlib import Path
import sys,tempfile,unittest,zipfile,hashlib,stat
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'scripts'))
from bootstrap_clean_toolchain import ROOT,install_zip

class ToolchainPayload(unittest.TestCase):
    def test_verified_regular_payload_and_internal_tool_alias(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.cache')as temp:
            root=Path(temp);archive=root/'tools.zip';props=b'Pkg.Revision=1\n'
            with zipfile.ZipFile(archive,'w')as z:
                z.writestr('package/source.properties',props)
                i=zipfile.ZipInfo('package/bin/tool');i.external_attr=(stat.S_IFREG|0o755)<<16;z.writestr(i,b'synthetic-binary')
                i=zipfile.ZipInfo('package/bin/alias');i.external_attr=(stat.S_IFLNK|0o777)<<16;z.writestr(i,'tool')
            result=install_zip(archive,root/'sdk',hashlib.sha256(props).hexdigest())
            self.assertEqual(result['files_verified'],2);self.assertEqual(result['symlinks_verified'],1)
            self.assertEqual((root/'sdk/bin/alias').read_bytes(),b'synthetic-binary')
    def test_archive_link_cannot_escape_owned_package(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.cache')as temp:
            root=Path(temp);archive=root/'tools.zip';props=b'Pkg.Revision=1\n';sentinel=root/'outside';sentinel.write_bytes(b'preserve')
            with zipfile.ZipFile(archive,'w')as z:
                z.writestr('package/source.properties',props)
                i=zipfile.ZipInfo('package/link');i.external_attr=(stat.S_IFLNK|0o777)<<16;z.writestr(i,'../outside')
            with self.assertRaises(ValueError):install_zip(archive,root/'sdk',hashlib.sha256(props).hexdigest())
            self.assertEqual(sentinel.read_bytes(),b'preserve')

    def test_corrupt_download_is_not_trusted_or_silently_replaced(self):
        from bootstrap_clean_toolchain import fetch_archive
        from unittest.mock import patch
        with tempfile.TemporaryDirectory(dir=ROOT/'.cache')as temp:
            root=Path(temp);(root/'tool.zip').write_bytes(b'corrupt')
            row=dict(archive_file='tool.zip',url='https://example.invalid/tool.zip',sha256='0'*64,bytes=7)
            with patch('bootstrap_clean_toolchain.subprocess.run')as network:
                with self.assertRaises(ValueError):fetch_archive(row,root)
                network.assert_not_called()
