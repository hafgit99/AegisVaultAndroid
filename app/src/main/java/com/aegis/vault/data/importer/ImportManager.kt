package com.aegis.vault.data.importer

import com.aegis.vault.data.local.VaultEntity
import com.aegis.vault.security.SecurityUtils
import com.opencsv.CSVReader
import java.io.Reader
import java.util.UUID

object ImportManager {

    /**
     * Bitwarden CSV Importer
     * Format: folder,favorite,type,name,notes,fields,reprompt,login_uri,login_username,login_password,login_totp
     */
    fun importBitwardenCsv(reader: Reader, masterKey: ByteArray): List<VaultEntity> {
        val entries = mutableListOf<VaultEntity>()
        val csvReader = CSVReader(reader)

        val rows = csvReader.readAll()
        if (rows.isEmpty()) return emptyList()

        // Skip header
        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.size < 10) continue

            val title = row[3]
            val username = row[8]
            val password = row[9]
            val website = row[7]
            val totp = if (row.size > 10) row[10] else null

            if (title.isNotBlank()) {
                val (encrypted, iv) = SecurityUtils.encrypt(password.toByteArray(), masterKey)
                entries.add(VaultEntity(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    username = username,
                    website = website,
                    encryptedPayload = encrypted,
                    iv = iv,
                    totpSecret = totp.takeIf { it?.isNotBlank() == true },
                    lastModified = System.currentTimeMillis()
                ))
            }
        }
        return entries
    }

    /**
     * LastPass CSV Importer
     * Format: url,username,password,extra,name,grouping,fav
     */
    fun importLastPassCsv(reader: Reader, masterKey: ByteArray): List<VaultEntity> {
        val entries = mutableListOf<VaultEntity>()
        val csvReader = CSVReader(reader)

        val rows = csvReader.readAll()
        if (rows.isEmpty()) return emptyList()

        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.size < 5) continue

            val website = row[0]
            val username = row[1]
            val password = row[2]
            val title = row[4]

            if (title.isNotBlank()) {
                val (encrypted, iv) = SecurityUtils.encrypt(password.toByteArray(), masterKey)
                entries.add(VaultEntity(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    username = username,
                    website = website,
                    encryptedPayload = encrypted,
                    iv = iv,
                    lastModified = System.currentTimeMillis()
                ))
            }
        }
        return entries
    }
}
