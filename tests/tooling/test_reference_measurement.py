# SPDX-License-Identifier: GPL-3.0-or-later
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
from PIL import Image, ImageDraw

PROJECT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(PROJECT/'scripts'))
import measure_reference_ui as measurement

class ReferenceMeasurements(unittest.TestCase):
    def setUp(self):
        (PROJECT/'.private').mkdir(exist_ok=True)
        self.temp=tempfile.TemporaryDirectory(dir=PROJECT/'.private')
        self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
        for relative in ['spec/reference','.private/references/ios-20261023','artifacts/testhost','docs/testing/evidence/m2']:(self.root/relative).mkdir(parents=True)
        source=Image.new('RGB',(100,200),(34,34,34));ImageDraw.Draw(source).rectangle((20,100,79,139),fill=(70,70,70))
        self.source=self.root/'.private/references/ios-20261023/R01-original.jpg';source.save(self.source)
        actual=Image.new('RGB',(200,240),(34,34,34));ImageDraw.Draw(actual).rectangle((40,40,159,119),fill=(70,70,70))
        self.actual=self.root/'artifacts/testhost/synthetic-r01-chinese.png';actual.save(self.actual)
        reference=dict(id='R01',source_sha256=measurement.digest(self.source),source_dimensions=[100,200],keyboard_crop=[0,80,100,200],detected_key_rectangles=[dict(box_source_px=[20,100,80,140])],color_measurements={})
        spec=dict(reference_rows=[reference,dict(id='R02')]);(self.root/'spec/reference/ios-dark-v1.json').write_text(json.dumps(spec))
        self.record=self.root/'docs/testing/evidence/m2/synthetic.json'
        self.record.write_text(json.dumps(dict(**{'pass':False},pass_status='FAIL',reference_captures=[dict(id='r01-chinese',width=200,height=240,png_sha256=measurement.digest(self.actual),keys={'layout-primary':[40,40,160,120]})])))
        self.mock=patch.object(measurement,'ROOT',self.root);self.mock.start();self.addCleanup(self.mock.stop)
    def test_normalizes_crop_offset_and_retains_missing_and_failed_scope(self):
        result=measurement.measure(self.record);row=result['references'][0]
        self.assertEqual(row['height_delta_px'],0)
        self.assertEqual(row['max_abs_edge_delta_px'],0)
        self.assertEqual(row['matched_rectangles'],1)
        self.assertEqual(result['references'][1]['status'],'NOT_CAPTURED')
        self.assertFalse(result['input_scope_pass'])
        self.assertEqual(result['status'],'MEASUREMENTS_ONLY')
    def test_changed_capture_is_rejected_before_measurement(self):
        self.actual.write_bytes(self.actual.read_bytes()+b'changed')
        with self.assertRaisesRegex(ValueError,'E_IMAGE_HASH'):measurement.measure(self.record)
    def test_changed_reference_is_rejected_before_measurement(self):
        self.source.write_bytes(self.source.read_bytes()+b'changed')
        with self.assertRaisesRegex(ValueError,'E_IMAGE_HASH'):measurement.measure(self.record)
