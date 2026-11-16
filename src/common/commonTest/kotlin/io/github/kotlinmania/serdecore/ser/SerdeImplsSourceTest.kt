// port-lint: tests serde/src/core/ser/impls.rs
package io.github.kotlinmania.serdecore.ser

import kotlin.test.Test
import kotlin.test.assertContentEquals

class SerdeImplsSourceTest {
    @Test
    fun testFormatU8() {
        var value: UByte = 0u
        while (true) {
            val buffer = ByteArray(3)
            val written = formatU8(value, buffer)
            assertContentEquals(value.toString().encodeToByteArray(), buffer.copyOfRange(0, written))
            if (value == UByte.MAX_VALUE) break
            value = (value + 1u).toUByte()
        }
    }
}
