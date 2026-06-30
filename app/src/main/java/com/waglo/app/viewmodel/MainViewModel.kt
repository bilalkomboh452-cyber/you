
package com.waglo.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.waglo.app.repository.DashboardStats
import com.waglo.app.repository.GroupLinkRepository
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GroupLinkRepository(app)

    private val _stats = MutableLiveData<DashboardStats>()
    val stats: LiveData<DashboardStats> = _stats

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun loadStats() {
        viewModelScope.launch {
            _loading.value = true
            _stats.value = repo.getStats()
            _loading.value = false
        }
    }
}
