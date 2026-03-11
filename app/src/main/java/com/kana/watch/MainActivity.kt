package com.kana.watch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "WatchMain"
    }

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
        syncFromServer()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun syncFromServer() {
        val syncToken = AppSettings.getSyncToken(this) ?: return
        val apiUrl = AppSettings.getApiUrl(this)

        Log.d(TAG, "Syncing packs from server...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$apiUrl/watch/sync/$syncToken").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val jsonObj = JSONObject(json)
                    val packsArray = jsonObj.getJSONArray("packs")

                    val remoteTokens = mutableSetOf<String>()

                    for (i in 0 until packsArray.length()) {
                        val packObj = packsArray.getJSONObject(i)
                        val token = packObj.getString("token")
                        remoteTokens.add(token)

                        val name = packObj.optString("name", "Pack $token")
                        val updatedAt = packObj.optString("updated_at", "")
                        val qLang = packObj.optString("question_lang", "")
                        val aLang = packObj.optString("answer_lang", "")
                        val author = packObj.optString("author", "")
                        val dlCount = packObj.optInt("download_count", 0)
                        val wordsArray = packObj.getJSONArray("words")
                        val words = mutableListOf<Word>()

                        for (j in 0 until wordsArray.length()) {
                            val w = wordsArray.getJSONObject(j)
                            words.add(
                                Word(
                                    question = w.getString("question"),
                                    answer = w.getString("answer"),
                                    reading = w.optString("reading", ""),
                                    audioUrl = w.optString("audio", "")
                                )
                            )
                        }

                        // Check if pack needs updating
                        val localPacks = WordStorage.loadAllPacks(this@MainActivity)
                        val localPack = localPacks.find { it.token == token }

                        if (localPack == null || localPack.updated != updatedAt) {
                            val pack = WordPack(token = token, name = name, updated = updatedAt, words = words, questionLang = qLang, answerLang = aLang, author = author, downloadCount = dlCount)
                            WordStorage.savePack(this@MainActivity, pack)
                            AudioCache.downloadPackAudio(this@MainActivity, words)

                            // Auto-enable new packs
                            if (localPack == null) {
                                val enabled = AppSettings.getEnabledPacks(this@MainActivity).toMutableSet()
                                enabled.add(token)
                                AppSettings.setEnabledPacks(this@MainActivity, enabled)
                            }

                            Log.d(TAG, "Synced pack '$name' ($token) with ${words.size} words")
                        }
                    }

                    // Remove packs that no longer exist on server
                    val localPacks = WordStorage.loadAllPacks(this@MainActivity)
                    for (localPack in localPacks) {
                        if (localPack.token !in remoteTokens) {
                            Log.d(TAG, "Removing deleted pack: ${localPack.name} (${localPack.token})")
                            WordStorage.deletePack(this@MainActivity, localPack.token)
                        }
                    }

                    withContext(Dispatchers.Main) { refreshUI() }
                    Log.d(TAG, "Sync complete. ${packsArray.length()} packs from server.")
                } else if (connection.responseCode == 401) {
                    Log.e(TAG, "Sync token invalid — unpaired")
                    AppSettings.unpair(this@MainActivity)
                    withContext(Dispatchers.Main) { refreshUI() }
                } else {
                    Log.e(TAG, "Sync failed: HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync error: ${e.message}", e)
            }
        }
    }

    private fun refreshUI() {
        val isPaired = AppSettings.isPaired(this)

        setContent {
            KanaWatchTheme {
                if (isPaired) {
                    MainMenuScreen(
                        isActive = AppSettings.isNotificationsActive(this),
                        intervalMinutes = AppSettings.getIntervalMinutes(this),
                        onStartQuiz = { startQuiz() },
                        onSettings = { openSettings() },
                        onSync = { syncFromServer() },
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
                } else {
                    PairScreen(
                        onPaired = {
                            syncFromServer()
                            refreshUI()
                        }
                    )
                }
            }
        }
    }

    private fun startQuiz() {
        val words = WordStorage.getEnabledWords(this)

        if (words.isEmpty()) {
            Toast.makeText(this, "No word packs enabled! Check Settings.", Toast.LENGTH_SHORT).show()
            return
        }

        val item = words.random()
        startActivity(Intent(this, QuizActivity::class.java).apply {
            putExtra(QuizExtras.EXTRA_CHARACTER, item.question)
            putExtra(QuizExtras.EXTRA_ROMAJI, item.answer)
            putExtra(QuizExtras.EXTRA_TYPE, "WORD")
            putExtra(QuizExtras.EXTRA_READING, item.reading)
            putExtra(QuizExtras.EXTRA_AUDIO_URL, item.audioUrl)
        })
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}

@Composable
fun PairScreen(onPaired: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var digits by remember { mutableStateOf(listOf(0, 0, 0, 0, 0, 0)) }
    var selectedDigit by remember { mutableStateOf(0) }
    var pairing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

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
                Text(
                    "Pair with\nPhone",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Text(
                    "Enter code from phone",
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Digit display - row 1 (first 3 digits)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0..2) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CompactChip(
                                onClick = {
                                    val updated = digits.toMutableList()
                                    updated[i] = (updated[i] + 1) % 10
                                    digits = updated
                                },
                                label = { Text("▲", fontSize = 10.sp) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = MaterialTheme.colors.surface
                                )
                            )
                            Text(
                                "${digits[i]}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onBackground,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            CompactChip(
                                onClick = {
                                    val updated = digits.toMutableList()
                                    updated[i] = if (updated[i] == 0) 9 else updated[i] - 1
                                    digits = updated
                                },
                                label = { Text("▼", fontSize = 10.sp) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = MaterialTheme.colors.surface
                                )
                            )
                        }
                    }
                }
            }

            // Digit display - row 2 (last 3 digits)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 3..5) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CompactChip(
                                onClick = {
                                    val updated = digits.toMutableList()
                                    updated[i] = (updated[i] + 1) % 10
                                    digits = updated
                                },
                                label = { Text("▲", fontSize = 10.sp) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = MaterialTheme.colors.surface
                                )
                            )
                            Text(
                                "${digits[i]}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onBackground,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            CompactChip(
                                onClick = {
                                    val updated = digits.toMutableList()
                                    updated[i] = if (updated[i] == 0) 9 else updated[i] - 1
                                    digits = updated
                                },
                                label = { Text("▼", fontSize = 10.sp) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = MaterialTheme.colors.surface
                                )
                            )
                        }
                    }
                }
            }

            if (error.isNotBlank()) {
                item {
                    Text(error, fontSize = 11.sp, color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center)
                }
            }

            item {
                Chip(
                    onClick = {
                        if (pairing) return@Chip
                        pairing = true
                        error = ""
                        val code = digits.joinToString("")
                        val apiUrl = AppSettings.getApiUrl(context)

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val connection = URL("$apiUrl/watch/pair").openConnection() as HttpURLConnection
                                connection.requestMethod = "POST"
                                connection.setRequestProperty("Content-Type", "application/json")
                                connection.doOutput = true
                                connection.connectTimeout = 10000
                                connection.readTimeout = 10000

                                val body = JSONObject()
                                body.put("code", code)
                                OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

                                val responseCode = connection.responseCode
                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                    val json = connection.inputStream.bufferedReader().readText()
                                    val obj = JSONObject(json)
                                    val syncToken = obj.getString("syncToken")
                                    AppSettings.setSyncToken(context, syncToken)

                                    withContext(Dispatchers.Main) {
                                        pairing = false
                                        onPaired()
                                    }
                                } else {
                                    val errJson = connection.errorStream?.bufferedReader()?.readText() ?: ""
                                    val errMsg = try {
                                        JSONObject(errJson).optString("error", "Pairing failed")
                                    } catch (_: Exception) { "Pairing failed" }

                                    withContext(Dispatchers.Main) {
                                        pairing = false
                                        error = errMsg
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    pairing = false
                                    error = "Connection failed"
                                }
                            }
                        }
                    },
                    label = { Text(if (pairing) "Pairing..." else "Pair", fontSize = 13.sp) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.primary
                    ),
                    enabled = !pairing,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}

@Composable
fun MainMenuScreen(
    isActive: Boolean,
    intervalMinutes: Int,
    onStartQuiz: () -> Unit,
    onSettings: () -> Unit,
    onSync: () -> Unit,
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
                    text = "Language\nLearning",
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

            // Sync
            item {
                Chip(
                    onClick = onSync,
                    label = { Text("Sync Packs", fontSize = 13.sp) },
                    secondaryLabel = { Text("From phone account", fontSize = 10.sp) },
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
