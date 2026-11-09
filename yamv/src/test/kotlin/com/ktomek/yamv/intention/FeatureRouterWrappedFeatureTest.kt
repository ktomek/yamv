package com.ktomek.yamv.intention

import com.google.common.truth.Truth.assertWithMessage
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.ActionTypedFeature
import com.ktomek.yamv.feature.DefaultFeatureScope
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.FunctionTypedFeature
import com.ktomek.yamv.feature.HasFeatureDispatcher
import com.ktomek.yamv.feature.HasFeatureScope
import com.ktomek.yamv.feature.TypedFeature
import com.ktomek.yamv.feature.TypedUnitFeature
import com.ktomek.yamv.feature.wrap
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import com.ktomek.yamv.state.ErrorSource
import com.ktomek.yamv.state.MviErrorContext
import com.ktomek.yamv.state.MviExceptionHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.coroutines.ContinuationInterceptor

private data class WrapState(val n: Int = 0) : State

private data object Ping

/** Records the dispatcher the feature body actually ran on. */
private class Probe {
    var dispatcher: CoroutineDispatcher? = null

    suspend fun capture() {
        dispatcher = currentCoroutineContext()[ContinuationInterceptor] as? CoroutineDispatcher
    }
}

private class ScopedTyped(private val probe: Probe, scope: HasFeatureScope) :
    TypedFeature<WrapState, Ping>, HasFeatureScope by scope {
    override fun invoke(intention: Flow<Ping>): Flow<Outcome<WrapState>> =
        intention.map {
            probe.capture()
            StateOutcome { s -> s }
        }
}

private class ScopedTypedUnit(private val probe: Probe, scope: HasFeatureScope) :
    TypedUnitFeature<WrapState, Ping>, HasFeatureScope by scope {
    override fun invoke(intention: Flow<Ping>): Flow<Unit> = intention.map { probe.capture() }
}

private class ScopedFunction(private val probe: Probe, scope: HasFeatureScope) :
    FunctionTypedFeature<WrapState, Ping>, HasFeatureScope by scope {
    override suspend fun invoke(intention: Ping): Outcome<WrapState> {
        probe.capture()
        return StateOutcome { s -> s }
    }
}

private class ScopedAction(private val probe: Probe, scope: HasFeatureScope) :
    ActionTypedFeature<WrapState, Ping>, HasFeatureScope by scope {
    override suspend fun invoke(intention: Ping) {
        probe.capture()
    }
}

private class Wrapped(val original: Any, val feature: Feature<WrapState>)

/** One entry per framework `wrap()` builder. */
private class WrapperKind(
    val name: String,
    val scoped: (Probe, HasFeatureScope) -> Wrapped,
    val plain: () -> Wrapped,
    val failing: () -> Wrapped,
)

private class Boom : RuntimeException("boom")

private val wrapperKinds = listOf(
    WrapperKind(
        name = "TypedFeature",
        scoped = { probe, scope -> ScopedTyped(probe, scope).let { Wrapped(it, it.wrap()) } },
        plain = {
            TypedFeature<WrapState, Ping> { it.map { StateOutcome { s -> s } } }
                .let { Wrapped(it, it.wrap()) }
        },
        failing = {
            TypedFeature<WrapState, Ping> { it.map { throw Boom() } }.let { Wrapped(it, it.wrap()) }
        },
    ),
    WrapperKind(
        name = "TypedUnitFeature",
        scoped = { probe, scope -> ScopedTypedUnit(probe, scope).let { Wrapped(it, it.wrap()) } },
        plain = {
            TypedUnitFeature<WrapState, Ping> { it.map { } }.let { Wrapped(it, it.wrap()) }
        },
        failing = {
            TypedUnitFeature<WrapState, Ping> { it.map { throw Boom() } }.let { Wrapped(it, it.wrap()) }
        },
    ),
    WrapperKind(
        name = "FunctionTypedFeature",
        scoped = { probe, scope -> ScopedFunction(probe, scope).let { Wrapped(it, it.wrap()) } },
        plain = {
            FunctionTypedFeature<WrapState, Ping> { StateOutcome { s -> s } }
                .let { Wrapped(it, it.wrap()) }
        },
        failing = {
            FunctionTypedFeature<WrapState, Ping> { throw Boom() }.let { Wrapped(it, it.wrap()) }
        },
    ),
    WrapperKind(
        name = "ActionTypedFeature",
        scoped = { probe, scope -> ScopedAction(probe, scope).let { Wrapped(it, it.wrap()) } },
        plain = {
            ActionTypedFeature<WrapState, Ping> { }.let { Wrapped(it, it.wrap()) }
        },
        failing = {
            ActionTypedFeature<WrapState, Ping> { throw Boom() }.let { Wrapped(it, it.wrap()) }
        },
    ),
)

/**
 * The router must see through every framework `wrap()` builder to the user's original feature
 * when honouring [HasFeatureDispatcher] / [HasFeatureScope] and when consulting
 * [CoroutineDispatcherConfig.provideFeatureDispatcher] (#73).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeatureRouterWrappedFeatureTest {

    @TestFactory
    fun `wrapped feature runs on the dispatcher of its HasFeatureScope`(): List<DynamicTest> =
        wrapperKinds.map { kind ->
            dynamicTest(kind.name) {
                runTest {
                    val scopeDispatcher = StandardTestDispatcher(testScheduler, name = "scope")
                    val configDispatcher = StandardTestDispatcher(testScheduler, name = "config")
                    val probe = Probe()
                    val wrapped = kind.scoped(probe, DefaultFeatureScope(scopeDispatcher))

                    val router = FeatureRouter(features = setOf(wrapped.feature))
                    router.initialize(this, recordingConfig(configDispatcher))
                    testScheduler.advanceUntilIdle()
                    launch { router.dispatchIntention(Ping) }
                    testScheduler.advanceUntilIdle()

                    assertWithMessage(kind.name).that(probe.dispatcher).isSameInstanceAs(scopeDispatcher)
                    router.shutdown()
                }
            }
        }

    @TestFactory
    fun `wrapped feature HasFeatureScope is cancelled on shutdown`(): List<DynamicTest> =
        wrapperKinds.map { kind ->
            dynamicTest(kind.name) {
                runTest {
                    val dispatcher = StandardTestDispatcher(testScheduler)
                    val scope = DefaultFeatureScope(dispatcher)
                    val wrapped = kind.scoped(Probe(), scope)

                    val router = FeatureRouter(features = setOf(wrapped.feature))
                    router.initialize(this, recordingConfig(dispatcher))
                    testScheduler.advanceUntilIdle()
                    assertWithMessage(kind.name).that(scope.featureScope.isActive).isTrue()

                    router.shutdown()

                    assertWithMessage(kind.name).that(scope.featureScope.isActive).isFalse()
                }
            }
        }

    @TestFactory
    fun `provideFeatureDispatcher receives the original feature, not the wrapper`(): List<DynamicTest> =
        wrapperKinds.map { kind ->
            dynamicTest(kind.name) {
                runTest {
                    val dispatcher = StandardTestDispatcher(testScheduler)
                    val config = recordingConfig(dispatcher)
                    val wrapped = kind.plain()

                    val router = FeatureRouter(features = setOf(wrapped.feature))
                    router.initialize(this, config)
                    testScheduler.advanceUntilIdle()

                    assertWithMessage(kind.name).that(config.features).containsExactly(wrapped.original)
                    router.shutdown()
                }
            }
        }

    @TestFactory
    fun `error context exposes the original feature of a failing wrapped feature`(): List<DynamicTest> =
        wrapperKinds.map { kind ->
            dynamicTest(kind.name) {
                runTest {
                    val dispatcher = StandardTestDispatcher(testScheduler)
                    val wrapped = kind.failing()
                    val contexts = mutableListOf<MviErrorContext>()

                    val router = FeatureRouter(features = setOf(wrapped.feature))
                    router.initialize(
                        this,
                        recordingConfig(dispatcher),
                        MviExceptionHandler { context, _ -> contexts += context },
                    )
                    testScheduler.advanceUntilIdle()
                    launch { router.dispatchIntention(Ping) }
                    testScheduler.advanceUntilIdle()

                    val context = contexts.single()
                    assertWithMessage(kind.name).that(context.source).isEqualTo(ErrorSource.FEATURE)
                    assertWithMessage(kind.name).that(context.feature).isSameInstanceAs(wrapped.feature)
                    assertWithMessage(kind.name).that(context.originalFeature).isSameInstanceAs(wrapped.original)
                    router.shutdown()
                }
            }
        }

    @Test
    fun `error context originalFeature falls back to the feature itself or null`() {
        val raw = Feature.FlowFeature<WrapState> { it.map { StateOutcome { s -> s } } }

        assertWithMessage("raw").that(MviErrorContext(ErrorSource.FEATURE, feature = raw).originalFeature)
            .isSameInstanceAs(raw)
        assertWithMessage("none").that(MviErrorContext(ErrorSource.REDUCER).originalFeature).isNull()
    }

    @Test
    fun `TypedFeature with HasFeatureDispatcher runs on its own dispatcher`() = runTest {
        val featureDispatcher = StandardTestDispatcher(testScheduler, name = "feature")
        val configDispatcher = StandardTestDispatcher(testScheduler, name = "config")
        val probe = Probe()
        val original = object : TypedFeature<WrapState, Ping>, HasFeatureDispatcher {
            override val featureDispatcher = featureDispatcher
            override fun invoke(intention: Flow<Ping>): Flow<Outcome<WrapState>> =
                intention.map {
                    probe.capture()
                    StateOutcome { s -> s }
                }
        }
        val config = recordingConfig(configDispatcher)

        val router = FeatureRouter(features = setOf(original.wrap()))
        router.initialize(this, config)
        testScheduler.advanceUntilIdle()
        launch { router.dispatchIntention(Ping) }
        testScheduler.advanceUntilIdle()

        assertWithMessage("feature dispatcher").that(probe.dispatcher).isSameInstanceAs(featureDispatcher)
        assertWithMessage("config consulted for features").that(config.features).isEmpty()
        router.shutdown()
    }
}

private class RecordingDispatcherConfig(private val dispatcher: CoroutineDispatcher) :
    CoroutineDispatcherConfig {
    val features = mutableListOf<Any>()
    override fun provideIntentionDispatcher(intention: Any?) = dispatcher
    override fun provideReducerDispatcher() = dispatcher
    override fun provideFeatureDispatcher(feature: Any): CoroutineDispatcher {
        features += feature
        return dispatcher
    }
}

private fun recordingConfig(dispatcher: CoroutineDispatcher) = RecordingDispatcherConfig(dispatcher)
