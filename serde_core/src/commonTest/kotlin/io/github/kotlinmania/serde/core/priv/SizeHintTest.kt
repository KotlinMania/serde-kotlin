// port-lint: source serde_core/src/private/size_hint.rs
package io.github.kotlinmania.serde.core.`private`

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

public class SizeHintTest {
    @Test
    public fun fromBoundsReturnsExactUpperBoundOnly() {
        assertEquals(3, fromBounds(SizeHintIterator(3, 3)))
        assertNull(fromBounds(SizeHintIterator(2, 3)))
        assertNull(fromBounds(SizeHintIterator(2, null)))
    }

    @Test
    public fun cautiousCapsByElementSize() {
        assertEquals(0, cautious<Unit>(2_000_000))
        assertEquals(1_048_576, cautious<Byte>(2_000_000))
        assertEquals(524_288, cautious<Short>(2_000_000))
        assertEquals(262_144, cautious<Int>(2_000_000))
        assertEquals(131_072, cautious<Long>(2_000_000))
    }
}

private class SizeHintIterator(
    private val lower: Int,
    private val upper: Int?,
) : IteratorWithSizeHint<Unit> {
    override fun hasNext(): Boolean = false

    override fun next(): Unit = throw NoSuchElementException()

    override fun sizeHint(): Pair<Int, Int?> = lower to upper
}
