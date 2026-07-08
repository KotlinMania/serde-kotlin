// port-lint: source private/string.rs
package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serde.serdeCatching

fun fromUtf8Lossy(bytes: ByteArray): String = bytes.decodeToString()

// The generated code calls this like:
//
//     val value = io.github.kotlinmania.serdecore.priv.fromUtf8Lossy(bytes)
//     Err(serde.de.Error.unknownVariant(value, VARIANTS))
//
// so it is okay for the return type to be different from the standard-library
// case as long as the above works.
fun fromUtf8LossyNoAlloc(bytes: ByteArray): String =
    serdeCatching { bytes.decodeToString(throwOnInvalidSequence = true) }
        .getOrElse {
            // Three unicode replacement characters if it fails. They look like a
            // white-on-black question mark. The user will recognize it as invalid
            // UTF-8.
            "\ufffd\ufffd\ufffd"
        }
