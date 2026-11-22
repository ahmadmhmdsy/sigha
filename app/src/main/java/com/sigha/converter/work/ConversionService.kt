package com.sigha.converter.work

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sigha.converter.MainActivity
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

    /** Set once the queue stops the batch; late update() calls must not repost the notification. */
    @Volatile
    private var stopped = false

    /** True once onStartCommand reached startForeground(). */
    @Volatile
    private var foregroundReady = false

    override fun onCreate() {
        super.onCreate()
        stopped = false
        foregroundReady = false
        instance = this
        ensureChannel(this)
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
            // Conversion still runs in the process; only the notification is lost -- and any
            // notification left from an earlier run must not survive as a ghost.
            startPending = false
            pendingStop = false
            cancelNotification()
            stopSelf()
            return START_NOT_STICKY
        }
        foregroundReady = true
        startPending = false
        if (pendingStop) {
            // The batch finished while this start was still on its way (a corrupt file can fail
            // in milliseconds). startForeground() has now been called, so stopping is safe.
            pendingStop = false
            stopSelf()
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
        stopped = true
        foregroundReady = false
        startPending = false
        if (instance === this) instance = null
        cancelNotification()
        super.onDestroy()
    }

    private fun cancelNotification() {
        runCatching {
            getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        }
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
        launchIntent(this)?.let(builder::setContentIntent)
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
        private const val NOTIFICATION_RESULT_ID = 1002
        private const val ACTION_START = "com.sigha.converter.action.CONVERSION_START"
        private const val ACTION_STOP = "com.sigha.converter.action.CONVERSION_STOP"
        private const val EXTRA_TEXT = "text"

        @Volatile
        private var instance: ConversionService? = null

        /**
         * True between startForegroundService() and the service reaching startForeground().
         * A batch that fails fast (a corrupt file retried right after it was picked) can end
         * before onCreate/onStartCommand run; stopping the service at that moment cancels the
         * pending start, leaves Android's "startForegroundService() did not then call
         * startForeground()" requirement unmet and kills the app about five seconds later.
         */
        @Volatile
        private var startPending = false

        /** Set when a batch ends while [startPending]: onStartCommand stops the service itself. */
        @Volatile
        private var pendingStop = false

        /** Starts the foreground service; a refusal never fails the conversion itself. */
        fun start(context: Context) {
            try {
                startPending = true
                pendingStop = false
                val intent = Intent(context, ConversionService::class.java).setAction(ACTION_START)
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                // Refused (background-start restriction, missing permission): the conversion still
                // runs in the process and no startForeground() requirement was ever created.
                startPending = false
                pendingStop = false
            }
        }

        /**
         * Stops the service even when [instance] is still null (a stop pressed in the window
         * between startForegroundService and onCreate). startService(ACTION_STOP) would throw
         * from the background and leave the service, and its notification, running forever;
         * stopService is allowed from the background.
         */
        private fun stopNow(context: Context) {
            runCatching {
                context.stopService(Intent(context, ConversionService::class.java))
            }
        }

        /** Updates the ongoing notification; a no-op when the service is not running. */
        fun update(context: Context, text: String, progress: Float?) {
            val service = instance ?: return
            if (service.stopped) return
            runCatching {
                val manager = context.getSystemService(NotificationManager::class.java) ?: return
                manager.notify(NOTIFICATION_ID, service.buildNotification(text, progress))
            }
        }

        /**
         * Posts the one-shot "finished" notification. It deliberately outlives the foreground
         * service: the progress notification disappears with the service, and a user who left
         * the app while a batch ran would otherwise never learn that it finished. Same channel
         * (low importance, silent) so it never makes noise, but it stays in the shade until
         * tapped or dismissed.
         */
        @SuppressLint("MissingPermission")
        fun notifyBatchFinished(context: Context, doneCount: Int, failedCount: Int) {
            if (doneCount <= 0 && failedCount <= 0) return
            runCatching {
                ensureChannel(context)
                val manager = context.getSystemService(NotificationManager::class.java)
                    ?: return@runCatching
                val title = context.getString(
                    if (doneCount > 0) R.string.ui_service_result_title
                    else R.string.ui_service_result_failed_title
                )
                val doneText = if (doneCount > 0) {
                    context.resources.getQuantityString(
                        R.plurals.ui_service_result_done,
                        doneCount,
                        doneCount
                    )
                } else {
                    ""
                }
                val failedText = if (failedCount > 0) {
                    context.resources.getQuantityString(
                        R.plurals.ui_service_result_failed,
                        failedCount,
                        failedCount
                    )
                } else {
                    ""
                }
                val text = when {
                    doneText.isNotEmpty() && failedText.isNotEmpty() -> doneText + " · " + failedText
                    doneText.isNotEmpty() -> doneText
                    else -> failedText
                }
                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setSilent(true)
                launchIntent(context)?.let(builder::setContentIntent)
                manager.notify(NOTIFICATION_RESULT_ID, builder.build())
            }
        }

        /** Removes the previous batch's finished notification when a new batch starts. */
        fun clearResult(context: Context) {
            runCatching {
                context.getSystemService(NotificationManager::class.java)
                    ?.cancel(NOTIFICATION_RESULT_ID)
            }
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.ui_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.ui_service_channel_description)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }

        /**
         * Tapping the progress or result notification must bring the app back: a user who
         * starts a batch and leaves the screen otherwise has no way to see results or cancel,
         * and taps do nothing.
         */
        private fun launchIntent(context: Context): PendingIntent? = runCatching {
            val launch = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            PendingIntent.getActivity(
                context,
                0,
                launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }.getOrNull()

        /** Stops the foreground service and removes its notification. */
        fun stop(context: Context) {
            if (startPending) {
                // The start request has not reached startForeground() yet: stopping now would
                // cancel it and the OS would kill the app five seconds later. onStartCommand
                // honours this request right after startForeground().
                pendingStop = true
                // onStartCommand may have completed between the check and the flag write; then
                // nobody is left to read the request and the stop has to be finished here.
                if (startPending) return
                pendingStop = false
            }
            val service = instance
            if (service != null) {
                service.stopped = true
                runCatching { service.stopForeground(STOP_FOREGROUND_REMOVE) }
                runCatching { service.cancelNotification() }
                runCatching { service.stopSelf() }
                instance = null
            } else {
                // No instance yet (or already destroyed): stopService plus an explicit cancel
                // clears a notification posted by a start whose onCreate never ran.
                stopNow(context)
                runCatching {
                    context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
                }
            }
        }
    }
}
