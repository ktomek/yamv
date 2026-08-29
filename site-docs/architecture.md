# Architecture

## MVI Data Flow

YAMV implements strict unidirectional data flow:

``` mermaid
flowchart LR
    UI(["🖥️ UI"])
    UI -->|"dispatch(Intention)"| Store
    Store(["🏪 Store"])
    Store -->|"routes to"| Features
    Features(["🧩 Features"])
    Features -->|"Outcome‹S›"| Store
    Store -->|"StateFlow‹S›"| UI
    Store -.->|"effects"| UI
```

**The cycle:** UI dispatches an intention → Store routes it to matching features → features emit outcomes → outcomes update state → UI observes new state.

> The UI observes the full `StateFlow<S>` by default. To project state down to a
> UI-only type and skip recompositions on internal changes, see
> [UI State Projection](features.md#ui-state-projection).

Outcomes come in three kinds:

- **`StateOutcome`** — pure `(S) → S` reducer, applied via `scan()`
- **`EffectOutcome`** — side effect (navigation, toast, analytics)
- **`IntentionOutcome`** — re-dispatches another intention back into the cycle

## Outcome Types

``` mermaid
flowchart LR
    feature(["🧩 Feature"]) --> outcome{"Outcome‹S›"}
    outcome -->|StateOutcome| reducer["🔄 reduces state\n`(S) → S`"]
    outcome -->|EffectOutcome| effect["⚡ side effect"]
    outcome -->|IntentionOutcome| intention["🔁 re-dispatches\nintention"]
```

Declare outcome subclasses as **standalone classes** — they are decoupled from features and testable in isolation. See [Features Guide](features.md#outcomes-as-separate-classes).

## Feature Abstraction Levels

Sealed `Feature<S>` has two raw shapes; four typed wrappers cover the common cases. Choose the simplest one that fits:

| Type | API | When to use |
|------|-----|-------------|
| `Feature.FlowFeature<S>` | `(Flow<Any>) -> Flow<Outcome<S>>` | Low-level; multi-intention or untyped |
| `Feature.FlowUnitFeature<S>` | `(Flow<Any>) -> Flow<Unit>` | Low-level fire-and-forget (no state contribution) |
| `TypedFeature<S, I>` | `(Flow<I>) -> Flow<Outcome<S>>` | Typed stream → outcomes |
| `FunctionTypedFeature<S, I>` | `suspend (I) -> Outcome<S>` | One outcome per intention |
| `ActionTypedFeature<S, I>` | `suspend (I) -> Unit` | One side effect per intention, no state contribution |
| `TypedUnitFeature<S, I>` | `(Flow<I>) -> Flow<Unit>` | Typed streamed side effects, no state contribution |

All four typed wrappers call `.wrap()` to become a `Feature<S>` — automatic with `@AutoFeature` (Hilt) or the Koin `mviStore { add(…) }` DSL, manual otherwise.

Use `functionTypedFeature<S, I> { }` builder for inline definitions.

## Dispatcher Architecture

`CoroutineDispatcherConfig` controls which dispatcher each part of the pipeline runs on:

```
MviRuntime owns CoroutineScope(SupervisorJob() + reducerDispatcher)
│
├── launch(reducerDispatcher)    ── Reducer collector: scan outcomes → update StateFlow
├── launch(reducerDispatcher)    ── Effect collector: forward EffectOutcomes
├── launch(intentionDispatcher)  ── IntentionOutcome collector: re-dispatch
│
└── FeatureRouter initialized with this scope
    │
    ├── launch(featureDispatcher) ── Feature A coroutine
    ├── launch(featureDispatcher) ── Feature B coroutine
    └── launch(featureDispatcher) ── Feature C coroutine
```

Default dispatchers (`DefaultCoroutineDispatcherConfig`):

| Operation | Dispatcher | Rationale |
|-----------|-----------|-----------|
| Intention dispatch | `Main` | UI-safe, async launch |
| Reducers (`scan`) | `Main` | State mutations must be serialized |
| Effects | `Main` | Observers expect UI thread |
| Features | `Default` | Non-blocking, concurrent processing |

### Customizing Dispatchers

Override `CoroutineDispatcherConfig` to control dispatcher assignment per intention or feature:

```kotlin
class CustomDispatcherConfig : CoroutineDispatcherConfig {
    // Single-threaded dispatcher for sequential feature processing
    private val singleThread = Dispatchers.Default.limitedParallelism(1)

    override fun provideIntentionDispatcher(intention: Any?) = Dispatchers.Main
    override fun provideReducerDispatcher() = Dispatchers.Main
    override fun provideFeatureDispatcher(feature: Any) = when (feature) {
        is NetworkFeature -> Dispatchers.IO
        else -> singleThread
    }
}
```

**Per-feature dispatcher:** Features can implement `HasFeatureDispatcher` to declare their own dispatcher, which takes precedence over `CoroutineDispatcherConfig`:

```kotlin
class NetworkFeature : TypedFeature<MyState, FetchData>, HasFeatureDispatcher {
    override val featureDispatcher = Dispatchers.IO
    // ...
}
```

Features with `HasFeatureScope` (via `DefaultFeatureScope`) automatically expose the scope's dispatcher.

**Per-state config (Hilt):** Annotate a `CoroutineDispatcherConfig` class with `@AutoDispatcherConfig` to have the Dagger module generated automatically:

```kotlin
// Global default — applies to all states without a specific override
@AutoDispatcherConfig
class IoDispatcherConfig : CoroutineDispatcherConfig { ... }

// Per-state — overrides default for CounterState only
@AutoDispatcherConfig(CounterState::class)
class CounterDispatcherConfig : CoroutineDispatcherConfig { ... }

// Multi-state — same config for several states
@AutoDispatcherConfig(TimerState::class, AnimationState::class)
class SharedConfig : CoroutineDispatcherConfig { ... }
```

Precedence: per-state > global default > `DefaultCoroutineDispatcherConfig()`

See [Code Generation — @AutoDispatcherConfig](code-generation.md#autodispatcherconfig) for generated output details.

!!! info "Subscription safety"
    `FeatureRouter` uses `CompletableDeferred` to ensure all features are subscribed to the intention `SharedFlow` before the first intention is dispatched. This prevents race conditions at startup.

## Exception Handling

`MviExceptionHandler` controls what happens when an exception occurs in the MVI pipeline. The default handler rethrows and cancels the runtime scope — a broken feature means illegal application state.

```kotlin
// Default: fail fast (recommended)
val runtime = MviRuntime(
    features = features,
    defaultState = MyState(),
)

// Custom: log + rethrow
val runtime = MviRuntime(
    features = features,
    defaultState = MyState(),
    exceptionHandler = MviExceptionHandler { context, e ->
        crashlytics.recordException(e)
        throw e  // still fail fast, but reported
    },
)
```

The handler receives `MviErrorContext` with:

| Field | Description |
|-------|-------------|
| `source: ErrorSource` | Where it happened: `REDUCER`, `FEATURE`, `EFFECT`, `INTENTION_REDISPATCH` |
| `intention: Any?` | The intention being processed (when available) |
| `feature: Feature<*>?` | The feature that failed (for `FEATURE` source) |

**Fail-fast behavior (default):** When the handler rethrows, the entire `CoroutineScope` is cancelled — all collectors (reducers, effects, intention re-dispatch) and all features stop. The runtime is dead.

**Degraded mode (opt-in):** If the handler does _not_ rethrow, the pipeline continues with the previous state. This is the user's explicit choice — the framework does not silently swallow exceptions.

## Lifecycle

``` mermaid
sequenceDiagram
    participant UI as 🖥️ UI
    participant Store as 🏪 MviRetainedStore
    participant Runtime as ⚙️ MviRuntime
    participant Router as 🚦 FeatureRouter
    participant F as 🧩 Features

    Note over Store,Runtime: Construction
    Store->>Runtime: create MviRuntime
    Runtime->>Router: initialize(scope, config, exceptionHandler)
    Router->>F: launch coroutine per feature
    F-->>Router: subscribe to intentionFlow
    Note over Router: CompletableDeferred ✅

    Note over UI,F: Dispatch
    UI->>Store: dispatch(intention)
    Store->>Runtime: scope.launch { dispatchIntention }
    Runtime->>Router: intentionFlow.emit(intention)
    Router->>F: intention broadcast
    F-->>Router: Outcome‹S›
    Router-->>Runtime: outcomeFlow
    Runtime-->>Runtime: scan → StateFlow
    Runtime-->>UI: StateFlow‹S› updated

    Note over UI,F: Cleanup
    UI->>Store: onCleared()
    Store->>Runtime: clear()
    Runtime->>Router: shutdown()
    Router->>F: cancel all jobs + scopes
```
