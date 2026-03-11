package com.kana.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.kana.watch.theme.KanaWatchTheme

class QuizActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val character = intent.getStringExtra(QuizExtras.EXTRA_CHARACTER) ?: "あ"
        val romaji = intent.getStringExtra(QuizExtras.EXTRA_ROMAJI) ?: "a"
        val type = intent.getStringExtra(QuizExtras.EXTRA_TYPE) ?: "WORD"
        val reading = intent.getStringExtra(QuizExtras.EXTRA_READING) ?: ""
        val audioUrl = intent.getStringExtra(QuizExtras.EXTRA_AUDIO_URL) ?: ""

        setContent {
            KanaWatchTheme {
                QuizScreen(
                    character = character,
                    romaji = romaji,
                    type = type,
                    reading = reading,
                    audioUrl = audioUrl,
                    onNext = { loadNext() },
                    onClose = { finish() }
                )
            }
        }
    }

    private fun loadNext() {
        val words = WordStorage.getEnabledWords(this)

        if (words.isEmpty()) {
            finish()
            return
        }

        val item = words.random()
        intent.putExtra(QuizExtras.EXTRA_CHARACTER, item.question)
        intent.putExtra(QuizExtras.EXTRA_ROMAJI, item.answer)
        intent.putExtra(QuizExtras.EXTRA_TYPE, "WORD")
        intent.putExtra(QuizExtras.EXTRA_READING, item.reading)
        intent.putExtra(QuizExtras.EXTRA_AUDIO_URL, item.audioUrl)
        recreate()
    }
}

@Composable
fun QuizScreen(
    character: String,
    romaji: String,
    type: String,
    reading: String,
    audioUrl: String = "",
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val hasAudio = audioUrl.isNotBlank() && AudioCache.getCachedFile(context, audioUrl) != null
    var revealed by remember(character) { mutableStateOf(false) }
    var hintShown by remember(character) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Big character / question
            Text(
                text = character,
                fontSize = if (character.length > 4) 28.sp else 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center,
                modifier = if (hasAudio) Modifier
                    .border(2.dp, MaterialTheme.colors.secondary, RoundedCornerShape(8.dp))
                    .clickable { AudioCache.play(context, audioUrl) }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                else Modifier
            )

            // Reading hint (hidden until tapped or answer revealed)
            if (reading.isNotBlank() && (hintShown || revealed)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reading,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.secondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!revealed) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactChip(
                        onClick = onClose,
                        label = { Text("←", fontSize = 12.sp) },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.surface
                        )
                    )
                    if (reading.isNotBlank() && !hintShown) {
                        CompactChip(
                            onClick = { hintShown = true },
                            label = { Text("Hint", fontSize = 12.sp) },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = MaterialTheme.colors.surface
                            )
                        )
                    }
                    CompactChip(
                        onClick = { revealed = true },
                        label = { Text("Show", fontSize = 12.sp) },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    )
                }
            } else {
                Text(
                    text = romaji,
                    fontSize = if (romaji.length > 8) 20.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                CompactChip(
                    onClick = onNext,
                    label = { Text("Next", fontSize = 11.sp) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.secondary
                    )
                )
            }
        }
    }
}
