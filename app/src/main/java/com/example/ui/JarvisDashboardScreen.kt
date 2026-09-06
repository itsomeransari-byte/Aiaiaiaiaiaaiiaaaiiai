package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.engine.JarvisCommandEngine
import com.example.engine.JarvisCommandResult
import com.example.engine.JarvisSpeaker
import com.example.engine.JarvisVoiceRecognizer
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisBackgroundService
import com.example.ui.theme.BentoAlertContainer
import com.example.ui.theme.BentoAlertRed
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.BentoBadgeGray
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCardSurface
import com.example.ui.theme.BentoGreenActive
import com.example.ui.theme.BentoLavenderPrimary
import com.example.ui.theme.BentoNavSurface
import com.example.ui.theme.BentoPurpleDeep
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary

@Composable
fun JarvisDashboardScreen(
    speaker: JarvisSpeaker,
    commandEngine: JarvisCommandEngine,
    voiceRecognizer: JarvisVoiceRecognizer?,
    onRestartVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // State flows
    val isAccessibilityActive by JarvisAccessibilityService.isServiceActive.collectAsState()
    val isBackgroundActive by JarvisBackgroundService.isBackgroundActive.collectAsState()
    val isSpeaking by speaker.isSpeaking.collectAsState()
    val lastSpokenText by speaker.lastSpeech.collectAsState()
    val serviceLogs by JarvisAccessibilityService.serviceLogs.collectAsState()

    val isListening by (voiceRecognizer?.isListening?.collectAsState() ?: remember { mutableStateOf(false) })
    val audioRms by (voiceRecognizer?.audioRms?.collectAsState() ?: remember { mutableFloatStateOf(0f) })
    val lastRecognizedText by (voiceRecognizer?.lastRecognizedText?.collectAsState() ?: remember { mutableStateOf("") })

    var customCommandText by remember { mutableStateOf("") }
    var voicePitch by remember { mutableFloatStateOf(0.95f) }
    var voiceSpeed by remember { mutableFloatStateOf(1.05f) }
    var showVoiceSettings by remember { mutableStateOf(false) }
    var showLogsExpanded by remember { mutableStateOf(false) }

    // Permission launcher for audio
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onRestartVoice()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val logListState = rememberLazyListState()
    LaunchedEffect(serviceLogs.size) {
        if (serviceLogs.isNotEmpty()) {
            logListState.animateScrollToItem(serviceLogs.size - 1)
        }
    }

    fun executeTextCommand(command: String) {
        if (command.isBlank()) return
        focusManager.clearFocus()
        customCommandText = ""
        val result = commandEngine.processCommand(command)
        when (result) {
            is JarvisCommandResult.Success -> {
                speaker.speak(result.speechResponse)
            }
            is JarvisCommandResult.Error -> {
                speaker.speak(result.speechResponse)
            }
        }
    }

    Surface(
        color = BentoBackground,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Bento Header (Matching HTML header layout)
            JarvisTelemetryHeader(
                onSettingsClick = { showVoiceSettings = !showVoiceSettings }
            )

            // Main Bento Grid Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Center Hero Arc Reactor with Bento Glow & Italic Speech Subtitle
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        JarvisArcReactor(
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            audioRms = audioRms,
                            onReactorClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    if (isListening) {
                                        voiceRecognizer?.stopListening()
                                    } else {
                                        voiceRecognizer?.startListening()
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Audio Frequency Waveform Visualizer
                        JarvisAudioVisualizer(
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            audioRms = audioRms
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Italic Prompt / Command Subtitle (Matching HTML: "Jarvis, scroll down and click...")
                        val displaySubtitle = when {
                            lastRecognizedText.isNotBlank() -> "\"$lastRecognizedText\""
                            lastSpokenText.isNotBlank() -> "\"$lastSpokenText\""
                            else -> "\"Jarvis, slowly swipe right and click the 2nd video\""
                        }
                        Text(
                            text = displaySubtitle,
                            color = BentoTextPrimary,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        )
                    }
                }

                // 2. Bento Grid (grid grid-cols-2 gap-3)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bento Card 1: Voice Service (col-span-1 bg-[#2B2930] p-4 rounded-3xl border border-[#49454F])
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
                            border = BorderStroke(1.dp, BentoBorder),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (isListening) voiceRecognizer?.stopListening() else voiceRecognizer?.startListening()
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top icon container (bg-[#D0BCFF]/10 w-8 h-8 rounded-lg)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BentoLavenderPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                        contentDescription = "Voice Service",
                                        tint = BentoLavenderPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Column {
                                    Text(
                                        text = "VOICE SERVICE",
                                        color = BentoTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                    Text(
                                        text = if (isListening) "Listening" else "Online",
                                        color = BentoTextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Bento Card 2: Accessibility Core (col-span-1 bg-[#381E72] p-4 rounded-3xl)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAccessibilityActive) BentoPurpleDeep else BentoAlertContainer
                            ),
                            border = BorderStroke(1.dp, if (isAccessibilityActive) BentoBorder else BentoAlertRed),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (!isAccessibilityActive) {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                                .testTag("enable_accessibility_button")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top icon container (bg-white/10 w-8 h-8 rounded-lg)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isAccessibilityActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = "Accessibility",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Column {
                                    Text(
                                        text = "ACCESSIBILITY",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                    Text(
                                        text = if (isAccessibilityActive) "Authorized" else "Enable",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Bento Card 3: Background Automation Hands-Free (col-span-2 bg-[#2B2930] p-4 rounded-3xl border border-[#49454F])
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
                        border = BorderStroke(1.dp, BentoBorder),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(BentoLavenderPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Smartphone,
                                            contentDescription = "Background service",
                                            tint = BentoLavenderPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "BACKGROUND AUTOMATION",
                                            color = BentoTextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = if (isBackgroundActive) "Active when minimized" else "Enable hands-free background control",
                                            color = BentoTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Switch(
                                    checked = isBackgroundActive,
                                    onCheckedChange = { enable ->
                                        if (enable) {
                                            JarvisBackgroundService.startService(context)
                                        } else {
                                            JarvisBackgroundService.stopService(context)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BentoPurpleDeep,
                                        checkedTrackColor = BentoLavenderPrimary,
                                        uncheckedThumbColor = BentoTextSecondary,
                                        uncheckedTrackColor = BentoBadgeGray
                                    ),
                                    modifier = Modifier.testTag("background_automation_switch")
                                )
                            }

                            // Overlay Permission prompt if required
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BentoBadgeGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Floating HUD Overlay permission required",
                                        color = BentoLavenderPrimary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            ).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BentoLavenderPrimary,
                                            contentColor = BentoPurpleDeep
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("GRANT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Bento Card 4: Recent Task / Logs Card (col-span-2 bg-[#2B2930] p-4 rounded-3xl border border-[#49454F])
                item {
                    val lastTaskLog = serviceLogs.lastOrNull() ?: "Ready. Awaiting voice gesture commands."
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
                        border = BorderStroke(1.dp, BentoBorder),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLogsExpanded = !showLogsExpanded }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "RECENT TASK",
                                        color = BentoTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = lastTaskLog,
                                        color = BentoTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = if (showLogsExpanded) Int.MAX_VALUE else 2
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // w-10 h-10 rounded-full bg-[#4A4458] flex items-center justify-center
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(BentoBadgeGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Expand logs",
                                        tint = BentoLavenderPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Expanded Terminal Logs
                            AnimatedVisibility(visible = showLogsExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 80.dp, max = 160.dp)
                                            .background(BentoBackground, RoundedCornerShape(12.dp))
                                            .border(1.dp, BentoBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        LazyColumn(
                                            state = logListState,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(serviceLogs) { log ->
                                                Text(
                                                    text = log,
                                                    color = if (log.contains("Gesture") || log.contains("Dispatched") || log.contains("Clicked")) BentoGreenActive else BentoTextSecondary,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Command Protocol Deck (Quick Action Chips in Bento Styling)
                item {
                    Text(
                        text = "COMMAND PROTOCOL DECK",
                        color = BentoTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            QuickActionChip(
                                label = "Slowly Swipe Right",
                                icon = Icons.Default.ArrowForward,
                                testTag = "slow_swipe_right_chip",
                                onClick = { executeTextCommand("slowly swipe right") }
                            )
                        }
                        item {
                            QuickActionChip(
                                label = "Click 2nd Video",
                                icon = Icons.Default.PlayArrow,
                                testTag = "click_second_video_chip",
                                onClick = { executeTextCommand("click the 2 video") }
                            )
                        }
                        item {
                            QuickActionChip(
                                label = "Slowly Swipe Left",
                                icon = Icons.Default.ArrowBack,
                                testTag = "slow_swipe_left_chip",
                                onClick = { executeTextCommand("slowly swipe left") }
                            )
                        }
                        item {
                            QuickActionChip(
                                label = "Scroll Down",
                                icon = Icons.Default.ArrowDownward,
                                testTag = "scroll_down_chip",
                                onClick = { executeTextCommand("scroll down") }
                            )
                        }
                        item {
                            QuickActionChip(
                                label = "Scroll Up",
                                icon = Icons.Default.ArrowUpward,
                                testTag = "scroll_up_chip",
                                onClick = { executeTextCommand("scroll up") }
                            )
                        }
                        item {
                            QuickActionChip(
                                label = "Open YouTube",
                                icon = Icons.Default.PlayArrow,
                                testTag = "open_youtube_chip",
                                onClick = { executeTextCommand("open youtube") }
                            )
                        }
                        item {
                            QuickActionChip(
                                label = "Go Home",
                                icon = Icons.Default.Home,
                                testTag = "go_home_chip",
                                onClick = { executeTextCommand("go home") }
                            )
                        }
                        item {
                            QuickActionChip(
                                label = "Lock Screen",
                                icon = Icons.Default.Lock,
                                testTag = "lock_screen_chip",
                                onClick = { executeTextCommand("lock mobile") }
                            )
                        }
                    }
                }

                // 6. Text Command Input Bar (Bento styled)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customCommandText,
                            onValueChange = { customCommandText = it },
                            placeholder = {
                                Text(
                                    "Type command (e.g. slowly swipe right)...",
                                    color = BentoTextSecondary.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoLavenderPrimary,
                                unfocusedBorderColor = BentoBorder,
                                focusedTextColor = BentoTextPrimary,
                                unfocusedTextColor = BentoTextPrimary,
                                cursorColor = BentoLavenderPrimary,
                                focusedContainerColor = BentoCardSurface,
                                unfocusedContainerColor = BentoCardSurface
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                executeTextCommand(customCommandText)
                            }),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("command_input_field")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (customCommandText.isNotBlank()) {
                                    executeTextCommand(customCommandText)
                                } else {
                                    if (isListening) voiceRecognizer?.stopListening() else voiceRecognizer?.startListening()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(BentoLavenderPrimary)
                                .testTag("submit_or_mic_button")
                        ) {
                            Icon(
                                imageVector = if (customCommandText.isNotBlank()) Icons.Default.Send else if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Execute Command",
                                tint = BentoPurpleDeep
                            )
                        }
                    }
                }

                // 7. Voice Tuning Settings Drawer
                item {
                    AnimatedVisibility(visible = showVoiceSettings) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BentoCardSurface),
                            border = BorderStroke(1.dp, BentoBorder),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "VOICE AUDIO SYNTHESIS CALIBRATION",
                                    color = BentoTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Pitch: ${(voicePitch * 100).toInt()}%",
                                    color = BentoTextPrimary,
                                    fontSize = 12.sp
                                )
                                Slider(
                                    value = voicePitch,
                                    onValueChange = {
                                        voicePitch = it
                                        speaker.setVoiceParameters(voicePitch, voiceSpeed)
                                    },
                                    valueRange = 0.6f..1.4f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = BentoLavenderPrimary,
                                        activeTrackColor = BentoLavenderPrimary,
                                        inactiveTrackColor = BentoBadgeGray
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Speed: ${(voiceSpeed * 100).toInt()}%",
                                    color = BentoTextPrimary,
                                    fontSize = 12.sp
                                )
                                Slider(
                                    value = voiceSpeed,
                                    onValueChange = {
                                        voiceSpeed = it
                                        speaker.setVoiceParameters(voicePitch, voiceSpeed)
                                    },
                                    valueRange = 0.7f..1.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = BentoLavenderPrimary,
                                        activeTrackColor = BentoLavenderPrimary,
                                        inactiveTrackColor = BentoBadgeGray
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        speaker.speak("At your service, sir. Jarvis audio vocalization calibrated.")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BentoPurpleDeep,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("TEST VOCAL CORD AUDIO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 8. Bento Navigation Bar (bg-[#211F26] border-t border-[#49454F] h-20 px-6 flex items-center justify-around)
            BentoBottomNav(
                isListening = isListening,
                onMicClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        if (isListening) voiceRecognizer?.stopListening() else voiceRecognizer?.startListening()
                    }
                },
                onHistoryClick = { showLogsExpanded = !showLogsExpanded },
                onSettingsClick = { showVoiceSettings = !showVoiceSettings }
            )
        }
    }
}

/**
 * Bento Grid Bottom Navigation Bar
 * Matching HTML: bg-[#211F26] border-t border-[#49454F] with elevated center FAB in #D0BCFF
 */
@Composable
fun BentoBottomNav(
    isListening: Boolean,
    onMicClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BentoNavSurface)
            .border(BorderStroke(1.dp, BentoBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { /* Home default */ }
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = BentoLavenderPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Home",
                    color = BentoLavenderPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 2. History
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onHistoryClick)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = BentoTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "History",
                    color = BentoTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 3. Center Elevated FAB (w-14 h-14 rounded-2xl -mt-10 bg-[#D0BCFF] shadow-xl shadow-[#381E72]/40)
            Box(
                modifier = Modifier
                    .offset(y = (-14).dp)
                    .size(56.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = BentoPurpleDeep)
                    .clip(RoundedCornerShape(18.dp))
                    .background(BentoLavenderPrimary)
                    .clickable(onClick = onMicClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Mic Trigger",
                    tint = BentoPurpleDeep,
                    modifier = Modifier.size(30.dp)
                )
            }

            // 4. Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onHistoryClick)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Actions",
                    tint = BentoTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Actions",
                    color = BentoTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 5. Settings / Profile
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onSettingsClick)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = BentoTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Tune",
                    color = BentoTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BentoCardSurface,
        border = BorderStroke(1.dp, BentoBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = BentoLavenderPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = BentoTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
