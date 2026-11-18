package com.sigha.converter.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sigha.converter.R

/**
 * Data-sync foreground service that keeps a running batch alive when the app is backgrounded.
 *
 * The service is owned by [ConversionQueue]: it is started when a batch starts, the notification
 * is updated with the current file and progress, and it is stopped when the batch ends. If the
 * process is killed the notification disappears with it; the queue's durable flag tells the next
 * launch that the batch was interrupted.
 *
 * The service never does conversion work itself - that stays in the app-scoped coroutine - it
 * only gives Android a reason to keep the process alive and shows progress to the user.
 */
class ConversionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: getString(R.string.ui_service_preparing)
        try {
            startForeground(NOTIFICATION_ID, buildNotification(text, null))
        } catch (_: Exception) {
            // A foreground service can be refused (missing permission or OS restriction).
            // Conversion still runs in the process; only the notification is lost.
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    /**
     * Android 15+ timeout for a long-running dataSync foreground service (6 h in 24 h). The
     * conversion keeps running in the app process; releasing the foreground state here is what
     * keeps Android from throwing a RemoteServiceException when nobody handles the timeout.
     */
    override fun onTimeout(startId: Int) {
        stopSelf(startId)
    }

    /** Android 15+ calls this overload for dataSync; the one-argument form serves short services. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf(startId)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.ui_service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.ui_service_channel_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String, progress: Float?): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.ui_service_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setSilent(true)
        if (progress != null) {
            val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
            builder.setProgress(100, percent, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    companion object {
        private const val CHANNEL_ID = "conversion_progress"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.sigha.converter.action.CONVERSION_START"
        private const val ACTION_STOP = "com.sigha.converter.action.CONVERSION_STOP"
        private const val EXTRA_TEXT = "text"

        @Volatile
        private var instance: ConversionService? = null

        /** Starts the foreground service; a refusal never fails the conversion itself. */
        fun start(context: Context) {
            runCatching {
                val intent = Intent(context, ConversionService::class.java).setAction(ACTION_START)
                ContextCompat.startForegroundService(context, intent)
            }
        }

        /** Updates the ongoing notification; a no-op when the service is not running. */
        fun update(context: Context, text: String, progress: Float?) {
            val service = instance ?: return
            runCatching {
                val manager = context.getSystemService(NotificationManager::class.java) ?: return
                manager.notify(NOTIFICATION_ID, service.buildNotification(text, progress))
            }
        }

        /** Stops the foreground service and removes its notification. */
        fun stop(context: Context) {
            val service = instance
            if (service != null) {
                runCatching { service.stopForeground(STOP_FOREGROUND_REMOVE) }
                runCatching { service.stopSelf() }
                instance = null
            } else {
                runCatching {
                    context.startService(
                        Intent(context, ConversionService::class.java).setAction(ACTION_STOP)
                    )
                }
            }
        }
    }
}
