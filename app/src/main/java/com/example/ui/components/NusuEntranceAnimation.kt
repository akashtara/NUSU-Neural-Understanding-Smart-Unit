package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * Premium, futuristic staggered animation utility mimicking ultra-smooth CSS easing curves.
 * Translates, scales, and fades elements sequentially for that premium cyber vibe.
 */
fun Modifier.nusuEntranceTransition(
    index: Int = 0,
    staggerDelayMs: Int = 75,
    durationMs: Int = 500
): Modifier = composed {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Safe check and staggered delay
        delay(index * staggerDelayMs.toLong())
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMs,
                easing = CubicBezierEasing(0.15f, 1f, 0.3f, 1f) // easeOutExpo curve for CSS elegance
            )
        )
    }

    val progress = animatable.value
    val alphaVal = progress
    val scaleVal = 0.95f + (0.05f * progress)
    val translateY = 30f * (1f - progress) // slide up slightly

    this.graphicsLayer {
        alpha = alphaVal
        scaleX = scaleVal
        scaleY = scaleVal
        translationY = translateY * density
    }
}
