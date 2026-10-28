// port-lint: tests serde_derive_internals/src/case.rs
package io.github.kotlinmania.serderive.internals

import kotlin.test.Test
import kotlin.test.assertEquals

class CaseInternalsTest {
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
        for (case in cases) {
            assertEquals(case.original, RenameRule.None.applyToVariant(case.original))
            assertEquals(case.lower, RenameRule.LowerCase.applyToVariant(case.original))
            assertEquals(case.upper, RenameRule.UpperCase.applyToVariant(case.original))
            assertEquals(case.original, RenameRule.PascalCase.applyToVariant(case.original))
            assertEquals(case.camel, RenameRule.CamelCase.applyToVariant(case.original))
            assertEquals(case.snake, RenameRule.SnakeCase.applyToVariant(case.original))
            assertEquals(case.screaming, RenameRule.ScreamingSnakeCase.applyToVariant(case.original))
            assertEquals(case.kebab, RenameRule.KebabCase.applyToVariant(case.original))
            assertEquals(case.screamingKebab, RenameRule.ScreamingKebabCase.applyToVariant(case.original))
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
        for (case in cases) {
            assertEquals(case.original, RenameRule.None.applyToField(case.original))
            assertEquals(case.upper, RenameRule.UpperCase.applyToField(case.original))
            assertEquals(case.pascal, RenameRule.PascalCase.applyToField(case.original))
            assertEquals(case.camel, RenameRule.CamelCase.applyToField(case.original))
            assertEquals(case.original, RenameRule.SnakeCase.applyToField(case.original))
            assertEquals(case.screaming, RenameRule.ScreamingSnakeCase.applyToField(case.original))
            assertEquals(case.kebab, RenameRule.KebabCase.applyToField(case.original))
            assertEquals(case.screamingKebab, RenameRule.ScreamingKebabCase.applyToField(case.original))
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
