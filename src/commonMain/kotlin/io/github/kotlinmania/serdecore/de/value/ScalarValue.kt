package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.priv.contentaccess.validScalar
import io.github.kotlinmania.serdecore.priv.SharedByteSlice

/** A deserializer holding one complete Unicode scalar. */
class CharCodePointDeserializer private constructor(override val value: Int) : PrimitiveDeserializer<Int>() {
    init { require(validScalar(value)) }
    companion object { fun new(codePoint: Int): CharCodePointDeserializer = CharCodePointDeserializer(codePoint) }
    override fun <V> visit(visitor: Visitor<V>, value: Int): SerdeResult<V> = visitor.visitCharCodePoint(value)
}

/** A deserializer retaining exactly the selected immutable byte region. */
class BorrowedByteSliceDeserializer private constructor(override val value: SharedByteSlice) : PrimitiveDeserializer<SharedByteSlice>() {
    companion object { fun new(value: SharedByteSlice): BorrowedByteSliceDeserializer = BorrowedByteSliceDeserializer(value) }
    override fun <V> visit(visitor: Visitor<V>, value: SharedByteSlice): SerdeResult<V> = visitor.visitBorrowedByteSlice(value)
}
