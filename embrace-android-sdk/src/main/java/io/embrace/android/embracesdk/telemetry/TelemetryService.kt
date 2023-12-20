package io.embrace.android.embracesdk.telemetry

internal interface TelemetryService {

    /**
     * Tracks the usage of a public API by name. We only track public APIs that are called when the SDK is initialized.
     * Name should be snake_case, e.g. "start_session".
     */
    fun onPublicApiCalled(name: String)

    /**
     * Returns a map with every telemetry value. This is called when the session ends.
     * We clear the usage count map so we don't count the same usages in the next session.
     */
    fun getAndClearTelemetryAttributes(): Map<String, String>
}