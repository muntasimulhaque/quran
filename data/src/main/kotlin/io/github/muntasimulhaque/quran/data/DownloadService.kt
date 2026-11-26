package io.github.muntasimulhaque.quran.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The one transfer that keeps going when the window goes away.
 *
 * The download used to live in the view model's scope. A reader who tapped
 * Download on Al-Baqarah and switched to WhatsApp could lose the 186 MB
 * transfer with the process, and the recitation downloader would then begin
 * again at zero. The service is a dataSync foreground service: the reader's tap
 * starts it, the notification carries progress and Cancel, and it stops when
 * the queue is empty (owner decision, 23.1).
 *
 * The service does not download anything itself. [DownloadCenter] owns the
 * queue and the downloaders; this class owns the Android obligation to stay
 * alive, the notification, and the cancel action. A process death keeps the
 * part file, and [DownloadSpec] lets a redelivered start pick the same job up.
 */
class DownloadService : Service() {

    private val main = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foreground = false

    /**
     * The notification follows the reader's language, the same way the
     * recitation notification does: the service's own resources decide the
     * channel name, the title, and the cancel word (owner decision, 23.1).
     */
    override fun attachBaseContext(newBase: Context) {
        val tag = LanguagePreference(newBase).tag() ?: UiLanguage.ofSystem().tag
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        main.launch {
            DownloadCenter.state.collect { state -> render(state) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!startForegroundNotice()) return START_NOT_STICKY
                val id = intent.getStringExtra(EXTRA_ID).orEmpty()
                DownloadCenter.onServiceStart(this, id) {
                    if (!DownloadCenter.hasWork()) stopSelf()
                }
            }
            ACTION_CANCEL -> {
                intent.getStringExtra(EXTRA_ID)?.let { id ->
                    DownloadCenter.cancel(this, id)
                }
                if (!DownloadCenter.hasWork()) stopSelf()
            }
        }
        // A process that dies mid-transfer is restarted with the same intent.
        // The spec on disk and the part file are the state; the queue is not.
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        main.cancel()
        if (foreground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foreground = false
        }
        super.onDestroy()
    }

    private fun startForegroundNotice(): Boolean {
        val state = DownloadCenter.state.value
        val notification = notification(state)
        val started = runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                },
            )
        }.onFailure { failure ->
            android.util.Log.w(TAG, "the download service could not enter the foreground", failure)
        }.isSuccess
        foreground = started
        if (!started) {
            DownloadCenter.abortQueue()
            DownloadSpec.clear(this)
            stopSelf()
        }
        return started
    }

    /**
     * The notification is the state, not a second progress store. A queued job
     * says it is preparing; a running one says the title and the percent; a
     * terminal state removes the shade entry and lets the queue move on.
     */
    private fun render(state: DownloadState) {
        if (state.phase == DownloadPhase.Idle) return
        if (state.phase == DownloadPhase.Done ||
            state.phase == DownloadPhase.Failed ||
            state.phase == DownloadPhase.Cancelled
        ) {
            if (foreground) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                foreground = false
            }
            return
        }
        if (!foreground && !startForegroundNotice()) return
        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification(state))
        }.onFailure { failure ->
            android.util.Log.w(TAG, "the download notification could not be posted", failure)
        }
    }

    private fun notification(state: DownloadState): Notification {
        val title = state.title.ifBlank { getString(R.string.download_notification_title) }
        val text = if (state.phase == DownloadPhase.Queued || state.total <= 0) {
            getString(R.string.download_notification_preparing, title)
        } else {
            getString(R.string.download_notification_progress, title, state.percent)
        }
        val cancel = PendingIntent.getService(
            this,
            state.id?.hashCode() ?: 0,
            Intent(this, DownloadService::class.java)
                .setAction(ACTION_CANCEL)
                .putExtra(EXTRA_ID, state.id.orEmpty()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = packageManager.getLaunchIntentForPackage(packageName)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_mark)
            .setContentTitle(getString(R.string.download_notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(
                100,
                state.percent,
                state.phase == DownloadPhase.Queued || state.total <= 0,
            )
            .addAction(0, getString(R.string.download_notification_cancel), cancel)
        if (open != null) {
            builder.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        return builder.build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.download_channel_description)
                setShowBadge(false)
            },
        )
    }

    companion object {
        private const val TAG = "DownloadService"
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 3
        private const val ACTION_START = "io.github.muntasimulhaque.quran.action.DOWNLOAD_START"
        private const val ACTION_CANCEL = "io.github.muntasimulhaque.quran.action.DOWNLOAD_CANCEL"
        private const val EXTRA_ID = "download_id"

        /** True when the platform let the service start. A background start
         * the phone refuses is a failed transfer, not an exception on its way
         * out of a settings row. */
        fun start(context: Context, id: String): Boolean {
            val intent = Intent(context, DownloadService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_ID, id)
            return runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure { failure ->
                android.util.Log.w(TAG, "the download service could not start", failure)
            }.isSuccess
        }

        fun cancel(context: Context, id: String) {
            val intent = Intent(context, DownloadService::class.java)
                .setAction(ACTION_CANCEL)
                .putExtra(EXTRA_ID, id)
            runCatching { context.startService(intent) }
        }
    }
}
