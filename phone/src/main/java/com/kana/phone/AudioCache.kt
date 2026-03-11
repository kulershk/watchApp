package com.kana.phone

import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AudioCache {

    private const val AUDIO_BASE = "https://watch.osrs.lv/api/audio/"

    private fun cacheDir(context: Context): File {
        val dir = File(context.cacheDir, "audio")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun buildUrl(audioFile: String): String {
        if (audioFile.isBlank()) return ""
        return "$AUDIO_BASE$audioFile"
    }

    fun getCachedFile(context: Context, audioFile: String): File? {
        if (audioFile.isBlank()) return null
        val file = File(cacheDir(context), audioFile)
        return if (file.exists()) file else null
    }

    fun download(context: Context, audioFile: String) {
        if (audioFile.isBlank()) return
        val file = File(cacheDir(context), audioFile)
        if (file.exists()) return

        try {
            val url = buildUrl(audioFile)
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (_: Exception) {
            file.delete()
        }
    }

    fun play(context: Context, audioFile: String) {
        val file = getCachedFile(context, audioFile) ?: return
        try {
            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener { it.release() }
            player.prepare()
            player.start()
        } catch (_: Exception) {}
    }

    /** Delete cached audio files for a list of filenames */
    fun deleteFiles(context: Context, audioFiles: List<String>) {
        val dir = cacheDir(context)
        for (filename in audioFiles) {
            if (filename.isBlank()) continue
            val file = File(dir, filename)
            if (file.exists()) file.delete()
        }
    }

    /** Delete all cached audio for a pack's words */
    fun deletePackAudio(context: Context, words: List<Word>) {
        deleteFiles(context, words.map { it.audioUrl })
    }

    /** Delete old audio files that are no longer in the new word list */
    fun cleanOldAudio(context: Context, oldWords: List<Word>, newWords: List<Word>) {
        val newAudioFiles = newWords.map { it.audioUrl }.filter { it.isNotBlank() }.toSet()
        val toDelete = oldWords.map { it.audioUrl }.filter { it.isNotBlank() && it !in newAudioFiles }
        deleteFiles(context, toDelete)
    }

    fun downloadPackAudio(context: Context, words: List<Word>) {
        for (word in words) {
            download(context, word.audioUrl)
        }
    }
}
