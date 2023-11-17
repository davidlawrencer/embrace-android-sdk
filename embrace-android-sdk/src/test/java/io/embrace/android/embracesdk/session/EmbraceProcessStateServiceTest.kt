package io.embrace.android.embracesdk.session

import android.app.Application
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.embrace.android.embracesdk.capture.orientation.OrientationService
import io.embrace.android.embracesdk.fakes.FakeClock
import io.embrace.android.embracesdk.session.lifecycle.EmbraceProcessStateService
import io.embrace.android.embracesdk.session.lifecycle.ProcessStateListener
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

internal class EmbraceProcessStateServiceTest {

    private lateinit var stateService: EmbraceProcessStateService

    companion object {
        private lateinit var mockLooper: Looper
        private lateinit var mockLifeCycleOwner: LifecycleOwner
        private lateinit var mockLifecycle: Lifecycle
        private lateinit var mockApplication: Application
        private lateinit var mockOrientationService: OrientationService
        private val fakeClock = FakeClock()

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            mockLooper = mockk()
            mockLifeCycleOwner = mockk()
            mockLifecycle = mockk(relaxed = true)
            mockkStatic(Looper::class)
            mockkStatic(ProcessLifecycleOwner::class)
            mockApplication = mockk(relaxed = true)
            mockOrientationService = mockk()

            fakeClock.setCurrentTime(1234)
            every { mockApplication.registerActivityLifecycleCallbacks(any()) } returns Unit
            every { Looper.getMainLooper() } returns mockLooper
            every { mockLooper.thread } returns Thread.currentThread()
            every { ProcessLifecycleOwner.get() } returns mockLifeCycleOwner
            every { mockLifeCycleOwner.lifecycle } returns mockLifecycle
            every { mockLifecycle.addObserver(any()) } returns Unit
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            unmockkAll()
        }
    }

    @Before
    fun before() {
        clearAllMocks(
            answers = false,
            objectMocks = false,
            constructorMocks = false,
            staticMocks = false
        )

        stateService = EmbraceProcessStateService(
            fakeClock
        )
    }

    @Test
    fun `verify on activity foreground for cold start triggers listeners`() {
        val mockProcessStateListener = mockk<ProcessStateListener>()
        stateService.addListener(mockProcessStateListener)

        stateService.onForeground()

        verify { mockProcessStateListener.onForeground(true, fakeClock.now(), fakeClock.now()) }
    }

    @Test
    fun `verify on activity foreground called twice is not a cold start`() {
        val mockProcessStateListener = mockk<ProcessStateListener>()
        stateService.addListener(mockProcessStateListener)

        with(stateService) {
            onForeground()
            // repeat so it's not a cold start
            onForeground()
        }

        verify { mockProcessStateListener.onForeground(true, fakeClock.now(), fakeClock.now()) }
        verify { mockProcessStateListener.onForeground(true, fakeClock.now(), fakeClock.now()) }
    }

    @Test
    fun `verify on activity background triggers listeners`() {
        val mockProcessStateListener = mockk<ProcessStateListener>()
        stateService.addListener(mockProcessStateListener)

        stateService.onBackground()

        verify { mockProcessStateListener.onBackground(any()) }
    }

    @Test
    fun `verify isInBackground returns true by default`() {
        assertTrue(stateService.isInBackground)
    }

    @Test
    fun `verify isInBackground returns false if it was previously on foreground`() {
        stateService.onForeground()

        assertFalse(stateService.isInBackground)
    }

    @Test
    fun `verify isInBackground returns true if it was previously on background`() {
        stateService.onBackground()

        assertTrue(stateService.isInBackground)
    }

    @Test
    fun `verify a listener is added`() {
        // assert empty list first
        assertEquals(0, stateService.listeners.size)

        val mockProcessStateListener = mockk<ProcessStateListener>()
        stateService.addListener(mockProcessStateListener)

        assertEquals(1, stateService.listeners.size)
    }

    @Test
    fun `verify if listener is already present, then it does not add anything`() {
        val mockProcessStateListener = mockk<ProcessStateListener>()
        stateService.addListener(mockProcessStateListener)
        // add it for a 2nd time
        stateService.addListener(mockProcessStateListener)

        assertEquals(1, stateService.listeners.size)
    }

    @Test
    fun `verify a listener is added with priority`() {
        val mockProcessStateListener = mockk<ProcessStateListener>()
        val mockProcessStateListener2 = mockk<ProcessStateListener>()
        stateService.addListener(mockProcessStateListener)

        stateService.addListener(mockProcessStateListener2)

        assertEquals(2, stateService.listeners.size)
        assertEquals(mockProcessStateListener2, stateService.listeners[1])
    }

    @Test
    fun `verify close cleans everything`() {
        // add a listener first, so we then check that listener have been cleared
        val mockProcessStateListener = mockk<ProcessStateListener>()
        stateService.addListener(mockProcessStateListener)
        stateService.close()
        assertTrue(stateService.listeners.isEmpty())
    }
}