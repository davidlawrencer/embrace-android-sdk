package io.embrace.android.embracesdk.worker

import java.io.Closeable

/**
 * A set of shared executors to be used throughout the SDK
 */
internal interface WorkerThreadModule : Closeable {

    /**
     * Return a [BackgroundWorker] matching the [workerName]
     */
    fun backgroundWorker(workerName: WorkerName): BackgroundWorker

    /**
     * Return the [ScheduledWorker] given the [workerName]
     */
    fun scheduledWorker(workerName: WorkerName): ScheduledWorker

    /**
     * This should only be invoked when the SDK is shutting down. Closing all the worker threads in production means the
     * SDK will not be functional afterwards.
     */
    override fun close()
}

/**
 * The key used to reference a specific shared [BackgroundWorker] or the [ScheduledWorker] that uses it
 */
internal enum class WorkerName(internal val threadName: String) {

    /**
     * Used primarily to perform short-lived tasks that need to execute only once, or
     * recurring tasks that don't use I/O or block for long periods of time.
     */
    BACKGROUND_REGISTRATION("background-reg"),

    /**
     * Reads any sessions that are cached on disk & loads then sends them to the server.
     * Runnables are only added to this during SDK initialization.
     */
    CACHED_SESSIONS("cached-sessions"),

    /**
     * Saves/loads request information from files cached on disk.
     */
    DELIVERY_CACHE("delivery-cache"),

    /**
     * All HTTP requests are performed on this executor.
     */
    NETWORK_REQUEST("network-request"),

    /**
     * Used for periodic writing of session/background activity payloads to disk.
     */
    PERIODIC_CACHE("periodic-cache"),

    /**
     * Used to construct log messages. Log messages are sent to the server on a separate thread -
     * the intention behind this is to offload unnecessary CPU work from the main thread.
     */
    REMOTE_LOGGING("remote-logging"),
}
