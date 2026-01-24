package com.aegis.vault

import android.os.Bundle
import android.util.Base64
import android.view.WindowManager
import android.widget.Toast
import android.content.ClipboardManager
import android.content.Context
import android.content.ClipData
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.lifecycle.lifecycleScope
import com.aegis.vault.security.BiometricHelper
import com.aegis.vault.security.KeystoreManager
import androidx.biometric.BiometricPrompt
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.aegis.vault.security.SecurityUtils
import com.aegis.vault.ui.screens.LockScreen
import com.aegis.vault.ui.screens.SettingsScreen
import com.aegis.vault.ui.screens.SetupScreen
import com.aegis.vault.ui.screens.VaultScreen
import com.aegis.vault.ui.theme.AegisVaultTheme
import com.aegis.vault.ui.theme.ElectricCyan
import com.aegis.vault.ui.theme.MidnightBlue
import com.aegis.vault.ui.viewmodel.VaultViewModel
import com.aegis.vault.ui.viewmodel.VaultViewModelFactory
import com.aegis.vault.ui.screens.FilePickResult
import com.aegis.vault.ui.screens.PremiumScreen
import com.aegis.vault.ui.screens.SecurityAuditScreen
import com.aegis.vault.ui.screens.PasswordGeneratorSheet
import com.aegis.vault.ui.theme.LocalStrings
import com.aegis.vault.ui.theme.StringsEN
import com.aegis.vault.ui.theme.StringsTR

enum class Screen { Vault, Settings, Premium, Audit, Generator }

class MainActivity : AppCompatActivity() {
    private val viewModel: VaultViewModel by viewModels {
        VaultViewModelFactory((application as AegisApplication).repository)
    }

    private val _pickedFile = mutableStateOf<FilePickResult?>(null)
    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>>
    
    private var bytesToSave: ByteArray? = null
    private lateinit var fileSaverLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = contentResolver.openInputStream(it)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()
                        
                        if (bytes != null) {
                            var fileName = ""
                            val cursor = contentResolver.query(it, null, null, null, null)
                            cursor?.use { c ->
                                if (c.moveToFirst()) {
                                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                    if (nameIndex >= 0) fileName = c.getString(nameIndex)
                                }
                            }
                            if (fileName.isEmpty()) fileName = it.lastPathSegment ?: "dosya"
                            val fileType = contentResolver.getType(it) ?: "application/octet-stream"
                            withContext(Dispatchers.Main) {
                                _pickedFile.value = FilePickResult(fileName, fileType, bytes)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Dosya okuma hatası", e)
                    }
                }
            }
        }

        fileSaverLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
            uri?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        contentResolver.openOutputStream(it)?.use { os ->
                            bytesToSave?.let { bytes -> os.write(bytes) }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Dosya kaydedildi", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Kayıt hatası!", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        bytesToSave = null
                    }
                }
            }
        }

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        val prefManager = (application as AegisApplication).preferenceManager

        @OptIn(ExperimentalMaterial3Api::class)
        setContent {
            AegisVaultTheme {
                val pickedFile by _pickedFile
                var isSetupCompleteState by remember { mutableStateOf(prefManager.isSetupComplete) }
                var isLocked by remember { mutableStateOf(true) }
                var currentScreen by remember { mutableStateOf(Screen.Vault) }
                var isProcessingSetup by remember { mutableStateOf(false) }
                
                // Dil state yönetimi
                var appLanguage by remember { mutableStateOf(prefManager.language) }
                val strings = if (appLanguage == "en") StringsEN else StringsTR
                
                CompositionLocalProvider(LocalStrings provides strings) {

                // --- OTOMATİK KİLİTLEME MANTIĞI ---
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val lastBg = prefManager.lastBackgroundTime
                            val timeout = prefManager.autoLockTimeout
                            if (lastBg > 0 && timeout != -1L) {
                                val diff = System.currentTimeMillis() - lastBg
                                if (diff >= timeout) {
                                    isLocked = true
                                    viewModel.masterKey = null
                                }
                            }
                            prefManager.lastBackgroundTime = 0 // Sıfırla
                        } else if (event == Lifecycle.Event.ON_PAUSE) {
                            prefManager.lastBackgroundTime = System.currentTimeMillis()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                // ---------------------------------

                // Deneme süresi hesaplama
                val trialDaysLeft = remember(isSetupCompleteState) {
                    val days = 3 - ((System.currentTimeMillis() - prefManager.installTime) / (1000 * 60 * 60 * 24)).toInt()
                    days.coerceAtLeast(0)
                }
                val isTrialExpired = trialDaysLeft <= 0 && !prefManager.isPremium

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        isProcessingSetup -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(strings.creatingSecureKey, color = ElectricCyan, fontWeight = FontWeight.Bold)
                                    Text(strings.computingArgon2, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                            }
                        }
                        
                        !isSetupCompleteState -> {
                            SetupScreen(
                                currentLanguage = appLanguage,
                                onLanguageChange = { 
                                    appLanguage = it
                                    prefManager.language = it
                                },
                                onSetupComplete = { password, recoveryPhrase ->
                                isProcessingSetup = true
                                lifecycleScope.launch(Dispatchers.Default) {
                                    try {
                                        val salt = SecurityUtils.generateSalt()
                                        val verificationHash = SecurityUtils.deriveVerificationHash(password, salt)
                                        val encryptionKey = SecurityUtils.deriveEncryptionKey(password, salt)
                                        prefManager.isSetupComplete = true
                                        // Set install time if not already set
                                        val x = prefManager.installTime 
                                        prefManager.saveMasterPasswordData(
                                            Base64.encodeToString(verificationHash, Base64.NO_WRAP),
                                            Base64.encodeToString(salt, Base64.NO_WRAP)
                                        )
                                        prefManager.recoveryPhrase = recoveryPhrase
                                        withContext(Dispatchers.Main) {
                                            viewModel.masterKey = encryptionKey
                                            isProcessingSetup = false
                                            isSetupCompleteState = true
                                            isLocked = false
                                        }
                                    } catch (e: Throwable) {
                                        withContext(Dispatchers.Main) {
                                            isProcessingSetup = false
                                            Toast.makeText(this@MainActivity, if(appLanguage == "tr") "Hata: ${e.message}" else "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            })
                        }
                        
                        isLocked -> {
                            LockScreen(
                                onUnlockClick = {
                                    val bioKey = prefManager.bioMasterKey
                                    val bioIv = prefManager.bioMasterIv
                                    
                                    if (bioKey != null && bioIv != null) {
                                        // Parmak iziyle anahtarı çözme akışı
                                        try {
                                            val cipher = KeystoreManager.getDecryptCipher(Base64.decode(bioIv, Base64.NO_WRAP))
                                            BiometricHelper.showBiometricPrompt(
                                                this@MainActivity,
                                                BiometricPrompt.CryptoObject(cipher),
                                                onSuccess = { result ->
                                                    val decryptedCipher = result.cryptoObject?.cipher
                                                    if (decryptedCipher != null) {
                                                        val encryptedKeyBytes = Base64.decode(bioKey, Base64.NO_WRAP)
                                                        val masterKey = KeystoreManager.decryptMasterKey(encryptedKeyBytes, decryptedCipher)
                                                        viewModel.masterKey = masterKey
                                                        isLocked = false
                                                    }
                                                },
                                                onError = { _, _ -> }
                                            )
                                        } catch (e: Exception) {
                                            // Anahtar geçersizleşmişse (yeni parmak izi eklendiyse) temizle
                                            prefManager.clearBiometricData()
                                        }
                                    } else {
                                        // Henüz parmak izi anahtarı yoksa sadece yetkilendirme yap
                                        BiometricHelper.showBiometricPrompt(this@MainActivity, 
                                            onSuccess = { if (viewModel.masterKey != null) isLocked = false },
                                            onError = { _, _ -> })
                                    }
                                },
                                onPasswordSubmit = { input, onResult ->
                                    lifecycleScope.launch(Dispatchers.Default) {
                                        val salt = prefManager.salt
                                        val storedHash = prefManager.masterPasswordHash
                                        if (salt != null && storedHash != null) {
                                            val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
                                            val inputHash = SecurityUtils.deriveVerificationHash(input, saltBytes)
                                            if (Base64.encodeToString(inputHash, Base64.NO_WRAP) == storedHash) {
                                                val derivedKey = SecurityUtils.deriveEncryptionKey(input, saltBytes)
                                                
                                                // İlk kez şifre girildiğinde, anahtarı parmak iziyle korumak üzere hazırla
                                                withContext(Dispatchers.Main) {
                                                    viewModel.masterKey = derivedKey
                                                    isLocked = false
                                                    onResult(true)
                                                    
                                                    // Kullanıcıdan parmak iziyle anahtarı Keystore'a kilitlemesini iste
                                                    try {
                                                        val encryptCipher = KeystoreManager.getEncryptCipher()
                                                        BiometricHelper.showBiometricPrompt(
                                                            this@MainActivity,
                                                            BiometricPrompt.CryptoObject(encryptCipher),
                                                            onSuccess = { result ->
                                                                val cipher = result.cryptoObject?.cipher
                                                                if (cipher != null) {
                                                                    val (encrypted, iv) = KeystoreManager.encryptMasterKey(derivedKey, cipher)
                                                                    prefManager.saveBiometricMasterKey(
                                                                        Base64.encodeToString(encrypted, Base64.NO_WRAP),
                                                                        Base64.encodeToString(iv, Base64.NO_WRAP)
                                                                    )
                                                                }
                                                            },
                                                            onError = { _, _ -> }
                                                        )
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("MainActivity", "Keystore init failed", e)
                                                    }
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) { onResult(false) }
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) { onResult(false) }
                                        }
                                    }
                                }
                            )
                        }
                        
                        isTrialExpired || currentScreen == Screen.Premium -> {
                            PremiumScreen(
                                isTrialExpired = isTrialExpired,
                                onBack = { currentScreen = Screen.Vault },
                                onLicenseValidated = { key ->
                                    prefManager.licenseKey = key
                                    currentScreen = Screen.Vault
                                    Toast.makeText(this@MainActivity, "Premium Aktif Edildi!", Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        else -> {
                            when (currentScreen) {
                                Screen.Vault -> VaultScreen(
                                    viewModel = viewModel, 
                                    onSettingsClick = { currentScreen = Screen.Settings },
                                    pickedFile = pickedFile,
                                    onFilePickRequest = { 
                                        _pickedFile.value = null
                                        filePickerLauncher.launch(arrayOf("*/*")) 
                                    },
                                    onClearPickedFile = { _pickedFile.value = null },
                                    onFileSaveRequest = { name, bytes ->
                                        bytesToSave = bytes
                                        fileSaverLauncher.launch(name)
                                    },
                                    onLockClick = {
                                        viewModel.masterKey = null
                                        isLocked = true
                                    }
                                )
                                Screen.Settings -> {
                                    val entries by viewModel.allEntries.collectAsState(initial = emptyList())
                                    SettingsScreen(
                                        onBack = { currentScreen = Screen.Vault },
                                        onResetDatabase = {
                                            viewModel.deleteAllEntries()
                                            prefManager.isSetupComplete = false
                                            isSetupCompleteState = false
                                            isLocked = true
                                        },
                                        onChangeMasterPassword = { oldPass, newPass, onResult ->
                                            val salt = prefManager.salt
                                            val storedHash = prefManager.masterPasswordHash
                                            if (salt != null && storedHash != null) {
                                                val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
                                                val inputHash = SecurityUtils.deriveVerificationHash(oldPass, saltBytes)
                                                
                                                if (Base64.encodeToString(inputHash, Base64.NO_WRAP) == storedHash) {
                                                    lifecycleScope.launch(Dispatchers.Default) {
                                                        try {
                                                            val oldKey = viewModel.masterKey ?: SecurityUtils.deriveEncryptionKey(oldPass, saltBytes)
                                                            val newSalt = SecurityUtils.generateSalt()
                                                            val newKey = SecurityUtils.deriveEncryptionKey(newPass, newSalt)
                                                            val newVerificationHash = SecurityUtils.deriveVerificationHash(newPass, newSalt)
                                                            
                                                            val encryptedEntries = entries.map { entry ->
                                                                val passBytes = SecurityUtils.decrypt(entry.encryptedPayload, oldKey, entry.iv)
                                                                val (newEnc, newIv) = SecurityUtils.encrypt(passBytes, newKey)
                                                                
                                                                var newAttachmentIv = entry.attachmentIv
                                                                val newEncAttachment = entry.encryptedAttachment?.let { attachment ->
                                                                    val decAtt = SecurityUtils.decrypt(attachment, oldKey, entry.attachmentIv ?: entry.iv)
                                                                    val (encAtt, attIv) = SecurityUtils.encrypt(decAtt, newKey)
                                                                    newAttachmentIv = attIv
                                                                    encAtt
                                                                }

                                                                entry.copy(
                                                                    encryptedPayload = newEnc,
                                                                    iv = newIv,
                                                                    encryptedAttachment = newEncAttachment,
                                                                    attachmentIv = newAttachmentIv
                                                                )
                                                            }
                                                            
                                                            encryptedEntries.forEach { viewModel.updateEntry(it) }
                                                            
                                                            prefManager.saveMasterPasswordData(
                                                                Base64.encodeToString(newVerificationHash, Base64.NO_WRAP),
                                                                Base64.encodeToString(newSalt, Base64.NO_WRAP)
                                                            )
                                                            
                                                            withContext(Dispatchers.Main) {
                                                                viewModel.masterKey = newKey
                                                                onResult(true)
                                                            }
                                                        } catch (e: Exception) {
                                                            android.util.Log.e("MainActivity", "Re-encryption failed", e)
                                                            withContext(Dispatchers.Main) { onResult(false) }
                                                        }
                                                    }
                                                } else {
                                                    onResult(false)
                                                }
                                            } else {
                                                onResult(false)
                                            }
                                        },
                                        masterKey = viewModel.masterKey ?: ByteArray(32),
                                        allEntries = entries,
                                        onImportEntries = { list -> list.forEach { viewModel.addEntry(it) } },
                                        onPremiumClick = { currentScreen = Screen.Premium },
                                        onAuditClick = { currentScreen = Screen.Audit },
                                        onPasswordGenClick = { currentScreen = Screen.Generator },
                                        autoLockTimeout = prefManager.autoLockTimeout,
                                        onAutoLockChange = { prefManager.autoLockTimeout = it },
                                        currentLanguage = appLanguage,
                                        onLanguageChange = { 
                                            appLanguage = it
                                            prefManager.language = it
                                        },
                                        isPremium = prefManager.isPremium,
                                        trialDaysLeft = trialDaysLeft
                                    )
                                }
                                Screen.Generator -> {
                                    Scaffold(
                                        topBar = {
                                            TopAppBar(
                                                title = { Text(strings.generator, fontWeight = FontWeight.Bold) },
                                                navigationIcon = {
                                                    IconButton(onClick = { currentScreen = Screen.Settings }) {
                                                        Icon(Icons.Default.ArrowBack, contentDescription = strings.back)
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
                                    ) { p ->
                                        Box(modifier = Modifier.padding(p)) {
                                            PasswordGeneratorSheet(onPasswordGenerated = { pass ->
                                                val clipboard = this@MainActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("AegisGenerated", pass))
                                                Toast.makeText(this@MainActivity, strings.passwordCopied, Toast.LENGTH_SHORT).show()
                                            })
                                        }
                                    }
                                }
                                Screen.Audit -> {
                                    val entries by viewModel.allEntries.collectAsState(initial = emptyList())
                                    SecurityAuditScreen(
                                        entries = entries,
                                        masterKey = viewModel.masterKey ?: ByteArray(32),
                                        isPremium = prefManager.isPremium,
                                        onBack = { currentScreen = Screen.Settings },
                                        onPremiumUpgrade = { currentScreen = Screen.Premium }
                                    )
                                }
                                Screen.Premium -> {
                                    // Managed by the top-level branch, but kept here for clarity
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
