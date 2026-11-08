
// port-lint: source ser.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeMap
import io.github.kotlinmania.serdecore.ser.SerializeStruct
import io.github.kotlinmania.serdecore.ser.SerializeStructVariant
import io.github.kotlinmania.serdecore.ser.SerializeTuple
import io.github.kotlinmania.serdecore.ser.SerializeTupleStruct
import io.github.kotlinmania.serdecore.ser.SerializeTupleVariant
import io.github.kotlinmania.serdecore.ser.Serializer

/**
 * Used to check that serde(getter) attributes return the expected type.
 * Not public API.
 */
internal fun <T> constrain(t: T): T = t

/**
 * Not public API.
 */
internal fun <Ok, S, T> serializeTaggedNewtype(
    serializer: S,
    typeIdent: String,
    variantIdent: String,
    tag: String,
    variantName: String,
    value: T,
): SerdeResult<Ok>
    where S : Serializer<Ok>,
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

private class TaggedSerializer<Ok, S>(
    private val typeIdent: String,
    private val variantIdent: String,
    private val tag: String,
    private val variantName: String,
    private val delegate: S,
) : Serializer<Ok>
    where S : Serializer<Ok> {
    private fun badType(what: Unsupported): SerdeError =
        SerdeError.custom(
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
        delegate.serializeMap(1).flatMap { map ->
            map.serializeEntry(Content.String(tag), Content.String(variantName)).flatMap {
                map.end()
            }
        }

    override fun serializeUnitStruct(name: String): SerdeResult<Ok> =
        delegate.serializeMap(1).flatMap { map ->
            map.serializeEntry(Content.String(tag), Content.String(variantName)).flatMap {
                map.end()
            }
        }

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Ok> =
        delegate.serializeMap(2).flatMap { map ->
            map.serializeEntry(Content.String(tag), Content.String(variantName)).flatMap {
                map.serializeEntry(Content.String(variant), Content.Unit).flatMap {
                    map.end()
                }
            }
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
        delegate.serializeMap(2).flatMap { map ->
            map.serializeEntry(Content.String(tag), Content.String(variantName)).flatMap {
                map.serializeEntry(Content.String(variant), value).flatMap {
                    map.end()
                }
            }
        }

    override fun serializeSeq(len: Int?): SerdeResult<io.github.kotlinmania.serdecore.ser.SerializeSeq<Ok>> =
        SerdeResult.failure(badType(Unsupported.Sequence))

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Ok>> = SerdeResult.failure(badType(Unsupported.Tuple))

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Ok>> = SerdeResult.failure(badType(Unsupported.TupleStruct))

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Ok>> =
        delegate.serializeMap(2).flatMap { map ->
            map.serializeEntry(Content.String(tag), Content.String(variantName)).flatMap {
                map.serializeKey(Content.String(variant)).map {
                    SerializeTupleVariantAsMapValue(map = map, name = variant, len = len)
                }
            }
        }

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Ok>> =
        delegate.serializeMap(len?.plus(1)).flatMap { map ->
            map.serializeEntry(Content.String(tag), Content.String(variantName)).map { map }
        }

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Ok>> =
        delegate.serializeStruct(name, len + 1).flatMap { state ->
            state.serializeField(tag, Content.String(variantName)).map { state }
        }

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Ok>> =
        delegate.serializeMap(2).flatMap { map ->
            map.serializeEntry(Content.String(tag), Content.String(variantName)).flatMap {
                map.serializeKey(Content.String(variant)).map {
                    SerializeStructVariantAsMapValue(map = map, name = variant, len = len)
                }
            }
        }
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
    ;

    fun fmt(): String =
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
        }

    override fun toString(): String = fmt()
}

internal sealed interface Content : Serialize {
    data class Bool(
        val value: Boolean,
    ) : Content {
        override fun hashCode(): Int = if (value) 1231 else 1237
    }

    data class U8(
        val value: UByte,
    ) : Content

    data class U16(
        val value: UShort,
    ) : Content

    data class U32(
        val value: UInt,
    ) : Content

    data class U64(
        val value: ULong,
    ) : Content

    data class I8(
        val value: Byte,
    ) : Content {
        override fun hashCode(): Int = value.toInt()
    }

    data class I16(
        val value: Short,
    ) : Content {
        override fun hashCode(): Int = value.toInt()
    }

    data class I32(
        val value: Int,
    ) : Content {
        override fun hashCode(): Int = value
    }

    data class I64(
        val value: Long,
    ) : Content {
        override fun hashCode(): Int = (value xor (value ushr 32)).toInt()
    }

    data class F32(
        val value: Float,
    ) : Content {
        override fun hashCode(): Int = value.toBits()
    }

    data class F64(
        val value: Double,
    ) : Content {
        override fun hashCode(): Int {
            val bits = value.toBits()
            return (bits xor (bits ushr 32)).toInt()
        }
    }

    data class Char(
        val value: kotlin.Char,
    ) : Content {
        override fun hashCode(): Int = value.code
    }

    data class String(
        val value: kotlin.String,
    ) : Content

    data class Bytes(
        val value: ByteArray,
    ) : Content {
        override fun equals(other: Any?): Boolean = this === other || (other is Bytes && value.contentEquals(other.value))

        override fun hashCode(): Int = value.contentHashCode()
    }

    data object None : Content

    data class Some(
        val value: Content,
    ) : Content

    data object Unit : Content

    data class UnitStruct(
        val name: kotlin.String,
    ) : Content

    data class UnitVariant(
        val name: kotlin.String,
        val variantIndex: UInt,
        val variant: kotlin.String,
    ) : Content

    data class NewtypeStruct(
        val name: kotlin.String,
        val value: Content,
    ) : Content

    data class NewtypeVariant(
        val name: kotlin.String,
        val variantIndex: UInt,
        val variant: kotlin.String,
        val value: Content,
    ) : Content

    data class Seq(
        val elements: List<Content>,
    ) : Content

    data class Tuple(
        val elements: List<Content>,
    ) : Content

    data class TupleStruct(
        val name: kotlin.String,
        val fields: List<Content>,
    ) : Content

    data class TupleVariant(
        val name: kotlin.String,
        val variantIndex: UInt,
        val variant: kotlin.String,
        val fields: List<Content>,
    ) : Content

    data class Map(
        val entries: List<Pair<Content, Content>>,
    ) : Content

    data class Struct(
        val name: kotlin.String,
        val fields: List<Pair<kotlin.String, Content>>,
    ) : Content

    data class StructVariant(
        val name: kotlin.String,
        val variantIndex: UInt,
        val variant: kotlin.String,
        val fields: List<Pair<kotlin.String, Content>>,
    ) : Content

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
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
                serializer.serializeTuple(elements.size).flatMap { tuple ->
                    for (element in elements) {
                        when (val result = tuple.serializeElement(element)) {
                            is SerdeResult.Failure -> return@flatMap result
                            is SerdeResult.Success -> {}
                        }
                    }
                    tuple.end()
                }
            is TupleStruct ->
                serializer.serializeTupleStruct(name, fields.size).flatMap { tupleStruct ->
                    for (field in fields) {
                        when (val result = tupleStruct.serializeField(field)) {
                            is SerdeResult.Failure -> return@flatMap result
                            is SerdeResult.Success -> {}
                        }
                    }
                    tupleStruct.end()
                }
            is TupleVariant ->
                serializer.serializeTupleVariant(name, variantIndex, variant, fields.size).flatMap { tupleVariant ->
                    for (field in fields) {
                        when (val result = tupleVariant.serializeField(field)) {
                            is SerdeResult.Failure -> return@flatMap result
                            is SerdeResult.Success -> {}
                        }
                    }
                    tupleVariant.end()
                }
            is Map ->
                serializer.serializeMap(entries.size).flatMap { map ->
                    for ((k, v) in entries) {
                        when (val result = map.serializeEntry(k, v)) {
                            is SerdeResult.Failure -> return@flatMap result
                            is SerdeResult.Success -> {}
                        }
                    }
                    map.end()
                }
            is Struct ->
                serializer.serializeStruct(name, fields.size).flatMap { struct ->
                    for ((k, v) in fields) {
                        when (val result = struct.serializeField(k, v)) {
                            is SerdeResult.Failure -> return@flatMap result
                            is SerdeResult.Success -> {}
                        }
                    }
                    struct.end()
                }
            is StructVariant ->
                serializer.serializeStructVariant(name, variantIndex, variant, fields.size).flatMap { structVariant ->
                    for ((k, v) in fields) {
                        when (val result = structVariant.serializeField(k, v)) {
                            is SerdeResult.Failure -> return@flatMap result
                            is SerdeResult.Success -> {}
                        }
                    }
                    structVariant.end()
                }
        }
}

private class ContentSerializer : Serializer<Content> {
    companion object {
        fun new(): ContentSerializer = ContentSerializer()
    }

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

    override fun serializeBytes(v: ByteArray): SerdeResult<Content> = SerdeResult.success(Content.Bytes(v.copyOf()))

    override fun serializeNone(): SerdeResult<Content> = SerdeResult.success(Content.None)

    override fun <T> serializeSome(value: T): SerdeResult<Content>
        where T : Serialize =
        value.serialize(this).map { Content.Some(it) }

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
        value.serialize(this).map { Content.NewtypeStruct(name, it) }

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Content>
        where T : Serialize =
        value.serialize(this).map { Content.NewtypeVariant(name, variantIndex, variant, it) }

    override fun serializeSeq(len: Int?): SerdeResult<io.github.kotlinmania.serdecore.ser.SerializeSeq<Content>> =
        SerdeResult.success(SerializeSeq(len))

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Content>> = SerdeResult.success(SerializeTuple(len))

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Content>> = SerdeResult.success(SerializeTupleStruct(name, len))

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Content>> = SerdeResult.success(SerializeTupleVariant(name, variantIndex, variant, len))

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Content>> = SerdeResult.success(SerializeMap(len))

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Content>> = SerdeResult.success(SerializeStruct(name, len))

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Content>> = SerdeResult.success(SerializeStructVariant(name, variantIndex, variant, len))
}

private class SerializeSeq(
    len: Int?,
) : io.github.kotlinmania.serdecore.ser.SerializeSeq<Content> {
    private val elements: MutableList<Content> = ArrayList(len ?: 0)

    override fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize =
        value.serialize(ContentSerializer()).map { contentValue ->
            elements += contentValue
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.Seq(elements))
}

private class SerializeTuple(
    len: Int,
) : SerializeTuple<Content> {
    private val elements: MutableList<Content> = ArrayList(len)

    override fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize =
        value.serialize(ContentSerializer()).map { contentValue ->
            elements += contentValue
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.Tuple(elements))
}

private class SerializeTupleStruct(
    private val name: String,
    len: Int,
) : SerializeTupleStruct<Content> {
    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize =
        value.serialize(ContentSerializer()).map { contentValue ->
            fields += contentValue
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.TupleStruct(name, fields))
}

private class SerializeTupleVariant(
    private val name: String,
    private val variantIndex: UInt,
    private val variant: String,
    len: Int,
) : SerializeTupleVariant<Content> {
    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize =
        value.serialize(ContentSerializer()).map { contentValue ->
            fields += contentValue
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.TupleVariant(name, variantIndex, variant, fields))
}

private class SerializeMap(
    len: Int?,
) : SerializeMap<Content> {
    private val entries: MutableList<Pair<Content, Content>> = ArrayList(len ?: 0)
    private var key: Content? = null

    override fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize =
        key.serialize(ContentSerializer()).map { contentKey ->
            this.key = contentKey
        }

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize =
        run {
            val storedKey = checkNotNull(key) { "serializeValue called before serializeKey" }
            key = null
            value.serialize(ContentSerializer()).map { contentValue ->
                entries.add(storedKey to contentValue)
                Unit
            }
        }

    override fun <K, V> serializeEntry(
        key: K,
        value: V,
    ): SerdeResult<Unit>
        where K : Serialize,
              V : Serialize =
        key.serialize(ContentSerializer()).flatMap { contentKey ->
            value.serialize(ContentSerializer()).map { contentValue ->
                entries.add(contentKey to contentValue)
                Unit
            }
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.Map(entries))
}

private class SerializeStruct(
    private val name: String,
    len: Int,
) : SerializeStruct<Content> {
    private val fields: MutableList<Pair<String, Content>> = ArrayList(len)

    override fun <T> serializeField(
        key: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize =
        value.serialize(ContentSerializer()).map { contentValue ->
            fields += key to contentValue
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.Struct(name, fields))
}

private class SerializeStructVariant(
    private val name: String,
    private val variantIndex: UInt,
    private val variant: String,
    len: Int,
) : SerializeStructVariant<Content> {
    private val fields: MutableList<Pair<String, Content>> = ArrayList(len)

    override fun <T> serializeField(
        key: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize =
        value.serialize(ContentSerializer()).map { contentValue ->
            fields += key to contentValue
        }

    override fun end(): SerdeResult<Content> = SerdeResult.success(Content.StructVariant(name, variantIndex, variant, fields))
}

private class SerializeTupleVariantAsMapValue<Ok, M>(
    private val map: M,
    private val name: String,
    len: Int,
) : SerializeTupleVariant<Ok>
    where M : SerializeMap<Ok> {
    companion object {
        fun <Ok, M : SerializeMap<Ok>> new(
            map: M,
            name: String,
            len: Int,
        ): SerializeTupleVariantAsMapValue<Ok, M> = SerializeTupleVariantAsMapValue(map, name, len)
    }

    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize =
        value.serialize(ContentSerializer()).map { contentValue ->
            fields += contentValue
        }

    override fun end(): SerdeResult<Ok> =
        map.serializeValue(Content.TupleStruct(name, fields)).flatMap {
            map.end()
        }
}

private class SerializeStructVariantAsMapValue<Ok, M>(
    private val map: M,
    private val name: String,
    len: Int,
) : SerializeStructVariant<Ok>
    where M : SerializeMap<Ok> {
    companion object {
        fun <Ok, M : SerializeMap<Ok>> new(
            map: M,
            name: String,
            len: Int,
        ): SerializeStructVariantAsMapValue<Ok, M> = SerializeStructVariantAsMapValue(map, name, len)
    }

    private val fields: MutableList<Pair<String, Content>> = ArrayList(len)

    override fun <T> serializeField(
        key: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize =
        value.serialize(ContentSerializer()).map { contentValue ->
            fields += key to contentValue
        }

    override fun end(): SerdeResult<Ok> =
        map.serializeValue(Content.Struct(name, fields)).flatMap {
            map.end()
        }
}

internal class FlatMapSerializer<MOk, M>(
    val map: M,
) : Serializer<Unit>
    where M : SerializeMap<MOk> {
    private fun badType(what: Unsupported): SerdeError = SerdeError.custom("can only flatten structs and maps (got $what)")

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
    ): SerdeResult<Unit> = map.serializeEntry(Content.String(variant), Content.Unit)

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
        where T : Serialize = map.serializeEntry(Content.String(variant), value)

    override fun serializeSeq(len: Int?): SerdeResult<io.github.kotlinmania.serdecore.ser.SerializeSeq<Unit>> =
        SerdeResult.failure(badType(Unsupported.Sequence))

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit>> = SerdeResult.failure(badType(Unsupported.Tuple))

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Unit>> = SerdeResult.failure(badType(Unsupported.TupleStruct))

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Unit>> =
        map.serializeKey(Content.String(variant)).map {
            FlatMapSerializeTupleVariantAsMapValue(map = map, len = len)
        }

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> =
        SerdeResult.success(FlatMapSerializeMap(map))

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Unit>> =
        SerdeResult.success(FlatMapSerializeStruct(map))

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Unit>> =
        map.serializeKey(Content.String(variant)).map {
            FlatMapSerializeStructVariantAsMapValue(map = map, name = variant)
        }
}

private class FlatMapSerializeMap<MOk, M>(
    private val map: M,
) : SerializeMap<Unit>
    where M : SerializeMap<MOk> {
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

private class FlatMapSerializeStruct<MOk, M>(
    private val map: M,
) : SerializeStruct<Unit>
    where M : SerializeMap<MOk> {
    override fun <T> serializeField(
        key: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize = map.serializeEntry(Content.String(key), value)

    override fun end(): SerdeResult<Unit> = SerdeResult.success(Unit)
}

private class FlatMapSerializeTupleVariantAsMapValue<MOk, M>(
    private val map: M,
    len: Int,
) : SerializeTupleVariant<Unit>
    where M : SerializeMap<MOk> {
    companion object {
        fun <MOk, M : SerializeMap<MOk>> new(map: M, len: Int = 0): FlatMapSerializeTupleVariantAsMapValue<MOk, M> =
            FlatMapSerializeTupleVariantAsMapValue(map, len)
    }

    private val fields: MutableList<Content> = ArrayList(len)

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize =
        value.serialize(ContentSerializer()).map { contentValue ->
            fields += contentValue
        }

    override fun end(): SerdeResult<Unit> = map.serializeValue(Content.Seq(fields))
}

private class FlatMapSerializeStructVariantAsMapValue<MOk, M>(
    private val map: M,
    private val name: String,
) : SerializeStructVariant<Unit>
    where M : SerializeMap<MOk> {
    companion object {
        fun <MOk, M : SerializeMap<MOk>> new(
            map: M,
            name: String,
        ): FlatMapSerializeStructVariantAsMapValue<MOk, M> = FlatMapSerializeStructVariantAsMapValue(map, name)
    }

    private val fields: MutableList<Pair<String, Content>> = ArrayList()

    override fun <T> serializeField(
        key: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize =
        value.serialize(ContentSerializer()).map { contentValue ->
            fields += key to contentValue
        }

    override fun end(): SerdeResult<Unit> = map.serializeValue(Content.Struct(name, fields))
}

internal data class AdjacentlyTaggedEnumVariant(
    val enumName: String,
    val variantIndex: UInt,
    val variantName: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeUnitVariant(enumName, variantIndex, variantName)
}

// SerdeError when Serialize for a nonExhaustive remote enum encounters a variant
// that is not recognized.
internal data class CannotSerializeVariant<T>(
    val value: T,
) {
    fun fmt(): String = "enum variant cannot be serialized: $value"

    override fun toString(): String = fmt()
}
