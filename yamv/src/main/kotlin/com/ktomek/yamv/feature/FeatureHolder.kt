package com.ktomek.yamv.feature

/**
 * Implemented by framework feature wrappers produced by the `wrap()` builders
 * ([TypedFeature.wrap], [TypedUnitFeature.wrap], [FunctionTypedFeature.wrap], [ActionTypedFeature.wrap])
 * to expose the user's original feature instance.
 *
 * [com.ktomek.yamv.intention.FeatureRouter] unwraps through this interface so that opt-in
 * capabilities declared on the original feature — [HasFeatureDispatcher], [HasFeatureScope] — are
 * honoured, and so that [com.ktomek.yamv.state.CoroutineDispatcherConfig.provideFeatureDispatcher]
 * receives the original rather than an anonymous wrapper (#73).
 *
 * Like [WrappedFeature], this is a bare interface: it must NOT extend any [Feature] subtype, so it
 * does not affect the `FlowFeature`/`FlowUnitFeature` branch a wrapper falls into.
 */
interface FeatureHolder {
    /** The original, user-defined feature this wrapper delegates to. */
    val feature: Any
}
