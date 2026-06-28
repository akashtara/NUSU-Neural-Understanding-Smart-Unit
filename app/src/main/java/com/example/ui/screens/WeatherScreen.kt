package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CyberCard
import com.example.ui.components.nusuPulsingNeonGlow
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(onNavigateBack: () -> Unit) {
    val activeTheme = LocalNusuTheme.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atmosphere", color = activeTheme.primaryColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = activeTheme.secondaryColor)
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
                .nusuPulsingNeonGlow()
        ) {
            LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Current", color = NusuPureWhite.copy(alpha = 0.5f), fontSize = 14.sp)
                                Text("24°C", color = NusuYellow, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                                Text("Sunny", color = activeTheme.secondaryColor, fontSize = 18.sp)
                            }
                            Icon(Icons.Default.WbSunny, contentDescription = "Sunny", modifier = Modifier.size(64.dp), tint = NusuWarning)
                        }
                    }
                }
            }
        }
    }
}
