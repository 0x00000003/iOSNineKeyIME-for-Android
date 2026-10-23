#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Decode actual APK manifest AND referenced backup XML. Static gate only."""
import hashlib
import json
from pathlib import Path
import subprocess
import re
import sys
import xml.etree.ElementTree as E
import zipfile

A='{http://schemas.android.com/apk/res/android}'
DOMAINS={'root','file','database','sharedpref','external','device_root','device_file','device_database','device_sharedpref'}


def manifest_errors(xml, release=False):
    root=E.fromstring(xml)
    errors=[]
    perms=[e.get(A+'name') for e in root if e.tag.startswith('uses-permission')]
    if 'android.permission.INTERNET' in perms:errors.append('INTERNET present, including sdk/maxSdk variants')
    if root.get(A+'sharedUserId'):errors.append('shared UID')
    app=root.find('application')
    if app is None:return ['missing application']
    if app.get(A+'allowBackup')!='false':errors.append('allowBackup must be explicitly false')
    if app.get(A+'backupAgent'):errors.append('backupAgent present')
    if release and app.get(A+'debuggable')=='true':errors.append('debuggable release')
    if not app.get(A+'fullBackupContent','').startswith(('@xml/', '@ref/0x')):errors.append('missing legacy backup XML')
    if not app.get(A+'dataExtractionRules','').startswith(('@xml/', '@ref/0x')):errors.append('missing extraction XML')
    for e in app:
        if e.tag=='service' and e.get(A+'exported')=='true' and e.get(A+'permission')!='android.permission.BIND_INPUT_METHOD':errors.append('unprotected exported service')
        if e.tag in ('provider','receiver') and e.get(A+'exported')=='true':errors.append('unexpected exported '+e.tag)
    return errors


def backup_errors(xml, modern):
    root=E.fromstring(xml)
    errors=[]
    if root.tag!=('data-extraction-rules' if modern else 'full-backup-content'):errors.append('unexpected backup root')
    groups=[root.find(n) for n in ('cloud-backup','device-transfer')] if modern else [root]
    for group in groups:
        if group is None:
            errors.append('missing backup/transfer section')
            continue
        excluded={e.get('domain') for e in group.findall('exclude') if e.get('path')=='.'}
        if excluded!=DOMAINS:errors.append('incomplete exclusion domains')
        if group.findall('include'):errors.append('unexpected include rule')
    return errors


def resolve_xml(value, table):
    if value.startswith('@xml/'):
        return ['res/xml/'+value.split('/',1)[1]+'.xml']
    match = re.search(r'^    resource '+re.escape(value.removeprefix('@ref/'))+r' [^\n]+\n(.*?)(?=^    resource|^  type|\Z)', table, flags=re.M|re.S)
    if not match:
        raise ValueError('APK backup resource reference not found')
    paths = re.findall(r'\(file\) ([^\s]+) type=XML', match[1])
    if not paths:
        raise ValueError('APK backup reference is not an XML file')
    return paths


def asset_errors(apk, expected):
    errors=[]
    with zipfile.ZipFile(apk) as archive:
        names=archive.namelist()
        for name,(sha,size) in expected.items():
            entry='assets/'+name
            if names.count(entry)!=1:
                errors.append('missing/duplicate locked runtime asset: '+name)
                continue
            info=archive.getinfo(entry)
            if info.file_size!=size:
                errors.append('locked runtime asset size mismatch: '+name)
                continue
            digest=hashlib.sha256()
            with archive.open(info) as stream:
                for block in iter(lambda:stream.read(1024*1024),b''):digest.update(block)
            if digest.hexdigest()!=sha:errors.append('locked runtime asset hash mismatch: '+name)
        if 'assets/runtime-artifacts-release.json' not in names:errors.append('missing exact runtime inventory')
    return errors


def main():
    apk=Path(sys.argv[1]).resolve()
    destination=Path(sys.argv[2])
    destination.mkdir(parents=True,exist_ok=True)
    tool=Path(__file__).resolve().parents[1]/'.cache/android-sdk/cmdline-tools/19.0/bin/apkanalyzer'
    def run(*args):return subprocess.check_output([str(tool),*args,str(apk)],text=True)
    manifest=run('manifest','print')
    (destination/'manifest.xml').write_text(manifest)
    errors=manifest_errors(manifest,release='--release' in sys.argv[3:])
    from ensure_runtime_assets import expected_assets, ROOT
    expected=expected_assets()
    notice=json.loads((ROOT/'third_party/offline-notice-sources.json').read_text())
    expected['offline-notices.json']=(notice['sha256'],notice['bytes'])
    errors.extend(asset_errors(apk,expected))
    app=E.fromstring(manifest).find('application')
    aapt=tool.parents[3]/'build-tools/36.0.0/aapt2'
    table=subprocess.check_output([str(aapt),'dump','resources',str(apk)],text=True)
    for attr,modern in [('fullBackupContent',False),('dataExtractionRules',True)]:
        value=app.get(A+attr,'')
        if not value.startswith(('@xml/', '@ref/0x')):continue
        for index,path in enumerate(resolve_xml(value,table)):
            content=run('resources','xml','--file',path)
            (destination/(attr+'-'+str(index)+'.xml')).write_text(content)
            errors.extend(backup_errors(content,modern))
    result={'scope':'static APK manifest/packaged backup XML/locked runtime assets, NOT target-OS backup/network/learning acceptance',
            'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'bytes':apk.stat().st_size,
            'package':E.fromstring(manifest).get('package'),'errors':errors,'pass':not errors}
    (destination/'audit.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result))
    return bool(errors)


if __name__=='__main__':sys.exit(main())
