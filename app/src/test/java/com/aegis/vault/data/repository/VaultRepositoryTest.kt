package com.aegis.vault.data.repository

import com.aegis.vault.data.local.VaultDao
import com.aegis.vault.data.local.VaultEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.mockito.kotlin.mock

class VaultRepositoryTest {

    private lateinit var mockVaultDao: VaultDao

    private lateinit var repository: VaultRepository

    private val testEntity = VaultEntity(
        id = "test-id-1",
        title = "Test Entry",
        username = "testuser",
        website = "https://example.com",
        encryptedPayload = "encrypted-data".toByteArray(),
        iv = "iv-data".toByteArray()
    )

    @Before
    fun setup() {
        mockVaultDao = mock()
        repository = VaultRepository(mockVaultDao)
    }

    @Test
    fun testAllEntries_returnsFlow() = runTest {
        val expectedEntries = listOf(testEntity)
        whenever(mockVaultDao.getAllEntries()).thenReturn(flowOf(expectedEntries))

        val entries = repository.allEntries.first()

        assertEquals("Should return all entries", expectedEntries, entries)
        verify { mockVaultDao.getAllEntries() }
    }

    @Test
    fun testInsert() = runTest {
        repository.insert(testEntity)

        coVerify { mockVaultDao.insertEntry(testEntity) }
    }

    @Test
    fun testUpdate() = runTest {
        repository.update(testEntity)

        coVerify { mockVaultDao.updateEntry(testEntity) }
    }

    @Test
    fun testDelete() = runTest {
        repository.delete(testEntity)

        coVerify { mockVaultDao.deleteEntry(testEntity) }
    }

    @Test
    fun testDeleteAll() = runTest {
        repository.deleteAll()

        coVerify { mockVaultDao.deleteAll() }
    }

    @Test
    fun testSearch() = runTest {
        val query = "test"
        val expectedResults = listOf(testEntity)
        whenever(mockVaultDao.searchEntries(query)).thenReturn(flowOf(expectedResults))

        val results = repository.search(query).first()

        assertEquals("Should return search results", expectedResults, results)
        verify { mockVaultDao.searchEntries(query) }
    }

    @Test
    fun testSearch_emptyQuery() = runTest {
        val query = ""
        val expectedResults = emptyList<VaultEntity>()
        whenever(mockVaultDao.searchEntries(query)).thenReturn(flowOf(expectedResults))

        val results = repository.search(query).first()

        assertEquals("Should handle empty query", expectedResults, results)
    }

    @Test
    fun testGetById_found() = runTest {
        whenever(mockVaultDao.getEntryById("test-id-1")).thenReturn(testEntity)

        val result = repository.getById("test-id-1")

        assertNotNull("Should return entry when found", result)
        assertEquals("Should return correct entry", testEntity, result)
        coVerify { mockVaultDao.getEntryById("test-id-1") }
    }

    @Test
    fun testGetById_notFound() = runTest {
        whenever(mockVaultDao.getEntryById("non-existent")).thenReturn(null)

        val result = repository.getById("non-existent")

        assertNull("Should return null when not found", result)
        coVerify { mockVaultDao.getEntryById("non-existent") }
    }

    @Test
    fun testGetAllEntriesPaging() {
        val pagingSource = mock<androidx.paging.PagingSource<Int, VaultEntity>>()
        whenever(mockVaultDao.getAllPaging()).thenReturn(pagingSource)

        repository.getAllEntriesPaging()

        verify { mockVaultDao.getAllPaging() }
    }

    @Test
    fun testSearchEntriesPaging() {
        val query = "search-term"
        val pagingSource = mock<androidx.paging.PagingSource<Int, VaultEntity>>()
        whenever(mockVaultDao.searchPaging(query)).thenReturn(pagingSource)

        repository.searchEntriesPaging(query)

        verify { mockVaultDao.searchPaging(query) }
    }
}
