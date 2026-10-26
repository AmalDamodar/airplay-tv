package io.github.jqssun.airplay.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.service.AirPlayService.ServerState

private val StatusGreen = Color(0xFF30D371)
private val StatusYellow = Color(0xFFFFC21F)
private val StatusRed = Color(0xFFFF5A52)

/**
 * Status chip: a dark frosted pill with a glowing dot, green when the receiver is ready or streaming, yellow
 * (breathing) while a device is connecting, red when stopped or in error. Meant to sit on a coloured tile.
 */
@Composable
internal fun ReceiverStatus(state: ServerState, connections: Int = 0, connecting: Boolean = false) {
    val waiting = state == ServerState.RUNNING && connections > 0 && connecting
    val color = when {
        state == ServerState.ERROR || state == ServerState.STOPPED -> StatusRed
        waiting -> StatusYellow
        else -> StatusGreen
    }
    Row(
        Modifier.background(Color.Black.copy(alpha = 0.28f), CircleShape)
            .padding(start = 8.dp, end = 14.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GlowDot(color, breathing = waiting)
        Text(
            when {
                state == ServerState.ERROR -> stringResource(R.string.error_label)
                state == ServerState.STOPPED -> stringResource(R.string.stopped_label)
                waiting -> stringResource(R.string.connecting_label)
                connections > 0 -> stringResource(R.string.connected_count, connections)
                else -> stringResource(R.string.ready_label)
            },
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun GlowDot(color: Color, breathing: Boolean) {
    val transition = rememberInfiniteTransition(label = "statusDot")
    val breath by transition.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "statusBreath",
    )
    val level = if (breathing) breath else 1f
    Box(
        Modifier.size(26.dp).drawBehind {
            val r = size.minDimension / 2
            drawCircle(
                Brush.radialGradient(
                    0f to color.copy(alpha = 0.75f * level), 0.4f to color.copy(alpha = 0.32f * level),
                    1f to Color.Transparent, radius = r,
                ),
                radius = r,
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(10.dp).background(color.copy(alpha = 0.55f + 0.45f * level), CircleShape))
    }
}

/** The small "Start" / "Stop" label on the tile. Only a label: the whole tile is the button. */
@Composable
internal fun ActionPill(running: Boolean) {
    Row(
        Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape).padding(start = 10.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(if (running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null, Modifier.size(20.dp), tint = Color.White)
        Text(
            stringResource(if (running) R.string.btn_stop else R.string.btn_start),
            style = MaterialTheme.typography.labelLarge, color = Color.White,
        )
    }
}

/**
 * The first tile on Home: what the receiver is doing and how to reach it. Violet while it runs, grey when stopped.
 * Selecting it starts or stops the receiver.
 */
@Composable
internal fun MirrorTile(
    name: String,
    state: ServerState,
    connections: Int,
    connecting: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val running = state == ServerState.RUNNING
    GlowTile(if (running) TileColors.Violet else TileColors.Slate, modifier, onClick = onToggle) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconChip(Icons.Rounded.Cast)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ReceiverStatus(state, connections, connecting)
                    ActionPill(running)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    name, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(when (state) {
                        ServerState.RUNNING -> R.string.receiver_cast_help
                        ServerState.STOPPED -> R.string.server_stopped
                        ServerState.ERROR -> R.string.error_starting_server
                    }, name),
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
