package com.aegis.vault.data.prefs

import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

class PreferenceManagerTest {

    private lateinit var context: android.content.Context

    private lateinit var metaPrefs: android.content.SharedPreferences

    private lateinit var basicPrefs: android.content.SharedPreferences

    private lateinit var securePrefs: android.content.SharedPreferences

    private lateinit var editor: android.content.SharedPreferences.Editor

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        context = org.mockito.kotlin.mock()
        metaPrefs = org.mockito.kotlin.mock()
        basicPrefs = org.mockito.kotlin.mock()
        securePrefs = org.mockito.kotlin.mock()
        editor = org.mockito.kotlin.mock()

        whenever(context.getSharedPreferences("aegis_prefs_meta", android.content.Context.MODE_PRIVATE))
            .thenReturn(metaPrefs)

        whenever(context.getSharedPreferences("aegis_basic_settings", android.content.Context.MODE_PRIVATE))
            .thenReturn(basicPrefs)

        whenever(basicPrefs.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.putLong(any(), any())).thenReturn(editor)
        whenever(editor.apply()).then { }
    }

    @Test
    fun testIsSetupComplete_initialValue() {
        whenever(basicPrefs.getBoolean("is_setup_complete", false)).thenReturn(false)

        val preferenceManager = PreferenceManager(context)
        val isComplete = preferenceManager.isSetupComplete

        assertFalse("Initial setup complete should be false", isComplete)
    }

    @Test
    fun testIsSetupComplete_setValue() {
        whenever(basicPrefs.edit()).thenReturn(editor)

        val preferenceManager = PreferenceManager(context)
        preferenceManager.isSetupComplete = true

        verify(editor).putBoolean("is_setup_complete", true)
        verify(editor).apply()
    }

    @Test
    fun testRecoveryPhrase_get() {
        val testPhrase = "abandon ability able about above absent"
        whenever(securePrefs.getString("recovery_phrase_encrypted", null)).thenReturn(testPhrase)
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }

        val phrase = preferenceManager.recoveryPhrase

        assertEquals("Recovery phrase should match", testPhrase, phrase)
    }

    @Test
    fun testRecoveryPhrase_set() {
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }
        whenever(securePrefs.edit()).thenReturn(editor)

        preferenceManager.recoveryPhrase = "test recovery phrase"

        verify(editor).putString("recovery_phrase_encrypted", "test recovery phrase")
        verify(editor).apply()
    }

    @Test
    fun testRecoveryPhrase_null() {
        whenever(securePrefs.getString("recovery_phrase_encrypted", null)).thenReturn(null)
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }

        val phrase = preferenceManager.recoveryPhrase

        assertNull("Recovery phrase should be null when not set", phrase)
    }

    @Test
    fun testGetOrCreateDatabaseKey_existingKey() {
        val existingKey = "existing-database-key-12345"
        whenever(securePrefs.getString("db_unique_encryption_key", null)).thenReturn(existingKey)
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }

        val key = preferenceManager.getOrCreateDatabaseKey()

        assertEquals("Should return existing key", existingKey, key)
    }

    @Test
    fun testGetOrCreateDatabaseKey_newKey() {
        whenever(securePrefs.getString("db_unique_encryption_key", null)).thenReturn(null)
        whenever(securePrefs.edit()).thenReturn(editor)
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }

        val key = preferenceManager.getOrCreateDatabaseKey()

        assertFalse("New key should not be empty", key.isEmpty())
        assertEquals("New key should be 64 chars (2 UUIDs)", 64, key.length)
        verify(editor).putString(eq("db_unique_encryption_key"), any())
    }

    @Test
    fun testSaveMasterPasswordData() {
        val hash = "hashed-password"
        val salt = "salt-value"
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }
        whenever(securePrefs.edit()).thenReturn(editor)

        preferenceManager.saveMasterPasswordData(hash, salt)

        verify(editor).putString("master_password_hash", hash)
        verify(editor).putString("master_password_salt", salt)
        verify(editor).apply()
    }

    @Test
    fun testMasterPasswordHash() {
        val hash = "test-hash-value"
        whenever(securePrefs.getString("master_password_hash", null)).thenReturn(hash)
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }

        val retrievedHash = preferenceManager.masterPasswordHash

        assertEquals("Master password hash should match", hash, retrievedHash)
    }

    @Test
    fun testSalt() {
        val salt = "test-salt-value"
        whenever(securePrefs.getString("master_password_salt", null)).thenReturn(salt)
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }

        val retrievedSalt = preferenceManager.salt

        assertEquals("Salt should match", salt, retrievedSalt)
    }

    @Test
    fun testMasterPasswordHash_null() {
        whenever(securePrefs.getString("master_password_hash", null)).thenReturn(null)
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }

        val hash = preferenceManager.masterPasswordHash

        assertNull("Hash should be null when not set", hash)
    }

    @Test
    fun testSalt_null() {
        whenever(securePrefs.getString("master_password_salt", null)).thenReturn(null)
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }

        val salt = preferenceManager.salt

        assertNull("Salt should be null when not set", salt)
    }

    @Test
    fun testExceptionHandling_securePrefs() {
        whenever(securePrefs.edit()).thenThrow(RuntimeException("Test exception"))
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }

        try {
            preferenceManager.recoveryPhrase = "test phrase"
            assertTrue("Should not throw exception", true)
        } catch (e: Exception) {
            fail("Should handle exception gracefully")
        }
    }

    @Test
    fun testExceptionHandling_getString() {
        whenever(securePrefs.getString(any(), any())).thenThrow(RuntimeException("Test exception"))
        val preferenceManager = PreferenceManager(context)
        preferenceManager::class.java.getDeclaredField("securePrefs").apply {
            isAccessible = true
            set(preferenceManager, securePrefs)
        }

        val phrase = preferenceManager.recoveryPhrase

        assertNull("Should return null on exception", phrase)
    }
}
