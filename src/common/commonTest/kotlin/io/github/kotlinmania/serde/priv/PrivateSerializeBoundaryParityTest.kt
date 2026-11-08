// port-lint: tests test_suite/tests/test_ser.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeException
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.ser.FormatterSerializer
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeMap
import io.github.kotlinmania.serdecore.ser.SerializeSeq
import io.github.kotlinmania.serdecore.ser.SerializeStruct
import io.github.kotlinmania.serdecore.ser.SerializeStructVariant
import io.github.kotlinmania.serdecore.ser.SerializeTuple
import io.github.kotlinmania.serdecore.ser.SerializeTupleStruct
import io.github.kotlinmania.serdecore.ser.SerializeTupleVariant
import io.github.kotlinmania.serdecore.ser.Serializer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class PrivateSerializeBoundaryParityTest {
    private class Probe(
        private val failAt: Int = -1,
        private val failure: SerdeResult<Nothing>? = null,
        private val panic: Throwable? = null,
    ) : Serializer<Unit> by FormatterSerializer(StringBuilder()) {
        val events = mutableListOf<String>()

        private fun step(event: String): SerdeResult<Unit> {
            events.add(event)
            if (events.lastIndex == failAt) {
                panic?.let { throw it }
                failure?.let { return it }
            }
            return SerdeResult.success(Unit)
        }

        val compound = Compound(::step)

        override fun <T : Serialize> collectSeq(iter: Iterable<T>): SerdeResult<Unit> = super<Serializer>.collectSeq(iter)
        override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> = step("seq:$len").map { compound }
        override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit>> = step("tuple:$len").map { compound }
        override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<Unit>> =
            step("tupleStruct:$name:$len").map { compound }
        override fun serializeTupleVariant(name: String, variantIndex: UInt, variant: String, len: Int): SerdeResult<SerializeTupleVariant<Unit>> =
            step("tupleVariant:$name:$variantIndex:$variant:$len").map { compound }
        override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> = step("map:$len").map { compound }
        override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<Unit>> = step("struct:$name:$len").map { compound }
        override fun serializeStructVariant(name: String, variantIndex: UInt, variant: String, len: Int): SerdeResult<SerializeStructVariant<Unit>> =
            step("structVariant:$name:$variantIndex:$variant:$len").map { compound }
    }

    private class Compound(private val step: (String) -> SerdeResult<Unit>) : SerializeSeq<Unit>, SerializeTuple<Unit>, SerializeTupleStruct<Unit>,
        SerializeTupleVariant<Unit>, SerializeMap<Unit>, SerializeStruct<Unit>, SerializeStructVariant<Unit> {
        override fun <T : Serialize> serializeElement(value: T): SerdeResult<Unit> = step("element")
        override fun <T : Serialize> serializeField(value: T): SerdeResult<Unit> = step("field")
        override fun <T : Serialize> serializeField(key: String, value: T): SerdeResult<Unit> = step("field:$key")
        override fun <T : Serialize> serializeKey(key: T): SerdeResult<Unit> = step("key")
        override fun <T : Serialize> serializeValue(value: T): SerdeResult<Unit> = step("value")
        override fun <K : Serialize, V : Serialize> serializeEntry(key: K, value: V): SerdeResult<Unit> = step("entry")
        override fun skipField(key: String): SerdeResult<Unit> = step("skip:$key")
        override fun end(): SerdeResult<Unit> = step("end")
    }

    private fun compoundContents(): List<Content> {
        val values = listOf(Content.I32(7), Content.I32(8))
        val fields = listOf("first" to values[0], "second" to values[1])
        return listOf(
            Content.Seq(values), Content.Tuple(values), Content.TupleStruct("T", values),
            Content.TupleVariant("E", 1u, "V", values),
            Content.Map(listOf(Content.String("first") to values[0], Content.String("second") to values[1])),
            Content.Struct("S", fields), Content.StructVariant("E", 2u, "V", fields),
        )
    }

    private fun checkBoundaries(invoke: (Probe) -> SerdeResult<Unit>) {
        val normal = Probe()
        invoke(normal).getOrThrow()
        for (index in normal.events.indices) {
            val failure = SerdeResult.failure(SerdeError.custom("returned failure"))
            val failed = Probe(index, failure = failure)
            assertSame(failure, invoke(failed))
            assertEquals(normal.events.take(index + 1), failed.events)
            val panic = SerdeException(SerdeError.custom("thrown callback"))
            val panicked = Probe(index, panic = panic)
            assertSame(panic, assertFailsWith<SerdeException> { invoke(panicked) })
            assertEquals(normal.events.take(index + 1), panicked.events)
        }
    }

    @Test
    fun contentReplayPreservesFailuresAndPanicsAtEveryDelegateBoundary() {
        for (content in compoundContents()) checkBoundaries { content.serialize(it) }
    }

    @Test
    fun taggedCompoundsPreserveFailuresAndPanicsAtEveryDelegateBoundary() {
        val content = listOf(
            Content.Unit, Content.UnitStruct("U"), Content.UnitVariant("E", 0u, "V"),
            Content.NewtypeVariant("E", 1u, "V", Content.I32(7)),
        ) + compoundContents().drop(3)
        for (value in content) {
            checkBoundaries { serializeTaggedNewtype(it, "Outer", "Variant", "tag", "name", value) }
        }
    }

    @Test
    fun taggedMapAndStructReturnTheOriginalDelegateStatesAndLengthHints() {
        for (len in listOf(null, 0, 2)) {
            val probe = Probe()
            val value = object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serializer.serializeMap(len).flatMap { state ->
                        assertSame<Any>(probe.compound, state)
                        state.end()
                    }
            }
            serializeTaggedNewtype(probe, "Outer", "Variant", "tag", "name", value).getOrThrow()
            assertEquals(listOf("map:${len?.plus(1)}", "entry", "end"), probe.events)
        }
        val probe = Probe()
        val value = object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                serializer.serializeStruct("S", 2).flatMap { state ->
                    assertSame<Any>(probe.compound, state)
                    state.end()
                }
        }
        serializeTaggedNewtype(probe, "Outer", "Variant", "tag", "name", value).getOrThrow()
        assertEquals(listOf("struct:S:3", "field:tag", "end"), probe.events)
    }

    private class CaptureMap : SerializeMap<Unit> {
        val values = mutableListOf<Content>()
        override fun <T : Serialize> serializeKey(key: T): SerdeResult<Unit> = SerdeResult.success(Unit)
        override fun <T : Serialize> serializeValue(value: T): SerdeResult<Unit> {
            values.add(assertIs<Content>(value))
            return SerdeResult.success(Unit)
        }
        override fun end(): SerdeResult<Unit> = SerdeResult.success(Unit)
    }

    private fun capture(value: Serialize): Content {
        val map = CaptureMap()
        val state = FlatMapSerializer(map).serializeTupleVariant("Carrier", 0u, "V", 1).getOrThrow()
        state.serializeField(value).getOrThrow()
        state.end().getOrThrow()
        return assertIs<Content.Seq>(map.values.single()).elements.single()
    }

    private class FailureValue(private val failure: SerdeResult<Nothing>) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = failure
    }

    private class PanicValue(private val panic: Throwable) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = throw panic
    }

    private class Wrapper(private val kind: Int, private val value: Serialize) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = when (kind) {
            0 -> serializer.serializeSome(value)
            1 -> serializer.serializeNewtypeStruct("N", value)
            else -> serializer.serializeNewtypeVariant("E", 1u, "V", value)
        }
    }

    @Test
    fun contentWrappersPreserveFailureAndPanicWithoutAppendingFailedValues() {
        for (kind in 0..2) {
            val map = CaptureMap()
            val state = FlatMapSerializer(map).serializeTupleVariant("Carrier", 0u, "V", 3).getOrThrow()
            val failure = SerdeResult.failure(SerdeError.custom("returned failure"))
            assertSame(failure, state.serializeField(Wrapper(kind, FailureValue(failure))))
            val panic = SerdeException(SerdeError.custom("thrown callback"))
            assertSame(panic, assertFailsWith<SerdeException> { state.serializeField(Wrapper(kind, PanicValue(panic))) })
            state.serializeField(Content.I32(7)).getOrThrow()
            state.end().getOrThrow()
            assertEquals(Content.Seq(listOf(Content.I32(7))), map.values.single())
        }
    }

    private class Buffer<Ok>(val add: (Serialize) -> SerdeResult<Unit>, val end: () -> SerdeResult<Ok>)

    private fun <Ok> buffer(serializer: Serializer<Ok>, kind: Int): SerdeResult<Buffer<Ok>> = when (kind) {
        0 -> serializer.serializeSeq(3).map { state -> Buffer({ state.serializeElement(it) }, { state.end() }) }
        1 -> serializer.serializeTuple(3).map { state -> Buffer({ state.serializeElement(it) }, { state.end() }) }
        2 -> serializer.serializeTupleStruct("T", 3).map { state -> Buffer({ state.serializeField(it) }, { state.end() }) }
        3 -> serializer.serializeTupleVariant("E", 1u, "V", 3).map { state -> Buffer({ state.serializeField(it) }, { state.end() }) }
        4 -> serializer.serializeMap(3).map { state -> Buffer({ state.serializeEntry(Content.String("key"), it) }, { state.end() }) }
        5 -> serializer.serializeStruct("S", 3).map { state -> Buffer({ state.serializeField("key", it) }, { state.end() }) }
        else -> serializer.serializeStructVariant("E", 1u, "V", 3).map { state -> Buffer({ state.serializeField("key", it) }, { state.end() }) }
    }

    @Test
    fun allContentBuffersAppendOnlyAfterSuccessfulSerialization() {
        val expected = listOf(
            Content.Seq(listOf(Content.I32(7))), Content.Tuple(listOf(Content.I32(7))),
            Content.TupleStruct("T", listOf(Content.I32(7))), Content.TupleVariant("E", 1u, "V", listOf(Content.I32(7))),
            Content.Map(listOf(Content.String("key") to Content.I32(7))),
            Content.Struct("S", listOf("key" to Content.I32(7))),
            Content.StructVariant("E", 1u, "V", listOf("key" to Content.I32(7))),
        )
        for (kind in expected.indices) {
            val value = object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    buffer(serializer, kind).flatMap { state ->
                        val failure = SerdeResult.failure(SerdeError.custom("returned failure"))
                        assertSame(failure, state.add(FailureValue(failure)))
                        val panic = SerdeException(SerdeError.custom("thrown callback"))
                        assertSame(panic, assertFailsWith<SerdeException> { state.add(PanicValue(panic)) })
                        state.add(Content.I32(7)).flatMap { state.end() }
                    }
            }
            assertEquals(expected[kind], capture(value))
        }
    }

    @Test
    fun contentByteBufferOwnsItsSnapshotBeforeCallerMutation() {
        val bytes = byteArrayOf(1, 2, 3)
        val value = object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                serializer.serializeBytes(bytes).map { captured ->
                    bytes[1] = 99
                    captured
                }
        }
        val content = assertIs<Content.Bytes>(capture(value))
        assertContentEquals(byteArrayOf(1, 2, 3), content.value)
        assertContentEquals(byteArrayOf(1, 99, 3), bytes)
    }

    @Test
    fun flatMapAdaptersForwardOperationsWithoutEndingTheOuterMap() {
        val probe = Probe()
        val flat = FlatMapSerializer(probe.compound)
        val map = flat.serializeMap(null).getOrThrow()
        map.serializeKey(Content.String("key")).getOrThrow()
        map.serializeValue(Content.I32(7)).getOrThrow()
        map.serializeEntry(Content.String("other"), Content.I32(8)).getOrThrow()
        map.end().getOrThrow()
        val struct = flat.serializeStruct("S", 1).getOrThrow()
        struct.serializeField("field", Content.I32(9)).getOrThrow()
        struct.end().getOrThrow()
        assertEquals(listOf("key", "value", "entry", "entry"), probe.events)
    }
}
