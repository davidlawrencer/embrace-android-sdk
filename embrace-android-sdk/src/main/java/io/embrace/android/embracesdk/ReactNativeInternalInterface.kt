package io.embrace.android.embracesdk

import android.content.Context
import io.embrace.android.embracesdk.annotation.InternalApi
import io.embrace.android.embracesdk.internal.EmbraceInternalInterface

/**
 * Provides an internal interface to Embrace that is intended for use by the React Native SDK as its
 * sole source of communication with the Android SDK.
 */
@InternalApi
public interface ReactNativeInternalInterface : EmbraceInternalInterface {

    public fun logUnhandledJsException(
        name: String,
        message: String,
        type: String?,
        stacktrace: String?
    )

    public fun logHandledJsException(
        name: String,
        message: String,
        properties: Map<String, Any>,
        stacktrace: String?
    )

    public fun setJavaScriptPatchNumber(number: String?)

    public fun setReactNativeSdkVersion(version: String?)

    public fun setReactNativeVersionNumber(version: String?)

    /**
     * Sets the React Native Bundle URL.
     * @param context the context
     * @param url the JavaScript bundle URL
     */
    public fun setJavaScriptBundleUrl(context: Context, url: String)

    /**
     * Sets the React Native Bundle URL, indicating if the bundle was updated or not.
     * If it was updated, the bundle ID will be recomputed.
     * If not, the bundle ID will be retrieved from cache.
     * @param context the context
     * @param url the JavaScript bundle URL
     * @param didUpdate if the bundle was updated
     */
    public fun setCacheableJavaScriptBundleUrl(context: Context, url: String, didUpdate: Boolean)

    /**
     * Logs a React Native Redux Action - this is not intended for public use.
     */
    public fun logRnAction(
        name: String,
        startTime: Long,
        endTime: Long,
        properties: Map<String?, Any?>,
        bytesSent: Int,
        output: String
    )

    /**
     * Logs the fact that a particular view was entered.
     *
     * If the previously logged view has the same name, a duplicate view breadcrumb will not be
     * logged.
     *
     * @param screen the name of the view to log
     */
    public fun logRnView(screen: String)
}
