package com.aegis.vault.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.security.SecurityUtils
import com.aegis.vault.ui.theme.ElectricCyan
import com.aegis.vault.ui.theme.ErrorRed
import com.aegis.vault.ui.theme.LocalStrings
import com.aegis.vault.ui.theme.MidnightBlue
import androidx.compose.material.icons.filled.Language

// Callback artık sadece şifre ve kurtarma kelimelerini alıyor, işlem MainActivity'de yapılacak
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onLanguageChange: (String) -> Unit,
    currentLanguage: String,
    onSetupComplete: (password: String, recoveryPhrase: String) -> Unit
) {
    var setupStep by remember { mutableIntStateOf(0) }
    val s = LocalStrings.current
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val recoveryPhrase = remember { SecurityUtils.generateRecoveryPhrase() }

    Crossfade(
        targetState = setupStep,
        animationSpec = tween(500),
        label = "SetupStepTransition"
    ) { step ->
        when (step) {
            0 -> {
                // Step 0: Language Selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Language / Dil",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = { 
                            onLanguageChange("en")
                            setupStep = 1 
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if(currentLanguage == "en") ElectricCyan else Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ENGLISH", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { 
                            onLanguageChange("tr")
                            setupStep = 1 
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if(currentLanguage == "tr") ElectricCyan else Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("TÜRKÇE", fontWeight = FontWeight.Bold)
                    }
                }
            }
            1 -> {
                // Step 1: Master Password
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        s.setupVault,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        s.setupDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    val hasMinLength = password.length >= 12
                    val hasUppercase = password.any { it.isUpperCase() }
                    val hasLowercase = password.any { it.isLowerCase() }
                    val hasDigit = password.any { it.isDigit() }
                    val hasSpecial = password.any { !it.isLetterOrDigit() }
                    val passwordsMatch = password.trim() == confirmPassword.trim() && password.trim().isNotEmpty()
                    
                    val isPasswordValid = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecial

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(s.masterPassword) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            focusedLabelColor = ElectricCyan,
                            cursorColor = ElectricCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Requirements UI
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        RequirementItem(s.reqMinLength, hasMinLength)
                        RequirementItem(s.reqUpperLower, hasUppercase && hasLowercase)
                        RequirementItem(s.reqDigit, hasDigit)
                        RequirementItem(s.reqSpecial, hasSpecial)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(s.confirmPassword) },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (passwordsMatch) ElectricCyan else Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = ElectricCyan,
                            cursorColor = ElectricCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                        Text(
                            s.passwordMismatch,
                            color = Color.Red.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { setupStep = 2 },
                        enabled = isPasswordValid && passwordsMatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(s.continueCaps, fontWeight = FontWeight.Bold)
                    }
                }
            }
            2 -> {
                // Step 2: Recovery Phrase
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        s.recoveryPhrase,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        s.recoveryDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed,
                        modifier = Modifier.padding(vertical = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Text(
                            text = recoveryPhrase,
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = ElectricCyan,
                            lineHeight = 32.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = {
                            onSetupComplete(password, recoveryPhrase)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(s.savedStart, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RequirementItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isMet) ElectricCyan else Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (isMet) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}
