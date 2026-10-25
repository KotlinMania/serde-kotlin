package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serdecore.priv.contentaccess.validScalar

/** An unexpected Unicode scalar that need not fit in one UTF-16 code unit. */
class UnexpectedScalarChar(val codePoint: Int) : Unexpected() {
    init { require(validScalar(codePoint)) }
    override fun equals(other: Any?): Boolean = other is UnexpectedScalarChar && codePoint == other.codePoint
    override fun hashCode(): Int = codePoint
}
