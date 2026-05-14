// port-lint: source serde_core/src/private/string.rs
package io.github.kotlinmania.serde.core.`private`

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
