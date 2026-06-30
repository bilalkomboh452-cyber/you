
package com.waglo.app.engine

import com.waglo.app.model.GroupLink

/**
 * Computes a confidence score [0.0, 1.0] for a [GroupLink].
 *
 * Score factors:
 *  - Valid canonical link (+0.4)
 *  - Group name present (+0.2)
 *  - Category classified (not OTHERS) (+0.2)
 *  - Source is accessibility/discovery (more trusted) (+0.1)
 *  - Recency: discovered < 7 days ago (+0.1)
 */
object LinkScoringEngine {

    private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000

    fun score(link: GroupLink): Float {
        var score = 0f

        // Valid link
        if (LinkValidator.isValid(link.inviteLink)) score += 0.4f

        // Has meaningful group name
        if (link.groupName.isNotBlank() && link.groupName.length > 2) score += 0.2f

        // Classified beyond OTHERS
        if (link.category != GroupLink.Category.OTHERS) score += 0.2f

        // Trusted source
        if (link.source in setOf(GroupLink.Source.ACCESSIBILITY, GroupLink.Source.DISCOVERY)) score += 0.1f

        // Recency
        val ageMs = System.currentTimeMillis() - link.discoveryTime
        if (ageMs < SEVEN_DAYS_MS) score += 0.1f

        return score.coerceIn(0f, 1f)
    }

    /** Score and sort a list descending by confidence. */
    fun scoreAndSort(links: List<GroupLink>): List<GroupLink> {
        return links.map { it.apply { confidenceScore = score(it) } }
            .sortedByDescending { it.confidenceScore }
    }
}
