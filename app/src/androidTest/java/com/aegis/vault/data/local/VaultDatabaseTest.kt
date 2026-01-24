package com.aegis.vault.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class VaultDatabaseTest {

    private lateinit var db: VaultDatabase
    private lateinit var dao: VaultDao

    private val testEntity = VaultEntity(
        id = "test-id-1",
        title = "Test Entry",
        username = "testuser",
        website = "https://example.com",
        encryptedPayload = "encrypted-data".toByteArray(),
        iv = "iv-data".toByteArray(),
        totpSecret = "JBSWY3DPEHPK3PXP",
        lastModified = 123456789L
    )

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            VaultDatabase::class.java
        ).build()
        dao = db.vaultDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertEntry() = runTest {
        dao.insertEntry(testEntity)

        val retrieved = dao.getEntryById("test-id-1")

        assertNotNull("Entry should be retrieved", retrieved)
        assertEquals("ID should match", testEntity.id, retrieved?.id)
        assertEquals("Title should match", testEntity.title, retrieved?.title)
        assertEquals("Username should match", testEntity.username, retrieved?.username)
    }

    @Test
    fun testInsertMultipleEntries() = runTest {
        val entity1 = VaultEntity(
            id = "id-1",
            title = "Entry 1",
            username = "user1",
            website = "https://site1.com",
            encryptedPayload = "enc1".toByteArray(),
            iv = "iv1".toByteArray()
        )
        val entity2 = VaultEntity(
            id = "id-2",
            title = "Entry 2",
            username = "user2",
            website = "https://site2.com",
            encryptedPayload = "enc2".toByteArray(),
            iv = "iv2".toByteArray()
        )

        dao.insertEntry(entity1)
        dao.insertEntry(entity2)

        val allEntries = dao.getAllEntries().first()

        assertEquals("Should have 2 entries", 2, allEntries.size)
        assertTrue("Should contain entity1", allEntries.any { it.id == "id-1" })
        assertTrue("Should contain entity2", allEntries.any { it.id == "id-2" })
    }

    @Test
    fun testUpdateEntry() = runTest {
        dao.insertEntry(testEntity)

        val updatedEntity = testEntity.copy(
            title = "Updated Title",
            username = "updateduser",
            lastModified = 999999999L
        )
        dao.updateEntry(updatedEntity)

        val retrieved = dao.getEntryById("test-id-1")

        assertEquals("Title should be updated", "Updated Title", retrieved?.title)
        assertEquals("Username should be updated", "updateduser", retrieved?.username)
        assertEquals("Last modified should be updated", 999999999L, retrieved?.lastModified)
    }

    @Test
    fun testDeleteEntry() = runTest {
        dao.insertEntry(testEntity)

        dao.deleteEntry(testEntity)

        val retrieved = dao.getEntryById("test-id-1")

        assertNull("Entry should be deleted", retrieved)
    }

    @Test
    fun testDeleteAll() = runTest {
        val entity1 = VaultEntity(
            id = "id-1",
            title = "Entry 1",
            username = "user1",
            website = "https://site1.com",
            encryptedPayload = "enc1".toByteArray(),
            iv = "iv1".toByteArray()
        )
        val entity2 = VaultEntity(
            id = "id-2",
            title = "Entry 2",
            username = "user2",
            website = "https://site2.com",
            encryptedPayload = "enc2".toByteArray(),
            iv = "iv2".toByteArray()
        )

        dao.insertEntry(entity1)
        dao.insertEntry(entity2)
        dao.deleteAll()

        val allEntries = dao.getAllEntries().first()

        assertTrue("Database should be empty", allEntries.isEmpty())
    }

    @Test
    fun testSearchEntries_byTitle() = runTest {
        dao.insertEntry(testEntity)

        val results = dao.searchEntries("Test").first()

        assertEquals("Should find entry by title", 1, results.size)
        assertEquals("Should find test entity", testEntity.id, results[0].id)
    }

    @Test
    fun testSearchEntries_byWebsite() = runTest {
        dao.insertEntry(testEntity)

        val results = dao.searchEntries("example").first()

        assertEquals("Should find entry by website", 1, results.size)
        assertEquals("Should find test entity", testEntity.id, results[0].id)
    }

    @Test
    fun testSearchEntries_noMatch() = runTest {
        dao.insertEntry(testEntity)

        val results = dao.searchEntries("nonexistent").first()

        assertTrue("Should not find any entries", results.isEmpty())
    }

    @Test
    fun testSearchEntries_caseSensitive() = runTest {
        dao.insertEntry(testEntity)

        val results = dao.searchEntries("test").first()

        assertTrue("Search should be case-insensitive", results.isNotEmpty())
    }

    @Test
    fun testGetAllEntries_ordering() = runTest {
        val entity1 = VaultEntity(
            id = "id-1",
            title = "Entry 1",
            username = "user1",
            website = "https://site1.com",
            encryptedPayload = "enc1".toByteArray(),
            iv = "iv1".toByteArray(),
            lastModified = 100000L
        )
        val entity2 = VaultEntity(
            id = "id-2",
            title = "Entry 2",
            username = "user2",
            website = "https://site2.com",
            encryptedPayload = "enc2".toByteArray(),
            iv = "iv2".toByteArray(),
            lastModified = 200000L
        )

        dao.insertEntry(entity1)
        dao.insertEntry(entity2)

        val allEntries = dao.getAllEntries().first()

        assertEquals("Should be ordered by lastModified DESC", entity2.id, allEntries[0].id)
    }

    @Test
    fun testByteArrayFields_storedCorrectly() = runTest {
        dao.insertEntry(testEntity)

        val retrieved = dao.getEntryById("test-id-1")

        assertNotNull("Entry should be retrieved", retrieved)
        assertArrayEquals("Encrypted payload should match", testEntity.encryptedPayload, retrieved?.encryptedPayload)
        assertArrayEquals("IV should match", testEntity.iv, retrieved?.iv)
    }

    @Test
    fun testTotpSecretField() = runTest {
        dao.insertEntry(testEntity)

        val retrieved = dao.getEntryById("test-id-1")

        assertNotNull("Entry should be retrieved", retrieved)
        assertEquals("TOTP secret should match", testEntity.totpSecret, retrieved?.totpSecret)
    }

    @Test
    fun testReplaceOnConflict() = runTest {
        dao.insertEntry(testEntity)

        val updatedEntity = testEntity.copy(
            title = "Replaced Title",
            lastModified = 555555L
        )
        dao.insertEntry(updatedEntity)

        val allEntries = dao.getAllEntries().first()

        assertEquals("Should still have only 1 entry", 1, allEntries.size)
        assertEquals("Should have updated title", "Replaced Title", allEntries[0].title)
    }

    @Test
    fun testGetEntryById_notFound() = runTest {
        val retrieved = dao.getEntryById("non-existent-id")

        assertNull("Should return null for non-existent ID", retrieved)
    }

    @Test
    fun testGetAllEntries_emptyDatabase() = runTest {
        val allEntries = dao.getAllEntries().first()

        assertTrue("Database should be empty initially", allEntries.isEmpty())
    }
}
