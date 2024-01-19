package io.embrace.android.embracesdk.session

import android.app.Activity
import io.embrace.android.embracesdk.FakeBreadcrumbService
import io.embrace.android.embracesdk.FakeDeliveryService
import io.embrace.android.embracesdk.FakeNdkService
import io.embrace.android.embracesdk.capture.PerformanceInfoService
import io.embrace.android.embracesdk.capture.connectivity.NetworkConnectivityService
import io.embrace.android.embracesdk.capture.thermalstate.NoOpThermalStatusService
import io.embrace.android.embracesdk.capture.webview.WebViewService
import io.embrace.android.embracesdk.config.local.LocalConfig
import io.embrace.android.embracesdk.config.local.SdkLocalConfig
import io.embrace.android.embracesdk.config.local.SessionLocalConfig
import io.embrace.android.embracesdk.config.remote.RemoteConfig
import io.embrace.android.embracesdk.config.remote.SessionRemoteConfig
import io.embrace.android.embracesdk.event.EventService
import io.embrace.android.embracesdk.event.LogMessageService
import io.embrace.android.embracesdk.fakes.FakeActivityTracker
import io.embrace.android.embracesdk.fakes.FakeAndroidMetadataService
import io.embrace.android.embracesdk.fakes.FakeClock
import io.embrace.android.embracesdk.fakes.FakeConfigService
import io.embrace.android.embracesdk.fakes.FakeEventService
import io.embrace.android.embracesdk.fakes.FakeGatingService
import io.embrace.android.embracesdk.fakes.FakeLogMessageService
import io.embrace.android.embracesdk.fakes.FakeMemoryCleanerService
import io.embrace.android.embracesdk.fakes.FakePerformanceInfoService
import io.embrace.android.embracesdk.fakes.FakePreferenceService
import io.embrace.android.embracesdk.fakes.FakeProcessStateService
import io.embrace.android.embracesdk.fakes.FakeTelemetryService
import io.embrace.android.embracesdk.fakes.FakeUserService
import io.embrace.android.embracesdk.fakes.FakeWebViewService
import io.embrace.android.embracesdk.fakes.fakeAutoDataCaptureBehavior
import io.embrace.android.embracesdk.fakes.fakeDataCaptureEventBehavior
import io.embrace.android.embracesdk.fakes.fakeSession
import io.embrace.android.embracesdk.fakes.fakeSessionBehavior
import io.embrace.android.embracesdk.fakes.system.mockActivity
import io.embrace.android.embracesdk.fixtures.testSpan
import io.embrace.android.embracesdk.internal.OpenTelemetryClock
import io.embrace.android.embracesdk.internal.spans.EmbraceSpanData
import io.embrace.android.embracesdk.internal.spans.EmbraceSpansService
import io.embrace.android.embracesdk.internal.spans.isPrivate
import io.embrace.android.embracesdk.internal.utils.Uuid
import io.embrace.android.embracesdk.logging.EmbraceInternalErrorService
import io.embrace.android.embracesdk.logging.InternalEmbraceLogger
import io.embrace.android.embracesdk.payload.Session
import io.embrace.android.embracesdk.payload.SessionMessage
import io.embrace.android.embracesdk.payload.UserInfo
import io.embrace.android.embracesdk.session.properties.EmbraceSessionProperties
import io.embrace.android.embracesdk.worker.ScheduledWorker
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.opentelemetry.api.trace.SpanId
import io.opentelemetry.api.trace.StatusCode
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal class SessionHandlerTest {

    companion object {
        private val networkConnectivityService: NetworkConnectivityService =
            mockk(relaxUnitFun = true)
        private val eventService: EventService = FakeEventService()
        private val logMessageService: LogMessageService = FakeLogMessageService()
        private val clock = FakeClock()
        private val internalErrorService = EmbraceInternalErrorService(FakeProcessStateService(), clock, false)
        private val sessionPeriodicCacheExecutorService: ScheduledWorker =
            mockk(relaxed = true)
        private const val sessionUuid = "99fcae22-0db5-4b63-b49d-315eecce4889"
        private const val now = 123L
        private var sessionNumber = 5
        private val sessionProperties: EmbraceSessionProperties = mockk(relaxed = true)
        private val emptyMapSessionProperties: Map<String, String> = emptyMap()

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            mockkStatic(ScheduledExecutorService::class)
            mockkStatic(ExecutorService::class)
            mockkStatic(Uuid::class)
            every { Uuid.getEmbUuid() } returns sessionUuid
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            unmockkAll()
        }
    }

    private val logger: InternalEmbraceLogger = InternalEmbraceLogger()
    private val userService: FakeUserService = FakeUserService()
    private val activityLifecycleTracker = FakeActivityTracker()
    private val performanceInfoService: PerformanceInfoService = FakePerformanceInfoService()
    private val webViewService: WebViewService = FakeWebViewService()
    private val userInfo: UserInfo = UserInfo()
    private var activeSession: Session = fakeSession()

    private lateinit var preferencesService: FakePreferenceService
    private lateinit var metadataService: FakeAndroidMetadataService
    private lateinit var localConfig: LocalConfig
    private lateinit var remoteConfig: RemoteConfig
    private lateinit var sessionLocalConfig: SessionLocalConfig
    private lateinit var deliveryService: FakeDeliveryService
    private lateinit var gatingService: FakeGatingService
    private lateinit var configService: FakeConfigService
    private lateinit var spansService: EmbraceSpansService
    private lateinit var ndkService: FakeNdkService
    private lateinit var breadcrumbService: FakeBreadcrumbService
    private lateinit var memoryCleanerService: FakeMemoryCleanerService
    private lateinit var sessionService: EmbraceSessionService

    @Before
    fun before() {
        clock.setCurrentTime(now)
        activeSession = fakeSession()
        every { sessionProperties.get() } returns emptyMapSessionProperties
        ndkService = FakeNdkService()
        metadataService = FakeAndroidMetadataService()
        breadcrumbService = FakeBreadcrumbService()
        breadcrumbService.viewBreadcrumbScreenName = "screen"
        memoryCleanerService = FakeMemoryCleanerService()

        localConfig = LocalConfig(
            appId = metadataService.getAppId(),
            ndkEnabled = true,
            sdkConfig = SdkLocalConfig()
        )
        sessionLocalConfig = SessionLocalConfig()
        remoteConfig = RemoteConfig()
        configService = FakeConfigService(
            autoDataCaptureBehavior = fakeAutoDataCaptureBehavior(
                localCfg = { localConfig },
                remoteCfg = { remoteConfig }
            ),
            sessionBehavior = fakeSessionBehavior(
                localCfg = { sessionLocalConfig },
                remoteCfg = { remoteConfig }
            ),
            dataCaptureEventBehavior = fakeDataCaptureEventBehavior(
                remoteCfg = { remoteConfig }
            )
        )
        gatingService = FakeGatingService(configService = configService)
        preferencesService = FakePreferenceService()
        deliveryService = FakeDeliveryService()
        spansService = EmbraceSpansService(OpenTelemetryClock(embraceClock = clock), FakeTelemetryService())
        val payloadMessageCollator = PayloadMessageCollator(
            configService,
            metadataService,
            eventService,
            logMessageService,
            internalErrorService,
            performanceInfoService,
            webViewService,
            NoOpThermalStatusService(),
            null,
            breadcrumbService,
            userService,
            preferencesService,
            spansService,
            clock
        )
        spansService = EmbraceSpansService(OpenTelemetryClock(embraceClock = clock), FakeTelemetryService())
        sessionService = EmbraceSessionService(
            logger,
            configService,
            userService,
            networkConnectivityService,
            metadataService,
            breadcrumbService,
            ndkService,
            internalErrorService,
            memoryCleanerService,
            deliveryService,
            payloadMessageCollator,
            sessionProperties,
            clock,
            spansService,
            sessionPeriodicCacheScheduledWorker = sessionPeriodicCacheExecutorService
        )
    }

    @After
    fun after() {
        clearAllMocks(answers = false)
    }

    @Test
    fun `onSession started successfully`() {
        sessionLocalConfig = SessionLocalConfig()
        userService.obj = userInfo

        val sessionStartType = Session.LifeEventType.STATE
        // this is needed so session handler creates automatic session stopper

        sessionService.startSession(
            true,
            sessionStartType,
            now
        )

        // verify record connection type
        verify { networkConnectivityService.networkStatusOnSessionStarted(now) }
        // verify active session is set
        assertEquals(sessionUuid, metadataService.activeSessionId)
        // verify periodic caching worker has been scheduled
        verify {
            sessionPeriodicCacheExecutorService.scheduleWithFixedDelay(
                any(),
                0,
                2,
                TimeUnit.SECONDS
            )
        }
        // verify session id gets updated if ndk enabled
        assertEquals(sessionUuid, ndkService.sessionId)
        // verify session is correctly built

        with(checkNotNull(sessionService.activeSession)) {
            assertEquals(sessionUuid, this.sessionId)
            assertEquals(startTime, now)
            assertTrue(isColdStart)
            assertEquals(sessionStartType, startType)
            assertEquals(emptyMapSessionProperties, properties)
            assertEquals("en", messageType)
            assertEquals("foreground", appState)
            assertEquals(userInfo, user)
        }
        assertEquals(1, preferencesService.incrementAndGetSessionNumberCount)
    }

    @Test
    fun `onSession started successfully with no preference service session number`() {
        // return absent session number
        sessionNumber = 0
        sessionLocalConfig = SessionLocalConfig()
        val sessionStartType = Session.LifeEventType.STATE
        // this is needed so session handler creates automatic session stopper

        sessionService.startSession(
            true,
            sessionStartType,
            now
        )

        assertEquals(1, preferencesService.incrementAndGetSessionNumberCount)
        checkNotNull(sessionService.activeSession)
        // no need to verify anything else because it's already verified in another test case
    }

    @Test
    fun `onSession started and resuming with no previous screen name but with foregroundActivity, it should force log view breadcrumb`() {
        breadcrumbService.viewBreadcrumbScreenName = null
        val mockActivity: Activity = mockActivity()
        // let's return a foreground activity
        activityLifecycleTracker.foregroundActivity = mockActivity
        val sessionStartType = Session.LifeEventType.STATE

        sessionService.startSession(
            true,
            sessionStartType,
            now
        )

        // verify we are forcing log view with foreground activity class name
        assertEquals(now, breadcrumbService.firstViewBreadcrumbCalls.single())
        checkNotNull(sessionService.activeSession)
        // no need to verify anything else because it's already verified in another test case
    }

    @Test
    fun `onSession not allowed to end because session control is disabled for MANUAL event type`() {
        sessionService.endSession(
            Session.LifeEventType.MANUAL,
            1000,
            false
        )

        verify { sessionPeriodicCacheExecutorService wasNot Called }
        assertEquals(0, memoryCleanerService.callCount)
        assertTrue(deliveryService.lastSentSessions.isEmpty())
        assertEquals(0, gatingService.sessionMessagesFiltered.size)
    }

    @Test
    fun `onSession not allowed to end MANUALLY because session duration is less than 5 seconds`() {
        remoteConfig = RemoteConfig(sessionConfig = SessionRemoteConfig(isEnabled = true))
        // since now=123, then duration will be less than 5 seconds
        val startTime = 120L
        activeSession = activeSession.copy(startTime = startTime)

        sessionService.endSession(
            Session.LifeEventType.MANUAL,
            1000,
            false
        )

        verify { sessionPeriodicCacheExecutorService wasNot Called }
        assertEquals(0, memoryCleanerService.callCount)
        assertTrue(deliveryService.lastSentSessions.isEmpty())
        assertEquals(0, gatingService.sessionMessagesFiltered.size)
    }

    @Test
    fun `onCrash ended session successfully`() {
        startFakeSession()

        val crashId = "crash-id"
        val startTime = 120L
        val sdkStartupDuration = 2L
        activeSession = fakeSession().copy(
            startTime = startTime,
            isColdStart = true
        )

        sessionService.endSessionWithCrash(crashId)

        // when crashing, the following calls should not be made, this is because since we're
        // about to crash we can save some time on not doing these //
        assertEquals(0, memoryCleanerService.callCount)
        verify(exactly = 0) { sessionProperties.clearTemporary() }

        val session = checkNotNull(deliveryService.lastSavedSession).session

        with(session) {
            assertFalse(checkNotNull(isEndedCleanly))
            assertEquals("en", messageType)
            assertEquals("foreground", appState)
            assertEquals(emptyList<String>(), eventIds)
            assertEquals(emptyList<String>(), infoLogIds)
            assertEquals(emptyList<String>(), warningLogIds)
            assertEquals(emptyList<String>(), errorLogIds)
            assertEquals(emptyList<String>(), networkLogIds)
            assertEquals(0, infoLogsAttemptedToSend)
            assertEquals(0, warnLogsAttemptedToSend)
            assertEquals(0, errorLogsAttemptedToSend)
            assertNull(exceptionError)
            assertEquals(now, lastHeartbeatTime)
            assertEquals(sessionProperties.get(), properties)
            assertEquals(Session.LifeEventType.STATE, endType)
            assertEquals(0, unhandledExceptions)
            assertEquals(crashId, crashReportId)
            assertEquals(now, endTime)
            assertEquals(sdkStartupDuration, sdkStartupDuration)
            assertEquals(0L, startupDuration)
            assertEquals(0L, startupThreshold)
            assertEquals(0, webViewInfo?.size)
        }
    }

    @Test
    fun `onPeriodicCacheActiveSession caches session successfully`() {
        startFakeSession()
        val sessionMessage = sessionService.onPeriodicCacheActiveSessionImpl()

        assertNotNull(sessionMessage)

        // when periodic caching, the following calls should not be made
        assertEquals(0, memoryCleanerService.callCount)
        verify(exactly = 0) { sessionProperties.clearTemporary() }
        assertEquals(0, gatingService.sessionMessagesFiltered.size)
    }

    @Test
    fun `endSession includes completed spans in message`() {
        startFakeSession()
        initializeServices()
        spansService.recordSpan("test-span") {
            // do nothing
        }
        clock.tick(30000)
        sessionService.endSession(
            endType = Session.LifeEventType.STATE,
            endTime = 10L,
            false
        )
        assertSpanInSessionMessage(deliveryService.lastSentSessions.last().first)
    }

    @Test
    fun `clearing user info disallowed for state sessions`() {
        startFakeSession()
        clock.tick(30000)
        sessionService.endSession(
            endType = Session.LifeEventType.STATE,
            endTime = 10L,
            true
        )
        assertEquals(0, userService.clearedCount)
    }

    @Test
    fun `endSession clears user info`() {
        remoteConfig = remoteConfig.copy(
            sessionConfig = SessionRemoteConfig(
                isEnabled = true
            )
        )
        sessionService.startSession(
            coldStart = true,
            startType = Session.LifeEventType.MANUAL,
            startTime = clock.now()
        )
        clock.tick(30000)
        sessionService.endSession(
            endType = Session.LifeEventType.MANUAL,
            endTime = clock.now(),
            true
        )
        assertEquals(1, userService.clearedCount)
    }

    @Test
    fun `crashes includes completed spans in message`() {
        startFakeSession()
        initializeServices()
        spansService.recordSpan("test-span") {
            // do nothing
        }
        sessionService.endSessionWithCrash("fakeCrashId")
        assertSpanInSessionMessage(deliveryService.lastSavedSession)
    }

    @Test
    fun `periodically cached sessions included currently completed spans`() {
        startFakeSession()
        initializeServices()
        val sessionMessage = sessionService.onPeriodicCacheActiveSessionImpl(listOf(testSpan))
        val spans = checkNotNull(sessionMessage?.spans)
        assertEquals(testSpan, spans.single())
    }

    @Test
    fun `start session successfully`() {
        assertNull(sessionService.getSessionId())
        startFakeSession()
        assertNotNull(sessionService.getSessionId())
    }

    @Test
    fun `verify periodic caching`() {
        startFakeSession()
        sessionService.onPeriodicCacheActiveSessionImpl()
        val session = checkNotNull(deliveryService.lastSavedSession).session
        assertEquals(false, session.isEndedCleanly)
        assertEquals(true, session.isReceivedTermination)
    }

    @Test
    fun `backgrounding flushes completed spans`() {
        startFakeSession()
        initializeServices()
        assertEquals(1, spansService.completedSpans()?.size)

        clock.tick(15000L)
        sessionService.endSession(
            endType = Session.LifeEventType.STATE,
            endTime = clock.now(),
            false
        )

        val sessionMessage = checkNotNull(deliveryService.lastSentSessions.last().first)
        val spans = checkNotNull(sessionMessage.spans)
        assertEquals(2, spans.size)
        assertEquals(0, spansService.completedSpans()?.size)
    }

    @Test
    fun `crash ending flushes completed spans`() {
        startFakeSession()
        initializeServices()
        assertEquals(1, spansService.completedSpans()?.size)

        sessionService.endSessionWithCrash("crashId")
        assertEquals(0, spansService.completedSpans()?.size)
    }

    @Test
    fun `session message is sent`() {
        startFakeSession()
        clock.tick(10000)
        sessionService.endSession(
            endType = Session.LifeEventType.STATE,
            endTime = clock.now(),
            false
        )
        val sessions = deliveryService.lastSentSessions
        assertEquals(1, sessions.size)
        assertEquals(1, sessions.count { it.second == SessionSnapshotType.NORMAL_END })
    }

    @Test
    fun `initialization records SDK startup span`() {
        val startTimeMillis = clock.now()
        clock.tick(10L)
        val endTimeMillis = clock.now()
        spansService.initializeService(startTimeMillis)
        sessionService.setSdkStartupInfo(startTimeMillis, endTimeMillis)
        val currentSpans = checkNotNull(spansService.completedSpans())
        assertEquals(1, currentSpans.size)
        with(currentSpans[0]) {
            assertEquals("emb-sdk-init", name)
            assertEquals(SpanId.getInvalid(), parentSpanId)
            assertEquals(TimeUnit.MILLISECONDS.toNanos(startTimeMillis), startTimeNanos)
            assertEquals(TimeUnit.MILLISECONDS.toNanos(endTimeMillis), endTimeNanos)
            assertEquals(
                io.embrace.android.embracesdk.internal.spans.EmbraceAttributes.Type.PERFORMANCE.name,
                attributes[io.embrace.android.embracesdk.internal.spans.EmbraceAttributes.Type.PERFORMANCE.keyName()]
            )
            assertTrue(isPrivate())
            assertEquals(StatusCode.OK, status)
        }
    }

    @Test
    fun `second sdk startup span will not be recorded if you try to set the startup info twice`() {
        spansService.initializeService(10)
        sessionService.setSdkStartupInfo(10, 20)
        assertEquals(1, spansService.completedSpans()?.size)
        sessionService.setSdkStartupInfo(10, 20)
        sessionService.setSdkStartupInfo(10, 20)
        assertEquals(1, spansService.completedSpans()?.size)
    }

    @Test
    fun `sdk startup span recorded if the startup info is set before span service initializes`() {
        sessionService.setSdkStartupInfo(10, 20)
        spansService.initializeService(10)
        assertEquals(1, spansService.completedSpans()?.size)
    }

    private fun startFakeSession() {
        sessionService.startSession(
            coldStart = true,
            startType = Session.LifeEventType.STATE,
            startTime = clock.now()
        )
    }

    private fun initializeServices(startTimeMillis: Long = clock.now(), endTimeMillis: Long = startTimeMillis + 10L) {
        spansService.initializeService(startTimeMillis)
        sessionService.setSdkStartupInfo(startTimeMillis, endTimeMillis)
    }

    private fun assertSpanInSessionMessage(sessionMessage: SessionMessage?) {
        assertNotNull(sessionMessage)
        val spans = checkNotNull(sessionMessage?.spans)
        assertEquals(3, spans.size)
        val expectedSpans = listOf("emb-sdk-init", "emb-test-span", "emb-session-span")
        assertEquals(expectedSpans, spans.map(EmbraceSpanData::name))
    }
}
