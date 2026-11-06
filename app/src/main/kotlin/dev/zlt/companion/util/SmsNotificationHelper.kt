package dev.zlt.companion.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.zlt.companion.R
import dev.zlt.companion.data.SmsItem
import dev.zlt.companion.ui.MainActivity

object SmsNotificationHelper {
    private const val CHANNEL_ID = "vemo_sms_channel"
    private const val CHANNEL_NAME = "VEMO Messages"
    private var isChannelCreated = false

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for incoming SMS messages on VEMO router"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            isChannelCreated = true
        }
    }

    fun showNewSmsNotification(context: Context, sms: SmsItem) {
        if (!isChannelCreated) {
            createChannel(context)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TARGET_TAB, "SMS")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            sms.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (sms.number.isNotEmpty()) "New SMS • ${sms.number}" else "New SMS"
        val body = sms.decodedContent.ifEmpty { "(No text content)" }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_sms)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(sms.id.hashCode(), notification)
    }
}
