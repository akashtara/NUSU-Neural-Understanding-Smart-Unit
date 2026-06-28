package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AnimatedAICoreOrb
import com.example.ui.components.nusuEntranceTransition
import com.example.ui.components.nusuPulsingNeonGlow
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToAestheticHub: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToMood: () -> Unit,
    onNavigateToCallGuardian: () -> Unit,
    onNavigateToFirewall: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeTheme = LocalNusuTheme.current
    val sharedPrefs = remember { context.getSharedPreferences("NUSU_PRO_PREFS", Context.MODE_PRIVATE) }
    val ownerName = remember { sharedPrefs.getString("ownerName", "Akash") ?: "Akash" }

    // Dynamic hour greeting
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val timeGreeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nusuPulsingNeonGlow()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nusuEntranceTransition(0),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SYSTEM ONLINE",
                            color = activeTheme.primaryColor.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "$timeGreeting, $ownerName",
                            color = NusuPureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(activeTheme.cardBackgroundColor)
                            .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.5f), CircleShape)
                            .clickable { onNavigateToAestheticHub() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Themes & Personality Settings",
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .nusuEntranceTransition(1),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedAICoreOrb(modifier = Modifier.size(180.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "NUSU™",
                                    color = activeTheme.primaryColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "Neural Understanding Smart Unit",
                                    color = activeTheme.secondaryColor.copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SENTIENT LOGIC ACTIVE",
                            color = activeTheme.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 4.sp,
                            modifier = Modifier.alpha(0.8f)
                        )
                    }
                }
            }

            item {
                // Bento Grid Content
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Weather and Productivity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nusuEntranceTransition(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BentoCard(
                            modifier = Modifier.weight(1f).height(110.dp),
                            onClick = onNavigateToWeather
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ENVIRONMENT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Text("☁️", color = activeTheme.secondaryColor)
                                }
                                Column {
                                    Text("24°C", color = NusuPureWhite, fontSize = 24.sp, fontWeight = FontWeight.Light)
                                    Text("Kolkata, Clear", color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }

                        BentoCard(
                            modifier = Modifier.weight(1f).height(110.dp),
                            borderColor = activeTheme.accentColor.copy(alpha = 0.4f),
                            borderWidth = 2.dp, // Emulate the left border accent
                            onClick = onNavigateToMood
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("NEXT PHASE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Text("✦", color = activeTheme.accentColor)
                                }
                                Column {
                                    Text("UI Review", color = NusuPureWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("11:30 AM", color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // Theme Customizer Hub Entrance
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nusuEntranceTransition(3),
                        borderColor = activeTheme.accentColor.copy(alpha = 0.6f),
                        onClick = onNavigateToAestheticHub
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(activeTheme.accentColor.copy(alpha = 0.15f))
                                    .border(1.dp, activeTheme.accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎨", fontSize = 18.sp)
                            }
                            Column {
                                Text(
                                    text = "NUSU AESTHETIC HUB PRO™",
                                    color = activeTheme.primaryColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Design Your Own Future • 100+ Live OS Themes",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // 1000-LAYER QUANTUM FIREWALL CARD
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nusuEntranceTransition(4),
                        borderColor = activeTheme.primaryColor.copy(alpha = 0.5f),
                        onClick = onNavigateToFirewall
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(activeTheme.primaryColor.copy(alpha = 0.15f))
                                    .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🛡️", fontSize = 18.sp)
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "1000-LAYER QUANTUM DEFENDER",
                                        color = activeTheme.primaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(activeTheme.successColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "UNBREAKABLE",
                                            color = activeTheme.successColor,
                                            fontSize = 6.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "Active Auto-Recovery Sentinel • Protect User & Commander Data",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Row 2: Memory Engine
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nusuEntranceTransition(5),
                        borderColor = activeTheme.successColor.copy(alpha = 0.2f),
                        onClick = onNavigateToMemory
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(activeTheme.successColor.copy(alpha = 0.1f))
                                    .border(1.dp, activeTheme.successColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🧠", fontSize = 18.sp)
                            }
                            Column {
                                Text("RECENT INSIGHT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "\"Akash, you preferred Dark Mode for the studio modules last night.\"",
                                    color = NusuPureWhite.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }

                    // Row 3: Device Stats
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nusuEntranceTransition(6),
                        contentPadding = PaddingValues(16.dp),
                        onClick = onNavigateToFirewall
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatItem("CPU", "14%", activeTheme.primaryColor)
                            HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = Color.White.copy(alpha = 0.1f))
                            StatItem("MEM", "2.4GB", activeTheme.accentColor)
                            HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = Color.White.copy(alpha = 0.1f))
                            StatItem("SEC", "ENCRYPTED", activeTheme.successColor)
                        }
                    }

                    // NUSU™ AI CALL GUARDIAN & INTERCEPT PANEL
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nusuEntranceTransition(7),
                        borderColor = activeTheme.successColor.copy(alpha = 0.5f),
                        borderWidth = 1.dp,
                        onClick = onNavigateToCallGuardian
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Header badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, activeTheme.successColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.nusu_orb_logo_1780607404126),
                                            contentDescription = "Call Guardian Icon",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Text(
                                        text = "NUSU™ AI CALL GUARDIAN",
                                        color = activeTheme.primaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(activeTheme.successColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, activeTheme.successColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", color = activeTheme.successColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Interactive Lock Screen Display Card Mock
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "PROTECTING AKASH",
                                    color = activeTheme.primaryColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "Neural Call Intercept Shield Enabled",
                                    color = activeTheme.secondaryColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Preventing telemarketing spam bots, defending time and attention, organizing incoming voice lines in Hinglish.",
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            // Features Grid
                            Text(
                                text = "CORE TELEPHONY PROTECTION SERVICES",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val features = listOf(
                                    "AI Intercept Screen" to "Dynamic voice screening interactive simulator",
                                    "VIP Registry Room" to "Define Mom or Family bypass emergency exceptions",
                                    "Spam Blacklist" to "Heuristic blocker auto-silences bad prefixes",
                                    "Communication Logs" to "Room Database cryptographically tracks dialogue"
                                )

                                features.forEach { (feat, desc) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(feat, color = NusuPureWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text(desc, color = Color.Gray, fontSize = 9.sp)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(activeTheme.successColor)
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

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color? = null,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val activeTheme = LocalNusuTheme.current
    val finalBorderColor = borderColor ?: activeTheme.primaryColor.copy(alpha = 0.2f)
    val shape = RoundedCornerShape(24.dp)
    Card(
        modifier = modifier
            .border(borderWidth, finalBorderColor, shape)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.7f)
        )
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun StatItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}
