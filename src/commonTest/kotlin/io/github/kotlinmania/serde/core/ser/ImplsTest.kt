// port-lint: source serde_core/src/ser/impls.rs
package io.github.kotlinmania.serde.core.ser

import kotlin.test.Test
import kotlin.test.assertContentEquals

public class ImplsTest {
    @Test
    public fun testFormatU8() {
        var i: UByte = 0u

        while (true) {
            val buf = ByteArray(3)
            val written = formatU8(i, buf)
            assertContentEquals(i.toString().encodeToByteArray(), buf.copyOfRange(0, written))

            if (i == UByte.MAX_VALUE) {
                break
            }
            i = (i + 1u).toUByte()
        }
    }
}

