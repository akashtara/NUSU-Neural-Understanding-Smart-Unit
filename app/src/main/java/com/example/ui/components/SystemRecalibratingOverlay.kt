package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.SystemErrorState
import com.example.ui.theme.LocalNusuTheme

@Composable
fun SystemRecalibratingOverlay(onDismiss: () -> Unit) {
    val activeTheme = LocalNusuTheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { /* Block clicks */ },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large pulsing neural core
            AnimatedAICoreOrb(modifier = Modifier.size(160.dp))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "SYSTEM RECALIBRATING",
                color = activeTheme.glowColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "API 503 THRESHOLD BREACHED",
                color = activeTheme.primaryColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recalibration Loading bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.DarkGray)
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val progress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "loading"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(activeTheme.primaryColor)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeTheme.primaryColor.copy(alpha = 0.2f),
                    contentColor = activeTheme.primaryColor
                ),
                border = BorderStroke(1.dp, activeTheme.primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("REINITIALIZE CORE", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}
