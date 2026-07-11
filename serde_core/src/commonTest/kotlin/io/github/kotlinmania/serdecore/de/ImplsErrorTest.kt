// port-lint: tests test_suite/tests/test_de_error.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serdecore.de.value.intoDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals

public class ImplsErrorTest {
    @Test
    public fun cStringInternalNullReportsUpstreamError() {
        val error =
            CStringDeserialize
                .deserialize(byteArrayOf('a'.code.toByte(), 0, 'b'.code.toByte()).intoDeserializer())
                .exceptionOrNull()

        assertEquals("nul byte found in provided data at position: 1", error?.message)
    }

    @Test
    public fun cStringInternalNullEndReportsUpstreamError() {
        val error =
            CStringDeserialize
                .deserialize(byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 0).intoDeserializer())
                .exceptionOrNull()

        assertEquals("nul byte found in provided data at position: 2", error?.message)
    }

    @Test
    public fun durationOverflowSeqReportsUpstreamError() {
        val error =
            DurationDeserialize
                .deserialize(
                    listOf(
                        ULong.MAX_VALUE.intoDeserializer(),
                        1_000_000_000u.intoDeserializer(),
                    ).intoDeserializer(),
                ).exceptionOrNull()

        assertEquals("overflow deserializing Duration", error?.message)
    }

    @Test
    public fun durationOverflowStructReportsUpstreamError() {
        val error =
            DurationDeserialize
                .deserialize(
                    mapOf(
                        "secs".intoDeserializer() to ULong.MAX_VALUE.intoDeserializer(),
                        "nanos".intoDeserializer() to 1_000_000_000u.intoDeserializer(),
                    ).intoDeserializer(),
                ).exceptionOrNull()

        assertEquals("overflow deserializing Duration", error?.message)
    }

    @Test
    public fun systemTimeEpochOffsetOverflowSeqReportsUpstreamError() {
        val error =
            SystemTimeDeserialize
                .deserialize(
                    listOf(
                        ULong.MAX_VALUE.intoDeserializer(),
                        1_000_000_000u.intoDeserializer(),
                    ).intoDeserializer(),
                ).exceptionOrNull()

        assertEquals("overflow deserializing SystemTime epoch offset", error?.message)
    }

    @Test
    public fun systemTimeEpochOffsetOverflowStructReportsUpstreamError() {
        val error =
            SystemTimeDeserialize
                .deserialize(
                    mapOf(
                        "secs_since_epoch".intoDeserializer() to ULong.MAX_VALUE.intoDeserializer(),
                        "nanos_since_epoch".intoDeserializer() to 1_000_000_000u.intoDeserializer(),
                    ).intoDeserializer(),
                ).exceptionOrNull()

        assertEquals("overflow deserializing SystemTime epoch offset", error?.message)
    }

    @Test
    public fun systemTimeOverflowReportsUpstreamError() {
        val error =
            SystemTimeDeserialize
                .deserialize(
                    listOf(
                        ULong.MAX_VALUE.intoDeserializer(),
                        0u.intoDeserializer(),
                    ).intoDeserializer(),
                ).exceptionOrNull()

        assertEquals("overflow deserializing SystemTime", error?.message)
    }
}
