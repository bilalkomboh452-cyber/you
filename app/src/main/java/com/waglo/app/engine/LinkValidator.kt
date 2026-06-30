
package com.waglo.app.engine

/**
 * Validates that a (possibly already normalised) invite link
 * conforms to the canonical WhatsApp invite URL structure.
 */
object LinkValidator {

    // WhatsApp invite codes are 20–22 alphanumeric characters.
    private val VALID_LINK_REGEX = Regex(
        """^https://chat\.whatsapp\.com/[A-Za-z0-9]{10,}$"""
    )

    fun isValid(link: String): Boolean = VALID_LINK_REGEX.matches(link.trim())

    /**
     * Returns a [ValidationResult] with the code if valid.
     */
    fun validate(link: String): ValidationResult {
        val trimmed = link.trim()
        if (trimmed.isEmpty()) return ValidationResult.Empty
        val normalized = LinkNormalizer.normalize(trimmed)
            ?: return ValidationResult.Invalid("Not a WhatsApp invite link")
        return if (VALID_LINK_REGEX.matches(normalized))
            ValidationResult.Valid(normalized)
        else
            ValidationResult.Invalid("Code too short or contains invalid characters")
    }

    sealed class ValidationResult {
        data class Valid(val canonicalLink: String) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
        object Empty : ValidationResult()
    }
}
