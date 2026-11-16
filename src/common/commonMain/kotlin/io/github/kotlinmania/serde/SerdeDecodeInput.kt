package io.github.kotlinmania.serde

import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.value.BoolDeserializer
import io.github.kotlinmania.serdecore.de.value.I64Deserializer
import io.github.kotlinmania.serdecore.de.value.StrDeserializer
import io.github.kotlinmania.serdecore.de.value.UnitDeserializer

/** An input supplied by a concrete format to a serialization data-model decoder. */
class SerdeDecodeInput private constructor(
    private val decoder: Deserializer,
) {
    /** Runs the generic Kotlin decoder selected by the caller. */
    fun <T> decode(deserialize: Deserialize<T>): SerdeResult<T> = deserialize.deserialize(decoder)

    companion object {
        fun fromDeserializer(deserializer: Deserializer): SerdeDecodeInput = SerdeDecodeInput(deserializer)

        fun text(value: String): SerdeDecodeInput = SerdeDecodeInput(StrDeserializer.new(value))

        fun signed(value: Long): SerdeDecodeInput = SerdeDecodeInput(I64Deserializer.new(value))

        fun boolean(value: Boolean): SerdeDecodeInput = SerdeDecodeInput(BoolDeserializer.new(value))

        fun unit(): SerdeDecodeInput = SerdeDecodeInput(UnitDeserializer.new())
    }
}
