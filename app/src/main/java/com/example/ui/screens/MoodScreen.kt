package com.example.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CyberCard
import com.example.ui.components.nusuPulsingNeonGlow
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(onNavigateBack: () -> Unit, nusuViewModel: NusuViewModel = viewModel()) {
    val context = LocalContext.current
    val activeTheme = LocalNusuTheme.current
    val moodEntries by nusuViewModel.allMoodEntries.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf("Happy") }
    var productivity by remember { mutableStateOf(50f) }
    var notes by remember { mutableStateOf("") }

    val moodOptions = listOf("Happy", "Focused", "Stressed", "Tired", "Creative", "Calm")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Check-in & Mood", color = activeTheme.primaryColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = activeTheme.secondaryColor)
                    }
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Log", tint = activeTheme.secondaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeTheme.cardBackgroundColor)
            )
        },
        containerColor = activeTheme.backgroundColor,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = activeTheme.primaryColor,
                contentColor = NusuPureWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Mood")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nusuPulsingNeonGlow()
        ) {
            if (moodEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No mood patterns logged yet.", color = activeTheme.secondaryColor.copy(alpha=0.6f))
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        CyberCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, contentDescription = "Metrics", modifier = Modifier.size(48.dp), tint = activeTheme.accentColor)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Interaction Patterns Analysis", color = NusuPureWhite.copy(alpha = 0.5f), fontSize = 12.sp)
                                    Text("Tracking ${moodEntries.size} emotional states", color = activeTheme.primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    items(moodEntries) { entry ->
                        val dateString = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
                        CyberCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(entry.moodTitle.uppercase(), color = activeTheme.primaryColor, fontWeight = FontWeight.Bold)
                                    Text(dateString, color = NusuPureWhite.copy(alpha = 0.5f), fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Productivity: ${entry.productivityLevel}%", color = activeTheme.successColor, fontSize = 12.sp)
                                if (entry.privateNotes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(entry.privateNotes, color = NusuPureWhite, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Log Status", color = activeTheme.primaryColor) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Current Mood", color = NusuPureWhite)
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            // First 3 options
                            moodOptions.take(3).forEach { m ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedMood == m) activeTheme.primaryColor else activeTheme.cardBackgroundColor)
                                        .clickable { selectedMood = m }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(m, color = if (selectedMood == m) activeTheme.backgroundColor else NusuPureWhite, fontSize = 12.sp)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            // Last 3 options
                            moodOptions.drop(3).forEach { m ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedMood == m) activeTheme.primaryColor else activeTheme.cardBackgroundColor)
                                        .clickable { selectedMood = m }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(m, color = if (selectedMood == m) activeTheme.backgroundColor else NusuPureWhite, fontSize = 12.sp)
                                }
                            }
                        }

                        Text("Productivity Limit: ${productivity.toInt()}%", color = NusuPureWhite)
                        Slider(
                            value = productivity,
                            onValueChange = { productivity = it },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = activeTheme.primaryColor,
                                activeTrackColor = activeTheme.secondaryColor
                            )
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Contextual triggers...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        nusuViewModel.insertMood(selectedMood, productivity.toInt(), notes)
                        showDialog = false
                        notes = ""
                        productivity = 50f
                        selectedMood = "Happy"
                    }, colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor)) {
                        Text("SAVE PATTERN", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("CANCEL", color = activeTheme.secondaryColor)
                    }
                },
                containerColor = activeTheme.cardBackgroundColor,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            )
        }
    }
}
