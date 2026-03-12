package com.kana.watch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "kana_quiz_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val enabledWords = WordStorage.getEnabledWords(context)

        if (enabledWords.isEmpty()) {
            NotificationScheduler.schedule(context, AppSettings.getIntervalMinutes(context))
            return
        }

        val item = enabledWords.random()
        showWordNotification(context, item)
        NotificationScheduler.schedule(context, AppSettings.getIntervalMinutes(context))
    }

    private fun showWordNotification(context: Context, item: Word) {
        val quizIntent = Intent(context, QuizActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(QuizExtras.EXTRA_CHARACTER, item.question)
            putExtra(QuizExtras.EXTRA_ROMAJI, item.answer)
            putExtra(QuizExtras.EXTRA_TYPE, "WORD")
            putExtra(QuizExtras.EXTRA_READING, item.reading)
            putExtra(QuizExtras.EXTRA_AUDIO_URL, item.audioUrl)
            putExtra(QuizExtras.EXTRA_IMAGE_URL, item.imageUrl)
        }

        sendNotification(
            context,
            title = "Language Learning!",
            text = "What does ${item.question} mean?",
            intent = quizIntent
        )
    }

    private fun sendNotification(context: Context, title: String, text: String, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Language Learning",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Periodic quiz notifications"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

}
