#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Bootstrap fresh project-local SDK/JDK/cache from verified archives. Never invokes adb."""
import argparse,hashlib,json,os,shutil,stat,subprocess,tarfile,zipfile
from pathlib import Path,PurePosixPath
from artifact_utils import verify
ROOT=Path(__file__).resolve().parents[1]


def safe_parts(name):
    parts=PurePosixPath(name).parts
    if not parts or name.startswith('/') or '..'in parts or '\\'in name or '\0'in name:raise ValueError('E_TOOL_ARCHIVE_PATH')
    return parts


def fetch_archive(row,folder):
    if len(safe_parts(row['archive_file']))!=1:raise ValueError('E_TOOL_ARCHIVE_NAME')
    folder.mkdir(parents=True,exist_ok=True)
    folder.resolve().relative_to(ROOT)
    target=folder/row['archive_file']
    if not target.exists():
        if not row['url'].startswith('https://'):raise ValueError('E_TOOL_DOWNLOAD_SCHEME')
        partial=target.with_name(target.name+'.part')
        with target.with_name(target.name+'.curl.txt').open('w')as log:
            run=subprocess.run(['curl','--fail','--location','--proto','=https','--proto-redir','=https',
                                '--tlsv1.2','--retry','3','--connect-timeout','30','--max-time','900',
                                '--continue-at','-','--output',str(partial),row['url']],stdout=log,stderr=log,timeout=1200)
        if run.returncode:raise RuntimeError('E_TOOL_DOWNLOAD')
        verify(partial,row['sha256'],row['bytes'])
        partial.replace(target)
    verify(target,row['sha256'],row['bytes'])
    return target


def install_zip(archive,dest,expected_properties):
    if dest.exists():raise FileExistsError('E_TOOL_DEST_EXISTS')
    with zipfile.ZipFile(archive)as z:
        names=z.namelist();properties=[n for n in names if n=='source.properties'or n.endswith('/source.properties')]
        if len(properties)!=1:raise ValueError('E_TOOL_PROPERTIES_COUNT')
        if hashlib.sha256(z.read(properties[0])).hexdigest()!=expected_properties:raise ValueError('E_TOOL_PROPERTIES_PIN')
        prefix=PurePosixPath(properties[0]).parent
        entries=[];seen=set()
        for info in z.infolist():
            safe_parts(info.filename)
            rel=PurePosixPath(info.filename).relative_to(prefix)
            if str(rel)=='.':continue
            folded=str(rel).casefold()
            if folded in seen:raise ValueError('E_TOOL_ARCHIVE_ALIAS')
            seen.add(folded)
            mode=info.external_attr>>16;kind=stat.S_IFMT(mode)
            if not info.is_dir()and kind not in (0,stat.S_IFREG,stat.S_IFLNK):raise ValueError('E_TOOL_ARCHIVE_TYPE')
            entries.append((info,Path(*rel.parts),mode,kind))
        dest.mkdir(parents=True)
        expected=[];links=[]
        # No symlink exists during writes; an archive cannot redirect file creation outside dest.
        for info,rel,mode,kind in entries:
            target=dest/rel
            if info.is_dir():target.mkdir(parents=True,exist_ok=True);continue
            target.parent.mkdir(parents=True,exist_ok=True)
            if kind==stat.S_IFLNK:
                value=z.read(info).decode('utf-8')
                if value.startswith('/')or'\0'in value:raise ValueError('E_TOOL_LINK')
                links.append((target,value));continue
            d=hashlib.sha256()
            with z.open(info)as src,target.open('xb')as out:
                for block in iter(lambda:src.read(1024*1024),b''):out.write(block);d.update(block)
            target.chmod((mode & 0o777) or 0o644)
            expected.append((target,d.hexdigest(),info.file_size))
        for target,value in links:target.symlink_to(value)
        for target,_ in links:target.resolve(strict=False).relative_to(dest.resolve())
        manifest=hashlib.sha256()
        for target,sha,size in sorted(expected):
            verify(target,sha,size)
            manifest.update((target.relative_to(dest).as_posix()+'\0'+sha+'\0'+str(size)+'\n').encode())
        for target,value in sorted(links):
            if os.readlink(target)!=value:raise ValueError('E_TOOL_LINK_CHANGED')
            manifest.update((target.relative_to(dest).as_posix()+'\0link\0'+value+'\n').encode())
        return dict(files_verified=len(expected),symlinks_verified=len(links),payload_manifest_sha256=manifest.hexdigest())


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--destination',type=Path,required=True);parser.add_argument('--source-commit',required=True)
    parser.add_argument('--fetch-archives',action='store_true',help='Fetch missing exact SHA256-locked public archives over HTTPS')
    args=parser.parse_args();base=(ROOT/args.destination).resolve();base.relative_to(ROOT)
    if base.exists():raise FileExistsError('E_BOOTSTRAP_DEST_EXISTS')
    if shutil.disk_usage(ROOT).free<12*1024**3:raise RuntimeError('E_BOOTSTRAP_SPACE_BUDGET')
    source_commit=subprocess.run(['git','rev-parse','--verify','--end-of-options',args.source_commit+'^{commit}'],cwd=ROOT,capture_output=True,text=True,check=True).stdout.strip()
    if len(source_commit)!=40 or any(ch not in'0123456789abcdef'for ch in source_commit):raise ValueError('E_SOURCE_COMMIT')
    base.mkdir();source=base/'source';source.mkdir()
    archive=base/'source.tar';subprocess.run(['git','archive','--format=tar','--output='+str(archive),source_commit],cwd=ROOT,check=True)
    with tarfile.open(archive)as t:
        for member in t.getmembers():
            safe_parts(member.name)
            if not member.isfile()and not member.isdir():raise ValueError('E_SOURCE_ARCHIVE_TYPE')
        t.extractall(source)
    cache=source/'.cache';cache.mkdir()
    # Public data only; generation re-verifies all source/table/model hashes before APK packaging.
    for name in ['upstream','probe-prebuilt']:(cache/name).symlink_to(ROOT/'.cache'/name,target_is_directory=True)
    inventory=json.loads((ROOT/'third_party/toolchain-inventory.json').read_text())
    properties={r['path']:r['source_properties_sha256']for r in inventory['sdk_packages']}
    rows=json.loads((ROOT/'third_party/toolchain-payloads.json').read_text())['artifacts'];results=[];java_home=None
    for row in rows:
        folder=ROOT/'.cache/clean-toolchain-downloads'
        payload=fetch_archive(row,folder)if args.fetch_archives else folder/row['archive_file']
        verify(payload,row['sha256'],row['bytes'])
        if row['id'].startswith('jdk;'):
            dest=cache/'jdk';dest.mkdir()
            with tarfile.open(payload)as t:
                members=t.getmembers()
                for member in members:
                    safe_parts(member.name)
                    if not member.isfile()and not member.isdir():raise ValueError('E_JDK_ARCHIVE_TYPE')
                    member.mode&=0o777
                t.extractall(dest)
                roots={m.name.split('/')[0]for m in members}
                if len(roots)!=1:raise ValueError('E_JDK_ROOT')
                java_home=dest/roots.pop()/'Contents/Home'
                manifest=hashlib.sha256();count=0
                for member in sorted(members,key=lambda m:m.name):
                    if not member.isfile():continue
                    digest=hashlib.sha256()
                    with t.extractfile(member)as stream:
                        for block in iter(lambda:stream.read(1024*1024),b''):digest.update(block)
                    digest=digest.hexdigest()
                    verify(dest/member.name,digest,member.size);count+=1
                    manifest.update((member.name+'\0'+digest+'\0'+str(member.size)+'\n').encode())
            result=dict(id=row['id'],archive_sha256=row['sha256'],archive_members=len(members),files_verified=count,payload_manifest_sha256=manifest.hexdigest())
        else:
            path=row['sdk_path'];dest=cache/'android-sdk'/path
            result=install_zip(payload,dest,properties[path]);result.update(id=row['id'],archive_sha256=row['sha256'])
            metadata=row['local_package_metadata']
            if metadata:
                original=ROOT/metadata['path'];verify(original,metadata['sha256']);shutil.copy2(original,dest/'package.xml')
        results.append(result);print(json.dumps(dict(installed=row['id'],verified=True)),flush=True)
    licenses=cache/'android-sdk/licenses';licenses.mkdir()
    license_markers=[]
    for original in sorted((ROOT/'.cache/android-sdk/licenses').iterdir()):
        if not original.is_file():continue
        raw=original.read_bytes()
        if len(raw)>4096 or any(chr(b)not in'0123456789abcdefABCDEF\r\n 'for b in raw):raise ValueError('E_LICENSE_MARKER')
        shutil.copy2(original,licenses/original.name);license_markers.append(dict(file=original.name,sha256=hashlib.sha256(raw).hexdigest()))
    if not license_markers:raise RuntimeError('E_PRIOR_SDK_ACCEPTANCE_REQUIRED')
    version=subprocess.run([str(java_home/'bin/java'),'-version'],capture_output=True,text=True,check=True)
    if 'Temurin-21.0.7+6'not in version.stderr+version.stdout:raise RuntimeError('E_FRESH_JDK_PIN')
    record=dict(scope='fresh SDK/JDK executable payloads checked against pinned downloaded archives; empty Gradle cache; public data/table/model cache and existing fixed-term SDK acceptance reused; host OS unchanged; no adb or signing key used',source_commit=args.source_commit,packages=results,license_acceptance_markers=license_markers,gradle_cache_initially_empty=not(cache/'gradle').exists(),java_version=(version.stderr+version.stdout).strip().splitlines(),build='NOT_RUN')
    (base/'bootstrap-receipt.json').write_text(json.dumps(record,indent=2)+'\n')
    env=dict(os.environ,JAVA_HOME=str(java_home))
    # Pin the launcher and disable discovery of other JDK installations for toolchain selection.
    command=[str(source/'scripts/gradle.sh'),'-Porg.gradle.java.installations.auto-detect=false','-Porg.gradle.java.installations.paths='+str(java_home),':app:assembleRelease']
    record['build_command']=command
    with(base/'gradle-build.txt').open('x')as log:run=subprocess.run(command,cwd=source,env=env,stdout=log,stderr=subprocess.STDOUT)
    record['build_exit_code']=run.returncode;record['build']='PASS'if run.returncode==0 else'FAIL'
    (base/'bootstrap-receipt.json').write_text(json.dumps(record,indent=2)+'\n')
    print(json.dumps(dict(build_exit_code=run.returncode,source_commit=args.source_commit)),flush=True)
    if run.returncode:raise RuntimeError('E_FRESH_TOOLCHAIN_BUILD')


if __name__=='__main__':main()
