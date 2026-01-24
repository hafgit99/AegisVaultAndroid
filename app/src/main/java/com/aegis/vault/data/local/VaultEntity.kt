package com.aegis.vault.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vault_entries",
    indices = [
        Index(value = ["title"]),
        Index(value = ["username"])
    ]
)
data class VaultEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val username: String,
    val website: String,
    val encryptedPayload: ByteArray,
    val iv: ByteArray,
    val totpSecret: String? = null,
    val lastModified: Long = System.currentTimeMillis(),
    
    // Dosya ekleri için yeni alanlar
    val attachmentName: String? = null,       // Dosya adı (örn: belge.pdf)
    val attachmentType: String? = null,       // MIME tipi (örn: application/pdf)
    val encryptedAttachment: ByteArray? = null, // Şifrelenmiş dosya verisi
    val attachmentIv: ByteArray? = null       // Dosya şifreleme IV'si
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VaultEntity
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    fun hasAttachment(): Boolean = attachmentName != null && encryptedAttachment != null
}
