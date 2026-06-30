
package com.waglo.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.waglo.app.database.DatabaseHelper
import com.waglo.app.model.GroupLink
import com.waglo.app.repository.GroupLinkRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GroupListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GroupLinkRepository(app)

    private val _groups = MutableLiveData<List<GroupLink>>(emptyList())
    val groups: LiveData<List<GroupLink>> = _groups

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    // Current filter state
    var sortBy: String = DatabaseHelper.COL_ADDED_AT
    var filterCategory: String? = null
    var filterLanguage: String? = null
    var filterStatus: String?   = null
    var onlyFavorites: Boolean  = false
    var dateFrom: Long?         = null
    var dateTo: Long?           = null

    private var searchJob: Job? = null
    private var currentQuery: String = ""

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _groups.value = if (currentQuery.isBlank()) {
                repo.getAll(
                    sortBy = sortBy,
                    filterCategory = filterCategory,
                    filterLanguage = filterLanguage,
                    filterStatus   = filterStatus,
                    onlyFavorites  = onlyFavorites,
                    dateFrom       = dateFrom,
                    dateTo         = dateTo
                )
            } else {
                repo.search(currentQuery)
            }
            _loading.value = false
        }
    }

    /** Debounced search — waits 300ms after last keystroke. */
    fun search(query: String) {
        currentQuery = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            load()
        }
    }

    fun clearSearch() {
        currentQuery = ""
        load()
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch {
            repo.deleteGroup(id)
            load()
        }
    }

    fun deleteDuplicates() {
        viewModelScope.launch {
            _loading.value = true
            repo.deleteDuplicates()
            load()
            _message.value = "Duplicates removed"
        }
    }

    fun toggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch {
            repo.toggleFavorite(id, !current)
            load()
        }
    }

    fun updateNotes(id: Long, notes: String) {
        viewModelScope.launch {
            repo.updateNotes(id, notes)
            load()
        }
    }

    fun clearMessage() { _message.value = null }
}
