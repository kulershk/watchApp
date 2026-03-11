package com.kana.phone

import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object PackUpdater {

    fun checkForUpdates(context: Context) {
        val packs = WordStorage.loadAllPacks(context)
        if (packs.isEmpty()) return

        val baseUrl = AppSettings.getBaseUrl(context)

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
                            WordStorage.savePack(context, updated)
                            AudioCache.downloadPackAudio(context, words)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun downloadPack(context: Context, token: String, onResult: (Boolean, String) -> Unit) {
        val baseUrl = AppSettings.getBaseUrl(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$baseUrl$token").openConnection() as HttpURLConnection
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

                    val pack = WordPack(token = token, name = name, updated = updated, words = words)
                    WordStorage.savePack(context, pack)
                    AudioCache.downloadPackAudio(context, words)

                    val enabled = AppSettings.getEnabledPacks(context).toMutableSet()
                    enabled.add(token)
                    AppSettings.setEnabledPacks(context, enabled)

                    withContext(Dispatchers.Main) {
                        onResult(true, "Downloaded \"$name\" (${words.size} words)")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Invalid token or server error")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Connection failed: ${e.message}")
                }
            }
        }
    }

    fun updatePack(context: Context, token: String, onResult: (Boolean, String) -> Unit) {
        val baseUrl = AppSettings.getBaseUrl(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$baseUrl$token").openConnection() as HttpURLConnection
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

                    val pack = WordPack(token = token, name = name, updated = updated, words = words)
                    WordStorage.savePack(context, pack)
                    AudioCache.downloadPackAudio(context, words)

                    withContext(Dispatchers.Main) {
                        onResult(true, "Updated \"$name\" (${words.size} words)")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Update failed: server error")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Update failed: ${e.message}")
                }
            }
        }
    }
}
