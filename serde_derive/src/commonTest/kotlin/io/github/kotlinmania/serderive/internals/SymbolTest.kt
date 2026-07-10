// port-lint: tests internals/symbol.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.syn.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SymbolTest {
    @Test
    fun identMatchesSymbolWord() {
        val ident = Ident.new("serialize", Span.callSite())

        assertTrue(ident.eq(SERIALIZE))
        assertFalse(ident.eq(DESERIALIZE))
    }

    @Test
    fun pathMatchesSingleSegmentSymbolWord() {
        val path = Path.from(Ident.new("deserialize", Span.callSite()))

        assertTrue(path.eq(DESERIALIZE))
        assertFalse(path.eq(SERIALIZE))
    }

    @Test
    fun symbolDisplaysItsWord() {
        assertEquals("rename_all", RENAME_ALL.toString())
    }
}
