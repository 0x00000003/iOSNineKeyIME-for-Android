# SPDX-License-Identifier: GPL-3.0-or-later
import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'scripts'))
from run_probe import streamed_result, selected_apk, ROOT

class ProbeTransport(unittest.TestCase):
    def test_nonce_rejects_previous_or_truncated_result(self):
        self.assertIsNone(streamed_result('INSTRUMENTATION_RESULT: stream={"probe_run_id":"old","pass":true}', 'new'))
        self.assertIsNone(streamed_result('INSTRUMENTATION_RESULT: stream={"probe_run_id":"new",', 'new'))
        output='INSTRUMENTATION_RESULT: stream={"probe_run_id":"new","pass":false}\nINSTRUMENTATION_CODE: 0'
        self.assertFalse(streamed_result(output,'new')['pass'])
    def test_metadata_selects_one_package_confined_file(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.cache') as temp:
            folder=Path(temp)
            path=folder/'output-metadata.json'
            path.write_text(json.dumps(dict(applicationId='own',elements=[dict(outputFile='../outside.apk')])))
            with self.assertRaisesRegex(ValueError,'E_APK_PATH'):selected_apk(folder,'own')
            path.write_text(json.dumps(dict(applicationId='other',elements=[dict(outputFile='own.apk')])))
            with self.assertRaisesRegex(ValueError,'E_APK_METADATA'):selected_apk(folder,'own')
