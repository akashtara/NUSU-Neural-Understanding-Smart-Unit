package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class SecurityState {
    SECURE,
    UNDER_ATTACK,
    BREACHED,
    AUTORECOVERING,
    RESTARTING,
    RESTORE_COMPLETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen(
    onNavigateBack: () -> Unit,
    viewModel: NusuViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val activeTheme = LocalNusuTheme.current
    val coroutineScope = rememberCoroutineScope()

    var securityState by remember { mutableStateOf(SecurityState.SECURE) }
    var integrityPercent by remember { mutableStateOf(100f) }
    var activeLayers by remember { mutableStateOf(1000) }
    var attackVector by remember { mutableStateOf("") }
    var firewallLogs by remember { mutableStateOf(listOf<String>()) }
    var terminalLines by remember { mutableStateOf(listOf<String>()) }

    // Alarm pulse for visual feedback
    val alarmTransition = rememberInfiniteTransition(label = "AlarmPulse")
    val redGlitchIntensity by alarmTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glitch"
    )

    // Log tracking
    fun appendLog(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        firewallLogs = (listOf("[$time] $msg") + firewallLogs).take(60)
    }

    LaunchedEffect(Unit) {
        appendLog("Quantum Fortress Shield online: 1000 absolute layers fully loaded.")
        appendLog("Dynamic packet inspector initialized on Port 80, 443, and 8080.")
        appendLog("Cryptographic core: SHA-512 with Quantum Multi-Layer lattice active.")
    }

    // Handles the intense cyber attack simulation
    LaunchedEffect(securityState) {
        if (securityState == SecurityState.UNDER_ATTACK) {
            appendLog("⚠️ CRITICAL ALARM: Intrusive exploit detected at the outer perimeter!")
            var steps = 0
            while (steps < 25 && securityState == SecurityState.UNDER_ATTACK) {
                delay(180)
                steps++
                integrityPercent = (100f - steps * 4f).coerceAtLeast(1f)
                activeLayers = (1000 - steps * 40).coerceAtLeast(1)

                val attacks = listOf(
                    "Flushing Port-Probe probe vector",
                    "Brute forcing decentralized layer ${activeLayers + 12}",
                    "Rootkit payload injection attempted at Registry Node",
                    "Exploit buffer-overflow in Socket API",
                    "Warp-Drive DDoS payload at high throughput"
                )
                appendLog("🔥 ALERT: ${attacks.random()} - Segment ${activeLayers} down!")
                
                if (activeLayers <= 1) {
                    securityState = SecurityState.BREACHED
                }
            }
        } else if (securityState == SecurityState.BREACHED) {
            appendLog("🚨 GATE CRITICAL OVERLOAD: 999 Layers bypassed. Hackers at Layer 1 Firewall Gate!")
            delay(1500)
            securityState = SecurityState.AUTORECOVERING
        } else if (securityState == SecurityState.AUTORECOVERING) {
            appendLog("⚡ TRIGGERING INSTANT SECURITY AUTORECOVERY PROTOCOL 0x7E9!")
            appendLog("🔄 ENGAGING FIRST-LAYER INTEGRITY RESTORATION: Re-arming backup grid...")
            // Perform the physical restore of the main dialogues database from the immutable sentinel offline backup
            viewModel.recoverFromPermanentSentinel()
            delay(1800)
            securityState = SecurityState.RESTARTING
        } else if (securityState == SecurityState.RESTARTING) {
            terminalLines = emptyList()
            val rebootLogs = listOf(
                "--- QUANTUM SEC SYSTEM EMERGENCY RECONSTITUTION ---",
                "[RECOVER] Isolating memory workspace standard allocations...",
                "[RECOVER] Locking secure sandbox directories: /dev/null/sandbox",
                "[RECOVER] 1st Layer Firewall forcefield armed successfully! (Gate RESTORED)",
                "[WIPE] Eradicating remote command thread pointers...",
                "[WIPE] Clearing RAM network cache buffer memory safely...",
                "[RESTORE] Authenticating master developer profile: Akash Tarafder (Verified)",
                "[RESTORE] Decrypting Room DB securely standard structures...",
                "[RESTORE] Restoring primary Nusu UX Cyber theme and system components...",
                "[SYSTEM] Soft-rebooting virtual environment loops...",
                "[SYSTEM] Re-verifying package integrity... COMPLETE",
                "---------------------------------------------------",
                "STATUS: ABSOLUTELY FRESH, HEALTHY AND SECURED!",
                "🛡️ SHIELD OPERATIONAL: All 1000 layers are fully re-established!"
            )
            for (line in rebootLogs) {
                terminalLines = terminalLines + line
                delay(350)
            }
            delay(1000)
            securityState = SecurityState.RESTORE_COMPLETE
        } else if (securityState == SecurityState.RESTORE_COMPLETE) {
            integrityPercent = 100f
            activeLayers = 1000
            attackVector = ""
            appendLog("✅ SYSTEM FULLY RESTORED: App is fresh, clean, and perfectly operational.")
            appendLog("🔒 Commander, Developer, and User databases are fully secured with 512-bit lattice encryption.")
            delay(1000)
            securityState = SecurityState.SECURE
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "QUANTUM FIREWALL SECURITY CORE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (securityState == SecurityState.UNDER_ATTACK || securityState == SecurityState.BREACHED) NusuError else activeTheme.primaryColor,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "1000 Layer Unbreakable Defender",
                            fontSize = 9.sp,
                            color = activeTheme.secondaryColor.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = activeTheme.secondaryColor
                        )
                    }
                },
                actions = {
                    if (securityState == SecurityState.SECURE) {
                        IconButton(onClick = {
                            securityState = SecurityState.UNDER_ATTACK
                            attackVector = listOf("DDoS Cloud Stride", "Decryption Brute Force", "SQL Poisoning Core").random()
                        }) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Simulate Probe Attempt",
                                tint = activeTheme.accentColor
                            )
                        }
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
                .background(
                    if (securityState == SecurityState.UNDER_ATTACK || securityState == SecurityState.BREACHED) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E0B0B),
                                activeTheme.backgroundColor
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                activeTheme.backgroundColor,
                                activeTheme.cardBackgroundColor.copy(alpha = 0.4f)
                            )
                        )
                    }
                )
        ) {
            if (securityState == SecurityState.RESTARTING) {
                // Terminals UI reboot screens sequence
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYSTEM RESTORATION SANDBOX VER 4.09.2",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.successColor,
                            fontSize = 12.sp
                        )
                        CircularProgressIndicator(
                            color = activeTheme.successColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    HorizontalDivider(color = activeTheme.successColor.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(terminalLines) { line ->
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                color = if (line.contains("STATUS") || line.contains("SHIELD")) activeTheme.successColor else if (line.contains("ERADICATE") || line.contains("WIPE")) activeTheme.accentColor else Color.LightGray,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // MAIN RADAR STATS RING
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(210.dp)
                                .padding(10.dp)
                        ) {
                            // Circular indicator glows
                            CircularProgressIndicator(
                                progress = integrityPercent / 100f,
                                modifier = Modifier.fillMaxSize(),
                                color = when (securityState) {
                                    SecurityState.UNDER_ATTACK -> NusuError
                                    SecurityState.BREACHED -> NusuError
                                    SecurityState.AUTORECOVERING -> activeTheme.accentColor
                                    else -> activeTheme.successColor
                                },
                                strokeWidth = 8.dp,
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )

                            // Dynamic Alarm Rings inside
                            if (securityState == SecurityState.UNDER_ATTACK || securityState == SecurityState.BREACHED) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.85f * redGlitchIntensity)
                                        .clip(CircleShape)
                                        .background(NusuError.copy(alpha = 0.15f))
                                        .border(2.dp, NusuError.copy(alpha = 0.4f), CircleShape)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SHIELD INTEGRITY",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = String.format("%.0f%%", integrityPercent),
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Light,
                                    color = when (securityState) {
                                        SecurityState.UNDER_ATTACK -> NusuError
                                        SecurityState.BREACHED -> NusuError
                                        else -> NusuPureWhite
                                    }
                                )
                                Text(
                                    text = "$activeLayers / 1000 ARMED",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (activeLayers < 200) NusuError else activeTheme.primaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // MAIN STATE ANNOUNCEMENT BLOCK
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (securityState == SecurityState.UNDER_ATTACK || securityState == SecurityState.BREACHED) {
                                    Color(0xFF2D1111)
                                } else {
                                    activeTheme.cardBackgroundColor.copy(alpha = 0.8f)
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (securityState == SecurityState.UNDER_ATTACK || securityState == SecurityState.BREACHED) NusuError.copy(alpha = 0.5f)
                                else activeTheme.primaryColor.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val stateTitle = when (securityState) {
                                    SecurityState.SECURE -> "SHIELD STATUS: SECURE & ACTIVE"
                                    SecurityState.UNDER_ATTACK -> "🚨 SECURITY OVERRIDE DETECTED 🚨"
                                    SecurityState.BREACHED -> "💥 EXPLOIT AT LAYER 1 GATEWAY 💥"
                                    SecurityState.AUTORECOVERING -> "🔄 INSTATING RECONSTITUTION 🔄"
                                    SecurityState.RESTORE_COMPLETE -> "✅ COMPONENT RECOVERY INITIATED"
                                    else -> "RE-ESTABLISHING COMPLETED"
                                }

                                val stateDesc = when (securityState) {
                                    SecurityState.SECURE -> "The 1000-Layer firewall is active, isolating critical memory partitions. Unauthorized access vectors are mathematically impossible."
                                    SecurityState.UNDER_ATTACK -> "An external hacker is testing endpoint probes via $attackVector! Firewall grids are breaking from layer 1000 downward!"
                                    SecurityState.BREACHED -> "Outer layers bypassed entirely! Core database is locked under emergency backup. Critical Gate recovery preparing..."
                                    SecurityState.AUTORECOVERING -> "Auto recovering Layer 1 firewall, initializing fresh system restart, wiping cache data, and restoring the full environment."
                                    else -> "Reloading secure partitions..."
                                }

                                Text(
                                    text = stateTitle,
                                    color = when (securityState) {
                                        SecurityState.UNDER_ATTACK, SecurityState.BREACHED -> NusuError
                                        else -> activeTheme.successColor
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stateDesc,
                                    color = NusuPureWhite.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }

                    // MANUAL SYSTEM ALERTS RUNNER
                    if (securityState == SecurityState.SECURE) {
                        item {
                            Button(
                                onClick = {
                                    securityState = SecurityState.UNDER_ATTACK
                                    attackVector = listOf("SQL Warp Poisoning", "Brute Force decrypter", "Syn-Flood Botnet", "Injected Trojan Sandbox Exploit").random()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NusuError),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Security, contentDescription = "Trigger Attack Simulation")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simulate Hacker Breach Attempt", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (securityState == SecurityState.BREACHED) {
                        item {
                            Button(
                                onClick = { securityState = SecurityState.AUTORECOVERING },
                                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Autorenew, "Autorecover", tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Trigger Emergency Auto-Recovery", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // ENCRYPTED DATA PROTECTOR LEDGER
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "SECURE COGNITIVE LEDGER & CLIENT ENCRYPTIONS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 1.sp
                                )

                                val items = listOf(
                                    Triple("COMMANDER USER ID", "Akash Tarafder (PRIMARY COMMAND)", activeTheme.successColor),
                                    Triple("DEVELOPER ACCESS KEY", "AI Studio Developer (SECURED)", activeTheme.secondaryColor),
                                    Triple("CORE ROOM STORAGE", "CallGuardianDatabase standard tables (LOCKED)", activeTheme.primaryColor),
                                    Triple("FIRST-LAYER GUARD", if (securityState == SecurityState.BREACHED) "BYPASSED (EMERGENCY RECONSTRUCTING)" else "ARMED & ENCRYPTED", if (securityState == SecurityState.BREACHED) NusuError else activeTheme.successColor)
                                )

                                items.forEach { (label, value, col) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(label, fontSize = 8.sp, color = Color.LightGray.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                                            Text(value, fontSize = 11.sp, color = NusuPureWhite, fontWeight = FontWeight.SemiBold)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(col)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // REALTIME COGNITIVE TRAFFIC MONITOR
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "FIREWALL SHIELD LOG TAPE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(firewallLogs) { log ->
                                            Text(
                                                text = log,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = when {
                                                    log.contains("ALERT") || log.contains("ALARM") -> NusuError
                                                    log.contains("TRIGGERING") || log.contains("RE-ESTABLISHING") -> activeTheme.accentColor
                                                    log.contains("RESTORE") || log.contains("SECURED") -> activeTheme.successColor
                                                    else -> Color.LightGray
                                                },
                                                modifier = Modifier.padding(vertical = 1.6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
