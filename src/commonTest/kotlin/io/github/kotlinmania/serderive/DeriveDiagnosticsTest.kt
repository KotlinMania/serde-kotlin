package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.procmacro2.TokenTree
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.Container
import io.github.kotlinmania.serderive.internals.Ctxt
import io.github.kotlinmania.serderive.internals.Data
import io.github.kotlinmania.serderive.internals.Derive
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.DeriveInputParse
import io.github.kotlinmania.syn.ExprParse
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.parse2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeriveDiagnosticsTest {
    @Test
    fun unknownContainerAttributeBecomesACompilerDiagnostic() {
        val source = "#[serde(unknown)] struct Example;"
        val expected = "unknown serde container attribute `unknown`"
        assertEquals(listOf(expected), errors(source).map { it.message })
        assertEquals(listOf(expected), errors(source, deserialize = true).map { it.message })
        for (derive in listOf(::deriveSerialize, ::deriveDeserialize)) {
            val output = derive(tokens(source))
            assertTrue(output.toString().contains(expected), output.toString())
            assertEquals(1, output.filterIsInstance<TokenTree.Group>().size)
        }
    }

    @Test
    fun errorsAccumulateAcrossContainerVariantAndFieldAttributes() {
        val source = """
            #[serde(container_problem)]
            enum Example {
                #[serde(variant_problem)]
                Variant { #[serde(field_problem)] value: u8 }
            }
        """.trimIndent()
        val expected = listOf(
            "unknown serde container attribute `container_problem`",
            "unknown serde variant attribute `variant_problem`",
            "unknown serde field attribute `field_problem`",
        )
        assertEquals(expected, errors(source).map { it.message })
        assertEquals(expected, errors(source, deserialize = true).map { it.message })
    }

    @Test
    fun failedNestedRenameDoesNotCommitPartialValues() {
        val source = """
            #[serde(rename(serialize = "partial", invalid = "ignored"))]
            #[serde(rename = "complete")]
            struct Example;
        """.trimIndent()
        assertEquals(
            listOf("malformed rename attribute, expected `rename(serialize = ..., deserialize = ...)`"),
            errors(source).map { it.message },
        )
    }

    @Test
    fun failureStopsOnlyItsOwnAttribute() {
        val source = "#[serde(first, ignored)] #[serde(second)] struct Example;"
        assertEquals(
            listOf("unknown serde container attribute `first`", "unknown serde container attribute `second`"),
            errors(source).map { it.message },
        )
    }

    @Test
    fun duplicateRenameNamesTheAttributeForBothDirections() {
        val source = """#[serde(rename = "a", rename = "b")] struct Example;"""
        assertEquals(List(2) { "duplicate serde attribute `rename`" }, errors(source).map { it.message })
    }

    @Test
    fun nonStringExpressionGetsTheSerdeStringDiagnostic() {
        val source = "#[serde(rename = name)] struct Example;"
        assertEquals(
            listOf("expected serde rename attribute to be a string: `rename = \"...\"`"),
            errors(source).map { it.message },
        )
    }

    @Test
    fun stringSuffixIsDiagnosed() {
        val source = """#[serde(rename = "name"suffix)] struct Example;"""
        assertEquals(listOf("unexpected suffix `suffix` on string literal"), errors(source).map { it.message })
    }

    @Test
    fun unknownAttributeKeepsItsSourceSpanInCompilerTokens() {
        val source = "#[serde(unknown)] struct Example;"
        val diagnostic = errors(source).single()
        assertEquals("unknown", diagnostic.span().sourceText())
        val output = deriveSerialize(tokens(source))
        val group = output.filterIsInstance<TokenTree.Group>().single()
        assertEquals("unknown", group.span().sourceText())
    }

    @Test
    fun invalidTypeStringKeepsItsLiteralSpan() {
        val source = """#[serde(from = "Vec<")] struct Example;"""
        val diagnostic = errors(source).single()
        assertEquals("failed to parse type: from = \"Vec<\"", diagnostic.message)
        assertEquals("\"Vec<\"", diagnostic.span().sourceText())
    }

    @Test
    fun unionReturnsFailureInsteadOfEmptyOutput() {
        val source = "union Example { value: u8 }"
        val expected = "Serde does not support derive for unions"
        assertEquals(listOf(expected), errors(source).map { it.message })
        assertEquals(listOf(expected), errors(source, deserialize = true).map { it.message })
        assertTrue(deriveSerialize(tokens(source)).toString().contains(expected))
    }

    @Test
    fun syntacticallyInvalidInputEmitsACompilerError() {
        for (derive in listOf(::deriveSerialize, ::deriveDeserialize)) {
            val output = derive(tokens("struct"))
            assertTrue(output.toString().contains("compile_error"), output.toString())
        }
    }

    @Test
    fun validDirectionalNamesAndBoundsArePreserved() {
        val input = input("""#[serde(rename(serialize = "Out", deserialize = "In"), bound = "T: Clone")] struct Example<T> { value: T }""")
        val cx = Ctxt()
        val attrs = AttrContainer.fromAst(cx, input)
        assertTrue(cx.check().isSuccess)
        assertEquals("Out", attrs.name().serializeName().value)
        assertEquals("In", attrs.name().deserializeName().value)
        assertEquals(1, attrs.serBound()?.size)
        assertEquals(1, attrs.deBound()?.size)
    }

    @Test
    fun remoteSelfUsesTheDeclaredIdentifier() {
        val cx = Ctxt()
        val attrs = AttrContainer.fromAst(cx, input("""#[serde(remote = "Self")] struct Example;"""))
        assertTrue(cx.check().isSuccess)
        assertEquals("Example", attrs.remote().toString())
    }

    @Test
    fun rawIdentifierUsesItsUnprefixedSerdeName() {
        val cx = Ctxt()
        val attrs = AttrContainer.fromAst(cx, input("struct r#type;"))
        assertTrue(cx.check().isSuccess)
        assertEquals("type", attrs.name().serializeName().value)
    }

    @Test
    fun qualifiedSerializerPathRetainsItsQualifiedSelf() {
        val source = """struct Example<T> { #[serde(serialize_with = "<T as Trait>::serialize")] value: T }"""
        val cx = Ctxt()
        val container = assertNotNull(Container.fromAst(cx, input(source), Derive.Serialize, Private.ident()))
        val checked = cx.check()
        assertEquals(null, checked.exceptionOrNull()?.toCompileError()?.toString())
        val path = assertNotNull(container.data.allFields().single().attrs.serializeWith())
        assertNotNull(path.qself)
    }

    @Test
    fun synDependencyParsesQualifiedSerializerPath() {
        val parsed = parse2(ExprParse::parse, tokens("<T as Trait>::serialize"))
        assertEquals(null, parsed.exceptionOrNull()?.toCompileError()?.toString())
    }

    @Test
    fun explicitVariantAliasesAreNotRenamed() {
        val source = """#[serde(rename_all = "UPPERCASE")] enum Example { #[serde(alias = "oldName")] Variant }"""
        val cx = Ctxt()
        val container = assertNotNull(Container.fromAst(cx, input(source), Derive.Deserialize, Private.ident()))
        assertTrue(cx.check().isSuccess)
        val variant = (container.data as Data.Enum).variants.single()
        assertEquals(listOf("VARIANT", "oldName"), variant.attrs.aliases().map { it.value })
    }

    @Test
    fun explicitFieldAliasesAreNotRenamed() {
        val source = """#[serde(rename_all = "UPPERCASE")] struct Example { #[serde(alias = "oldName")] value: u8 }"""
        val cx = Ctxt()
        val container = assertNotNull(Container.fromAst(cx, input(source), Derive.Deserialize, Private.ident()))
        assertTrue(cx.check().isSuccess)
        assertEquals(listOf("VALUE", "oldName"), container.data.allFields().single().attrs.aliases().map { it.value })
    }

    @Test
    fun packedDetectionUsesTopLevelIdentifiers() {
        for (attribute in listOf("#[repr(unpacked)]", "#[repr(\"packed\")]", "#[repr(wrapper(packed))]")) {
            val cx = Ctxt()
            val attrs = AttrContainer.fromAst(cx, input("$attribute struct Example;"))
            assertTrue(cx.check().isSuccess)
            assertEquals(false, attrs.isPacked(), attribute)
        }
    }

    @Test
    fun packedDetectionAccumulatesAcrossAttributes() {
        val cx = Ctxt()
        val attrs = AttrContainer.fromAst(cx, input("#[repr(packed)] #[repr(C)] struct Example;"))
        assertTrue(cx.check().isSuccess)
        assertTrue(attrs.isPacked())
    }

    @Test
    fun borrowedLifetimeDiagnosticNamesTheActualFieldAndLifetime() {
        val source = """struct Example<'a, 'b> { #[serde(borrow = "'b")] value: &'a str }"""
        assertEquals(listOf("field `value` does not have lifetime 'b"), errors(source).map { it.message })
    }

    private fun errors(source: String, deserialize: Boolean = false): List<SynError> {
        val result = if (deserialize) expandDeriveDeserialize(input(source)) else expandDeriveSerialize(input(source))
        return assertNotNull(result.exceptionOrNull(), source).toList()
    }

    private fun input(source: String): DeriveInput = parse2(DeriveInputParse::parse, tokens(source)).getOrThrow()

    private fun tokens(source: String): TokenStream = TokenStream.fromString(source).getOrThrow()
}
