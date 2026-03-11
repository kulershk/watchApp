package com.kana.watch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.*
import com.kana.watch.theme.KanaWatchTheme
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* continue regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        refreshUI()
        checkForPackUpdates()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun checkForPackUpdates() {
        val packs = WordStorage.loadAllPacks(this)
        if (packs.isEmpty()) return

        val baseUrl = AppSettings.getBaseUrl(this)

        CoroutineScope(Dispatchers.IO).launch {
            for (pack in packs) {
                try {
                    val connection = URL("$baseUrl${pack.token}").openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val json = connection.inputStream.bufferedReader().readText()
                        val jsonObj = JSONObject(json)
                        val remoteUpdated = jsonObj.optString("updated_at", "")

                        if (remoteUpdated.isNotBlank() && remoteUpdated != pack.updated) {
                            val name = jsonObj.optString("name", "Pack ${pack.token}")
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

                            val updated = WordPack(
                                token = pack.token,
                                name = name,
                                updated = remoteUpdated,
                                words = words
                            )
                            WordStorage.savePack(this@MainActivity, updated)
                            AudioCache.downloadPackAudio(this@MainActivity, words)
                        }
                    }
                } catch (_: Exception) {
                    // skip failed pack silently
                }
            }
        }
    }

    private fun refreshUI() {
        setContent {
            KanaWatchTheme {
                MainMenuScreen(
                    isActive = AppSettings.isNotificationsActive(this),
                    intervalMinutes = AppSettings.getIntervalMinutes(this),
                    onStartQuiz = { startQuiz() },
                    onDownloadPack = { openDownload() },
                    onSettings = { openSettings() },
                    onToggleNotifications = { start ->
                        if (start) {
                            val interval = AppSettings.getIntervalMinutes(this)
                            NotificationScheduler.schedule(this, interval)
                        } else {
                            NotificationScheduler.cancel(this)
                        }
                        refreshUI()
                    }
                )
            }
        }
    }

    private fun startQuiz() {
        // Build quiz pool from settings
        val pool = mutableListOf<Any>()

        if (AppSettings.isHiraganaEnabled(this)) {
            pool.addAll(KanaData.hiragana)
        }
        if (AppSettings.isKatakanaEnabled(this)) {
            pool.addAll(KanaData.katakana)
        }

        val enabledWords = WordStorage.getEnabledWords(this)
        pool.addAll(enabledWords)

        if (pool.isEmpty()) {
            Toast.makeText(this, "Nothing enabled! Check Settings.", Toast.LENGTH_SHORT).show()
            return
        }

        val item = pool.random()

        when (item) {
            is Kana -> {
                startActivity(Intent(this, QuizActivity::class.java).apply {
                    putExtra(QuizExtras.EXTRA_CHARACTER, item.character)
                    putExtra(QuizExtras.EXTRA_ROMAJI, item.romaji)
                    putExtra(QuizExtras.EXTRA_TYPE, item.type.name)
                })
            }
            is Word -> {
                startActivity(Intent(this, QuizActivity::class.java).apply {
                    putExtra(QuizExtras.EXTRA_CHARACTER, item.question)
                    putExtra(QuizExtras.EXTRA_ROMAJI, item.answer)
                    putExtra(QuizExtras.EXTRA_TYPE, "WORD")
                    putExtra(QuizExtras.EXTRA_READING, item.reading)
                    putExtra(QuizExtras.EXTRA_AUDIO_URL, item.audioUrl)
                })
            }
        }
    }

    private fun openDownload() {
        startActivity(Intent(this, DownloadActivity::class.java))
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}

@Composable
fun MainMenuScreen(
    isActive: Boolean,
    intervalMinutes: Int,
    onStartQuiz: () -> Unit,
    onDownloadPack: () -> Unit,
    onSettings: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit
) {
    var active by remember(isActive) { mutableStateOf(isActive) }

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
            // Title
            item {
                Text(
                    text = "仮名\nKana Quiz",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Start Quiz
            item {
                Chip(
                    onClick = onStartQuiz,
                    label = { Text("Start Quiz", fontSize = 13.sp) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.secondary
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }

            // Download Pack
            item {
                Chip(
                    onClick = onDownloadPack,
                    label = { Text("Download Pack", fontSize = 13.sp) },
                    secondaryLabel = { Text("Enter 4-digit token", fontSize = 10.sp) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }

            // Toggle notifications
            item {
                Chip(
                    onClick = {
                        active = !active
                        onToggleNotifications(active)
                    },
                    label = {
                        Text(
                            if (active) "Stop Reminders" else "Start Reminders",
                            fontSize = 13.sp
                        )
                    },
                    secondaryLabel = {
                        Text(
                            if (active) "Every $intervalMinutes min" else "Tap to enable",
                            fontSize = 10.sp
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (active)
                            MaterialTheme.colors.error
                        else
                            MaterialTheme.colors.primary
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }

            // Settings
            item {
                Chip(
                    onClick = onSettings,
                    label = { Text("Settings", fontSize = 13.sp) },
                    secondaryLabel = { Text("Timer: $intervalMinutes min", fontSize = 10.sp) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}
