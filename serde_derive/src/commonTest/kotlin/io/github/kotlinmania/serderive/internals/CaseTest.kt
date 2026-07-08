// port-lint: tests internals/case.rs
package io.github.kotlinmania.serderive.internals

import kotlin.test.Test
import kotlin.test.assertEquals

class CaseTest {

    @Test
    fun renameVariants() {
        val cases = listOf(
            VariantCase("Outcome", "outcome", "OUTCOME", "outcome", "outcome", "OUTCOME", "outcome", "OUTCOME"),
            VariantCase(
                "VeryTasty",
                "verytasty",
                "VERYTASTY",
                "veryTasty",
                "very_tasty",
                "VERY_TASTY",
                "very-tasty",
                "VERY-TASTY",
            ),
            VariantCase("A", "a", "A", "a", "a", "A", "a", "A"),
            VariantCase("Z42", "z42", "Z42", "z42", "z42", "Z42", "z42", "Z42"),
        )
        for (c in cases) {
            assertEquals(c.original, RenameRule.None.applyToVariant(c.original))
            assertEquals(c.lower, RenameRule.LowerCase.applyToVariant(c.original))
            assertEquals(c.upper, RenameRule.UpperCase.applyToVariant(c.original))
            assertEquals(c.original, RenameRule.PascalCase.applyToVariant(c.original))
            assertEquals(c.camel, RenameRule.CamelCase.applyToVariant(c.original))
            assertEquals(c.snake, RenameRule.SnakeCase.applyToVariant(c.original))
            assertEquals(c.screaming, RenameRule.ScreamingSnakeCase.applyToVariant(c.original))
            assertEquals(c.kebab, RenameRule.KebabCase.applyToVariant(c.original))
            assertEquals(c.screamingKebab, RenameRule.ScreamingKebabCase.applyToVariant(c.original))
        }
    }

    @Test
    fun renameFields() {
        val cases = listOf(
            FieldCase("outcome", "OUTCOME", "Outcome", "outcome", "OUTCOME", "outcome", "OUTCOME"),
            FieldCase(
                "very_tasty",
                "VERY_TASTY",
                "VeryTasty",
                "veryTasty",
                "VERY_TASTY",
                "very-tasty",
                "VERY-TASTY",
            ),
            FieldCase("a", "A", "A", "a", "A", "a", "A"),
            FieldCase("z42", "Z42", "Z42", "z42", "Z42", "z42", "Z42"),
        )
        for (c in cases) {
            assertEquals(c.original, RenameRule.None.applyToField(c.original))
            assertEquals(c.upper, RenameRule.UpperCase.applyToField(c.original))
            assertEquals(c.pascal, RenameRule.PascalCase.applyToField(c.original))
            assertEquals(c.camel, RenameRule.CamelCase.applyToField(c.original))
            assertEquals(c.original, RenameRule.SnakeCase.applyToField(c.original))
            assertEquals(c.screaming, RenameRule.ScreamingSnakeCase.applyToField(c.original))
            assertEquals(c.kebab, RenameRule.KebabCase.applyToField(c.original))
            assertEquals(c.screamingKebab, RenameRule.ScreamingKebabCase.applyToField(c.original))
        }
    }

    private data class VariantCase(
        val original: String,
        val lower: String,
        val upper: String,
        val camel: String,
        val snake: String,
        val screaming: String,
        val kebab: String,
        val screamingKebab: String,
    )

    private data class FieldCase(
        val original: String,
        val upper: String,
        val pascal: String,
        val camel: String,
        val screaming: String,
        val kebab: String,
        val screamingKebab: String,
    )
}