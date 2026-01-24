package com.aegis.vault.ui.viewmodel

import androidx.lifecycle.*
import com.aegis.vault.data.local.VaultEntity
import com.aegis.vault.data.repository.VaultRepository
import com.aegis.vault.security.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModel(private val repository: VaultRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    var masterKey: ByteArray? = null

    val allEntries: StateFlow<List<VaultEntity>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pagingEntries: Flow<PagingData<VaultEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.getAllEntriesPaging()
            } else {
                repository.searchEntriesPaging(query)
            }
        }
        .cachedIn(viewModelScope)

    val entries: StateFlow<List<VaultEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allEntries
            } else {
                repository.search(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun addEntry(entry: VaultEntity) {
        viewModelScope.launch {
            repository.insert(entry)
        }
    }

    fun updateEntry(entry: VaultEntity) {
        viewModelScope.launch {
            repository.update(entry)
        }
    }

    fun deleteEntry(entry: VaultEntity) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }

    fun deleteAllEntries() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun clearMasterKey() {
        SecurityUtils.wipeData(masterKey)
        masterKey = null
    }

    override fun onCleared() {
        super.onCleared()
        clearMasterKey()
    }
}

class VaultViewModelFactory(private val repository: VaultRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VaultViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
