package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Ipv4Address
import io.github.kotlinmania.serdecore.de.Ipv6Address
import io.github.kotlinmania.serdecore.de.SocketAddress
import io.github.kotlinmania.serdecore.de.SocketAddressDeserialize
import io.github.kotlinmania.serdecore.de.SocketAddressV4
import io.github.kotlinmania.serdecore.de.SocketAddressV4Deserialize
import io.github.kotlinmania.serdecore.de.SocketAddressV6
import io.github.kotlinmania.serdecore.de.SocketAddressV6Deserialize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SocketPortSyntaxParityTest {
    private object Check {
        val localhost4 = Ipv4Address(listOf(127u, 0u, 0u, 1u).map { it.toUByte() })
        val localhost6 = Ipv6Address(List(15) { 0u.toUByte() } + 1u.toUByte())

        fun v4(port: String): SerdeResult<SocketAddressV4> =
            SocketAddressV4Deserialize.deserialize(StrDeserializer.new("127.0.0.1:$port"))

        fun v6(port: String): SerdeResult<SocketAddressV6> =
            SocketAddressV6Deserialize.deserialize(StrDeserializer.new("[::1]:$port"))

        fun rejected(port: String) {
            assertEquals("invalid IPv4 socket address syntax", v4(port).exceptionOrNull()?.message, port)
            assertEquals("invalid IPv6 socket address syntax", v6(port).exceptionOrNull()?.message, port)
        }
    }

    @Test
    fun allV4PortValues() {
        for (port in 0..UShort.MAX_VALUE.toInt()) {
            assertEquals(SocketAddressV4(Check.localhost4, port.toUShort()), Check.v4(port.toString()).getOrThrow())
        }
    }

    @Test
    fun v6PortBoundaries() {
        for (port in listOf(0, 1, 9, 10, 80, 443, 65534, 65535)) {
            assertEquals(SocketAddressV6(Check.localhost6, port.toUShort()), Check.v6(port.toString()).getOrThrow())
        }
    }

    @Test
    fun leadingZerosAreAllowed() {
        val cases = listOf("00" to 0, "00080" to 80, ("0".repeat(128) + "80") to 80, "00065535" to 65535)
        for ((port, expected) in cases) {
            assertEquals(expected.toUShort(), Check.v4(port).getOrThrow().port)
            assertEquals(expected.toUShort(), Check.v6(port).getOrThrow().port)
        }
    }

    @Test
    fun rejectsSigns() {
        for (port in listOf("+0", "+80", "-0", "-1")) {
            Check.rejected(port)
        }
    }

    @Test
    fun rejectsNonAsciiDigits() {
        for (port in listOf("٨٠", "८०", "８０", "٠", "²")) {
            Check.rejected(port)
        }
    }

    @Test
    fun rejectsOverflow() {
        val cases = listOf("65536", "99999", "18446744073709551615", "9".repeat(128), "0".repeat(128) + "65536")
        for (port in cases) {
            Check.rejected(port)
        }
    }

    @Test
    fun rejectsMalformedPorts() {
        for (port in listOf("", " 80", "80 ", "80\n", "8.0", "0x50", "80:1", "80\u0000")) {
            Check.rejected(port)
        }
    }

    @Test
    fun genericSocketCallerUsesPortRules() {
        for (input in listOf("127.0.0.1:+80", "[::1]:+80", "127.0.0.1:٨٠", "[::1]:65536")) {
            assertTrue(SocketAddressDeserialize.deserialize(StrDeserializer.new(input)).isFailure, input)
        }
        val expected4 = SocketAddress.V4(SocketAddressV4(Check.localhost4, 80u.toUShort()))
        val expected6 = SocketAddress.V6(SocketAddressV6(Check.localhost6, 80u.toUShort()))
        assertEquals(
            expected4,
            SocketAddressDeserialize
                .deserialize(StrDeserializer.new("127.0.0.1:00080"))
                .getOrThrow(),
        )
        assertEquals(
            expected6,
            SocketAddressDeserialize
                .deserialize(StrDeserializer.new("[::1]:00080"))
                .getOrThrow(),
        )
    }

    @Test
    fun ownedStringUsesPortRules() {
        val valid =
            SocketAddressV4Deserialize
                .deserialize(StringDeserializer.new("127.0.0.1:00080"))
                .getOrThrow()
        assertEquals(80u.toUShort(), valid.port)
        val failure = SocketAddressV4Deserialize.deserialize(StringDeserializer.new("127.0.0.1:+80")).exceptionOrNull()
        assertEquals("invalid IPv4 socket address syntax", failure?.message)
    }
}
