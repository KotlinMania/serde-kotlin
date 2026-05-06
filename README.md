# Serde Kotlin &emsp; [![Build status]][actions] [![Maven Central]][maven]

[Build status]: https://img.shields.io/github/actions/workflow/status/KotlinMania/serde-kotlin/ci.yml?branch=main
[actions]: https://github.com/KotlinMania/serde-kotlin/actions
[Maven Central]: https://img.shields.io/maven-central/v/io.github.kotlinmania/serde-kotlin
[maven]: https://central.sonatype.com/artifact/io.github.kotlinmania/serde-kotlin

**Serde Kotlin is a Kotlin Multiplatform port of Serde, a framework for *ser*ializing and *de*serializing data structures efficiently and generically.**

This repository ports the Rust [`serde`](https://crates.io/crates/serde) crate into Kotlin Multiplatform. The Rust `serde_core` crate is kept together with `serde-kotlin` in this workspace, while preserving the Rust-derived API and namespace structure under `io.github.kotlinmania.serde.core`.

Kotlin port by Sydney Renee <sydney@solace.ofharmony.ai> ([@sydneyrenee](https://github.com/sydneyrenee)) for The Solace Project.

---

You may be looking for:

- [The upstream Serde overview](https://serde.rs)
- [Data formats supported by Serde](https://serde.rs/#data-formats)
- [Upstream derive documentation](https://serde.rs/derive.html)
- [Upstream examples](https://serde.rs/examples.html)
- [Upstream API documentation](https://docs.rs/serde)
- [KotlinMania serde-kotlin repository](https://github.com/KotlinMania/serde-kotlin)

## Port Status

This port is in progress. Kotlin files are translated one Rust file at a time and carry a `// port-lint: source <path>` header so provenance checks can map each Kotlin file back to its upstream Rust source.

The intended artifact is `serde-kotlin`, not a separate `serde-core-kotlin` artifact. Rust's `serde` crate imports and re-exports `serde_core`; this Kotlin port keeps that split visible in packages while shipping one Serde surface for consumers.

## Serde In Action

<details>
<summary>Click to show Gradle Kotlin DSL dependencies.</summary>

```kotlin
dependencies {
    // The core APIs, including Serialize and Deserialize support.
    implementation("io.github.kotlinmania:serde-kotlin:0.1.0-SNAPSHOT")

    // Each data format lives in its own artifact; this sample uses JSON.
    implementation("io.github.kotlinmania:serde-json-kotlin:0.1.0-SNAPSHOT")
}
```

For local development before publication, use a composite build substitution from a sibling checkout:

```kotlin
includeBuild("../serde-kotlin") {
    dependencySubstitution {
        substitute(module("io.github.kotlinmania:serde-kotlin")).using(project(":"))
    }
}
```

</details>
<p></p>

Expected Kotlin shape once the corresponding Serde and JSON APIs are ported:

```kotlin
import kotlinx.serialization.Serializable
import io.github.kotlinmania.serdejson.Json

@Serializable
data class Point(
    val x: Int,
    val y: Int,
)

fun main() {
    val point = Point(x = 1, y = 2)

    // Convert the Point to a JSON string.
    val serialized = Json.toString(point).getOrThrow()

    // Prints serialized = {"x":1,"y":2}
    println("serialized = $serialized")

    // Convert the JSON string back to a Point.
    val deserialized: Point = Json.fromString(serialized).getOrThrow()

    // Prints deserialized = Point(x=1, y=2)
    println("deserialized = $deserialized")
}
```

## Supported Targets

- macOS arm64 / x64
- Linux x64
- Windows mingw-x64
- iOS arm64 / x64 / simulator-arm64
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

## Build

```bash
./gradlew build
./gradlew test
```

## Porting Guidelines

See [CLAUDE.md](CLAUDE.md) and [AGENTS.md](AGENTS.md) for translator discipline, port-lint header convention, Rust-to-Kotlin idiom mapping, and the rule that each Rust file maps to one Kotlin file.

## Getting Help

For Serde concepts, the upstream [Serde documentation](https://serde.rs) remains the best starting point. For KotlinMania port work, use this repository's issues and project notes.

Serde is one of the most widely used Rust libraries, so many upstream design questions have existing answers in the Rust community. The upstream README points readers toward the unofficial Rust community Discord, the official Rust Project Discord, Rust Zulip, StackOverflow, the Rust subreddit, and the Rust Discourse forum.

## Acknowledgements

Thank you to the original Serde authors, maintainers, and contributors, including Erick Tryzelaar, David Tolnay, and the broader `serde-rs` community. This Kotlin Multiplatform port exists because of their careful design, documentation, examples, and long stewardship of Serde.

## License

This Kotlin port is licensed under either of [Apache License, Version 2.0](LICENSE-APACHE) or [MIT license](LICENSE-MIT) at your option.

Copyright (c) 2026 Sydney Renee <sydney@solace.ofharmony.ai> and The Solace Project.

The upstream Rust Serde project is licensed under the same Apache-2.0-or-MIT terms. These license files are copied from the upstream Serde repository.

Unless explicitly stated otherwise, contributions intentionally submitted for inclusion in this Kotlin port are licensed as above, without additional terms or conditions.
