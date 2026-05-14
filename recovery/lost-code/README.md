# serde-kotlin lost-code recovery

Created on branch `recovery/lost-code` after `git fsck --no-reflogs --unreachable`
surfaced commits and blobs that were no longer reachable from normal branch refs.

The branch tip `9b95269` is a recovery anchor. Its tree matches the branch that
was checked out at recovery time, and its parents include the recovered commits so
the original objects stay reachable.

## Recovered commits

| Commit | Subject | Contents |
|---|---|---|
| `2d92ae9` | `On docs/mod-rs-reexport-rule: audit-claude-md` | Adds the `CLAUDE.md` workflow for upstream `mod.rs` re-exports: no central Kotlin alias/typealias API, migrate callers to the original symbol, and keep `Mod.kt` as a caller-migration ledger. |
| `0d948ab` | `index on docs/mod-rs-reexport-rule: 07d68f9 Persist WIP before starlark-kotlin template conformance` | Stash index parent for the CLAUDE re-export-rule work. No additional file delta from its first parent. |
| `75a7825` | `WIP on main: 251703d Extend serde value deserializers` | Adds `NeverDeserializer` to `src/commonMain/kotlin/io/github/kotlinmania/serde/core/de/value/Value.kt`. The type is backed by `Nothing`, forwards all `deserialize*` calls through `deserializeAny`, and implements `IntoDeserializer`. |
| `a50cf00` | `index on main: 251703d Extend serde value deserializers` | Stash index parent for the `Value.kt` work. No additional file delta from its first parent. |
| `16c8ac7` | `Port serde Content` | Adds `src/commonMain/kotlin/io/github/kotlinmania/serde/core/private/Content.kt`, a sealed `Content` hierarchy for bools, signed and unsigned integers, floats, chars, strings, byte arrays, options, unit, newtype, sequences, and maps. Also changes `.gitignore`, formats the default test-task list, and rewrites `Integer128.kt` header text. |
| `089b6f6` | `Conform build/CI/license to starlark-kotlin template` | Changes CI triggers to `main, master`, removes the macOS Intel x64/iOS x64 CI job, removes `setup-android-sdk`, derives `local.properties` from `ANDROID_SDK_ROOT` or `ANDROID_HOME`, removes `macosX64` and `iosX64` targets, rewrites README content, trims `LICENSE-MIT`, and updates Maven POM description/license details. |

Generated patch exports live in `patches/`:

- `*.show.patch` is the direct `git show` export.
- `*.first-parent.diff` is the first-parent diff for stash-shaped commits and
  regular commits.

## Recovered loose blobs

These blobs were exported because they appeared in the unreachable-object scan.
Some match files in recovered commits; others were loose source/document blobs not
mapped by the recovered commit trees.

| Blob | Size | Known content |
|---|---:|---|
| `268bfe1fa5c813617b3ab6366edfda6d89f5392e` | 957 | `Integer128.kt`, matching the version in `16c8ac7`. |
| `45fd29060913772a208612af31279e0c5ec8bcd5` | 1242 | `serde_core/src/private/size_hint.rs` port with `IteratorWithSizeHint`, `fromBounds`, `cautious`, `helper`, and `sizeOf`. |
| `a3c4e1088fd22e530ab904d21f7ec60367e9d457` | 1855 | `serde_core/src/de/value.rs` related test/code blob with `ValueTest`, `StringKindVisitor`, `StringSeed`, and `StringOnlyVisitor`. |
| `b1e8d81723090971b497ae9345ed36da81b8afa4` | 2229 | `Content.kt`, matching the new sealed `Content` hierarchy in `16c8ac7`. |
| `bf7c2e73ece8e717565ffc827daac19f57b20820` | 3684 | `serde/src/private/mod.rs` port blob with `Clone`, `From`, `Into`, `TryFrom`, `Default`, `fmt`, `Formatter`, `PhantomData`, `Option`, `ptr`, `Ok`, `Err`, and UTF-8 lossy helpers. |
| `cda0eeead6904585ef0a7069b4c31291be6beb25` | 4970 | `serde_core/src/macros.rs` port blob with `ForwardToDeserializeAnyDeserializer`. |
| `4845aead0089445db7931a68496b0426b43c6375` | 7033 | `serde/src/lib.rs` port blob with root serde/core typealiases and `__requireSerdeNotSerdeCore`. |
| `5e61689a688025b6f1b8e443f2073b94653c3f53` | 19354 | `serde-kotlin` agent guide / operating contract. |

`blob-map.tsv` records sizes and any paths discovered in the recovered commit
trees. `commit-map.tsv` records the recovered commit parents and subjects.

## Recovered tree maps

After the recovery anchor and blob export commit, `git fsck --no-reflogs --unreachable`
reported no unreachable commits and no unreachable blobs. It still reported 28
unreachable tree objects. Those tree objects do not add new file contents, but
they preserve directory snapshots and path-to-blob relationships that may matter
while reconstructing prior work.

`unreachable-tree-map.tsv` records those relationships. The map contains:

- 28 tree objects.
- 139 unique blob hashes.
- 661 tree/path entries.

The paths include repository metadata and build files, GitHub workflow files,
Gradle wrapper and npm patch files, Kotlin source under `src/commonMain` and
`src/commonTest`, serde core/de/ser/private sources, serde derive internals,
and report/task artifacts such as `PORT_REPORT.md`, `RUST_CALLERS.md`,
and `tasks_core.json`.

The 28 tree objects were then anchored by creating one commit per tree and
merging those commits into `recovery/lost-code` with an ours-strategy merge
commit. `tree-anchor-map.tsv` records the tree hash to anchor-commit mapping.
