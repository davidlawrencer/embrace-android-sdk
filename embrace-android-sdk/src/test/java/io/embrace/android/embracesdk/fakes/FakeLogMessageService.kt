package io.embrace.android.embracesdk.fakes

import io.embrace.android.embracesdk.Embrace
import io.embrace.android.embracesdk.EventType
import io.embrace.android.embracesdk.LogExceptionType
import io.embrace.android.embracesdk.event.LogMessageService
import io.embrace.android.embracesdk.payload.NetworkCapturedCall

internal class FakeLogMessageService : LogMessageService {

    internal class LogData(
        val message: String,
        val type: EventType,
        val logExceptionType: LogExceptionType,
        val properties: Map<String, Any>?,
        val stackTraceElements: Array<StackTraceElement>?,
        val customStackTrace: String?,
        val framework: Embrace.AppFramework,
        val context: String?,
        val library: String?,
        val exceptionName: String?,
        val exceptionMessage: String?
    )

    val loggedMessages = mutableListOf<LogData>()
    val networkCalls = mutableListOf<NetworkCapturedCall>()

    override fun logNetwork(networkCaptureCall: NetworkCapturedCall?) {
        networkCaptureCall?.let(networkCalls::add)
    }

    override fun log(
        message: String,
        type: EventType,
        logExceptionType: LogExceptionType,
        properties: Map<String, Any>?,
        stackTraceElements: Array<StackTraceElement>?,
        customStackTrace: String?,
        framework: Embrace.AppFramework,
        context: String?,
        library: String?,
        exceptionName: String?,
        exceptionMessage: String?
    ) {
        loggedMessages.add(
            LogData(
                message = message,
                type = type,
                logExceptionType = logExceptionType,
                properties = properties,
                stackTraceElements = stackTraceElements,
                customStackTrace = customStackTrace,
                framework = framework,
                context = context,
                library = library,
                exceptionName = exceptionName,
                exceptionMessage = exceptionMessage
            )
        )
    }

    override fun findInfoLogIds(startTime: Long, endTime: Long): List<String> {
        return emptyList()
    }

    override fun findWarningLogIds(startTime: Long, endTime: Long): List<String> {
        return emptyList()
    }

    override fun findErrorLogIds(startTime: Long, endTime: Long): List<String> {
        return emptyList()
    }

    override fun findNetworkLogIds(startTime: Long, endTime: Long): List<String> {
        return emptyList()
    }

    override fun getInfoLogsAttemptedToSend(): Int {
        return 0
    }

    override fun getWarnLogsAttemptedToSend(): Int {
        return 0
    }

    override fun getErrorLogsAttemptedToSend(): Int {
        return 0
    }

    override fun getUnhandledExceptionsSent(): Int {
        return 0
    }

    override fun cleanCollections() {
    }
}
