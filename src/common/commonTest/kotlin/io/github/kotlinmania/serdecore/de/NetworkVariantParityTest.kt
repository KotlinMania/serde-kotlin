package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.BytesDeserializer
import io.github.kotlinmania.serdecore.de.value.StrDeserializer
import io.github.kotlinmania.serdecore.de.value.U16Deserializer
import io.github.kotlinmania.serdecore.de.value.U64Deserializer
import io.github.kotlinmania.serdecore.de.value.U8Deserializer
import io.github.kotlinmania.serdecore.de.value.UnitDeserializer
import io.github.kotlinmania.serdecore.de.value.seqDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NetworkVariantParityTest {
    private class BinaryMode(
        source: Deserializer,
    ) : Deserializer by source {
        override fun isHumanReadable(): Boolean = false
    }

    private class Node(
        private val source: Deserializer,
    ) : IntoDeserializer {
        override fun intoDeserializer(): Deserializer = BinaryMode(source)
    }

    private class Payload(
        private val node: Node,
        private val trace: MutableList<String>,
    ) : VariantAccess {
        override fun unitVariant(): SerdeResult<Unit> = SerdeResult.failure(SerdeError.custom("unexpected unit"))

        override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> {
            trace.add("newtype")
            return seed.deserialize(node.intoDeserializer())
        }

        override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): SerdeResult<V> =
            SerdeResult.failure(SerdeError.custom("unexpected tuple"))

        override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
            SerdeResult.failure(SerdeError.custom("unexpected struct"))
    }

    private class Input(
        private val identifier: Deserializer,
        private val payload: Node,
    ) : Deserializer by UnitDeserializer.new() {
        val trace = mutableListOf<String>()

        override fun isHumanReadable(): Boolean = false

        override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> =
            visitor.visitEnum(
                object : EnumAccess {
                    override fun <R> variantSeed(seed: DeserializeSeed<R>): SerdeResult<Pair<R, VariantAccess>> =
                        seed.deserialize(identifier).map { it to Payload(payload, trace) }
                },
            )
    }

    private val ipv4 = listOf(127, 0, 0, 1)
    private val ipv6 = List(15) { 0 } + 1

    private fun ipPayload(octets: List<Int>): Node =
        Node(seqDeserializer(octets.map { Node(U8Deserializer.new(it.toUByte())) }.iterator()))

    private fun socketPayload(octets: List<Int>): Node =
        Node(seqDeserializer(listOf(ipPayload(octets), Node(U16Deserializer.new(8080u.toUShort()))).iterator()))

    @Test
    fun numericIpVariantsDecodeBothAddressFamilies() {
        val expected =
            listOf<IpAddress>(
                IpAddress.V4(Ipv4Address(ipv4.map { it.toUByte() })),
                IpAddress.V6(Ipv6Address(ipv6.map { it.toUByte() })),
            )
        for ((index, octets) in listOf(ipv4, ipv6).withIndex()) {
            val input = Input(U64Deserializer.new(index.toULong()), ipPayload(octets))
            assertEquals(expected[index], IpAddressDeserialize.deserialize(input).getOrThrow())
            assertEquals(listOf("newtype"), input.trace)
        }
    }

    @Test
    fun numericSocketVariantsDecodeAddressAndPort() {
        val expected =
            listOf<SocketAddress>(
                SocketAddress.V4(SocketAddressV4(Ipv4Address(ipv4.map { it.toUByte() }), 8080u.toUShort())),
                SocketAddress.V6(SocketAddressV6(Ipv6Address(ipv6.map { it.toUByte() }), 8080u.toUShort())),
            )
        for ((index, octets) in listOf(ipv4, ipv6).withIndex()) {
            val input = Input(U64Deserializer.new(index.toULong()), socketPayload(octets))
            assertEquals(expected[index], SocketAddressDeserialize.deserialize(input).getOrThrow())
            assertEquals(listOf("newtype"), input.trace)
        }
    }

    @Test
    fun knownByteIpNamesKeepBothFamilies() {
        val expected =
            listOf<IpAddress>(
                IpAddress.V4(Ipv4Address(ipv4.map { it.toUByte() })),
                IpAddress.V6(Ipv6Address(ipv6.map { it.toUByte() })),
            )
        for ((index, name) in listOf("V4", "V6").withIndex()) {
            val input = Input(BytesDeserializer.new(name.encodeToByteArray()), ipPayload(if (index == 0) ipv4 else ipv6))
            assertEquals(expected[index], IpAddressDeserialize.deserialize(input).getOrThrow())
            assertEquals(listOf("newtype"), input.trace)
        }
    }

    @Test
    fun unknownSocketNameIsAnUnknownVariantBeforePayload() {
        val input = Input(StrDeserializer.new("Other"), socketPayload(ipv4))
        val failure = assertIs<SerdeResult.Failure>(SocketAddressDeserialize.deserialize(input))
        assertEquals("unknown variant `Other`, expected `V4` or `V6`", failure.error.message)
        assertEquals(emptyList(), input.trace)
    }

    @Test
    fun malformedIpNameBytesAreInvalidBytesBeforePayload() {
        val input = Input(BytesDeserializer.new(byteArrayOf(0xff.toByte())), ipPayload(ipv4))
        val failure = assertIs<SerdeResult.Failure>(IpAddressDeserialize.deserialize(input))
        assertEquals("invalid value: byte array, expected `V4` or `V6`", failure.error.message)
        assertEquals(emptyList(), input.trace)
    }

    @Test
    fun outOfRangeNetworkIndicesAreInvalidValuesBeforePayload() {
        val ip = Input(U64Deserializer.new(2uL), ipPayload(ipv4))
        val socket = Input(U64Deserializer.new(2uL), socketPayload(ipv4))
        assertEquals(
            "invalid value: integer `2`, expected `V4` or `V6`",
            assertIs<SerdeResult.Failure>(IpAddressDeserialize.deserialize(ip)).error.message,
        )
        assertEquals(
            "invalid value: integer `2`, expected `V4` or `V6`",
            assertIs<SerdeResult.Failure>(SocketAddressDeserialize.deserialize(socket)).error.message,
        )
        assertEquals(emptyList(), ip.trace)
        assertEquals(emptyList(), socket.trace)
    }

    @Test
    fun humanReadableIpv4PathsKeepWorking() {
        assertEquals(
            IpAddress.V4(Ipv4Address(ipv4.map { it.toUByte() })),
            IpAddressDeserialize.deserialize(StrDeserializer.new("127.0.0.1")).getOrThrow(),
        )
        assertEquals(
            SocketAddress.V4(SocketAddressV4(Ipv4Address(ipv4.map { it.toUByte() }), 80u.toUShort())),
            SocketAddressDeserialize.deserialize(StrDeserializer.new("127.0.0.1:80")).getOrThrow(),
        )
    }
}
