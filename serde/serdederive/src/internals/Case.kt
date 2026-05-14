// port-lint: source serde_derive/src/internals/case.rs
package io.github.kotlinmania.serde.serdederive.src.internals

/**
 * Code to convert declaration names such as `myField` or `MyType` to the case used in the
 * serialized source, for example `my-field` or `MY_FIELD`.
 */

/**
 * The different possible ways to change case of fields in a class, or variants in a sealed type.
 */
public enum class RenameRule {
    /**
     * Don't apply a default rename rule.
     */
    None,

    /**
     * Rename direct children to "lowercase" style.
     */
    LowerCase,

    /**
     * Rename direct children to "UPPERCASE" style.
     */
    UpperCase,

    /**
     * Rename direct children to "PascalCase" style, as typically used for enum variants.
     */
    PascalCase,

    /**
     * Rename direct children to "camelCase" style.
     */
    CamelCase,

    /**
     * Rename direct children to snake case, as commonly used for fields.
     */
    SnakeCase,

    /**
     * Rename direct children to screaming snake case, as commonly used for constants.
     */
    ScreamingSnakeCase,

    /**
     * Rename direct children to "kebab-case" style.
     */
    KebabCase,

    /**
     * Rename direct children to "SCREAMING-KEBAB-CASE" style.
     */
    ScreamingKebabCase,
    ;

    /**
     * Apply a renaming rule to an enum variant, returning the version expected in the source.
     */
    public fun applyToVariant(variant: String): String =
        when (this) {
            None,
            PascalCase,
            -> variant

            LowerCase -> variant.toAsciiLowercase()
            UpperCase -> variant.toAsciiUppercase()
            CamelCase -> variant.substring(0, 1).toAsciiLowercase() + variant.substring(1)
            SnakeCase -> buildString {
                for ((index, ch) in variant.withIndex()) {
                    if (index > 0 && ch.isAsciiUppercase()) {
                        append('_')
                    }
                    append(ch.toAsciiLowercase())
                }
            }

            ScreamingSnakeCase -> SnakeCase.applyToVariant(variant).toAsciiUppercase()
            KebabCase -> SnakeCase.applyToVariant(variant).replace('_', '-')
            ScreamingKebabCase -> ScreamingSnakeCase.applyToVariant(variant).replace('_', '-')
        }

    /**
     * Apply a renaming rule to a class field, returning the version expected in the source.
     */
    public fun applyToField(field: String): String =
        when (this) {
            None,
            LowerCase,
            SnakeCase,
            -> field

            UpperCase -> field.toAsciiUppercase()
            PascalCase -> buildString {
                var capitalize = true
                for (ch in field) {
                    if (ch == '_') {
                        capitalize = true
                    } else if (capitalize) {
                        append(ch.toAsciiUppercase())
                        capitalize = false
                    } else {
                        append(ch)
                    }
                }
            }

            CamelCase -> {
                val pascal = PascalCase.applyToField(field)
                pascal.substring(0, 1).toAsciiLowercase() + pascal.substring(1)
            }

            ScreamingSnakeCase -> field.toAsciiUppercase()
            KebabCase -> field.replace('_', '-')
            ScreamingKebabCase -> ScreamingSnakeCase.applyToField(field).replace('_', '-')
        }

    /**
     * Returns this `RenameRule` if it is not `None`, `ruleB` otherwise.
     */
    public fun or(ruleB: RenameRule): RenameRule =
        when (this) {
            None -> ruleB
            else -> this
        }

    public companion object {
        public fun fromStr(renameAllStr: String): Result<RenameRule> {
            for ((name, rule) in RENAME_RULES) {
                if (renameAllStr == name) {
                    return Result.success(rule)
                }
            }
            return Result.failure(ParseError(renameAllStr))
        }
    }
}

private val RENAME_RULES: List<Pair<String, RenameRule>> =
    listOf(
        "lowercase" to RenameRule.LowerCase,
        "UPPERCASE" to RenameRule.UpperCase,
        "PascalCase" to RenameRule.PascalCase,
        "camelCase" to RenameRule.CamelCase,
        "snake_case" to RenameRule.SnakeCase,
        "SCREAMING_SNAKE_CASE" to RenameRule.ScreamingSnakeCase,
        "kebab-case" to RenameRule.KebabCase,
        "SCREAMING-KEBAB-CASE" to RenameRule.ScreamingKebabCase,
    )

public class ParseError(
    public val unknown: String,
) : IllegalArgumentException() {
    override val message: String
        get() =
            buildString {
                append("unknown rename rule `rename_all = ")
                append(debugString(unknown))
                append("`, expected one of ")
                for ((index, pair) in RENAME_RULES.withIndex()) {
                    if (index > 0) {
                        append(", ")
                    }
                    append(debugString(pair.first))
                }
            }

    public fun fmt(formatter: Appendable): Result<Unit> =
        runCatching {
            formatter.append(message)
            Unit
        }

    override fun toString(): String = message
}

private fun String.toAsciiLowercase(): String =
    buildString(length) {
        for (ch in this@toAsciiLowercase) {
            append(ch.toAsciiLowercase())
        }
    }

private fun String.toAsciiUppercase(): String =
    buildString(length) {
        for (ch in this@toAsciiUppercase) {
            append(ch.toAsciiUppercase())
        }
    }

private fun Char.isAsciiUppercase(): Boolean = this in 'A'..'Z'

private fun Char.toAsciiLowercase(): Char =
    if (this in 'A'..'Z') {
        (code + ('a'.code - 'A'.code)).toChar()
    } else {
        this
    }

private fun Char.toAsciiUppercase(): Char =
    if (this in 'a'..'z') {
        (code - ('a'.code - 'A'.code)).toChar()
    } else {
        this
    }

private fun debugString(value: String): String =
    buildString {
        append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }
