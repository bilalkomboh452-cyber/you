
package com.waglo.app.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.waglo.app.model.GroupLink

/**
 * SQLite helper. Version 2 adds:
 *  - source, language, confidence_score, status, is_favorite, discovery_time columns
 *  - additional indexes for fast filtering
 *  - full-text-search helper via LIKE with indexes on name + link
 *  - pagination support
 *  - per-category statistics
 */
class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DatabaseHelper"
        const val DATABASE_NAME = "waglo.db"
        const val DATABASE_VERSION = 2
        const val TABLE_GROUPS = "group_links"

        // Columns
        const val COL_ID             = "id"
        const val COL_SERIAL         = "serial_number"
        const val COL_GROUP_NAME     = "group_name"
        const val COL_INVITE_LINK    = "invite_link"
        const val COL_CHAT_NAME      = "chat_name"
        const val COL_MESSAGE_DATE   = "message_date"
        const val COL_CATEGORY       = "category"
        const val COL_SOURCE         = "source"
        const val COL_LANGUAGE       = "language"
        const val COL_CONFIDENCE     = "confidence_score"
        const val COL_STATUS         = "status"
        const val COL_IS_FAVORITE    = "is_favorite"
        const val COL_NOTES          = "notes"
        const val COL_DISCOVERY_TIME = "discovery_time"
        const val COL_ADDED_AT       = "added_at"

        private val ALLOWED_SORT_COLS = setOf(
            COL_SERIAL, COL_CATEGORY, COL_ADDED_AT, COL_GROUP_NAME,
            COL_CONFIDENCE, COL_DISCOVERY_TIME, COL_STATUS
        )

        @Volatile private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseHelper(context).also { INSTANCE = it }
            }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()            // WAL: better concurrency on low-end hardware
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_GROUPS (
                $COL_ID             INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SERIAL         INTEGER,
                $COL_GROUP_NAME     TEXT NOT NULL DEFAULT '',
                $COL_INVITE_LINK    TEXT NOT NULL UNIQUE,
                $COL_CHAT_NAME      TEXT DEFAULT '',
                $COL_MESSAGE_DATE   TEXT DEFAULT '',
                $COL_CATEGORY       TEXT DEFAULT '${GroupLink.Category.OTHERS}',
                $COL_SOURCE         TEXT DEFAULT '${GroupLink.Source.ACCESSIBILITY}',
                $COL_LANGUAGE       TEXT DEFAULT '${GroupLink.Language.UNKNOWN}',
                $COL_CONFIDENCE     REAL DEFAULT 0.0,
                $COL_STATUS         TEXT DEFAULT '${GroupLink.Status.ACTIVE}',
                $COL_IS_FAVORITE    INTEGER DEFAULT 0,
                $COL_NOTES          TEXT DEFAULT '',
                $COL_DISCOVERY_TIME INTEGER,
                $COL_ADDED_AT       INTEGER
            )
        """)
        createIndexes(db)
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_category ON $TABLE_GROUPS($COL_CATEGORY)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_invite_link ON $TABLE_GROUPS($COL_INVITE_LINK)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_added_at ON $TABLE_GROUPS($COL_ADDED_AT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_status ON $TABLE_GROUPS($COL_STATUS)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_language ON $TABLE_GROUPS($COL_LANGUAGE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_favorite ON $TABLE_GROUPS($COL_IS_FAVORITE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_source ON $TABLE_GROUPS($COL_SOURCE)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Migrate v1 → v2: add new columns with safe defaults.
            val newCols = listOf(
                "ALTER TABLE $TABLE_GROUPS ADD COLUMN $COL_SOURCE TEXT DEFAULT '${GroupLink.Source.ACCESSIBILITY}'",
                "ALTER TABLE $TABLE_GROUPS ADD COLUMN $COL_LANGUAGE TEXT DEFAULT '${GroupLink.Language.UNKNOWN}'",
                "ALTER TABLE $TABLE_GROUPS ADD COLUMN $COL_CONFIDENCE REAL DEFAULT 0.0",
                "ALTER TABLE $TABLE_GROUPS ADD COLUMN $COL_STATUS TEXT DEFAULT '${GroupLink.Status.ACTIVE}'",
                "ALTER TABLE $TABLE_GROUPS ADD COLUMN $COL_IS_FAVORITE INTEGER DEFAULT 0",
                "ALTER TABLE $TABLE_GROUPS ADD COLUMN $COL_DISCOVERY_TIME INTEGER DEFAULT 0"
            )
            for (sql in newCols) {
                try { db.execSQL(sql) } catch (e: Exception) {
                    Log.w(TAG, "Migration column may already exist: ${e.message}")
                }
            }
            createIndexes(db)
        }
    }

    // -----------------------------------------------------------------------
    // WRITE OPERATIONS
    // -----------------------------------------------------------------------

    fun insertGroup(group: GroupLink): Long {
        return try {
            val nextSerial = getNextSerial()
            val now = System.currentTimeMillis()
            val values = ContentValues().apply {
                put(COL_SERIAL,         nextSerial)
                put(COL_GROUP_NAME,     group.groupName)
                put(COL_INVITE_LINK,    group.inviteLink)
                put(COL_CHAT_NAME,      group.chatName)
                put(COL_MESSAGE_DATE,   group.messageDate)
                put(COL_CATEGORY,       group.category)
                put(COL_SOURCE,         group.source)
                put(COL_LANGUAGE,       group.language)
                put(COL_CONFIDENCE,     group.confidenceScore)
                put(COL_STATUS,         group.status)
                put(COL_IS_FAVORITE,    if (group.isFavorite) 1 else 0)
                put(COL_NOTES,          group.notes)
                put(COL_DISCOVERY_TIME, if (group.discoveryTime > 0) group.discoveryTime else now)
                put(COL_ADDED_AT,       now)
            }
            writableDatabase.insertWithOnConflict(
                TABLE_GROUPS, null, values, SQLiteDatabase.CONFLICT_IGNORE
            )
        } catch (e: Exception) {
            Log.e(TAG, "insertGroup failed", e)
            -1L
        }
    }

    fun insertBatch(groups: List<GroupLink>): Int {
        var inserted = 0
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (group in groups) {
                if (insertGroupInternal(db, group) > 0) inserted++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return inserted
    }

    private fun insertGroupInternal(db: SQLiteDatabase, group: GroupLink): Long {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COL_SERIAL,         getNextSerial())
            put(COL_GROUP_NAME,     group.groupName)
            put(COL_INVITE_LINK,    group.inviteLink)
            put(COL_CHAT_NAME,      group.chatName)
            put(COL_MESSAGE_DATE,   group.messageDate)
            put(COL_CATEGORY,       group.category)
            put(COL_SOURCE,         group.source)
            put(COL_LANGUAGE,       group.language)
            put(COL_CONFIDENCE,     group.confidenceScore)
            put(COL_STATUS,         group.status)
            put(COL_IS_FAVORITE,    if (group.isFavorite) 1 else 0)
            put(COL_NOTES,          group.notes)
            put(COL_DISCOVERY_TIME, if (group.discoveryTime > 0) group.discoveryTime else now)
            put(COL_ADDED_AT,       now)
        }
        return db.insertWithOnConflict(TABLE_GROUPS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun updateGroup(group: GroupLink): Boolean {
        return try {
            val values = ContentValues().apply {
                put(COL_GROUP_NAME,  group.groupName)
                put(COL_CATEGORY,    group.category)
                put(COL_NOTES,       group.notes)
                put(COL_STATUS,      group.status)
                put(COL_IS_FAVORITE, if (group.isFavorite) 1 else 0)
                put(COL_LANGUAGE,    group.language)
                put(COL_CONFIDENCE,  group.confidenceScore)
            }
            writableDatabase.update(
                TABLE_GROUPS, values, "$COL_ID=?", arrayOf(group.id.toString())
            ) > 0
        } catch (e: Exception) {
            Log.e(TAG, "updateGroup failed", e)
            false
        }
    }

    fun updateNotes(id: Long, notes: String) {
        try {
            val values = ContentValues().apply { put(COL_NOTES, notes) }
            writableDatabase.update(TABLE_GROUPS, values, "$COL_ID=?", arrayOf(id.toString()))
        } catch (e: Exception) {
            Log.e(TAG, "updateNotes failed", e)
        }
    }

    fun toggleFavorite(id: Long, isFavorite: Boolean) {
        try {
            val values = ContentValues().apply { put(COL_IS_FAVORITE, if (isFavorite) 1 else 0) }
            writableDatabase.update(TABLE_GROUPS, values, "$COL_ID=?", arrayOf(id.toString()))
        } catch (e: Exception) {
            Log.e(TAG, "toggleFavorite failed", e)
        }
    }

    fun markStatus(id: Long, status: String) {
        try {
            val values = ContentValues().apply { put(COL_STATUS, status) }
            writableDatabase.update(TABLE_GROUPS, values, "$COL_ID=?", arrayOf(id.toString()))
        } catch (e: Exception) {
            Log.e(TAG, "markStatus failed", e)
        }
    }

    fun deleteGroup(id: Long) {
        try {
            writableDatabase.delete(TABLE_GROUPS, "$COL_ID=?", arrayOf(id.toString()))
            reNumberSerials()
        } catch (e: Exception) {
            Log.e(TAG, "deleteGroup failed", e)
        }
    }

    fun deleteAllDuplicates() {
        try {
            writableDatabase.execSQL("""
                DELETE FROM $TABLE_GROUPS WHERE $COL_ID NOT IN (
                    SELECT MIN($COL_ID) FROM $TABLE_GROUPS GROUP BY $COL_INVITE_LINK
                )
            """)
            reNumberSerials()
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllDuplicates failed", e)
        }
    }

    fun deleteAll() {
        try {
            writableDatabase.delete(TABLE_GROUPS, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAll failed", e)
        }
    }

    // -----------------------------------------------------------------------
    // READ OPERATIONS
    // -----------------------------------------------------------------------

    /**
     * Full-featured query with optional filters, sorting, and pagination.
     * All parameters default to "no filter / no limit".
     */
    fun getAllGroups(
        sortBy: String = COL_ADDED_AT,
        filterCategory: String? = null,
        filterLanguage: String? = null,
        filterSource: String? = null,
        filterStatus: String? = null,
        onlyFavorites: Boolean = false,
        dateFrom: Long? = null,
        dateTo: Long? = null,
        limit: Int = -1,
        offset: Int = 0
    ): List<GroupLink> {
        val safeSortBy = sanitizeSortColumn(sortBy)
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()
        filterCategory?.let { conditions += "$COL_CATEGORY=?"; args += it }
        filterLanguage?.let  { conditions += "$COL_LANGUAGE=?";  args += it }
        filterSource?.let    { conditions += "$COL_SOURCE=?";    args += it }
        filterStatus?.let    { conditions += "$COL_STATUS=?";    args += it }
        if (onlyFavorites)   { conditions += "$COL_IS_FAVORITE=1" }
        dateFrom?.let { conditions += "$COL_ADDED_AT>=?"; args += it.toString() }
        dateTo?.let   { conditions += "$COL_ADDED_AT<=?"; args += it.toString() }

        val where = if (conditions.isEmpty()) null else conditions.joinToString(" AND ")
        val limitStr = if (limit > 0) limit.toString() else null
        val offsetStr = if (limit > 0 && offset > 0) offset.toString() else null

        return try {
            readableDatabase.query(
                TABLE_GROUPS, null, where,
                if (args.isEmpty()) null else args.toTypedArray(),
                null, null, "$safeSortBy DESC",
                if (limitStr != null && offsetStr != null) "$limitStr OFFSET $offsetStr"
                else limitStr
            ).use { cursorToList(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getAllGroups failed", e)
            emptyList()
        }
    }

    fun searchGroups(query: String): List<GroupLink> {
        val q = "%${query.trim()}%"
        return try {
            readableDatabase.query(
                TABLE_GROUPS, null,
                "$COL_GROUP_NAME LIKE ? OR $COL_CHAT_NAME LIKE ? OR $COL_INVITE_LINK LIKE ? OR $COL_NOTES LIKE ?",
                arrayOf(q, q, q, q), null, null, "$COL_ADDED_AT DESC"
            ).use { cursorToList(it) }
        } catch (e: Exception) {
            Log.e(TAG, "searchGroups failed", e)
            emptyList()
        }
    }

    fun getAllInviteLinks(): Set<String> {
        return try {
            readableDatabase.query(
                TABLE_GROUPS, arrayOf(COL_INVITE_LINK),
                null, null, null, null, null
            ).use { cursor ->
                val set = HashSet<String>(cursor.count * 2)
                while (cursor.moveToNext()) set.add(cursor.getString(0))
                set
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAllInviteLinks failed", e)
            emptySet()
        }
    }

    fun getFavorites(): List<GroupLink> =
        getAllGroups(onlyFavorites = true)

    // -----------------------------------------------------------------------
    // STATISTICS
    // -----------------------------------------------------------------------

    fun getTotalCount(): Int = count()
    fun getActiveCount(): Int = count("$COL_STATUS='${GroupLink.Status.ACTIVE}'")
    fun getFavoriteCount(): Int = count("$COL_IS_FAVORITE=1")
    fun getDuplicateCount(): Int = countRaw(
        "SELECT COUNT(*) FROM $TABLE_GROUPS WHERE $COL_INVITE_LINK IN " +
        "(SELECT $COL_INVITE_LINK FROM $TABLE_GROUPS GROUP BY $COL_INVITE_LINK HAVING COUNT(*)>1)"
    )

    fun getCategoryStats(): Map<String, Int> {
        val result = LinkedHashMap<String, Int>()
        GroupLink.Category.ALL.forEach { result[it] = 0 }
        return try {
            readableDatabase.rawQuery(
                "SELECT $COL_CATEGORY, COUNT(*) FROM $TABLE_GROUPS GROUP BY $COL_CATEGORY", null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val cat = cursor.getString(0) ?: GroupLink.Category.OTHERS
                    result[cat] = cursor.getInt(1)
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "getCategoryStats failed", e)
            result
        }
    }

    fun getSourceStats(): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        return try {
            readableDatabase.rawQuery(
                "SELECT $COL_SOURCE, COUNT(*) FROM $TABLE_GROUPS GROUP BY $COL_SOURCE", null
            ).use { cursor ->
                while (cursor.moveToNext()) result[cursor.getString(0) ?: "UNKNOWN"] = cursor.getInt(1)
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "getSourceStats failed", e); result
        }
    }

    // Legacy helpers kept for backward compatibility with MainActivity
    fun getNewsCount(): Int   = count("$COL_CATEGORY='${GroupLink.Category.NEWS}'")
    fun getOtherCount(): Int  = count("$COL_CATEGORY='${GroupLink.Category.OTHERS}'")

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    private fun count(where: String? = null): Int {
        val sql = if (where != null)
            "SELECT COUNT(*) FROM $TABLE_GROUPS WHERE $where"
        else
            "SELECT COUNT(*) FROM $TABLE_GROUPS"
        return countRaw(sql)
    }

    private fun countRaw(sql: String): Int {
        return try {
            readableDatabase.rawQuery(sql, null).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "countRaw failed: $sql", e); 0
        }
    }

    private fun getNextSerial(): Int {
        return try {
            readableDatabase.rawQuery(
                "SELECT MAX($COL_SERIAL) FROM $TABLE_GROUPS", null
            ).use {
                if (it.moveToFirst() && !it.isNull(0)) it.getInt(0) + 1 else 1
            }
        } catch (e: Exception) {
            Log.e(TAG, "getNextSerial failed", e); 1
        }
    }

    private fun reNumberSerials() {
        try {
            val db = writableDatabase
            db.beginTransaction()
            try {
                db.query(TABLE_GROUPS, arrayOf(COL_ID), null, null, null, null, COL_ADDED_AT)
                    .use { cursor ->
                        var serial = 1
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(0)
                            val cv = ContentValues().apply { put(COL_SERIAL, serial++) }
                            db.update(TABLE_GROUPS, cv, "$COL_ID=?", arrayOf(id.toString()))
                        }
                    }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "reNumberSerials failed", e)
        }
    }

    private fun sanitizeSortColumn(col: String): String =
        if (col in ALLOWED_SORT_COLS) col else COL_ADDED_AT

    private fun cursorToList(cursor: Cursor): List<GroupLink> {
        val list = ArrayList<GroupLink>(cursor.count)
        val iId   = cursor.getColumnIndexOrThrow(COL_ID)
        val iSer  = cursor.getColumnIndexOrThrow(COL_SERIAL)
        val iName = cursor.getColumnIndexOrThrow(COL_GROUP_NAME)
        val iLink = cursor.getColumnIndexOrThrow(COL_INVITE_LINK)
        val iChat = cursor.getColumnIndexOrThrow(COL_CHAT_NAME)
        val iDate = cursor.getColumnIndexOrThrow(COL_MESSAGE_DATE)
        val iCat  = cursor.getColumnIndexOrThrow(COL_CATEGORY)
        val iSrc  = cursor.getColumnIndex(COL_SOURCE).takeIf { it >= 0 }
        val iLang = cursor.getColumnIndex(COL_LANGUAGE).takeIf { it >= 0 }
        val iConf = cursor.getColumnIndex(COL_CONFIDENCE).takeIf { it >= 0 }
        val iStat = cursor.getColumnIndex(COL_STATUS).takeIf { it >= 0 }
        val iFav  = cursor.getColumnIndex(COL_IS_FAVORITE).takeIf { it >= 0 }
        val iNote = cursor.getColumnIndexOrThrow(COL_NOTES)
        val iDisc = cursor.getColumnIndex(COL_DISCOVERY_TIME).takeIf { it >= 0 }
        val iAdd  = cursor.getColumnIndexOrThrow(COL_ADDED_AT)
        while (cursor.moveToNext()) {
            list.add(GroupLink(
                id             = cursor.getLong(iId),
                serialNumber   = cursor.getInt(iSer),
                groupName      = cursor.getString(iName) ?: "",
                inviteLink     = cursor.getString(iLink) ?: "",
                chatName       = cursor.getString(iChat) ?: "",
                messageDate    = cursor.getString(iDate) ?: "",
                category       = cursor.getString(iCat) ?: GroupLink.Category.OTHERS,
                source         = iSrc?.let { cursor.getString(it) } ?: GroupLink.Source.ACCESSIBILITY,
                language       = iLang?.let { cursor.getString(it) } ?: GroupLink.Language.UNKNOWN,
                confidenceScore= iConf?.let { cursor.getFloat(it) } ?: 0f,
                status         = iStat?.let { cursor.getString(it) } ?: GroupLink.Status.ACTIVE,
                isFavorite     = iFav?.let { cursor.getInt(it) == 1 } ?: false,
                notes          = cursor.getString(iNote) ?: "",
                discoveryTime  = iDisc?.let { cursor.getLong(it) } ?: 0L,
                addedAt        = cursor.getLong(iAdd)
            ))
        }
        return list
    }
}
