package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.LocalNusuTheme

/**
 * Custom modifier that replaces standard flat backgrounds with a highly interactive,
 * subtle pulsing neon glow behind the container. Responds automatically to the active theme.
 */
fun Modifier.nusuPulsingNeonGlow(): Modifier = composed {
    val activeTheme = LocalNusuTheme.current

    // Looping infinite transitions to emulate high-fidelity CSS glow animations
    val infiniteTransition = rememberInfiniteTransition(label = "NeonBasePulse")

    // Glow scale pulsing
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale2"
    )

    // Glow opacity breathing
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha1"
    )

    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha2"
    )

    // Subtle drifting orbits (X & Y deviations for floating space light)
    val driftX by infiniteTransition.animateFloat(
        initialValue = -40f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DriftX"
    )

    val driftY by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(8500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DriftY"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.995f,
        targetValue = 1.002f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "InterfaceBreathing"
    )

    this.graphicsLayer {
        scaleX = breathingScale
        scaleY = breathingScale
    }.drawBehind {
        val width = size.width
        val height = size.height

        // 1. Paint core solid background color of active theme
        drawRect(color = activeTheme.backgroundColor)

        // 2. Draw Glow 1: Top Right Nebula (Primary neon color)
        val center1 = Offset(
            x = width * 0.85f + driftX,
            y = height * 0.15f + driftY
        )
        val radius1 = minOf(width, height) * 0.55f * scale1
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    activeTheme.primaryColor.copy(alpha = alpha1),
                    activeTheme.primaryColor.copy(alpha = alpha1 * 0.4f),
                    Color.Transparent
                ),
                center = center1,
                radius = radius1
            ),
            center = center1,
            radius = radius1
        )

        // 3. Draw Glow 2: Bottom Left Cyber Core (Accent / Secondary neon color)
        val center2 = Offset(
            x = width * 0.15f - driftX,
            y = height * 0.85f - driftY
        )
        val radius2 = minOf(width, height) * 0.6f * scale2
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    activeTheme.accentColor.copy(alpha = alpha2),
                    activeTheme.secondaryColor.copy(alpha = alpha2 * 0.3f),
                    Color.Transparent
                ),
                center = center2,
                radius = radius2
            ),
            center = center2,
            radius = radius2
        )

        // 4. Draw Glow 3: Center Ambient Halo (Subtle permanent light pool behind active bento grids)
        val center3 = Offset(width * 0.5f, height * 0.5f)
        val radius3 = minOf(width, height) * 0.4f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    activeTheme.glowColor.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = center3,
                radius = radius3
            ),
            center = center3,
            radius = radius3
        )
    }
}
