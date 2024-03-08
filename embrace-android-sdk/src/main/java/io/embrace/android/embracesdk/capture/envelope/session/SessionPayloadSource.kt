package io.embrace.android.embracesdk.capture.envelope.session

import io.embrace.android.embracesdk.internal.payload.SessionPayload

/**
 * Creates a [SessionPayload] object.
 */
internal fun interface SessionPayloadSource {
    fun getSessionPayload(): SessionPayload
}