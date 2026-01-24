package com.aegis.vault.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

class PreferenceManager(private val context: Context) {
    
    // 1. KURULUM DURUMU (Şifresiz - Fresh v10 for absolute safety)
    private val plainPrefs: SharedPreferences = context.getSharedPreferences("aegis_start_v11", Context.MODE_PRIVATE)
    
    // 2. ŞİFRELİ VERİLER (Lazy ve Fallback korumalı)
    private val securePrefs: SharedPreferences by lazy {
        try {
            val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                context, "aegis_secure_v11", key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e("PreferenceManager", "Encryption failed, using plain fallback", e)
            context.getSharedPreferences("aegis_secure_v11_fallback", Context.MODE_PRIVATE)
        }
    }

    var isSetupComplete: Boolean
        get() = plainPrefs.getBoolean("setup_done", false)
        set(value) { plainPrefs.edit().putBoolean("setup_done", value).commit() }

    fun saveMasterPasswordData(hash: String, salt: String) {
        securePrefs.edit()
            .putString("hash", hash)
            .putString("salt", salt)
            .commit()
    }

    val masterPasswordHash: String? get() = securePrefs.getString("hash", null)
    val salt: String? get() = securePrefs.getString("salt", null)

    fun saveBiometricMasterKey(encryptedKey: String, iv: String) {
        securePrefs.edit()
            .putString("bio_master_key", encryptedKey)
            .putString("bio_master_iv", iv)
            .apply()
    }

    val bioMasterKey: String? get() = securePrefs.getString("bio_master_key", null)
    val bioMasterIv: String? get() = securePrefs.getString("bio_master_iv", null)
    
    fun clearBiometricData() {
        securePrefs.edit()
            .remove("bio_master_key")
            .remove("bio_master_iv")
            .apply()
    }
    
    var recoveryPhrase: String?
        get() = securePrefs.getString("recovery", null)
        set(value) { securePrefs.edit().putString("recovery", value).commit() }

    fun getOrCreateDatabaseKey(): String {
        var key = securePrefs.getString("db_key", null)
        if (key == null) {
            key = UUID.randomUUID().toString() + UUID.randomUUID().toString()
            securePrefs.edit().putString("db_key", key).commit()
        }
        return key ?: "emergency_static_key_v11"
    }

    // --- LİSANS VE DENEME SÜRESİ ---
    
    var installTime: Long
        get() = plainPrefs.getLong("install_time", 0L).let {
            if (it == 0L) {
                val now = System.currentTimeMillis()
                plainPrefs.edit().putLong("install_time", now).commit()
                now
            } else it
        }
        set(value) { plainPrefs.edit().putLong("install_time", value).commit() }

    var licenseKey: String?
        get() = securePrefs.getString("license_key", null)
        set(value) { securePrefs.edit().putString("license_key", value).commit() }

    val isPremium: Boolean
        get() = licenseKey != null

    // --- GÜVENLİK AYARLARI ---

    var autoLockTimeout: Long
        get() = plainPrefs.getLong("auto_lock_timeout", 60000L) // Varsayılan 1 dk
        set(value) { plainPrefs.edit().putLong("auto_lock_timeout", value).commit() }

    var lastBackgroundTime: Long
        get() = plainPrefs.getLong("last_bg_time", 0L)
        set(value) { plainPrefs.edit().putLong("last_bg_time", value).commit() }

    var language: String
        get() = plainPrefs.getString("app_language", "tr") ?: "tr"
        set(value) { plainPrefs.edit().putString("app_language", value).commit() }
}
