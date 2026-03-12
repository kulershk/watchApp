package com.kana.watch

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ImageCache {

    private const val IMAGE_BASE = "https://watch.osrs.lv/api/images/"

    private fun cacheDir(context: Context): File {
        val dir = File(context.cacheDir, "images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCachedFile(context: Context, imageFile: String): File? {
        if (imageFile.isBlank()) return null
        val file = File(cacheDir(context), imageFile)
        return if (file.exists()) file else null
    }

    fun download(context: Context, imageFile: String) {
        if (imageFile.isBlank()) return
        val file = File(cacheDir(context), imageFile)
        if (file.exists()) return

        try {
            val url = "$IMAGE_BASE$imageFile"
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

    fun deleteFiles(context: Context, imageFiles: List<String>) {
        val dir = cacheDir(context)
        for (filename in imageFiles) {
            if (filename.isBlank()) continue
            val file = File(dir, filename)
            if (file.exists()) file.delete()
        }
    }

    fun deletePackImages(context: Context, words: List<Word>) {
        deleteFiles(context, words.map { it.imageUrl })
    }

    fun cleanOldImages(context: Context, oldWords: List<Word>, newWords: List<Word>) {
        val newImageFiles = newWords.map { it.imageUrl }.filter { it.isNotBlank() }.toSet()
        val toDelete = oldWords.map { it.imageUrl }.filter { it.isNotBlank() && it !in newImageFiles }
        deleteFiles(context, toDelete)
    }

    fun downloadPackImages(context: Context, words: List<Word>) {
        for (word in words) {
            download(context, word.imageUrl)
        }
    }
}
