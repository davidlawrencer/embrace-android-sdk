package io.embrace.android.embracesdk.fakes.injection

import io.embrace.android.embracesdk.FakeSessionPropertiesService
import io.embrace.android.embracesdk.FakeSessionService
import io.embrace.android.embracesdk.fakes.FakeSessionOrchestrator
import io.embrace.android.embracesdk.injection.SessionModule
import io.embrace.android.embracesdk.session.BackgroundActivityService
import io.embrace.android.embracesdk.session.PayloadMessageCollator
import io.embrace.android.embracesdk.session.SessionService
import io.embrace.android.embracesdk.session.caching.PeriodicSessionCacher
import io.embrace.android.embracesdk.session.orchestrator.SessionOrchestrator
import io.embrace.android.embracesdk.session.properties.SessionPropertiesService

internal class FakeSessionModule(
    override val backgroundActivityService: BackgroundActivityService? = null,
    override val sessionService: SessionService = FakeSessionService(),
    override val sessionPropertiesService: SessionPropertiesService = FakeSessionPropertiesService(),
    override val sessionOrchestrator: SessionOrchestrator = FakeSessionOrchestrator()
) : SessionModule {

    override val payloadMessageCollator: PayloadMessageCollator
        get() = TODO("Not yet implemented")

    override val periodicSessionCacher: PeriodicSessionCacher
        get() = TODO("Not yet implemented")
}
