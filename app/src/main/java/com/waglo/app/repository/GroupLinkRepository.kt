
package com.waglo.app.repository

import android.content.Context
import com.waglo.app.database.DatabaseHelper
import com.waglo.app.engine.DuplicateDetector
import com.waglo.app.engine.LinkNormalizer
import com.waglo.app.engine.LinkParser
import com.waglo.app.engine.LinkScoringEngine
import com.waglo.app.engine.LinkValidator
import com.waglo.app.model.GroupLink
import com.waglo.app.utils.Classifier
import com.waglo.app.utils.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single source of truth for all [GroupLink] data.
 * All database I/O is dispatched to [Dispatchers.IO].
 */
class GroupLinkRepository(context: Context) {

    private val db = DatabaseHelper.getInstance(context)
    private val appContext = context.applicationContext

    // -----------------------------------------------------------------------
    // INSERT
    // -----------------------------------------------------------------------

    /**
     * Insert a single link after normalisation, validation, classification,
     * and scoring. Returns the new row id, or -1 if duplicate / invalid.
     */
    suspend fun insertLink(rawLink: String, source: String, groupName: String = ""): Long =
        withContext(Dispatchers.IO) {
            val canonical = LinkNormalizer.normalize(rawLink) ?: return@withContext -1L
            if (!LinkValidator.isValid(canonical)) return@withContext -1L

            val category = Classifier.classify(groupName)
            val group = GroupLink(
                inviteLink      = canonical,
                groupName       = groupName,
                source          = source,
                category        = category,
                discoveryTime   = System.currentTimeMillis()
            )
            group.confidenceScore = LinkScoringEngine.score(group)
            db.insertGroup(group)
        }

    /**
     * Insert a pre-built [GroupLink], skipping invalid links.
     */
    suspend fun insertGroup(group: GroupLink): Long =
        withContext(Dispatchers.IO) {
            if (!LinkValidator.isValid(group.inviteLink)) return@withContext -1L
            db.insertGroup(group)
        }

    /**
     * Batch-insert a list, deduplicating against existing DB links.
     * Returns count of newly inserted rows.
     */
    suspend fun insertBatch(groups: List<GroupLink>): Int =
        withContext(Dispatchers.IO) {
            val existingLinks = db.getAllInviteLinks()
            val newGroups = DuplicateDetector.filterNew(groups, existingLinks)
            val scored    = LinkScoringEngine.scoreAndSort(newGroups)
            db.insertBatch(scored)
        }

    // -----------------------------------------------------------------------
    // READ
    // -----------------------------------------------------------------------

    suspend fun getAll(
        sortBy: String = DatabaseHelper.COL_ADDED_AT,
        filterCategory: String? = null,
        filterLanguage: String? = null,
        filterStatus: String? = null,
        onlyFavorites: Boolean = false,
        dateFrom: Long? = null,
        dateTo: Long? = null,
        limit: Int = -1,
        offset: Int = 0
    ): List<GroupLink> = withContext(Dispatchers.IO) {
        db.getAllGroups(
            sortBy = sortBy,
            filterCategory = filterCategory,
            filterLanguage = filterLanguage,
            filterStatus = filterStatus,
            onlyFavorites = onlyFavorites,
            dateFrom = dateFrom,
            dateTo = dateTo,
            limit = limit,
            offset = offset
        )
    }

    suspend fun search(query: String): List<GroupLink> =
        withContext(Dispatchers.IO) { db.searchGroups(query) }

    suspend fun getFavorites(): List<GroupLink> =
        withContext(Dispatchers.IO) { db.getFavorites() }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    suspend fun updateNotes(id: Long, notes: String) =
        withContext(Dispatchers.IO) { db.updateNotes(id, notes) }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        withContext(Dispatchers.IO) { db.toggleFavorite(id, isFavorite) }

    suspend fun updateGroup(group: GroupLink) =
        withContext(Dispatchers.IO) { db.updateGroup(group) }

    // -----------------------------------------------------------------------
    // DELETE
    // -----------------------------------------------------------------------

    suspend fun deleteGroup(id: Long) =
        withContext(Dispatchers.IO) { db.deleteGroup(id) }

    suspend fun deleteDuplicates() =
        withContext(Dispatchers.IO) { db.deleteAllDuplicates() }

    suspend fun deleteAll() =
        withContext(Dispatchers.IO) { db.deleteAll() }

    // -----------------------------------------------------------------------
    // STATS
    // -----------------------------------------------------------------------

    suspend fun getStats(): DashboardStats = withContext(Dispatchers.IO) {
        DashboardStats(
            total       = db.getTotalCount(),
            active      = db.getActiveCount(),
            favorites   = db.getFavoriteCount(),
            duplicates  = db.getDuplicateCount(),
            byCategoryMap = db.getCategoryStats(),
            bySource    = db.getSourceStats()
        )
    }

    // -----------------------------------------------------------------------
    // DISCOVERY: parse raw text / HTML and batch-insert
    // -----------------------------------------------------------------------

    suspend fun discoverFromText(
        text: String,
        source: String = GroupLink.Source.DISCOVERY,
        isHtml: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        val validLinks = LinkParser.extractValid(text, isHtml)
        if (validLinks.isEmpty()) return@withContext 0
        val groups = validLinks.map { link ->
            GroupLink(
                inviteLink    = link,
                source        = source,
                category      = GroupLink.Category.OTHERS,
                discoveryTime = System.currentTimeMillis()
            )
        }
        insertBatch(groups)
    }
}

data class DashboardStats(
    val total: Int,
    val active: Int,
    val favorites: Int,
    val duplicates: Int,
    val byCategoryMap: Map<String, Int>,
    val bySource: Map<String, Int>
)
