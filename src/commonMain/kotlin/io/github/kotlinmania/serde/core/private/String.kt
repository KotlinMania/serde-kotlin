// port-lint: source core/private/string.rs
package io.github.kotlinmania.serde.core.`private`

/*
 * Copyright (c) 2026 Sydney Renee <sydney@solace.ofharmony.ai>
 * and The Solace Project.
 *
 * Licensed under the Apache License, Version 2.0. See LICENSE and NOTICE.
 *
 * This Kotlin source is a port of upstream Serde code, which is licensed
 * under either Apache-2.0 or MIT at your option; see LICENSE-APACHE and
 * LICENSE-MIT.
 */

public fun fromUtf8Lossy(bytes: ByteArray): String = bytes.decodeToString()

// The generated code calls this like:
//
//     val value = serde.private.fromUtf8Lossy(bytes)
//     Err(serde.de.Error.unknownVariant(value, VARIANTS))
//
// so it is okay for the return type to be different from the standard-library
// case as long as the above works.
public fun fromUtf8LossyNoAlloc(bytes: ByteArray): String =
    runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }
        .getOrElse {
            // Three unicode replacement characters if it fails. They look like a
            // white-on-black question mark. The user will recognize it as invalid
            // UTF-8.
            "\ufffd\ufffd\ufffd"
        }
