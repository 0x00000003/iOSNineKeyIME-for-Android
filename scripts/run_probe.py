#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Run only this project's synthetic probe, without selecting an IME or logging a serial."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target', choices=['pixel', 'emulator-5580'], required=True)
    parser.add_argument('--record', required=True, help='New evidence name, e.g. extended-pixel')
    parser.add_argument('--scope', choices=['full', 'editor', 'engine', 'resources', 'controller', 'quality', 'learning', 'context'], default='full')
    args = parser.parse_args()
    if not re.fullmatch(r'[a-z0-9-]+', args.record):
        raise ValueError('E_RECORD_NAME')
    evidence = ROOT / 'docs/testing/evidence/m1' / (args.record + '.json')
    raw_output = ROOT / 'artifacts/m1' / (args.record + '-instrument.txt')
    if evidence.exists() or raw_output.exists():
        raise FileExistsError('E_EXISTING_RECORD')
    selected = (ROOT / '.private/test-device').read_text().strip()
    target = selected if args.target == 'pixel' else args.target
    adb = ROOT / '.cache/android-sdk/platform-tools/adb'

    def call(*arguments):
        try:
            r = subprocess.run([str(adb), '-s', target, *arguments], capture_output=True, text=True, timeout=300)
        except subprocess.TimeoutExpired:
            raise RuntimeError('E_ADB_TIMEOUT') from None
        if r.returncode:
            raise RuntimeError('E_ADB_STEP')  # Command/exception repr could contain the serial.
        return (r.stdout + r.stderr).replace(selected, '<selected-device>')

    model = call('shell', 'getprop', 'ro.product.model').strip()
    if args.target == 'pixel' and model != 'Pixel 9 Pro':
        raise RuntimeError('E_SELECTED_MODEL')
    if args.target != 'pixel' and call('shell', 'getprop', 'ro.kernel.qemu').strip() != '1':
        raise RuntimeError('E_NOT_OWN_EMULATOR')
    outputs = ROOT / 'android/trime/app/build/outputs/apk'
    apk, = (outputs / 'debug').glob('*.apk')
    test, = (outputs / 'androidTest/debug').glob('*.apk')
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    test_digest = hashlib.sha256(test.read_bytes()).hexdigest()
    frozen_test = ROOT / 'artifacts/m1' / (args.record + '-test-' + test_digest[:12] + '.apk')
    shutil.copy2(test, frozen_test)
    frozen = ROOT / 'artifacts/m1' / (args.record + '-' + digest[:12] + '.apk')
    shutil.copy2(apk, frozen)
    installation = []
    for file, package, expected_sha in ((frozen, 'dev.local.iosninekeyime.probe.debug', digest),
                                       (frozen_test, 'dev.local.iosninekeyime.probe.debug.test', test_digest)):
        existing_matches = False
        try:
            locations = [line.removeprefix('package:').strip() for line in call('shell', 'pm', 'path', package).splitlines() if line.startswith('package:')]
            if len(locations) == 1 and re.fullmatch(r'/data/app/[A-Za-z0-9_~+./=-]+/base\.apk', locations[0]):
                actual = call('shell', 'sha256sum', locations[0]).split()[0]
                existing_matches = actual == expected_sha
        except (RuntimeError, IndexError):
            pass
        if existing_matches:
            installation.append(dict(package=package, state='existing APK bytes verified by SHA256'))
            continue
        try:
            if 'Success' not in call('install', '-r', str(file)):
                raise RuntimeError('E_INSTALL')
        except RuntimeError as error:
            evidence.write_text(json.dumps(dict(pass_=False, status='BLOCKED', scope='installation did not complete; assertions NOT_RUN',
                                               tested_apk_sha256=digest, tested_test_apk_sha256=test_digest,
                                               phase='install', error_code=str(error)), indent=2).replace('"pass_"', '"pass"') + '\n')
            raise
        installation.append(dict(package=package, state='installed local frozen APK'))
    run_id = uuid.uuid4().hex
    output = call('shell', 'am', 'instrument', '-w', '-r', '-e', 'probe_scope', args.scope, '-e', 'probe_run_id', run_id,
                  'dev.local.iosninekeyime.probe.debug.test/dev.local.iosninekeyime.probe.ProbeInstrumentation')
    raw_output.write_text(output)
    result = None
    try:
        candidate = json.loads(call('shell', 'run-as', 'dev.local.iosninekeyime.probe.debug',
                                   'cat', 'no_backup/m1/probe-result.json'))
        if candidate.get('probe_run_id') == run_id:
            result = candidate
    except (RuntimeError, json.JSONDecodeError):
        pass
    if result is None:
        evidence.write_text(json.dumps({'pass': False, 'scope': 'instrumentation assertions failed; no stale result consumed',
                                           'tested_apk_sha256': digest}, indent=2) + '\n')
        raise RuntimeError('E_PROBE_ASSERTIONS')
    result.update(tested_apk_sha256=digest, tested_test_apk_sha256=test_digest, installation=installation,
                  device='Pixel 9 Pro / Android 17 API 37' if args.target == 'pixel' else 'dedicated AOSP API36 ARM64 emulator',
                  requested_scope=args.scope,
                  scope_note='synthetic component integration; NOT full IME, first install, ten-word learning or all-path privacy/performance acceptance')
    evidence.write_text(json.dumps(result, indent=2) + '\n')
    passed = result['pass'] and 'm1_probe_pass=true' in output and 'INSTRUMENTATION_CODE: -1' in output
    print(json.dumps(dict(target=args.target, passed=passed, elapsed_ms=result.get('elapsed_ms'), apk_sha256=digest)))
    if not passed:
        raise RuntimeError('E_PROBE_ASSERTIONS')


if __name__ == '__main__':
    main()
