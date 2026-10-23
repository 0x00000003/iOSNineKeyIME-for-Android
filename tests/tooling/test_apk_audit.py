# SPDX-License-Identifier: GPL-3.0-or-later
import importlib.util
from pathlib import Path
import unittest
S=importlib.util.spec_from_file_location('apk',Path(__file__).resolve().parents[2]/'scripts/audit_apk.py')
a=importlib.util.module_from_spec(S);S.loader.exec_module(a)

class ApkAuditTests(unittest.TestCase):
    def xml(self,perm='',attrs='',components=''):
        return '<manifest xmlns:android="http://schemas.android.com/apk/res/android">'+perm+'<application android:allowBackup="false" android:fullBackupContent="@xml/backup" android:dataExtractionRules="@xml/extraction" '+attrs+'>'+components+'</application></manifest>'
    def test_internet_even_with_max_sdk(self):
        self.assertTrue(a.manifest_errors(self.xml('<uses-permission-sdk-23 android:name="android.permission.INTERNET" android:maxSdkVersion="23"/>')))
    def test_backup_agent_and_debuggable_release(self):
        self.assertTrue(a.manifest_errors(self.xml(attrs='android:backupAgent="Agent"')))
        self.assertTrue(a.manifest_errors(self.xml(attrs='android:debuggable="true"'),release=True))
    def test_exported_ime_requires_bind_permission(self):
        self.assertTrue(a.manifest_errors(self.xml(components='<service android:exported="true"/>')))
        self.assertFalse(a.manifest_errors(self.xml(components='<service android:exported="true" android:permission="android.permission.BIND_INPUT_METHOD"/>')))
    def test_device_transfer_cannot_be_missing_or_incomplete(self):
        e=''.join('<exclude domain="'+d+'" path="."/>' for d in a.DOMAINS)
        self.assertTrue(a.backup_errors('<data-extraction-rules><cloud-backup>'+e+'</cloud-backup></data-extraction-rules>',True))
        self.assertFalse(a.backup_errors('<data-extraction-rules><cloud-backup>'+e+'</cloud-backup><device-transfer>'+e+'</device-transfer></data-extraction-rules>',True))
    def test_valid_probe_manifest(self):
        self.assertEqual(a.manifest_errors(self.xml()),[])

    def test_numeric_xml_reference_is_resolved_from_actual_resource_table(self):
        table='    resource 0x7f010001 xml/rules\n      () (file) res/xml/rules.xml type=XML\n      (v31) (file) res/xml-v31/rules.xml type=XML\n'
        self.assertEqual(a.resolve_xml('@ref/0x7f010001',table),['res/xml/rules.xml','res/xml-v31/rules.xml'])
        with self.assertRaises(ValueError):a.resolve_xml('@ref/0x7f000000',table)

    def test_clean_build_missing_or_altered_assets_cannot_pass(self):
        import hashlib,io,zipfile
        expected={'ime-probe.zip':(hashlib.sha256(b'locked').hexdigest(),6)}
        for content in [None,b'broken',b'locked']:
            stream=io.BytesIO()
            with zipfile.ZipFile(stream,'w') as archive:
                archive.writestr('assets/runtime-artifacts-release.json','[]')
                if content is not None:archive.writestr('assets/ime-probe.zip',content)
            stream.seek(0)
            self.assertEqual(bool(a.asset_errors(stream,expected)),content!=b'locked')

    def test_font_license_requires_exact_packaged_binary_not_parent_pom(self):
        import hashlib,io,zipfile
        stream=io.BytesIO()
        with zipfile.ZipFile(stream,'w') as archive:archive.writestr('res/obfuscated.ttf',b'reviewed-font')
        stream.seek(0)
        self.assertTrue(a.font_errors(stream,{'0'*64})[0])
        stream.seek(0)
        self.assertFalse(a.font_errors(stream,{hashlib.sha256(b'reviewed-font').hexdigest()})[0])
