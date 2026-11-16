// port-lint: tests test_suite/tests/test_ser.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.ser.FormatArguments
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeStruct
import io.github.kotlinmania.serdecore.ser.SerializeStructVariant
import io.github.kotlinmania.serdecore.ser.SerializeTupleVariant
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertSerTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import io.github.kotlinmania.serdetest.Serializer as TestTokenSerializer

private class BufferedDisplayBugException(
    message: String,
) : RuntimeException(message)

private class BufferedCallbackBugException(
    message: String,
) : RuntimeException(message)

private class BufferedThrowingDisplay(
    private val throwable: BufferedDisplayBugException,
) {
    var calls: Int = 0
        private set

    override fun toString(): String {
        calls += 1
        throw throwable
    }
}

private data class BufferedIntField(
    val value: Int,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeI32(value)
}

private data class BufferedStrField(
    val value: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr(value)
}

private class BufferedFailingSerialize(
    private val failure: SerdeResult<Nothing>,
) : Serialize {
    var calls: Int = 0
        private set

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        calls += 1
        return failure
    }
}

private class BufferedThrowingSerialize(
    private val throwable: RuntimeException,
) : Serialize {
    var calls: Int = 0
        private set

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        calls += 1
        throw throwable
    }
}

private interface BufferedStructScenario {
    fun <Ok> execute(builder: SerializeStruct<Ok>)
}

private interface BufferedStructVariantScenario {
    fun <Ok> execute(builder: SerializeStructVariant<Ok>)
}

private interface BufferedTupleVariantScenario {
    fun <Ok> execute(builder: SerializeTupleVariant<Ok>)
}

private class SingleFieldStructPayload(
    private val structName: String,
    private val key: String,
    private val field: Serialize,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStruct(structName, 1).flatMap { state ->
            state.serializeField(key, field).flatMap {
                state.end()
            }
        }
}

private class ScenarioStructPayload(
    private val structName: String,
    private val lenHint: Int,
    private val scenario: BufferedStructScenario,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStruct(structName, lenHint).flatMap { state ->
            scenario.execute(state)
            state.end()
        }
}

private class SingleFieldStructVariantPayload(
    private val enumName: String,
    private val variantIndex: UInt,
    private val variantName: String,
    private val key: String,
    private val field: Serialize,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStructVariant(enumName, variantIndex, variantName, 1).flatMap { state ->
            state.serializeField(key, field).flatMap {
                state.end()
            }
        }
}

private class ScenarioStructVariantPayload(
    private val enumName: String,
    private val variantIndex: UInt,
    private val variantName: String,
    private val lenHint: Int,
    private val scenario: BufferedStructVariantScenario,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStructVariant(enumName, variantIndex, variantName, lenHint).flatMap { state ->
            scenario.execute(state)
            state.end()
        }
}

private class SingleFieldTupleVariantPayload(
    private val enumName: String,
    private val variantIndex: UInt,
    private val variantName: String,
    private val field: Serialize,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeTupleVariant(enumName, variantIndex, variantName, 1).flatMap { state ->
            state.serializeField(field).flatMap {
                state.end()
            }
        }
}

private class ScenarioTupleVariantPayload(
    private val enumName: String,
    private val variantIndex: UInt,
    private val variantName: String,
    private val lenHint: Int,
    private val scenario: BufferedTupleVariantScenario,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeTupleVariant(enumName, variantIndex, variantName, lenHint).flatMap { state ->
            scenario.execute(state)
            state.end()
        }
}

private class FlattenedTupleContentCarrier(
    private val bufferedPayload: Serialize,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeMap(null).flatMap { outerMap ->
            FlatMapSerializer(outerMap)
                .serializeTupleVariant("OuterEnum", 0u, "TupleCarrier", 1)
                .flatMap { tupleVariant ->
                    tupleVariant.serializeField(bufferedPayload).flatMap {
                        tupleVariant.end().flatMap {
                            outerMap.end()
                        }
                    }
                }
        }
}

private fun <T : Serialize> taggedNewtypePayload(
    typeIdent: String,
    variantIdent: String,
    tag: String,
    variantName: String,
    value: T,
): Serialize =
    object : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializeTaggedNewtype(
                serializer = serializer,
                typeIdent = typeIdent,
                variantIdent = variantIdent,
                tag = tag,
                variantName = variantName,
                value = value,
            )
    }

class BufferedFieldFailurePropagationTest {
    @Test
    fun contentStructSerializeFieldPropagatesFormattingAndCallbackThrowsPreservesErrorsAndSkipsMutation() {
        val prefixTokens = listOf(Token.Map(null), Token.Str("TupleCarrier"))

        val returnedError = SerdeError("struct field failed", source = SerdeError.custom("struct field cause"))
        val returnedFailure = SerdeResult.failure(returnedError)
        val failingField = BufferedFailingSerialize(returnedFailure)
        val failSer = TestTokenSerializer.new(prefixTokens)
        val failResult =
            FlattenedTupleContentCarrier(
                SingleFieldStructPayload("BufferedStruct", "bad", failingField),
            ).serialize(failSer)
        assertTrue(failResult.isFailure)
        assertEquals(1, failingField.calls)
        assertSame(returnedError, failResult.exceptionOrNull())
        assertEquals(0, failSer.remaining())

        val displayBug = BufferedDisplayBugException("content struct display formatting bug")
        val throwingDisplay = BufferedThrowingDisplay(displayBug)
        val displaySer = TestTokenSerializer.new(prefixTokens)
        val thrownDisplay =
            assertFailsWith<BufferedDisplayBugException> {
                val unexpected =
                    FlattenedTupleContentCarrier(
                        SingleFieldStructPayload("BufferedStruct", "fmt", FormatArguments(throwingDisplay)),
                    ).serialize(displaySer)
                error("expected BufferedDisplayBugException, got $unexpected")
            }
        assertSame(displayBug, thrownDisplay)
        assertEquals(1, throwingDisplay.calls)
        assertEquals(0, displaySer.remaining())

        val callbackBug = BufferedCallbackBugException("content struct user serialize bug")
        val throwingCallback = BufferedThrowingSerialize(callbackBug)
        val callbackSer = TestTokenSerializer.new(prefixTokens)
        val thrownCallback =
            assertFailsWith<BufferedCallbackBugException> {
                val unexpected =
                    FlattenedTupleContentCarrier(
                        SingleFieldStructPayload("BufferedStruct", "cb", throwingCallback),
                    ).serialize(callbackSer)
                error("expected BufferedCallbackBugException, got $unexpected")
            }
        assertSame(callbackBug, thrownCallback)
        assertEquals(1, throwingCallback.calls)
        assertEquals(0, callbackSer.remaining())
        assertSame(returnedFailure, failResult)

        assertSerTokens(
            FlattenedTupleContentCarrier(
                ScenarioStructPayload(
                    structName = "BufferedStruct",
                    lenHint = 2,
                    scenario =
                        object : BufferedStructScenario {
                            override fun <Ok> execute(builder: SerializeStruct<Ok>) {
                                builder.serializeField("before", BufferedIntField(10)).getOrThrow()

                                val inlineError = SerdeError("inline struct error", source = SerdeError.custom("cause"))
                                val inlineFailure = SerdeResult.failure(inlineError)
                                val inlineFailing = BufferedFailingSerialize(inlineFailure)
                                val inlineResult = builder.serializeField("droppedError", inlineFailing)
                                assertEquals(1, inlineFailing.calls)
                                assertSame(inlineFailure, inlineResult)
                                assertSame(inlineError, inlineResult.exceptionOrNull())

                                val inlineDisplayBug = BufferedDisplayBugException("inline struct display bug")
                                val inlineDisplay = BufferedThrowingDisplay(inlineDisplayBug)
                                val caughtDisplay =
                                    assertFailsWith<BufferedDisplayBugException> {
                                        val unexpected =
                                            builder.serializeField("droppedDisplay", FormatArguments(inlineDisplay))
                                        error("expected BufferedDisplayBugException, got $unexpected")
                                    }
                                assertSame(inlineDisplayBug, caughtDisplay)
                                assertEquals(1, inlineDisplay.calls)

                                val inlineCallbackBug = BufferedCallbackBugException("inline struct callback bug")
                                val inlineCallback = BufferedThrowingSerialize(inlineCallbackBug)
                                val caughtCallback =
                                    assertFailsWith<BufferedCallbackBugException> {
                                        val unexpected = builder.serializeField("droppedCallback", inlineCallback)
                                        error("expected BufferedCallbackBugException, got $unexpected")
                                    }
                                assertSame(inlineCallbackBug, caughtCallback)
                                assertEquals(1, inlineCallback.calls)

                                builder.serializeField("after", BufferedStrField("kept")).getOrThrow()
                            }
                        },
                ),
            ),
            listOf(
                Token.Map(null),
                Token.Str("TupleCarrier"),
                Token.Seq(1),
                Token.Struct("BufferedStruct", 2),
                Token.Str("before"),
                Token.I32(10),
                Token.Str("after"),
                Token.Str("kept"),
                Token.StructEnd,
                Token.SeqEnd,
                Token.MapEnd,
            ),
        )
    }

    @Test
    fun contentStructVariantSerializeFieldPropagatesFormattingAndCallbackThrowsPreservesErrorsAndSkipsMutation() {
        val prefixTokens = listOf(Token.Map(null), Token.Str("TupleCarrier"))

        val returnedError =
            SerdeError("struct variant field failed", source = SerdeError.custom("struct variant cause"))
        val returnedFailure = SerdeResult.failure(returnedError)
        val failingField = BufferedFailingSerialize(returnedFailure)
        val failSer = TestTokenSerializer.new(prefixTokens)
        val failResult =
            FlattenedTupleContentCarrier(
                SingleFieldStructVariantPayload("InnerEnum", 2u, "StructVar", "bad", failingField),
            ).serialize(failSer)
        assertTrue(failResult.isFailure)
        assertEquals(1, failingField.calls)
        assertSame(returnedError, failResult.exceptionOrNull())
        assertEquals(0, failSer.remaining())

        val displayBug = BufferedDisplayBugException("content struct variant display formatting bug")
        val throwingDisplay = BufferedThrowingDisplay(displayBug)
        val displaySer = TestTokenSerializer.new(prefixTokens)
        val thrownDisplay =
            assertFailsWith<BufferedDisplayBugException> {
                val unexpected =
                    FlattenedTupleContentCarrier(
                        SingleFieldStructVariantPayload(
                            "InnerEnum",
                            2u,
                            "StructVar",
                            "fmt",
                            FormatArguments(throwingDisplay),
                        ),
                    ).serialize(displaySer)
                error("expected BufferedDisplayBugException, got $unexpected")
            }
        assertSame(displayBug, thrownDisplay)
        assertEquals(1, throwingDisplay.calls)
        assertEquals(0, displaySer.remaining())

        val callbackBug = BufferedCallbackBugException("content struct variant user serialize bug")
        val throwingCallback = BufferedThrowingSerialize(callbackBug)
        val callbackSer = TestTokenSerializer.new(prefixTokens)
        val thrownCallback =
            assertFailsWith<BufferedCallbackBugException> {
                val unexpected =
                    FlattenedTupleContentCarrier(
                        SingleFieldStructVariantPayload("InnerEnum", 2u, "StructVar", "cb", throwingCallback),
                    ).serialize(callbackSer)
                error("expected BufferedCallbackBugException, got $unexpected")
            }
        assertSame(callbackBug, thrownCallback)
        assertEquals(1, throwingCallback.calls)
        assertEquals(0, callbackSer.remaining())
        assertSame(returnedFailure, failResult)

        assertSerTokens(
            FlattenedTupleContentCarrier(
                ScenarioStructVariantPayload(
                    enumName = "InnerEnum",
                    variantIndex = 2u,
                    variantName = "StructVar",
                    lenHint = 2,
                    scenario =
                        object : BufferedStructVariantScenario {
                            override fun <Ok> execute(builder: SerializeStructVariant<Ok>) {
                                builder.serializeField("before", BufferedIntField(10)).getOrThrow()

                                val inlineError =
                                    SerdeError("inline struct variant error", source = SerdeError.custom("cause"))
                                val inlineFailure = SerdeResult.failure(inlineError)
                                val inlineFailing = BufferedFailingSerialize(inlineFailure)
                                val inlineResult = builder.serializeField("droppedError", inlineFailing)
                                assertEquals(1, inlineFailing.calls)
                                assertSame(inlineFailure, inlineResult)
                                assertSame(inlineError, inlineResult.exceptionOrNull())

                                val inlineDisplayBug = BufferedDisplayBugException("inline struct variant display bug")
                                val inlineDisplay = BufferedThrowingDisplay(inlineDisplayBug)
                                val caughtDisplay =
                                    assertFailsWith<BufferedDisplayBugException> {
                                        val unexpected =
                                            builder.serializeField("droppedDisplay", FormatArguments(inlineDisplay))
                                        error("expected BufferedDisplayBugException, got $unexpected")
                                    }
                                assertSame(inlineDisplayBug, caughtDisplay)
                                assertEquals(1, inlineDisplay.calls)

                                val inlineCallbackBug =
                                    BufferedCallbackBugException("inline struct variant callback bug")
                                val inlineCallback = BufferedThrowingSerialize(inlineCallbackBug)
                                val caughtCallback =
                                    assertFailsWith<BufferedCallbackBugException> {
                                        val unexpected = builder.serializeField("droppedCallback", inlineCallback)
                                        error("expected BufferedCallbackBugException, got $unexpected")
                                    }
                                assertSame(inlineCallbackBug, caughtCallback)
                                assertEquals(1, inlineCallback.calls)

                                builder.serializeField("after", BufferedStrField("kept")).getOrThrow()
                            }
                        },
                ),
            ),
            listOf(
                Token.Map(null),
                Token.Str("TupleCarrier"),
                Token.Seq(1),
                Token.StructVariant("InnerEnum", "StructVar", 2),
                Token.Str("before"),
                Token.I32(10),
                Token.Str("after"),
                Token.Str("kept"),
                Token.StructVariantEnd,
                Token.SeqEnd,
                Token.MapEnd,
            ),
        )
    }

    @Test
    fun taggedTupleVariantAsMapValueSerializeFieldPropagatesThrowsPreservesErrorsAndSkipsMutation() {
        val prefixTokens =
            listOf(
                Token.Map(2),
                Token.Str("type"),
                Token.Str("WrappedTuple"),
                Token.Str("TuplePayload"),
            )

        val returnedError =
            SerdeError("tagged tuple variant field failed", source = SerdeError.custom("tuple variant cause"))
        val returnedFailure = SerdeResult.failure(returnedError)
        val failingField = BufferedFailingSerialize(returnedFailure)
        val failSer = TestTokenSerializer.new(prefixTokens)
        val failResult =
            taggedNewtypePayload(
                typeIdent = "TaggedOuter",
                variantIdent = "WrappedTuple",
                tag = "type",
                variantName = "WrappedTuple",
                value = SingleFieldTupleVariantPayload("InnerEnum", 1u, "TuplePayload", failingField),
            ).serialize(failSer)
        assertTrue(failResult.isFailure)
        assertEquals(1, failingField.calls)
        assertSame(returnedError, failResult.exceptionOrNull())
        assertEquals(0, failSer.remaining())

        val displayBug = BufferedDisplayBugException("tagged tuple variant display formatting bug")
        val throwingDisplay = BufferedThrowingDisplay(displayBug)
        val displaySer = TestTokenSerializer.new(prefixTokens)
        val thrownDisplay =
            assertFailsWith<BufferedDisplayBugException> {
                val unexpected =
                    taggedNewtypePayload(
                        typeIdent = "TaggedOuter",
                        variantIdent = "WrappedTuple",
                        tag = "type",
                        variantName = "WrappedTuple",
                        value =
                            SingleFieldTupleVariantPayload(
                                "InnerEnum",
                                1u,
                                "TuplePayload",
                                FormatArguments(throwingDisplay),
                            ),
                    ).serialize(displaySer)
                error("expected BufferedDisplayBugException, got $unexpected")
            }
        assertSame(displayBug, thrownDisplay)
        assertEquals(1, throwingDisplay.calls)
        assertEquals(0, displaySer.remaining())

        val callbackBug = BufferedCallbackBugException("tagged tuple variant user serialize bug")
        val throwingCallback = BufferedThrowingSerialize(callbackBug)
        val callbackSer = TestTokenSerializer.new(prefixTokens)
        val thrownCallback =
            assertFailsWith<BufferedCallbackBugException> {
                val unexpected =
                    taggedNewtypePayload(
                        typeIdent = "TaggedOuter",
                        variantIdent = "WrappedTuple",
                        tag = "type",
                        variantName = "WrappedTuple",
                        value = SingleFieldTupleVariantPayload("InnerEnum", 1u, "TuplePayload", throwingCallback),
                    ).serialize(callbackSer)
                error("expected BufferedCallbackBugException, got $unexpected")
            }
        assertSame(callbackBug, thrownCallback)
        assertEquals(1, throwingCallback.calls)
        assertEquals(0, callbackSer.remaining())
        assertSame(returnedFailure, failResult)

        assertSerTokens(
            taggedNewtypePayload(
                typeIdent = "TaggedOuter",
                variantIdent = "WrappedTuple",
                tag = "type",
                variantName = "WrappedTuple",
                value =
                    ScenarioTupleVariantPayload(
                        enumName = "InnerEnum",
                        variantIndex = 1u,
                        variantName = "TuplePayload",
                        lenHint = 2,
                        scenario =
                            object : BufferedTupleVariantScenario {
                                override fun <Ok> execute(builder: SerializeTupleVariant<Ok>) {
                                    builder.serializeField(BufferedIntField(10)).getOrThrow()

                                    val inlineError =
                                        SerdeError("inline tagged tuple error", source = SerdeError.custom("cause"))
                                    val inlineFailure = SerdeResult.failure(inlineError)
                                    val inlineFailing = BufferedFailingSerialize(inlineFailure)
                                    val inlineResult = builder.serializeField(inlineFailing)
                                    assertEquals(1, inlineFailing.calls)
                                    assertSame(inlineFailure, inlineResult)
                                    assertSame(inlineError, inlineResult.exceptionOrNull())

                                    val inlineDisplayBug =
                                        BufferedDisplayBugException("inline tagged tuple display bug")
                                    val inlineDisplay = BufferedThrowingDisplay(inlineDisplayBug)
                                    val caughtDisplay =
                                        assertFailsWith<BufferedDisplayBugException> {
                                            val unexpected = builder.serializeField(FormatArguments(inlineDisplay))
                                            error("expected BufferedDisplayBugException, got $unexpected")
                                        }
                                    assertSame(inlineDisplayBug, caughtDisplay)
                                    assertEquals(1, inlineDisplay.calls)

                                    val inlineCallbackBug =
                                        BufferedCallbackBugException("inline tagged tuple callback bug")
                                    val inlineCallback = BufferedThrowingSerialize(inlineCallbackBug)
                                    val caughtCallback =
                                        assertFailsWith<BufferedCallbackBugException> {
                                            val unexpected = builder.serializeField(inlineCallback)
                                            error("expected BufferedCallbackBugException, got $unexpected")
                                        }
                                    assertSame(inlineCallbackBug, caughtCallback)
                                    assertEquals(1, inlineCallback.calls)

                                    builder.serializeField(BufferedStrField("kept")).getOrThrow()
                                }
                            },
                    ),
            ),
            listOf(
                Token.Map(2),
                Token.Str("type"),
                Token.Str("WrappedTuple"),
                Token.Str("TuplePayload"),
                Token.TupleStruct("TuplePayload", 2),
                Token.I32(10),
                Token.Str("kept"),
                Token.TupleStructEnd,
                Token.MapEnd,
            ),
        )
    }

    @Test
    fun taggedStructVariantAsMapValueSerializeFieldPropagatesThrowsPreservesErrorsAndSkipsMutation() {
        val prefixTokens =
            listOf(
                Token.Map(2),
                Token.Str("type"),
                Token.Str("WrappedStruct"),
                Token.Str("StructPayload"),
            )

        val returnedError =
            SerdeError("tagged struct variant field failed", source = SerdeError.custom("struct variant cause"))
        val returnedFailure = SerdeResult.failure(returnedError)
        val failingField = BufferedFailingSerialize(returnedFailure)
        val failSer = TestTokenSerializer.new(prefixTokens)
        val failResult =
            taggedNewtypePayload(
                typeIdent = "TaggedOuter",
                variantIdent = "WrappedStruct",
                tag = "type",
                variantName = "WrappedStruct",
                value = SingleFieldStructVariantPayload("InnerEnum", 2u, "StructPayload", "bad", failingField),
            ).serialize(failSer)
        assertTrue(failResult.isFailure)
        assertEquals(1, failingField.calls)
        assertSame(returnedError, failResult.exceptionOrNull())
        assertEquals(0, failSer.remaining())

        val displayBug = BufferedDisplayBugException("tagged struct variant display formatting bug")
        val throwingDisplay = BufferedThrowingDisplay(displayBug)
        val displaySer = TestTokenSerializer.new(prefixTokens)
        val thrownDisplay =
            assertFailsWith<BufferedDisplayBugException> {
                val unexpected =
                    taggedNewtypePayload(
                        typeIdent = "TaggedOuter",
                        variantIdent = "WrappedStruct",
                        tag = "type",
                        variantName = "WrappedStruct",
                        value =
                            SingleFieldStructVariantPayload(
                                "InnerEnum",
                                2u,
                                "StructPayload",
                                "fmt",
                                FormatArguments(throwingDisplay),
                            ),
                    ).serialize(displaySer)
                error("expected BufferedDisplayBugException, got $unexpected")
            }
        assertSame(displayBug, thrownDisplay)
        assertEquals(1, throwingDisplay.calls)
        assertEquals(0, displaySer.remaining())

        val callbackBug = BufferedCallbackBugException("tagged struct variant user serialize bug")
        val throwingCallback = BufferedThrowingSerialize(callbackBug)
        val callbackSer = TestTokenSerializer.new(prefixTokens)
        val thrownCallback =
            assertFailsWith<BufferedCallbackBugException> {
                val unexpected =
                    taggedNewtypePayload(
                        typeIdent = "TaggedOuter",
                        variantIdent = "WrappedStruct",
                        tag = "type",
                        variantName = "WrappedStruct",
                        value =
                            SingleFieldStructVariantPayload(
                                "InnerEnum",
                                2u,
                                "StructPayload",
                                "cb",
                                throwingCallback,
                            ),
                    ).serialize(callbackSer)
                error("expected BufferedCallbackBugException, got $unexpected")
            }
        assertSame(callbackBug, thrownCallback)
        assertEquals(1, throwingCallback.calls)
        assertEquals(0, callbackSer.remaining())
        assertSame(returnedFailure, failResult)

        assertSerTokens(
            taggedNewtypePayload(
                typeIdent = "TaggedOuter",
                variantIdent = "WrappedStruct",
                tag = "type",
                variantName = "WrappedStruct",
                value =
                    ScenarioStructVariantPayload(
                        enumName = "InnerEnum",
                        variantIndex = 2u,
                        variantName = "StructPayload",
                        lenHint = 2,
                        scenario =
                            object : BufferedStructVariantScenario {
                                override fun <Ok> execute(builder: SerializeStructVariant<Ok>) {
                                    builder.serializeField("before", BufferedIntField(10)).getOrThrow()

                                    val inlineError =
                                        SerdeError("inline tagged struct error", source = SerdeError.custom("cause"))
                                    val inlineFailure = SerdeResult.failure(inlineError)
                                    val inlineFailing = BufferedFailingSerialize(inlineFailure)
                                    val inlineResult = builder.serializeField("droppedError", inlineFailing)
                                    assertEquals(1, inlineFailing.calls)
                                    assertSame(inlineFailure, inlineResult)
                                    assertSame(inlineError, inlineResult.exceptionOrNull())

                                    val inlineDisplayBug =
                                        BufferedDisplayBugException("inline tagged struct display bug")
                                    val inlineDisplay = BufferedThrowingDisplay(inlineDisplayBug)
                                    val caughtDisplay =
                                        assertFailsWith<BufferedDisplayBugException> {
                                            val unexpected =
                                                builder.serializeField("droppedDisplay", FormatArguments(inlineDisplay))
                                            error("expected BufferedDisplayBugException, got $unexpected")
                                        }
                                    assertSame(inlineDisplayBug, caughtDisplay)
                                    assertEquals(1, inlineDisplay.calls)

                                    val inlineCallbackBug =
                                        BufferedCallbackBugException("inline tagged struct callback bug")
                                    val inlineCallback = BufferedThrowingSerialize(inlineCallbackBug)
                                    val caughtCallback =
                                        assertFailsWith<BufferedCallbackBugException> {
                                            val unexpected = builder.serializeField("droppedCallback", inlineCallback)
                                            error("expected BufferedCallbackBugException, got $unexpected")
                                        }
                                    assertSame(inlineCallbackBug, caughtCallback)
                                    assertEquals(1, inlineCallback.calls)

                                    builder.serializeField("after", BufferedStrField("kept")).getOrThrow()
                                }
                            },
                    ),
            ),
            listOf(
                Token.Map(2),
                Token.Str("type"),
                Token.Str("WrappedStruct"),
                Token.Str("StructPayload"),
                Token.Struct("StructPayload", 2),
                Token.Str("before"),
                Token.I32(10),
                Token.Str("after"),
                Token.Str("kept"),
                Token.StructEnd,
                Token.MapEnd,
            ),
        )
    }
}
