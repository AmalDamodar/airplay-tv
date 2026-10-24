package io.github.jqssun.airplay.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.ui.theme.ThemeMode
import io.github.jqssun.airplay.viewmodel.MainViewModel
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import kotlin.math.roundToInt


@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val serverName by viewModel.serverName.collectAsState()
    val h265Enabled by viewModel.h265Enabled.collectAsState()
    val enforceSdr by viewModel.enforceSdr.collectAsState()
    val alacEnabled by viewModel.alacEnabled.collectAsState()
    val aacEnabled by viewModel.aacEnabled.collectAsState()
    val resolution by viewModel.resolution.collectAsState()
    val idlePreview by viewModel.idlePreview.collectAsState()
    val autoFullscreen by viewModel.autoFullscreen.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val advertiseVideo by viewModel.advertiseVideo.collectAsState()
    val advertiseAudio by viewModel.advertiseAudio.collectAsState()
    val launchOnConnect by viewModel.launchOnConnect.collectAsState()
    val maxFps by viewModel.maxFps.collectAsState()
    val overscanned by viewModel.overscanned.collectAsState()
    val requirePin by viewModel.requirePin.collectAsState()
    val allowNewConn by viewModel.allowNewConn.collectAsState()
    val autoStart by viewModel.autoStart.collectAsState()
    val bootAutoStart by viewModel.bootAutoStart.collectAsState()
    val runInBackground by viewModel.runInBackground.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val audioLatencyMs by viewModel.audioLatencyMs.collectAsState()
    val forceSwAlac by viewModel.forceSwAlac.collectAsState()
    val debugEnabled by viewModel.debugEnabled.collectAsState()
    val keyAllowFrameDrop by viewModel.keyAllowFrameDrop.collectAsState()
    val realtimeDecoderPriority by viewModel.realtimeDecoderPriority.collectAsState()
    val lowLatency by viewModel.lowLatency.collectAsState()
    val operatingRate by viewModel.operatingRate.collectAsState()
    val scheduledOutputBufferRelease by viewModel.scheduledOutputBufferRelease.collectAsState()
    val benchmarkLog by viewModel.benchmarkLog.collectAsState()
    val audioAutoBuffer by viewModel.audioAutoBuffer.collectAsState()
    val audioCushionMs by viewModel.audioCushionMs.collectAsState()
    val audioAdaptiveStep by viewModel.audioAdaptiveStep.collectAsState()
    val oboeBufferFrames by viewModel.oboeBufferFrames.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()


    var advancedOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxHeight().fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isTv()) 12.dp else 16.dp, vertical = if (isTv()) 4.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 6.dp)) {
                Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.settings_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsGroup(stringResource(R.string.section_server)) {
                SettingTextField(
                    label = stringResource(R.string.setting_server_name),
                    value = serverName,
                    onCommit = { viewModel.setServerName(it) }
                )
                SettingSwitch(
                    title = stringResource(R.string.setting_boot_auto_start),
                    description = stringResource(R.string.setting_boot_auto_start_desc),
                    checked = bootAutoStart,
                    onCheckedChange = { viewModel.setBootAutoStart(it) }
                )

                SettingSwitch(
                    title = stringResource(R.string.setting_run_in_background),
                    description = stringResource(R.string.setting_run_in_background_desc),
                    checked = runInBackground,
                    onCheckedChange = { viewModel.setRunInBackground(it) }
                )
            }
            SettingsGroup(stringResource(R.string.section_connection)) {
                SettingSwitch(
                    title = stringResource(R.string.setting_require_pin),
                    description = stringResource(R.string.setting_require_pin_desc),
                    checked = requirePin,
                    onCheckedChange = { viewModel.setRequirePin(it) }
                )

                val ctx = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                var hasOverlayPermission by remember { mutableStateOf(canAutoLaunch(ctx)) }
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) hasOverlayPermission = canAutoLaunch(ctx)
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                val needsOverlayPermission = launchOnConnect && !hasOverlayPermission
                SettingsRow(
                    modifier = Modifier
                        .dpadFocus(FocusShape, focusScale = 1f)
                        .toggleable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null,
                            value = launchOnConnect,
                            role = Role.Switch,
                            onValueChange = {
                                viewModel.setLaunchOnConnect(it)
                                if (it && !canAutoLaunch(ctx)) ctx.startActivity(_overlayIntent(ctx))
                            }
                        ),
                    headlineContent = { Text(stringResource(R.string.setting_launch_on_connect)) },
                    supportingContent = {
                        Text(stringResource(
                            if (needsOverlayPermission) R.string.setting_launch_on_connect_no_permission
                            else R.string.setting_launch_on_connect_desc
                        ))
                    },
                    trailingContent = {
                        Switch(checked = launchOnConnect, onCheckedChange = null, colors = glassSwitchColors())
                    }
                )
            }
            SettingsGroup(stringResource(R.string.section_appearance)) {
                SettingThemeMode(
                    value = themeMode,
                    onValueChange = viewModel::setThemeMode,
                )
            }
            SettingsGroup(null) {
                AdvancedHeader(open = advancedOpen, onToggle = { advancedOpen = !advancedOpen })
            }
            if (advancedOpen) {
                SettingsGroup(stringResource(R.string.section_compat), stringResource(R.string.section_compat_note)) {
                    SettingSwitch(
                        title = stringResource(R.string.setting_advertise_video),
                        description = stringResource(R.string.setting_advertise_video_desc),
                        checked = advertiseVideo,
                        onCheckedChange = { viewModel.setAdvertiseVideo(it) }
                    )
                    SettingSwitch(
                        title = stringResource(R.string.setting_advertise_audio),
                        description = stringResource(R.string.setting_advertise_audio_desc),
                        checked = advertiseAudio,
                        onCheckedChange = { viewModel.setAdvertiseAudio(it) }
                    )
                    SettingSwitch(
                        title = stringResource(R.string.setting_alac),
                        description = stringResource(R.string.setting_alac_desc),
                        checked = alacEnabled,
                        onCheckedChange = { viewModel.setAlacEnabled(it) }
                    )
                    SettingSwitch(
                        title = stringResource(R.string.setting_aac),
                        description = stringResource(R.string.setting_aac_desc),
                        checked = aacEnabled,
                        onCheckedChange = { viewModel.setAacEnabled(it) }
                    )
                }
                SettingsGroup(stringResource(R.string.section_display)) {
                SettingSwitch(
                    title = stringResource(R.string.setting_auto_fullscreen),
                    description = stringResource(R.string.setting_auto_fullscreen_desc),
                    checked = autoFullscreen,
                    onCheckedChange = { viewModel.setAutoFullscreen(it) }
                )

                SettingResolution(
                    value = resolution,
                    onValueChange = { viewModel.setResolution(it) }
                )

                SettingChipField(
                    title = stringResource(R.string.setting_max_fps),
                    description = stringResource(R.string.setting_max_fps_desc),
                    value = maxFps.toString(),
                    presets = listOf("24" to "24", "30" to "30", "60" to "60", "120" to "120"),
                    placeholder = stringResource(R.string.setting_max_fps_placeholder),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { it.toIntOrNull()?.let { v -> viewModel.setMaxFps(v) } }
                )

                SettingSwitch(
                    title = stringResource(R.string.setting_overscanned),
                    description = stringResource(R.string.setting_overscanned_desc),
                    checked = overscanned,
                    onCheckedChange = { viewModel.setOverscanned(it) }
                )
                    SettingSwitch(
                        title = stringResource(R.string.setting_keep_screen_on),
                        description = stringResource(R.string.setting_keep_screen_on_desc),
                        checked = keepScreenOn,
                        onCheckedChange = { viewModel.setKeepScreenOn(it) }
                    )

                    SettingSwitch(
                        title = stringResource(R.string.setting_idle_preview),
                        description = stringResource(R.string.setting_idle_preview_desc),
                        checked = idlePreview,
                        onCheckedChange = { viewModel.setIdlePreview(it) }
                    )
                }
                SettingsGroup(stringResource(R.string.section_network)) {
                SettingTextField(
                    label = stringResource(R.string.setting_server_port),
                    value = serverPort.toString(),
                    onCommit = { viewModel.setServerPort(it.toInt()) },
                    range = 1..65535
                )
                SettingSwitch(
                    title = stringResource(R.string.setting_allow_new_conn),
                    description = stringResource(R.string.setting_allow_new_conn_desc),
                    checked = allowNewConn,
                    onCheckedChange = { viewModel.setAllowNewConn(it) }
                )
                    SettingSwitch(
                        title = stringResource(R.string.setting_auto_start),
                        description = stringResource(R.string.setting_auto_start_desc),
                        checked = autoStart,
                        onCheckedChange = { viewModel.setAutoStart(it) }
                    )
                }
                SettingsGroup(stringResource(R.string.section_decode)) {
                SettingSwitch(
                    title = stringResource(R.string.setting_h265),
                    description = stringResource(R.string.setting_h265_desc),
                    checked = h265Enabled,
                    onCheckedChange = { viewModel.setH265Enabled(it) }
                )

                SettingSwitch(
                    title = stringResource(R.string.setting_sw_alac),
                    description = stringResource(R.string.setting_sw_alac_desc),
                    checked = forceSwAlac,
                    onCheckedChange = { viewModel.setForceSwAlac(it) }
                )
                    SettingSwitch(
                        title = stringResource(R.string.setting_key_allow_frame_drop),
                        description = stringResource(R.string.setting_key_allow_frame_drop_desc),
                        checked = keyAllowFrameDrop,
                        onCheckedChange = { viewModel.setKeyAllowFrameDrop(it) }
                    )

                    SettingSwitch(
                        title = stringResource(R.string.setting_enforce_sdr),
                        description = stringResource(R.string.setting_enforce_sdr_desc),
                        checked = enforceSdr,
                        onCheckedChange = { viewModel.setEnforceSdr(it) }
                    )

                    SettingSwitch(
                        title = stringResource(R.string.setting_realtime_decoder_priority),
                        description = stringResource(R.string.setting_realtime_decoder_priority_desc),
                        checked = realtimeDecoderPriority,
                        onCheckedChange = { viewModel.setRealtimeDecoderPriority(it) }
                    )

                    SettingChips(
                        title = stringResource(R.string.setting_operating_rate),
                        description = stringResource(R.string.setting_operating_rate_desc),
                        value = operatingRate,
                        options = listOf(
                            Prefs.AUTO to stringResource(R.string.chip_auto),
                            Prefs.ON to stringResource(R.string.chip_on),
                            Prefs.OFF to stringResource(R.string.chip_off),
                        ),
                        onValueChange = { viewModel.setOperatingRate(it) }
                    )

                    SettingSwitch(
                        title = stringResource(R.string.setting_low_latency),
                        description = stringResource(R.string.setting_low_latency_desc),
                        checked = lowLatency,
                        onCheckedChange = { viewModel.setLowLatency(it) }
                    )

                    SettingSwitch(
                        title = stringResource(R.string.setting_scheduled_output_buffer_release),
                        description = stringResource(R.string.setting_scheduled_output_buffer_release_desc),
                        checked = scheduledOutputBufferRelease,
                        onCheckedChange = { viewModel.setScheduledOutputBufferRelease(it) }
                    )
                }
                SettingsGroup(stringResource(R.string.section_audio_sync)) {
                    SettingSwitch(
                        title = stringResource(R.string.setting_audio_delay),
                        description = stringResource(R.string.setting_audio_delay_desc),
                        checked = audioLatencyMs >= 0,
                        onCheckedChange = { viewModel.setAudioLatencyMs(if (it) 250 else -1) }
                    )

                    if (audioLatencyMs >= 0) {
                        var sliderVal by remember(audioLatencyMs) { mutableFloatStateOf(audioLatencyMs.toFloat()) }
                        SettingsRow(
                            headlineContent = {
                                Slider(
                                    value = sliderVal,
                                    onValueChange = { sliderVal = it },
                                    onValueChangeFinished = { viewModel.setAudioLatencyMs(sliderVal.roundToInt()) },
                                    valueRange = 0f..1000f,
                                    steps = 19,
                                    modifier = Modifier.heightIn(min = 48.dp).dpadFocus(FocusShape, focusScale = 1f).dpadAdjust(
                                        onLeft = { viewModel.setAudioLatencyMs((audioLatencyMs - 50).coerceIn(0, 1000)) },
                                        onRight = { viewModel.setAudioLatencyMs((audioLatencyMs + 50).coerceIn(0, 1000)) }
                                    )
                                )
                            },
                            trailingContent = { Text(stringResource(R.string.audio_delay_value, sliderVal.roundToInt())) }
                        )
                    }

                    SettingSwitch(
                        title = stringResource(R.string.setting_audio_auto_buffer),
                        description = stringResource(R.string.setting_audio_auto_buffer_desc),
                        checked = audioAutoBuffer,
                        onCheckedChange = { viewModel.setAudioAutoBuffer(it) }
                    )

                    if (audioAutoBuffer) {
                        val maxStep = Prefs.ADAPTIVE_PERCENTILES.size - 1
                        var stepVal by remember(audioAdaptiveStep) { mutableFloatStateOf(audioAdaptiveStep.toFloat()) }
                        SettingsRow(
                            headlineContent = {
                                Slider(
                                    value = stepVal,
                                    onValueChange = { stepVal = it },
                                    onValueChangeFinished = { viewModel.setAudioAdaptiveStep(stepVal.roundToInt()) },
                                    valueRange = 0f..maxStep.toFloat(),
                                    steps = maxStep - 1,
                                    modifier = Modifier.heightIn(min = 48.dp).dpadFocus(FocusShape, focusScale = 1f).dpadAdjust(
                                        onLeft = { viewModel.setAudioAdaptiveStep((audioAdaptiveStep - 1).coerceIn(0, maxStep)) },
                                        onRight = { viewModel.setAudioAdaptiveStep((audioAdaptiveStep + 1).coerceIn(0, maxStep)) }
                                    )
                                )
                            },
                            trailingContent = {
                                val step = stepVal.roundToInt().coerceIn(0, maxStep)
                                Text(stringResource(R.string.audio_adaptive_value,
                                    Prefs.ADAPTIVE_PERCENTILES[step],
                                    stringArrayResource(R.array.audio_adaptive_step_names)[step]))
                            }
                        )
                    } else {
                        SettingTextField(
                            label = stringResource(R.string.setting_audio_cushion_ms),
                            value = audioCushionMs.toString(),
                            onCommit = { viewModel.setAudioCushionMs(it.toInt()) },
                            description = stringResource(R.string.setting_audio_cushion_ms_desc),
                            range = 1..1000
                        )
                    }

                    SettingTextField(
                        label = stringResource(R.string.setting_oboe_buffer_frames),
                        value = oboeBufferFrames.toString(),
                        onCommit = { viewModel.setOboeBufferFrames(it.toInt()) },
                        description = stringResource(R.string.setting_oboe_buffer_frames_desc),
                        range = 0..8192
                    )
                }
                SettingsGroup(stringResource(R.string.section_diagnostics)) {
                    SettingSwitch(
                        title = stringResource(R.string.setting_debug_overlay),
                        description = stringResource(R.string.setting_debug_overlay_desc),
                        checked = debugEnabled,
                        onCheckedChange = { viewModel.setDebugEnabled(it) }
                    )

                    SettingSwitch(
                        title = stringResource(R.string.setting_benchmark_log),
                        description = stringResource(R.string.setting_benchmark_log_desc),
                        checked = benchmarkLog,
                        onCheckedChange = { viewModel.setBenchmarkLog(it) }
                    )
                }
            }
        }
    }
}


private fun canAutoLaunch(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(ctx)

private fun _overlayIntent(ctx: Context): Intent =
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)


@Composable
private fun SettingsGroup(
    title: String?,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp),
            )
        }
        Column(
            Modifier.fillMaxWidth().glass().padding(vertical = 6.dp)
                // the last row's divider would sit on the panel edge: cut the bottom 2 dp off
                .drawWithContent { clipRect(bottom = size.height - 2.dp.toPx()) { this@drawWithContent.drawContent() } },
            content = content,
        )
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 22.dp),
            )
        }
    }
}

/** One row of a group: text on the left, control on the right, hairline divider underneath. */
@Composable
private fun SettingsRow(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val divider = LocalGlass.current.divider
    // Inset by 8 dp so the focus ring is never cut off by the rounded panel.
    Box(
        Modifier.padding(horizontal = 8.dp).drawBehind {
            drawLine(divider, Offset(20.dp.toPx(), size.height), Offset(size.width - 20.dp.toPx(), size.height), 1.dp.toPx())
        }
    ) {
        ListItem(
            headlineContent = headlineContent,
            supportingContent = supportingContent,
            trailingContent = trailingContent,
            modifier = Modifier.padding(vertical = 2.dp).then(modifier).heightIn(min = if (isTv()) 84.dp else 68.dp),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun AdvancedHeader(open: Boolean, onToggle: () -> Unit) {
    SettingsRow(
        modifier = Modifier.dpadFocus(FocusShape, focusScale = 1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle),
        headlineContent = { Text(stringResource(R.string.section_advanced)) },
        supportingContent = { Text(stringResource(R.string.section_advanced_desc)) },
        trailingContent = {
            Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
        },
    )
}

private val FocusShape = RoundedCornerShape(20.dp)

@Composable
private fun glassSwitchColors(): SwitchColors {
    val g = LocalGlass.current
    val onTrack = g.text
    val onThumb = if (g.dark) Color(0xFF111114) else Color.White
    val offTrack = if (g.dark) Color(0x33FFFFFF) else Color(0x33000000)
    val offThumb = if (g.dark) Color(0xE6FFFFFF) else Color.White
    return SwitchDefaults.colors(
        checkedThumbColor = onThumb, checkedTrackColor = onTrack, checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = offThumb, uncheckedTrackColor = offTrack, uncheckedBorderColor = Color.Transparent,
    )
}

@Composable
private fun glassChipColors(): SelectableChipColors {
    val g = LocalGlass.current
    return FilterChipDefaults.filterChipColors(
        containerColor = Color.Transparent,
        labelColor = g.text,
        selectedContainerColor = g.pill,
        selectedLabelColor = g.text,
    )
}

@Composable
private fun glassFieldColors(): TextFieldColors {
    val g = LocalGlass.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = g.text, unfocusedTextColor = g.text,
        focusedBorderColor = g.text, unfocusedBorderColor = g.textDim.copy(alpha = 0.45f),
        focusedLabelColor = g.text, unfocusedLabelColor = g.textDim,
        cursorColor = g.text,
        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        focusedSupportingTextColor = g.textDim, unfocusedSupportingTextColor = g.textDim,
    )
}

/** Apple-style segmented control: one pill track, the chosen option raised. */
@Composable
private fun <T> GlassSegmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier.clip(CircleShape).background(g.divider).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val chosen = value == selected
            Box(
                Modifier
                    .dpadFocus(CircleShape, focusScale = 1f)
                    .clip(CircleShape)
                    .background(if (chosen) g.pill else Color.Transparent)
                    .selectable(
                        selected = chosen, role = Role.RadioButton, onClick = { onSelect(value) },
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                    )
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = g.text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SettingThemeMode(
    value: ThemeMode,
    onValueChange: (ThemeMode) -> Unit,
) {
    val choices = listOf(
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark),
    )
    SettingsRow(
        headlineContent = { Text(stringResource(R.string.setting_theme)) },
        trailingContent = { GlassSegmented(choices, value, onValueChange) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingResolution(
    value: String,
    onValueChange: (String) -> Unit
) {
    val presets = listOf(
        Prefs.AUTO to stringResource(R.string.chip_auto),
        "1280x720" to "1280x720",
        "1920x1080" to "1920x1080",
        "3840x2160" to "3840x2160"
    )
    val devicePresets = listOf(
        "portrait" to stringResource(R.string.chip_device_portrait),
        "landscape" to stringResource(R.string.chip_device_landscape)
    )
    val isPreset = (presets + devicePresets).any { it.first == value }
    var editing by remember { mutableStateOf(false) }
    val parts = if (!isPreset && value.contains("x")) value.split("x", limit = 2) else listOf("", "")
    var width by remember(value) { mutableStateOf(if (isPreset) "" else parts[0]) }
    var height by remember(value) { mutableStateOf(if (isPreset) "" else parts.getOrElse(1) { "" }) }

    SettingsRow(
        headlineContent = { Text(stringResource(R.string.setting_resolution)) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.setting_resolution_desc))
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    @Composable
                    fun presetChip(key: String, label: String) = FilterChip(
 colors = glassChipColors(),
                        selected = value == key && !editing,
                        onClick = {
                            editing = false
                            width = ""; height = ""
                            onValueChange(key)
                        },
                        label = { Text(label) },
                        modifier = Modifier.heightIn(min = 48.dp).dpadFocus(FocusShape, focusScale = 1f)
                    )
                    presets.forEach { (key, label) -> presetChip(key, label) }
                    FilterChip(
 colors = glassChipColors(),
                        selected = !isPreset || editing,
                        onClick = { editing = true },
                        label = { Text(stringResource(R.string.chip_custom)) },
                        modifier = Modifier.heightIn(min = 48.dp).dpadFocus(FocusShape, focusScale = 1f)
                    )
                    devicePresets.forEach { (key, label) -> presetChip(key, label) }
                }
                if (editing || !isPreset) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
 colors = glassFieldColors(),
                            value = width,
                            onValueChange = { width = it.filter { c -> c.isDigit() }.take(5) },
                            singleLine = true,
                            label = { Text(stringResource(R.string.setting_resolution_width)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1f).dpadFocus(FocusShape, focusScale = 1f)
                        )
                        OutlinedTextField(
 colors = glassFieldColors(),
                            value = height,
                            onValueChange = { height = it.filter { c -> c.isDigit() }.take(5) },
                            singleLine = true,
                            label = { Text(stringResource(R.string.setting_resolution_height)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = doneAndHide {
                                val w = width.toIntOrNull()
                                val h = height.toIntOrNull()
                                if (w != null && w > 0 && h != null && h > 0) {
                                    onValueChange("${w}x${h}")
                                    editing = false
                                }
                            },
                            modifier = Modifier.weight(1f).dpadFocus(FocusShape, focusScale = 1f)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun doneAndHide(commit: () -> Unit): KeyboardActions {
    val keyboard = LocalSoftwareKeyboardController.current
    return KeyboardActions(onDone = { commit(); keyboard?.hide() })
}

/**
 * A row showing the current value. OK opens a small edit dialog, so a text box never sits in the list
 * and takes the D-pad keys away from scrolling.
 */
@Composable
private fun SettingTextField(
    label: String,
    value: String,
    onCommit: (String) -> Unit,
    description: String? = null,
    range: IntRange? = null
) {
    var open by remember { mutableStateOf(false) }
    SettingsRow(
        modifier = Modifier
            .dpadFocus(FocusShape, focusScale = 1f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = true },
        headlineContent = { Text(label) },
        supportingContent = description?.let { { Text(it) } },
        trailingContent = {
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
    if (open) {
        TextEditDialog(
            label = label,
            initial = value,
            range = range,
            onDismiss = { open = false },
            onConfirm = onCommit,
        )
    }
}

@Composable
private fun TextEditDialog(
    label: String,
    initial: String,
    range: IntRange?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val valid = if (range == null) text.isNotBlank() else text.toIntOrNull()?.let { it in range } == true
    val requester = remember { FocusRequester() }
    fun confirm() {
        if (!valid) return
        if (text != initial) onConfirm(text)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(label) },
        text = {
            // the dialog is its own window with its own focus manager, so ask for it here
            val focus = LocalFocusManager.current
            OutlinedTextField(
                colors = glassFieldColors(),
                value = text,
                onValueChange = { text = if (range == null) it else it.filter { c -> c.isDigit() }.take(6) },
                singleLine = true,
                isError = !valid,
                supportingText = range?.let { { Text("${it.first} - ${it.last}") } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (range == null) KeyboardType.Text else KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(requester)
                    // Down leaves the box for the buttons instead of moving the cursor
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown) {
                            focus.moveFocus(FocusDirection.Down); true
                        } else false
                    },
            )
        },
        confirmButton = {
            TextButton(onClick = ::confirm, enabled = valid, modifier = Modifier.dpadFocus(CircleShape)) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.dpadFocus(CircleShape)) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
    LaunchedEffect(Unit) { requester.requestFocusUntilLanded() }
}

@Composable
private fun SettingChipField(
    title: String,
    description: String,
    value: String,
    presets: List<Pair<String, String>>,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    val isPreset = presets.any { it.first == value }
    var editing by remember { mutableStateOf(false) }
    var text by remember(value) { mutableStateOf(if (isPreset) "" else value) }

    SettingsRow(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text(description)
                Spacer(Modifier.height(8.dp))
                ChipRow(if (editing) "" else value, presets, { editing = false; text = ""; onValueChange(it) }) {
                    FilterChip(
 colors = glassChipColors(),
                        selected = !isPreset || editing,
                        onClick = { editing = true },
                        label = { Text(stringResource(R.string.chip_custom)) },
                        modifier = Modifier.heightIn(min = 48.dp).dpadFocus(FocusShape, focusScale = 1f)
                    )
                }
                if (editing || !isPreset) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
 colors = glassFieldColors(),
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        label = { Text(placeholder) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = doneAndHide {
                            if (text.isNotBlank()) {
                                onValueChange(text)
                                editing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().dpadFocus(FocusShape, focusScale = 1f)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            FilterChip(
 colors = glassChipColors(),
                selected = value == key,
                onClick = { onSelect(key) },
                label = { Text(label) },
                modifier = Modifier.heightIn(min = 48.dp).dpadFocus(FocusShape, focusScale = 1f)
            )
        }
        trailing()
    }
}

@Composable
private fun SettingChips(
    title: String,
    description: String,
    value: String,
    options: List<Pair<String, String>>,
    onValueChange: (String) -> Unit
) {
    SettingsRow(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text(description)
                Spacer(Modifier.height(8.dp))
                ChipRow(value, options, onValueChange)
            }
        }
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(
        modifier = Modifier
            .dpadFocus(FocusShape, focusScale = 1f)
            .toggleable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                value = checked, role = Role.Switch, onValueChange = onCheckedChange,
            ),
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, colors = glassSwitchColors()) }
    )
}
