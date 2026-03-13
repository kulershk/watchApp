package com.kana.phone

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.*

@Composable
fun PackPreviewDialog(
    pack: RemotePack,
    words: List<Word>,
    loading: Boolean,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    pack.name.ifBlank { "Unnamed" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${pack.wordCount} words",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (pack.questionLang.isNotBlank() || pack.answerLang.isNotBlank()) {
                    Text(
                        "${langWithFlag(pack.questionLang).ifBlank { "?" }} \u2192 ${langWithFlag(pack.answerLang).ifBlank { "?" }}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (loading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (words.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Failed to load preview",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(words) { index, word ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                // Image
                                if (word.imageUrl.isNotBlank()) {
                                    var bitmap by remember(word.imageUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
                                    LaunchedEffect(word.imageUrl) {
                                        withContext(Dispatchers.IO) {
                                            ImageCache.download(context, word.imageUrl)
                                        }
                                        val file = ImageCache.getCachedFile(context, word.imageUrl)
                                        if (file != null) {
                                            bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                        }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap!!.asImageBitmap(),
                                            contentDescription = "Word image",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 120.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Audio play button
                                    if (word.audioUrl.isNotBlank()) {
                                        var playing by remember { mutableStateOf(false) }
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (playing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                                .clickable {
                                                    if (playing) return@clickable
                                                    playing = true
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        AudioCache.download(context, word.audioUrl)
                                                        withContext(Dispatchers.Main) {
                                                            val cached = AudioCache.getCachedFile(context, word.audioUrl)
                                                            if (cached != null) {
                                                                try {
                                                                    val player = MediaPlayer()
                                                                    player.setDataSource(cached.absolutePath)
                                                                    player.setOnCompletionListener { it.release(); playing = false }
                                                                    player.setOnErrorListener { mp, _, _ -> mp.release(); playing = false; true }
                                                                    player.prepare()
                                                                    player.start()
                                                                } catch (_: Exception) { playing = false }
                                                            } else { playing = false }
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                                contentDescription = if (playing) "Stop" else "Play",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Text(
                                        word.question,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        word.answer,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.End
                                    )
                                }
                                if (word.reading.isNotBlank()) {
                                    Text(
                                        word.reading,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            if (index < words.lastIndex) {
                                Divider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
