
package com.waglo.app.engine

/**
 * Extracts WhatsApp invite links from arbitrary text (plain text or HTML).
 * Thread-safe: all state is local.
 */
object LinkParser {

    // Matches both raw and HTML-escaped variants of WhatsApp invite URLs.
    private val LINK_REGEX = Regex(
        """(?:https?://)?(?:www\.)?chat\.whatsapp\.com/[A-Za-z0-9]{10,}""",
        RegexOption.IGNORE_CASE
    )

    // Basic HTML tag stripping (no full parse needed for link extraction).
    private val HTML_TAG_REGEX = Regex("""<[^>]+>""")
    private val HTML_ENTITY_REGEX = Regex("""&amp;|&lt;|&gt;|&quot;|&#39;""")

    /**
     * Parse links from plain text.
     * Returns raw (un-normalised) links.
     */
    fun parseText(text: String): List<String> {
        return LINK_REGEX.findAll(text).map { it.value }.toList()
    }

    /**
     * Strip HTML tags first, then parse.
     */
    fun parseHtml(html: String): List<String> {
        var cleaned = html
            .replace(HTML_TAG_REGEX, " ")
            .replace(HTML_ENTITY_REGEX, " ")
        return parseText(cleaned)
    }

    /**
     * Parse, normalise, and validate in one step.
     * Returns only canonical valid links.
     */
    fun extractValid(text: String, isHtml: Boolean = false): List<String> {
        val raw = if (isHtml) parseHtml(text) else parseText(text)
        return raw
            .mapNotNull { LinkNormalizer.normalize(it) }
            .filter { LinkValidator.isValid(it) }
            .distinct()
    }
}
