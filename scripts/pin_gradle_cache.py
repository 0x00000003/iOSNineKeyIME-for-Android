#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Pin first-acquired Gradle artifact bytes; never overwrite conflicting pins.

This is a TOFU inventory from this project's cache, NOT an independent publisher
signature or license audit. New downloads must be reviewed before another run.
"""
import hashlib
from pathlib import Path
import sys
import xml.etree.ElementTree as E

ROOT=Path(__file__).resolve().parents[1]
NS='https://schema.gradle.org/dependency-verification'
E.register_namespace('',NS)
E.register_namespace('xsi','http://www.w3.org/2001/XMLSchema-instance')
def q(name):return '{'+NS+'}'+name


def main():
    target=Path(sys.argv[1])
    tree=E.parse(target) if target.exists() else E.ElementTree(E.Element(q('verification-metadata')))
    root=tree.getroot()
    config=root.find(q('configuration'))
    if config is None:
        config=E.SubElement(root,q('configuration'))
        E.SubElement(config,q('verify-metadata')).text='true'
        E.SubElement(config,q('verify-signatures')).text='false'
    components=root.find(q('components'))
    if components is None: components=E.SubElement(root,q('components'))
    existing={(c.get('group'),c.get('name'),c.get('version')):c for c in components}
    cache=ROOT/'.cache/gradle/caches/modules-2/files-2.1'
    added=0
    for p in sorted(cache.glob('*/*/*/*/*')):
        if not p.is_file():continue
        group,name,version,_,filename=p.relative_to(cache).parts
        digest=hashlib.sha256(p.read_bytes()).hexdigest()
        key=(group,name,version)
        if key not in existing:
            existing[key]=E.SubElement(components,q('component'),dict(zip(('group','name','version'),key)))
        comp=existing[key]
        artifact=next((a for a in comp if a.get('name')==filename),None)
        if artifact is None:artifact=E.SubElement(comp,q('artifact'),{'name':filename})
        pins=[e.get('value') for e in artifact if e.tag==q('sha256')]
        if pins and digest not in pins:raise ValueError('Conflicting artifact; investigate '+':'.join(key)+'/'+filename)
        if not pins:
            E.SubElement(artifact,q('sha256'),{'value':digest,'origin':'First acquisition in project cache; TOFU, publisher audit pending'})
            added+=1
    components[:]=sorted(components,key=lambda c:(c.get('group'),c.get('name'),c.get('version')))
    E.indent(tree,space='   ')
    target.parent.mkdir(parents=True,exist_ok=True)
    tree.write(target,encoding='UTF-8',xml_declaration=True)
    print('Added',added,'artifact SHA256 pins; verification remains strict, TOFU origin explicit.')


if __name__=='__main__':main()
