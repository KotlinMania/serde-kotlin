// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Interface used by `Serialize` implementations to generically construct errors belonging to the
 * `Serializer` against which they are currently running.
 *
 * # Example implementation
 *
 * The example data format presented on the website shows an error type appropriate for a basic JSON
 * data format.
 */
public interface Error : StdError {
    public companion object {
        /**
         * Used when a `Serialize` implementation encounters any error while serializing a type.
         *
         * The message should not be capitalized and should not end with a period.
         *
         * For example, a filesystem `Path` may refuse to serialize itself if it contains invalid
         * UTF-8 data.
         */
        public fun custom(msg: Any?): Throwable = SerdeSerializationException(msg.toString())
    }
}
