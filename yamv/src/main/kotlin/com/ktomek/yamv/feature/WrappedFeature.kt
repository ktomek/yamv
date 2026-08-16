package com.ktomek.yamv.feature

/**
 * Marker for framework feature wrappers produced by the `wrap()` builders
 * ([TypedFeature.wrap], [TypedUnitFeature.wrap], [FunctionTypedFeature.wrap], [ActionTypedFeature.wrap]).
 *
 * These wrappers subscribe to the intentions flow **asynchronously**, inside a `channelFlow`, i.e.
 * strictly after their returned flow starts being collected. The router
 * ([com.ktomek.yamv.intention.FeatureRouter]) must therefore gate its readiness signal on the
 * *actual* subscription (`onSubscription` on the intentions flow) rather than marking such a feature
 * eagerly — otherwise the first dispatched intention races the deferred subscription and is dropped
 * by the `replay = 0` intentions flow (see #69).
 *
 * Raw [Feature.FlowFeature]/[Feature.FlowUnitFeature] implementations do **not** carry this marker:
 * they either consume intentions synchronously at collect, or ignore them entirely (a source-driven
 * observer that never subscribes and never completes). Those are marked eagerly so the readiness
 * gate never stalls waiting on a feature that will never subscribe (see #71). A raw feature that
 * defers its own subscription (a hand-written `channelFlow`, or `flow { delay; emitAll(intentions) }`)
 * is unsupported for first-intention delivery under immediate dispatch — use a [TypedFeature] /
 * [TypedUnitFeature] wrapper if that guarantee is required.
 *
 * This is a bare marker: it must NOT extend any [Feature] subtype, so it does not affect the
 * `FlowFeature`/`FlowUnitFeature` branch a wrapper falls into.
 */
interface WrappedFeature
