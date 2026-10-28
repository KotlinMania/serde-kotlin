package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.procmacro2.TokenTree
import io.github.kotlinmania.syn.SynError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CtxtResultTest {
    @Test
    fun emptyContextSucceedsAndCannotBeConsumedTwice() {
        val cx = Ctxt()
        assertTrue(cx.check().isSuccess)
        assertFailsWith<IllegalStateException> { cx.check() }
    }

    @Test
    fun combinedErrorsKeepTheirOriginalMessagesAndSpans() {
        val tokens = TokenStream.fromString("first second").getOrThrow().toList()
        val first = SynError.new(tokens[0].span(), "first problem")
        val second = SynError.new(tokens[1].span(), "second problem")
        val cx = Ctxt()
        cx.synError(first)
        cx.synError(second)

        val error = assertNotNull(cx.check().exceptionOrNull())
        assertSame(first, error)
        val diagnostics = error.toList()
        assertEquals(listOf("first problem", "second problem"), diagnostics.map { it.message })
        assertEquals(tokens.map { it.span() }, diagnostics.map { it.span() })
        val output = error.intoCompileError()
        val groups = output.filterIsInstance<TokenTree.Group>()
        assertEquals(tokens.map { it.span() }, groups.map { it.span() })
        assertEquals(
            listOf("\"first problem\"", "\"second problem\""),
            groups.map { it.value.stream().toString() },
        )
    }

    @Test
    fun addingAnErrorAfterCheckingIsRejected() {
        val cx = Ctxt()
        assertTrue(cx.check().isSuccess)
        val tokens = TokenStream.fromString("field").getOrThrow()
        assertFailsWith<IllegalStateException> { cx.errorSpannedBy(tokens, "late problem") }
        val error = SynError.newSpanned(tokens, "late problem")
        assertFailsWith<IllegalStateException> { cx.synError(error) }
    }
}
