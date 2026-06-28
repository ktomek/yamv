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

## UI State Projection

`store.state` exposes the full business-logic `State`. In real apps that state often
carries internal bookkeeping the UI never renders — retry counters, validation tokens,
pagination cursors. Because Compose recomposes whenever the collected value changes, a
screen observing the full state recomposes on every internal change, even when nothing
visible moved.

YAMV offers an **opt-in** projection from `State` to a UI-only type. `store.state`
remains the default path; reach for projection only when internal churn causes wasted
recompositions.

### `UiMappable` + `uiState()`

Have the state declare its UI projection by implementing `UiMappable<UiS>`:

```kotlin
data class ProfileState(
    val name: String = "",
    val email: String = "",
    val isSaving: Boolean = false,
    // internal — never rendered:
    val saveAttempts: Int = 0,
    val lastValidationToken: Long = 0L,
) : State, UiMappable<ProfileUiState> {
    override fun toUiState() = ProfileUiState(
        name = name,
        email = email,
        isSaving = isSaving,
        canSave = name.isNotBlank() && email.contains("@") && !isSaving, // derived
    )
}

data class ProfileUiState(
    val name: String,
    val email: String,
    val isSaving: Boolean,
    val canSave: Boolean,
)
```

In Compose, collect the projection with the type-safe `uiState()` shorthand. It applies
`distinctUntilChanged`, so the screen only recomposes when a projected value actually
changes — bumping `saveAttempts` re-emits `State` but does **not** recompose the UI:

```kotlin
@Composable
fun ProfileScreen(store: ProfileStateStore = hiltMviStore()) {
    val scope = rememberCoroutineScope()
    val ui by remember(store, scope) {
        store.uiState<ProfileState, ProfileUiState>(scope)
    }.collectAsState()

    // ui.name, ui.email, ui.canSave, ui.isSaving
}
```

### `mapToUiState()` — any mapper

When the state does not implement `UiMappable` (or you want a screen-specific
projection), map the `StateFlow` directly:

```kotlin
val ui by remember(store, scope) {
    store.state.mapToUiState(scope) { state ->
        ProfileUiState(
            name = state.name,
            email = state.email,
            isSaving = state.isSaving,
            canSave = state.name.isNotBlank() && state.email.contains("@"),
        )
    }
}.collectAsState()
```

Both helpers live in `:yamv` and work on every Kotlin Multiplatform target, so the same
projection applies under Hilt or Koin. A runnable end-to-end demo lives in the Hilt
sample app (`app/hilt`, the **Profile** screen), which shows a projected collector and a
raw-state collector side by side so the difference in recompositions is visible.
