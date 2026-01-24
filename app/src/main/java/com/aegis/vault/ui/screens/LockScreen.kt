package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.ui.theme.ElectricCyan
import com.aegis.vault.ui.theme.LocalStrings
import com.aegis.vault.ui.theme.MidnightBlue
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    onUnlockClick: () -> Unit,
    onPasswordSubmit: (String, (Boolean) -> Unit) -> Unit // success callback added
) {
    val s = LocalStrings.current
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableStateOf(0) }
    var lockoutTime by remember { mutableStateOf(0L) }
    var errorMessage by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }

    // Lockout Timer Logic
    LaunchedEffect(lockoutTime) {
        if (lockoutTime > 0) {
            while (lockoutTime > 0) {
                delay(1000)
                lockoutTime--
            }
            errorMessage = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlue),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background
        Box(
            modifier = Modifier
                .size(400.dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ElectricCyan.copy(alpha = 0.1f), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = ElectricCyan.copy(alpha = 0.1f),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isVerifying) {
                        CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(40.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = ElectricCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AEGIS VAULT",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = ElectricCyan
            )

            Text(
                text = if (isVerifying) s.unlocking else s.vaultLocked,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(s.enterMasterPassword) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isVerifying) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) s.hide else s.show,
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(0.85f),
                enabled = lockoutTime == 0L && !isVerifying,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (errorMessage.isNotEmpty() && !isVerifying) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (lockoutTime > 0) {
                val timeLabel = when {
                    lockoutTime >= 60 -> "${lockoutTime / 60} ${s.minuteSuffix}"
                    else -> "$lockoutTime ${s.secondsSuffix}"
                }
                Text(
                    text = "${s.tooManyAttempts} $timeLabel",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    isVerifying = true
                    onPasswordSubmit(password) { success ->
                        isVerifying = false
                        if (success) {
                            failedAttempts = 0
                            errorMessage = ""
                        } else {
                            failedAttempts++
                            password = ""
                            
                            when {
                                failedAttempts >= 20 -> lockoutTime = 3600 // 1 hour
                                failedAttempts >= 10 -> lockoutTime = 300  // 5 minutes
                                failedAttempts >= 3 -> lockoutTime = 60   // 1 minute
                                else -> errorMessage = "${s.wrongPassword}. ${s.remainingAttempts}: ${3 - failedAttempts}"
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp),
                enabled = password.isNotBlank() && lockoutTime == 0L && !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(s.unlockWithPassword, fontWeight = FontWeight.Bold)
                }
            }

            TextButton(
                onClick = onUnlockClick,
                enabled = lockoutTime == 0L && !isVerifying,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = ElectricCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(s.useBiometric, color = ElectricCyan)
            }
        }
    }
}
