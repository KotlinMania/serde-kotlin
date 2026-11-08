// port-lint: tests test_suite/tests/test_ser.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.BoundValue
import io.github.kotlinmania.serdecore.de.CStringDeserialize
import io.github.kotlinmania.serde.priv.ContentDeserializer
import io.github.kotlinmania.serdecore.priv.contentaccess.newByteBufContent
import io.github.kotlinmania.serdecore.de.PathValue
import io.github.kotlinmania.serdecore.de.RangeValue
import io.github.kotlinmania.serdecore.de.RangeInclusiveValue
import io.github.kotlinmania.serdecore.de.RangeFromValue
import io.github.kotlinmania.serdecore.de.RangeToValue
import io.github.kotlinmania.serdecore.de.ResultValue
import io.github.kotlinmania.serdecore.de.Ipv4Address
import io.github.kotlinmania.serdecore.de.Ipv6Address
import io.github.kotlinmania.serdecore.de.IpAddress
import io.github.kotlinmania.serdecore.de.SocketAddress
import io.github.kotlinmania.serdecore.de.SocketAddressV4
import io.github.kotlinmania.serdecore.de.SocketAddressV6
import io.github.kotlinmania.serdetest.compact
import io.github.kotlinmania.serdetest.readable
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertSerTokens
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.nanoseconds
import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serdetest.assertSerTokensError
import io.github.kotlinmania.btree.BTreeMap
import io.github.kotlinmania.btree.BTreeSet

class TestSerTest {
    private class UnitStruct : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeUnitStruct("UnitStruct")
    }

    private class TupleStruct(val field0: Int, val field1: Int, val field2: Int) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeTupleStruct("TupleStruct", 0 + 1 + 1 + 1).flatMap { serdeState ->
                serdeState.serializeField(Serialize.I32.bind(field0)).flatMap {
                    serdeState.serializeField(Serialize.I32.bind(field1))
                }.flatMap {
                    serdeState.serializeField(Serialize.I32.bind(field2))
                }.flatMap { serdeState.end() }
            }
    }

    private class Struct(val a: Int, val b: Int, val c: Int) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeStruct("Struct", 0 + 1 + 1 + 1).flatMap { serdeState ->
                serdeState.serializeField("a", Serialize.I32.bind(a)).flatMap {
                    serdeState.serializeField("b", Serialize.I32.bind(b))
                }.flatMap {
                    serdeState.serializeField("c", Serialize.I32.bind(c))
                }.flatMap { serdeState.end() }
            }
    }

    private data object NotSerializable

    private sealed class Enum : Serialize {
        data object Unit : Enum()
        data class One(val value: Int) : Enum()
        data class Seq(val field0: Int, val field1: Int) : Enum()
        data class Map(val a: Int, val b: Int) : Enum()
        data object SkippedUnit : Enum()
        data class SkippedOne(val value: Int) : Enum()
        data class SkippedSeq(val field0: Int, val field1: Int) : Enum()
        data class SkippedMap(val _a: Int, val _b: Int) : Enum()
        data class OneWithSkipped(val value: NotSerializable) : Enum()

        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            when (this) {
                Unit -> serializer.serializeUnitVariant("Enum", 0u, "Unit")
                is One -> serializer.serializeNewtypeVariant("Enum", 1u, "One", Serialize.I32.bind(value))
                is Seq -> serializer.serializeTupleVariant("Enum", 2u, "Seq", 0 + 1 + 1).flatMap { serdeState ->
                    serdeState.serializeField(Serialize.I32.bind(field0)).flatMap {
                        serdeState.serializeField(Serialize.I32.bind(field1))
                    }.flatMap { serdeState.end() }
                }
                is Map -> serializer.serializeStructVariant("Enum", 3u, "Map", 0 + 1 + 1).flatMap { serdeState ->
                    serdeState.serializeField("a", Serialize.I32.bind(a)).flatMap {
                        serdeState.serializeField("b", Serialize.I32.bind(b))
                    }.flatMap { serdeState.end() }
                }
                SkippedUnit -> SerdeResult.failure(SerdeError.custom("the enum variant Enum::SkippedUnit cannot be serialized"))
                is SkippedOne -> SerdeResult.failure(SerdeError.custom("the enum variant Enum::SkippedOne cannot be serialized"))
                is SkippedSeq -> SerdeResult.failure(SerdeError.custom("the enum variant Enum::SkippedSeq cannot be serialized"))
                is SkippedMap -> SerdeResult.failure(SerdeError.custom("the enum variant Enum::SkippedMap cannot be serialized"))
                is OneWithSkipped -> serializer.serializeUnitVariant("Enum", 8u, "OneWithSkipped")
            }
    }

    private fun <T> checkSet(value: BTreeSet<T>, element: Serialize.For<T>, tokens: List<Token>) {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer, element)
        }, tokens)
    }

    private fun <K, V> checkMap(value: BTreeMap<K, V>, key: Serialize.For<K>, element: Serialize.For<V>, tokens: List<Token>) {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer, key, element)
        }, tokens)
    }

    @Test
    public fun testUnit() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (Unit).serialize(serializer)
        }, listOf(Token.UnitValue))
    }

    @Test
    public fun testBool() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (true).serialize(serializer)
        }, listOf(Token.Bool(true)))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (false).serialize(serializer)
        }, listOf(Token.Bool(false)))
    }

    @Test
    public fun testIsizes() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (0.toByte()).serialize(serializer)
        }, listOf(Token.I8(0)))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (0.toShort()).serialize(serializer)
        }, listOf(Token.I16(0)))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (0).serialize(serializer)
        }, listOf(Token.I32(0)))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (0L).serialize(serializer)
        }, listOf(Token.I64(0)))
    }

    @Test
    public fun testUsizes() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (0u.toUByte()).serialize(serializer)
        }, listOf(Token.U8(0u)))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (0u.toUShort()).serialize(serializer)
        }, listOf(Token.U16(0u)))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (0u).serialize(serializer)
        }, listOf(Token.U32(0u)))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (0uL).serialize(serializer)
        }, listOf(Token.U64(0uL)))
    }

    @Test
    public fun testFloats() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (0.0f).serialize(serializer)
        }, listOf(Token.F32(0.0f)))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (0.0).serialize(serializer)
        }, listOf(Token.F64(0.0)))
    }

    @Test
    public fun testChar() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                ('a').serialize(serializer)
        }, listOf(Token.CharValue('a')))
    }

    @Test
    public fun testStr() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                ("abc").serialize(serializer)
        }, listOf(Token.Str("abc")))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                ("abc").serialize(serializer)
        }, listOf(Token.Str("abc")))
    }

    @Test
    public fun testOption() {
        val absent: Serialize? = null
        val present: Serialize? = Serialize.I32.bind(1)
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                absent.serialize(serializer)
        }, listOf(Token.None))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                present.serialize(serializer)
        }, listOf(Token.Some, Token.I32(1)))
    }

    @Test
    public fun testResult() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (ResultValue.Ok<Int>(0)).serialize(serializer, Serialize.I32, Serialize.I32)
        }, listOf(Token.NewtypeVariant(name = "Result", variant = "Ok"), Token.I32(0)))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (ResultValue.Err<Int>(1)).serialize(serializer, Serialize.I32, Serialize.I32)
        }, listOf(Token.NewtypeVariant(name = "Result", variant = "Err"), Token.I32(1)))
    }

    @Test
    public fun testSlice() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (listOf(0).subList(0, 0)).serialize(serializer, Serialize.I32)
        }, listOf(Token.Seq(len = 0), Token.SeqEnd))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (listOf(1, 2, 3)).serialize(serializer, Serialize.I32)
        }, listOf(Token.Seq(len = 3), Token.I32(1), Token.I32(2), Token.I32(3), Token.SeqEnd))
    }

    @Test
    public fun testArray() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (Array(0) { 0 }).serialize(serializer, Serialize.I32)
        }, listOf(Token.Tuple(len = 0), Token.TupleEnd))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (arrayOf(1, 2, 3)).serialize(serializer, Serialize.I32)
        }, listOf(Token.Tuple(len = 3), Token.I32(1), Token.I32(2), Token.I32(3), Token.TupleEnd))
    }

    @Test
    public fun testVec() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                mutableListOf<Long>().serialize(serializer, Serialize.I64)
        }, listOf(Token.Seq(len = 0), Token.SeqEnd))
        val integers = object : Serialize.For<MutableList<Int>> {
            override fun <Ok> serialize(value: MutableList<Int>, serializer: Serializer<Ok>): SerdeResult<Ok> =
                value.serialize(serializer, Serialize.I32)
        }
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                mutableListOf(mutableListOf<Int>(), mutableListOf(1), mutableListOf(2, 3)).serialize(serializer, integers)
        }, listOf(
            Token.Seq(len = 3), Token.Seq(len = 0), Token.SeqEnd,
            Token.Seq(len = 1), Token.I32(1), Token.SeqEnd,
            Token.Seq(len = 2), Token.I32(2), Token.I32(3), Token.SeqEnd,
            Token.SeqEnd,
        ))
    }

    @Test
    fun testBtreeset() {
        checkSet(BTreeSet<Long>(), Serialize.I64, listOf(Token.Seq(len = 0), Token.SeqEnd))
        checkSet(BTreeSet.of(1), Serialize.I32, listOf(Token.Seq(len = 1), Token.I32(1), Token.SeqEnd))
    }

    // testHashset requires the custom FixedState hasher accepted by the upstream set API.

    // testTuple requires independently typed one-element and three-element tuples.

    @Test
    fun testBtreemap() {
        checkMap(BTreeMap.of(1 to 2), Serialize.I32, Serialize.I32,
            listOf(Token.Map(len = 1), Token.I32(1), Token.I32(2), Token.MapEnd))
        checkMap(BTreeMap.of(1 to 2, 3 to 4), Serialize.I32, Serialize.I32,
            listOf(Token.Map(len = 2), Token.I32(1), Token.I32(2), Token.I32(3), Token.I32(4), Token.MapEnd))
        val nestedMap = object : Serialize.For<BTreeMap<Int, Int>> {
            override fun <Ok> serialize(value: BTreeMap<Int, Int>, serializer: Serializer<Ok>): SerdeResult<Ok> =
                value.serialize(serializer, Serialize.I32, Serialize.I32)
        }
        checkMap(BTreeMap.of(1 to BTreeMap<Int, Int>(), 2 to BTreeMap.of(3 to 4, 5 to 6)), Serialize.I32, nestedMap,
            listOf(Token.Map(len = 2), Token.I32(1), Token.Map(len = 0), Token.MapEnd,
                Token.I32(2), Token.Map(len = 2), Token.I32(3), Token.I32(4), Token.I32(5), Token.I32(6), Token.MapEnd, Token.MapEnd))
    }

    // testHashmap requires the custom FixedState hasher accepted by the upstream map API.

    @Test
    public fun testUnitStruct() {
        assertSerTokens(UnitStruct(), listOf(Token.UnitStruct(name = "UnitStruct")))
    }

    @Test
    public fun testTupleStruct() {
        assertSerTokens(TupleStruct(1, 2, 3), listOf(Token.TupleStruct(name = "TupleStruct", len = 3), Token.I32(1), Token.I32(2), Token.I32(3), Token.TupleStructEnd))
    }

    @Test
    public fun testStruct() {
        assertSerTokens(Struct(a = 1, b = 2, c = 3), listOf(Token.Struct(name = "Struct", len = 3), Token.Str("a"), Token.I32(1), Token.Str("b"), Token.I32(2), Token.Str("c"), Token.I32(3), Token.StructEnd))
    }

    @Test
    public fun testEnum() {
        assertSerTokens(Enum.Unit, listOf(Token.UnitVariant(name = "Enum", variant = "Unit")))
        assertSerTokens(Enum.One(42), listOf(Token.NewtypeVariant(name = "Enum", variant = "One"), Token.I32(42)))
        assertSerTokens(Enum.Seq(1, 2), listOf(Token.TupleVariant(name = "Enum", variant = "Seq", len = 2), Token.I32(1), Token.I32(2), Token.TupleVariantEnd))
        assertSerTokens(Enum.Map(a = 1, b = 2), listOf(Token.StructVariant(name = "Enum", variant = "Map", len = 2), Token.Str("a"), Token.I32(1), Token.Str("b"), Token.I32(2), Token.StructVariantEnd))
        assertSerTokens(Enum.OneWithSkipped(NotSerializable), listOf(Token.UnitVariant(name = "Enum", variant = "OneWithSkipped")))
    }

    // testBox requires the owned Box carrier and dereference serialization.

    // testBoxedSlice requires an owned boxed three-element array serialized as a tuple.

    @Test
    public fun testDuration() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                ((1.seconds + 2.nanoseconds)).serialize(serializer)
        }, listOf(Token.Struct(name = "Duration", len = 2), Token.Str("secs"), Token.U64(1uL), Token.Str("nanos"), Token.U32(2u), Token.StructEnd))
    }

    @Test
    public fun testSystemTime() {
        val systemTime = kotlin.time.Instant.fromEpochSeconds(1, 200)
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (systemTime).serialize(serializer)
        }, listOf(Token.Struct(name = "SystemTime", len = 2), Token.Str("secs_since_epoch"), Token.U64(1uL), Token.Str("nanos_since_epoch"), Token.U32(200u), Token.StructEnd))
    }

    @Test
    public fun testRange() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                ((RangeValue(1u, 2u))).serialize(serializer, Serialize.U32)
        }, listOf(Token.Struct(name = "Range", len = 2), Token.Str("start"), Token.U32(1u), Token.Str("end"), Token.U32(2u), Token.StructEnd))
    }

    @Test
    public fun testRangeInclusive() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                ((RangeInclusiveValue(1u, 2u))).serialize(serializer, Serialize.U32)
        }, listOf(Token.Struct(name = "RangeInclusive", len = 2), Token.Str("start"), Token.U32(1u), Token.Str("end"), Token.U32(2u), Token.StructEnd))
    }

    @Test
    public fun testRangeFrom() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                ((RangeFromValue(1u))).serialize(serializer, Serialize.U32)
        }, listOf(Token.Struct(name = "RangeFrom", len = 1), Token.Str("start"), Token.U32(1u), Token.StructEnd))
    }

    @Test
    public fun testRangeTo() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                ((RangeToValue(2u))).serialize(serializer, Serialize.U32)
        }, listOf(Token.Struct(name = "RangeTo", len = 1), Token.Str("end"), Token.U32(2u), Token.StructEnd))
    }

    @Test
    public fun testBound() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (BoundValue.Unbounded).serialize(serializer, Serialize.U8)
        }, listOf(Token.Enum(name = "Bound"), Token.Str("Unbounded"), Token.UnitValue))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (BoundValue.Included(0u.toUByte())).serialize(serializer, Serialize.U8)
        }, listOf(Token.Enum(name = "Bound"), Token.Str("Included"), Token.U8(0u)))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (BoundValue.Excluded(0u.toUByte())).serialize(serializer, Serialize.U8)
        }, listOf(Token.Enum(name = "Bound"), Token.Str("Excluded"), Token.U8(0u)))
    }

    @Test
    public fun testPath() {
        assertSerTokens(PathValue("/usr/local/lib"), listOf(Token.Str("/usr/local/lib")))
    }

    @Test
    public fun testPathBuf() {
        assertSerTokens(PathValue("/usr/local/lib"), listOf(Token.Str("/usr/local/lib")))
    }

    @Test
    public fun testCstring() {
        assertSerTokens(CStringDeserialize.deserialize(ContentDeserializer.new(newByteBufContent("abc".encodeToByteArray()))).getOrThrow(), listOf(Token.Bytes("abc".encodeToByteArray())))
    }

    // testCstr requires a borrowed NUL-terminated byte view with CStr invariants.

    // testRc requires reference-counted ownership and dereference serialization.

    // testRcWeakSome requires weak-reference upgrade while a strong owner is alive.

    // testRcWeakNone requires an empty weak reference that upgrades to no value.

    // testArc requires atomic reference-counted ownership and dereference serialization.

    // testArcWeakSome requires atomic weak-reference upgrade while a strong owner is alive.

    // testArcWeakNone requires an empty atomic weak reference that upgrades to no value.

    @Test
    public fun testWrapping() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                Wrapping(1uL).serialize(serializer, Serialize.U64)
        }, listOf(Token.U64(1uL)))
    }

    @Test
    public fun testSaturating() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                Saturating(1uL).serialize(serializer, Serialize.U64)
        }, listOf(Token.U64(1uL)))
    }

    // testRcDst requires reference-counted dynamically sized strings and slices.

    // testArcDst requires atomically reference-counted dynamically sized strings and slices.

    @Test
    public fun testFmtArguments() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                (FormatArguments("${1}${'a'}")).serialize(serializer)
        }, listOf(Token.Str("1a")))
    }

    // testAtomic requires atomic Boolean and integer loads with relaxed ordering.

    // testAtomic64 requires atomic 64-bit integer loads with relaxed ordering.

    @Test
    public fun testNetIpv4addrReadable() {
        assertSerTokens(Ipv4Address(listOf(1u, 2u, 3u, 4u).map(UInt::toUByte)).readable(), listOf(Token.Str("1.2.3.4")))
    }

    @Test
    public fun testNetIpv6addrReadable() {
        assertSerTokens(Ipv6Address(List(15) { 0.toUByte() } + 1.toUByte()).readable(), listOf(Token.Str("::1")))
    }

    @Test
    public fun testNetIpaddrReadable() {
        assertSerTokens(IpAddress.V4(Ipv4Address(listOf(1u, 2u, 3u, 4u).map(UInt::toUByte))).readable(), listOf(Token.Str("1.2.3.4")))
    }

    @Test
    public fun testNetSocketaddrReadable() {
        assertSerTokens(SocketAddress.V4(SocketAddressV4(Ipv4Address(listOf(1u, 2u, 3u, 4u).map(UInt::toUByte)), 1234u)).readable(), listOf(Token.Str("1.2.3.4:1234")))
        assertSerTokens(SocketAddressV4(Ipv4Address(listOf(1u, 2u, 3u, 4u).map(UInt::toUByte)), 1234u).readable(), listOf(Token.Str("1.2.3.4:1234")))
        assertSerTokens(SocketAddressV6(Ipv6Address(List(15) { 0.toUByte() } + 1.toUByte()), 1234u).readable(), listOf(Token.Str("[::1]:1234")))
    }

    @Test
    public fun testNetIpv4addrCompact() {
        val octets = "1234".encodeToByteArray().map { it.toUByte() }
        assertSerTokens(Ipv4Address(octets).compact(),
            listOf(Token.Tuple(len = 4)) + octets.map { Token.U8(it) } + Token.TupleEnd)
    }

    @Test
    public fun testNetIpv6addrCompact() {
        val octets = "1234567890123456".encodeToByteArray().map { it.toUByte() }
        assertSerTokens(Ipv6Address(octets).compact(),
            listOf(Token.Tuple(len = 16)) + octets.map { Token.U8(it) } + Token.TupleEnd)
    }

    @Test
    public fun testNetIpaddrCompact() {
        val octets = "1234".encodeToByteArray().map { it.toUByte() }
        assertSerTokens(IpAddress.V4(Ipv4Address(octets)).compact(),
            listOf(Token.NewtypeVariant(name = "IpAddr", variant = "V4"), Token.Tuple(len = 4)) +
                octets.map { Token.U8(it) } + Token.TupleEnd)
    }

    @Test
    public fun testNetSocketaddrCompact() {
        val ipv4 = Ipv4Address("1234".encodeToByteArray().map { it.toUByte() })
        val ipv6 = Ipv6Address("1234567890123456".encodeToByteArray().map { it.toUByte() })
        val ipv4Tokens = listOf(Token.Tuple(len = 2), Token.Tuple(len = 4)) +
            ipv4.octets.map { Token.U8(it) } + listOf(Token.TupleEnd, Token.U16(1234u), Token.TupleEnd)
        val ipv6Tokens = listOf(Token.Tuple(len = 2), Token.Tuple(len = 16)) +
            ipv6.octets.map { Token.U8(it) } + listOf(Token.TupleEnd, Token.U16(1234u), Token.TupleEnd)
        assertSerTokens(SocketAddress.V6(SocketAddressV6(ipv6, 1234u)).compact(),
            listOf(Token.NewtypeVariant(name = "SocketAddr", variant = "V6")) + ipv6Tokens)
        assertSerTokens(SocketAddressV4(ipv4, 1234u).compact(), ipv4Tokens)
        assertSerTokens(SocketAddressV6(ipv6, 1234u).compact(), ipv6Tokens)
    }

    @Test
    public fun testNeverResult() {
        // The error type is uninhabited, so only the success branch has a value.
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                ResultValue.Ok(0u.toUByte()).serialize<Ok, UByte, Nothing>(serializer, Serialize.U8, Serialize.U8)
        }, listOf(Token.NewtypeVariant(name = "Result", variant = "Ok"), Token.U8(0u)))
    }

    // testCannotSerializePaths requires a native path that retains invalid UTF-8 bytes.

    // testCannotSerializeMutablyBorrowedRefCell requires a live exclusive borrow that prevents serialization.

    @Test
    public fun testEnumSkipped() {
        assertSerTokensError(Enum.SkippedUnit, listOf(), "the enum variant Enum::SkippedUnit cannot be serialized")
        assertSerTokensError(Enum.SkippedOne(42), listOf(), "the enum variant Enum::SkippedOne cannot be serialized")
        assertSerTokensError(Enum.SkippedSeq(1, 2), listOf(), "the enum variant Enum::SkippedSeq cannot be serialized")
        assertSerTokensError(Enum.SkippedMap(_a = 1, _b = 2), listOf(), "the enum variant Enum::SkippedMap cannot be serialized")
    }

    // testInteger128 requires concrete signed and unsigned 128-bit values.

    // testRefcellDst requires shared borrowing of dynamically sized values.

    // testMutexDst requires mutex guard acquisition for dynamically sized values.

    // testRwlockDst requires shared lock guard acquisition for dynamically sized values.
}
