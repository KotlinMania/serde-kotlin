package io.github.kotlinmania.serde

import io.github.kotlinmania.serde.priv.ContentRefDeserializer
import io.github.kotlinmania.serde.priv.ContentVisitor
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.priv.generated.content.Content

/** Captured Serde input that can be tried against multiple untagged variants. */
public class UntaggedContent private constructor(private val content: Content) {
    /** Starts an independent decoding attempt over the captured value. */
    public fun <T> decode(decoder: Deserialize<T>): SerdeResult<T> =
        decoder.deserialize(ContentRefDeserializer.new(content))

    public companion object {
        /** Captures one complete value using Serde's existing content visitor. */
        public fun read(deserializer: Deserializer): SerdeResult<UntaggedContent> =
            ContentVisitor.new().deserialize(deserializer).map { UntaggedContent(it) }
    }
}
