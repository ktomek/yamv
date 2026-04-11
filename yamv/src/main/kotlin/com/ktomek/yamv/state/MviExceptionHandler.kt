package com.ktomek.yamv.state

import com.ktomek.yamv.feature.Feature

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
 */
data class MviErrorContext(
    val source: ErrorSource,
    val intention: Any? = null,
    val feature: Feature<*>? = null,
)

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
