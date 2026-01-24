package com.aegis.vault.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.security.HardwareUtils
import com.aegis.vault.ui.theme.StringsTR
import com.aegis.vault.security.LicenseManager
import com.aegis.vault.ui.theme.ElectricCyan
import com.aegis.vault.ui.theme.LocalStrings
import com.aegis.vault.ui.theme.MidnightBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onLicenseValidated: (String) -> Unit,
    onBack: () -> Unit,
    isTrialExpired: Boolean
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val deviceId = remember { HardwareUtils.getDeviceId(context) }
    var licenseInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val wallets = listOf(
        "BTC (Bitcoin)" to "bc1qqsuljwzs32ckkqdrsdus7wgqzuetty3g0x47l7",
        "TRON (TRC20)" to "TQBz3q8Ddjap3K8QdFQHtJKBxbvXMCi62E",
        "ETH (Ethereum)" to "0x4bd17Cc073D08E3E021Fd315d840554c840843E1",
        "SOL (Solana)" to "81H1rKZHjpSsnr6Epumw9XVTfqAnqSHcTKm7D3VsEd74",
        "LTC (Litecoin)" to "LZC3egqj1K9aZ3i42HbsRWK7m1SbUgXmak",
        "BCH (Bitcoin Cash)" to "qzfd46kp4tguu8pxrs6gnux0qxndhnqk8sa83q08wm",
        "XTZ (Tezos)" to "tz1Tij1ujzkEyvA949x1q7EW17s6pUNbEUdV"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if(s is StringsTR) "Aegis Vault Premium" else "Aegis Vault Premium", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (!isTrialExpired) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = s.back)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightBlue,
                    titleContentColor = ElectricCyan,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = MidnightBlue
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isTrialExpired) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            s.trialExpired,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            s.trialExpiredSub,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Text(
                s.premiumDescription,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                s.premiumSubtitle,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Device ID Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(s.deviceIdLabel, color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(deviceId, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Device ID", deviceId)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, s.idCopied, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = s.idCopied, tint = ElectricCyan)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(s.cryptoPayments, color = ElectricCyan, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Text(s.cryptoDescription, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            wallets.forEach { (name, address) ->
                WalletItem(name, address)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // License Entry
            OutlinedTextField(
                value = licenseInput,
                onValueChange = { licenseInput = it },
                label = { Text(s.enterLicense) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (LicenseManager.verifyLicense(deviceId, licenseInput.trim())) {
                        onLicenseValidated(licenseInput.trim())
                    } else {
                        Toast.makeText(context, s.invalidLicense, Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                enabled = licenseInput.isNotBlank()
            ) {
                Text(s.activateLicense, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun WalletItem(name: String, address: String) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.03f),
        onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(name, address)
            clipboard.setPrimaryClip(clip)
            val s = (context.applicationContext as? com.aegis.vault.AegisApplication)?.let { 
                if(it.preferenceManager.language == "en") com.aegis.vault.ui.theme.StringsEN else com.aegis.vault.ui.theme.StringsTR 
            } ?: com.aegis.vault.ui.theme.StringsTR
            Toast.makeText(context, "$name ${s.addressCopied}", Toast.LENGTH_SHORT).show()
        }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(address, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, maxLines = 1)
            }
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        }
    }
}
