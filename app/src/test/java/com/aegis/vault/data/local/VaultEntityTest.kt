package com.aegis.vault.data.local

import org.junit.Assert.*
import org.junit.Test

class VaultEntityTest {

    @Test
    fun testVaultEntity_creation() {
        val entity = VaultEntity(
            id = "test-id-1",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray()
        )
        
        assertEquals("ID should match", "test-id-1", entity.id)
        assertEquals("Title should match", "Test Entry", entity.title)
        assertEquals("Username should match", "testuser", entity.username)
        assertEquals("Website should match", "https://example.com", entity.website)
    }

    @Test
    fun testVaultEntity_withOptionalFields() {
        val entity = VaultEntity(
            id = "test-id-2",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray(),
            totpSecret = "JBSWY3DPEHPK3PXP",
            lastModified = 123456789L
        )
        
        assertEquals("TOTP secret should match", "JBSWY3DPEHPK3PXP", entity.totpSecret)
        assertEquals("Last modified should match", 123456789L, entity.lastModified)
    }

    @Test
    fun testVaultEntity_defaultTotpSecret() {
        val entity = VaultEntity(
            id = "test-id-3",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray()
        )
        
        assertNull("Default TOTP secret should be null", entity.totpSecret)
    }

    @Test
    fun testVaultEntity_defaultLastModified() {
        val beforeCreation = System.currentTimeMillis()
        val entity = VaultEntity(
            id = "test-id-4",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray()
        )
        val afterCreation = System.currentTimeMillis()
        
        assertTrue("Default lastModified should be recent", 
            entity.lastModified >= beforeCreation && entity.lastModified <= afterCreation)
    }

    @Test
    fun testEquals_sameObject() {
        val entity = VaultEntity(
            id = "test-id-5",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray()
        )
        
        assertTrue("Entity should equal itself", entity == entity)
    }

    @Test
    fun testEquals_sameId() {
        val entity1 = VaultEntity(
            id = "test-id-6",
            title = "Entry 1",
            username = "user1",
            website = "https://site1.com",
            encryptedPayload = "data1".toByteArray(),
            iv = "iv1".toByteArray()
        )
        
        val entity2 = VaultEntity(
            id = "test-id-6",
            title = "Entry 2",
            username = "user2",
            website = "https://site2.com",
            encryptedPayload = "data2".toByteArray(),
            iv = "iv2".toByteArray()
        )
        
        assertTrue("Entities with same ID should be equal", entity1 == entity2)
    }

    @Test
    fun testEquals_differentId() {
        val entity1 = VaultEntity(
            id = "test-id-7",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray()
        )
        
        val entity2 = VaultEntity(
            id = "test-id-8",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray()
        )
        
        assertFalse("Entities with different IDs should not be equal", entity1 == entity2)
    }

    @Test
    fun testEquals_null() {
        val entity = VaultEntity(
            id = "test-id-9",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray()
        )
        
        assertFalse("Entity should not equal null", entity == null)
    }

    @Test
    fun testEquals_differentClass() {
        val entity = VaultEntity(
            id = "test-id-10",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray()
        )
        
        assertFalse("Entity should not equal different class", entity.equals("string"))
    }

    @Test
    fun testHashCode_consistency() {
        val entity = VaultEntity(
            id = "test-id-11",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray()
        )
        
        val hash1 = entity.hashCode()
        val hash2 = entity.hashCode()
        
        assertEquals("HashCode should be consistent", hash1, hash2)
    }

    @Test
    fun testHashCode_basedOnId() {
        val entity1 = VaultEntity(
            id = "test-id-12",
            title = "Entry 1",
            username = "user1",
            website = "https://site1.com",
            encryptedPayload = "data1".toByteArray(),
            iv = "iv1".toByteArray()
        )
        
        val entity2 = VaultEntity(
            id = "test-id-12",
            title = "Entry 2",
            username = "user2",
            website = "https://site2.com",
            encryptedPayload = "data2".toByteArray(),
            iv = "iv2".toByteArray()
        )
        
        assertEquals("Entities with same ID should have same hashCode", 
            entity1.hashCode(), entity2.hashCode())
    }

    @Test
    fun testHashCode_differentIds() {
        val entity1 = VaultEntity(
            id = "test-id-13",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray()
        )
        
        val entity2 = VaultEntity(
            id = "test-id-14",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = "encrypted-data".toByteArray(),
            iv = "iv-data".toByteArray()
        )
        
        assertNotEquals("Entities with different IDs should have different hashCodes", 
            entity1.hashCode(), entity2.hashCode())
    }

    @Test
    fun testByteArrayFields() {
        val encryptedData = "test-encrypted-payload".toByteArray()
        val ivData = "test-iv-12bytes!!".toByteArray()
        
        val entity = VaultEntity(
            id = "test-id-15",
            title = "Test Entry",
            username = "testuser",
            website = "https://example.com",
            encryptedPayload = encryptedData,
            iv = ivData
        )
        
        assertArrayEquals("Encrypted payload should match", encryptedData, entity.encryptedPayload)
        assertArrayEquals("IV should match", ivData, entity.iv)
    }
}
