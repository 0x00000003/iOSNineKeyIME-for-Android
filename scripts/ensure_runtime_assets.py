#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Build the exact locked APK assets from verified public cache inputs; fail closed."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
from artifact_utils import verify

ROOT=Path(__file__).resolve().parents[1]


def expected_assets(root=ROOT):
    manifest=(root/'third_party/android-probe-resources.json').read_bytes()
    data=json.loads(manifest)
    rows={'ime-probe.zip':(data['archive_sha256'],data['archive_bytes']),
          'ime-probe-manifest.json':(hashlib.sha256(manifest).hexdigest(),len(manifest)),
          'ime-probe.sha256':(hashlib.sha256((data['archive_sha256']+'\n').encode()).hexdigest(),65)}
    if data['model']:
        model=data['model_asset'];rows[model['name']]=(model['sha256'],model['bytes'])
    return rows


def validate_assets(folder, root=ROOT):
    rows=expected_assets(root)
    if {p.name for p in folder.iterdir()}!=set(rows):raise ValueError('E_ASSET_OUTPUT_CONTENT')
    for name,(sha,size) in rows.items():
        path=folder/name
        if path.is_symlink() or not path.is_file():raise ValueError('E_ASSET_OUTPUT_PATH')
        verify(path,sha,size)


def main():
    build=ROOT/'android/trime/app/build/generated'
    build.mkdir(parents=True,exist_ok=True)
    output=build/'imeAssets'
    if output.is_symlink():raise ValueError('E_ASSET_OUTPUT_PATH')
    with tempfile.TemporaryDirectory(prefix='runtime-assets-',dir=build) as temp:
        work=Path(temp)
        subprocess.run([sys.executable,str(ROOT/'scripts/prepare_probe.py'),'--stage',str(work/'source'),'--no-write-manifest'],check=True)
        subprocess.run([sys.executable,str(ROOT/'scripts/package_android_probe.py'),'--prebuilt','--octagram-model',
                        '--source',str(work/'source'),'--stage',str(work/'stage'),'--output',str(work/'assets'),'--no-write-manifest'],check=True)
        validate_assets(work/'assets')
        output.mkdir(exist_ok=True)
        allowed=set(expected_assets())
        if any(p.name not in allowed or p.is_symlink() for p in output.iterdir()):raise ValueError('E_ASSET_OUTPUT_CONTENT')
        for source in (work/'assets').iterdir():
            destination=output/source.name
            if destination.is_file() and destination.stat().st_size==source.stat().st_size and hashlib.sha256(destination.read_bytes()).digest()==hashlib.sha256(source.read_bytes()).digest():continue
            source.replace(destination)
    validate_assets(output)
    print(json.dumps(dict(scope='locked build-host APK assets only; not Android runtime acceptance',files=len(expected_assets()),pass_=True)).replace('"pass_"','"pass"'))


if __name__=='__main__':main()
