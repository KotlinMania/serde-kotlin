from pathlib import Path
import argparse, difflib, hashlib, json, subprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
RAW = ROOT.parent / 'rust-kotlin/verification/foundation-batch-04/seed-01'
p = argparse.ArgumentParser()
p.add_argument('--out', required=True, type=Path)
out = p.parse_args().out.resolve()
out.mkdir(parents=True, exist_ok=False)
sha = lambda p: hashlib.sha256(Path(p).read_bytes()).hexdigest()
report = {'status': 'running', 'commands': [], 'target': 'macos_arm64',
          'generated_body_compiled_separately': False}

def run(label, argv):
    argv = list(map(str, argv))
    r = subprocess.run(argv, cwd=ROOT, capture_output=True)
    (out / (label + '.stdout')).write_bytes(r.stdout)
    (out / (label + '.stderr')).write_bytes(r.stderr)
    report['commands'].append({'label': label, 'argv': argv, 'exit': r.returncode})
    assert r.returncode == 0, (label, r.stderr.decode())
    print(label, 'passed', flush=True)
    return r.stdout

try:
    seed = ROOT / 'serde_core/src/commonMain/kotlin/io/github/kotlinmania/serdecore/priv/Seed.kt'
    prefix = b'// port-lint: source private/seed.rs\npackage io.github.kotlinmania.serdecore.priv\n\n'
    assert sha(RAW / 'generated/generated.kt') == '53ca4518e8e60cd801b1250ac6bbc40282fb353b552fbbf2f9c6682c6aa84d34'
    assert seed.read_bytes() == prefix + (RAW / 'generated/generated.kt').read_bytes()
    frozen = ROOT / 'tmp/serde/serde/src/core/private/seed.rs'
    assert sha(frozen) == '3f6e098c5bd314788370dcaf3ab0152fcd7feb6bcf36a9c51808938cd58071eb'
    original = json.loads((RAW / 'check-02/validation.json').read_text())
    assert original['status'] == 'passed' and all(sha(p) == h for p, h in original['sources'].items())
    assert (HERE / 'Oracle.kt').read_bytes() == b'import io.github.kotlinmania.serdecore.priv.InPlaceSeed\n' + (RAW / 'Oracle.kt').read_bytes()
    prep = ROOT / 'verification/seed-place-prep-01'
    previous = json.loads((prep / 'validation.json').read_text())
    rust_vec = ROOT / 'tmp/serde/serde_core/src/de/impls.rs'
    assert sha(rust_vec) == previous['sources'][str(rust_vec)]
    tests = ROOT / 'serde_core/src/commonTest/kotlin/io/github/kotlinmania/serdecore/de/MutablePlaceTest.kt'
    impls = ROOT / 'serde_core/src/commonMain/kotlin/io/github/kotlinmania/serdecore/de/Impls.kt'
    sources = [seed, impls, tests, HERE / 'Oracle.kt', prep / 'Oracle.kt', frozen, rust_vec]
    report['source_hashes'] = {str(p): sha(p) for p in sources}
    core = ROOT / 'serde_core/build/classes/kotlin/macosArm64/main/klib/serde-core'
    assert core.exists()
    report['production_klib'] = {str(p.relative_to(core)): sha(p) for p in core.rglob('*') if p.is_file()}
    receipt = ROOT.parent / 'rust-kotlin/verification/production-adoption-01/serde-native-attempt-02.log'
    deps = [Path(s.removeprefix('NATIVE_INPUT=')) for s in receipt.read_text().splitlines() if s.startswith('NATIVE_INPUT=')]
    report['library_hashes'] = {str(p): sha(p) for p in deps}
    compiler = '/Users/sydney/.konan/kotlin-native-prebuilt-macos-aarch64-2.4.21/bin/konanc'
    flags = [compiler, '-target', 'macos_arm64', '-Werror', '-Xreturn-value-checker=full',
             '-Xoverride-konan-properties=airplaneMode=true', '-Xbackend-threads=1', '-library', core,
             *[x for p in deps for x in ['-library', p]]]
    observations = {}
    for label, inputs, expected_file in [
        ('seed', [HERE / 'Oracle.kt'], RAW / 'check-02/rust.stdout'),
        ('collection', [prep / 'Oracle.kt', tests], prep / 'rust.stdout'),
    ]:
        run(label + '-compile', flags + inputs + ['-o', out / label])
        actual = run(label, [out / (label + '.kexe')])
        expected = expected_file.read_bytes()
        (out / (label + '-rust.stdout')).write_bytes(expected)
        (out / (label + '.diff')).write_text(''.join(difflib.unified_diff(expected.decode().splitlines(True), actual.decode().splitlines(True))))
        assert actual == expected, label
        observations[label] = len(actual.splitlines())
    assert observations == {'seed': 28, 'collection': 9}
    assert all(sha(p) == h for p, h in report['source_hashes'].items())
    report.update(status='passed', observations=observations,
        scope='Installed production Seed and its production Vec in-place consumer versus retained canonical Rust outputs; oracle compile inputs contain no generated Seed source copy.')
except Exception as e:
    report.update(status='failed', error=str(e))
    raise
finally:
    (out / 'validation.json').write_text(json.dumps(report, indent=2) + '\n')
