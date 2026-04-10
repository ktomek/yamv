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
```

**The cycle:** UI dispatches an intention → Store routes it to matching features → features emit outcomes → outcomes update state → UI observes new state.

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

Three abstraction levels (choose the simplest one that fits):

| Type | API | When to use |
|------|-----|-------------|
| `Feature.FlowFeature<S>` | `(Flow<Any>) -> Flow<Outcome<S>>` | Low-level; handles multiple intention types |
| `TypedFeature<S, I>` | `(Flow<I>) -> Flow<Outcome<S>>` | Typed; one feature per intention type |
| `FunctionTypedFeature<S, I>` | `suspend (I) -> Outcome<S>` | Simplest; one outcome per intention |

Use `.wrap()` to convert `TypedFeature` or `FunctionTypedFeature` to `Feature<S>` when wiring manually. With `@AutoFeature` (Hilt) or `mviStore {}` DSL (Koin), wrapping is automatic.

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

**Per-state config (Hilt):** Use the `@MviDispatcherConfig` qualifier to inject a custom config per state type:

```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object MyDispatcherModule {
    @Provides @MviDispatcherConfig(CounterState::class)
    fun provide(): CoroutineDispatcherConfig = CustomDispatcherConfig()
}
```

!!! info "Subscription safety"
    `FeatureRouter` uses `CompletableDeferred` to ensure all features are subscribed to the intention `SharedFlow` before the first intention is dispatched. This prevents race conditions at startup.

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
    Runtime->>Router: initialize(scope, config)
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
