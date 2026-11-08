// port-lint: tests test_suite/tests/test_de.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertDeTokens
import io.github.kotlinmania.serdetest.assertDeTokensError
import kotlin.test.Test
import kotlin.time.Instant

class SystemTimeNanosParityTest {
    @Test
    fun fullUnsignedNanosecondsNormalizeBeforeSystemTimeConstruction() {
        val cases = listOf(
            Triple(0u, 10L, 0),
            Triple(999_999_999u, 10L, 999_999_999),
            Triple(1_000_000_000u, 11L, 0),
            Triple(2_147_483_648u, 12L, 147_483_648),
            Triple(3_000_000_000u, 13L, 0),
            Triple(UInt.MAX_VALUE, 14L, 294_967_295),
        )
        for ((nanos, seconds, remainder) in cases) {
            val expected = Instant.fromEpochSeconds(seconds, remainder)
            assertDeTokens(
                expected,
                SystemTimeDeserialize,
                listOf(Token.Seq(2), Token.U64(10uL), Token.U32(nanos), Token.SeqEnd),
            )
            assertDeTokens(
                expected,
                SystemTimeDeserialize,
                listOf(
                    Token.Struct("SystemTime", 2),
                    Token.Str("secs_since_epoch"), Token.U64(10uL),
                    Token.Str("nanos_since_epoch"), Token.U32(nanos),
                    Token.StructEnd,
                ),
            )
        }
    }

    @Test
    fun unsignedSecondCarryOverflowRetainsTheUpstreamError() {
        assertDeTokensError(
            SystemTimeDeserialize,
            listOf(Token.Seq(2), Token.U64(ULong.MAX_VALUE), Token.U32(UInt.MAX_VALUE), Token.SeqEnd),
            "overflow deserializing SystemTime epoch offset",
        )
    }
}
