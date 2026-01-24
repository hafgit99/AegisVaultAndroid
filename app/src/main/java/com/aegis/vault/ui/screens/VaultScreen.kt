package com.aegis.vault.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.animation.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableIntStateOf
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.aegis.vault.data.local.VaultEntity
import com.aegis.vault.ui.theme.ElectricCyan
import com.aegis.vault.ui.theme.GlassWhite
import com.aegis.vault.ui.theme.MidnightBlue
import com.aegis.vault.ui.viewmodel.VaultViewModel
import com.aegis.vault.security.TotpUtils
import com.aegis.vault.security.SecurityUtils
import com.aegis.vault.ui.theme.LocalStrings
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onSettingsClick: () -> Unit,
    pickedFile: FilePickResult? = null,
    onFilePickRequest: () -> Unit,
    onClearPickedFile: () -> Unit,
    onFileSaveRequest: (String, ByteArray) -> Unit,
    onLockClick: () -> Unit
) {
    val s = LocalStrings.current
    val pagingEntries = viewModel.pagingEntries.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<VaultEntity?>(null) }
    var editingEntry by remember { mutableStateOf<VaultEntity?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    if (showAddSheet) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { 
                showAddSheet = false 
                onClearPickedFile()
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MidnightBlue) {
                AddEntryScreen(
                    masterKey = viewModel.masterKey ?: ByteArray(32),
                    onDismiss = { 
                        showAddSheet = false 
                        onClearPickedFile()
                    },
                    onSave = { entity ->
                        viewModel.addEntry(entity)
                        showAddSheet = false
                        onClearPickedFile()
                    },
                    onRequestFilePick = onFilePickRequest,
                    pickedFile = pickedFile
                )
            }
        }
    }

    if (editingEntry != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { 
                editingEntry = null 
                onClearPickedFile()
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MidnightBlue) {
                AddEntryScreen(
                    initialEntry = editingEntry,
                    masterKey = viewModel.masterKey ?: ByteArray(32),
                    onDismiss = { 
                        editingEntry = null 
                        onClearPickedFile()
                    },
                    onSave = { entity ->
                        viewModel.updateEntry(entity)
                        editingEntry = null
                        onClearPickedFile()
                    },
                    onRequestFilePick = onFilePickRequest,
                    pickedFile = pickedFile
                )
            }
        }
    }

    if (selectedEntry != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedEntry = null },
            containerColor = MidnightBlue,
            dragHandle = { BottomSheetDefaults.DragHandle(color = ElectricCyan.copy(alpha = 0.5f)) }
        ) {
            EntryDetailSheet(
                entry = selectedEntry!!,
                masterKey = viewModel.masterKey ?: ByteArray(32),
                onDismiss = { selectedEntry = null },
                onDelete = {
                    viewModel.deleteEntry(it)
                    selectedEntry = null
                },
                onEdit = {
                    editingEntry = it
                    selectedEntry = null
                },
                onRequestFileSave = onFileSaveRequest
            )
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MidnightBlue)) {
                CenterAlignedTopAppBar(
                    title = {
                        if (!isSearching) {
                            Text("AEGIS VAULT", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = ElectricCyan)
                        }
                    },
                    navigationIcon = {
                        if (!isSearching) {
                            IconButton(onClick = onLockClick) {
                                Icon(Icons.Default.Lock, contentDescription = s.lock, tint = ElectricCyan)
                            }
                        }
                    },
                    actions = {
                        if (!isSearching) {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = s.searchPlaceholder, tint = Color.White)
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, contentDescription = s.settings, tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )

                if (isSearching) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        placeholder = { Text(s.searchPlaceholder, color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricCyan) },
                        trailingIcon = {
                            IconButton(onClick = { 
                                isSearching = false 
                                viewModel.onSearchQueryChange("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = s.cancel, tint = Color.White)
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GlassWhite,
                            unfocusedContainerColor = GlassWhite,
                            focusedIndicatorColor = ElectricCyan,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = ElectricCyan,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = ElectricCyan,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ekle")
            }
        },
        containerColor = MidnightBlue
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                count = pagingEntries.itemCount,
                key = pagingEntries.itemKey { it.id },
                contentType = pagingEntries.itemContentType { "vault_entry" }
            ) { index ->
                val entry = pagingEntries[index]
                if (entry != null) {
                    VaultCard(
                        entry = entry,
                        onClick = { selectedEntry = entry }
                    )
                }
            }
        }
    }
}

@Composable
fun VaultCard(entry: VaultEntity, onClick: () -> Unit) {
    var totpCode by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableIntStateOf(30) }

    if (entry.totpSecret != null) {
        LaunchedEffect(Unit) {
            while (true) {
                totpCode = SecurityUtils.getTotpCode(entry.totpSecret)
                timeRemaining = TotpUtils.getTimeRemaining()
                delay(1000)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(ElectricCyan.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.title.take(1).uppercase(),
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = entry.username,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }

            if (entry.totpSecret != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = totpCode.chunked(3).joinToString(" "),
                        color = ElectricCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                    Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = timeRemaining / 30f,
                            modifier = Modifier.fillMaxSize(),
                            color = ElectricCyan,
                            strokeWidth = 2.dp,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            if (entry.hasAttachment()) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Dosya Eki",
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
