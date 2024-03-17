package io.embrace.android.embracesdk.comms.delivery

import io.embrace.android.embracesdk.concurrency.ExecutionCoordinator
import io.embrace.android.embracesdk.fakes.FakeLoggerAction
import io.embrace.android.embracesdk.fakes.FakeStorageService
import io.embrace.android.embracesdk.fakes.TestPlatformSerializer
import io.embrace.android.embracesdk.fixtures.testSessionMessage
import io.embrace.android.embracesdk.fixtures.testSessionMessage2
import io.embrace.android.embracesdk.fixtures.testSessionMessageOneMinuteLater
import io.embrace.android.embracesdk.logging.InternalEmbraceLogger
import io.embrace.android.embracesdk.logging.InternalStaticEmbraceLogger
import io.embrace.android.embracesdk.payload.SessionMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

internal class EmbraceCacheServiceConcurrentAccessTest {
    private lateinit var embraceCacheService: EmbraceCacheService
    private lateinit var storageService: FakeStorageService
    private lateinit var serializer: TestPlatformSerializer
    private lateinit var loggerAction: FakeLoggerAction
    private lateinit var logger: InternalEmbraceLogger
    private lateinit var executionCoordinator: ExecutionCoordinator

    @Before
    fun setUp() {
        storageService = FakeStorageService()
        serializer = TestPlatformSerializer()
        loggerAction = FakeLoggerAction()
        logger = InternalEmbraceLogger().apply { addLoggerAction(loggerAction) }
        embraceCacheService = EmbraceCacheService(
            storageService,
            serializer,
            logger
        )
        executionCoordinator = ExecutionCoordinator(
            executionModifiers = serializer,
            errorLogsProvider = ::getErrorLogs
        )
    }

    @Test
    fun `concurrent write attempts to the same non-existent session file should lead to last finished persist`() {
        executionCoordinator.executeOperations(
            first = { embraceCacheService.writeSession(FILENAME, testSessionMessage) },
            second = { embraceCacheService.writeSession(FILENAME, testSessionMessageOneMinuteLater) },
            firstBlocksSecond = true
        )

        assertEquals(
            executionCoordinator.getErrorMessage(),
            testSessionMessageOneMinuteLater,
            embraceCacheService.loadObject(FILENAME, SessionMessage::class.java)
        )
    }

    @Test
    fun `accessing sessions with different names should not block`() {
        embraceCacheService.writeSession(FILENAME, testSessionMessage2)

        executionCoordinator.executeOperations(
            first = { embraceCacheService.writeSession(FILENAME, testSessionMessage) },
            second = { embraceCacheService.loadObject(FILENAME_2, SessionMessage::class.java) },
            firstBlocksSecond = false
        )
    }

    @Test
    fun `reading a session should not block other reads to same session`() {
        embraceCacheService.writeSession(FILENAME, testSessionMessage)

        executionCoordinator.executeOperations(
            first = { embraceCacheService.loadObject(FILENAME, SessionMessage::class.java) },
            second = { embraceCacheService.loadObject(FILENAME, SessionMessage::class.java) },
            firstBlocksSecond = false
        )
    }

    @Test
    fun `reads should block writes`() {
        embraceCacheService.writeSession(FILENAME, testSessionMessage)

        executionCoordinator.executeOperations(
            first = { embraceCacheService.loadObject(FILENAME, SessionMessage::class.java) },
            second = { embraceCacheService.writeSession(FILENAME, testSessionMessageOneMinuteLater) },
            firstBlocksSecond = true
        )

        assertEquals(
            executionCoordinator.getErrorMessage(),
            testSessionMessageOneMinuteLater,
            embraceCacheService.loadObject(FILENAME, SessionMessage::class.java)
        )
    }

    @Test
    fun `reading a file that is being written to should block and succeed`() {
        var readSession: SessionMessage? = null

        executionCoordinator.executeOperations(
            first = { embraceCacheService.writeSession(FILENAME, testSessionMessage) },
            second = { readSession = embraceCacheService.loadObject(FILENAME, SessionMessage::class.java) },
            firstBlocksSecond = true
        )

        assertEquals(executionCoordinator.getErrorMessage(), testSessionMessage, readSession)
    }

    @Test
    fun `reading a file that is being rewritten to should block and succeed`() {
        var readSession: SessionMessage? = null
        embraceCacheService.writeSession(FILENAME, testSessionMessage)

        executionCoordinator.executeOperations(
            first = { embraceCacheService.writeSession(FILENAME, testSessionMessageOneMinuteLater) },
            second = { readSession = embraceCacheService.loadObject(FILENAME, SessionMessage::class.java) },
            firstBlocksSecond = true
        )

        assertEquals(executionCoordinator.getErrorMessage(), testSessionMessageOneMinuteLater, readSession)
    }

    // flakey: Unexpected run order expected:<[1, 2, 3, 6]> but was:<[1, 5, 2, 3]>
    fun `interrupting a session write should not leave a file`() {
        executionCoordinator.executeOperations(
            first = { embraceCacheService.writeSession(FILENAME, testSessionMessage) },
            second = { executionCoordinator.shutdownFirstThread() },
            firstBlocksSecond = false,
            firstOperationFails = true
        )

        assertNull(embraceCacheService.loadObject(FILENAME, SessionMessage::class.java))
    }

    // flakey: Unexpected run order expected:<[1, 2, 3, 6]> but was:<[1, 5, 2, 3]>
    fun `interrupting a session rewrite should not overwrite the file`() {
        embraceCacheService.writeSession(FILENAME, testSessionMessage)

        executionCoordinator.executeOperations(
            first = { embraceCacheService.writeSession(FILENAME, testSessionMessageOneMinuteLater) },
            second = { executionCoordinator.shutdownFirstThread() },
            firstBlocksSecond = false,
            firstOperationFails = true
        )

        assertEquals(
            executionCoordinator.getErrorMessage(),
            testSessionMessage,
            embraceCacheService.loadObject(FILENAME, SessionMessage::class.java)
        )
    }

    private fun getErrorLogs() = loggerAction
        .msgQueue
        .filter { it.severity == InternalStaticEmbraceLogger.Severity.ERROR }
        .toList()

    companion object {
        private const val FILENAME = "testfile-1"
        private const val FILENAME_2 = "testfile-2"
    }
}
