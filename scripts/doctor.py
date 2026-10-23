#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Read-only host inventory. Does not enumerate devices or inspect user data."""
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess


def safe_path(p):
    return str(p).replace(str(Path.home()), '~') if p else None


def version(command):
    try:
        result = subprocess.run(command, capture_output=True, text=True, timeout=15, check=False)
        return {'exit_code': result.returncode, 'summary': (result.stdout + result.stderr).strip().splitlines()[:3]}
    except (OSError, subprocess.TimeoutExpired) as exc:
        return {'status': 'UNAVAILABLE', 'reason': type(exc).__name__}


def main():
    result = {'scope': 'host inventory only; no Gradle/Android build or device test', 'os': platform.system(),
              'python': platform.python_version(), 'commands': {}, 'sdk_candidates': []}
    for tool in ['git', 'java', 'adb', 'gradle', 'cmake', 'ninja']:
        binary = shutil.which(tool)
        entry = {'path': safe_path(binary)}
        if binary:
            entry['version'] = version([binary, '-version' if tool == 'java' else '--version' if tool != 'adb' else 'version'])
        result['commands'][tool] = entry
    candidates = [os.environ.get('ANDROID_HOME'), os.environ.get('ANDROID_SDK_ROOT'), str(Path.home() / 'Library/Android/sdk')]
    seen = set()
    for raw in candidates:
        if not raw or raw in seen:
            continue
        seen.add(raw)
        sdk = Path(raw)
        item = {'path': safe_path(sdk), 'exists': sdk.is_dir(), 'components': {}}
        for kind in ['platforms', 'build-tools', 'ndk', 'cmake', 'cmdline-tools']:
            p = sdk / kind
            item['components'][kind] = sorted(x.name for x in p.iterdir() if x.is_dir()) if p.is_dir() else []
        result['sdk_candidates'].append(item)
    result['android_project_present'] = (Path(__file__).resolve().parents[1] / 'android/trime/gradlew').is_file()
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
