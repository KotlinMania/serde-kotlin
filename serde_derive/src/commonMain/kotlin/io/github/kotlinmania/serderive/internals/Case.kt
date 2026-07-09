// port-lint: source internals/case.rs
package io.github.kotlinmania.serderive.internals

// Code to convert the styled field/variant (e.g. myField, MyType) to
// the case of the source (e.g. my-field, MY_FIELD).

// The different possible ways to change case of fields in a struct, or
// variants in an enum.
public enum class RenameRule {
    // Don't apply a default rename rule.
    None,
    // Rename direct children to "lowercase" style.
    LowerCase,
    // Rename direct children to "UPPERCASE" style.
    UpperCase,
    // Rename direct children to "PascalCase" style, as typically used for
    // enum variants.
    PascalCase,
    // Rename direct children to "camelCase" style.
    CamelCase,
    // Rename direct children to "snake_case" style, as commonly used for
    // fields.
    SnakeCase,
    // Rename direct children to "SCREAMING_SNAKE_CASE" style, as commonly
    // used for constants.
    ScreamingSnakeCase,
    // Rename direct children to "kebab-case" style.
    KebabCase,
    // Rename direct children to "SCREAMING-KEBAB-CASE" style.
    ScreamingKebabCase;

    public fun applyToVariant(variant: String): String {
        return when (this) {
            None, PascalCase -> variant
            LowerCase -> variant.lowercase()
            UpperCase -> variant.uppercase()
            CamelCase -> {
                if (variant.isEmpty()) variant
                else variant[0].lowercase() + variant.substring(1)
            }
            SnakeCase -> {
                val snake = StringBuilder()
                for ((i, ch) in variant.withIndex()) {
                    if (i > 0 && ch.isUpperCase()) {
                        snake.append('_')
                    }
                    snake.append(ch.lowercaseChar())
                }
                snake.toString()
            }
            ScreamingSnakeCase -> SnakeCase.applyToVariant(variant).uppercase()
            KebabCase -> SnakeCase.applyToVariant(variant).replace('_', '-')
            ScreamingKebabCase -> ScreamingSnakeCase.applyToVariant(variant).replace('_', '-')
        }
    }

    public fun applyToField(field: String): String {
        return when (this) {
            None, LowerCase, SnakeCase -> field
            UpperCase -> field.uppercase()
            PascalCase -> {
                val pascal = StringBuilder()
                var capitalize = true
                for (ch in field) {
                    if (ch == '_') {
                        capitalize = true
                    } else if (capitalize) {
                        pascal.append(ch.uppercaseChar())
                        capitalize = false
                    } else {
                        pascal.append(ch)
                    }
                }
                pascal.toString()
            }
            CamelCase -> {
                val pascal = PascalCase.applyToField(field)
                if (pascal.isEmpty()) pascal
                else pascal[0].lowercase() + pascal.substring(1)
            }
            ScreamingSnakeCase -> field.uppercase()
            KebabCase -> field.replace('_', '-')
            ScreamingKebabCase -> ScreamingSnakeCase.applyToField(field).replace('_', '-')
        }
    }

    public fun or(ruleB: RenameRule): RenameRule {
        return if (this == None) ruleB else this
    }

    public companion object {
        public fun fromStr(renameAllStr: String): RenameRule {
            return when (renameAllStr) {
                "lowercase" -> LowerCase
                "UPPERCASE" -> UpperCase
                "PascalCase" -> PascalCase
                "camelCase" -> CamelCase
                "snake_case" -> SnakeCase
                "SCREAMING_SNAKE_CASE" -> ScreamingSnakeCase
                "kebab-case" -> KebabCase
                "SCREAMING-KEBAB-CASE" -> ScreamingKebabCase
                else -> throw ParseError(renameAllStr)
            }
        }
    }
}

public class ParseError(public val unknown: String) : Exception(
    "unknown rename rule `rename_all = \"$unknown\"`, expected one of \"lowercase\", \"UPPERCASE\", \"PascalCase\", \"camelCase\", \"snake_case\", \"SCREAMING_SNAKE_CASE\", \"kebab-case\", \"SCREAMING-KEBAB-CASE\""
)