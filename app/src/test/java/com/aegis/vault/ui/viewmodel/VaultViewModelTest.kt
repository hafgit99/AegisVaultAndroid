package com.aegis.vault.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.aegis.vault.data.local.VaultEntity
import com.aegis.vault.data.repository.VaultRepository
import com.aegis.vault.security.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class VaultViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockRepository: VaultRepository

    private lateinit var viewModel: VaultViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    private val testEntries = listOf(
        VaultEntity(
            id = "id-1",
            title = "Entry 1",
            username = "user1",
            website = "https://site1.com",
            encryptedPayload = "enc1".toByteArray(),
            iv = "iv1".toByteArray()
        ),
        VaultEntity(
            id = "id-2",
            title = "Entry 2",
            username = "user2",
            website = "https://site2.com",
            encryptedPayload = "enc2".toByteArray(),
            iv = "iv2".toByteArray()
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockRepository = mock()
        whenever(mockRepository.allEntries).thenReturn(flowOf(testEntries))
        whenever(mockRepository.getAllEntriesPaging()).thenReturn(flowOf())

        viewModel = VaultViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialSearchQuery_empty() {
        val query = viewModel.searchQuery.value

        assertEquals("Initial search query should be empty", "", query)
    }

    @Test
    fun testOnSearchQueryChange() {
        val newQuery = "test query"

        viewModel.onSearchQueryChange(newQuery)

        assertEquals("Search query should be updated", newQuery, viewModel.searchQuery.value)
    }

    @Test
    fun testOnSearchQueryChange_empty() {
        viewModel.onSearchQueryChange("non-empty")
        viewModel.onSearchQueryChange("")

        assertEquals("Search query should be empty", "", viewModel.searchQuery.value)
    }

    @Test
    fun testAddEntry() = runTest {
        val entry = testEntries[0]

        viewModel.addEntry(entry)

        coVerify { mockRepository.insert(entry) }
    }

    @Test
    fun testUpdateEntry() = runTest {
        val entry = testEntries[1]

        viewModel.updateEntry(entry)

        coVerify { mockRepository.update(entry) }
    }

    @Test
    fun testDeleteEntry() = runTest {
        val entry = testEntries[0]

        viewModel.deleteEntry(entry)

        coVerify { mockRepository.delete(entry) }
    }

    @Test
    fun testDeleteAllEntries() = runTest {
        viewModel.deleteAllEntries()

        coVerify { mockRepository.deleteAll() }
    }

    @Test
    fun testMasterKey_initiallyNull() {
        assertNull("Master key should be null initially", viewModel.masterKey)
    }

    @Test
    fun testMasterKey_canBeSet() {
        val testKey = ByteArray(32) { it.toByte() }

        viewModel.masterKey = testKey

        assertArrayEquals("Master key should be set", testKey, viewModel.masterKey)
    }

    @Test
    fun testClearMasterKey() {
        val testKey = ByteArray(32) { 0xFF.toByte() }
        viewModel.masterKey = testKey

        viewModel.clearMasterKey()

        assertNull("Master key should be cleared", viewModel.masterKey)

        assertTrue("Master key data should be wiped", testKey.all { it == 0.toByte() })
    }

    @Test
    fun testClearMasterKey_whenAlreadyNull() {
        viewModel.masterKey = null

        viewModel.clearMasterKey()

        assertNull("Should handle null master key", viewModel.masterKey)
    }

    @Test
    fun testAllEntries_flow() = runTest {
        val entries = viewModel.allEntries.value

        assertNotNull("All entries should not be null", entries)
        assertTrue("Should contain test entries", entries?.isNotEmpty() == true)
    }

    @Test
    fun testEntries_withEmptySearch() = runTest {
        val entries = viewModel.entries.value

        assertNotNull("Entries should not be null", entries)
    }

    @Test
    fun testPagingEntries_exists() {
        assertNotNull("Paging entries flow should exist", viewModel.pagingEntries)
    }

    @Test
    fun testAddMultipleEntries() = runTest {
        for (entry in testEntries) {
            viewModel.addEntry(entry)
        }

        coVerify(exactly = 2) { mockRepository.insert(any()) }
    }

    @Test
    fun testUpdateMultipleEntries() = runTest {
        for (entry in testEntries) {
            viewModel.updateEntry(entry)
        }

        coVerify(exactly = 2) { mockRepository.update(any()) }
    }

    @Test
    fun testSearchQueryChange_triggersSearch() = runTest {
        whenever(mockRepository.search("test")).thenReturn(flowOf(listOf(testEntries[0])))

        viewModel.onSearchQueryChange("test")

        coVerify { mockRepository.search("test") }
    }
}
