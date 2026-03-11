package com.kana.phone

import android.media.MediaPlayer
import android.widget.Toast
import kotlinx.coroutines.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WordAudioControls(
    audio: String,
    index: Int,
    context: android.content.Context,
    onAudioChanged: (String) -> Unit
) {
    var recording by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val recorder = remember { AudioRecorderHelper(context) }
    var hasPermission by remember { mutableStateOf(recorder.hasPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            if (recorder.startRecording()) {
                recording = true
            } else {
                Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { recorder.cleanup() }
    }

    if (audio.isNotBlank()) {
        // Has audio - WhatsApp-style playback bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Play button (circle)
            IconButton(
                onClick = {
                    if (playing) return@IconButton
                    playing = true
                    CoroutineScope(Dispatchers.IO).launch {
                        AudioCache.download(context, audio)
                        withContext(Dispatchers.Main) {
                            val cached = AudioCache.getCachedFile(context, audio)
                            if (cached != null) {
                                try {
                                    val player = MediaPlayer()
                                    player.setDataSource(cached.absolutePath)
                                    player.setOnCompletionListener {
                                        it.release()
                                        playing = false
                                    }
                                    player.setOnErrorListener { mp, _, _ ->
                                        mp.release()
                                        playing = false
                                        true
                                    }
                                    player.prepare()
                                    player.start()
                                } catch (_: Exception) {
                                    playing = false
                                    Toast.makeText(context, "Playback failed", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                playing = false
                                Toast.makeText(context, "Failed to download audio", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                enabled = !playing
            ) {
                Text(
                    if (playing) "\u25a0" else "\u25b6",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp
                )
            }

            // Waveform placeholder bars — fill available width
            BoxWithConstraints(
                modifier = Modifier.weight(1f).height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val barWidth = 3.dp
                val gap = 2.dp
                val barCount = ((maxWidth + gap) / (barWidth + gap)).toInt().coerceAtLeast(1)
                val heights = listOf(6, 12, 8, 16, 10, 14, 6, 12, 18, 8, 14, 10, 16, 6, 12, 8)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(barCount) { i ->
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(heights[i % heights.size].dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    if (playing) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }

            // Delete button
            IconButton(
                onClick = {
                    ApiClient.deleteAudio(audio, context)
                    onAudioChanged("")
                },
                modifier = Modifier.size(32.dp)
            ) {
                Text("\u2715", color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
            }
        }
    } else if (recording) {
        // WhatsApp-style recording indicator
        var elapsed by remember { mutableStateOf(0L) }
        LaunchedEffect(recording) {
            val start = System.currentTimeMillis()
            while (recording) {
                elapsed = (System.currentTimeMillis() - start) / 1000
                delay(500)
            }
        }

        val pulseAnim = rememberInfiniteTransition(label = "pulse")
        val pulse by pulseAnim.animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pulsing red dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            )

            // Timer
            Text(
                text = String.format("%d:%02d", elapsed / 60, elapsed % 60),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )

            // Animated waveform bars — fill available width
            BoxWithConstraints(
                modifier = Modifier.weight(1f).height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val barWidth = 3.dp
                val gap = 2.dp
                val barCount = ((maxWidth + gap) / (barWidth + gap)).toInt().coerceAtLeast(1)
                val waveAnim = rememberInfiniteTransition(label = "wave")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(barCount) { i ->
                        val height by waveAnim.animateFloat(
                            initialValue = 4f,
                            targetValue = 18f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 300 + (i % 7) * 80,
                                    easing = FastOutSlowInEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bar$i"
                        )
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(height.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                        )
                    }
                }
            }

            // Stop / send button
            IconButton(
                onClick = {
                    val file = recorder.stopRecording()
                    recording = false
                    if (file != null && file.exists()) {
                        uploading = true
                        ApiClient.uploadAudio(file, context) { success, result ->
                            uploading = false
                            if (success) {
                                onAudioChanged(result)
                            } else {
                                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                            }
                            file.delete()
                        }
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Text("\u2713", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
            }
        }
    } else if (uploading) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
            Text(
                "Uploading...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        // Mic button - WhatsApp style
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    if (hasPermission) {
                        if (recorder.startRecording()) {
                            recording = true
                        } else {
                            Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                val micColor = MaterialTheme.colorScheme.onPrimary
                Canvas(modifier = Modifier.size(20.dp)) {
                    val w = size.width
                    val h = size.height
                    val stroke = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    // Mic body (rounded rect)
                    drawRoundRect(
                        color = micColor,
                        topLeft = androidx.compose.ui.geometry.Offset(w * 0.33f, h * 0.1f),
                        size = androidx.compose.ui.geometry.Size(w * 0.34f, h * 0.45f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.17f),
                    )
                    // Arc around mic
                    val arcPath = Path().apply {
                        moveTo(w * 0.22f, h * 0.45f)
                        cubicTo(w * 0.22f, h * 0.72f, w * 0.35f, h * 0.78f, w * 0.5f, h * 0.78f)
                        cubicTo(w * 0.65f, h * 0.78f, w * 0.78f, h * 0.72f, w * 0.78f, h * 0.45f)
                    }
                    drawPath(arcPath, color = micColor, style = stroke)
                    // Stem
                    drawLine(micColor, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.78f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.9f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
                }
            }
            Text(
                "Tap to record",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
