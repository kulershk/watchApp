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

class DownloadActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KanaWatchTheme {
                DownloadScreen(
                    onDownload = { token -> downloadPack(token) },
                    onClose = { finish() }
                )
            }
        }
    }

    private fun downloadPack(token: String) {
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
                    WordStorage.savePack(this@DownloadActivity, pack)

                    // Cache audio files
                    AudioCache.downloadPackAudio(this@DownloadActivity, words)

                    // Auto-enable the new pack
                    val enabled = AppSettings.getEnabledPacks(this@DownloadActivity).toMutableSet()
                    enabled.add(token)
                    AppSettings.setEnabledPacks(this@DownloadActivity, enabled)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@DownloadActivity,
                            "Downloaded \"$name\" (${words.size} words)",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@DownloadActivity,
                            "Invalid token or server error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DownloadActivity,
                        "Connection failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

@Composable
fun DownloadScreen(
    onDownload: (String) -> Unit,
    onClose: () -> Unit
) {
    var digit1 by remember { mutableStateOf(0) }
    var digit2 by remember { mutableStateOf(0) }
    var digit3 by remember { mutableStateOf(0) }
    var digit4 by remember { mutableStateOf(0) }
    var selectedDigit by remember { mutableStateOf(0) }

    val digits = listOf(digit1, digit2, digit3, digit4)
    val token = "${digit1}${digit2}${digit3}${digit4}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        val listState = rememberScalingLazyListState(initialCenterItemIndex = 3)
        ScalingLazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
            anchorType = ScalingLazyListAnchorType.ItemCenter
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

            item {
                Text(
                    text = "Enter Token",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0..3) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CompactChip(
                                onClick = {
                                    selectedDigit = i
                                    when (i) {
                                        0 -> digit1 = (digit1 + 1) % 10
                                        1 -> digit2 = (digit2 + 1) % 10
                                        2 -> digit3 = (digit3 + 1) % 10
                                        3 -> digit4 = (digit4 + 1) % 10
                                    }
                                },
                                label = { Text("▲", fontSize = 10.sp) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = if (selectedDigit == i)
                                        MaterialTheme.colors.primary
                                    else
                                        MaterialTheme.colors.surface
                                )
                            )

                            Text(
                                text = "${digits[i]}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedDigit == i)
                                    MaterialTheme.colors.primary
                                else
                                    MaterialTheme.colors.onBackground
                            )

                            CompactChip(
                                onClick = {
                                    selectedDigit = i
                                    when (i) {
                                        0 -> digit1 = if (digit1 == 0) 9 else digit1 - 1
                                        1 -> digit2 = if (digit2 == 0) 9 else digit2 - 1
                                        2 -> digit3 = if (digit3 == 0) 9 else digit3 - 1
                                        3 -> digit4 = if (digit4 == 0) 9 else digit4 - 1
                                    }
                                },
                                label = { Text("▼", fontSize = 10.sp) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = if (selectedDigit == i)
                                        MaterialTheme.colors.primary
                                    else
                                        MaterialTheme.colors.surface
                                )
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Chip(
                    onClick = { onDownload(token) },
                    label = { Text("Download", fontSize = 13.sp) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.primary
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}
