package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.security.SecurityUtils
import com.aegis.vault.ui.theme.ElectricCyan
import com.aegis.vault.ui.theme.LocalStrings
import com.aegis.vault.ui.theme.MidnightBlue

@Composable
fun PasswordGeneratorSheet(
    onPasswordGenerated: (String) -> Unit
) {
    val s = LocalStrings.current
    var passwordLength by remember { mutableStateOf(16f) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    
    var generatedPassword by remember {
        mutableStateOf(SecurityUtils.generatePassword(passwordLength.toInt(), includeUppercase, includeNumbers, includeSymbols))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            s.generator,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ElectricCyan
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Password Display Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = generatedPassword,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                IconButton(onClick = {
                    generatedPassword = SecurityUtils.generatePassword(
                        passwordLength.toInt(),
                        includeUppercase,
                        includeNumbers,
                        includeSymbols
                    )
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = s.refresh, tint = ElectricCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Length Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(s.length, color = Color.White.copy(alpha = 0.6f))
                Text("${passwordLength.toInt()}", color = ElectricCyan, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = passwordLength,
                onValueChange = { passwordLength = it },
                valueRange = 8f..32f,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Options
        GeneratorOption(s.includeUppercase, includeUppercase) { includeUppercase = it }
        GeneratorOption(s.includeNumbers, includeNumbers) { includeNumbers = it }
        GeneratorOption(s.includeSymbols, includeSymbols) { includeSymbols = it }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { onPasswordGenerated(generatedPassword) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(s.useThisPassword, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun GeneratorOption(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ElectricCyan,
                checkedTrackColor = ElectricCyan.copy(alpha = 0.5f)
            )
        )
    }
}
