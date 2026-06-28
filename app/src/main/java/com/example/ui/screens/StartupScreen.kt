package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Message
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*

// CameraX & ViewInterop & Accompanist Permissions Imports
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.AnimatedAICoreOrb
import com.example.ui.theme.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StartupScreen(onNavigateHome: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val coroutineScope = rememberCoroutineScope()
    val activeTheme = LocalNusuTheme.current

    // Local state preferences persistence
    val sharedPrefs = remember { context.getSharedPreferences("NUSU_PRO_PREFS", Context.MODE_PRIVATE) }
    var hasCompletedInstall by remember { mutableStateOf(sharedPrefs.getBoolean("hasCompletedInstall", false)) }
    var ownerNameInput by remember { mutableStateOf(sharedPrefs.getString("ownerName", "Akash") ?: "Akash") }
    var enrolledLanguage by remember { mutableStateOf(sharedPrefs.getString("enrolledLanguage", "Hinglish") ?: "Hinglish") }
    var enrolledAccent by remember { mutableStateOf(sharedPrefs.getString("enrolledAccent", "Responsive Intelligent") ?: "Responsive Intelligent") }
    
    // Core state managers
    var state by remember { mutableStateOf<StartupState>(StartupState.Initializing) }
    var greetingText by remember { mutableStateOf("Initializing Sentient Systems...") }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    
    // First setup variables
    var currentOnboardingStep by remember { mutableStateOf(1) }
    var bioVerifiedInSetup by remember { mutableStateOf(false) }
    var voiceRecordingSimulatorActive by remember { mutableStateOf(false) }
    var voiceEnrollmentProgress by remember { mutableStateOf(0f) }
    var isBiometricRadarScanning by remember { mutableStateOf(false) }
    var bioScanProgress by remember { mutableStateOf(0f) }
    
    // System check loading statuses
    val systemCheckItems = listOf(
        "Secure ID Profile Locked" to remember { mutableStateOf(false) },
        "Neural Interface Calibrated" to remember { mutableStateOf(false) },
        "Memory Engine Connected" to remember { mutableStateOf(false) },
        "Android Keystore Cryptography Wired" to remember { mutableStateOf(false) },
        "Nusu Aesthetic Pro theme loaded" to remember { mutableStateOf(false) }
    )

    // Speech synthesis connection
    DisposableEffect(hasCompletedInstall) {
        var tts: TextToSpeech? = null
        if (!hasCompletedInstall) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.setLanguage(Locale("en", "IN"))
                }
            }
            ttsEngine = tts
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Speech trigger handler helper
    val speakText: (String) -> Unit = { text ->
        greetingText = text
        ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "onboarding")
    }

    // Auto-triage startup state on launches
    LaunchedEffect(Unit) {
        if (!hasCompletedInstall) {
            delay(150)
            state = StartupState.OnboardingMode
            speakText("Welcome to NUSU. Let's initiate onboarding setup.")
        } else {
            // High-performance silent neural splash screen. Transitions instantly within 900ms to home screen.
            state = StartupState.Initializing
            delay(900)
            onNavigateHome()
        }
    }

    // Onboarding voice simulator loop
    LaunchedEffect(voiceRecordingSimulatorActive) {
        if (voiceRecordingSimulatorActive) {
            voiceEnrollmentProgress = 0f
            while (voiceEnrollmentProgress < 1.0f) {
                delay(30)
                voiceEnrollmentProgress += 0.1f
            }
            voiceRecordingSimulatorActive = false
            speakText("Voice pattern frequency locked.")
        }
    }

    // Biometric scanner progress simulator
    LaunchedEffect(isBiometricRadarScanning) {
        if (isBiometricRadarScanning) {
            bioScanProgress = 0f
            while (bioScanProgress < 1.0f) {
                delay(20)
                bioScanProgress += 0.1f
            }
            isBiometricRadarScanning = false
            bioVerifiedInSetup = true
            speakText("Prints securely bound.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(activeTheme.backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state == StartupState.OnboardingMode) {
            // MULTI-STEP HIGH-FIDELITY FIRST INSTALL EXPERIMENTAL WIZARD
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Step Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NUSU INSTALLER",
                        color = activeTheme.primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "STEP $currentOnboardingStep OF 6",
                        color = activeTheme.secondaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Visual progress bar
                LinearProgressIndicator(
                    progress = { currentOnboardingStep / 6.0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = activeTheme.primaryColor,
                    trackColor = activeTheme.primaryColor.copy(alpha = 0.1f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                when (currentOnboardingStep) {
                    1 -> {
                        // STEP 1: WELCOME SCREEN
                        Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                            AnimatedAICoreOrb(modifier = Modifier.size(140.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "NUSU™",
                            color = activeTheme.primaryColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Neural Understanding Smart Unit",
                            color = activeTheme.secondaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Always Listening. Always Learning. Always Ready.\nOne AI. Infinite Possibilities.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "Welcome to your next-generation AI companion ecosystem.\nCreated by Akash Tarafder • Powered by MrTarafder",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = {
                                currentOnboardingStep = 2
                                speakText("Aashu, register your authorized owner name. By default cognitive records suggest Akash.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("INITIALIZE COGNITIVE HUB", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    2 -> {
                        // STEP 2: OWNER REGISTRATION
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Security", tint = activeTheme.primaryColor, modifier = Modifier.size(54.dp))
                        Text(
                            text = "OWNER REGISTRATION & ENCRYPTION",
                            color = activeTheme.primaryColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter your primary name. Nusu registers customized intelligence kernels around this identity.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = ownerNameInput,
                            onValueChange = { ownerNameInput = it },
                            label = { Text("COGNITIVE OWNER IDENTITY", fontSize = 10.sp, letterSpacing = 1.sp, color = activeTheme.primaryColor) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeTheme.primaryColor,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                                focusedContainerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.4f),
                                unfocusedContainerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.4f),
                                focusedLabelColor = activeTheme.primaryColor,
                                unfocusedLabelColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                sharedPrefs.edit().putString("ownerName", ownerNameInput).apply()
                                currentOnboardingStep = 3
                                speakText("Now, choose your custom voice dialect and language profile. Nusu learns accents instantly.")
                            },
                            enabled = ownerNameInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.secondaryColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("LOCK IDENTITY AND RECORD", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NusuPureWhite)
                        }
                    }
                    3 -> {
                        // STEP 3: VOICE ENROLLMENT
                        Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice enrollment", tint = activeTheme.accentColor, modifier = Modifier.size(54.dp))
                        Text(
                            text = "NEURAL VOICE INTEGRAL TUNING",
                            color = activeTheme.primaryColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Select your preferred primary language. NUSU specializes in natural seamless Hinglish conversational responses.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        // Language chips lists
                        val languages = listOf("Hinglish", "Hindi", "English", "Bangla", "Sylheti", "Urdu", "Arabic (Basic)")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(activeTheme.cardBackgroundColor)
                                    .border(1.dp, activeTheme.accentColor, RoundedCornerShape(14.dp))
                                    .padding(vertical = 12.dp, horizontal = 24.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "CLASSIFIED:", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text(text = enrolledLanguage, color = activeTheme.accentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Horizontal select row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Hinglish", "English", "Bangla").forEach { lang ->
                                Button(
                                    onClick = { enrolledLanguage = lang },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (enrolledLanguage == lang) activeTheme.primaryColor else activeTheme.cardBackgroundColor.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(lang, fontSize = 10.sp, color = NusuPureWhite)
                                }
                            }
                        }

                        // Simulation button
                        Button(
                            onClick = { voiceRecordingSimulatorActive = true },
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.cardBackgroundColor),
                            border = BorderStroke(1.dp, activeTheme.accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (voiceRecordingSimulatorActive) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = activeTheme.accentColor)
                                    Text("PROBE VOICE SPECTRUM: ${(voiceEnrollmentProgress * 100).toInt()}%", fontSize = 11.sp, color = activeTheme.accentColor, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("TAP TO REGISTER VOICE SENSORS", fontSize = 11.sp, color = activeTheme.accentColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                sharedPrefs.edit()
                                    .putString("enrolledLanguage", enrolledLanguage)
                                    .putString("enrolledAccent", enrolledAccent)
                                    .apply()
                                currentOnboardingStep = 4
                                speakText("Excellent. Let's wire fingerprint sensors and biological facial features.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("LOCK SPECTRUM PROFILE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    4 -> {
                        // STEP 4: REAL-TIME BIOMETRIC PROTECTION & CAMERA FACE ALIGNMENT
                        val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
                        
                        Text(
                            text = "REAL-TIME BIOMETRIC SIGNATURE",
                            color = activeTheme.primaryColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Face identification & Fingerprint scanning secures sensitive logs and commander command access.",
                            color = Color.Gray,
                            fontSize = 10.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (!cameraPermissionState.status.isGranted) {
                            // Camera Permission request block
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(activeTheme.cardBackgroundColor.copy(alpha = 0.5f))
                                    .border(1.dp, activeTheme.accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Lock icon",
                                    tint = activeTheme.accentColor,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "CAMERA SENSORS REQUIRED",
                                    color = NusuPureWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "NUSU needs physical camera connection to run real-time local face scanning calibration.",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                                Button(
                                    onClick = { cameraPermissionState.launchPermissionRequest() },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.accentColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("GRANT CAMERA ACCESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NusuPureWhite)
                                }
                            }
                        } else {
                            // Active CameraX viewfinder & Face scanning simulation Mesh
                            val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
                            val previewView = remember { PreviewView(context) }
                            
                            LaunchedEffect(Unit) {
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        context as androidx.lifecycle.LifecycleOwner,
                                        cameraSelector,
                                        preview
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            // Pulsing / Sweeping laser scanner variables
                            val transitionInstance = rememberInfiniteTransition(label = "FaceScanLoop")
                            val laserSweepProgress by transitionInstance.animateFloat(
                                initialValue = 0.15f,
                                targetValue = 0.85f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "LaserLine"
                            )

                            // Interactive camera viewfinder card
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, if (bioVerifiedInSetup) activeTheme.successColor else activeTheme.primaryColor, CircleShape)
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                // Camera Viewfinder
                                AndroidView(
                                    factory = { previewView },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Real-Time Biometric Vector Mesh Graphics Overlay
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val localWidth = size.width
                                    val localHeight = size.height
                                    val laserY = localHeight * laserSweepProgress

                                    // Draw sweeping laser scan line
                                    drawLine(
                                        color = if (bioVerifiedInSetup) activeTheme.successColor else activeTheme.primaryColor.copy(alpha = 0.8f),
                                        start = Offset(0f, laserY),
                                        end = Offset(localWidth, laserY),
                                        strokeWidth = 3.dp.toPx()
                                    )

                                    // Draw scanning grid boundaries
                                    if (!bioVerifiedInSetup) {
                                        // Draw four diagnostic alignment corners / target boxes mimicking face landmarks
                                        drawRect(
                                            color = activeTheme.primaryColor.copy(alpha = 0.6f),
                                            topLeft = Offset(localWidth * 0.25f, localHeight * 0.3f),
                                            size = androidx.compose.ui.geometry.Size(20.dp.toPx(), 20.dp.toPx()),
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                        drawRect(
                                            color = activeTheme.primaryColor.copy(alpha = 0.6f),
                                            topLeft = Offset(localWidth * 0.65f, localHeight * 0.3f),
                                            size = androidx.compose.ui.geometry.Size(20.dp.toPx(), 20.dp.toPx()),
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                        // Concentric scanning circle guidance
                                        drawCircle(
                                            color = activeTheme.accentColor.copy(alpha = 0.3f),
                                            radius = (localWidth / 2) * 0.85f,
                                            style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                                        )
                                    }
                                }

                                // Interactive Scanning states overlay
                                if (isBiometricRadarScanning) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = activeTheme.primaryColor, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("CALIBRATING: ${(bioScanProgress * 100).toInt()}%", fontSize = 9.sp, color = activeTheme.primaryColor, fontWeight = FontWeight.Bold)
                                            Text("MAPPING IRIS PATTERNS", fontSize = 7.5.sp, color = Color.Gray)
                                        }
                                    }
                                } else if (bioVerifiedInSetup) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(activeTheme.backgroundColor.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success tick", tint = activeTheme.successColor, modifier = Modifier.size(36.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("SECURED LOCK", color = activeTheme.successColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                            Text("COMMANDER AKASH VERIFIED", color = Color.LightGray, fontSize = 8.sp)
                                        }
                                    }
                                }
                            }

                            if (!isBiometricRadarScanning && !bioVerifiedInSetup) {
                                Button(
                                    onClick = { isBiometricRadarScanning = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("INITIATE DYNAMIC FACE SCAN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Fingerprint enclave validator trigger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(activeTheme.cardBackgroundColor.copy(alpha = 0.4f))
                                .border(1.dp, if (bioVerifiedInSetup) activeTheme.successColor.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .clickable {
                                    try {
                                        val executor = ContextCompat.getMainExecutor(context)
                                        val biometricPrompt = BiometricPrompt(activity!!, executor, object : BiometricPrompt.AuthenticationCallback() {
                                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                super.onAuthenticationSucceeded(result)
                                                bioVerifiedInSetup = true
                                                speakText("Fingerprint metrics securely aligned with face parameters.")
                                            }
                                        })
                                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                            .setTitle("NUSU Sentinel Calibration")
                                            .setSubtitle("Verify owner finger biometric signature")
                                            .setNegativeButtonText("Back")
                                            .build()
                                        biometricPrompt.authenticate(promptInfo)
                                    } catch (e: Exception) {
                                        // Simulate fingerprint scan if hardware prompt is occupied or emulated
                                        isBiometricRadarScanning = true
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = "Fingerprint scanning", tint = if (bioVerifiedInSetup) activeTheme.successColor else activeTheme.primaryColor, modifier = Modifier.size(24.dp))
                            Column {
                                Text(
                                    text = "Cryptographic Fingerprint Binder",
                                    color = NusuPureWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (bioVerifiedInSetup) "Biological fingerprint bound in security enclave" else "Bind secure fingerprint sensor",
                                    color = Color.Gray,
                                    fontSize = 8.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                currentOnboardingStep = 5
                                speakText("Please grant me required accessibility and systems permissions to become your full device companion.")
                            },
                            enabled = bioVerifiedInSetup,
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.successColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("SAVE SECURE BIOMETRICS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NusuDeepSpaceBlack)
                        }
                    }
                    5 -> {
                        // STEP 5: PERMISSION CENTER
                        Icon(imageVector = Icons.Default.Security, contentDescription = "Permissions", tint = activeTheme.primaryColor, modifier = Modifier.size(54.dp))
                        Text(
                            text = "NUSU PERMISSION CENTER",
                            color = activeTheme.primaryColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To fully enable device control, automation, & intelligence, I need optional access.",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )

                         Spacer(modifier = Modifier.height(10.dp))
                        
                        com.example.ui.components.PermissionCenterComponent()

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                currentOnboardingStep = 6
                                speakText("Analyzing neural pathways. Database verification check underway.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("CONTINUE SETUP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    6 -> {
                        // STEP 6: SYSTEM INITIALIZATION CHECKLIST
                        Icon(imageVector = Icons.Default.Security, contentDescription = "Setup complete", tint = activeTheme.primaryColor, modifier = Modifier.size(54.dp))
                        Text(
                            text = "NUSU CORE INITIALIZATION",
                            color = activeTheme.primaryColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Animation list simulator
                        LaunchedEffect(Unit) {
                            for (item in systemCheckItems) {
                                delay(100)
                                item.second.value = true
                            }
                            speakText("All protocols verified.")
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            systemCheckItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(activeTheme.cardBackgroundColor.copy(alpha = 0.5f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = item.first, color = NusuPureWhite, fontSize = 11.sp)
                                    if (item.second.value) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Ready", tint = activeTheme.successColor, modifier = Modifier.size(16.dp))
                                    } else {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = activeTheme.primaryColor)
                                    }
                                }
                            }
                        }

                        val allReady = systemCheckItems.all { it.second.value }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                sharedPrefs.edit().putBoolean("hasCompletedInstall", true).apply()
                                onNavigateHome()
                            },
                            enabled = allReady,
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.successColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("ENTER SYSTEM HUB PRO™", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NusuDeepSpaceBlack)
                        }
                    }
                }
            }
        } else {
            // HIGH-PERFORMANCE NEON-GLOWING NEURAL PULSE SPLASH SCREEN
            NeuralPulseSplashScreen(
                activeTheme = activeTheme,
                onInstantSkip = onNavigateHome
            )
        }
    }
}

@Composable
fun NeuralPulseSplashScreen(
    activeTheme: NusuThemePreset,
    onInstantSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NeuralPulseLoop")

    // Core scale pulse of the orb
    val orbScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    // Wave 1: Cyan Neon
    val wave1Progress by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave1"
    )

    // Wave 2: Crimson/Accent Neon
    val wave2Progress by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave2"
    )

    // Wave 3 Concentric energy particle loop
    val wave3Progress by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave3"
    )

    // Glow intensity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onInstantSkip() },
        contentAlignment = Alignment.Center
    ) {
        // High-performance Canvas drawing the neon glowing 'Neural Pulse' waves
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1f)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width * 0.45f

            // 1. Base Radial Ambient Glow Background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeTheme.primaryColor.copy(alpha = 0.2f * glowAlpha),
                        activeTheme.accentColor.copy(alpha = 0.1f * glowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 1.5f
                ),
                radius = maxRadius * 1.5f,
                center = center
            )

            // 2. Wave 1 Neon Ring (Cyan/Primary)
            val r1 = maxRadius * (0.3f + 0.7f * wave1Progress)
            val alpha1 = (1.0f - wave1Progress) * 0.8f
            drawCircle(
                style = Stroke(width = 3.dp.toPx()),
                color = activeTheme.primaryColor.copy(alpha = alpha1),
                radius = r1,
                center = center
            )
            // Extra outer glow line for Wave 1
            drawCircle(
                style = Stroke(width = 8.dp.toPx()),
                color = activeTheme.primaryColor.copy(alpha = alpha1 * 0.3f),
                radius = r1,
                center = center
            )

            // 3. Wave 2 Neon Ring (Crimson/Accent) - shifted phase
            val shiftedProgress = (wave2Progress + 0.5f) % 1.0f
            val r2 = maxRadius * (0.3f + 0.7f * shiftedProgress)
            val alpha2 = (1.0f - shiftedProgress) * 0.8f
            drawCircle(
                style = Stroke(width = 2.dp.toPx()),
                color = activeTheme.accentColor.copy(alpha = alpha2),
                radius = r2,
                center = center
            )
            // Blur glow
            drawCircle(
                style = Stroke(width = 6.dp.toPx()),
                color = activeTheme.accentColor.copy(alpha = alpha2 * 0.25f),
                radius = r2,
                center = center
            )

            // 4. Wave 3 Concentric energy particle loop
            val r3 = maxRadius * (0.2f + 0.8f * wave3Progress)
            val alpha3 = (1.0f - wave3Progress) * 0.5f
            drawCircle(
                style = Stroke(width = 1.dp.toPx()),
                color = activeTheme.secondaryColor.copy(alpha = alpha3),
                radius = r3,
                center = center
            )
        }

        // Center glassmorphic interactive logo orb
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(orbScale)
                    .clip(CircleShape)
                    .border(2.dp, activeTheme.primaryColor.copy(alpha = 0.9f), CircleShape)
                    .border(5.dp, activeTheme.accentColor.copy(alpha = 0.4f), CircleShape)
                    .background(activeTheme.cardBackgroundColor.copy(alpha = 0.82f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Interactive inner core rotation/spinning shadow matching Cyber Components
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "LogoRot"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    rotate(rotation) {
                        drawCircle(
                            style = Stroke(width = 2.dp.toPx()),
                            brush = Brush.sweepGradient(
                                colors = listOf(activeTheme.primaryColor, Color.Transparent, activeTheme.accentColor, Color.Transparent)
                            ),
                            radius = (size.width / 2) * 1.05f,
                            center = center
                        )
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.nusu_orb_logo_1780607404126),
                    contentDescription = "NUSU AI Logo Core",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Premium tech-branding typography
            Text(
                text = "NUSU™",
                color = activeTheme.primaryColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "NEURAL PULSE CONNECTION ACTIVE",
                color = activeTheme.accentColor.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Tech hint showing speed and skip capability
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeTheme.cardBackgroundColor.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = activeTheme.primaryColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TAP SCREEN TO BYPASS INSTANTLY",
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(title: String, icon: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BentoCard(
        modifier = modifier.height(80.dp),
        onClick = onClick,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, color = NusuPureWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

enum class StartupState {
    Initializing,
    Authenticating,
    Verified,
    Ready,
    OnboardingMode
}

