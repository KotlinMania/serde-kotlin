# Borrowed Content storage repair

Two existing core adapter branches copied borrowed byte arrays: ContentVisitor.visitBorrowedBytes and contentClone(Content.Bytes). Both now retain the original array, matching the canonical Rust private/de.rs branches and the existing root Kotlin consumer. Temporary visitBytes still copies, and cloning owned ByteBuf still copies. The API remains ByteArray; the owner must keep borrowed contents immutable while retained Content values or their clones live. This repairs whole-array borrowing only; it does not add byte-range APIs, full Unicode scalar consumers, or claim generated content.rs adoption.

Native validation compiles the actual local serde crate and calls its ContentVisitor and ContentRefDeserializer, including the actual private clone path. An external friend consumer links the complete production serde-core KLIB. Ten observations match for empty and nonempty borrowed/owned storage, identity and content. Two common tests also check nested Some/Seq/Map/Newtype clone retention and owned-copy separation. Existing JVM and macOS ARM64 suites pass; exact counts and source/library hashes are in validation.json.

No public signatures, dependency/configuration, target, kernel, FFI or foreign edits were changed. This is a consumer repair, counted as zero new whole files and zero new adoptions. ByteArray mutation is technically possible in Kotlin; the immutable-owner obligation is explicit. The earlier non-Apple list-growth adapter is unchanged and remains an allocation portability adapter, not exact Rust allocation/ABI equivalence.

Reproduce after compiling the production Native KLIB: `python3 verification/content-borrow-01/check.py --out tmp/content-borrow-01/oracle-new`.
