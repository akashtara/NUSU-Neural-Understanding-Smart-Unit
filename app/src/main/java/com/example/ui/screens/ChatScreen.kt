package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.speech.RecognizerIntent
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.NusuDialogue
import com.example.ui.components.AnimatedAICoreOrb
import com.example.ui.components.nusuEntranceTransition
import com.example.ui.components.nusuPulsingNeonGlow
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: NusuViewModel = viewModel()
) {
    val activeTheme = LocalNusuTheme.current
    val dialogues by viewModel.allDialogues.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val isVoiceFeedbackEnabled by viewModel.isVoiceFeedbackEnabled.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isProcessingComplexQuery by remember { mutableStateOf(false) }
    var isHudExpanded by remember { mutableStateOf(true) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showPlusMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val personalityMode by viewModel.personalityMode.collectAsState()
    val speechVoice by viewModel.speechVoice.collectAsState()
    val isTouchOverlayEnabled by viewModel.isTouchOverlayEnabled.collectAsState()
    val touchSensitivity by viewModel.touchSensitivity.collectAsState()

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                inputText = spokenText
            }
        }
    }

    // Live complexity checker of the query as the user is typing
    val isCurrentInputComplex = remember(inputText) {
        evaluateQueryComplexity(inputText)
    }

    // Advanced core state mapping to dictate pulsing speed, expansion velocity and layers
    val coreState = remember(isThinking, isProcessingComplexQuery, isCurrentInputComplex) {
        when {
            isThinking && isProcessingComplexQuery -> NeuralCoreState.THINKING_COMPLEX
            isThinking -> NeuralCoreState.THINKING_STANDARD
            !isThinking && isCurrentInputComplex -> NeuralCoreState.PRE_ACTIVATED
            else -> NeuralCoreState.IDLE
        }
    }

    // Custom background grid color
    val gridColor = activeTheme.primaryColor

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "NEURAL LINK",
                                color = activeTheme.primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                letterSpacing = 2.sp
                            )
                            Text(
                                "Companion Protocol active",
                                color = NusuPureWhite.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        // Sophisticated interactive pulsed indicator 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isHudExpanded = !isHudExpanded }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .border(0.5.dp, activeTheme.primaryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .background(activeTheme.cardBackgroundColor.copy(alpha = 0.4f))
                        ) {
                            NeuralCoreStatusIndicator(
                                coreState = coreState,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Icon(
                                imageVector = if (isHudExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle HUD",
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = activeTheme.secondaryColor)
                    }
                },
                actions = {
                    // Rotate Screen Action Button
                    IconButton(
                        onClick = { 
                            val activity = context as? android.app.Activity
                            val currentOrientation = activity?.requestedOrientation
                            if (currentOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                            viewModel.performSystemBackup()
                        },
                        modifier = Modifier.testTag("top_bar_rotate_screen")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rotate Screen",
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Voice Toggle Icon Button
                    IconButton(
                        onClick = { viewModel.setVoiceFeedbackEnabled(!isVoiceFeedbackEnabled) },
                        modifier = Modifier.testTag("top_bar_chat_voice_toggle")
                    ) {
                        Icon(
                            imageVector = if (isVoiceFeedbackEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (isVoiceFeedbackEnabled) "Mute Voice" else "Unmute Voice",
                            tint = if (isVoiceFeedbackEnabled) activeTheme.primaryColor else NusuPureWhite.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Clear / Reset Icon Button
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.testTag("top_bar_chat_clear_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = activeTheme.secondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Settings Icon Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("top_bar_chat_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Sentient Settings",
                            tint = activeTheme.secondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeTheme.cardBackgroundColor)
            )
        },
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(
                color = activeTheme.cardBackgroundColor.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Notch/Navigation bar safe area
            ) {
                // Outer cyber border for the command prompt
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(1.dp, activeTheme.primaryColor.copy(alpha = 0.15f))
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            IconButton(onClick = { showPlusMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add options",
                                    tint = activeTheme.primaryColor
                                )
                            }
                            DropdownMenu(
                                expanded = showPlusMenu,
                                onDismissRequest = { showPlusMenu = false },
                                modifier = Modifier.background(activeTheme.cardBackgroundColor)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rotate Screen", color = NusuPureWhite) },
                                    onClick = { 
                                        showPlusMenu = false 
                                        val activity = context as? android.app.Activity
                                        val currentOrientation = activity?.requestedOrientation
                                        if (currentOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        } else {
                                            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        }
                                        viewModel.performSystemBackup()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = "Rotate", tint = activeTheme.primaryColor) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add Photo / File", color = NusuPureWhite) },
                                    onClick = { showPlusMenu = false },
                                    leadingIcon = { Icon(Icons.Default.AddCircle, contentDescription = null, tint = activeTheme.primaryColor) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import Feature/Skill", color = NusuPureWhite) },
                                    onClick = { 
                                        showPlusMenu = false 
                                        viewModel.performSystemBackup()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = activeTheme.primaryColor) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Open Diagnostics", color = NusuPureWhite) },
                                    onClick = { 
                                        showPlusMenu = false 
                                        com.example.api.SystemErrorState.openDiagnosticsDrawer()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Diagnostics", tint = activeTheme.primaryColor) }
                                )
                            }
                        }

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .testTag("chat_input_text"),
                            placeholder = {
                                Text(
                                    "Enter system command...",
                                    color = NusuPureWhite.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = activeTheme.backgroundColor.copy(alpha = 0.7f),
                                unfocusedContainerColor = activeTheme.backgroundColor.copy(alpha = 0.5f),
                                focusedTextColor = NusuPureWhite,
                                unfocusedTextColor = NusuPureWhite,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak directly to NUSU...")
                                }
                                speechRecognizerLauncher.launch(intent)
                            },
                            modifier = Modifier.testTag("chat_mic_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Dictate", modifier = Modifier.size(20.dp), tint = activeTheme.primaryColor)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isThinking) {
                                    val userMsg = inputText
                                    isProcessingComplexQuery = evaluateQueryComplexity(userMsg)
                                    inputText = ""
                                    isThinking = true
                                    viewModel.sendMessage(userMsg) {
                                        isThinking = false
                                    }
                                }
                            },
                            enabled = inputText.isNotBlank() && !isThinking,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank() && !isThinking) activeTheme.primaryColor.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (inputText.isNotBlank() && !isThinking) activeTheme.primaryColor
                                    else activeTheme.secondaryColor.copy(alpha = 0.3f),
                                    CircleShape
                                )
                                .testTag("chat_send_button"),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (inputText.isNotBlank()) activeTheme.primaryColor else activeTheme.secondaryColor.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nusuPulsingNeonGlow()
                .drawCyberGrid(gridColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Animated Collapsible Matrix HUD
                AnimatedVisibility(
                    visible = isHudExpanded,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    NusuCoreMatrixHUD(
                        selectedModel = selectedModel,
                        coreState = coreState,
                        isThinking = isThinking,
                        onModelSelected = { model ->
                            viewModel.selectModel(model)
                        },
                        onCollapse = { isHudExpanded = false },
                        isVoiceFeedbackEnabled = isVoiceFeedbackEnabled,
                        onVoiceFeedbackToggle = { viewModel.setVoiceFeedbackEnabled(it) },
                        onClearAllHistory = { showClearConfirmDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (dialogues.size <= 1) { // Shows default greeting or empty setup
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NusuEmptyDiagHeader(activeTheme, selectedModel)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("chat_messages_list"),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(dialogues) { idx, dialogue ->
                            MessageBubble(
                                dialogue = dialogue,
                                modifier = Modifier.nusuEntranceTransition(minOf(idx, 4))
                            )
                        }
                        if (isThinking) {
                            item {
                                ThinkingHUDLoader(activeTheme)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "INITIALIZE TOTAL REBOOT?",
                    color = activeTheme.primaryColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Warning: This protocol will completely wipe all current dialogue history and telemetry data. The companion synapse loop will be reset to its initial neutral state.",
                    color = NusuPureWhite.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearAllHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_wipe_history_button")
                ) {
                    Text(
                        text = "CONFIRM WIPEOUT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmDialog = false },
                    modifier = Modifier.testTag("dismiss_wipe_history_button")
                ) {
                    Text(
                        text = "ABORT PROTOCOL",
                        color = activeTheme.secondaryColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.98f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, activeTheme.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "SENTIENT CONFIGURATION",
                    color = activeTheme.primaryColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("Personality Mode", color = NusuPureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    val modes = listOf("Calm (Default)", "Energetic", "Professional", "Sassy")
                    modes.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setPersonalityMode(mode) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (personalityMode == mode),
                                onClick = { viewModel.setPersonalityMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = activeTheme.primaryColor, unselectedColor = activeTheme.secondaryColor)
                            )
                            Text(mode, color = NusuPureWhite, modifier = Modifier.padding(start = 8.dp), fontFamily = FontFamily.Monospace)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Voice Profile", color = NusuPureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    val voices = listOf(
                        "en-IN" to "English (India)",
                        "hi-IN" to "Hindi (India)",
                        "en-US" to "English (US)",
                        "en-GB" to "English (UK)",
                        "en-AU" to "English (Australia)"
                    )
                    voices.forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSpeechVoice(code) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (speechVoice == code),
                                onClick = { viewModel.setSpeechVoice(code) },
                                colors = RadioButtonDefaults.colors(selectedColor = activeTheme.primaryColor, unselectedColor = activeTheme.secondaryColor)
                            )
                            Text(label, color = NusuPureWhite, modifier = Modifier.padding(start = 8.dp), fontFamily = FontFamily.Monospace)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("[DEV] System Touch Overlay", color = activeTheme.primaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        androidx.compose.material3.Switch(
                            checked = isTouchOverlayEnabled,
                            onCheckedChange = { viewModel.setTouchOverlayEnabled(it) },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = activeTheme.primaryColor, checkedTrackColor = activeTheme.secondaryColor)
                        )
                    }

                    if (isTouchOverlayEnabled) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text("Touch Sensitivity (Threshold): ${touchSensitivity.toInt()}", color = NusuPureWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            androidx.compose.material3.Slider(
                                value = touchSensitivity,
                                onValueChange = { viewModel.setTouchSensitivity(it) },
                                valueRange = 0f..50f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = activeTheme.primaryColor,
                                    activeTrackColor = activeTheme.primaryColor,
                                    inactiveTrackColor = activeTheme.secondaryColor.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(activeTheme.primaryColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "[DEV] Diagnostics Panel",
                                color = activeTheme.primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Track API errors & warnings",
                                color = NusuPureWhite.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Button(
                            onClick = {
                                showSettingsDialog = false
                                com.example.api.SystemErrorState.openDiagnosticsDrawer()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeTheme.primaryColor,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(28.dp).testTag("open_diagnostics_drawer_btn")
                        ) {
                            Text(
                                text = "LAUNCH",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.98f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, activeTheme.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE & ACTIVATE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

enum class NeuralCoreState {
    IDLE,
    PRE_ACTIVATED,
    THINKING_STANDARD,
    THINKING_COMPLEX
}

fun evaluateQueryComplexity(text: String): Boolean {
    if (text.isBlank()) return false
    val words = text.lowercase(Locale.ROOT).split(Regex("\\s+"))
    val complexKeywords = setOf(
        "code", "kotlin", "java", "script", "database", "query", "restore", "backup",
        "neural", "algorithm", "theme", "design", "logic", "implement", "create", "build",
        "function", "class", "programming", "complexity", "sequence", "framer", "motion",
        "pulse", "indicator", "activation", "compute", "calculate", "math", "synthesize", "encrypt"
    )
    val hasComplexKeyword = words.any { it in complexKeywords }
    val isLong = text.length > 35 || words.size > 5
    return hasComplexKeyword || isLong || text.contains("\n")
}

@Composable
fun NeuralCoreStatusIndicator(
    coreState: NeuralCoreState,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalNusuTheme.current

    // Framer Motion style sequence: re-bound animations when coreState transitions.
    // This allows keyframe timings, speeds, curves, and layers to dynamically adapt.
    key(coreState) {
        val infiniteTransition = rememberInfiniteTransition(label = "NeuralCoreSequence")

        // Period duration adjusts dynamically from ultra slow to high-intensity compile mode
        val duration = when (coreState) {
            NeuralCoreState.IDLE -> 1800
            NeuralCoreState.PRE_ACTIVATED -> 900
            NeuralCoreState.THINKING_STANDARD -> 600
            NeuralCoreState.THINKING_COMPLEX -> 320
        }

        // Animate central core breathe scale
        val coreScale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = when (coreState) {
                NeuralCoreState.IDLE -> 1.15f
                NeuralCoreState.PRE_ACTIVATED -> 1.3f
                NeuralCoreState.THINKING_STANDARD -> 1.45f
                NeuralCoreState.THINKING_COMPLEX -> 1.6f
            },
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "coreScale"
        )

        // Wave Layer 1
        val wave1Progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration * 2, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave1Progress"
        )

        // Wave Layer 2 (Staggered offset)
        val wave2Progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = duration * 2,
                    delayMillis = (duration * 0.6f).toInt(),
                    easing = LinearOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave2Progress"
        )

        // Holographic rotary swirl speed speeds up dramatically under complex workload
        val swirlRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = when (coreState) {
                        NeuralCoreState.IDLE -> 6000
                        NeuralCoreState.PRE_ACTIVATED -> 3000
                        NeuralCoreState.THINKING_STANDARD -> 1800
                        NeuralCoreState.THINKING_COMPLEX -> 800
                    },
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "swirlRotation"
        )

        val statusText = when (coreState) {
            NeuralCoreState.IDLE -> "CORE: LIVE"
            NeuralCoreState.PRE_ACTIVATED -> "CORE: READY"
            NeuralCoreState.THINKING_STANDARD -> "CORE: THINK"
            NeuralCoreState.THINKING_COMPLEX -> "NEURAL: HYPER"
        }

        val statusColor = when (coreState) {
            NeuralCoreState.IDLE -> activeTheme.secondaryColor
            NeuralCoreState.PRE_ACTIVATED -> activeTheme.primaryColor
            NeuralCoreState.THINKING_STANDARD -> activeTheme.accentColor
            NeuralCoreState.THINKING_COMPLEX -> activeTheme.glowColor
        }

        val indicatorBgColor = when (coreState) {
            NeuralCoreState.THINKING_COMPLEX -> activeTheme.accentColor.copy(alpha = 0.15f)
            else -> activeTheme.cardBackgroundColor.copy(alpha = 0.6f)
        }

        val borderStrokeColor = when (coreState) {
            NeuralCoreState.THINKING_COMPLEX -> activeTheme.glowColor.copy(alpha = 0.6f)
            NeuralCoreState.THINKING_STANDARD -> activeTheme.primaryColor.copy(alpha = 0.4f)
            else -> activeTheme.primaryColor.copy(alpha = 0.2f)
        }

        Row(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(indicatorBgColor)
                .border(1.dp, borderStrokeColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val baseRadius = size.width / 4

                    // Draw concentric staggered wave 1
                    if (coreState != NeuralCoreState.IDLE) {
                        val wave1Radius = baseRadius + (baseRadius * 1.5f * wave1Progress)
                        val wave1Alpha = (1f - wave1Progress) * 0.6f
                        drawCircle(
                            color = statusColor.copy(alpha = wave1Alpha),
                            radius = wave1Radius,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Draw concentric staggered wave 2
                    if (coreState == NeuralCoreState.THINKING_STANDARD || coreState == NeuralCoreState.THINKING_COMPLEX) {
                        val wave2Radius = baseRadius + (baseRadius * 1.5f * wave2Progress)
                        val wave2Alpha = (1f - wave2Progress) * 0.4f
                        drawCircle(
                            color = statusColor.copy(alpha = wave2Alpha),
                            radius = wave2Radius,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Draw holographic rotary notch ticks in HYPER mode
                    if (coreState == NeuralCoreState.THINKING_COMPLEX) {
                        val tickRadius = baseRadius * 2.2f
                        rotate(swirlRotation) {
                            for (i in 0 until 4) {
                                rotate(i * 90f) {
                                    drawLine(
                                        color = activeTheme.glowColor,
                                        start = Offset(center.x, center.y - tickRadius),
                                        end = Offset(center.x, center.y - tickRadius - 2.dp.toPx()),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }
                            }
                        }
                    }

                    // Inner radial emission light
                    drawCircle(
                        color = statusColor.copy(alpha = 0.15f),
                        radius = baseRadius * coreScale * 1.6f,
                        center = center
                    )

                    // Solid central core vector dot
                    drawCircle(
                        color = statusColor,
                        radius = baseRadius * coreScale,
                        center = center
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun NusuEmptyDiagHeader(activeTheme: NusuThemePreset, selectedModel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .testTag("empty_neural_orb_container"),
            contentAlignment = Alignment.Center
        ) {
            // Large pulsing neural core
            AnimatedAICoreOrb(modifier = Modifier.size(140.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic information
        Text(
            text = "NUSU INTELLIGENCE COMPANION",
            color = activeTheme.primaryColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "SYSTEM STATUS: ACTIVE // TRANSMISSION LOGS REDIRECTED",
            color = activeTheme.secondaryColor.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Diagnostic specs in HUD columns
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(activeTheme.cardBackgroundColor.copy(alpha = 0.4f))
                .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                DiagnosticLabel("CODENAME", "NUSU-CORE-v3")
                Spacer(modifier = Modifier.height(6.dp))
                DiagnosticLabel("STABILITY", "99.98% ERRIL")
            }
            Column {
                val engineLabel = if (selectedModel == "gemini-1.5-pro") "GEMINI-PRO-1.5" else "GEMINI-FLASH"
                DiagnosticLabel("ENGINE", engineLabel)
                Spacer(modifier = Modifier.height(6.dp))
                val latencyLabel = if (selectedModel == "gemini-1.5-pro") "410ms DUX" else "48ms DUX"
                DiagnosticLabel("LATENCY", latencyLabel)
            }
        }
    }
}

@Composable
fun NusuCoreMatrixHUD(
    selectedModel: String,
    coreState: NeuralCoreState,
    isThinking: Boolean,
    onModelSelected: (String) -> Unit,
    onCollapse: () -> Unit,
    isVoiceFeedbackEnabled: Boolean,
    onVoiceFeedbackToggle: (Boolean) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalNusuTheme.current
    
    // Telemetry values flickering dynamically
    var coreTemp by remember { mutableStateOf(37.2f) }
    var synapsesHz by remember { mutableStateOf(4.2f) }
    var networkLatency by remember { mutableStateOf(128) }
    var processingLoad by remember { mutableStateOf(12) }

    LaunchedEffect(isThinking, selectedModel) {
        val rx = kotlin.random.Random
        while (true) {
            delay(450)
            val baseTemp = if (selectedModel == "gemini-1.5-pro") 48.2f else 35.6f
            val tempMultiplier = if (isThinking) 1.5f else 1.0f
            val randomOffsetTemp = (rx.nextFloat() * 2.4f) - 1.2f
            coreTemp = (baseTemp + randomOffsetTemp) * tempMultiplier
            
            val baseHz = if (selectedModel == "gemini-1.5-pro") 8.4f else 4.1f
            val randomOffsetHz = if (isThinking) (rx.nextFloat() * 1.6f + 0.5f) else (rx.nextFloat() * 0.4f - 0.2f)
            synapsesHz = baseHz + randomOffsetHz
            
            networkLatency = if (selectedModel == "gemini-1.5-pro") {
                if (isThinking) rx.nextInt(640, 820) else rx.nextInt(220, 310)
            } else {
                if (isThinking) rx.nextInt(290, 380) else rx.nextInt(110, 150)
            }
            
            processingLoad = if (isThinking) {
                if (selectedModel == "gemini-1.5-pro") rx.nextInt(85, 97) else rx.nextInt(64, 78)
            } else {
                rx.nextInt(10, 22)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (selectedModel == "gemini-1.5-pro") activeTheme.glowColor.copy(alpha = 0.4f)
                else activeTheme.primaryColor.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.95f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isThinking) activeTheme.glowColor else activeTheme.successColor
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COGNITIVE SYNERGY MATRIX HUD",
                        color = NusuPureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Collapse HUD",
                    tint = NusuPureWhite.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onCollapse() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Core Selecting Blocks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Lite Core
                val isFlash = selectedModel == "gemini-1.5-flash"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isFlash) activeTheme.primaryColor.copy(alpha = 0.12f)
                            else Color.Black.copy(alpha = 0.3f)
                        )
                        .border(
                            1.dp,
                            if (isFlash) activeTheme.primaryColor else activeTheme.primaryColor.copy(alpha = 0.15f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onModelSelected("gemini-1.5-flash") }
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "NEURAL-LITE CORE",
                            color = if (isFlash) activeTheme.primaryColor else NusuPureWhite.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Model: Gemini Flash\nUltra-fast synapse, lower latency dialogues.",
                            color = NusuPureWhite.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            lineHeight = 11.sp
                        )
                    }
                }

                // Pro Core
                val isPro = selectedModel == "gemini-1.5-pro"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isPro) activeTheme.glowColor.copy(alpha = 0.12f)
                            else Color.Black.copy(alpha = 0.3f)
                        )
                        .border(
                            1.dp,
                            if (isPro) activeTheme.glowColor else activeTheme.primaryColor.copy(alpha = 0.15f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onModelSelected("gemini-1.5-pro") }
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "QUANTUM-PRO HUD",
                                color = if (isPro) activeTheme.glowColor else NusuPureWhite.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            if (isPro) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(activeTheme.glowColor, RoundedCornerShape(2.dp))
                                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                ) {
                                    Text("BEST", color = Color.Black, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Model: Gemini 3.1 Pro\nMax logical depth, complex coding synthesis.",
                            color = NusuPureWhite.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            lineHeight = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // INTERACTIVE SYSTEM UTILITIES BAR
            Text(
                text = "INTERACTIVE SYSTEM UTILITIES",
                color = NusuPureWhite.copy(alpha = 0.4f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Persistent Switch for voice audible feedback
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(activeTheme.cardBackgroundColor.copy(alpha = 0.7f))
                        .border(0.5.dp, activeTheme.primaryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isVoiceFeedbackEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Voice State Indicator",
                        tint = if (isVoiceFeedbackEnabled) activeTheme.primaryColor else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isVoiceFeedbackEnabled) "VOICE: AUDIBLE" else "VOICE: SILENT",
                        color = NusuPureWhite,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isVoiceFeedbackEnabled,
                        onCheckedChange = onVoiceFeedbackToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = activeTheme.primaryColor,
                            checkedTrackColor = activeTheme.primaryColor.copy(alpha = 0.4f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .scale(0.7f)
                            .testTag("chat_voice_feedback_switch")
                    )
                }

                // Clear/Reset core button
                Button(
                    onClick = onClearAllHistory,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.secondaryColor.copy(alpha = 0.15f),
                        contentColor = activeTheme.secondaryColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, activeTheme.secondaryColor.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("hud_clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Companion",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RESET SYSTEM",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Diagnostic progress loads
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYNAPSE MATRIX PROCESSING LOAD",
                        color = NusuPureWhite.copy(alpha = 0.4f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$processingLoad%",
                        color = if (processingLoad > 80) activeTheme.glowColor else activeTheme.primaryColor,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = processingLoad / 100f,
                    color = if (processingLoad > 80) activeTheme.glowColor else if (processingLoad > 55) activeTheme.accentColor else activeTheme.primaryColor,
                    trackColor = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Telemeters columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryItem(
                    label = "CORE TEMP",
                    value = String.format("%.1f°C", coreTemp),
                    valueColor = when {
                        coreTemp > 75 -> activeTheme.glowColor
                        coreTemp > 45 -> activeTheme.accentColor
                        else -> activeTheme.successColor
                    },
                    subtext = if (isThinking) "COMPUTING SPIKES" else "STABLE LINK"
                )
                TelemetryItem(
                    label = "SYNAPSE FREQ",
                    value = String.format("%.2f GHz", synapsesHz),
                    valueColor = activeTheme.secondaryColor,
                    subtext = if (selectedModel == "gemini-1.5-pro") "HYPER SPEED" else "ECO SYNERGY"
                )
                TelemetryItem(
                    label = "PING PROBE",
                    value = "$networkLatency ms",
                    valueColor = if (networkLatency > 500) activeTheme.accentColor else activeTheme.successColor,
                    subtext = "TLS SEC COMPLIANT"
                )
            }
        }
    }
}

@Composable
fun TelemetryItem(
    label: String,
    value: String,
    valueColor: Color,
    subtext: String
) {
    Column {
        Text(
            text = label,
            color = NusuPureWhite.copy(alpha = 0.3f),
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtext,
            color = NusuPureWhite.copy(alpha = 0.25f),
            fontFamily = FontFamily.Monospace,
            fontSize = 7.sp,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
fun DiagnosticLabel(label: String, value: String) {
    val activeTheme = LocalNusuTheme.current
    Column {
        Text(
            text = label,
            color = NusuPureWhite.copy(alpha = 0.4f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = activeTheme.secondaryColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ThinkingHUDLoader(activeTheme: NusuThemePreset) {
    val infiniteTransition = rememberInfiniteTransition(label = "LoaderSpin")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Spin"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(activeTheme.cardBackgroundColor.copy(alpha = 0.5f))
            .border(1.dp, activeTheme.accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("chat_thinking_indicator"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer(rotationZ = rotation),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = activeTheme.accentColor.copy(alpha = 0.2f),
                    radius = size.width / 2
                )
                drawArc(
                    color = activeTheme.accentColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "NEURAL COMPILATION IN PROGRESS",
                color = activeTheme.accentColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Synthesizing intelligence model matrix lines...",
                color = NusuPureWhite.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun MessageBubble(dialogue: NusuDialogue, modifier: Modifier = Modifier) {
    val activeTheme = LocalNusuTheme.current
    val context = LocalContext.current
    val isUser = dialogue.sender == "USER"
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    
    // Formatting system logs display
    val senderLabel = if (isUser) "[ACCESS_LINK://USER_AKASH]" else "[NEURAL_CORE://NUSU_SYSTEM]"
    val headerColor = if (isUser) activeTheme.primaryColor.copy(alpha = 0.8f) else activeTheme.secondaryColor.copy(alpha = 0.8f)

    val contentBgColor = if (isUser) {
        Brush.horizontalGradient(
            listOf(activeTheme.accentColor.copy(alpha = 0.15f), activeTheme.primaryColor.copy(alpha = 0.1f))
        )
    } else {
        Brush.horizontalGradient(
            listOf(activeTheme.cardBackgroundColor.copy(alpha = 0.9f), activeTheme.backgroundColor.copy(alpha = 0.8f))
        )
    }

    val bubbleBorder = BorderStroke(
        width = 1.dp,
        color = if (isUser) activeTheme.primaryColor.copy(alpha = 0.3f) else activeTheme.accentColor.copy(alpha = 0.2f)
    )

    val timeStampStr = remember(dialogue.timestamp) {
        val sdf = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.ROOT)
        sdf.format(Date(dialogue.timestamp))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("message_bubble_${dialogue.id}"),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(contentBgColor)
                .border(bubbleBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            // Diagnostic system header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                     text = senderLabel,
                     color = headerColor,
                     fontFamily = FontFamily.Monospace,
                     fontSize = 9.sp,
                     fontWeight = FontWeight.Bold,
                     letterSpacing = 0.5.sp
                )
                Text(
                    text = timeStampStr,
                    color = NusuPureWhite.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))

            if (!isUser && dialogue.isCodeSnippet) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Code",
                            modifier = Modifier.size(14.dp),
                            tint = activeTheme.secondaryColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = (dialogue.codeLanguage ?: "CODE").uppercase(Locale.ROOT),
                            color = activeTheme.secondaryColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "COPY",
                        color = activeTheme.primaryColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Nusu Code", dialogue.messageText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dialogue.messageText,
                    color = NusuPureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )
            } else {
                Text(
                    text = dialogue.messageText,
                    color = NusuPureWhite,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Draw futuristic overlay grid line background
fun Modifier.drawCyberGrid(color: Color): Modifier = this.drawBehind {
    val size = this.size
    val step = 32.dp.toPx()
    // Vertical grid lines
    var x = 0f
    while (x < size.width) {
        drawLine(
            color = color.copy(alpha = 0.05f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += step
    }
    // Horizontal grid lines
    var y = 0f
    while (y < size.height) {
        drawLine(
            color = color.copy(alpha = 0.05f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += step
    }
}
