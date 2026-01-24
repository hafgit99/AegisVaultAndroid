package com.aegis.vault.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreManager {
    private const val KEY_ALIAS = "AegisBiometricKeyV2"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true) // Parmak izi zorunlu!
            .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG) // Sadece parmak izi
            .setInvalidatedByBiometricEnrollment(true) // Yeni parmak izi eklenirse anahtar silinsin (Ultra Güvenlik)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun getEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        return cipher
    }

    fun getDecryptCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        return cipher
    }

    fun encryptMasterKey(masterKey: ByteArray, cipher: Cipher): Pair<ByteArray, ByteArray> {
        val encrypted = cipher.doFinal(masterKey)
        return encrypted to cipher.iv
    }

    fun decryptMasterKey(encryptedData: ByteArray, cipher: Cipher): ByteArray {
        return cipher.doFinal(encryptedData)
    }
}
