package com.aegis.vault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.aegis.vault.data.prefs.PreferenceManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [VaultEntity::class], version = 2, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context, prefManager: PreferenceManager): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val dbKey = prefManager.getOrCreateDatabaseKey()
                    val factory: SupportSQLiteOpenHelper.Factory = SupportOpenHelperFactory(dbKey.toByteArray())
                    
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        VaultDatabase::class.java,
                        "aegis_vault_db_v11" // Sync with v11
                    )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    throw RuntimeException("Database initialization failed: ${e.message}", e)
                }
            }
        }
    }
}
