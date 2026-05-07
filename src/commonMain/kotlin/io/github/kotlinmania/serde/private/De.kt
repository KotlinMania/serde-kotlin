// port-lint: source serde/src/private/de.rs
package io.github.kotlinmania.serde.`private`

import io.github.kotlinmania.serde.core.`private`.Content
import io.github.kotlinmania.serde.core.`private`.cautious
import io.github.kotlinmania.serde.core.de.Deserialize
import io.github.kotlinmania.serde.core.de.DeserializeSeed
import io.github.kotlinmania.serde.core.de.Deserializer
import io.github.kotlinmania.serde.core.de.EnumAccess
import io.github.kotlinmania.serde.core.de.Error
import io.github.kotlinmania.serde.core.de.Expected
import io.github.kotlinmania.serde.core.de.IgnoredAny
import io.github.kotlinmania.serde.core.de.MapAccess
import io.github.kotlinmania.serde.core.de.SeqAccess
import io.github.kotlinmania.serde.core.de.SerdeDeserializationException
import io.github.kotlinmania.serde.core.de.Unexpected
import io.github.kotlinmania.serde.core.de.VariantAccess
import io.github.kotlinmania.serde.core.de.Visitor
import io.github.kotlinmania.serde.core.de.value.BytesDeserializer
import io.github.kotlinmania.serde.core.de.value.SeqAccessDeserializer
import io.github.kotlinmania.serde.core.de.value.intoDeserializer

/**
 * If the missing field is of type `T?` then treat is as `null`, otherwise it is an error.
 */
public fun <V> missingField(field: String, deserialize: Deserialize<V>): Result<V> {
    class MissingFieldDeserializer(private val name: String) : Deserializer {
        override fun <T> deserializeAny(visitor: Visitor<T>): Result<T> {
            visitor.hashCode()
            return Result.failure(Error.missingField(name))
        }

        override fun <T> deserializeBool(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeI8(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeI16(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeI32(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeI64(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeU8(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeU16(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeU32(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeU64(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeF32(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeF64(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeChar(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeStr(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeString(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeBytes(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeByteBuf(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)

        override fun <T> deserializeOption(visitor: Visitor<T>): Result<T> =
            visitor.visitNone()

        override fun <T> deserializeUnit(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeUnitStruct(name: String, visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeNewtypeStruct(name: String, visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeSeq(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeTuple(len: Int, visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeMap(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeIdentifier(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
        override fun <T> deserializeIgnoredAny(visitor: Visitor<T>): Result<T> = deserializeAny(visitor)
    }

    return deserialize.deserialize(MissingFieldDeserializer(field))
}

public fun borrowCowStr(deserializer: Deserializer): Result<String> {
    val cowStrVisitor =
        object : Visitor<String> {
        override fun expecting(): String = "a string"

        override fun visitStr(v: String): Result<String> = Result.success(v)
        override fun visitBorrowedStr(v: String): Result<String> = Result.success(v)
        override fun visitString(v: String): Result<String> = Result.success(v)

        override fun visitBytes(v: ByteArray): Result<String> =
            runCatching { v.decodeToString(throwOnInvalidSequence = true) }
                .recoverCatching { throw Error.invalidValue(Unexpected.Bytes(v), this) }

        override fun visitBorrowedBytes(v: ByteArray): Result<String> =
            runCatching { v.decodeToString(throwOnInvalidSequence = true) }
                .recoverCatching { throw Error.invalidValue(Unexpected.Bytes(v), this) }

        override fun visitByteBuf(v: ByteArray): Result<String> =
            runCatching { v.decodeToString(throwOnInvalidSequence = true) }
                .recoverCatching { throw Error.invalidValue(Unexpected.Bytes(v), this) }
        }

    return deserializer.deserializeStr(cowStrVisitor)
}

public fun borrowCowBytes(deserializer: Deserializer): Result<ByteArray> {
    val cowBytesVisitor =
        object : Visitor<ByteArray> {
        override fun expecting(): String = "a byte array"

        override fun visitStr(v: String): Result<ByteArray> = Result.success(v.encodeToByteArray())
        override fun visitBorrowedStr(v: String): Result<ByteArray> = Result.success(v.encodeToByteArray())
        override fun visitString(v: String): Result<ByteArray> = Result.success(v.encodeToByteArray())

        override fun visitBytes(v: ByteArray): Result<ByteArray> = Result.success(v)
        override fun visitBorrowedBytes(v: ByteArray): Result<ByteArray> = Result.success(v)
        override fun visitByteBuf(v: ByteArray): Result<ByteArray> = Result.success(v)
        }

    return deserializer.deserializeBytes(cowBytesVisitor)
}

////////////////////////////////////////////////////////////////////////////////

// This module is private and nothing here should be used outside of generated code.
//
// We will iterate on the implementation for a few releases and only have to
// worry about backward compatibility for the `untagged` and `tag` attributes
// rather than for this entire mechanism.
//
// This issue is tracking making some of this stuff public:
// https://github.com/serde-rs/serde/issues/741

public fun contentAsStr(content: Content): String? =
    when (content) {
        is Content.Str -> content.value
        is Content.String -> content.value
        is Content.Bytes ->
            runCatching { content.value.decodeToString(throwOnInvalidSequence = true) }.getOrNull()

        is Content.ByteBuf ->
            runCatching { content.value.decodeToString(throwOnInvalidSequence = true) }.getOrNull()

        else -> null
    }

private fun contentClone(content: Content): Content =
    when (content) {
        is Content.Bool -> content
        is Content.U8 -> content
        is Content.U16 -> content
        is Content.U32 -> content
        is Content.U64 -> content
        is Content.I8 -> content
        is Content.I16 -> content
        is Content.I32 -> content
        is Content.I64 -> content
        is Content.F32 -> content
        is Content.F64 -> content
        is Content.Char -> content
        is Content.String -> Content.String(content.value)
        is Content.Str -> Content.Str(content.value)
        is Content.ByteBuf -> Content.ByteBuf(content.value.copyOf())
        is Content.Bytes -> Content.Bytes(content.value)
        Content.None -> Content.None
        is Content.Some -> Content.Some(contentClone(content.value))
        Content.Unit -> Content.Unit
        is Content.Newtype -> Content.Newtype(contentClone(content.value))
        is Content.Seq -> Content.Seq(content.value.map(::contentClone))
        is Content.Map -> Content.Map(content.value.map { (k, v) -> contentClone(k) to contentClone(v) })
    }

private fun contentUnexpected(content: Content): Unexpected =
    when (content) {
        is Content.Bool -> Unexpected.Bool(content.value)
        is Content.U8 -> Unexpected.Unsigned(content.value.toULong())
        is Content.U16 -> Unexpected.Unsigned(content.value.toULong())
        is Content.U32 -> Unexpected.Unsigned(content.value.toULong())
        is Content.U64 -> Unexpected.Unsigned(content.value)
        is Content.I8 -> Unexpected.Signed(content.value.toLong())
        is Content.I16 -> Unexpected.Signed(content.value.toLong())
        is Content.I32 -> Unexpected.Signed(content.value.toLong())
        is Content.I64 -> Unexpected.Signed(content.value)
        is Content.F32 -> Unexpected.FloatValue(content.value.toDouble())
        is Content.F64 -> Unexpected.FloatValue(content.value)
        is Content.Char -> Unexpected.CharValue(content.value)
        is Content.String -> Unexpected.Str(content.value)
        is Content.Str -> Unexpected.Str(content.value)
        is Content.ByteBuf -> Unexpected.Bytes(content.value)
        is Content.Bytes -> Unexpected.Bytes(content.value)
        Content.None, is Content.Some -> Unexpected.Option
        Content.Unit -> Unexpected.UnitValue
        is Content.Newtype -> Unexpected.NewtypeStruct
        is Content.Seq -> Unexpected.Seq
        is Content.Map -> Unexpected.Map
    }

public class ContentVisitor : DeserializeSeed<Content>, Visitor<Content> {
    public companion object {
        public fun new(): ContentVisitor = ContentVisitor()
    }

    override fun expecting(): String = "any value"

    override fun <D> deserialize(deserializer: D): Result<Content>
        where D : Deserializer =
        deserializer.deserializeAny(this)

    override fun visitBool(v: Boolean): Result<Content> = Result.success(Content.Bool(v))
    override fun visitI8(v: Byte): Result<Content> = Result.success(Content.I8(v))
    override fun visitI16(v: Short): Result<Content> = Result.success(Content.I16(v))
    override fun visitI32(v: Int): Result<Content> = Result.success(Content.I32(v))
    override fun visitI64(v: Long): Result<Content> = Result.success(Content.I64(v))
    override fun visitU8(v: UByte): Result<Content> = Result.success(Content.U8(v))
    override fun visitU16(v: UShort): Result<Content> = Result.success(Content.U16(v))
    override fun visitU32(v: UInt): Result<Content> = Result.success(Content.U32(v))
    override fun visitU64(v: ULong): Result<Content> = Result.success(Content.U64(v))
    override fun visitF32(v: Float): Result<Content> = Result.success(Content.F32(v))
    override fun visitF64(v: Double): Result<Content> = Result.success(Content.F64(v))
    override fun visitChar(v: Char): Result<Content> = Result.success(Content.Char(v))
    override fun visitStr(v: String): Result<Content> = Result.success(Content.String(v))
    override fun visitBorrowedStr(v: String): Result<Content> = Result.success(Content.Str(v))
    override fun visitString(v: String): Result<Content> = Result.success(Content.String(v))
    override fun visitBytes(v: ByteArray): Result<Content> = Result.success(Content.ByteBuf(v.copyOf()))
    override fun visitBorrowedBytes(v: ByteArray): Result<Content> = Result.success(Content.Bytes(v))
    override fun visitByteBuf(v: ByteArray): Result<Content> = Result.success(Content.ByteBuf(v))
    override fun visitUnit(): Result<Content> = Result.success(Content.Unit)
    override fun visitNone(): Result<Content> = Result.success(Content.None)

    override fun <D> visitSome(deserializer: D): Result<Content>
        where D : Deserializer =
        deserialize(deserializer).map { Content.Some(it) }

    override fun <D> visitNewtypeStruct(deserializer: D): Result<Content>
        where D : Deserializer =
        deserialize(deserializer).map { Content.Newtype(it) }

    override fun <A> visitSeq(seq: A): Result<Content>
        where A : SeqAccess =
        runCatching {
            val hint = seq.sizeHint()
            val values = ArrayList<Content>(cautious<Content>(hint))
            while (true) {
                val next = seq.nextElementSeed(new()).getOrThrow() ?: break
                values.add(next)
            }
            Content.Seq(values)
        }

    override fun <A> visitMap(map: A): Result<Content>
        where A : MapAccess =
        runCatching {
            val hint = map.sizeHint()
            val values = ArrayList<Pair<Content, Content>>(cautious<Pair<Content, Content>>(hint))
            while (true) {
                val next = map.nextEntrySeed(new(), new()).getOrThrow() ?: break
                values.add(next)
            }
            Content.Map(values)
        }

    override fun <A> visitEnum(data: A): Result<Content>
        where A : EnumAccess =
        Result.failure(
            Error.custom("untagged and internally tagged enums do not support enum input"),
        )
}

/**
 * This is the type of the map keys in an internally tagged enum.
 *
 * Not public API.
 */
public sealed class TagOrContent {
    public data object Tag : TagOrContent()
    public data class ContentValue(public val value: Content) : TagOrContent()
}

/**
 * Serves as a seed for deserializing a key of internally tagged enum.
 * Cannot capture externally tagged enums, `i128` and `u128`.
 */
public class TagOrContentVisitor(
    private val name: String,
) : DeserializeSeed<TagOrContent>, Visitor<TagOrContent> {
    public companion object {
        public fun new(name: String): TagOrContentVisitor = TagOrContentVisitor(name)
    }

    override fun expecting(): String = "a type tag `$name` or any other value"

    override fun <D> deserialize(deserializer: D): Result<TagOrContent>
        where D : Deserializer =
        // Internally tagged enums are only supported in self-describing formats.
        deserializer.deserializeAny(this)

    private fun content(value: Content): Result<TagOrContent> =
        Result.success(TagOrContent.ContentValue(value))

    override fun visitBool(v: Boolean): Result<TagOrContent> = ContentVisitor.new().visitBool(v).map { TagOrContent.ContentValue(it) }
    override fun visitI8(v: Byte): Result<TagOrContent> = ContentVisitor.new().visitI8(v).map { TagOrContent.ContentValue(it) }
    override fun visitI16(v: Short): Result<TagOrContent> = ContentVisitor.new().visitI16(v).map { TagOrContent.ContentValue(it) }
    override fun visitI32(v: Int): Result<TagOrContent> = ContentVisitor.new().visitI32(v).map { TagOrContent.ContentValue(it) }
    override fun visitI64(v: Long): Result<TagOrContent> = ContentVisitor.new().visitI64(v).map { TagOrContent.ContentValue(it) }
    override fun visitU8(v: UByte): Result<TagOrContent> = ContentVisitor.new().visitU8(v).map { TagOrContent.ContentValue(it) }
    override fun visitU16(v: UShort): Result<TagOrContent> = ContentVisitor.new().visitU16(v).map { TagOrContent.ContentValue(it) }
    override fun visitU32(v: UInt): Result<TagOrContent> = ContentVisitor.new().visitU32(v).map { TagOrContent.ContentValue(it) }
    override fun visitU64(v: ULong): Result<TagOrContent> = ContentVisitor.new().visitU64(v).map { TagOrContent.ContentValue(it) }
    override fun visitF32(v: Float): Result<TagOrContent> = ContentVisitor.new().visitF32(v).map { TagOrContent.ContentValue(it) }
    override fun visitF64(v: Double): Result<TagOrContent> = ContentVisitor.new().visitF64(v).map { TagOrContent.ContentValue(it) }
    override fun visitChar(v: Char): Result<TagOrContent> = ContentVisitor.new().visitChar(v).map { TagOrContent.ContentValue(it) }

    override fun visitStr(v: String): Result<TagOrContent> =
        if (v == name) {
            Result.success(TagOrContent.Tag)
        } else {
            ContentVisitor.new().visitStr(v).map { TagOrContent.ContentValue(it) }
        }

    override fun visitBorrowedStr(v: String): Result<TagOrContent> =
        if (v == name) {
            Result.success(TagOrContent.Tag)
        } else {
            ContentVisitor.new().visitBorrowedStr(v).map { TagOrContent.ContentValue(it) }
        }

    override fun visitString(v: String): Result<TagOrContent> =
        if (v == name) {
            Result.success(TagOrContent.Tag)
        } else {
            ContentVisitor.new().visitString(v).map { TagOrContent.ContentValue(it) }
        }

    override fun visitBytes(v: ByteArray): Result<TagOrContent> =
        if (v.contentEquals(name.encodeToByteArray())) {
            Result.success(TagOrContent.Tag)
        } else {
            ContentVisitor.new().visitBytes(v).map { TagOrContent.ContentValue(it) }
        }

    override fun visitBorrowedBytes(v: ByteArray): Result<TagOrContent> =
        if (v.contentEquals(name.encodeToByteArray())) {
            Result.success(TagOrContent.Tag)
        } else {
            ContentVisitor.new().visitBorrowedBytes(v).map { TagOrContent.ContentValue(it) }
        }

    override fun visitByteBuf(v: ByteArray): Result<TagOrContent> =
        if (v.contentEquals(name.encodeToByteArray())) {
            Result.success(TagOrContent.Tag)
        } else {
            ContentVisitor.new().visitByteBuf(v).map { TagOrContent.ContentValue(it) }
        }

    override fun visitUnit(): Result<TagOrContent> = ContentVisitor.new().visitUnit().map { TagOrContent.ContentValue(it) }
    override fun visitNone(): Result<TagOrContent> = ContentVisitor.new().visitNone().map { TagOrContent.ContentValue(it) }

    override fun <D> visitSome(deserializer: D): Result<TagOrContent>
        where D : Deserializer =
        ContentVisitor.new().visitSome(deserializer).map { TagOrContent.ContentValue(it) }

    override fun <D> visitNewtypeStruct(deserializer: D): Result<TagOrContent>
        where D : Deserializer =
        ContentVisitor.new().visitNewtypeStruct(deserializer).map { TagOrContent.ContentValue(it) }

    override fun <A> visitSeq(seq: A): Result<TagOrContent>
        where A : SeqAccess =
        ContentVisitor.new().visitSeq(seq).map { TagOrContent.ContentValue(it) }

    override fun <A> visitMap(map: A): Result<TagOrContent>
        where A : MapAccess =
        ContentVisitor.new().visitMap(map).map { TagOrContent.ContentValue(it) }

    override fun <A> visitEnum(data: A): Result<TagOrContent>
        where A : EnumAccess =
        ContentVisitor.new().visitEnum(data).map { TagOrContent.ContentValue(it) }
}

/**
 * Used by generated code to deserialize an internally tagged enum.
 *
 * Captures map or sequence from the original deserializer and searches
 * a tag in it (in case of sequence, tag is the first element of sequence).
 *
 * Not public API.
 */
public class TaggedContentVisitor<T>(
    private val tagName: String,
    private val expecting: String,
    private val deserializeTag: Deserialize<T>,
) : Visitor<Pair<T, Content>>
    where T : Any {
    public companion object {
        public fun <T : Any> new(
            name: String,
            expecting: String,
            deserializeTag: Deserialize<T>,
        ): TaggedContentVisitor<T> = TaggedContentVisitor(name, expecting, deserializeTag)
    }

    override fun expecting(): String = expecting

    override fun <A> visitSeq(seq: A): Result<Pair<T, Content>>
        where A : SeqAccess =
        runCatching {
            val tag = seq.nextElementSeed(object : DeserializeSeed<T> {
                override fun <D> deserialize(deserializer: D): Result<T>
                    where D : Deserializer =
                    deserializeTag.deserialize(deserializer)
            }).getOrThrow() ?: throw Error.missingField(tagName)

            val rest = SeqAccessDeserializer.new(seq)
            val content = ContentVisitor.new().deserialize(rest).getOrThrow()
            tag to content
        }

    override fun <A> visitMap(map: A): Result<Pair<T, Content>>
        where A : MapAccess =
        runCatching {
            var tag: T? = null
            val values = ArrayList<Pair<Content, Content>>(cautious<Pair<Content, Content>>(map.sizeHint()))
            while (true) {
                val key = map.nextKeySeed(TagOrContentVisitor.new(tagName)).getOrThrow() ?: break
                when (key) {
                    TagOrContent.Tag -> {
                        if (tag != null) {
                            throw Error.duplicateField(tagName)
                        }
                        tag = map.nextValueSeed(object : DeserializeSeed<T> {
                            override fun <D> deserialize(deserializer: D): Result<T>
                                where D : Deserializer =
                                deserializeTag.deserialize(deserializer)
                        }).getOrThrow()
                    }

                    is TagOrContent.ContentValue -> {
                        val value = map.nextValueSeed(ContentVisitor.new()).getOrThrow()
                        values.add(key.value to value)
                    }
                }
            }

            when (val finalTag = tag) {
                null -> throw Error.missingField(tagName)
                else -> finalTag to Content.Map(values)
            }
        }
}

/**
 * Used by generated code to deserialize an adjacently tagged enum.
 *
 * Not public API.
 */
public enum class TagOrContentField {
    Tag,
    Content,
}

/**
 * Not public API.
 */
public class TagOrContentFieldVisitor(
    /**
     * Name of the tag field of the adjacently tagged enum
     */
    public val tag: String,
    /**
     * Name of the content field of the adjacently tagged enum
     */
    public val content: String,
) : DeserializeSeed<TagOrContentField>, Visitor<TagOrContentField> {
    override fun expecting(): String = "\"$tag\" or \"$content\""

    override fun <D> deserialize(deserializer: D): Result<TagOrContentField>
        where D : Deserializer =
        deserializer.deserializeIdentifier(this)

    override fun visitU64(v: ULong): Result<TagOrContentField> =
        when (v.toLong()) {
            0L -> Result.success(TagOrContentField.Tag)
            1L -> Result.success(TagOrContentField.Content)
            else -> Result.failure(Error.invalidValue(Unexpected.Unsigned(v), this))
        }

    override fun visitStr(v: String): Result<TagOrContentField> =
        when (v) {
            tag -> Result.success(TagOrContentField.Tag)
            content -> Result.success(TagOrContentField.Content)
            else -> Result.failure(Error.invalidValue(Unexpected.Str(v), this))
        }

    override fun visitBytes(v: ByteArray): Result<TagOrContentField> =
        when {
            v.contentEquals(tag.encodeToByteArray()) -> Result.success(TagOrContentField.Tag)
            v.contentEquals(content.encodeToByteArray()) -> Result.success(TagOrContentField.Content)
            else -> Result.failure(Error.invalidValue(Unexpected.Bytes(v), this))
        }
}

/**
 * Used by generated code to deserialize an adjacently tagged enum when
 * ignoring unrelated fields is allowed.
 *
 * Not public API.
 */
public enum class TagContentOtherField {
    Tag,
    Content,
    Other,
}

/**
 * Not public API.
 */
public class TagContentOtherFieldVisitor(
    /**
     * Name of the tag field of the adjacently tagged enum
     */
    public val tag: String,
    /**
     * Name of the content field of the adjacently tagged enum
     */
    public val content: String,
) : DeserializeSeed<TagContentOtherField>, Visitor<TagContentOtherField> {
    override fun expecting(): String = "\"$tag\", \"$content\", or other ignored fields"

    override fun <D> deserialize(deserializer: D): Result<TagContentOtherField>
        where D : Deserializer =
        deserializer.deserializeIdentifier(this)

    override fun visitU64(v: ULong): Result<TagContentOtherField> =
        when (v.toLong()) {
            0L -> Result.success(TagContentOtherField.Tag)
            1L -> Result.success(TagContentOtherField.Content)
            else -> Result.success(TagContentOtherField.Other)
        }

    override fun visitStr(v: String): Result<TagContentOtherField> =
        visitBytes(v.encodeToByteArray())

    override fun visitBytes(v: ByteArray): Result<TagContentOtherField> =
        when {
            v.contentEquals(tag.encodeToByteArray()) -> Result.success(TagContentOtherField.Tag)
            v.contentEquals(content.encodeToByteArray()) -> Result.success(TagContentOtherField.Content)
            else -> Result.success(TagContentOtherField.Other)
        }
}

////////////////////////////////////////////////////////////////////////////////

public class ContentDeserializer(
    private val content: Content,
) : Deserializer {
    public companion object {
        // Private API, don't use.
        public fun new(content: Content): ContentDeserializer = ContentDeserializer(content)
    }

    private fun invalidType(exp: Expected): Throwable =
        Error.invalidType(contentUnexpected(content), exp)

    private fun <V> deserializeInteger(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.U8 -> visitor.visitU8(content.value)
            is Content.U16 -> visitor.visitU16(content.value)
            is Content.U32 -> visitor.visitU32(content.value)
            is Content.U64 -> visitor.visitU64(content.value)
            is Content.I8 -> visitor.visitI8(content.value)
            is Content.I16 -> visitor.visitI16(content.value)
            is Content.I32 -> visitor.visitI32(content.value)
            is Content.I64 -> visitor.visitI64(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    private fun <V> deserializeFloat(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.F32 -> visitor.visitF32(content.value)
            is Content.F64 -> visitor.visitF64(content.value)
            is Content.U8 -> visitor.visitU8(content.value)
            is Content.U16 -> visitor.visitU16(content.value)
            is Content.U32 -> visitor.visitU32(content.value)
            is Content.U64 -> visitor.visitU64(content.value)
            is Content.I8 -> visitor.visitI8(content.value)
            is Content.I16 -> visitor.visitI16(content.value)
            is Content.I32 -> visitor.visitI32(content.value)
            is Content.I64 -> visitor.visitI64(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Bool -> visitor.visitBool(content.value)
            is Content.U8 -> visitor.visitU8(content.value)
            is Content.U16 -> visitor.visitU16(content.value)
            is Content.U32 -> visitor.visitU32(content.value)
            is Content.U64 -> visitor.visitU64(content.value)
            is Content.I8 -> visitor.visitI8(content.value)
            is Content.I16 -> visitor.visitI16(content.value)
            is Content.I32 -> visitor.visitI32(content.value)
            is Content.I64 -> visitor.visitI64(content.value)
            is Content.F32 -> visitor.visitF32(content.value)
            is Content.F64 -> visitor.visitF64(content.value)
            is Content.Char -> visitor.visitChar(content.value)
            is Content.String -> visitor.visitString(content.value)
            is Content.Str -> visitor.visitBorrowedStr(content.value)
            is Content.ByteBuf -> visitor.visitByteBuf(content.value)
            is Content.Bytes -> visitor.visitBorrowedBytes(content.value)
            Content.Unit -> visitor.visitUnit()
            Content.None -> visitor.visitNone()
            is Content.Some -> visitor.visitSome(new(content.value))
            is Content.Newtype -> visitor.visitNewtypeStruct(new(content.value))
            is Content.Seq -> visitContentSeq(content.value, visitor)
            is Content.Map -> visitContentMap(content.value, visitor)
        }

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Bool -> visitor.visitBool(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeFloat(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeFloat(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Char -> visitor.visitChar(content.value)
            is Content.String -> visitor.visitString(content.value)
            is Content.Str -> visitor.visitBorrowedStr(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeString(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.String -> visitor.visitString(content.value)
            is Content.Str -> visitor.visitBorrowedStr(content.value)
            is Content.ByteBuf -> visitor.visitByteBuf(content.value)
            is Content.Bytes -> visitor.visitBorrowedBytes(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeByteBuf(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.String -> visitor.visitString(content.value)
            is Content.Str -> visitor.visitBorrowedStr(content.value)
            is Content.ByteBuf -> visitor.visitByteBuf(content.value)
            is Content.Bytes -> visitor.visitBorrowedBytes(content.value)
            is Content.Seq -> visitContentSeq(content.value, visitor)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> =
        when (content) {
            Content.None -> visitor.visitNone()
            is Content.Some -> visitor.visitSome(new(content.value))
            Content.Unit -> visitor.visitUnit()
            // Some data formats do not encode an indication of whether a value is optional.
            //
            // An example is JSON, and a counterexample is RON. When requesting `deserializeAny` in
            // JSON, the data format never performs `Visitor.visitSome` but we still must be able
            // to deserialize the resulting content into data structures with optional fields.
            else -> visitor.visitSome(this)
        }

    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> =
        when (content) {
            Content.Unit -> visitor.visitUnit()

            // Allow deserializing a newtype enum variant containing unit.
            //
            // This supports self-describing formats in which a unit-like newtype variant may be
            // represented as an object that only contains the tag, with no content.
            //
            // We want an object like `{"result":"Success"}` to be able to deserialize into a
            // response type whose payload type is `Unit`.
            is Content.Map -> if (content.value.isEmpty()) visitor.visitUnit() else Result.failure(invalidType(visitor))
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> =
        when (content) {
            // As a special case, allow deserializing an untagged newtype variant containing a
            // unit struct.
            //
            // This supports self-describing formats in which a unit-struct-like newtype variant may
            // be represented as an empty object or empty sequence when only the tag is present.
            is Content.Map -> if (content.value.isEmpty()) visitor.visitUnit() else deserializeAny(visitor)
            is Content.Seq -> if (content.value.isEmpty()) visitor.visitUnit() else deserializeAny(visitor)
            else -> deserializeAny(visitor)
        }

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Newtype -> visitor.visitNewtypeStruct(new(content.value))

            // Some data formats encode newtype structs and their underlying data the same, with no
            // indication whether a newtype wrapper was present. For example JSON does this, while
            // RON does not. In RON a newtype's name is included in the serialized representation
            // and it knows to call `Visitor.visitNewtypeStruct` from `deserializeAny`.
            //
            // JSON's `deserializeAny` never calls `visitNewtypeStruct` but we still must be able to
            // deserialize the resulting content into newtypes.
            else -> visitor.visitNewtypeStruct(this)
        }

    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Seq -> visitContentSeq(content.value, visitor)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeSeq(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeSeq(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Map -> visitContentMap(content.value, visitor)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Seq -> visitContentSeq(content.value, visitor)
            is Content.Map -> visitContentMap(content.value, visitor)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> =
        runCatching {
            val (variant, value) =
                when (content) {
                    is Content.Map -> {
                        val iter = content.value.iterator()
                        val first = iter.nextOrNull()
                            ?: throw Error.invalidValue(Unexpected.Map, object : Expected { override fun expecting(): String = "map with a single key" })
                        if (iter.hasNext()) {
                            throw Error.invalidValue(Unexpected.Map, object : Expected { override fun expecting(): String = "map with a single key" })
                        }
                        // Enums are encoded in JSON as objects with a single key-value pair.
                        first.first to first.second
                    }

                    is Content.String -> content to null
                    is Content.Str -> content to null
                    else -> throw Error.invalidType(contentUnexpected(content), object : Expected { override fun expecting(): String = "string or map" })
                }

            visitor.visitEnum(EnumDeserializer.new(variant, value)).getOrThrow()
        }

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.String -> visitor.visitString(content.value)
            is Content.Str -> visitor.visitBorrowedStr(content.value)
            is Content.ByteBuf -> visitor.visitByteBuf(content.value)
            is Content.Bytes -> visitor.visitBorrowedBytes(content.value)
            is Content.U8 -> visitor.visitU8(content.value)
            is Content.U64 -> visitor.visitU64(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> =
        visitor.visitUnit()
}

private fun <V> visitContentSeq(content: List<Content>, visitor: Visitor<V>): Result<V> =
    runCatching {
        val seqVisitor = SeqDeserializer.new(content)
        val value = visitor.visitSeq(seqVisitor).getOrThrow()
        seqVisitor.end().getOrThrow()
        value
    }

private fun <V> visitContentMap(content: List<Pair<Content, Content>>, visitor: Visitor<V>): Result<V> =
    runCatching {
        val mapVisitor = MapDeserializer.new(content)
        val value = visitor.visitMap(mapVisitor).getOrThrow()
        mapVisitor.end().getOrThrow()
        value
    }

private class SeqDeserializer(
    private val iter: Iterator<Content>,
) : Deserializer, SeqAccess {
    private var count: Int = 0
    private var remainingHint: Int? = null

    public companion object {
        public fun new(content: List<Content>): SeqDeserializer = SeqDeserializer(content.iterator()).also {
            it.remainingHint = content.size
        }
    }

    fun end(): Result<Unit> =
        runCatching {
            val remaining = if (iter.hasNext()) iter.asSequence().count() else 0
            if (remaining == 0) {
                Unit
            } else {
                // First argument is the number of elements in the data, second argument is the
                // number of elements expected by the deserializer.
                throw Error.invalidLength(count + remaining, ExpectedInSeq(count))
            }
        }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        runCatching {
            val value = visitor.visitSeq(this).getOrThrow()
            end().getOrThrow()
            value
        }

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): Result<T?> =
        runCatching {
            if (remainingHint != null) {
                remainingHint = maxOf(0, remainingHint!! - 1)
            }
            val next = if (iter.hasNext()) iter.next() else null
            if (next == null) {
                null
            } else {
                count += 1
                seed.deserialize(ContentDeserializer.new(next)).getOrThrow()
            }
        }

    override fun sizeHint(): Int? = remainingHint
}

private class ExpectedInSeq(
    private val count: Int,
) : Expected {
    override fun expecting(): String =
        if (count == 1) {
            "1 element in sequence"
        } else {
            "$count elements in sequence"
        }
}

private class MapDeserializer(
    private val iter: Iterator<Pair<Content, Content>>,
) : Deserializer, MapAccess, SeqAccess {
    private var value: Content? = null
    private var count: Int = 0
    private var remainingHint: Int? = null

    public companion object {
        public fun new(content: List<Pair<Content, Content>>): MapDeserializer =
            MapDeserializer(content.iterator()).also { it.remainingHint = content.size }
    }

    fun end(): Result<Unit> =
        runCatching {
            val remaining = if (iter.hasNext()) iter.asSequence().count() else 0
            if (remaining == 0) {
                Unit
            } else {
                // First argument is the number of elements in the data, second argument is the
                // number of elements expected by the deserializer.
                throw Error.invalidLength(count + remaining, ExpectedInMap(count))
            }
        }

    private fun nextPair(): Pair<Content, Content>? =
        if (iter.hasNext()) {
            count += 1
            if (remainingHint != null) {
                remainingHint = maxOf(0, remainingHint!! - 1)
            }
            iter.next()
        } else {
            null
        }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        runCatching {
            val value = visitor.visitMap(this).getOrThrow()
            end().getOrThrow()
            value
        }

    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> =
        runCatching {
            val value = visitor.visitSeq(this).getOrThrow()
            end().getOrThrow()
            value
        }

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> =
        deserializeSeq(visitor)

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): Result<K?> =
        runCatching {
            val pair = nextPair()
            if (pair == null) {
                null
            } else {
                value = pair.second
                seed.deserialize(ContentDeserializer.new(pair.first)).getOrThrow()
            }
        }

    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): Result<V> =
        runCatching {
            val v = value
                // Throw because this indicates a bug in the program rather than an expected
                // failure.
                ?: throw IllegalStateException("MapAccess.nextValue called before nextKey")
            value = null
            seed.deserialize(ContentDeserializer.new(v)).getOrThrow()
        }

    override fun <K, V> nextEntrySeed(keySeed: DeserializeSeed<K>, valueSeed: DeserializeSeed<V>): Result<Pair<K, V>?> =
        runCatching {
            val pair = nextPair()
            if (pair == null) {
                null
            } else {
                val key = keySeed.deserialize(ContentDeserializer.new(pair.first)).getOrThrow()
                val value = valueSeed.deserialize(ContentDeserializer.new(pair.second)).getOrThrow()
                key to value
            }
        }

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): Result<T?> =
        runCatching {
            val pair = nextPair()
            if (pair == null) {
                null
            } else {
                seed.deserialize(PairDeserializer(pair.first, pair.second)).getOrThrow()
            }
        }

    override fun sizeHint(): Int? = remainingHint
}

private class PairDeserializer(
    private val key: Content,
    private val value: Content,
) : Deserializer {
    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> = deserializeSeq(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> =
        runCatching {
            val pairVisitor = PairVisitor(key, value)
            val pair = visitor.visitSeq(pairVisitor).getOrThrow()
            if (!pairVisitor.hasRemaining()) {
                pair
            } else {
                val remaining = pairVisitor.sizeHint() ?: 0
                // First argument is the number of elements in the data, second argument is the
                // number of elements expected by the deserializer.
                throw Error.invalidLength(2, ExpectedInSeq(2 - remaining))
            }
        }

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> =
        if (len == 2) {
            deserializeSeq(visitor)
        } else {
            // First argument is the number of elements in the data, second argument is the number
            // of elements expected by the deserializer.
            Result.failure(Error.invalidLength(2, ExpectedInSeq(len)))
        }

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
}

private class PairVisitor(
    key: Content,
    value: Content,
) : SeqAccess {
    private var keySlot: Content? = key
    private var valueSlot: Content? = value

    fun hasRemaining(): Boolean = keySlot != null || valueSlot != null

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): Result<T?> =
        runCatching {
            val next =
                when {
                    keySlot != null -> keySlot.also { keySlot = null }
                    valueSlot != null -> valueSlot.also { valueSlot = null }
                    else -> null
                }
            if (next == null) {
                null
            } else {
                seed.deserialize(ContentDeserializer.new(next)).getOrThrow()
            }
        }

    override fun sizeHint(): Int? =
        when {
            keySlot != null -> 2
            valueSlot != null -> 1
            else -> 0
        }
}

private class ExpectedInMap(
    private val count: Int,
) : Expected {
    override fun expecting(): String =
        if (count == 1) {
            "1 element in map"
        } else {
            "$count elements in map"
        }
}

public class EnumDeserializer private constructor(
    private val variant: Content,
    private val value: Content?,
) : EnumAccess {
    public companion object {
        public fun new(variant: Content, value: Content?): EnumDeserializer =
            EnumDeserializer(variant, value)
    }

    override fun <V> variantSeed(seed: DeserializeSeed<V>): Result<Pair<V, VariantAccess>> =
        seed.deserialize(ContentDeserializer.new(variant)).map { it to VariantDeserializer(value) }
}

private class VariantDeserializer(
    private val value: Content?,
) : VariantAccess {
    override fun unitVariant(): Result<Unit> =
        when (val v = value) {
            null -> Result.success(Unit)
            else -> IgnoredAny.deserialize(ContentDeserializer.new(v)).map { Unit }
        }

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): Result<T> =
        when (val v = value) {
            null -> Result.failure(Error.invalidType(Unexpected.UnitVariant, object : Expected { override fun expecting(): String = "newtype variant" }))
            else -> seed.deserialize(ContentDeserializer.new(v))
        }

    override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): Result<V> =
        when (val v = value) {
            is Content.Seq -> SeqDeserializer.new(v.value).deserializeAny(visitor)
            null -> Result.failure(Error.invalidType(Unexpected.UnitVariant, object : Expected { override fun expecting(): String = "tuple variant" }))
            else -> Result.failure(Error.invalidType(contentUnexpected(v), object : Expected { override fun expecting(): String = "tuple variant" }))
        }

    override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): Result<V> =
        when (val v = value) {
            is Content.Map -> MapDeserializer.new(v.value).deserializeAny(visitor)
            is Content.Seq -> SeqDeserializer.new(v.value).deserializeAny(visitor)
            null -> Result.failure(Error.invalidType(Unexpected.UnitVariant, object : Expected { override fun expecting(): String = "struct variant" }))
            else -> Result.failure(Error.invalidType(contentUnexpected(v), object : Expected { override fun expecting(): String = "struct variant" }))
        }
}

////////////////////////////////////////////////////////////////////////////////

// Like `IntoDeserializer` but also implemented for `ByteArray`. This is used for
// the newtype fallthrough case of `field_identifier`.
//
// Consider a field-identifier enum with a fallback string case, conceptually:
//
//    enum class F { A, B, Other(String) }
//
// The fallback case is deserialized using `IdentifierDeserializer`.
public interface IdentifierDeserializer {
    public fun intoIdentifierDeserializer(): Deserializer
}

public class Borrowed<T>(
    public val value: T,
)

public class U64IdentifierDeserializer(
    private val value: ULong,
) : IdentifierDeserializer {
    override fun intoIdentifierDeserializer(): Deserializer = value.intoDeserializer()
}

public class StrIdentifierDeserializer(
    private val value: String,
) : IdentifierDeserializer {
    override fun intoIdentifierDeserializer(): Deserializer = StrDeserializer(value)
}

public class BorrowedStrIdentifierDeserializer(
    private val value: String,
) : IdentifierDeserializer {
    override fun intoIdentifierDeserializer(): Deserializer = BorrowedStrDeserializer(value)
}

public class BytesIdentifierDeserializer(
    private val value: ByteArray,
) : IdentifierDeserializer {
    override fun intoIdentifierDeserializer(): Deserializer = BytesDeserializer.new(value)
}

public class BorrowedBytesIdentifierDeserializer(
    private val value: ByteArray,
) : IdentifierDeserializer {
    override fun intoIdentifierDeserializer(): Deserializer = BorrowedBytesDeserializer.new(value)
}

public class StrDeserializer(
    private val value: String,
) : Deserializer {
    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> = visitor.visitStr(value)

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
}

public class BorrowedStrDeserializer(
    private val value: String,
) : Deserializer {
    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> = visitor.visitBorrowedStr(value)

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
}

public class BorrowedBytesDeserializer private constructor(
    private val value: ByteArray,
) : Deserializer {
    public companion object {
        public fun new(value: ByteArray): BorrowedBytesDeserializer = BorrowedBytesDeserializer(value)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> = visitor.visitBorrowedBytes(value)

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
}

////////////////////////////////////////////////////////////////////////////////

public class FlatMapDeserializer(
    public val entries: MutableList<Pair<Content, Content>?>,
) : Deserializer {
    private fun <V> deserializeOther(): Result<V> =
        Result.failure(Error.custom("can only flatten structs and maps"))

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        deserializeMap(visitor)

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> =
        runCatching {
            for (index in entries.indices) {
                val entry = entries[index]
                val found = flatMapTakeEntry(entry, variants)
                if (found != null) {
                    val (key, value) = found
                    return@runCatching visitor.visitEnum(EnumDeserializer.new(key, value)).getOrThrow()
                }
            }

            throw Error.custom("no variant of enum $name found in flattened data")
        }

    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> =
        visitor.visitMap(
            FlatMapAccess(
                iter = entries.iterator(),
            ),
        )

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> =
        visitor.visitMap(
            FlatStructAccess(
                iter = entries.listIterator(),
                fields = fields,
            ),
        )

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> =
        visitor.visitNewtypeStruct(this)

    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> {
        val privateResult = visitor.privateVisitUntaggedOption(this)
        return if (privateResult.isSuccess) {
            privateResult
        } else {
            deserializeOther()
        }
    }

    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> =
        visitor.visitUnit()

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> =
        visitor.visitUnit()

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> =
        visitor.visitUnit()

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeOther()
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeOther()
}

private class FlatMapAccess(
    private val iter: Iterator<Pair<Content, Content>?>,
) : MapAccess {
    private var pendingContent: Content? = null

    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): Result<K?> =
        runCatching {
            for (item in iter) {
                // Items in the vector are nulled out when used by a struct.
                val entry = item ?: continue
                // Do not take(), instead borrow this entry. The internally tagged
                // enum does its own buffering so we can't tell whether this entry
                // is going to be consumed. Borrowing here leaves the entry
                // available for later flattened fields.
                pendingContent = entry.second
                return@runCatching seed.deserialize(ContentRefDeserializer.new(entry.first)).getOrThrow()
            }
            null
        }

    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): Result<V> =
        runCatching {
            val value = pendingContent ?: throw Error.custom("value is missing")
            pendingContent = null
            seed.deserialize(ContentRefDeserializer.new(value)).getOrThrow()
        }
}

private class FlatStructAccess(
    private val iter: MutableListIterator<Pair<Content, Content>?>,
    private val fields: List<String>,
) : MapAccess {
    private var pendingContent: Content? = null

    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): Result<K?> =
        runCatching {
            while (iter.hasNext()) {
                val index = iter.nextIndex()
                val entry = iter.next()
                val found = flatMapTakeEntry(entry, fields)
                if (found != null) {
                    val (key, value) = found
                    iter.set(null)
                    pendingContent = value
                    return@runCatching seed.deserialize(ContentDeserializer.new(key)).getOrThrow()
                }
            }
            null
        }

    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): Result<V> =
        runCatching {
            val value = pendingContent ?: throw Error.custom("value is missing")
            pendingContent = null
            seed.deserialize(ContentDeserializer.new(value)).getOrThrow()
        }
}

/**
 * Claims one key-value pair from a FlatMapDeserializer's field buffer if the
 * field name matches any of the recognized ones.
 */
private fun flatMapTakeEntry(
    entry: Pair<Content, Content>?,
    recognized: List<String>,
): Pair<Content, Content>? {
    // Entries in the FlatMapDeserializer buffer are nulled out as they get
    // claimed for deserialization. We only use an entry if it is still present
    // and if the field is one recognized by the current data structure.
    val isRecognized =
        if (entry == null) {
            false
        } else {
            val name = contentAsStr(entry.first)
            name != null && recognized.contains(name)
        }

    return if (isRecognized) entry else null
}

public class AdjacentlyTaggedEnumVariantSeed<F>(
    public val enumName: String,
    public val variants: List<String>,
    private val deserializeFieldsEnum: Deserialize<F>,
) : DeserializeSeed<F>
    where F : Any {
    override fun <D> deserialize(deserializer: D): Result<F>
        where D : Deserializer =
        deserializer.deserializeEnum(
            enumName,
            variants,
            AdjacentlyTaggedEnumVariantVisitor(
                enumName = enumName,
                deserializeFieldsEnum = deserializeFieldsEnum,
            ),
        )
}

public class AdjacentlyTaggedEnumVariantVisitor<F>(
    private val enumName: String,
    private val deserializeFieldsEnum: Deserialize<F>,
) : Visitor<F>
    where F : Any {
    override fun expecting(): String = "variant of enum $enumName"

    override fun <A> visitEnum(data: A): Result<F>
        where A : EnumAccess =
        runCatching {
            val (variant, variantAccess) =
                data.variantSeed(object : DeserializeSeed<F> {
                    override fun <D> deserialize(deserializer: D): Result<F>
                        where D : Deserializer =
                        deserializeFieldsEnum.deserialize(deserializer)
                }).getOrThrow()
            variantAccess.unitVariant().getOrThrow()
            variant
        }
}

////////////////////////////////////////////////////////////////////////////////

public class ContentRefDeserializer(
    private val content: Content,
) : Deserializer {
    public companion object {
        // Private API, don't use.
        public fun new(content: Content): ContentRefDeserializer = ContentRefDeserializer(content)
    }

    private fun invalidType(exp: Expected): Throwable =
        Error.invalidType(contentUnexpected(content), exp)

    private fun <V> deserializeInteger(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.U8 -> visitor.visitU8(content.value)
            is Content.U16 -> visitor.visitU16(content.value)
            is Content.U32 -> visitor.visitU32(content.value)
            is Content.U64 -> visitor.visitU64(content.value)
            is Content.I8 -> visitor.visitI8(content.value)
            is Content.I16 -> visitor.visitI16(content.value)
            is Content.I32 -> visitor.visitI32(content.value)
            is Content.I64 -> visitor.visitI64(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    private fun <V> deserializeFloat(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.F32 -> visitor.visitF32(content.value)
            is Content.F64 -> visitor.visitF64(content.value)
            is Content.U8 -> visitor.visitU8(content.value)
            is Content.U16 -> visitor.visitU16(content.value)
            is Content.U32 -> visitor.visitU32(content.value)
            is Content.U64 -> visitor.visitU64(content.value)
            is Content.I8 -> visitor.visitI8(content.value)
            is Content.I16 -> visitor.visitI16(content.value)
            is Content.I32 -> visitor.visitI32(content.value)
            is Content.I64 -> visitor.visitI64(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Bool -> visitor.visitBool(content.value)
            is Content.U8 -> visitor.visitU8(content.value)
            is Content.U16 -> visitor.visitU16(content.value)
            is Content.U32 -> visitor.visitU32(content.value)
            is Content.U64 -> visitor.visitU64(content.value)
            is Content.I8 -> visitor.visitI8(content.value)
            is Content.I16 -> visitor.visitI16(content.value)
            is Content.I32 -> visitor.visitI32(content.value)
            is Content.I64 -> visitor.visitI64(content.value)
            is Content.F32 -> visitor.visitF32(content.value)
            is Content.F64 -> visitor.visitF64(content.value)
            is Content.Char -> visitor.visitChar(content.value)
            is Content.String -> visitor.visitStr(content.value)
            is Content.Str -> visitor.visitBorrowedStr(content.value)
            is Content.ByteBuf -> visitor.visitBytes(content.value)
            is Content.Bytes -> visitor.visitBorrowedBytes(content.value)
            Content.Unit -> visitor.visitUnit()
            Content.None -> visitor.visitNone()
            is Content.Some -> visitor.visitSome(new(content.value))
            is Content.Newtype -> visitor.visitNewtypeStruct(new(content.value))
            is Content.Seq -> visitContentSeqRef(content.value, visitor)
            is Content.Map -> visitContentMapRef(content.value, visitor)
        }

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Bool -> visitor.visitBool(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeInteger(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeFloat(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeFloat(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Char -> visitor.visitChar(content.value)
            is Content.String -> visitor.visitStr(content.value)
            is Content.Str -> visitor.visitBorrowedStr(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.String -> visitor.visitStr(content.value)
            is Content.Str -> visitor.visitBorrowedStr(content.value)
            is Content.ByteBuf -> visitor.visitBytes(content.value)
            is Content.Bytes -> visitor.visitBorrowedBytes(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeStr(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.String -> visitor.visitStr(content.value)
            is Content.Str -> visitor.visitBorrowedStr(content.value)
            is Content.ByteBuf -> visitor.visitBytes(content.value)
            is Content.Bytes -> visitor.visitBorrowedBytes(content.value)
            is Content.Seq -> visitContentSeqRef(content.value, visitor)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeBytes(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> =
        when (content) {
            Content.None -> visitor.visitNone()
            is Content.Some -> visitor.visitSome(new(content.value))
            Content.Unit -> visitor.visitUnit()
            else -> visitor.visitSome(this)
        }

    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> =
        when (content) {
            Content.Unit -> visitor.visitUnit()
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeUnit(visitor)

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Newtype -> visitor.visitNewtypeStruct(new(content.value))
            else -> visitor.visitNewtypeStruct(this)
        }

    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Seq -> visitContentSeqRef(content.value, visitor)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeSeq(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeSeq(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Map -> visitContentMapRef(content.value, visitor)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.Seq -> visitContentSeqRef(content.value, visitor)
            is Content.Map -> visitContentMapRef(content.value, visitor)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> =
        runCatching {
            val (variant, value) =
                when (content) {
                    is Content.Map -> {
                        val iter = content.value.iterator()
                        val first = iter.nextOrNull()
                            ?: throw Error.invalidValue(Unexpected.Map, object : Expected { override fun expecting(): String = "map with a single key" })
                        if (iter.hasNext()) {
                            throw Error.invalidValue(Unexpected.Map, object : Expected { override fun expecting(): String = "map with a single key" })
                        }
                        // Enums are encoded in JSON as objects with a single key-value pair.
                        first.first to first.second
                    }

                    is Content.String -> content to null
                    is Content.Str -> content to null
                    else -> throw Error.invalidType(contentUnexpected(content), object : Expected { override fun expecting(): String = "string or map" })
                }

            visitor.visitEnum(EnumRefDeserializer(variant, value)).getOrThrow()
        }

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> =
        when (content) {
            is Content.String -> visitor.visitStr(content.value)
            is Content.Str -> visitor.visitBorrowedStr(content.value)
            is Content.ByteBuf -> visitor.visitBytes(content.value)
            is Content.Bytes -> visitor.visitBorrowedBytes(content.value)
            is Content.U8 -> visitor.visitU8(content.value)
            is Content.U64 -> visitor.visitU64(content.value)
            else -> Result.failure(invalidType(visitor))
        }

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> =
        visitor.visitUnit()
}

private fun <V> visitContentSeqRef(content: List<Content>, visitor: Visitor<V>): Result<V> =
    runCatching {
        val seqVisitor = SeqRefDeserializer.new(content)
        val value = visitor.visitSeq(seqVisitor).getOrThrow()
        seqVisitor.end().getOrThrow()
        value
    }

private fun <V> visitContentMapRef(content: List<Pair<Content, Content>>, visitor: Visitor<V>): Result<V> =
    runCatching {
        val mapVisitor = MapRefDeserializer.new(content)
        val value = visitor.visitMap(mapVisitor).getOrThrow()
        mapVisitor.end().getOrThrow()
        value
    }

private class SeqRefDeserializer(
    private val iter: Iterator<Content>,
) : Deserializer, SeqAccess {
    private var count: Int = 0
    private var remainingHint: Int? = null

    public companion object {
        public fun new(content: List<Content>): SeqRefDeserializer = SeqRefDeserializer(content.iterator()).also {
            it.remainingHint = content.size
        }
    }

    fun end(): Result<Unit> =
        runCatching {
            val remaining = if (iter.hasNext()) iter.asSequence().count() else 0
            if (remaining == 0) {
                Unit
            } else {
                // First argument is the number of elements in the data, second argument is the
                // number of elements expected by the deserializer.
                throw Error.invalidLength(count + remaining, ExpectedInSeq(count))
            }
        }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        runCatching {
            val value = visitor.visitSeq(this).getOrThrow()
            end().getOrThrow()
            value
        }

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): Result<T?> =
        runCatching {
            if (remainingHint != null) {
                remainingHint = maxOf(0, remainingHint!! - 1)
            }
            val next = if (iter.hasNext()) iter.next() else null
            if (next == null) {
                null
            } else {
                count += 1
                seed.deserialize(ContentRefDeserializer.new(next)).getOrThrow()
            }
        }

    override fun sizeHint(): Int? = remainingHint
}

private class MapRefDeserializer(
    private val iter: Iterator<Pair<Content, Content>>,
) : Deserializer, MapAccess, SeqAccess {
    private var value: Content? = null
    private var count: Int = 0
    private var remainingHint: Int? = null

    public companion object {
        public fun new(content: List<Pair<Content, Content>>): MapRefDeserializer =
            MapRefDeserializer(content.iterator()).also { it.remainingHint = content.size }
    }

    fun end(): Result<Unit> =
        runCatching {
            val remaining = if (iter.hasNext()) iter.asSequence().count() else 0
            if (remaining == 0) {
                Unit
            } else {
                // First argument is the number of elements in the data, second argument is the
                // number of elements expected by the deserializer.
                throw Error.invalidLength(count + remaining, ExpectedInMap(count))
            }
        }

    private fun nextPair(): Pair<Content, Content>? =
        if (iter.hasNext()) {
            count += 1
            if (remainingHint != null) {
                remainingHint = maxOf(0, remainingHint!! - 1)
            }
            iter.next()
        } else {
            null
        }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        runCatching {
            val value = visitor.visitMap(this).getOrThrow()
            end().getOrThrow()
            value
        }

    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> =
        runCatching {
            val value = visitor.visitSeq(this).getOrThrow()
            end().getOrThrow()
            value
        }

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> =
        deserializeSeq(visitor)

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): Result<K?> =
        runCatching {
            val pair = nextPair()
            if (pair == null) {
                null
            } else {
                value = pair.second
                seed.deserialize(ContentRefDeserializer.new(pair.first)).getOrThrow()
            }
        }

    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): Result<V> =
        runCatching {
            val v = value
                // Throw because this indicates a bug in the program rather than an expected
                // failure.
                ?: throw IllegalStateException("MapAccess.nextValue called before nextKey")
            value = null
            seed.deserialize(ContentRefDeserializer.new(v)).getOrThrow()
        }

    override fun <K, V> nextEntrySeed(keySeed: DeserializeSeed<K>, valueSeed: DeserializeSeed<V>): Result<Pair<K, V>?> =
        runCatching {
            val pair = nextPair()
            if (pair == null) {
                null
            } else {
                val key = keySeed.deserialize(ContentRefDeserializer.new(pair.first)).getOrThrow()
                val value = valueSeed.deserialize(ContentRefDeserializer.new(pair.second)).getOrThrow()
                key to value
            }
        }

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): Result<T?> =
        runCatching {
            val pair = nextPair()
            if (pair == null) {
                null
            } else {
                seed.deserialize(PairRefDeserializer(pair.first, pair.second)).getOrThrow()
            }
        }

    override fun sizeHint(): Int? = remainingHint
}

private class PairRefDeserializer(
    private val key: Content,
    private val value: Content,
) : Deserializer {
    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> = deserializeSeq(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> =
        runCatching {
            val pairVisitor = PairRefVisitor(key, value)
            val pair = visitor.visitSeq(pairVisitor).getOrThrow()
            if (!pairVisitor.hasRemaining()) {
                pair
            } else {
                val remaining = pairVisitor.sizeHint() ?: 0
                // First argument is the number of elements in the data, second argument is the
                // number of elements expected by the deserializer.
                throw Error.invalidLength(2, ExpectedInSeq(2 - remaining))
            }
        }

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> =
        if (len == 2) {
            deserializeSeq(visitor)
        } else {
            // First argument is the number of elements in the data, second argument is the number
            // of elements expected by the deserializer.
            Result.failure(Error.invalidLength(2, ExpectedInSeq(len)))
        }

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
}

private class PairRefVisitor(
    key: Content,
    value: Content,
) : SeqAccess {
    private var keySlot: Content? = key
    private var valueSlot: Content? = value

    fun hasRemaining(): Boolean = keySlot != null || valueSlot != null

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): Result<T?> =
        runCatching {
            val next =
                when {
                    keySlot != null -> keySlot.also { keySlot = null }
                    valueSlot != null -> valueSlot.also { valueSlot = null }
                    else -> null
                }
            if (next == null) {
                null
            } else {
                seed.deserialize(ContentRefDeserializer.new(next)).getOrThrow()
            }
        }

    override fun sizeHint(): Int? =
        when {
            keySlot != null -> 2
            valueSlot != null -> 1
            else -> 0
        }
}

private class EnumRefDeserializer(
    private val variant: Content,
    private val value: Content?,
) : EnumAccess {
    override fun <V> variantSeed(seed: DeserializeSeed<V>): Result<Pair<V, VariantAccess>> =
        seed.deserialize(ContentRefDeserializer.new(variant)).map { it to VariantRefDeserializer(value) }
}

private class VariantRefDeserializer(
    private val value: Content?,
) : VariantAccess {
    override fun unitVariant(): Result<Unit> =
        when (val v = value) {
            null -> Result.success(Unit)
            else -> IgnoredAny.deserialize(ContentRefDeserializer.new(v)).map { Unit }
        }

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): Result<T> =
        when (val v = value) {
            null -> Result.failure(Error.invalidType(Unexpected.UnitVariant, object : Expected { override fun expecting(): String = "newtype variant" }))
            else -> seed.deserialize(ContentRefDeserializer.new(v))
        }

    override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): Result<V> =
        when (val v = value) {
            is Content.Seq -> visitContentSeqRef(v.value, visitor)
            null -> Result.failure(Error.invalidType(Unexpected.UnitVariant, object : Expected { override fun expecting(): String = "tuple variant" }))
            else -> Result.failure(Error.invalidType(contentUnexpected(v), object : Expected { override fun expecting(): String = "tuple variant" }))
        }

    override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): Result<V> =
        when (val v = value) {
            is Content.Map -> visitContentMapRef(v.value, visitor)
            is Content.Seq -> visitContentSeqRef(v.value, visitor)
            null -> Result.failure(Error.invalidType(Unexpected.UnitVariant, object : Expected { override fun expecting(): String = "struct variant" }))
            else -> Result.failure(Error.invalidType(contentUnexpected(v), object : Expected { override fun expecting(): String = "struct variant" }))
        }
}

////////////////////////////////////////////////////////////////////////////////

private fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null
