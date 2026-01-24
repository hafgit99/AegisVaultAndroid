package com.aegis.vault.ui.screens

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.data.local.VaultEntity
import com.aegis.vault.security.SecurityUtils
import com.aegis.vault.ui.theme.ElectricCyan
import com.aegis.vault.ui.theme.LocalStrings
import com.aegis.vault.ui.theme.MidnightBlue
import java.util.UUID

// Dosya seçim sonucu için data class
data class FilePickResult(
    val name: String,
    val type: String,
    val bytes: ByteArray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    initialEntry: VaultEntity? = null,
    masterKey: ByteArray,
    onDismiss: () -> Unit,
    onSave: (VaultEntity) -> Unit,
    onRequestFilePick: (() -> Unit)? = null,  // Activity'den gelen dosya seçici tetikleyici
    pickedFile: FilePickResult? = null         // Activity'den gelen seçilen dosya
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialEntry?.title ?: "") }
    var username by remember { mutableStateOf(initialEntry?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var website by remember { mutableStateOf(initialEntry?.website ?: "") }
    var totpSecret by remember { mutableStateOf(initialEntry?.totpSecret ?: "") }
    var showGenerator by remember { mutableStateOf(false) }
    
    // Dosya eki için state'ler
    var attachmentName by remember { mutableStateOf(initialEntry?.attachmentName ?: "") }
    var attachmentBytes by remember { mutableStateOf<ByteArray?>(null) }
    var attachmentType by remember { mutableStateOf(initialEntry?.attachmentType ?: "") }
    
    // Eğer yeni dosya seçildiyse güncelle
    LaunchedEffect(pickedFile) {
        pickedFile?.let {
            attachmentName = it.name
            attachmentType = it.type
            attachmentBytes = it.bytes
        }
    }

    val isFormValid = title.isNotBlank() && username.isNotBlank() && (password.isNotBlank() || initialEntry != null)

    if (showGenerator) {
        ModalBottomSheet(
            onDismissRequest = { showGenerator = false },
            containerColor = MidnightBlue
        ) {
            PasswordGeneratorSheet(onPasswordGenerated = {
                password = it
                showGenerator = false
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (initialEntry == null) s.addEntry else s.editEntry, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = s.cancel)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(s.titlePlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(s.usernamePlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (initialEntry != null) s.passwordNew else s.password) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { showGenerator = true }) {
                        Text("🎲", fontSize = 20.sp)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )

            OutlinedTextField(
                value = totpSecret,
                onValueChange = { totpSecret = it },
                label = { Text(s.totpPlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )

            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                label = { Text(s.websitePlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )
            
            // Dosya Eki Bölümü (Sadece callback varsa göster)
            if (onRequestFilePick != null) {
                Text(
                    s.attachment,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable { onRequestFilePick() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (attachmentName.isNotEmpty()) ElectricCyan else Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = if (attachmentName.isNotEmpty()) ElectricCyan else Color.White.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            if (attachmentName.isNotEmpty()) {
                                Column {
                                    Text(
                                        attachmentName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        formatFileSize(attachmentBytes?.size ?: 0),
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Text(
                                    s.attachmentPlaceholder,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                        
                        if (attachmentName.isNotEmpty()) {
                            IconButton(onClick = {
                                attachmentName = ""
                                attachmentBytes = null
                                attachmentType = ""
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = s.removeFile,
                                    tint = Color.Red.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                
                Text(
                    s.supportedFormats,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Şifreyi şifrele
                    val (encrypted, iv) = if (password.isNotBlank()) {
                        SecurityUtils.encrypt(password.toByteArray(), masterKey)
                    } else {
                        Pair(initialEntry?.encryptedPayload ?: ByteArray(0), initialEntry?.iv ?: ByteArray(0))
                    }
                    
                    // Dosya ekini şifrele
                    val (encryptedAttachment, attachmentIv) = if (attachmentBytes != null) {
                        SecurityUtils.encrypt(attachmentBytes!!, masterKey)
                    } else if (initialEntry?.encryptedAttachment != null && attachmentName.isEmpty()) {
                        Pair(initialEntry.encryptedAttachment, initialEntry.attachmentIv)
                    } else {
                        Pair(null, null)
                    }
                    
                    val newEntry = VaultEntity(
                        id = initialEntry?.id ?: UUID.randomUUID().toString(),
                        title = title,
                        username = username,
                        website = website,
                        encryptedPayload = encrypted,
                        iv = iv,
                        totpSecret = totpSecret.takeIf { it.isNotBlank() },
                        lastModified = System.currentTimeMillis(),
                        attachmentName = if (attachmentName.isNotEmpty()) attachmentName else null,
                        attachmentType = if (attachmentType.isNotEmpty()) attachmentType else null,
                        encryptedAttachment = encryptedAttachment,
                        attachmentIv = attachmentIv
                    )
                    onSave(newEntry)
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (initialEntry == null) s.save else s.update,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Int): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
