# Features Guide

A **Feature** transforms a stream of intentions into a stream of outcomes. Each feature is responsible for one piece of logic.

## Outcomes as Separate Classes

Declare `StateOutcome` subclasses as standalone classes. This decouples state transformations from features, making them independently testable with simple unit tests (no Flow, no coroutines):

```kotlin
// Outcome — pure function, tested in isolation
class IncrementOutcome : StateOutcome<CounterState> {
    override fun reduce(prevState: CounterState) =
        prevState.copy(count = prevState.count + 1)
}

class DecrementOutcome : StateOutcome<CounterState> {
    override fun reduce(prevState: CounterState) =
        prevState.copy(count = prevState.count - 1)
}

// Test — simple, no coroutines needed
@Test
fun `IncrementOutcome increments count`() {
    val result = IncrementOutcome().reduce(CounterState(count = 5))
    assertThat(result.count).isEqualTo(6)
}
```

Features then reference these outcomes:

```kotlin
val incrementFeature = functionTypedFeature<CounterState, CounterIntention.Increment> { _ ->
    IncrementOutcome()
}
```

This separation applies to all outcome types — `EffectOutcome` and `IntentionOutcome` subclasses benefit from the same pattern.

## FunctionTypedFeature — simplest

For one-shot operations (no streaming, no background work):

```kotlin
val incrementFeature = functionTypedFeature<CounterState, CounterIntention.Increment> { _ ->
    IncrementOutcome()
}
```

Or as a class:

```kotlin
class IncrementFeature : FunctionTypedFeature<CounterState, CounterIntention.Increment> {
    override suspend fun invoke(intention: CounterIntention.Increment): Outcome<CounterState> =
        IncrementOutcome()
}
```

> **Note:** Multiple concurrent intentions are processed concurrently. If your `invoke` does async work (network, DB), each intention gets its own coroutine automatically.

### Using `.wrap()`

All four typed feature shapes — `FunctionTypedFeature`, `TypedFeature`, `ActionTypedFeature`, `TypedUnitFeature` — must be converted to `Feature<S>` before passing to `MviRuntime`. If you're **not** using Hilt code generation (`@AutoFeature`) or the Koin `mviStore {}` DSL, call `.wrap()` manually:

```kotlin
val runtime = MviRuntime(
    features = setOf(
        IncrementFeature().wrap(),
        DecrementFeature().wrap(),
    ),
    defaultState = CounterState(),
)
```

With **Hilt** (`@AutoFeature`), the generated module calls `.wrap()` for you.
With **Koin** `mviStore {}` DSL, features are wrapped automatically via `FeatureRegistrar`.

## TypedFeature — typed streaming

For features that transform a stream of intentions. Operate directly on the `intentions` flow — use standard flow operators (`map`, `flatMapLatest`, `flatMapMerge`, etc.) to transform intentions into outcomes:

```kotlin
class FetchDataFeature(
    private val repository: DataRepository,
) : TypedFeature<AppState, AppIntention.Fetch> {
    override fun invoke(
        intentions: Flow<AppIntention.Fetch>
    ): Flow<Outcome<AppState>> =
        intentions
            .flatMapMerge { intention ->
                flow { emit(repository.fetch(intention.id)) }
                    .map(::DataLoadedOutcome)
                    .onStart { emit(LoadingOutcome()) }
            }
}
```

Use `flatMapMerge` for concurrent processing, `flatMapLatest` to cancel previous work on new intention, or `map` for simple 1:1 transforms:

```kotlin
class IncrementFeature : TypedFeature<CounterState, CounterIntention.Increment> {
    override fun invoke(
        intentions: Flow<CounterIntention.Increment>
    ): Flow<Outcome<CounterState>> =
        intentions.map { IncrementOutcome() }
}
```

## ActionTypedFeature — typed fire-and-forget

For typed side effects that contribute *no* state — analytics, logging, navigation triggers:

```kotlin
class TrackEventFeature(
    private val analytics: Analytics,
) : ActionTypedFeature<AppState, AppIntention.Track> {
    override suspend fun invoke(intention: AppIntention.Track) {
        analytics.log(intention.event)
    }
}
```

Each matching intention spawns its own coroutine, so slow actions do not block each other. The wrapped result is routed through the `FlowUnitFeature` branch — outcomes are not collected.

## TypedUnitFeature — typed streaming side effects

Like `TypedFeature`, but returns `Flow<Unit>` for streaming side effects with no state contribution:

```kotlin
class ConnectivityWatcherFeature(
    private val connectivity: Connectivity,
) : TypedUnitFeature<AppState, AppIntention.WatchConnectivity> {
    override fun invoke(intentions: Flow<AppIntention.WatchConnectivity>): Flow<Unit> =
        intentions
            .flatMapLatest { connectivity.observe() }
            .map { /* push to a tracker, no Outcome */ }
}
```

## FlowFeature — low-level

For features that need to handle multiple intention types or work with the raw `Flow<Any>`:

```kotlin
class LoggingFeature : Feature.FlowUnitFeature<AppState> {
    override fun invoke(intentions: Flow<Any>): Flow<Unit> =
        intentions.onEach { intention ->
            Log.d("YAMV", "Intention dispatched: $intention")
        }
}
```

`Feature.FlowFeature<S>` (returns `Flow<Outcome<S>>`) and `Feature.FlowUnitFeature<S>` (returns `Flow<Unit>`) are the two raw shapes; the four typed wrappers above sugar these for the common cases.

## Combining Outcomes

A single feature can emit multiple outcome types using flow operators. Keep the chain simple — one operator per line, extract to a function if it grows:

```kotlin
class LoginFeature(
    private val auth: AuthService,
) : TypedFeature<AppState, AppIntention.Login> {
    override fun invoke(
        intentions: Flow<AppIntention.Login>
    ): Flow<Outcome<AppState>> =
        intentions
            .flatMapMerge { intention -> login(intention) }

    private fun login(intention: AppIntention.Login): Flow<Outcome<AppState>> =
        flow { emit(auth.login(intention.email, intention.password)) }
            .map<User, Outcome<AppState>> { user -> LoginSuccessOutcome(user) }
            .onStart { emit(LoginLoadingOutcome()) }
            .catch { e -> emit(LoginErrorOutcome(e.message)) }
}
```
