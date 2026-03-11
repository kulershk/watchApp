package com.kana.phone

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Word(
    val question: String,
    val answer: String,
    val reading: String = "",
    val audioUrl: String = ""
)

data class WordPack(
    val token: String,
    val name: String,
    val updated: String,
    val words: List<Word>
)

object WordStorage {

    private const val PREFS_NAME = "word_packs"
    private const val KEY_PACKS = "packs"

    fun savePack(context: Context, pack: WordPack) {
        val existing = loadAllPacks(context).toMutableList()
        val oldPack = existing.find { it.token == pack.token }
        if (oldPack != null) {
            AudioCache.cleanOldAudio(context, oldPack.words, pack.words)
        }
        existing.removeAll { it.token == pack.token }
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
                        audioUrl = wordObj.optString("audio", "")
                    )
                )
            }

            packs.add(
                WordPack(
                    token = packObj.getString("token"),
                    name = packObj.optString("name", "Pack ${packObj.getString("token")}"),
                    updated = packObj.optString("updated", ""),
                    words = words
                )
            )
        }

        return packs
    }

    fun getEnabledWords(context: Context): List<Word> {
        val enabledTokens = AppSettings.getEnabledPacks(context)
        return loadAllPacks(context)
            .filter { it.token in enabledTokens }
            .flatMap { it.words }
    }

    fun deletePack(context: Context, token: String) {
        val packs = loadAllPacks(context).toMutableList()
        val pack = packs.find { it.token == token }
        if (pack != null) {
            AudioCache.deletePackAudio(context, pack.words)
        }
        packs.removeAll { it.token == token }
        saveAll(context, packs)

        val enabled = AppSettings.getEnabledPacks(context).toMutableSet()
        enabled.remove(token)
        AppSettings.setEnabledPacks(context, enabled)
    }

    private fun saveAll(context: Context, packs: List<WordPack>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()

        for (p in packs) {
            val packObj = JSONObject()
            packObj.put("token", p.token)
            packObj.put("name", p.name)
            packObj.put("updated", p.updated)

            val wordsArray = JSONArray()
            for (w in p.words) {
                val wordObj = JSONObject()
                wordObj.put("question", w.question)
                wordObj.put("answer", w.answer)
                wordObj.put("reading", w.reading)
                wordObj.put("audio", w.audioUrl)
                wordsArray.put(wordObj)
            }
            packObj.put("words", wordsArray)
            jsonArray.put(packObj)
        }

        prefs.edit().putString(KEY_PACKS, jsonArray.toString()).apply()
    }
}
