package com.kana.phone

import android.content.Context

object AppSettings {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_INTERVAL = "notification_interval_minutes"
    private const val KEY_NOTIFICATIONS_ACTIVE = "notifications_active"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_ENABLED_PACKS = "enabled_packs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_FRIEND_CODE = "friend_code"
    private const val KEY_DISPLAY_NAME = "display_name"

    private const val DEFAULT_INTERVAL = 20
    private val DEFAULT_URL = "${BuildConfig.API_BASE}/words/"

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

    fun getEnabledPacks(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_ENABLED_PACKS, emptySet()) ?: emptySet()
    }

    fun setEnabledPacks(context: Context, ids: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_ENABLED_PACKS, ids).apply()
        if (isLoggedIn(context)) {
            ApiClient.pushWatchSyncPacks(context, ids)
        }
    }

    fun getAuthToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun setAuthToken(context: Context, token: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getUserEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun setUserEmail(context: Context, email: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getFriendCode(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FRIEND_CODE, null)
    }

    fun setFriendCode(context: Context, code: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FRIEND_CODE, code).apply()
    }

    fun getDisplayName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DISPLAY_NAME, null)
    }

    fun setDisplayName(context: Context, name: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISPLAY_NAME, name).apply()
    }

    fun getBrowseVerifiedOnly(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("browse_verified_only", true)
    }

    fun setBrowseVerifiedOnly(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("browse_verified_only", value).apply()
    }

    fun isLoggedIn(context: Context): Boolean = getAuthToken(context) != null

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_AUTH_TOKEN).remove(KEY_USER_EMAIL).remove(KEY_FRIEND_CODE).remove(KEY_DISPLAY_NAME).apply()
    }
}
