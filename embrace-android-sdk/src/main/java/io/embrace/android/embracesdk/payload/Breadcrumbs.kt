package io.embrace.android.embracesdk.payload

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Contains lists of [ViewBreadcrumb], [TapBreadcrumb], [CustomBreadcrumb],
 * and [WebViewBreadcrumb] within a particular time window, created by the
 * [EmbraceBreadcrumbService].
 *
 * Breadcrumbs are used to track user journeys throughout the apps, such as transitions between
 * screens or taps on particular UI elements. A developer can create a [CustomBreadcrumb] if
 * they would like to label some particular event or interaction within their app on the timeline.
 */
@JsonClass(generateAdapter = true)
internal data class Breadcrumbs(

    /**
     * List of RN Action breadcrumbs.
     */
    @Json(name = "rna")
    val rnActionBreadcrumbs: List<RnActionBreadcrumb>? = null,
)
