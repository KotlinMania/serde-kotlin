// port-lint: source private/string.rs
package io.github.kotlinmania.serdecore.priv.generated.string

// Generated from Syn AST; exact original retained in input.rs.
// Native borrowed bytes/Cow retain storage. The caller must keep the selected input range immutable while a borrowed result is live.
// Rust use resolved through explicit library bindings.
// Rust #[doc(hidden)]: omitted from source API documentation.
fun `from_utf8_lossy`(`bytes`: io.github.kotlinmania.serdecore.priv.SharedByteSlice): io.github.kotlinmania.serdecore.priv.Utf8Cow = io.github.kotlinmania.serdecore.priv.Utf8Cow.fromUtf8Lossy(`bytes`)


// The generated code calls this like:
//
//     let value = &_serde::__private::from_utf8_lossy(bytes);
//     Err(_serde::de::Error::unknown_variant(value, VARIANTS))
//
// so it is okay for the return type to be different from the std case as long
// as the above works.
// Rust cfg item excluded by explicit configuration; original retained in input.rs.
