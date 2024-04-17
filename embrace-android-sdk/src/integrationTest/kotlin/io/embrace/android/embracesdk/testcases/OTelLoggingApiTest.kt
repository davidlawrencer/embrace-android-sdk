package io.embrace.android.embracesdk.testcases

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.embrace.android.embracesdk.IntegrationTestRule
import io.embrace.android.embracesdk.LogExceptionType
import io.embrace.android.embracesdk.assertions.assertOtelLogReceived
import io.embrace.android.embracesdk.config.remote.OTelRemoteConfig
import io.embrace.android.embracesdk.config.remote.RemoteConfig
import io.embrace.android.embracesdk.fakes.FakeClock
import io.embrace.android.embracesdk.fakes.fakeOTelBehavior
import io.embrace.android.embracesdk.fakes.injection.FakeInitModule
import io.embrace.android.embracesdk.fakes.injection.FakeWorkerThreadModule
import io.embrace.android.embracesdk.getLastSentLog
import io.embrace.android.embracesdk.internal.utils.getSafeStackTrace
import io.embrace.android.embracesdk.worker.WorkerName
import io.opentelemetry.api.logs.Severity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.IllegalArgumentException

@RunWith(AndroidJUnit4::class)
internal class OTelLoggingApiTest {
    @Rule
    @JvmField
    val testRule: IntegrationTestRule = IntegrationTestRule {
        val clock = FakeClock(IntegrationTestRule.DEFAULT_SDK_START_TIME_MS)
        val fakeInitModule = FakeInitModule(clock = clock)
        IntegrationTestRule.Harness(
            overriddenClock = clock,
            overriddenInitModule = fakeInitModule,
            overriddenWorkerThreadModule = FakeWorkerThreadModule(fakeInitModule = fakeInitModule, name = WorkerName.REMOTE_LOGGING)
        )
    }

    @Before
    fun setup() {
        testRule.harness.overriddenConfigService.oTelBehavior = fakeOTelBehavior(
            remoteCfg = {
                RemoteConfig(oTelConfig = OTelRemoteConfig(isBetaEnabled = true))
            }
        )
    }

    @Test
    fun `log info message sent`() {
        with(testRule) {
            embrace.logInfo("test message")
            flushLogs()
            val log = harness.getLastSentLog()
            assertOtelLogReceived(
                log,
                message = "test message",
                severityNumber = getOtelSeverity(io.embrace.android.embracesdk.Severity.INFO).severityNumber,
                severityText = io.embrace.android.embracesdk.Severity.INFO.name
            )
        }
    }

    @Test
    fun `log warning message sent`() {
        with(testRule) {
            embrace.logWarning("test message")
            flushLogs()
            val log = harness.getLastSentLog()
            assertOtelLogReceived(
                log,
                message = "test message",
                severityNumber = getOtelSeverity(io.embrace.android.embracesdk.Severity.WARNING).severityNumber,
                severityText = io.embrace.android.embracesdk.Severity.WARNING.name
            )
        }
    }

    @Test
    fun `log error message sent`() {
        with(testRule) {
            embrace.logError("test message")
            flushLogs()
            val log = harness.getLastSentLog()
            assertOtelLogReceived(
                log,
                message = "test message",
                severityNumber = getOtelSeverity(io.embrace.android.embracesdk.Severity.ERROR).severityNumber,
                severityText = io.embrace.android.embracesdk.Severity.ERROR.name
            )
        }
    }

    @Test
    fun `log messages with different severities sent`() {
        with(testRule) {
            io.embrace.android.embracesdk.Severity.values().forEach { severity ->
                val expectedMessage = "test message ${severity.name}"
                embrace.logMessage(expectedMessage, severity)
                flushLogs()
                val log = harness.getLastSentLog()
                assertOtelLogReceived(
                    log,
                    message = expectedMessage,
                    severityNumber = getOtelSeverity(severity).severityNumber,
                    severityText = severity.name
                )
            }
        }
    }

    @Test
    fun `log messages with different severities and properties sent`() {
        with(testRule) {
            io.embrace.android.embracesdk.Severity.values().forEach { severity ->
                val expectedMessage = "test message ${severity.name}"
                embrace.logMessage(expectedMessage, severity, customProperties)
                flushLogs()
                val log = harness.getLastSentLog()
                assertOtelLogReceived(
                    log,
                    message = expectedMessage,
                    severityNumber = getOtelSeverity(severity).severityNumber,
                    severityText = severity.name,
                    properties = customProperties
                )
            }
        }
    }

    @Test
    fun `log exception message sent`() {
        with(testRule) {
            embrace.logException(testException)
            flushLogs()
            val log = harness.getLastSentLog()
            assertOtelLogReceived(
                log,
                message = checkNotNull(testException.message),
                severityNumber = Severity.ERROR.severityNumber,
                severityText = io.embrace.android.embracesdk.Severity.ERROR.name,
                type = LogExceptionType.HANDLED.value,
                exception = testException,
                stack = testException.getSafeStackTrace()?.toList()
            )
        }
    }

    @Test
    fun `log exception with different severities sent`() {
        with(testRule) {
            embrace.logException(testException, io.embrace.android.embracesdk.Severity.INFO)
            flushLogs()
            val log = harness.getLastSentLog()
            assertOtelLogReceived(
                log,
                message = checkNotNull(testException.message),
                severityNumber = Severity.INFO.severityNumber,
                severityText = io.embrace.android.embracesdk.Severity.INFO.name,
                type = LogExceptionType.HANDLED.value,
                exception = testException,
                stack = testException.getSafeStackTrace()?.toList()
            )
        }
    }

    @Test
    fun `log exception with different severities and properties sent`() {
        with(testRule) {
            io.embrace.android.embracesdk.Severity.values().forEach { severity ->
                embrace.logException(
                    testException, severity,
                    customProperties
                )
                flushLogs()
                val log = harness.getLastSentLog()
                assertOtelLogReceived(
                    log,
                    message = checkNotNull(testException.message),
                    severityNumber = getOtelSeverity(severity).severityNumber,
                    severityText = severity.name,
                    type = LogExceptionType.HANDLED.value,
                    exception = testException,
                    stack = testException.getSafeStackTrace()?.toList(),
                    properties = customProperties
                )
            }
        }
    }

    @Test
    fun `log exception with different severities, properties, and custom message sent`() {
        with(testRule) {
            io.embrace.android.embracesdk.Severity.values().forEach { severity ->
                val expectedMessage = "test message ${severity.name}"
                embrace.logException(testException, severity, customProperties, expectedMessage)
                flushLogs()
                val log = harness.getLastSentLog()
                assertOtelLogReceived(
                    log,
                    message = expectedMessage,
                    severityNumber = getOtelSeverity(severity).severityNumber,
                    severityText = severity.name,
                    type = LogExceptionType.HANDLED.value,
                    exception = testException,
                    stack = testException.getSafeStackTrace()?.toList(),
                    properties = customProperties
                )
            }
        }
    }

    @Test
    fun `log custom stacktrace message sent`() {
        with(testRule) {
            embrace.logCustomStacktrace(stacktrace)
            flushLogs()
            val log = harness.getLastSentLog()
            assertOtelLogReceived(
                log,
                message = "",
                severityNumber = getOtelSeverity(io.embrace.android.embracesdk.Severity.ERROR).severityNumber,
                severityText = io.embrace.android.embracesdk.Severity.ERROR.name,
                type = LogExceptionType.HANDLED.value,
                stack = stacktrace.toList()
            )
        }
    }

    @Test
    fun `log custom stacktrace with different severities sent`() {
        with(testRule) {
            io.embrace.android.embracesdk.Severity.values().forEach { severity ->
                embrace.logCustomStacktrace(stacktrace, severity)
                flushLogs()
                val log = harness.getLastSentLog()
                assertOtelLogReceived(
                    log,
                    message = "",
                    severityNumber = getOtelSeverity(severity).severityNumber,
                    severityText = severity.name,
                    type = LogExceptionType.HANDLED.value,
                    stack = stacktrace.toList()
                )
            }
        }
    }

    @Test
    fun `log custom stacktrace with different severities and properties sent`() {
        with(testRule) {
            io.embrace.android.embracesdk.Severity.values().forEach { severity ->
                embrace.logCustomStacktrace(stacktrace, severity, customProperties)
                flushLogs()
                val log = harness.getLastSentLog()
                assertOtelLogReceived(
                    log,
                    message = "",
                    severityNumber = getOtelSeverity(severity).severityNumber,
                    severityText = severity.name,
                    type = LogExceptionType.HANDLED.value,
                    stack = stacktrace.toList(),
                    properties = customProperties
                )
            }
        }
    }

    @Test
    fun `log custom stacktrace with different severities, properties, and custom message sent`() {
        with(testRule) {
            io.embrace.android.embracesdk.Severity.values().forEach { severity ->
                val expectedMessage = "test message ${severity.name}"
                embrace.logCustomStacktrace(stacktrace, severity, customProperties, expectedMessage)
                flushLogs()
                val log = harness.getLastSentLog()
                assertOtelLogReceived(
                    log,
                    message = expectedMessage,
                    severityNumber = getOtelSeverity(severity).severityNumber,
                    severityText = severity.name,
                    type = LogExceptionType.HANDLED.value,
                    stack = stacktrace.toList(),
                    properties = customProperties
                )
            }
        }
    }

    private fun flushLogs() {
        val executor = (testRule.harness.overriddenWorkerThreadModule as FakeWorkerThreadModule).executor
        executor.runCurrentlyBlocked()
        val logOrchestrator = testRule.bootstrapper.customerLogModule.logOrchestrator
        logOrchestrator.flush(false)
    }

    private fun getOtelSeverity(severity: io.embrace.android.embracesdk.Severity): Severity {
        return when (severity) {
            io.embrace.android.embracesdk.Severity.INFO -> Severity.INFO
            io.embrace.android.embracesdk.Severity.WARNING -> Severity.WARN
            io.embrace.android.embracesdk.Severity.ERROR -> Severity.ERROR
        }
    }

    companion object {
        private val testException = IllegalArgumentException("nooooooo")
        private val customProperties: Map<String, Any> = linkedMapOf(Pair("first", 1), Pair("second", "two"), Pair("third", true))
        private val stacktrace = Thread.currentThread().stackTrace
    }
}