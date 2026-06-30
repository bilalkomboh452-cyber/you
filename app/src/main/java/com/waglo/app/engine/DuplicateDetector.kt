
package com.waglo.app.engine

import com.waglo.app.model.GroupLink

/**
 * Detects and removes duplicate [GroupLink] objects based on their
 * normalised invite link. Also cross-checks against an existing set
 * of known links (e.g., those already in the database).
 */
object DuplicateDetector {

    /**
     * Deduplicate a list, preserving the first occurrence of each link.
     */
    fun deduplicate(links: List<GroupLink>): List<GroupLink> {
        val seen = HashSet<String>(links.size * 2)
        return links.filter { link ->
            val key = link.inviteLink.trim().lowercase()
            seen.add(key)
        }
    }

    /**
     * Filter out links that already exist in [existingLinks].
     */
    fun filterNew(
        candidates: List<GroupLink>,
        existingLinks: Set<String>
    ): List<GroupLink> {
        val normalised = existingLinks.map { it.trim().lowercase() }.toHashSet()
        return candidates.filter { candidate ->
            candidate.inviteLink.trim().lowercase() !in normalised
        }
    }

    /**
     * Returns the set of duplicate links in a list (links that appear more than once).
     */
    fun findDuplicates(links: List<GroupLink>): Set<String> {
        val seen = HashSet<String>()
        val duplicates = HashSet<String>()
        for (link in links) {
            val key = link.inviteLink.trim().lowercase()
            if (!seen.add(key)) duplicates.add(key)
        }
        return duplicates
    }
}
