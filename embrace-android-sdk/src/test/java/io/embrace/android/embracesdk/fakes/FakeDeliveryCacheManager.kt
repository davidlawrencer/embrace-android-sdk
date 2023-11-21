package io.embrace.android.embracesdk.fakes

import io.embrace.android.embracesdk.comms.delivery.DeliveryCacheManager
import io.embrace.android.embracesdk.comms.delivery.FailedApiCallsPerEndpoint
import io.embrace.android.embracesdk.payload.BackgroundActivityMessage
import io.embrace.android.embracesdk.payload.EventMessage
import io.embrace.android.embracesdk.payload.SessionMessage

internal class FakeDeliveryCacheManager : DeliveryCacheManager {
    override fun saveSession(sessionMessage: SessionMessage): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun saveSessionOnCrash(sessionMessage: SessionMessage) {
        TODO("Not yet implemented")
    }

    override fun loadSession(sessionId: String): SessionMessage? {
        TODO("Not yet implemented")
    }

    override fun loadSessionBytes(sessionId: String): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun deleteSession(sessionId: String) {
        TODO("Not yet implemented")
    }

    override fun getAllCachedSessionIds(): List<String> {
        TODO("Not yet implemented")
    }

    override fun saveBackgroundActivity(backgroundActivityMessage: BackgroundActivityMessage): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun loadBackgroundActivity(backgroundActivityId: String): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun saveCrash(crash: EventMessage) {
        TODO("Not yet implemented")
    }

    override fun loadCrash(): EventMessage? {
        TODO("Not yet implemented")
    }

    override fun deleteCrash() {
        TODO("Not yet implemented")
    }

    override fun savePayload(bytes: ByteArray): String {
        TODO("Not yet implemented")
    }

    override fun loadPayload(name: String): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun deletePayload(name: String) {
        TODO("Not yet implemented")
    }

    override fun saveFailedApiCalls(failedApiCalls: FailedApiCallsPerEndpoint) {
        TODO("Not yet implemented")
    }

    override fun loadFailedApiCalls(): FailedApiCallsPerEndpoint {
        TODO("Not yet implemented")
    }
}
