// port-lint: source private/doc.rs
package io.github.kotlinmania.serdecore.priv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

public class DocTest {
    @Test
    public fun hiddenErrorCarriesCustomDisplayText() {
        val error = Error.custom("documentation serializer failure")

        assertEquals("documentation serializer failure", error.description())
        assertEquals("documentation serializer failure", error.toString())
        assertNull(error.source)
    }
}
