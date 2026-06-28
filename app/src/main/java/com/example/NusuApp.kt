package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.MoodScreen
import com.example.ui.screens.WeatherScreen
import com.example.ui.screens.VoiceScreen
import com.example.ui.screens.StartupScreen
import com.example.ui.screens.AestheticHubScreen
import com.example.ui.screens.CallGuardianScreen
import com.example.ui.screens.FirewallScreen
import com.example.ui.theme.*
import com.example.services.NusuVoiceService
import com.example.ui.components.AnimatedAICoreOrb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import com.example.ui.components.SystemRecalibratingOverlay
import com.example.ui.components.TouchCoordinatesOverlay
import com.example.ui.components.DiagnosticsDrawer
import com.example.api.SystemErrorState

@Composable
fun NusuApp(onStartVoiceService: () -> Unit = {}) {
    val navController = rememberNavController()
    val activeTheme = LocalNusuTheme.current
    val isSystemRecalibrating by SystemErrorState.isSystemRecalibrating.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = activeTheme.backgroundColor,
            bottomBar = {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                if (currentRoute != "voice" && currentRoute != "startup" && currentRoute != "aesthetic_hub") {
                    NavigationBar(
                        containerColor = activeTheme.cardBackgroundColor,
                        contentColor = activeTheme.secondaryColor
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Core") },
                            selected = currentRoute == "home",
                            onClick = { navController.navigate("home") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeTheme.backgroundColor,
                                selectedTextColor = activeTheme.primaryColor,
                                indicatorColor = activeTheme.primaryColor,
                                unselectedIconColor = NusuPureWhite.copy(alpha = 0.5f),
                                unselectedTextColor = NusuPureWhite.copy(alpha = 0.5f)
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                            label = { Text("Neural") },
                            selected = currentRoute == "chat",
                            onClick = { navController.navigate("chat") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeTheme.backgroundColor,
                                selectedTextColor = activeTheme.accentColor,
                                indicatorColor = activeTheme.accentColor,
                                unselectedIconColor = NusuPureWhite.copy(alpha = 0.5f),
                                unselectedTextColor = NusuPureWhite.copy(alpha = 0.5f)
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Mic, contentDescription = "Voice") },
                            label = { Text("Voice") },
                            selected = currentRoute == "voice",
                            onClick = { navController.navigate("voice") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeTheme.backgroundColor,
                                selectedTextColor = activeTheme.secondaryColor,
                                indicatorColor = activeTheme.secondaryColor,
                                unselectedIconColor = NusuPureWhite.copy(alpha = 0.5f),
                                unselectedTextColor = NusuPureWhite.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "startup",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("startup") {
                    StartupScreen(
                        onNavigateHome = {
                            navController.navigate("home") {
                                popUpTo("startup") { inclusive = true }
                            }
                        }
                    )
                }
                composable("home") {
                    LaunchedEffect(Unit) {
                        onStartVoiceService()
                    }
                    HomeScreen(
                        onNavigateToChat = { navController.navigate("chat") },
                        onNavigateToVoice = { navController.navigate("voice") },
                        onNavigateToAestheticHub = { navController.navigate("aesthetic_hub") },
                        onNavigateToWeather = { navController.navigate("weather") },
                        onNavigateToMemory = { navController.navigate("memory") },
                        onNavigateToMood = { navController.navigate("mood") },
                        onNavigateToCallGuardian = { navController.navigate("call_guardian") },
                        onNavigateToFirewall = { navController.navigate("firewall") }
                    )
                }
                composable("aesthetic_hub") {
                    AestheticHubScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("chat") {
                    ChatScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("voice") {
                    VoiceScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("memory") {
                    MemoryScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("weather") {
                    WeatherScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("mood") {
                    MoodScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("call_guardian") {
                    CallGuardianScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("firewall") {
                    FirewallScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
        }

        // Active omnipresent Floating Assistant Orb
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        if (currentRoute != "startup" && currentRoute != "voice") {
            NusuFloatingAssistant(navController = navController)
        }

        if (isSystemRecalibrating) {
            SystemRecalibratingOverlay(onDismiss = { SystemErrorState.dismissRecalibration() })
        }

        DiagnosticsDrawer()
    }
}

@Composable
fun NusuFloatingAssistant(navController: androidx.navigation.NavController) {
    val serviceState by NusuVoiceService.serviceState.collectAsState()
    val activeTheme = LocalNusuTheme.current
    val coroutineScope = rememberCoroutineScope()

    // Position Offsets
    var offsetX by remember { mutableStateOf(290f) }
    var offsetY by remember { mutableStateOf(480f) }

    var isExpanded by remember { mutableStateOf(false) }
    var isMinimized by remember { mutableStateOf(false) }

    // Glow properties driven by state
    val orbGlowColor = when (serviceState) {
        NusuVoiceService.NusuState.LISTENING -> activeTheme.successColor
        NusuVoiceService.NusuState.THINKING -> activeTheme.accentColor
        NusuVoiceService.NusuState.SPEAKING -> activeTheme.primaryColor
        NusuVoiceService.NusuState.NOTIFICATION -> Color(0xFFFF9800) // Orange Warning Glow
        else -> activeTheme.secondaryColor
    }

    val dynamicGlowWidth = when (serviceState) {
        NusuVoiceService.NusuState.LISTENING -> 6.dp
        NusuVoiceService.NusuState.THINKING -> 8.dp
        NusuVoiceService.NusuState.SPEAKING -> 10.dp
        NusuVoiceService.NusuState.NOTIFICATION -> 12.dp
        else -> 2.dp
    }

    // Gentle Breathing pulse for IDLE
    val infiniteTransition = rememberInfiniteTransition(label = "OrbBreathe")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "breathe"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        isExpanded = false
                    }
            )
        }
        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .graphicsLayer(
                    scaleX = if (isMinimized) 0.85f else breathingScale,
                    scaleY = if (isMinimized) 0.85f else breathingScale
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // GLASSMORPHIC QUICK ACTION short-cuts Menu
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandHorizontally(expandFrom = Alignment.End),
                exit = shrinkHorizontally(shrinkTowards = Alignment.End)
            ) {
                Row(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    activeTheme.cardBackgroundColor.copy(alpha = 0.95f),
                                    activeTheme.backgroundColor.copy(alpha = 0.9f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingMenuButton(Icons.Default.Mic, "Voice", activeTheme.secondaryColor) {
                        isExpanded = false
                        navController.navigate("voice")
                    }
                    FloatingMenuButton(Icons.AutoMirrored.Filled.Chat, "Chat", activeTheme.accentColor) {
                        isExpanded = false
                        navController.navigate("chat")
                    }
                    FloatingMenuButton(Icons.Default.Memory, "Memory", activeTheme.successColor) {
                        isExpanded = false
                        navController.navigate("memory")
                    }
                    FloatingMenuButton(Icons.Default.WbSunny, "Weather", NusuYellow) {
                        isExpanded = false
                        navController.navigate("weather")
                    }
                    FloatingMenuButton(Icons.Default.ColorLens, "Themes", activeTheme.primaryColor) {
                        isExpanded = false
                        navController.navigate("aesthetic_hub")
                    }
                }
            }

            // Interactive Circle AI Orb Core
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .shadow(12.dp, CircleShape, spotColor = orbGlowColor)
                    .background(activeTheme.backgroundColor)
                    .border(dynamicGlowWidth, orbGlowColor.copy(alpha = 0.8f), CircleShape)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            var lastClickTime = 0L
                            while (true) {
                                val down = awaitFirstDown()
                                var dragAmount = Offset.Zero
                                var isDrag = false
                                var pointer = down
                                
                                do {
                                    val event = awaitPointerEvent()
                                    val dragEvent = event.changes.firstOrNull { it.id == pointer.id }
                                    if (dragEvent != null && dragEvent.pressed) {
                                        val positionChange = dragEvent.positionChange()
                                        if (positionChange != Offset.Zero) {
                                            dragAmount += positionChange
                                            if (dragAmount.getDistance() > 15f) {
                                                isDrag = true
                                                isMinimized = false
                                            }
                                            if (isDrag) {
                                                dragEvent.consume()
                                                offsetX = (offsetX + positionChange.x).coerceIn(-30f, 320f)
                                                offsetY = (offsetY + positionChange.y).coerceIn(40f, 650f)
                                            }
                                        }
                                        pointer = dragEvent
                                    }
                                } while (event.changes.any { it.pressed })
                                
                                if (!isDrag) {
                                    val clickTime = System.currentTimeMillis()
                                    if (clickTime - lastClickTime < 300L) {
                                        isExpanded = false
                                        navController.navigate("voice")
                                    } else {
                                        isExpanded = !isExpanded
                                        if (isMinimized) {
                                            if (offsetX > 200f) offsetX = 280f else offsetX = 40f
                                            isMinimized = false
                                        }
                                    }
                                    lastClickTime = clickTime
                                } else {
                                    if (offsetX > 250f) {
                                        offsetX = 295f
                                        isMinimized = true
                                    } else if (offsetX < 50f) {
                                        offsetX = -25f
                                        isMinimized = true
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedAICoreOrb(modifier = Modifier.fillMaxSize(0.9f))

                // Custom Notification mode Badge style
                if (serviceState == NusuVoiceService.NusuState.LISTENING) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(activeTheme.successColor)
                    )
                } else if (serviceState == NusuVoiceService.NusuState.NOTIFICATION) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9800))
                    )
                }
            }
        }
        
        // Developer Touch Overlay Feature
        TouchCoordinatesOverlay()
    }
}

@Composable
fun FloatingMenuButton(
    icon: ImageVector,
    desc: String,
    tintColor: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .background(tintColor.copy(alpha = 0.15f), CircleShape)
            .border(1.dp, tintColor.copy(alpha = 0.4f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = tintColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
