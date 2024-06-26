package io.embrace.android.embracesdk.arch.destination

/**
 * Represents a span attribute that can be added to the current session span.
 */
internal data class SpanAttributeData(
    val key: String,
    val value: String
)
