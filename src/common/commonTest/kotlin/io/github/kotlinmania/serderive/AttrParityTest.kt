package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.Container
import io.github.kotlinmania.serderive.internals.Ctxt
import io.github.kotlinmania.serderive.internals.Derive
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.DeriveInputParse
import io.github.kotlinmania.syn.Lifetime
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.parse2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AttrParityTest {
    @Test
    fun sharedReferencesAreImplicitlyBorrowed() {
        for (type in listOf("&'a str", "&'a [u8]", "Option<&'a str>", "Option<&'a [u8]>")) {
            val field = container("struct Example<'a> { value: $type }").data.allFields().single()
            assertEquals(listOf("'a"), field.attrs.borrowedLifetimes().map { it.toString() }, type)
        }
    }

    @Test
    fun mutableReferencesAreNotImplicitlyBorrowed() {
        for (type in listOf("&'a mut str", "&'a mut [u8]", "Option<&'a mut str>", "Option<&'a mut [u8]>")) {
            val field = container("struct Example<'a> { value: $type }").data.allFields().single()
            assertEquals(emptyList(), field.attrs.borrowedLifetimes().map { it.toString() }, type)
        }
    }

    @Test
    fun explicitBorrowStillCollectsMutableReferenceLifetimes() {
        for (type in listOf("&'a mut str", "&'a mut [u8]", "Option<&'a mut str>", "Option<&'a mut [u8]>")) {
            val field = container("struct Example<'a> { #[serde(borrow)] value: $type }").data.allFields().single()
            assertEquals(listOf("'a"), field.attrs.borrowedLifetimes().map { it.toString() }, type)
        }
    }

    @Test
    fun collectedBorrowedLifetimesAreSortedAndDeduplicated() {
        val source = "struct Example<'z, 'a> { #[serde(borrow)] value: (Marker<'z>, Marker<'a>, Marker<'z>) }"
        val field = container(source).data.allFields().single()
        assertEquals(listOf("'a", "'z"), field.attrs.borrowedLifetimes().map { it.toString() })
    }

    @Test
    fun explicitBorrowedLifetimesAreSorted() {
        val source = """struct Example<'z, 'a> { #[serde(borrow = "'z + 'a")] value: (Marker<'z>, Marker<'a>) }"""
        val field = container(source).data.allFields().single()
        assertEquals(listOf("'a", "'z"), field.attrs.borrowedLifetimes().map { it.toString() })
    }

    @Test
    fun deserializerBoundsAreSortedAndExcludeSkippedFields() {
        val source = """
            struct Example<'z, 'a, 'ignored> {
                z: &'z str,
                a: &'a [u8],
                #[serde(skip_deserializing)] ignored: &'ignored str,
            }
        """.trimIndent()
        val borrowed = Parameters(container(source)).borrowed
        val bounds = assertNotNull(borrowed.deLifetimeParam()).bounds
        assertEquals(listOf("'a", "'z"), bounds.toList().map { it.toString() })
    }

    @Test
    fun generatedLifetimeBoundsOwnTheirCopies() {
        val lifetime = Lifetime.new("'a", Span.callSite())
        val borrowed = BorrowedLifetimes.Borrowed(setOf(lifetime))
        val parameter = assertNotNull(borrowed.deLifetimeParam())
        parameter.bounds.toList().single().ident = Lifetime.new("'changed", Span.callSite()).ident
        assertEquals("'a", lifetime.toString())
    }

    @Test
    fun conflictingTagAttributesRetainEverySourceSpan() {
        val cases = listOf(
            """#[serde(untagged, tag = "kind")] enum Example { Variant }""" to listOf("untagged", "tag"),
            """#[serde(untagged, content = "data")] enum Example { Variant }""" to listOf("untagged", "content"),
            """#[serde(untagged, tag = "kind", content = "data")] enum Example { Variant }""" to listOf("untagged", "tag", "content"),
        )
        for ((source, expected) in cases) {
            assertEquals(expected, containerErrors(source).map { it.span().sourceText() }, source)
        }
    }

    @Test
    fun conflictingIdentifierAttributesRetainBothSourceSpans() {
        val source = "#[serde(field_identifier, variant_identifier)] enum Example { Variant }"
        val errors = containerErrors(source)
        assertEquals(listOf("field_identifier", "variant_identifier"), errors.map { it.span().sourceText() })
        assertTrue(errors.all { it.message.contains("cannot both be set") })
    }

    private fun container(source: String): Container {
        val cx = Ctxt()
        val result = assertNotNull(Container.fromAst(cx, input(source), Derive.Deserialize, Private.ident()))
        assertEquals(null, cx.check().exceptionOrNull()?.toCompileError()?.toString(), source)
        return result
    }

    private fun containerErrors(source: String): List<SynError> {
        val cx = Ctxt()
        val attrs = AttrContainer.fromAst(cx, input(source))
        assertEquals("Example", attrs.name().serializeName().value)
        return assertNotNull(cx.check().exceptionOrNull(), source).toList()
    }

    private fun input(source: String): DeriveInput =
        parse2(DeriveInputParse::parse, TokenStream.fromString(source).getOrThrow()).getOrThrow()
}
