package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimatedAICoreOrb
import com.example.ui.components.nusuEntranceTransition
import com.example.ui.components.nusuPulsingNeonGlow
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AestheticHubScreen(
    onNavigateBack: () -> Unit
) {
    val currentTheme by ThemeManager.currentTheme.collectAsState()
    val favorites by ThemeManager.favoriteThemeIds.collectAsState()

    // Preview state - allows selecting a theme to preview BEFORE applying
    var selectedPreviewTheme by remember { mutableStateOf(currentTheme) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCollectionFilter by remember { mutableStateOf("All") }
    var generativePrompt by remember { mutableStateOf("") }
    var isSynthesizing by remember { mutableStateOf(false) }
    var showSynthesisSuccess by remember { mutableStateOf(false) }

    val collections = listOf("All", "Bento", "Original", "Neon", "Cyberpunk", "Holographic", "Quantum", "Jarvis", "Matrix", "Luxury")

    // Filter themes
    val filteredThemes = remember(searchQuery, selectedCollectionFilter, currentTheme) {
        ThemeManager.premiumThemes.filter { theme ->
            val matchesSearch = theme.name.lowercase().contains(searchQuery.lowercase()) ||
                    theme.collectionName.lowercase().contains(searchQuery.lowercase())
            val matchesCollection = if (selectedCollectionFilter == "All") true else {
                theme.collectionName.lowercase().contains(selectedCollectionFilter.lowercase())
            }
            matchesSearch && matchesCollection
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NUSU AESTHETIC HUB PRO™",
                            color = currentTheme.primaryColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Design Your Own Future",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = currentTheme.primaryColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        ThemeManager.restorePreviousTheme()
                        selectedPreviewTheme = ThemeManager.currentTheme.value
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recall Previous OS Theme",
                            tint = currentTheme.secondaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = currentTheme.backgroundColor
                )
            )
        },
        containerColor = Color.Transparent
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // UI Core System Live Preview Deck
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "LIVE SYSTEM PREVIEW",
                        color = selectedPreviewTheme.primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Miniature Visual Dashboard Sandbox in selected theme colors
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nusuEntranceTransition(0)
                            .border(1.dp, selectedPreviewTheme.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = selectedPreviewTheme.cardBackgroundColor.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("NUSU PRO OS", color = selectedPreviewTheme.primaryColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Preview Sandbox Mode", color = Color.Gray, fontSize = 9.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(selectedPreviewTheme.successColor)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            // Miniature Core Orb using selected colors
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(selectedPreviewTheme.backgroundColor, CircleShape)
                                    .border(1.dp, selectedPreviewTheme.primaryColor.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                KeyedAICoreOrb(
                                    primaryColor = selectedPreviewTheme.primaryColor,
                                    secondaryColor = selectedPreviewTheme.secondaryColor,
                                    innerColor = selectedPreviewTheme.orbInnerColor,
                                    outerColor = selectedPreviewTheme.orbOuterColor,
                                    modifier = Modifier.size(75.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Miniature Glass Card Mockup
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(55.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(selectedPreviewTheme.backgroundColor.copy(alpha = 0.5f))
                                        .border(1.dp, selectedPreviewTheme.secondaryColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text("WIDGET ALPHA", color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                        Text("Telemetry Sync", color = NusuPureWhite, fontSize = 9.sp)
                                    }
                                }

                                // Interactive Swatch colors representation
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(55.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(selectedPreviewTheme.backgroundColor.copy(alpha = 0.5f))
                                        .border(1.dp, selectedPreviewTheme.accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text("DESIGN SWATCHES", color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(selectedPreviewTheme.primaryColor))
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(selectedPreviewTheme.secondaryColor))
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(selectedPreviewTheme.accentColor))
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(selectedPreviewTheme.backgroundColor))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Generative Theme Studio Suite
                item {
                    Text(
                        text = "AI GENERATIVE SYSTEM TRANSFORMATION",
                        color = currentTheme.secondaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nusuEntranceTransition(1)
                            .border(1.dp, currentTheme.secondaryColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = currentTheme.cardBackgroundColor.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Describe your conceptual future design. Nusu AI will synthesize custom color systems, dynamic orbs, glass panels, and typography on-the-fly.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = generativePrompt,
                                onValueChange = { generativePrompt = it },
                                placeholder = { Text("e.g., molten fiery lava core with dark volcanic stone", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Generative AI",
                                        tint = currentTheme.secondaryColor
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = currentTheme.secondaryColor.copy(alpha = 0.2f),
                                    focusedBorderColor = currentTheme.secondaryColor,
                                    focusedLabelColor = currentTheme.secondaryColor,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (generativePrompt.isNotBlank()) {
                                        isSynthesizing = true
                                        // Delay simulate heavy synthesis algorithm compile
                                        showSynthesisSuccess = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = currentTheme.secondaryColor.copy(alpha = 0.15f),
                                    contentColor = currentTheme.secondaryColor
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isSynthesizing && generativePrompt.isNotBlank()
                            ) {
                                if (isSynthesizing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = currentTheme.secondaryColor, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SYNTHESIZING DESIGN COMPASS...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("SYNTHESIZE DESIGN PACK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Dynamic generation trigger simulation
                            LaunchedEffect(isSynthesizing) {
                                if (isSynthesizing) {
                                    kotlinx.coroutines.delay(2000)
                                    val generated = ThemeManager.generateAITheme(generativePrompt)
                                    selectedPreviewTheme = generated
                                    isSynthesizing = false
                                    showSynthesisSuccess = true
                                    generativePrompt = ""
                                }
                            }

                            AnimatedVisibility(visible = showSynthesisSuccess) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Success", tint = currentTheme.successColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI Design Synthesis succeeded! Dynamic swatch uploaded below.", color = currentTheme.successColor, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // ORIGINAL NUSU AUDIO SYNTHESIS ENGINE
                item {
                    Text(
                        text = "ORIGINAL SYNTHETIC SOUND SYSTEM",
                        color = currentTheme.accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nusuEntranceTransition(2)
                            .border(1.dp, currentTheme.accentColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = currentTheme.cardBackgroundColor.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Choose a dynamically synthesized physical sound pack. Sounds are procedurally synthesized in real-time as beautiful high-tech wave signals.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val soundPacks = listOf(
                                com.example.services.SoundPack.SILENT to "Silent / Haptic Only",
                                com.example.services.SoundPack.SOFT_PULSE to "Soft Pulse Wave",
                                com.example.services.SoundPack.DIGITAL_RESONANCE to "Digital Sweep Wave",
                                com.example.services.SoundPack.HOLOGRAPHIC_CHORD to "Sci-Fi Chord Wave"
                            )

                            var activePack by remember { mutableStateOf(com.example.services.NusuSoundManager.currentSoundPack) }
                            var activeHaptics by remember { mutableStateOf(com.example.services.NusuSoundManager.isHapticEnabled) }
                            val context = androidx.compose.ui.platform.LocalContext.current

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                soundPacks.forEach { (pack, name) ->
                                    val isSelected = activePack == pack
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) currentTheme.accentColor.copy(alpha = 0.15f)
                                                else currentTheme.backgroundColor.copy(alpha = 0.3f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) currentTheme.accentColor else Color.Gray.copy(alpha = 0.1f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                com.example.services.NusuSoundManager.currentSoundPack = pack
                                                activePack = pack
                                                com.example.services.NusuSoundManager.playCue(context, "start")
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = name,
                                            color = if (isSelected) currentTheme.accentColor else NusuPureWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(currentTheme.accentColor)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(currentTheme.backgroundColor.copy(alpha = 0.3f))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Tactile Feedback (Haptics)", color = NusuPureWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Subtle mechanical pulses on activation", color = Color.Gray, fontSize = 9.sp)
                                }
                                Switch(
                                    checked = activeHaptics,
                                    onCheckedChange = {
                                        com.example.services.NusuSoundManager.isHapticEnabled = it
                                        activeHaptics = it
                                        if (it) {
                                            com.example.services.NusuSoundManager.playCue(context, "end")
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = currentTheme.accentColor,
                                        checkedTrackColor = currentTheme.accentColor.copy(alpha = 0.4f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Collection Filter Slider & Premium Hub Store
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nusuEntranceTransition(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PREMIUM UTILITY OS THEMES (100+)",
                            color = currentTheme.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        // Collection chips selection
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(collections) { col ->
                                val isSelected = selectedCollectionFilter == col
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) currentTheme.primaryColor.copy(alpha = 0.2f)
                                            else currentTheme.cardBackgroundColor.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) currentTheme.primaryColor else Color.Gray.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedCollectionFilter = col }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = col,
                                        color = if (isSelected) currentTheme.primaryColor else NusuPureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Search system bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search 100+ Premium Themes...", fontSize = 12.sp, color = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f),
                                focusedBorderColor = currentTheme.primaryColor
                            )
                        )
                    }
                }

                // Premium Hub Cards List
                itemsIndexed(filteredThemes.take(30)) { idx, theme ->
                    val isSelectedPreview = selectedPreviewTheme.id == theme.id
                    val isCurrentlyApplied = currentTheme.id == theme.id
                    val isFavObj = favorites.contains(theme.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nusuEntranceTransition(3 + minOf(idx, 6))
                            .border(
                                if (isSelectedPreview) 2.dp else 1.dp,
                                if (isSelectedPreview) theme.primaryColor else Color.Gray.copy(alpha = 0.1f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedPreviewTheme = theme
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = theme.cardBackgroundColor.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Dynamic swatch indicator bubble
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(theme.backgroundColor)
                                        .border(1.dp, theme.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.sweepGradient(
                                                    colors = listOf(theme.primaryColor, theme.secondaryColor, theme.primaryColor)
                                                )
                                            )
                                    )
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = theme.name,
                                            color = NusuPureWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isCurrentlyApplied) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(theme.successColor.copy(alpha = 0.2f))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("ACTIVE", color = theme.successColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${theme.collectionName} • ${theme.edition}",
                                        color = Color.Gray,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { ThemeManager.toggleFavorite(theme.id) }
                            ) {
                                Icon(
                                    imageVector = if (isFavObj) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavObj) Color.Red else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp)) // Floating platform escape gap
                }
            }

            // Glowing Floating Activation Bar (Apply actions)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, currentTheme.backgroundColor)
                        )
                    )
                    .padding(24.dp)
            ) {
                Button(
                    onClick = {
                        ThemeManager.applyTheme(selectedPreviewTheme)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                listOf(selectedPreviewTheme.primaryColor, selectedPreviewTheme.secondaryColor)
                            ),
                            RoundedCornerShape(28.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedPreviewTheme.cardBackgroundColor.copy(alpha = 0.9f),
                        contentColor = selectedPreviewTheme.primaryColor
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Apply Dynamic Theme",
                        modifier = Modifier.size(20.dp),
                        tint = selectedPreviewTheme.primaryColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "APPLY SELECTED DESIGN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = NusuPureWhite
                    )
                }
            }
        }
    }
}

// Special dynamic-color AICoreOrb specifically for previewing design systems on various combinations
@Composable
fun KeyedAICoreOrb(
    primaryColor: Color,
    secondaryColor: Color,
    innerColor: Color,
    outerColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbSpinPreview")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ), label = "SpinRot"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "Pulse"
    )

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        val radius = (size.width / 2) * 0.5f * pulse

        rotate(rotation) {
            drawCircle(
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()),
                brush = Brush.sweepGradient(
                    colors = listOf(primaryColor, Color.Transparent, secondaryColor, Color.Transparent)
                ),
                radius = radius * 1.5f,
                center = center
            )
            drawCircle(
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, secondaryColor, Color.Transparent, primaryColor)
                ),
                radius = radius * 1.2f,
                center = center
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(innerColor, outerColor, Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center,
            blendMode = androidx.compose.ui.graphics.BlendMode.Screen
        )
    }
}
