#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Scan ONLY the selected synthetic app UID's logcat in RAM. Never save raw lines."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT=Path(__file__).resolve().parents[1]
PACKAGE='dev.local.iosninekeyime.probe.debug'


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target',choices=['pixel','emulator-5580'],required=True)
    parser.add_argument('--record',required=True)
    args=parser.parse_args()
    if not re.fullmatch('[a-z0-9-]+',args.record):raise ValueError('E_RECORD_NAME')
    output=ROOT/'docs/testing/evidence/m1'/(args.record+'.json')
    if output.exists():raise FileExistsError('E_EXISTING_RECORD')
    selected=(ROOT/'.private/test-device').read_text().strip()
    target=selected if args.target=='pixel' else args.target
    adb=ROOT/'.cache/android-sdk/platform-tools/adb'
    def call(*parameters):
        try:r=subprocess.run([str(adb),'-s',target,'shell',*parameters],capture_output=True,timeout=45)
        except subprocess.TimeoutExpired:raise RuntimeError('E_OBSERVATION_TIMEOUT')from None
        if r.returncode:raise RuntimeError('E_OBSERVATION_UNAVAILABLE')
        return r.stdout
    if args.target=='pixel' and call('getprop','ro.product.model').strip()!=b'Pixel 9 Pro':raise RuntimeError('E_SELECTED_MODEL')
    if args.target!='pixel' and call('getprop','ro.kernel.qemu').strip()!=b'1':raise RuntimeError('E_NOT_OWN_EMULATOR')
    match=re.search(rb'^package:'+re.escape(PACKAGE.encode())+rb' uid:(\d+)\s*$',call('cmd','package','list','packages','-U',PACKAGE),re.M)
    if not match:raise RuntimeError('E_OWN_UID')
    uid=match.group(1).decode()
    fixture=json.loads((ROOT/'tests/fixtures/privacy-canary-v1.json').read_text())
    def count():
        logs=call('logcat','-d','--uid='+uid,'-b','main','-b','system','-v','brief')
        if len(logs)>4*1024*1024:raise RuntimeError('E_LOG_BUDGET')
        # Only counts escape memory. No raw logs, excerpts or input-bearing tags are written.
        return logs.count(fixture['text'].encode())+logs.count(fixture['text'].encode('utf-16-le'))
    before=count()
    run_record=args.record+'-controller'
    run=subprocess.run(['python3',str(ROOT/'scripts/run_probe.py'),'--target',args.target,'--scope','controller','--record',run_record],cwd=ROOT,capture_output=True,text=True,timeout=400)
    after=count()
    evidence=json.loads((ROOT/'docs/testing/evidence/m1'/(run_record+'.json')).read_text())
    result=dict(scope='synthetic private direct-text fixture, app UID main/system logcat only; raw lines NEVER persisted; NOT system-UID/other-host/all-path SEC04 acceptance',
                fixture_id=fixture['id'],target=args.target,canary_matches_before=before,canary_matches_after=after,
                synthetic_controller_pass=evidence.get('pass'),tested_apk_sha256=evidence.get('tested_apk_sha256'),
                tested_test_apk_sha256=evidence.get('tested_test_apk_sha256'),
                pass_=run.returncode==0 and evidence.get('pass')and before==0 and after==0)
    result['pass']=result.pop('pass_')
    output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(dict(target=args.target,passed=result['pass'],canary_matches_after=after)))
    if not result['pass']:raise RuntimeError('E_CANARY_OBSERVATION')


if __name__=='__main__':main()
