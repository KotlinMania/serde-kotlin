from pathlib import Path
import json,subprocess,hashlib,argparse,shutil
ROOT=Path(__file__).resolve().parents[2]
HERE=Path(__file__).resolve().parent
p=argparse.ArgumentParser();p.add_argument('--out',required=True,type=Path);p.add_argument('--reuse-build',type=Path);p.add_argument('--content-regressions',action='store_true');args=p.parse_args()
out=args.out.resolve();out.mkdir(parents=True,exist_ok=False)
compiler='/Users/sydney/.konan/kotlin-native-prebuilt-macos-aarch64-2.4.21/bin/konanc'
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
report={'status':'running','commands':[],'target':'macos_arm64','compiler':compiler,'validation_mode':'Direct Native compilation of complete current production source sets and their existing test suites; no Maven resolution'}
def run(label,cmd):
 cmd=list(map(str,cmd));r=subprocess.run(cmd,cwd=ROOT,capture_output=True)
 (out/(label+'.stdout')).write_bytes(r.stdout);(out/(label+'.stderr')).write_bytes(r.stderr)
 report['commands'].append({'label':label,'argv':cmd,'exit':r.returncode})
 (out/'validation.json').write_text(json.dumps(report,indent=2)+'\n')
 if r.returncode: raise RuntimeError(f'{label} failed: '+r.stderr.decode()[-6000:])
 print(label,'passed',flush=True)
 return r.stdout
try:
 common=sorted((ROOT/'serde_core/src/commonMain/kotlin').rglob('*.kt'))
 platform=sorted((ROOT/'serde_core/src/nativeMain/kotlin').rglob('*.kt'))
 k=ROOT.parent/'rust-lang-kotlin/candidates'
 kernel=[k/'core/kotlin/org/rustlang/core/slice/SharedByteSlice.kt',k/'core/kotlin/org/rustlang/core/str/Utf8ByteOperations.kt',k/'alloc/kotlin/org/rustlang/alloc/borrow/Utf8Cow.kt']
 deps=[Path(line.removeprefix('NATIVE_INPUT=')) for line in (ROOT.parent/'rust-kotlin/verification/production-adoption-01/serde-native-attempt-02.log').read_text().splitlines() if line.startswith('NATIVE_INPUT=')]
 report['dependency_hashes']={str(p):sha(p) for p in deps}
 flags=[compiler,'-target','macos_arm64','-Werror','-Xexpect-actual-classes','-Xreturn-value-checker=full','-Xoverride-konan-properties=airplaneMode=true','-Xbackend-threads=1']
 libraries=[x for p in deps for x in ['-library',p]]
 core=out/'serde-core.klib';root=out/'serde.klib'
 rootsources=sorted((ROOT/'src/commonMain/kotlin').rglob('*.kt'))
 coretests=sorted((ROOT/'serde_core/src/commonTest/kotlin').rglob('*.kt'))+sorted((ROOT/'serde_core/src/macosArm64Test/kotlin').rglob('*.kt'))
 roottests=sorted((ROOT/'src/commonTest/kotlin').rglob('*.kt'))+sorted((ROOT/'src/macosArm64Test/kotlin').rglob('*.kt'))
 if args.content_regressions:
  report['excluded_tests']={'src/commonTest/kotlin/io/github/kotlinmania/serde/TestRoundtripTest.kt':'Two network-address roundtrip tests require the serde-test transitive dependency graph; not part of Content regression validation.'}
  roottests=[p for p in roottests if p.name!='TestRoundtripTest.kt']
 sources=common+platform+kernel+rootsources+coretests+roottests+[HERE/'Oracle.kt']
 report['source_hashes']={str(p):sha(p) for p in sources}
 if args.reuse_build:
  previous=json.loads((args.reuse_build/'validation.json').read_text())
  assert all(previous['source_hashes'].get(p)==h for p,h in report['source_hashes'].items()),'Source changed since the compiled libraries'
  assert previous['dependency_hashes']==report['dependency_hashes']
  shutil.copy2(args.reuse_build/'serde-core.klib',core);shutil.copy2(args.reuse_build/'serde.klib',root)
  report['reused_build']=str(args.reuse_build)
  report['prior_core_tests_stdout_sha256']=sha(args.reuse_build/'core-tests.stdout')
 else:
  run('core-compile',flags+['-produce','library','-module-name','serde_core','-Xmulti-platform','-Xcommon-sources='+','.join(map(str,common+kernel)),*common,*platform,*kernel,*libraries,'-o',core])
  run('root-compile',flags+['-produce','library','-module-name','serde',*rootsources,'-library',core,*libraries,'-o',root])
  run('core-tests-compile',flags+['-generate-test-runner',*coretests,'-library',core,'-friend-modules',core,*libraries,'-o',out/'core-tests'])
  run('core-tests',[out/'core-tests.kexe'])
 run('root-tests-compile',flags+['-generate-test-runner',*roottests,'-library',root,'-library',core,'-friend-modules',root,*libraries,'-o',out/'root-tests'])
 run('root-tests',[out/'root-tests.kexe'])
 run('oracle-compile',flags+[HERE/'Oracle.kt','-library',core,'-friend-modules',core,*libraries,'-o',out/'oracle'])
 actual=run('oracle',[out/'oracle.kexe'])
 expected=(ROOT.parent/'rust-kotlin/verification/foundation-batch-04/content-01/rust.stdout').read_bytes()
 assert actual==expected,'Generated production payloads differ from the canonical Rust observations'
 assert all(sha(p)==h for p,h in report['source_hashes'].items()),'Source changed during validation'
 report.update(status='passed',observations=len(actual.splitlines()),production_core_sha256=sha(core),production_root_sha256=sha(root))
except Exception as e:
 report.update(status='failed',error=str(e));raise
finally:
 (out/'validation.json').write_text(json.dumps(report,indent=2)+'\n')
