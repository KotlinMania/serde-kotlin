// port-lint: source serde/src/private/ser.rs
package io.github.kotlinmania.serde.`private`

import io.github.kotlinmania.serde.core.ser.*
import io.github.kotlinmania.serde.core.ser.SerializeMap
import io.github.kotlinmania.serde.core.ser.SerializeStruct
import io.github.kotlinmania.serde.core.ser.SerializeStructVariant
import io.github.kotlinmania.serde.core.ser.SerializeTuple
import io.github.kotlinmania.serde.core.ser.SerializeTupleStruct
import io.github.kotlinmania.serde.core.ser.SerializeTupleVariant

/**
 * Used to check that serde(getter) attributes return the expected type.
 * Not public API.
 */
fun <T> constrain(t: T): T = t

/**
 * Not public API.
 */
fun <Ok, E, S, T> serializeTaggedNewtype(
    serializer: S,
    typeIdent: String,
    variantIdent: String,
    tag: String,
    variantName: String,
    value: T,
): Result<Ok>
    where E : Error,
          S : Serializer<Ok, E>,
          T : Serialize =
    value.serialize(
        TaggedSerializer(
            typeIdent = typeIdent,
            variantIdent = variantIdent,
            tag = tag,
            variantName = variantName,
            delegate = serializer,
        ),
    )

private class TaggedSerializer<Ok, E, S>(
    private val typeIdent: String,
    private val variantIdent: String,
    private val tag: String,
    private val variantName: String,
    private val delegate: S,
) : Serializer<Ok, E>
    where E : Error,
          S : Serializer<Ok, E> {
    private fun badType(what: Unsupported): Throwable =
        Error.custom(
            "cannot serialize tagged newtype variant $typeIdent::$variantIdent containing $what",
        )

    override fun serializeBool(v: Boolean): Result<Ok> {
        return Result.failure(badType(Unsupported.Boolean))
    }

    override fun serializeI8(v: Byte): Result<Ok> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeI16(v: Short): Result<Ok> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeI32(v: Int): Result<Ok> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeI64(v: Long): Result<Ok> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeU8(v: UByte): Result<Ok> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeU16(v: UShort): Result<Ok> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeU32(v: UInt): Result<Ok> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeU64(v: ULong): Result<Ok> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeF32(v: Float): Result<Ok> {
        return Result.failure(badType(Unsupported.Float))
    }

    override fun serializeF64(v: Double): Result<Ok> {
        return Result.failure(badType(Unsupported.Float))
    }

    override fun serializeChar(v: Char): Result<Ok> {
        return Result.failure(badType(Unsupported.Char))
    }

    override fun serializeStr(v: String): Result<Ok> {
        return Result.failure(badType(Unsupported.String))
    }

    override fun serializeBytes(v: ByteArray): Result<Ok> {
        return Result.failure(badType(Unsupported.ByteArray))
    }

    override fun serializeNone(): Result<Ok> = Result.failure(badType(Unsupported.Optional))

    override fun <T> serializeSome(value: T): Result<Ok>
        where T : Serialize {
        return Result.failure(badType(Unsupported.Optional))
    }

    override fun serializeUnit(): Result<Ok> =
        runCatching {
            val map = delegate.serializeMap(1).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.end().getOrThrow()
        }

    override fun serializeUnitStruct(name: String): Result<Ok> =
        runCatching {
            val map = delegate.serializeMap(1).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.end().getOrThrow()
        }

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): Result<Ok> =
        runCatching {
            val map = delegate.serializeMap(2).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.serializeEntry(Content.String(variant), Content.Unit).getOrThrow()
            map.end().getOrThrow()
        }

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): Result<Ok>
        where T : Serialize {
        return value.serialize(this)
    }

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): Result<Ok>
        where T : Serialize =
        runCatching {
            val map = delegate.serializeMap(2).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.serializeEntry(Content.String(variant), value).getOrThrow()
            map.end().getOrThrow()
        }

    override fun serializeSeq(len: Int?): Result<io.github.kotlinmania.serde.core.ser.SerializeSeq<Ok, E>> {
        return Result.failure(badType(Unsupported.Sequence))
    }

    override fun serializeTuple(len: Int): Result<SerializeTuple<Ok, E>> {
        return Result.failure(badType(Unsupported.Tuple))
    }

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): Result<SerializeTupleStruct<Ok, E>> {
        return Result.failure(badType(Unsupported.TupleStruct))
    }

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<Ok, E>> =
        runCatching {
            val map = delegate.serializeMap(2).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.serializeKey(Content.String(variant)).getOrThrow()
            SerializeTupleVariantAsMapValue(map = map, name = variant, len = len)
        }

    override fun serializeMap(len: Int?): Result<SerializeMap<Ok, E>> =
        runCatching {
            val map = delegate.serializeMap(len?.plus(1)).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map
        }

    override fun serializeStruct(
        name: String,
        len: Int,
    ): Result<SerializeStruct<Ok, E>> =
        runCatching {
            val state = delegate.serializeStruct(name, len + 1).getOrThrow()
            state.serializeField(tag, Content.String(variantName)).getOrThrow()
            state
        }

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<Ok, E>> =
        runCatching {
            val map = delegate.serializeMap(2).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.serializeKey(Content.String(variant)).getOrThrow()
            SerializeStructVariantAsMapValue(map = map, name = variant, len = len)
        }

    override fun collectStr(value: String): Result<Ok> = Result.failure(badType(Unsupported.String))
}

private enum class Unsupported {
    Boolean,
    Integer,
    Float,
    Char,
    String,
    ByteArray,
    Optional,
    Sequence,
    Tuple,
    TupleStruct,
    Enum,
    ;

    override fun toString(): String =
        when (this) {
            Boolean -> "a boolean"
            Integer -> "an integer"
            Float -> "a float"
            Char -> "a char"
            String -> "a string"
            ByteArray -> "a byte array"
            Optional -> "an optional"
            Sequence -> "a sequence"
            Tuple -> "a tuple"
            TupleStruct -> "a tuple struct"
            Enum -> "an enum"
        }
}

private sealed class Content : Serialize {
    data class Bool(
        val value: Boolean,
    ) : Content()

    data class U8(
        val value: UByte,
    ) : Content()

    data class U16(
        val value: UShort,
    ) : Content()

    data class U32(
        val value: UInt,
    ) : Content()

    data class U64(
        val value: ULong,
    ) : Content()

    data class I8(
        val value: Byte,
    ) : Content()

    data class I16(
        val value: Short,
    ) : Content()

    data class I32(
        val value: Int,
    ) : Content()

    data class I64(
        val value: Long,
    ) : Content()

    data class F32(
        val value: Float,
    ) : Content()

    data class F64(
        val value: Double,
    ) : Content()

    data class Char(
        val value: kotlin.Char,
    ) : Content()

    data class String(
        val value: kotlin.String,
    ) : Content()

    data class Bytes(
        val value: ByteArray,
    ) : Content() {
        override fun equals(other: Any?): Boolean = this === other || other is Bytes && value.contentEquals(other.value)

        override fun hashCode(): Int = value.contentHashCode()
    }

    data object None : Content()

    data class Some(
        val value: Content,
    ) : Content()

    data object Unit : Content()

    data class UnitStruct(
        val name: kotlin.String,
    ) : Content()

    data class UnitVariant(
        val name: kotlin.String,
        val variantIndex: UInt,
        val variant: kotlin.String,
    ) : Content()

    data class NewtypeStruct(
        val name: kotlin.String,
        val value: Content,
    ) : Content()

    data class NewtypeVariant(
        val name: kotlin.String,
        val variantIndex: UInt,
        val variant: kotlin.String,
        val value: Content,
    ) : Content()

    data class Seq(
        val elements: List<Content>,
    ) : Content()

    data class Tuple(
        val elements: List<Content>,
    ) : Content()

    data class TupleStruct(
        val name: kotlin.String,
        val fields: List<Content>,
    ) : Content()

    data class TupleVariant(
        val name: kotlin.String,
        val variantIndex: UInt,
        val variant: kotlin.String,
        val fields: List<Content>,
    ) : Content()

    data class Map(
        val entries: List<Pair<Content, Content>>,
    ) : Content()

    data class Struct(
        val name: kotlin.String,
        val fields: List<Pair<kotlin.String, Content>>,
    ) : Content()

    data class StructVariant(
        val name: kotlin.String,
        val variantIndex: UInt,
        val variant: kotlin.String,
        val fields: List<Pair<kotlin.String, Content>>,
    ) : Content()

    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error =
        when (this) {
            is Bool -> serializer.serializeBool(value)
            is U8 -> serializer.serializeU8(value)
            is U16 -> serializer.serializeU16(value)
            is U32 -> serializer.serializeU32(value)
            is U64 -> serializer.serializeU64(value)
            is I8 -> serializer.serializeI8(value)
            is I16 -> serializer.serializeI16(value)
            is I32 -> serializer.serializeI32(value)
            is I64 -> serializer.serializeI64(value)
            is F32 -> serializer.serializeF32(value)
            is F64 -> serializer.serializeF64(value)
            is Char -> serializer.serializeChar(value)
            is String -> serializer.serializeStr(value)
            is Bytes -> serializer.serializeBytes(value)
            None -> serializer.serializeNone()
            is Some -> serializer.serializeSome(value)
            Unit -> serializer.serializeUnit()
            is UnitStruct -> serializer.serializeUnitStruct(name)
            is UnitVariant -> serializer.serializeUnitVariant(name, variantIndex, variant)
            is NewtypeStruct -> serializer.serializeNewtypeStruct(name, value)
            is NewtypeVariant ->
                serializer.serializeNewtypeVariant(name, variantIndex, variant, value)
            is Seq ->
                serializer.collectSeq(elements)
            is Tuple ->
                runCatching {
                    val tuple = serializer.serializeTuple(elements.size).getOrThrow()
                    for (element in elements) {
                        tuple.serializeElement(element).getOrThrow()
                    }
                    tuple.end().getOrThrow()
                }
            is TupleStruct ->
                runCatching {
                    val tupleStruct = serializer.serializeTupleStruct(name, fields.size).getOrThrow()
                    for (field in fields) {
                        tupleStruct.serializeField(field).getOrThrow()
                    }
                    tupleStruct.end().getOrThrow()
                }
            is TupleVariant ->
                runCatching {
                    val tupleVariant =
                        serializer.serializeTupleVariant(name, variantIndex, variant, fields.size).getOrThrow()
                    for (field in fields) {
                        tupleVariant.serializeField(field).getOrThrow()
                    }
                    tupleVariant.end().getOrThrow()
                }
            is Map ->
                runCatching {
                    val map = serializer.serializeMap(entries.size).getOrThrow()
                    for ((k, v) in entries) {
                        map.serializeEntry(k, v).getOrThrow()
                    }
                    map.end().getOrThrow()
                }
            is Struct ->
                runCatching {
                    val struct = serializer.serializeStruct(name, fields.size).getOrThrow()
                    for ((k, v) in fields) {
                        struct.serializeField(k, v).getOrThrow()
                    }
                    struct.end().getOrThrow()
                }
            is StructVariant ->
                runCatching {
                    val structVariant =
                        serializer.serializeStructVariant(name, variantIndex, variant, fields.size).getOrThrow()
                    for ((k, v) in fields) {
                        structVariant.serializeField(k, v).getOrThrow()
                    }
                    structVariant.end().getOrThrow()
                }
        }
}

private class ContentSerializer<E> : Serializer<Content, E>
    where E : Error {
    override fun serializeBool(v: Boolean): Result<Content> = Result.success(Content.Bool(v))

    override fun serializeI8(v: Byte): Result<Content> = Result.success(Content.I8(v))

    override fun serializeI16(v: Short): Result<Content> = Result.success(Content.I16(v))

    override fun serializeI32(v: Int): Result<Content> = Result.success(Content.I32(v))

    override fun serializeI64(v: Long): Result<Content> = Result.success(Content.I64(v))

    override fun serializeU8(v: UByte): Result<Content> = Result.success(Content.U8(v))

    override fun serializeU16(v: UShort): Result<Content> = Result.success(Content.U16(v))

    override fun serializeU32(v: UInt): Result<Content> = Result.success(Content.U32(v))

    override fun serializeU64(v: ULong): Result<Content> = Result.success(Content.U64(v))

    override fun serializeF32(v: Float): Result<Content> = Result.success(Content.F32(v))

    override fun serializeF64(v: Double): Result<Content> = Result.success(Content.F64(v))

    override fun serializeChar(v: Char): Result<Content> = Result.success(Content.Char(v))

    override fun serializeStr(v: String): Result<Content> = Result.success(Content.String(v))

    override fun serializeBytes(v: ByteArray): Result<Content> = Result.success(Content.Bytes(v))

    override fun serializeNone(): Result<Content> = Result.success(Content.None)

    override fun <T> serializeSome(value: T): Result<Content>
        where T : Serialize =
        runCatching { Content.Some(value.serialize(ContentSerializer<E>()).getOrThrow()) }

    override fun serializeUnit(): Result<Content> = Result.success(Content.Unit)

    override fun serializeUnitStruct(name: String): Result<Content> = Result.success(Content.UnitStruct(name))

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): Result<Content> = Result.success(Content.UnitVariant(name, variantIndex, variant))

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): Result<Content>
        where T : Serialize =
        runCatching { Content.NewtypeStruct(name, value.serialize(ContentSerializer<E>()).getOrThrow()) }

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): Result<Content>
        where T : Serialize =
        runCatching {
            Content.NewtypeVariant(name, variantIndex, variant, value.serialize(ContentSerializer<E>()).getOrThrow())
        }

    override fun serializeSeq(len: Int?): Result<io.github.kotlinmania.serde.core.ser.SerializeSeq<Content, E>> =
        Result.success(SerializeSeq(len))

    override fun serializeTuple(len: Int): Result<SerializeTuple<Content, E>> = Result.success(SerializeTuple(len))

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): Result<SerializeTupleStruct<Content, E>> = Result.success(SerializeTupleStruct(name, len))

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<Content, E>> = Result.success(SerializeTupleVariant(name, variantIndex, variant, len))

    override fun serializeMap(len: Int?): Result<SerializeMap<Content, E>> = Result.success(SerializeMap(len))

    override fun serializeStruct(
        name: String,
        len: Int,
    ): Result<SerializeStruct<Content, E>> = Result.success(SerializeStruct(name, len))

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<Content, E>> = Result.success(SerializeStructVariant(name, variantIndex, variant, len))
}

private class SerializeSeq<E>(
    len: Int?,
) : io.github.kotlinmania.serde.core.ser.SerializeSeq<Content, E>
    where E : Error {
    private val elements: MutableList<Content> = ArrayList(len ?: 0)

    override fun <T> serializeElement(value: T): Result<Unit>
        where T : Serialize =
        runCatching {
            elements.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): Result<Content> = Result.success(Content.Seq(elements))
}

private class SerializeTuple<E>(
    len: Int,
) : SerializeTuple<Content, E>
    where E : Error {
    private val elements: MutableList<Content> = ArrayList(len)

    override fun <T> serializeElement(value: T): Result<Unit>
        where T : Serialize =
        runCatching {
            elements.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): Result<Content> = Result.success(Content.Tuple(elements))
}

private class SerializeTupleStruct<E>(
    private val name: String,
    len: Int,
) : SerializeTupleStruct<Content, E>
    where E : Error {
    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): Result<Unit>
        where T : Serialize =
        runCatching {
            fields.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): Result<Content> = Result.success(Content.TupleStruct(name, fields))
}

private class SerializeTupleVariant<E>(
    private val name: String,
    private val variantIndex: UInt,
    private val variant: String,
    len: Int,
) : SerializeTupleVariant<Content, E>
    where E : Error {
    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): Result<Unit>
        where T : Serialize =
        runCatching {
            fields.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): Result<Content> = Result.success(Content.TupleVariant(name, variantIndex, variant, fields))
}

private class SerializeMap<E>(
    len: Int?,
) : SerializeMap<Content, E>
    where E : Error {
    private val entries: MutableList<Pair<Content, Content>> = ArrayList(len ?: 0)
    private var key: Content? = null

    override fun <T> serializeKey(key: T): Result<Unit>
        where T : Serialize =
        runCatching {
            this.key = key.serialize(ContentSerializer<E>()).getOrThrow()
            Unit
        }

    override fun <T> serializeValue(value: T): Result<Unit>
        where T : Serialize =
        runCatching {
            val storedKey = checkNotNull(key) { "serializeValue called before serializeKey" }
            key = null
            val contentValue = value.serialize(ContentSerializer<E>()).getOrThrow()
            entries.add(storedKey to contentValue)
            Unit
        }

    override fun <K, V> serializeEntry(
        key: K,
        value: V,
    ): Result<Unit>
        where K : Serialize,
              V : Serialize =
        runCatching {
            val contentKey = key.serialize(ContentSerializer<E>()).getOrThrow()
            val contentValue = value.serialize(ContentSerializer<E>()).getOrThrow()
            entries.add(contentKey to contentValue)
            Unit
        }

    override fun end(): Result<Content> = Result.success(Content.Map(entries))
}

private class SerializeStruct<E>(
    private val name: String,
    len: Int,
) : SerializeStruct<Content, E>
    where E : Error {
    private val fields: MutableList<Pair<String, Content>> = ArrayList(len)

    override fun <T> serializeField(
        key: String,
        value: T,
    ): Result<Unit>
        where T : Serialize =
        runCatching {
            fields.add(key to value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): Result<Content> = Result.success(Content.Struct(name, fields))
}

private class SerializeStructVariant<E>(
    private val name: String,
    private val variantIndex: UInt,
    private val variant: String,
    len: Int,
) : SerializeStructVariant<Content, E>
    where E : Error {
    private val fields: MutableList<Pair<String, Content>> = ArrayList(len)

    override fun <T> serializeField(
        key: String,
        value: T,
    ): Result<Unit>
        where T : Serialize =
        runCatching {
            fields.add(key to value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): Result<Content> = Result.success(Content.StructVariant(name, variantIndex, variant, fields))
}

private class SerializeTupleVariantAsMapValue<Ok, E, M>(
    private val map: M,
    private val name: String,
    len: Int,
) : SerializeTupleVariant<Ok, E>
    where E : Error,
          M : SerializeMap<Ok, E> {
    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): Result<Unit>
        where T : Serialize =
        runCatching {
            fields.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): Result<Ok> =
        runCatching {
            map.serializeValue(Content.TupleStruct(name, fields)).getOrThrow()
            map.end().getOrThrow()
        }
}

private class SerializeStructVariantAsMapValue<Ok, E, M>(
    private val map: M,
    private val name: String,
    len: Int,
) : SerializeStructVariant<Ok, E>
    where E : Error,
          M : SerializeMap<Ok, E> {
    private val fields: MutableList<Pair<String, Content>> = ArrayList(len)

    override fun <T> serializeField(
        key: String,
        value: T,
    ): Result<Unit>
        where T : Serialize =
        runCatching {
            fields.add(key to value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): Result<Ok> =
        runCatching {
            map.serializeValue(Content.Struct(name, fields)).getOrThrow()
            map.end().getOrThrow()
        }
}

class FlatMapSerializer<MOk, E, M>(
    private val map: M,
) : Serializer<Unit, E>
    where E : Error,
          M : SerializeMap<MOk, E> {
    private fun badType(what: Unsupported): Throwable = Error.custom("can only flatten structs and maps (got $what)")

    override fun serializeBool(v: Boolean): Result<Unit> {
        return Result.failure(badType(Unsupported.Boolean))
    }

    override fun serializeI8(v: Byte): Result<Unit> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeI16(v: Short): Result<Unit> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeI32(v: Int): Result<Unit> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeI64(v: Long): Result<Unit> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeU8(v: UByte): Result<Unit> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeU16(v: UShort): Result<Unit> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeU32(v: UInt): Result<Unit> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeU64(v: ULong): Result<Unit> {
        return Result.failure(badType(Unsupported.Integer))
    }

    override fun serializeF32(v: Float): Result<Unit> {
        return Result.failure(badType(Unsupported.Float))
    }

    override fun serializeF64(v: Double): Result<Unit> {
        return Result.failure(badType(Unsupported.Float))
    }

    override fun serializeChar(v: Char): Result<Unit> {
        return Result.failure(badType(Unsupported.Char))
    }

    override fun serializeStr(v: String): Result<Unit> {
        return Result.failure(badType(Unsupported.String))
    }

    override fun serializeBytes(v: ByteArray): Result<Unit> {
        return Result.failure(badType(Unsupported.ByteArray))
    }

    override fun serializeNone(): Result<Unit> = Result.success(Unit)

    override fun <T> serializeSome(value: T): Result<Unit>
        where T : Serialize = value.serialize(this)

    override fun serializeUnit(): Result<Unit> = Result.success(Unit)

    override fun serializeUnitStruct(name: String): Result<Unit> {
        return Result.success(Unit)
    }

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): Result<Unit> =
        runCatching {
            map.serializeEntry(Content.String(variant), Content.Unit).getOrThrow()
            Unit
        }

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): Result<Unit>
        where T : Serialize {
        return value.serialize(this)
    }

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): Result<Unit>
        where T : Serialize =
        runCatching {
            map.serializeEntry(Content.String(variant), value).getOrThrow()
            Unit
        }

    override fun serializeSeq(len: Int?): Result<io.github.kotlinmania.serde.core.ser.SerializeSeq<Unit, E>> {
        return Result.failure(badType(Unsupported.Sequence))
    }

    override fun serializeTuple(len: Int): Result<SerializeTuple<Unit, E>> {
        return Result.failure(badType(Unsupported.Tuple))
    }

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): Result<SerializeTupleStruct<Unit, E>> {
        return Result.failure(badType(Unsupported.TupleStruct))
    }

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<Unit, E>> =
        runCatching {
            map.serializeKey(Content.String(variant)).getOrThrow()
            FlatMapSerializeTupleVariantAsMapValue(map = map, len = len)
        }

    override fun serializeMap(len: Int?): Result<SerializeMap<Unit, E>> =
        runCatching {
            FlatMapSerializeMap(map)
        }

    override fun serializeStruct(
        name: String,
        len: Int,
    ): Result<SerializeStruct<Unit, E>> =
        runCatching {
            FlatMapSerializeStruct(map)
        }

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<Unit, E>> =
        runCatching {
            map.serializeKey(Content.String(variant)).getOrThrow()
            FlatMapSerializeStructVariantAsMapValue(map = map, name = variant)
        }
}

private class FlatMapSerializeMap<MOk, E, M>(
    private val map: M,
) : SerializeMap<Unit, E>
    where E : Error,
          M : SerializeMap<MOk, E> {
    override fun <T> serializeKey(key: T): Result<Unit>
        where T : Serialize = map.serializeKey(key)

    override fun <T> serializeValue(value: T): Result<Unit>
        where T : Serialize = map.serializeValue(value)

    override fun <K, V> serializeEntry(
        key: K,
        value: V,
    ): Result<Unit>
        where K : Serialize,
              V : Serialize = map.serializeEntry(key, value)

    override fun end(): Result<Unit> = Result.success(Unit)
}

private class FlatMapSerializeStruct<MOk, E, M>(
    private val map: M,
) : SerializeStruct<Unit, E>
    where E : Error,
          M : SerializeMap<MOk, E> {
    override fun <T> serializeField(
        key: String,
        value: T,
    ): Result<Unit>
        where T : Serialize = map.serializeEntry(Content.String(key), value)

    override fun end(): Result<Unit> = Result.success(Unit)
}

private class FlatMapSerializeTupleVariantAsMapValue<MOk, E, M>(
    private val map: M,
    len: Int,
) : SerializeTupleVariant<Unit, E>
    where E : Error,
          M : SerializeMap<MOk, E> {
    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): Result<Unit>
        where T : Serialize =
        runCatching {
            fields.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): Result<Unit> =
        runCatching {
            map.serializeValue(Content.Seq(fields)).getOrThrow()
            Unit
        }
}

private class FlatMapSerializeStructVariantAsMapValue<MOk, E, M>(
    private val map: M,
    private val name: String,
) : SerializeStructVariant<Unit, E>
    where E : Error,
          M : SerializeMap<MOk, E> {
    private val fields: MutableList<Pair<String, Content>> = ArrayList()

    override fun <T> serializeField(
        key: String,
        value: T,
    ): Result<Unit>
        where T : Serialize =
        runCatching {
            fields.add(key to value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): Result<Unit> =
        runCatching {
            map.serializeValue(Content.Struct(name, fields)).getOrThrow()
            Unit
        }
}

data class AdjacentlyTaggedEnumVariant(
    val enumName: String,
    val variantIndex: UInt,
    val variantName: String,
) : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error =
        serializer.serializeUnitVariant(enumName, variantIndex, variantName)
}

// Error when Serialize for a nonExhaustive remote enum encounters a variant
// that is not recognized.
data class CannotSerializeVariant<T>(
    val value: T,
) {
    override fun toString(): String = "enum variant cannot be serialized: $value"
}
