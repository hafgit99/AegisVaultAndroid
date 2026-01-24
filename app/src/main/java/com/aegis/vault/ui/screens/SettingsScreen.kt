package com.aegis.vault.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.ui.theme.LocalStrings
import com.aegis.vault.ui.theme.StringsTR
import com.aegis.vault.data.local.VaultEntity
import com.aegis.vault.data.model.VaultBackup
import com.aegis.vault.data.importer.ImportManager
import com.aegis.vault.ui.theme.ElectricCyan
import com.aegis.vault.ui.theme.ErrorRed
import com.aegis.vault.ui.theme.MidnightBlue
import com.aegis.vault.security.SecurityUtils
import com.google.gson.Gson
import com.opencsv.CSVWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.StringWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onResetDatabase: () -> Unit,
    onChangeMasterPassword: (String, String, (Boolean) -> Unit) -> Unit, // old, new, onResult
    masterKey: ByteArray,
    allEntries: List<VaultEntity>,
    onImportEntries: (List<VaultEntity>) -> Unit,
    onPremiumClick: () -> Unit,
    onAuditClick: () -> Unit,
    onPasswordGenClick: () -> Unit,
    autoLockTimeout: Long,
    onAutoLockChange: (Long) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    isPremium: Boolean,
    trialDaysLeft: Int
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showChangeMasterPasswordDialog by remember { mutableStateOf(false) }
    var isExportingEncrypted by remember { mutableStateOf(false) }
    var importType by remember { mutableStateOf("JSON") }
    var backupPassword by remember { mutableStateOf("") }
    
    // Şifre değiştirme state'leri
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    
    // Görünürlük state'leri
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // İşlem durumu
    var isProcessingChange by remember { mutableStateOf(false) }
    
    // Store URI temporary for password protected flow
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            if (isExportingEncrypted) {
                pendingUri = it
                showPasswordDialog = true
            } else {
                try {
                    val backup = VaultBackup(entries = allEntries)
                    val json = Gson().toJson(backup)
                    context.contentResolver.openOutputStream(it).use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(json)
                        }
                    }
                    Toast.makeText(context, s.exportJsonSuccess, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it).use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        val csvWriter = CSVWriter(writer)
                        csvWriter.writeNext(arrayOf("Title", "Username", "Website", "TOTP"))
                        allEntries.forEach { entry ->
                            csvWriter.writeNext(arrayOf(entry.title, entry.username, entry.website, entry.totpSecret ?: ""))
                        }
                    }
                }
                Toast.makeText(context, s.exportCsvSuccess, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it).use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val imported = when(importType) {
                            "BITWARDEN" -> ImportManager.importBitwardenCsv(reader, masterKey)
                            "LASTPASS" -> ImportManager.importLastPassCsv(reader, masterKey)
                            else -> Gson().fromJson(reader, VaultBackup::class.java).entries
                        }
                        onImportEntries(imported)
                        Toast.makeText(context, "${imported.size} ${s.importCount}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "${s.importError}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text(s.backupPasswordTitle, color = Color.White) },
            text = {
                OutlinedTextField(
                    value = backupPassword,
                    onValueChange = { backupPassword = it },
                    label = { Text(s.password) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    pendingUri?.let { uri ->
                        try {
                            val salt = SecurityUtils.generateSalt()
                            val key = SecurityUtils.deriveEncryptionKey(backupPassword, salt)
                            val entriesJson = Gson().toJson(allEntries)
                            val (encrypted, iv) = SecurityUtils.encrypt(entriesJson.toByteArray(), key)

                            val backup = VaultBackup(
                                isEncrypted = true,
                                encryptedData = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP),
                                backupIv = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP),
                                backupSalt = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP),
                                entries = emptyList() // Hidden in encryptedData
                            )
                            
                            val json = Gson().toJson(backup)
                            context.contentResolver.openOutputStream(uri).use { os ->
                                OutputStreamWriter(os).use { it.write(json) }
                            }
                            Toast.makeText(context, s.backupSuccess, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "${s.backupError}: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showPasswordDialog = false
                    backupPassword = ""
                }) {
                    Text(s.encryptAndSave)
                }
            },
            containerColor = MidnightBlue
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(s.importSource, color = Color.White) },
            text = {
                Column {
                    Text(s.importSourceDesc, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { importType = "JSON"; importLauncher.launch(arrayOf("application/json")); showImportDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(s.aegisJsonRecommended)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { importType = "BITWARDEN"; importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")); showImportDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Bitwarden CSV")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { importType = "LASTPASS"; importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")); showImportDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("LastPass CSV")
                    }
                }
            },
            confirmButton = {},
            containerColor = MidnightBlue
        )
    }

    if (showChangeMasterPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isProcessingChange) {
                    showChangeMasterPasswordDialog = false
                    oldPassword = ""; newPassword = ""; confirmNewPassword = ""
                }
            },
            title = { Text(s.changePassword, color = Color.White) },
            text = {
                if (isProcessingChange) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = ElectricCyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(s.reencrypting, color = Color.White)
                        Text(if(s is StringsTR) "Lütfen bekleyin." else "Please wait.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(s.reencryptingDesc, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        
                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = { Text(s.oldPassword) },
                            visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                                    Icon(imageVector = if (oldPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if(oldPasswordVisible) s.hide else s.show, tint = Color.White.copy(alpha = 0.5f))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text(s.newPasswordReq) },
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if(newPasswordVisible) s.hide else s.show, tint = Color.White.copy(alpha = 0.5f))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        
                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            label = { Text(s.confirmNewPasswordLabel) },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if(confirmPasswordVisible) s.hide else s.show, tint = Color.White.copy(alpha = 0.5f))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            },
            confirmButton = {
                if (!isProcessingChange) {
                    Button(
                        onClick = {
                            if (newPassword == confirmNewPassword && newPassword.length >= 12) {
                                isProcessingChange = true
                                onChangeMasterPassword(oldPassword, newPassword) { success ->
                                    isProcessingChange = false
                                    if (success) {
                                        showChangeMasterPasswordDialog = false
                                        oldPassword = ""; newPassword = ""; confirmNewPassword = ""
                                        Toast.makeText(context, s.passwordChangedSuccess, Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, s.wrongOldPassword, Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                val msg = if (newPassword.length < 12) s.passwordMinLengthError else s.passwordMismatch
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = oldPassword.isNotBlank() && newPassword.isNotBlank() && newPassword == confirmNewPassword,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black)
                    ) {
                        Text(s.update)
                    }
                }
            },
            dismissButton = {
                if (!isProcessingChange) {
                    TextButton(onClick = { 
                        showChangeMasterPasswordDialog = false
                        oldPassword = ""; newPassword = ""; confirmNewPassword = ""
                    }) {
                        Text(s.cancel, color = Color.White)
                    }
                }
            },
            containerColor = MidnightBlue
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(s.resetDatabaseTitle, color = Color.White) },
            text = { Text(s.resetDatabaseWarning, color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    onResetDatabase()
                    showResetDialog = false
                }) {
                    Text(s.reset, color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(s.cancel, color = Color.White)
                }
            },
            containerColor = MidnightBlue
        )
    }

    if (showAutoLockDialog) {
        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            title = { Text(s.autoLock, color = Color.White) },
            text = {
                Column {
                    val options = listOf(
                        s.immediately to 0L,
                        s.minute1 to 60000L,
                        s.minutes5 to 300000L,
                        s.minutes15 to 900000L,
                        s.never to -1L
                    )
                    options.forEach { option ->
                        val label = option.first
                        val value = option.second
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onAutoLockChange(value)
                                    showAutoLockDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = autoLockTimeout == value,
                                onClick = { 
                                    onAutoLockChange(value)
                                    showAutoLockDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan, unselectedColor = Color.White.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = MidnightBlue
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(s.language, color = Color.White) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLanguageChange("tr")
                                showLanguageDialog = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == "tr",
                            onClick = {
                                onLanguageChange("tr")
                                showLanguageDialog = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan, unselectedColor = Color.White.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Türkçe", color = Color.White)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLanguageChange("en")
                                showLanguageDialog = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == "en",
                            onClick = {
                                onLanguageChange("en")
                                showLanguageDialog = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan, unselectedColor = Color.White.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("English", color = Color.White)
                    }
                }
            },
            confirmButton = {},
            containerColor = MidnightBlue
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settings, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = MidnightBlue
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lisans Durumu Kartı
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremium) ElectricCyan.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
                ),
                onClick = onPremiumClick
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isPremium) Icons.Default.Verified else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isPremium) ElectricCyan else Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            if (isPremium) "Aegis Vault Premium" else s.freeTrial,
                            fontWeight = FontWeight.Bold,
                            color = if (isPremium) ElectricCyan else Color.White
                        )
                        Text(
                            if (isPremium) s.lifetimeLicenseActive 
                            else s.trialDaysRemaining.format(trialDaysLeft),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Text(
                s.general,
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan,
                modifier = Modifier.padding(start = 8.dp)
            )

            SettingsItem(
                title = s.language,
                subtitle = if (s is StringsTR) "Türkçe" else "English",
                icon = Icons.Default.Language,
                onClick = { showLanguageDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                s.security,
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan,
                modifier = Modifier.padding(start = 8.dp)
            )

            SettingsItem(
                title = s.reencryptTitle,
                subtitle = s.reencryptSub,
                icon = Icons.Default.LockReset,
                onClick = { showChangeMasterPasswordDialog = true }
            )

            SettingsItem(
                title = s.securityAudit,
                subtitle = s.securityAuditItemSub,
                icon = Icons.Default.Shield,
                onClick = onAuditClick
            )

            SettingsItem(
                title = s.autoLock,
                subtitle = when(autoLockTimeout) {
                    0L -> s.immediately
                    60000L -> s.minute1
                    300000L -> s.minutes5
                    900000L -> s.minutes15
                    else -> s.never
                },
                icon = Icons.Default.Timer,
                onClick = { showAutoLockDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                s.backup,
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan,
                modifier = Modifier.padding(start = 8.dp)
            )

            SettingsItem(
                title = s.exportJson,
                subtitle = s.exportJsonSub,
                icon = Icons.Default.FileUpload,
                onClick = { isExportingEncrypted = false; exportJsonLauncher.launch("aegis_backup.json") }
            )

            SettingsItem(
                title = s.exportEncryptedJson,
                subtitle = s.exportEncryptedJsonSub,
                icon = Icons.Default.EnhancedEncryption,
                onClick = { isExportingEncrypted = true; exportJsonLauncher.launch("aegis_backup_locked.json") }
            )

            SettingsItem(
                title = s.exportCsv,
                subtitle = s.exportCsvSub,
                icon = Icons.Default.GridOn,
                onClick = { exportCsvLauncher.launch("aegis_backup.csv") }
            )

            SettingsItem(
                title = s.import,
                subtitle = s.importJsonSub,
                icon = Icons.Default.FileDownload,
                onClick = { showImportDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                s.dangerousArea,
                style = MaterialTheme.typography.titleMedium,
                color = ErrorRed,
                modifier = Modifier.padding(start = 8.dp)
            )

            SettingsItem(
                title = s.resetDatabaseItem,
                subtitle = s.resetDatabaseItemSub,
                icon = Icons.Default.DeleteForever,
                color = ErrorRed,
                onClick = { showResetDialog = true }
            )
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = color)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.5f))
            }
        }
    }
}
