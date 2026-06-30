
package com.waglo.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.waglo.app.database.DatabaseHelper.Companion.COL_ADDED_AT

object PrefsHelper {

    private const val PREFS_NAME = "waglo_prefs"
    private const val KEY_SORT_BY      = "sort_by"
    private const val KEY_DARK_MODE    = "dark_mode"
    private const val KEY_LANGUAGE     = "language"
    private const val KEY_FILTER_CAT   = "filter_category"
    private const val KEY_FILTER_LANG  = "filter_language"
    private const val KEY_SHOW_FAVS    = "show_favorites"
    private const val KEY_AUTO_CLASSIFY= "auto_classify"
    private const val KEY_FIRST_RUN    = "first_run"
    private const val KEY_TOTAL_FOUND  = "total_found_session"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSortBy(ctx: Context): String  = prefs(ctx).getString(KEY_SORT_BY, COL_ADDED_AT) ?: COL_ADDED_AT
    fun setSortBy(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_SORT_BY, v).apply()

    fun isDarkMode(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_DARK_MODE, false)
    fun setDarkMode(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_DARK_MODE, v).apply()

    fun getLanguage(ctx: Context): String = prefs(ctx).getString(KEY_LANGUAGE, "en") ?: "en"
    fun setLanguage(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_LANGUAGE, v).apply()

    fun getFilterCategory(ctx: Context): String? = prefs(ctx).getString(KEY_FILTER_CAT, null)
    fun setFilterCategory(ctx: Context, v: String?) = prefs(ctx).edit().putString(KEY_FILTER_CAT, v).apply()

    fun getFilterLanguage(ctx: Context): String? = prefs(ctx).getString(KEY_FILTER_LANG, null)
    fun setFilterLanguage(ctx: Context, v: String?) = prefs(ctx).edit().putString(KEY_FILTER_LANG, v).apply()

    fun isShowFavorites(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_SHOW_FAVS, false)
    fun setShowFavorites(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_SHOW_FAVS, v).apply()

    fun isAutoClassify(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_AUTO_CLASSIFY, true)
    fun setAutoClassify(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_AUTO_CLASSIFY, v).apply()

    fun isFirstRun(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_FIRST_RUN, true)
    fun setFirstRun(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_FIRST_RUN, v).apply()

    fun getTotalFoundSession(ctx: Context): Int = prefs(ctx).getInt(KEY_TOTAL_FOUND, 0)
    fun addTotalFoundSession(ctx: Context, count: Int) {
        val cur = getTotalFoundSession(ctx)
        prefs(ctx).edit().putInt(KEY_TOTAL_FOUND, cur + count).apply()
    }
    fun resetTotalFoundSession(ctx: Context) = prefs(ctx).edit().putInt(KEY_TOTAL_FOUND, 0).apply()
}
