package io.github.jqssun.airplay.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

inline fun Modifier.thenIf(condition: Boolean, block: Modifier.() -> Modifier): Modifier =
    if (condition) block() else this

@Composable
fun Modifier.focusRing(
    shape: Shape = CircleShape,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 3.dp,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, tween(160), label = "focusScale")
    val highlight by animateFloatAsState(if (focused) 0.14f else 0f, tween(160), label = "focusTint")
    return this
        .onFocusChanged { focused = it.hasFocus }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        // Draw above opaque Material containers, not behind them. No extra focus target.
        .drawWithContent {
            drawContent()
            if (highlight > 0f) {
                drawOutline(shape.createOutline(size, layoutDirection, this), color.copy(alpha = highlight))
            }
        }
        .thenIf(focused) { border(width = width, color = color, shape = shape) }
}

// focus can only land once the target is composed; retry across frames
suspend fun FocusRequester.requestFocusUntilLanded(
    attempts: Int = 10,
    isFocused: (() -> Boolean)? = null,
): Boolean {
    repeat(attempts) {
        val requested = runCatching { requestFocus() }.isSuccess
        if (isFocused?.invoke() ?: requested) return true
        withFrameNanos { }
        if (isFocused?.invoke() == true) return true
    }
    return false
}
