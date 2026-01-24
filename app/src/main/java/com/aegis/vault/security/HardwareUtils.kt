package com.aegis.vault.security

import android.content.Context
import android.provider.Settings
import android.os.Build
import java.security.MessageDigest
import java.util.Locale

object HardwareUtils {
    
    /**
     * Kullanıcıya özel, donanım tabanlı benzersiz cihaz kimliği üretir.
     * Bu kimlik Windows tarafındaki lisans aracıyla uyumlu formatta (Hex) döner.
     */
    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "anonymous"
        val hardwareInfo = "${Build.BOARD}${Build.BRAND}${Build.DEVICE}${Build.MANUFACTURER}${Build.MODEL}${Build.PRODUCT}"
        
        val combined = androidId + hardwareInfo
        return hashString(combined).take(16).uppercase(Locale.ROOT)
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
