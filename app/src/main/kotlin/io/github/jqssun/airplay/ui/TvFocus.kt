package io.github.jqssun.airplay.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

inline fun Modifier.thenIf(condition: Boolean, block: Modifier.() -> Modifier): Modifier =
    if (condition) block() else this

/** Selection: one soft rounded fill behind the focused element. No outline, no glow. */
@Composable
fun Modifier.focusRing(
    shape: Shape = CircleShape,
    focusScale: Float = 1f,
): Modifier {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    val lit by animateFloatAsState(if (focused) 1f else 0f, tween(140), label = "focusLit")
    val scale by animateFloatAsState(if (focused) focusScale else 1f, tween(140), label = "focusScale")
    return this
        .onFocusChanged { focused = it.hasFocus }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .drawBehind {
            if (lit > 0f) drawOutline(shape.createOutline(size, layoutDirection, this), g.focusFill, alpha = lit)
        }
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
