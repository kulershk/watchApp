package com.kana.phone

import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class RemotePack(
    val token: String,
    val name: String,
    val wordCount: Int,
    val updatedAt: String,
    val isPublic: Boolean = false,
    val tags: String = "",
    val author: String = "",
    val questionLang: String = "",
    val answerLang: String = "",
    val downloadCount: Int = 0,
    val isOwner: Boolean = true
)

data class Collaborator(
    val id: Int,
    val displayName: String,
    val friendCode: String
)

data class EditWord(
    var question: String = "",
    var answer: String = "",
    var reading: String = "",
    var audio: String = "",
    var enabled: Boolean = true
)

data class EditPack(
    val token: String,
    val name: String,
    val words: List<EditWord>,
    val isPublic: Boolean = false,
    val tags: String = "",
    val questionLang: String = "",
    val answerLang: String = ""
)

object ApiClient {

    private const val BASE = "https://watch.osrs.lv/api"

    private fun addAuth(connection: HttpURLConnection, context: Context) {
        val token = AppSettings.getAuthToken(context)
        if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
    }

    // ============ AUTH ============

    fun register(email: String, password: String, context: Context, onResult: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/auth/register").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val body = JSONObject()
                body.put("email", email)
                body.put("password", password)
                OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

                val responseCode = connection.responseCode
                val json = if (responseCode in 200..299)
                    connection.inputStream.bufferedReader().readText()
                else
                    connection.errorStream?.bufferedReader()?.readText() ?: ""

                val obj = JSONObject(json)
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    AppSettings.setAuthToken(context, obj.getString("token"))
                    val user = obj.getJSONObject("user")
                    AppSettings.setUserEmail(context, user.getString("email"))
                    AppSettings.setFriendCode(context, user.optString("friendCode", ""))
                    withContext(Dispatchers.Main) { onResult(true, "Registered") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, obj.optString("error", "Registration failed")) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Failed: ${e.message}") }
            }
        }
    }

    fun login(email: String, password: String, context: Context, onResult: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/auth/login").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val body = JSONObject()
                body.put("email", email)
                body.put("password", password)
                OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

                val responseCode = connection.responseCode
                val json = if (responseCode in 200..299)
                    connection.inputStream.bufferedReader().readText()
                else
                    connection.errorStream?.bufferedReader()?.readText() ?: ""

                val obj = JSONObject(json)
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    AppSettings.setAuthToken(context, obj.getString("token"))
                    val user = obj.getJSONObject("user")
                    AppSettings.setUserEmail(context, user.getString("email"))
                    AppSettings.setFriendCode(context, user.optString("friendCode", ""))
                    withContext(Dispatchers.Main) { onResult(true, "Logged in") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, obj.optString("error", "Login failed")) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Failed: ${e.message}") }
            }
        }
    }

    fun loginWithGoogle(idToken: String, context: Context, onResult: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/auth/google").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val body = JSONObject()
                body.put("idToken", idToken)
                OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

                val responseCode = connection.responseCode
                val json = if (responseCode in 200..299)
                    connection.inputStream.bufferedReader().readText()
                else
                    connection.errorStream?.bufferedReader()?.readText() ?: ""

                val obj = JSONObject(json)
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    AppSettings.setAuthToken(context, obj.getString("token"))
                    val user = obj.getJSONObject("user")
                    AppSettings.setUserEmail(context, user.getString("email"))
                    AppSettings.setFriendCode(context, user.optString("friendCode", ""))
                    withContext(Dispatchers.Main) { onResult(true, "Signed in with Google") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, obj.optString("error", "Google sign-in failed")) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Failed: ${e.message}") }
            }
        }
    }

    // ============ WATCH PAIRING ============

    fun requestPairCode(context: Context, onResult: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/watch/pair-code").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                addAuth(connection, context)

                OutputStreamWriter(connection.outputStream).use { it.write("{}") }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val obj = JSONObject(json)
                    val code = obj.getString("code")
                    withContext(Dispatchers.Main) { onResult(true, code) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Server error") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Failed: ${e.message}") }
            }
        }
    }

    // ============ PACKS ============

    fun fetchPackList(context: Context, onResult: (Boolean, List<RemotePack>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/packs").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                addAuth(connection, context)

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val arr = JSONArray(json)
                    val packs = mutableListOf<RemotePack>()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        packs.add(
                            RemotePack(
                                token = obj.getString("token"),
                                name = obj.optString("name", ""),
                                wordCount = obj.optInt("word_count", 0),
                                updatedAt = obj.optString("updated_at", ""),
                                isPublic = obj.optBoolean("is_public", false),
                                tags = obj.optString("tags", ""),
                                questionLang = obj.optString("question_lang", ""),
                                answerLang = obj.optString("answer_lang", ""),
                                downloadCount = obj.optInt("download_count", 0),
                                isOwner = obj.optBoolean("is_owner", true)
                            )
                        )
                    }

                    withContext(Dispatchers.Main) { onResult(true, packs) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, emptyList()) }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(false, emptyList()) }
            }
        }
    }

    fun fetchPublicPacks(search: String = "", tag: String = "", questionLang: String = "", answerLang: String = "", onResult: (Boolean, List<RemotePack>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val params = mutableListOf<String>()
                if (search.isNotBlank()) params.add("search=${java.net.URLEncoder.encode(search, "UTF-8")}")
                if (tag.isNotBlank()) params.add("tag=${java.net.URLEncoder.encode(tag, "UTF-8")}")
                if (questionLang.isNotBlank()) params.add("question_lang=${java.net.URLEncoder.encode(questionLang, "UTF-8")}")
                if (answerLang.isNotBlank()) params.add("answer_lang=${java.net.URLEncoder.encode(answerLang, "UTF-8")}")
                val queryString = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""

                val connection = URL("$BASE/packs/browse$queryString").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val arr = JSONArray(json)
                    val packs = mutableListOf<RemotePack>()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        packs.add(
                            RemotePack(
                                token = obj.getString("token"),
                                name = obj.optString("name", ""),
                                wordCount = obj.optInt("word_count", 0),
                                updatedAt = obj.optString("updated_at", ""),
                                tags = obj.optString("tags", ""),
                                author = obj.optString("author", ""),
                                questionLang = obj.optString("question_lang", ""),
                                answerLang = obj.optString("answer_lang", ""),
                                downloadCount = obj.optInt("download_count", 0)
                            )
                        )
                    }

                    withContext(Dispatchers.Main) { onResult(true, packs) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, emptyList()) }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(false, emptyList()) }
            }
        }
    }

    fun fetchPackForEdit(token: String, context: Context, onResult: (Boolean, EditPack?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/packs/$token/edit").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                addAuth(connection, context)

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val obj = JSONObject(json)
                    val name = obj.optString("name", "")
                    val wordsArr = obj.getJSONArray("words")
                    val words = mutableListOf<EditWord>()

                    for (i in 0 until wordsArr.length()) {
                        val w = wordsArr.getJSONObject(i)
                        words.add(
                            EditWord(
                                question = w.optString("question", ""),
                                answer = w.optString("answer", ""),
                                reading = w.optString("reading", ""),
                                audio = w.optString("audio", ""),
                                enabled = w.optBoolean("enabled", true)
                            )
                        )
                    }

                    val isPublic = obj.optBoolean("is_public", false)
                    val packTags = obj.optString("tags", "")
                    val questionLang = obj.optString("question_lang", "")
                    val answerLang = obj.optString("answer_lang", "")

                    withContext(Dispatchers.Main) {
                        onResult(true, EditPack(token, name, words, isPublic, packTags, questionLang, answerLang))
                    }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, null) }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(false, null) }
            }
        }
    }

    fun createPack(name: String, words: List<EditWord>, context: Context, isPublic: Boolean = false, tags: String = "", questionLang: String = "", answerLang: String = "", onResult: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/packs").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                addAuth(connection, context)

                val body = JSONObject()
                body.put("name", name)
                body.put("is_public", isPublic)
                body.put("tags", tags)
                body.put("question_lang", questionLang)
                body.put("answer_lang", answerLang)
                val wordsArr = JSONArray()
                for (w in words) {
                    val wo = JSONObject()
                    wo.put("question", w.question)
                    wo.put("answer", w.answer)
                    wo.put("reading", w.reading)
                    wo.put("audio", w.audio)
                    wo.put("enabled", w.enabled)
                    wordsArr.put(wo)
                }
                body.put("words", wordsArr)

                OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

                if (connection.responseCode == HttpURLConnection.HTTP_OK ||
                    connection.responseCode == HttpURLConnection.HTTP_CREATED) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val obj = JSONObject(json)
                    val token = obj.optString("token", "")
                    withContext(Dispatchers.Main) { onResult(true, token) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Server error") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Failed: ${e.message}") }
            }
        }
    }

    fun savePack(token: String, name: String, words: List<EditWord>, context: Context, isPublic: Boolean = false, tags: String = "", questionLang: String = "", answerLang: String = "", onResult: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/packs/$token").openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                addAuth(connection, context)

                val body = JSONObject()
                body.put("name", name)
                body.put("is_public", isPublic)
                body.put("tags", tags)
                body.put("question_lang", questionLang)
                body.put("answer_lang", answerLang)
                val wordsArr = JSONArray()
                for (w in words) {
                    val wo = JSONObject()
                    wo.put("question", w.question)
                    wo.put("answer", w.answer)
                    wo.put("reading", w.reading)
                    wo.put("audio", w.audio)
                    wo.put("enabled", w.enabled)
                    wordsArr.put(wo)
                }
                body.put("words", wordsArr)

                OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) { onResult(true, "Saved") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Server error") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Failed: ${e.message}") }
            }
        }
    }

    fun uploadAudio(file: File, context: Context, onResult: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bytes = file.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                val connection = URL("$BASE/audio").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                addAuth(connection, context)

                val body = JSONObject()
                body.put("data", "data:audio/mp4;base64,$base64")
                OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

                if (connection.responseCode == HttpURLConnection.HTTP_OK ||
                    connection.responseCode == HttpURLConnection.HTTP_CREATED) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val obj = JSONObject(json)
                    val filename = obj.optString("filename", "")
                    withContext(Dispatchers.Main) { onResult(true, filename) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Upload failed") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Upload failed: ${e.message}") }
            }
        }
    }

    fun deleteAudio(filename: String, context: Context, onResult: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/audio/$filename").openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                addAuth(connection, context)
                val success = connection.responseCode == HttpURLConnection.HTTP_OK ||
                        connection.responseCode == HttpURLConnection.HTTP_NO_CONTENT
                onResult?.let { withContext(Dispatchers.Main) { it(success) } }
            } catch (_: Exception) {
                onResult?.let { withContext(Dispatchers.Main) { it(false) } }
            }
        }
    }

    fun deletePack(token: String, context: Context, onResult: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/packs/$token").openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                addAuth(connection, context)

                if (connection.responseCode == HttpURLConnection.HTTP_OK ||
                    connection.responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    withContext(Dispatchers.Main) { onResult(true, "Deleted") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Server error") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Failed: ${e.message}") }
            }
        }
    }

    // ============ COLLABORATORS ============

    fun fetchCollaborators(token: String, context: Context, onResult: (Boolean, List<Collaborator>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/packs/$token/collaborators").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                addAuth(connection, context)

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val arr = JSONArray(json)
                    val collabs = mutableListOf<Collaborator>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        collabs.add(Collaborator(
                            id = obj.getInt("id"),
                            displayName = obj.optString("displayName", ""),
                            friendCode = obj.optString("friendCode", "")
                        ))
                    }
                    withContext(Dispatchers.Main) { onResult(true, collabs) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, emptyList()) }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(false, emptyList()) }
            }
        }
    }

    fun addCollaborator(token: String, friendCode: String, context: Context, onResult: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/packs/$token/collaborators").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                addAuth(connection, context)

                val body = JSONObject()
                body.put("friend_code", friendCode)
                OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) { onResult(true, "Added") }
                } else {
                    val errJson = connection.errorStream?.bufferedReader()?.readText() ?: ""
                    val msg = try { JSONObject(errJson).optString("error", "Failed") } catch (_: Exception) { "Failed" }
                    withContext(Dispatchers.Main) { onResult(false, msg) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Failed: ${e.message}") }
            }
        }
    }

    fun removeCollaborator(token: String, userId: Int, context: Context, onResult: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL("$BASE/packs/$token/collaborators/$userId").openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                addAuth(connection, context)

                val success = connection.responseCode == HttpURLConnection.HTTP_OK
                withContext(Dispatchers.Main) { onResult(success) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }
}
