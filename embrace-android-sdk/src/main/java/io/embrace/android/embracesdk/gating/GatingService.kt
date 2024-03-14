package io.embrace.android.embracesdk.gating

import io.embrace.android.embracesdk.internal.payload.Envelope
import io.embrace.android.embracesdk.internal.payload.SessionPayload
import io.embrace.android.embracesdk.payload.EventMessage
import io.embrace.android.embracesdk.payload.SessionMessage

internal interface GatingService {

    /**
     * Sanitizes a session message before send it to backend based on the Gating configuration.
     * Breadcrumbs, session properties, ANRs, logs, etc can be removed from the session payload.
     * This method should be called before send the session message to the ApiClient class.
     *
     * @param sessionMessage to be sanitized
     */
    fun gateSessionMessage(sessionMessage: SessionMessage): SessionMessage

    /**
     * Sanitizes a v2 session message before sending it to the backend based on the Gating configuration.
     * Breadcrumbs, session properties, ANRs, logs, etc can be removed from the session payload.
     * This method should be called before send the session message to the ApiClient class.
     *
     * @param envelope to be sanitized
     */
    fun gateSessionEnvelope(envelope: Envelope<SessionPayload>): Envelope<SessionPayload>

    /**
     * Sanitizes an event message before send it to backend based on the Gating configuration.
     * Log properties, stacktraces, etc can be removed from the event payload.
     * This method should be called before send the event message to the ApiClient class.
     *
     * @param eventMessage to be sanitized
     */
    fun gateEventMessage(eventMessage: EventMessage): EventMessage
}
