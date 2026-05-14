// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * A **data structure** that can be serialized into any data format supported by Serde.
 *
 * Serde provides `Serialize` implementations for many Kotlin primitive and standard library types.
 * All of these can be serialized using Serde out of the box.
 *
 * Additionally, Serde provides code generation to automatically generate `Serialize`
 * implementations for classes and sealed types in your program. See the derive section of the
 * manual for how to use this.
 *
 * In rare cases it may be necessary to implement `Serialize` manually for some type in your
 * program. See the Implementing `Serialize` section of the manual for more about this.
 *
 * Third-party crates may provide `Serialize` implementations for types that they expose. For
 * example the `linked-hash-map` crate provides a `LinkedHashMap<K, V>` type that is serializable by
 * Serde because the crate provides an implementation of `Serialize` for it.
 */
public interface Serialize {
    /**
     * Serialize this value into the given Serde serializer.
     *
     * See the Implementing `Serialize` section of the manual for more information about how to
     * implement this method.
     */
    public fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error
}
