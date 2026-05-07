// port-lint: source serde_core/src/ser/fmt.rs
package io.github.kotlinmania.serde.core.ser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class FmtTest {
    @Test
    public fun testSerializeUnitVariantFormatsVariant() {
        val formatter = StringBuilder()

        MessageType.StartRequest.serialize(FormatterSerializer(formatter)).getOrThrow()

        assertEquals("start-request", formatter.toString())
    }

    @Test
    public fun testSerializeNewtypeVariantDoesNotTouchValue() {
        val formatter = StringBuilder()
        val value = ExplodingSerialize()

        val result = FormatterSerializer(formatter).serializeNewtypeVariant(
            name = "Message",
            variantIndex = 0u,
            variant = "payload",
            value = value,
        )

        assertTrue(result.isFailure)
        assertFalse(value.serialized)
        assertEquals("", formatter.toString())
    }
}

private enum class MessageType : Serialize {
    StartRequest,
    EndRequest;

    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error =
        serializer.serializeUnitVariant(
            name = "MessageType",
            variantIndex = ordinal.toUInt(),
            variant = when (this) {
                StartRequest -> "start-request"
                EndRequest -> "end-request"
            },
        )
}

private class ExplodingSerialize : Serialize {
    var serialized: Boolean = false
        private set

    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error {
        serialized = true
        return Result.failure(SerdeSerializationException("value was serialized"))
    }

    override fun hashCode(): Int =
        throw IllegalStateException("value was observed")
}
