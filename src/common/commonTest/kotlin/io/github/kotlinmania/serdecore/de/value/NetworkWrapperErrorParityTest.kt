package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.IpAddress
import io.github.kotlinmania.serdecore.de.IpAddressDeserialize
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

class NetworkWrapperErrorParityTest {
    private object Check {
        val localhost4 = Ipv4Address(listOf(127u, 0u, 0u, 1u).map { it.toUByte() })
        val localhost6 = Ipv6Address(List(15) { 0u.toUByte() } + 1u.toUByte())
        val mapped = Ipv6Address(List(10) { 0u.toUByte() } + listOf(255u, 255u, 192u, 0u, 2u, 1u).map { it.toUByte() })

        private fun source(
            input: String,
            owned: Boolean,
        ): Deserializer = if (owned) StringDeserializer.new(input) else StrDeserializer.new(input)

        fun ip(
            input: String,
            owned: Boolean = false,
        ): SerdeResult<IpAddress> = IpAddressDeserialize.deserialize(source(input, owned))

        fun socket(
            input: String,
            owned: Boolean = false,
        ): SerdeResult<SocketAddress> = SocketAddressDeserialize.deserialize(source(input, owned))

        fun v4(
            input: String,
            owned: Boolean = false,
        ): SerdeResult<SocketAddressV4> = SocketAddressV4Deserialize.deserialize(source(input, owned))

        fun v6(
            input: String,
            owned: Boolean = false,
        ): SerdeResult<SocketAddressV6> = SocketAddressV6Deserialize.deserialize(source(input, owned))
    }

    @Test
    fun genericIpErrorsUseIpKind() {
        val cases = listOf("", "foo", "999.1.1.1", "+1.2.3.4", "1:2:3", "::ffff:001.2.3.4", "::1%2", "127.0.0.1:80", "[::1]")
        for (input in cases) {
            assertEquals("invalid IP address syntax", Check.ip(input).exceptionOrNull()?.message, input)
        }
    }

    @Test
    fun genericSocketErrorsUseSocketKind() {
        val cases =
            listOf(
                "",
                "foo",
                "127.0.0.1",
                "::1",
                "127.0.0.1:+80",
                "[::1]:+80",
                "999.1.1.1:80",
                "[z]:80",
                "[::1]:65536",
                "[::1]:",
                "127..0.1:80",
                "[::ffff:001.2.3.4]:80",
            )
        for (input in cases) {
            assertEquals("invalid socket address syntax", Check.socket(input).exceptionOrNull()?.message, input)
        }
    }

    @Test
    fun typedV4InvalidHostsUseSocketKind() {
        val cases = listOf("999.1.1.1:80", "01.2.3.4:80", "+1.2.3.4:80", "1.2.3:80", "::1:80", "[::1]:80")
        for (input in cases) {
            assertEquals("invalid IPv4 socket address syntax", Check.v4(input).exceptionOrNull()?.message, input)
        }
    }

    @Test
    fun typedV6InvalidHostsUseSocketKind() {
        val cases =
            listOf(
                "[z]:80",
                "[+1:0:0:0:0:0:0:1]:80",
                "[::ffff:001.2.3.4]:80",
                "[1::2::3]:80",
                "[::ffff:256.0.0.1]:80",
                "[::1.2.3.4.5]:80",
            )
        for (input in cases) {
            assertEquals("invalid IPv6 socket address syntax", Check.v6(input).exceptionOrNull()?.message, input)
        }
    }

    @Test
    fun genericIpValuesRemainDistinct() {
        assertEquals(IpAddress.V4(Check.localhost4), Check.ip("127.0.0.1").getOrThrow())
        assertEquals(IpAddress.V6(Check.localhost6), Check.ip("::1").getOrThrow())
        assertEquals(IpAddress.V6(Check.mapped), Check.ip("::ffff:192.0.2.1").getOrThrow())
    }

    @Test
    fun genericSocketValuesRemainDistinct() {
        assertEquals(
            SocketAddress.V4(SocketAddressV4(Check.localhost4, 80u.toUShort())),
            Check.socket("127.0.0.1:00080").getOrThrow(),
        )
        assertEquals(
            SocketAddress.V6(SocketAddressV6(Check.localhost6, 80u.toUShort())),
            Check.socket("[::1]:00080").getOrThrow(),
        )
        assertEquals(
            SocketAddress.V6(SocketAddressV6(Check.mapped, 443u.toUShort())),
            Check.socket("[::ffff:192.0.2.1]:443").getOrThrow(),
        )
    }

    @Test
    fun bothInvalidHostAndPortKeepTypedKind() {
        for (input in listOf("999.1.1.1:+80", "01.2.3.4:65536")) {
            assertEquals("invalid IPv4 socket address syntax", Check.v4(input).exceptionOrNull()?.message)
        }
        for (input in listOf("[z]:+80", "[1::2::3]:65536")) {
            assertEquals("invalid IPv6 socket address syntax", Check.v6(input).exceptionOrNull()?.message)
        }
    }

    @Test
    fun ownedStringsUseWrapperErrorKind() {
        assertEquals(
            "invalid IP address syntax",
            Check
                .ip("foo", owned = true)
                .exceptionOrNull()
                ?.message,
        )
        assertEquals(
            "invalid socket address syntax",
            Check
                .socket("[z]:80", owned = true)
                .exceptionOrNull()
                ?.message,
        )
        assertEquals(
            "invalid IPv4 socket address syntax",
            Check
                .v4("999.1.1.1:80", owned = true)
                .exceptionOrNull()
                ?.message,
        )
        assertEquals(
            "invalid IPv6 socket address syntax",
            Check
                .v6("[z]:80", owned = true)
                .exceptionOrNull()
                ?.message,
        )
    }

    @Test
    fun ownedStringsPreserveValues() {
        assertEquals(IpAddress.V6(Check.localhost6), Check.ip("::1", owned = true).getOrThrow())
        assertEquals(
            SocketAddress.V4(SocketAddressV4(Check.localhost4, 80u.toUShort())),
            Check.socket("127.0.0.1:00080", owned = true).getOrThrow(),
        )
        assertEquals(
            SocketAddressV6(Check.mapped, 443u.toUShort()),
            Check.v6("[::ffff:192.0.2.1]:443", owned = true).getOrThrow(),
        )
    }
}
