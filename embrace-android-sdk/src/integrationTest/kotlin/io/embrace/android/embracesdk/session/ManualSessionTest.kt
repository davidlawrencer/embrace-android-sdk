package io.embrace.android.embracesdk.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.embrace.android.embracesdk.IntegrationTestRule
import io.embrace.android.embracesdk.config.remote.RemoteConfig
import io.embrace.android.embracesdk.config.remote.SessionRemoteConfig
import io.embrace.android.embracesdk.fakes.fakeSessionBehavior
import io.embrace.android.embracesdk.getSentSessionMessages
import io.embrace.android.embracesdk.payload.SessionMessage
import io.embrace.android.embracesdk.recordSession
import io.embrace.android.embracesdk.verifySessionHappened
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Asserts that a stateful session can be recorded.
 */
@RunWith(AndroidJUnit4::class)
internal class ManualSessionTest {

    @Rule
    @JvmField
    val testRule: IntegrationTestRule = IntegrationTestRule()

    @Before
    fun setUp() {
        assertTrue(testRule.harness.getSentSessionMessages().isEmpty())
    }

    @Test
    fun `calling endSession ends stateful session`() {
        with(testRule) {
            harness.recordSession {
                embrace.endSession()
            }
            val messages = harness.getSentSessionMessages()

            // FIXME (future): ending a session manually drops the session end event of a
            //  stateful session.
            assertEquals(3, messages.size)
            val second = messages[2]
            verifySessionHappened(messages[1], second)
            assertEquals(2, second.session.number)
        }
    }

    @Test
    fun `calling endSession when session control enabled ends sessions`() {
        with(testRule) {
            harness.fakeConfigService.sessionBehavior = fakeSessionBehavior {
                RemoteConfig(sessionConfig = SessionRemoteConfig(isEnabled = true))
            }
            harness.recordSession {
                harness.fakeClock.tick(10000)
                embrace.endSession()
            }
            val messages = harness.getSentSessionMessages()
            assertEquals(4, messages.size)
            verifySessionHappened(messages[0], messages[1])
            verifySessionHappened(messages[2], messages[3])
            assertNotEquals(messages[1].session.sessionId, messages[3].session.sessionId)
        }
    }

    @Test
    fun `calling endSession when state session is below 5s has no effect`() {
        with(testRule) {
            harness.fakeConfigService.sessionBehavior = fakeSessionBehavior {
                RemoteConfig(sessionConfig = SessionRemoteConfig(isEnabled = true))
            }
            harness.recordSession {
                harness.fakeClock.tick(1000) // not enough to trigger new session
                embrace.endSession()
            }
            val messages = harness.getSentSessionMessages()

            // FIXME (future): ending a session manually drops the session end event of a
            //  stateful session.
            assertEquals(3, messages.size)
            val second = messages[2]
            verifySessionHappened(messages[1], second)
            assertEquals(2, second.session.number)
        }
    }
}