package io.github.muntasimulhaque.quran.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** What one transfer is doing, for the service and the two surfaces that show it. */
enum class DownloadPhase {
    Idle,
    Queued,
    Running,
    Done,
    Failed,
    Cancelled,
}

/**
 * The one transfer on the phone, in the words the notification and the app's
 * progress lines read.
 *
 * [read] and [total] are bytes. [percent] is the same number the notification
 * draws, so a surface never has to round a fraction twice (owner decision,
 * 23.1).
 */
data class DownloadState(
    val id: String? = null,
    val title: String = "",
    val phase: DownloadPhase = DownloadPhase.Idle,
    val read: Long = 0,
    val total: Long = 0,
    val failure: DownloadError? = null,
) {
    val percent: Int
        get() = if (total > 0) ((read * 100) / total).toInt().coerceIn(0, 100) else 0

    val fraction: Float?
        get() = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else null
}

/** What one caller waits for after it asks for a transfer. */
data class DownloadResult(
    val id: String,
    val success: Boolean,
    val cancelled: Boolean = false,
    val failure: DownloadError? = null,
)

/**
 * The queue behind the download service.
 *
 * One transfer at a time was already the app's contract: two taps must not
 * fill one progress line, and a reader never asked for two files at once. The
 * queue is the same rule in a place that outlives the window. A job added while
 * another is running waits its turn; a cancel takes a waiting job out or ends
 * the running one, and the next job takes its place (owner decision, 23.1).
 *
 * The queue is in memory because a caller in this process is the only thing
 * that can show a result. The transfer spec is written to disk before it
 * starts, so a service the platform restarts can pick the same part file up
 * after a process death. The part file, not the queue, is the durable thing.
 */
object DownloadCenter {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(DownloadState())
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    private val lock = Any()
    private val queue = ArrayDeque<Entry>()
    private val waiters = ConcurrentHashMap<String, CompletableDeferred<DownloadResult>>()
    private var current: Entry? = null
    private var currentRun: Job? = null
    private var runner: Job? = null

    private data class Entry(val id: String, val job: DownloadJob)

    /**
     * Adds one job and starts the service. The id is the caller's handle for
     * progress and for the result; a duplicate request for the same job returns
     * the request already in flight rather than fetching it twice.
     */
    fun enqueue(context: Context, job: DownloadJob): String {
        val existing = synchronized(lock) {
            current?.takeIf { it.job == job }?.id
                ?: queue.firstOrNull { it.job == job }?.id
        }
        if (existing != null) return existing

        val id = UUID.randomUUID().toString()
        waiters[id] = CompletableDeferred()
        synchronized(lock) {
            queue.addLast(Entry(id, job))
            if (current == null) {
                _state.value = DownloadState(
                    id = id,
                    title = job.title(context, language(context)),
                    phase = DownloadPhase.Queued,
                    total = job.bytes,
                )
            }
        }
        if (!DownloadService.start(context, id)) {
            synchronized(lock) {
                queue.removeAll { it.id == id }
                if (current == null) {
                    _state.value = DownloadState(
                        id = id,
                        title = job.title(context, language(context)),
                        phase = DownloadPhase.Failed,
                        total = job.bytes,
                        failure = DownloadError.Other,
                    )
                }
            }
            complete(id, DownloadResult(id, success = false, failure = DownloadError.Other))
        }
        return id
    }

    /** Waits for one job. A result survives the state being replaced by the next job. */
    suspend fun await(id: String): DownloadResult =
        waiters[id]?.await() ?: DownloadResult(id, success = false, failure = DownloadError.Other)

    /** The service could not enter the foreground; every waiting job is answered. */
    internal fun abortQueue() {
        val entries = synchronized(lock) {
            val all = buildList {
                current?.let(::add)
                addAll(queue)
            }
            queue.clear()
            current = null
            currentRun?.cancel()
            currentRun = null
            all
        }
        entries.forEach { entry ->
            complete(entry.id, DownloadResult(entry.id, success = false, failure = DownloadError.Other))
        }
    }

    /**
     * The reader's Cancel. A waiting job leaves the queue; a running job is
     * cancelled where it stands. The part file is kept, because the next tap
     * is the same transfer and should not begin at zero.
     */
    fun cancel(context: Context, id: String) {
        var waiting = false
        synchronized(lock) {
            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().id == id) {
                    iterator.remove()
                    waiting = true
                }
            }
            if (current?.id == id) currentRun?.cancel()
        }
        if (waiting) {
            complete(id, DownloadResult(id, success = false, cancelled = true))
        }
        DownloadService.cancel(context, id)
    }

    /**
     * The service's door. It starts the queue once, and restores the job the
     * last process was carrying when the platform redelivers the service's
     * intent after a process death.
     */
    internal fun onServiceStart(
        context: Context,
        id: String,
        onIdle: () -> Unit,
    ) {
        synchronized(lock) {
            if (runner?.isActive == true) return
            val known = current?.id == id || queue.any { it.id == id }
            if (!known) {
                DownloadSpec.read(context)?.let { queue.addFirst(Entry(id, it)) }
            }
            runner = scope.launch { runQueue(context, onIdle) }
        }
    }

    private suspend fun runQueue(context: Context, onIdle: () -> Unit) {
        try {
            while (true) {
                val entry = synchronized(lock) {
                    queue.removeFirstOrNull()?.also { current = it }
                } ?: break
                DownloadSpec.write(context, entry.job)
                _state.value = DownloadState(
                    id = entry.id,
                    title = entry.job.title(context, language(context)),
                    phase = DownloadPhase.Running,
                    total = entry.job.bytes,
                )
                val result = runOne(context, entry)
                complete(entry.id, result)
                synchronized(lock) {
                    current = null
                    currentRun = null
                }
            }
        } finally {
            val more = synchronized(lock) {
                runner = null
                current = null
                currentRun = null
                queue.isNotEmpty()
            }
            if (!more) {
                DownloadSpec.clear(context)
                _state.update { state ->
                    if (state.phase == DownloadPhase.Running || state.phase == DownloadPhase.Queued) {
                        state.copy(phase = DownloadPhase.Idle)
                    } else {
                        state
                    }
                }
            } else {
                // A job landed between the last loop read and this finally.
                // The queue is the truth; start it again instead of stopping
                // the service with work still waiting.
                synchronized(lock) {
                    if (runner?.isActive != true) {
                        runner = scope.launch { runQueue(context, onIdle) }
                    }
                }
            }
            if (!more) onIdle()
        }
    }

    /** True while the service still has a job or a queue to finish. */
    fun hasWork(): Boolean = synchronized(lock) {
        current != null || queue.isNotEmpty() || runner?.isActive == true
    }

    /**
     * One job, on its own child so Cancel ends this transfer and lets the queue
     * move on. The child's cancellation is a result, not a failure to report.
     */
    private suspend fun runOne(context: Context, entry: Entry): DownloadResult {
        val result = CompletableDeferred<DownloadResult>()
        val work = scope.launch(start = CoroutineStart.LAZY) {
            val outcome = try {
                if (!DownloadStorage.hasRoom(context, entry.job)) {
                    DownloadResult(entry.id, success = false, failure = DownloadError.Storage)
                } else {
                    val finished = when (entry.job) {
                        is DownloadJob.Pack -> PackDownloader(context).download(entry.job.pack) { read, total ->
                            publish(entry, read, total)
                        }
                        is DownloadJob.Recitation -> RecitationDownloader(context).download(
                            entry.job.packageToFetch,
                            entry.job.folder,
                        ) { read, total ->
                            publish(entry, read, total)
                        }
                        is DownloadJob.WordAudio -> WordAudioDownloader(context).download(
                            entry.job.packageToFetch,
                        ) { read, total ->
                            publish(entry, read, total)
                        }
                    }
                    if (finished.isSuccess) {
                        DownloadResult(entry.id, success = true)
                    } else {
                        DownloadResult(
                            entry.id,
                            success = false,
                            failure = finished.exceptionOrNull()?.downloadError() ?: DownloadError.Other,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                DownloadResult(entry.id, success = false, cancelled = true)
            } catch (error: Exception) {
                DownloadResult(
                    entry.id,
                    success = false,
                    failure = error.downloadError(),
                )
            }
            result.complete(outcome)
        }
        synchronized(lock) { currentRun = work }
        work.start()
        return result.await()
    }

    /** Publishes one percent, not one socket read, so the notification does not churn. */
    private fun publish(entry: Entry, read: Long, total: Long) {
        val state = _state.value
        if (state.id != entry.id) return
        val percent = if (total > 0) ((read * 100) / total).toInt() else -1
        if (read == state.read || (percent >= 0 && percent == state.percent)) return
        _state.value = state.copy(
            phase = DownloadPhase.Running,
            read = read,
            total = total.coerceAtLeast(0),
        )
    }

    /** The one terminal answer, written to the caller and to the service's state. */
    private fun complete(id: String, result: DownloadResult) {
        waiters[id]?.complete(result)
        _state.update { state ->
            if (state.id != id) return@update state
            state.copy(
                phase = when {
                    result.cancelled -> DownloadPhase.Cancelled
                    result.success -> DownloadPhase.Done
                    else -> DownloadPhase.Failed
                },
                read = if (result.success) state.total else state.read,
                failure = result.failure,
            )
        }
    }

    private fun language(context: Context): String =
        LanguagePreference(context).tag() ?: UiLanguage.English.tag
}
