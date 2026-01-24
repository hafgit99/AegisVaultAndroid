package com.aegis.vault.security

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Arrays

object SecurityUtils {

    private const val SALT_SIZE = 16
    private const val KEY_SIZE = 32
    private const val ITERATIONS = 3  // Güvenlik için artırıldı
    private const val MEMORY_LIMIT_KB = 131072 // 128 MB (Güçlü güvenlik)
    private const val PARALLELISM = 2 // Çift çekirdek kullanımı

    private fun argon2Hash(password: ByteArray, salt: ByteArray): ByteArray {
        return try {
            android.util.Log.d("SecurityUtils", "Argon2 Başlatıldı...")
            
            // Ultra hafif parametreler
            val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(ITERATIONS)
                .withMemoryAsKB(MEMORY_LIMIT_KB)
                .withParallelism(PARALLELISM)
                .withSalt(salt)
                .build()
            
            val generator = Argon2BytesGenerator()
            generator.init(params)
            
            val result = ByteArray(KEY_SIZE)
            generator.generateBytes(password, result)
            
            android.util.Log.d("SecurityUtils", "Argon2 Başarıyla Bitti.")
            result
        } catch (e: Throwable) {
            android.util.Log.e("SecurityUtils", "Argon2 KRİTİK HATA! SHA-256'ya geçiliyor...", e)
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            digest.digest(password)
        }
    }

    fun deriveVerificationHash(password: String, salt: ByteArray): ByteArray = argon2Hash(("V:" + password).toByteArray(), salt)
    fun deriveEncryptionKey(password: String, salt: ByteArray): ByteArray = argon2Hash(("E:" + password).toByteArray(), salt)

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_SIZE)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun encrypt(data: ByteArray, key: ByteArray): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return Pair(cipher.doFinal(data), iv)
    }

    fun decrypt(encryptedData: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(encryptedData)
    }

    fun generatePassword(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val charPool = "abcdefghijklmnopqrstuvwxyz" + 
                      (if(includeUppercase) "ABCDEFGHIJKLMNOPQRSTUVWXYZ" else "") +
                      (if(includeNumbers) "0123456789" else "") +
                      (if(includeSymbols) "!@#$%^&*()-_=+[]{}|;:,.<>?" else "")
        
        val random = SecureRandom()
        return (1..length).map { charPool[random.nextInt(charPool.length)] }.joinToString("")
    }

    fun wipeData(data: ByteArray?) { data?.fill(0) }
        
    fun generateRecoveryPhrase(): String {
        val wordList = listOf("abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "absurd", "abuse")
        return (1..12).map { wordList.random() }.joinToString(" ")
    }

    fun getTotpCode(secret: String): String = TotpUtils.generateTOTP(secret)

    enum class PasswordStrength { WEAK, MEDIUM, STRONG, VERY_STRONG }
    
    fun getPasswordStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.WEAK
        
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        
        return when {
            score >= 6 -> PasswordStrength.VERY_STRONG
            score >= 4 -> PasswordStrength.STRONG
            score >= 2 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
}
