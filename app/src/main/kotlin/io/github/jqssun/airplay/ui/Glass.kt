package io.github.jqssun.airplay.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

/**
 * Soft-glass look: a deep graphite background with faint teal glows, frosted panels, and big rounded tiles that
 * glow from the middle in one colour each (violet, mint, blue). Panels are translucent white with a bright top edge.
 */
data class GlassColors(
    val dark: Boolean,
    val fillTop: Color,
    val fillBottom: Color,
    val strokeTop: Color,
    val strokeBottom: Color,
    val divider: Color,
    val pill: Color,
    val text: Color,
    val textDim: Color,
    // the page behind everything: base gradient plus two soft glows
    val bgTop: Color,
    val bgBottom: Color,
    val glowA: Color,
    val glowB: Color,
    // the selected row or button: one soft fill, nothing else
    val focusFill: Color,
)

val DarkGlass = GlassColors(
    dark = true,
    fillTop = Color(0x2BFFFFFF),
    fillBottom = Color(0x12FFFFFF),
    strokeTop = Color(0x66FFFFFF),
    strokeBottom = Color(0x14FFFFFF),
    divider = Color(0x1FFFFFFF),
    pill = Color(0x3DFFFFFF),
    text = Color.White,
    textDim = Color(0xB3FFFFFF),
    bgTop = Color(0xFF10161C),
    bgBottom = Color(0xFF0B1015),
    glowA = Color(0xFF28444F),
    glowB = Color(0xFF22364A),
    focusFill = Color(0x33FFFFFF),
)

val LightGlass = GlassColors(
    dark = false,
    fillTop = Color(0xB3FFFFFF),
    fillBottom = Color(0x80FFFFFF),
    strokeTop = Color(0xF2FFFFFF),
    strokeBottom = Color(0x66FFFFFF),
    divider = Color(0x1F000000),
    pill = Color(0xCCFFFFFF),
    text = Color(0xFF111114),
    textDim = Color(0x99111114),
    bgTop = Color(0xFFEAEFF6),
    bgBottom = Color(0xFFDDE5EF),
    glowA = Color(0xFFC9D9F5),
    glowB = Color(0xFFD3EFE4),
    focusFill = Color(0x1F000000),
)

val LocalGlass = staticCompositionLocalOf { DarkGlass }

val GlassPanelShape = RoundedCornerShape(30.dp)
val TileShape = RoundedCornerShape(36.dp)

/** A frosted panel: translucent gradient fill, thin bright edge. */
fun Modifier.glass(shape: Shape = GlassPanelShape): Modifier = composed {
    val g = LocalGlass.current
    this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(g.fillTop, g.fillBottom)))
        .border(1.dp, Brush.verticalGradient(listOf(g.strokeTop, g.strokeBottom)), shape)
}

/** The page behind every screen: graphite, with a teal glow in the top left and a blue one in the bottom right. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier.fillMaxSize(), content: @Composable () -> Unit) {
    val g = LocalGlass.current
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(g.bgTop, g.bgBottom)))
            .drawBehind {
                val a = Offset(size.width * 0.08f, size.height * 0.05f)
                drawRect(Brush.radialGradient(listOf(g.glowA.copy(alpha = 0.85f), Color.Transparent), a, size.width * 0.55f))
                val b = Offset(size.width * 0.95f, size.height * 0.98f)
                drawRect(Brush.radialGradient(listOf(g.glowB.copy(alpha = 0.8f), Color.Transparent), b, size.width * 0.5f))
            },
    ) {
        content()
    }
}

/** The colour of one tile: the saturated middle and the pale frosted rim. */
data class TileColors(val core: Color, val rim: Color) {
    companion object {
        val Violet = TileColors(Color(0xFF5B60D0), Color(0xFFD2D3F6))
        val Mint = TileColors(Color(0xFF0FBF86), Color(0xFFA9ECD2))
        val Blue = TileColors(Color(0xFF0A68FF), Color(0xFF9EC3FF))
        // a stopped receiver: the tile loses its colour
        val Slate = TileColors(Color(0xFF4A5362), Color(0xFFB4BBC7))
    }
}

/**
 * A big rounded tile that glows from the middle: the saturated colour in the centre, a pale rim, a bright edge and
 * a soft coloured shadow underneath. White text sits on it in both themes. With [onClick] it is selectable and
 * brightens while selected; without, it only holds content (which can have focusable parts of its own).
 */
@Composable
fun GlowTile(
    colors: TileColors,
    modifier: Modifier = Modifier,
    shape: Shape = TileShape,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val lit by animateFloatAsState(if (focused) 1f else 0f, tween(140), label = "tileLit")
    val mid = lerp(colors.core, colors.rim, 0.32f)
    Box(
        modifier
            .shadow(26.dp, shape, clip = false, ambientColor = colors.core.copy(alpha = 0.5f), spotColor = colors.core.copy(alpha = 0.85f))
            .clip(shape)
            .drawBehind {
                // an ellipse, not a circle: squash a square gradient to the tile's width so wide tiles glow evenly
                val w = size.width
                val h = size.height
                withTransform({ scale(w / h, 1f, Offset(w / 2, h / 2)) }) {
                    drawRect(
                        Brush.radialGradient(
                            0f to colors.core, 0.5f to mid, 1f to colors.rim,
                            center = Offset(w / 2, h * 0.6f), radius = h * 0.95f,
                        ),
                        topLeft = Offset(w / 2 - h / 2, 0f), size = Size(h, h),
                    )
                }
                // frost: a little light from the top
                drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.22f), 0.45f to Color.Transparent))
            }
            .border(1.2.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.1f))), shape)
            .onFocusChanged { focused = it.isFocused }
            .thenIf(onClick != null) {
                clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick!!)
            }
            .drawWithContent {
                drawContent()
                if (lit > 0f) drawOutline(shape.createOutline(size, layoutDirection, this), Color.White, alpha = 0.22f * lit)
            },
        content = content,
    )
}

/** The round frosted icon at the top of a tile. */
@Composable
fun IconChip(icon: ImageVector, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 48.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(size * 0.5f), tint = Color.White)
    }
}
