package com.kana.phone

import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object PackUpdater {

    fun checkForUpdates(context: Context, onSynced: ((List<String>) -> Unit)? = null) {
        val packs = WordStorage.loadAllPacks(context)
        if (packs.isEmpty()) return

        val authToken = AppSettings.getAuthToken(context)

        CoroutineScope(Dispatchers.IO).launch {
            val syncedNames = mutableListOf<String>()

            for (pack in packs) {
                try {
                    val connection = URL("${BuildConfig.API_BASE}/words/${pack.id}").openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    if (authToken != null) {
                        connection.setRequestProperty("Authorization", "Bearer $authToken")
                    }

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val json = connection.inputStream.bufferedReader().readText()
                        val jsonObj = JSONObject(json)
                        val remoteUpdated = jsonObj.optString("updated_at", "")

                        if (remoteUpdated.isNotBlank() && remoteUpdated != pack.updated) {
                            val name = jsonObj.optString("name", "Pack ${pack.id}")
                            val qLang = jsonObj.optString("question_lang", "")
                            val aLang = jsonObj.optString("answer_lang", "")
                            val author = jsonObj.optString("author", "")
                            val dlCount = jsonObj.optInt("download_count", 0)
                            val wordsArray = jsonObj.getJSONArray("words")
                            val words = mutableListOf<Word>()

                            for (i in 0 until wordsArray.length()) {
                                val w = wordsArray.getJSONObject(i)
                                words.add(
                                    Word(
                                        question = w.getString("question"),
                                        answer = w.getString("answer"),
                                        reading = w.optString("reading", ""),
                                        audioUrl = w.optString("audio", ""),
                                        imageUrl = w.optString("image", "")
                                    )
                                )
                            }

                            val tags = jsonObj.optString("tags", "")
                            val verStatus = jsonObj.optString("verification_status", "none")
                            val avgRating = jsonObj.optDouble("avg_rating", 0.0).toFloat()
                            val ratingCount = jsonObj.optInt("rating_count", 0)
                            val updated = WordPack(
                                id = pack.id,
                                name = name,
                                updated = remoteUpdated,
                                words = words,
                                questionLang = qLang,
                                answerLang = aLang,
                                author = author,
                                downloadCount = dlCount,
                                tags = tags,
                                verificationStatus = verStatus,
                                avgRating = avgRating,
                                ratingCount = ratingCount
                            )
                            WordStorage.savePack(context, updated)
                            AudioCache.downloadPackAudio(context, words)
                            ImageCache.downloadPackImages(context, words)
                            syncedNames.add("\"$name\" (${words.size} words)")
                        }
                    }
                } catch (_: Exception) {}
            }

            if (syncedNames.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    onSynced?.invoke(syncedNames)
                }
            }
        }
    }

    fun downloadPack(context: Context, id: String, onResult: (Boolean, String) -> Unit) {
        val authToken = AppSettings.getAuthToken(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("${BuildConfig.API_BASE}/words/$id").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                if (authToken != null) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val jsonObj = JSONObject(json)

                    val name = jsonObj.optString("name", "Pack $id")
                    val updated = jsonObj.optString("updated_at", "")
                    val qLang = jsonObj.optString("question_lang", "")
                    val aLang = jsonObj.optString("answer_lang", "")
                    val author = jsonObj.optString("author", "")
                    val dlCount = jsonObj.optInt("download_count", 0)
                    val wordsArray = jsonObj.getJSONArray("words")
                    val words = mutableListOf<Word>()

                    for (i in 0 until wordsArray.length()) {
                        val w = wordsArray.getJSONObject(i)
                        words.add(
                            Word(
                                question = w.getString("question"),
                                answer = w.getString("answer"),
                                reading = w.optString("reading", ""),
                                audioUrl = w.optString("audio", ""),
                                imageUrl = w.optString("image", "")
                            )
                        )
                    }

                    val tags = jsonObj.optString("tags", "")
                    val verStatus = jsonObj.optString("verification_status", "none")
                    val avgRating = jsonObj.optDouble("avg_rating", 0.0).toFloat()
                    val ratingCount = jsonObj.optInt("rating_count", 0)
                    val pack = WordPack(id = id, name = name, updated = updated, words = words, questionLang = qLang, answerLang = aLang, author = author, downloadCount = dlCount, tags = tags, verificationStatus = verStatus, avgRating = avgRating, ratingCount = ratingCount)
                    WordStorage.savePack(context, pack)
                    AudioCache.downloadPackAudio(context, words)
                    ImageCache.downloadPackImages(context, words)

                    val enabled = AppSettings.getEnabledPacks(context).toMutableSet()
                    enabled.add(id)
                    AppSettings.setEnabledPacks(context, enabled)

                    withContext(Dispatchers.Main) {
                        onResult(true, "Downloaded \"$name\" (${words.size} words)")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Invalid pack or server error")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Connection failed: ${e.message}")
                }
            }
        }
    }

    fun redeemShareCode(context: Context, code: String, onResult: (Boolean, String) -> Unit) {
        val authToken = AppSettings.getAuthToken(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = java.net.URL("${BuildConfig.API_BASE}/packs/share/$code").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                if (authToken != null) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val jsonObj = JSONObject(json)

                    val id = jsonObj.getInt("id").toString()
                    val name = jsonObj.optString("name", "Pack $id")
                    val updated = jsonObj.optString("updated_at", "")
                    val qLang = jsonObj.optString("question_lang", "")
                    val aLang = jsonObj.optString("answer_lang", "")
                    val author = jsonObj.optString("author", "")
                    val dlCount = jsonObj.optInt("download_count", 0)
                    val wordsArray = jsonObj.getJSONArray("words")
                    val words = mutableListOf<Word>()

                    for (i in 0 until wordsArray.length()) {
                        val w = wordsArray.getJSONObject(i)
                        words.add(
                            Word(
                                question = w.getString("question"),
                                answer = w.getString("answer"),
                                reading = w.optString("reading", ""),
                                audioUrl = w.optString("audio", ""),
                                imageUrl = w.optString("image", "")
                            )
                        )
                    }

                    val tags = jsonObj.optString("tags", "")
                    val verStatus = jsonObj.optString("verification_status", "none")
                    val avgRating = jsonObj.optDouble("avg_rating", 0.0).toFloat()
                    val ratingCount = jsonObj.optInt("rating_count", 0)
                    val pack = WordPack(id = id, name = name, updated = updated, words = words, questionLang = qLang, answerLang = aLang, author = author, downloadCount = dlCount, tags = tags, verificationStatus = verStatus, avgRating = avgRating, ratingCount = ratingCount)
                    WordStorage.savePack(context, pack)
                    AudioCache.downloadPackAudio(context, words)
                    ImageCache.downloadPackImages(context, words)

                    val enabled = AppSettings.getEnabledPacks(context).toMutableSet()
                    enabled.add(id)
                    AppSettings.setEnabledPacks(context, enabled)

                    withContext(Dispatchers.Main) {
                        onResult(true, "Downloaded \"$name\" (${words.size} words)")
                    }
                } else if (connection.responseCode == 410) {
                    withContext(Dispatchers.Main) { onResult(false, "Share code expired") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Invalid share code") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Connection failed: ${e.message}") }
            }
        }
    }

    fun updatePack(context: Context, id: String, onResult: (Boolean, String) -> Unit) {
        val authToken = AppSettings.getAuthToken(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("${BuildConfig.API_BASE}/words/$id").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                if (authToken != null) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val jsonObj = JSONObject(json)

                    val name = jsonObj.optString("name", "Pack $id")
                    val updated = jsonObj.optString("updated_at", "")
                    val qLang = jsonObj.optString("question_lang", "")
                    val aLang = jsonObj.optString("answer_lang", "")
                    val author = jsonObj.optString("author", "")
                    val dlCount = jsonObj.optInt("download_count", 0)
                    val wordsArray = jsonObj.getJSONArray("words")
                    val words = mutableListOf<Word>()

                    for (i in 0 until wordsArray.length()) {
                        val w = wordsArray.getJSONObject(i)
                        words.add(
                            Word(
                                question = w.getString("question"),
                                answer = w.getString("answer"),
                                reading = w.optString("reading", ""),
                                audioUrl = w.optString("audio", ""),
                                imageUrl = w.optString("image", "")
                            )
                        )
                    }

                    val tags = jsonObj.optString("tags", "")
                    val verStatus = jsonObj.optString("verification_status", "none")
                    val avgRating = jsonObj.optDouble("avg_rating", 0.0).toFloat()
                    val ratingCount = jsonObj.optInt("rating_count", 0)
                    val pack = WordPack(id = id, name = name, updated = updated, words = words, questionLang = qLang, answerLang = aLang, author = author, downloadCount = dlCount, tags = tags, verificationStatus = verStatus, avgRating = avgRating, ratingCount = ratingCount)
                    WordStorage.savePack(context, pack)
                    AudioCache.downloadPackAudio(context, words)
                    ImageCache.downloadPackImages(context, words)

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
