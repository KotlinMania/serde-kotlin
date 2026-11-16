package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.IpAddress
import io.github.kotlinmania.serdecore.de.IpAddressDeserialize
import io.github.kotlinmania.serdecore.de.Ipv4Address
import io.github.kotlinmania.serdecore.de.Ipv4AddressDeserialize
import io.github.kotlinmania.serdecore.de.SocketAddressV4
import io.github.kotlinmania.serdecore.de.SocketAddressV4Deserialize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Ipv4SyntaxParityTest {
    private object Check {
        fun decode(input: String): SerdeResult<Ipv4Address> = Ipv4AddressDeserialize.deserialize(StrDeserializer.new(input))

        fun rejected(input: String) {
            assertEquals("invalid IPv4 address syntax", decode(input).exceptionOrNull()?.message, input)
        }

        fun piecesRejected(pieces: List<String>) {
            for (index in 0..3) {
                for (piece in pieces) {
                    val fields = mutableListOf("1", "2", "3", "4")
                    fields[index] = piece
                    rejected(fields.joinToString("."))
                }
            }
        }
    }

    @Test
    fun allOctetValuesInEachPosition() {
        for (index in 0..3) {
            for (value in 0..255) {
                val octets = MutableList(4) { 0u.toUByte() }
                octets[index] = value.toUByte()
                assertEquals(Ipv4Address(octets), Check.decode(octets.joinToString(".")).getOrThrow())
            }
        }
    }

    @Test
    fun rejectsSignedOctets() {
        Check.piecesRejected(listOf("+1", "+0", "-0", "-1"))
    }

    @Test
    fun rejectsMultiDigitZeroPrefixes() {
        Check.piecesRejected(listOf("00", "01", "001", "0000"))
    }

    @Test
    fun rejectsNonAsciiDigits() {
        Check.piecesRejected(listOf("١", "१", "１", "²"))
    }

    @Test
    fun rejectsOverflowAndWidth() {
        Check.piecesRejected(listOf("256", "999", "1000", "2147483648", "999999999999999999"))
    }

    @Test
    fun rejectsMalformedShapes() {
        for (input in listOf("", "1", "1.2.3", "1.2.3.4.5", "1..2.3", ".1.2.3", "1.2.3.", " 1.2.3.4", "1.2.3.4 ", "1.2.3.4\n", "1.2.3.4\u0000")) {
            Check.rejected(input)
        }
    }

    @Test
    fun ownedStringsUseSameRules() {
        val expected = Ipv4Address(listOf(127u, 0u, 0u, 1u).map { it.toUByte() })
        assertEquals(expected, Ipv4AddressDeserialize.deserialize(StringDeserializer.new("127.0.0.1")).getOrThrow())
        val failure = Ipv4AddressDeserialize.deserialize(StringDeserializer.new("+1.2.3.4")).exceptionOrNull()
        assertEquals("invalid IPv4 address syntax", failure?.message)
    }

    @Test
    fun genericIpCallerRejectsInvalidIpv4() {
        for (input in listOf("+1.2.3.4", "01.2.3.4", "١.2.3.4")) {
            assertTrue(IpAddressDeserialize.deserialize(StrDeserializer.new(input)).isFailure, input)
        }
        val expected = IpAddress.V4(Ipv4Address(listOf(127u, 0u, 0u, 1u).map { it.toUByte() }))
        assertEquals(expected, IpAddressDeserialize.deserialize(StrDeserializer.new("127.0.0.1")).getOrThrow())
    }

    @Test
    fun socketV4CallerRejectsInvalidHost() {
        for (input in listOf("+1.2.3.4:80", "01.2.3.4:80", "١.2.3.4:80")) {
            assertTrue(SocketAddressV4Deserialize.deserialize(StrDeserializer.new(input)).isFailure, input)
        }
        val expected = SocketAddressV4(Ipv4Address(listOf(127u, 0u, 0u, 1u).map { it.toUByte() }), 80u.toUShort())
        assertEquals(expected, SocketAddressV4Deserialize.deserialize(StrDeserializer.new("127.0.0.1:80")).getOrThrow())
    }
}
