package com.kana.watch

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Word(
    val question: String,
    val answer: String,
    val reading: String = "",
    val audioUrl: String = "",
    val imageUrl: String = ""
)

data class WordPack(
    val id: String,
    val name: String,
    val updated: String,
    val words: List<Word>,
    val questionLang: String = "",
    val answerLang: String = "",
    val author: String = "",
    val downloadCount: Int = 0
)

object WordStorage {

    private const val PREFS_NAME = "word_packs"
    private const val KEY_PACKS = "packs"

    fun savePack(context: Context, pack: WordPack) {
        val existing = loadAllPacks(context).toMutableList()
        val oldPack = existing.find { it.id == pack.id }
        if (oldPack != null) {
            AudioCache.cleanOldAudio(context, oldPack.words, pack.words)
            ImageCache.cleanOldImages(context, oldPack.words, pack.words)
        }
        existing.removeAll { it.id == pack.id }
        existing.add(pack)
        saveAll(context, existing)
    }

    fun loadAllPacks(context: Context): List<WordPack> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PACKS, null) ?: return emptyList()

        val packs = mutableListOf<WordPack>()
        val jsonArray = JSONArray(json)

        for (i in 0 until jsonArray.length()) {
            val packObj = jsonArray.getJSONObject(i)
            val wordsArray = packObj.getJSONArray("words")
            val words = mutableListOf<Word>()

            for (j in 0 until wordsArray.length()) {
                val wordObj = wordsArray.getJSONObject(j)
                words.add(
                    Word(
                        question = wordObj.getString("question"),
                        answer = wordObj.getString("answer"),
                        reading = wordObj.optString("reading", ""),
                        audioUrl = wordObj.optString("audio", ""),
                        imageUrl = wordObj.optString("image", "")
                    )
                )
            }

            val packId = packObj.optString("id", "").ifEmpty { packObj.optString("token", "") }
            packs.add(
                WordPack(
                    id = packId,
                    name = packObj.optString("name", "Pack $packId"),
                    updated = packObj.optString("updated", ""),
                    words = words,
                    questionLang = packObj.optString("question_lang", ""),
                    answerLang = packObj.optString("answer_lang", ""),
                    author = packObj.optString("author", ""),
                    downloadCount = packObj.optInt("download_count", 0)
                )
            )
        }

        return packs
    }

    fun getEnabledWords(context: Context): List<Word> {
        val enabledIds = AppSettings.getEnabledPacks(context)
        return loadAllPacks(context)
            .filter { it.id in enabledIds }
            .flatMap { it.words }
    }

    fun deletePack(context: Context, id: String) {
        val packs = loadAllPacks(context).toMutableList()
        val pack = packs.find { it.id == id }
        if (pack != null) {
            AudioCache.deletePackAudio(context, pack.words)
            ImageCache.deletePackImages(context, pack.words)
        }
        packs.removeAll { it.id == id }
        saveAll(context, packs)

        // Remove from enabled list too
        val enabled = AppSettings.getEnabledPacks(context).toMutableSet()
        enabled.remove(id)
        AppSettings.setEnabledPacks(context, enabled)
    }

    private fun saveAll(context: Context, packs: List<WordPack>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()

        for (p in packs) {
            val packObj = JSONObject()
            packObj.put("id", p.id.toInt())
            packObj.put("name", p.name)
            packObj.put("updated", p.updated)

            val wordsArray = JSONArray()
            for (w in p.words) {
                val wordObj = JSONObject()
                wordObj.put("question", w.question)
                wordObj.put("answer", w.answer)
                wordObj.put("reading", w.reading)
                wordObj.put("audio", w.audioUrl)
                wordObj.put("image", w.imageUrl)
                wordsArray.put(wordObj)
            }
            packObj.put("words", wordsArray)
            packObj.put("question_lang", p.questionLang)
            packObj.put("answer_lang", p.answerLang)
            packObj.put("author", p.author)
            packObj.put("download_count", p.downloadCount)
            jsonArray.put(packObj)
        }

        prefs.edit().putString(KEY_PACKS, jsonArray.toString()).apply()
    }
}
