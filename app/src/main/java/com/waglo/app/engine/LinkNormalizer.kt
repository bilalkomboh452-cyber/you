
package com.waglo.app.engine

/**
 * Normalises WhatsApp invite links to a canonical form:
 *   https://chat.whatsapp.com/{CODE}
 * Handles http://, www., trailing slashes, query params, mixed case.
 */
object LinkNormalizer {

    private val WA_HOST_REGEX = Regex(
        """(?:https?://)?(?:www\.)?chat\.whatsapp\.com/([A-Za-z0-9]{10,})"""
    )
    private const val CANONICAL_BASE = "https://chat.whatsapp.com/"

    /**
     * Returns canonical link or null if input is not a valid WA invite link.
     */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        val match = WA_HOST_REGEX.find(trimmed) ?: return null
        val code = match.groupValues[1]
        // WhatsApp codes are case-sensitive — preserve original case.
        return CANONICAL_BASE + code
    }

    /** Normalise a batch, returning only valid canonical links. */
    fun normalizeBatch(raws: List<String>): List<String> =
        raws.mapNotNull { normalize(it) }
}
