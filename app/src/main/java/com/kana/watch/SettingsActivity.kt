package com.kana.watch

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.kana.watch.theme.KanaWatchTheme
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshUI()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun refreshUI() {
        val packs = WordStorage.loadAllPacks(this)
        val enabledPacks = AppSettings.getEnabledPacks(this)

        setContent {
            KanaWatchTheme {
                SettingsScreen(
                    currentInterval = AppSettings.getIntervalMinutes(this),
                    packs = packs,
                    enabledPacks = enabledPacks,
                    onSaveInterval = { minutes ->
                        AppSettings.setIntervalMinutes(this, minutes)
                        if (AppSettings.isNotificationsActive(this)) {
                            NotificationScheduler.schedule(this, minutes)
                        }
                    },
                    onTogglePack = { token, enabled ->
                        val current = AppSettings.getEnabledPacks(this).toMutableSet()
                        if (enabled) current.add(token) else current.remove(token)
                        AppSettings.setEnabledPacks(this, current)
                        refreshUI()
                    },
                    onUpdatePack = { token -> updatePack(token) },
                    onDeletePack = { token ->
                        WordStorage.deletePack(this, token)
                        refreshUI()
                    },
                    onClose = { finish() },
                    onUnpair = {
                        AppSettings.unpair(this)
                        Toast.makeText(this, "Unpaired from phone", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }

    private fun updatePack(token: String) {
        val baseUrl = AppSettings.getBaseUrl(this)
        val url = "$baseUrl$token"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val jsonObj = JSONObject(json)

                    val name = jsonObj.optString("name", "Pack $token")
                    val updated = jsonObj.optString("updated_at", "")
                    val wordsArray = jsonObj.getJSONArray("words")
                    val words = mutableListOf<Word>()

                    for (i in 0 until wordsArray.length()) {
                        val w = wordsArray.getJSONObject(i)
                        words.add(
                            Word(
                                question = w.getString("question"),
                                answer = w.getString("answer"),
                                reading = w.optString("reading", ""),
                                audioUrl = w.optString("audio", "")
                            )
                        )
                    }

                    val pack = WordPack(
                        token = token,
                        name = name,
                        updated = updated,
                        words = words
                    )
                    WordStorage.savePack(this@SettingsActivity, pack)

                    // Cache audio files
                    AudioCache.downloadPackAudio(this@SettingsActivity, words)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SettingsActivity,
                            "Updated \"$name\" (${words.size} words)",
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshUI()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SettingsActivity,
                            "Update failed: server error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Update failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    currentInterval: Int,
    packs: List<WordPack>,
    enabledPacks: Set<String>,
    onSaveInterval: (Int) -> Unit,
    onTogglePack: (String, Boolean) -> Unit,
    onUpdatePack: (String) -> Unit,
    onDeletePack: (String) -> Unit,
    onClose: () -> Unit,
    onUnpair: () -> Unit
) {
    val presets = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)
    var selectedIndex by remember {
        mutableStateOf(presets.indexOf(currentInterval).coerceAtLeast(0))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                CompactChip(
                    onClick = onClose,
                    label = { Text("← Back", fontSize = 11.sp) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.surface
                    )
                )
            }

            // Title
            item {
                Text(
                    text = "Settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // --- Interval ---
            item {
                Text(
                    text = "Notification Interval",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactChip(
                        onClick = {
                            if (selectedIndex > 0) {
                                selectedIndex--
                                onSaveInterval(presets[selectedIndex])
                            }
                        },
                        label = { Text("◀", fontSize = 14.sp) },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.surface
                        )
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${presets[selectedIndex]}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onBackground
                        )
                        Text(
                            text = "minutes",
                            fontSize = 11.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    CompactChip(
                        onClick = {
                            if (selectedIndex < presets.size - 1) {
                                selectedIndex++
                                onSaveInterval(presets[selectedIndex])
                            }
                        },
                        label = { Text("▶", fontSize = 14.sp) },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.surface
                        )
                    )
                }
            }

            // --- Word packs ---
            if (packs.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Text(
                        text = "Word Packs",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }

                for (pack in packs) {
                    val isEnabled = pack.token in enabledPacks

                    // Toggle on/off
                    item {
                        ToggleChip(
                            checked = isEnabled,
                            onCheckedChange = { onTogglePack(pack.token, it) },
                            label = { Text(pack.name, fontSize = 12.sp) },
                            secondaryLabel = {
                                Text(
                                    "${pack.words.size} words • ${pack.updated}",
                                    fontSize = 10.sp
                                )
                            },
                            toggleControl = {
                                Switch(checked = isEnabled)
                            },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }

                    // Update & Delete buttons
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            CompactChip(
                                onClick = { onUpdatePack(pack.token) },
                                label = { Text("Update", fontSize = 11.sp) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = MaterialTheme.colors.secondary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            CompactChip(
                                onClick = { onDeletePack(pack.token) },
                                label = { Text("Delete", fontSize = 11.sp) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = MaterialTheme.colors.error
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Unpair
            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                Chip(
                    onClick = onUnpair,
                    label = { Text("Unpair Watch", fontSize = 13.sp) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.error
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}
