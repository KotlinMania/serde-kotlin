package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.IpAddress
import io.github.kotlinmania.serdecore.de.IpAddressDeserialize
import io.github.kotlinmania.serdecore.de.Ipv4Address
import io.github.kotlinmania.serdecore.de.Ipv6Address
import io.github.kotlinmania.serdecore.de.Ipv6AddressDeserialize
import io.github.kotlinmania.serdecore.de.SocketAddress
import io.github.kotlinmania.serdecore.de.SocketAddressDeserialize
import io.github.kotlinmania.serdecore.de.SocketAddressV6
import io.github.kotlinmania.serdecore.de.SocketAddressV6Deserialize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Ipv6EmbeddedTailParityTest {
    private object Check {
        fun decode(input: String): SerdeResult<Ipv6Address> = Ipv6AddressDeserialize.deserialize(StrDeserializer.new(input))

        fun rejected(input: String) {
            assertEquals("invalid IPv6 address syntax", decode(input).exceptionOrNull()?.message, input)
        }

        fun address(vararg groups: Int): Ipv6Address =
            Ipv6Address(groups.flatMap { listOf((it ushr 8).toUByte(), it.toUByte()) })

        val mapped = address(0, 0, 0, 0, 0, 0xffff, 0xc000, 0x0201)
    }

    @Test
    fun embeddedIpv4Vectors() {
        val cases =
            listOf(
                "::ffff:192.0.2.1" to Check.address(0, 0, 0, 0, 0, 0xffff, 0xc000, 0x0201),
                "::192.0.2.1" to Check.address(0, 0, 0, 0, 0, 0, 0xc000, 0x0201),
                "1:2:3:4:5:6:192.0.2.1" to Check.address(1, 2, 3, 4, 5, 6, 0xc000, 0x0201),
                "1:2:3:4:5::192.0.2.1" to Check.address(1, 2, 3, 4, 5, 0, 0xc000, 0x0201),
                "1::192.0.2.1" to Check.address(1, 0, 0, 0, 0, 0, 0xc000, 0x0201),
                "0:0:0:0:0:ffff:192.0.2.1" to Check.mapped,
                "::ffff:0.0.0.0" to Check.address(0, 0, 0, 0, 0, 0xffff, 0, 0),
                "::ffff:255.255.255.255" to Check.address(0, 0, 0, 0, 0, 0xffff, 0xffff, 0xffff),
            )
        for ((input, expected) in cases) {
            assertEquals(expected, Check.decode(input).getOrThrow(), input)
        }
    }

    @Test
    fun allMappedOctetPositions() {
        for (index in 0..3) {
            for (value in 0..255) {
                val v4 = MutableList(4) { 0u.toUByte() }
                v4[index] = value.toUByte()
                val input = "::ffff:${v4.joinToString(".")}"
                val expected = Ipv6Address(List(10) { 0u.toUByte() } + listOf(255u.toUByte(), 255u.toUByte()) + v4)
                assertEquals(expected, Check.decode(input).getOrThrow(), input)
            }
        }
    }

    @Test
    fun hexCaseAndZeroPrefixesRemainValid() {
        val cases =
            listOf(
                "::" to Check.address(0, 0, 0, 0, 0, 0, 0, 0),
                "::1" to Check.address(0, 0, 0, 0, 0, 0, 0, 1),
                "1:2:3:4:5:6:7:8" to Check.address(1, 2, 3, 4, 5, 6, 7, 8),
                "ABCD:0001:0002:0003:0004:0005:0006:ffff" to Check.address(0xabcd, 1, 2, 3, 4, 5, 6, 0xffff),
                "0000:0000:0000:0000:0000:0000:0000:0001" to Check.address(0, 0, 0, 0, 0, 0, 0, 1),
            )
        for ((input, expected) in cases) {
            assertEquals(expected, Check.decode(input).getOrThrow(), input)
        }
    }

    @Test
    fun rejectsInvalidHexGroups() {
        val cases =
            listOf(
                "+1:0:0:0:0:0:0:1",
                "-0:0:0:0:0:0:0:1",
                "１:0:0:0:0:0:0:1",
                "١:0:0:0:0:0:0:1",
                "Ｆ:0:0:0:0:0:0:1",
                "10000:0:0:0:0:0:0:1",
                "0x1:0:0:0:0:0:0:1",
            )
        for (input in cases) {
            Check.rejected(input)
        }
    }

    @Test
    fun rejectsInvalidIpv4TailOrPlacement() {
        val cases =
            listOf(
                "::ffff:192.168.001.1",
                "::ffff:256.1.2.3",
                "::ffff:+1.2.3.4",
                "::ffff:١.2.3.4",
                "::ffff:1.2.3",
                "::ffff:1.2.3.4.5",
                "::ffff:192.0.2.1:7",
                "192.0.2.1::",
                "1:2:3:4:5:6::192.0.2.1",
                "1:2:3:4:5:192.0.2.1",
            )
        for (input in cases) {
            Check.rejected(input)
        }
    }

    @Test
    fun rejectsMalformedCompressionOrGroupCount() {
        val cases =
            listOf(
                "",
                "1:2:3:4:5:6:7",
                "1:2:3:4:5:6:7:8:9",
                "1::2::3",
                ":::1",
                "1:::",
                "1:2:3:4:5:6:7:8::",
                "::1:2:3:4:5:6:7:8",
                ":1:2:3:4:5:6:7:8",
                "1:2:3:4:5:6:7:8:",
                "1::z",
                "::1%2",
            )
        for (input in cases) {
            Check.rejected(input)
        }
    }

    @Test
    fun ownedStringSupportsEmbeddedTail() {
        val valid = Ipv6AddressDeserialize.deserialize(StringDeserializer.new("::ffff:192.0.2.1"))
        assertEquals(Check.mapped, valid.getOrThrow())
        val invalid = Ipv6AddressDeserialize.deserialize(StringDeserializer.new("+1:0:0:0:0:0:0:1"))
        assertEquals("invalid IPv6 address syntax", invalid.exceptionOrNull()?.message)
    }

    @Test
    fun genericIpCallerSupportsEmbeddedTail() {
        val mapped = IpAddressDeserialize.deserialize(StrDeserializer.new("::ffff:192.0.2.1"))
        assertEquals(IpAddress.V6(Check.mapped), mapped.getOrThrow())
        val v4 = IpAddressDeserialize.deserialize(StrDeserializer.new("127.0.0.1"))
        assertEquals(IpAddress.V4(Ipv4Address(listOf(127u, 0u, 0u, 1u).map { it.toUByte() })), v4.getOrThrow())
        for (input in listOf("::ffff:192.168.001.1", "+1:0:0:0:0:0:0:1")) {
            assertTrue(IpAddressDeserialize.deserialize(StrDeserializer.new(input)).isFailure, input)
        }
    }

    @Test
    fun socketCallerSupportsEmbeddedTail() {
        val direct = SocketAddressV6Deserialize.deserialize(StrDeserializer.new("[::ffff:192.0.2.1]:00080"))
        assertEquals(SocketAddressV6(Check.mapped, 80u.toUShort()), direct.getOrThrow())
        val generic = SocketAddressDeserialize.deserialize(StrDeserializer.new("[::ffff:192.0.2.1]:80"))
        assertEquals(SocketAddress.V6(SocketAddressV6(Check.mapped, 80u.toUShort())), generic.getOrThrow())
        val invalid = SocketAddressV6Deserialize.deserialize(StrDeserializer.new("[::ffff:192.168.001.1]:80"))
        assertTrue(invalid.isFailure)
    }

    @Test
    fun compressedHeadLimitsReserveAZeroGroup() {
        for (count in 0..5) {
            val prefix = (1..count).joinToString(":") { it.toString(16) }
            val groups = MutableList(8) { 0 }
            for (index in 0 until count) {
                groups[index] = index + 1
            }
            groups[6] = 0xc000
            groups[7] = 0x0201
            val input = "$prefix::192.0.2.1"
            assertEquals(Check.address(*groups.toIntArray()), Check.decode(input).getOrThrow(), input)
        }
    }
}
