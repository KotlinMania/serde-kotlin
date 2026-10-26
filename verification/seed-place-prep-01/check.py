from pathlib import Path
import argparse, difflib, hashlib, json, os, subprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
p = argparse.ArgumentParser()
p.add_argument('--out', required=True, type=Path)
out = p.parse_args().out.resolve()
out.mkdir(parents=True, exist_ok=False)
sha = lambda p: hashlib.sha256(Path(p).read_bytes()).hexdigest()
report = {'status': 'running', 'new_translated_files': 0, 'new_adopted_files': 0, 'commands': []}

def run(label, argv, env=None):
    argv = list(map(str, argv))
    r = subprocess.run(argv, cwd=ROOT, capture_output=True, env=env)
    (out / (label + '.stdout')).write_bytes(r.stdout)
    (out / (label + '.stderr')).write_bytes(r.stderr)
    report['commands'].append({'label': label, 'argv': argv, 'exit': r.returncode})
    assert r.returncode == 0, (label, r.stderr.decode())
    print(label, 'passed', flush=True)
    return r.stdout

try:
    rust = json.loads((ROOT.parent / 'rust-lang-kotlin/verification/cow-utf8-native-01/baseline.json').read_text())
    version = run('rust-version', [rust['rustc'], '-Vv'])
    assert rust['rust_revision'].encode() in version
    env = os.environ.copy()
    env['RUSTC'] = rust['rustc']
    env['RUSTFLAGS'] = '-C overflow-checks=on -C debug-assertions=yes -C panic=unwind -Dwarnings'
    report['rust_environment'] = {k: env[k] for k in ['RUSTC', 'RUSTFLAGS']}
    base = ROOT / 'serde_core/src/commonMain/kotlin/io/github/kotlinmania/serdecore/de'
    tests = ROOT / 'serde_core/src/commonTest/kotlin/io/github/kotlinmania/serdecore/de/MutablePlaceTest.kt'
    sources = [base / 'MutablePlace.kt', base / 'Deserialize.kt', base / 'Impls.kt', tests,
               ROOT / 'tmp/serde/serde_core/src/de/impls.rs', ROOT / 'tmp/serde/serde_core/src/private/seed.rs',
               HERE / 'Oracle.rs', HERE / 'Oracle.kt']
    report['sources'] = {str(p): sha(p) for p in sources}
    run('rust-compile', ['cargo', 'build', '--offline', '--locked', '--quiet', '--manifest-path', HERE / 'Cargo.toml',
                         '--target', 'aarch64-apple-darwin', '--target-dir', out / 'cargo-target'], env)
    expected = run('rust', [out / 'cargo-target/aarch64-apple-darwin/debug/serde-writable-place-oracle'])
    receipt = ROOT.parent / 'rust-kotlin/verification/production-adoption-01/serde-native-attempt-02.log'
    deps = [Path(s.removeprefix('NATIVE_INPUT=')) for s in receipt.read_text().splitlines() if s.startswith('NATIVE_INPUT=')]
    core = ROOT / 'serde_core/build/classes/kotlin/macosArm64/main/klib/serde-core'
    assert core.exists() and deps
    report['production_klib'] = {str(p.relative_to(core)): sha(p) for p in core.rglob('*') if p.is_file()}
    report['libraries'] = {str(p): sha(p) for p in deps}
    compiler = '/Users/sydney/.konan/kotlin-native-prebuilt-macos-aarch64-2.4.21/bin/konanc'
    run('native-compile', [compiler, '-target', 'macos_arm64', '-Werror', '-Xreturn-value-checker=full',
        '-Xoverride-konan-properties=airplaneMode=true', '-Xbackend-threads=1', HERE / 'Oracle.kt', tests,
        '-library', core, *[x for p in deps for x in ['-library', p]], '-o', out / 'native'])
    actual = run('native', [out / 'native.kexe'])
    (out / 'compare.diff').write_text(''.join(difflib.unified_diff(expected.decode().splitlines(True), actual.decode().splitlines(True))))
    assert actual == expected
    assert all(sha(p) == h for p, h in report['sources'].items())
    report.update(status='passed', observations=len(actual.splitlines()), target='macos_arm64',
        scope='Actual Rust Vec in-place implementation versus production Kotlin mutable-list place overload; error specialization and provider panic boundaries are documented separately.')
except Exception as e:
    report.update(status='failed', error=str(e))
    raise
finally:
    (out / 'validation.json').write_text(json.dumps(report, indent=2) + '\n')
