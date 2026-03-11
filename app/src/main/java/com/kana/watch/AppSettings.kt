package com.kana.watch

import android.content.Context

object AppSettings {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_INTERVAL = "notification_interval_minutes"
    private const val KEY_NOTIFICATIONS_ACTIVE = "notifications_active"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_HIRAGANA_ENABLED = "hiragana_enabled"
    private const val KEY_KATAKANA_ENABLED = "katakana_enabled"
    private const val KEY_ENABLED_PACKS = "enabled_packs"

    private const val DEFAULT_INTERVAL = 20
    private const val DEFAULT_URL = "https://watch.osrs.lv/api/words/"

    fun getIntervalMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_INTERVAL, DEFAULT_INTERVAL)
    }

    fun setIntervalMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_INTERVAL, minutes).apply()
    }

    fun isNotificationsActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATIONS_ACTIVE, false)
    }

    fun setNotificationsActive(context: Context, active: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ACTIVE, active).apply()
    }

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BASE_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }

    fun isHiraganaEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HIRAGANA_ENABLED, true)
    }

    fun setHiraganaEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HIRAGANA_ENABLED, enabled).apply()
    }

    fun isKatakanaEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_KATAKANA_ENABLED, true)
    }

    fun setKatakanaEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_KATAKANA_ENABLED, enabled).apply()
    }

    fun getEnabledPacks(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_ENABLED_PACKS, emptySet()) ?: emptySet()
    }

    fun setEnabledPacks(context: Context, tokens: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_ENABLED_PACKS, tokens).apply()
    }
}
