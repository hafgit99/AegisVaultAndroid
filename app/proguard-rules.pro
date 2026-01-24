# Aegis Vault ProGuard Rules

# Room and SQLite
-keep class androidx.room.RoomDatabase {
    private <fields>;
}

# SQLCipher
-keep class net.zetetic.database.** { *; }

# Argon2 (Bouncy Castle)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Gson
-keep class com.google.gson.** { *; }
-keep class com.aegis.vault.data.model.** { *; }
-keep class com.aegis.vault.data.local.VaultEntity { *; }

# OpenCSV
-keep class com.opencsv.** { *; }

# Jetpack Compose
-keep class androidx.compose.material.icons.** { *; }

# Prevent obfuscation of sensitive classes that might be accessed via reflection
-keep class com.aegis.vault.security.** { *; }

# Optimization
-repackageclasses ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
