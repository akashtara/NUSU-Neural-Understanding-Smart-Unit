package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.NusuViewModel

@Composable
fun TouchCoordinatesOverlay(viewModel: NusuViewModel = viewModel()) {
    val isEnabled by viewModel.isTouchOverlayEnabled.collectAsState()
    val sensitivity by viewModel.touchSensitivity.collectAsState()
    
    if (!isEnabled) return

    var activePointers by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        activePointers = event.changes.filter { it.pressed }.map { it.position }
                    }
                }
            }
    ) {
        if (activePointers.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                activePointers.forEach { position ->
                    val color = Color.Cyan.copy(alpha = 0.7f)
                    val visualRadius = if (sensitivity > 0f) sensitivity * 3f else 60f
                    
                    // Draw crosshair
                    drawLine(color, Offset(position.x - 40f, position.y), Offset(position.x + 40f, position.y), strokeWidth = 2f)
                    drawLine(color, Offset(position.x, position.y - 40f), Offset(position.x, position.y + 40f), strokeWidth = 2f)
                    drawCircle(color.copy(alpha = 0.3f), radius = visualRadius, center = position)
                    drawCircle(color, radius = 5f, center = position)
                    
                    // We can draw text here natively if needed, but crosshair + circle is good enough
                    // for real-time visual touch input gesture detection calibration.
                }
            }
        }
    }
}
