package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.I32Deserializer
import kotlin.test.Test
import kotlin.test.assertEquals

class IgnoredAnyBoundaryParityTest {
    private fun probe(mode: Int, stage: String, target: String): SerdeResult<Unit> {
        if (stage == target) {
            if (mode == 1) return SerdeResult.failure(SerdeError.custom("probe error"))
            if (mode == 2) error("probe panic")
        }
        return SerdeResult.success(Unit)
    }

    private class Scalar(
        private val value: Int,
        private val trace: MutableList<String>,
    ) : Deserializer by I32Deserializer.new(value) {
        override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> {
            trace.add("discard:$value")
            return visitor.visitI32(value)
        }
    }

    private inner class Sequence(
        private val mode: Int,
        private val target: String,
        private val trace: MutableList<String>,
    ) : SeqAccess {
        private var index = 0

        override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> {
            val current = index++
            val stage = "seq:$current"
            trace.add(stage)
            return probe(mode, stage, target).flatMap {
                if (current == 2) {
                    SerdeResult.success(SeqElement.None)
                } else {
                    seed.deserialize(Scalar(10 * (current + 1), trace)).map { SeqElement.Some(it) }
                }
            }
        }
    }

    private inner class Map(
        private val mode: Int,
        private val target: String,
        private val trace: MutableList<String>,
    ) : MapAccess {
        private var index = 0

        override fun <T> nextKeySeed(seed: DeserializeSeed<T>): SerdeResult<T?> {
            val stage = "key:$index"
            trace.add(stage)
            return probe(mode, stage, target).flatMap {
                if (index == 2) {
                    SerdeResult.success(null)
                } else {
                    seed.deserialize(Scalar(index + 1, trace))
                }
            }
        }

        override fun <T> nextValueSeed(seed: DeserializeSeed<T>): SerdeResult<T> {
            val current = index
            val stage = "value:$current"
            trace.add(stage)
            return probe(mode, stage, target).flatMap {
                index += 1
                seed.deserialize(Scalar(10 * (current + 1), trace))
            }
        }
    }

    private inner class Enum(
        private val mode: Int,
        private val target: String,
        private val trace: MutableList<String>,
    ) : EnumAccess {
        override fun <T> variantSeed(seed: DeserializeSeed<T>): SerdeResult<Pair<T, VariantAccess>> {
            trace.add("variant")
            return probe(mode, "variant", target).flatMap {
                seed.deserialize(Scalar(99, trace)).map { key -> key to Variant(mode, target, trace) }
            }
        }
    }

    private inner class Variant(
        private val mode: Int,
        private val target: String,
        private val trace: MutableList<String>,
    ) : VariantAccess {
        override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> {
            trace.add("payload")
            return probe(mode, "payload", target).flatMap { seed.deserialize(Scalar(100, trace)) }
        }

        override fun unitVariant(): SerdeResult<Unit> = error("unexpected unit variant")

        override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): SerdeResult<V> = error("unexpected tuple variant")

        override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): SerdeResult<V> = error("unexpected struct variant")
    }

    private fun check(kind: String, mode: Int, target: String, outcome: String, events: List<String>) {
        val trace = mutableListOf<String>()
        val actual =
            try {
                val result =
                    when (kind) {
                        "seq" -> IgnoredAny.visitSeq(Sequence(mode, target, trace))
                        "map" -> IgnoredAny.visitMap(Map(mode, target, trace))
                        "enum" -> IgnoredAny.visitEnum(Enum(mode, target, trace))
                        else -> error("unexpected kind")
                    }
                when (result) {
                    is SerdeResult.Success -> "ok"
                    is SerdeResult.Failure -> "error:${result.error}"
                }
            } catch (exception: IllegalStateException) {
                "panic:${exception.message}"
            }
        assertEquals(outcome, actual)
        assertEquals(events, trace)
    }

    @Test
    fun seqSuccess() {
        check("seq", 0, "", "ok", listOf("seq:0", "discard:10", "seq:1", "discard:20", "seq:2"))
    }

    @Test
    fun seqErrors() {
        check("seq", 1, "seq:0", "error:probe error", listOf("seq:0"))
        check("seq", 1, "seq:1", "error:probe error", listOf("seq:0", "discard:10", "seq:1"))
        check("seq", 1, "seq:2", "error:probe error", listOf("seq:0", "discard:10", "seq:1", "discard:20", "seq:2"))
    }

    @Test
    fun seqPanics() {
        check("seq", 2, "seq:0", "panic:probe panic", listOf("seq:0"))
        check("seq", 2, "seq:1", "panic:probe panic", listOf("seq:0", "discard:10", "seq:1"))
        check("seq", 2, "seq:2", "panic:probe panic", listOf("seq:0", "discard:10", "seq:1", "discard:20", "seq:2"))
    }

    @Test
    fun mapSuccess() {
        check("map", 0, "", "ok", listOf("key:0", "discard:1", "value:0", "discard:10", "key:1", "discard:2", "value:1", "discard:20", "key:2"))
    }

    @Test
    fun mapErrors() {
        check("map", 1, "key:1", "error:probe error", listOf("key:0", "discard:1", "value:0", "discard:10", "key:1"))
        check("map", 1, "value:1", "error:probe error", listOf("key:0", "discard:1", "value:0", "discard:10", "key:1", "discard:2", "value:1"))
    }

    @Test
    fun mapPanics() {
        check("map", 2, "key:1", "panic:probe panic", listOf("key:0", "discard:1", "value:0", "discard:10", "key:1"))
        check("map", 2, "value:1", "panic:probe panic", listOf("key:0", "discard:1", "value:0", "discard:10", "key:1", "discard:2", "value:1"))
    }

    @Test
    fun enumSuccess() {
        check("enum", 0, "", "ok", listOf("variant", "discard:99", "payload", "discard:100"))
    }

    @Test
    fun enumErrors() {
        check("enum", 1, "variant", "error:probe error", listOf("variant"))
        check("enum", 1, "payload", "error:probe error", listOf("variant", "discard:99", "payload"))
    }

    @Test
    fun enumPanics() {
        check("enum", 2, "variant", "panic:probe panic", listOf("variant"))
        check("enum", 2, "payload", "panic:probe panic", listOf("variant", "discard:99", "payload"))
    }
}
