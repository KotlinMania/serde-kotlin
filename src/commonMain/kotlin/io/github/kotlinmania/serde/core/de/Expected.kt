// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * `Expected` represents an explanation of what data a `Visitor` was expecting to receive.
 *
 * This is used as an argument to the `invalidType`, `invalidValue`, and `invalidLength` methods of
 * the `Error` interface to build error messages. The message should be a noun or noun phrase that
 * completes the sentence "This Visitor expects to receive ...", for example the message could be
 * "an integer between 0 and 64". The message should not be capitalized and should not end with a
 * period.
 */
public fun interface Expected {
    /**
     * Format an explanation of what data was being expected.
     */
    public fun expecting(): String
}
