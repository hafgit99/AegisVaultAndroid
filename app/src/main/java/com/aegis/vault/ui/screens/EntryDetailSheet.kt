package com.aegis.vault.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableIntStateOf
import com.aegis.vault.ui.theme.StringsTR
import com.aegis.vault.data.local.VaultEntity
import com.aegis.vault.security.SecurityUtils
import com.aegis.vault.ui.theme.ElectricCyan
import com.aegis.vault.ui.theme.ErrorRed
import com.aegis.vault.ui.theme.LocalStrings
import com.aegis.vault.security.TotpUtils
import kotlinx.coroutines.delay

@Composable
fun EntryDetailSheet(
    entry: VaultEntity,
    masterKey: ByteArray,
    onDismiss: () -> Unit,
    onDelete: (VaultEntity) -> Unit,
    onEdit: (VaultEntity) -> Unit,
    onRequestFileSave: (String, ByteArray) -> Unit // Dosya kaydetme isteği
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var totpCode by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableIntStateOf(30) }
    
    if (entry.totpSecret != null) {
        LaunchedEffect(Unit) {
            while(true) {
                totpCode = SecurityUtils.getTotpCode(entry.totpSecret)
                timeRemaining = TotpUtils.getTimeRemaining()
                delay(1000)
            }
        }
    }

    val decryptedPassword = remember(entry) {
        try {
            val decrypted = SecurityUtils.decrypt(entry.encryptedPayload, masterKey, entry.iv)
            val pass = String(decrypted)
            SecurityUtils.wipeData(decrypted)
            pass
        } catch (e: Exception) {
            s.decryptError
        }
    }
    
    val strength = remember(decryptedPassword) {
        SecurityUtils.getPasswordStrength(decryptedPassword)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = entry.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ElectricCyan
        )
        
        Text(
            text = entry.website,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (entry.totpSecret != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElectricCyan.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(s.totpTitle, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    Text("$timeRemaining ${s.secondsSuffix}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                }
                Text(
                    text = totpCode.chunked(3).joinToString(" "),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    letterSpacing = 4.sp
                )
                LinearProgressIndicator(
                    progress = timeRemaining / 30f,
                    modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 8.dp),
                    color = ElectricCyan,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Attachment Section
        if (entry.attachmentName != null && entry.encryptedAttachment != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    s.attachment,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    entry.attachmentName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    entry.attachmentType ?: (if(s is StringsTR) "Bilinmeyen Tür" else "Unknown Type"),
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        IconButton(onClick = {
                            try {
                                val decryptedAttachment = SecurityUtils.decrypt(
                                    entry.encryptedAttachment, 
                                    masterKey, 
                                    entry.attachmentIv ?: entry.iv
                                )
                                onRequestFileSave(entry.attachmentName, decryptedAttachment)
                            } catch (e: Exception) {
                                Toast.makeText(context, s.attachmentDecryptError, Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Download, contentDescription = if(s is StringsTR) "İndir" else "Download", tint = ElectricCyan)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Password Strength Meter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    s.securityAnalysis,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = when(strength) {
                        SecurityUtils.PasswordStrength.WEAK -> s.weakCaps
                        SecurityUtils.PasswordStrength.MEDIUM -> s.mediumCaps
                        SecurityUtils.PasswordStrength.STRONG -> s.strongCaps
                        SecurityUtils.PasswordStrength.VERY_STRONG -> s.excellentCaps
                        else -> s.unknownCaps
                    },
                    color = when(strength) {
                        SecurityUtils.PasswordStrength.WEAK -> ErrorRed
                        SecurityUtils.PasswordStrength.MEDIUM -> Color(0xFFFFA500)
                        SecurityUtils.PasswordStrength.STRONG -> Color.Yellow
                        SecurityUtils.PasswordStrength.VERY_STRONG -> ElectricCyan
                        else -> Color.Gray
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = when(strength) {
                    SecurityUtils.PasswordStrength.WEAK -> 0.25f
                    SecurityUtils.PasswordStrength.MEDIUM -> 0.5f
                    SecurityUtils.PasswordStrength.STRONG -> 0.75f
                    SecurityUtils.PasswordStrength.VERY_STRONG -> 1.0f
                    else -> 0f
                },
                modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(3.dp)),
                color = when(strength) {
                    SecurityUtils.PasswordStrength.WEAK -> ErrorRed
                    SecurityUtils.PasswordStrength.MEDIUM -> Color(0xFFFFA500)
                    SecurityUtils.PasswordStrength.STRONG -> Color.Yellow
                    SecurityUtils.PasswordStrength.VERY_STRONG -> ElectricCyan
                    else -> Color.Gray
                },
                trackColor = Color.Transparent
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = s.usernamePlaceholder, value = entry.username, onCopy = {
            copyToClipboard(context, entry.username, s.usernameCopied, s.clipboardCleaned)
        })

        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = s.password, value = "••••••••••••", onCopy = {
            copyToClipboard(context, decryptedPassword, s.passwordCopied, s.clipboardCleaned)
        })

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { onEdit(entry) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(s.update, color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onDelete(entry) },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ErrorRed))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(s.delete, color = ErrorRed, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String, onCopy: () -> Unit) {
    val s = LocalStrings.current
    val contentDescription = if(s is StringsTR) "Kopyala" else "Copy"
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.4f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = contentDescription, tint = ElectricCyan)
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String, message: String, cleanMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("AegisVault", text)
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val extras = PersistableBundle()
        extras.putBoolean("android.content.extra.IS_SENSITIVE", true)
        clip.description.extras = extras
    }
    
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    handler.postDelayed({
        if (clipboard.primaryClip?.getItemAt(0)?.text == text) {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            Toast.makeText(context, cleanMessage, Toast.LENGTH_SHORT).show()
        }
    }, 30000)
}
