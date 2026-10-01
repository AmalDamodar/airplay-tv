package io.github.jqssun.airplay.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.service.AirPlayService.ServerState

@Composable
internal fun ReceiverStatus(state: ServerState, connections: Int = 0) {
    val color = when (state) {
        ServerState.RUNNING -> MaterialTheme.colorScheme.tertiary
        ServerState.ERROR -> MaterialTheme.colorScheme.error
        ServerState.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.1f), shape = CircleShape) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(7.dp).background(color, CircleShape))
            Text(
                when (state) {
                    ServerState.RUNNING -> if (connections > 0) stringResource(R.string.connected_count, connections)
                        else stringResource(R.string.ready_label)
                    ServerState.ERROR -> stringResource(R.string.error_label)
                    ServerState.STOPPED -> stringResource(R.string.stopped_label)
                },
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
    }
}

@Composable
internal fun ReceiverHero(name: String, state: ServerState) {
    val tv = isTv()
    // Scroll only on compact/tall-text layouts; no focusable decoration competes with Start.
    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(if (tv) 24.dp else 20.dp)
    ) {
        val wide = maxWidth >= 540.dp
        val glyph: @Composable () -> Unit = {
            ReceiverGlyph(running = state == ServerState.RUNNING, modifier = Modifier.size(if (wide) 148.dp else 88.dp))
        }
        val details: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReceiverStatus(state)
                Text(
                    name,
                    style = if (tv) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(when (state) {
                        ServerState.RUNNING -> R.string.receiver_ready_title
                        ServerState.STOPPED -> R.string.server_stopped
                        ServerState.ERROR -> R.string.error_starting_server
                    }),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.receiver_cast_help, name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.receiver_network_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (wide) {
            Row(
                Modifier.align(Alignment.Center).verticalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                glyph()
                Box(Modifier.weight(1f)) { details() }
            }
        } else {
            Column(
                Modifier.align(Alignment.Center).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                glyph()
                details()
            }
        }
    }
}

@Composable
private fun ReceiverGlyph(running: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
        // Only composed while running and idle. Works on API 30 without blur/render effects.
        if (running) {
            val transition = rememberInfiniteTransition(label = "receiverPulse")
            val pulse by transition.animateFloat(
                initialValue = 0.88f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
                label = "receiverRing",
            )
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = 0.35f }
                    .border(1.dp, primary, CircleShape)
            )
        }
        Box(
            Modifier.fillMaxSize(0.75f).background(primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Cast, null, Modifier.fillMaxSize(0.55f), tint = primary)
        }
    }
}
