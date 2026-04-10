package com.ktomek.yamv.koin

import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.ActionTypedFeature
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.FunctionTypedFeature
import com.ktomek.yamv.feature.TypedFeature
import com.ktomek.yamv.feature.wrap
import org.koin.core.scope.Scope

/**
 * Builder that assembles a [Set]<[Feature]<[S]>> for use in [mviStore].
 *
 * Call [add] for each feature. For typed feature interfaces ([FunctionTypedFeature],
 * [ActionTypedFeature], [TypedFeature]) the required `.wrap()` is applied automatically —
 * no call sites need to know about it.
 *
 * Koin dependency resolution is forwarded via the inline [get] function, keeping the same
 * ergonomics as a plain `Scope.() -> ...` lambda.
 */
class FeatureRegistrar<S : State>(@PublishedApi internal val scope: Scope) {

    @PublishedApi internal val features = mutableSetOf<Feature<S>>()

    /** Adds a [Feature]<[S]> directly — [Feature.FlowFeature] and builder results go here. */
    fun add(feature: Feature<S>) {
        features.add(feature)
    }

    /** Adds a [FunctionTypedFeature], applying [wrap] internally. */
    inline fun <reified I : Any> add(feature: FunctionTypedFeature<S, I>) {
        features.add(feature.wrap())
    }

    /** Adds an [ActionTypedFeature], applying [wrap] internally. */
    inline fun <reified I : Any> add(feature: ActionTypedFeature<S, I>) {
        features.add(feature.wrap())
    }

    /** Adds a [TypedFeature], applying [wrap] internally. */
    inline fun <reified I : Any> add(feature: TypedFeature<S, I>) {
        features.add(feature.wrap())
    }

    /** Resolves a Koin dependency — equivalent to calling `get<T>()` inside a [Scope] lambda. */
    inline fun <reified T : Any> get(): T = scope.get()

    @PublishedApi internal fun build(): Set<Feature<S>> = features.toSet()
}
