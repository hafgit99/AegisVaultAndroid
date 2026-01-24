package com.aegis.vault.security

import org.junit.Assert.*
import org.junit.Test
import org.junit.Before

class SecurityUtilsTest {

    @Test
    fun testGenerateSalt_length() {
        val salt = SecurityUtils.generateSalt()
        assertEquals("Salt should be 16 bytes", 16, salt.size)
    }

    @Test
    fun testGenerateSalt_uniqueness() {
        val salt1 = SecurityUtils.generateSalt()
        val salt2 = SecurityUtils.generateSalt()
        assertFalse("Salts should be unique", salt1.contentEquals(salt2))
    }

    @Test
    fun testDeriveVerificationHash_samePasswordSameSalt() {
        val password = "testPassword123"
        val salt = SecurityUtils.generateSalt()
        
        val hash1 = SecurityUtils.deriveVerificationHash(password, salt)
        val hash2 = SecurityUtils.deriveVerificationHash(password, salt)
        
        assertTrue("Same password and salt should produce same hash", hash1.contentEquals(hash2))
    }

    @Test
    fun testDeriveVerificationHash_differentSalt() {
        val password = "testPassword123"
        val salt1 = SecurityUtils.generateSalt()
        val salt2 = SecurityUtils.generateSalt()
        
        val hash1 = SecurityUtils.deriveVerificationHash(password, salt1)
        val hash2 = SecurityUtils.deriveVerificationHash(password, salt2)
        
        assertFalse("Different salts should produce different hashes", hash1.contentEquals(hash2))
    }

    @Test
    fun testDeriveEncryptionKey_keySize() {
        val password = "testPassword123"
        val salt = SecurityUtils.generateSalt()
        
        val key = SecurityUtils.deriveEncryptionKey(password, salt)
        assertEquals("Encryption key should be 32 bytes (256 bits)", 32, key.size)
    }

    @Test
    fun testDeriveEncryptionKey_differentFromVerificationHash() {
        val password = "testPassword123"
        val salt = SecurityUtils.generateSalt()
        
        val encryptionKey = SecurityUtils.deriveEncryptionKey(password, salt)
        val verificationHash = SecurityUtils.deriveVerificationHash(password, salt)
        
        assertFalse("Encryption key should differ from verification hash", 
            encryptionKey.contentEquals(verificationHash))
    }

    @Test
    fun testEncryptDecrypt_roundtrip() {
        val key = ByteArray(32) { it.toByte() }
        val originalData = "Secret message to encrypt".toByteArray()
        
        val (encryptedData, iv) = SecurityUtils.encrypt(originalData, key)
        val decryptedData = SecurityUtils.decrypt(encryptedData, key, iv)
        
        assertArrayEquals("Decrypted data should match original", originalData, decryptedData)
    }

    @Test
    fun testEncrypt_ivSize() {
        val key = ByteArray(32) { it.toByte() }
        val data = "test data".toByteArray()
        
        val (_, iv) = SecurityUtils.encrypt(data, key)
        assertEquals("IV should be 12 bytes for GCM", 12, iv.size)
    }

    @Test
    fun testEncrypt_differentIvEachTime() {
        val key = ByteArray(32) { it.toByte() }
        val data = "test data".toByteArray()
        
        val (_, iv1) = SecurityUtils.encrypt(data, key)
        val (_, iv2) = SecurityUtils.encrypt(data, key)
        
        assertFalse("Each encryption should produce unique IV", iv1.contentEquals(iv2))
    }

    @Test
    fun testDecrypt_wrongKey() {
        val key1 = ByteArray(32) { it.toByte() }
        val key2 = ByteArray(32) { (it + 1).toByte() }
        val originalData = "Secret message".toByteArray()
        
        val (encryptedData, iv) = SecurityUtils.encrypt(originalData, key1)
        
        try {
            SecurityUtils.decrypt(encryptedData, key2, iv)
            fail("Decryption with wrong key should fail")
        } catch (e: Exception) {
            assertTrue("Expected decryption failure", true)
        }
    }

    @Test
    fun testGeneratePassword_length() {
        val password = SecurityUtils.generatePassword(length = 20)
        assertEquals("Password should have requested length", 20, password.length)
    }

    @Test
    fun testGeneratePassword_defaultIncludesMixed() {
        val password = SecurityUtils.generatePassword(length = 50)
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        
        assertTrue("Should contain uppercase", hasUppercase)
        assertTrue("Should contain lowercase", hasLowercase)
        assertTrue("Should contain digits", hasDigit)
        assertTrue("Should contain symbols", hasSymbol)
    }

    @Test
    fun testGeneratePassword_lowercaseOnly() {
        val password = SecurityUtils.generatePassword(
            length = 20,
            includeUppercase = false,
            includeNumbers = false,
            includeSymbols = false
        )
        
        assertTrue("Should only contain lowercase", password.all { it.isLowerCase() })
        assertFalse("Should not contain digits", password.any { it.isDigit() })
    }

    @Test
    fun testWipeData() {
        val data = ByteArray(16) { 0xFF.toByte() }
        SecurityUtils.wipeData(data)
        
        assertTrue("Data should be zeroed after wipe", data.all { it == 0.toByte() })
    }

    @Test
    fun testWipeData_null() {
        SecurityUtils.wipeData(null)
        assertTrue("Should handle null gracefully", true)
    }

    @Test
    fun testGenerateRecoveryPhrase_wordCount() {
        val phrase = SecurityUtils.generateRecoveryPhrase()
        val words = phrase.split(" ")
        assertEquals("Recovery phrase should have 12 words", 12, words.size)
    }

    @Test
    fun testGetTotpCode_format() {
        val secret = "JBSWY3DPEHPK3PXP"
        val code = SecurityUtils.getTotpCode(secret)
        
        assertEquals("TOTP should be 6 digits", 6, code.length)
        assertTrue("TOTP should be numeric", code.all { it.isDigit() })
    }

    @Test
    fun testGetPasswordStrength_weak() {
        assertEquals("Short password should be weak", 
            SecurityUtils.PasswordStrength.WEAK, 
            SecurityUtils.getPasswordStrength("short"))
    }

    @Test
    fun testGetPasswordStrength_medium() {
        assertEquals("Password with mixed chars should be medium", 
            SecurityUtils.PasswordStrength.MEDIUM, 
            SecurityUtils.getPasswordStrength("Password1"))
    }

    @Test
    fun testGetPasswordStrength_strong() {
        assertEquals("Long password with symbols should be strong", 
            SecurityUtils.PasswordStrength.STRONG, 
            SecurityUtils.getPasswordStrength("StrongP@ss123"))
    }

    @Test
    fun testGetPasswordStrength_veryStrong() {
        assertEquals("Very long complex password should be very strong",
            SecurityUtils.PasswordStrength.VERY_STRONG,
            SecurityUtils.getPasswordStrength("V3ry\$tr0ng!P@ssw0rd#2024"))
    }

    @Test
    fun testGetPasswordStrength_onlyDigits() {
        assertEquals("Numeric only password should be weak", 
            SecurityUtils.PasswordStrength.WEAK, 
            SecurityUtils.getPasswordStrength("123456789012"))
    }

    @Test
    fun testGetPasswordStrength_onlyLowercase() {
        assertEquals("Lowercase only should be weak", 
            SecurityUtils.PasswordStrength.WEAK, 
            SecurityUtils.getPasswordStrength("onlylowercaseletters"))
    }
}
