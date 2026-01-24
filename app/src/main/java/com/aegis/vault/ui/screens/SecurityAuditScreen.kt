package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.data.local.VaultEntity
import com.aegis.vault.security.SecurityUtils
import com.aegis.vault.ui.theme.ElectricCyan
import com.aegis.vault.ui.theme.ErrorRed
import com.aegis.vault.ui.theme.LocalStrings
import com.aegis.vault.ui.theme.MidnightBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AuditResult(
    val totalCount: Int,
    val weakEntries: List<VaultEntity>,
    val reusedEntries: List<VaultEntity>, // Entries with duplicate passwords
    val secureCount: Int,
    val score: Int // 0-100
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityAuditScreen(
    entries: List<VaultEntity>,
    masterKey: ByteArray,
    isPremium: Boolean,
    onBack: () -> Unit,
    onPremiumUpgrade: () -> Unit
) {
    val s = LocalStrings.current
    var isAnalyzing by remember { mutableStateOf(true) }
    var auditResult by remember { mutableStateOf<AuditResult?>(null) }

    LaunchedEffect(entries) {
        withContext(Dispatchers.Default) {
            val decryptedPasswords = mutableMapOf<String, String>()
            val weak = mutableListOf<VaultEntity>()
            
            entries.forEach { entry ->
                try {
                    val decrypted = SecurityUtils.decrypt(entry.encryptedPayload, masterKey, entry.iv)
                    val pass = String(decrypted)
                    decryptedPasswords[entry.id] = pass
                    
                    if (SecurityUtils.getPasswordStrength(pass) == SecurityUtils.PasswordStrength.WEAK ||
                        SecurityUtils.getPasswordStrength(pass) == SecurityUtils.PasswordStrength.MEDIUM) {
                        weak.add(entry)
                    }
                    SecurityUtils.wipeData(decrypted)
                } catch (e: Exception) { }
            }

            // Find reused passwords
            val passCounts = decryptedPasswords.values.groupingBy { it }.eachCount()
            val reused = entries.filter { entry ->
                val pass = decryptedPasswords[entry.id]
                pass != null && (passCounts[pass] ?: 0) > 1
            }

            val secure = entries.size - weak.size - reused.distinctBy { decryptedPasswords[it.id] }.size
            val score = if (entries.isEmpty()) 100 else {
                ((secure.toFloat() / entries.size) * 100).toInt()
            }

            auditResult = AuditResult(
                totalCount = entries.size,
                weakEntries = weak,
                reusedEntries = reused,
                secureCount = secure.coerceAtLeast(0),
                score = score
            )
            isAnalyzing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.securityAudit, fontWeight = FontWeight.Bold) },
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
    ) { padding ->
        if (isAnalyzing) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ElectricCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(s.analyzing, color = Color.White)
                }
            }
        } else {
            val result = auditResult ?: return@Scaffold
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Score Header
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                            CircularProgressIndicator(
                                progress = result.score / 100f,
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 8.dp,
                                color = if (result.score > 80) ElectricCyan else if (result.score > 50) Color.Yellow else ErrorRed,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Text("${result.score}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(s.auditScoreDesc, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 8.dp))
                    }
                }

                // Stats Cards
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(s.weak, "${result.weakEntries.size}", ErrorRed, Modifier.weight(1f))
                        StatCard(s.reused, "${result.reusedEntries.size}", Color.Yellow, Modifier.weight(1f))
                        StatCard(s.secure, "${result.secureCount}", ElectricCyan, Modifier.weight(1f))
                    }
                }

                if (!isPremium) {
                    item {
                        Card(
                            onClick = onPremiumUpgrade,
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ElectricCyan.copy(alpha = 0.1f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricCyan)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(s.premiumAudit, fontWeight = FontWeight.Bold, color = ElectricCyan)
                                }
                                Text(
                                    s.premiumAuditDesc,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Detailed List for Premium Users
                    if (result.weakEntries.isNotEmpty()) {
                        item {
                            Text(s.weakPasswordsCaps, color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        items(result.weakEntries) { entry ->
                            AuditItem(entry, s.easyToGuess, ErrorRed)
                        }
                    }

                    if (result.reusedEntries.isNotEmpty()) {
                        item {
                            Text(s.reusedPasswordsCaps, color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        items(result.reusedEntries) { entry ->
                            AuditItem(entry, s.alsoUsedInOtherAccounts, Color.Yellow)
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun AuditItem(entry: VaultEntity, reason: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(entry.title, fontWeight = FontWeight.Bold, color = Color.White)
                Text(reason, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}
