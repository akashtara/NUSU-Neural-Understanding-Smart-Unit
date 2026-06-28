package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.DiagnosticLog
import com.example.api.LogLevel
import com.example.api.SystemErrorState
import com.example.ui.theme.LocalNusuTheme
import com.example.ui.theme.NusuPureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsDrawer() {
    val isOpen by SystemErrorState.isDiagnosticsDrawerOpen.collectAsState()
    val logs by SystemErrorState.logs.collectAsState()
    val activeTheme = LocalNusuTheme.current

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        // Full screen overlay with semi-transparent background to block touches on the outer screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    SystemErrorState.closeDiagnosticsDrawer()
                },
            contentAlignment = Alignment.CenterEnd
        ) {
            // Main drawer body container
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.85f)
                    .background(activeTheme.backgroundColor.copy(alpha = 0.98f))
                    .border(
                        width = 1.dp,
                        color = activeTheme.primaryColor.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .clickable(enabled = false) {} // block click-throughs
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Header (System title & controls)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NUSU™ DIAGNOSTICS",
                            color = activeTheme.primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("diagnostics_title")
                        )
                        Text(
                            text = "REAL-TIME LOG CONSOLE",
                            color = NusuPureWhite.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { SystemErrorState.clearLogs() },
                            modifier = Modifier.size(36.dp).background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Cache",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { SystemErrorState.closeDiagnosticsDrawer() },
                            modifier = Modifier.size(36.dp).background(activeTheme.primaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats / Status section
                val platformConfig by com.example.api.PlatformManager.currentPlatform.collectAsState()
                val platformText = platformConfig?.let { "${it.type.name} (FF: ${it.formFactor.name})" } ?: "DETECTING..."

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(activeTheme.cardBackgroundColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .border(1.dp, activeTheme.secondaryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("CONNECTION STATUS", color = NusuPureWhite.copy(alpha = 0.4f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("SECURE GATEWAY", color = activeTheme.successColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PLATFORM/DEVICE", color = NusuPureWhite.copy(alpha = 0.4f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(platformText, color = activeTheme.primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("LOG BUFFER", color = NusuPureWhite.copy(alpha = 0.4f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("${logs.size} / 150 EVENTS", color = activeTheme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Buttons Row
                var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterBadge(
                        label = "ALL",
                        isSelected = selectedFilter == null,
                        color = activeTheme.primaryColor,
                        onClick = { selectedFilter = null }
                    )
                    FilterBadge(
                        label = "INFO",
                        isSelected = selectedFilter == LogLevel.INFO,
                        color = Color.Cyan,
                        onClick = { selectedFilter = LogLevel.INFO }
                    )
                    FilterBadge(
                        label = "WARNING",
                        isSelected = selectedFilter == LogLevel.WARNING,
                        color = Color.Yellow,
                        onClick = { selectedFilter = LogLevel.WARNING }
                    )
                    FilterBadge(
                        label = "ERROR",
                        isSelected = selectedFilter == LogLevel.ERROR,
                        color = Color.Red,
                        onClick = { selectedFilter = LogLevel.ERROR }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Terminal Logs
                val filteredLogs = remember(logs, selectedFilter) {
                    if (selectedFilter == null) logs else logs.filter { it.level == selectedFilter }
                }

                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(activeTheme.cardBackgroundColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .border(1.dp, activeTheme.secondaryColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[ NO EVENT LOGS FOUND ]",
                            fontFamily = FontFamily.Monospace,
                            color = NusuPureWhite.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(activeTheme.cardBackgroundColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .border(1.dp, activeTheme.secondaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            LogItemRow(log = log, activeTheme = activeTheme)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.FilterBadge(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) color else color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) color else NusuPureWhite.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LogItemRow(log: DiagnosticLog, activeTheme: com.example.ui.theme.NusuThemePreset) {
    var isExpanded by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeString = remember(log.timestamp) { timeFormat.format(Date(log.timestamp)) }

    val levelColor = when (log.level) {
        LogLevel.INFO -> Color.Cyan
        LogLevel.WARNING -> Color.Yellow
        LogLevel.ERROR -> Color.Red
    }

    val borderAlpha = if (isExpanded) 0.5f else 0.15f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(activeTheme.backgroundColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .border(1.dp, levelColor.copy(alpha = borderAlpha), RoundedCornerShape(6.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Colored small indicator box
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(levelColor)
                )
                Text(
                    text = "[$timeString]",
                    color = NusuPureWhite.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = log.tag,
                    color = activeTheme.primaryColor.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = NusuPureWhite.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = log.message,
            color = NusuPureWhite,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis
        )

        AnimatedVisibility(
            visible = isExpanded && log.details != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            log.details?.let { details ->
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, activeTheme.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "DETAILS:",
                        color = activeTheme.primaryColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = details,
                        color = NusuPureWhite.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
