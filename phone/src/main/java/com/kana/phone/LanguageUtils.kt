package com.kana.phone

val languageFlags = mapOf(
    "Japanese" to "\uD83C\uDDEF\uD83C\uDDF5",
    "English" to "\uD83C\uDDEC\uD83C\uDDE7",
    "Spanish" to "\uD83C\uDDEA\uD83C\uDDF8",
    "French" to "\uD83C\uDDEB\uD83C\uDDF7",
    "German" to "\uD83C\uDDE9\uD83C\uDDEA",
    "Korean" to "\uD83C\uDDF0\uD83C\uDDF7",
    "Chinese" to "\uD83C\uDDE8\uD83C\uDDF3",
    "Russian" to "\uD83C\uDDF7\uD83C\uDDFA",
    "Portuguese" to "\uD83C\uDDF5\uD83C\uDDF9",
    "Italian" to "\uD83C\uDDEE\uD83C\uDDF9",
    "Arabic" to "\uD83C\uDDF8\uD83C\uDDE6",
    "Hindi" to "\uD83C\uDDEE\uD83C\uDDF3",
    "Turkish" to "\uD83C\uDDF9\uD83C\uDDF7",
    "Vietnamese" to "\uD83C\uDDFB\uD83C\uDDF3",
    "Thai" to "\uD83C\uDDF9\uD83C\uDDED",
    "Indonesian" to "\uD83C\uDDEE\uD83C\uDDE9",
    "Dutch" to "\uD83C\uDDF3\uD83C\uDDF1",
    "Polish" to "\uD83C\uDDF5\uD83C\uDDF1",
    "Swedish" to "\uD83C\uDDF8\uD83C\uDDEA",
    "Other" to "\uD83C\uDF10"
)

fun langWithFlag(lang: String): String {
    val flag = languageFlags[lang]
    return if (flag != null) "$flag $lang" else lang
}

fun timeAgo(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(isoDate.replace("Z", "").take(19)) ?: return ""
        val diff = System.currentTimeMillis() - date.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> if (days == 1L) "1 day ago" else "$days days ago"
            weeks < 5 -> if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
            else -> if (months == 1L) "1 month ago" else "$months months ago"
        }
    } catch (_: Exception) { "" }
}
