package com.example.ui.components

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.LocalNusuTheme

data class PermissionItemData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val type: PermissionType,
    val permissions: List<String> = emptyList(), // Used for RUNTIME type
    val actionIntent: Intent? = null // Used for SPECIAL type
)

enum class PermissionType {
    RUNTIME, SPECIAL
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PermissionCenterComponent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activeTheme = LocalNusuTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Recheck trigger updated when app returns from background settings
    var lifecycleRecheckTrigger by remember { mutableIntStateOf(0) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleRecheckTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val coreFeatures = remember {
        listOf(
            PermissionItemData(
                "Accessibility Service",
                "Automates UI actions or clicks for WhatsApp & YouTube control.",
                Icons.Default.Accessibility,
                PermissionType.SPECIAL,
                actionIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            ),
            PermissionItemData(
                "Notification Access",
                "Reads texts and group alerts to announce incoming messages.",
                Icons.Default.Notifications,
                PermissionType.SPECIAL,
                actionIntent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            ),
            PermissionItemData(
                "Overlay Permission",
                "Required to show NUSU's dynamic holographic AI orb over standard applications.",
                Icons.Default.Layers,
                PermissionType.SPECIAL,
                actionIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            ),
            PermissionItemData(
                "Microphone Access",
                "Required to receive and process your hands-free voice-first commands.",
                Icons.Default.Mic,
                PermissionType.RUNTIME,
                listOf(Manifest.permission.RECORD_AUDIO)
            )
        )
    }

    val privacyAccess = remember {
        listOf(
            PermissionItemData(
                "Contacts Link",
                "Allows looking up contact details to dial or write messages.",
                Icons.Default.Contacts,
                PermissionType.RUNTIME,
                listOf(Manifest.permission.READ_CONTACTS)
            ),
            PermissionItemData(
                "Phone dialer",
                "Trigger direct call initiation through your vocal request.",
                Icons.Default.Phone,
                PermissionType.RUNTIME,
                listOf(Manifest.permission.CALL_PHONE)
            ),
            PermissionItemData(
                "SMS Messenger",
                "Automates drafts send & previews.",
                Icons.AutoMirrored.Filled.Message,
                PermissionType.RUNTIME,
                listOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS)
            ),
            PermissionItemData(
                "Vision Camera",
                "Required for face-scanning sentinel & vision tasks.",
                Icons.Default.CameraAlt,
                PermissionType.RUNTIME,
                listOf(Manifest.permission.CAMERA)
            ),
            PermissionItemData(
                "Precise Location",
                "Provides hyper-local answers regarding weather or directions.",
                Icons.Default.LocationOn,
                PermissionType.RUNTIME,
                listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            ),
            PermissionItemData(
                "Calendar Ledger",
                "Synchronizes tasks and meeting entries into chronological databases.",
                Icons.Default.Event,
                PermissionType.RUNTIME,
                listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            )
        )
    }

    // State of expandables
    var isCoreExpanded by remember { mutableStateOf(true) }
    var isPrivacyExpanded by remember { mutableStateOf(false) }

    // Sequential onboarding walkthrough engine state
    var showGuidedExplainDialog by remember { mutableStateOf(false) }
    var currentExplainPermissionItem by remember { mutableStateOf<PermissionItemData?>(null) }
    var sequentialModeActive by remember { mutableStateOf(false) }

    // All runtime permissions mapping
    val allRuntimePermissions = remember {
        listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    }

    // Multi permission launcher
    val multipleRuntimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultsMap ->
        lifecycleRecheckTrigger++
        // If sequential mode is active, check next step
        if (sequentialModeActive) {
            // Check special permissions sequentially
            evaluateSequentialStatus(context, coreFeatures, privacyAccess) { nextItem ->
                if (nextItem != null) {
                    currentExplainPermissionItem = nextItem
                    showGuidedExplainDialog = true
                } else {
                    sequentialModeActive = false
                }
            }
        }
    }

    // Continuous evaluation on resume
    LaunchedEffect(lifecycleRecheckTrigger, sequentialModeActive) {
        if (sequentialModeActive) {
            evaluateSequentialStatus(context, coreFeatures, privacyAccess) { nextItem ->
                if (nextItem != null) {
                    currentExplainPermissionItem = nextItem
                    showGuidedExplainDialog = true
                } else {
                    sequentialModeActive = false
                }
            }
        }
    }

    // Count statistics
    val coreGrantedCount = coreFeatures.count { checkPermission(context, it) }
    val privacyGrantedCount = privacyAccess.count { checkPermission(context, it) }
    val totalGrantedCount = coreGrantedCount + privacyGrantedCount
    val totalPermissionsCount = coreFeatures.size + privacyAccess.size
    val isAllGranted = totalGrantedCount == totalPermissionsCount

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Upper stats banner & "Grant All / Guide Sequential" triggers
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, activeTheme.primaryColor.copy(alpha = 0.23f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Calibration Status",
                            tint = if (isAllGranted) activeTheme.successColor else activeTheme.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "CALIBRATION STATUS",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "$totalGrantedCount / $totalPermissionsCount COMPLETED",
                        color = if (isAllGranted) activeTheme.successColor else activeTheme.secondaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Smooth linear status tracker indicator
                LinearProgressIndicator(
                    progress = { totalGrantedCount.toFloat() / totalPermissionsCount.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isAllGranted) activeTheme.successColor else activeTheme.primaryColor,
                    trackColor = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (!isAllGranted) {
                    Button(
                        onClick = {
                            sequentialModeActive = true
                            // Trigger runtimes first
                            val ungrantedRuntimes = allRuntimePermissions.filter {
                                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                            }
                            if (ungrantedRuntimes.isNotEmpty()) {
                                multipleRuntimeLauncher.launch(ungrantedRuntimes.toTypedArray())
                            } else {
                                // Runtimes are all granted, proceed with sequential special guide
                                evaluateSequentialStatus(context, coreFeatures, privacyAccess) { nextItem ->
                                    if (nextItem != null) {
                                        currentExplainPermissionItem = nextItem
                                        showGuidedExplainDialog = true
                                    } else {
                                        sequentialModeActive = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeTheme.primaryColor
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Grant All", modifier = Modifier.size(16.dp), tint = Color.Black)
                            Text(
                                "SEQUENTIALLY GRANT ALL REQUIRED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(activeTheme.successColor.copy(alpha = 0.12f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ ALL ADVANCED CREDENTIALS SANCTIONED",
                            color = activeTheme.successColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Expandable category 1: Core Assistant features
        CategoryHeader(
            title = "CORE ASSISTANT FEATURES",
            subtitle = "Sensing & Automation engines",
            countText = "$coreGrantedCount / ${coreFeatures.size}",
            isExpanded = isCoreExpanded,
            onToggle = { isCoreExpanded = !isCoreExpanded },
            color = activeTheme.primaryColor
        )

        AnimatedVisibility(
            visible = isCoreExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                coreFeatures.forEach { item ->
                    PermissionRowItem(item = item, context = context, recheckTrigger = lifecycleRecheckTrigger)
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Expandable category 2: Privacy / Device access features
        CategoryHeader(
            title = "PRIVACY & DEVICE ACCESS",
            subtitle = "Local communications & system queries",
            countText = "$privacyGrantedCount / ${privacyAccess.size}",
            isExpanded = isPrivacyExpanded,
            onToggle = { isPrivacyExpanded = !isPrivacyExpanded },
            color = activeTheme.secondaryColor
        )

        AnimatedVisibility(
            visible = isPrivacyExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                privacyAccess.forEach { item ->
                    PermissionRowItem(item = item, context = context, recheckTrigger = lifecycleRecheckTrigger)
                }
            }
        }

        // Custom Guided Sequential Dialog
        if (showGuidedExplainDialog && currentExplainPermissionItem != null) {
            val item = currentExplainPermissionItem!!
            Dialog(
                onDismissRequest = { 
                    showGuidedExplainDialog = false
                    sequentialModeActive = false // Break sequence on dismiss
                },
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = activeTheme.backgroundColor),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(activeTheme.primaryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "GRANT ${item.title.uppercase()}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = getGuidedInstructionsText(item.title, item.description),
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { 
                                    showGuidedExplainDialog = false
                                    sequentialModeActive = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                border = ButtonDefaults.outlinedButtonBorder.copy()
                            ) {
                                Text("SKIP", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    showGuidedExplainDialog = false
                                    if (item.actionIntent != null) {
                                        try {
                                            context.startActivity(item.actionIntent)
                                        } catch (e: Exception) {}
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("OPEN SETTINGS", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(12.dp), tint = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Function to decide guided detailed step-by-step instructions
private fun getGuidedInstructionsText(title: String, baseDesc: String): String {
    return when (title) {
        "Accessibility Service" -> "1. Click Open Settings below.\n2. Tap Installed Services or Accessibility Menu.\n3. Locate 'NUSU' and toggle the Enable Switch.\n4. Accept overlay warnings to align speech systems."
        "Notification Access" -> "1. Click Open Settings below.\n2. Scroll down to find 'NUSU'.\n3. Click and enable 'Allow Notification Access'.\nThis lets NUSU read incoming SMS or WhatsApp texts to review alerts for you!"
        "Overlay Permission" -> "1. Click Open Settings below.\n2. Find 'NUSU' on the system draws checklist.\n3. Set 'Allow drawing over other apps' to ON.\nThis allows NUSU's dynamic ambient floating HUD to operate!"
        else -> baseDesc
    }
}

// Evaluate next sequential step
private fun evaluateSequentialStatus(
    context: Context,
    core: List<PermissionItemData>,
    privacy: List<PermissionItemData>,
    onDecision: (PermissionItemData?) -> Unit
) {
    // Check if any special core permission is ungranted. If so, return it so we prompt the dialog.
    for (item in core) {
        if (!checkPermission(context, item)) {
            if (item.type == PermissionType.SPECIAL) {
                onDecision(item)
                return
            }
        }
    }
    // Check privacy specials
    for (item in privacy) {
        if (!checkPermission(context, item)) {
            if (item.type == PermissionType.SPECIAL) {
                onDecision(item)
                return
            }
        }
    }
    onDecision(null) // Complete sequence!
}

@Composable
private fun CategoryHeader(
    title: String,
    subtitle: String,
    countText: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    color: Color
) {
    val activeTheme = LocalNusuTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = subtitle,
                color = Color.LightGray.copy(alpha = 0.8f),
                fontSize = 9.sp
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = countText, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PermissionRowItem(
    item: PermissionItemData,
    context: Context,
    recheckTrigger: Int
) {
    val activeTheme = LocalNusuTheme.current
    var isGranted by remember(recheckTrigger) { mutableStateOf(checkPermission(context, item)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        isGranted = checkPermission(context, item)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(activeTheme.cardBackgroundColor.copy(alpha = 0.4f))
            .clickable {
                if (!isGranted) {
                    if (item.type == PermissionType.RUNTIME) {
                        launcher.launch(item.permissions.toTypedArray())
                    } else if (item.type == PermissionType.SPECIAL && item.actionIntent != null) {
                        try {
                            context.startActivity(item.actionIntent)
                        } catch (e: Exception) {}
                    }
                }
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(activeTheme.primaryColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isGranted) activeTheme.successColor else activeTheme.primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.description,
                color = Color.LightGray,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Switch(
            checked = isGranted,
            onCheckedChange = { checked ->
                if (checked) {
                    if (item.type == PermissionType.RUNTIME) {
                        launcher.launch(item.permissions.toTypedArray())
                    } else if (item.type == PermissionType.SPECIAL && item.actionIntent != null) {
                        try {
                            context.startActivity(item.actionIntent)
                        } catch (e: Exception) {}
                    }
                } else {
                    if (item.type == PermissionType.RUNTIME) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } else if (item.type == PermissionType.SPECIAL && item.actionIntent != null) {
                        try {
                            context.startActivity(item.actionIntent)
                        } catch (e: Exception) {}
                    }
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = activeTheme.backgroundColor,
                checkedTrackColor = activeTheme.successColor,
                uncheckedThumbColor = activeTheme.backgroundColor,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

private fun checkPermission(context: Context, item: PermissionItemData): Boolean {
    return when (item.type) {
        PermissionType.RUNTIME -> {
            item.permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
        PermissionType.SPECIAL -> {
            when (item.title) {
                "Accessibility Service" -> {
                    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                    enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
                }
                "Notification Access" -> {
                    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                }
                "Overlay Permission" -> {
                    Settings.canDrawOverlays(context)
                }
                else -> false
            }
        }
    }
}
