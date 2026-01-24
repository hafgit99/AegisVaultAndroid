package com.aegis.vault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_entries ORDER BY lastModified DESC")
    fun getAllEntries(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_entries WHERE id = :id")
    suspend fun getEntryById(id: String): VaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: VaultEntity)

    @Update
    suspend fun updateEntry(entry: VaultEntity)

    @Delete
    suspend fun deleteEntry(entry: VaultEntity)

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM vault_entries WHERE title LIKE '%' || :query || '%' OR website LIKE '%' || :query || '%'")
    fun searchEntries(query: String): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_entries ORDER BY lastModified DESC")
    fun getAllPaging(): PagingSource<Int, VaultEntity>

    @Query("SELECT * FROM vault_entries WHERE title LIKE '%' || :query || '%' OR website LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun searchPaging(query: String): PagingSource<Int, VaultEntity>
}
