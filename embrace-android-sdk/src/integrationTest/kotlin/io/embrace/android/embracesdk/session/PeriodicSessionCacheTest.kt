package io.embrace.android.embracesdk.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.embrace.android.embracesdk.IntegrationTestRule
import io.embrace.android.embracesdk.fakes.FakeClock
import io.embrace.android.embracesdk.fakes.injection.FakeInitModule
import io.embrace.android.embracesdk.fakes.injection.FakeWorkerThreadModule
import io.embrace.android.embracesdk.getLastSavedSession
import io.embrace.android.embracesdk.getLastSentSession
import io.embrace.android.embracesdk.recordSession
import io.embrace.android.embracesdk.worker.WorkerName.PERIODIC_CACHE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Asserts that the session is periodically cached.
 */
@RunWith(AndroidJUnit4::class)
internal class PeriodicSessionCacheTest {

    @Rule
    @JvmField
    val testRule: IntegrationTestRule = IntegrationTestRule {
        val clock = FakeClock(IntegrationTestRule.DEFAULT_SDK_START_TIME_MS)
        val fakeInitModule = FakeInitModule(clock = clock)
        IntegrationTestRule.Harness(
            overriddenClock = clock,
            overriddenInitModule = fakeInitModule,
            overriddenWorkerThreadModule = FakeWorkerThreadModule(fakeInitModule = fakeInitModule, name = PERIODIC_CACHE)
        )
    }

    @Test
    fun `session is periodically cached`() {
        with(testRule) {
            val executor = (harness.overriddenWorkerThreadModule as FakeWorkerThreadModule).executor

            harness.recordSession {
                executor.runCurrentlyBlocked()
                embrace.addSessionProperty("Test", "Test", true)

                var endMessage = checkNotNull(harness.getLastSavedSession())
                assertEquals("en", endMessage.session.messageType)
                assertEquals(false, endMessage.session.isEndedCleanly)
                assertEquals(true, endMessage.session.isReceivedTermination)
                assertEquals(0, endMessage.session.properties?.size)

                // trigger another periodic cache
                executor.moveForwardAndRunBlocked(2000)
                endMessage = checkNotNull(harness.getLastSavedSession())
                assertEquals("en", endMessage.session.messageType)
                assertEquals(false, endMessage.session.isEndedCleanly)
                assertEquals(true, endMessage.session.isReceivedTermination)
                assertEquals("Test", checkNotNull(endMessage.session.properties)["Test"])
            }

            val endMessage = checkNotNull(harness.getLastSentSession())
            assertEquals("en", endMessage.session.messageType)
            assertEquals(true, endMessage.session.isEndedCleanly)
            assertNull(endMessage.session.isReceivedTermination)
            assertEquals("Test", checkNotNull(endMessage.session.properties)["Test"])
        }
    }

}
