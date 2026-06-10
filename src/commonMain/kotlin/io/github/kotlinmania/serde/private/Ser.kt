// port-lint: source serde/src/private/ser.rs
package io.github.kotlinmania.serde.`private`

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.core.ser.*
import io.github.kotlinmania.serde.core.ser.SerializeMap
import io.github.kotlinmania.serde.core.ser.SerializeStruct
import io.github.kotlinmania.serde.core.ser.SerializeStructVariant
import io.github.kotlinmania.serde.core.ser.SerializeTuple
import io.github.kotlinmania.serde.core.ser.SerializeTupleStruct
import io.github.kotlinmania.serde.core.ser.SerializeTupleVariant
import io.github.kotlinmania.serde.serdeCatching

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
): SerdeResult<Ok>
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
    private fun badType(what: Unsupported): SerdeError =
        Error.custom(
            "cannot serialize tagged newtype variant $typeIdent::$variantIdent containing $what",
        )

    override fun serializeBool(v: Boolean): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Boolean))

    override fun serializeI8(v: Byte): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeI16(v: Short): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeI32(v: Int): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeI64(v: Long): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeU8(v: UByte): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeU16(v: UShort): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeU32(v: UInt): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeU64(v: ULong): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeF32(v: Float): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Float))

    override fun serializeF64(v: Double): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Float))

    override fun serializeChar(v: Char): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Char))

    override fun serializeStr(v: String): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.String))

    override fun serializeBytes(v: ByteArray): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.ByteArray))

    override fun serializeNone(): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.Optional))

    override fun <T> serializeSome(value: T): SerdeResult<Ok>
        where T : Serialize =
        SerdeResult.failure(badType(Unsupported.Optional))

    override fun serializeUnit(): SerdeResult<Ok> =
        serdeCatching {
            val map = delegate.serializeMap(1).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.end().getOrThrow()
        }

    override fun serializeUnitStruct(name: String): SerdeResult<Ok> =
        serdeCatching {
            val map = delegate.serializeMap(1).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.end().getOrThrow()
        }

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Ok> =
        serdeCatching {
            val map = delegate.serializeMap(2).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.serializeEntry(Content.String(variant), Content.Unit).getOrThrow()
            map.end().getOrThrow()
        }

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Ok>
        where T : Serialize = value.serialize(this)

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Ok>
        where T : Serialize =
        serdeCatching {
            val map = delegate.serializeMap(2).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.serializeEntry(Content.String(variant), value).getOrThrow()
            map.end().getOrThrow()
        }

    override fun serializeSeq(len: Int?): SerdeResult<io.github.kotlinmania.serde.core.ser.SerializeSeq<Ok, E>> =
        SerdeResult.failure(badType(Unsupported.Sequence))

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Ok, E>> = SerdeResult.failure(badType(Unsupported.Tuple))

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Ok, E>> = SerdeResult.failure(badType(Unsupported.TupleStruct))

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Ok, E>> =
        serdeCatching {
            val map = delegate.serializeMap(2).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.serializeKey(Content.String(variant)).getOrThrow()
            SerializeTupleVariantAsMapValue(map = map, name = variant, len = len)
        }

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Ok, E>> =
        serdeCatching {
            val map = delegate.serializeMap(len?.plus(1)).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map
        }

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Ok, E>> =
        serdeCatching {
            val state = delegate.serializeStruct(name, len + 1).getOrThrow()
            state.serializeField(tag, Content.String(variantName)).getOrThrow()
            state
        }

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Ok, E>> =
        serdeCatching {
            val map = delegate.serializeMap(2).getOrThrow()
            map.serializeEntry(Content.String(tag), Content.String(variantName)).getOrThrow()
            map.serializeKey(Content.String(variant)).getOrThrow()
            SerializeStructVariantAsMapValue(map = map, name = variant, len = len)
        }

    override fun collectStr(value: String): SerdeResult<Ok> = SerdeResult.failure(badType(Unsupported.String))
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

    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): SerdeResult<Ok>
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
                serdeCatching {
                    val tuple = serializer.serializeTuple(elements.size).getOrThrow()
                    for (element in elements) {
                        tuple.serializeElement(element).getOrThrow()
                    }
                    tuple.end().getOrThrow()
                }
            is TupleStruct ->
                serdeCatching {
                    val tupleStruct = serializer.serializeTupleStruct(name, fields.size).getOrThrow()
                    for (field in fields) {
                        tupleStruct.serializeField(field).getOrThrow()
                    }
                    tupleStruct.end().getOrThrow()
                }
            is TupleVariant ->
                serdeCatching {
                    val tupleVariant =
                        serializer.serializeTupleVariant(name, variantIndex, variant, fields.size).getOrThrow()
                    for (field in fields) {
                        tupleVariant.serializeField(field).getOrThrow()
                    }
                    tupleVariant.end().getOrThrow()
                }
            is Map ->
                serdeCatching {
                    val map = serializer.serializeMap(entries.size).getOrThrow()
                    for ((k, v) in entries) {
                        map.serializeEntry(k, v).getOrThrow()
                    }
                    map.end().getOrThrow()
                }
            is Struct ->
                serdeCatching {
                    val struct = serializer.serializeStruct(name, fields.size).getOrThrow()
                    for ((k, v) in fields) {
                        struct.serializeField(k, v).getOrThrow()
                    }
                    struct.end().getOrThrow()
                }
            is StructVariant ->
                serdeCatching {
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
    override fun serializeBool(v: Boolean): SerdeResult<Content> = SerdeResult.success(Content.Bool(v))

    override fun serializeI8(v: Byte): SerdeResult<Content> = SerdeResult.success(Content.I8(v))

    override fun serializeI16(v: Short): SerdeResult<Content> = SerdeResult.success(Content.I16(v))

    override fun serializeI32(v: Int): SerdeResult<Content> = SerdeResult.success(Content.I32(v))

    override fun serializeI64(v: Long): SerdeResult<Content> = SerdeResult.success(Content.I64(v))

    override fun serializeU8(v: UByte): SerdeResult<Content> = SerdeResult.success(Content.U8(v))

    override fun serializeU16(v: UShort): SerdeResult<Content> = SerdeResult.success(Content.U16(v))

    override fun serializeU32(v: UInt): SerdeResult<Content> = SerdeResult.success(Content.U32(v))

    override fun serializeU64(v: ULong): SerdeResult<Content> = SerdeResult.success(Content.U64(v))

    override fun serializeF32(v: Float): SerdeResult<Content> = SerdeResult.success(Content.F32(v))

    override fun serializeF64(v: Double): SerdeResult<Content> = SerdeResult.success(Content.F64(v))

    override fun serializeChar(v: Char): SerdeResult<Content> = SerdeResult.success(Content.Char(v))

    override fun serializeStr(v: String): SerdeResult<Content> = SerdeResult.success(Content.String(v))

    override fun serializeBytes(v: ByteArray): SerdeResult<Content> = SerdeResult.success(Content.Bytes(v))

    override fun serializeNone(): SerdeResult<Content> = SerdeResult.success(Content.None)

    override fun <T> serializeSome(value: T): SerdeResult<Content>
        where T : Serialize =
        serdeCatching { Content.Some(value.serialize(ContentSerializer<E>()).getOrThrow()) }

    override fun serializeUnit(): SerdeResult<Content> = SerdeResult.success(Content.Unit)

    override fun serializeUnitStruct(name: String): SerdeResult<Content> = SerdeResult.success(Content.UnitStruct(name))

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Content> = SerdeResult.success(Content.UnitVariant(name, variantIndex, variant))

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Content>
        where T : Serialize =
        serdeCatching { Content.NewtypeStruct(name, value.serialize(ContentSerializer<E>()).getOrThrow()) }

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Content>
        where T : Serialize =
        serdeCatching {
            Content.NewtypeVariant(name, variantIndex, variant, value.serialize(ContentSerializer<E>()).getOrThrow())
        }

    override fun serializeSeq(len: Int?): SerdeResult<io.github.kotlinmania.serde.core.ser.SerializeSeq<Content, E>> =
        SerdeResult.success(SerializeSeq(len))

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Content, E>> = SerdeResult.success(SerializeTuple(len))

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Content, E>> = SerdeResult.success(SerializeTupleStruct(name, len))

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Content, E>> = SerdeResult.success(SerializeTupleVariant(name, variantIndex, variant, len))

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Content, E>> = SerdeResult.success(SerializeMap(len))

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Content, E>> = SerdeResult.success(SerializeStruct(name, len))

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Content, E>> = SerdeResult.success(SerializeStructVariant(name, variantIndex, variant, len))
}

private class SerializeSeq<E>(
    len: Int?,
) : io.github.kotlinmania.serde.core.ser.SerializeSeq<Content, E>
    where E : Error {
    private val elements: MutableList<Content> = ArrayList(len ?: 0)

    override fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            elements.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.Seq(elements))
}

private class SerializeTuple<E>(
    len: Int,
) : SerializeTuple<Content, E>
    where E : Error {
    private val elements: MutableList<Content> = ArrayList(len)

    override fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            elements.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.Tuple(elements))
}

private class SerializeTupleStruct<E>(
    private val name: String,
    len: Int,
) : SerializeTupleStruct<Content, E>
    where E : Error {
    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            fields.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.TupleStruct(name, fields))
}

private class SerializeTupleVariant<E>(
    private val name: String,
    private val variantIndex: UInt,
    private val variant: String,
    len: Int,
) : SerializeTupleVariant<Content, E>
    where E : Error {
    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            fields.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.TupleVariant(name, variantIndex, variant, fields))
}

private class SerializeMap<E>(
    len: Int?,
) : SerializeMap<Content, E>
    where E : Error {
    private val entries: MutableList<Pair<Content, Content>> = ArrayList(len ?: 0)
    private var key: Content? = null

    override fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            this.key = key.serialize(ContentSerializer<E>()).getOrThrow()
            Unit
        }

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            val storedKey = checkNotNull(key) { "serializeValue called before serializeKey" }
            key = null
            val contentValue = value.serialize(ContentSerializer<E>()).getOrThrow()
            entries.add(storedKey to contentValue)
            Unit
        }

    override fun <K, V> serializeEntry(
        key: K,
        value: V,
    ): SerdeResult<Unit>
        where K : Serialize,
              V : Serialize =
        serdeCatching {
            val contentKey = key.serialize(ContentSerializer<E>()).getOrThrow()
            val contentValue = value.serialize(ContentSerializer<E>()).getOrThrow()
            entries.add(contentKey to contentValue)
            Unit
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.Map(entries))
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
    ): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            fields.add(key to value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.Struct(name, fields))
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
    ): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            fields.add(key to value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.StructVariant(name, variantIndex, variant, fields))
}

private class SerializeTupleVariantAsMapValue<Ok, E, M>(
    private val map: M,
    private val name: String,
    len: Int,
) : SerializeTupleVariant<Ok, E>
    where E : Error,
          M : SerializeMap<Ok, E> {
    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            fields.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): SerdeResult<Ok> =
        serdeCatching {
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
    ): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            fields.add(key to value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): SerdeResult<Ok> =
        serdeCatching {
            map.serializeValue(Content.Struct(name, fields)).getOrThrow()
            map.end().getOrThrow()
        }
}

class FlatMapSerializer<MOk, E, M>(
    private val map: M,
) : Serializer<Unit, E>
    where E : Error,
          M : SerializeMap<MOk, E> {
    private fun badType(what: Unsupported): SerdeError = Error.custom("can only flatten structs and maps (got $what)")

    override fun serializeBool(v: Boolean): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Boolean))

    override fun serializeI8(v: Byte): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeI16(v: Short): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeI32(v: Int): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeI64(v: Long): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeU8(v: UByte): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeU16(v: UShort): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeU32(v: UInt): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeU64(v: ULong): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Integer))

    override fun serializeF32(v: Float): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Float))

    override fun serializeF64(v: Double): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Float))

    override fun serializeChar(v: Char): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.Char))

    override fun serializeStr(v: String): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.String))

    override fun serializeBytes(v: ByteArray): SerdeResult<Unit> = SerdeResult.failure(badType(Unsupported.ByteArray))

    override fun serializeNone(): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun <T> serializeSome(value: T): SerdeResult<Unit>
        where T : Serialize = value.serialize(this)

    override fun serializeUnit(): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeUnitStruct(name: String): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Unit> =
        serdeCatching {
            map.serializeEntry(Content.String(variant), Content.Unit).getOrThrow()
            Unit
        }

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize = value.serialize(this)

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            map.serializeEntry(Content.String(variant), value).getOrThrow()
            Unit
        }

    override fun serializeSeq(len: Int?): SerdeResult<io.github.kotlinmania.serde.core.ser.SerializeSeq<Unit, E>> =
        SerdeResult.failure(badType(Unsupported.Sequence))

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit, E>> = SerdeResult.failure(badType(Unsupported.Tuple))

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Unit, E>> = SerdeResult.failure(badType(Unsupported.TupleStruct))

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Unit, E>> =
        serdeCatching {
            map.serializeKey(Content.String(variant)).getOrThrow()
            FlatMapSerializeTupleVariantAsMapValue(map = map, len = len)
        }

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit, E>> =
        serdeCatching {
            FlatMapSerializeMap(map)
        }

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Unit, E>> =
        serdeCatching {
            FlatMapSerializeStruct(map)
        }

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Unit, E>> =
        serdeCatching {
            map.serializeKey(Content.String(variant)).getOrThrow()
            FlatMapSerializeStructVariantAsMapValue(map = map, name = variant)
        }
}

private class FlatMapSerializeMap<MOk, E, M>(
    private val map: M,
) : SerializeMap<Unit, E>
    where E : Error,
          M : SerializeMap<MOk, E> {
    override fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize = map.serializeKey(key)

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize = map.serializeValue(value)

    override fun <K, V> serializeEntry(
        key: K,
        value: V,
    ): SerdeResult<Unit>
        where K : Serialize,
              V : Serialize = map.serializeEntry(key, value)

    override fun end(): SerdeResult<Unit> = SerdeResult.success(Unit)
}

private class FlatMapSerializeStruct<MOk, E, M>(
    private val map: M,
) : SerializeStruct<Unit, E>
    where E : Error,
          M : SerializeMap<MOk, E> {
    override fun <T> serializeField(
        key: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize = map.serializeEntry(Content.String(key), value)

    override fun end(): SerdeResult<Unit> = SerdeResult.success(Unit)
}

private class FlatMapSerializeTupleVariantAsMapValue<MOk, E, M>(
    private val map: M,
    len: Int,
) : SerializeTupleVariant<Unit, E>
    where E : Error,
          M : SerializeMap<MOk, E> {
    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            fields.add(value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): SerdeResult<Unit> =
        serdeCatching {
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
    ): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            fields.add(key to value.serialize(ContentSerializer<E>()).getOrThrow())
            Unit
        }

    override fun end(): SerdeResult<Unit> =
        serdeCatching {
            map.serializeValue(Content.Struct(name, fields)).getOrThrow()
            Unit
        }
}

data class AdjacentlyTaggedEnumVariant(
    val enumName: String,
    val variantIndex: UInt,
    val variantName: String,
) : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): SerdeResult<Ok>
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
