package com.aegis.vault

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.aegis.vault.data.local.VaultDatabase
import com.aegis.vault.data.repository.VaultRepository
import com.aegis.vault.data.prefs.PreferenceManager

class AegisApplication : Application() {
    val preferenceManager by lazy { PreferenceManager(this) }
    private val database by lazy { VaultDatabase.getDatabase(this, preferenceManager) }
    val repository by lazy { VaultRepository(database.vaultDao()) }

    override fun onCreate() {
        super.onCreate()
        
        // SQLCipher native kütüphanesini yükle
        try {
            System.loadLibrary("sqlcipher")
            android.util.Log.d("AegisApplication", "SQLCipher kütüphanesi başarıyla yüklendi.")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("AegisApplication", "SQLCipher yüklenemedi!", e)
        }
        
        // Global Hata Yakalayıcı: Uygulama çökerse hatayı ekranda göster
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "KRİTİK HATA: ${throwable.message}", Toast.LENGTH_LONG).show()
            }
            android.util.Log.e("AegisVault", "Hata Oluştu!", throwable)
        }
    }
}
