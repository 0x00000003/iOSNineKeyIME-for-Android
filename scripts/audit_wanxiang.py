#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Static resource inventory. Does NOT certify runtime closure or licensing.

Reads only the verified release extraction; records literal references, unresolved
Lua file access and missing model separately. No upstream tool code is executed.
"""
import hashlib
import json
from pathlib import Path
import re
import sys


def audit(root):
    root = Path(root)
    files = []
    references = []
    operations = []
    for p in sorted(root.rglob('*')):
        if not p.is_file():
            continue
        if p.is_symlink():
            raise ValueError('resource symlink is forbidden')
        data = p.read_bytes()
        rel = p.relative_to(root).as_posix()
        files.append({'path': rel, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()})
        if p.suffix not in ('.yaml', '.lua', '.json'):
            continue
        # Dict body is data, not configuration (and can contain apparent code).
        text = data.decode('utf-8-sig')
        if p.name.endswith('.dict.yaml'):
            text = text.split('\n...', 1)[0]
        import_list = False
        for line_no, line in enumerate(text.splitlines(), 1):
            clean = line.split('#', 1)[0].strip()
            refs = []
            if p.suffix == '.yaml':
                m = re.match(r'dictionary:\s*[\"\']?([\w/.-]+)', clean)
                if m:
                    refs.append(('dictionary', m[1]+'.dict.yaml', False))
                if clean.startswith('import_tables:'):
                    import_list = True
                elif import_list:
                    m = re.match(r'-\s+([\w/.-]+)\s*$', clean)
                    if m:
                        refs.append(('import_table', m[1]+'.dict.yaml', False))
                    elif clean and not clean.startswith('-'):
                        import_list = False
                for name, optional in re.findall(r'([\w.-]+):/[^\s]*?(\?)?(?=\s|$)', clean):
                    refs.append(('include_or_patch', name+'.yaml', bool(optional)))
                for name in re.findall(r'lua_(?:processor|translator|filter)@\*([\w.]+)', clean):
                    refs.append(('lua_component', 'lua/'+name.replace('.', '/')+'.lua', False))
                m = re.match(r'opencc_config:\s*([\w.-]+)', clean)
                if m:
                    refs.append(('opencc', 'opencc/'+m[1], False))
                m = re.match(r'language:\s*([\w.-]+)', clean)
                if m:
                    refs.append(('model', m[1]+'.gram', False))
            if p.suffix == '.lua':
                for name in re.findall(r'(?:\brequire\s*\(?|\bpcall\s*\(\s*require\s*,)\s*[\"\']([\w./-]+)[\"\']', line):
                    refs.append(('lua_require', 'lua/'+name.replace('.', '/')+'.lua', False))
                for op in re.findall(r'\b(?:io\.(?:open|popen)|os\.(?:execute|remove|rename|getenv)|package\.loadlib|LevelDb|UserDb|Memory)\b', line):
                    operations.append({'source': rel, 'line': line_no, 'operation': op,
                                       'status': 'REVIEW_REQUIRED; literal scan may include comments'})
            if p.suffix == '.json':
                for name in re.findall(r'"file"\s*:\s*"([^"]+)"', line):
                    refs.append(('opencc_dictionary', 'opencc/'+name, False))
            for kind, target, optional in refs:
                if '..' in Path(target).parts or Path(target).is_absolute():
                    raise ValueError('unsafe resource reference')
                references.append({'source': rel, 'line': line_no, 'kind': kind, 'target': target,
                                   'optional': optional, 'exists': (root/target).is_file()})
    return {'scope': 'static literal-reference inventory, NOT runtime closure or license approval',
            'files': files, 'references': references,
            'missing_references': [r for r in references if not r['exists']],
            'lua_operations_requiring_review': operations,
            'limits': ['Dynamic Lua require/file paths are not resolved.',
                       'Rime patches and YAML inheritance must be validated by real deployment.',
                       'License exceptions and embedded third-party attribution require manual review.']}


if __name__ == '__main__':
    result = audit(sys.argv[1])
    Path(sys.argv[2]).write_text(json.dumps(result, indent=2, ensure_ascii=False)+'\n')
    print('files:',len(result['files']), 'references:',len(result['references']),
          'missing:',len(result['missing_references']), 'Lua review sites:',len(result['lua_operations_requiring_review']))
