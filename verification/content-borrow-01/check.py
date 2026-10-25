from pathlib import Path
import argparse,json,hashlib,subprocess,os,difflib
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1];WORKSPACE=ROOT.parent
p=argparse.ArgumentParser();p.add_argument('--out',required=True,type=Path);a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
report={'status':'running','new_translated_files':0,'commands':[]}
def run(label,argv,env=None):
 argv=list(map(str,argv));r=subprocess.run(argv,cwd=ROOT,capture_output=True,env=env);(out/(label+'.stdout')).write_bytes(r.stdout);(out/(label+'.stderr')).write_bytes(r.stderr);report['commands'].append(dict(label=label,argv=argv,exit=r.returncode));assert r.returncode==0,(label,r.stderr.decode());return r.stdout
try:
 evidence=json.loads((WORKSPACE/'rust-lang-kotlin/verification/cow-utf8-native-01/baseline.json').read_text())
 version=run('rust-version',[evidence['rustc'],'-Vv']);assert evidence['rust_revision'].encode() in version
 env=os.environ.copy();env['RUSTC']=evidence['rustc'];env['RUSTFLAGS']='--cfg feature="std" --cfg feature="alloc" --check-cfg=cfg(feature,values("std","alloc")) -C overflow-checks=on -C debug-assertions=yes -C panic=unwind -Dwarnings'
 report['compiler_environment']={k:env[k] for k in ['RUSTC','RUSTFLAGS']}
 sources=[ROOT/'tmp/serde/serde/src/private/de.rs',ROOT/'tmp/serde/serde/src/core/private/content.rs',ROOT/'serde_core/src/commonMain/kotlin/io/github/kotlinmania/serdecore/priv/ContentVisitor.kt',HERE/'Oracle.rs',HERE/'Oracle.kt']
 report['sources']={str(p):sha(p) for p in sources}
 run('rust-compile',['cargo','build','--offline','--quiet','--manifest-path',HERE/'Cargo.toml','--target','aarch64-apple-darwin','--target-dir',out/'cargo-target'],env)
 expected=run('rust',[out/'cargo-target/aarch64-apple-darwin/debug/serde-content-borrow-oracle'])
 old=WORKSPACE/'rust-kotlin/verification/production-adoption-01/serde-native-attempt-02.log'
 deps=[Path(line.removeprefix('NATIVE_INPUT=')) for line in old.read_text().splitlines() if line.startswith('NATIVE_INPUT=')]
 core=ROOT/'serde_core/build/classes/kotlin/macosArm64/main/klib/serde-core'
 assert core.exists() and deps
 report['production_klib']={str(p.relative_to(core)):sha(p) for p in core.rglob('*') if p.is_file()}
 report['libraries']={str(p):sha(p) for p in deps}
 compiler='/Users/sydney/.konan/kotlin-native-prebuilt-macos-aarch64-2.4.21/bin/konanc'
 run('native-compile',[compiler,'-target','macos_arm64','-Werror','-Xoverride-konan-properties=airplaneMode=true','-Xbackend-threads=1',HERE/'Oracle.kt','-friend-modules',core,'-library',core,*[x for p in deps for x in ['-library',p]],'-o',out/'native'])
 actual=run('native',[out/'native.kexe']);(out/'compare.diff').write_text(''.join(difflib.unified_diff(expected.decode().splitlines(True),actual.decode().splitlines(True))))
 assert actual==expected;assert all(sha(Path(p))==h for p,h in report['sources'].items())
 report.update(status='passed',observations=len(actual.splitlines()),target='macos_arm64',rust_target='aarch64-apple-darwin',scope='Actual Rust ContentVisitor + ContentRefDeserializer clone versus actual production serde-core KLIB; whole ByteArray borrowing only')
except Exception as e:report.update(status='failed',error=str(e));raise
finally:(out/'validation.json').write_text(json.dumps(report,indent=2)+'\n')
print(report['status'],report['observations'])
