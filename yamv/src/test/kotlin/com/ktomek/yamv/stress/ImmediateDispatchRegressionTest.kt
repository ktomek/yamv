package com.ktomek.yamv.stress

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.ActionTypedFeature
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.TypedFeature
import com.ktomek.yamv.feature.TypedUnitFeature
import com.ktomek.yamv.feature.functionTypedFeature
import com.ktomek.yamv.feature.wrap
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import com.ktomek.yamv.state.MviRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Regression tests for #69: an intention dispatched immediately after store creation must not be
 * silently dropped. Two independent causes produced this symptom, both fixed:
 *
 * 1. [com.ktomek.yamv.intention.FeatureRouter]'s readiness gate opened before the typed-feature
 *    wrappers (which subscribe to the intentions flow inside a `channelFlow`) had actually
 *    subscribed, so an `emit` in that window hit a `replay = 0` SharedFlow with no collector and
 *    was discarded.
 * 2. [com.ktomek.yamv.state.MviRuntime.dispatch] could race ahead of `initialize()`, so
 *    `dispatchIntention` threw "Router has not been initialized"; that failure reached the scope's
 *    exception handler but was swallowed because the default `Yamv` log level is `NONE` — the
 *    intention silently vanished.
 *
 * These tests use the same real-dispatcher harness as [MviRuntimeFlowFeatureIntegrationTest]
 * (single-threaded intentions/reducers, multi-threaded features) and dispatch immediately with
 * NO settle delay — the settle is precisely the workaround this fix removes. They MUST fail on the
 * pre-fix code and pass after it.
 */
@Timeout(30, unit = TimeUnit.SECONDS)
class ImmediateDispatchRegressionTest {

    private data class RegState(
        val applied: Boolean = false,
        val count: Int = 0,
    ) : State

    private sealed interface RegIntention {
        data object Load : RegIntention
    }

    /** Mirrors production: single-threaded intentions/reducers, multi-threaded features. */
    private fun realDispatcherConfig() = object : CoroutineDispatcherConfig {
        override fun provideIntentionDispatcher(intention: Any?) = Dispatchers.Default.limitedParallelism(1)
        override fun provideReducerDispatcher() = Dispatchers.Default.limitedParallelism(1)
        override fun provideFeatureDispatcher(feature: Any) = Dispatchers.Default
    }

    private suspend fun awaitState(
        runtime: MviRuntime<RegState>,
        condition: (RegState) -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
        while (!condition(runtime.state.value)) {
            if (System.currentTimeMillis() > deadline) break
            delay(POLL_INTERVAL_MS)
        }
    }

    private suspend fun awaitTrue(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) break
            delay(POLL_INTERVAL_MS)
        }
    }

    // --- Deterministic (Flow -> Flow wrappers: subscription timing is under the feature's control) ---

    @Test
    fun `TypedFeature receives intention dispatched immediately after construction`() = runBlocking {
        val feature = TypedFeature<RegState, RegIntention> { intentions ->
            // Subscribe to intentions LATE and deterministically: this widens the
            // mark-before-subscribe window so a pre-fix dispatch is guaranteed to be dropped.
            flow {
                delay(LATE_SUBSCRIBE_MS)
                emitAll(intentions.map { StateOutcome<RegState> { state -> state.copy(applied = true) } })
            }
        }.wrap()
        val runtime = MviRuntime(setOf(feature), RegState(), realDispatcherConfig())

        runtime.dispatch(RegIntention.Load) // immediate — no settle

        awaitState(runtime) { it.applied }
        assertThat(runtime.state.value.applied).isTrue()
        runtime.clear()
    }

    @Test
    fun `TypedUnitFeature receives intention dispatched immediately after construction`() = runBlocking {
        val received = AtomicBoolean(false)
        val feature = TypedUnitFeature<RegState, RegIntention> { intentions ->
            flow {
                delay(LATE_SUBSCRIBE_MS)
                emitAll(intentions.onEach { received.set(true) }.map { })
            }
        }.wrap()
        val runtime = MviRuntime(setOf(feature), RegState(), realDispatcherConfig())

        runtime.dispatch(RegIntention.Load) // immediate — no settle

        awaitTrue { received.get() }
        assertThat(received.get()).isTrue()
        runtime.clear()
    }

    // --- Bounded real-timing (single-intention wrappers: subscription timing is internal, so we
    // rely on the cross-thread scheduling hop making the drop overwhelmingly likely; repeat to be
    // reliable). ---

    @RepeatedTest(REPEATS)
    fun `FunctionTypedFeature receives intention dispatched immediately after construction`() = runBlocking {
        val feature = functionTypedFeature<RegState, RegIntention> {
            StateOutcome { state -> state.copy(count = state.count + 1) }
        }
        val runtime = MviRuntime(setOf(feature), RegState(), realDispatcherConfig())

        runtime.dispatch(RegIntention.Load) // immediate — no settle

        awaitState(runtime) { it.count == 1 }
        assertThat(runtime.state.value.count).isEqualTo(1)
        runtime.clear()
    }

    @RepeatedTest(REPEATS)
    fun `ActionTypedFeature receives intention dispatched immediately after construction`() = runBlocking {
        val received = AtomicBoolean(false)
        val feature = ActionTypedFeature<RegState, RegIntention> { received.set(true) }.wrap()
        val runtime = MviRuntime(setOf(feature), RegState(), realDispatcherConfig())

        runtime.dispatch(RegIntention.Load) // immediate — no settle

        awaitTrue { received.get() }
        assertThat(received.get()).isTrue()
        runtime.clear()
    }

    // --- #71 regression: a store that also contains a source-driven feature which ignores
    // intentions and never completes (e.g. `combine(sourceA, sourceB)`). Such a feature never marks
    // itself toward the readiness gate, which must NOT stall dispatch for the OTHER features. ---

    @Test
    fun `typed feature still receives immediate dispatch when store has an infinite intention-ignoring FlowFeature`() =
        runBlocking {
            val typed = functionTypedFeature<RegState, RegIntention> {
                StateOutcome<RegState> { state -> state.copy(applied = true) }
            }
            // Ignores intentions, never subscribes, never completes.
            val observer = Feature.FlowFeature<RegState> { _ -> flow<Outcome<RegState>> { awaitCancellation() } }
            val runtime = MviRuntime(setOf(typed, observer), RegState(), realDispatcherConfig())

            runtime.dispatch(RegIntention.Load) // immediate — no settle

            awaitState(runtime) { it.applied }
            assertThat(runtime.state.value.applied).isTrue()
            runtime.clear()
        }

    @Test
    fun `typed feature still receives dispatch after a settle when store has an infinite observer`() =
        runBlocking {
            val typed = functionTypedFeature<RegState, RegIntention> {
                StateOutcome<RegState> { state -> state.copy(applied = true) }
            }
            val observer = Feature.FlowFeature<RegState> { _ -> flow<Outcome<RegState>> { awaitCancellation() } }
            val runtime = MviRuntime(setOf(typed, observer), RegState(), realDispatcherConfig())

            // Even after settling, the gate never opens on the buggy code — proves a permanent hang,
            // not just an init race.
            delay(SETTLE_MS)
            runtime.dispatch(RegIntention.Load)

            awaitState(runtime) { it.applied }
            assertThat(runtime.state.value.applied).isTrue()
            runtime.clear()
        }

    @Test
    fun `typed feature still receives immediate dispatch when store has an infinite FlowUnitFeature observer`() =
        runBlocking {
            val received = AtomicBoolean(false)
            val typed = ActionTypedFeature<RegState, RegIntention> { received.set(true) }.wrap()
            val observer = Feature.FlowUnitFeature<RegState> { _ -> flow<Unit> { awaitCancellation() } }
            val runtime = MviRuntime(setOf(typed, observer), RegState(), realDispatcherConfig())

            runtime.dispatch(RegIntention.Load) // immediate — no settle

            awaitTrue { received.get() }
            assertThat(received.get()).isTrue()
            runtime.clear()
        }

    private companion object {
        const val AWAIT_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 20L
        const val LATE_SUBSCRIBE_MS = 500L
        const val SETTLE_MS = 300L
        const val REPEATS = 20
    }
}
