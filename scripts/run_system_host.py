#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Run the separate synthetic host against the actual selected IME. No warning bypass/IME enable."""
import argparse,hashlib,json,re,subprocess,uuid
from pathlib import Path
from run_probe import ROOT,selected_apk,freeze_apk,streamed_result


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target',choices=['pixel','emulator-5580'],required=True)
    parser.add_argument('--scope',choices=['readiness','core','private-delete','privacy-queue','reference-ui','reference-pixels','restore-pixels','private-pixels','queue-pixels','lifecycle','lifecycle-diagnostic','lifecycle-sync-diagnostic'],required=True)
    parser.add_argument('--variant',choices=['debug','release'],default='debug')
    parser.add_argument('--record',required=True)
    args=parser.parse_args()
    if not re.fullmatch('[a-z0-9-]+',args.record):raise ValueError('E_RECORD_NAME')
    evidence=ROOT/'docs/testing/evidence/m2'/(args.record+'.json');evidence.parent.mkdir(exist_ok=True)
    raw=ROOT/'artifacts/testhost'/(args.record+'-instrument.txt');raw.parent.mkdir(exist_ok=True)
    if evidence.exists()or raw.exists():raise FileExistsError('E_EXISTING_RECORD')
    selected=(ROOT/'.private/test-device').read_text().strip()
    target=selected if args.target=='pixel'else args.target
    adb=ROOT/'.cache/android-sdk/platform-tools/adb'
    def call(*parameters,timeout=45):
        try:p=subprocess.run([str(adb),'-s',target,*parameters],capture_output=True,text=True,timeout=timeout)
        except subprocess.TimeoutExpired:raise RuntimeError('E_ADB_TIMEOUT')from None
        if p.returncode:raise RuntimeError('E_ADB_STEP')
        return (p.stdout+p.stderr).replace(selected,'<selected-device>')
    if args.target=='pixel'and call('shell','getprop','ro.product.model').strip()!='Pixel 9 Pro':raise RuntimeError('E_SELECTED_MODEL')
    if args.target!='pixel'and call('shell','getprop','ro.kernel.qemu').strip()!='1':raise RuntimeError('E_OWN_EMULATOR')
    ime='dev.local.iosninekeyime.probe'+('.debug'if args.variant=='debug'else'')
    component=ime+'/dev.local.iosninekeyime.ime.NineKeyInputMethodService'
    def installed_sha(package):
        paths=[s[8:].strip()for s in call('shell','pm','path',package).splitlines()if s.startswith('package:')]
        if len(paths)!=1 or not re.fullmatch('/data/app/[A-Za-z0-9_~+./=-]+/base.apk',paths[0]):raise RuntimeError('E_INSTALLED_APK')
        value=call('shell','sha256sum',paths[0]).split()[0]
        if not re.fullmatch('[a-f0-9]{64}',value):raise RuntimeError('E_INSTALLED_HASH')
        return value
    ime_sha=installed_sha(ime)
    selection='unchanged'
    if args.scope!='readiness':
        current=call('shell','settings','get','secure','default_input_method').strip()
        if current!=component:
            enabled=[s.split(';')[0]for s in call('shell','settings','get','secure','enabled_input_methods').strip().split(':')]
            if component not in enabled:
                evidence.write_text(json.dumps({'status':'BLOCKED','scope':args.scope,'pass':False,'reason':'own IME not enabled; runner never bypasses system enable warning','ime_apk_sha256':ime_sha},indent=2)+'\n')
                raise RuntimeError('E_OWN_IME_NOT_ENABLED')
            # Explicit user authorization recorded in AGENTS: select already-enabled own variants only.
            call('shell','ime','set',component)
            if call('shell','settings','get','secure','default_input_method').strip()!=component:raise RuntimeError('E_SELECTION')
            selection='selected already-enabled own IME under explicit user authorization'
    package='dev.local.iosninekeyime.testhost';out=ROOT/'android/trime/testhost/build/outputs/apk'
    hashes={}
    for rel,name,pkg in [('debug','host',package),('androidTest/debug','test',package+'.test')]:
        apk=selected_apk(out/rel,pkg);sha=hashlib.sha256(apk.read_bytes()).hexdigest();hashes[name+'_apk_sha256']=sha
        frozen=raw.parent/(args.record+'-'+name+'-'+sha[:12]+'.apk');freeze_apk(apk,frozen,sha)
        match=False
        try:match=installed_sha(pkg)==sha
        except RuntimeError:pass
        if not match and 'Success'not in call('install','-r',str(frozen),timeout=120):raise RuntimeError('E_HOST_INSTALL')
        if installed_sha(pkg)!=sha:raise RuntimeError('E_INSTALLED_HASH')
    nonce=uuid.uuid4().hex
    output=call('shell','am','instrument','-w','-r','-e','host_scope',args.scope,'-e','host_run_id',nonce,'-e','ime_package',ime,package+'.test/dev.local.iosninekeyime.testhost.HostInstrumentation',timeout=300)
    raw.write_text(output)
    result=streamed_result(output,nonce,'host_run_id')
    if result is None:result=dict(status='FAIL',pass_=False,reason='missing nonce-bound instrumentation result')
    if 'pass_'in result:result['pass']=result.pop('pass_')
    for capture in result.get('reference_captures',[]):
        name=capture.get('id','')
        if not re.fullmatch(r'r0[1-5]-(chinese|number|symbols|latin|emoji|pressed)',name):raise RuntimeError('E_CAPTURE_NAME')
        shot=subprocess.run([str(adb),'-s',target,'exec-out','run-as',package,'cat','cache/host-'+nonce+'-'+name+'.png'],capture_output=True,timeout=30)
        if shot.returncode or not shot.stdout.startswith(b'\x89PNG\r\n\x1a\n')or len(shot.stdout)>10*1024*1024:raise RuntimeError('E_CAPTURE_TRANSPORT')
        destination=raw.parent/(args.record+'-'+name+'.png')
        with destination.open('xb')as output_image:output_image.write(shot.stdout)
        capture['png_sha256']=hashlib.sha256(shot.stdout).hexdigest()
        capture['pixel_storage']='ignored local artifacts/testhost; synthetic IME crop only'
    if result.get('debug_ime_crop_saved'):
        # Owned synthetic keyboard crop only; raw image stays in ignored local artifacts.
        shot=subprocess.run([str(adb),'-s',target,'exec-out','run-as',package,'cat','cache/synthetic-ime-failure.png'],capture_output=True,timeout=30)
        if shot.returncode==0 and shot.stdout.startswith(b'\x89PNG\r\n\x1a\n')and len(shot.stdout)<=10*1024*1024:
            (raw.parent/(args.record+'-ime.png')).write_bytes(shot.stdout)
    if result.get('debug_owned_bottom_crop_saved'):
        shot=subprocess.run([str(adb),'-s',target,'exec-out','run-as',package,'cat','cache/host-'+nonce+'-bottom-diagnostic.png'],capture_output=True,timeout=30)
        if shot.returncode==0 and shot.stdout.startswith(b'\x89PNG\r\n\x1a\n')and len(shot.stdout)<=10*1024*1024:
            (raw.parent/(args.record+'-owned-bottom.png')).write_bytes(shot.stdout)

    result.update(hashes);result.update(ime_apk_sha256=ime_sha,ime_variant=args.variant,requested_scope=args.scope,target=args.target,selection=selection,
        transport='instrumentation stream + fresh host_run_id + success flag/code',scope_note='actual selected system IME in a separate synthetic host UID; metadata readiness alone is NOT system IME acceptance')
    result['pass']=bool(result.get('pass'))and 'INSTRUMENTATION_RESULT: host_pass=true'in output and 'INSTRUMENTATION_CODE: -1'in output
    if installed_sha(ime)!=ime_sha:result.update(pass_=False,status='FAIL',reason='IME APK changed during test');result['pass']=result.pop('pass_')
    evidence.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(dict(target=args.target,scope=args.scope,status=result.get('status'),passed=result['pass'],ime_apk_sha256=ime_sha)))
    if not result['pass']:raise RuntimeError('E_SYSTEM_HOST_ASSERTIONS')


if __name__=='__main__':main()
