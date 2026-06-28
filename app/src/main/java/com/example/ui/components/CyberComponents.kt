package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NusuDarkNavy
import com.example.ui.theme.NusuElectricBlue
import com.example.ui.theme.NusuNeonCyan

import com.example.ui.theme.LocalNusuTheme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import com.example.R

@Composable
fun AnimatedAICoreOrb(modifier: Modifier = Modifier) {
    val activeTheme = LocalNusuTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "OrbSpin")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "SpinRot"
    )

    val bounceY by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "BounceFloat"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Pulse"
    )

    Box(
        modifier = modifier.graphicsLayer {
            translationY = bounceY
            rotationZ = rotation
            rotationX = bounceY * 1.5f
            rotationY = rotation * 0.5f // 360 degree floating 3D movement
        },
        contentAlignment = Alignment.Center
    ) {
        // Holographic Cyber Glowing Rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.width / 2) * 0.72f * pulse

            // Outer Glow ring
            rotate(rotation) {
                drawCircle(
                    style = Stroke(width = 4.dp.toPx()),
                    brush = Brush.sweepGradient(
                        colors = listOf(activeTheme.primaryColor, Color.Transparent, activeTheme.accentColor, Color.Transparent, activeTheme.secondaryColor)
                    ),
                    radius = radius * 1.05f,
                    center = center
                )
                // Secondary inner energy loop
                drawCircle(
                    style = Stroke(width = 2.dp.toPx()),
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Transparent, activeTheme.secondaryColor, Color.Transparent, activeTheme.primaryColor)
                    ),
                    radius = radius * 0.92f,
                    center = center
                )
            }

            // Radial Glow backdrop
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(activeTheme.primaryColor.copy(alpha = 0.25f), activeTheme.accentColor.copy(alpha = 0.1f), Color.Transparent),
                    center = center,
                    radius = radius * 1.2f
                ),
                radius = radius * 1.2f,
                center = center
            )
        }

        // 100% Round Floating Orb Core utilizing the official NUSU Brand Logo
        Box(
            modifier = Modifier
                .fillMaxSize(0.65f)
                .clip(CircleShape)
                .background(activeTheme.backgroundColor)
                .border(2.dp, activeTheme.primaryColor.copy(alpha = 0.8f), CircleShape)
                .border(4.dp, activeTheme.accentColor.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.nusu_orb_logo_1780607404126),
                contentDescription = "NUSU AI Core Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val activeTheme = LocalNusuTheme.current
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.3f), shape)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = activeTheme.cardBackgroundColor.copy(alpha = 0.8f)
        )
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalNusuTheme.current
    Button(
        onClick = onClick,
        modifier = modifier.border(1.dp, activeTheme.secondaryColor, RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = activeTheme.primaryColor.copy(alpha = 0.2f),
            contentColor = activeTheme.secondaryColor
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text)
    }
}
