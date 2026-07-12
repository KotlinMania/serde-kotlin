// port-lint: tests test_suite/tests/test_roundtrip.rs
package io.github.kotlinmania.serde

import io.github.kotlinmania.serdecore.de.IpAddress
import io.github.kotlinmania.serdecore.de.IpAddressDeserialize
import io.github.kotlinmania.serdecore.de.Ipv4Address
import io.github.kotlinmania.serdecore.de.Ipv6Address
import io.github.kotlinmania.serdecore.de.SocketAddress
import io.github.kotlinmania.serdecore.de.SocketAddressDeserialize
import io.github.kotlinmania.serdecore.de.SocketAddressV6
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertDeTokens
import io.github.kotlinmania.serdetest.assertSerTokens
import io.github.kotlinmania.serdetest.compact
import kotlin.test.Test

class TestRoundtripTest {
    @Test
    fun ipAddrRoundtrip() {
        val address = IpAddress.V4(Ipv4Address("1234".octets()))
        val tokens =
            listOf(
                Token.NewtypeVariant(name = "IpAddr", variant = "V4"),
                Token.Tuple(4),
                Token.U8('1'.code.toUByte()),
                Token.U8('2'.code.toUByte()),
                Token.U8('3'.code.toUByte()),
                Token.U8('4'.code.toUByte()),
                Token.TupleEnd,
            )

        assertSerTokens(address.compact(), tokens)
        assertDeTokens(address, IpAddressDeserialize.compact(), tokens)
    }

    @Test
    fun socketAddrRoundtrip() {
        val address =
            SocketAddress.V6(
                SocketAddressV6(
                    Ipv6Address("1234567890123456".octets()),
                    1234u,
                ),
            )
        val tokens =
            buildList {
                add(Token.NewtypeVariant(name = "SocketAddr", variant = "V6"))
                add(Token.Tuple(2))
                add(Token.Tuple(16))
                "1234567890123456".forEach { add(Token.U8(it.code.toUByte())) }
                add(Token.TupleEnd)
                add(Token.U16(1234u))
                add(Token.TupleEnd)
            }

        assertSerTokens(address.compact(), tokens)
        assertDeTokens(address, SocketAddressDeserialize.compact(), tokens)
    }
}

private fun String.octets(): List<UByte> = map { it.code.toUByte() }
