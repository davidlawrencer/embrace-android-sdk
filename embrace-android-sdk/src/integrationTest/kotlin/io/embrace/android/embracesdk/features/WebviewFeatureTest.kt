package io.embrace.android.embracesdk.features

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.Types
import io.embrace.android.embracesdk.IntegrationTestRule
import io.embrace.android.embracesdk.ResourceReader
import io.embrace.android.embracesdk.arch.schema.EmbType
import io.embrace.android.embracesdk.config.remote.RemoteConfig
import io.embrace.android.embracesdk.config.remote.WebViewVitals
import io.embrace.android.embracesdk.fakes.fakeWebViewVitalsBehavior
import io.embrace.android.embracesdk.findEventAttribute
import io.embrace.android.embracesdk.findEventsOfType
import io.embrace.android.embracesdk.findSessionSpan
import io.embrace.android.embracesdk.internal.serialization.EmbraceSerializer
import io.embrace.android.embracesdk.payload.WebVital
import io.embrace.android.embracesdk.payload.WebVitalType
import io.embrace.android.embracesdk.recordSession
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class WebviewFeatureTest {

    private val expectedCompleteData =
        ResourceReader.readResourceAsText("expected_core_vital_script.json")
    private val serializer = EmbraceSerializer()

    @Rule
    @JvmField
    val testRule: IntegrationTestRule = IntegrationTestRule()


    @Before
    fun setup() {
        testRule.harness.overriddenConfigService.webViewVitalsBehavior =
            fakeWebViewVitalsBehavior(remoteCfg = {
                RemoteConfig(webViewVitals = WebViewVitals(100f, 50))
            })
    }

    @Test
    fun `webview info feature`() {
        with(testRule) {
            val message = checkNotNull(harness.recordSession {
                embrace.trackWebViewPerformance("myWebView", expectedCompleteData)
            })

            val events = message.findSessionSpan().findEventsOfType(EmbType.System.WebViewInfo)
            assertEquals(1, events.size)

            val event = events[0]

            assertEquals("emb-webview-info", event.name)
            assertEquals("myWebView", event.findEventAttribute("emb.webview_info.tag"))
            assertEquals("https://embrace.io/", event.findEventAttribute("emb.webview_info.url"))

            val webVitalsAttr: String = event.findEventAttribute("emb.webview_info.web_vitals")
            val type = Types.newParameterizedType(List::class.java, WebVital::class.java)
            val webVitals: List<WebVital> = serializer.fromJson(webVitalsAttr, type)

            assertEquals(4, webVitals.size)
            webVitals.forEach { wv ->
                when (wv.type) {
                    WebVitalType.CLS -> assertEquals(10L, wv.duration)
                    WebVitalType.LCP -> assertEquals(2222, wv.startTime)
                    else -> {}
                }
            }
        }
    }
}
