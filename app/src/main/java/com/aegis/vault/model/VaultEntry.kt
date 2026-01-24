package com.aegis.vault.model

data class VaultEntry(
    val id: String,
    val title: String,
    val username: String,
    val website: String,
    val iconRes: Int? = null,
    val lastUsed: Long = System.currentTimeMillis()
)
