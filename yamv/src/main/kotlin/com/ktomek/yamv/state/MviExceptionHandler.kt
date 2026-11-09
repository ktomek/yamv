package com.ktomek.yamv.state

import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.FeatureHolder

/**
 * Handler for exceptions thrown during MVI pipeline execution.
 *
 * The default handler rethrows the exception, enforcing fail-fast behavior.
 * A broken feature means illegal application state — the runtime should not
 * silently degrade.
 *
 * Users can provide a custom handler for crash reporting or other logic,
 * but should rethrow after handling to maintain fail-fast semantics.
 */
fun interface MviExceptionHandler {

    /**
     * Called when an exception occurs in the MVI pipeline.
     *
     * @param context describes where and why the exception occurred
     * @param exception the exception that was thrown
     */
    fun handle(context: MviErrorContext, exception: Throwable)

    companion object {
        /** Default: rethrows — fail fast. */
        val Default = MviExceptionHandler { _, e -> throw e }
    }
}

/**
 * Context describing where in the MVI pipeline an exception occurred.
 *
 * @property feature the feature as registered with the runtime — for typed features this is the
 * framework wrapper produced by `wrap()`. Use [originalFeature] to identify the user's own class.
 */
data class MviErrorContext(
    val source: ErrorSource,
    val intention: Any? = null,
    val feature: Feature<*>? = null,
) {
    /**
     * The user's original feature behind a framework `wrap()` builder ([FeatureHolder]), or
     * [feature] itself when it is not wrapped; `null` when no feature is involved.
     */
    val originalFeature: Any?
        get() = (feature as? FeatureHolder)?.feature ?: feature
}

/**
 * The phase of the MVI pipeline where the error occurred.
 */
enum class ErrorSource {
    /** Exception in a reducer (StateOutcome.reduce) */
    REDUCER,

    /** Exception in a feature's processing */
    FEATURE,

    /** Exception during IntentionOutcome re-dispatch */
    INTENTION_REDISPATCH,

    /** Exception during effect emission */
    EFFECT,
}
