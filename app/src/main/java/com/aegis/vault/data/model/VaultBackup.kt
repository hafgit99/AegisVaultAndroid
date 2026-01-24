package com.aegis.vault.data.model

import com.aegis.vault.data.local.VaultEntity

data class VaultBackup(
    val version: Int = 2,
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val encryptedData: String? = null,
    val backupIv: String? = null,
    val backupSalt: String? = null,
    val entries: List<VaultEntity> = emptyList()
)
