package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.BentoBadgeGray
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCardSurface
import com.example.ui.theme.BentoGreenActive
import com.example.ui.theme.BentoLavenderPrimary
import com.example.ui.theme.BentoPurpleDeep
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bento Grid Arc Reactor / Voice Energy Sphere with glowing purple/lavender gradient
 * and responsive voice RMS audio reactivity.
 */
@Composable
fun JarvisArcReactor(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier,
    onReactorClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bento_arc_reactor_anim")

    // Slow clockwise rotation for outer ring
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    // Faster counter-clockwise rotation for inner segmented ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    // Pulsing core breathing effect (animate-ping / breathing)
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Ping expanding wave
    val pingWave by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ping_wave"
    )
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ping_alpha"
    )

    val activeRmsMultiplier = if (isListening || isSpeaking) 1f + audioRms * 1.2f else 1f
    val dynamicPulse = corePulse * activeRmsMultiplier

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(240.dp)
            .clickable(onClick = onReactorClick)
    ) {
        Canvas(modifier = Modifier.size(230.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f

            // 1. Ambient Expanding Ping Wave (animate-ping)
            drawCircle(
                color = BentoLavenderPrimary.copy(alpha = pingAlpha),
                radius = maxRadius * 0.95f * pingWave,
                center = center,
                style = Stroke(width = 2f)
            )

            // 2. Outer Bento Glow Orb Gradient from #D0BCFF via #381E72 to #1C1B1F
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BentoPurpleDeep.copy(alpha = 0.9f),
                        BentoPurpleDeep.copy(alpha = 0.6f),
                        BentoBackground.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 0.88f
                ),
                radius = maxRadius * 0.85f,
                center = center
            )

            // 3. Rotating outer decorative dashed track with lavender tick nodes
            rotate(outerRotation, pivot = center) {
                drawCircle(
                    color = BentoLavenderPrimary.copy(alpha = 0.30f),
                    radius = maxRadius * 0.80f,
                    center = center,
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                )

                for (i in 0 until 8) {
                    val angle = (i * 45.0) * Math.PI / 180.0
                    val tickRadius = maxRadius * 0.80f
                    val tickX = center.x + (tickRadius * cos(angle)).toFloat()
                    val tickY = center.y + (tickRadius * sin(angle)).toFloat()
                    drawCircle(
                        color = BentoLavenderPrimary.copy(alpha = 0.6f),
                        radius = 2.5f,
                        center = Offset(tickX, tickY)
                    )
                }
            }

            // 4. Inner Bento Orb: Gradient-to-br from #D0BCFF via #381E72 to #1C1B1F
            val orbRadius = maxRadius * 0.62f * dynamicPulse
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BentoLavenderPrimary,
                        BentoPurpleDeep,
                        BentoBackground
                    ),
                    start = Offset(center.x - orbRadius, center.y - orbRadius),
                    end = Offset(center.x + orbRadius, center.y + orbRadius)
                ),
                radius = orbRadius,
                center = center
            )

            // 5. White semi-transparent inner ring (border-2 border-white/20)
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = orbRadius * 0.70f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 6. Central Glowing Power Core Dot with soft shadow glow
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = 14f * dynamicPulse,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 7f,
                center = center
            )
        }

        // Center Status Badge inside Bento Orb
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.Bolt,
                contentDescription = "Jarvis Status Icon",
                tint = if (isListening) BentoGreenActive else if (isSpeaking) BentoLavenderPrimary else Color.White,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isListening) "LISTENING" else if (isSpeaking) "SPEAKING" else "READY",
                color = if (isListening) BentoGreenActive else BentoLavenderPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.5.sp
            )
        }
    }
}

/**
 * Audio frequency spectrum waveform bars in Bento Lavender/Green styling
 */
@Composable
fun JarvisAudioVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave_anim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 20
        for (i in 0 until barCount) {
            val offset = i.toFloat() * 0.32f
            val baseHeight = if (isListening || isSpeaking) {
                val wave = sin(wavePhase + offset) * 0.5f + 0.5f
                (6f + wave * 20f * (0.4f + audioRms * 1.4f)).coerceIn(4f, 28f)
            } else {
                3f + (i % 4) * 2f
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(baseHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isListening) BentoGreenActive
                        else if (isSpeaking) BentoLavenderPrimary
                        else BentoBadgeGray.copy(alpha = 0.7f)
                    )
            )
        }
    }
}

/**
 * Bento Grid Header Bar
 * Matching HTML: J round badge in #D0BCFF with text #381E72, "Jarvis AI", green active dot, settings button in #2B2930
 */
@Composable
fun JarvisTelemetryHeader(
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left profile & system indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 40.dp circle badge in #D0BCFF with 'J' in #381E72
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BentoLavenderPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "J",
                    color = BentoPurpleDeep,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Column {
                Text(
                    text = "Jarvis AI",
                    color = BentoTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BentoGreenActive)
                    )
                    Text(
                        text = "SYSTEM ACTIVE",
                        color = BentoGreenActive,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Right Settings Button (w-10 h-10 rounded-xl bg-[#2B2930] border border-[#49454F])
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BentoCardSurface)
                .border(1.dp, BentoBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onSettingsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = BentoTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
