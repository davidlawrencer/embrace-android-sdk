package io.embrace.android.embracesdk.internal.api.delegate

import io.embrace.android.embracesdk.injection.ModuleInitBootstrapper
import io.embrace.android.embracesdk.injection.embraceImplInject
import io.embrace.android.embracesdk.internal.api.WebViewApi

internal class WebViewApiDelegate(
    bootstrapper: ModuleInitBootstrapper,
    private val sdkCallChecker: SdkCallChecker
) : WebViewApi {

    private val logger = bootstrapper.initModule.logger
    private val sdkClock = bootstrapper.initModule.clock
    private val breadcrumbService by embraceImplInject(sdkCallChecker) {
        bootstrapper.dataCaptureServiceModule.breadcrumbService
    }
    private val webviewService by embraceImplInject(sdkCallChecker) { bootstrapper.dataCaptureServiceModule.webviewService }
    private val configService by embraceImplInject(sdkCallChecker) { bootstrapper.essentialServiceModule.configService }
    private val sessionOrchestrator by embraceImplInject(sdkCallChecker) { bootstrapper.sessionModule.sessionOrchestrator }

    override fun logWebView(url: String?) {
        if (sdkCallChecker.check("log_web_view")) {
            breadcrumbService?.logWebView(url, sdkClock.now())
            sessionOrchestrator?.reportBackgroundActivityStateChange()
        }
    }

    override fun trackWebViewPerformance(tag: String, message: String) {
        if (sdkCallChecker.started.get() && configService?.webViewVitalsBehavior?.isWebViewVitalsEnabled() == true) {
            webviewService?.collectWebData(tag, message)
        }
    }
}
