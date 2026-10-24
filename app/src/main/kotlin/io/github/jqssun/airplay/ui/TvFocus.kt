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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

inline fun Modifier.thenIf(condition: Boolean, block: Modifier.() -> Modifier): Modifier =
    if (condition) block() else this

/**
 * Focus look that belongs to the glass theme: the element lights up like a lit pane of glass (brighter translucent
 * fill, a thin specular edge, a faint soft glow) instead of getting a hard outline. The glow stays within a few dp of
 * the element so panels never clip it.
 */
@Composable
fun Modifier.focusRing(
    shape: Shape = CircleShape,
    focusScale: Float = 1.035f,
): Modifier {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    val lit by animateFloatAsState(if (focused) 1f else 0f, tween(180), label = "focusLit")
    val scale by animateFloatAsState(if (focused) focusScale else 1f, tween(180), label = "focusScale")
    return this
        .onFocusChanged { focused = it.hasFocus }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .drawWithContent {
            val outline = shape.createOutline(size, layoutDirection, this)
            if (lit > 0f) {
                val glow = 4.dp.toPx()
                drawOutline(outline, g.focusGlow, alpha = g.focusGlowAlpha * 0.5f * lit, style = Stroke(width = glow * 2.5f))
                drawOutline(outline, g.focusGlow, alpha = g.focusGlowAlpha * lit, style = Stroke(width = glow))
                drawOutline(outline, Brush.verticalGradient(listOf(g.focusFillTop, g.focusFillBottom)), alpha = lit)
            }
            drawContent()
            if (lit > 0f) {
                drawOutline(outline, g.focusHairline, alpha = lit, style = Stroke(width = 1.dp.toPx()))
                drawOutline(
                    outline, Brush.verticalGradient(listOf(g.focusEdgeTop, g.focusEdgeBottom)),
                    alpha = lit, style = Stroke(width = 1.5.dp.toPx()),
                )
            }
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
