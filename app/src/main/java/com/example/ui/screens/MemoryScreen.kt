package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.NusuDialogue
import com.example.ui.components.CyberCard
import com.example.ui.components.nusuPulsingNeonGlow
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: NusuViewModel = viewModel()
) {
    val activeTheme = LocalNusuTheme.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Database flow streams
    val dialogues by viewModel.allDialogues.collectAsState()
    val snippets by viewModel.savedCodeSnippets.collectAsState()

    // Backup & Restore states
    var showImportDialog by remember { mutableStateOf(false) }
    var backupJsonInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    // Compute stats
    val totalDialogs = dialogues.size
    val totalSnippets = snippets.size
    val totalCharacters = dialogues.sumOf { it.messageText.length }
    // Simulated size calc: 2 bytes per char
    val totalBytes = totalCharacters * 2
    val sizeText = when {
        totalBytes < 1024 -> "$totalBytes B"
        totalBytes < 1024 * 1024 -> String.format(Locale.ROOT, "%.2f KB", totalBytes / 1024f)
        else -> String.format(Locale.ROOT, "%.2f MB", totalBytes / (1024f * 1024f))
    }

    // Filter snippets based on search query
    val filteredSnippets = remember(snippets, searchQuery) {
        if (searchQuery.isBlank()) {
            snippets
        } else {
            snippets.filter {
                it.messageText.contains(searchQuery, ignoreCase = true) ||
                (it.codeLanguage ?: "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Core Memory", color = activeTheme.primaryColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("memory_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = activeTheme.secondaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeTheme.cardBackgroundColor)
            )
        },
        containerColor = activeTheme.backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nusuPulsingNeonGlow()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("memory_scroll_column"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Section 1: Dashboard Stats Card
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth().testTag("memory_stats_card")) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Memory, contentDescription = "Memory", modifier = Modifier.size(36.dp), tint = activeTheme.accentColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("NUSU Cloud & Local Engine", color = NusuPureWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text("Status: Online & Healthy", color = activeTheme.successColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = activeTheme.accentColor.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatMiniBox(title = "Total dialogs", value = "$totalDialogs", activeTheme.secondaryColor)
                                StatMiniBox(title = "Code snippets", value = "$totalSnippets", activeTheme.primaryColor)
                                StatMiniBox(title = "Backup weight", value = sizeText, activeTheme.accentColor)
                            }
                        }
                    }
                }

                // Section 2: Backup Operations / Buttons
                item {
                    Text(
                        text = "BACKUP & RECOVERY",
                        color = NusuPureWhite.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Export Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val filepath = viewModel.exportBackup()
                                    if (filepath != null) {
                                        // Read file content and copy to clipboard for convenient backup storage
                                        val backupFile = File(filepath)
                                        if (backupFile.exists()) {
                                            val fileContent = backupFile.readText()
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Nusu Backup JSON", fileContent)
                                            clipboard.setPrimaryClip(clip)
                                            
                                            Toast.makeText(
                                                context,
                                                "Backup saved to: $filepath\nContent copied to clipboard!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Backup creation failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("export_backup_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, activeTheme.primaryColor)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Export", tint = activeTheme.primaryColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Core Storage Backup", color = NusuPureWhite, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Restore Button
                        Button(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("restore_backup_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.accentColor.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, activeTheme.accentColor)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Import", tint = activeTheme.accentColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore Memory Backup Container", color = NusuPureWhite, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Sentinel Recovery Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val success = viewModel.recoverFromPermanentSentinel()
                                    if (success) {
                                        Toast.makeText(context, "🛡️ Shield Auto-Recovery Complete! All dialogue and code data restored seamlessly from offline directories.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "No permanent brain backup file found yet. Chat to seed automatic archives.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("sentinel_recovery_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.successColor.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, activeTheme.successColor)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = "Sentinel System RECON", tint = activeTheme.successColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recon Permanent Sentinel Folders", color = NusuPureWhite, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Flush/Clear Button
                        Button(
                            onClick = {
                                viewModel.clearAllHistory()
                                Toast.makeText(context, "Memory modules cleared, system re-seeded", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("clear_history_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color.Red)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Purge", tint = androidx.compose.ui.graphics.Color.Red)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Emergency Purge Memory", color = NusuPureWhite, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Section 3: Code Snippet Locker / Vault
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SAVED CODE VAULT",
                            color = NusuPureWhite.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        if (totalSnippets > 0) {
                            Text(
                                text = "$totalSnippets Item(s)",
                                color = activeTheme.secondaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Search field
                item {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .testTag("code_search_input"),
                        placeholder = { Text("Filter code by name or language...", color = NusuPureWhite.copy(alpha = 0.4f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = activeTheme.secondaryColor) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = NusuPureWhite)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = activeTheme.cardBackgroundColor,
                            unfocusedContainerColor = activeTheme.cardBackgroundColor,
                            focusedTextColor = NusuPureWhite,
                            unfocusedTextColor = NusuPureWhite,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                if (filteredSnippets.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("empty_vault_card"),
                            colors = CardDefaults.cardColors(containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Code, contentDescription = "", modifier = Modifier.size(48.dp), tint = activeTheme.primaryColor.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isEmpty()) "No code snippets in backup storage yet." else "No snippets match '$searchQuery'",
                                    color = NusuPureWhite.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Codes generated during conversations with NUSU are automatically backed up here.",
                                    color = NusuPureWhite.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredSnippets, key = { it.id }) { snippet ->
                        SnippetLayoutCard(snippet, viewModel, activeTheme, context)
                    }
                }
            }
        }

        // Import Backup Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Restore Container File", color = activeTheme.primaryColor) },
                text = {
                    Column {
                        Text(
                            "Paste the JSON raw contents of a previously exported backup file to restore your dialog and coding memory directories:",
                            fontSize = 13.sp,
                            color = NusuPureWhite.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextField(
                            value = backupJsonInput,
                            onValueChange = { backupJsonInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .testTag("backup_import_textarea"),
                            placeholder = { Text("Paste JSON metadata here...", color = NusuPureWhite.copy(alpha = 0.4f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                focusedTextColor = NusuPureWhite,
                                unfocusedTextColor = NusuPureWhite
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (backupJsonInput.isNotBlank()) {
                                coroutineScope.launch {
                                    val success = viewModel.restoreBackup(backupJsonInput)
                                    if (success) {
                                        Toast.makeText(context, "Memory restore complete!", Toast.LENGTH_SHORT).show()
                                        showImportDialog = false
                                        backupJsonInput = ""
                                    } else {
                                        Toast.makeText(context, "Invalid backup payload", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeTheme.successColor)
                    ) {
                        Text("Restore Now", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel", color = NusuPureWhite)
                    }
                },
                containerColor = activeTheme.cardBackgroundColor
            )
        }
    }
}

@Composable
fun StatMiniBox(title: String, value: String, color: Color) {
    Column {
        Text(title, color = NusuPureWhite.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Normal)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SnippetLayoutCard(
    snippet: NusuDialogue,
    viewModel: NusuViewModel,
    activeTheme: NusuThemePreset,
    context: Context
) {
    val dateString = SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.getDefault()).format(Date(snippet.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .testTag("snippet_card_${snippet.id}"),
        colors = CardDefaults.cardColors(containerColor = activeTheme.cardBackgroundColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(activeTheme.primaryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = (snippet.codeLanguage ?: "CODE").uppercase(Locale.ROOT),
                            color = activeTheme.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = dateString,
                        color = NusuPureWhite.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
                
                Row {
                    // Copy code Button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Nusu Vault Code", snippet.messageText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied from Vault!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp).testTag("snippet_copy_${snippet.id}")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", modifier = Modifier.size(16.dp), tint = activeTheme.secondaryColor)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    // Delete Button
                    IconButton(
                        onClick = {
                            viewModel.deleteDialogue(snippet.id)
                            Toast.makeText(context, "Removed from Vault backup", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp).testTag("snippet_delete_${snippet.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = Color.Red.copy(alpha = 0.7f))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Format code block
            Text(
                text = snippet.messageText,
                color = NusuPureWhite,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )
        }
    }
}
