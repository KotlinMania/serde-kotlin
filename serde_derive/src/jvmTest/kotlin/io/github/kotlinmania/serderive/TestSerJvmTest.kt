// port-lint: tests test_suite/tests/test_ser.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.BoundValue
import io.github.kotlinmania.serdecore.de.IpAddress
import io.github.kotlinmania.serdecore.de.Ipv4Address
import io.github.kotlinmania.serdecore.de.Ipv6Address
import io.github.kotlinmania.serdecore.de.RangeFromValue
import io.github.kotlinmania.serdecore.de.RangeInclusiveValue
import io.github.kotlinmania.serdecore.de.RangeToValue
import io.github.kotlinmania.serdecore.de.RangeValue
import io.github.kotlinmania.serdecore.de.ResultValue
import io.github.kotlinmania.serdecore.de.SocketAddress
import io.github.kotlinmania.serdecore.de.SocketAddressV4
import io.github.kotlinmania.serdecore.de.SocketAddressV6
import io.github.kotlinmania.serdecore.ser.FormatArguments
import io.github.kotlinmania.serdecore.ser.Saturating
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdecore.ser.Wrapping
import io.github.kotlinmania.serdecore.ser.serialize
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.TokenSerializer
import io.github.kotlinmania.serdetest.compact
import io.github.kotlinmania.serdetest.readable
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TestSerJvmTest {
    @Test
    fun testUnit() {
        assertKotlinSerTokens(listOf(Token.UnitValue)) { Unit.serialize(it) }
    }

    @Test
    fun testBool() {
        assertKotlinSerTokens(listOf(Token.Bool(true))) { true.serialize(it) }
        assertKotlinSerTokens(listOf(Token.Bool(false))) { false.serialize(it) }
    }

    @Test
    fun testIsizes() {
        assertKotlinSerTokens(listOf(Token.I8(0))) { 0.toByte().serialize(it) }
        assertKotlinSerTokens(listOf(Token.I16(0))) { 0.toShort().serialize(it) }
        assertKotlinSerTokens(listOf(Token.I32(0))) { 0.serialize(it) }
        assertKotlinSerTokens(listOf(Token.I64(0))) { 0L.serialize(it) }
    }

    @Test
    fun testUsizes() {
        assertKotlinSerTokens(listOf(Token.U8(0u))) { 0.toUByte().serialize(it) }
        assertKotlinSerTokens(listOf(Token.U16(0u))) { 0.toUShort().serialize(it) }
        assertKotlinSerTokens(listOf(Token.U32(0u))) { 0u.serialize(it) }
        assertKotlinSerTokens(listOf(Token.U64(0u))) { 0uL.serialize(it) }
    }

    @Test
    fun testFloats() {
        assertKotlinSerTokens(listOf(Token.F32(0f))) { 0f.serialize(it) }
        assertKotlinSerTokens(listOf(Token.F64(0.0))) { 0.0.serialize(it) }
    }

    @Test
    fun testChar() {
        assertKotlinSerTokens(listOf(Token.CharValue('a'))) { 'a'.serialize(it) }
    }

    @Test
    fun testStr() {
        assertKotlinSerTokens(listOf(Token.Str("abc"))) { "abc".serialize(it) }
    }

    @Test
    fun testOption() {
        val none: I32Value? = null
        val some: I32Value? = I32Value(1)
        assertKotlinSerTokens(listOf(Token.None)) { none.serialize(it) }
        assertKotlinSerTokens(listOf(Token.Some, Token.I32(1))) { some.serialize(it) }
    }

    @Test
    fun testResult() {
        val ok: ResultValue<I32Value, I32Value> = ResultValue.Ok(I32Value(0))
        val err: ResultValue<I32Value, I32Value> = ResultValue.Err(I32Value(1))
        assertKotlinSerTokens(
            listOf(Token.NewtypeVariant("Result", "Ok"), Token.I32(0)),
        ) { ok.serialize(it) }
        assertKotlinSerTokens(
            listOf(Token.NewtypeVariant("Result", "Err"), Token.I32(1)),
        ) { err.serialize(it) }
    }

    @Test
    fun testSlice() {
        assertKotlinSerTokens(listOf(Token.Seq(0), Token.SeqEnd)) {
            emptyList<I32Value>().serialize(it)
        }
        assertKotlinSerTokens(
            listOf(Token.Seq(3), Token.I32(1), Token.I32(2), Token.I32(3), Token.SeqEnd),
        ) { listOf(I32Value(1), I32Value(2), I32Value(3)).serialize(it) }
    }

    @Test
    fun testArray() {
        assertKotlinSerTokens(listOf(Token.Tuple(0), Token.TupleEnd)) {
            emptyArray<I32Value>().serialize(it)
        }
        assertKotlinSerTokens(
            listOf(Token.Tuple(3), Token.I32(1), Token.I32(2), Token.I32(3), Token.TupleEnd),
        ) { arrayOf(I32Value(1), I32Value(2), I32Value(3)).serialize(it) }
    }

    @Test
    fun testVec() {
        val values =
            listOf(
                SequenceValue(emptyList()),
                SequenceValue(listOf(I32Value(1))),
                SequenceValue(listOf(I32Value(2), I32Value(3))),
            )
        assertKotlinSerTokens(
            listOf(
                Token.Seq(3),
                Token.Seq(0),
                Token.SeqEnd,
                Token.Seq(1),
                Token.I32(1),
                Token.SeqEnd,
                Token.Seq(2),
                Token.I32(2),
                Token.I32(3),
                Token.SeqEnd,
                Token.SeqEnd,
            ),
        ) { values.serialize(it) }
    }

    @Test
    fun testBtreeset() {
        assertKotlinSerTokens(listOf(Token.Seq(0), Token.SeqEnd)) {
            emptySet<I32Value>().serialize(it)
        }
        assertKotlinSerTokens(listOf(Token.Seq(1), Token.I32(1), Token.SeqEnd)) {
            linkedSetOf(I32Value(1)).serialize(it)
        }
    }

    @Test
    fun testHashset() {
        assertKotlinSerTokens(listOf(Token.Seq(0), Token.SeqEnd)) {
            emptySet<I32Value>().serialize(it)
        }
        assertKotlinSerTokens(listOf(Token.Seq(1), Token.I32(1), Token.SeqEnd)) {
            hashSetOf(I32Value(1)).serialize(it)
        }
    }

    @Test
    fun testTuple() {
        // Rust singleton tuple syntax has no Kotlin tuple counterpart.
        assertKotlinSerTokens(
            listOf(Token.Tuple(3), Token.I32(1), Token.I32(2), Token.I32(3), Token.TupleEnd),
        ) { Triple(I32Value(1), I32Value(2), I32Value(3)).serialize(it) }
    }

    @Test
    fun testBtreemap() {
        assertKotlinSerTokens(
            listOf(Token.Map(2), Token.I32(1), Token.I32(2), Token.I32(3), Token.I32(4), Token.MapEnd),
        ) {
            linkedMapOf(I32Value(1) to I32Value(2), I32Value(3) to I32Value(4)).serialize(it)
        }
        val nested =
            linkedMapOf(
                I32Value(1) to MapValue(emptyMap()),
                I32Value(2) to MapValue(linkedMapOf(I32Value(3) to I32Value(4), I32Value(5) to I32Value(6))),
            )
        assertKotlinSerTokens(
            listOf(
                Token.Map(2),
                Token.I32(1),
                Token.Map(0),
                Token.MapEnd,
                Token.I32(2),
                Token.Map(2),
                Token.I32(3),
                Token.I32(4),
                Token.I32(5),
                Token.I32(6),
                Token.MapEnd,
                Token.MapEnd,
            ),
        ) { nested.serialize(it) }
    }

    @Test
    fun testHashmap() {
        assertKotlinSerTokens(listOf(Token.Map(0), Token.MapEnd)) {
            emptyMap<I32Value, I32Value>().serialize(it)
        }
        assertKotlinSerTokens(listOf(Token.Map(1), Token.I32(1), Token.I32(2), Token.MapEnd)) {
            mapOf(I32Value(1) to I32Value(2)).serialize(it)
        }
    }

    @Test
    fun testUnitStruct() = runDerivedSerRustTest("test_unit_struct")

    @Test
    fun testTupleStruct() = runDerivedSerRustTest("test_tuple_struct")

    @Test
    fun testStruct() = runDerivedSerRustTest("test_struct")

    @Test
    fun testEnum() = runDerivedSerRustTest("test_enum")

    // test_box: Rust Box ownership and transparent dereference serialization have no Kotlin counterpart.
    // test_boxed_slice: Rust boxed dynamically sized slice ownership has no Kotlin counterpart.

    @Test
    fun testDuration() {
        assertKotlinSerTokens(
            listOf(
                Token.Struct("Duration", 2),
                Token.Str("secs"),
                Token.U64(1u),
                Token.Str("nanos"),
                Token.U32(2u),
                Token.StructEnd,
            ),
        ) { (1.seconds + 2.nanoseconds).serialize(it) }
    }

    @Test
    fun testSystemTime() {
        assertKotlinSerTokens(
            listOf(
                Token.Struct("SystemTime", 2),
                Token.Str("secs_since_epoch"),
                Token.U64(1u),
                Token.Str("nanos_since_epoch"),
                Token.U32(200u),
                Token.StructEnd,
            ),
        ) { Instant.fromEpochSeconds(1, 200).serialize(it) }
    }

    @Test
    fun testRange() {
        assertKotlinSerTokens(rangeTokens("Range", "start" to 1, "end" to 2)) {
            RangeValue(U32Value(1u), U32Value(2u)).serialize(it)
        }
    }

    @Test
    fun testRangeInclusive() {
        assertKotlinSerTokens(rangeTokens("RangeInclusive", "start" to 1, "end" to 2)) {
            RangeInclusiveValue(U32Value(1u), U32Value(2u)).serialize(it)
        }
    }

    @Test
    fun testRangeFrom() {
        assertKotlinSerTokens(rangeTokens("RangeFrom", "start" to 1)) {
            RangeFromValue(U32Value(1u)).serialize(it)
        }
    }

    @Test
    fun testRangeTo() {
        assertKotlinSerTokens(rangeTokens("RangeTo", "end" to 2)) {
            RangeToValue(U32Value(2u)).serialize(it)
        }
    }

    @Test
    fun testBound() {
        val unbounded: BoundValue<U8Value> = BoundValue.Unbounded
        assertKotlinSerTokens(listOf(Token.Enum("Bound"), Token.Str("Unbounded"), Token.UnitValue)) {
            unbounded.serialize(it)
        }
        assertKotlinSerTokens(listOf(Token.Enum("Bound"), Token.Str("Included"), Token.U8(0u))) {
            BoundValue.Included(U8Value(0u)).serialize(it)
        }
        assertKotlinSerTokens(listOf(Token.Enum("Bound"), Token.Str("Excluded"), Token.U8(0u))) {
            BoundValue.Excluded(U8Value(0u)).serialize(it)
        }
    }

    // test_path: Rust Path preserves platform path semantics not represented by a Kotlin String.
    // test_path_buf: Rust owned PathBuf semantics have no common Kotlin filesystem-path counterpart.
    // test_cstring: Rust CString NUL-terminated byte ownership has no common Kotlin counterpart.
    // test_cstr: Rust borrowed CStr lifetime and NUL-terminated bytes have no common Kotlin counterpart.
    // test_rc: Rust single-threaded reference-counted ownership has no Kotlin counterpart.
    // test_rc_weak_some: Rust Rc weak-upgrade liveness semantics have no Kotlin counterpart.
    // test_rc_weak_none: Rust empty RcWeak state has no Kotlin counterpart.
    // test_arc: Rust atomic reference-counted ownership has no Kotlin counterpart.
    // test_arc_weak_some: Rust Arc weak-upgrade liveness semantics have no Kotlin counterpart.
    // test_arc_weak_none: Rust empty ArcWeak state has no Kotlin counterpart.

    @Test
    fun testWrapping() {
        assertKotlinSerTokens(listOf(Token.U64(1u))) { Wrapping(U64Value(1u)).serialize(it) }
    }

    @Test
    fun testSaturating() {
        assertKotlinSerTokens(listOf(Token.U64(1u))) { Saturating(U64Value(1u)).serialize(it) }
    }

    // test_rc_dst: Rust Rc ownership of dynamically sized str and slice values has no Kotlin counterpart.
    // test_arc_dst: Rust Arc ownership of dynamically sized str and slice values has no Kotlin counterpart.

    @Test
    fun testFmtArguments() {
        assertKotlinSerTokens(listOf(Token.Str("1a"))) { FormatArguments("${1}${'a'}").serialize(it) }
    }

    // test_atomic: Rust atomic integer load ordering and width semantics have no common Kotlin equivalent in this port.
    // test_atomic64: Rust target-gated 64-bit atomic semantics have no common Kotlin equivalent in this port.

    @Test
    fun testNetIpv4addrReadable() {
        val address = Ipv4Address(listOf(1u, 2u, 3u, 4u).map(UInt::toUByte))
        assertKotlinSerTokens(listOf(Token.Str("1.2.3.4"))) { address.readable().serialize(it) }
    }

    @Test
    fun testNetIpv6addrReadable() {
        val address = Ipv6Address(List(15) { 0.toUByte() } + 1.toUByte())
        assertKotlinSerTokens(listOf(Token.Str("::1"))) { address.readable().serialize(it) }
    }

    @Test
    fun testNetIpaddrReadable() {
        val address = IpAddress.V4(Ipv4Address(listOf(1u, 2u, 3u, 4u).map(UInt::toUByte)))
        assertKotlinSerTokens(listOf(Token.Str("1.2.3.4"))) { address.readable().serialize(it) }
    }

    @Test
    fun testNetSocketaddrReadable() {
        val ipv4 = Ipv4Address(listOf(1u, 2u, 3u, 4u).map(UInt::toUByte))
        val ipv6 = Ipv6Address(List(15) { 0.toUByte() } + 1.toUByte())
        assertKotlinSerTokens(listOf(Token.Str("1.2.3.4:1234"))) {
            SocketAddress.V4(SocketAddressV4(ipv4, 1234u)).readable().serialize(it)
        }
        assertKotlinSerTokens(listOf(Token.Str("1.2.3.4:1234"))) {
            SocketAddressV4(ipv4, 1234u).readable().serialize(it)
        }
        assertKotlinSerTokens(listOf(Token.Str("[::1]:1234"))) {
            SocketAddressV6(ipv6, 1234u).readable().serialize(it)
        }
    }

    @Test
    fun testNetIpv4addrCompact() {
        val octets = "1234".octets()
        assertKotlinSerTokens(octetTuple(octets)) { Ipv4Address(octets).compact().serialize(it) }
    }

    @Test
    fun testNetIpv6addrCompact() {
        val octets = "1234567890123456".octets()
        assertKotlinSerTokens(octetTuple(octets)) { Ipv6Address(octets).compact().serialize(it) }
    }

    @Test
    fun testNetIpaddrCompact() {
        val octets = "1234".octets()
        assertKotlinSerTokens(
            listOf(Token.NewtypeVariant("IpAddr", "V4")) + octetTuple(octets),
        ) { IpAddress.V4(Ipv4Address(octets)).compact().serialize(it) }
    }

    @Test
    fun testNetSocketaddrCompact() {
        val ipv4 = Ipv4Address("1234".octets())
        val ipv6 = Ipv6Address("1234567890123456".octets())
        val ipv4Tokens = listOf(Token.Tuple(2)) + octetTuple(ipv4.octets) + listOf(Token.U16(1234u), Token.TupleEnd)
        val ipv6Tokens = listOf(Token.Tuple(2)) + octetTuple(ipv6.octets) + listOf(Token.U16(1234u), Token.TupleEnd)
        assertKotlinSerTokens(
            listOf(Token.NewtypeVariant("SocketAddr", "V6")) + ipv6Tokens,
        ) { SocketAddress.V6(SocketAddressV6(ipv6, 1234u)).compact().serialize(it) }
        assertKotlinSerTokens(ipv4Tokens) { SocketAddressV4(ipv4, 1234u).compact().serialize(it) }
        assertKotlinSerTokens(ipv6Tokens) { SocketAddressV6(ipv6, 1234u).compact().serialize(it) }
    }

    // test_never_result: Rust never type inhabitation and Result<u8, !> have no Kotlin semantic equivalent.
    // test_cannot_serialize_paths: Rust Unix paths can contain invalid UTF-8 bytes, while Kotlin String cannot.
    // test_cannot_serialize_mutably_borrowed_ref_cell: Rust RefCell runtime borrow failure has no Kotlin equivalent.

    @Test
    fun testEnumSkipped() = runDerivedSerRustTest("test_enum_skipped")

    @Test
    fun testInteger128() {
        assertKotlinSerTokensError(emptyList(), "i128 is not supported") { it.serializeI128("1") }
        assertKotlinSerTokensError(emptyList(), "u128 is not supported") { it.serializeU128("1") }
    }

    // test_refcell_dst: Rust RefCell ownership of a dynamically sized slice has no Kotlin equivalent.
    // test_mutex_dst: Rust poisonable Mutex ownership of a dynamically sized slice has no common Kotlin equivalent.
    // test_rwlock_dst: Rust poisonable RwLock ownership of a dynamically sized slice has no common Kotlin equivalent.
}

private val serFixtureSource by lazy(::buildSerFixtureSource)

private fun runDerivedSerRustTest(testName: String) {
    runExactSerdeRustTest(
        fixtureName = "test_ser",
        source = serFixtureSource,
        testName = testName,
        extraDependencies = "foldhash = \"0.2\"",
        serdeFeatures = listOf("rc"),
    )
}

private fun buildSerFixtureSource(): String {
    val root = findRepositoryRoot()
    val macros =
        Files.readString(root.resolve("tmp/serde/test_suite/tests/macros/mod.rs"))
            .removePrefix("#![allow(unused_macro_rules)]\n\n")
    val source =
        Files.readString(root.resolve("tmp/serde/test_suite/tests/test_ser.rs"))
            .replace(
                "#[macro_use]\nmod macros;",
                "#[macro_use]\n#[allow(unused_macro_rules)]\nmod macros {\n$macros\n}",
            )
    val generated = generateRustFixtureFromSerdeDerives(source)
    require(generated.serdeDerivedItems == 4) {
        "Expected 4 serde-derived declarations, transformed ${generated.serdeDerivedItems}"
    }
    return generated.source
}

private fun assertKotlinSerTokens(
    tokens: List<Token>,
    serialize: (Serializer<Unit>) -> SerdeResult<Unit>,
) {
    val serializer = TokenSerializer.new(tokens)
    when (val result = serialize(serializer)) {
        is SerdeResult.Success -> Unit
        is SerdeResult.Failure -> error("value failed to serialize: ${result.error}")
    }
    assertEquals(0, serializer.remaining(), "remaining tokens")
}

private fun assertKotlinSerTokensError(
    tokens: List<Token>,
    error: String,
    serialize: (Serializer<Unit>) -> SerdeResult<Unit>,
) {
    val serializer = TokenSerializer.new(tokens)
    when (val result = serialize(serializer)) {
        is SerdeResult.Success -> error("value serialized successfully")
        is SerdeResult.Failure -> assertEquals(error, result.error.message)
    }
    assertEquals(0, serializer.remaining(), "remaining tokens")
}

private fun rangeTokens(
    name: String,
    vararg fields: Pair<String, Int>,
): List<Token> =
    buildList {
        add(Token.Struct(name, fields.size))
        for ((field, value) in fields) {
            add(Token.Str(field))
            add(Token.U32(value.toUInt()))
        }
        add(Token.StructEnd)
    }

private fun octetTuple(octets: List<UByte>): List<Token> =
    buildList {
        add(Token.Tuple(octets.size))
        octets.forEach { add(Token.U8(it)) }
        add(Token.TupleEnd)
    }

private fun String.octets(): List<UByte> = map { it.code.toUByte() }

private data class I32Value(
    val value: Int,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class U8Value(
    val value: UByte,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class U32Value(
    val value: UInt,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class U64Value(
    val value: ULong,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class SequenceValue<T : Serialize>(
    val values: List<T>,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = values.serialize(serializer)
}

private data class MapValue<K : Serialize, V : Serialize>(
    val values: Map<K, V>,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = values.serialize(serializer)
}
