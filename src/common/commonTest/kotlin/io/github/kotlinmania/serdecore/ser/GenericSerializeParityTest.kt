package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.ResultValue
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertSerTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class GenericSerializeParityTest {
    private fun <T> sequence(values: List<T>, implementation: Serialize.For<T>): Serialize = object : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            values.serialize(serializer, implementation)
    }

    @Test
    fun signedAndFloatingWidthsSurviveNonReifiedGenericCalls() {
        fun <T> check(value: T, implementation: Serialize.For<T>, token: Token) {
            assertSerTokens(sequence(listOf(value), implementation), listOf(Token.Seq(1), token, Token.SeqEnd))
        }
        check(1.toByte(), Serialize.I8, Token.I8(1))
        check(1.toShort(), Serialize.I16, Token.I16(1))
        check(1, Serialize.I32, Token.I32(1))
        check(1L, Serialize.I64, Token.I64(1))
        check(1.0f, Serialize.F32, Token.F32(1.0f))
        check(1.0, Serialize.F64, Token.F64(1.0))
    }

    @Test
    fun dictionariesComposeThroughNestedCollections() {
        val integers = object : Serialize.For<List<Int>> {
            override fun <Ok> serialize(value: List<Int>, serializer: Serializer<Ok>): SerdeResult<Ok> =
                value.serialize(serializer, Serialize.I32)
        }
        assertSerTokens(sequence(listOf(listOf(1, 2)), integers), listOf(
            Token.Seq(1), Token.Seq(2), Token.I32(1), Token.I32(2), Token.SeqEnd, Token.SeqEnd,
        ))
    }

    @Test
    fun resultAndTupleKeepIndependentTypeImplementations() {
        val results = object : Serialize.For<ResultValue<Byte, Int>> {
            override fun <Ok> serialize(value: ResultValue<Byte, Int>, serializer: Serializer<Ok>): SerdeResult<Ok> =
                value.serialize(serializer, Serialize.I8, Serialize.I32)
        }
        assertSerTokens(results.bind(ResultValue.Ok(1)), listOf(Token.NewtypeVariant("Result", "Ok"), Token.I8(1)))
        assertSerTokens(results.bind(ResultValue.Err(1)), listOf(Token.NewtypeVariant("Result", "Err"), Token.I32(1)))
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                Pair(1.toByte(), 1).serialize(serializer, Serialize.I8, Serialize.I32)
        }, listOf(Token.Tuple(2), Token.I8(1), Token.I32(1), Token.TupleEnd))
    }

    @Test
    fun bindingDefersCallbacksAndPreservesReturnedFailureAndThrownException() {
        val failure = SerdeResult.failure(SerdeError.custom("typed failure"))
        val panic = IllegalStateException("typed panic")
        var calls = 0
        val implementation = object : Serialize.For<Int> {
            override fun <Ok> serialize(value: Int, serializer: Serializer<Ok>): SerdeResult<Ok> {
                calls += 1
                if (value == 2) throw panic
                return failure
            }
        }
        val bound = implementation.bind(1)
        assertEquals(0, calls)
        assertSame(failure, bound.serialize(FormatterSerializer(StringBuilder())))
        assertEquals(1, calls)
        assertSame(panic, assertFailsWith<IllegalStateException> {
            implementation.bind(2).serialize(FormatterSerializer(StringBuilder()))
        })
        assertEquals(2, calls)
    }

    @Test
    fun mapKeyFailurePreventsValueDictionaryAndEnd() {
        val failure = SerdeResult.failure(SerdeError.custom("key failure"))
        val events = mutableListOf<String>()
        val key = object : Serialize.For<Int> {
            override fun <Ok> serialize(value: Int, serializer: Serializer<Ok>): SerdeResult<Ok> {
                events.add("key")
                return failure
            }
        }
        val value = object : Serialize.For<Int> {
            override fun <Ok> serialize(value: Int, serializer: Serializer<Ok>): SerdeResult<Ok> =
                error("value dictionary must not run after key failure")
        }
        val probe = object : Serializer<Unit> by FormatterSerializer(StringBuilder()) {
            override fun <K, V> collectMap(iter: Iterable<Pair<K, V>>): SerdeResult<Unit>
                where K : Serialize, V : Serialize = super<Serializer>.collectMap(iter)
            override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> {
                events.add("begin:$len")
                val scalarSerializer = this
                return SerdeResult.success(object : SerializeMap<Unit> {
                    override fun <T> serializeKey(key: T): SerdeResult<Unit> where T : Serialize =
                        key.serialize(scalarSerializer)
                    override fun <T> serializeValue(value: T): SerdeResult<Unit> where T : Serialize =
                        error("value must not be visited after key failure")
                    override fun end(): SerdeResult<Unit> = error("end must not follow key failure")
                })
            }
        }
        assertSame(failure, mapOf(1 to 2).serialize(probe, key, value))
        assertEquals(listOf("begin:1", "key"), events)
    }

    @Test
    fun reverseWrapperPreservesItsInnerStaticType() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                Reverse(1).serialize(serializer, Serialize.I32)
        }, listOf(Token.I32(1)))
    }
}
