// port-lint: source serde_derive/src/internals/case.rs
package io.github.kotlinmania.serde.serdederive.src.internals

import kotlin.test.Test
import kotlin.test.assertEquals

public class CaseTest {
    @Test
    public fun renameVariants() {
        val cases =
            listOf(
                RenameVariantCase(
                    original = "Outcome",
                    lower = "outcome",
                    upper = "OUTCOME",
                    camel = "outcome",
                    snake = "outcome",
                    screaming = "OUTCOME",
                    kebab = "outcome",
                    screamingKebab = "OUTCOME",
                ),
                RenameVariantCase(
                    original = "VeryTasty",
                    lower = "verytasty",
                    upper = "VERYTASTY",
                    camel = "veryTasty",
                    snake = "very_tasty",
                    screaming = "VERY_TASTY",
                    kebab = "very-tasty",
                    screamingKebab = "VERY-TASTY",
                ),
                RenameVariantCase(
                    original = "A",
                    lower = "a",
                    upper = "A",
                    camel = "a",
                    snake = "a",
                    screaming = "A",
                    kebab = "a",
                    screamingKebab = "A",
                ),
                RenameVariantCase(
                    original = "Z42",
                    lower = "z42",
                    upper = "Z42",
                    camel = "z42",
                    snake = "z42",
                    screaming = "Z42",
                    kebab = "z42",
                    screamingKebab = "Z42",
                ),
            )

        for ((original, lower, upper, camel, snake, screaming, kebab, screamingKebab) in cases) {
            assertEquals(original, RenameRule.None.applyToVariant(original))
            assertEquals(lower, RenameRule.LowerCase.applyToVariant(original))
            assertEquals(upper, RenameRule.UpperCase.applyToVariant(original))
            assertEquals(original, RenameRule.PascalCase.applyToVariant(original))
            assertEquals(camel, RenameRule.CamelCase.applyToVariant(original))
            assertEquals(snake, RenameRule.SnakeCase.applyToVariant(original))
            assertEquals(screaming, RenameRule.ScreamingSnakeCase.applyToVariant(original))
            assertEquals(kebab, RenameRule.KebabCase.applyToVariant(original))
            assertEquals(screamingKebab, RenameRule.ScreamingKebabCase.applyToVariant(original))
        }
    }

    @Test
    public fun renameFields() {
        val cases =
            listOf(
                RenameFieldCase(
                    original = "outcome",
                    upper = "OUTCOME",
                    pascal = "Outcome",
                    camel = "outcome",
                    screaming = "OUTCOME",
                    kebab = "outcome",
                    screamingKebab = "OUTCOME",
                ),
                RenameFieldCase(
                    original = "very_tasty",
                    upper = "VERY_TASTY",
                    pascal = "VeryTasty",
                    camel = "veryTasty",
                    screaming = "VERY_TASTY",
                    kebab = "very-tasty",
                    screamingKebab = "VERY-TASTY",
                ),
                RenameFieldCase(
                    original = "a",
                    upper = "A",
                    pascal = "A",
                    camel = "a",
                    screaming = "A",
                    kebab = "a",
                    screamingKebab = "A",
                ),
                RenameFieldCase(
                    original = "z42",
                    upper = "Z42",
                    pascal = "Z42",
                    camel = "z42",
                    screaming = "Z42",
                    kebab = "z42",
                    screamingKebab = "Z42",
                ),
            )

        for ((original, upper, pascal, camel, screaming, kebab, screamingKebab) in cases) {
            assertEquals(original, RenameRule.None.applyToField(original))
            assertEquals(upper, RenameRule.UpperCase.applyToField(original))
            assertEquals(pascal, RenameRule.PascalCase.applyToField(original))
            assertEquals(camel, RenameRule.CamelCase.applyToField(original))
            assertEquals(original, RenameRule.SnakeCase.applyToField(original))
            assertEquals(screaming, RenameRule.ScreamingSnakeCase.applyToField(original))
            assertEquals(kebab, RenameRule.KebabCase.applyToField(original))
            assertEquals(screamingKebab, RenameRule.ScreamingKebabCase.applyToField(original))
        }
    }
}

private data class RenameVariantCase(
    val original: String,
    val lower: String,
    val upper: String,
    val camel: String,
    val snake: String,
    val screaming: String,
    val kebab: String,
    val screamingKebab: String,
)

private data class RenameFieldCase(
    val original: String,
    val upper: String,
    val pascal: String,
    val camel: String,
    val screaming: String,
    val kebab: String,
    val screamingKebab: String,
)
