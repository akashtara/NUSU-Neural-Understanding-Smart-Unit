package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalPhone
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import java.util.Locale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.services.NusuVoiceService
import com.example.services.NusuVoiceService.NusuState
import com.example.ui.components.AnimatedAICoreOrb
import com.example.ui.theme.*

@Composable
fun VoiceScreen(
    onNavigateBack: () -> Unit
) {
    val activeTheme = LocalNusuTheme.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val serviceState by NusuVoiceService.serviceState.collectAsState()
    val liveTranscription by NusuVoiceService.liveTranscription.collectAsState()
    val latestAIResponse by NusuVoiceService.latestAIResponse.collectAsState()
    val rmsDb by NusuVoiceService.rmsDb.collectAsState()

    // Collect voice biometric streams
    val detectedVoiceLabel by NusuVoiceService.detectedVoiceLabel.collectAsState()
    val voiceMatchScore by NusuVoiceService.voiceMatchScore.collectAsState()
    val isGuestVoiceSpeaker by NusuVoiceService.isGuestVoiceSpeaker.collectAsState()

    // Breathing scale animation for listening or speaking
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceBreathing")
    val orbPulseScale by infiniteTransition.animateFloat(
        initialValue = if (serviceState == NusuState.LISTENING || serviceState == NusuState.SPEAKING) 0.95f else 0.85f,
        targetValue = if (serviceState == NusuState.LISTENING || serviceState == NusuState.SPEAKING) 1.15f else 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(activeTheme.backgroundColor, activeTheme.cardBackgroundColor)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header Info Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = when (serviceState) {
                        NusuState.STANDBY -> "STANDBY ACTIVE"
                        NusuState.LISTENING -> "LISTENING..."
                        NusuState.THINKING -> "NUSU IS THINKING..."
                        NusuState.SPEAKING -> "NUSU IS SPEAKING"
                        NusuState.MUTED -> "NUSU IS MUTED"
                        NusuState.NOTIFICATION -> "RECEIVING NOTIFICATION"
                    },
                    color = when (serviceState) {
                        NusuState.LISTENING -> activeTheme.successColor
                        NusuState.THINKING -> activeTheme.accentColor
                        NusuState.SPEAKING -> activeTheme.primaryColor
                        NusuState.NOTIFICATION -> Color(0xFFFF9800)
                        else -> activeTheme.secondaryColor
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (serviceState) {
                        NusuState.STANDBY -> "Say 'Hey NUSU' or 'सुनो NUSU' to activate"
                        NusuState.LISTENING -> "Speak now, I'm listening to you"
                        NusuState.THINKING -> "Decrypting neural patterns..."
                        NusuState.SPEAKING -> "Streaming synthesized response"
                        NusuState.MUTED -> "Tap the microphone symbol to wake me up"
                        NusuState.NOTIFICATION -> "Reading system notification"
                    },
                    color = NusuPureWhite.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            // High Fidelity Animated Core Center
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                // Background visual pulses matching voice activity
                if (serviceState == NusuState.LISTENING || serviceState == NusuState.SPEAKING) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        (if (serviceState == NusuState.LISTENING) activeTheme.successColor else activeTheme.primaryColor).copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Box(
                    modifier = Modifier.size((250 * orbPulseScale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedAICoreOrb(modifier = Modifier.fillMaxSize())
                }
            }

            // Waveform visual feedback
            if (serviceState == NusuState.LISTENING) {
                NusuVoiceWaveform(
                    rmsDb = rmsDb,
                    isListening = true,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else if (serviceState == NusuState.SPEAKING) {
                // Also show a passive responsive playback waveform!
                NusuVoiceWaveform(
                    rmsDb = 2f + 4f * (kotlin.math.sin(System.currentTimeMillis() / 150.0).toFloat()),
                    isListening = true,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Real-time subtitles / live dialog container
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                if (serviceState == NusuState.LISTENING && liveTranscription.isNotEmpty()) {
                    Text(
                        text = "\" $liveTranscription \"",
                        color = activeTheme.primaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                } else if (serviceState == NusuState.SPEAKING && latestAIResponse.isNotEmpty()) {
                    Text(
                        text = latestAIResponse,
                        color = NusuPureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                } else {
                    Text(
                        text = "Continuous Loop Listening Active",
                        color = NusuPureWhite.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Neural Acoustic Calibration & Biometric Security Lock Dashboard
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(activeTheme.cardBackgroundColor.copy(alpha = 0.6f))
                        .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Acoustic Bio-Identity Signature".uppercase(),
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = detectedVoiceLabel,
                                color = if (isGuestVoiceSpeaker) activeTheme.accentColor else activeTheme.primaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isGuestVoiceSpeaker) activeTheme.accentColor.copy(alpha = 0.15f) else activeTheme.successColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isGuestVoiceSpeaker) "BIO LOCK SECURED" else "PRIMARY REGISTERED",
                                color = if (isGuestVoiceSpeaker) activeTheme.accentColor else activeTheme.successColor,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Microphone Pattern Correlation Score",
                            color = Color.LightGray.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        )
                        Text(
                            text = String.format(Locale.ROOT, "%.1f %%", voiceMatchScore),
                            color = if (isGuestVoiceSpeaker) activeTheme.accentColor else activeTheme.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { voiceMatchScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = if (isGuestVoiceSpeaker) activeTheme.accentColor else activeTheme.primaryColor,
                        trackColor = Color.Gray.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeTheme.backgroundColor.copy(alpha = 0.5f))
                            .clickable {
                                NusuVoiceService.toggleGuestSpeakerSimulation(!isGuestVoiceSpeaker)
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simulate Stranger Voice",
                                color = NusuPureWhite,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Verify command lock and respectful response",
                                color = Color.Gray,
                                fontSize = 7.5.sp
                            )
                        }
                        Switch(
                            checked = isGuestVoiceSpeaker,
                            onCheckedChange = {
                                NusuVoiceService.toggleGuestSpeakerSimulation(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = activeTheme.accentColor,
                                checkedTrackColor = activeTheme.accentColor.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Control elements
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Manual Standby Toggle Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(activeTheme.cardBackgroundColor)
                            .border(1.dp, activeTheme.accentColor.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                if (serviceState == NusuState.MUTED) {
                                    NusuVoiceService.wakeup(context)
                                } else {
                                    NusuVoiceService.sleep(context)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PowerSettingsNew,
                            contentDescription = "Sleep/Wake NUSU",
                            tint = if (serviceState == NusuState.MUTED) activeTheme.secondaryColor else activeTheme.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Large status orb icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        activeTheme.primaryColor.copy(alpha = 0.2f),
                                        activeTheme.cardBackgroundColor
                                    )
                                )
                            )
                            .border(2.dp, activeTheme.primaryColor, CircleShape)
                            .clickable {
                                NusuVoiceService.toggleListening(context)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "Voice Processing",
                            tint = if (serviceState == NusuState.LISTENING) activeTheme.successColor else activeTheme.primaryColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Direct exit
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(activeTheme.cardBackgroundColor)
                            .border(1.dp, NusuError.copy(alpha = 0.5f), CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Exit Voice",
                            tint = NusuError,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NusuVoiceWaveform(
    rmsDb: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalNusuTheme.current
    val barCount = 13 // Density of columns for elegant visualizer

    // Smooth the rmsDb input so it doesn't jump too abruptly
    val smoothedRms by animateFloatAsState(
        targetValue = if (isListening) rmsDb.coerceIn(-2f, 12f) else -2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SmoothedRms"
    )

    // Compute a generalized amplitude between 0 and 1
    val amplitude = ((smoothedRms + 2f) / 14f).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "WaveformPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "WaveformPhaseVal"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            // Distance from center (0 in middle, up to 1 at edges)
            val distanceToCenter = Math.abs(i - (barCount / 2)) / (barCount / 2f)
            val centerWeight = (1f - distanceToCenter).coerceIn(0.2f, 1.0f)

            // Dynamic oscillation per bar based on phase
            val sineWave = kotlin.math.sin(phase + (i * 0.45f))

            // Final targeted height
            val baseHeight = 6.dp
            val peakVoiceHeight = 54.dp

            // If listening but silent, keep a subtle breathing/idling wave or phase osc
            val idleAmplitude = if (isListening) 0.12f else 0.0f
            val finalAmplitude = if (amplitude > 0.05f) amplitude else idleAmplitude

            // Combine calculations
            val waveHeight = baseHeight + (peakVoiceHeight * finalAmplitude * centerWeight * (0.35f + 0.65f * kotlin.math.abs(sineWave)))

            val color = when {
                i < barCount / 3 -> activeTheme.primaryColor
                i < 2 * barCount / 3 -> activeTheme.accentColor
                else -> activeTheme.secondaryColor
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.5.dp)
                    .width(4.5.dp)
                    .height(waveHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        color = color.copy(alpha = if (isListening) 0.9f else 0.2f)
                    )
            )
        }
    }
}
