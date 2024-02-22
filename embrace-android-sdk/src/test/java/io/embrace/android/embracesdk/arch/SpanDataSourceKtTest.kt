package io.embrace.android.embracesdk.arch

import io.embrace.android.embracesdk.arch.datasource.startSpanCapture
import io.embrace.android.embracesdk.arch.destination.StartSpanData
import io.embrace.android.embracesdk.fakes.FakeClock
import io.embrace.android.embracesdk.fakes.injection.FakeInitModule
import io.embrace.android.embracesdk.internal.spans.SpanServiceImpl
import org.junit.Test

internal class SpanDataSourceKtTest {

    @Test
    fun `start span`() {
        val initModule = FakeInitModule(FakeClock())
        val service = SpanServiceImpl(
            spanRepository = initModule.openTelemetryModule.spanRepository,
            currentSessionSpan = initModule.openTelemetryModule.currentSessionSpan,
            tracer = initModule.openTelemetryModule.tracer
        )
        service.initializeService(1500000000000)

        val span = service.startSpanCapture("") {
            StartSpanData(
                "spanName",
                1500000000000,
                mapOf("key" to "value")
            )
        }
        checkNotNull(span)
    }
}
