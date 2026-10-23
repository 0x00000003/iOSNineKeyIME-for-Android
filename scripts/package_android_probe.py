#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Build the small, no-model/no-upstream-Lua M1 APK resource pack."""
import hashlib
import argparse
import json
from pathlib import Path
import re
import shutil
import zipfile

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--prebuilt', action='store_true', help='Include only hash-verified public static tables from prepare_prebuilt_tables.py')
args=parser.parse_args()
source=ROOT/'.cache/probe-shared'
stage=ROOT/'.cache/android-probe-resources'
if stage.exists():raise FileExistsError(stage)
names=['default.yaml','wanxiang_t9.schema.yaml','wanxiang_algebra.yaml','wanxiang_symbols.yaml',
       'wanxiang_lite.dict.yaml','wanxiang_abbrev_t9.schema.yaml','wanxiang_abbrev_t9.dict.yaml',
       'wanxiang_phrase_t9.schema.yaml','custom_phrase.dict.yaml','dicts/t9_abbrev.dict.yaml',
       'lua/ime_probe.lua','LICENSE.wanxiang']
names += ['dicts/'+n+'.lite.dict.yaml' for n in ['zi','jichu','cuoyin','duoyin','diming','mingren','yiren']]
for name in names:
 if args.prebuilt and name.endswith('.dict.yaml'):continue
 p=stage/name
 p.parent.mkdir(parents=True,exist_ok=True)
 shutil.copy2(source/name,p)
schema=stage/'wanxiang_t9.schema.yaml'
text=schema.read_text()
engine='''engine:
  processors:
    - speller
    - selector
    - navigator
    - express_editor
  segmentors:
    - abc_segmentor
    - fallback_segmentor
  translators:
    - script_translator
  filters:
    - lua_filter@*ime_probe
    - uniquifier

'''
text,count=re.subn(r'^engine:\n.*?(?=^[a-z_]+:)',engine,text,flags=re.M|re.S)
assert count==1
schema.write_text(text)
private_text=text.replace('schema_id: wanxiang_t9', 'schema_id: wanxiang_t9_nolearn', 1)
private_text=private_text.replace('  enable_user_dict: true', '  enable_user_dict: false', 1)
assert private_text != text and '  enable_user_dict: true' not in private_text
(stage/'wanxiang_t9_nolearn.schema.yaml').write_text(private_text)
default=stage/'default.yaml'
default_text=default.read_text()
assert default_text.count('  - schema: wanxiang_t9\n')==1
default.write_text(default_text.replace('  - schema: wanxiang_t9\n', '  - schema: wanxiang_t9_nolearn\n  - schema: wanxiang_t9\n'))
# Test marker is original GPL code; data/config retains its own CC-BY attribution.
(stage/'NOTICE.txt').write_text('M1 compatibility probe only; no model.\nData/config: amzxyz and rime-wanxiang contributors, v18.1.7 lite asset 659385471, CC-BY-4.0.\nhttps://github.com/amzxyz/rime-wanxiang/tree/89075fe7b870dcf80249dfa0ece7474f1a960d91\nModified: no grammar; native-only pipeline plus test Lua marker; statistics and upstream Lua omitted; separate no-learning schema disables translator user dictionary.\nProbe Lua: this project, GPL-3.0-or-later.\n')
prebuilt_manifest=None
if args.prebuilt:
 raw=(ROOT/'third_party/prebuilt-tables.json').read_bytes()
 prebuilt_manifest=hashlib.sha256(raw).hexdigest()
 rows=json.loads(raw)['files']
 allowed={base+suffix for base in ['custom_phrase','wanxiang_abbrev_t9','wanxiang_lite'] for suffix in ['.table.bin','.reverse.bin']}
 assert {row['path'] for row in rows}=={'build/'+name for name in allowed} and len(rows)==6
 for row in rows:
  p=ROOT/'.cache/probe-prebuilt'/Path(row['path']).name
  assert p.stat().st_size==row['bytes'] and hashlib.sha256(p.read_bytes()).hexdigest()==row['sha256']
  destination=stage/row['path'];destination.parent.mkdir(exist_ok=True);shutil.copy2(p,destination)
 with (stage/'NOTICE.txt').open('a') as notice:
  notice.write('Static dictionary table/reverse binaries compiled with pinned librime 1.17.0 from the same public CC-BY-4.0 dictionaries; raw dictionary source retained in the build provenance, omitted from this runtime pack. No learned user data, prism, model or upstream tool programs included.\n')
output=ROOT/'android/trime/app/build/generated/imeAssets'
output.mkdir(parents=True,exist_ok=True)
archive=output/'ime-probe.zip'
with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for p in sorted(stage.rglob('*')):
  if not p.is_file():continue
  info=zipfile.ZipInfo(p.relative_to(stage).as_posix(),date_time=(2026,10,23,0,0,0))
  info.compress_type=zipfile.ZIP_DEFLATED
  info.external_attr=0o100644<<16
  z.writestr(info,p.read_bytes())
digest=hashlib.sha256(archive.read_bytes()).hexdigest()
(output/'ime-probe.sha256').write_text(digest+'\n')
manifest={'scope':'M1 compatibility APK only, NOT full product pack','version':'probe-3' if args.prebuilt else 'probe-2',
          'source_asset':659385471,'source_sha256':'4f76c49b70f711c10b6abdf142249d4f0e88dd5b00ffe380fd45b0cd1b39d42a',
          'archive_sha256':digest,'archive_bytes':archive.stat().st_size,
          'model':False,'upstream_lua':False,
          'prebuilt_tables':args.prebuilt,'prebuilt_manifest_sha256':prebuilt_manifest,
          'license_review':'individual data/config headers inspected; no file-level contrary grant observed; third-party app/native dependency audit remains incomplete',
          'files':[{'path':p.relative_to(stage).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'bytes':p.stat().st_size,
                    'license':'GPL-3.0-or-later' if p.name=='ime_probe.lua' else 'CC-BY-4.0' if p.name!='NOTICE.txt' else 'project attribution'}
                   for p in sorted(stage.rglob('*')) if p.is_file()]}
manifest_text=json.dumps(manifest,indent=2,ensure_ascii=False)+'\n'
(ROOT/'third_party/android-probe-resources.json').write_text(manifest_text)
(output/'ime-probe-manifest.json').write_text(manifest_text)
print('Generated deterministic M1 probe asset',archive.stat().st_size,digest)
