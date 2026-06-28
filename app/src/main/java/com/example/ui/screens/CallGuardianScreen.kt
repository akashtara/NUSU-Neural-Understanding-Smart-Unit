package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.BlockedContact
import com.example.database.CallHistoryRecord
import com.example.database.VIPContact
import com.example.ui.components.nusuPulsingNeonGlow
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallGuardianScreen(
    onNavigateBack: () -> Unit,
    viewModel: CallGuardianViewModel = viewModel()
) {
    val activeTheme = LocalNusuTheme.current

    // Observe state from ViewModel
    val activeCall by viewModel.activeCall.collectAsState()
    val allCallHistory by viewModel.allCallHistory.collectAsState()
    val allBlockedContacts by viewModel.allBlockedContacts.collectAsState()
    val allVIPContacts by viewModel.allVIPContacts.collectAsState()
    val systemLogs by viewModel.systemLogs.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("Screening", "VIP Setup", "Blacklist", "Memory History")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "NUSU™ AI CALL GUARDIAN",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.primaryColor,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Active Protection Shield v2.4",
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
                    IconButton(onClick = { viewModel.finishActiveCall() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Simulation",
                            tint = activeTheme.primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeTheme.cardBackgroundColor)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nusuPulsingNeonGlow()
        ) {
            // Horizontal Tab Bar with Custom Glassmorphic Style (clean default Material 3 indicator)
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = activeTheme.cardBackgroundColor.copy(alpha = 1.0f),
                contentColor = activeTheme.primaryColor,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == index) activeTheme.primaryColor else NusuPureWhite.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> ScreeningTab(viewModel, activeCall, systemLogs)
                    1 -> VIPSetupTab(viewModel, allVIPContacts)
                    2 -> BlacklistTab(viewModel, allBlockedContacts)
                    3 -> MemoryHistoryTab(viewModel, allCallHistory)
                }
            }
        }
    }
}

// ==========================================================================================
// TABS 0: AI CALL SCREENING INTERCEPT ENGINE
// ==========================================================================================

@Composable
fun ScreeningTab(
    viewModel: CallGuardianViewModel,
    activeCall: ActiveCallState,
    systemLogs: List<String>
) {
    val activeTheme = LocalNusuTheme.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Alert Card showing that system is ONLINE
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(activeTheme.cardBackgroundColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .border(1.dp, activeTheme.successColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(activeTheme.successColor)
                    )
                    Text(
                        text = "NUSU SHIELD ACTIVE: Intercepting all numbers automatically.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = activeTheme.successColor
                    )
                }
            }
        }

        // Active Call Simulator Panel (IF ACTIVE) or simulator choices launcher
        if (activeCall.status != CallStatus.IDLE) {
            item {
                ActiveCallHUD(viewModel, activeCall)
            }
        } else {
            // Simulator Choice Panels
            item {
                Text(
                    text = "SELECT AN INCOMING CALL PRESET TO SIMULATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.primaryColor,
                    letterSpacing = 1.sp
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SimulatorPresetCard(
                        title = "Mom ❤️ (Authorized VIP Category)",
                        subtitle = "+91 98300 12345 • Hinglish VIP Contact",
                        category = "VIP",
                        color = activeTheme.accentColor,
                        onClick = {
                            viewModel.triggerIncomingCall(
                                callerName = "Mom ❤️",
                                phoneNumber = "+91 98300 12345",
                                companyName = "Home Core",
                                initReason = "Dinner and health routine inquiry",
                                presetId = "mom",
                                voiceType = "Hinglish 🇮🇳"
                            )
                        }
                    )

                    SimulatorPresetCard(
                        title = "Vikram Malhotra (Hindi Credit Card Call)",
                        subtitle = "+91 84439 12242 • HDFC premium limit upgrade",
                        category = "HINDI 🇮🇳",
                        color = activeTheme.primaryColor,
                        onClick = {
                            viewModel.triggerIncomingCall(
                                callerName = "Vikram Malhotra",
                                phoneNumber = "+91 84439 12242",
                                companyName = "HDFC Bank Ltd",
                                initReason = "Premium credit card limit upgrade details and onboarding",
                                presetId = "hindi",
                                voiceType = "Hindi 🇮🇳"
                            )
                        }
                    )

                    SimulatorPresetCard(
                        title = "Dr. Aris Wells (English Academic Call)",
                        subtitle = "+44 2079 460192 • Oxford Genomic research",
                        category = "ENGLISH 🇬🇧",
                        color = activeTheme.accentColor,
                        onClick = {
                            viewModel.triggerIncomingCall(
                                callerName = "Dr. Aris Wells",
                                phoneNumber = "+44 2079 460192",
                                companyName = "University of Oxford",
                                initReason = "Urgent paper publication genomic dataset approval verification",
                                presetId = "english",
                                voiceType = "English 🇬🇧"
                            )
                        }
                    )

                    SimulatorPresetCard(
                        title = "Subhojit Sen (Bengali Land Deed Registry)",
                        subtitle = "+91 98311 02441 • Special court registry paperwork",
                        category = "BENGALI 🇧🇩",
                        color = activeTheme.successColor,
                        onClick = {
                            viewModel.triggerIncomingCall(
                                callerName = "Subhojit Sen",
                                phoneNumber = "+91 98311 02441",
                                companyName = "Kolkata Legal Associates",
                                initReason = "Urgent call regarding property registry deed file 402 signature requirements",
                                presetId = "bengali",
                                voiceType = "Bengali 🇧🇩"
                            )
                        }
                    )

                    SimulatorPresetCard(
                        title = "Zomato Delivery Executive",
                        subtitle = "+91 99342 11023 • Gate access blocked",
                        category = "SERVICE",
                        color = NusuYellow,
                        onClick = {
                            viewModel.triggerIncomingCall(
                                callerName = "Zomato Delivery Executive",
                                phoneNumber = "+91 99342 11023",
                                companyName = "Zomato Food Express",
                                initReason = "Zomato partner at gate unable to enter flat",
                                presetId = "zomato",
                                voiceType = "Hinglish 🇮🇳"
                            )
                        }
                    )

                    SimulatorPresetCard(
                        title = "Apex Hospital Emergency Line (🚨 Keywords)",
                        subtitle = "+91 91122 33445 • Critical hospitalization keywords",
                        category = "EMERGENCY",
                        color = NusuError,
                        onClick = {
                            viewModel.triggerIncomingCall(
                                callerName = "Apex Hospital Emergency Line",
                                phoneNumber = "+91 91122 33445",
                                companyName = "Apex Care Clinic",
                                initReason = "Emergency call regarding accident and admission confirmation",
                                presetId = "emergency",
                                voiceType = "Hinglish 🇮🇳"
                            )
                        }
                    )

                    SimulatorPresetCard(
                        title = "Mega Financials Campaign Spammer (⚠️ Risk)",
                        subtitle = "+91 140 334125 • Instant auto-shield defense test",
                        category = "SPAM INDICATOR",
                        color = NusuError,
                        onClick = {
                            viewModel.triggerIncomingCall(
                                callerName = "Loan Agency Robot",
                                phoneNumber = "+91 140 334125",
                                companyName = "Mega Web Financials",
                                initReason = "Robocall pre-approved card offering telemarketing campaign",
                                presetId = "spam",
                                voiceType = "English 🇬🇧"
                            )
                        }
                    )
                }
            }

            // Custom Sim triggers
            item {
                CustomMockTriggerCard(viewModel)
            }
        }

        // Live system events logs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "LIVE SHIELD SECURITY ACTIVITY LOG",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.height(100.dp)) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(systemLogs) { log ->
                                Text(
                                    text = log,
                                    fontSize = 9.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = if (log.contains("AUTO DEFENSE")) NusuError else activeTheme.successColor.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================================================
// ACTIVE SCREEN: REAL TIME LIVE HUD & GRAPHIC TRANSCRIPTS
// ==========================================================================================

@Composable
fun ActiveCallHUD(
    viewModel: CallGuardianViewModel,
    call: ActiveCallState
) {
    val activeTheme = LocalNusuTheme.current

    // Pulsing circle animation representing incoming or screening live pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRing")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseSize"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        if (call.spamRisk > 70) NusuError else activeTheme.primaryColor,
                        activeTheme.accentColor
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = activeTheme.cardBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BADGES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (call.urgencyStatus == "Emergency") NusuError.copy(alpha = 0.15f) else activeTheme.primaryColor.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = call.urgencyStatus.uppercase(),
                        fontSize = 9.sp,
                        color = if (call.urgencyStatus == "Emergency") NusuError else activeTheme.primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            activeTheme.accentColor.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = call.voiceType.uppercase(),
                        fontSize = 9.sp,
                        color = activeTheme.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .background(
                            when (call.status) {
                                CallStatus.SCREENING -> activeTheme.accentColor.copy(alpha = 0.15f)
                                CallStatus.MONITORED -> activeTheme.successColor.copy(alpha = 0.15f)
                                CallStatus.CONNECTED -> activeTheme.successColor.copy(alpha = 0.25f)
                                CallStatus.REJECTED -> NusuError.copy(alpha = 0.15f)
                                else -> NusuYellow.copy(alpha = 0.15f)
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = call.status.name,
                        fontSize = 9.sp,
                        color = when (call.status) {
                            CallStatus.SCREENING -> activeTheme.accentColor
                            CallStatus.MONITORED -> activeTheme.successColor
                            CallStatus.CONNECTED -> activeTheme.successColor
                            CallStatus.REJECTED -> NusuError
                            else -> NusuYellow
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CALLER PHOTO & INFO HUD
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .scale(if (call.status == CallStatus.SCREENING) pulseSize else 1f)
            ) {
                // Outer Ring glowing gradient
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (call.spamRisk > 70) NusuError else activeTheme.primaryColor,
                                    activeTheme.accentColor
                                )
                            )
                        )
                )

                // Inner content
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(activeTheme.backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (call.callerName.isNotEmpty()) call.callerName.take(2).uppercase() else "IN",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTheme.primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = call.callerName,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = NusuPureWhite
            )
            Text(
                text = "${call.phoneNumber} • ${call.companyName}",
                fontSize = 11.sp,
                color = Color.LightGray.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // STAT GAUGES: Emotion, Spam Risk, Trust Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPill(title = "SPAM RISK", value = "${call.spamRisk}%", color = if (call.spamRisk > 60) NusuError else activeTheme.successColor)
                StatPill(title = "TRUST SCORE", value = "${call.trustScore}/100", color = if (call.trustScore > 70) activeTheme.successColor else NusuError)
                StatPill(title = "EMOTION", value = call.emotionStatus.uppercase(), color = activeTheme.primaryColor)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI INTENT REASON
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "DETECTED CALL INTENT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = call.reasonForCalling,
                        fontSize = 12.sp,
                        color = NusuPureWhite.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI RECOMMENDATION ENGINE PANEL
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f)),
                border = BorderStroke(
                    width = 1.dp,
                    color = when (call.aiRecommendation) {
                        "Connect" -> activeTheme.successColor.copy(alpha = 0.4f)
                        "Reject" -> NusuError.copy(alpha = 0.4f)
                        "Schedule Callback" -> NusuYellow.copy(alpha = 0.4f)
                        else -> activeTheme.primaryColor.copy(alpha = 0.3f)
                    }
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left indicator dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when (call.aiRecommendation) {
                                    "Connect" -> activeTheme.successColor
                                    "Reject" -> NusuError
                                    "Schedule Callback" -> NusuYellow
                                    else -> activeTheme.primaryColor
                                },
                                shape = CircleShape
                            )
                    )

                    Column {
                        Text(
                            text = "AI RECOMMENDATION: ${call.aiRecommendation.uppercase()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (call.aiRecommendation) {
                                "Connect" -> activeTheme.successColor
                                "Reject" -> NusuError
                                "Schedule Callback" -> NusuYellow
                                else -> activeTheme.primaryColor
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = call.aiRecommendationReason,
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI RECEPTIONIST ACTIVE DIALOG TRANSCRIPT CONSOLE
            Text(
                text = "🔴 LIVE VOICE SCREENING TRANSCRIPT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NusuError,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = false
                ) {
                    items(call.transcript) { line ->
                        DialogueBubble(line)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // INSTRUCTION STATUS TAPE
            Text(
                text = "NUSU LOGIC TAPE: ${call.aiSummaryStatus}",
                fontSize = 11.sp,
                color = activeTheme.primaryColor,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // THE SIMULATOR RECEPTION CONTROLS
            when (call.status) {
                CallStatus.INCOMING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startScreening() },
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.accentColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Call, "Screen", tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Let NUSU Screen", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.rejectCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = NusuError),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.CallEnd, "Reject")
                            Spacer(Modifier.width(8.dp))
                            Text("Reject Call", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                CallStatus.SCREENING, CallStatus.MONITORED -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // PRIVATE AI COMMAND CHANNEL FOR AKASH
                        Text(
                            text = "💬 PRIVATE AI COMMAND CHANNEL (CALLER CANNOT HEAR)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.secondaryColor,
                            letterSpacing = 0.5.sp
                        )

                        // Command Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val commands = listOf(
                                "Ask urgency" to "Usse urgency poochho",
                                "Ask meeting time" to "Meeting ka time poochho",
                                "Record message" to "Message save karo"
                            )

                            commands.forEach { (label, commandText) ->
                                AssistChip(
                                    onClick = { viewModel.processPrivateCommand(commandText) },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = activeTheme.primaryColor,
                                        containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }

                        // Text Field custom commands
                        var customCommandText by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = customCommandText,
                            onValueChange = { customCommandText = it },
                            placeholder = { Text("Secret custom instruction for NUSU...", fontSize = 12.sp, color = Color.Gray) },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (customCommandText.isNotEmpty()) {
                                            viewModel.processPrivateCommand(customCommandText)
                                            customCommandText = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Send, "Send Code", tint = activeTheme.primaryColor)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = NusuPureWhite,
                                unfocusedTextColor = NusuPureWhite,
                                focusedBorderColor = activeTheme.primaryColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // CORE ACTION CONTROLS FOR AKASH
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { viewModel.connectCall() },
                                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.successColor),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, "Connect", tint = Color.Black)
                                Spacer(Modifier.width(4.dp))
                                Text("Allow / Connect", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.rejectCall() },
                                colors = ButtonDefaults.buttonColors(containerColor = NusuError),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Close, "Reject")
                                Spacer(Modifier.width(4.dp))
                                Text("Reject / Busy", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                CallStatus.CONNECTED, CallStatus.REJECTED, CallStatus.FINISHED -> {
                    Button(
                        onClick = { viewModel.finishActiveCall() },
                        colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Reset Simulation Controller", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun DialogueBubble(line: TranscriptLine) {
    val activeTheme = LocalNusuTheme.current

    val (bg, align, labelColor) = when (line.speaker) {
        "NUSU" -> Triple(activeTheme.primaryColor.copy(alpha = 0.15f), Alignment.Start, activeTheme.primaryColor)
        "Caller" -> Triple(activeTheme.cardBackgroundColor.copy(alpha = 0.8f), Alignment.Start, NusuPureWhite)
        "Akash (Private)" -> Triple(activeTheme.accentColor.copy(alpha = 0.2f), Alignment.End, activeTheme.accentColor)
        else -> Triple(Color.DarkGray.copy(alpha = 0.3f), Alignment.CenterHorizontally, Color.Gray)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .background(bg, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = line.speaker.uppercase(),
                    fontSize = 8.sp,
                    color = labelColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = line.text,
                    fontSize = 11.sp,
                    color = NusuPureWhite
                )
            }
        }
    }
}

@Composable
fun StatPill(title: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SimulatorPresetCard(
    title: String,
    subtitle: String,
    category: String,
    color: Color,
    onClick: () -> Unit
) {
    val activeTheme = LocalNusuTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(activeTheme.cardBackgroundColor.copy(alpha = 0.9f))
            .clickable { onClick() }
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NusuPureWhite)
                Text(subtitle, fontSize = 10.sp, color = Color.Gray)
            }

            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(category, fontSize = 8.sp, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomMockTriggerCard(viewModel: CallGuardianViewModel) {
    val activeTheme = LocalNusuTheme.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("INITIALIZE CUSTOM MOCK CHANNEL SCREEN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = activeTheme.primaryColor)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Caller Name", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NusuPureWhite,
                    unfocusedTextColor = NusuPureWhite,
                    focusedBorderColor = activeTheme.primaryColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NusuPureWhite,
                        unfocusedTextColor = NusuPureWhite,
                        focusedBorderColor = activeTheme.primaryColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NusuPureWhite,
                        unfocusedTextColor = NusuPureWhite,
                        focusedBorderColor = activeTheme.primaryColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Calling Intent", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NusuPureWhite,
                    unfocusedTextColor = NusuPureWhite,
                    focusedBorderColor = activeTheme.primaryColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                )
            )

            Button(
                onClick = {
                    if (name.isNotEmpty() && phone.isNotEmpty()) {
                        viewModel.triggerIncomingCall(
                            callerName = name,
                            phoneNumber = phone,
                            companyName = if (company.isEmpty()) "Private" else company,
                            initReason = if (reason.isEmpty()) "Interception Verification" else reason
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor)
            ) {
                Text("Launch Channel Intercept", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ==========================================================================================
// TABS 1: VIP REGISTER & CATEGORY SETUP
// ==========================================================================================

@Composable
fun VIPSetupTab(
    viewModel: CallGuardianViewModel,
    vipList: List<VIPContact>
) {
    val activeTheme = LocalNusuTheme.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Family") }
    val categories = listOf("Family", "Favorite", "Business", "Emergency")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = activeTheme.cardBackgroundColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "REGISTER NEW REAL-TIME VIP CONTACT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTheme.primaryColor,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("E.g., Mom ❤️, Akash Tarafder", fontSize = 12.sp, color = Color.Gray) },
                        label = { Text("Name", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NusuPureWhite,
                            unfocusedTextColor = NusuPureWhite,
                            focusedBorderColor = activeTheme.primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = { Text("E.g., +91 98300 12345", fontSize = 12.sp, color = Color.Gray) },
                        label = { Text("Phone Number", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NusuPureWhite,
                            unfocusedTextColor = NusuPureWhite,
                            focusedBorderColor = activeTheme.primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )

                    Text("SELECT VIP SUITE CATEGORY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedCategory == cat) activeTheme.primaryColor else activeTheme.backgroundColor
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selectedCategory == cat) activeTheme.primaryColor else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCategory == cat) Color.Black else NusuPureWhite
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (name.isNotEmpty() && phone.isNotEmpty()) {
                                viewModel.addVIPContact(name, phone, selectedCategory)
                                name = ""
                                phone = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor)
                    ) {
                        Text("Register Verified VIP", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "ACTIVE REGISTERED VIP SUITE (${vipList.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = activeTheme.primaryColor,
                letterSpacing = 1.sp
            )
        }

        if (vipList.isEmpty()) {
            item {
                Text(
                    text = "No VIP contacts designated or registered yet.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        } else {
            items(vipList) { vip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(activeTheme.cardBackgroundColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(vip.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NusuPureWhite)
                        Text(vip.phoneNumber, fontSize = 11.sp, color = Color.Gray)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(activeTheme.accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(vip.category.uppercase(), fontSize = 8.sp, color = activeTheme.accentColor, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = { viewModel.deleteVIPContact(vip.id, vip.name) }) {
                            Icon(Icons.Default.Delete, "Delete VIP Contact", tint = NusuError)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================================================
// TABS 2: BLACKLIST / SPAM PROTECTION LAYER
// ==========================================================================================

@Composable
fun BlacklistTab(
    viewModel: CallGuardianViewModel,
    blockedList: List<BlockedContact>
) {
    val activeTheme = LocalNusuTheme.current
    var blacklistPhone by remember { mutableStateOf("") }
    var blacklistReason by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shield Gauge Illustration
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .border(1.dp, NusuError.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Active",
                        modifier = Modifier.size(48.dp),
                        tint = NusuError
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SPAM DEFENDER SHIELD STATUS: ARMED & ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NusuError,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NUSU runs deep learning spam patterns across phone prefixes. Spammers are auto-silenced.",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Add Manual block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = activeTheme.cardBackgroundColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "MANUALLY BLOCK SUSPICIOUS/SPAM NUMBER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTheme.primaryColor,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = blacklistPhone,
                        onValueChange = { blacklistPhone = it },
                        placeholder = { Text("E.g., +91 140 334125, +1 800 500", fontSize = 12.sp, color = Color.Gray) },
                        label = { Text("Phone Number", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NusuPureWhite,
                            unfocusedTextColor = NusuPureWhite,
                            focusedBorderColor = activeTheme.primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )

                    OutlinedTextField(
                        value = blacklistReason,
                        onValueChange = { blacklistReason = it },
                        placeholder = { Text("E.g., Credit Card Sales Spammer, telemarketer", fontSize = 12.sp, color = Color.Gray) },
                        label = { Text("Reason for blocking", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NusuPureWhite,
                            unfocusedTextColor = NusuPureWhite,
                            focusedBorderColor = activeTheme.primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )

                    Button(
                        onClick = {
                            if (blacklistPhone.isNotEmpty()) {
                                viewModel.blockContact(
                                    if (blacklistReason.isEmpty()) "Spam Indicator Detected" else blacklistReason,
                                    blacklistPhone
                                )
                                blacklistPhone = ""
                                blacklistReason = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NusuError)
                    ) {
                        Text("Add to Blacklist Deflection", color = NusuPureWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "HEURISTIC SUSPICIOUS BLACKLIST (${blockedList.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = activeTheme.primaryColor,
                letterSpacing = 1.sp
            )
        }

        if (blockedList.isEmpty()) {
            item {
                Text(
                    text = "Shield database empty. No callers currently deflected.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        } else {
            items(blockedList) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(activeTheme.cardBackgroundColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.callerName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NusuPureWhite)
                        Text(item.phoneNumber, fontSize = 11.sp, color = Color.Gray)
                    }

                    Button(
                        onClick = { viewModel.unblockContact(item.phoneNumber) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, activeTheme.successColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("Unblock", color = activeTheme.successColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================================================================
// TABS 3: HISTORICAL SPEECH SCREENING COMMUNICATIONS
// ==========================================================================================

@Composable
fun MemoryHistoryTab(
    viewModel: CallGuardianViewModel,
    records: List<CallHistoryRecord>
) {
    val activeTheme = LocalNusuTheme.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCREENED COMMUNICATIONS (${records.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.primaryColor,
                    letterSpacing = 1.sp
                )

                if (records.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearLogHistory() }) {
                        Text("Wipe Storage DB", color = NusuError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (records.isEmpty()) {
            item {
                Text(
                    text = "No prior audio communication sessions archived.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        } else {
            items(records) { record ->
                HistoryRecordCard(record, viewModel)
            }
        }
    }
}

@Composable
fun HistoryRecordCard(
    record: CallHistoryRecord,
    viewModel: CallGuardianViewModel
) {
    val activeTheme = LocalNusuTheme.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (record.callStatus.lowercase() == "blocked") NusuError.copy(alpha = 0.3f) 
                else activeTheme.primaryColor.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = activeTheme.cardBackgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(record.callerName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NusuPureWhite)
                    Text(
                        text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.callTime)),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            when (record.callStatus.lowercase()) {
                                "blocked" -> NusuError.copy(alpha = 0.15f)
                                "connected" -> activeTheme.successColor.copy(alpha = 0.15f)
                                else -> Color.LightGray.copy(alpha = 0.1f)
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = record.callStatus.uppercase(),
                        fontSize = 8.sp,
                        color = when (record.callStatus.lowercase()) {
                            "blocked" -> NusuError
                            "connected" -> activeTheme.successColor
                            else -> NusuPureWhite
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI SUM: ${record.aiSummary}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = activeTheme.primaryColor
            )

            // Expanded transcript logs view
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("SPEECH INTERCEPT DIALOG LOGS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = record.voiceTranscript,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = NusuPureWhite.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ph: ${record.phoneNumber}", fontSize = 10.sp, color = Color.Gray)
                        IconButton(onClick = { viewModel.deleteHistoryRecord(record.id) }) {
                            Icon(Icons.Default.Delete, "Delete record", tint = NusuError)
                        }
                    }
                }
            }
        }
    }
}
