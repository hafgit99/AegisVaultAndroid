package com.aegis.vault.data.repository

import com.aegis.vault.data.local.VaultDao
import com.aegis.vault.data.local.VaultEntity
import kotlinx.coroutines.flow.Flow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData

class VaultRepository(private val vaultDao: VaultDao) {

    val allEntries: Flow<List<VaultEntity>> = vaultDao.getAllEntries()

    suspend fun insert(entry: VaultEntity) {
        vaultDao.insertEntry(entry)
    }

    suspend fun update(entry: VaultEntity) {
        vaultDao.updateEntry(entry)
    }

    suspend fun delete(entry: VaultEntity) {
        vaultDao.deleteEntry(entry)
    }

    suspend fun deleteAll() {
        vaultDao.deleteAll()
    }

    fun search(query: String): Flow<List<VaultEntity>> {
        return vaultDao.searchEntries(query)
    }

    suspend fun getById(id: String): VaultEntity? {
        return vaultDao.getEntryById(id)
    }

    fun getAllEntriesPaging(): Flow<PagingData<VaultEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { vaultDao.getAllPaging() }
        ).flow
    }

    fun searchEntriesPaging(query: String): Flow<PagingData<VaultEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { vaultDao.searchPaging(query) }
        ).flow
    }
}
